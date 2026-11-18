package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class StoreProfile(
    val userId: String = "",
    val storeName: String = "Toko KasirKu",
    val address: String = "",
    val phone: String = "",
    val receiptFooter: String = "Terima kasih telah berbelanja!",
    val taxPercent: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val category: String = "Umum",
    val price: Long = 0L,
    val wholesalePrice: Long = 0L,
    val wholesaleMinQty: Long = 0L,
    val wholesaleUnit: String = "",
    val stock: Long = 0L,
    val trackStock: Boolean = true,
    val unit: String = "pcs",
    val sku: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    val hasWholesalePricing: Boolean
        get() = wholesalePrice > 0L && wholesalePrice < price
}

@Keep
@IgnoreExtraProperties
data class TransactionItem(
    val productId: String = "",
    val name: String = "",
    val unitPrice: Long = 0L,
    val quantity: Long = 1L,
    val subtotal: Long = 0L,
)

enum class PaymentMethod(val code: String, val label: String) {
    CASH("CASH", "Tunai"),
    QRIS("QRIS", "QRIS"),
    TRANSFER("TRANSFER", "Transfer");

    companion object {
        fun fromCode(raw: String): PaymentMethod =
            entries.firstOrNull { it.code == raw } ?: CASH
    }
}

enum class SaleMode(val code: String, val label: String) {
    RETAIL("RETAIL", "Eceran"),
    WHOLESALE("WHOLESALE", "Grosir");

    companion object {
        fun fromCode(raw: String): SaleMode =
            entries.firstOrNull { it.code == raw } ?: RETAIL
    }
}

enum class TransactionStatus(val code: String, val label: String) {
    COMPLETED("COMPLETED", "Selesai"),
    VOID("VOID", "Dibatalkan");

    companion object {
        fun fromCode(raw: String): TransactionStatus =
            entries.firstOrNull { it.code == raw } ?: COMPLETED
    }
}

@Keep
@IgnoreExtraProperties
data class PosTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val receiptNumber: String = "",
    val customerName: String = "",
    val saleMode: String = SaleMode.RETAIL.code,
    val items: List<TransactionItem> = emptyList(),
    val itemCount: Long = 0L,
    val subtotal: Long = 0L,
    val discountAmount: Long = 0L,
    val taxAmount: Long = 0L,
    val totalAmount: Long = 0L,
    val paymentMethod: String = PaymentMethod.CASH.code,
    val cashReceived: Long = 0L,
    val changeAmount: Long = 0L,
    val status: String = TransactionStatus.COMPLETED.code,
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

data class CartItem(
    val productId: String,
    val name: String,
    val retailPrice: Long,
    val wholesalePrice: Long = 0L,
    val wholesaleMinQty: Long = 0L,
    val wholesaleUnit: String = "",
    val forceWholesale: Boolean = false,
    val quantity: Long,
    val unit: String,
    val trackStock: Boolean,
    val availableStock: Long,
) {
    val wholesaleActive: Boolean
        get() = wholesalePrice > 0L && (forceWholesale || (wholesaleMinQty > 0L && quantity >= wholesaleMinQty))

    val unitPrice: Long
        get() = if (wholesaleActive) wholesalePrice else retailPrice

    val subtotal: Long
        get() = unitPrice * quantity

    val savingsAmount: Long
        get() = if (wholesaleActive && retailPrice > wholesalePrice) {
            (retailPrice - wholesalePrice) * quantity
        } else {
            0L
        }
}
