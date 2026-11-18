package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.example.data.model.CartItem
import com.example.data.model.PaymentMethod
import com.example.data.model.PosTransaction
import com.example.data.model.Product
import com.example.data.model.SaleMode
import com.example.data.model.StoreProfile
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionStatus
import com.example.data.repository.PosDataSource
import com.example.util.generateReceiptNumber
import com.google.firebase.Timestamp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date
import java.util.UUID

@Entity(tableName = "local_store_profile")
data class LocalStoreEntity(
    @PrimaryKey val userId: String,
    val storeName: String,
    val address: String,
    val phone: String,
    val receiptFooter: String,
    val taxPercent: Double,
    val updatedAtMillis: Long,
)

@Entity(tableName = "local_products")
data class LocalProductEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val category: String,
    val price: Long,
    val wholesalePrice: Long,
    val wholesaleMinQty: Long,
    val wholesaleUnit: String,
    val stock: Long,
    val trackStock: Boolean,
    val unit: String,
    val sku: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

@Entity(tableName = "local_transactions")
data class LocalTransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val receiptNumber: String,
    val customerName: String,
    val saleMode: String,
    val itemsJson: String,
    val itemCount: Long,
    val subtotal: Long,
    val discountAmount: Long,
    val taxAmount: Long,
    val totalAmount: Long,
    val paymentMethod: String,
    val cashReceived: Long,
    val changeAmount: Long,
    val status: String,
    val note: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

@Dao
interface LocalStoreDao {
    @Query("SELECT * FROM local_store_profile WHERE userId = :userId LIMIT 1")
    fun observeProfile(userId: String): Flow<LocalStoreEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(entity: LocalStoreEntity)
}

@Dao
interface LocalProductDao {
    @Query("SELECT * FROM local_products WHERE userId = :userId ORDER BY LOWER(name) ASC")
    fun observeProducts(userId: String): Flow<List<LocalProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProduct(entity: LocalProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<LocalProductEntity>)

    @Query("DELETE FROM local_products WHERE id = :id AND userId = :userId")
    suspend fun deleteProduct(id: String, userId: String)

    @Query("UPDATE local_products SET stock = :newStock, updatedAtMillis = :updatedAt WHERE id = :id AND userId = :userId")
    suspend fun updateStock(id: String, userId: String, newStock: Long, updatedAt: Long)
}

@Dao
interface LocalTransactionDao {
    @Query("SELECT * FROM local_transactions WHERE userId = :userId ORDER BY createdAtMillis DESC")
    fun observeTransactions(userId: String): Flow<List<LocalTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(entity: LocalTransactionEntity)

    @Query("UPDATE local_transactions SET status = :status, updatedAtMillis = :updatedAt WHERE id = :id AND userId = :userId")
    suspend fun updateStatus(id: String, userId: String, status: String, updatedAt: Long)
}

@Database(
    entities = [LocalStoreEntity::class, LocalProductEntity::class, LocalTransactionEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class KasirLocalDatabase : RoomDatabase() {
    abstract fun storeDao(): LocalStoreDao
    abstract fun productDao(): LocalProductDao
    abstract fun transactionDao(): LocalTransactionDao

    companion object {
        @Volatile
        private var instance: KasirLocalDatabase? = null

        fun getInstance(context: Context): KasirLocalDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    KasirLocalDatabase::class.java,
                    "kasirku_local.db",
                ).build().also { instance = it }
            }
    }
}

