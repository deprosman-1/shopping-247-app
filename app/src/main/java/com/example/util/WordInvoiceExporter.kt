package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.CreditVerificationStatus
import com.example.data.model.Order
import com.example.data.model.StoreProfileConfig
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates an official Microsoft Word (.doc) invoice & payment receipt embedded
 * with the clickable ODIYA official recipient badge & verification stamp.
 */
object WordInvoiceExporter {

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }

    fun buildWordHtmlContent(
        order: Order,
        storeProfile: StoreProfileConfig
    ): String {
        val naira = NumberFormat.getCurrencyInstance(Locale("en", "NG")).apply {
            maximumFractionDigits = 0
        }
        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(order.createdAt))
        val cleanSummary = order.itemsSummary.replace(Regex("""\s*\[Sold:[^\]]+\]"""), "").trim()
            .replace("\n", "<br/>")
        val verificationLabel = when {
            order.isCreditAlertConfirmed || order.creditVerificationStatus == CreditVerificationStatus.CONFIRMED_CREDITED ->
                "ORDER &amp; PAYMENT CONFIRMED (ODIYA ACCOUNT CREDITED)"
            order.creditVerificationStatus == CreditVerificationStatus.NETWORK_DELAYED_PENDING ->
                "ORDER &amp; PAYMENT PENDING NETWORK RESTORE"
            order.creditVerificationStatus == CreditVerificationStatus.PAYMENT_FAILED_UNCREDITED ->
                "ORDER &amp; PAYMENT FAILED — AWAITING CREDIT ALERT"
            else -> escapeHtml(order.status.label)
        }
        val storeTitle = escapeHtml(storeProfile.storeName.ifBlank { "ODIYA" }.uppercase())

        return """
            <html xmlns:o="urn:schemas-microsoft-com:office:office"
                  xmlns:w="urn:schemas-microsoft-com:office:word"
                  xmlns="http://www.w3.org/TR/REC-html40">
            <head>
              <meta charset="utf-8"/>
              <title>$storeTitle Official Invoice - ${escapeHtml(order.orderId)}</title>
              <style>
                body { font-family: Calibri, Arial, sans-serif; color: #111827; padding: 24px; }
                .header { background-color: #065F46; color: #FFFFFF; padding: 18px; border-radius: 8px; }
                .brand-badge { display: inline-block; background: #F59E0B; color: #052E16; font-weight: 900; padding: 8px 14px; border-radius: 999px; font-size: 14px; }
                .status-box { background-color: #ECFDF5; border: 2px solid #10B981; padding: 10px 14px; margin-top: 14px; font-weight: bold; color: #065F46; }
                .sec-title { color: #065F46; font-size: 14px; font-weight: bold; margin-top: 16px; border-bottom: 1px solid #D1D5DB; padding-bottom: 4px; }
                table { width: 100%; border-collapse: collapse; margin-top: 8px; }
                td { padding: 6px 4px; font-size: 12.5px; vertical-align: top; }
                td.lbl { font-weight: bold; color: #4B5563; width: 38%; }
                .total-box { background-color: #052E16; color: #F59E0B; padding: 14px; margin-top: 16px; font-size: 16px; font-weight: 900; }
              </style>
            </head>
            <body>
              <div class="header">
                <div class="brand-badge">★ $storeTitle OFFICIAL RECIPIENT ✓</div>
                <h2 style="margin:8px 0 2px 0;">$storeTitle — OFFICIAL PURCHASE RECEIPT (WORD FORMAT)</h2>
                <p style="margin:0;font-size:12px;">Target Recipient Selected: <b>$storeTitle</b> • Authorized Commercial Invoice</p>
              </div>
              <div class="status-box">$verificationLabel<br/><span style="font-weight:normal;font-size:12px;">${escapeHtml(order.verificationNote)}</span></div>
              <div class="sec-title">ORDER &amp; PORTAL SETTLEMENT</div>
              <table>
                <tr><td class="lbl">Order Reference:</td><td>${escapeHtml(order.orderId)}</td></tr>
                <tr><td class="lbl">Date &amp; Time:</td><td>$dateStr</td></tr>
                <tr><td class="lbl">Payment Portal:</td><td><b>${escapeHtml(order.paymentMethod.title)}</b></td></tr>
                <tr><td class="lbl">Target Recipient Account:</td><td><b>${escapeHtml(order.paymentAccount)} ($storeTitle)</b></td></tr>
                <tr><td class="lbl">Transaction / Portal Ref:</td><td>${escapeHtml(order.paymentReference)}</td></tr>
                <tr><td class="lbl">Sender / Customer Account:</td><td>${escapeHtml(order.senderName)}</td></tr>
              </table>
              <div class="sec-title">CUSTOMER &amp; DELIVERY DESTINATION</div>
              <table>
                <tr><td class="lbl">Customer Name:</td><td>${escapeHtml(order.customerName)}</td></tr>
                <tr><td class="lbl">Phone Line:</td><td>${escapeHtml(order.customerPhone)}</td></tr>
                <tr><td class="lbl">Delivery Address:</td><td>${escapeHtml(order.deliveryAddress)}</td></tr>
              </table>
              <div class="sec-title">PURCHASED ITEMS</div>
              <p style="font-size:12.5px;line-height:1.5;">$cleanSummary</p>
              <div class="total-box">TOTAL PAYABLE / SETTLED AMOUNT: ${escapeHtml(naira.format(order.totalAmount))}</div>
              <p style="font-size:11px;color:#6B7280;margin-top:14px;">Generated by $storeTitle Mobile Commerce Portal. Recipient verified as $storeTitle.</p>
            </body>
            </html>
        """.trimIndent()
    }

    private fun writeInvoiceWord(
        outputStream: OutputStream,
        order: Order,
        storeProfile: StoreProfileConfig
    ) {
        val htmlDoc = buildWordHtmlContent(order, storeProfile)
        outputStream.write(htmlDoc.toByteArray(Charsets.UTF_8))
    }

    fun generateAndGetUri(
        context: Context,
        order: Order,
        storeProfile: StoreProfileConfig
    ): Uri? {
        return try {
            val dir = File(context.cacheDir, "invoices").apply { if (!exists()) mkdirs() }
            val safeId = order.orderId.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val file = File(dir, "${safeId}_invoice.doc")
            FileOutputStream(file).use { out ->
                writeInvoiceWord(out, order, storeProfile)
            }
            FileProvider.getUriForFile(
                context,
                "com.aistudio.deprosboutique.kqwmxp.fileprovider",
                file
            )
        } catch (_: Exception) {
            null
        }
    }

    fun writeToUri(
        context: Context,
        targetUri: Uri,
        order: Order,
        storeProfile: StoreProfileConfig
    ): Boolean {
        return try {
            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                writeInvoiceWord(out, order, storeProfile)
            } ?: return false
            true
        } catch (_: Exception) {
            false
        }
    }

    fun shareWordInvoice(
        context: Context,
        order: Order,
        storeProfile: StoreProfileConfig
    ) {
        val docUri = generateAndGetUri(context, order, storeProfile)
        if (docUri == null) {
            Toast.makeText(context, "Could not generate Word (.doc) invoice", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/msword"
            putExtra(Intent.EXTRA_STREAM, docUri)
            putExtra(Intent.EXTRA_SUBJECT, "${storeProfile.storeName} Official Word Invoice - ${order.orderId}")
            putExtra(Intent.EXTRA_TEXT, "Official Word format receipt for Order ${order.orderId} (${storeProfile.storeName}).")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(Intent.createChooser(intent, "Share Word Invoice (.doc)"))
        } catch (_: Exception) {
            Toast.makeText(context, "No app available to share Word invoice", Toast.LENGTH_SHORT).show()
        }
    }
}
