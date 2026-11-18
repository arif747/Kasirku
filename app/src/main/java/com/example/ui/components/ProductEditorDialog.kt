package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.model.Product

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductEditorDialog(
    initialProduct: Product?,
    isSaving: Boolean,
    onSave: (Product) -> Unit,
    onDismiss: () -> Unit,
) {
    val isEditing = initialProduct != null && initialProduct.id.isNotBlank()
    var name by remember(initialProduct) { mutableStateOf(initialProduct?.name ?: "") }
    var category by remember(initialProduct) { mutableStateOf(initialProduct?.category ?: "Minuman") }
    var priceText by remember(initialProduct) {
        mutableStateOf(if (initialProduct != null && initialProduct.price > 0L) initialProduct.price.toString() else "")
    }
    var wholesalePriceText by remember(initialProduct) {
        mutableStateOf(
            if (initialProduct != null && initialProduct.wholesalePrice > 0L) {
                initialProduct.wholesalePrice.toString()
            } else ""
        )
    }
    var wholesaleMinQtyText by remember(initialProduct) {
        mutableStateOf(
            if (initialProduct != null && initialProduct.wholesaleMinQty > 0L) {
                initialProduct.wholesaleMinQty.toString()
            } else ""
        )
    }
    var wholesaleUnit by remember(initialProduct) { mutableStateOf(initialProduct?.wholesaleUnit ?: "") }
    var stockText by remember(initialProduct) {
        mutableStateOf(initialProduct?.stock?.toString() ?: "10")
    }
    var unit by remember(initialProduct) { mutableStateOf(initialProduct?.unit ?: "pcs") }
    var sku by remember(initialProduct) { mutableStateOf(initialProduct?.sku ?: "") }
    var trackStock by remember(initialProduct) { mutableStateOf(initialProduct?.trackStock ?: true) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val quickCategories = listOf("Minuman", "Makanan", "Snack", "Sembako", "Lainnya")

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
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = if (isEditing) stringResource(R.string.products_edit) else stringResource(R.string.products_add),
                    style = MaterialTheme.typography.titleLarge,
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(80); validationError = null },
                    label = { Text(stringResource(R.string.products_name_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_name_input"),
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it.take(40) },
                    label = { Text(stringResource(R.string.products_category_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_category_input"),
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    quickCategories.forEach { cat ->
                        FilterChip(
                            selected = category.equals(cat, ignoreCase = true),
                            onClick = { category = cat },
                            label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it.filter(Char::isDigit).take(10); validationError = null },
                        label = { Text(stringResource(R.string.products_price_label)) },
                        prefix = { Text("Rp ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_price_input"),
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it.take(20) },
                        label = { Text(stringResource(R.string.products_unit_label)) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.75f)
                            .testTag("product_unit_input"),
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Text(
                    text = stringResource(R.string.products_wholesale_section),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )

                OutlinedTextField(
                    value = wholesalePriceText,
                    onValueChange = { wholesalePriceText = it.filter(Char::isDigit).take(10) },
                    label = { Text(stringResource(R.string.products_wholesale_price_label)) },
                    prefix = { Text("Rp ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_wholesale_price_input"),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = wholesaleMinQtyText,
                        onValueChange = { wholesaleMinQtyText = it.filter(Char::isDigit).take(6) },
                        label = { Text(stringResource(R.string.products_wholesale_min_qty_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_wholesale_min_qty_input"),
                    )
                    OutlinedTextField(
                        value = wholesaleUnit,
                        onValueChange = { wholesaleUnit = it.take(20) },
                        label = { Text(stringResource(R.string.products_wholesale_unit_label)) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_wholesale_unit_input"),
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = stockText,
                        onValueChange = { stockText = it.filter(Char::isDigit).take(7) },
                        label = { Text(stringResource(R.string.products_stock_label)) },
                        enabled = trackStock,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(0.8f)
                            .testTag("product_stock_input"),
                    )
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it.take(40) },
                        label = { Text(stringResource(R.string.products_sku_label)) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_sku_input"),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.products_track_stock_label),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = trackStock,
                        onCheckedChange = { trackStock = it },
                        modifier = Modifier.testTag("product_track_stock_switch"),
                    )
                }

                if (validationError != null) {
                    Text(
                        text = validationError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.common_cancel))
                    }
                    Button(
                        enabled = !isSaving,
                        onClick = {
                            val cleanName = name.trim()
                            val parsedPrice = priceText.toLongOrNull()
                            val parsedWholesalePrice = wholesalePriceText.toLongOrNull() ?: 0L
                            val parsedWholesaleMin = wholesaleMinQtyText.toLongOrNull() ?: 0L
                            val parsedStock = stockText.toLongOrNull() ?: 0L
                            if (cleanName.isEmpty()) {
                                validationError = "Nama produk tidak boleh kosong."
                                return@Button
                            }
                            if (parsedPrice == null || parsedPrice < 0L) {
                                validationError = "Masukkan harga eceran yang valid."
                                return@Button
                            }
                            val result = (initialProduct ?: Product()).copy(
                                name = cleanName,
                                category = category.trim().ifBlank { "Umum" },
                                price = parsedPrice,
                                wholesalePrice = parsedWholesalePrice.coerceAtLeast(0L),
                                wholesaleMinQty = if (parsedWholesalePrice > 0L) {
                                    parsedWholesaleMin.coerceAtLeast(1L)
                                } else 0L,
                                wholesaleUnit = wholesaleUnit.trim(),
                                stock = parsedStock.coerceAtLeast(0L),
                                trackStock = trackStock,
                                unit = unit.trim().ifBlank { "pcs" },
                                sku = sku.trim(),
                            )
                            onSave(result)
                        },
                        modifier = Modifier.testTag("save_product_button"),
                    ) {
                        Text(stringResource(R.string.products_save))
                    }
                }
            }
        }
    }
}
