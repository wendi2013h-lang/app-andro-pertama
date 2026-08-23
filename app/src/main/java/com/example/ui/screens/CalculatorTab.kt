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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GoldPriceState
import com.example.data.model.PivotPoints
import com.example.data.model.TechnicalSummary
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
import java.text.NumberFormat
import java.util.Locale

enum class GoldUnit(val label: String, val gramsPerUnit: Double) {
    GRAM("Gram (g)", 1.0),
    TROY_OUNCE("Troy Ounce (oz t)", 31.1034768),
    KILOGRAM("Kilogram (kg)", 1000.0),
    TAEL("Tael (Hong Kong)", 37.429),
    TOLA("Tola (India/South Asia)", 11.6638),
    DINAR("Dinar (4.25g 22K)", 4.25)
}

enum class GoldPurity(val label: String, val karat: String, val purityFactor: Double) {
    K24("24K (99.99% Fine Gold)", "24K", 0.9999),
    K22("22K (91.6% Fine Gold)", "22K", 0.916),
    K18("18K (75.0% Fine Gold)", "18K", 0.750),
    K14("14K (58.5% Fine Gold)", "14K", 0.585)
}

enum class CurrencyTarget(val code: String, val symbol: String, val rateFromUsd: Double) {
    IDR("IDR", "Rp", 16250.0),
    USD("USD", "$", 1.0),
    EUR("EUR", "€", 0.92),
    SGD("SGD", "S$", 1.35),
    MYR("MYR", "RM", 4.42)
}

@Composable
fun CalculatorTab(
    priceState: GoldPriceState,
    pivotPoints: PivotPoints,
    technicalSummary: TechnicalSummary,
    modifier: Modifier = Modifier
) {
    var amountInput by remember { mutableStateOf("1.0") }
    var selectedUnit by remember { mutableStateOf(GoldUnit.GRAM) }
    var selectedPurity by remember { mutableStateOf(GoldPurity.K24) }
    var selectedCurrency by remember { mutableStateOf(CurrencyTarget.IDR) }

    val amountNumber = amountInput.toDoubleOrNull() ?: 0.0
    val totalGrams = amountNumber * selectedUnit.gramsPerUnit
    val fineGrams = totalGrams * selectedPurity.purityFactor

    // 1 Troy oz = 31.1034768g
    val pricePerGramUsd = priceState.currentPrice / 31.1034768
    val totalValueUsd = fineGrams * pricePerGramUsd
    val totalValueSelectedCurrency = totalValueUsd * selectedCurrency.rateFromUsd

    val currencyFormatter = remember(selectedCurrency) {
        when (selectedCurrency) {
            CurrencyTarget.IDR -> NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }
            CurrencyTarget.USD -> NumberFormat.getCurrencyInstance(Locale.US)
            CurrencyTarget.EUR -> NumberFormat.getCurrencyInstance(Locale.GERMANY)
            else -> NumberFormat.getCurrencyInstance(Locale.US)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Interactive Gold Valuation Calculator Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(SlateCardDark, ObsidianDark)
                        )
                    )
                    .border(1.dp, SlateBorderDark, RoundedCornerShape(20.dp))
                    .padding(18.dp)
                    .testTag("gold_calculator_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(GoldPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Kalkulator Nilai Emas",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Hitung taksiran harga berdasarkan berat & kadar kemurnian",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Input Weight & Unit selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text("Jumlah / Berat") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("calc_amount_input")
                    )

                    UnitDropdownSelector(
                        selectedUnit = selectedUnit,
                        onUnitSelected = { selectedUnit = it }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Purity Selector
                Text("Kadar Kemurnian:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GoldPurity.values().forEach { purity ->
                        val isSelected = purity == selectedPurity
                        Surface(
                            onClick = { selectedPurity = purity },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f).height(32.dp).testTag("purity_${purity.karat}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = purity.karat,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Currency Selector
                Text("Mata Uang Konversi:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CurrencyTarget.values().forEach { curr ->
                        val isSelected = curr == selectedCurrency
                        Surface(
                            onClick = { selectedCurrency = curr },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) GoldSecondary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f).height(30.dp).testTag("curr_${curr.code}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = curr.code,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Calculated Valuation Result Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF2E2204), Color(0xFF1E1705))
                            )
                        )
                        .border(1.dp, GoldPrimary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = "Estimasi Nilai Total Pasar",
                            fontSize = 11.sp,
                            color = GoldLight.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currencyFormatter.format(totalValueSelectedCurrency),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = GoldPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Berat Murni: ${String.format(Locale.US, "%.3f", fineGrams)} g",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "≈ $${String.format(Locale.US, "%.2f", totalValueUsd)} USD",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GoldSecondary
                            )
                        }
                    }
                }
            }
        }

        // 2. Technical Analysis & Sentiment Meter
        item {
            TechnicalSentimentCard(summary = technicalSummary)
        }

        // 3. Key Pivot Points Analysis (Floor Pivots R3, R2, R1, P, S1, S2, S3)
        item {
            PivotPointsCard(pivots = pivotPoints, currentPrice = priceState.currentPrice)
        }
    }
}

