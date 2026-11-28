package com.example.util

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.data.model.PaymentMethod
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ParsedReceiptText(
    val detectedPaymentMethod: PaymentMethod? = null,
    val extractedReference: String = "",
    val extractedSender: String = "",
    val extractedAmount: String = "",
    val sourceAppLabel: String = ""
)

data class PendingSharedReceipt(
    val receiptUri: String,
    val detectedPaymentMethod: PaymentMethod? = null,
    val extractedReference: String = "",
    val extractedSender: String = "",
    val rawTextSummary: String = "",
    val sourceAppLabel: String = "",
    val receivedAtMillis: Long = System.currentTimeMillis()
)

data class ClipboardReceiptCapture(
    val fingerprint: String,
    val pendingReceipt: PendingSharedReceipt
)

data class ExtractedStoreSyncCredentials(
    val rawText: String,
    val supabaseUrl: String,
    val supabaseKey: String
)

object SharedReceiptHelper {

    fun parseReceiptShareText(rawText: String, subject: String = ""): ParsedReceiptText {
        val combined = listOf(subject, rawText)
            .filter { it.isNotBlank() }
            .joinToString("\n")
            .trim()
        if (combined.isBlank()) {
            return ParsedReceiptText()
        }

        val lower = combined.lowercase()
        val detectedMethod = when {
            lower.contains("paystack") -> PaymentMethod.PAYSTACK
            lower.contains("opay") || lower.contains("paycom") -> PaymentMethod.OPAY
            lower.contains("moniepoint") || lower.contains("monie") -> PaymentMethod.MONIEPOINT
            lower.contains("palmpay") || lower.contains("palm pay") -> PaymentMethod.PALMPAY
            lower.contains("pos terminal") || lower.contains("pos transfer") -> PaymentMethod.POS_TRANSFER
            else -> null
        }
        val sourceLabel = when (detectedMethod) {
            PaymentMethod.PAYSTACK -> "Paystack"
            PaymentMethod.OPAY -> "OPay"
            PaymentMethod.MONIEPOINT -> "Moniepoint"
            PaymentMethod.PALMPAY -> "PalmPay"
            PaymentMethod.POS_TRANSFER -> "POS Receipt"
            else -> "Payment App"
        }

        val refPatterns = listOf(
            Regex("""(?:session\s*id|transaction\s*(?:ref(?:erence)?|id|no|number)|trans\.?\s*id|reference\s*(?:id|no|number)?|payment\s*ref(?:erence)?|trxref|stan|rrn|trx\s*id)\s*[:#\-]?\s*([A-Za-z0-9\-_]{5,48})""", RegexOption.IGNORE_CASE),
            Regex("""\bref\s*[:#\-]\s*([A-Za-z0-9\-_]{5,48})""", RegexOption.IGNORE_CASE),
            Regex("""\b((?:PSK|ODIYA)[_-][A-Za-z0-9\-_]{4,40})\b""", RegexOption.IGNORE_CASE)
        )
        val extractedRef = refPatterns
            .asSequence()
            .mapNotNull { it.find(combined)?.groupValues?.getOrNull(1)?.trim() }
            .firstOrNull { it.isNotBlank() }
            ?: ""

        val senderPatterns = listOf(
            Regex("""(?:sender(?:\s*name)?|payer(?:\s*name)?|paid\s*by|account\s*holder|source\s*account|from)\s*[:\-]\s*([A-Za-z][A-Za-z0-9 .,'\-]{2,60})""", RegexOption.IGNORE_CASE)
        )
        val extractedSender = senderPatterns
            .asSequence()
            .mapNotNull { it.find(combined)?.groupValues?.getOrNull(1)?.lineSequence()?.firstOrNull()?.trim() }
            .firstOrNull { candidate ->
                candidate.isNotBlank() &&
                    !candidate.equals("OPay", ignoreCase = true) &&
                    !candidate.equals("Moniepoint", ignoreCase = true) &&
                    !candidate.equals("PalmPay", ignoreCase = true) &&
                    !candidate.equals("Paystack", ignoreCase = true)
            }
            ?: ""

        val amountMatch = Regex("""(?:₦|NGN\s*|Amount\s*[:\-]?\s*₦?\s*)([0-9][0-9,]*(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
            .find(combined)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            .orEmpty()

        return ParsedReceiptText(
            detectedPaymentMethod = detectedMethod,
            extractedReference = extractedRef,
            extractedSender = extractedSender,
            extractedAmount = amountMatch,
            sourceAppLabel = sourceLabel
        )
    }

    fun isLikelyMerchantReceiptText(
        rawText: String,
        subject: String = "",
        hasActiveMerchantSession: Boolean = false
    ): Boolean {
        val combined = listOf(subject, rawText).filter { it.isNotBlank() }.joinToString("\n").trim()
        if (combined.length < 6) return false

        // Never treat store sync links or pure account/amount numbers copied by ODIYA as a receipt
        val lower = combined.lowercase()
        if (lower.contains("sb_url=") || lower.contains("sb_key=")) return false
        if (Regex("""^\d{8,12}$""").matches(combined) || Regex("""^\d+(\.\d{1,2})?$""").matches(combined)) {
            return false
        }

        val parsed = parseReceiptShareText(rawText, subject)
        if (parsed.extractedReference.isNotBlank() && parsed.detectedPaymentMethod != null) return true
        if (parsed.extractedReference.isNotBlank() && parsed.extractedSender.isNotBlank()) return true

        val receiptSignals = listOf(
            "receipt", "transaction", "session id", "transfer", "successful", "success",
            "beneficiary", "sender", "payer", "amount", "ngn", "₦", "credited", "debited",
            "reference", "stan", "rrn", "opay", "moniepoint", "palmpay", "paystack", "odiya"
        ).count { lower.contains(it) }

        if (receiptSignals >= 2) return true
        if (hasActiveMerchantSession && (parsed.extractedReference.isNotBlank() || receiptSignals >= 1)) {
            return true
        }
        return false
    }

    fun renderTextReceiptToImageFile(
        context: Context,
        parsed: ParsedReceiptText,
        rawText: String,
        fallbackMethodTitle: String = "Merchant Bank"
    ): Uri? {
        return try {
            val width = 1080
            val height = 1360
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.parseColor("#F8FAFC"))

            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#064E3B")
            }
            canvas.drawRect(0f, 0f, width.toFloat(), 230f, headerPaint)

            val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 46f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("ODIYA VERIFIED PAYMENT RECEIPT", 64f, 102f, titlePaint)

            val methodName = parsed.detectedPaymentMethod?.title
                ?: parsed.sourceAppLabel.takeIf { it.isNotBlank() && it != "Payment App" }
                ?: fallbackMethodTitle
            val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FDE047")
                textSize = 34f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("Source: $methodName", 64f, 165f, subtitlePaint)

            val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            val datePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#CBD5E1")
                textSize = 26f
            }
            canvas.drawText("Captured: $dateStr", 64f, 206f, datePaint)

            val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
            }
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#10B981")
                style = Paint.Style.STROKE
                strokeWidth = 4f
            }
            val cardRect = RectF(48f, 268f, (width - 48).toFloat(), (height - 64).toFloat())
            canvas.drawRoundRect(cardRect, 28f, 28f, cardPaint)
            canvas.drawRoundRect(cardRect, 28f, 28f, borderPaint)

            var y = 338f
            val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F172A")
                textSize = 31f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#334155")
                textSize = 29f
            }

            if (parsed.extractedAmount.isNotBlank()) {
                canvas.drawText("Amount: ₦${parsed.extractedAmount}", 84f, y, labelPaint)
                y += 52f
            }
            if (parsed.extractedSender.isNotBlank()) {
                canvas.drawText("Sender: ${parsed.extractedSender}", 84f, y, labelPaint)
                y += 52f
            }
            if (parsed.extractedReference.isNotBlank()) {
                canvas.drawText("Reference / Session ID: ${parsed.extractedReference}", 84f, y, labelPaint)
                y += 56f
            }

            val dividerPaint = Paint().apply {
                color = Color.parseColor("#E2E8F0")
                strokeWidth = 3f
            }
            canvas.drawLine(84f, y, (width - 84).toFloat(), y, dividerPaint)
            y += 40f

            canvas.drawText("Merchant Receipt Details:", 84f, y, labelPaint)
            y += 32f

            val cleanBody = rawText.trim().ifBlank { "Payment Receipt Attached ($methodName)" }.take(1400)
            val textWidth = width - 168
            val staticLayout = StaticLayout.Builder
                .obtain(cleanBody, 0, cleanBody.length, bodyPaint, textWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(6f, 1.15f)
                .build()

            canvas.save()
            canvas.translate(84f, y)
            staticLayout.draw(canvas)
            canvas.restore()

            val dir = File(context.filesDir, "shared_receipts").apply { mkdirs() }
            val outFile = File(dir, "receipt_auto_${System.currentTimeMillis()}.jpg")
            FileOutputStream(outFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos)
            }
            bitmap.recycle()
            if (outFile.exists() && outFile.length() > 0L) Uri.fromFile(outFile) else null
        } catch (_: Exception) {
            null
        }
    }

    fun tryCaptureReceiptFromClipboard(
        context: Context,
        defaultMethod: PaymentMethod? = null,
        hasActiveMerchantSession: Boolean = false
    ): ClipboardReceiptCapture? {
        return try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: return null
            val clip = cm.primaryClip ?: return null
            if (clip.itemCount <= 0) return null

            val label = clip.description?.label?.toString().orEmpty()
            // Skip ODIYA's own copied account numbers & payable amounts
            if (label.startsWith("ODIYA ", ignoreCase = true)) {
                return null
            }

            val mimeHint = if ((clip.description?.mimeTypeCount ?: 0) > 0) {
                clip.description?.getMimeType(0)
            } else {
                null
            }

            for (i in 0 until clip.itemCount) {
                val item = clip.getItemAt(i) ?: continue
                val uri = item.uri
                if (uri != null) {
                    val uriMime = (mimeHint ?: context.contentResolver.getType(uri)).orEmpty().lowercase()
                    val isReceiptAsset = uriMime.startsWith("image/") ||
                        uriMime.contains("pdf") ||
                        uri.toString().endsWith(".png", ignoreCase = true) ||
                        uri.toString().endsWith(".jpg", ignoreCase = true) ||
                        uri.toString().endsWith(".jpeg", ignoreCase = true) ||
                        uri.toString().endsWith(".pdf", ignoreCase = true) ||
                        uriMime == "application/octet-stream"
                    if (isReceiptAsset) {
                        val savedUri = persistSharedReceiptToAppStorage(
                            context = context,
                            sourceUri = uri,
                            mimeTypeHint = uriMime.ifBlank { mimeHint }
                        )
                        if (savedUri != null) {
                            val itemText = item.text?.toString().orEmpty()
                            val parsed = parseReceiptShareText(itemText, label)
                            val effectiveMethod = parsed.detectedPaymentMethod ?: defaultMethod
                            val pending = PendingSharedReceipt(
                                receiptUri = savedUri.toString(),
                                detectedPaymentMethod = effectiveMethod,
                                extractedReference = parsed.extractedReference,
                                extractedSender = parsed.extractedSender,
                                rawTextSummary = itemText.trim(),
                                sourceAppLabel = effectiveMethod?.title ?: parsed.sourceAppLabel.ifBlank { "Merchant Receipt" },
                                receivedAtMillis = System.currentTimeMillis()
                            )
                            return ClipboardReceiptCapture(
                                fingerprint = "uri:${uri}:${clip.description?.timestamp ?: 0L}",
                                pendingReceipt = pending
                            )
                        }
                    }
                }

                val text = (item.text ?: item.coerceToText(context))?.toString()?.trim().orEmpty()
                if (text.isNotBlank() && isLikelyMerchantReceiptText(text, label, hasActiveMerchantSession)) {
                    val parsed = parseReceiptShareText(text, label)
                    val effectiveMethod = parsed.detectedPaymentMethod ?: defaultMethod
                    val fallbackLabel = effectiveMethod?.title ?: parsed.sourceAppLabel.ifBlank { "Merchant Bank" }
                    val generatedUri = renderTextReceiptToImageFile(
                        context = context,
                        parsed = parsed,
                        rawText = text,
                        fallbackMethodTitle = fallbackLabel
                    )
                    if (generatedUri != null) {
                        val pending = PendingSharedReceipt(
                            receiptUri = generatedUri.toString(),
                            detectedPaymentMethod = effectiveMethod,
                            extractedReference = parsed.extractedReference,
                            extractedSender = parsed.extractedSender,
                            rawTextSummary = text,
                            sourceAppLabel = fallbackLabel,
                            receivedAtMillis = System.currentTimeMillis()
                        )
                        return ClipboardReceiptCapture(
                            fingerprint = "txt:${text.hashCode()}:${text.length}",
                            pendingReceipt = pending
                        )
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    // Extracts Supabase Store Sync URL + Anon/Publishable Key from deep links, web links,
    // or full WhatsApp Store Invite messages (either query params or plain Supabase URL + key).
    fun extractStoreSyncCredentials(rawInput: String): ExtractedStoreSyncCredentials? {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) return null

        var parsedUrl = ""
        var parsedKey = ""

        if (trimmed.contains("sb_url=") || trimmed.contains("sb_key=")) {
            try {
                val embeddedLink = Regex("""(https?://|depros://|deprosboutique://)[^\s<>"']+""", RegexOption.IGNORE_CASE)
                    .findAll(trimmed)
                    .map { it.value.trimEnd('.', ',', ';', ')', ']', '*', '_') }
                    .firstOrNull { it.contains("sb_url=") || it.contains("sb_key=") }
                val candidateUri = when {
                    embeddedLink != null -> Uri.parse(embeddedLink)
                    trimmed.startsWith("sb_url=", ignoreCase = true) || trimmed.startsWith("sb_key=", ignoreCase = true) ->
                        Uri.parse("depros://connect?$trimmed")
                    else -> Uri.parse(trimmed)
                }
                parsedUrl = candidateUri.getQueryParameter("sb_url")?.trim().orEmpty()
                parsedKey = candidateUri.getQueryParameter("sb_key")?.trim().orEmpty()
            } catch (_: Exception) {
            }
        }

        if (parsedUrl.isBlank()) {
            parsedUrl = Regex("""https?://[a-zA-Z0-9-]+\.supabase\.co\b""", RegexOption.IGNORE_CASE)
                .find(trimmed)?.value?.trim().orEmpty()
        }

        if (parsedKey.isBlank()) {
            val jwtMatch = Regex("""\beyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\b""")
                .find(trimmed)?.value?.trim()
            val pubKeyMatch = Regex("""\bsb_publishable_[A-Za-z0-9_-]{12,}\b""")
                .find(trimmed)?.value?.trim()
            parsedKey = jwtMatch ?: pubKeyMatch.orEmpty()
        }

        if (parsedUrl.isBlank() || parsedKey.isBlank()) return null
        return ExtractedStoreSyncCredentials(
            rawText = trimmed,
            supabaseUrl = parsedUrl,
            supabaseKey = parsedKey
        )
    }

