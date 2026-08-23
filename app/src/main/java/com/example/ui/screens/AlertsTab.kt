package com.example.ui.screens

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.NotificationsPaused
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlertHistory
import com.example.data.model.AlertType
import com.example.data.model.PriceAlert
import com.example.notification.NotificationHelper
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BearishRedBg
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.BullishGreenBg
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.SlateBorderDark
import com.example.ui.theme.SlateCardDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsTab(
    currentPrice: Double,
    alerts: List<PriceAlert>,
    alertHistory: List<AlertHistory>,
    onCreateAlert: (Double, AlertType, Double, String) -> Unit,
    onToggleAlert: (PriceAlert) -> Unit,
    onDeleteAlert: (PriceAlert) -> Unit,
    onClearHistory: () -> Unit,
    onDeleteHistoryItem: (Long) -> Unit,
    onTriggerTestAlert: () -> Unit,
    isBackgroundServiceRunning: Boolean = true,
    isBackgroundServiceEnabled: Boolean = true,
    onToggleBackgroundService: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val notificationHelper = remember { NotificationHelper(context) }
    var hasPermission by remember { mutableStateOf(notificationHelper.hasNotificationPermission()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            onToggleBackgroundService(true)
        }
    }

    var selectedSubTab by remember { mutableIntStateOf(0) } // 0: Active Alerts, 1: History
    var showCreateDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Notification Permission Prompt (if missing on Android 13+)
        if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3B2807)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("permission_card")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsPaused,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Aktifkan Notifikasi Instan",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldLight
                            )
                            Text(
                                text = "Izinkan aplikasi mengirim peringatan saat harga XAU/USD mencapai target Anda.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Izinkan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 1. Background Price Monitoring Service Card (24/7 Monitor)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCardDark),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isBackgroundServiceRunning) BullishGreen.copy(alpha = 0.5f) else SlateBorderDark
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("background_service_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isBackgroundServiceRunning) BullishGreen.copy(alpha = 0.2f)
                                        else GoldPrimary.copy(alpha = 0.15f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isBackgroundServiceRunning) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                    contentDescription = null,
                                    tint = if (isBackgroundServiceRunning) BullishGreen else GoldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Background Monitor (24/7)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isBackgroundServiceRunning) "Layanan latar belakang aktif" else "Layanan dijeda / mati",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isBackgroundServiceEnabled,
                            onCheckedChange = { checked ->
                                if (checked && !hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onToggleBackgroundService(checked)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = BullishGreen,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.testTag("background_service_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Status details pill row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isBackgroundServiceRunning) BullishGreen else BearishRed)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBackgroundServiceRunning) "Memantau Harga Real-Time" else "Tidak Berjalan di Latar Belakang",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isBackgroundServiceRunning) BullishGreen else BearishRed
                            )
                        }

                        val activePendingCount = alerts.count { it.isActive && !it.isTriggered }
                        Text(
                            text = "$activePendingCount target siap dipicu",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = GoldLight
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Aplikasi akan secara otomatis memicu suara & getar notifikasi saat harga XAU/USD menembus target, bahkan jika layar terkunci atau aplikasi ditutup.",
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Action Buttons Row: Create Alert + Test Notification
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag("create_alert_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = ObsidianDark
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Buat Notifikasi Baru", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = onTriggerTestAlert,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("test_alert_btn"),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldSecondary)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = GoldSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tes Alert", color = GoldSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Sub Tabs: Active Alerts vs History
        item {
            TabRow(
                selectedTabIndex = selectedSubTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = GoldPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .height(42.dp)
            ) {
                Tab(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    text = {
                        Text(
                            text = "Target Aktif (${alerts.count { it.isActive && !it.isTriggered }})",
                            fontSize = 12.sp,
                            fontWeight = if (selectedSubTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    text = {
                        Text(
                            text = "Riwayat Notifikasi (${alertHistory.size})",
                            fontSize = 12.sp,
                            fontWeight = if (selectedSubTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        // Tab 0: Alert Targets List
        if (selectedSubTab == 0) {
            if (alerts.isEmpty()) {
                item {
                    EmptyAlertsState(onCreateClick = { showCreateDialog = true })
                }
            } else {
                items(alerts, key = { it.id }) { alert ->
                    AlertItemCard(
                        alert = alert,
                        currentPrice = currentPrice,
                        onToggle = { onToggleAlert(alert) },
                        onDelete = { onDeleteAlert(alert) }
                    )
                }
            }
        } else {
            // Tab 1: Alert History Log
            if (alertHistory.isEmpty()) {
                item {
                    EmptyHistoryState()
                }
            } else {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onClearHistory,
                            modifier = Modifier.testTag("clear_history_btn")
                        ) {
                            Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, tint = BearishRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hapus Semua Riwayat", color = BearishRed, fontSize = 12.sp)
                        }
                    }
                }

                items(alertHistory, key = { it.id }) { history ->
                    AlertHistoryCard(
                        history = history,
                        onDelete = { onDeleteHistoryItem(history.id) }
                    )
                }
            }
        }
    }

    // Create Price Alert Dialog
    if (showCreateDialog) {
        CreateAlertDialog(
            currentPrice = currentPrice,
            onDismiss = { showCreateDialog = false },
            onConfirm = { target, type, pct, note ->
                onCreateAlert(target, type, pct, note)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun AlertItemCard(
    alert: PriceAlert,
    currentPrice: Double,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val isPriceAbove = alert.alertType == AlertType.PRICE_ABOVE
    val diff = alert.targetPrice - currentPrice
    val diffPercent = if (currentPrice > 0) (diff / currentPrice) * 100 else 0.0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SlateCardDark)
            .border(
                1.dp,
                if (alert.isTriggered) SlateBorderDark else if (alert.isActive) GoldPrimary.copy(alpha = 0.5f) else SlateBorderDark,
                RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
            .testTag("alert_card_${alert.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Direction Icon
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (alert.isTriggered) SlateBorderDark
                            else if (isPriceAbove) BullishGreenBg else BearishRedBg
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (alert.isTriggered) Icons.Default.CheckCircle
                        else if (isPriceAbove) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = if (alert.isTriggered) MaterialTheme.colorScheme.onSurfaceVariant
                        else if (isPriceAbove) BullishGreen else BearishRed,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isPriceAbove) "Naik Menembus" else "Turun Di Bawah",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (alert.isTriggered) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TERPICU",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldPrimary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(GoldDark.copy(alpha = 0.4f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Text(
                        text = "$${String.format(Locale.US, "%,.2f", alert.targetPrice)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (alert.isTriggered) MaterialTheme.colorScheme.onSurfaceVariant else GoldLight
                    )

                    // Distance to Target
                    if (!alert.isTriggered) {
                        val distanceText = if (diff >= 0) "+$${String.format(Locale.US, "%.2f", diff)} (+${String.format(Locale.US, "%.2f%%", diffPercent)})"
                        else "-$${String.format(Locale.US, "%.2f", abs(diff))} (${String.format(Locale.US, "%.2f%%", diffPercent)})"

                        Text(
                            text = "Jarak: $distanceText dari $${String.format(Locale.US, "%.2f", currentPrice)}",
                            fontSize = 11.sp,
                            color = if (diff >= 0) BullishGreen else BearishRed
                        )
                    }

                    if (alert.note.isNotEmpty()) {
                        Text(
                            text = "Catatan: ${alert.note}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = alert.isActive && !alert.isTriggered,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ObsidianDark,
                        checkedTrackColor = GoldPrimary
                    ),
                    modifier = Modifier.testTag("switch_alert_${alert.id}")
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp).testTag("delete_alert_${alert.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertHistoryCard(
    history: AlertHistory,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SlateCardDark)
            .border(1.dp, SlateBorderDark, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(GoldPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = history.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight
                    )
                    Text(
                        text = history.message,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = dateFormat.format(Date(history.timestamp)),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Hapus",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun CreateAlertDialog(
    currentPrice: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double, AlertType, Double, String) -> Unit
) {
    var targetPriceText by remember { mutableStateOf(String.format(Locale.US, "%.2f", currentPrice + 10.0)) }
    var selectedType by remember { mutableStateOf(AlertType.PRICE_ABOVE) }
    var noteText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Buat Notifikasi Harga XAU/USD", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Harga Terkini: $${String.format(Locale.US, "%.2f", currentPrice)} / oz",
                    fontSize = 13.sp,
                    color = GoldSecondary,
                    fontWeight = FontWeight.SemiBold
                )

                // Condition Type Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == AlertType.PRICE_ABOVE,
                        onClick = {
                            selectedType = AlertType.PRICE_ABOVE
                            if (targetPriceText.toDoubleOrNull() ?: 0.0 <= currentPrice) {
                                targetPriceText = String.format(Locale.US, "%.2f", currentPrice + 10.0)
                            }
                        },
                        label = { Text("📈 Naik Di Atas", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f).testTag("chip_alert_above")
                    )

                    FilterChip(
                        selected = selectedType == AlertType.PRICE_BELOW,
                        onClick = {
                            selectedType = AlertType.PRICE_BELOW
                            if (targetPriceText.toDoubleOrNull() ?: 0.0 >= currentPrice) {
                                targetPriceText = String.format(Locale.US, "%.2f", currentPrice - 10.0)
                            }
                        },
                        label = { Text("📉 Turun Di Bawah", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f).testTag("chip_alert_below")
                    )
                }

                // Price Input
                OutlinedTextField(
                    value = targetPriceText,
                    onValueChange = {
                        targetPriceText = it
                        errorMessage = null
                    },
                    label = { Text("Target Harga (USD / Troy Oz)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = errorMessage != null,
                    modifier = Modifier.fillMaxWidth().testTag("target_price_input")
                )

                if (errorMessage != null) {
                    Text(errorMessage!!, color = BearishRed, fontSize = 11.sp)
                }

                // Quick Delta Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val base = currentPrice
                    val deltas = if (selectedType == AlertType.PRICE_ABOVE) listOf(5.0, 10.0, 20.0, 50.0) else listOf(-5.0, -10.0, -20.0, -50.0)
                    deltas.forEach { d ->
                        Surface(
                            onClick = {
                                targetPriceText = String.format(Locale.US, "%.2f", base + d)
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.height(26.dp)
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
                                Text("${if (d > 0) "+" else ""}$${d.toInt()}", fontSize = 10.sp, color = GoldLight)
                            }
                        }
                    }
                }

                // Note Input
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Catatan / Label (Opsional)") },
                    placeholder = { Text("misal: Take Profit, Resistance, Buy Dip") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("alert_note_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = targetPriceText.toDoubleOrNull()
                    if (target == null || target <= 0) {
                        errorMessage = "Masukkan angka harga yang valid"
                        return@Button
                    }
                    onConfirm(target, selectedType, 0.0, noteText)
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                modifier = Modifier.testTag("confirm_create_alert_btn")
            ) {
                Text("Simpan Alert", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
private fun EmptyAlertsState(onCreateClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(GoldPrimary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsOff,
                contentDescription = null,
                tint = GoldPrimary,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Belum Ada Target Notifikasi",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Pasang target harga untuk menerima notifikasi instan saat harga emas naik atau turun menembus level penting.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onCreateClick,
            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("+ Buat Target Pertama", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun EmptyHistoryState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(SlateBorderDark.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Belum Ada Riwayat Peringatan",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Semua notifikasi lonjakan atau target harga yang terpicu akan tersimpan otomatis di sini.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}
