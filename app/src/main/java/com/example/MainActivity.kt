package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.util.ShopNotificationHelper
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.ReceiptViewDialog
import com.example.ui.screens.AdminControlRoomScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CartScreen
import com.example.ui.screens.CheckoutScreen
import com.example.ui.screens.HelpSupportScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OrdersReceiptsScreen
import com.example.ui.screens.PartnerPortalScreen
import com.example.ui.screens.ProductDetailScreen
import com.example.ui.screens.WishlistScreen
import com.example.ui.theme.AccentGold
import com.example.ui.theme.MyApplicationTheme
import com.example.util.CatalogBackgroundSyncJobService

class MainActivity : ComponentActivity() {
    private val pendingCloudCredentials = kotlinx.coroutines.flow.MutableStateFlow<Pair<String, String>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val coilOkHttp = okhttp3.OkHttpClient.Builder()
            .dispatcher(
                okhttp3.Dispatcher().apply {
                    maxRequests = 64
                    maxRequestsPerHost = 16
                }
            )
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        coil.Coil.setImageLoader(
            coil.ImageLoader.Builder(applicationContext)
                .okHttpClient(coilOkHttp)
                .respectCacheHeaders(false)
                .bitmapFactoryMaxParallelism(4)
                .memoryCache {
                    coil.memory.MemoryCache.Builder(applicationContext)
                        .maxSizePercent(0.30)
                        .build()
                }
                .diskCache {
                    coil.disk.DiskCache.Builder()
                        .directory(applicationContext.cacheDir.resolve("coil_img_cache"))
                        .maxSizeBytes(160L * 1024L * 1024L)
                        .build()
                }
                .allowRgb565(false)
                .crossfade(false)
                .build()
        )
        CatalogBackgroundSyncJobService.schedulePeriodicSync(applicationContext)
        handleDeepLinkIntent(intent)
        setContent {
            MyApplicationTheme {
                DeprosApp(pendingCloudCredentials = pendingCloudCredentials)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkIntent(intent)
    }

    private fun handleDeepLinkIntent(intent: android.content.Intent?) {
        val data = intent?.data
        if (data != null) {
            val sbUrl = data.getQueryParameter("sb_url")?.trim()
            val sbKey = data.getQueryParameter("sb_key")?.trim()
            if (!sbUrl.isNullOrBlank() && !sbKey.isNullOrBlank()) {
                pendingCloudCredentials.value = sbUrl to sbKey
                android.widget.Toast.makeText(
                    this,
                    "🔗 Linking 24/7 Shopping to Store Live Cloud...",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            } else {
                android.widget.Toast.makeText(
                    this,
                    "Welcome to 24/7 Shopping! App Opened via direct link",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

@Composable
fun DeprosApp(
    viewModel: MainViewModel = viewModel(),
    pendingCloudCredentials: kotlinx.coroutines.flow.MutableStateFlow<Pair<String, String>?>? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsState()
    val cartCount by viewModel.cartTotalCount.collectAsState()
    val wishlistCount by viewModel.wishlistCount.collectAsState()
    val activeReceipt by viewModel.activeReceipt.collectAsState()
    val isFullScreen by viewModel.isFullScreen.collectAsState()
    val storeProfile by viewModel.storeProfile.collectAsState()
    val currentCustomer by viewModel.currentCustomer.collectAsState()
    val isAdminUnlocked by viewModel.isAdminUnlocked.collectAsState()
    val showAdminControl = isAdminUnlocked || (currentCustomer?.isAdmin == true)

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        ShopNotificationHelper.ensureChannels(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val deepLinkCreds = pendingCloudCredentials?.collectAsState()?.value
    LaunchedEffect(deepLinkCreds) {
        if (deepLinkCreds != null) {
            val (u, k) = deepLinkCreds
            pendingCloudCredentials?.value = null
            viewModel.applyStoreSyncLinkOrCredentials(u, k) { _, msg ->
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    val view = LocalView.current
    LaunchedEffect(isFullScreen) {
        val window = (view.context as ComponentActivity).window
        val insetsController = WindowCompat.getInsetsController(window, view)
        if (isFullScreen) {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Handle system back button smoothly
    BackHandler(enabled = currentScreen != AppScreen.HOME) {
        viewModel.navigateBack()
    }

    Scaffold(
        contentWindowInsets = if (isFullScreen) WindowInsets(0, 0, 0, 0) else WindowInsets.statusBars,
        bottomBar = {
            // Show bottom navigation bar on primary destinations
            if (currentScreen != AppScreen.PRODUCT_DETAIL &&
                currentScreen != AppScreen.CHECKOUT &&
                currentScreen != AppScreen.PARTNER_PORTAL &&
                currentScreen != AppScreen.HELP_SUPPORT &&
                !isFullScreen
            ) {
                NavigationBar(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("bottom_nav_bar"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    // Home
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.HOME,
                        onClick = { viewModel.navigateTo(AppScreen.HOME) },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == AppScreen.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = "Home"
                            )
                        },
                        label = { Text("Home", fontWeight = if (currentScreen == AppScreen.HOME) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("nav_home")
                    )

                    // Wishlist
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.WISHLIST,
                        onClick = { viewModel.navigateTo(AppScreen.WISHLIST) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (wishlistCount > 0) {
                                        Badge(
                                            containerColor = Color(0xFFEF4444),
                                            contentColor = Color.White
                                        ) {
                                            Text(text = wishlistCount.toString(), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (currentScreen == AppScreen.WISHLIST) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Wishlist",
                                    tint = if (currentScreen == AppScreen.WISHLIST) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        label = { Text("Saved", fontWeight = if (currentScreen == AppScreen.WISHLIST) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("nav_wishlist")
                    )

                    // Cart
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.CART,
                        onClick = { viewModel.navigateTo(AppScreen.CART) },
                        icon = {
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
                                    imageVector = if (currentScreen == AppScreen.CART) Icons.Filled.ShoppingCart else Icons.Outlined.ShoppingCart,
                                    contentDescription = "Cart"
                                )
                            }
                        },
                        label = { Text("Cart", fontWeight = if (currentScreen == AppScreen.CART) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("nav_cart")
                    )

                    // Orders & Receipts
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.ORDERS,
                        onClick = { viewModel.navigateTo(AppScreen.ORDERS) },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == AppScreen.ORDERS) Icons.Filled.ReceiptLong else Icons.Outlined.ReceiptLong,
                                contentDescription = "Receipts"
                            )
                        },
                        label = { Text("Receipts", fontWeight = if (currentScreen == AppScreen.ORDERS) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("nav_orders")
                    )

                    // Account
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.AUTH,
                        onClick = { viewModel.navigateTo(AppScreen.AUTH) },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == AppScreen.AUTH) Icons.Filled.Person else Icons.Outlined.Person,
                                contentDescription = "Account"
                            )
                        },
                        label = { Text("Account", fontWeight = if (currentScreen == AppScreen.AUTH) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("nav_account")
                    )

                    // Admin Control Room Shortcut (Hidden from customers; shown only on Admin-unlocked/authorized devices)
                    if (showAdminControl) {
                        NavigationBarItem(
                            selected = currentScreen == AppScreen.ADMIN_CONTROL_ROOM,
                            onClick = {
                                viewModel.navigateTo(AppScreen.ADMIN_CONTROL_ROOM)
                            },
                            icon = {
                                Icon(
                                    imageVector = if (currentScreen == AppScreen.ADMIN_CONTROL_ROOM) Icons.Filled.AdminPanelSettings else Icons.Outlined.AdminPanelSettings,
                                    contentDescription = "Admin Control Room Shortcut",
                                    tint = if (currentScreen == AppScreen.ADMIN_CONTROL_ROOM) AccentGold else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            label = { Text("Control", fontWeight = if (currentScreen == AppScreen.ADMIN_CONTROL_ROOM) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AccentGold,
                                selectedTextColor = AccentGold
                            ),
                            modifier = Modifier.testTag("nav_admin")
                        )
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.HOME -> HomeScreen(viewModel = viewModel)
                AppScreen.PRODUCT_DETAIL -> ProductDetailScreen(viewModel = viewModel)
                AppScreen.CART -> CartScreen(viewModel = viewModel)
                AppScreen.CHECKOUT -> CheckoutScreen(viewModel = viewModel)
                AppScreen.ORDERS -> OrdersReceiptsScreen(viewModel = viewModel)
                AppScreen.AUTH -> AuthScreen(viewModel = viewModel)
                AppScreen.WISHLIST -> WishlistScreen(viewModel = viewModel)
                AppScreen.ADMIN_CONTROL_ROOM -> AdminControlRoomScreen(viewModel = viewModel)
                AppScreen.PARTNER_PORTAL -> PartnerPortalScreen(viewModel = viewModel)
                AppScreen.HELP_SUPPORT -> HelpSupportScreen(viewModel = viewModel)
            }

            // Global Official Receipt Dialog Popup
            activeReceipt?.let { receiptOrder ->
                ReceiptViewDialog(
                    order = receiptOrder,
                    storeProfile = storeProfile,
                    onDismiss = { viewModel.dismissReceipt() }
                )
            }
        }
    }
}
