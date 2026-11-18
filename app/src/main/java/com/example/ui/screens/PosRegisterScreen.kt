package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
fun PosRegisterScreen(
    productsState: UiState<List<Product>>,
    storeProfile: StoreProfile,
    cart: CartUiState,
    searchQuery: String,
    selectedCategory: String,
    isSaving: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onAddToCart: (Product) -> Unit,
    onAddCustomItem: (String, Long, Long) -> Unit,
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
    var showCheckoutSheet by remember { mutableStateOf(false) }
    var showCustomItemDialog by remember { mutableStateOf(false) }

    val (discountAmount, taxAmount, totalAmount) = remember(cart, storeProfile) {
        onComputeTotals(cart, storeProfile)
    }

    if (showCheckoutSheet) {
        BackHandler { showCheckoutSheet = false }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTwoPane = maxWidth >= 720.dp

        Row(modifier = Modifier.fillMaxSize()) {
            // Left / Main Catalog Area
            Box(
                modifier = Modifier
                    .weight(if (isTwoPane) 0.58f else 1f)
                    .fillMaxHeight(),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = storeProfile.storeName.ifBlank { "KasirKu" },
                                style = MaterialTheme.typography.headlineSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "Siap melayani transaksi",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        OutlinedButton(
                            onClick = { showCustomItemDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("custom_item_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.pos_custom_item))
                        }
                    }

                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text(stringResource(R.string.pos_search_hint)) },
                        leadingIcon = {
                            Icon(Icons.Filled.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Hapus pencarian")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("pos_search_input"),
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    when (productsState) {
                        is UiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        is UiState.Error -> {
                            Box(
                                modifier = Modifier
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
                                EmptyCatalogPrompt(
                                    isSaving = isSaving,
                                    onSeedStarter = onSeedStarterProducts,
                                    onGoToProducts = onNavigateToProducts,
                                )
                            } else {
                                val categories = remember(allProducts) {
                                    listOf("Semua") + allProducts.map { it.category }.distinct().sorted()
                                }
                                val filtered = remember(allProducts, searchQuery, selectedCategory) {
                                    allProducts.filter { p ->
                                        val matchesCat = selectedCategory == "Semua" ||
                                            p.category.equals(selectedCategory, ignoreCase = true)
                                        val q = searchQuery.trim()
                                        val matchesQuery = q.isEmpty() ||
                                            p.name.contains(q, ignoreCase = true) ||
                                            p.sku.contains(q, ignoreCase = true)
                                        matchesCat && matchesQuery
                                    }
                                }
                                val cartQuantities = remember(cart.items) {
                                    cart.items.associate { it.productId to it.quantity }
                                }

                                // Category Chips
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    items(categories) { category ->
                                        val isSelected = selectedCategory == category
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { onCategorySelect(category) },
                                            label = { Text(category) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                            ),
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (filtered.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "Tidak ada produk yang cocok dengan pencarian.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                } else {
                                    LazyVerticalGrid(
                                        columns = GridCells.Adaptive(minSize = 154.dp),
                                        contentPadding = PaddingValues(
                                            start = 16.dp,
                                            end = 16.dp,
                                            top = 4.dp,
                                            bottom = if (!isTwoPane && cart.items.isNotEmpty()) 96.dp else 20.dp,
                                        ),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxSize(),
                                    ) {
                                        items(filtered, key = { it.id }) { product ->
                                            PosProductCard(
                                                product = product,
                                                cartQty = cartQuantities[product.id] ?: 0L,
                                                onClick = { onAddToCart(product) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Compact Floating Cart Summary Bar
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
                                .testTag("open_cart_bar"),
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
                                            text = "${cart.itemCount} Item di Keranjang",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text(
                                            text = "Ketuk untuk bayar",
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

            // Tablet Split-Pane Right Checkout Panel
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

    if (showCustomItemDialog) {
        CustomItemDialog(
            onAdd = { itemName, itemPrice, itemQty ->
                onAddCustomItem(itemName, itemPrice, itemQty)
                showCustomItemDialog = false
            },
            onDismiss = { showCustomItemDialog = false },
        )
    }
}

@Composable
private fun PosProductCard(
    product: Product,
    cartQty: Long,
    onClick: () -> Unit,
) {
    val isOutOfStock = product.trackStock && product.stock <= 0L
    val isLowStock = product.trackStock && product.stock in 1L..5L

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (cartQty > 0L) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (cartQty > 0L) 1.5.dp else 1.dp,
                color = if (cartQty > 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.medium,
            )
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = !isOutOfStock, onClick = onClick)
            .testTag("pos_product_card_${product.id}"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Text(
                        text = product.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    )
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
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        )
                    }
                }
            }

            Text(
                text = product.name,
                style = MaterialTheme.typography.titleSmall,
                color = if (isOutOfStock) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f) else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(40.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text(
                        text = formatRupiah(product.price),
                        style = MonoPriceStyle,
                        color = if (isOutOfStock) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = when {
                            isOutOfStock -> stringResource(R.string.pos_out_of_stock)
                            !product.trackStock -> "Tanpa batas • ${product.unit}"
                            else -> "Stok: ${product.stock} ${product.unit}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            isOutOfStock -> MaterialTheme.colorScheme.error
                            isLowStock -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = if (isOutOfStock) {
                                MaterialTheme.colorScheme.surfaceVariant
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            shape = RoundedCornerShape(10.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddShoppingCart,
                        contentDescription = "Tambah ${product.name}",
                        tint = if (isOutOfStock) Color.Gray else MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyCatalogPrompt(
    isSaving: Boolean,
    onSeedStarter: () -> Unit,
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
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = CircleShape,
            modifier = Modifier.size(68.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Inventory2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.pos_empty_catalog_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.pos_empty_catalog_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(18.dp))
        Button(
            onClick = onSeedStarter,
            enabled = !isSaving,
            modifier = Modifier.testTag("seed_starter_products_button"),
        ) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.pos_seed_starter))
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onGoToProducts,
            modifier = Modifier.testTag("go_add_product_button"),
        ) {
            Text(stringResource(R.string.pos_go_add_product))
        }
    }
}

@Composable
private fun CustomItemDialog(
    onAdd: (String, Long, Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var qtyText by remember { mutableStateOf("1") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pos_custom_item)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(80); errorText = null },
                    label = { Text("Nama Item") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter(Char::isDigit).take(10); errorText = null },
                    label = { Text("Harga Satuan (Rp)") },
                    prefix = { Text("Rp ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it.filter(Char::isDigit).take(4) },
                    label = { Text("Jumlah") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
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
                    val price = priceText.toLongOrNull()
                    val qty = qtyText.toLongOrNull() ?: 1L
                    if (name.trim().isEmpty() || price == null || price < 0L || qty < 1L) {
                        errorText = "Lengkapi nama dan harga yang valid."
                    } else {
                        onAdd(name.trim(), price, qty)
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
