package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen
import com.example.util.ProductImageHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder

@Composable
fun Product3DCard(
    product: Product,
    onClick: () -> Unit,
    onAddToCart: (Product) -> Unit,
    modifier: Modifier = Modifier,
    isWishlisted: Boolean = false,
    onToggleWishlist: () -> Unit = {},
    isCatalogLocked: Boolean = false,
    isDeliveredOrdered: Boolean = false
) {
    val coroutineScope = rememberCoroutineScope()
    var rotX by remember { mutableFloatStateOf(0f) }
    var rotY by remember { mutableFloatStateOf(0f) }
    var addedFeedback by remember { mutableStateOf(false) }

    val nairaFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "NG")).apply {
            maximumFractionDigits = 0
        }
    }

    val displayPrice = product.calculatePrice()
    val originalPrice = product.basePrice

    val isExpired = product.expiryMonthsRemaining <= 0
    val isShortDated = product.expiryMonthsRemaining in 1..3
    val isLowStock = product.stockQuantity in 1..2
    val isOutOfStock = product.stockQuantity == 0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}")
            .graphicsLayer {
                rotationX = rotX
                rotationY = rotY
                cameraDistance = 16f * density
                shadowElevation = 8.dp.toPx()
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        coroutineScope.launch {
                            val animX = Animatable(rotX)
                            val animY = Animatable(rotY)
                            launch { animX.animateTo(0f, tween(400, easing = FastOutSlowInEasing)) }
                            launch { animY.animateTo(0f, tween(400, easing = FastOutSlowInEasing)) }
                            rotX = 0f
                            rotY = 0f
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        rotX = (rotX - dragAmount.y * 0.15f).coerceIn(-18f, 18f)
                        rotY = (rotY + dragAmount.x * 0.15f).coerceIn(-18f, 18f)
                    }
                )
            }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 3D Image Container with Badges
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.1f)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .then(
                        if (isExpired || isShortDated) Modifier.border(
                            BorderStroke(2.5.dp, Color(0xFFDC2626)),
                            RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                        ) else Modifier
                    )
                    .background(Color.White)
            ) {
                // Product Image with 3D offset
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
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = 1.05f
                            scaleY = 1.05f
                            translationX = rotY * 1.5f
                            translationY = -rotX * 1.5f
                        }
                )

                // 3D Light Reflection Sheen
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.15f + (rotY / 60f).coerceIn(0f, 0.2f)),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // 3D Badge on Top Left
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    modifier = Modifier
                        .padding(10.dp)
                        .align(Alignment.TopStart)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewInAr,
                            contentDescription = "3D Interactive",
                            tint = CyberCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "3D VIEW",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                // Catalog Lock or Stock/Expiry Status Badge
                if (isCatalogLocked) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF334155),
                        border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.85f)),
                        modifier = Modifier
                            .padding(10.dp)
                            .align(Alignment.BottomEnd)
                    ) {
                        Text(
                            text = "🔒 DISPLAY ONLY",
                            color = AccentGold,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
                if (isOutOfStock || isLowStock || isExpired || isShortDated) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isExpired || isShortDated || isOutOfStock) Color(0xFFDC2626) else Color(0xFFEA580C),
                        modifier = Modifier
                            .padding(10.dp)
                            .align(Alignment.BottomStart)
                    ) {
                        Text(
                            text = when {
                                isExpired -> "⛔ EXPIRED"
                                isShortDated -> "⚠️ SHORT-DATED (${product.expiryMonthsRemaining}mo)"
                                isOutOfStock -> "OUT OF STOCK"
                                else -> "LOW STOCK (${product.stockQuantity} remaining)"
                            },
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                // Top Right: Discount Badge & Wishlist Heart Button
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (product.discountPercent > 0) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.error
                        ) {
                            Text(
                                text = "24/7 SHOPPING",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Wishlist Heart Button
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onToggleWishlist)
                            .testTag("wishlist_toggle_${product.id}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isWishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (isWishlisted) "Remove from Wishlist" else "Save to Wishlist",
                                tint = if (isWishlisted) Color(0xFFEF4444) else Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }

                // Brand Pill Bottom Left
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    modifier = Modifier
                        .padding(10.dp)
                        .align(Alignment.BottomStart)
                ) {
                    Text(
                        text = product.brand,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Product Details and Add to Cart
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isExpired || isShortDated) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Rating (Active ONLY on Delivered Ordered Products)
                    if (isDeliveredOrdered) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Delivered Product Rating",
                                tint = AccentGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${product.rating} (${product.reviewCount})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = "Rate on Delivery",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    // Stock indication with remaining quantity (default <= 2)
                    Text(
                        text = when {
                            isOutOfStock -> "0 Remaining (Out of Stock)"
                            isLowStock -> "Only ${product.stockQuantity} remaining"
                            else -> "${product.stockQuantity} in stock"
                        },
                        fontSize = 11.sp,
                        color = when {
                            isOutOfStock -> Color(0xFFDC2626)
                            isLowStock -> Color(0xFFEA580C)
                            else -> SuccessGreen
                        },
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Price and Add to Cart Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = nairaFormat.format(displayPrice),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (product.discountPercent > 0) {
                            Text(
                                text = nairaFormat.format(originalPrice),
                                style = MaterialTheme.typography.bodySmall,
                                textDecoration = TextDecoration.LineThrough,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        } else if (product.variantPrices.map { it.amount }.distinct().size > 1) {
                            val minV = product.minVariantAmount() ?: displayPrice
                            val maxV = product.maxVariantAmount() ?: displayPrice
                            Text(
                                text = "${nairaFormat.format(minV)} – ${nairaFormat.format(maxV)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CyberCyan
                            )
                        }
                    }

                    // ADD TO CART ICON BUTTON (Disabled in Display Only mode)
                    FilledIconButton(
                        onClick = {
                            if (isCatalogLocked) return@FilledIconButton
                            onAddToCart(product)
                            if (!isOutOfStock) {
                                addedFeedback = true
                                coroutineScope.launch {
                                    delay(1200)
                                    addedFeedback = false
                                }
                            }
                        },
                        enabled = !isCatalogLocked,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("add_to_cart_button_${product.id}"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = when {
                                isCatalogLocked || isOutOfStock -> Color(0xFF6B7280).copy(alpha = 0.45f)
                                addedFeedback -> SuccessGreen
                                else -> MaterialTheme.colorScheme.primary
                            },
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFF475569).copy(alpha = 0.55f),
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
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
