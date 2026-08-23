package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GoldPriceState
import com.example.data.model.PriceTrend
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BearishRedBg
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.BullishGreenBg
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.SlateBorderDark
import com.example.ui.theme.SlateCardDark
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

@Composable
fun LivePriceCard(
    priceState: GoldPriceState,
    onQuickAlertClick: () -> Unit,
    onOpenCalibration: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isPositive = priceState.change24h >= 0

    // Flash background color briefly on each tick change
    var flashColor by remember { mutableStateOf(Color.Transparent) }
    LaunchedEffect(priceState.currentPrice) {
        flashColor = when (priceState.priceTrend) {
            PriceTrend.UP -> BullishGreen.copy(alpha = 0.22f)
            PriceTrend.DOWN -> BearishRed.copy(alpha = 0.22f)
            PriceTrend.NEUTRAL -> Color.Transparent
        }
        delay(400)
        flashColor = Color.Transparent
    }

    val animatedFlash by animateColorAsState(
        targetValue = flashColor,
        animationSpec = tween(durationMillis = 350),
        label = "flashAnim"
    )

    // Pulse animation for live beacon
    val infiniteTransition = rememberInfiniteTransition(label = "beacon")
    val beaconAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beaconAlpha"
    )

    // Calculate gram price in IDR (1 Troy Ounce = 31.1034768 grams)
    val pricePerGramUsd = priceState.currentPrice / 31.1034768
    val pricePerGramIdr = pricePerGramUsd * priceState.usdIdrRate
    val idrFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SlateCardDark,
                        ObsidianDark
                    )
                )
            )
            .border(1.dp, SlateBorderDark, RoundedCornerShape(24.dp))
            .background(animatedFlash)
            .padding(18.dp)
            .testTag("live_price_card")
    ) {
        // Top Header: Symbol, Broker / Source Pill, Live Status Chip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Gold Icon Badge
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(GoldPrimary, GoldSecondary)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Au",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "XAU / USD",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Spot Gold • 1 Troy Oz",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // Right side: Source Badge & Live Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Source / Broker Benchmark Selector Pill
                Surface(
                    onClick = onOpenCalibration,
                    shape = RoundedCornerShape(20.dp),
                    color = SlateCardDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f)),
                    modifier = Modifier.testTag("source_calibration_badge_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Kalibrasi",
                            tint = GoldPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = priceState.dataSource.badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldLight
                        )
                    }
                }

                // Live Pill Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = BullishGreen.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BullishGreen.copy(alpha = 0.5f)),
                    modifier = Modifier.testTag("live_status_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(BullishGreen.copy(alpha = beaconAlpha))
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "LIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BullishGreen
                        )
                    }
                }
            }
        }

        // Active Offset Banner (if calibrated)
        if (abs(priceState.brokerOffset) > 0.001) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                onClick = onOpenCalibration,
                shape = RoundedCornerShape(8.dp),
                color = GoldPrimary.copy(alpha = 0.10f),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Tersinkronisasi MT5: Offset ${if (priceState.brokerOffset >= 0) "+" else ""}${String.format(Locale.US, "%.2f", priceState.brokerOffset)}$",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GoldLight
                        )
                    }
                    Text(
                        text = "Ubah",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Large Price Display with 24h Change Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "$${String.format(Locale.US, "%,.2f", priceState.currentPrice)}",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    color = GoldLight,
                    letterSpacing = (-0.5).sp,
                    modifier = Modifier.testTag("live_gold_price_text")
                )
                // IDR Estimation
                Text(
                    text = "≈ ${idrFormatter.format(pricePerGramIdr)} / gram",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = GoldSecondary
                )
            }

            // 24h Change Pill
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isPositive) BullishGreenBg else BearishRedBg,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isPositive) BullishGreen.copy(alpha = 0.4f) else BearishRed.copy(alpha = 0.4f)
                ),
                modifier = Modifier.testTag("change_24h_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = if (isPositive) BullishGreen else BearishRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${if (isPositive) "+" else ""}${String.format(Locale.US, "%.2f", priceState.change24h)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPositive) BullishGreen else BearishRed
                        )
                        Text(
                            text = "${if (isPositive) "+" else ""}${String.format(Locale.US, "%.2f%%", priceState.changePercent24h)}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isPositive) BullishGreen else BearishRed
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Bid / Ask / Spread Stats Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("BID", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = String.format(Locale.US, "%.2f", priceState.bid),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = BearishRed
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "Spread: $${String.format(Locale.US, "%.2f", priceState.spread)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = GoldSecondary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("ASK", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = String.format(Locale.US, "%.2f", priceState.ask),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = BullishGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 24H Range Progress Bar
        val range = maxOf(priceState.high24h - priceState.low24h, 0.01)
        val progress = ((priceState.currentPrice - priceState.low24h) / range).toFloat().coerceIn(0f, 1f)

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "24h Low: $${String.format(Locale.US, "%.2f", priceState.low24h)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "24h High: $${String.format(Locale.US, "%.2f", priceState.high24h)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(SlateBorderDark)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progress)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(BullishGreen, GoldPrimary, BearishRed)
                            )
                        )
                )
            }
        }
    }
}

