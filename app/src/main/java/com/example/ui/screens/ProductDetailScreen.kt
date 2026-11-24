package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import android.content.Intent
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import com.example.ui.components.DEPROS_DOWNLOAD_URL
import androidx.compose.material3.Badge
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import com.example.data.model.AdminBargainConfig
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import coil.request.ImageRequest
import coil.size.Precision
import com.example.AppScreen
import com.example.MainViewModel
import com.example.data.model.ProductColor
import com.example.ui.components.DualAxisScrollContainer
import com.example.ui.theme.AccentGold
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.SuccessGreen
import com.example.util.ProductImageHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProductDetailScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val product = viewModel.selectedProduct.collectAsState().value
    val selectedBrand by viewModel.detailBrand.collectAsState()
    val selectedSize by viewModel.detailSize.collectAsState()
    val selectedColor by viewModel.detailColor.collectAsState()
    val cartCount by viewModel.cartTotalCount.collectAsState()
    val storeProfile by viewModel.storeProfile.collectAsState()
    val isWideDualAxisMode by viewModel.isWideDualAxisMode.collectAsState()
    val lockedPartnerIds by viewModel.lockedPartnerIds.collectAsState()
    val allOrders by viewModel.allOrders.collectAsState()
    val isDeliveredOrderedProduct = remember(product?.title, allOrders) {
        val title = product?.title?.trim().orEmpty()
        title.isNotBlank() && allOrders.any { ord ->
            ord.status == com.example.data.model.OrderStatus.DELIVERED &&
                ord.itemsSummary.contains(title, ignoreCase = true)
        }
    }

    var quantity by remember { mutableIntStateOf(1) }
    var addedFeedback by remember { mutableStateOf(false) }
    var showFullScreenImage by remember { mutableStateOf(false) }
    var appliedBargainPrice by remember { mutableStateOf<Double?>(null) }
    var bargainResultMsg by remember { mutableStateOf("") }

    val nairaFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "NG")).apply {
            maximumFractionDigits = 0
        }
    }

    BackHandler {
        viewModel.navigateBack()
    }

    if (product == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Product not found")
        }
        return
    }

    val promoDiscountPercent = viewModel.getEffectiveDiscountPercent(product.category)
    val effectiveDiscount = if (promoDiscountPercent > 0) promoDiscountPercent else product.discountPercent
    val calculatedUnitPrice = product.calculatePrice(selectedBrand, selectedSize, effectiveDiscount, selectedColor.name)
    val adminPromo by viewModel.adminPromo.collectAsState()
    val backInStockIds by viewModel.backInStockProductIds.collectAsState()
    val isRestockWatched = backInStockIds.contains(product.id)
    val galleryImages = remember(product.id, product.imageUrl, product.galleryUrls) {
        product.allImageUrls.ifEmpty { listOf(product.imageUrl) }
    }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { galleryImages.size })

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(adminPromo.isActive, adminPromo.expiresAtMillis) {
        while (adminPromo.isActive && adminPromo.expiresAtMillis > System.currentTimeMillis()) {
            nowMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.navigationBars,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = product.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val wishlistIds by viewModel.wishlistIds.collectAsState()
                    val isWishlisted = wishlistIds.contains(product.id)

                    IconButton(
                        onClick = {
                            viewModel.toggleWishlist(product.id) { added ->
                                Toast.makeText(
                                    context,
                                    if (added) "Saved to your Wishlist in Supabase Cloud!" else "Removed from Wishlist",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier.testTag("detail_wishlist_button")
                    ) {
                        Icon(
                            imageVector = if (isWishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Wishlist",
                            tint = if (isWishlisted) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = {
                            val storeTitle = storeProfile.storeName.ifBlank { "ODIYA" }
                            val portalBase = com.example.util.WebPortalExporter.getSavedCustomPortalUrl(context)
                                .ifBlank {
                                    com.example.util.WebPortalExporter.normalizePortalUrl(storeProfile.apkDownloadUrl)
                                        .takeUnless { com.example.util.WebPortalExporter.isDirectApkReleaseUrl(it) }
                                        .orEmpty()
                                }
                                .ifBlank { DEPROS_DOWNLOAD_URL }
                            val shareTargetUrl = viewModel.buildCustomerSyncLink(portalBase) ?: "${portalBase.trimEnd('/')}/?open_app=1"
                            val shareProductMsg = buildString {
                                append("🛍️ Check out ${product.title} at $storeTitle!\n")
                                append("Price: ${nairaFormat.format(calculatedUnitPrice)}\n")
                                if (storeProfile.phoneNumber.isNotBlank()) {
                                    append("📞 Call / WhatsApp: ${storeProfile.phoneNumber}\n")
                                }
                                if (storeProfile.storeAddress.isNotBlank()) {
                                    append("📍 Location: ${storeProfile.storeAddress}\n")
                                }
                                append("\n📲 Open $storeTitle App & Store:\n$shareTargetUrl")
                            }

                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "$storeTitle - ${product.title}")
                                putExtra(Intent.EXTRA_TEXT, shareProductMsg)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Product & App Link"))
                        },
                        modifier = Modifier.testTag("detail_share_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share Product & App Link", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(
                        onClick = { viewModel.navigateToHome() },
                        modifier = Modifier.testTag("detail_home_button")
                    ) {
                        Icon(Icons.Default.Home, contentDescription = "Back to Home Page", tint = CyberCyan)
                    }

                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.CART) },
                        modifier = Modifier.testTag("detail_cart_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (cartCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = Color.White
                                    ) {
                                        Text(text = cartCount.toString(), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 500.dp)
                            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 28.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isOutOfStock = product.stockQuantity <= 0
                        val isCatalogLocked = product.isCatalogLocked ||
                            (product.partnerId.isNotBlank() && product.partnerId in lockedPartnerIds)
                        Button(
                            onClick = {
                                if (isCatalogLocked) return@Button
                                viewModel.addToCart(
                                    product = product,
                                    brand = selectedBrand,
                                    size = selectedSize,
                                    color = selectedColor,
                                    quantity = quantity,
                                    customBargainPrice = appliedBargainPrice,
                                    bargainNote = bargainResultMsg,
                                    onFeedback = { success, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        if (success) {
                                            viewModel.navigateTo(AppScreen.CART)
                                        }
                                    }
                                )
                            },
                            enabled = !isOutOfStock && !isCatalogLocked,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isOutOfStock || isCatalogLocked) Color(0xFF6B7280) else MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1.1f)
                                .height(50.dp)
                                .testTag("detail_add_to_cart_button")
                        ) {
                            Icon(
                                imageVector = if (isOutOfStock || isCatalogLocked) Icons.Default.Lock else Icons.Default.AddShoppingCart,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when {
                                    isCatalogLocked -> "Display Only"
                                    isOutOfStock -> "Out of Stock"
                                    else -> "Add to Cart"
                                },
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                if (isCatalogLocked) {
                                    Toast.makeText(context, "Display Only — Add-to-Cart paused until hosting is renewed.", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.addToCart(
                                        product = product,
                                        brand = selectedBrand,
                                        size = selectedSize,
                                        color = selectedColor,
                                        quantity = quantity,
                                        customBargainPrice = appliedBargainPrice,
                                        bargainNote = bargainResultMsg
                                    )
                                    Toast.makeText(context, "Opening Cart...", Toast.LENGTH_SHORT).show()
                                    viewModel.navigateTo(AppScreen.CART)
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(0.9f)
                                .height(50.dp)
                                .testTag("detail_buy_now_button")
                        ) {
                            Icon(
                                imageVector = if (isCatalogLocked) Icons.Default.Lock else Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = if (isCatalogLocked) Color(0xFF94A3B8) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isCatalogLocked) "Catalog Locked" else "View Cart",
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        DualAxisScrollContainer(
            wideMode = isWideDualAxisMode,
            minWideWidth = 680.dp,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { _, _ ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (isWideDualAxisMode) Modifier else Modifier.widthIn(max = 500.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp)
            ) {
                // ==============================================================
                // 1. MULTI-PHOTO SWIPEABLE GALLERY BOX (TAP TO ZOOM)
                // ==============================================================
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .testTag("product_detail_photo_card")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            beyondViewportPageCount = 1,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            val pageUrl = galleryImages[page]
                            val pinnedDetailImageRequest = ProductImageHelper.rememberImageRequest(
                                imageUrl = pageUrl,
                                category = product.category,
                                title = product.title,
                                highRes = true
                            )
                            AsyncImage(
                                model = pinnedDetailImageRequest,
                                contentDescription = "${product.title} photo ${page + 1}",
                                contentScale = ContentScale.Fit,
                                filterQuality = androidx.compose.ui.graphics.FilterQuality.High,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { showFullScreenImage = true }
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                        }

                        if (galleryImages.size > 1) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.Black.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "${pagerState.currentPage + 1} / ${galleryImages.size} • Swipe ↔",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Crisp bottom-right zoom indicator pill
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Black.copy(alpha = 0.65f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp)
                                .clickable { showFullScreenImage = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ZoomIn,
                                    contentDescription = "Tap to zoom",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Tap to zoom",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // SPECIAL APPEARANCE ADMIN PROMO DISCOUNT BADGE
                        if (effectiveDiscount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(bottomEnd = 14.dp, topStart = 16.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFFB91C1C),
                                                Color(0xFFDC2626),
                                                Color(0xFFF59E0B)
                                            )
                                        )
                                    )
                                    .border(
                                        BorderStroke(1.dp, Color(0xFFFDE047)),
                                        RoundedCornerShape(bottomEnd = 14.dp, topStart = 16.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("detail_promo_special_mark_${product.id}")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = Color(0xFFFDE047),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "ODIYA",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Multi-Photo Thumbnail Selector Strip
                if (galleryImages.size > 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        galleryImages.forEachIndexed { idx, thumbUrl ->
                            val isSelectedPage = pagerState.currentPage == idx
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White)
                                    .border(
                                        width = if (isSelectedPage) 2.5.dp else 1.dp,
                                        color = if (isSelectedPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        coroutineScope.launch { pagerState.animateScrollToPage(idx) }
                                    }
                            ) {
                                AsyncImage(
                                    model = ProductImageHelper.rememberImageRequest(
                                        imageUrl = thumbUrl,
                                        category = product.category,
                                        title = product.title
                                    ),
                                    contentDescription = "Thumbnail ${idx + 1}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                // High-Clarity Pinch & Double-Tap Zoomable Modal Dialog
                if (showFullScreenImage) {
                    val activeImageUrl = galleryImages.getOrNull(pagerState.currentPage) ?: product.imageUrl
                    com.example.ui.components.ZoomableImageDialog(
                        imageUrl = activeImageUrl,
                        title = "${product.title} (${pagerState.currentPage + 1}/${galleryImages.size})",
                        category = product.category,
                        onDismiss = { showFullScreenImage = false }
                    )
                }

                // Live Promo Countdown Timer Pill
                val remainingPromoMs = (adminPromo.expiresAtMillis - nowMillis).coerceAtLeast(0L)
                if (effectiveDiscount > 0 && adminPromo.isActive && remainingPromoMs > 0L) {
                    val hrs = remainingPromoMs / 3600000L
                    val mins = (remainingPromoMs % 3600000L) / 60000L
                    val secs = (remainingPromoMs % 60000L) / 1000L
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFB91C1C).copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${adminPromo.title} ends in",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFEF4444)
                            )
                        }
                    }
                }

                val isDetailCatalogLocked = product.isCatalogLocked ||
                    (product.partnerId.isNotBlank() && product.partnerId in lockedPartnerIds)
                if (isDetailCatalogLocked) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.5.dp, AccentGold.copy(alpha = 0.8f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("detail_catalog_locked_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Display Only",
                                tint = AccentGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "DISPLAY ONLY — PARTNER HOSTING PERIOD ENDED",
                                    color = AccentGold,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.5.sp
                                )
                                Text(
                                    text = "Add-to-Cart is temporarily locked by Admin while the Partner renews their hosting contract.",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // =========================================================================================
                // 2. SMALL RECTANGULAR BOX LOCATED NEXT BELOW IMAGE: ALL PRODUCT INFO COMPACTED NEATLY
                //    (Brands, Size, Price, Colors, Product Description, Rating, Stock Status)
                // =========================================================================================
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_detail_compact_info_box")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        // Title & Rating Header Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = product.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            if (isDeliveredOrderedProduct) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = AccentGold.copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Star, contentDescription = "Delivered Rating", tint = AccentGold, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "${product.rating} (${product.reviewCount})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = AccentGold
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                ) {
                                    Text(
                                        text = "Delivered to Rate",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Category: ${product.category} • SKU: ${product.sku}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(8.dp))

                        // PRICE SECTION
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (quantity > 1) "Total Price (${quantity} items)" else "Price",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val currentUnitPrice = appliedBargainPrice ?: calculatedUnitPrice
                                    Text(
                                        text = nairaFormat.format(currentUnitPrice * quantity),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = if (appliedBargainPrice != null) SuccessGreen else if (effectiveDiscount > 0) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary
                                    )
                                    if (effectiveDiscount > 0 || appliedBargainPrice != null) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = nairaFormat.format(product.basePrice * quantity),
                                            style = MaterialTheme.typography.bodySmall,
                                            textDecoration = TextDecoration.LineThrough,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    Brush.horizontalGradient(
                                                        colors = listOf(Color(0xFFDC2626), Color(0xFFF59E0B))
                                                    )
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.LocalFireDepartment,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = "ODIYA",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }
                                    }
                                }
                                val currentUnitPrice = appliedBargainPrice ?: calculatedUnitPrice
                                val totalSavings = (product.basePrice * quantity) - (currentUnitPrice * quantity)
                                if (totalSavings > 0) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (appliedBargainPrice != null) "💎 Total Bargain Savings: ${nairaFormat.format(totalSavings)}!" 
                                               else "🔥 Admin Special Offer: You Save ${nairaFormat.format(totalSavings)}!",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (appliedBargainPrice != null) SuccessGreen else Color(0xFF16A34A),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                    modifier = Modifier.testTag("detail_selected_variant_format")
                                ) {
                                    Text(
                                        text = "$selectedBrand / $selectedSize / ${nairaFormat.format(currentUnitPrice)} / ${selectedColor.name}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // Stock status
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when {
                                    product.stockQuantity == 0 -> Color(0xFFDC2626).copy(alpha = 0.15f)
                                    product.stockQuantity <= 3 -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                    else -> SuccessGreen.copy(alpha = 0.15f)
                                }
                            ) {
                                Text(
                                    text = when {
                                        product.stockQuantity == 0 -> "OUT OF STOCK"
                                        product.stockQuantity <= 3 -> "LOW STOCK (${product.stockQuantity})"
                                        else -> "${product.stockQuantity} in Stock"
                                    },
                                    color = when {
                                        product.stockQuantity == 0 -> Color(0xFFDC2626)
                                        product.stockQuantity <= 3 -> Color(0xFFF59E0B)
                                        else -> SuccessGreen
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        if (product.stockQuantity <= 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    val nowWatching = viewModel.toggleBackInStockWatch(product)
                                    Toast.makeText(
                                        context,
                                        if (nowWatching) "Alert set! We will notify you as soon as ${product.title} is restocked."
                                        else "Back-in-stock alert removed.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                border = BorderStroke(1.2.dp, if (isRestockWatched) SuccessGreen else CyberCyan),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("detail_restock_notify_button")
                            ) {
                                Icon(
                                    imageVector = if (isRestockWatched) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                                    contentDescription = "Back in stock alert",
                                    tint = if (isRestockWatched) SuccessGreen else CyberCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isRestockWatched) "Back-in-Stock Alert Active ✓ (Tap to cancel)" else "Notify Me When Available",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isRestockWatched) SuccessGreen else CyberCyan,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(10.dp))

                        val hasVariantPricing = product.variantPrices.isNotEmpty()
                        val displayBrands = remember(product.id, product.availableBrands, product.variantPrices) {
                            (product.availableBrands + product.variantPrices.map { it.brand })
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                                .distinctBy { it.lowercase() }
                                .ifEmpty { listOf(product.brand) }
                        }
                        val displaySizes = remember(product.id, product.availableSizes, product.variantPrices) {
                            (product.availableSizes + product.variantPrices.map { it.size })
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                                .distinctBy { it.lowercase() }
                                .ifEmpty { listOf("Standard") }
                        }
                        val displayColors = remember(product.id, product.availableColors, product.variantPrices) {
                            val combined = product.availableColors.toMutableList()
                            product.variantPrices.forEach { vp ->
                                if (vp.colorName.isNotBlank() && combined.none { it.name.equals(vp.colorName, ignoreCase = true) }) {
                                    combined.add(com.example.data.model.ProductColor(vp.colorName.trim(), vp.colorHex.ifBlank { "#111827" }))
                                }
                            }
                            combined
                        }
                        val brandPricesVary = remember(product.id, displayBrands, selectedSize, selectedColor, effectiveDiscount) {
                            displayBrands.map { product.calculatePrice(it, selectedSize, effectiveDiscount, selectedColor.name) }.distinct().size > 1
                        }
                        val sizePricesVary = remember(product.id, displaySizes, selectedBrand, selectedColor, effectiveDiscount) {
                            displaySizes.map { product.calculatePrice(selectedBrand, it, effectiveDiscount, selectedColor.name) }.distinct().size > 1
                        }
                        val colorPricesVary = remember(product.id, displayColors, selectedBrand, selectedSize, effectiveDiscount) {
                            displayColors.map { product.calculatePrice(selectedBrand, selectedSize, effectiveDiscount, it.name) }.distinct().size > 1
                        }

                        // BRANDS SELECTION
                        Text(
                            text = "Brand: $selectedBrand",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            displayBrands.forEach { b ->
                                val brandPrice = product.calculatePrice(b, selectedSize, effectiveDiscount, selectedColor.name)
                                val chipText = if (hasVariantPricing && brandPricesVary) "$b • ${nairaFormat.format(brandPrice)}" else b
                                FilterChip(
                                    selected = selectedBrand.equals(b, ignoreCase = true),
                                    onClick = { 
                                        viewModel.setDetailBrand(b)
                                        appliedBargainPrice = null
                                        bargainResultMsg = ""
                                    },
                                    label = { Text(chipText, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                                        selectedLabelColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.testTag("detail_brand_chip_$b")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // SIZES SELECTION
                        Text(
                            text = "Size: $selectedSize",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            displaySizes.forEach { sz ->
                                val sizePrice = product.calculatePrice(selectedBrand, sz, effectiveDiscount, selectedColor.name)
                                val sizeLabel = if (hasVariantPricing || sizePricesVary) "$sz • ${nairaFormat.format(sizePrice)}" else sz
                                FilterChip(
                                    selected = selectedSize.equals(sz, ignoreCase = true),
                                    onClick = { 
                                        viewModel.setDetailSize(sz)
                                        appliedBargainPrice = null
                                        bargainResultMsg = ""
                                    },
                                    label = { Text(sizeLabel, fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("detail_size_chip_$sz")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // COLORS SELECTION
                        Text(
                            text = "Colour: ${selectedColor.name}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            displayColors.forEach { c ->
                                val parsedColor = try {
                                    Color(android.graphics.Color.parseColor(c.hex))
                                } catch (e: Exception) {
                                    Color.Gray
                                }
                                val isSelected = selectedColor.name.equals(c.name, ignoreCase = true)
                                val colorPrice = product.calculatePrice(selectedBrand, selectedSize, effectiveDiscount, c.name)
                                Surface(
                                    onClick = {
                                        viewModel.setDetailColor(c)
                                        appliedBargainPrice = null
                                        bargainResultMsg = ""
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                    ),
                                    modifier = Modifier.testTag("detail_color_chip_${c.name}")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(parsedColor)
                                                .border(
                                                    width = if (isSelected) 2.dp else 1.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.3f),
                                                    shape = CircleShape
                                                )
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = if (c.hex.equals("#FFFFFF", ignoreCase = true) || c.hex.equals("#F3F4F6", ignoreCase = true)) Color.Black else Color.White,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = c.name,
                                                fontSize = 11.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (hasVariantPricing || colorPricesVary) {
                                                Text(
                                                    text = nairaFormat.format(colorPrice),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // QUANTITY SELECTION
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Quantity", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilledTonalIconButton(
                                    onClick = { if (quantity > 1) quantity-- },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    text = quantity.toString(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                                FilledTonalIconButton(
                                    onClick = { if (quantity < product.stockQuantity) quantity++ },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // PRODUCT DESCRIPTION
                        Text(
                            text = "Product Description",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = product.description,
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Expiry / Short-dated warning if applicable
                        if (product.expiryMonthsRemaining < 5) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, Color(0xFFF59E0B))
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (product.expiryMonthsRemaining <= 0) "Expired Batch Alert" else "Short-dated item (${product.expiryMonthsRemaining} months remaining to expiry)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF59E0B)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ==============================================================
                // CUSTOMER PRICE BARGAIN INPUT BOX (CONDITIONAL ON ₦200,000 CHECKOUT TOTAL)
                // ==============================================================
                val bargainConfig by viewModel.adminBargainConfig.collectAsState()
                val cartSubtotal by viewModel.cartSubtotal.collectAsState()

                val isCategoryEligible = viewModel.isCategoryEligibleForBargain(product.category)
                val effectiveDefaultBargainAmount = viewModel.getDefaultBargainAmountForCategory(product.category)
                val totalProductAmount = calculatedUnitPrice * quantity
                val isThresholdMet = bargainConfig.minCheckoutThreshold <= 0.0 || cartSubtotal >= bargainConfig.minCheckoutThreshold || totalProductAmount >= bargainConfig.minCheckoutThreshold
                val isEligible = bargainConfig.isActive && isCategoryEligible && isThresholdMet
                var proposedBargainInput by remember { mutableStateOf("") }
                var isCappedMsg by remember { mutableStateOf(false) }

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isEligible) CyberCyan.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(
                        width = 1.5.dp,
                        color = if (isEligible) CyberCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                        .testTag("product_detail_bargain_box")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isEligible) CyberCyan.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isEligible) Icons.Default.Sell else Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = if (isEligible) CyberCyan else Color.Gray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "SPECIAL OFFER: PRICE BARGAIN BOX",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isEligible) CyberCyan else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = when {
                                            !bargainConfig.isActive -> "Bargain privilege is currently LOCKED by Admin"
                                            !isCategoryEligible -> "Offer designated exclusively for '${bargainConfig.designatedCategory}'"
                                            !isThresholdMet -> "Requires ${nairaFormat.format(bargainConfig.minCheckoutThreshold)} checkout total"
                                            else -> "Active for ${product.category} (Default: ${nairaFormat.format(effectiveDefaultBargainAmount)} off)"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = if (isEligible) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (!bargainConfig.isActive) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF450A0A),
                                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "🔒 Price bargaining is currently deactivated by store administrator.",
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        } else if (!isCategoryEligible) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Sell, contentDescription = null, tint = AccentGold, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Admin has designated the active Price Bargain offer exclusively to '${bargainConfig.designatedCategory}'. Standard catalog price applies to '${product.category}'.",
                                        fontSize = 11.5.sp,
                                        lineHeight = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else if (!isThresholdMet) {
                            val currentTotal = cartSubtotal.coerceAtLeast(totalProductAmount)
                            val remainingAmount = (bargainConfig.minCheckoutThreshold - currentTotal).coerceAtLeast(0.0)
                            // INACTIVE MESSAGE FOR CHECKOUT LESS THAN MINIMUM THRESHOLD
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF450A0A),
                                border = BorderStroke(1.dp, Color(0xFFEF4444))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "BARGAIN PRIVILEGE LOCKED",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Unlock the bargain offer by adding the remaining amount (${nairaFormat.format(remainingAmount)}) to unlock the offer!",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 16.sp,
                                        color = Color(0xFFFCA5A5)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Price bargaining requires a minimum total of ${nairaFormat.format(bargainConfig.minCheckoutThreshold)}. Build your cart to ${nairaFormat.format(bargainConfig.minCheckoutThreshold)} or more to automatically unlock custom price bargaining!",
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        color = Color.White
                                    )

                                    val neededToReachThreshold = (bargainConfig.minCheckoutThreshold - cartSubtotal).coerceAtLeast(0.0)
                                    val unitsNeededToUnlock = kotlin.math.ceil(neededToReachThreshold / calculatedUnitPrice).toInt().coerceAtLeast(1)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            quantity = unitsNeededToUnlock.coerceAtMost(product.stockQuantity)
                                            viewModel.addToCart(
                                                product = product,
                                                brand = selectedBrand,
                                                size = selectedSize,
                                                color = selectedColor,
                                                quantity = unitsNeededToUnlock,
                                                customBargainPrice = null
                                            )
                                            Toast.makeText(context, "Added $unitsNeededToUnlock units to Cart! Price Bargain Offer Unlocked!", Toast.LENGTH_LONG).show()
                                            viewModel.navigateTo(AppScreen.CART)
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("add_units_unlock_bargain_pdp_button")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "➕ Add $unitsNeededToUnlock Units to Cart to Unlock Bargain Offer (${nairaFormat.format(calculatedUnitPrice * unitsNeededToUnlock)})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        } else {
                            // ACTIVE PRICE BARGAIN INPUT BOX
                            val defaultDiscountedPrice = (calculatedUnitPrice - effectiveDefaultBargainAmount).coerceAtLeast(calculatedUnitPrice * 0.10)

                            // Quick 1-Tap Apply Admin Default Offer Button
                            Button(
                                onClick = {
                                    proposedBargainInput = defaultDiscountedPrice.toLong().toString()
                                    val res = viewModel.evaluateBargain(calculatedUnitPrice, defaultDiscountedPrice, product.category)
                                    appliedBargainPrice = res.approvedPrice
                                    bargainResultMsg = res.message
                                    isCappedMsg = res.isCappedAtMax
                                    Toast.makeText(context, "Applied Admin Default Bargain Offer!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color.Black),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("apply_default_bargain_offer_button")
                            ) {
                                Icon(Icons.Default.Sell, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "⚡ Apply Default Offer: Save ${nairaFormat.format(effectiveDefaultBargainAmount)} (Pay ${nairaFormat.format(defaultDiscountedPrice)})",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Or propose your custom offer amount (₦):",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = proposedBargainInput,
                                    onValueChange = { proposedBargainInput = it },
                                    placeholder = { Text("e.g. ${defaultDiscountedPrice.toLong()}") },
                                    label = { Text("Proposed Amount (₦)") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("customer_bargain_amount_input")
                                )

                                Button(
                                    onClick = {
                                        val proposed = proposedBargainInput.toDoubleOrNull() ?: 0.0
                                        if (proposed <= 0.0) {
                                            Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        val res = viewModel.evaluateBargain(calculatedUnitPrice, proposed, product.category)
                                        appliedBargainPrice = res.approvedPrice
                                        bargainResultMsg = res.message
                                        isCappedMsg = res.isCappedAtMax
                                        Toast.makeText(context, if (res.isCappedAtMax) "Bargain Approved at Max Limit!" else "Bargain Approved!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .height(54.dp)
                                        .testTag("apply_customer_bargain_button")
                                ) {
                                    Text("APPLY BARGAIN", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                }
                            }

                            // DISPLAY RESULT MESSAGE / CAP WARNING
                            if (bargainResultMsg.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isCappedMsg) AccentGold.copy(alpha = 0.15f) else SuccessGreen.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, if (isCappedMsg) AccentGold else SuccessGreen)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isCappedMsg) Icons.Default.Warning else Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = if (isCappedMsg) AccentGold else SuccessGreen,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isCappedMsg) "BARGAIN CAPPED AT MAX ADMIN LIMIT" else "BARGAIN APPROVED AUTOMATICALLY",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.sp,
                                                color = if (isCappedMsg) AccentGold else SuccessGreen
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = bargainResultMsg,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            lineHeight = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Delivery & Trust Highlights
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Express Delivery Available (24 - 48 Hours)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Official Verified Settlement: OPay, Moniepoint & POS", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Customer Product Reviews & Verified Purchase Ratings (Active ONLY on Delivered Ordered Products)
                val allReviews by viewModel.productReviews.collectAsState()
                val reviewsForProduct = remember(allReviews, product.id) {
                    allReviews.filter { it.productId == product.id }
                }
                var reviewStars by remember { mutableIntStateOf(5) }
                var reviewComment by remember { mutableStateOf("") }
                val reviewDateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        1.dp,
                        if (isDeliveredOrderedProduct) SuccessGreen.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_reviews_section")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Customer Reviews & Ratings",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = if (isDeliveredOrderedProduct) {
                                        "${product.rating} ★ overall (${product.reviewCount} total ratings)"
                                    } else {
                                        "Active ONLY on delivered ordered products"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isDeliveredOrderedProduct) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SuccessGreen.copy(alpha = 0.14f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Verified, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Delivered Order", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        Spacer(modifier = Modifier.height(12.dp))

                        if (!isDeliveredOrderedProduct) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reviews_locked_notice")
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Ratings & Views Locked",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = AccentGold
                                    )
                                    Text(
                                        text = "Ratings and customer review views become active only after an order containing ${product.title} is marked DELIVERED.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    OutlinedButton(
                                        onClick = { viewModel.navigateTo(AppScreen.ORDERS) },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("reviews_locked_track_orders_button")
                                    ) {
                                        Text("Track My Orders", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "Rate this delivered item:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                (1..5).forEach { star ->
                                    IconButton(
                                        onClick = { reviewStars = star },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("review_star_$star")
                                    ) {
                                        Icon(
                                            imageVector = if (star <= reviewStars) Icons.Default.Star else Icons.Default.StarBorder,
                                            contentDescription = "$star Stars",
                                            tint = if (star <= reviewStars) AccentGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$reviewStars / 5 Stars",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = AccentGold
                                )
                            }

                            OutlinedTextField(
                                value = reviewComment,
                                onValueChange = { reviewComment = it },
                                label = { Text("Share your review or fit feedback") },
                                placeholder = { Text("Quality, sizing, packaging, delivery speed...") },
                                minLines = 2,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("product_review_comment_input")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    viewModel.submitProductReview(
                                        productId = product.id,
                                        rating = reviewStars,
                                        comment = reviewComment
                                    ) { success, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        if (success) reviewComment = ""
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("submit_product_review_button")
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Post Review", fontWeight = FontWeight.Bold)
                            }

                            if (reviewsForProduct.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                reviewsForProduct.take(6).forEachIndexed { idx, rev ->
                                    if (idx > 0) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                                        )
                                    }
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = rev.customerName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.5.sp
                                                )
                                                if (rev.isVerifiedPurchase) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = SuccessGreen.copy(alpha = 0.14f)
                                                    ) {
                                                        Text(
                                                            text = "✓ Delivered Buyer",
                                                            fontSize = 9.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = SuccessGreen,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = reviewDateFormat.format(Date(rev.createdAt)),
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        ) {
                                            repeat(rev.rating) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = AccentGold,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = rev.comment,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
