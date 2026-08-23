package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AlertHistory
import com.example.data.model.AlertType
import com.example.data.model.CandleData
import com.example.data.model.GoldPriceState
import com.example.data.model.OrderBookEntry
import com.example.data.model.PivotPoints
import com.example.data.model.PriceAlert
import com.example.data.model.TechnicalSummary
import com.example.data.model.TimeFrame
import com.example.data.repository.GoldRepository
import com.example.notification.NotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TriggeredAlertEvent(
    val title: String,
    val message: String,
    val price: Double,
    val timestamp: Long = System.currentTimeMillis()
)

class GoldViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GoldRepository
    private val notificationHelper: NotificationHelper = NotificationHelper(application)

    // UI States
    private val _priceState = MutableStateFlow(GoldPriceState())
    val priceState: StateFlow<GoldPriceState> = _priceState.asStateFlow()

    private val _selectedTimeFrame = MutableStateFlow(TimeFrame.M15)
    val selectedTimeFrame: StateFlow<TimeFrame> = _selectedTimeFrame.asStateFlow()

    private val _candles = MutableStateFlow<List<CandleData>>(emptyList())
    val candles: StateFlow<List<CandleData>> = _candles.asStateFlow()

    private val _orderBookBids = MutableStateFlow<List<OrderBookEntry>>(emptyList())
    val orderBookBids: StateFlow<List<OrderBookEntry>> = _orderBookBids.asStateFlow()

    private val _orderBookAsks = MutableStateFlow<List<OrderBookEntry>>(emptyList())
    val orderBookAsks: StateFlow<List<OrderBookEntry>> = _orderBookAsks.asStateFlow()

    private val _pivotPoints = MutableStateFlow(PivotPoints(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0))
    val pivotPoints: StateFlow<PivotPoints> = _pivotPoints.asStateFlow()

    private val _technicalSummary = MutableStateFlow(TechnicalSummary())
    val technicalSummary: StateFlow<TechnicalSummary> = _technicalSummary.asStateFlow()

    private val _marketSentiment = MutableStateFlow(
        com.example.data.repository.MarketSentimentCalculator.calculateSentiment(
            candles = emptyList(),
            priceState = GoldPriceState(),
            orderBookBids = emptyList(),
            orderBookAsks = emptyList()
        )
    )
    val marketSentiment: StateFlow<com.example.data.model.MarketSentimentData> = _marketSentiment.asStateFlow()

    private val _inAppAlert = MutableStateFlow<TriggeredAlertEvent?>(null)
    val inAppAlert: StateFlow<TriggeredAlertEvent?> = _inAppAlert.asStateFlow()

    private val _refreshSpeedMs = MutableStateFlow(1500L)
    val refreshSpeedMs: StateFlow<Long> = _refreshSpeedMs.asStateFlow()

    // Alert flows from Room
    val allAlerts: StateFlow<List<PriceAlert>>
    val alertHistory: StateFlow<List<AlertHistory>>

    private var streamJob: Job? = null

    init {
        val db = AppDatabase.getDatabase(application)
        repository = GoldRepository(db.alertDao())

        allAlerts = repository.allAlerts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        alertHistory = repository.alertHistory.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        loadCandlesForTimeFrame(TimeFrame.M15)
        startRealTimeStream()
    }

    fun setTimeFrame(timeFrame: TimeFrame) {
        _selectedTimeFrame.value = timeFrame
        loadCandlesForTimeFrame(timeFrame)
    }

    fun setRefreshSpeed(speedMs: Long) {
        _refreshSpeedMs.value = speedMs
        startRealTimeStream()
    }

    private fun loadCandlesForTimeFrame(timeFrame: TimeFrame) {
        viewModelScope.launch {
            val list = repository.fetchCandles(timeFrame)
            _candles.value = list
            updateTechnicals(list, _priceState.value.currentPrice)
        }
    }

    private fun startRealTimeStream() {
        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            repository.startLivePriceStream(_refreshSpeedMs.value).collect { state ->
                _priceState.value = state

                // Update Order Book
                val (bids, asks) = repository.getOrderBook(state.currentPrice)
                _orderBookBids.value = bids
                _orderBookAsks.value = asks

                // Update Pivot Points
                _pivotPoints.value = repository.calculatePivotPoints(
                    high = state.high24h,
                    low = state.low24h,
                    close = state.currentPrice
                )

                // Update Market Sentiment & Volatility Sweep Data
                _marketSentiment.value = repository.calculateMarketSentiment(
                    candles = _candles.value,
                    priceState = state,
                    orderBookBids = bids,
                    orderBookAsks = asks
                )

                // Check and trigger price alerts
                evaluateAlerts(state)
            }
        }
    }

    private fun updateTechnicals(candles: List<CandleData>, price: Double) {
        _technicalSummary.value = repository.calculateTechnicalSummary(candles, price)
        _marketSentiment.value = repository.calculateMarketSentiment(
            candles = candles,
            priceState = _priceState.value,
            orderBookBids = _orderBookBids.value,
            orderBookAsks = _orderBookAsks.value
        )
    }

    private suspend fun evaluateAlerts(state: GoldPriceState) {
        val currentAlerts = allAlerts.value
        val currentPrice = state.currentPrice

        for (alert in currentAlerts) {
            if (!alert.isActive || alert.isTriggered) continue

            var triggered = false
            var title = ""
            var message = ""

            when (alert.alertType) {
                AlertType.PRICE_ABOVE -> {
                    if (currentPrice >= alert.targetPrice) {
                        triggered = true
                        title = "🎯 Target Harga Tercapai (Naik)!"
                        message = "Emas XAU/USD menembus $${String.format("%.2f", alert.targetPrice)} (Saat ini: $${String.format("%.2f", currentPrice)})"
                    }
                }
                AlertType.PRICE_BELOW -> {
                    if (currentPrice <= alert.targetPrice) {
                        triggered = true
                        title = "⚠️ Peringatan Harga Turun!"
                        message = "Emas XAU/USD turun di bawah $${String.format("%.2f", alert.targetPrice)} (Saat ini: $${String.format("%.2f", currentPrice)})"
                    }
                }
                AlertType.PERCENT_SURGE -> {
                    val ref = if (alert.referencePrice > 0) alert.referencePrice else state.open24h
                    val surgePct = ((currentPrice - ref) / ref) * 100
                    if (surgePct >= alert.percentThreshold) {
                        triggered = true
                        title = "🚀 Lonjakan Harga Terdeteksi!"
                        message = "XAU/USD melonjak +${String.format("%.2f", surgePct)}% ke $${String.format("%.2f", currentPrice)}"
                    }
                }
                AlertType.PERCENT_DROP -> {
                    val ref = if (alert.referencePrice > 0) alert.referencePrice else state.open24h
                    val dropPct = ((ref - currentPrice) / ref) * 100
                    if (dropPct >= alert.percentThreshold) {
                        triggered = true
                        title = "📉 Penurunan Cepat Terdeteksi!"
                        message = "XAU/USD turun -${String.format("%.2f", dropPct)}% ke $${String.format("%.2f", currentPrice)}"
                    }
                }
            }

            if (triggered) {
                val now = System.currentTimeMillis()
                repository.markAlertTriggered(alert.id, now)

                // Save to Alert History
                repository.addAlertHistory(
                    AlertHistory(
                        alertId = alert.id,
                        title = title,
                        message = message,
                        price = currentPrice,
                        timestamp = now
                    )
                )

                // Trigger in-app banner
                _inAppAlert.value = TriggeredAlertEvent(title, message, currentPrice, now)

                // Trigger Android system notification
                notificationHelper.showPriceAlertNotification(
                    notificationId = alert.id.toInt().coerceAtLeast(1001),
                    title = title,
                    message = message,
                    currentPrice = currentPrice
                )
            }
        }
    }

    fun dismissInAppAlert() {
        _inAppAlert.value = null
    }

    fun createPriceAlert(
        targetPrice: Double,
        type: AlertType,
        percentThreshold: Double = 0.0,
        note: String = ""
    ) {
        viewModelScope.launch {
            val alert = PriceAlert(
                targetPrice = targetPrice,
                alertType = type,
                referencePrice = _priceState.value.currentPrice,
                percentThreshold = percentThreshold,
                isActive = true,
                isTriggered = false,
                note = note,
                createdAt = System.currentTimeMillis()
            )
            repository.insertAlert(alert)
            ensureBackgroundServiceStarted()
        }
    }

    fun toggleAlertActive(alert: PriceAlert) {
        viewModelScope.launch {
            if (alert.isTriggered) {
                // Reactivate
                repository.updateAlert(alert.copy(isActive = true, isTriggered = false, triggeredAt = null))
            } else {
                repository.setAlertActive(alert.id, !alert.isActive)
            }
        }
    }

    fun deleteAlert(alert: PriceAlert) {
        viewModelScope.launch {
            repository.deleteAlert(alert)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteHistoryById(id)
        }
    }

    // Quick Alert Presets
    fun addQuickAlert(deltaDollars: Double) {
        val target = _priceState.value.currentPrice + deltaDollars
        val type = if (deltaDollars >= 0) AlertType.PRICE_ABOVE else AlertType.PRICE_BELOW
        val note = if (deltaDollars >= 0) "Quick Target +$${deltaDollars}" else "Quick Stop -$${-deltaDollars}"
        createPriceAlert(targetPrice = target, type = type, note = note)
    }

    fun triggerTestAlert() {
        val currentPrice = _priceState.value.currentPrice
        val title = "🔔 Tes Notifikasi XAU/USD"
        val message = "Sistem notifikasi instan harga emas berfungsi normal pada level saat ini."
        _inAppAlert.value = TriggeredAlertEvent(title, message, currentPrice)
        notificationHelper.showPriceAlertNotification(9999, title, message, currentPrice)
    }

    // Price Calibration & Broker Source Controls
    fun calibrateToBrokerPrice(targetPrice: Double) {
        viewModelScope.launch {
            repository.calibratePrice(targetPrice)
            loadCandlesForTimeFrame(_selectedTimeFrame.value)
        }
    }

    fun setDataSource(source: com.example.data.model.DataSourceProvider) {
        viewModelScope.launch {
            repository.setDataSource(source)
            loadCandlesForTimeFrame(_selectedTimeFrame.value)
        }
    }

    fun resetCalibration() {
        viewModelScope.launch {
            repository.resetCalibration()
            loadCandlesForTimeFrame(_selectedTimeFrame.value)
        }
    }

    // Background 24/7 Price Monitoring Service Controls
    val isBackgroundServiceRunning: StateFlow<Boolean> = com.example.service.GoldPriceMonitorService.isRunning

    private val _isBackgroundServiceEnabled = MutableStateFlow(
        com.example.service.BackgroundMonitorManager.isBackgroundServiceEnabled(getApplication())
    )
    val isBackgroundServiceEnabled: StateFlow<Boolean> = _isBackgroundServiceEnabled.asStateFlow()

    fun toggleBackgroundService(enable: Boolean) {
        _isBackgroundServiceEnabled.value = enable
        com.example.service.BackgroundMonitorManager.setBackgroundServiceEnabled(getApplication(), enable)
    }

    fun ensureBackgroundServiceStarted() {
        if (_isBackgroundServiceEnabled.value) {
            com.example.service.BackgroundMonitorManager.startService(getApplication())
        }
    }
}

