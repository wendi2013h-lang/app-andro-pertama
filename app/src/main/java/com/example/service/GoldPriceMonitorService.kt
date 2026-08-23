package com.example.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.example.data.local.AppDatabase
import com.example.data.model.AlertHistory
import com.example.data.model.AlertType
import com.example.data.model.GoldPriceState
import com.example.data.model.PriceAlert
import com.example.data.remote.GoldMarketEngine
import com.example.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class GoldPriceMonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var monitoringJob: Job? = null

    private lateinit var notificationHelper: NotificationHelper
    private lateinit var database: AppDatabase
    private val marketEngine = GoldMarketEngine()

    private var lastObservedPrice: Double = 0.0

    companion object {
        const val ACTION_START = "com.example.action.START_MONITORING_SERVICE"
        const val ACTION_STOP = "com.example.action.STOP_MONITORING_SERVICE"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _lastMonitoredPrice = MutableStateFlow(0.0)
        val lastMonitoredPrice: StateFlow<Double> = _lastMonitoredPrice.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, GoldPriceMonitorService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, GoldPriceMonitorService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(applicationContext)
        database = AppDatabase.getDatabase(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        if (action == ACTION_STOP || action == NotificationHelper.ACTION_STOP_SERVICE) {
            stopMonitoring()
            stopSelf()
            return START_NOT_STICKY
        }

        // Start Foreground Service with sticky ongoing notification
        val initialNotification = notificationHelper.buildForegroundServiceNotification(
            currentPrice = lastObservedPrice,
            activeAlertsCount = 0
        )

        val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }

        try {
            ServiceCompat.startForeground(
                this,
                NotificationHelper.FOREGROUND_NOTIFICATION_ID,
                initialNotification,
                foregroundType
            )
        } catch (e: Exception) {
            // Fallback for older or restricted environments
            startForeground(NotificationHelper.FOREGROUND_NOTIFICATION_ID, initialNotification)
        }

        _isRunning.value = true
        startMonitoringLoop()

        return START_STICKY
    }

    private fun startMonitoringLoop() {
        monitoringJob?.cancel()
        monitoringJob = serviceScope.launch {
            var tickCount = 0
            while (isActive) {
                try {
                    // Fetch real market price or simulated accurate tick
                    val realState = marketEngine.fetchRealGoldPrice()
                    val currentState = realState ?: marketEngine.generateNextTick()

                    lastObservedPrice = currentState.currentPrice
                    _lastMonitoredPrice.value = currentState.currentPrice

                    // Fetch active pending alerts from database
                    val pendingAlerts = database.alertDao().getPendingActiveAlertsSnapshot()

                    // Check thresholds and evaluate
                    evaluateAlerts(currentState, pendingAlerts)

                    // Update ongoing foreground notification every 3 ticks (~4.5s) to preserve battery
                    tickCount++
                    if (tickCount % 3 == 0) {
                        val updatedNotification = notificationHelper.buildForegroundServiceNotification(
                            currentPrice = currentState.currentPrice,
                            activeAlertsCount = pendingAlerts.size
                        )
                        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.notify(
                            NotificationHelper.FOREGROUND_NOTIFICATION_ID,
                            updatedNotification
                        )
                    }

                } catch (e: Exception) {
                    // Resilience against network dropouts
                }

                // Poll interval: 2 seconds
                delay(2000L)
            }
        }
    }

    private suspend fun evaluateAlerts(state: GoldPriceState, alerts: List<PriceAlert>) {
        val currentPrice = state.currentPrice

        for (alert in alerts) {
            if (!alert.isActive || alert.isTriggered) continue

            var triggered = false
            var title = ""
            var message = ""

            when (alert.alertType) {
                AlertType.PRICE_ABOVE -> {
                    if (currentPrice >= alert.targetPrice) {
                        triggered = true
                        title = "🎯 Target Harga Tercapai (Naik)!"
                        message = "Emas XAU/USD menembus $${String.format(Locale.US, "%.2f", alert.targetPrice)} (Harga Terkini: $${String.format(Locale.US, "%.2f", currentPrice)})"
                    }
                }
                AlertType.PRICE_BELOW -> {
                    if (currentPrice <= alert.targetPrice) {
                        triggered = true
                        title = "⚠️ Peringatan Harga Turun!"
                        message = "Emas XAU/USD turun di bawah $${String.format(Locale.US, "%.2f", alert.targetPrice)} (Harga Terkini: $${String.format(Locale.US, "%.2f", currentPrice)})"
                    }
                }
                AlertType.PERCENT_SURGE -> {
                    val ref = if (alert.referencePrice > 0) alert.referencePrice else state.open24h
                    val surgePct = if (ref > 0) ((currentPrice - ref) / ref) * 100 else 0.0
                    if (surgePct >= alert.percentThreshold) {
                        triggered = true
                        title = "🚀 Lonjakan Harga Terdeteksi!"
                        message = "XAU/USD melonjak +${String.format(Locale.US, "%.2f", surgePct)}% ke $${String.format(Locale.US, "%.2f", currentPrice)}"
                    }
                }
                AlertType.PERCENT_DROP -> {
                    val ref = if (alert.referencePrice > 0) alert.referencePrice else state.open24h
                    val dropPct = if (ref > 0) ((ref - currentPrice) / ref) * 100 else 0.0
                    if (dropPct >= alert.percentThreshold) {
                        triggered = true
                        title = "📉 Penurunan Cepat Terdeteksi!"
                        message = "XAU/USD turun -${String.format(Locale.US, "%.2f", dropPct)}% ke $${String.format(Locale.US, "%.2f", currentPrice)}"
                    }
                }
            }

            if (triggered) {
                val now = System.currentTimeMillis()
                // Mark alert as triggered in local Room database
                database.alertDao().markAlertTriggered(alert.id, now)

                // Save to Alert History
                database.alertDao().insertAlertHistory(
                    AlertHistory(
                        alertId = alert.id,
                        title = title,
                        message = message,
                        price = currentPrice,
                        timestamp = now
                    )
                )

                // Trigger high-priority local notification with sound & vibration
                notificationHelper.showPriceAlertNotification(
                    notificationId = alert.id.toInt(),
                    title = title,
                    message = message,
                    currentPrice = currentPrice
                )
            }
        }
    }

    private fun stopMonitoring() {
        monitoringJob?.cancel()
        _isRunning.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        stopMonitoring()
        serviceScope.cancel()
        _isRunning.value = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
