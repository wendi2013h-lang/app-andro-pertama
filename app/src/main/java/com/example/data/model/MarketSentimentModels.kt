package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class SweepRiskLevel(val label: String, val hexColor: Long) {
    HIGH("High Risk / Imminent", 0xFFFF4444),
    MODERATE("Moderate Alert", 0xFFFFB300),
    LOW("Normal / Far Range", 0xFF00E676)
}

enum class VolatilityState(val label: String, val description: String) {
    COMPRESSION("Volatility Compression", "Range menyempit, potensi breakout eksplosif segera terjadi"),
    NORMAL("Normal Range Flow", "Volatilitas stabil dalam kisaran harian normal"),
    EXPANSION("Volatility Expansion", "Pergerakan impulsif cepat, potensi slippage & lonjakan spread"),
    EXTREME_SPIKE("Extreme Liquidity Hunt", "Aktivitas pemburuan stop-loss / likuidasi massal terdeteksi!")
}

data class LiquidityPoolZone(
    val type: String, // "Buy-Side Liquidity (BSL)" or "Sell-Side Liquidity (SSL)"
    val targetPriceStart: Double,
    val targetPriceEnd: Double,
    val distanceUsd: Double,
    val distancePips: Double,
    val riskLevel: SweepRiskLevel,
    val estimatedVolumeLots: Double,
    val description: String
)

data class LiquiditySweepWarning(
    val isSweepActive: Boolean,
    val volatilityState: VolatilityState,
    val atr14Value: Double,
    val volatilityPercent: Double,
    val upperPool: LiquidityPoolZone,
    val lowerPool: LiquidityPoolZone,
    val warningHeadline: String,
    val adviceMessage: String
)

data class MarketSentimentData(
    val bullishPercentage: Int = 65,
    val bearishPercentage: Int = 35,
    val sentimentRating: String = "Bullish Bias",
    val rsiValue: Double = 58.5,
    val rsiStatus: String = "Bullish Momentum",
    val orderFlowDeltaLots: Double = 3450.0,
    val orderBookBuyerRatio: Int = 62,
    val institutionalBias: String = "Markup / Accumulation",
    val sweepWarning: LiquiditySweepWarning
)
