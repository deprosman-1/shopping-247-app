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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import android.view.WindowManager
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.util.AppDefenseManager
import com.example.util.ShopNotificationHelper
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CloudOff
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import com.example.ui.components.AppUpdateDialog
import com.example.ui.components.ReceiptViewDialog
import com.example.ui.screens.AdminControlRoomScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CartScreen
import com.example.ui.screens.CheckoutScreen
import com.example.ui.screens.HelpSupportScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LiveCallScreen
import com.example.ui.screens.OrdersReceiptsScreen
import com.example.ui.screens.PartnerPortalScreen
import com.example.ui.screens.ProductDetailScreen
import com.example.ui.screens.WishlistScreen
import com.example.data.remote.LiveCallMode
import com.example.data.remote.LiveCallPhase
import com.example.ui.theme.AccentGold
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.CatalogBackgroundSyncJobService

data class RawSharedReceiptPayload(
    val streamUris: List<android.net.Uri>,
    val mimeType: String?,
    val sharedText: String?,
    val sharedSubject: String?,
    val receivedAtMillis: Long = System.currentTimeMillis()
)

class MainActivity : ComponentActivity() {
    private val pendingCloudCredentials = kotlinx.coroutines.flow.MutableStateFlow<Pair<String, String>?>(null)
    private val pendingSharedReceiptPayload = kotlinx.coroutines.flow.MutableStateFlow<RawSharedReceiptPayload?>(null)
    private val pendingPaystackReference = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppDefenseManager.installCrashGuard(applicationContext)
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
                DeprosApp(
                    pendingCloudCredentials = pendingCloudCredentials,
                    pendingSharedReceiptPayload = pendingSharedReceiptPayload,
                    pendingPaystackReference = pendingPaystackReference
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkIntent(intent)
    }

