package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.model.PaymentMethod
import com.example.data.model.PosTransaction
import com.example.data.model.StoreProfile
import com.example.data.model.TransactionStatus
import com.example.ui.theme.MonoReceiptStyle
import com.example.util.buildShareableReceiptText
import com.example.util.formatRupiah
import com.example.util.formatTimestamp

@Composable
fun ReceiptDialog(
    transaction: PosTransaction,
    store: StoreProfile,
    allowVoid: Boolean,
    onVoidTransaction: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var showVoidConfirm by remember { mutableStateOf(false) }
    val status = TransactionStatus.fromCode(transaction.status)
    val isVoid = status == TransactionStatus.VOID

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = if (isVoid) Icons.Filled.Block else Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = if (isVoid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isVoid) stringResource(R.string.receipt_void_badge) else stringResource(R.string.receipt_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isVoid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Thermal paper card
                val paperBg = Color(0xFFFFFDF8)
                val paperInk = Color(0xFF1F2421)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(paperBg, RoundedCornerShape(10.dp))
                        .padding(14.dp),
                ) {
                    Text(
                        text = store.storeName.ifBlank { "KasirKu" }.uppercase(),
                        style = MonoReceiptStyle.copy(fontWeight = FontWeight.Bold),
                        color = paperInk,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (store.address.isNotBlank()) {
                        Text(
                            text = store.address,
                            style = MonoReceiptStyle,
                            color = paperInk.copy(alpha = 0.78f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (store.phone.isNotBlank()) {
                        Text(
                            text = "Telp: ${store.phone}",
                            style = MonoReceiptStyle,
                            color = paperInk.copy(alpha = 0.78f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = paperInk.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "No: ${transaction.receiptNumber}",
                        style = MonoReceiptStyle,
                        color = paperInk,
                    )
                    Text(
                        text = "Tgl: ${formatTimestamp(transaction.createdAt)}",
                        style = MonoReceiptStyle,
                        color = paperInk,
                    )
                    if (transaction.saleMode == com.example.data.model.SaleMode.WHOLESALE.code) {
                        Text(
                            text = "Mode: GROSIR",
                            style = MonoReceiptStyle.copy(fontWeight = FontWeight.Bold),
                            color = paperInk,
                        )
                    }
                    if (transaction.customerName.isNotBlank()) {
                        Text(
                            text = "Plg: ${transaction.customerName}",
                            style = MonoReceiptStyle,
                            color = paperInk,
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = paperInk.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))

                    transaction.items.forEach { item ->
                        Text(
                            text = item.name,
                            style = MonoReceiptStyle.copy(fontWeight = FontWeight.Medium),
                            color = paperInk,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "  ${item.quantity} x ${formatRupiah(item.unitPrice)}",
                                style = MonoReceiptStyle,
                                color = paperInk.copy(alpha = 0.8f),
                            )
                            Text(
                                text = formatRupiah(item.subtotal),
                                style = MonoReceiptStyle,
                                color = paperInk,
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(color = paperInk.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))

                    ReceiptLine("Subtotal", formatRupiah(transaction.subtotal), paperInk)
                    if (transaction.discountAmount > 0L) {
                        ReceiptLine("Diskon", "-${formatRupiah(transaction.discountAmount)}", paperInk)
                    }
                    if (transaction.taxAmount > 0L) {
                        ReceiptLine("Pajak/PPN", formatRupiah(transaction.taxAmount), paperInk)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    ReceiptLine("TOTAL", formatRupiah(transaction.totalAmount), paperInk, bold = true)
                    ReceiptLine(
                        "Metode",
                        PaymentMethod.fromCode(transaction.paymentMethod).label,
                        paperInk,
                    )
                    if (transaction.paymentMethod == PaymentMethod.CASH.code) {
                        ReceiptLine("Tunai", formatRupiah(transaction.cashReceived), paperInk)
                        ReceiptLine("Kembali", formatRupiah(transaction.changeAmount), paperInk)
                    }
                    if (transaction.note.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Catatan: ${transaction.note}",
                            style = MonoReceiptStyle,
                            color = paperInk.copy(alpha = 0.78f),
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = store.receiptFooter.ifBlank { "Terima kasih telah berbelanja!" },
                        style = MonoReceiptStyle,
                        color = paperInk.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // Serrated bottom edge accent
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp),
                ) {
                    val teeth = 22
                    val step = size.width / teeth
                    val path = Path().apply {
                        moveTo(0f, 0f)
                        for (i in 0 until teeth) {
                            lineTo(i * step + step / 2f, size.height)
                            lineTo((i + 1) * step, 0f)
                        }
                        close()
                    }
                    drawPath(path = path, color = Color(0xFFFFFDF8))
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            val shareText = buildShareableReceiptText(transaction, store)
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Bagikan Struk"))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_receipt_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.receipt_share))
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("close_receipt_button"),
                    ) {
                        Text(stringResource(R.string.receipt_close))
                    }
                }

                if (allowVoid && !isVoid && transaction.id.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(
                        onClick = { showVoidConfirm = true },
                        modifier = Modifier.testTag("void_transaction_button"),
                    ) {
                        Text(
                            text = stringResource(R.string.receipt_void_action),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }

    if (showVoidConfirm) {
        AlertDialog(
            onDismissRequest = { showVoidConfirm = false },
            title = { Text("Batalkan Transaksi?") },
            text = { Text("Transaksi ${transaction.receiptNumber} akan ditandai VOID dan tidak masuk omzet.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showVoidConfirm = false
                        onVoidTransaction(transaction.id)
                    }
                ) {
                    Text(stringResource(R.string.common_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showVoidConfirm = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
private fun ReceiptLine(
    label: String,
    value: String,
    ink: Color,
    bold: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = if (bold) MonoReceiptStyle.copy(fontWeight = FontWeight.Bold) else MonoReceiptStyle,
            color = ink,
        )
        Text(
            text = value,
            style = if (bold) MonoReceiptStyle.copy(fontWeight = FontWeight.Bold) else MonoReceiptStyle,
            color = ink,
        )
    }
}
