package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.Order
import com.example.data.model.StoreProfileConfig
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfInvoiceExporter {

    private fun writeInvoicePdf(
        outputStream: OutputStream,
        order: Order,
        storeProfile: StoreProfileConfig
    ) {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        val naira = NumberFormat.getCurrencyInstance(Locale("en", "NG")).apply {
            maximumFractionDigits = 0
        }
        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(order.createdAt))

        val headerPaint = Paint().apply { color = Color.parseColor("#065F46") }
        canvas.drawRect(0f, 0f, 595f, 118f, headerPaint)

        val goldPaint = Paint().apply {
            color = Color.parseColor("#F59E0B")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(storeProfile.storeName.uppercase(), 36f, 44f, goldPaint)

        val subHeaderPaint = Paint().apply {
            color = Color.WHITE
            textSize = 11f
            isAntiAlias = true
        }
        canvas.drawText("OFFICIAL PURCHASE INVOICE & PAYMENT RECEIPT", 36f, 66f, subHeaderPaint)

        val storePhone = listOf(storeProfile.phoneNumber, storeProfile.secondaryPhoneNumber)
            .filter { it.isNotBlank() }.joinToString(" / ")
        val contactMeta = listOfNotNull(
            storePhone.takeIf { it.isNotBlank() }?.let { "Tel: $it" },
            storeProfile.supportEmail.takeIf { it.isNotBlank() },
            storeProfile.storeAddress.takeIf { it.isNotBlank() }
        ).joinToString("  •  ")
        if (contactMeta.isNotBlank()) {
            canvas.drawText(contactMeta.take(92), 36f, 88f, subHeaderPaint)
        }

        val labelPaint = Paint().apply {
            color = Color.parseColor("#4B5563")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val valuePaint = Paint().apply {
            color = Color.parseColor("#111827")
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        val sectionPaint = Paint().apply {
            color = Color.parseColor("#065F46")
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#D1D5DB")
            strokeWidth = 1f
        }

        var y = 152f

        fun drawRow(label: String, value: String) {
            canvas.drawText(label, 36f, y, labelPaint)
            val safeValue = value.ifBlank { "-" }
            if (safeValue.length <= 52) {
                canvas.drawText(safeValue, 210f, y, valuePaint)
                y += 22f
            } else {
                val words = safeValue.split(" ")
                var currentLine = ""
                for (w in words) {
                    val candidate = if (currentLine.isEmpty()) w else "$currentLine $w"
                    if (candidate.length > 52) {
                        canvas.drawText(currentLine, 210f, y, valuePaint)
                        y += 16f
                        currentLine = w
                    } else {
                        currentLine = candidate
                    }
                }
                if (currentLine.isNotEmpty()) {
                    canvas.drawText(currentLine, 210f, y, valuePaint)
                    y += 20f
                }
            }
        }

        canvas.drawText("ORDER & STATUS", 36f, y, sectionPaint)
        y += 18f
        drawRow("Order Reference:", order.orderId)
        drawRow("Date & Time:", dateStr)
        drawRow("Status:", order.status.label)
        if (order.verificationNote.isNotBlank()) {
            drawRow("Verification Note:", order.verificationNote)
        }

        y += 6f
        canvas.drawLine(36f, y, 559f, y, linePaint)
        y += 24f

        canvas.drawText("CUSTOMER & DELIVERY DETAILS", 36f, y, sectionPaint)
        y += 18f
        drawRow("Customer Name:", order.customerName)
        drawRow("Contact Phone:", order.customerPhone)
        drawRow("Delivery Destination:", order.deliveryAddress)

        y += 6f
        canvas.drawLine(36f, y, 559f, y, linePaint)
        y += 24f

        canvas.drawText("PAYMENT SETTLEMENT", 36f, y, sectionPaint)
        y += 18f
        drawRow("Payment Method:", order.paymentMethod.title)
        drawRow("Destination Account:", order.paymentAccount)
        drawRow("Sender Account Name:", order.senderName)
        drawRow("Session / STAN / Ref:", order.paymentReference)

        y += 6f
        canvas.drawLine(36f, y, 559f, y, linePaint)
        y += 24f

        canvas.drawText("PURCHASED ITEMS", 36f, y, sectionPaint)
        y += 20f
        val cleanSummary = order.itemsSummary.replace(Regex("""\s*\[Sold:[^\]]+\]"""), "").trim()
        for (rawLine in cleanSummary.split("\n")) {
            val trimmed = rawLine.trim()
            if (trimmed.isNotBlank() && y < 740f) {
                canvas.drawText("• ${trimmed.take(78)}", 42f, y, valuePaint)
                y += 18f
            }
        }

        y += 12f
        val totalBoxPaint = Paint().apply { color = Color.parseColor("#ECFDF5") }
        canvas.drawRect(36f, y, 559f, y + 48f, totalBoxPaint)
        val totalLabelPaint = Paint().apply {
            color = Color.parseColor("#065F46")
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val totalValuePaint = Paint().apply {
            color = Color.parseColor("#047857")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("TOTAL PAYABLE AMOUNT:", 50f, y + 30f, totalLabelPaint)
        canvas.drawText(naira.format(order.totalAmount), 395f, y + 31f, totalValuePaint)

        val footerPaint = Paint().apply {
            color = Color.parseColor("#6B7280")
            textSize = 9.5f
            isAntiAlias = true
        }
        canvas.drawText("Thank you for shopping with ${storeProfile.storeName}. Keep this official invoice for dispatch verification.", 36f, 810f, footerPaint)

        doc.finishPage(page)
        doc.writeTo(outputStream)
        doc.close()
    }

    fun generateAndGetUri(
        context: Context,
        order: Order,
        storeProfile: StoreProfileConfig
    ): Uri? {
        return try {
            val dir = File(context.cacheDir, "invoices")
            if (!dir.exists()) dir.mkdirs()
            val safeId = order.orderId.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val file = File(dir, "${safeId}_invoice.pdf")
            FileOutputStream(file).use { out ->
                writeInvoicePdf(out, order, storeProfile)
            }
            FileProvider.getUriForFile(
                context,
                "com.aistudio.deprosboutique.kqwmxp.fileprovider",
                file
            )
        } catch (e: Exception) {
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
                writeInvoicePdf(out, order, storeProfile)
            } ?: return false
            true
        } catch (_: Exception) {
            false
        }
    }

    fun sharePdfInvoice(
        context: Context,
        order: Order,
        storeProfile: StoreProfileConfig
    ) {
        val pdfUri = generateAndGetUri(context, order, storeProfile)
        if (pdfUri == null) {
            Toast.makeText(context, "Could not generate PDF invoice", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            putExtra(Intent.EXTRA_SUBJECT, "${storeProfile.storeName} Official Invoice - ${order.orderId}")
            putExtra(Intent.EXTRA_TEXT, "Official payment receipt & invoice for Order ${order.orderId} from ${storeProfile.storeName}.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(Intent.createChooser(intent, "Share PDF Invoice"))
        } catch (_: Exception) {
            Toast.makeText(context, "No app available to share PDF", Toast.LENGTH_SHORT).show()
        }
    }
}
