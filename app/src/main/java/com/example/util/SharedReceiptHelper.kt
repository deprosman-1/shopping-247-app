package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.example.data.model.PaymentMethod
import java.io.File
import java.io.FileOutputStream

data class ParsedReceiptText(
    val detectedPaymentMethod: PaymentMethod? = null,
    val extractedReference: String = "",
    val extractedSender: String = "",
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
            Regex("""(?:session\s*id|transaction\s*ref(?:erence)?|reference\s*(?:id|no|number)?|payment\s*ref(?:erence)?|stan|trx\s*id)\s*[:#\-]?\s*([A-Za-z0-9\-_]{5,48})""", RegexOption.IGNORE_CASE),
            Regex("""\bref\s*[:#\-]\s*([A-Za-z0-9\-_]{5,48})""", RegexOption.IGNORE_CASE)
        )
        val extractedRef = refPatterns
            .asSequence()
            .mapNotNull { it.find(combined)?.groupValues?.getOrNull(1)?.trim() }
            .firstOrNull { it.isNotBlank() }
            ?: ""

        val senderPatterns = listOf(
            Regex("""(?:sender(?:\s*name)?|payer(?:\s*name)?|paid\s*by|account\s*holder|from)\s*[:\-]\s*([A-Za-z][A-Za-z0-9 .,'\-]{2,60})""", RegexOption.IGNORE_CASE)
        )
        val extractedSender = senderPatterns
            .asSequence()
            .mapNotNull { it.find(combined)?.groupValues?.getOrNull(1)?.lineSequence()?.firstOrNull()?.trim() }
            .firstOrNull { candidate ->
                candidate.isNotBlank() &&
                    !candidate.equals("OPay", ignoreCase = true) &&
                    !candidate.equals("Moniepoint", ignoreCase = true) &&
                    !candidate.equals("PalmPay", ignoreCase = true)
            }
            ?: ""

        return ParsedReceiptText(
            detectedPaymentMethod = detectedMethod,
            extractedReference = extractedRef,
            extractedSender = extractedSender,
            sourceAppLabel = sourceLabel
        )
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
