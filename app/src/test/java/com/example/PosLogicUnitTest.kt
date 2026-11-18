package com.example

import com.example.data.model.CartItem
import com.example.data.model.PaymentMethod
import com.example.data.model.PosTransaction
import com.example.data.model.Product
import com.example.data.model.SaleMode
import com.example.data.model.StoreProfile
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionStatus
import com.example.util.buildShareableReceiptText
import com.example.util.formatRupiah
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PosLogicUnitTest {

    @Test
    fun `formatRupiah formats Indonesian currency accurately`() {
        assertEquals("Rp 0", formatRupiah(0L))
        assertEquals("Rp 18.000", formatRupiah(18_000L))
        assertEquals("Rp 1.250.000", formatRupiah(1_250_000L))
        assertEquals("-Rp 5.000", formatRupiah(-5_000L))
    }

    @Test
    fun `enum fallbacks handle unknown schema values safely`() {
        assertEquals(PaymentMethod.CASH, PaymentMethod.fromCode("UNKNOWN"))
        assertEquals(PaymentMethod.QRIS, PaymentMethod.fromCode("QRIS"))
        assertEquals(TransactionStatus.COMPLETED, TransactionStatus.fromCode("LEGACY"))
        assertEquals(TransactionStatus.VOID, TransactionStatus.fromCode("VOID"))
        assertEquals(SaleMode.RETAIL, SaleMode.fromCode("UNKNOWN"))
        assertEquals(SaleMode.WHOLESALE, SaleMode.fromCode("WHOLESALE"))
    }

    @Test
    fun `CartItem automatically applies wholesale price and savings at min qty or when forced`() {
        val product = Product(
            id = "p1",
            name = "Minyak Goreng 1L",
            price = 20_000L,
            wholesalePrice = 17_500L,
            wholesaleMinQty = 6L,
            wholesaleUnit = "dus",
        )
        assertTrue(product.hasWholesalePricing)

        val retailCartItem = CartItem(
            productId = product.id,
            name = product.name,
            retailPrice = product.price,
            wholesalePrice = product.wholesalePrice,
            wholesaleMinQty = product.wholesaleMinQty,
            wholesaleUnit = product.wholesaleUnit,
            forceWholesale = false,
            quantity = 3L,
            unit = "pcs",
            trackStock = true,
            availableStock = 50L,
        )
        assertFalse(retailCartItem.wholesaleActive)
        assertEquals(20_000L, retailCartItem.unitPrice)
        assertEquals(60_000L, retailCartItem.subtotal)
        assertEquals(0L, retailCartItem.savingsAmount)

        val autoWholesaleItem = retailCartItem.copy(quantity = 6L)
        assertTrue(autoWholesaleItem.wholesaleActive)
        assertEquals(17_500L, autoWholesaleItem.unitPrice)
        assertEquals(105_000L, autoWholesaleItem.subtotal)
        assertEquals(15_000L, autoWholesaleItem.savingsAmount)

        val forcedWholesaleItem = retailCartItem.copy(forceWholesale = true, quantity = 2L)
        assertTrue(forcedWholesaleItem.wholesaleActive)
        assertEquals(17_500L, forcedWholesaleItem.unitPrice)
        assertEquals(35_000L, forcedWholesaleItem.subtotal)
        assertEquals(5_000L, forcedWholesaleItem.savingsAmount)
    }

    @Test
    fun `buildShareableReceiptText includes store name, wholesale mode, items, and total`() {
        val store = StoreProfile(
            storeName = "Warung Kopi Kita",
            address = "Jl. Sudirman 10",
            phone = "0811223344",
            receiptFooter = "Matur nuwun!",
        )
        val tx = PosTransaction(
            receiptNumber = "INV-20261117-101",
            saleMode = SaleMode.WHOLESALE.code,
            items = listOf(
                TransactionItem(
                    productId = "p1",
                    name = "Kopi Susu",
                    unitPrice = 18_000L,
                    quantity = 2L,
                    subtotal = 36_000L,
                )
            ),
            itemCount = 2L,
            subtotal = 36_000L,
            totalAmount = 36_000L,
            paymentMethod = "CASH",
            cashReceived = 50_000L,
            changeAmount = 14_000L,
        )
        val text = buildShareableReceiptText(tx, store)
        assertTrue(text.contains("WARUNG KOPI KITA"))
        assertTrue(text.contains("INV-20261117-101"))
        assertTrue(text.contains("Mode  : GROSIR"))
        assertTrue(text.contains("Kopi Susu"))
        assertTrue(text.contains("Rp 36.000"))
        assertTrue(text.contains("Rp 14.000"))
        assertTrue(text.contains("Matur nuwun!"))
    }
}
