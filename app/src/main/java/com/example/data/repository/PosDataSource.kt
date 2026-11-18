package com.example.data.repository

import com.example.data.model.CartItem
import com.example.data.model.PaymentMethod
import com.example.data.model.PosTransaction
import com.example.data.model.Product
import com.example.data.model.SaleMode
import com.example.data.model.StoreProfile
import kotlinx.coroutines.flow.Flow

interface PosDataSource {
    fun observeStoreProfile(userId: String): Flow<StoreProfile?>
    suspend fun saveStoreProfile(userId: String, profile: StoreProfile): Result<Unit>

    fun observeProducts(userId: String): Flow<List<Product>>
    suspend fun saveProduct(userId: String, product: Product): Result<Unit>
    suspend fun deleteProduct(userId: String, productId: String): Result<Unit>
    suspend fun seedStarterProducts(userId: String): Result<Unit>

    fun observeTransactions(userId: String): Flow<List<PosTransaction>>
    suspend fun completeCheckout(
        userId: String,
        cartItems: List<CartItem>,
        productsById: Map<String, Product>,
        discountAmount: Long,
        taxAmount: Long,
        paymentMethod: PaymentMethod,
        saleMode: SaleMode,
        cashReceived: Long,
        customerName: String,
        note: String,
    ): Result<PosTransaction>

    suspend fun voidTransaction(userId: String, transactionId: String): Result<Unit>
}
