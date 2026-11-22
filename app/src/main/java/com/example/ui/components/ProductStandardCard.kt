package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Product
import com.example.ui.theme.AccentGold
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.SuccessGreen
import com.example.util.ProductImageHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

private val sharedCardNairaFormatter: ThreadLocal<NumberFormat> = object : ThreadLocal<NumberFormat>() {
    override fun initialValue(): NumberFormat =
        NumberFormat.getCurrencyInstance(Locale("en", "NG")).apply {
            maximumFractionDigits = 0
        }
}

private fun formatNairaCompact(amount: Double): String {
    return (sharedCardNairaFormatter.get() ?: NumberFormat.getCurrencyInstance(Locale("en", "NG")).apply {
        maximumFractionDigits = 0
    }).format(amount)
}

private val CardShape16 = RoundedCornerShape(16.dp)
private val ImageTopShape16 = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
private val PanelBottomShape16 = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
private val ExpiredGradientBrush = Brush.horizontalGradient(listOf(Color(0xFFDC2626), Color(0xFFEF4444)))
private val PromoBorderBrush = Brush.horizontalGradient(listOf(Color(0xFFDC2626), Color(0xFFF59E0B), Color(0xFFEF4444)))
private val PromoBadgeBrush = Brush.horizontalGradient(listOf(Color(0xFFB91C1C), Color(0xFFDC2626), Color(0xFFF59E0B)))
private val PriceTagBrush = Brush.horizontalGradient(listOf(Color(0xFFDC2626), Color(0xFFF59E0B)))

/**
 * Clean catalog card featuring a distinct product image box and a small rectangular panel
 * placed directly below the image containing all product details.
 */
