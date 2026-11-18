package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.PaymentMethod
import com.example.data.model.PosTransaction
import com.example.data.model.TransactionStatus
import com.example.data.model.UiState
import com.example.ui.theme.MonoPriceStyle
import com.example.util.formatRupiah
import com.example.util.formatTimestamp
import com.example.util.isToday

private enum class HistoryFilter(val label: String) {
    ALL("Semua"),
    TODAY("Hari Ini"),
    COMPLETED("Selesai"),
    VOID("Dibatalkan"),
}

@Composable
fun HistoryScreen(
    transactionsState: UiState<List<PosTransaction>>,
    onSelectTransaction: (PosTransaction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var filter by remember { mutableStateOf(HistoryFilter.ALL) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.history_title),
                style = MaterialTheme.typography.headlineSmall,
            )
        }

        when (transactionsState) {
            is UiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is UiState.Error -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = transactionsState.message,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            is UiState.Success -> {
                val allTransactions = transactionsState.data
                val todayCompleted = remember(allTransactions) {
                    allTransactions.filter {
                        isToday(it.createdAt) && TransactionStatus.fromCode(it.status) == TransactionStatus.COMPLETED
                    }
                }
                val todayRevenue = remember(todayCompleted) {
                    todayCompleted.sumOf { it.totalAmount }
                }
                val avgOrder = remember(todayCompleted, todayRevenue) {
                    if (todayCompleted.isEmpty()) 0L else todayRevenue / todayCompleted.size
                }
                val cashToday = remember(todayCompleted) {
                    todayCompleted.filter { it.paymentMethod == PaymentMethod.CASH.code }.sumOf { it.totalAmount }
                }
                val nonCashToday = remember(todayRevenue, cashToday) {
                    (todayRevenue - cashToday).coerceAtLeast(0L)
                }

                // Today's Omzet Hero Gradient Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary,
                                )
                            )
                        )
                        .padding(18.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(R.string.history_today_revenue),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                        Text(
                            text = formatRupiah(todayRevenue),
                            style = MonoPriceStyle.copy(
                                fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = Color.White,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            MiniHeroMetric(
                                label = stringResource(R.string.history_today_tx),
                                value = "${todayCompleted.size}x",
                                modifier = Modifier.weight(1f),
                            )
                            MiniHeroMetric(
                                label = stringResource(R.string.history_avg_order),
                                value = formatRupiah(avgOrder),
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            MiniHeroMetric(
                                label = "Tunai",
                                value = formatRupiah(cashToday),
                                modifier = Modifier.weight(1f),
                            )
                            MiniHeroMetric(
                                label = "QRIS / Transfer",
                                value = formatRupiah(nonCashToday),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(HistoryFilter.entries) { item ->
                        FilterChip(
                            selected = filter == item,
                            onClick = { filter = item },
                            label = { Text(item.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val filtered = remember(allTransactions, filter) {
                    when (filter) {
                        HistoryFilter.ALL -> allTransactions
                        HistoryFilter.TODAY -> allTransactions.filter { isToday(it.createdAt) }
                        HistoryFilter.COMPLETED -> allTransactions.filter {
                            TransactionStatus.fromCode(it.status) == TransactionStatus.COMPLETED
                        }
                        HistoryFilter.VOID -> allTransactions.filter {
                            TransactionStatus.fromCode(it.status) == TransactionStatus.VOID
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(44.dp),
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.history_empty_title),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.history_empty_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(filtered, key = { it.id }) { tx ->
                            TransactionHistoryCard(
                                tx = tx,
                                onClick = { onSelectTransaction(tx) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniHeroMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = Color.White.copy(alpha = 0.14f),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f),
            )
            Text(
                text = value,
                style = MonoPriceStyle.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize),
                color = Color.White,
            )
        }
    }
}

@Composable
private fun TransactionHistoryCard(
    tx: PosTransaction,
    onClick: () -> Unit,
) {
    val isVoid = TransactionStatus.fromCode(tx.status) == TransactionStatus.VOID
    val methodLabel = PaymentMethod.fromCode(tx.paymentMethod).label

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag("tx_card_${tx.id}"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = tx.receiptNumber,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Surface(
                        color = if (isVoid) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Text(
                            text = if (isVoid) "VOID" else methodLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isVoid) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                    if (tx.saleMode == com.example.data.model.SaleMode.WHOLESALE.code) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(6.dp),
                        ) {
                            Text(
                                text = "GROSIR",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildString {
                        append(formatTimestamp(tx.createdAt))
                        append(" • ${tx.itemCount} item")
                        if (tx.customerName.isNotBlank()) append(" • ${tx.customerName}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = formatRupiah(tx.totalAmount),
                style = MonoPriceStyle,
                color = if (isVoid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
        }
    }
}
