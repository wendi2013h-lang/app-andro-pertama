package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlertType
import com.example.data.model.LiquidityPoolZone
import com.example.data.model.MarketSentimentData
import com.example.data.model.SweepRiskLevel
import com.example.data.model.VolatilityState
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.SlateBorderDark
import com.example.ui.theme.SlateCardDark
import java.util.Locale

@Composable
fun MarketSentimentCard(
    sentimentData: MarketSentimentData,
    currentPrice: Double,
    onSetSweepAlert: (Double, AlertType) -> Unit,
    modifier: Modifier = Modifier
) {
    var isEducationExpanded by remember { mutableStateOf(false) }

    val warning = sentimentData.sweepWarning
    val isSweepActive = warning.isSweepActive

    // Pulse animation for high risk warnings
    val infiniteTransition = rememberInfiniteTransition(label = "radarPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Animated Bullish bar ratio
    val animatedBullRatio by animateFloatAsState(
        targetValue = sentimentData.bullishPercentage / 100f,
        animationSpec = tween(600),
        label = "bullRatio"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = SlateCardDark),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.2.dp,
            if (isSweepActive) BearishRed.copy(alpha = pulseAlpha) else SlateBorderDark
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("market_sentiment_dashboard_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 1. Header with Radar Icon and Dynamic Sentiment Rating
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSweepActive) BearishRed.copy(alpha = 0.2f)
                                else GoldPrimary.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = null,
                            tint = if (isSweepActive) BearishRed else GoldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Market Sentiment & Liquidity",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Real-time Order Flow & Sweep Radar",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Dynamic Rating Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (sentimentData.bullishPercentage >= 55) BullishGreen.copy(alpha = 0.16f)
                            else if (sentimentData.bullishPercentage <= 45) BearishRed.copy(alpha = 0.16f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(
                        1.dp,
                        if (sentimentData.bullishPercentage >= 55) BullishGreen.copy(alpha = 0.5f)
                        else if (sentimentData.bullishPercentage <= 45) BearishRed.copy(alpha = 0.5f)
                        else SlateBorderDark
                    )
                ) {
                    Text(
                        text = sentimentData.sentimentRating,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (sentimentData.bullishPercentage >= 55) BullishGreen
                                else if (sentimentData.bullishPercentage <= 45) BearishRed
                                else GoldLight,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Bullish vs Bearish Power Meter
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = BullishGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Bulls ${sentimentData.bullishPercentage}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BullishGreen
                        )
                    }

                    Text(
                        text = sentimentData.institutionalBias,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = GoldLight
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${sentimentData.bearishPercentage}% Bears",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BearishRed
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = BearishRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Dual progress visualizer bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(BearishRed.copy(alpha = 0.85f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedBullRatio)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(BullishGreen, BullishGreen.copy(alpha = 0.8f))
                                )
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Technical Indicator Breakdown Matrix
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricMiniCard(
                    title = "RSI (14)",
                    value = String.format(Locale.US, "%.1f", sentimentData.rsiValue),
                    subtitle = sentimentData.rsiStatus,
                    color = if (sentimentData.rsiValue > 55) BullishGreen else if (sentimentData.rsiValue < 45) BearishRed else GoldSecondary,
                    modifier = Modifier.weight(1f)
                )

                MetricMiniCard(
                    title = "Order Flow Delta",
                    value = (if (sentimentData.orderFlowDeltaLots >= 0) "+" else "") +
                            String.format(Locale.US, "%,.0f", sentimentData.orderFlowDeltaLots) + " oz",
                    subtitle = "${sentimentData.orderBookBuyerRatio}% Buyers",
                    color = if (sentimentData.orderFlowDeltaLots >= 0) BullishGreen else BearishRed,
                    modifier = Modifier.weight(1f)
                )

                MetricMiniCard(
                    title = "ATR Volatility",
                    value = "$${String.format(Locale.US, "%.2f", warning.atr14Value)}",
                    subtitle = warning.volatilityState.label,
                    color = when (warning.volatilityState) {
                        VolatilityState.EXTREME_SPIKE -> BearishRed
                        VolatilityState.EXPANSION -> Color(0xFFFF9800)
                        VolatilityState.COMPRESSION -> GoldPrimary
                        VolatilityState.NORMAL -> BullishGreen
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Real-Time Volatility & Liquidity Sweep Warning Banner
            val bannerBackground = when {
                warning.volatilityState == VolatilityState.EXTREME_SPIKE -> BearishRed.copy(alpha = 0.15f)
                isSweepActive -> Color(0xFFFF9800).copy(alpha = 0.15f)
                warning.volatilityState == VolatilityState.COMPRESSION -> GoldPrimary.copy(alpha = 0.12f)
                else -> BullishGreen.copy(alpha = 0.10f)
            }

            val bannerBorderColor = when {
                warning.volatilityState == VolatilityState.EXTREME_SPIKE -> BearishRed.copy(alpha = 0.7f)
                isSweepActive -> Color(0xFFFF9800).copy(alpha = 0.6f)
                warning.volatilityState == VolatilityState.COMPRESSION -> GoldPrimary.copy(alpha = 0.5f)
                else -> BullishGreen.copy(alpha = 0.4f)
            }

            val bannerIconTint = when {
                warning.volatilityState == VolatilityState.EXTREME_SPIKE -> BearishRed
                isSweepActive -> Color(0xFFFF9800)
                warning.volatilityState == VolatilityState.COMPRESSION -> GoldPrimary
                else -> BullishGreen
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = bannerBackground,
                border = BorderStroke(1.dp, bannerBorderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("liquidity_sweep_warning_banner")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (isSweepActive) Icons.Default.WarningAmber else Icons.Default.Shield,
                            contentDescription = null,
                            tint = bannerIconTint,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = warning.warningHeadline,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = warning.adviceMessage,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Liquidity Pools Matrix (Buy-Side vs Sell-Side Liquidity)
            Text(
                text = "Key Liquidity Pools (Smart Money Stop Hunt Zones)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Upper Pool (BSL)
            LiquidityPoolRow(
                pool = warning.upperPool,
                isUpper = true,
                onSetAlert = {
                    onSetSweepAlert(warning.upperPool.targetPriceStart, AlertType.PRICE_ABOVE)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Lower Pool (SSL)
            LiquidityPoolRow(
                pool = warning.lowerPool,
                isUpper = false,
                onSetAlert = {
                    onSetSweepAlert(warning.lowerPool.targetPriceEnd, AlertType.PRICE_BELOW)
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 6. Educational Toggle for Liquidity Concepts
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isEducationExpanded = !isEducationExpanded }
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Apa itu Liquidity Sweep & Stop Hunt?",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GoldLight
                    )
                }
                Icon(
                    imageVector = if (isEducationExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = GoldLight,
                    modifier = Modifier.size(16.dp)
                )
            }

            AnimatedVisibility(
                visible = isEducationExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "💡 Konsep Smart Money (ICT):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Institusi besar dan market maker membutuhkan likuiditas masif untuk mengeksekusi order. Mereka sering mendorong harga melewati swing high (BSL) atau swing low (SSL) untuk menyapu stop loss retail sebelum membalikkan arah harga (Fakeout / Reversal).",
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LiquidityPoolRow(
    pool: LiquidityPoolZone,
    isUpper: Boolean,
    onSetAlert: () -> Unit
) {
    val poolColor = if (isUpper) BearishRed else BullishGreen
    val riskColor = Color(pool.riskLevel.hexColor)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SlateCardDark,
        border = BorderStroke(
            1.dp,
            if (pool.riskLevel == SweepRiskLevel.HIGH) riskColor.copy(alpha = 0.6f) else SlateBorderDark
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(poolColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isUpper) "Upper BSL (Short Squeeze)" else "Lower SSL (Long Washout)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Risk Level Chip
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = riskColor.copy(alpha = 0.15f),
                    border = BorderStroke(0.8.dp, riskColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = pool.riskLevel.label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = riskColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", pool.targetPriceStart)} - $${String.format(Locale.US, "%.2f", pool.targetPriceEnd)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight
                    )
                    Text(
                        text = "${if (isUpper) "+" else "-"}$${String.format(Locale.US, "%.2f", pool.distanceUsd)} (${String.format(Locale.US, "%.1f", pool.distancePips)} pips)",
                        fontSize = 10.sp,
                        color = if (pool.riskLevel == SweepRiskLevel.HIGH) riskColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Set Alert for this liquidity level button
                Surface(
                    onClick = onSetAlert,
                    shape = RoundedCornerShape(8.dp),
                    color = GoldPrimary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f)),
                    modifier = Modifier.testTag(if (isUpper) "set_upper_sweep_alert_btn" else "set_lower_sweep_alert_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddAlert,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+ Alert",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldLight
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricMiniCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, SlateBorderDark, RoundedCornerShape(10.dp))
            .padding(8.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = subtitle,
                fontSize = 9.sp,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
