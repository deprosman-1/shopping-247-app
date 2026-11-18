package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * High-fidelity, 100% standards-compliant ISO/IEC 18004 QR Code View using ZXing.
 * Generates an authentic scannable QR code that any Android camera, QR scanner,
 * or desktop webcam can scan immediately.
 */
@Composable
fun QrCodeView(
    content: String,
    modifier: Modifier = Modifier,
    sizePx: Int = 720
) {
    val qrBitmap = remember(content, sizePx) {
        generateQrBitmap(content, sizePx)
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (qrBitmap != null) {
            Image(
                bitmap = qrBitmap.asImageBitmap(),
                contentDescription = "Official Scannable App Download QR Code",
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Generates a high-contrast, error-corrected QR Code Bitmap ready for scanning.
 */
fun generateQrBitmap(content: String, size: Int = 720): Bitmap? {
    if (content.isBlank()) return null
    return try {
        val hints = mapOf(
            EncodeHintType.MARGIN to 2,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val bitMatrix = QRCodeWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            size,
            size,
            hints
        )
        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            val offset = y * size
            for (x in 0 until size) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) {
                    android.graphics.Color.BLACK
                } else {
                    android.graphics.Color.WHITE
                }
            }
        }
        Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, size, 0, 0, size, size)
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * Generates a high-resolution shareable/printable QR Code Poster PNG so users can send
 * or print the scannable QR code on WhatsApp, flyers, desktops, or Android devices.
 */
fun generateShareableQrPosterBitmap(
    content: String,
    title: String = "24/7 SHOPPING",
    subtitle: String = "Scan with Phone Camera or QR Scanner"
): Bitmap? {
    val qr = generateQrBitmap(content, 720) ?: return null
    val width = 960
    val height = 1160
    val poster = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(poster)

    // Crisp white poster background
    canvas.drawColor(android.graphics.Color.WHITE)

    // Header banner
    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#0F172A")
    }
    canvas.drawRoundRect(RectF(36f, 36f, width - 36f, 200f), 28f, 28f, headerPaint)

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 52f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(title, width / 2f, 114f, titlePaint)

    val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#38BDF8")
        textSize = 28f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(subtitle, width / 2f, 166f, subPaint)

    // QR border card
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#10B981")
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    canvas.drawRoundRect(RectF(96f, 232f, width - 96f, 1000f), 32f, 32f, borderPaint)

    // Draw QR bitmap centered
    val qrLeft = (width - qr.width) / 2f
    canvas.drawBitmap(qr, qrLeft, 256f, null)

    // Footer instruction & URL
    val ctaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#0F172A")
        textSize = 28f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("Install on Android, Windows, Mac & Chromebook", width / 2f, 1058f, ctaPaint)

    val urlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#0284C7")
        textSize = 21f
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
    }
    val displayUrl = if (content.length > 62) content.take(59) + "..." else content
    canvas.drawText(displayUrl, width / 2f, 1106f, urlPaint)

    return poster
}
