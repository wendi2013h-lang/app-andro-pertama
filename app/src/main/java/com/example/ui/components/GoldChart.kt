package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CandleData
import com.example.data.model.ChartType
import com.example.data.model.TimeFrame
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.SlateBorderDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@Composable
fun InteractiveGoldChart(
    candles: List<CandleData>,
    currentPrice: Double,
    selectedTimeFrame: TimeFrame,
    onTimeFrameSelected: (TimeFrame) -> Unit,
    modifier: Modifier = Modifier
) {
    var chartType by remember { mutableStateOf(ChartType.CANDLESTICK) }
    var showEMA by remember { mutableStateOf(true) }
    var selectedCandleIndex by remember { mutableStateOf<Int?>(null) }
    val textMeasurer = rememberTextMeasurer()

    // Pulse animation for live price dot
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseRadius"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp)
            .testTag("interactive_gold_chart")
    ) {
        // Controls Row: Chart Type & Timeframe Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Timeframes
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimeFrame.values().forEach { tf ->
                    val isSelected = tf == selectedTimeFrame
                    Surface(
                        onClick = { onTimeFrameSelected(tf) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) GoldPrimary else Color.Transparent,
                        contentColor = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .height(28.dp)
                            .testTag("tf_button_${tf.label}")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = tf.label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Chart Mode & Indicator Toggles
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // EMA Toggle
                IconButton(
                    onClick = { showEMA = !showEMA },
                    modifier = Modifier.size(32.dp).testTag("toggle_ema_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = "Toggle EMA/SMA",
                        tint = if (showEMA) GoldSecondary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Candlestick vs Line Toggle
                IconButton(
                    onClick = {
                        chartType = if (chartType == ChartType.CANDLESTICK) ChartType.AREA_LINE else ChartType.CANDLESTICK
                    },
                    modifier = Modifier.size(32.dp).testTag("toggle_chart_type_button")
                ) {
                    Icon(
                        imageVector = if (chartType == ChartType.CANDLESTICK) Icons.Default.CandlestickChart else Icons.Default.ShowChart,
                        contentDescription = "Toggle Chart Type",
                        tint = GoldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Active Scrub/Hover Information Bar
        val activeCandle = selectedCandleIndex?.let { idx ->
            if (idx in candles.indices) candles[idx] else null
        } ?: candles.lastOrNull()

        if (activeCandle != null) {
            val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }
            val isBullish = activeCandle.close >= activeCandle.open
            val delta = activeCandle.close - activeCandle.open
            val deltaPercent = if (activeCandle.open > 0) (delta / activeCandle.open) * 100 else 0.0

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = dateFormat.format(Date(activeCandle.timestamp)),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "$${String.format(Locale.US, "%.2f", activeCandle.close)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${if (delta >= 0) "+" else ""}${String.format(Locale.US, "%.2f (%.2f%%)", delta, deltaPercent)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isBullish) BullishGreen else BearishRed
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OhlcItem(label = "O", value = activeCandle.open)
                    OhlcItem(label = "H", value = activeCandle.high)
                    OhlcItem(label = "L", value = activeCandle.low)
                    OhlcItem(label = "C", value = activeCandle.close)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Main Chart Canvas
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            val chartWidth = constraints.maxWidth.toFloat()
            val chartHeight = constraints.maxHeight.toFloat()

            if (candles.isNotEmpty()) {
                val minPrice = candles.minOfOrNull { it.low }?.minus(1.0) ?: (currentPrice - 10.0)
                val maxPrice = candles.maxOfOrNull { it.high }?.plus(1.0) ?: (currentPrice + 10.0)
                val priceRange = max(maxPrice - minPrice, 0.01)

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(candles) {
                            detectTapGestures(
                                onPress = { offset ->
                                    val count = candles.size
                                    if (count > 0) {
                                        val candleWidth = size.width / count
                                        val idx = (offset.x / candleWidth).toInt().coerceIn(0, count - 1)
                                        selectedCandleIndex = idx
                                    }
                                }
                            )
                        }
                        .pointerInput(candles) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val count = candles.size
                                    if (count > 0) {
                                        val candleWidth = size.width / count
                                        val idx = (offset.x / candleWidth).toInt().coerceIn(0, count - 1)
                                        selectedCandleIndex = idx
                                    }
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val count = candles.size
                                    if (count > 0) {
                                        val candleWidth = size.width / count
                                        val idx = (change.position.x / candleWidth).toInt().coerceIn(0, count - 1)
                                        selectedCandleIndex = idx
                                    }
                                },
                                onDragEnd = {
                                    // keep active or reset
                                }
                            )
                        }
                ) {
                    val count = candles.size
                    if (count < 2) return@Canvas

                    val plotHeight = chartHeight * 0.78f
                    val volumeTop = chartHeight * 0.82f
                    val volumeHeight = chartHeight * 0.18f
                    val maxVolume = candles.maxOfOrNull { it.volume } ?: 1.0

                    // 1. Draw Grid Lines and Price Labels
                    val gridSteps = 4
                    val priceStep = priceRange / gridSteps
                    for (i in 0..gridSteps) {
                        val gridPrice = minPrice + (i * priceStep)
                        val y = plotHeight - ((gridPrice - minPrice) / priceRange * plotHeight).toFloat()

                        drawLine(
                            color = SlateBorderDark.copy(alpha = 0.4f),
                            start = Offset(0f, y),
                            end = Offset(chartWidth, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        )

                        val priceLabel = "$${String.format(Locale.US, "%.1f", gridPrice)}"
                        drawText(
                            textMeasurer = textMeasurer,
                            text = priceLabel,
                            topLeft = Offset(chartWidth - 56.dp.toPx(), y - 14.dp.toPx()),
                            style = TextStyle(
                                color = Color.Gray.copy(alpha = 0.6f),
                                fontSize = 9.sp
                            )
                        )
                    }

                    // 2. Draw Candlesticks or Area Line
                    val candleStep = chartWidth / count
                    val candleWidth = max(2f, candleStep * 0.65f)

                    if (chartType == ChartType.CANDLESTICK) {
                        candles.forEachIndexed { i, candle ->
                            val x = (i * candleStep) + (candleStep / 2f)
                            val openY = plotHeight - ((candle.open - minPrice) / priceRange * plotHeight).toFloat()
                            val closeY = plotHeight - ((candle.close - minPrice) / priceRange * plotHeight).toFloat()
                            val highY = plotHeight - ((candle.high - minPrice) / priceRange * plotHeight).toFloat()
                            val lowY = plotHeight - ((candle.low - minPrice) / priceRange * plotHeight).toFloat()

                            val isUp = candle.close >= candle.open
                            val candleColor = if (isUp) BullishGreen else BearishRed

                            // Wick
                            drawLine(
                                color = candleColor,
                                start = Offset(x, highY),
                                end = Offset(x, lowY),
                                strokeWidth = 1.5.dp.toPx()
                            )

                            // Candle Body
                            val topY = min(openY, closeY)
                            val bottomY = max(openY, closeY)
                            val bodyHeight = max(2f, bottomY - topY)

                            drawRect(
                                color = candleColor,
                                topLeft = Offset(x - (candleWidth / 2f), topY),
                                size = Size(candleWidth, bodyHeight)
                            )

                            // Volume Bar below
                            val volH = (candle.volume / maxVolume * volumeHeight).toFloat()
                            drawRect(
                                color = candleColor.copy(alpha = 0.35f),
                                topLeft = Offset(x - (candleWidth / 2f), chartHeight - volH),
                                size = Size(candleWidth, volH)
                            )
                        }
                    } else {
                        // Area & Smooth Curve Line
                        val linePath = Path()
                        val fillPath = Path()

                        candles.forEachIndexed { i, candle ->
                            val x = (i * candleStep) + (candleStep / 2f)
                            val y = plotHeight - ((candle.close - minPrice) / priceRange * plotHeight).toFloat()

                            if (i == 0) {
                                linePath.moveTo(x, y)
                                fillPath.moveTo(x, plotHeight)
                                fillPath.lineTo(x, y)
                            } else {
                                linePath.lineTo(x, y)
                                fillPath.lineTo(x, y)
                            }

                            // Volume Bar below
                            val isUp = candle.close >= candle.open
                            val volColor = if (isUp) BullishGreen else BearishRed
                            val volH = (candle.volume / maxVolume * volumeHeight).toFloat()
                            drawRect(
                                color = volColor.copy(alpha = 0.3f),
                                topLeft = Offset(x - (candleWidth / 2f), chartHeight - volH),
                                size = Size(candleWidth, volH)
                            )
                        }

                        val lastX = ((count - 1) * candleStep) + (candleStep / 2f)
                        fillPath.lineTo(lastX, plotHeight)
                        fillPath.close()

                        // Draw golden glow area
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    GoldPrimary.copy(alpha = 0.45f),
                                    GoldSecondary.copy(alpha = 0.15f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = plotHeight
                            )
                        )

                        // Draw golden stroke line
                        drawPath(
                            path = linePath,
                            color = GoldPrimary,
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // 3. Draw EMA 20 & SMA 50 if toggled
                    if (showEMA && count > 10) {
                        drawEmaCurve(candles, 20, Color(0xFF818CF8), candleStep, minPrice, priceRange, plotHeight)
                        drawEmaCurve(candles, 50, Color(0xFFF97316), candleStep, minPrice, priceRange, plotHeight)
                    }

                    // 4. Draw Current Price Line & Beacon
                    val currentPriceY = plotHeight - ((currentPrice - minPrice) / priceRange * plotHeight).toFloat()
                    drawLine(
                        color = GoldPrimary,
                        start = Offset(0f, currentPriceY),
                        end = Offset(chartWidth, currentPriceY),
                        strokeWidth = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )

                    // Glowing beacon dot on right edge
                    drawCircle(
                        color = GoldPrimary.copy(alpha = 0.35f),
                        radius = pulseRadius.dp.toPx(),
                        center = Offset(chartWidth - 12.dp.toPx(), currentPriceY)
                    )
                    drawCircle(
                        color = GoldPrimary,
                        radius = 4.dp.toPx(),
                        center = Offset(chartWidth - 12.dp.toPx(), currentPriceY)
                    )

                    // 5. Crosshair when touching
                    selectedCandleIndex?.let { idx ->
                        if (idx in candles.indices) {
                            val scrubX = (idx * candleStep) + (candleStep / 2f)
                            val candle = candles[idx]
                            val scrubY = plotHeight - ((candle.close - minPrice) / priceRange * plotHeight).toFloat()

                            // Vertical crosshair line
                            drawLine(
                                color = Color.White.copy(alpha = 0.8f),
                                start = Offset(scrubX, 0f),
                                end = Offset(scrubX, chartHeight),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                            )

                            // Crosshair intersection circle
                            drawCircle(
                                color = Color.White,
                                radius = 5.dp.toPx(),
                                center = Offset(scrubX, scrubY)
                            )
                        }
                    }
                }
            }
        }

        // EMA Legend
        if (showEMA) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF818CF8)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("EMA 20", fontSize = 10.sp, color = Color(0xFF818CF8))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFF97316)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SMA 50", fontSize = 10.sp, color = Color(0xFFF97316))
                }
            }
        }
    }
}

private fun DrawScope.drawEmaCurve(
    candles: List<CandleData>,
    period: Int,
    color: Color,
    candleStep: Float,
    minPrice: Double,
    priceRange: Double,
    plotHeight: Float
) {
    if (candles.size < 3) return
    val k = 2.0 / (period + 1)
    var ema = candles.first().close
    val path = Path()

    candles.forEachIndexed { i, candle ->
        ema = (candle.close * k) + (ema * (1 - k))
        val x = (i * candleStep) + (candleStep / 2f)
        val y = plotHeight - ((ema - minPrice) / priceRange * plotHeight).toFloat()

        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
    )
}

@Composable
private fun OhlcItem(label: String, value: Double) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label:",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = String.format(Locale.US, "%.1f", value),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}