    // Immediately copies a transiently shared content URI (e.g. from OPay / Moniepoint Share Sheet)
    // into app-internal filesDir/shared_receipts so it remains readable across recompositions and uploads.
    // If the shared file is a PDF invoice, renders page 1 to a crisp JPEG image automatically.
    fun persistSharedReceiptToAppStorage(
        context: Context,
        sourceUri: Uri,
        mimeTypeHint: String? = null
    ): Uri? {
        return try {
            val resolver = context.contentResolver
            val resolvedMime = (mimeTypeHint ?: resolver.getType(sourceUri) ?: "").lowercase()
            val bytes = resolver.openInputStream(sourceUri)?.use { it.readBytes() } ?: return null
            if (bytes.isEmpty()) return null

            val isPdf = resolvedMime.contains("pdf") ||
                sourceUri.toString().endsWith(".pdf", ignoreCase = true) ||
                (bytes.size >= 4 &&
                    bytes[0] == '%'.code.toByte() &&
                    bytes[1] == 'P'.code.toByte() &&
                    bytes[2] == 'D'.code.toByte() &&
                    bytes[3] == 'F'.code.toByte())

            val dir = File(context.filesDir, "shared_receipts").apply { mkdirs() }
            val outFile = File(dir, "receipt_${System.currentTimeMillis()}.jpg")

            if (isPdf) {
                val tempPdf = File(context.cacheDir, "shared_invoice_${System.currentTimeMillis()}.pdf")
                tempPdf.writeBytes(bytes)
                try {
                    val pfd = ParcelFileDescriptor.open(tempPdf, ParcelFileDescriptor.MODE_READ_ONLY)
                    pfd.use { fd ->
                        PdfRenderer(fd).use { renderer ->
                            if (renderer.pageCount <= 0) return null
                            renderer.openPage(0).use { page ->
                                val targetWidth = 1080
                                val scale = targetWidth.toFloat() / page.width.coerceAtLeast(1)
                                val targetHeight = (page.height * scale).toInt().coerceIn(400, 2400)
                                val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                                Canvas(bitmap).drawColor(Color.WHITE)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                FileOutputStream(outFile).use { fos ->
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos)
                                }
                                bitmap.recycle()
                            }
                        }
                    }
                } finally {
                    tempPdf.delete()
                }
            } else {
                FileOutputStream(outFile).use { fos ->
                    fos.write(bytes)
                }
            }

            if (outFile.exists() && outFile.length() > 0L) Uri.fromFile(outFile) else null
        } catch (_: Exception) {
            null
        }
    }
}