@Composable
private fun UnitDropdownSelector(
    selectedUnit: GoldUnit,
    onUnitSelected: (GoldUnit) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorderDark),
            modifier = Modifier.height(56.dp)
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = selectedUnit.label.split(" ").first(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldLight
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            GoldUnit.values().forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unit.label) },
                    onClick = {
                        onUnitSelected(unit)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun TechnicalSentimentCard(summary: TechnicalSummary) {
    val isBullish = summary.sentimentScore >= 50

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
            .testTag("technical_sentiment_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.QueryStats,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ringkasan Indikator Teknikal",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isBullish) BullishGreenBg else BearishRedBg,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isBullish) BullishGreen.copy(alpha = 0.5f) else BearishRed.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = summary.overallSentiment,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isBullish) BullishGreen else BearishRed,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Sentiment Progress Gauge
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Bearish (0)", fontSize = 10.sp, color = BearishRed)
                Text("Sentiment Score: ${summary.sentimentScore}/100", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldLight)
                Text("Bullish (100)", fontSize = 10.sp, color = BullishGreen)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(SlateBorderDark)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = (summary.sentimentScore / 100f).coerceIn(0.05f, 1f))
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(BearishRed, GoldPrimary, BullishGreen)
                            )
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Indicator Signals Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SignalItem(label = "RSI (14)", value = String.format(Locale.US, "%.1f", summary.rsi14), note = summary.rsiSignal, modifier = Modifier.weight(1f))
            SignalItem(label = "EMA (20)", value = "Trend", note = summary.ema20Status, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun SignalItem(label: String, value: String, note: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SlateCardDark)
            .border(1.dp, SlateBorderDark, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldLight)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(note, fontSize = 10.sp, color = GoldSecondary)
        }
    }
}

@Composable
private fun PivotPointsCard(pivots: PivotPoints, currentPrice: Double) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
            .testTag("pivot_points_card")
    ) {
        Text(
            text = "Pivot Points Harian (Floor S/R)",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PivotRow(label = "R3 (Extreme Resistance)", value = pivots.r3, color = BearishRed, currentPrice = currentPrice)
            PivotRow(label = "R2 (Major Resistance)", value = pivots.r2, color = BearishRed.copy(alpha = 0.8f), currentPrice = currentPrice)
            PivotRow(label = "R1 (Resistance 1)", value = pivots.r1, color = BearishRed.copy(alpha = 0.6f), currentPrice = currentPrice)
            PivotRow(label = "P (Central Pivot Point)", value = pivots.pivot, color = GoldPrimary, currentPrice = currentPrice, isMain = true)
            PivotRow(label = "S1 (Support 1)", value = pivots.s1, color = BullishGreen.copy(alpha = 0.6f), currentPrice = currentPrice)
            PivotRow(label = "S2 (Major Support)", value = pivots.s2, color = BullishGreen.copy(alpha = 0.8f), currentPrice = currentPrice)
            PivotRow(label = "S3 (Extreme Support)", value = pivots.s3, color = BullishGreen, currentPrice = currentPrice)
        }
    }
}

@Composable
private fun PivotRow(
    label: String,
    value: Double,
    color: Color,
    currentPrice: Double,
    isMain: Boolean = false
) {
    val isNear = kotlin.math.abs(value - currentPrice) < 3.0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (isNear) color.copy(alpha = 0.15f) else Color.Transparent)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = if (isMain) 12.sp else 11.sp,
                fontWeight = if (isMain) FontWeight.Bold else FontWeight.Normal,
                color = if (isMain) GoldLight else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "$${String.format(Locale.US, "%.2f", value)}",
            fontSize = if (isMain) 13.sp else 11.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
