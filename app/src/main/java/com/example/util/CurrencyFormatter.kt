package com.example.util

import com.example.data.model.PaymentMethod
import com.example.data.model.PosTransaction
import com.example.data.model.StoreProfile
import com.example.data.model.TransactionStatus
import com.google.firebase.Timestamp
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val idLocale = Locale.forLanguageTag("id-ID")
private val rupiahSymbols = DecimalFormatSymbols(idLocale).apply {
    groupingSeparator = '.'
    decimalSeparator = ','
}
private val rupiahFormatter = DecimalFormat("#,###", rupiahSymbols)
private val dateTimeFormatter = SimpleDateFormat("dd MMM yyyy, HH:mm", idLocale)
private val shortTimeFormatter = SimpleDateFormat("HH:mm", idLocale)
private val receiptNumberDateFormatter = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)

fun formatRupiah(amount: Long): String {
    val prefix = if (amount < 0) "-Rp " else "Rp "
    val absValue = kotlin.math.abs(amount)
    return prefix + rupiahFormatter.format(absValue)
}

fun formatTimestamp(timestamp: Timestamp?): String {
    val date = timestamp?.toDate() ?: Date()
    return dateTimeFormatter.format(date)
}

fun formatTimeOnly(timestamp: Timestamp?): String {
    val date = timestamp?.toDate() ?: Date()
    return shortTimeFormatter.format(date)
}

fun generateReceiptNumber(): String {
    val stamp = receiptNumberDateFormatter.format(Date())
    val suffix = (100..999).random()
    return "INV-$stamp-$suffix"
}

fun isToday(timestamp: Timestamp?): Boolean {
    val date = timestamp?.toDate() ?: return false
    val dayFmt = SimpleDateFormat("yyyyMMdd", Locale.US)
    return dayFmt.format(date) == dayFmt.format(Date())
}

fun buildShareableReceiptText(
    tx: PosTransaction,
    store: StoreProfile,
): String = buildString {
    appendLine("================================")
    appendLine(store.storeName.ifBlank { "KasirKu" }.uppercase(idLocale))
    if (store.address.isNotBlank()) appendLine(store.address)
    if (store.phone.isNotBlank()) appendLine("Telp: ${store.phone}")
    appendLine("================================")
    appendLine("No    : ${tx.receiptNumber}")
    appendLine("Waktu : ${formatTimestamp(tx.createdAt)}")
    if (tx.saleMode == com.example.data.model.SaleMode.WHOLESALE.code) {
        appendLine("Mode  : GROSIR")
    }
    if (tx.customerName.isNotBlank()) {
        appendLine("Plg   : ${tx.customerName}")
    }
    if (TransactionStatus.fromCode(tx.status) == TransactionStatus.VOID) {
        appendLine("STATUS: DIBATALKAN (VOID)")
    }
    appendLine("--------------------------------")
    tx.items.forEach { item ->
        appendLine(item.name)
        appendLine("  ${item.quantity} x ${formatRupiah(item.unitPrice)} = ${formatRupiah(item.subtotal)}")
    }
    appendLine("--------------------------------")
    appendLine("Subtotal   : ${formatRupiah(tx.subtotal)}")
    if (tx.discountAmount > 0L) {
        appendLine("Diskon     : -${formatRupiah(tx.discountAmount)}")
    }
    if (tx.taxAmount > 0L) {
        appendLine("Pajak/PPN  : ${formatRupiah(tx.taxAmount)}")
    }
    appendLine("TOTAL      : ${formatRupiah(tx.totalAmount)}")
    appendLine("Metode     : ${PaymentMethod.fromCode(tx.paymentMethod).label}")
    if (tx.paymentMethod == PaymentMethod.CASH.code) {
        appendLine("Dibayar    : ${formatRupiah(tx.cashReceived)}")
        appendLine("Kembalian  : ${formatRupiah(tx.changeAmount)}")
    }
    if (tx.note.isNotBlank()) {
        appendLine("Catatan    : ${tx.note}")
    }
    appendLine("================================")
    appendLine(store.receiptFooter.ifBlank { "Terima kasih telah berbelanja!" })
}
