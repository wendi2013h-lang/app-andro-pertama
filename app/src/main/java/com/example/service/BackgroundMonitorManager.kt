package com.example.service

import android.content.Context
import android.content.SharedPreferences

object BackgroundMonitorManager {

    private const val PREFS_NAME = "xauusd_monitor_prefs"
    private const val KEY_SERVICE_ENABLED = "key_background_monitor_enabled"
    private const val KEY_AUTO_START_ON_ALERT = "key_auto_start_on_alert"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isBackgroundServiceEnabled(context: Context): Boolean {
        // Defaults to true so users get reliable background alerts as expected
        return getPrefs(context).getBoolean(KEY_SERVICE_ENABLED, true)
    }

    fun setBackgroundServiceEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SERVICE_ENABLED, enabled).apply()
        if (enabled) {
            startService(context)
        } else {
            stopService(context)
        }
    }

    fun startService(context: Context) {
        try {
            GoldPriceMonitorService.start(context)
        } catch (e: Exception) {
            // Ignored to avoid crash if OS blocks foreground service
        }
    }

    fun stopService(context: Context) {
        try {
            GoldPriceMonitorService.stop(context)
        } catch (e: Exception) {
            // Ignored
        }
    }

    fun isServiceRunning(): Boolean {
        return GoldPriceMonitorService.isRunning.value
    }
}
