package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AlertType {
    PRICE_ABOVE,
    PRICE_BELOW,
    PERCENT_SURGE,
    PERCENT_DROP
}

@Entity(tableName = "price_alerts")
data class PriceAlert(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val targetPrice: Double,
    val alertType: AlertType,
    val referencePrice: Double = 0.0,
    val percentThreshold: Double = 0.0,
    val isActive: Boolean = true,
    val isTriggered: Boolean = false,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val triggeredAt: Long? = null
)

@Entity(tableName = "alert_history")
data class AlertHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val alertId: Long,
    val title: String,
    val message: String,
    val price: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
