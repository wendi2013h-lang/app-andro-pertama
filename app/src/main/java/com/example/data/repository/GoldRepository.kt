package com.example.data.repository

import com.example.data.local.AlertDao
import com.example.data.model.AlertHistory
import com.example.data.model.CandleData
import com.example.data.model.GoldPriceState
import com.example.data.model.OrderBookEntry
import com.example.data.model.PivotPoints
import com.example.data.model.PriceAlert
import com.example.data.model.TechnicalSummary
import com.example.data.model.TimeFrame
import com.example.data.remote.GoldMarketEngine
import kotlinx.coroutines.flow.Flow
import kotlin.math.max
import kotlin.math.min

class GoldRepository(
    private val alertDao: AlertDao,
    private val marketEngine: GoldMarketEngine = GoldMarketEngine()
) {

    // Database Flows
    val allAlerts: Flow<List<PriceAlert>> = alertDao.getAllAlerts()
    val activeAlerts: Flow<List<PriceAlert>> = alertDao.getActiveAlerts()
    val alertHistory: Flow<List<AlertHistory>> = alertDao.getAlertHistory()

    // Alert CRUD
    suspend fun insertAlert(alert: PriceAlert): Long = alertDao.insertAlert(alert)

    suspend fun updateAlert(alert: PriceAlert) = alertDao.updateAlert(alert)

    suspend fun deleteAlert(alert: PriceAlert) = alertDao.deleteAlert(alert)

    suspend fun deleteAlertById(id: Long) = alertDao.deleteAlertById(id)

    suspend fun setAlertActive(id: Long, isActive: Boolean) = alertDao.setAlertActive(id, isActive)

    suspend fun markAlertTriggered(id: Long, triggeredAt: Long) = alertDao.markAlertTriggered(id, triggeredAt)

    // History CRUD
    suspend fun addAlertHistory(history: AlertHistory): Long = alertDao.insertAlertHistory(history)

    suspend fun clearHistory() = alertDao.clearAlertHistory()

    suspend fun deleteHistoryById(id: Long) = alertDao.deleteHistoryById(id)

    // Market Data
    fun startLivePriceStream(intervalMs: Long = 1500L): Flow<GoldPriceState> {
        return marketEngine.startLivePriceStream(intervalMs)
    }

    suspend fun fetchCandles(timeFrame: TimeFrame): List<CandleData> {
        return marketEngine.fetchRealCandles(timeFrame) ?: marketEngine.getCandles(timeFrame)
    }

    fun getOrderBook(price: Double): Pair<List<OrderBookEntry>, List<OrderBookEntry>> {
        return marketEngine.getLiveOrderBook(price)
    }

    fun calibratePrice(targetPrice: Double) {
        marketEngine.calibrateToTargetPrice(targetPrice)
    }

    fun setDataSource(source: com.example.data.model.DataSourceProvider) {
        marketEngine.setDataSource(source)
    }

    fun resetCalibration() {
        marketEngine.resetCalibration()
    }

    // Technical & Calculations
    fun calculatePivotPoints(high: Double, low: Double, close: Double): PivotPoints {
        val p = (high + low + close) / 3.0
        val r1 = (2 * p) - low
        val s1 = (2 * p) - high
        val r2 = p + (high - low)
        val s2 = p - (high - low)
        val r3 = high + 2 * (p - low)
        val s3 = low - 2 * (high - p)
        return PivotPoints(pivot = p, r1 = r1, r2 = r2, r3 = r3, s1 = s1, s2 = s2, s3 = s3)
    }

    fun calculateTechnicalSummary(candles: List<CandleData>, currentPrice: Double): TechnicalSummary {
        if (candles.size < 14) {
            return TechnicalSummary()
        }

        // Calculate RSI 14
        var gains = 0.0
        var losses = 0.0
        val period = min(14, candles.size - 1)
        val recentCandles = candles.takeLast(period + 1)
        for (i in 1 until recentCandles.size) {
            val change = recentCandles[i].close - recentCandles[i - 1].close
            if (change >= 0) gains += change else losses += -change
        }
        val avgGain = gains / period
        val avgLoss = if (losses == 0.0) 0.001 else losses / period
        val rs = avgGain / avgLoss
        val rsi = 100 - (100 / (1 + rs))

        // EMA 20
        val ema20 = calculateEMA(candles.map { it.close }, 20)

        val rsiSignal = when {
            rsi >= 70 -> "Overbought (Sell Warning)"
            rsi <= 30 -> "Oversold (Buy Signal)"
            rsi > 55 -> "Bullish Momentum"
            rsi < 45 -> "Bearish Pressure"
            else -> "Neutral"
        }

        val emaStatus = if (currentPrice >= ema20) "Above EMA 20 (Bullish)" else "Below EMA 20 (Bearish)"
        val sentimentScore = ((rsi * 0.5) + (if (currentPrice > ema20) 40 else 10)).toInt().coerceIn(10, 95)
        val sentiment = when {
            sentimentScore >= 75 -> "Strong Buy"
            sentimentScore >= 55 -> "Buy"
            sentimentScore >= 45 -> "Neutral"
            sentimentScore >= 30 -> "Sell"
            else -> "Strong Sell"
        }

        return TechnicalSummary(
            rsi14 = rsi,
            rsiSignal = rsiSignal,
            macdStatus = if (currentPrice > ema20) "Bullish Trend" else "Bearish Trend",
            ema20Status = emaStatus,
            overallSentiment = sentiment,
            sentimentScore = sentimentScore
        )
    }

    fun calculateMarketSentiment(
        candles: List<CandleData>,
        priceState: GoldPriceState,
        orderBookBids: List<OrderBookEntry>,
        orderBookAsks: List<OrderBookEntry>
    ): com.example.data.model.MarketSentimentData {
        return MarketSentimentCalculator.calculateSentiment(
            candles = candles,
            priceState = priceState,
            orderBookBids = orderBookBids,
            orderBookAsks = orderBookAsks
        )
    }

    private fun calculateEMA(prices: List<Double>, period: Int): Double {
        if (prices.isEmpty()) return 0.0
        val k = 2.0 / (period + 1)
        var ema = prices.first()
        for (i in 1 until prices.size) {
            ema = (prices[i] * k) + (ema * (1 - k))
        }
        return ema
    }
}
