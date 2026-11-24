package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder

/**
 * High-fidelity, 100% standards-compliant ISO/IEC 18004 QR Code View using ZXing Encoder.
 * Renders integer-aligned crisp dark/light modules with FilterQuality.None and ISO quiet zones
 * so phone cameras, QR scanners, and Google Lens decode both short and long sync URLs instantly.
 */
@Composable
fun QrCodeView(
    content: String,
    modifier: Modifier = Modifier,
    sizePx: Int = 720
) {
    val cleanContent = content.trim()
    val qrBitmap = remember(cleanContent, sizePx) {
        generateQrBitmap(cleanContent, sizePx)
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        if (qrBitmap != null) {
            Image(
                bitmap = qrBitmap.asImageBitmap(),
                contentDescription = "Official Scannable App Download QR Code",
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.None,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Generates a high-contrast, integer-aligned QR Code Bitmap ready for fast camera scanning.
 */
fun generateQrBitmap(content: String, size: Int = 720): Bitmap? {
    val cleanContent = content.trim()
    if (cleanContent.isBlank()) return null
    return try {
        val ecLevel = when {
            cleanContent.length > 220 -> ErrorCorrectionLevel.M
            cleanContent.length > 110 -> ErrorCorrectionLevel.Q
            else -> ErrorCorrectionLevel.H
        }
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val qrCode = Encoder.encode(cleanContent, ecLevel, hints)
        val matrix = qrCode.matrix
        val quietModules = 4
        val totalModulesX = matrix.width + quietModules * 2
        val totalModulesY = matrix.height + quietModules * 2
        val modulePx = (size.coerceAtLeast(240) / maxOf(totalModulesX, totalModulesY)).coerceAtLeast(4)
        val outWidth = totalModulesX * modulePx
        val outHeight = totalModulesY * modulePx
        val pixels = IntArray(outWidth * outHeight) { android.graphics.Color.WHITE }

        for (my in 0 until matrix.height) {
            val startPy = (my + quietModules) * modulePx
            for (mx in 0 until matrix.width) {
                if (matrix.get(mx, my).toInt() != 0) {
                    val startPx = (mx + quietModules) * modulePx
                    for (py in startPy until (startPy + modulePx)) {
                        val rowOffset = py * outWidth
                        for (px in startPx until (startPx + modulePx)) {
                            pixels[rowOffset + px] = android.graphics.Color.BLACK
                        }
                    }
                }
            }
        }
        Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, outWidth, 0, 0, outWidth, outHeight)
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
    title: String = "ODIYA",
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

    // Draw razor-sharp QR bitmap scaled cleanly into the card without blurring
    val targetSide = 700
    val qrLeft = (width - targetSide) / 2
    val qrTop = 266
    val nearestPaint = Paint().apply {
        isFilterBitmap = false
        isAntiAlias = false
        isDither = false
    }
    canvas.drawBitmap(
        qr,
        Rect(0, 0, qr.width, qr.height),
        Rect(qrLeft, qrTop, qrLeft + targetSide, qrTop + targetSide),
        nearestPaint
    )

    // Footer instruction & URL
    val ctaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#0F172A")
        textSize = 28f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("Install on Android, iPhone, Windows, Mac & Chromebook", width / 2f, 1058f, ctaPaint)

    val urlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#0284C7")
        textSize = 21f
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
    }
    val cleanUrl = content.trim()
    val displayUrl = if (cleanUrl.length > 62) cleanUrl.take(59) + "..." else cleanUrl
    canvas.drawText(displayUrl, width / 2f, 1106f, urlPaint)

    return poster
}
