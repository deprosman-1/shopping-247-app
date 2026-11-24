package com.example.ui.screens

import com.example.R
import coil.compose.AsyncImage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items as lazyListItems
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.Loyalty
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AppScreen
import com.example.MainViewModel
import com.example.data.model.CatalogBrowseMode
import com.example.data.model.CatalogSortOption
import com.example.data.model.PriceRangeFilter
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.ui.components.CategorySelectorButton
import com.example.ui.components.DualAxisScrollContainer
import com.example.ui.components.HorizontalCategoryStrip
import com.example.ui.components.ShopEmblemWatermark
import com.example.ui.components.ProductStandardCard
import com.example.ui.components.QuickEditProductDialog
import com.example.ui.components.ShareAppDialog
import com.example.ui.components.AppUpdateDialog
import com.example.ui.theme.AccentGold
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.SuccessGreen
import com.example.util.ProductImageHelper
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allProducts by viewModel.allProducts.collectAsState()
    val products by viewModel.filteredProducts.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var isSearchExpanded by remember { mutableStateOf(false) }
    val cartCount by viewModel.cartTotalCount.collectAsState()
    val wishlistCount by viewModel.wishlistCount.collectAsState()
    val wishlistIds by viewModel.wishlistIds.collectAsState()
    val pendingSharedReceipt by viewModel.pendingSharedReceipt.collectAsState()
    val currentCustomer by viewModel.currentCustomer.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isFullScreen by viewModel.isFullScreen.collectAsState()
    val adminPromo by viewModel.adminPromo.collectAsState()
    val updateAvailable by viewModel.updateAvailable.collectAsState()
    val latestVersionName by viewModel.latestVersionName.collectAsState()
    val releaseNotes by viewModel.releaseNotes.collectAsState()
    val updateDownloadUrl by viewModel.updateDownloadUrl.collectAsState()
    val storeProfile by viewModel.storeProfile.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val priceRangeFilter by viewModel.priceRangeFilter.collectAsState()
    val catalogBrowseMode by viewModel.catalogBrowseMode.collectAsState()
    val isWideDualAxisMode by viewModel.isWideDualAxisMode.collectAsState()
    val inStockOnly by viewModel.inStockOnly.collectAsState()
    val selectedBrandFilter by viewModel.selectedBrandFilter.collectAsState()
    val availableBrandFilters by viewModel.availableBrandFilters.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val backInStockIds by viewModel.backInStockProductIds.collectAsState()
    val loyaltyAccount by viewModel.loyaltyAccount.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val offlineQueue by viewModel.offlineQueue.collectAsState()
    val lockedPartnerIds by viewModel.lockedPartnerIds.collectAsState()
    val allOrders by viewModel.allOrders.collectAsState()

    LaunchedEffect(products) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            ProductImageHelper.preloadProductImages(context, products)
        }
    }

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(adminPromo.isActive, adminPromo.expiresAtMillis) {
        while (adminPromo.isActive && adminPromo.expiresAtMillis > System.currentTimeMillis()) {
            nowMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    val listState = rememberLazyListState()
    val wishlistSet = remember(wishlistIds) { wishlistIds.toSet() }
    val stableOpenProductDetails = remember(viewModel) {
        { product: Product -> viewModel.openProductDetails(product) }
    }
    val stableToggleWishlist = remember(viewModel) {
        { productId: String -> viewModel.toggleWishlist(productId) }
    }
    val stableAddToCart = remember(viewModel, context) {
        { product: Product ->
            viewModel.addToCart(
                product = product,
                onFeedback = { _, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    var showQrShareDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        val effectiveWide = false
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_screen")
        ) {
            // App Top Brand Header (Compact & Hidden in Full Screen to maximize catalog space)
            AnimatedVisibility(
                visible = !isFullScreen,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Prominent ODIYA Logo Badge (Long-press opens restricted Admin area)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF052E16),
                                border = BorderStroke(1.2.dp, AccentGold),
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .combinedClickable(
                                        onClick = { viewModel.navigateToHome() },
                                        onLongClick = { viewModel.navigateTo(AppScreen.ADMIN_CONTROL_ROOM) }
                                    )
                                    .testTag("home_brand_logo_box")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                ) {
                                    AsyncImage(
                                        model = R.drawable.shopping_logo_icon_1794752250458,
                                        contentDescription = "ODIYA Logo",
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "ODIYA",
                                        fontSize = 10.sp,
                                        lineHeight = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = AccentGold,
                                        softWrap = false
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = storeProfile.storeName.uppercase(),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = storeProfile.tagline,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Compact Top Right Actions
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (currentCustomer != null) {
                                        viewModel.playSignInAudio(currentCustomer!!.fullName)
                                    } else {
                                        viewModel.playWelcomeAudio()
                                    }
                                    Toast.makeText(context, "Playing Live Welcome Voice Message...", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(40.dp).testTag("voice_welcome_nav_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Live Welcome Audio",
                                    tint = AccentGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = { showQrShareDialog = true },
                                modifier = Modifier.size(40.dp).testTag("qr_share_nav_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = "Share App & QR Code",
                                    tint = AccentGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = { viewModel.openCustomerAccountShortcut() },
                                modifier = Modifier.size(48.dp).testTag("home_customer_account_shortcut")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Customer Account Shortcut",
                                    tint = if (currentCustomer != null) SuccessGreen else AccentGold,
                                    modifier = Modifier.size(21.dp)
                                )
                            }

                            IconButton(
                                onClick = { viewModel.navigateTo(AppScreen.CART) },
                                modifier = Modifier.size(48.dp).testTag("cart_nav_button")
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
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = "Shopping Cart",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Slim Top Live Digital Clock, Search Trigger & Store Actions Strip (Hidden in Full Screen)
            AnimatedVisibility(
                visible = !isFullScreen,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF064E3B),
                    border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                        .testTag("home_top_clock_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DigitalClock(
                            modifier = Modifier.weight(1f)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            // Compact Search Pill (Opens full catalog search + history on demand)
                            Surface(
                                onClick = { isSearchExpanded = !isSearchExpanded },
                                shape = RoundedCornerShape(9.dp),
                                color = if (isSearchExpanded || searchQuery.isNotEmpty()) AccentGold else Color.White.copy(alpha = 0.12f),
                                modifier = Modifier
                                    .height(30.dp)
                                    .testTag("catalog_search_toggle_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search Catalog",
                                        tint = if (isSearchExpanded || searchQuery.isNotEmpty()) Color.Black else AccentGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (searchQuery.isNotEmpty()) "\"${searchQuery.take(8)}\"" else "Search",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isSearchExpanded || searchQuery.isNotEmpty()) Color.Black else Color.White,
                                        maxLines = 1
                                    )
                                }
                            }

                            Surface(
                                onClick = {
                                    viewModel.refreshData { success ->
                                        Toast.makeText(
                                            context,
                                            if (success) "Live Store Catalog Refreshed!" else "Local Catalog Refreshed",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                shape = RoundedCornerShape(9.dp),
                                color = Color.White.copy(alpha = 0.10f),
                                modifier = Modifier.height(30.dp).testTag("home_refresh_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh Catalog",
                                        modifier = Modifier.size(14.dp),
                                        tint = CyberCyan
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isRefreshing) "..." else "Sync",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = CyberCyan
                                    )
                                }
                            }

                            Surface(
                                onClick = { viewModel.toggleFullScreen() },
                                shape = RoundedCornerShape(9.dp),
                                color = Color.White.copy(alpha = 0.10f),
                                modifier = Modifier.size(30.dp).testTag("full_screen_toggle")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                        contentDescription = "Toggle Full Screen",
                                        tint = AccentGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Offline Mode & Queued Sync Status Strip
            if (!isOnline || offlineQueue.isNotEmpty()) {
                Surface(
                    onClick = {
                        if (isOnline && offlineQueue.isNotEmpty()) {
                            viewModel.triggerOfflineQueueSync { count ->
                                Toast.makeText(context, "Synced $count queued action(s)!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Offline mode active — actions are queued safely and sync automatically on reconnect.", Toast.LENGTH_LONG).show()
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = if (!isOnline) Color(0xFFB45309).copy(alpha = 0.16f) else CyberCyan.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, if (!isOnline) AccentGold else CyberCyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                        .testTag("home_offline_status_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (!isOnline) Icons.Default.CloudOff else Icons.Default.Sync,
                                contentDescription = "Connectivity status",
                                tint = if (!isOnline) AccentGold else CyberCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (!isOnline) {
                                    "Offline Mode (${offlineQueue.size} queued) — Will sync when reconnected"
                                } else {
                                    "${offlineQueue.size} queued action(s) ready — Tap to sync now"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!isOnline) AccentGold else CyberCyan,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            if (pendingSharedReceipt != null) {
                val receiptLabel = pendingSharedReceipt?.detectedPaymentMethod?.title
                    ?: pendingSharedReceipt?.sourceAppLabel
                    ?: "OPay / Moniepoint"
                Surface(
                    onClick = {
                        if (cartCount > 0) viewModel.navigateTo(AppScreen.CHECKOUT)
                        else Toast.makeText(context, "Add any item to Cart, then Checkout to attach your $receiptLabel receipt.", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = SuccessGreen.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, SuccessGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                        .testTag("home_pending_shared_receipt_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (cartCount > 0) "$receiptLabel Receipt Attached — Tap to Checkout" else "$receiptLabel Receipt Ready — Add item(s) to Checkout",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { viewModel.clearPendingSharedReceipt() },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Dismiss shared receipt", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // Automatic App Maintenance & Upgrade Banner
            if (storeProfile.isMaintenanceMode) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF7C2D12),
                    border = BorderStroke(1.2.dp, AccentGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 3.dp)
                        .testTag("home_maintenance_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = "App Maintenance & Upgrade Alert",
                            tint = AccentGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = storeProfile.maintenanceMessage.ifBlank {
                                "Scheduled system maintenance & app upgrade in progress. Orders will auto-sync cleanly."
                            },
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Rearranged Unified Upper Toolbar: Pinned Category Dropdown + Scrollable Action & Filter Strip
            val categoryCounts = remember(allProducts) {
                allProducts.groupingBy { it.category }.eachCount()
            }
            var sortMenuExpanded by remember { mutableStateOf(false) }
            var priceMenuExpanded by remember { mutableStateOf(false) }
            var brandMenuExpanded by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 3.dp)
                    .testTag("home_upper_controls_row"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pinned Category Selector Button (Never scrolls away, saves full row height)
                CategorySelectorButton(
                    selectedCategory = selectedCategory,
                    categoryCounts = categoryCounts,
                    totalCount = allProducts.size,
                    onSelectCategory = { viewModel.selectCategory(it) }
                )

                Spacer(modifier = Modifier.width(7.dp))
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(22.dp)
                        .background(AccentGold.copy(alpha = 0.35f))
                )
                Spacer(modifier = Modifier.width(7.dp))

                // Compact Horizontally Scrollable Action & Filter Chips beside Category Button
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Live Audio & Video Call with Admin
                    AssistChip(
                        onClick = { viewModel.openLiveCallScreen() },
                        leadingIcon = {
                            Icon(Icons.Default.Videocam, contentDescription = "Live Audio and Video Call", tint = SuccessGreen, modifier = Modifier.size(15.dp))
                        },
                        label = {
                            Text("Live Call", fontSize = 11.sp, fontWeight = FontWeight.Black, color = SuccessGreen)
                        },
                        modifier = Modifier.height(34.dp).testTag("home_live_call_chip")
                    )

                    // Single-Tap Layout Mode Toggle (Grid View <-> Category Shelves)
                    val nextBrowseMode = if (catalogBrowseMode == CatalogBrowseMode.CATEGORY_SHELVES) {
                        CatalogBrowseMode.CAROUSEL_AND_GRID
                    } else {
                        CatalogBrowseMode.CATEGORY_SHELVES
                    }
                    val modeIcon = if (catalogBrowseMode == CatalogBrowseMode.CATEGORY_SHELVES) {
                        Icons.Default.ViewAgenda
                    } else {
                        Icons.Default.ViewCarousel
                    }
                    FilterChip(
                        selected = catalogBrowseMode == CatalogBrowseMode.CATEGORY_SHELVES,
                        onClick = { viewModel.setCatalogBrowseMode(nextBrowseMode) },
                        leadingIcon = {
                            Icon(imageVector = modeIcon, contentDescription = catalogBrowseMode.label, modifier = Modifier.size(15.dp))
                        },
                        label = {
                            Text(
                                text = if (catalogBrowseMode == CatalogBrowseMode.CATEGORY_SHELVES) "Shelves" else "Grid View",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentGold.copy(alpha = 0.22f),
                            selectedLabelColor = AccentGold
                        ),
                        modifier = Modifier.height(34.dp).testTag("browse_mode_${catalogBrowseMode.name}")
                    )

                    // Sort Dropdown Chip
                    Box {
                        FilterChip(
                            selected = sortOption != CatalogSortOption.FEATURED,
                            onClick = { sortMenuExpanded = true },
                            leadingIcon = {
                                Icon(Icons.Default.Sort, contentDescription = "Sort Products", modifier = Modifier.size(15.dp))
                            },
                            label = {
                                Text("Sort: ${sortOption.label}", fontSize = 11.sp, fontWeight = FontWeight.Black)
                            },
                            trailingIcon = {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(15.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                labelColor = Color.White,
                                iconColor = AccentGold,
                                selectedContainerColor = AccentGold.copy(alpha = 0.22f),
                                selectedLabelColor = AccentGold,
                                selectedLeadingIconColor = AccentGold,
                                selectedTrailingIconColor = AccentGold
                            ),
                            modifier = Modifier.height(34.dp).testTag("catalog_sort_chip")
                        )

                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false },
                            containerColor = Color.White,
                            border = BorderStroke(1.2.dp, Color(0xFFD1D5DB))
                        ) {
                            CatalogSortOption.entries.forEach { option ->
                                val isSelected = option == sortOption
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.label,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.ExtraBold,
                                            color = if (isSelected) Color(0xFFDC2626) else Color(0xFF000000)
                                        )
                                    },
                                    trailingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    colors = MenuDefaults.itemColors(
                                        textColor = Color(0xFF000000),
                                        leadingIconColor = Color(0xFF000000),
                                        trailingIconColor = Color(0xFFDC2626)
                                    ),
                                    onClick = {
                                        viewModel.setSortOption(option)
                                        sortMenuExpanded = false
                                    },
                                    modifier = Modifier
                                        .background(if (isSelected) Color(0xFFFEF2F2) else Color.White)
                                        .testTag("sort_option_${option.name}")
                                )
                            }
                        }
                    }

                    // Price Range Dropdown Chip (Consolidates 4 price chips cleanly)
                    Box {
                        FilterChip(
                            selected = priceRangeFilter != PriceRangeFilter.ALL,
                            onClick = { priceMenuExpanded = true },
                            leadingIcon = {
                                Icon(Icons.Default.FilterList, contentDescription = "Price Filter", modifier = Modifier.size(15.dp))
                            },
                            label = {
                                Text(
                                    text = if (priceRangeFilter == PriceRangeFilter.ALL) "Price" else priceRangeFilter.label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            },
                            trailingIcon = {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(15.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                labelColor = Color.White,
                                iconColor = AccentGold,
                                selectedContainerColor = AccentGold.copy(alpha = 0.22f),
                                selectedLabelColor = AccentGold,
                                selectedLeadingIconColor = AccentGold,
                                selectedTrailingIconColor = AccentGold
                            ),
                            modifier = Modifier.height(34.dp).testTag("catalog_price_filter_chip")
                        )

                        DropdownMenu(
                            expanded = priceMenuExpanded,
                            onDismissRequest = { priceMenuExpanded = false },
                            containerColor = Color.White,
                            border = BorderStroke(1.2.dp, Color(0xFFD1D5DB))
                        ) {
                            PriceRangeFilter.entries.forEach { range ->
                                val isSelected = range == priceRangeFilter
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = range.label,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.ExtraBold,
                                            color = if (isSelected) Color(0xFFDC2626) else Color(0xFF000000)
                                        )
                                    },
                                    trailingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    colors = MenuDefaults.itemColors(
                                        textColor = Color(0xFF000000),
                                        leadingIconColor = Color(0xFF000000),
                                        trailingIconColor = Color(0xFFDC2626)
                                    ),
                                    onClick = {
                                        viewModel.setPriceRangeFilter(range)
                                        priceMenuExpanded = false
                                    },
                                    modifier = Modifier
                                        .background(if (isSelected) Color(0xFFFEF2F2) else Color.White)
                                        .testTag("price_filter_${range.name}")
                                )
                            }
                        }
                    }

                    // Brand Filter Dropdown Chip
                    if (availableBrandFilters.isNotEmpty()) {
                        Box {
                            FilterChip(
                                selected = selectedBrandFilter != null,
                                onClick = { brandMenuExpanded = true },
                                label = {
                                    Text(
                                        text = selectedBrandFilter ?: "Brands",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                },
                                trailingIcon = {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(15.dp))
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    labelColor = Color.White,
                                    iconColor = AccentGold,
                                    selectedContainerColor = AccentGold.copy(alpha = 0.22f),
                                    selectedLabelColor = AccentGold,
                                    selectedTrailingIconColor = AccentGold
                                ),
                                modifier = Modifier.height(34.dp).testTag("catalog_brand_filter_chip")
                            )
                            DropdownMenu(
                                expanded = brandMenuExpanded,
                                onDismissRequest = { brandMenuExpanded = false },
                                containerColor = Color.White,
                                border = BorderStroke(1.2.dp, Color(0xFFD1D5DB))
                            ) {
                                val allSelected = selectedBrandFilter == null
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "All Brands",
                                            fontSize = 14.sp,
                                            fontWeight = if (allSelected) FontWeight.Black else FontWeight.ExtraBold,
                                            color = if (allSelected) Color(0xFFDC2626) else Color(0xFF000000)
                                        )
                                    },
                                    trailingIcon = if (allSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    colors = MenuDefaults.itemColors(
                                        textColor = Color(0xFF000000),
                                        leadingIconColor = Color(0xFF000000),
                                        trailingIconColor = Color(0xFFDC2626)
                                    ),
                                    onClick = {
                                        viewModel.selectBrandFilter(null)
                                        brandMenuExpanded = false
                                    },
                                    modifier = Modifier
                                        .background(if (allSelected) Color(0xFFFEF2F2) else Color.White)
                                        .testTag("brand_filter_all")
                                )
                                availableBrandFilters.forEach { bName ->
                                    val isSelected = selectedBrandFilter == bName
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = bName,
                                                fontSize = 14.sp,
                                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.ExtraBold,
                                                color = if (isSelected) Color(0xFFDC2626) else Color(0xFF000000)
                                            )
                                        },
                                        trailingIcon = if (isSelected) {
                                            { Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp)) }
                                        } else null,
                                        colors = MenuDefaults.itemColors(
                                            textColor = Color(0xFF000000),
                                            leadingIconColor = Color(0xFF000000),
                                            trailingIconColor = Color(0xFFDC2626)
                                        ),
                                        onClick = {
                                            viewModel.selectBrandFilter(bName)
                                            brandMenuExpanded = false
                                        },
                                        modifier = Modifier
                                            .background(if (isSelected) Color(0xFFFEF2F2) else Color.White)
                                            .testTag("brand_filter_${bName.lowercase()}")
                                    )
                                }
                            }
                        }
                    }

                    // In-Stock Only Filter Chip
                    FilterChip(
                        selected = inStockOnly,
                        onClick = { viewModel.setInStockOnly(!inStockOnly) },
                        leadingIcon = {
                            Icon(Icons.Default.Inventory, contentDescription = "In stock only", modifier = Modifier.size(14.dp))
                        },
                        label = {
                            Text(
                                text = if (inStockOnly) "In Stock ✓" else "In Stock",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SuccessGreen.copy(alpha = 0.2f),
                            selectedLabelColor = SuccessGreen
                        ),
                        modifier = Modifier.height(34.dp).testTag("catalog_instock_chip")
                    )

                    // Loyalty Points Quick Access Chip
                    AssistChip(
                        onClick = { viewModel.navigateTo(AppScreen.AUTH) },
                        leadingIcon = {
                            Icon(Icons.Default.Loyalty, contentDescription = "Loyalty points", tint = AccentGold, modifier = Modifier.size(15.dp))
                        },
                        label = {
                            Text("${loyaltyAccount.pointsBalance} pts", fontSize = 11.sp, fontWeight = FontWeight.Black, color = AccentGold)
                        },
                        modifier = Modifier.height(34.dp).testTag("home_loyalty_chip")
                    )

                    // Help Center, Support & Legal Chip
                    AssistChip(
                        onClick = { viewModel.navigateTo(AppScreen.HELP_SUPPORT) },
                        leadingIcon = {
                            Icon(Icons.Default.HelpOutline, contentDescription = "Help and support", tint = CyberCyan, modifier = Modifier.size(15.dp))
                        },
                        label = {
                            Text("Help", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                        },
                        modifier = Modifier.height(34.dp).testTag("home_help_support_chip")
                    )
                }
            }

            // Sideways-Scrollable 23-Category Strip
            HorizontalCategoryStrip(
                selectedCategory = selectedCategory,
                categoryCounts = categoryCounts,
                totalCount = allProducts.size,
                onSelectCategory = { viewModel.selectCategory(it) }
            )

            // Expandable Search Panel (Takes 0dp until tapped so catalog stays front-and-center)
            AnimatedVisibility(
                visible = isSearchExpanded || searchQuery.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                DropdownSearchBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    recentSearches = recentSearches,
                    onRecordSearch = { viewModel.recordRecentSearch(it) },
                    onClearRecentSearches = { viewModel.clearRecentSearches() },
                    onCollapse = { isSearchExpanded = false }
                )
            }

            // Main Catalog Content (Smooth Vertical Scrolling)
            DualAxisScrollContainer(
                wideMode = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { _, _ ->
                if (catalogBrowseMode == CatalogBrowseMode.CATEGORY_SHELVES) {
                    // DUAL-AXIS MODE 2: Vertically stacked Category Rows, each scrolling Horizontally!
                    val categoryShelves = remember(products) {
                        val known = ProductCategory.entries.map { it.displayName }
                        val extras = products.map { it.category }.distinct().filter { c ->
                            known.none { it.equals(c, ignoreCase = true) }
                        }
                        (known + extras).mapNotNull { catName ->
                            val itemsInCat = products.filter { it.category.equals(catName, ignoreCase = true) }
                            if (itemsInCat.isNotEmpty()) catName to itemsInCat else null
                        }
                    }

                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("home_category_shelves_list")
                    ) {
                        if (searchQuery.isEmpty() && selectedCategory == null) {
                            item(contentType = "crest") {
                                ShopEmblemWatermark(
                                    onOpenQrShare = { showQrShareDialog = true },
                                    storeName = storeProfile.storeName,
                                    tagline = storeProfile.tagline,
                                    storePhone = storeProfile.phoneNumber,
                                    secondaryPhone = storeProfile.secondaryPhoneNumber,
                                    storeAddress = storeProfile.storeAddress
                                )
                            }
                        }

                        if (adminPromo.isActive) {
                            item(contentType = "promo_banner") {
                                val remMs = (adminPromo.expiresAtMillis - nowMillis).coerceAtLeast(0L)
                                val timerStr = if (remMs > 0L) {
                                    String.format(
                                        Locale.US,
                                        " • %02d:%02d:%02d left",
                                        remMs / 3600000L,
                                        (remMs % 3600000L) / 60000L,
                                        (remMs % 60000L) / 1000L
                                    )
                                } else ""
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFB91C1C),
                                    border = BorderStroke(1.2.dp, Color(0xFFFDE047)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFFDE047))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "🔥 ${adminPromo.title} — ${adminPromo.discountPercent}% OFF!$timerStr",
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        if (products.isEmpty()) {
                            item(contentType = "empty") {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(40.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.LocalMall,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("No products match your filter.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(onClick = {
                                            viewModel.selectCategory(null)
                                            viewModel.setSearchQuery("")
                                            viewModel.setSortOption(CatalogSortOption.FEATURED)
                                            viewModel.setPriceRangeFilter(PriceRangeFilter.ALL)
                                        }) {
                                            Text("Reset Filters")
                                        }
                                    }
                                }
                            }
                        } else {
                            categoryShelves.forEach { (catName, catItems) ->
                                item(key = "shelf_$catName", contentType = "category_shelf") {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = catName.uppercase(),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Black,
                                                color = AccentGold
                                            )
                                            Text(
                                                text = "${catItems.size} items • Swipe ↔",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = CyberCyan
                                            )
                                        }
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            contentPadding = PaddingValues(end = 8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            lazyListItems(
                                                items = catItems,
                                                key = { "shelf_${catName}_${it.id}" },
                                                contentType = { "product_card" }
                                            ) { product ->
                                                val effectiveDiscount = viewModel.getEffectiveDiscountPercent(product.category)
                                                val isLocked = product.isCatalogLocked || (product.partnerId.isNotBlank() && product.partnerId in lockedPartnerIds)
                                                val isDeliveredOrdered = remember(product.title, allOrders) {
                                                    allOrders.any { it.status == com.example.data.model.OrderStatus.DELIVERED && it.itemsSummary.contains(product.title, ignoreCase = true) }
                                                }
                                                ProductStandardCard(
                                                    product = product,
                                                    promoDiscountPercent = effectiveDiscount,
                                                    onClick = { stableOpenProductDetails(product) },
                                                    onAddToCart = stableAddToCart,
                                                    isWishlisted = wishlistSet.contains(product.id),
                                                    onToggleWishlist = { stableToggleWishlist(product.id) },
                                                    isRestockWatched = backInStockIds.contains(product.id),
                                                    onToggleRestockWatch = {
                                                        val watching = viewModel.toggleBackInStockWatch(product)
                                                        Toast.makeText(context, if (watching) "Restock alert set!" else "Restock alert removed", Toast.LENGTH_SHORT).show()
                                                    },
                                                    isCatalogLocked = isLocked,
                                                    isDeliveredOrdered = isDeliveredOrdered,
                                                    modifier = Modifier.width(168.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val gridColumns = when {
                        maxWidth >= 920.dp -> 4
                        maxWidth >= 600.dp -> 3
                        else -> 2
                    }

                    // Main Catalog Grid (Adaptive columns + Horizontal Featured Row)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(gridColumns),
                        state = gridState,
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // ODIYA Official Installed Crest
                        if (searchQuery.isEmpty() && selectedCategory == null) {
                            item(span = { GridItemSpan(gridColumns) }) {
                                ShopEmblemWatermark(
                                    onOpenQrShare = { showQrShareDialog = true },
                                    storeName = storeProfile.storeName,
                                    tagline = storeProfile.tagline,
                                    storePhone = storeProfile.phoneNumber,
                                    secondaryPhone = storeProfile.secondaryPhoneNumber,
                                    storeAddress = storeProfile.storeAddress
                                )
                            }
                        }

                        // Active Admin Promo Discount Banner
                        if (adminPromo.isActive) {
                            item(span = { GridItemSpan(gridColumns) }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(16.dp))
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
                                            BorderStroke(1.5.dp, Color(0xFFFDE047)),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .testTag("active_admin_promo_banner")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = Color.White,
                                                shadowElevation = 4.dp
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.LocalFireDepartment,
                                                        contentDescription = null,
                                                        tint = Color(0xFFDC2626),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text(
                                                        text = "ODIYA",
                                                        color = Color(0xFFDC2626),
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "🔥 ${adminPromo.title.uppercase()} 🔥",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 13.sp,
                                                    letterSpacing = 0.5.sp
                                                )
                                                val categoryText = if (adminPromo.targetCategory == null) "STOREWIDE ON ALL ITEMS" else "ON ${adminPromo.targetCategory!!.uppercase()}"
                                                Text(
                                                    text = "ADMIN SPECIAL OFFER: ENJOY ${adminPromo.discountPercent}%- DISCOUNT $categoryText!",
                                                    color = Color.White.copy(alpha = 0.95f),
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                val remMs = (adminPromo.expiresAtMillis - nowMillis).coerceAtLeast(0L)
                                                if (remMs > 0L) {
                                                    val hrs = remMs / 3600000L
                                                    val mins = (remMs % 3600000L) / 60000L
                                                    val secs = (remMs % 60000L) / 1000L
                                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                                        Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFFFDE047), modifier = Modifier.size(12.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = String.format(Locale.US, "Ends in %02d:%02d:%02d", hrs, mins, secs),
                                                            color = Color(0xFFFDE047),
                                                            fontSize = 10.5.sp,
                                                            fontWeight = FontWeight.Black
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Business Partnership Portal Entry Banner
                        item(span = { GridItemSpan(gridColumns) }) {
                            Surface(
                                onClick = { viewModel.navigateTo(AppScreen.PARTNER_PORTAL) },
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF065F46),
                                border = BorderStroke(1.2.dp, AccentGold.copy(alpha = 0.7f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("home_partner_portal_banner")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Default.Handshake, contentDescription = null, tint = AccentGold, modifier = Modifier.size(22.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Partner With ${storeProfile.storeName}",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 12.5.sp,
                                                color = AccentGold
                                            )
                                            Text(
                                                text = "Sign MOU & host categorized products in your Partner Folder",
                                                fontSize = 10.5.sp,
                                                color = Color.White.copy(alpha = 0.9f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = AccentGold
                                    ) {
                                        Text(
                                            text = "OPEN",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 10.5.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Horizontal Featured Strip inside vertical grid (Dual-axis browsing out of the box)
                        if (products.isNotEmpty() && catalogBrowseMode == CatalogBrowseMode.CAROUSEL_AND_GRID) {
                            val horizontalSpotlight = products.take(12)
                            item(span = { GridItemSpan(gridColumns) }) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Featured Showcase",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Black,
                                            color = AccentGold
                                        )
                                        Text(
                                            text = "Swipe ↔ Horizontal",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CyberCyan
                                        )
                                    }
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        contentPadding = PaddingValues(end = 6.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("home_horizontal_showcase_row")
                                    ) {
                                        lazyListItems(
                                            items = horizontalSpotlight,
                                            key = { "spot_${it.id}" },
                                            contentType = { "product_card" }
                                        ) { product ->
                                            val effectiveDiscount = viewModel.getEffectiveDiscountPercent(product.category)
                                            val isLocked = product.isCatalogLocked || (product.partnerId.isNotBlank() && product.partnerId in lockedPartnerIds)
                                            val isDeliveredOrdered = remember(product.title, allOrders) {
                                                allOrders.any { it.status == com.example.data.model.OrderStatus.DELIVERED && it.itemsSummary.contains(product.title, ignoreCase = true) }
                                            }
                                            ProductStandardCard(
                                                product = product,
                                                promoDiscountPercent = effectiveDiscount,
                                                onClick = { stableOpenProductDetails(product) },
                                                onAddToCart = stableAddToCart,
                                                isWishlisted = wishlistSet.contains(product.id),
                                                onToggleWishlist = { stableToggleWishlist(product.id) },
                                                isRestockWatched = backInStockIds.contains(product.id),
                                                onToggleRestockWatch = {
                                                    val watching = viewModel.toggleBackInStockWatch(product)
                                                    Toast.makeText(context, if (watching) "Restock alert set!" else "Restock alert removed", Toast.LENGTH_SHORT).show()
                                                },
                                                isCatalogLocked = isLocked,
                                                isDeliveredOrdered = isDeliveredOrdered,
                                                modifier = Modifier.width(166.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Products Header
                        item(span = { GridItemSpan(gridColumns) }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedCategory ?: "Product Catalog",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = if (effectiveWide) {
                                            "${products.size} Products • Pan Horizontal ↔ & Scroll Vertical ↕"
                                        } else {
                                            "${products.size} Products Available"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (effectiveWide) CyberCyan else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Empty state
                        if (products.isEmpty()) {
                            item(span = { GridItemSpan(gridColumns) }) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(40.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.LocalMall,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "No products match your filter.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(onClick = {
                                            viewModel.selectCategory(null)
                                            viewModel.setSearchQuery("")
                                            viewModel.setSortOption(CatalogSortOption.FEATURED)
                                            viewModel.setPriceRangeFilter(PriceRangeFilter.ALL)
                                        }) {
                                            Text("Reset Filters")
                                        }
                                    }
                                }
                            }
                        }

                        // Standard Free-Scrolling Product Cards
                        items(
                            items = products,
                            key = { "grid_${it.id}" },
                            contentType = { "product_card" }
                        ) { product ->
                            val effectiveDiscount = viewModel.getEffectiveDiscountPercent(product.category)
                            val isLocked = product.isCatalogLocked || (product.partnerId.isNotBlank() && product.partnerId in lockedPartnerIds)
                            val isDeliveredOrdered = remember(product.title, allOrders) {
                                allOrders.any { it.status == com.example.data.model.OrderStatus.DELIVERED && it.itemsSummary.contains(product.title, ignoreCase = true) }
                            }
                            ProductStandardCard(
                                product = product,
                                promoDiscountPercent = effectiveDiscount,
                                onClick = { stableOpenProductDetails(product) },
                                onAddToCart = stableAddToCart,
                                isWishlisted = wishlistSet.contains(product.id),
                                onToggleWishlist = { stableToggleWishlist(product.id) },
                                isRestockWatched = backInStockIds.contains(product.id),
                                onToggleRestockWatch = {
                                    val watching = viewModel.toggleBackInStockWatch(product)
                                    Toast.makeText(context, if (watching) "Restock alert set!" else "Restock alert removed", Toast.LENGTH_SHORT).show()
                                },
                                isCatalogLocked = isLocked,
                                isDeliveredOrdered = isDeliveredOrdered
                            )
                        }
                    }
                    }
                }
            }
        }

        // --- FULL SCREEN EXIT OVERLAY ---
        if (isFullScreen) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Surface(
                    onClick = { viewModel.toggleFullScreen() },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.5f),
                    contentColor = Color.White,
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FullscreenExit,
                            contentDescription = "Exit Full Screen",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("EXIT FULL SCREEN", fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        if (showQrShareDialog) {
            ShareAppDialog(onDismiss = { showQrShareDialog = false }, viewModel = viewModel)
        }
    }
}

@Composable
private fun Hero3DBanner(
    onExploreNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onExploreNow),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF052E16),
                            Color(0xFF065F46),
                            Color(0xFF16A34A)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth(0.65f)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CyberCyan.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "NEW 3D SPATIAL CATALOG",
                        color = CyberCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Interactive 360° Shopping",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Touch and rotate products in full 3D space with instant offline settlement.",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onExploreNow,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberCyan,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Explore 3D", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            // Decorative 3D badge icon on right
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(90.dp)
                    .align(Alignment.CenterEnd)
                    .graphicsLayer {
                        rotationZ = 12f
                    }
                    .background(
                        Color.White.copy(alpha = 0.12f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.ViewInAr,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(54.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DropdownSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    recentSearches: List<String> = emptyList(),
    onRecordSearch: (String) -> Unit = {},
    onClearRecentSearches: () -> Unit = {},
    onCollapse: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)
        ),
        border = BorderStroke(
            1.dp,
            if (searchQuery.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .animateContentSize()
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🔍 SEARCH CATALOG",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    ),
                    color = AccentGold
                )
                Surface(
                    onClick = onCollapse,
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "HIDE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Hide Search",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Type brand, item, or keyword...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("catalog_dropdown_search_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Recent Search History
            if (recentSearches.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Searches:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Clear",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        modifier = Modifier.clickable { onClearRecentSearches() }
                    )
                }
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    recentSearches.forEach { term ->
                        AssistChip(
                            onClick = {
                                onSearchQueryChange(term)
                                onRecordSearch(term)
                                onCollapse()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(13.dp))
                            },
                            label = { Text(term, fontSize = 10.sp) },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }

            // Quick Search Tags
            Text(
                text = "Popular Searches:",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val tags = listOf("3D Sneaker", "Nike", "Leather", "Gucci", "Cold Brew", "Smart", "Perfume", "Jacket")
                tags.forEach { tag ->
                    AssistChip(
                        onClick = {
                            onSearchQueryChange(tag)
                            onRecordSearch(tag)
                            onCollapse()
                        },
                        label = { Text(tag, fontSize = 10.sp) },
                        modifier = Modifier.height(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DigitalClock(modifier: Modifier = Modifier) {
    var time by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            val now = java.util.Calendar.getInstance().time
            time = java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault()).format(now)
            date = java.text.SimpleDateFormat("EEE, dd MMM", java.util.Locale.getDefault()).format(now)
            kotlinx.coroutines.delay(1000L) // Live second-by-second digital clock
        }
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Text(
            text = time,
            fontSize = 13.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Black,
            color = AccentGold,
            letterSpacing = 0.5.sp,
            maxLines = 1
        )
        Text(
            text = date,
            fontSize = 9.5.sp,
            lineHeight = 11.sp,
            fontWeight = FontWeight.Bold,
            color = CyberCyan,
            maxLines = 1
        )
    }
}