@Composable
fun ProductStandardCard(
    product: Product,
    onClick: () -> Unit,
    onAddToCart: (Product) -> Unit,
    modifier: Modifier = Modifier,
    isWishlisted: Boolean = false,
    promoDiscountPercent: Int = 0,
    onToggleWishlist: () -> Unit = {},
    isRestockWatched: Boolean = false,
    onToggleRestockWatch: (() -> Unit)? = null,
    isCatalogLocked: Boolean = false,
    isDeliveredOrdered: Boolean = false
) {
    val coroutineScope = rememberCoroutineScope()
    var addedFeedback by remember(product.id) { mutableStateOf(false) }

    val effectiveDiscount = if (promoDiscountPercent > 0) promoDiscountPercent else product.discountPercent
    val displayPrice = remember(product.id, product.basePrice, product.brand, product.availableSizes, product.variantPrices, effectiveDiscount) {
        product.calculatePrice(promoDiscountPercent = effectiveDiscount)
    }
    val hasDistinctVariantPrices = remember(product.variantPrices) {
        product.variantPrices.map { it.amount }.distinct().size > 1
    }
    val formattedDisplayPrice = remember(displayPrice) { formatNairaCompact(displayPrice) }
    val formattedOriginalPrice = remember(product.basePrice) { formatNairaCompact(product.basePrice) }
    val parsedPreviewColors = remember(product.availableColors) {
        product.availableColors.take(3).map { c ->
            try {
                Color(android.graphics.Color.parseColor(c.hex))
            } catch (_: Exception) {
                Color.Gray
            }
        }
    }
    val sizesPreviewText = remember(product.availableSizes) {
        "Sizes: ${product.availableSizes.take(3).joinToString("/")}"
    }

    val isExpired = product.expiryMonthsRemaining <= 0
    val isShortDated = product.expiryMonthsRemaining in 1..3
    val isLowStock = product.stockQuantity in 1..2
    val isOutOfStock = product.stockQuantity == 0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("standard_product_card_${product.id}")
            .clickable(onClick = onClick),
        shape = CardShape16,
        colors = CardDefaults.cardColors(
            containerColor = if (isExpired || isShortDated) Color(0xFFFEF2F2) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(
            width = if (isExpired || isShortDated) 2.dp else if (effectiveDiscount > 0) 1.5.dp else 1.dp,
            brush = if (isExpired || isShortDated)
                ExpiredGradientBrush
            else if (effectiveDiscount > 0)
                PromoBorderBrush
            else
                Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)))
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isExpired || isShortDated || effectiveDiscount > 0) 4.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // TOP: Product Image Box - Clean crystal-clear white backdrop (no green tint), Red border if expired/short-dated
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.15f)
                    .clip(ImageTopShape16)
                    .background(Color.White)
                    .then(
                        if (isExpired || isShortDated) Modifier.border(
                            BorderStroke(2.5.dp, Color(0xFFDC2626)),
                            ImageTopShape16
                        ) else Modifier
                    )
            ) {
                val pinnedImageRequest = ProductImageHelper.rememberImageRequest(
                    imageUrl = product.imageUrl,
                    category = product.category,
                    title = product.title
                )
                AsyncImage(
                    model = pinnedImageRequest,
                    contentDescription = product.title,
                    contentScale = ContentScale.Crop,
                    filterQuality = androidx.compose.ui.graphics.FilterQuality.High,
                    modifier = Modifier.fillMaxSize()
                )

                // EXPIRED OR SHORT-DATED RED ALERT BADGE OVER IMAGE
                if (isExpired || isShortDated) {
                    Surface(
                        shape = RoundedCornerShape(bottomEnd = 10.dp, topStart = 16.dp),
                        color = Color(0xFFDC2626),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = if (isExpired) "⛔ EXPIRED PRODUCT" else "⚠️ SHORT-DATED (${product.expiryMonthsRemaining}mo)",
                            color = Color.White,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                } else if (isLowStock || isOutOfStock) {
                    Surface(
                        shape = RoundedCornerShape(bottomEnd = 10.dp, topStart = 16.dp),
                        color = if (isOutOfStock) Color(0xFF7F1D1D) else Color(0xFFEA580C),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = if (isOutOfStock) "OUT OF STOCK" else "LOW STOCK (${product.stockQuantity} left)",
                            color = Color.White,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                // DISPLAY ONLY (HOSTING PERIOD ENDED / CATALOG LOCKED BY ADMIN) BADGE
                if (isCatalogLocked) {
                    Surface(
                        shape = RoundedCornerShape(bottomEnd = 10.dp, topStart = 16.dp),
                        color = Color(0xFF334155),
                        border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.85f)),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .testTag("card_display_only_badge_${product.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Display Only",
                                tint = AccentGold,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "DISPLAY ONLY",
                                color = Color.White,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                } else if (effectiveDiscount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .clip(RoundedCornerShape(bottomEnd = 14.dp, topStart = 16.dp))
                            .background(PromoBadgeBrush)
                            .border(
                                BorderStroke(1.dp, Color(0xFFFDE047)),
                                RoundedCornerShape(bottomEnd = 14.dp, topStart = 16.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("promo_special_mark_${product.id}")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color(0xFFFDE047),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "24/7 SHOPPING",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.5.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                if (product.allImageUrls.size > 1) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "📷 ${product.allImageUrls.size}",
                            color = Color.White,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // BOTTOM: PRODUCT INFORMATION PANEL LOCATED DIRECTLY BELOW IMAGE
            Surface(
                shape = PanelBottomShape16,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                ) {
                    // Category & Wishlist Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = product.category.uppercase(),
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when {
                                    isCatalogLocked -> Icons.Default.Lock
                                    addedFeedback -> Icons.Default.Check
                                    else -> Icons.Default.AddShoppingCart
                                },
                                contentDescription = when {
                                    isCatalogLocked -> "Display Only"
                                    isOutOfStock -> "Out of Stock"
                                    else -> "Add to Cart"
                                },
                                tint = when {
                                    isCatalogLocked || isOutOfStock -> Color(0xFF9CA3AF).copy(alpha = 0.65f)
                                    addedFeedback -> SuccessGreen
                                    else -> MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier
                                    .size(18.dp)
                                    .testTag("card_add_to_cart_icon_${product.id}")
                                    .clickable(enabled = !isCatalogLocked) {
                                        onAddToCart(product)
                                        if (!isOutOfStock) {
                                            addedFeedback = true
                                            coroutineScope.launch {
                                                delay(1000)
                                                addedFeedback = false
                                            }
                                        }
                                    }
                            )

                            Icon(
                                imageVector = if (isWishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Wishlist",
                                tint = if (isWishlisted) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(18.dp)
                                    .testTag("card_wishlist_icon_${product.id}")
                                    .clickable(onClick = onToggleWishlist)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Title - Displayed in RED if Expired or Short-Dated
                    Text(
                        text = product.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isExpired || isShortDated) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface
                    )

                    // Stock Quantity Indicator (Default <=2 is Low Stock)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = when {
                                isCatalogLocked -> Color(0xFF475569).copy(alpha = 0.16f)
                                isOutOfStock -> Color(0xFFDC2626).copy(alpha = 0.15f)
                                isLowStock -> Color(0xFFEA580C).copy(alpha = 0.15f)
                                else -> SuccessGreen.copy(alpha = 0.12f)
                            }
                        ) {
                            Text(
                                text = when {
                                    isCatalogLocked -> "Display Only (Locked)"
                                    isOutOfStock -> "Out of Stock (0 remaining)"
                                    isLowStock -> "Low Stock: Only ${product.stockQuantity} remaining"
                                    else -> "${product.stockQuantity} in stock"
                                },
                                color = when {
                                    isCatalogLocked -> Color(0xFF94A3B8)
                                    isOutOfStock -> Color(0xFFDC2626)
                                    isLowStock -> Color(0xFFEA580C)
                                    else -> SuccessGreen
                                },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }

                        if (isExpired) {
                            Text("EXPIRED", color = Color(0xFFDC2626), fontSize = 8.5.sp, fontWeight = FontWeight.Black)
                        } else if (isShortDated) {
                            Text("${product.expiryMonthsRemaining}mo left", color = Color(0xFFDC2626), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (isOutOfStock && onToggleRestockWatch != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            onClick = onToggleRestockWatch,
                            shape = RoundedCornerShape(8.dp),
                            color = if (isRestockWatched) SuccessGreen.copy(alpha = 0.16f) else CyberCyan.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, if (isRestockWatched) SuccessGreen else CyberCyan.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("restock_notify_button_${product.id}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = if (isRestockWatched) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                                    contentDescription = "Back in stock alert",
                                    tint = if (isRestockWatched) SuccessGreen else CyberCyan,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isRestockWatched) "Alert Set ✓" else "Notify When In Stock",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isRestockWatched) SuccessGreen else CyberCyan,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // Brand & Rating Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Brand: ${product.brand}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyan,
                            maxLines = 1,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (isDeliveredOrdered) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Delivered Product Rating",
                                    tint = AccentGold,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "${product.rating}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Text(
                                text = "Delivered to Rate",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // Sizes & Colors Compact Preview Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = sizesPreviewText,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Color dots preview
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            parsedPreviewColors.forEach { dotColor ->
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                        .border(0.5.dp, Color.Black.copy(alpha = 0.2f), CircleShape)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Price & Action Button Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = formattedDisplayPrice,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.5.sp,
                                    color = if (effectiveDiscount > 0) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary
                                )
                                if (effectiveDiscount > 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(PriceTagBrush)
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.LocalFireDepartment,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(9.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = "24/7 SHOPPING",
                                                color = Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 8.5.sp
                                            )
                                        }
                                    }
                                }
                            }
                            if (effectiveDiscount > 0) {
                                Text(
                                    text = formattedOriginalPrice,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    textDecoration = TextDecoration.LineThrough,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            } else if (hasDistinctVariantPrices) {
                                val minV = product.minVariantAmount() ?: displayPrice
                                val maxV = product.maxVariantAmount() ?: displayPrice
                                Text(
                                    text = "${formatNairaCompact(minV)}–${formatNairaCompact(maxV)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CyberCyan
                                )
                            }
                        }

                        // Add to Cart Button (Disabled in Display Only mode)
                        FilledIconButton(
                            onClick = {
                                if (isCatalogLocked) return@FilledIconButton
                                onAddToCart(product)
                                if (!isOutOfStock) {
                                    addedFeedback = true
                                    coroutineScope.launch {
                                        delay(1000)
                                        addedFeedback = false
                                    }
                                }
                            },
                            enabled = !isCatalogLocked,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("card_add_button_${product.id}"),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = when {
                                    isCatalogLocked || isOutOfStock -> Color(0xFF6B7280).copy(alpha = 0.45f)
                                    addedFeedback -> SuccessGreen
                                    else -> MaterialTheme.colorScheme.primary
                                },
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFF475569).copy(alpha = 0.5f),
                                disabledContentColor = Color.White.copy(alpha = 0.75f)
                            )
                        ) {
                            Icon(
                                imageVector = when {
                                    isCatalogLocked -> Icons.Default.Lock
                                    addedFeedback -> Icons.Default.Check
                                    else -> Icons.Default.AddShoppingCart
                                },
                                contentDescription = when {
                                    isCatalogLocked -> "Display Only"
                                    isOutOfStock -> "Out of Stock"
                                    else -> "Add to Cart"
                                },
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
