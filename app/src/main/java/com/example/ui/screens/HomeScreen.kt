package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CandlestickChart
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PriceAlert
import com.example.ui.components.AlertBanner
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.SlateBorderDark
import com.example.ui.theme.SlateCardDark
import com.example.ui.viewmodel.GoldViewModel

data class NavItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: GoldViewModel,
    modifier: Modifier = Modifier
) {
    val priceState by viewModel.priceState.collectAsStateWithLifecycle()
    val candles by viewModel.candles.collectAsStateWithLifecycle()
    val selectedTimeFrame by viewModel.selectedTimeFrame.collectAsStateWithLifecycle()
    val orderBookBids by viewModel.orderBookBids.collectAsStateWithLifecycle()
    val orderBookAsks by viewModel.orderBookAsks.collectAsStateWithLifecycle()
    val pivotPoints by viewModel.pivotPoints.collectAsStateWithLifecycle()
    val technicalSummary by viewModel.technicalSummary.collectAsStateWithLifecycle()
    val marketSentiment by viewModel.marketSentiment.collectAsStateWithLifecycle()
    val allAlerts by viewModel.allAlerts.collectAsStateWithLifecycle()
    val alertHistory by viewModel.alertHistory.collectAsStateWithLifecycle()
    val inAppAlert by viewModel.inAppAlert.collectAsStateWithLifecycle()
    val refreshSpeedMs by viewModel.refreshSpeedMs.collectAsStateWithLifecycle()
    val isBackgroundServiceRunning by viewModel.isBackgroundServiceRunning.collectAsStateWithLifecycle()
    val isBackgroundServiceEnabled by viewModel.isBackgroundServiceEnabled.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }

    val navItems = listOf(
        NavItem(
            title = "Grafik & Pasar",
            selectedIcon = Icons.Filled.CandlestickChart,
            unselectedIcon = Icons.Outlined.CandlestickChart,
            testTag = "nav_chart"
        ),
        NavItem(
            title = "Peringatan",
            selectedIcon = Icons.Filled.NotificationsActive,
            unselectedIcon = Icons.Outlined.Notifications,
            testTag = "nav_alerts"
        ),
        NavItem(
            title = "Kalkulator",
            selectedIcon = Icons.Filled.Calculate,
            unselectedIcon = Icons.Outlined.Calculate,
            testTag = "nav_calculator"
        )
    )

    val activeAlertCount = allAlerts.count { it.isActive && !it.isTriggered }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "XAU/USD Gold Live",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = GoldLight,
                        letterSpacing = 0.5.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ObsidianDark,
                    titleContentColor = GoldLight
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SlateCardDark,
                contentColor = GoldLight,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar")
            ) {
                navItems.forEachIndexed { index, item ->
                    val isSelected = selectedTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        icon = {
                            if (index == 1 && activeAlertCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = GoldPrimary,
                                            contentColor = Color.Black
                                        ) {
                                            Text(activeAlertCount.toString(), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.title,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        },
                        label = {
                            Text(
                                text = item.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            indicatorColor = GoldPrimary,
                            selectedTextColor = GoldPrimary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag(item.testTag)
                    )
                }
            }
        },
        containerColor = ObsidianDark,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> LiveChartTab(
                    priceState = priceState,
                    candles = candles,
                    selectedTimeFrame = selectedTimeFrame,
                    orderBookBids = orderBookBids,
                    orderBookAsks = orderBookAsks,
                    sentimentData = marketSentiment,
                    refreshSpeedMs = refreshSpeedMs,
                    onTimeFrameSelected = { viewModel.setTimeFrame(it) },
                    onQuickAlert = { delta -> viewModel.addQuickAlert(delta) },
                    onSetSweepAlert = { target, type ->
                        val note = if (type == com.example.data.model.AlertType.PRICE_ABOVE) "BSL Liquidity Sweep Level" else "SSL Liquidity Sweep Level"
                        viewModel.createPriceAlert(target, type, 0.0, note)
                    },
                    onSetRefreshSpeed = { viewModel.setRefreshSpeed(it) },
                    onNavigateToAlerts = { selectedTab = 1 },
                    onCalibratePrice = { targetPrice -> viewModel.calibrateToBrokerPrice(targetPrice) },
                    onSelectSource = { source -> viewModel.setDataSource(source) },
                    onResetCalibration = { viewModel.resetCalibration() }
                )
                1 -> AlertsTab(
                    currentPrice = priceState.currentPrice,
                    alerts = allAlerts,
                    alertHistory = alertHistory,
                    onCreateAlert = { target, type, pct, note ->
                        viewModel.createPriceAlert(target, type, pct, note)
                    },
                    onToggleAlert = { viewModel.toggleAlertActive(it) },
                    onDeleteAlert = { viewModel.deleteAlert(it) },
                    onClearHistory = { viewModel.clearHistory() },
                    onDeleteHistoryItem = { viewModel.deleteHistoryItem(it) },
                    onTriggerTestAlert = { viewModel.triggerTestAlert() },
                    isBackgroundServiceRunning = isBackgroundServiceRunning,
                    isBackgroundServiceEnabled = isBackgroundServiceEnabled,
                    onToggleBackgroundService = { viewModel.toggleBackgroundService(it) }
                )
                2 -> CalculatorTab(
                    priceState = priceState,
                    pivotPoints = pivotPoints,
                    technicalSummary = technicalSummary
                )
            }

            // In-App Popup Alert Banner
            inAppAlert?.let { alertEvent ->
                AlertBanner(
                    title = alertEvent.title,
                    message = alertEvent.message,
                    price = alertEvent.price,
                    onDismiss = { viewModel.dismissInAppAlert() },
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }
}
