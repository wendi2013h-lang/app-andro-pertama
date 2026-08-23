package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CandleData
import com.example.data.model.DataSourceProvider
import com.example.data.model.GoldPriceState
import com.example.data.model.MarketSentimentData
import com.example.data.model.OrderBookEntry
import com.example.data.model.TimeFrame
import com.example.data.model.AlertType
import com.example.ui.components.BrokerCalibrationDialog
import com.example.ui.components.InteractiveGoldChart
import com.example.ui.components.LivePriceCard
import com.example.ui.components.MarketSentimentCard
import com.example.ui.components.OrderBookDepth
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
fun LiveChartTab(
    priceState: GoldPriceState,
    candles: List<CandleData>,
    selectedTimeFrame: TimeFrame,
    orderBookBids: List<OrderBookEntry>,
    orderBookAsks: List<OrderBookEntry>,
    sentimentData: MarketSentimentData,
    refreshSpeedMs: Long,
    onTimeFrameSelected: (TimeFrame) -> Unit,
    onQuickAlert: (Double) -> Unit,
    onSetSweepAlert: (Double, AlertType) -> Unit = { _, _ -> },
    onSetRefreshSpeed: (Long) -> Unit,
    onNavigateToAlerts: () -> Unit,
    onCalibratePrice: (Double) -> Unit = {},
    onSelectSource: (DataSourceProvider) -> Unit = {},
    onResetCalibration: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var speedMenuOpen by remember { mutableStateOf(false) }
    var showCalibrationDialog by remember { mutableStateOf(false) }

    if (showCalibrationDialog) {
        BrokerCalibrationDialog(
            currentPrice = priceState.currentPrice,
            activeSource = priceState.dataSource,
            activeOffset = priceState.brokerOffset,
            onCalibratePrice = onCalibratePrice,
            onSelectSource = onSelectSource,
            onResetCalibration = onResetCalibration,
            onDismiss = { showCalibrationDialog = false }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Main Live Price Hero Card with direct Calibration action
        item {
            LivePriceCard(
                priceState = priceState,
                onQuickAlertClick = onNavigateToAlerts,
                onOpenCalibration = { showCalibrationDialog = true }
            )
        }

        // 2. Quick 1-Tap Price Alert Bar, Broker Calibration trigger & Speed control
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AddAlert,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Quick Price Alerts",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Quick Broker Sync / Calibrate Button
                        Surface(
                            onClick = { showCalibrationDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            color = SlateCardDark,
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
                            modifier = Modifier.testTag("open_calibration_quick_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "MT5 Sync",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldLight
                                )
                            }
                        }

                        // Refresh Speed Menu
                        Box {
                            Surface(
                                onClick = { speedMenuOpen = true },
                                shape = RoundedCornerShape(8.dp),
                                color = SlateCardDark,
                                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorderDark),
                                modifier = Modifier.testTag("speed_dropdown_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = GoldSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${refreshSpeedMs / 1000.0}s",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = speedMenuOpen,
                                onDismissRequest = { speedMenuOpen = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("⚡ 1.0s (Ultra Real-Time)") },
                                    onClick = { onSetRefreshSpeed(1000L); speedMenuOpen = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("⏱️ 1.5s (Default Live)") },
                                    onClick = { onSetRefreshSpeed(1500L); speedMenuOpen = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("🔋 3.0s (Battery Saver)") },
                                    onClick = { onSetRefreshSpeed(3000L); speedMenuOpen = false }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Alert Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickAlertButton(label = "+$5", color = BullishGreen, onClick = { onQuickAlert(5.0) })
                    QuickAlertButton(label = "+$10", color = BullishGreen, onClick = { onQuickAlert(10.0) })
                    QuickAlertButton(label = "+$25", color = BullishGreen, onClick = { onQuickAlert(25.0) })
                    QuickAlertButton(label = "-$10", color = BearishRed, onClick = { onQuickAlert(-10.0) })
                    QuickAlertButton(label = "-$25", color = BearishRed, onClick = { onQuickAlert(-25.0) })
                }
            }
        }

        // 3. Interactive Chart
        item {
            InteractiveGoldChart(
                candles = candles,
                currentPrice = priceState.currentPrice,
                selectedTimeFrame = selectedTimeFrame,
                onTimeFrameSelected = onTimeFrameSelected
            )
        }

        // 4. Market Sentiment & Liquidity Sweep Radar Dashboard Component
        item {
            MarketSentimentCard(
                sentimentData = sentimentData,
                currentPrice = priceState.currentPrice,
                onSetSweepAlert = onSetSweepAlert
            )
        }

        // 5. Live Order Book Depth
        item {
            OrderBookDepth(
                bids = orderBookBids,
                asks = orderBookAsks
            )
        }

        // 6. Market Statistics Grid
        item {
            MarketStatsGrid(priceState = priceState)
        }
    }
}


@Composable
private fun QuickAlertButton(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = Modifier.height(30.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun MarketStatsGrid(priceState: GoldPriceState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp)
            .testTag("market_stats_grid")
    ) {
        Text(
            text = "XAU/USD Key Market Statistics",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatBox(
                title = "24h High",
                value = "$${String.format(Locale.US, "%.2f", priceState.high24h)}",
                color = BullishGreen,
                modifier = Modifier.weight(1f)
            )
            StatBox(
                title = "24h Low",
                value = "$${String.format(Locale.US, "%.2f", priceState.low24h)}",
                color = BearishRed,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatBox(
                title = "24h Open",
                value = "$${String.format(Locale.US, "%.2f", priceState.open24h)}",
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            StatBox(
                title = "24h Volatility",
                value = "$${String.format(Locale.US, "%.2f (%.2f%%)", priceState.high24h - priceState.low24h, ((priceState.high24h - priceState.low24h)/priceState.low24h)*100)}",
                color = GoldSecondary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatBox(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SlateCardDark)
            .border(1.dp, SlateBorderDark, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
