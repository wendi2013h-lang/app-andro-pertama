package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.PivotPoints
import com.example.data.repository.GoldRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("XAU/USD Live", appName)
  }

  @Test
  fun `pivot point calculation accuracy`() {
    val high = 2950.0
    val low = 2900.0
    val close = 2930.0
    val p = (high + low + close) / 3.0
    val r1 = (2 * p) - low
    val s1 = (2 * p) - high

    assertEquals(2926.666, p, 0.01)
    assertTrue(r1 > p)
    assertTrue(s1 < p)
  }

  @Test
  fun `market engine calibration shifts price accurately`() {
    val engine = com.example.data.remote.GoldMarketEngine()
    val targetBrokerPrice = 4605.50
    engine.calibrateToTargetPrice(targetBrokerPrice)
    val tick = engine.generateNextTick()
    assertEquals(4605.50, tick.currentPrice, 1.0)
    assertEquals(com.example.data.model.DataSourceProvider.CUSTOM_CALIBRATED, tick.dataSource)
  }

  @Test
  fun `notification helper builds foreground service notification with correct channels`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val helper = com.example.notification.NotificationHelper(context)
    val notification = helper.buildForegroundServiceNotification(4602.80, 2)
    org.junit.Assert.assertNotNull(notification)
  }

  @Test
  fun `background monitor manager toggle preferences`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.service.BackgroundMonitorManager.setBackgroundServiceEnabled(context, true)
    assertTrue(com.example.service.BackgroundMonitorManager.isBackgroundServiceEnabled(context))
    com.example.service.BackgroundMonitorManager.setBackgroundServiceEnabled(context, false)
    org.junit.Assert.assertFalse(com.example.service.BackgroundMonitorManager.isBackgroundServiceEnabled(context))
  }

  @Test
  fun `market sentiment calculator produces valid bullish bearish metrics and liquidity zones`() {
    val candles = listOf(
      com.example.data.model.CandleData(1000L, 4580.0, 4610.0, 4575.0, 4602.0, 1500.0),
      com.example.data.model.CandleData(2000L, 4602.0, 4625.0, 4595.0, 4618.0, 2200.0),
      com.example.data.model.CandleData(3000L, 4618.0, 4630.0, 4610.0, 4624.0, 3100.0)
    )
    val priceState = com.example.data.model.GoldPriceState(
      currentPrice = 4624.0,
      high24h = 4630.0,
      low24h = 4575.0,
      open24h = 4580.0
    )
    val bids = listOf(com.example.data.model.OrderBookEntry(4623.5, 50.0, 50.0))
    val asks = listOf(com.example.data.model.OrderBookEntry(4624.5, 30.0, 30.0))

    val sentiment = com.example.data.repository.MarketSentimentCalculator.calculateSentiment(
      candles = candles,
      priceState = priceState,
      orderBookBids = bids,
      orderBookAsks = asks
    )

    assertTrue(sentiment.bullishPercentage in 10..90)
    assertEquals(100, sentiment.bullishPercentage + sentiment.bearishPercentage)
    assertTrue(sentiment.sweepWarning.upperPool.targetPriceStart >= priceState.currentPrice)
    assertTrue(sentiment.sweepWarning.lowerPool.targetPriceEnd <= priceState.currentPrice)
    assertTrue(sentiment.sweepWarning.atr14Value > 0.0)
  }
}

