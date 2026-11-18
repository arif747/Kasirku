package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CartItem
import com.example.data.model.PaymentMethod
import com.example.data.model.SaleMode
import com.example.data.model.StoreProfile
import com.example.ui.theme.MonoPriceStyle
import com.example.ui.viewmodel.CartUiState
import com.example.util.formatRupiah

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CheckoutPanel(
    cart: CartUiState,
    store: StoreProfile,
    discountAmount: Long,
    taxAmount: Long,
    totalAmount: Long,
    onIncrement: (String) -> Unit,
    onDecrement: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClearCart: () -> Unit,
    onDiscountChange: (Long) -> Unit,
    onCustomerNameChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onPaymentMethodChange: (PaymentMethod) -> Unit,
    onCashReceivedChange: (String) -> Unit,
    onConfirmPay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var discountInput by remember(cart.discountAmount) {
        mutableStateOf(if (cart.discountAmount > 0L) cart.discountAmount.toString() else "")
    }

    val cashReceived = if (cart.paymentMethod == PaymentMethod.CASH) {
        cart.cashReceivedInput.toLongOrNull() ?: totalAmount
    } else {
        totalAmount
    }
    val changeAmount = (cashReceived - totalAmount).coerceAtLeast(0L)
    val isCashInsufficient = cart.paymentMethod == PaymentMethod.CASH &&
        cart.cashReceivedInput.isNotEmpty() &&
        (cart.cashReceivedInput.toLongOrNull() ?: 0L) < totalAmount

    val isWholesaleCart = cart.saleMode == SaleMode.WHOLESALE || cart.items.any { it.wholesaleActive }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.ShoppingCart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.pos_cart_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                if (cart.items.isNotEmpty() && isWholesaleCart) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.wholesale_active_badge),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        )
                    }
                }
            }
            if (cart.items.isNotEmpty()) {
                TextButton(
                    onClick = onClearCart,
                    modifier = Modifier.testTag("clear_cart_button"),
                ) {
                    Text(
                        text = stringResource(R.string.pos_clear_cart),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (cart.items.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Filled.ShoppingCart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(36.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.pos_cart_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                cart.items.forEach { item ->
                    CartItemRow(
                        item = item,
                        onIncrement = { onIncrement(item.productId) },
                        onDecrement = { onDecrement(item.productId) },
                        onRemove = { onRemove(item.productId) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = cart.customerName,
                    onValueChange = onCustomerNameChange,
                    label = { Text(stringResource(R.string.checkout_customer_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("checkout_customer_input"),
                )
                OutlinedTextField(
                    value = discountInput,
                    onValueChange = { raw ->
                        val digits = raw.filter { it.isDigit() }.take(9)
                        discountInput = digits
                        onDiscountChange(digits.toLongOrNull() ?: 0L)
                    },
                    label = { Text(stringResource(R.string.checkout_discount_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("checkout_discount_input"),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = cart.note,
                onValueChange = onNoteChange,
                label = { Text(stringResource(R.string.checkout_note_label)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("checkout_note_input"),
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = stringResource(R.string.checkout_method_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PaymentMethod.entries.forEach { method ->
                    val selected = cart.paymentMethod == method
                    val icon = when (method) {
                        PaymentMethod.CASH -> Icons.Filled.Payments
                        PaymentMethod.QRIS -> Icons.Filled.QrCode2
                        PaymentMethod.TRANSFER -> Icons.Filled.AccountBalance
                    }
                    FilterChip(
                        selected = selected,
                        onClick = { onPaymentMethodChange(method) },
                        label = { Text(method.label) },
                        leadingIcon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = method.label,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("payment_method_${method.code.lowercase()}"),
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (cart.paymentMethod) {
                PaymentMethod.CASH -> {
                    OutlinedTextField(
                        value = cart.cashReceivedInput,
                        onValueChange = onCashReceivedChange,
                        label = { Text(stringResource(R.string.checkout_cash_received)) },
                        placeholder = { Text(totalAmount.toString()) },
                        prefix = { Text("Rp ") },
                        singleLine = true,
                        isError = isCashInsufficient,
                        supportingText = if (isCashInsufficient) {
                            { Text(stringResource(R.string.checkout_insufficient_cash)) }
                        } else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cash_received_input"),
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val suggestions = remember(totalAmount) {
                        buildList {
                            add("Uang Pas" to totalAmount)
                            listOf(20_000L, 50_000L, 100_000L, 500_000L).forEach { denom ->
                                if (denom > totalAmount) {
                                    add(formatRupiah(denom) to denom)
                                }
                            }
                        }.distinctBy { it.second }
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        suggestions.forEach { (label, amount) ->
                            OutlinedButton(
                                onClick = { onCashReceivedChange(amount.toString()) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.minimumInteractiveComponentSize(),
                            ) {
                                Text(label, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                PaymentMethod.QRIS -> {
                    QrisPreviewCard(
                        storeName = store.storeName,
                        totalAmount = totalAmount,
                    )
                }

                PaymentMethod.TRANSFER -> {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.checkout_transfer_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SummaryAmountRow(
                        label = stringResource(R.string.checkout_subtotal),
                        amount = formatRupiah(cart.subtotal),
                    )
                    if (cart.totalWholesaleSavings > 0L) {
                        SummaryAmountRow(
                            label = stringResource(R.string.wholesale_savings_label),
                            amount = formatRupiah(cart.totalWholesaleSavings),
                            valueColor = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (discountAmount > 0L) {
                        SummaryAmountRow(
                            label = stringResource(R.string.checkout_discount),
                            amount = "-${formatRupiah(discountAmount)}",
                            valueColor = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                    if (taxAmount > 0L) {
                        SummaryAmountRow(
                            label = "${stringResource(R.string.checkout_tax)} (${store.taxPercent}%)",
                            amount = formatRupiah(taxAmount),
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.checkout_total),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = formatRupiah(totalAmount),
                            style = MonoPriceStyle.copy(
                                fontSize = MaterialTheme.typography.titleLarge.fontSize,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (cart.paymentMethod == PaymentMethod.CASH) {
                        SummaryAmountRow(
                            label = stringResource(R.string.checkout_change),
                            amount = formatRupiah(changeAmount),
                            valueColor = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onConfirmPay,
                enabled = !cart.isProcessing && !isCashInsufficient && cart.items.isNotEmpty(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("confirm_pay_button"),
            ) {
                if (cart.isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${stringResource(R.string.checkout_confirm_pay)} • ${formatRupiah(totalAmount)}",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (item.wholesaleActive) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(4.dp),
                        ) {
                            Text(
                                text = "Grosir",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            )
                        }
                    }
                }
                Text(
                    text = "${formatRupiah(item.unitPrice)} / ${item.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onDecrement,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Remove,
                        contentDescription = "Kurangi jumlah ${item.name}",
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    text = item.quantity.toString(),
                    style = MonoPriceStyle,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                IconButton(
                    onClick = onIncrement,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Tambah jumlah ${item.name}",
                        modifier = Modifier.size(18.dp),
                    )
                }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Hapus ${item.name}",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryAmountRow(
    label: String,
    amount: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = amount,
            style = MonoPriceStyle.copy(fontWeight = FontWeight.SemiBold),
            color = valueColor,
        )
    }
}

@Composable
private fun QrisPreviewCard(
    storeName: String,
    totalAmount: Long,
) {
    Surface(
        color = Color.White,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Canvas(
                modifier = Modifier
                    .size(72.dp)
                    .background(Color.White)
                    .padding(4.dp),
            ) {
                val grid = 9
                val cell = size.width / grid
                val seed = (storeName.hashCode() xor totalAmount.toInt())
                for (r in 0 until grid) {
                    for (c in 0 until grid) {
                        val isFinder = (r < 3 && c < 3) || (r < 3 && c >= grid - 3) || (r >= grid - 3 && c < 3)
                        val fill = isFinder || (((r * 31 + c * 17 + seed) and 1) == 0)
                        if (fill) {
                            drawRect(
                                color = Color(0xFF111827),
                                topLeft = Offset(c * cell, r * cell),
                                size = Size(cell * 0.9f, cell * 0.9f),
                            )
                        }
                    }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "QRIS • ${storeName.ifBlank { "KasirKu" }}",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF111827),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.checkout_qris_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4B5563),
                )
            }
        }
    }
}
