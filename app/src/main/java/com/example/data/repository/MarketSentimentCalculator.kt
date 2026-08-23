package com.example.data.repository

import com.example.data.model.CandleData
import com.example.data.model.GoldPriceState
import com.example.data.model.LiquidityPoolZone
import com.example.data.model.LiquiditySweepWarning
import com.example.data.model.MarketSentimentData
import com.example.data.model.OrderBookEntry
import com.example.data.model.SweepRiskLevel
import com.example.data.model.VolatilityState
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object MarketSentimentCalculator {

    fun calculateSentiment(
        candles: List<CandleData>,
        priceState: GoldPriceState,
        orderBookBids: List<OrderBookEntry>,
        orderBookAsks: List<OrderBookEntry>
    ): MarketSentimentData {
        val currentPrice = priceState.currentPrice

        // 1. Calculate ATR (14 period)
        val atr = calculateATR(candles, period = 14).coerceAtLeast(10.0)

        // 2. Order Book Imbalance Calculation
        val totalBidVolume = orderBookBids.sumOf { it.amount }
        val totalAskVolume = orderBookAsks.sumOf { it.amount }
        val totalOrderBook = totalBidVolume + totalAskVolume
        val orderBookRatio = if (totalOrderBook > 0) {
            ((totalBidVolume / totalOrderBook) * 100).toInt().coerceIn(10, 90)
        } else {
            50
        }
        val orderFlowDelta = totalBidVolume - totalAskVolume

        // 3. RSI 14 calculation
        val rsi = calculateRSI(candles, period = 14).coerceIn(15.0, 85.0)

        // 4. Volatility State Determination
        val latestCandle = candles.lastOrNull()
        val latestRange = if (latestCandle != null) latestCandle.high - latestCandle.low else atr
        val volatilityRatio = latestRange / atr

        val volatilityState = when {
            volatilityRatio >= 2.0 -> VolatilityState.EXTREME_SPIKE
            volatilityRatio >= 1.3 -> VolatilityState.EXPANSION
            volatilityRatio <= 0.65 -> VolatilityState.COMPRESSION
            else -> VolatilityState.NORMAL
        }

        // 5. Bullish vs Bearish Percentage Composite
        // Weights: 35% RSI + 30% Order Book Imbalance + 35% 24h Change & Position vs 24h range
        val range24h = max(1.0, priceState.high24h - priceState.low24h)
        val pricePositionScore = (((currentPrice - priceState.low24h) / range24h) * 100).coerceIn(10.0, 90.0)

        val bullScore = (
            (rsi * 0.35) +
            (orderBookRatio * 0.30) +
            (pricePositionScore * 0.35)
        ).toInt().coerceIn(12, 88)

        val bearScore = 100 - bullScore

        val sentimentRating = when {
            bullScore >= 70 -> "Strong Bullish 🔥"
            bullScore >= 55 -> "Bullish Momentum 📈"
            bullScore in 46..54 -> "Neutral Consolidation ⚖️"
            bullScore in 30..45 -> "Bearish Pressure 📉"
            else -> "Strong Bearish ❄️"
        }

        val rsiStatus = when {
            rsi >= 70 -> "Overbought (Jenuh Beli)"
            rsi <= 30 -> "Oversold (Jenuh Jual)"
            rsi > 54 -> "Bullish Divergence"
            rsi < 46 -> "Bearish Pressure"
            else -> "Netral / Ranging"
        }

        val institutionalBias = when {
            bullScore >= 65 && volatilityState == VolatilityState.EXPANSION -> "Markup Phase (Smart Money Inflow)"
            bullScore >= 55 -> "Accumulation Range (Institutional Buying)"
            bullScore <= 35 && volatilityState == VolatilityState.EXPANSION -> "Markdown Phase (Heavy Sell Off)"
            bullScore <= 45 -> "Distribution (Smart Money Offloading)"
            else -> "Balanced Re-accumulation"
        }

        // 6. Liquidity Sweep Zones (Buy-Side Liquidity & Sell-Side Liquidity)
        val swingHigh = if (candles.isNotEmpty()) {
            max(candles.takeLast(min(30, candles.size)).maxOf { it.high }, priceState.high24h)
        } else {
            priceState.high24h
        }

        val swingLow = if (candles.isNotEmpty()) {
            min(candles.takeLast(min(30, candles.size)).minOf { it.low }, priceState.low24h)
        } else {
            priceState.low24h
        }

        // Upper Pool: BSL (Short Stop Losses & Breakout Buys)
        val upperStart = swingHigh
        val upperEnd = swingHigh + (atr * 0.4)
        val upperDistUsd = max(0.0, upperStart - currentPrice)
        val upperDistPips = upperDistUsd * 10.0 // 1 pip in XAU/USD = $0.10
        val upperRisk = when {
            upperDistUsd <= atr * 0.75 -> SweepRiskLevel.HIGH
            upperDistUsd <= atr * 1.6 -> SweepRiskLevel.MODERATE
            else -> SweepRiskLevel.LOW
        }

        val upperPool = LiquidityPoolZone(
            type = "Buy-Side Liquidity (BSL / Short Squeeze)",
            targetPriceStart = upperStart,
            targetPriceEnd = upperEnd,
            distanceUsd = upperDistUsd,
            distancePips = upperDistPips,
            riskLevel = upperRisk,
            estimatedVolumeLots = (12400.0 + (swingHigh % 100) * 85),
            description = "Kluster Stop-Loss penjual (Shorts) & order Buy Stop institusional."
        )

        // Lower Pool: SSL (Long Stop Losses & Panic Sells)
        val lowerStart = swingLow - (atr * 0.4)
        val lowerEnd = swingLow
        val lowerDistUsd = max(0.0, currentPrice - lowerEnd)
        val lowerDistPips = lowerDistUsd * 10.0
        val lowerRisk = when {
            lowerDistUsd <= atr * 0.75 -> SweepRiskLevel.HIGH
            lowerDistUsd <= atr * 1.6 -> SweepRiskLevel.MODERATE
            else -> SweepRiskLevel.LOW
        }

        val lowerPool = LiquidityPoolZone(
            type = "Sell-Side Liquidity (SSL / Long Washout)",
            targetPriceStart = lowerStart,
            targetPriceEnd = lowerEnd,
            distanceUsd = lowerDistUsd,
            distancePips = lowerDistPips,
            riskLevel = lowerRisk,
            estimatedVolumeLots = (11800.0 + (swingLow % 100) * 78),
            description = "Kluster Stop-Loss pembeli (Longs) & order Sell Stop retail."
        )

        // 7. Liquidity Sweep Warning Banner & Advice
        val isSweepActive = upperRisk == SweepRiskLevel.HIGH || lowerRisk == SweepRiskLevel.HIGH || volatilityState == VolatilityState.EXTREME_SPIKE

        val (headline, advice) = when {
            volatilityState == VolatilityState.EXTREME_SPIKE -> {
                Pair(
                    "⚡ PERINGATAN: Sapuan Likuiditas Ekstrem Sedang Berlangsung!",
                    "Terjadi lonjakan volatilitas cepat! Waspadai eksekusi sumbu panjang (wick hunts) dan slippage tinggi sebelum membuka posisi baru."
                )
            }
            upperRisk == SweepRiskLevel.HIGH -> {
                Pair(
                    "🚨 Waspada Sapuan Atas: Mendekati Pool BSL ($${String.format(Locale.US, "%.2f", upperStart)})",
                    "Harga hanya berjarak ${String.format(Locale.US, "%.1f", upperDistPips)} pips dari Stop Loss kluster penjual. Potensi false breakout ke atas sebelum pullback."
                )
            }
            lowerRisk == SweepRiskLevel.HIGH -> {
                Pair(
                    "🚨 Waspada Sapuan Bawah: Mendekati Pool SSL ($${String.format(Locale.US, "%.2f", lowerEnd)})",
                    "Harga hanya berjarak ${String.format(Locale.US, "%.1f", lowerDistPips)} pips dari Stop Loss kluster pembeli. Waspadai manipulasi wash out sebelum pantulan."
                )
            }
            volatilityState == VolatilityState.COMPRESSION -> {
                Pair(
                    "⏳ Kompresi Volatilitas (Volatility Squeeze)",
                    "Rentang harga menyempit signifikan. Bersiap menghadapi pergerakan breakout tajam menuju pool likuiditas terdekat."
                )
            }
            else -> {
                Pair(
                    "🛡️ Likuiditas Terkendali (Normal Market Flow)",
                    "Pasar bergerak dalam batas volatilitas wajar (ATR $${String.format(Locale.US, "%.2f", atr)}). Level BSL dan SSL tetap berada dalam jarak aman."
                )
            }
        }

        val sweepWarning = LiquiditySweepWarning(
            isSweepActive = isSweepActive,
            volatilityState = volatilityState,
            atr14Value = atr,
            volatilityPercent = (atr / currentPrice) * 100.0,
            upperPool = upperPool,
            lowerPool = lowerPool,
            warningHeadline = headline,
            adviceMessage = advice
        )

        return MarketSentimentData(
            bullishPercentage = bullScore,
            bearishPercentage = bearScore,
            sentimentRating = sentimentRating,
            rsiValue = rsi,
            rsiStatus = rsiStatus,
            orderFlowDeltaLots = orderFlowDelta,
            orderBookBuyerRatio = orderBookRatio,
            institutionalBias = institutionalBias,
            sweepWarning = sweepWarning
        )
    }

    private fun calculateATR(candles: List<CandleData>, period: Int): Double {
        if (candles.size < 2) return 18.50
        val n = min(period, candles.size - 1)
        val recent = candles.takeLast(n + 1)

        var trSum = 0.0
        for (i in 1 until recent.size) {
            val h = recent[i].high
            val l = recent[i].low
            val prevC = recent[i - 1].close
            val tr = max(h - l, max(abs(h - prevC), abs(l - prevC)))
            trSum += tr
        }
        return trSum / n
    }

    private fun calculateRSI(candles: List<CandleData>, period: Int): Double {
        if (candles.size < 2) return 50.0
        val n = min(period, candles.size - 1)
        val recent = candles.takeLast(n + 1)

        var gains = 0.0
        var losses = 0.0
        for (i in 1 until recent.size) {
            val diff = recent[i].close - recent[i - 1].close
            if (diff >= 0) gains += diff else losses += -diff
        }
        val avgGain = gains / n
        val avgLoss = if (losses == 0.0) 0.001 else losses / n
        val rs = avgGain / avgLoss
        return 100.0 - (100.0 / (1.0 + rs))
    }
}