class LocalPosRepository(
    private val db: KasirLocalDatabase,
) : PosDataSource {

    constructor(context: Context) : this(KasirLocalDatabase.getInstance(context))

    override fun observeStoreProfile(userId: String): Flow<StoreProfile?> =
        db.storeDao().observeProfile(userId).map { entity ->
            entity?.let {
                StoreProfile(
                    userId = it.userId,
                    storeName = it.storeName,
                    address = it.address,
                    phone = it.phone,
                    receiptFooter = it.receiptFooter,
                    taxPercent = it.taxPercent,
                    updatedAt = Timestamp(Date(it.updatedAtMillis)),
                )
            }
        }

    override suspend fun saveStoreProfile(userId: String, profile: StoreProfile): Result<Unit> =
        try {
            val cleanName = profile.storeName.trim().take(60)
            require(cleanName.isNotEmpty()) { "Nama toko wajib diisi." }
            db.storeDao().upsertProfile(
                LocalStoreEntity(
                    userId = userId,
                    storeName = cleanName,
                    address = profile.address.trim().take(150),
                    phone = profile.phone.trim().take(30),
                    receiptFooter = profile.receiptFooter.trim().take(120),
                    taxPercent = profile.taxPercent.coerceIn(0.0, 30.0),
                    updatedAtMillis = System.currentTimeMillis(),
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    override fun observeProducts(userId: String): Flow<List<Product>> =
        db.productDao().observeProducts(userId).map { list ->
            list.map { e ->
                Product(
                    id = e.id,
                    userId = e.userId,
                    name = e.name,
                    category = e.category,
                    price = e.price,
                    wholesalePrice = e.wholesalePrice,
                    wholesaleMinQty = e.wholesaleMinQty,
                    wholesaleUnit = e.wholesaleUnit,
                    stock = e.stock,
                    trackStock = e.trackStock,
                    unit = e.unit,
                    sku = e.sku,
                    createdAt = Timestamp(Date(e.createdAtMillis)),
                    updatedAt = Timestamp(Date(e.updatedAtMillis)),
                )
            }
        }

    override suspend fun saveProduct(userId: String, product: Product): Result<Unit> =
        try {
            val cleanName = product.name.trim().take(80)
            require(cleanName.isNotEmpty()) { "Nama produk wajib diisi." }
            val now = System.currentTimeMillis()
            val id = product.id.ifBlank { UUID.randomUUID().toString() }
            db.productDao().upsertProduct(
                LocalProductEntity(
                    id = id,
                    userId = userId,
                    name = cleanName,
                    category = product.category.trim().ifBlank { "Umum" }.take(40),
                    price = product.price.coerceIn(0L, 1_000_000_000L),
                    wholesalePrice = product.wholesalePrice.coerceIn(0L, 1_000_000_000L),
                    wholesaleMinQty = product.wholesaleMinQty.coerceIn(0L, 100_000L),
                    wholesaleUnit = product.wholesaleUnit.trim().take(20),
                    stock = product.stock.coerceIn(0L, 1_000_000L),
                    trackStock = product.trackStock,
                    unit = product.unit.trim().ifBlank { "pcs" }.take(20),
                    sku = product.sku.trim().take(40),
                    createdAtMillis = product.createdAt?.toDate()?.time ?: now,
                    updatedAtMillis = now,
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun deleteProduct(userId: String, productId: String): Result<Unit> =
        try {
            require(productId.isNotBlank()) { "ID produk tidak valid." }
            db.productDao().deleteProduct(productId, userId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun seedStarterProducts(userId: String): Result<Unit> =
        try {
            val now = System.currentTimeMillis()
            val starter = listOf(
                Triple("Es Kopi Susu Aren", "Minuman", 18000L) to listOf(15000L, 10L, "cup", 40L, "pack"),
                Triple("Americano Dingin", "Minuman", 15000L) to listOf(12500L, 10L, "cup", 35L, "pack"),
                Triple("Matcha Latte", "Minuman", 22000L) to listOf(18500L, 6L, "cup", 25L, "pack"),
                Triple("Teh Tarik Klasik", "Minuman", 12000L) to listOf(9500L, 12L, "cup", 50L, "lusin"),
                Triple("Nasi Goreng Spesial", "Makanan", 28000L) to listOf(24000L, 5L, "porsi", 20L, "paket"),
                Triple("Mie Goreng Jawa", "Makanan", 24000L) to listOf(20500L, 5L, "porsi", 20L, "paket"),
                Triple("Roti Bakar Coklat Keju", "Snack", 16000L) to listOf(13500L, 5L, "porsi", 30L, "box"),
                Triple("Air Mineral 600ml", "Minuman", 4000L) to listOf(2800L, 24L, "botol", 120L, "dus"),
            ).mapIndexed { index, (meta, extra) ->
                LocalProductEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    name = meta.first,
                    category = meta.second,
                    price = meta.third,
                    wholesalePrice = extra[0] as Long,
                    wholesaleMinQty = extra[1] as Long,
                    wholesaleUnit = extra[4] as String,
                    stock = extra[3] as Long,
                    trackStock = true,
                    unit = extra[2] as String,
                    sku = "PRD-00${index + 1}",
                    createdAtMillis = now,
                    updatedAtMillis = now,
                )
            }
            db.productDao().upsertAll(starter)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    override fun observeTransactions(userId: String): Flow<List<PosTransaction>> =
        db.transactionDao().observeTransactions(userId).map { list ->
            list.map { e ->
                PosTransaction(
                    id = e.id,
                    userId = e.userId,
                    receiptNumber = e.receiptNumber,
                    customerName = e.customerName,
                    saleMode = e.saleMode,
                    items = decodeItemsJson(e.itemsJson),
                    itemCount = e.itemCount,
                    subtotal = e.subtotal,
                    discountAmount = e.discountAmount,
                    taxAmount = e.taxAmount,
                    totalAmount = e.totalAmount,
                    paymentMethod = e.paymentMethod,
                    cashReceived = e.cashReceived,
                    changeAmount = e.changeAmount,
                    status = e.status,
                    note = e.note,
                    createdAt = Timestamp(Date(e.createdAtMillis)),
                    updatedAt = Timestamp(Date(e.updatedAtMillis)),
                )
            }
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
    ): Result<PosTransaction> =
        try {
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
            val now = System.currentTimeMillis()
            val txId = UUID.randomUUID().toString()
            val receiptNumber = generateReceiptNumber()
            val cleanCustomer = customerName.trim().take(60)
            val cleanNote = note.trim().take(150)

            val entity = LocalTransactionEntity(
                id = txId,
                userId = userId,
                receiptNumber = receiptNumber,
                customerName = cleanCustomer,
                saleMode = saleMode.code,
                itemsJson = encodeItemsJson(lineItems),
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
                createdAtMillis = now,
                updatedAtMillis = now,
            )

            db.withTransaction {
                db.transactionDao().insertTransaction(entity)
                cartItems.forEach { cartItem ->
                    val product = productsById[cartItem.productId]
                    if (product != null && product.trackStock && product.id.isNotBlank()) {
                        val newStock = (product.stock - cartItem.quantity).coerceIn(0L, 1_000_000L)
                        db.productDao().updateStock(product.id, userId, newStock, now)
                    }
                }
            }

            Result.success(
                PosTransaction(
                    id = txId,
                    userId = userId,
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
                    createdAt = Timestamp(Date(now)),
                    updatedAt = Timestamp(Date(now)),
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun voidTransaction(userId: String, transactionId: String): Result<Unit> =
        try {
            require(transactionId.isNotBlank()) { "ID transaksi tidak valid." }
            db.transactionDao().updateStatus(
                id = transactionId,
                userId = userId,
                status = TransactionStatus.VOID.code,
                updatedAt = System.currentTimeMillis(),
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    private fun encodeItemsJson(items: List<TransactionItem>): String {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("productId", item.productId)
                    put("name", item.name)
                    put("unitPrice", item.unitPrice)
                    put("quantity", item.quantity)
                    put("subtotal", item.subtotal)
                }
            )
        }
        return arr.toString()
    }

    private fun decodeItemsJson(json: String): List<TransactionItem> {
        val arr = JSONArray(json)
        return List(arr.length()) { i ->
            val obj = arr.getJSONObject(i)
            TransactionItem(
                productId = obj.optString("productId", ""),
                name = obj.optString("name", ""),
                unitPrice = obj.optLong("unitPrice", 0L),
                quantity = obj.optLong("quantity", 1L),
                subtotal = obj.optLong("subtotal", 0L),
            )
        }
    }
}
