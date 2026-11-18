package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.StoreProfile
import com.example.data.model.UiState
import com.example.ui.components.CheckoutPanel
import com.example.ui.theme.MonoPriceStyle
import com.example.ui.viewmodel.CartUiState
import com.example.util.formatRupiah

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WholesaleScreen(
    productsState: UiState<List<Product>>,
    storeProfile: StoreProfile,
    cart: CartUiState,
    isSaving: Boolean,
    onAddWholesale: (Product, Long) -> Unit,
    onIncrementCartItem: (String) -> Unit,
    onDecrementCartItem: (String) -> Unit,
    onRemoveFromCart: (String) -> Unit,
    onClearCart: () -> Unit,
    onDiscountChange: (Long) -> Unit,
    onCustomerNameChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onPaymentMethodChange: (PaymentMethod) -> Unit,
    onCashReceivedChange: (String) -> Unit,
    onComputeTotals: (CartUiState, StoreProfile) -> Triple<Long, Long, Long>,
    onConfirmPay: () -> Unit,
    onSeedStarterProducts: () -> Unit,
    onNavigateToProducts: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var search by remember { mutableStateOf("") }
    var onlyConfiguredWholesale by remember { mutableStateOf(true) }
    var showCheckoutSheet by remember { mutableStateOf(false) }
    var bulkQtyProduct by remember { mutableStateOf<Product?>(null) }

    val (discountAmount, taxAmount, totalAmount) = remember(cart, storeProfile) {
        onComputeTotals(cart, storeProfile)
    }

    if (showCheckoutSheet) {
        BackHandler { showCheckoutSheet = false }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTwoPane = maxWidth >= 720.dp

        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(if (isTwoPane) 0.58f else 1f)
                    .fillMaxHeight(),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    MaterialTheme.colorScheme.tertiaryContainer,
                                    RoundedCornerShape(12.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocalShipping,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.wholesale_title),
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            Text(
                                text = stringResource(R.string.wholesale_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = { Text(stringResource(R.string.pos_search_hint)) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (search.isNotEmpty()) {
                                IconButton(onClick = { search = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Hapus pencarian")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("wholesale_search_input"),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            FilterChip(
                                selected = onlyConfiguredWholesale,
                                onClick = { onlyConfiguredWholesale = true },
                                label = { Text(stringResource(R.string.wholesale_only_configured)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                                modifier = Modifier.testTag("filter_wholesale_only"),
                            )
                        }
                        item {
                            FilterChip(
                                selected = !onlyConfiguredWholesale,
                                onClick = { onlyConfiguredWholesale = false },
                                label = { Text(stringResource(R.string.wholesale_show_all)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                                modifier = Modifier.testTag("filter_wholesale_all"),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    when (productsState) {
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
                                    text = productsState.message,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }

                        is UiState.Success -> {
                            val allProducts = productsState.data
                            if (allProducts.isEmpty()) {
                                EmptyWholesaleState(
                                    hasCatalog = false,
                                    isSaving = isSaving,
                                    onSeedStarter = onSeedStarterProducts,
                                    onShowAll = { onlyConfiguredWholesale = false },
                                    onGoToProducts = onNavigateToProducts,
                                )
                            } else {
                                val filtered = remember(allProducts, search, onlyConfiguredWholesale) {
                                    val q = search.trim()
                                    allProducts.filter { p ->
                                        val matchesMode = !onlyConfiguredWholesale || p.wholesalePrice > 0L
                                        val matchesQuery = q.isEmpty() ||
                                            p.name.contains(q, ignoreCase = true) ||
                                            p.sku.contains(q, ignoreCase = true) ||
                                            p.category.contains(q, ignoreCase = true)
                                        matchesMode && matchesQuery
                                    }
                                }
                                val cartQuantities = remember(cart.items) {
                                    cart.items.associate { it.productId to it.quantity }
                                }

                                if (filtered.isEmpty()) {
                                    EmptyWholesaleState(
                                        hasCatalog = true,
                                        isSaving = isSaving,
                                        onSeedStarter = onSeedStarterProducts,
                                        onShowAll = { onlyConfiguredWholesale = false },
                                        onGoToProducts = onNavigateToProducts,
                                    )
                                } else {
                                    LazyColumn(
                                        contentPadding = PaddingValues(
                                            start = 16.dp,
                                            end = 16.dp,
                                            top = 4.dp,
                                            bottom = if (!isTwoPane && cart.items.isNotEmpty()) 96.dp else 20.dp,
                                        ),
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxSize(),
                                    ) {
                                        items(filtered, key = { it.id }) { product ->
                                            WholesaleProductCard(
                                                product = product,
                                                cartQty = cartQuantities[product.id] ?: 0L,
                                                onAddMinBundle = {
                                                    val qty = if (product.wholesaleMinQty > 0L) product.wholesaleMinQty else 5L
                                                    onAddWholesale(product, qty)
                                                },
                                                onAddTen = { onAddWholesale(product, 10L) },
                                                onCustomBulk = { bulkQtyProduct = product },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (!isTwoPane) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = cart.items.isNotEmpty(),
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = RoundedCornerShape(16.dp),
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { showCheckoutSheet = true }
                                .testTag("wholesale_open_cart_bar"),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                                        shape = CircleShape,
                                        modifier = Modifier.size(34.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.ShoppingBag,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = "${cart.itemCount} Item Grosir",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text(
                                            text = if (cart.totalWholesaleSavings > 0L) {
                                                "Hemat ${formatRupiah(cart.totalWholesaleSavings)}"
                                            } else {
                                                "Ketuk untuk bayar"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                                        )
                                    }
                                }

                                Text(
                                    text = formatRupiah(totalAmount),
                                    style = MonoPriceStyle.copy(
                                        fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                    ),
                                )
                            }
                        }
                    }
                }
            }

            if (isTwoPane) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    CheckoutPanel(
                        cart = cart,
                        store = storeProfile,
                        discountAmount = discountAmount,
                        taxAmount = taxAmount,
                        totalAmount = totalAmount,
                        onIncrement = onIncrementCartItem,
                        onDecrement = onDecrementCartItem,
                        onRemove = onRemoveFromCart,
                        onClearCart = onClearCart,
                        onDiscountChange = onDiscountChange,
                        onCustomerNameChange = onCustomerNameChange,
                        onNoteChange = onNoteChange,
                        onPaymentMethodChange = onPaymentMethodChange,
                        onCashReceivedChange = onCashReceivedChange,
                        onConfirmPay = onConfirmPay,
                    )
                }
            }
        }
    }

    if (showCheckoutSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showCheckoutSheet = false },
            sheetState = sheetState,
        ) {
            CheckoutPanel(
                cart = cart,
                store = storeProfile,
                discountAmount = discountAmount,
                taxAmount = taxAmount,
                totalAmount = totalAmount,
                onIncrement = onIncrementCartItem,
                onDecrement = onDecrementCartItem,
                onRemove = onRemoveFromCart,
                onClearCart = {
                    onClearCart()
                    showCheckoutSheet = false
                },
                onDiscountChange = onDiscountChange,
                onCustomerNameChange = onCustomerNameChange,
                onNoteChange = onNoteChange,
                onPaymentMethodChange = onPaymentMethodChange,
                onCashReceivedChange = onCashReceivedChange,
                onConfirmPay = {
                    onConfirmPay()
                    showCheckoutSheet = false
                },
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
    }

    val targetBulk = bulkQtyProduct
    if (targetBulk != null) {
        BulkQuantityDialog(
            product = targetBulk,
            onConfirm = { qty ->
                onAddWholesale(targetBulk, qty)
                bulkQtyProduct = null
            },
            onDismiss = { bulkQtyProduct = null },
        )
    }
}

@Composable
private fun WholesaleProductCard(
    product: Product,
    cartQty: Long,
    onAddMinBundle: () -> Unit,
    onAddTen: () -> Unit,
    onCustomBulk: () -> Unit,
) {
    val isOutOfStock = product.trackStock && product.stock <= 0L
    val hasWholesale = product.wholesalePrice > 0L
    val activeUnitPrice = if (hasWholesale) product.wholesalePrice else product.price
    val minQty = if (product.wholesaleMinQty > 0L) product.wholesaleMinQty else 5L
    val savingsPct = if (hasWholesale && product.price > product.wholesalePrice && product.price > 0L) {
        (((product.price - product.wholesalePrice) * 100L) / product.price).toInt()
    } else 0

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (cartQty > 0L) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (cartQty > 0L) 1.5.dp else 1.dp,
                color = if (cartQty > 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.medium,
            )
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = !isOutOfStock, onClick = onCustomBulk)
            .testTag("wholesale_card_${product.id}"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Text(
                            text = product.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        )
                    }
                    if (savingsPct > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(6.dp),
                        ) {
                            Text(
                                text = "Hemat $savingsPct%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            )
                        }
                    }
                }

                if (cartQty > 0L) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                    ) {
                        Text(
                            text = "${cartQty}x",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }

            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "${formatRupiah(activeUnitPrice)} / ${product.unit}",
                            style = MonoPriceStyle,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        if (hasWholesale && product.price > product.wholesalePrice) {
                            Text(
                                text = formatRupiah(product.price),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    textDecoration = TextDecoration.LineThrough,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text(
                        text = buildString {
                            if (hasWholesale) {
                                append("Min. $minQty ${product.unit}")
                                if (product.wholesaleUnit.isNotBlank()) {
                                    append(" (1 ${product.wholesaleUnit})")
                                }
                                append(" • ")
                            }
                            append(
                                when {
                                    isOutOfStock -> stringResource(R.string.pos_out_of_stock)
                                    !product.trackStock -> "Stok tak terbatas"
                                    else -> "Stok: ${product.stock} ${product.unit}"
                                }
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isOutOfStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onAddMinBundle,
                    enabled = !isOutOfStock,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("wholesale_add_min_${product.id}"),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (product.wholesaleUnit.isNotBlank() && hasWholesale) {
                            "+1 ${product.wholesaleUnit} ($minQty)"
                        } else {
                            "+$minQty ${product.unit}"
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                OutlinedButton(
                    onClick = onAddTen,
                    enabled = !isOutOfStock,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("wholesale_add_10_${product.id}"),
                ) {
                    Text("+10")
                }

                OutlinedButton(
                    onClick = onCustomBulk,
                    enabled = !isOutOfStock,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("wholesale_custom_qty_${product.id}"),
                ) {
                    Text("Jumlah")
                }
            }
        }
    }
}

@Composable
private fun EmptyWholesaleState(
    hasCatalog: Boolean,
    isSaving: Boolean,
    onSeedStarter: () -> Unit,
    onShowAll: () -> Unit,
    onGoToProducts: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = CircleShape,
            modifier = Modifier.size(64.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Inventory,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.wholesale_empty_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.wholesale_empty_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (!hasCatalog) {
            Button(
                onClick = onSeedStarter,
                enabled = !isSaving,
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.pos_seed_starter))
            }
            Spacer(modifier = Modifier.height(8.dp))
        } else {
            Button(onClick = onShowAll) {
                Text(stringResource(R.string.wholesale_show_all))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        OutlinedButton(onClick = onGoToProducts) {
            Text(stringResource(R.string.products_title))
        }
    }
}

@Composable
private fun BulkQuantityDialog(
    product: Product,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val defaultQty = if (product.wholesaleMinQty > 0L) product.wholesaleMinQty else 5L
    var qtyText by remember(product) { mutableStateOf(defaultQty.toString()) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val unitPrice = if (product.wholesalePrice > 0L) product.wholesalePrice else product.price
    val parsedQty = qtyText.toLongOrNull() ?: 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Order Grosir • ${product.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Harga grosir: ${formatRupiah(unitPrice)} / ${product.unit}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it.filter(Char::isDigit).take(5); errorText = null },
                    label = { Text("Jumlah (${product.unit})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (parsedQty > 0L) {
                    Text(
                        text = "Subtotal: ${formatRupiah(parsedQty * unitPrice)}",
                        style = MonoPriceStyle,
                    )
                }
                if (errorText != null) {
                    Text(
                        text = errorText ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = qtyText.toLongOrNull()
                    if (qty == null || qty < 1L) {
                        errorText = "Masukkan jumlah minimal 1."
                    } else {
                        onConfirm(qty)
                    }
                }
            ) {
                Text(stringResource(R.string.common_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}
