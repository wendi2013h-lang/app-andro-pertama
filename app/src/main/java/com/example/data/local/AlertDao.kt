package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AlertHistory
import com.example.data.model.PriceAlert
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {
    @Query("SELECT * FROM price_alerts ORDER BY createdAt DESC")
    fun getAllAlerts(): Flow<List<PriceAlert>>

    @Query("SELECT * FROM price_alerts WHERE isActive = 1")
    fun getActiveAlerts(): Flow<List<PriceAlert>>

    @Query("SELECT * FROM price_alerts WHERE isActive = 1 AND isTriggered = 0")
    suspend fun getPendingActiveAlertsSnapshot(): List<PriceAlert>

    @Query("SELECT COUNT(*) FROM price_alerts WHERE isActive = 1 AND isTriggered = 0")
    suspend fun getPendingActiveAlertsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: PriceAlert): Long

    @Update
    suspend fun updateAlert(alert: PriceAlert)

    @Delete
    suspend fun deleteAlert(alert: PriceAlert)

    @Query("DELETE FROM price_alerts WHERE id = :id")
    suspend fun deleteAlertById(id: Long)

    @Query("UPDATE price_alerts SET isActive = :isActive WHERE id = :id")
    suspend fun setAlertActive(id: Long, isActive: Boolean)

    @Query("UPDATE price_alerts SET isTriggered = 1, triggeredAt = :triggeredAt WHERE id = :id")
    suspend fun markAlertTriggered(id: Long, triggeredAt: Long)

    // History
    @Query("SELECT * FROM alert_history ORDER BY timestamp DESC LIMIT 100")
    fun getAlertHistory(): Flow<List<AlertHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlertHistory(history: AlertHistory): Long

    @Query("DELETE FROM alert_history")
    suspend fun clearAlertHistory()

    @Query("DELETE FROM alert_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)
}