    @Suppress("DEPRECATION")
    private fun extractStreamUris(intent: android.content.Intent): List<android.net.Uri> {
        val result = mutableListOf<android.net.Uri>()
        if (intent.action == android.content.Intent.ACTION_SEND_MULTIPLE) {
            val list: ArrayList<android.net.Uri>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, android.net.Uri::class.java)
            } else {
                intent.getParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM)
            }
            list?.forEach { if (!result.contains(it)) result.add(it) }
        } else {
            val single: android.net.Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(android.content.Intent.EXTRA_STREAM, android.net.Uri::class.java)
            } else {
                intent.getParcelableExtra(android.content.Intent.EXTRA_STREAM)
            }
            if (single != null) result.add(single)
        }
        val clip = intent.clipData
        if (clip != null) {
            for (i in 0 until clip.itemCount) {
                val clipUri = clip.getItemAt(i)?.uri
                if (clipUri != null && !result.contains(clipUri)) {
                    result.add(clipUri)
                }
            }
        }
        return result
    }

    private fun handleDeepLinkIntent(intent: android.content.Intent?) {
        if (intent == null) return
        val action = intent.action
        if (action == android.content.Intent.ACTION_SEND || action == android.content.Intent.ACTION_SEND_MULTIPLE) {
            val uris = extractStreamUris(intent)
            val sharedText = intent.getStringExtra(android.content.Intent.EXTRA_TEXT)
            val sharedSubject = intent.getStringExtra(android.content.Intent.EXTRA_SUBJECT)
            if (uris.isNotEmpty() || !sharedText.isNullOrBlank()) {
                pendingSharedReceiptPayload.value = RawSharedReceiptPayload(
                    streamUris = uris,
                    mimeType = intent.type,
                    sharedText = sharedText,
                    sharedSubject = sharedSubject,
                    receivedAtMillis = System.currentTimeMillis()
                )
            }
            return
        }

        val data = intent.data
        if (data != null) {
            val isPaystackCallback = data.host.equals("paystack-callback", ignoreCase = true) ||
                data.path?.contains("paystack-callback", ignoreCase = true) == true
            val paystackRef = data.getQueryParameter("reference")?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: data.getQueryParameter("trxref")?.trim()
            if (isPaystackCallback && !paystackRef.isNullOrBlank()) {
                pendingPaystackReference.value = paystackRef
                return
            }

            val sbUrl = data.getQueryParameter("sb_url")?.trim()
            val sbKey = data.getQueryParameter("sb_key")?.trim()
            if (!sbUrl.isNullOrBlank() && !sbKey.isNullOrBlank()) {
                pendingCloudCredentials.value = sbUrl to sbKey
                android.widget.Toast.makeText(
                    this,
                    "🔗 Linking Odiya to Store Live Cloud...",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            } else {
                android.widget.Toast.makeText(
                    this,
                    "Welcome to Odiya! App Opened via direct link",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

@Composable
fun DeprosApp(
    viewModel: MainViewModel = viewModel(),
    pendingCloudCredentials: kotlinx.coroutines.flow.MutableStateFlow<Pair<String, String>?>? = null,
    pendingSharedReceiptPayload: kotlinx.coroutines.flow.MutableStateFlow<RawSharedReceiptPayload?>? = null,
    pendingPaystackReference: kotlinx.coroutines.flow.MutableStateFlow<String?>? = null
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
    val liveCallState by viewModel.liveCallState.collectAsState()
    val updateAvailable by viewModel.updateAvailable.collectAsState()
    val latestVersionName by viewModel.latestVersionName.collectAsState()
    val releaseNotes by viewModel.releaseNotes.collectAsState()
    val updateDownloadUrl by viewModel.updateDownloadUrl.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val offlineQueue by viewModel.offlineQueue.collectAsState()

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

    var showFullScreenBrandSplash by rememberSaveable { mutableStateOf(true) }

    val deepLinkCreds = pendingCloudCredentials?.collectAsState()?.value
    LaunchedEffect(deepLinkCreds) {
        if (deepLinkCreds != null) {
            showFullScreenBrandSplash = false
            val (u, k) = deepLinkCreds
            pendingCloudCredentials?.value = null
            viewModel.applyStoreSyncLinkOrCredentials(u, k) { _, msg ->
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    val incomingReceiptPayload = pendingSharedReceiptPayload?.collectAsState()?.value
    LaunchedEffect(incomingReceiptPayload) {
        if (incomingReceiptPayload != null) {
            showFullScreenBrandSplash = false
            pendingSharedReceiptPayload?.value = null
            viewModel.handleIncomingSharedReceipt(
                context = context,
                streamUris = incomingReceiptPayload.streamUris,
                mimeTypeHint = incomingReceiptPayload.mimeType,
                sharedText = incomingReceiptPayload.sharedText,
                sharedSubject = incomingReceiptPayload.sharedSubject
            ) { msg ->
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    val incomingPaystackRef = pendingPaystackReference?.collectAsState()?.value
    LaunchedEffect(incomingPaystackRef) {
        if (!incomingPaystackRef.isNullOrBlank()) {
            showFullScreenBrandSplash = false
            val refToVerify = incomingPaystackRef
            pendingPaystackReference?.value = null
            viewModel.verifyPaystackPayment(reference = refToVerify) { _, msg ->
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(showFullScreenBrandSplash) {
        if (showFullScreenBrandSplash) {
            delay(2200L)
            showFullScreenBrandSplash = false
        }
    }

    val view = LocalView.current
    val effectiveImmersiveFullScreen = isFullScreen || showFullScreenBrandSplash
    LaunchedEffect(effectiveImmersiveFullScreen) {
        val window = (view.context as? ComponentActivity)?.window ?: return@LaunchedEffect
        val insetsController = WindowCompat.getInsetsController(window, view)
        if (effectiveImmersiveFullScreen) {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Dynamic anti-spying & screen-capture shield on sensitive payment, auth & Admin screens
    DisposableEffect(currentScreen) {
        val activity = view.context as? ComponentActivity
        val protectScreen = currentScreen == AppScreen.ADMIN_CONTROL_ROOM ||
            currentScreen == AppScreen.CHECKOUT ||
            currentScreen == AppScreen.AUTH ||
            currentScreen == AppScreen.PARTNER_PORTAL
        runCatching {
            if (protectScreen) {
                activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
        onDispose { }
    }

    // Handle system back button smoothly
    BackHandler(enabled = currentScreen != AppScreen.HOME) {
        viewModel.navigateBack()
    }

    Scaffold(
        contentWindowInsets = if (isFullScreen) {
            WindowInsets(0, 0, 0, 0)
        } else {
            WindowInsets.statusBars.union(WindowInsets.displayCutout)
        },
        bottomBar = {
            // Show bottom navigation bar on primary destinations
            if (currentScreen != AppScreen.PRODUCT_DETAIL &&
                currentScreen != AppScreen.CHECKOUT &&
                currentScreen != AppScreen.PARTNER_PORTAL &&
                currentScreen != AppScreen.HELP_SUPPORT &&
                currentScreen != AppScreen.LIVE_CALL &&
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
                        onClick = { viewModel.openCustomerAccountShortcut() },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!isOnline && currentScreen != AppScreen.HOME) {
                Surface(
                    color = Color(0xFF78350F),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentGold.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("global_offline_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "No Internet Connection",
                            tint = AccentGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (offlineQueue.isNotEmpty()) {
                                "No Internet Connection • ${offlineQueue.size} action(s) queued to auto-sync"
                            } else {
                                "No Internet Connection • Browsing cached store data"
                            },
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
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
                    AppScreen.LIVE_CALL -> LiveCallScreen(viewModel = viewModel)
                }

            // Floating Incoming Customer Live Call Alert for Admin across all screens
            val pendingRing = liveCallState.incomingRequests.firstOrNull()
            if (showAdminControl &&
                pendingRing != null &&
                liveCallState.phase == LiveCallPhase.IDLE &&
                currentScreen != AppScreen.LIVE_CALL
            ) {
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                    color = Color(0xFF052E16),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, AccentGold),
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.TopCenter)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("global_incoming_call_banner")
                ) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Incoming ${pendingRing.mode.label} • ${pendingRing.callerName}",
                            color = AccentGold,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Topic: ${pendingRing.topic}",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                        androidx.compose.foundation.layout.Row(
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                        ) {
                            androidx.compose.material3.Button(
                                onClick = { viewModel.answerIncomingLiveCall(pendingRing, LiveCallMode.AUDIO) },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Answer Audio", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                            androidx.compose.material3.Button(
                                onClick = { viewModel.answerIncomingLiveCall(pendingRing, LiveCallMode.VIDEO) },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color(0xFF052E16)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Answer Video", fontSize = 11.5.sp, fontWeight = FontWeight.Black)
                            }
                            androidx.compose.material3.OutlinedButton(
                                onClick = { viewModel.declineIncomingLiveCall(pendingRing) },
                                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFCA5A5))
                            ) {
                                Text("Decline", fontSize = 11.5.sp)
                            }
                        }
                    }
                }
            }

            // Global Official Receipt Dialog Popup
            activeReceipt?.let { receiptOrder ->
                ReceiptViewDialog(
                    order = receiptOrder,
                    storeProfile = storeProfile,
                    onDismiss = { viewModel.dismissReceipt() }
                )
            }

            // Global Live App Update Prompt (appears immediately when Admin publishes a newer APK)
            if (updateAvailable) {
                val installedVer = remember(viewModel) { viewModel.getInstalledAppVersion().second }
                AppUpdateDialog(
                    latestVersionName = latestVersionName,
                    releaseNotes = releaseNotes,
                    updateUrl = updateDownloadUrl,
                    installedVersionName = installedVer,
                    onDismiss = { viewModel.dismissUpdateDialog() }
                )
            }
            }
        }

        // Full-Screen "ODIYA" Brand Launch Overlay (Edge-to-Edge Immersive View)
        AnimatedVisibility(
            visible = showFullScreenBrandSplash,
            exit = fadeOut(animationSpec = tween(durationMillis = 380))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showFullScreenBrandSplash = false }
                    .testTag("fullscreen_brand_splash")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF022C22),
                                Color(0xFF052E16),
                                Color(0xFF064E3B),
                                Color(0xFF022C22)
                            )
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                AccentGold.copy(alpha = 0.22f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.5f, size.height * 0.42f),
                            radius = size.minDimension * 0.65f
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.42f),
                        radius = size.minDimension * 0.65f
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            color = Color(0xFF064E3B),
                            border = androidx.compose.foundation.BorderStroke(2.dp, AccentGold),
                            shadowElevation = 12.dp,
                            modifier = Modifier.size(104.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Image(
                                    painter = painterResource(id = R.drawable.shopping_logo_icon_1794752250458),
                                    contentDescription = "ODIYA Official Brand Crest",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                )
                            }
                        }

                        Text(
                            text = "ODIYA",
                            fontSize = 36.sp,
                            lineHeight = 40.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 3.sp,
                            color = AccentGold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("fullscreen_brand_title")
                        )

                        Text(
                            text = storeProfile.tagline.ifBlank { "Shopping with ease and comfort" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.92f),
                            textAlign = TextAlign.Center
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = AccentGold.copy(alpha = 0.16f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentGold.copy(alpha = 0.7f))
                    ) {
                        Text(
                            text = "OPENING STOREFRONT • TAP TO ENTER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = AccentGold,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}
