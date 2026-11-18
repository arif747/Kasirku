package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.CartItem
import com.example.data.model.PaymentMethod
import com.example.data.model.PosTransaction
import com.example.data.model.Product
import com.example.data.model.SaleMode
import com.example.data.model.StoreProfile
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionStatus
import com.example.util.generateReceiptNumber
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class PosRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) : PosDataSource {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireAuthorizedUser(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Pengguna harus masuk dengan Google terlebih dahulu.")
        check(uid == expectedUserId) { "Sesi akun berubah. Silakan muat ulang." }
        return uid
    }

    override fun observeStoreProfile(userId: String): Flow<StoreProfile?> =
        db.collection("users")
            .document(userId)
            .collection("store")
            .document("profile")
            .snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) {
                    null
                } else {
                    snapshot.toObject(
                        StoreProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
            }

    override suspend fun saveStoreProfile(userId: String, profile: StoreProfile): Result<Unit> {
        val docPath = "users/$userId/store/profile"
        return try {
            val uid = requireAuthorizedUser(userId)
            val cleanName = profile.storeName.trim().take(60)
            require(cleanName.isNotEmpty()) { "Nama toko wajib diisi." }
            val cleanAddress = profile.address.trim().take(150)
            val cleanPhone = profile.phone.trim().take(30)
            val cleanFooter = profile.receiptFooter.trim().take(120)
            val cleanTax = profile.taxPercent.coerceIn(0.0, 30.0)

            val docRef = db.collection("users").document(uid)
                .collection("store").document("profile")
            val exists = docRef.get().await().exists()

            if (!exists) {
                val payload = mapOf(
                    "userId" to uid,
                    "storeName" to cleanName,
                    "address" to cleanAddress,
                    "phone" to cleanPhone,
                    "receiptFooter" to cleanFooter,
                    "taxPercent" to cleanTax,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                docRef.set(payload).await()
            } else {
                val payload = mapOf(
                    "storeName" to cleanName,
                    "address" to cleanAddress,
                    "phone" to cleanPhone,
                    "receiptFooter" to cleanFooter,
                    "taxPercent" to cleanTax,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                docRef.update(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    override fun observeProducts(userId: String): Flow<List<Product>> =
        db.collection("users")
            .document(userId)
            .collection("products")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    Product::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.name.lowercase() }
            }

    override suspend fun saveProduct(userId: String, product: Product): Result<Unit> {
        val isCreate = product.id.isBlank()
        val op = if (isCreate) OperationType.CREATE else OperationType.UPDATE
        var docPath = "users/$userId/products/${product.id}"
        return try {
            val uid = requireAuthorizedUser(userId)
            val cleanName = product.name.trim().take(80)
            val cleanCategory = product.category.trim().ifBlank { "Umum" }.take(40)
            val cleanUnit = product.unit.trim().ifBlank { "pcs" }.take(20)
            val cleanWholesaleUnit = product.wholesaleUnit.trim().take(20)
            val cleanSku = product.sku.trim().take(40)
            val cleanPrice = product.price.coerceIn(0L, 1_000_000_000L)
            val cleanWholesalePrice = product.wholesalePrice.coerceIn(0L, 1_000_000_000L)
            val cleanWholesaleMinQty = product.wholesaleMinQty.coerceIn(0L, 100_000L)
            val cleanStock = product.stock.coerceIn(0L, 1_000_000L)
            require(cleanName.isNotEmpty()) { "Nama produk wajib diisi." }

            val colRef = db.collection("users").document(uid).collection("products")
            if (isCreate) {
                val docRef = colRef.document()
                docPath = docRef.path
                val payload = mapOf(
                    "userId" to uid,
                    "name" to cleanName,
                    "category" to cleanCategory,
                    "price" to cleanPrice,
                    "wholesalePrice" to cleanWholesalePrice,
                    "wholesaleMinQty" to cleanWholesaleMinQty,
                    "wholesaleUnit" to cleanWholesaleUnit,
                    "stock" to cleanStock,
                    "trackStock" to product.trackStock,
                    "unit" to cleanUnit,
                    "sku" to cleanSku,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                docRef.set(payload).await()
            } else {
                val docRef = colRef.document(product.id)
                docPath = docRef.path
                val payload = mapOf(
                    "name" to cleanName,
                    "category" to cleanCategory,
                    "price" to cleanPrice,
                    "wholesalePrice" to cleanWholesalePrice,
                    "wholesaleMinQty" to cleanWholesaleMinQty,
                    "wholesaleUnit" to cleanWholesaleUnit,
                    "stock" to cleanStock,
                    "trackStock" to product.trackStock,
                    "unit" to cleanUnit,
                    "sku" to cleanSku,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                docRef.update(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, op, docPath)
            Result.failure(e)
        }
    }

    override suspend fun deleteProduct(userId: String, productId: String): Result<Unit> {
        val docPath = "users/$userId/products/$productId"
        return try {
            val uid = requireAuthorizedUser(userId)
            require(productId.isNotBlank()) { "ID produk tidak valid." }
            db.collection("users").document(uid)
                .collection("products").document(productId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun seedStarterProducts(userId: String): Result<Unit> {
        val colPath = "users/$userId/products"
        return try {
            val uid = requireAuthorizedUser(userId)
            val colRef = db.collection("users").document(uid).collection("products")
            val starterItems = listOf(
                Triple("Es Kopi Susu Aren", "Minuman", 18000L) to listOf(15000L, 10L, "cup", 40L, "pack"),
                Triple("Americano Dingin", "Minuman", 15000L) to listOf(12500L, 10L, "cup", 35L, "pack"),
                Triple("Matcha Latte", "Minuman", 22000L) to listOf(18500L, 6L, "cup", 25L, "pack"),
                Triple("Teh Tarik Klasik", "Minuman", 12000L) to listOf(9500L, 12L, "cup", 50L, "lusin"),
                Triple("Nasi Goreng Spesial", "Makanan", 28000L) to listOf(24000L, 5L, "porsi", 20L, "paket"),
                Triple("Mie Goreng Jawa", "Makanan", 24000L) to listOf(20500L, 5L, "porsi", 20L, "paket"),
                Triple("Roti Bakar Coklat Keju", "Snack", 16000L) to listOf(13500L, 5L, "porsi", 30L, "box"),
                Triple("Air Mineral 600ml", "Minuman", 4000L) to listOf(2800L, 24L, "botol", 120L, "dus"),
            )
            val batch = db.batch()
            starterItems.forEachIndexed { index, (meta, extra) ->
                val docRef = colRef.document()
                val payload = mapOf(
                    "userId" to uid,
                    "name" to meta.first,
                    "category" to meta.second,
                    "price" to meta.third,
                    "wholesalePrice" to (extra[0] as Long),
                    "wholesaleMinQty" to (extra[1] as Long),
                    "wholesaleUnit" to (extra[4] as String),
                    "stock" to (extra[3] as Long),
                    "trackStock" to true,
                    "unit" to (extra[2] as String),
                    "sku" to "PRD-00${index + 1}",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                batch.set(docRef, payload)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colPath)
            Result.failure(e)
        }
    }

    override fun observeTransactions(userId: String): Flow<List<PosTransaction>> =
        db.collection("users")
            .document(userId)
            .collection("transactions")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    PosTransaction::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    override suspend fun completeCheckout(
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
    ): Result<PosTransaction> {
        var docPath = "users/$userId/transactions"
        return try {
            val uid = requireAuthorizedUser(userId)
            require(cartItems.isNotEmpty() && cartItems.size <= 100) {
                "Keranjang minimal 1 item dan maksimal 100 item."
            }

            val lineItems = cartItems.map { item ->
                TransactionItem(
                    productId = item.productId,
                    name = item.name.take(80),
                    unitPrice = item.unitPrice.coerceAtLeast(0L),
                    quantity = item.quantity.coerceAtLeast(1L),
                    subtotal = (item.unitPrice * item.quantity).coerceAtLeast(0L),
                )
            }
            val itemCount = lineItems.sumOf { it.quantity }.coerceIn(1L, 10_000L)
            val subtotal = lineItems.sumOf { it.subtotal }.coerceIn(0L, 1_000_000_000L)
            val cleanDiscount = discountAmount.coerceIn(0L, subtotal)
            val afterDiscount = (subtotal - cleanDiscount).coerceAtLeast(0L)
            val cleanTax = taxAmount.coerceIn(0L, 1_000_000_000L)
            val totalAmount = (afterDiscount + cleanTax).coerceIn(0L, 1_000_000_000L)

            val paidAmount = if (paymentMethod == PaymentMethod.CASH) {
                cashReceived.coerceAtLeast(totalAmount)
            } else {
                totalAmount
            }
            val changeAmount = (paidAmount - totalAmount).coerceAtLeast(0L)

            val cleanCustomer = customerName.trim().take(60)
            val cleanNote = note.trim().take(150)
            val receiptNumber = generateReceiptNumber()

            val txDocRef = db.collection("users").document(uid)
                .collection("transactions").document()
            docPath = txDocRef.path

            val itemsPayload = lineItems.map { item ->
                mapOf(
                    "productId" to item.productId,
                    "name" to item.name,
                    "unitPrice" to item.unitPrice,
                    "quantity" to item.quantity,
                    "subtotal" to item.subtotal,
                )
            }

            val txPayload = mapOf(
                "userId" to uid,
                "receiptNumber" to receiptNumber,
                "customerName" to cleanCustomer,
                "saleMode" to saleMode.code,
                "items" to itemsPayload,
                "itemCount" to itemCount,
                "subtotal" to subtotal,
                "discountAmount" to cleanDiscount,
                "taxAmount" to cleanTax,
                "totalAmount" to totalAmount,
                "paymentMethod" to paymentMethod.code,
                "cashReceived" to paidAmount,
                "changeAmount" to changeAmount,
                "status" to TransactionStatus.COMPLETED.code,
                "note" to cleanNote,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )

            val batch = db.batch()
            batch.set(txDocRef, txPayload)

            cartItems.forEach { cartItem ->
                val product = productsById[cartItem.productId]
                if (product != null && product.trackStock && product.id.isNotBlank()) {
                    val newStock = (product.stock - cartItem.quantity).coerceIn(0L, 1_000_000L)
                    val productRef = db.collection("users").document(uid)
                        .collection("products").document(product.id)
                    batch.update(
                        productRef,
                        mapOf(
                            "stock" to newStock,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    )
                }
            }

            batch.commit().await()

            val createdTx = PosTransaction(
                id = txDocRef.id,
                userId = uid,
                receiptNumber = receiptNumber,
                customerName = cleanCustomer,
                saleMode = saleMode.code,
                items = lineItems,
                itemCount = itemCount,
                subtotal = subtotal,
                discountAmount = cleanDiscount,
                taxAmount = cleanTax,
                totalAmount = totalAmount,
                paymentMethod = paymentMethod.code,
                cashReceived = paidAmount,
                changeAmount = changeAmount,
                status = TransactionStatus.COMPLETED.code,
                note = cleanNote,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now(),
            )
            Result.success(createdTx)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun voidTransaction(userId: String, transactionId: String): Result<Unit> {
        val docPath = "users/$userId/transactions/$transactionId"
        return try {
            val uid = requireAuthorizedUser(userId)
            require(transactionId.isNotBlank()) { "ID transaksi tidak valid." }
            db.collection("users").document(uid)
                .collection("transactions").document(transactionId)
                .update(
                    mapOf(
                        "status" to TransactionStatus.VOID.code,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }
}
