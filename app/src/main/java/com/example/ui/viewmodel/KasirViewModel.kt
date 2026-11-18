package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CartItem
import com.example.data.model.PaymentMethod
import com.example.data.model.PosTransaction
import com.example.data.model.Product
import com.example.data.model.SaleMode
import com.example.data.model.StoreProfile
import com.example.data.model.UiState
import com.example.data.repository.PosDataSource
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CartUiState(
    val items: List<CartItem> = emptyList(),
    val saleMode: SaleMode = SaleMode.RETAIL,
    val discountAmount: Long = 0L,
    val customerName: String = "",
    val note: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val cashReceivedInput: String = "",
    val isProcessing: Boolean = false,
) {
    val itemCount: Long get() = items.sumOf { it.quantity }
    val subtotal: Long get() = items.sumOf { it.subtotal }
    val totalWholesaleSavings: Long get() = items.sumOf { it.savingsAmount }
}

class KasirViewModel(
    private val repository: PosDataSource,
    private val currentUserId: String,
) : ViewModel() {

    val productsState: StateFlow<UiState<List<Product>>> =
        repository.observeProducts(currentUserId)
            .map<List<Product>, UiState<List<Product>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing products", root)
                emit(UiState.Error(root.localizedMessage ?: "Gagal memuat produk"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading,
            )

    val transactionsState: StateFlow<UiState<List<PosTransaction>>> =
        repository.observeTransactions(currentUserId)
            .map<List<PosTransaction>, UiState<List<PosTransaction>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing transactions", root)
                emit(UiState.Error(root.localizedMessage ?: "Gagal memuat riwayat transaksi"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading,
            )

    val storeProfileState: StateFlow<StoreProfile> =
        repository.observeStoreProfile(currentUserId)
            .map { it ?: StoreProfile(userId = currentUserId) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing store profile", firestoreError ?: error)
                emit(StoreProfile(userId = currentUserId))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = StoreProfile(userId = currentUserId),
            )

    private val _cartState = MutableStateFlow(CartUiState())
    val cartState: StateFlow<CartUiState> = _cartState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Semua")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _activeReceipt = MutableStateFlow<PosTransaction?>(null)
    val activeReceipt: StateFlow<PosTransaction?> = _activeReceipt.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun addToCart(product: Product) {
        addProductWithQty(product = product, qtyToAdd = 1L, forceWholesale = false)
    }

    fun addWholesaleToCart(product: Product, qtyToAdd: Long) {
        addProductWithQty(
            product = product,
            qtyToAdd = qtyToAdd.coerceAtLeast(1L),
            forceWholesale = product.wholesalePrice > 0L,
        )
    }

    private fun addProductWithQty(
        product: Product,
        qtyToAdd: Long,
        forceWholesale: Boolean,
    ) {
        if (product.trackStock && product.stock <= 0L) {
            _statusMessage.value = "${product.name} sedang habis stoknya."
            return
        }
        _cartState.update { state ->
            val existing = state.items.firstOrNull { it.productId == product.id }
            val updatedItems = if (existing != null) {
                val targetQty = existing.quantity + qtyToAdd
                if (product.trackStock && targetQty > product.stock) {
                    _statusMessage.value = "Stok ${product.name} tersisa ${product.stock} ${product.unit}."
                    return@update state
                }
                state.items.map { item ->
                    if (item.productId == product.id) {
                        item.copy(
                            quantity = targetQty,
                            forceWholesale = item.forceWholesale || forceWholesale,
                        )
                    } else {
                        item
                    }
                }
            } else {
                if (product.trackStock && qtyToAdd > product.stock) {
                    _statusMessage.value = "Stok ${product.name} tersisa ${product.stock} ${product.unit}."
                    return@update state
                }
                state.items + CartItem(
                    productId = product.id,
                    name = product.name,
                    retailPrice = product.price,
                    wholesalePrice = product.wholesalePrice,
                    wholesaleMinQty = product.wholesaleMinQty,
                    wholesaleUnit = product.wholesaleUnit,
                    forceWholesale = forceWholesale,
                    quantity = qtyToAdd,
                    unit = product.unit,
                    trackStock = product.trackStock,
                    availableStock = product.stock,
                )
            }
            val nextMode = if (forceWholesale || updatedItems.any { it.wholesaleActive }) {
                SaleMode.WHOLESALE
            } else {
                state.saleMode
            }
            state.copy(items = updatedItems, saleMode = nextMode)
        }
    }

    fun addCustomItemToCart(name: String, price: Long, quantity: Long) {
        val cleanName = name.trim().take(80)
        if (cleanName.isEmpty() || price < 0L || quantity < 1L) {
            _statusMessage.value = "Nama dan harga item manual harus valid."
            return
        }
        val customId = "custom_${System.currentTimeMillis()}"
        _cartState.update { state ->
            state.copy(
                items = state.items + CartItem(
                    productId = customId,
                    name = cleanName,
                    retailPrice = price,
                    quantity = quantity,
                    unit = "pcs",
                    trackStock = false,
                    availableStock = 9999L,
                )
            )
        }
    }

    fun incrementCartItem(productId: String) {
        _cartState.update { state ->
            val updated = state.items.map { item ->
                if (item.productId == productId) {
                    if (item.trackStock && item.quantity >= item.availableStock) {
                        _statusMessage.value = "Mencapai batas stok (${item.availableStock} ${item.unit})."
                        item
                    } else {
                        item.copy(quantity = item.quantity + 1L)
                    }
                } else {
                    item
                }
            }
            state.copy(items = updated)
        }
    }

    fun decrementCartItem(productId: String) {
        _cartState.update { state ->
            val updated = state.items.mapNotNull { item ->
                if (item.productId == productId) {
                    if (item.quantity <= 1L) null else item.copy(quantity = item.quantity - 1L)
                } else {
                    item
                }
            }
            val nextMode = if (updated.isEmpty()) SaleMode.RETAIL else state.saleMode
            state.copy(items = updated, saleMode = nextMode)
        }
    }

    fun removeFromCart(productId: String) {
        _cartState.update { state ->
            val updated = state.items.filterNot { it.productId == productId }
            val nextMode = if (updated.isEmpty()) SaleMode.RETAIL else state.saleMode
            state.copy(items = updated, saleMode = nextMode)
        }
    }

    fun clearCart() {
        _cartState.value = CartUiState()
    }

    fun updateDiscount(amount: Long) {
        _cartState.update { it.copy(discountAmount = amount.coerceAtLeast(0L)) }
    }

    fun updateCustomerName(name: String) {
        _cartState.update { it.copy(customerName = name.take(60)) }
    }

    fun updateNote(note: String) {
        _cartState.update { it.copy(note = note.take(150)) }
    }

    fun updatePaymentMethod(method: PaymentMethod) {
        _cartState.update { it.copy(paymentMethod = method) }
    }

    fun updateCashReceivedInput(input: String) {
        val digitsOnly = input.filter { it.isDigit() }.take(12)
        _cartState.update { it.copy(cashReceivedInput = digitsOnly) }
    }

    fun computeTotals(cart: CartUiState, store: StoreProfile): Triple<Long, Long, Long> {
        val cleanDiscount = cart.discountAmount.coerceIn(0L, cart.subtotal)
        val afterDiscount = (cart.subtotal - cleanDiscount).coerceAtLeast(0L)
        val taxAmount = ((afterDiscount * store.taxPercent) / 100.0).toLong().coerceAtLeast(0L)
        val total = (afterDiscount + taxAmount).coerceAtLeast(0L)
        return Triple(cleanDiscount, taxAmount, total)
    }

    fun completeCheckout(onSuccess: () -> Unit = {}) {
        val cart = _cartState.value
        if (cart.items.isEmpty() || cart.isProcessing) return

        val store = storeProfileState.value
        val (cleanDiscount, taxAmount, totalAmount) = computeTotals(cart, store)
        val cashReceived = if (cart.paymentMethod == PaymentMethod.CASH) {
            cart.cashReceivedInput.toLongOrNull() ?: totalAmount
        } else {
            totalAmount
        }

        if (cart.paymentMethod == PaymentMethod.CASH && cashReceived < totalAmount) {
            _statusMessage.value = "Uang diterima kurang dari total tagihan."
            return
        }

        val currentProducts = (productsState.value as? UiState.Success)?.data.orEmpty()
        val productsById = currentProducts.associateBy { it.id }
        val effectiveMode = if (cart.saleMode == SaleMode.WHOLESALE || cart.items.any { it.wholesaleActive }) {
            SaleMode.WHOLESALE
        } else {
            SaleMode.RETAIL
        }

        _cartState.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            val result = repository.completeCheckout(
                userId = currentUserId,
                cartItems = cart.items,
                productsById = productsById,
                discountAmount = cleanDiscount,
                taxAmount = taxAmount,
                paymentMethod = cart.paymentMethod,
                saleMode = effectiveMode,
                cashReceived = cashReceived,
                customerName = cart.customerName,
                note = cart.note,
            )
            result.onSuccess { tx ->
                _cartState.value = CartUiState()
                _activeReceipt.value = tx
                onSuccess()
            }.onFailure { err ->
                _cartState.update { it.copy(isProcessing = false) }
                _statusMessage.value = err.localizedMessage ?: "Transaksi gagal disimpan."
            }
        }
    }

    fun showReceipt(tx: PosTransaction) {
        _activeReceipt.value = tx
    }

    fun dismissReceipt() {
        _activeReceipt.value = null
    }

    fun saveProduct(product: Product, onSaved: () -> Unit = {}) {
        if (_isSaving.value) return
        _isSaving.value = true
        viewModelScope.launch {
            val result = repository.saveProduct(currentUserId, product)
            _isSaving.value = false
            result.onSuccess {
                _statusMessage.value = if (product.id.isBlank()) {
                    "Produk \"${product.name}\" ditambahkan."
                } else {
                    "Produk \"${product.name}\" diperbarui."
                }
                onSaved()
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Gagal menyimpan produk."
            }
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            val result = repository.deleteProduct(currentUserId, product.id)
            result.onSuccess {
                _statusMessage.value = "Produk \"${product.name}\" dihapus."
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Gagal menghapus produk."
            }
        }
    }

    fun seedStarterProducts() {
        if (_isSaving.value) return
        _isSaving.value = true
        viewModelScope.launch {
            val result = repository.seedStarterProducts(currentUserId)
            _isSaving.value = false
            result.onSuccess {
                _statusMessage.value = "8 produk contoh (eceran & grosir) berhasil dimuat."
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Gagal memuat produk contoh."
            }
        }
    }

    fun voidTransaction(txId: String) {
        viewModelScope.launch {
            val result = repository.voidTransaction(currentUserId, txId)
            result.onSuccess {
                _activeReceipt.update { current ->
                    if (current?.id == txId) current.copy(status = "VOID") else current
                }
                _statusMessage.value = "Transaksi berhasil dibatalkan (VOID)."
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Gagal membatalkan transaksi."
            }
        }
    }

    fun saveStoreProfile(profile: StoreProfile) {
        if (_isSaving.value) return
        _isSaving.value = true
        viewModelScope.launch {
            val result = repository.saveStoreProfile(currentUserId, profile)
            _isSaving.value = false
            result.onSuccess {
                _statusMessage.value = "Pengaturan toko disimpan."
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Gagal menyimpan pengaturan toko."
            }
        }
    }

    fun clearMessage() {
        _statusMessage.value = null
    }

    private companion object {
        const val TAG = "KasirVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
