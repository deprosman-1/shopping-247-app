package com.example.util

import android.content.Context
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.R
import java.io.File

/**
 * Helper to ensure product images always load reliably and stay fixed to their
 * assigned photo without flashing or switching to hidden fallback drawables.
 */
object ProductImageHelper {

    fun isCloudUrl(imageUrl: String?): Boolean {
        val u = imageUrl?.trim().orEmpty()
        return u.startsWith("http://", ignoreCase = true) ||
            u.startsWith("https://", ignoreCase = true)
    }

    fun isDataUri(imageUrl: String?): Boolean {
        val u = imageUrl?.trim().orEmpty()
        return u.startsWith("data:image/", ignoreCase = true)
    }

    fun isLocalOnlyUri(imageUrl: String?): Boolean {
        val u = imageUrl?.trim().orEmpty()
        return u.startsWith("content://", ignoreCase = true) ||
            u.startsWith("file://", ignoreCase = true)
    }

    /**
     * Returns true when a product's imageUrl is blank or points to an expired/unreadable
     * local content:// or missing file:// URI (e.g., Photopicker URIs from an older install).
     */
    fun isUnreadableImageUri(context: Context, imageUrl: String?): Boolean {
        val u = imageUrl?.trim().orEmpty()
        if (u.isBlank()) return true
        if (isCloudUrl(u) || isDataUri(u) || u.startsWith("android.resource://", ignoreCase = true)) return false
        if (u.startsWith("file://", ignoreCase = true)) {
            val path = Uri.parse(u).path ?: return true
            val file = File(path)
            return !file.exists() || file.length() <= 0L
        }
        if (u.startsWith("content://", ignoreCase = true)) {
            return try {
                context.contentResolver.openAssetFileDescriptor(Uri.parse(u), "r")?.use {
                    false
                } ?: true
            } catch (_: Exception) {
                true
            }
        }
        return false
    }

    private val dataUriCache = android.util.LruCache<Int, ByteArray>(32)

    fun decodeDataUriBytes(dataUri: String): ByteArray? {
        val key = dataUri.hashCode()
        dataUriCache.get(key)?.let { return it }
        return try {
            val commaIdx = dataUri.indexOf(',')
            if (commaIdx < 0) return null
            val base64Part = dataUri.substring(commaIdx + 1).trim()
            val bytes = android.util.Base64.decode(base64Part, android.util.Base64.DEFAULT)
            if (bytes != null) {
                dataUriCache.put(key, bytes)
            }
            bytes
        } catch (_: Exception) {
            null
        }
    }

    @DrawableRes
    fun getFallbackDrawableRes(category: String, title: String = ""): Int {
        val cat = category.lowercase()
        val t = title.lowercase()
        return when {
            cat.contains("shoe") || t.contains("shoe") || t.contains("sneaker") || t.contains("boot") || t.contains("kick") || t.contains("loafer") || t.contains("oxford") || t.contains("sandal") || t.contains("slide") -> {
                R.drawable.img_sneaker_quantum_3d
            }
            cat.contains("bag") || t.contains("bag") || t.contains("backpack") || t.contains("tote") || t.contains("duffle") || t.contains("crossbody") || t.contains("clutch") || t.contains("briefcase") || t.contains("suitcase") -> {
                R.drawable.img_luxury_leather_bag
            }
            cat.contains("beauty") || cat.contains("cosmetic") || t.contains("serum") || t.contains("parfum") || t.contains("lipstick") || t.contains("fragrance") || t.contains("oil") || t.contains("mask") || t.contains("lotion") || t.contains("cream") -> {
                R.drawable.img_luxury_fragrance_bottle
            }
            cat.contains("electronic") || t.contains("headphone") || t.contains("audio") || t.contains("watch") || t.contains("phone") || t.contains("laptop") || t.contains("monitor") || t.contains("keyboard") || t.contains("camera") || t.contains("speaker") -> {
                R.drawable.img_cyber_headset_audiophile
            }
            cat.contains("food") || cat.contains("beverage") || t.contains("coffee") || t.contains("tea") || t.contains("espresso") || t.contains("juice") || t.contains("water") || t.contains("drink") || t.contains("chocolate") || t.contains("nuts") || t.contains("bread") || t.contains("butter") -> {
                R.drawable.img_gourmet_espresso_can
            }
            else -> {
                R.drawable.img_leather_luxury_jacket
            }
        }
    }

    /**
     * Builds a pinned ImageRequest that loads ONLY the product's actual assigned imageUrl
     * with strong memory/disk cache keys, fast GPU downsampling for cards, and crossfade disabled.
     */
    fun buildImageRequest(
        context: Context,
        imageUrl: String?,
        category: String = "",
        title: String = "",
        highRes: Boolean = false
    ): ImageRequest {
        val cleanUrl = imageUrl?.trim().orEmpty()
        if (cleanUrl.isBlank()) {
            return ImageRequest.Builder(context)
                .data(null)
                .crossfade(false)
                .build()
        }

        val resolvedUrl = if (!highRes && cleanUrl.contains("images.unsplash.com", ignoreCase = true)) {
            cleanUrl
                .replace("w=1200", "w=480")
                .replace("q=95", "q=82")
        } else {
            cleanUrl
        }

        val resolvedData: Any = if (isDataUri(resolvedUrl)) {
            decodeDataUriBytes(resolvedUrl) ?: resolvedUrl
        } else {
            resolvedUrl
        }
        val baseKey = if (resolvedUrl.length > 160) resolvedUrl.hashCode().toString() else resolvedUrl
        val cacheKey = if (highRes) "${baseKey}#orig" else "${baseKey}#thumb480"

        return ImageRequest.Builder(context)
            .data(resolvedData)
            .memoryCacheKey(cacheKey)
            .diskCacheKey(cacheKey)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .crossfade(false)
            .allowRgb565(false)
            .apply {
                if (highRes) {
                    size(coil.size.Size.ORIGINAL)
                    precision(coil.size.Precision.EXACT)
                    allowHardware(false)
                } else {
                    size(480, 480)
                    precision(coil.size.Precision.INEXACT)
                    allowHardware(true)
                }
            }
            .build()
    }

    @Composable
    fun rememberImageRequest(
        imageUrl: String?,
        category: String = "",
        title: String = "",
        highRes: Boolean = false
    ): ImageRequest {
        val context = LocalContext.current
        val normalized = imageUrl?.trim().orEmpty()
        return remember(context, normalized, highRes) {
            buildImageRequest(
                context = context,
                imageUrl = normalized,
                category = category,
                title = title,
                highRes = highRes
            )
        }
    }

    fun preloadProductImages(
        context: Context,
        products: List<com.example.data.model.Product>,
        limit: Int = 24
    ) {
        try {
            val loader = coil.Coil.imageLoader(context)
            products.take(limit).forEach { prod ->
                val primary = prod.imageUrl.trim()
                if (primary.isNotBlank()) {
                    loader.enqueue(buildImageRequest(context, primary, prod.category, prod.title, highRes = false))
                }
                prod.galleryUrls.take(2).forEach { extra ->
                    val clean = extra.trim()
                    if (clean.isNotBlank() && clean != primary) {
                        loader.enqueue(buildImageRequest(context, clean, prod.category, prod.title, highRes = false))
                    }
                }
            }
        } catch (_: Exception) {
        }
    }
}
