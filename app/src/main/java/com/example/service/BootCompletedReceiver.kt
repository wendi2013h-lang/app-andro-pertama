package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val isEnabled = BackgroundMonitorManager.isBackgroundServiceEnabled(context)
            if (isEnabled) {
                // Check if there are active pending alerts
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getDatabase(context)
                        val count = db.alertDao().getPendingActiveAlertsCount()
                        if (count > 0) {
                            BackgroundMonitorManager.startService(context)
                        }
                    } catch (e: Exception) {
                        // In case database isn't ready immediately
                        BackgroundMonitorManager.startService(context)
                    }
                }
            }
        }
    }
}
