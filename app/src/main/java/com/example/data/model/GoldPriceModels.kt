package com.example.data.model

data class CandleData(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)

enum class TimeFrame(val label: String, val minutes: Int) {
    M1("1M", 1),
    M5("5M", 5),
    M15("15M", 15),
    H1("1H", 60),
    H4("4H", 240),
    D1("1D", 1440),
    W1("1W", 10080)
}

enum class ChartType {
    CANDLESTICK,
    AREA_LINE
}

enum class PriceTrend {
    UP,
    DOWN,
    NEUTRAL
}

enum class DataSourceProvider(val title: String, val badge: String, val description: String) {
    TRADINGVIEW_OANDA("TradingView (OANDA)", "TradingView", "Spot Gold XAU/USD global standard benchmark"),
    METATRADER_EXNESS("MetaTrader 5 (Exness)", "MT5 Exness", "Low-spread Raw CFD feed"),
    METATRADER_ICMARKETS("MetaTrader 5 (IC Markets)", "MT5 ICM", "ECN Raw Spread interbank pricing"),
    YAHOO_GOLD_FUTURES("COMEX Gold Futures (GC=F)", "COMEX GC", "US Gold Futures benchmark"),
    BINANCE_PAXG("Binance PAXG (Physical Gold)", "PAXG/USDT", "1:1 Allocated London Good Delivery gold token"),
    CUSTOM_CALIBRATED("MT5 Custom Calibrated", "MT5 Custom", "Disinkronkan manual dengan chart broker Anda")
}

data class OrderBookEntry(
    val price: Double,
    val amount: Double,
    val total: Double
)

data class GoldPriceState(
    val currentPrice: Double = 2934.50,
    val bid: Double = 2934.20,
    val ask: Double = 2934.80,
    val spread: Double = 0.60,
    val change24h: Double = 18.30,
    val changePercent24h: Double = 0.63,
    val high24h: Double = 2948.70,
    val low24h: Double = 2912.10,
    val open24h: Double = 2916.20,
    val previousClose: Double = 2916.20,
    val lastUpdated: Long = System.currentTimeMillis(),
    val priceTrend: PriceTrend = PriceTrend.UP,
    val lastTickDelta: Double = 0.0,
    val usdIdrRate: Double = 16250.0,
    val usdEurRate: Double = 0.92,
    val isLiveConnected: Boolean = true,
    val refreshIntervalMs: Long = 1500L,
    val dataSource: DataSourceProvider = DataSourceProvider.TRADINGVIEW_OANDA,
    val brokerOffset: Double = 0.0
)

data class PivotPoints(
    val pivot: Double,
    val r1: Double,
    val r2: Double,
    val r3: Double,
    val s1: Double,
    val s2: Double,
    val s3: Double
)

data class TechnicalSummary(
    val rsi14: Double = 58.4,
    val rsiSignal: String = "Neutral / Buy",
    val macdStatus: String = "Bullish Crossover",
    val ema20Status: String = "Above EMA 20 (Bullish)",
    val overallSentiment: String = "Strong Buy",
    val sentimentScore: Int = 78 // 0 - 100
)

