package com.example

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AdminBargainConfig
import com.example.data.model.AppUpdateManifest
import com.example.data.model.CartItem
import com.example.data.model.CatalogBrowseMode
import com.example.data.model.CatalogSortOption
import com.example.data.model.CreditVerificationStatus
import com.example.data.model.Customer
import com.example.data.model.CustomerDeliveryInfo
import com.example.data.model.DeliveryOption
import com.example.data.model.DeliveryZoneOption
import com.example.data.model.HostingPeriodUnit
import com.example.data.model.LoyaltyAccount
import com.example.data.model.OfflineActionType
import com.example.data.model.OfflineQueuedAction
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.PartnerAgreementStatus
import com.example.data.model.PartnerFolderTransaction
import com.example.data.model.PartnerOrganization
import com.example.data.model.PartnerProductSubmission
import com.example.data.model.PartnerSubmissionStatus
import com.example.data.model.PartnerTransactionType
import com.example.data.model.PaymentMethod
import com.example.data.model.PriceRangeFilter
import com.example.data.model.Product
import com.example.data.model.ProductColor
import com.example.data.model.ProductReview
import com.example.data.model.PromoCoupon
import com.example.data.model.ReturnRefundRequest
import com.example.data.model.ReturnRefundStatus
import com.example.data.model.SavedAddress
import com.example.data.model.StoreProfileConfig
import com.example.data.model.SupabaseConfig
import com.example.data.model.SupportTicket
import com.example.data.model.SupportTicketStatus
import com.example.data.remote.LiveCallMessage
import com.example.data.remote.LiveCallMode
import com.example.data.remote.LiveCallPhase
import com.example.data.remote.LiveCallRequest
import com.example.data.remote.LiveCallSignalingClient
import com.example.data.remote.LiveCallUiState
import com.example.data.repository.EcommerceRepository
import com.example.util.CatalogBackgroundSyncJobService
import com.example.util.DeliveryZoneHelper
import com.example.util.OfflineConnectivityManager
import com.example.util.PendingSharedReceipt
import com.example.util.SharedReceiptHelper
import com.example.util.ShopNotificationHelper
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppScreen {
    HOME,
    PRODUCT_DETAIL,
    CART,
    CHECKOUT,
    ORDERS,
    AUTH,
    WISHLIST,
    ADMIN_CONTROL_ROOM,
    PARTNER_PORTAL,
    HELP_SUPPORT,
    LIVE_CALL
}

enum class CustomerAccountDestination {
    MENU,
    WELCOME_PROFILE,
    INBOX,
    ORDER_TRACKING,
    RATINGS_AND_REVIEWS,
    VOUCHERS,
    FOLLOW_STORE
}

enum class OrderTrackingFilter(val label: String) {
    ALL("All"),
    ACTIVE("In Progress"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled")
}

data class OrderTrackingUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val isLiveSynced: Boolean = false,
    val lastSyncedAtMillis: Long = 0L,
    val statusBanner: String = "",
    val errorMessage: String? = null,
    val selectedFilter: OrderTrackingFilter = OrderTrackingFilter.ALL,
    val focusedOrderId: String? = null,
    val searchedOrders: List<Order> = emptyList(),
    val hasActiveSearch: Boolean = false
)

data class AdminPromoConfig(
    val isActive: Boolean = false,
    val discountPercent: Int = 10,
    val title: String = "ODIYA Special Promo",
    val targetCategory: String? = null, // null indicates storewide across all categories
    val expiresAtMillis: Long = 0L
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val repository = EcommerceRepository(application, viewModelScope)
    val voiceManager = com.example.util.WelcomeVoiceManager(application)
    val paystackGateway = com.example.data.remote.PaystackGatewayService()
    private val adminPrefs = getApplication<Application>().getSharedPreferences("depros_admin_settings", Context.MODE_PRIVATE)
    private val _paystackCheckoutState = MutableStateFlow(com.example.data.remote.PaystackCheckoutUiState())
    val paystackCheckoutState: StateFlow<com.example.data.remote.PaystackCheckoutUiState> = _paystackCheckoutState.asStateFlow()

    fun playWelcomeAudio() {
        voiceManager.speakAppWelcome()
    }

    fun playSignInAudio(customerName: String) {
        voiceManager.speakCustomerSignIn(customerName)
    }

    // Current Screen Navigation
    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Screen back stack
    private val screenStack = mutableListOf<AppScreen>()

    // Current logged-in Customer
    private val _currentCustomer = MutableStateFlow<Customer?>(null)
    val currentCustomer: StateFlow<Customer?> = _currentCustomer.asStateFlow()

    // Direct OPay / Moniepoint / Banking App Shared Invoice Receipt
    private val _pendingSharedReceipt = MutableStateFlow<PendingSharedReceipt?>(null)
    val pendingSharedReceipt: StateFlow<PendingSharedReceipt?> = _pendingSharedReceipt.asStateFlow()

    // Products & Filtering
    val allProducts: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(CatalogSortOption.FEATURED)
    val sortOption: StateFlow<CatalogSortOption> = _sortOption.asStateFlow()

    private val _priceRangeFilter = MutableStateFlow(PriceRangeFilter.ALL)
    val priceRangeFilter: StateFlow<PriceRangeFilter> = _priceRangeFilter.asStateFlow()

    private val _inStockOnly = MutableStateFlow(false)
    val inStockOnly: StateFlow<Boolean> = _inStockOnly.asStateFlow()

    private val _selectedBrandFilter = MutableStateFlow<String?>(null)
    val selectedBrandFilter: StateFlow<String?> = _selectedBrandFilter.asStateFlow()

    val availableBrandFilters: StateFlow<List<String>> = combine(allProducts, _selectedCategory) { products, cat ->
        products
            .filter { cat == null || it.category.equals(cat, ignoreCase = true) }
            .map { it.brand.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
            .take(16)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val baseFilteredFlow = combine(
        allProducts,
        _selectedCategory,
        _searchQuery,
        _sortOption,
        _priceRangeFilter
    ) { products, cat, query, sortOpt, priceFilter ->
        Triple(products, Triple(cat, query, sortOpt), priceFilter)
    }

    val filteredProducts: StateFlow<List<Product>> = combine(
        baseFilteredFlow,
        _inStockOnly,
        _selectedBrandFilter
    ) { base, inStock, brandFilter ->
        val products = base.first
        val (cat, query, sortOpt) = base.second
        val priceFilter = base.third
        val filtered = products.filter { product ->
            val matchesCategory = cat == null || product.category.equals(cat, ignoreCase = true)
            val matchesQuery = query.isBlank() ||
                product.title.contains(query, ignoreCase = true) ||
                product.brand.contains(query, ignoreCase = true) ||
                product.category.contains(query, ignoreCase = true)
            val matchesStock = !inStock || product.stockQuantity > 0
            val matchesBrand = brandFilter == null || product.brand.equals(brandFilter, ignoreCase = true) ||
                product.availableBrands.any { it.equals(brandFilter, ignoreCase = true) }
            val effectiveDisc = getEffectiveDiscountPercent(product.category).let {
                if (it > 0) it else product.discountPercent
            }
            val effPrice = product.basePrice * (1.0 - (effectiveDisc / 100.0))
            val matchesMin = priceFilter.minPrice == null || effPrice >= priceFilter.minPrice
            val matchesMax = priceFilter.maxPrice == null || effPrice <= priceFilter.maxPrice
            matchesCategory && matchesQuery && matchesStock && matchesBrand && matchesMin && matchesMax
        }
        when (sortOpt) {
            CatalogSortOption.FEATURED -> filtered
            CatalogSortOption.NEWEST -> filtered.sortedByDescending { it.createdAt }
            CatalogSortOption.PRICE_LOW_HIGH -> filtered.sortedBy { p ->
                val d = getEffectiveDiscountPercent(p.category).let { if (it > 0) it else p.discountPercent }
                p.basePrice * (1.0 - (d / 100.0))
            }
            CatalogSortOption.PRICE_HIGH_LOW -> filtered.sortedByDescending { p ->
                val d = getEffectiveDiscountPercent(p.category).let { if (it > 0) it else p.discountPercent }
                p.basePrice * (1.0 - (d / 100.0))
            }
            CatalogSortOption.TOP_RATED -> filtered.sortedByDescending { it.rating }
            CatalogSortOption.ON_PROMO -> filtered.sortedByDescending { p ->
                val d = getEffectiveDiscountPercent(p.category).let { if (it > 0) it else p.discountPercent }
                d
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Product for single-click full view
    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct: StateFlow<Product?> = _selectedProduct.asStateFlow()

    // Product detail configured variants
    private val _detailBrand = MutableStateFlow("")
    val detailBrand: StateFlow<String> = _detailBrand.asStateFlow()

    private val _detailSize = MutableStateFlow("")
    val detailSize: StateFlow<String> = _detailSize.asStateFlow()

    private val _detailColor = MutableStateFlow(ProductColor("Default", "#1E40AF"))
    val detailColor: StateFlow<ProductColor> = _detailColor.asStateFlow()

    // Cart State
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    val cartTotalCount: StateFlow<Int> = combine(_cartItems) { items ->
        items.first().sumOf { it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val cartSubtotal: StateFlow<Double> = combine(_cartItems) { items ->
        items.first().sumOf { it.unitPrice * it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Per-Order ODIYA Credit-Alert Verification States (Persisted locally & overlaid on Room orders)
    private fun loadOrderCreditStates(): Map<String, Pair<Boolean, CreditVerificationStatus>> {
        val raw = adminPrefs.getString("order_credit_states_json", null) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            val result = mutableMapOf<String, Pair<Boolean, CreditVerificationStatus>>()
            obj.keys().forEach { ordId ->
                val item = obj.optJSONObject(ordId) ?: return@forEach
                val confirmed = item.optBoolean("confirmed", false)
                val statusName = item.optString("status", CreditVerificationStatus.PENDING_CREDIT.name)
                val status = CreditVerificationStatus.entries.firstOrNull { it.name == statusName }
                    ?: if (confirmed) CreditVerificationStatus.CONFIRMED_CREDITED else CreditVerificationStatus.PENDING_CREDIT
                result[ordId] = confirmed to status
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun persistOrderCreditStates(map: Map<String, Pair<Boolean, CreditVerificationStatus>>) {
        val root = JSONObject()
        map.forEach { (ordId, state) ->
            root.put(ordId, JSONObject().apply {
                put("confirmed", state.first)
                put("status", state.second.name)
            })
        }
        adminPrefs.edit().putString("order_credit_states_json", root.toString()).apply()
    }

    private val _orderCreditStates = MutableStateFlow(loadOrderCreditStates())

    private fun setOrderCreditState(
        orderId: String,
        confirmed: Boolean,
        status: CreditVerificationStatus
    ) {
        val updated = _orderCreditStates.value.toMutableMap()
        updated[orderId] = confirmed to status
        _orderCreditStates.value = updated
        persistOrderCreditStates(updated)
    }

    private fun enrichOrderWithCreditState(
        ord: Order,
        creditMap: Map<String, Pair<Boolean, CreditVerificationStatus>> = _orderCreditStates.value
    ): Order {
        val st = creditMap[ord.orderId]
        return when {
            st != null -> ord.copy(
                isCreditAlertConfirmed = st.first,
                creditVerificationStatus = st.second
            )
            ord.status == OrderStatus.CONFIRMED ||
                ord.status == OrderStatus.VERIFIED ||
                ord.status == OrderStatus.OUT_FOR_DELIVERY ||
                ord.status == OrderStatus.DELIVERED -> ord.copy(
                isCreditAlertConfirmed = true,
                creditVerificationStatus = CreditVerificationStatus.CONFIRMED_CREDITED
            )
            else -> ord
        }
    }

    // Orders & Receipts (Enriched with ODIYA Account Credit-Alert Verification Status)
    val allOrders: StateFlow<List<Order>> = combine(repository.allOrders, _orderCreditStates) { orders, creditMap ->
        orders.map { ord -> enrichOrderWithCreditState(ord, creditMap) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allCustomers: StateFlow<List<Customer>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently viewed receipt dialog
    private val _activeReceipt = MutableStateFlow<Order?>(null)
    val activeReceipt: StateFlow<Order?> = _activeReceipt.asStateFlow()

    // Cloud status & busy/connection state
    private val _cloudStatusMessage = MutableStateFlow<String?>(null)
    val cloudStatusMessage: StateFlow<String?> = _cloudStatusMessage.asStateFlow()

    private val _isSupabaseBusy = MutableStateFlow(false)
    val isSupabaseBusy: StateFlow<Boolean> = _isSupabaseBusy.asStateFlow()

    private val _isSupabaseConnected = MutableStateFlow(false)
    val isSupabaseConnected: StateFlow<Boolean> = _isSupabaseConnected.asStateFlow()

    private val _supabaseConfigState = MutableStateFlow(SupabaseConfig())
    val supabaseConfigState: StateFlow<SupabaseConfig> = _supabaseConfigState.asStateFlow()

    // App Update Notice State & Methods
    private val _updateAvailable = MutableStateFlow(false)
    val updateAvailable: StateFlow<Boolean> = _updateAvailable.asStateFlow()

    private val _latestVersionName = MutableStateFlow("1.0")
    val latestVersionName: StateFlow<String> = _latestVersionName.asStateFlow()

    private val _releaseNotes = MutableStateFlow(
        "New layout upgrades, performance boosts, and instant Supabase sync enhancements."
    )
    val releaseNotes: StateFlow<String> = _releaseNotes.asStateFlow()

    private val _updateDownloadUrl = MutableStateFlow("")
    val updateDownloadUrl: StateFlow<String> = _updateDownloadUrl.asStateFlow()

    fun getInstalledAppVersion(): Pair<Int, String> {
        val ctx = getApplication<Application>()
        return try {
            val info = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            @Suppress("DEPRECATION")
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode.toInt()
            } else {
                info.versionCode
            }
            val name = info.versionName?.ifBlank { "1.0" } ?: "1.0"
            code to name
        } catch (_: Exception) {
            1 to "1.0"
        }
    }

    fun checkForAppUpdates() {
        checkForAppUpdates(onResult = null)
    }

    fun dismissUpdateDialog() {
        _updateAvailable.value = false
    }

    fun simulateNewUpdateAvailable() {
        val (installedCode, installedName) = getInstalledAppVersion()
        _latestVersionName.value = _storeProfile.value.latestAppVersionName
            .takeIf { it.isNotBlank() && _storeProfile.value.latestAppVersionCode > installedCode }
            ?: "${installedName} (Update Preview)"
        _releaseNotes.value = _storeProfile.value.updateReleaseNotes.ifBlank {
            "Major performance upgrade, new design improvements, and real-time sync updates."
        }
        val configuredUrl = _storeProfile.value.apkDownloadUrl.ifBlank {
            com.example.util.WebPortalExporter.getSavedCustomPortalUrl(getApplication())
        }
        _updateDownloadUrl.value = configuredUrl
        _updateAvailable.value = true
    }

    fun publishAppUpdateNotice(
        versionCode: Int,
        versionName: String,
        releaseNotes: String,
        apkDownloadUrl: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val cleanCode = versionCode.coerceAtLeast(1)
            val cleanName = versionName.trim().ifBlank { "1.0" }
            val cleanNotes = releaseNotes.trim().ifBlank {
                "New code & design updates for ODIYA."
            }
            val cleanUrl = apkDownloadUrl.trim().let {
                if (it.isBlank()) "" else com.example.util.WebPortalExporter.normalizeApkOrPortalUrl(it)
            }
            val updatedProfile = _storeProfile.value.copy(
                apkDownloadUrl = cleanUrl,
                latestAppVersionCode = cleanCode,
                latestAppVersionName = cleanName,
                updateReleaseNotes = cleanNotes
            )
            updateStoreProfile(updatedProfile)
            _latestVersionName.value = cleanName
            _releaseNotes.value = cleanNotes
            _updateDownloadUrl.value = cleanUrl

            val manifest = AppUpdateManifest(
                versionCode = cleanCode,
                versionName = cleanName,
                releaseNotes = cleanNotes,
                downloadUrl = cleanUrl
            )
            val res = repository.supabaseSync.publishAppUpdateManifest(manifest)
            if (res.isSuccess) {
                startLiveCallSignalingIfConfigured()
                liveCallClient.sendSignal(
                    "APP_UPDATE",
                    "global_updates",
                    JSONObject().apply {
                        put("versionCode", cleanCode)
                        put("versionName", cleanName)
                        put("releaseNotes", cleanNotes)
                        put("updateUrl", cleanUrl)
                    }
                )
                _cloudStatusMessage.value = "✅ Published APK Update v$cleanName (Build $cleanCode) to Cloud & Live Customer Alerts!"
                onComplete(true, "✅ Published APK Update v$cleanName (Build $cleanCode)! All customer phones are notified automatically with 1-tap update.")
            } else {
                val err = res.exceptionOrNull()?.message ?: "Cloud not connected"
                onComplete(false, "Saved locally on this device, but cloud publish needs active Supabase: $err")
            }
        }
    }

    fun publishInstalledBuildAsUpdate(
        releaseNotes: String,
        apkDownloadUrl: String = "",
        onComplete: (Boolean, String) -> Unit
    ) {
        val (installedCode, installedName) = getInstalledAppVersion()
        val resolvedUrl = apkDownloadUrl.trim().ifBlank {
            _storeProfile.value.apkDownloadUrl.ifBlank {
                com.example.util.WebPortalExporter.DEFAULT_GITHUB_APK_RELEASE_URL
            }
        }
        publishAppUpdateNotice(
            versionCode = installedCode,
            versionName = installedName,
            releaseNotes = releaseNotes,
            apkDownloadUrl = resolvedUrl,
            onComplete = onComplete
        )
    }

    fun publishAppMaintenanceNotice(
        enabled: Boolean,
        message: String,
        isUpgradeNotice: Boolean = false,
        versionName: String = "",
        apkDownloadUrl: String = "",
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val cleanMsg = message.trim().ifBlank {
                "ODIYA scheduled system maintenance & upgrade in progress. Shopping and pending transfers will auto-sync cleanly."
            }
            val nowMs = System.currentTimeMillis()
            val updatedProfile = _storeProfile.value.copy(
                isMaintenanceMode = enabled,
                maintenanceMessage = cleanMsg,
                maintenanceStartedAt = if (enabled) nowMs else 0L
            )
            updateStoreProfile(updatedProfile)

            val title = when {
                enabled && isUpgradeNotice -> "🔧 Scheduled Maintenance & App Upgrade"
                enabled -> "🛠️ ODIYA App Maintenance Alert"
                else -> "✅ ODIYA Maintenance Completed"
            }
            val body = if (enabled) cleanMsg else "App upgrade & maintenance completed! Storefront and instant portals are 100% live."
            ShopNotificationHelper.notifyAppMaintenance(
                context = getApplication(),
                title = title,
                message = body
            )
            voiceManager.speakText(if (enabled) "App maintenance and upgrade notice sent to all customers." else "Storefront maintenance mode cleared.")

            startLiveCallSignalingIfConfigured()
            liveCallClient.sendSignal(
                "APP_MAINTENANCE",
                "global_updates",
                JSONObject().apply {
                    put("enabled", enabled)
                    put("message", body)
                    put("isUpgradeNotice", isUpgradeNotice)
                    put("versionName", versionName.ifBlank { updatedProfile.latestAppVersionName })
                    put("updateUrl", apkDownloadUrl.ifBlank { updatedProfile.apkDownloadUrl })
                    put("timestamp", nowMs)
                }
            )
            if (isUpgradeNotice && versionName.isNotBlank()) {
                val (curCode, _) = getInstalledAppVersion()
                publishAppUpdateNotice(
                    versionCode = maxOf(curCode + 1, updatedProfile.latestAppVersionCode),
                    versionName = versionName,
                    releaseNotes = cleanMsg,
                    apkDownloadUrl = apkDownloadUrl.ifBlank { updatedProfile.apkDownloadUrl },
                    onComplete = { _, _ -> }
                )
            }
            _cloudStatusMessage.value = "✅ $title dispatched to customers!"
            onComplete(true, "✅ Automatic Maintenance & Upgrade notification sent to all customers!")
        }
    }

    // Pre-Order & Special Brand Requests State & Methods (Loaded in init after adminPrefs is constructed)
    private val _preOrderRequests = MutableStateFlow<List<com.example.data.model.PreOrderRequest>>(emptyList())
    val preOrderRequests: StateFlow<List<com.example.data.model.PreOrderRequest>> = _preOrderRequests.asStateFlow()

    private fun loadPreOrderRequests(): List<com.example.data.model.PreOrderRequest> {
        val raw = adminPrefs.getString("pre_order_requests_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                val id = obj.optString("id", "").trim()
                if (id.isBlank()) return@mapNotNull null
                val statusName = obj.optString("status", com.example.data.model.PreOrderStatus.PENDING.name)
                val status = com.example.data.model.PreOrderStatus.entries.firstOrNull { it.name == statusName }
                    ?: com.example.data.model.PreOrderStatus.PENDING
                com.example.data.model.PreOrderRequest(
                    id = id,
                    customerId = obj.optString("customerId", "guest"),
                    customerName = obj.optString("customerName", "Valued Customer"),
                    customerPhone = obj.optString("customerPhone", ""),
                    productName = obj.optString("productName", ""),
                    brandName = obj.optString("brandName", ""),
                    category = obj.optString("category", "Clothes"),
                    preferredSize = obj.optString("preferredSize", "Standard"),
                    preferredColor = obj.optString("preferredColor", "Any"),
                    targetBudget = obj.optDouble("targetBudget", 0.0),
                    description = obj.optString("description", ""),
                    photoUrl = obj.optString("photoUrl", ""),
                    status = status,
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    adminResponse = obj.optString("adminResponse", ""),
                    adminQuotedPrice = obj.optDouble("adminQuotedPrice", 0.0),
                    customerReply = obj.optString("customerReply", "")
                )
            }.sortedByDescending { it.createdAt }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persistPreOrderRequests(list: List<com.example.data.model.PreOrderRequest>) {
        val arr = JSONArray()
        list.forEach { r ->
            arr.put(JSONObject().apply {
                put("id", r.id)
                put("customerId", r.customerId)
                put("customerName", r.customerName)
                put("customerPhone", r.customerPhone)
                put("productName", r.productName)
                put("brandName", r.brandName)
                put("category", r.category)
                put("preferredSize", r.preferredSize)
                put("preferredColor", r.preferredColor)
                put("targetBudget", r.targetBudget)
                put("description", r.description)
                put("photoUrl", r.photoUrl)
                put("status", r.status.name)
                put("createdAt", r.createdAt)
                put("adminResponse", r.adminResponse)
                put("adminQuotedPrice", r.adminQuotedPrice)
                put("customerReply", r.customerReply)
            })
        }
        adminPrefs.edit().putString("pre_order_requests_json", arr.toString()).apply()
    }

    private suspend fun syncPreOrdersWithCloudInternal(
        preferLocalIds: Set<String> = emptySet(),
        notifyOnNewRemote: Boolean = false
    ): Result<List<com.example.data.model.PreOrderRequest>> {
        if (repository.supabaseSync.isPlaceholderConfig()) {
            return Result.success(_preOrderRequests.value)
        }
        val beforeIds = _preOrderRequests.value.map { it.id }.toSet()
        val beforeSnapshot = _preOrderRequests.value.associateBy { it.id }
        val res = repository.supabaseSync.syncPreOrdersWithCloud(
            localList = _preOrderRequests.value,
            preferLocalIds = preferLocalIds
        )
        res.onSuccess { merged ->
            _preOrderRequests.value = merged
            persistPreOrderRequests(merged)
            if (notifyOnNewRemote) {
                val recentCutoffMs = appLaunchTimestampMs - 300_000L
                val isAdminDevice = _isAdminUnlocked.value || (_currentCustomer.value?.isAdmin == true)
                val newItems = merged.filter { it.id !in beforeIds && it.createdAt >= recentCutoffMs }
                if (newItems.isNotEmpty() && isAdminDevice) {
                    val latest = newItems.first()
                    ShopNotificationHelper.notifyAdminNewPreOrder(
                        context = getApplication(),
                        requestId = latest.id,
                        customerName = latest.customerName,
                        productName = latest.productName,
                        brandName = latest.brandName
                    )
                    voiceManager.speakText("New Pre-Order Request received from ${latest.customerName} for ${latest.productName}.")
                }
                // Notify customer if Admin updated an existing request remotely
                val myCustId = _currentCustomer.value?.id
                merged.forEach { cur ->
                    val prev = beforeSnapshot[cur.id]
                    if (prev != null &&
                        (prev.status != cur.status || (prev.adminResponse != cur.adminResponse && cur.adminResponse.isNotBlank())) &&
                        (myCustId == null || cur.customerId == myCustId)
                    ) {
                        ShopNotificationHelper.notifyPreOrderReply(
                            context = getApplication(),
                            requestId = cur.id,
                            productName = cur.productName,
                            statusLabel = cur.status.label
                        )
                    }
                }
            }
        }
        return res
    }

    fun syncPreOrdersNow(onComplete: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            _isRefreshing.value = true
            val res = syncPreOrdersWithCloudInternal(notifyOnNewRemote = false)
            _isRefreshing.value = false
            res.onSuccess { list ->
                onComplete?.invoke(true, "✅ Synced ${list.size} Pre-Order request(s) with Cloud.")
            }.onFailure { err ->
                onComplete?.invoke(false, "Saved locally. Cloud sync note: ${err.message ?: "Connect Supabase first"}")
            }
        }
    }

    fun submitPreOrderRequest(
        productName: String,
        brandName: String,
        category: String = "Clothes",
        preferredSize: String = "Standard",
        preferredColor: String = "Any",
        targetBudget: Double = 0.0,
        description: String = "",
        photoUrl: String = "",
        customerNameOverride: String = "",
        customerPhoneOverride: String = ""
    ): Boolean {
        val currentCust = _currentCustomer.value
        val savedInfo = _savedDeliveryInfo.value
        val resolvedName = customerNameOverride.trim().ifBlank {
            currentCust?.fullName?.takeIf { it.isNotBlank() }
                ?: savedInfo.fullName.takeIf { it.isNotBlank() }
                ?: "Valued Customer"
        }
        val resolvedPhone = customerPhoneOverride.trim().ifBlank {
            currentCust?.phoneNumber?.takeIf { it.isNotBlank() }
                ?: savedInfo.phoneNumber.takeIf { it.isNotBlank() }
                ?: ""
        }
        val resolvedDigits = resolvedPhone.filter { it.isDigit() }
        val matchedCustomerId = currentCust?.id ?: allCustomers.value.firstOrNull { c ->
            val cDigits = c.phoneNumber.filter { it.isDigit() }
            val phoneMatch = resolvedDigits.length >= 7 && cDigits.length >= 7 &&
                resolvedDigits.takeLast(minOf(10, resolvedDigits.length)) == cDigits.takeLast(minOf(10, cDigits.length))
            val nameMatch = resolvedName.isNotBlank() && resolvedName != "Valued Customer" &&
                c.fullName.equals(resolvedName, ignoreCase = true)
            phoneMatch || nameMatch
        }?.id ?: "guest"

        val req = com.example.data.model.PreOrderRequest(
            id = "REQ-${java.util.UUID.randomUUID().toString().take(6).uppercase()}",
            customerId = matchedCustomerId,
            customerName = resolvedName,
            customerPhone = resolvedPhone,
            productName = productName.trim(),
            brandName = brandName.trim(),
            category = category,
            preferredSize = preferredSize.ifBlank { "Standard" },
            preferredColor = preferredColor.ifBlank { "Any" },
            targetBudget = targetBudget,
            description = description.trim(),
            photoUrl = photoUrl,
            status = com.example.data.model.PreOrderStatus.PENDING,
            createdAt = System.currentTimeMillis()
        )
        val updated = (listOf(req) + _preOrderRequests.value).distinctBy { it.id }
        _preOrderRequests.value = updated
        persistPreOrderRequests(updated)
        if (savedInfo.phoneNumber.isBlank() && resolvedPhone.isNotBlank()) {
            persistDeliveryInfoLocally(
                fullName = resolvedName.takeIf { it != "Valued Customer" },
                phoneNumber = resolvedPhone
            )
        }
        voiceManager.speakText("Special Pre-Order Request submitted for ${productName.trim()} by ${brandName.trim()}.")

        viewModelScope.launch {
            var workingReq = req
            if (photoUrl.isNotBlank() && !photoUrl.startsWith("http", ignoreCase = true)) {
                val uploadedUrl = repository.persistReturnProofImage(req.id, photoUrl)
                if (uploadedUrl.isNotBlank() && uploadedUrl != photoUrl) {
                    workingReq = req.copy(photoUrl = uploadedUrl)
                    val refreshedList = _preOrderRequests.value.map { if (it.id == req.id) workingReq else it }
                    _preOrderRequests.value = refreshedList
                    persistPreOrderRequests(refreshedList)
                }
            }
            val isAdminLocal = _isAdminUnlocked.value || (_currentCustomer.value?.isAdmin == true)
            ShopNotificationHelper.notifyAdminNewPreOrder(
                context = getApplication(),
                requestId = workingReq.id,
                customerName = workingReq.customerName,
                productName = workingReq.productName,
                brandName = workingReq.brandName
            )
            if (isAdminLocal) {
                voiceManager.speakText("Admin Alert: New Pre-Order request from ${workingReq.customerName} for ${workingReq.productName}.")
            }
            val isUnconfiguredCloud = repository.supabaseSync.isPlaceholderConfig()
            val syncRes = if (isUnconfiguredCloud) {
                Result.failure(Exception("Supabase not configured on this device yet"))
            } else {
                syncPreOrdersWithCloudInternal(
                    preferLocalIds = setOf(workingReq.id),
                    notifyOnNewRemote = false
                )
            }
            startLiveCallSignalingIfConfigured()
            liveCallClient.sendSignal(
                "PRE_ORDER",
                "global_preorders",
                JSONObject().apply {
                    put("id", workingReq.id)
                    put("customerName", workingReq.customerName)
                    put("customerPhone", workingReq.customerPhone)
                    put("productName", workingReq.productName)
                    put("brandName", workingReq.brandName)
                }
            )
            if (isUnconfiguredCloud || syncRes.isFailure || !isOnline.value) {
                enqueueOfflineAction(
                    type = OfflineActionType.PREORDER_SYNC,
                    referenceId = workingReq.id,
                    summary = "Pre-Order ${workingReq.id}: ${workingReq.productName}"
                )
            }
        }
        return true
    }

    fun updatePreOrderResponse(
        requestId: String,
        adminResponseText: String = "",
        quotedPrice: Double = 0.0,
        status: com.example.data.model.PreOrderStatus = com.example.data.model.PreOrderStatus.PROCESSING
    ) {
        var updatedProductName = ""
        val updated = _preOrderRequests.value.map { req ->
            if (req.id == requestId) {
                updatedProductName = req.productName
                req.copy(
                    adminResponse = adminResponseText,
                    adminQuotedPrice = quotedPrice,
                    status = status
                )
            } else req
        }
        _preOrderRequests.value = updated
        persistPreOrderRequests(updated)
        voiceManager.speakText("Pre-order request $requestId has been updated by Admin.")
        if (updatedProductName.isNotBlank()) {
            ShopNotificationHelper.notifyPreOrderReply(
                context = getApplication(),
                requestId = requestId,
                productName = updatedProductName,
                statusLabel = status.label
            )
        }
        viewModelScope.launch {
            syncPreOrdersWithCloudInternal(preferLocalIds = setOf(requestId))
            startLiveCallSignalingIfConfigured()
            liveCallClient.sendSignal(
                "PRE_ORDER",
                "global_preorders",
                JSONObject().apply { put("id", requestId) }
            )
        }
    }

    fun submitCustomerPreOrderReply(requestId: String, replyText: String) {
        val updated = _preOrderRequests.value.map { req ->
            if (req.id == requestId) {
                req.copy(customerReply = replyText.trim())
            } else req
        }
        _preOrderRequests.value = updated
        persistPreOrderRequests(updated)
        voiceManager.speakText("New customer reply received for request $requestId.")
        viewModelScope.launch {
            syncPreOrdersWithCloudInternal(preferLocalIds = setOf(requestId))
            startLiveCallSignalingIfConfigured()
            liveCallClient.sendSignal(
                "PRE_ORDER",
                "global_preorders",
                JSONObject().apply { put("id", requestId) }
            )
        }
    }

    // Wishlist State (persisted in user profile within Supabase)
    private val _wishlistIds = MutableStateFlow<Set<String>>(emptySet())
    val wishlistIds: StateFlow<Set<String>> = _wishlistIds.asStateFlow()

    val wishlistProducts: StateFlow<List<Product>> = combine(allProducts, _wishlistIds) { products, ids ->
        products.filter { it.id in ids }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wishlistCount: StateFlow<Int> = combine(_wishlistIds) { ids ->
        ids.first().size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Delivery Options (Editable by Admin)
    private val _deliveryOptions = MutableStateFlow(
        listOf(
            DeliveryOption("opt_a", "Free local pickup / Zone A Express Delivery", 0.0),
            DeliveryOption("opt_b", "Standard ODIYA City Delivery", 1500.0),
            DeliveryOption("opt_c", "Inter-State Regional Delivery", 2500.0),
            DeliveryOption("opt_d", "Nationwide Express Doorstep Delivery", 5000.0),
            DeliveryOption("opt_e", "VIP Special Gift Package Local Delivery", 5000.0),
            DeliveryOption("opt_f", "VIP Special Gift Package Nationwide Delivery", 8000.0)
        )
    )
    val deliveryOptions: StateFlow<List<DeliveryOption>> = _deliveryOptions.asStateFlow()

    fun updateDeliveryOption(id: String, newTitle: String, newPrice: Double) {
        val updated = _deliveryOptions.value.map { opt ->
            if (opt.id == id) opt.copy(title = newTitle, price = newPrice) else opt
        }
        _deliveryOptions.value = updated
    }

    // Refresh / Syncing state
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Full Screen Mode
    private val _isFullScreen = MutableStateFlow(false)
    val isFullScreen: StateFlow<Boolean> = _isFullScreen.asStateFlow()

    fun toggleFullScreen() {
        _isFullScreen.value = !_isFullScreen.value
    }

    // Standard Vertical Scrolling Modes (Grid & Horizontal Shelves)
    private val _catalogBrowseMode = MutableStateFlow(CatalogBrowseMode.CAROUSEL_AND_GRID)
    val catalogBrowseMode: StateFlow<CatalogBrowseMode> = _catalogBrowseMode.asStateFlow()

    fun setCatalogBrowseMode(mode: CatalogBrowseMode) {
        _catalogBrowseMode.value = if (mode == CatalogBrowseMode.WIDE_MATRIX_2D) {
            CatalogBrowseMode.CAROUSEL_AND_GRID
        } else {
            mode
        }
        _isWideDualAxisMode.value = false
    }

    private val _isWideDualAxisMode = MutableStateFlow(false)
    val isWideDualAxisMode: StateFlow<Boolean> = _isWideDualAxisMode.asStateFlow()

    fun toggleWideDualAxisMode() {
        _isWideDualAxisMode.value = false
    }

    fun setWideDualAxisMode(enabled: Boolean) {
        _isWideDualAxisMode.value = false
    }

    // Customer Account Shortcut & Vertical Menu Destination
    private val _customerAccountDestination = MutableStateFlow(CustomerAccountDestination.MENU)
    val customerAccountDestination: StateFlow<CustomerAccountDestination> = _customerAccountDestination.asStateFlow()

    private val _orderTrackingState = MutableStateFlow(OrderTrackingUiState())
    val orderTrackingState: StateFlow<OrderTrackingUiState> = _orderTrackingState.asStateFlow()

    val customerScopedOrders: StateFlow<List<Order>> = combine(allOrders, _currentCustomer) { orders, customer ->
        if (customer == null) {
            orders
        } else {
            val custPhoneTail = customer.phoneNumber.filter { it.isDigit() }.let {
                if (it.length >= 10) it.takeLast(10) else it
            }
            val custEmail = customer.email.trim().lowercase()
            val matched = orders.filter { ord ->
                (customer.id.isNotBlank() && ord.customerId == customer.id) ||
                    (custPhoneTail.length >= 7 && ord.customerPhone.filter { it.isDigit() }.takeLast(10) == custPhoneTail) ||
                    (custEmail.isNotBlank() && !custEmail.endsWith("@247shopping.local") && !custEmail.endsWith("@ozich.local") && !custEmail.endsWith("@odiya.local") && ord.customerEmail.trim().lowercase() == custEmail)
            }
            matched.ifEmpty { orders }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _isFollowingStore = MutableStateFlow(adminPrefs.getBoolean("is_following_store", false))
    val isFollowingStore: StateFlow<Boolean> = _isFollowingStore.asStateFlow()

    fun openCustomerAccountShortcut(destination: CustomerAccountDestination = CustomerAccountDestination.MENU) {
        _customerAccountDestination.value = destination
        navigateTo(AppScreen.AUTH)
    }

    fun selectCustomerAccountDestination(destination: CustomerAccountDestination) {
        _customerAccountDestination.value = destination
    }

    fun openOrderTracking(orderId: String? = null) {
        _customerAccountDestination.value = CustomerAccountDestination.ORDER_TRACKING
        if (!orderId.isNullOrBlank()) {
            _orderTrackingState.value = _orderTrackingState.value.copy(
                query = orderId.trim(),
                focusedOrderId = orderId.trim(),
                hasActiveSearch = true
            )
            navigateTo(AppScreen.AUTH)
            searchAndTrackOrderFromSupabase(orderId.trim())
        } else {
            _orderTrackingState.value = _orderTrackingState.value.copy(
                focusedOrderId = null
            )
            navigateTo(AppScreen.AUTH)
            refreshCustomerOrderTracking()
        }
    }

    fun updateOrderTrackingQuery(query: String) {
        _orderTrackingState.value = _orderTrackingState.value.copy(
            query = query,
            errorMessage = null
        )
    }

    fun setOrderTrackingFilter(filter: OrderTrackingFilter) {
        _orderTrackingState.value = _orderTrackingState.value.copy(selectedFilter = filter)
    }

    fun clearOrderTrackingSearch() {
        _orderTrackingState.value = _orderTrackingState.value.copy(
            query = "",
            focusedOrderId = null,
            searchedOrders = emptyList(),
            hasActiveSearch = false,
            errorMessage = null
        )
        refreshCustomerOrderTracking()
    }

    fun refreshCustomerOrderTracking(focusOrderId: String? = null) {
        viewModelScope.launch {
            _orderTrackingState.value = _orderTrackingState.value.copy(
                isLoading = true,
                errorMessage = null,
                focusedOrderId = focusOrderId ?: _orderTrackingState.value.focusedOrderId
            )
            val cust = _currentCustomer.value
            val phone = cust?.phoneNumber?.ifBlank { _savedDeliveryInfo.value.phoneNumber } ?: _savedDeliveryInfo.value.phoneNumber
            val result = repository.refreshCustomerOrdersFromSupabase(
                customerId = cust?.id.orEmpty(),
                customerPhone = phone,
                customerEmail = cust?.email.orEmpty()
            )
            val enriched = result.orders.map { enrichOrderWithCreditState(it) }
            if (result.syncedFromSupabase) {
                _isSupabaseConnected.value = true
                _lastSyncTimestamp.value = result.syncedAtMillis
            }
            val currentSearch = _orderTrackingState.value
            _orderTrackingState.value = currentSearch.copy(
                isLoading = false,
                isLiveSynced = result.syncedFromSupabase,
                lastSyncedAtMillis = result.syncedAtMillis,
                statusBanner = result.message,
                searchedOrders = if (currentSearch.hasActiveSearch) {
                    val q = currentSearch.query.trim().lowercase()
                    enriched.filter {
                        it.orderId.lowercase().contains(q) ||
                            it.paymentReference.lowercase().contains(q) ||
                            it.customerPhone.lowercase().contains(q)
                    }
                } else {
                    emptyList()
                }
            )
        }
    }

    fun searchAndTrackOrderFromSupabase(query: String = _orderTrackingState.value.query) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) {
            clearOrderTrackingSearch()
            return
        }
        viewModelScope.launch {
            _orderTrackingState.value = _orderTrackingState.value.copy(
                query = cleanQuery,
                isLoading = true,
                hasActiveSearch = true,
                errorMessage = null
            )
            val res = repository.trackOrdersByQueryFromSupabase(cleanQuery)
            val enriched = res.orders.map { enrichOrderWithCreditState(it) }
            if (res.syncedFromSupabase) {
                _isSupabaseConnected.value = true
                _lastSyncTimestamp.value = res.syncedAtMillis
            }
            _orderTrackingState.value = _orderTrackingState.value.copy(
                isLoading = false,
                isLiveSynced = res.syncedFromSupabase,
                lastSyncedAtMillis = res.syncedAtMillis,
                statusBanner = res.message,
                searchedOrders = enriched,
                focusedOrderId = enriched.firstOrNull()?.orderId
            )
        }
    }

    fun refreshSingleOrderTracking(orderId: String, onDone: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            _orderTrackingState.value = _orderTrackingState.value.copy(
                isLoading = true,
                focusedOrderId = orderId
            )
            val res = repository.refreshSingleOrderFromSupabase(orderId)
            val enriched = res.orders.map { enrichOrderWithCreditState(it) }
            if (res.syncedFromSupabase) {
                _isSupabaseConnected.value = true
                _lastSyncTimestamp.value = res.syncedAtMillis
            }
            val prevSearched = _orderTrackingState.value.searchedOrders
            val updatedSearched = if (enriched.isNotEmpty() && _orderTrackingState.value.hasActiveSearch) {
                val updatedOrder = enriched.first()
                if (prevSearched.any { it.orderId == updatedOrder.orderId }) {
                    prevSearched.map { if (it.orderId == updatedOrder.orderId) updatedOrder else it }
                } else {
                    listOf(updatedOrder) + prevSearched
                }
            } else {
                prevSearched
            }
            _orderTrackingState.value = _orderTrackingState.value.copy(
                isLoading = false,
                isLiveSynced = res.syncedFromSupabase,
                lastSyncedAtMillis = res.syncedAtMillis,
                statusBanner = res.message,
                searchedOrders = updatedSearched
            )
            onDone?.invoke(res.syncedFromSupabase, res.message)
        }
    }

    fun toggleFollowStore(): Boolean {
        val next = !_isFollowingStore.value
        _isFollowingStore.value = next
        adminPrefs.edit().putBoolean("is_following_store", next).apply()
        return next
    }

    // Admin Security Settings (Password protected access)
    private val _adminPassword = MutableStateFlow(
        adminPrefs.getString("admin_password", "admin123")?.ifBlank { "admin123" } ?: "admin123"
    )
    val adminPassword: StateFlow<String> = _adminPassword.asStateFlow()

    private val _isAdminUnlocked = MutableStateFlow(false)
    val isAdminUnlocked: StateFlow<Boolean> = _isAdminUnlocked.asStateFlow()

    private val _adminSelectedTab = MutableStateFlow(0)
    val adminSelectedTab: StateFlow<Int> = _adminSelectedTab.asStateFlow()

    // False on every app launch so unauthenticated users never see the Admin Control button
    private val _isAdminDeviceAuthorized = MutableStateFlow(false)
    val isAdminDeviceAuthorized: StateFlow<Boolean> = _isAdminDeviceAuthorized.asStateFlow()

    fun selectAdminTab(tabIndex: Int) {
        _adminSelectedTab.value = tabIndex.coerceIn(0, 11)
    }

    fun openAdminControlRoom(initialTab: Int = 0) {
        _adminSelectedTab.value = initialTab.coerceIn(0, 11)
        navigateTo(AppScreen.ADMIN_CONTROL_ROOM)
    }

    fun setAdminPassword(newPass: String) {
        if (newPass.isNotBlank()) {
            val clean = newPass.trim()
            adminPrefs.edit().putString("admin_password", clean).apply()
            _adminPassword.value = clean
        }
    }

    fun verifyAdminPassword(input: String): Boolean {
        val matches = input.trim() == _adminPassword.value
        if (matches) {
            _isAdminUnlocked.value = true
            _isAdminDeviceAuthorized.value = true
        }
        return matches
    }

    fun unlockAdminWithBiometric() {
        _isAdminUnlocked.value = true
        _isAdminDeviceAuthorized.value = true
    }

    fun lockAdmin() {
        _isAdminUnlocked.value = false
        _isAdminDeviceAuthorized.value = false
        adminPrefs.edit().putBoolean("is_admin_device_authorized", false).apply()
    }

    fun hideAdminControlForCustomerMode() {
        _isAdminUnlocked.value = false
        _isAdminDeviceAuthorized.value = false
        adminPrefs.edit().putBoolean("is_admin_device_authorized", false).apply()
        if (_currentScreen.value == AppScreen.ADMIN_CONTROL_ROOM) {
            navigateToHome()
        }
    }

    private fun migrateLegacyStoreBrandPref(key: String): String {
        val raw = adminPrefs.getString(key, "ODIYA")?.trim().orEmpty()
        val isLegacyBrand = raw.isBlank() ||
            raw.equals("OZICH", ignoreCase = true) ||
            raw.equals("24/7 SHOPPING", ignoreCase = true) ||
            raw.equals("24/7 Smart Shop", ignoreCase = true) ||
            raw.equals("DEPROS 24/7 SMART SHOPPING MALL", ignoreCase = true) ||
            raw.equals("DEPROS BOUTIQUE", ignoreCase = true)
        return if (isLegacyBrand) {
            if (raw.isNotBlank() && !raw.equals("ODIYA", ignoreCase = true)) {
                adminPrefs.edit().putString(key, "ODIYA").apply()
            }
            "ODIYA"
        } else {
            raw
        }
    }

    // Store Contact, App Email, Address & Official Payment Accounts (Editable by Admin, reflected app-wide)
    private val _storeProfile = MutableStateFlow(
        StoreProfileConfig(
            storeName = migrateLegacyStoreBrandPref("store_name"),
            tagline = adminPrefs.getString("store_tagline", "Shopping with ease and comfort")?.ifBlank { "Shopping with ease and comfort" } ?: "Shopping with ease and comfort",
            phoneNumber = adminPrefs.getString("store_phone", "") ?: "",
            secondaryPhoneNumber = adminPrefs.getString("store_secondary_phone", "") ?: "",
            whatsappNumber = adminPrefs.getString("store_whatsapp", "") ?: "",
            supportEmail = adminPrefs.getString("store_email", "")?.trim() ?: "",
            storeAddress = adminPrefs.getString("store_address", "") ?: "",
            opayAccountNumber = adminPrefs.getString("store_opay_number", "") ?: "",
            opayAccountName = migrateLegacyStoreBrandPref("store_opay_name"),
            moniepointAccountNumber = adminPrefs.getString("store_moniepoint_number", "") ?: "",
            moniepointAccountName = migrateLegacyStoreBrandPref("store_moniepoint_name"),
            palmpayAccountNumber = adminPrefs.getString("store_palmpay_number", "") ?: "",
            palmpayAccountName = migrateLegacyStoreBrandPref("store_palmpay_name"),
            posTerminalId = adminPrefs.getString("store_pos_terminal_id", "") ?: "",
            posTerminalName = adminPrefs.getString("store_pos_terminal_name", "") ?: "",
            extraBankName = adminPrefs.getString("store_extra_bank_name", "") ?: "",
            extraBankAccountNumber = adminPrefs.getString("store_extra_bank_number", "") ?: "",
            extraBankAccountName = adminPrefs.getString("store_extra_bank_holder", "") ?: "",
            apkDownloadUrl = com.example.util.WebPortalExporter.normalizeApkOrPortalUrl(
                adminPrefs.getString("apk_download_url", "") ?: ""
            ).also { cleanSavedUrl ->
                val rawSavedUrl = adminPrefs.getString("apk_download_url", "") ?: ""
                if (rawSavedUrl != cleanSavedUrl) {
                    adminPrefs.edit().putString("apk_download_url", cleanSavedUrl).apply()
                }
            },
            latestAppVersionCode = adminPrefs.getInt("latest_app_version_code", 1),
            latestAppVersionName = adminPrefs.getString("latest_app_version_name", "1.0")?.ifBlank { "1.0" } ?: "1.0",
            updateReleaseNotes = adminPrefs.getString("update_release_notes", "") ?: "",
            isMaintenanceMode = adminPrefs.getBoolean("store_maintenance_mode", false),
            maintenanceMessage = adminPrefs.getString("store_maintenance_message", "") ?: "",
            maintenanceStartedAt = adminPrefs.getLong("store_maintenance_started_at", 0L),
            paystackPublicKey = run {
                val appCtx = getApplication<Application>()
                val vaultKey = com.example.util.EncryptedKeystoreVault.getSecret(
                    appCtx,
                    "paystack_public_key",
                    adminPrefs.getString("store_paystack_public_key", "") ?: ""
                )
                if (vaultKey.isNotBlank()) vaultKey
                else runCatching { BuildConfig.PAYSTACK_PUBLIC_KEY }
                    .getOrDefault("")
                    .trim()
                    .takeUnless { it.contains("YOUR_PAYSTACK", ignoreCase = true) }
                    .orEmpty()
            },
            paystackCallbackUrl = adminPrefs.getString("store_paystack_callback_url", "depros://paystack-callback")
                ?.ifBlank { "depros://paystack-callback" } ?: "depros://paystack-callback",
            paystackEdgeFunctionBaseUrl = adminPrefs.getString("store_paystack_edge_url", "") ?: ""
        )
    )
    val storeProfile: StateFlow<StoreProfileConfig> = _storeProfile.asStateFlow()

    fun updateStoreProfile(config: StoreProfileConfig) {
        val cleanPaystackPublic = com.example.util.AppDefenseManager.sanitizeTextInput(config.paystackPublicKey, 120)
        val cleanPaystackCallback = config.paystackCallbackUrl.trim().ifBlank { "depros://paystack-callback" }
        val cleanPaystackEdgeUrl = config.paystackEdgeFunctionBaseUrl.trim().trimEnd('/')
        com.example.util.EncryptedKeystoreVault.putSecret(
            getApplication(),
            "paystack_public_key",
            cleanPaystackPublic
        )
        val clean = config.copy(
            storeName = config.storeName.trim().ifBlank { "ODIYA" },
            tagline = config.tagline.trim().ifBlank { "Shopping with ease and comfort" },
            phoneNumber = config.phoneNumber.trim(),
            secondaryPhoneNumber = config.secondaryPhoneNumber.trim(),
            whatsappNumber = config.whatsappNumber.trim(),
            supportEmail = config.supportEmail.trim(),
            storeAddress = config.storeAddress.trim(),
            opayAccountNumber = config.opayAccountNumber.trim(),
            opayAccountName = config.opayAccountName.trim().ifBlank { "ODIYA" },
            moniepointAccountNumber = config.moniepointAccountNumber.trim(),
            moniepointAccountName = config.moniepointAccountName.trim().ifBlank { "ODIYA" },
            palmpayAccountNumber = config.palmpayAccountNumber.trim(),
            palmpayAccountName = config.palmpayAccountName.trim().ifBlank { "ODIYA" },
            posTerminalId = config.posTerminalId.trim(),
            posTerminalName = config.posTerminalName.trim(),
            extraBankName = config.extraBankName.trim(),
            extraBankAccountNumber = config.extraBankAccountNumber.trim(),
            extraBankAccountName = config.extraBankAccountName.trim(),
            apkDownloadUrl = com.example.util.WebPortalExporter.normalizeApkOrPortalUrl(config.apkDownloadUrl),
            latestAppVersionCode = config.latestAppVersionCode.coerceAtLeast(1),
            latestAppVersionName = config.latestAppVersionName.trim().ifBlank { "1.0" },
            updateReleaseNotes = config.updateReleaseNotes.trim(),
            maintenanceMessage = config.maintenanceMessage.trim(),
            paystackPublicKey = cleanPaystackPublic,
            paystackCallbackUrl = cleanPaystackCallback,
            paystackEdgeFunctionBaseUrl = cleanPaystackEdgeUrl
        )
        adminPrefs.edit()
            .putString("store_name", clean.storeName)
            .putString("store_tagline", clean.tagline)
            .putString("store_phone", clean.phoneNumber)
            .putString("store_secondary_phone", clean.secondaryPhoneNumber)
            .putString("store_whatsapp", clean.whatsappNumber)
            .putString("store_email", clean.supportEmail)
            .putString("store_address", clean.storeAddress)
            .putString("store_opay_number", clean.opayAccountNumber)
            .putString("store_opay_name", clean.opayAccountName)
            .putString("store_moniepoint_number", clean.moniepointAccountNumber)
            .putString("store_moniepoint_name", clean.moniepointAccountName)
            .putString("store_palmpay_number", clean.palmpayAccountNumber)
            .putString("store_palmpay_name", clean.palmpayAccountName)
            .putString("store_pos_terminal_id", clean.posTerminalId)
            .putString("store_pos_terminal_name", clean.posTerminalName)
            .putString("store_extra_bank_name", clean.extraBankName)
            .putString("store_extra_bank_number", clean.extraBankAccountNumber)
            .putString("store_extra_bank_holder", clean.extraBankAccountName)
            .putString("apk_download_url", clean.apkDownloadUrl)
            .putInt("latest_app_version_code", clean.latestAppVersionCode)
            .putString("latest_app_version_name", clean.latestAppVersionName)
            .putString("update_release_notes", clean.updateReleaseNotes)
            .putBoolean("store_maintenance_mode", clean.isMaintenanceMode)
            .putString("store_maintenance_message", clean.maintenanceMessage)
            .putLong("store_maintenance_started_at", clean.maintenanceStartedAt)
            .putString("store_paystack_callback_url", clean.paystackCallbackUrl)
            .putString("store_paystack_edge_url", clean.paystackEdgeFunctionBaseUrl)
            .remove("store_paystack_public_key")
            .apply()
        _storeProfile.value = clean
        _latestVersionName.value = clean.latestAppVersionName
        _releaseNotes.value = clean.updateReleaseNotes
        _updateDownloadUrl.value = clean.apkDownloadUrl
        if (clean.apkDownloadUrl.isNotBlank() &&
            !com.example.util.WebPortalExporter.isDirectApkReleaseUrl(clean.apkDownloadUrl) &&
            com.example.util.WebPortalExporter.getSavedCustomPortalUrl(getApplication()).isBlank()
        ) {
            com.example.util.WebPortalExporter.saveCustomPortalUrl(getApplication(), clean.apkDownloadUrl)
        }
    }

    fun savePermanentShareUrl(rawUrl: String): String {
        val ctx = getApplication<Application>()
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) {
            com.example.util.WebPortalExporter.saveCustomPortalUrl(ctx, "")
            return ""
        }
        val cleanPortalUrl = com.example.util.WebPortalExporter.normalizePortalUrl(trimmed)
        val cleanApkOrPortal = com.example.util.WebPortalExporter.normalizeApkOrPortalUrl(trimmed)
        val isApkRelease = com.example.util.WebPortalExporter.isDirectApkReleaseUrl(cleanApkOrPortal)
        if (!isApkRelease && cleanPortalUrl.isNotBlank()) {
            com.example.util.WebPortalExporter.saveCustomPortalUrl(ctx, cleanPortalUrl)
        }
        val currentProfile = _storeProfile.value
        val currentCleanApk = com.example.util.WebPortalExporter.normalizeApkOrPortalUrl(currentProfile.apkDownloadUrl)
        val effectiveReturnUrl = if (isApkRelease) cleanApkOrPortal else cleanPortalUrl
        if (effectiveReturnUrl.isNotBlank() && (currentCleanApk.isBlank() || isApkRelease)) {
            val updated = currentProfile.copy(apkDownloadUrl = effectiveReturnUrl)
            updateStoreProfile(updated)
            viewModelScope.launch {
                val manifest = AppUpdateManifest(
                    versionCode = updated.latestAppVersionCode.coerceAtLeast(1),
                    versionName = updated.latestAppVersionName.ifBlank { "1.0" },
                    releaseNotes = updated.updateReleaseNotes.ifBlank {
                        "New code & design updates for ODIYA."
                    },
                    downloadUrl = effectiveReturnUrl
                )
                repository.supabaseSync.publishAppUpdateManifest(manifest)
            }
        }
        return effectiveReturnUrl
    }

    // Automatically persisted customer delivery information so it stays permanent on the Order Confirmation page
    private val _savedDeliveryInfo = MutableStateFlow(
        CustomerDeliveryInfo(
            fullName = adminPrefs.getString("saved_cust_name", "") ?: "",
            phoneNumber = adminPrefs.getString("saved_cust_phone", "") ?: "",
            deliveryAddress = adminPrefs.getString("saved_cust_address", "") ?: ""
        )
    )
    val savedDeliveryInfo: StateFlow<CustomerDeliveryInfo> = _savedDeliveryInfo.asStateFlow()

    private val _savedSenderName = MutableStateFlow(
        adminPrefs.getString("saved_sender_name", "") ?: ""
    )
    val savedSenderName: StateFlow<String> = _savedSenderName.asStateFlow()

    fun persistDeliveryInfoLocally(
        fullName: String? = null,
        phoneNumber: String? = null,
        deliveryAddress: String? = null,
        senderName: String? = null
    ) {
        val prev = _savedDeliveryInfo.value
        val updated = CustomerDeliveryInfo(
            fullName = fullName?.trim()?.takeIf { it.isNotBlank() } ?: prev.fullName,
            phoneNumber = phoneNumber?.trim()?.takeIf { it.isNotBlank() } ?: prev.phoneNumber,
            deliveryAddress = deliveryAddress?.trim()?.takeIf { it.isNotBlank() } ?: prev.deliveryAddress
        )
        val cleanSender = senderName?.trim()?.takeIf { it.isNotBlank() } ?: _savedSenderName.value
        if (updated != prev || cleanSender != _savedSenderName.value) {
            _savedDeliveryInfo.value = updated
            _savedSenderName.value = cleanSender
            adminPrefs.edit()
                .putString("saved_cust_name", updated.fullName)
                .putString("saved_cust_phone", updated.phoneNumber)
                .putString("saved_cust_address", updated.deliveryAddress)
                .putString("saved_sender_name", cleanSender)
                .apply()
        }
    }

    fun saveCustomerDeliveryInfo(fullName: String, phoneNumber: String, deliveryAddress: String) {
        val prev = _savedDeliveryInfo.value
        val updated = CustomerDeliveryInfo(
            fullName = fullName.trim().ifBlank { prev.fullName },
            phoneNumber = phoneNumber.trim().ifBlank { prev.phoneNumber },
            deliveryAddress = deliveryAddress.trim().ifBlank { prev.deliveryAddress }
        )
        if (updated != prev) {
            _savedDeliveryInfo.value = updated
            adminPrefs.edit()
                .putString("saved_cust_name", updated.fullName)
                .putString("saved_cust_phone", updated.phoneNumber)
                .putString("saved_cust_address", updated.deliveryAddress)
                .apply()
        }
        _currentCustomer.value?.let { cust ->
            val updatedCust = cust.copy(
                fullName = updated.fullName.ifBlank { cust.fullName },
                phoneNumber = updated.phoneNumber.ifBlank { cust.phoneNumber },
                deliveryAddress = updated.deliveryAddress.ifBlank { cust.deliveryAddress }
            )
            if (updatedCust != cust) {
                _currentCustomer.value = updatedCust
                viewModelScope.launch {
                    repository.updateCustomerDeliveryDetails(
                        customerId = cust.id,
                        fullName = updatedCust.fullName,
                        phone = updatedCust.phoneNumber,
                        address = updatedCust.deliveryAddress
                    )
                }
            }
        }
    }

    // Admin Promo Activation State with Strong Persistence & Live Countdown Expiry
    private val _adminPromo = MutableStateFlow(
        AdminPromoConfig(
            isActive = adminPrefs.getBoolean("promo_is_active", false),
            discountPercent = adminPrefs.getInt("promo_discount_percent", 0),
            title = adminPrefs.getString("promo_title", "") ?: "",
            targetCategory = adminPrefs.getString("promo_target_category", null),
            expiresAtMillis = adminPrefs.getLong("promo_expires_at_ms", 0L)
        )
    )
    val adminPromo: StateFlow<AdminPromoConfig> = _adminPromo.asStateFlow()

    fun updatePromo(
        isActive: Boolean,
        discountPercent: Int,
        title: String,
        targetCategory: String? = null,
        durationHours: Int = 24
    ) {
        val cleanPercent = discountPercent.coerceIn(0, 90)
        val expiryMs = if (isActive && durationHours > 0) {
            System.currentTimeMillis() + (durationHours * 3_600_000L)
        } else 0L
        adminPrefs.edit()
            .putBoolean("promo_is_active", isActive)
            .putInt("promo_discount_percent", cleanPercent)
            .putString("promo_title", title)
            .putString("promo_target_category", targetCategory)
            .putLong("promo_expires_at_ms", expiryMs)
            .apply()

        _adminPromo.value = AdminPromoConfig(
            isActive = isActive,
            discountPercent = cleanPercent,
            title = title,
            targetCategory = targetCategory,
            expiresAtMillis = expiryMs
        )
        if (isActive) {
            ShopNotificationHelper.notifyPromoActivated(
                context = getApplication(),
                title = title,
                discountPercent = cleanPercent,
                category = targetCategory
            )
        }
    }

    fun togglePromo(isActive: Boolean) {
        val current = _adminPromo.value
        val nextExpiry = if (isActive && current.expiresAtMillis <= System.currentTimeMillis()) {
            System.currentTimeMillis() + (24 * 3_600_000L)
        } else current.expiresAtMillis
        adminPrefs.edit()
            .putBoolean("promo_is_active", isActive)
            .putLong("promo_expires_at_ms", nextExpiry)
            .apply()
        val updated = current.copy(isActive = isActive, expiresAtMillis = nextExpiry)
        _adminPromo.value = updated
        if (isActive) {
            ShopNotificationHelper.notifyPromoActivated(
                context = getApplication(),
                title = updated.title,
                discountPercent = updated.discountPercent,
                category = updated.targetCategory
            )
        }
    }

    fun getEffectiveDiscountPercent(productCategory: String): Int {
        val promo = _adminPromo.value
        val notExpired = promo.expiresAtMillis <= 0L || promo.expiresAtMillis > System.currentTimeMillis()
        return if (promo.isActive && notExpired && (promo.targetCategory == null || promo.targetCategory.equals(productCategory, ignoreCase = true))) {
            promo.discountPercent
        } else {
            0
        }
    }

    // Promo Coupon Codes (Admin-created, persisted in SharedPreferences)
    private fun loadPromoCoupons(): List<PromoCoupon> {
        val raw = adminPrefs.getString("promo_coupons_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                val code = obj.optString("code").trim().uppercase()
                if (code.isBlank()) return@mapNotNull null
                PromoCoupon(
                    code = code,
                    discountPercent = obj.optInt("discountPercent", 10).coerceIn(1, 90),
                    minOrderAmount = obj.optDouble("minOrderAmount", 0.0).coerceAtLeast(0.0),
                    isActive = obj.optBoolean("isActive", true),
                    description = obj.optString("description", "")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persistPromoCoupons(list: List<PromoCoupon>) {
        val arr = JSONArray()
        list.forEach { c ->
            arr.put(
                JSONObject().apply {
                    put("code", c.code)
                    put("discountPercent", c.discountPercent)
                    put("minOrderAmount", c.minOrderAmount)
                    put("isActive", c.isActive)
                    put("description", c.description)
                }
            )
        }
        adminPrefs.edit().putString("promo_coupons_json", arr.toString()).apply()
    }

    private val _promoCoupons = MutableStateFlow(loadPromoCoupons())
    val promoCoupons: StateFlow<List<PromoCoupon>> = _promoCoupons.asStateFlow()

    fun savePromoCoupon(
        code: String,
        discountPercent: Int,
        minOrderAmount: Double = 0.0,
        description: String = ""
    ): Boolean {
        val cleanCode = code.trim().uppercase().replace(" ", "")
        if (cleanCode.length < 3) return false
        val coupon = PromoCoupon(
            code = cleanCode,
            discountPercent = discountPercent.coerceIn(1, 90),
            minOrderAmount = minOrderAmount.coerceAtLeast(0.0),
            isActive = true,
            description = description.trim()
        )
        val updated = (_promoCoupons.value.filterNot { it.code.equals(cleanCode, ignoreCase = true) } + coupon)
            .sortedByDescending { it.discountPercent }
        _promoCoupons.value = updated
        persistPromoCoupons(updated)
        return true
    }

    fun togglePromoCoupon(code: String, isActive: Boolean) {
        val updated = _promoCoupons.value.map {
            if (it.code.equals(code, ignoreCase = true)) it.copy(isActive = isActive) else it
        }
        _promoCoupons.value = updated
        persistPromoCoupons(updated)
    }

    fun deletePromoCoupon(code: String) {
        val updated = _promoCoupons.value.filterNot { it.code.equals(code, ignoreCase = true) }
        _promoCoupons.value = updated
        persistPromoCoupons(updated)
    }

    fun validateCoupon(rawCode: String, subtotal: Double): Pair<PromoCoupon?, String> {
        val clean = rawCode.trim().uppercase()
        if (clean.isBlank()) return null to "Enter a promo code."
        val matched = _promoCoupons.value.find { it.code.equals(clean, ignoreCase = true) }
            ?: return null to "Coupon code '$clean' not found."
        if (!matched.isActive) return null to "Coupon '$clean' is currently paused by Admin."
        if (subtotal < matched.minOrderAmount) {
            val naira = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("en", "NG")).apply {
                maximumFractionDigits = 0
            }
            return null to "Minimum order of ${naira.format(matched.minOrderAmount)} required for '$clean'."
        }
        val saved = subtotal * (matched.discountPercent / 100.0)
        val naira = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("en", "NG")).apply {
            maximumFractionDigits = 0
        }
        return matched to "Coupon '$clean' applied! ${matched.discountPercent}% OFF (-${naira.format(saved)})"
    }

    // Customer Product Reviews & Verified Purchase Ratings
    private fun loadProductReviews(): List<ProductReview> {
        val raw = adminPrefs.getString("product_reviews_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                ProductReview(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    productId = obj.optString("productId", ""),
                    productTitle = obj.optString("productTitle", ""),
                    customerId = obj.optString("customerId", ""),
                    customerName = obj.optString("customerName", "Customer"),
                    rating = obj.optInt("rating", 5).coerceIn(1, 5),
                    comment = obj.optString("comment", ""),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    isVerifiedPurchase = obj.optBoolean("isVerifiedPurchase", false),
                    adminReply = obj.optString("adminReply", ""),
                    adminRepliedAt = obj.optLong("adminRepliedAt", 0L)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persistProductReviews(reviews: List<ProductReview>) {
        val arr = JSONArray()
        reviews.forEach { r ->
            arr.put(
                JSONObject().apply {
                    put("id", r.id)
                    put("productId", r.productId)
                    put("productTitle", r.productTitle)
                    put("customerId", r.customerId)
                    put("customerName", r.customerName)
                    put("rating", r.rating)
                    put("comment", r.comment)
                    put("createdAt", r.createdAt)
                    put("isVerifiedPurchase", r.isVerifiedPurchase)
                    put("adminReply", r.adminReply)
                    put("adminRepliedAt", r.adminRepliedAt)
                }
            )
        }
        adminPrefs.edit().putString("product_reviews_json", arr.toString()).apply()
    }

    private val _productReviews = MutableStateFlow(loadProductReviews())
    val productReviews: StateFlow<List<ProductReview>> = _productReviews.asStateFlow()

    fun hasVerifiedDeliveredPurchase(productTitle: String): Boolean {
        val cleanTitle = productTitle.trim()
        if (cleanTitle.isBlank()) return false
        return allOrders.value.any { ord ->
            ord.status == OrderStatus.DELIVERED &&
                ord.itemsSummary.contains(cleanTitle, ignoreCase = true)
        }
    }

    fun getDeliveredOrderedProducts(): List<Product> {
        val deliveredOrders = allOrders.value.filter { it.status == OrderStatus.DELIVERED }
        if (deliveredOrders.isEmpty()) return emptyList()
        return allProducts.value.filter { prod ->
            prod.title.isNotBlank() && deliveredOrders.any { ord ->
                ord.itemsSummary.contains(prod.title, ignoreCase = true)
            }
        }
    }

    fun submitProductReview(
        productId: String,
        rating: Int,
        comment: String,
        onResult: ((Boolean, String) -> Unit)? = null
    ) {
        val cleanComment = comment.trim()
        if (cleanComment.isBlank()) {
            onResult?.invoke(false, "Please write a short review comment.")
            return
        }
        val prod = allProducts.value.find { it.id == productId }
        if (prod == null || !hasVerifiedDeliveredPurchase(prod.title)) {
            onResult?.invoke(false, "Ratings & reviews are active ONLY on delivered ordered products.")
            return
        }
        val cleanRating = rating.coerceIn(1, 5)
        val customer = _currentCustomer.value
        val reviewerName = customer?.fullName?.ifBlank { null }
            ?: _savedDeliveryInfo.value.fullName.ifBlank { "Verified Shopper" }
        val newReview = ProductReview(
            productId = productId,
            productTitle = prod.title,
            customerId = customer?.id ?: "guest",
            customerName = reviewerName,
            rating = cleanRating,
            comment = cleanComment,
            createdAt = System.currentTimeMillis(),
            isVerifiedPurchase = true
        )
        val updatedReviews = listOf(newReview) + _productReviews.value
        _productReviews.value = updatedReviews
        persistProductReviews(updatedReviews)

        val productRevList = updatedReviews.filter { it.productId == productId }
        val newReviewCount = prod.reviewCount + 1
        val combinedRating = if (prod.reviewCount > 0) {
            ((prod.rating * prod.reviewCount) + cleanRating.toDouble()) / newReviewCount
        } else {
            productRevList.map { it.rating }.average()
        }
        val roundedRating = Math.round(combinedRating * 10.0) / 10.0
        val updatedProd = prod.copy(rating = roundedRating, reviewCount = newReviewCount)
        if (_selectedProduct.value?.id == productId) {
            _selectedProduct.value = updatedProd
        }
        viewModelScope.launch {
            repository.saveProduct(updatedProd)
        }
        ShopNotificationHelper.notifyCustomerNewReviewToAdmin(
            context = getApplication(),
            reviewerName = reviewerName,
            productTitle = prod.title,
            rating = cleanRating
        )
        voiceManager.speakText("New $cleanRating star rating and feedback received from $reviewerName for ${prod.title}.")
        startLiveCallSignalingIfConfigured()
        liveCallClient.sendSignal(
            "CUSTOMER_REVIEW",
            "global_reviews",
            JSONObject().apply {
                put("id", newReview.id)
                put("productId", newReview.productId)
                put("productTitle", prod.title)
                put("customerId", newReview.customerId)
                put("customerName", reviewerName)
                put("rating", cleanRating)
                put("comment", cleanComment)
                put("createdAt", newReview.createdAt)
            }
        )
        onResult?.invoke(true, "Thank you! Your $cleanRating★ review has been sent to Admin & published.")
    }

    fun replyToProductReview(reviewId: String, replyText: String) {
        val clean = replyText.trim()
        if (clean.isBlank()) return
        val target = _productReviews.value.find { it.id == reviewId } ?: return
        val updated = _productReviews.value.map { rev ->
            if (rev.id == reviewId) {
                rev.copy(adminReply = clean, adminRepliedAt = System.currentTimeMillis())
            } else rev
        }
        _productReviews.value = updated
        persistProductReviews(updated)
        ShopNotificationHelper.notifySupportTicketReply(
            context = getApplication(),
            ticketId = target.productTitle.ifBlank { target.productId },
            subject = "Admin Reply on Your ${target.rating}★ Review",
            reply = clean
        )
        voiceManager.speakText("Admin reply saved on ${target.customerName}'s review.")
    }

    // Admin Price Bargain Config State & Methods with Strong Persistence
    // Admin Price Bargain Config State & Methods with Strong Persistence
    private val _adminBargainConfig = MutableStateFlow(
        AdminBargainConfig(
            isActive = adminPrefs.getBoolean("bargain_is_active", true),
            maxBargainDiscountPercent = adminPrefs.getFloat("bargain_max_discount_percent", 10.0f).toDouble(),
            defaultBargainAmount = adminPrefs.getFloat("bargain_default_amount", 5000.0f).toDouble(),
            designatedCategory = adminPrefs.getString("bargain_designated_category", "All") ?: "All",
            minCheckoutThreshold = adminPrefs.getFloat("bargain_min_checkout_threshold", 0.0f).toDouble(),
            categoryBargainAmounts = loadCategoryBargainAmounts()
        )
    )
    val adminBargainConfig: StateFlow<AdminBargainConfig> = _adminBargainConfig.asStateFlow()

    private fun loadCategoryBargainAmounts(): Map<String, Double> {
        val map = mutableMapOf<String, Double>()
        com.example.data.model.ProductCategory.values().forEach { cat ->
            val key = "bargain_cat_${cat.displayName}"
            if (adminPrefs.contains(key)) {
                map[cat.displayName] = adminPrefs.getFloat(key, 5000.0f).toDouble()
            }
        }
        return map
    }

    fun updateBargainConfig(
        isActive: Boolean,
        maxDiscountPercent: Double,
        defaultAmount: Double = 5000.0,
        designatedCategory: String = "All",
        minThreshold: Double = 0.0,
        categoryAmounts: Map<String, Double> = emptyMap()
    ) {
        val maxPct = maxDiscountPercent.coerceIn(0.1, 90.0)
        val defAmt = defaultAmount.coerceAtLeast(0.0)
        val threshold = minThreshold.coerceAtLeast(0.0)

        val editor = adminPrefs.edit()
            .putBoolean("bargain_is_active", isActive)
            .putFloat("bargain_max_discount_percent", maxPct.toFloat())
            .putFloat("bargain_default_amount", defAmt.toFloat())
            .putString("bargain_designated_category", designatedCategory)
            .putFloat("bargain_min_checkout_threshold", threshold.toFloat())

        categoryAmounts.forEach { (cat, amt) ->
            editor.putFloat("bargain_cat_$cat", amt.toFloat())
        }
        editor.apply()

        _adminBargainConfig.value = AdminBargainConfig(
            isActive = isActive,
            maxBargainDiscountPercent = maxPct,
            defaultBargainAmount = defAmt,
            designatedCategory = designatedCategory,
            categoryBargainAmounts = categoryAmounts,
            minCheckoutThreshold = threshold
        )
    }

    fun isCategoryEligibleForBargain(category: String): Boolean {
        val config = _adminBargainConfig.value
        if (!config.isActive) return false
        if (config.designatedCategory.equals("All", ignoreCase = true)) return true
        return config.designatedCategory.equals(category, ignoreCase = true)
    }

    fun getDefaultBargainAmountForCategory(category: String): Double {
        val config = _adminBargainConfig.value
        return config.categoryBargainAmounts[category] ?: config.defaultBargainAmount
    }

    fun toggleBargainActive(isActive: Boolean) {
        adminPrefs.edit().putBoolean("bargain_is_active", isActive).apply()
        _adminBargainConfig.value = _adminBargainConfig.value.copy(isActive = isActive)
    }

    data class BargainEvaluationResult(
        val approvedPrice: Double,
        val approvedDiscountPercent: Double,
        val isCappedAtMax: Boolean,
        val message: String
    )

    fun evaluateBargain(
        catalogPrice: Double,
        proposedPrice: Double,
        category: String? = null
    ): BargainEvaluationResult {
        val config = _adminBargainConfig.value
        val nairaFormat = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("en", "NG")).apply {
            maximumFractionDigits = 0
        }

        if (proposedPrice >= catalogPrice || proposedPrice <= 0.0) {
            return BargainEvaluationResult(
                approvedPrice = catalogPrice,
                approvedDiscountPercent = 0.0,
                isCappedAtMax = false,
                message = "Standard catalog price retained."
            )
        }

        // Check if designated to a specific category
        if (category != null && !isCategoryEligibleForBargain(category)) {
            return BargainEvaluationResult(
                approvedPrice = catalogPrice,
                approvedDiscountPercent = 0.0,
                isCappedAtMax = true,
                message = "Price bargaining is designated exclusively for '${config.designatedCategory}'. Standard catalog price applies for this item."
            )
        }

        val requestedDiscountAmount = catalogPrice - proposedPrice
        val requestedDiscountPercent = (requestedDiscountAmount / catalogPrice) * 100.0
        val maxAllowed = config.maxBargainDiscountPercent
        val defaultAmount = if (category != null) getDefaultBargainAmountForCategory(category) else config.defaultBargainAmount

        // Effective max discount considers both percentage cap and default amount (capped at 90% of price)
        val maxDiscountByPercent = catalogPrice * (maxAllowed / 100.0)
        val effectiveMaxDiscount = maxOf(maxDiscountByPercent, defaultAmount).coerceAtMost(catalogPrice * 0.90)

        return if (requestedDiscountAmount <= effectiveMaxDiscount) {
            BargainEvaluationResult(
                approvedPrice = proposedPrice,
                approvedDiscountPercent = Math.round(requestedDiscountPercent * 10.0) / 10.0,
                isCappedAtMax = false,
                message = "Bargain Approved! You proposed ${nairaFormat.format(proposedPrice)} (${String.format(java.util.Locale.US, "%.1f", requestedDiscountPercent)}% off). Saved ${nairaFormat.format(requestedDiscountAmount)}!"
            )
        } else {
            val cappedPrice = (catalogPrice - effectiveMaxDiscount).coerceAtLeast(catalogPrice * 0.10)
            val savedAmount = catalogPrice - cappedPrice
            val approvedPercent = (savedAmount / catalogPrice) * 100.0
            BargainEvaluationResult(
                approvedPrice = cappedPrice,
                approvedDiscountPercent = Math.round(approvedPercent * 10.0) / 10.0,
                isCappedAtMax = true,
                message = "Bargain Approved at Admin Limit! You proposed a discount of ${nairaFormat.format(requestedDiscountAmount)}, capped at ${nairaFormat.format(effectiveMaxDiscount)}. Approved price: ${nairaFormat.format(cappedPrice)} (Saved ${nairaFormat.format(savedAmount)})!"
            )
        }
    }

    private val appLaunchTimestampMs = System.currentTimeMillis()
    @Volatile
    private var hasCompletedInitialCloudPull = false

    private val _lastSyncTimestamp = MutableStateFlow(System.currentTimeMillis())
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    // Back-in-Stock Alerts (#7) — declared before init{} because allProducts collector reads it on launch
    private val _backInStockProductIds = MutableStateFlow(
        adminPrefs.getStringSet("back_in_stock_product_ids", emptySet())?.toSet() ?: emptySet()
    )
    val backInStockProductIds: StateFlow<Set<String>> = _backInStockProductIds.asStateFlow()

    // Offline Queue (#10) — declared before offlineConnectivityManager because reconnect callback can fire immediately
    private val _offlineQueue = MutableStateFlow(loadOfflineQueue())
    val offlineQueue: StateFlow<List<OfflineQueuedAction>> = _offlineQueue.asStateFlow()

    private val offlineConnectivityManager = OfflineConnectivityManager(application) {
        viewModelScope.launch {
            flushOfflineQueue()
        }
    }
    val isOnline: StateFlow<Boolean> = offlineConnectivityManager.isOnline

    init {
        _preOrderRequests.value = loadPreOrderRequests()
        _latestVersionName.value = _storeProfile.value.latestAppVersionName
        if (_storeProfile.value.updateReleaseNotes.isNotBlank()) {
            _releaseNotes.value = _storeProfile.value.updateReleaseNotes
        }
        com.example.util.WebPortalExporter.purgeLegacyApkUrlPref(application)
        _updateDownloadUrl.value = _storeProfile.value.apkDownloadUrl
        if (_storeProfile.value.apkDownloadUrl.isNotBlank() &&
            !com.example.util.WebPortalExporter.isDirectApkReleaseUrl(_storeProfile.value.apkDownloadUrl) &&
            com.example.util.WebPortalExporter.getSavedCustomPortalUrl(application).isBlank()
        ) {
            com.example.util.WebPortalExporter.saveCustomPortalUrl(application, _storeProfile.value.apkDownloadUrl)
        }
        CatalogBackgroundSyncJobService.schedulePeriodicSync(application)

        // Monitor restocked items for any customer Back-in-Stock Watch alerts
        viewModelScope.launch {
            allProducts.collect { products ->
                val watched = _backInStockProductIds.value
                if (watched.isNotEmpty() && products.isNotEmpty()) {
                    val restocked = products.filter { it.id in watched && it.stockQuantity > 0 }
                    if (restocked.isNotEmpty()) {
                        restocked.forEach { p ->
                            ShopNotificationHelper.notifyBackInStock(getApplication(), p.id, p.title)
                        }
                        val remaining = watched - restocked.map { it.id }.toSet()
                        _backInStockProductIds.value = remaining
                        adminPrefs.edit().putStringSet("back_in_stock_product_ids", remaining).apply()
                    }
                }
            }
        }

        // Restore persisted Supabase URL & Publishable/Anon Key if valid, or pre-load from BuildConfig Secrets
        val prefUrl = adminPrefs.getString("supabase_url", null).orEmpty()
        val prefKey = adminPrefs.getString("supabase_anon_key", null).orEmpty()
        val buildCfgUrl = runCatching { BuildConfig.SUPABASE_URL }.getOrDefault("").trim()
        val buildCfgKey = runCatching { BuildConfig.SUPABASE_ANON_KEY }.getOrDefault("").trim()

        val (resolvedUrl, resolvedKey) = when {
            prefUrl.isNotBlank() && prefKey.isNotBlank() &&
                !repository.supabaseSync.isPlaceholderConfig(prefUrl, prefKey) -> prefUrl to prefKey
            buildCfgUrl.isNotBlank() && buildCfgKey.isNotBlank() &&
                !repository.supabaseSync.isPlaceholderConfig(buildCfgUrl, buildCfgKey) -> {
                val cleanBu = repository.supabaseSync.cleanUrl(buildCfgUrl)
                val cleanBk = repository.supabaseSync.sanitizeKey(buildCfgKey)
                adminPrefs.edit()
                    .putString("supabase_url", cleanBu)
                    .putString("supabase_anon_key", cleanBk)
                    .apply()
                cleanBu to cleanBk
            }
            else -> "" to ""
        }

        if (resolvedUrl.isNotBlank() && resolvedKey.isNotBlank() &&
            !repository.supabaseSync.isPlaceholderConfig(resolvedUrl, resolvedKey)
        ) {
            repository.supabaseSync.updateConfig(
                SupabaseConfig(url = resolvedUrl, anonKey = resolvedKey, isConnected = false)
            )
            _supabaseConfigState.value = repository.supabaseSync.getConfig()
            _cloudStatusMessage.value = "Saved Supabase URL loaded. Connecting live..."
            viewModelScope.launch {
                val testRes = repository.supabaseSync.testConnection()
                if (testRes.isSuccess) {
                    _isSupabaseConnected.value = true
                    val connectedCfg = repository.supabaseSync.getConfig().copy(isConnected = true)
                    repository.supabaseSync.updateConfig(connectedCfg)
                    _supabaseConfigState.value = repository.supabaseSync.getConfig()
                    val syncRes = repository.fetchAndSyncAllFromCloudDetailed()
                    syncPreOrdersWithCloudInternal(notifyOnNewRemote = false)
                    syncPartnerLedgerWithCloudInternal()
                    runAutomatic30DayHostingBilling()
                    hasCompletedInitialCloudPull = true
                    if (syncRes.isSuccess) {
                        val s = syncRes.getOrThrow()
                        _lastSyncTimestamp.value = System.currentTimeMillis()
                        _cloudStatusMessage.value = if (s.errors.isEmpty()) {
                            "✅ Connected! Synced ${s.syncedProducts} products (${s.customCloudProducts} uploaded), ${s.syncedCustomers} customers & ${s.syncedOrders} orders."
                        } else {
                            "⚠️ Connected with sync warnings: ${s.errors.joinToString("; ")}"
                        }
                    } else {
                        _cloudStatusMessage.value = testRes.getOrThrow()
                    }
                    checkForAppUpdates(showPopupIfFound = true, onResult = null)
                    startLiveCallSignalingIfConfigured()
                }
            }
        } else {
            // Clear any legacy placeholder values from SharedPreferences so UI starts clean
            adminPrefs.edit().remove("supabase_url").remove("supabase_anon_key").apply()
            _supabaseConfigState.value = SupabaseConfig()
        }

        // Restore ONLY the customer explicitly signed in on THIS device (never auto-seed or hijack demo accounts)
        viewModelScope.launch {
            repository.allCustomers.collect { customers ->
                val savedLoggedInId = adminPrefs.getString("logged_in_customer_id", null)
                if (!savedLoggedInId.isNullOrBlank() && customers.isNotEmpty()) {
                    val matched = customers.find { it.id == savedLoggedInId }
                    if (matched != null && (_currentCustomer.value == null || _currentCustomer.value?.id == matched.id)) {
                        _currentCustomer.value = matched
                        _wishlistIds.value = matched.wishlistProductIds.toSet()
                        saveCustomerDeliveryInfo(matched.fullName, matched.phoneNumber, matched.deliveryAddress)
                    }
                }
            }
        }

        // Backfill permanent delivery info & notify customer on real-time order status changes
        viewModelScope.launch {
            val previousStatuses = mutableMapOf<String, OrderStatus>()
            var seededStatuses = false
            repository.allOrders.collect { orders ->
                if (!seededStatuses) {
                    orders.forEach { previousStatuses[it.orderId] = it.status }
                    seededStatuses = true
                } else {
                    orders.forEach { ord ->
                        val prev = previousStatuses[ord.orderId]
                        if (prev != null && prev != ord.status) {
                            ShopNotificationHelper.notifyOrderStatusChange(
                                context = getApplication(),
                                orderId = ord.orderId,
                                newStatus = ord.status,
                                note = ord.verificationNote
                            )
                        }
                        previousStatuses[ord.orderId] = ord.status
                    }
                }

                val cur = _savedDeliveryInfo.value
                if (cur.fullName.isBlank() && cur.phoneNumber.isBlank() && cur.deliveryAddress.isBlank() && orders.isNotEmpty()) {
                    val latest = orders.first()
                    val cleanAddr = latest.deliveryAddress.replace(Regex("""\s*\[Delivery:[^\]]*\]"""), "").trim()
                    persistDeliveryInfoLocally(
                        fullName = latest.customerName,
                        phoneNumber = latest.customerPhone,
                        deliveryAddress = cleanAddr,
                        senderName = latest.senderName
                    )
                }
            }
        }

        // Real-Time Live Background Sync across all installed devices (every 10s)
        viewModelScope.launch {
            while (isActive) {
                delay(if (hasCompletedInitialCloudPull) 10000 else 3000)
                try {
                    if (!repository.supabaseSync.isPlaceholderConfig()) {
                        val wasBaselineEstablished = hasCompletedInitialCloudPull
                        val syncRes = repository.fetchAndSyncAllFromCloudDetailed()
                        syncPreOrdersWithCloudInternal(notifyOnNewRemote = wasBaselineEstablished)
                        syncPartnerLedgerWithCloudInternal()
                        runAutomatic30DayHostingBilling()
                        if (syncRes.isSuccess) {
                            val summary = syncRes.getOrThrow()
                            hasCompletedInitialCloudPull = true
                            if (summary.errors.isEmpty()) {
                                _isSupabaseConnected.value = true
                            }
                            _lastSyncTimestamp.value = System.currentTimeMillis()
                            if (wasBaselineEstablished) {
                                val recentCutoffMs = appLaunchTimestampMs - 300_000L
                                val newOrd = summary.latestNewOrder
                                if (summary.newRemoteOrders > 0 && newOrd != null && newOrd.createdAt >= recentCutoffMs) {
                                    voiceManager.speakNewOrderAlert(
                                        customerName = newOrd.customerName.ifBlank { "A shopper" },
                                        totalAmountText = "${newOrd.totalAmount.toInt()} Naira",
                                        orderId = newOrd.orderId
                                    )
                                    _cloudStatusMessage.value = "✅ New Order ${newOrd.orderId} received from ${newOrd.customerName}!"
                                } else if (summary.newRemoteCustomers > 0 && summary.latestNewCustomerCreatedAt >= recentCutoffMs) {
                                    val custName = summary.latestNewCustomerName ?: "New Customer"
                                    voiceManager.speakText("New customer sign up received! $custName just joined ODIYA.")
                                    _cloudStatusMessage.value = "✅ New customer registration synced: $custName"
                                }
                            }
                        }
                    }
                } catch (_: Exception) {
                    // ignore transient network glitches during background polling
                }
            }
        }
    }

    fun refreshData(onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            _isRefreshing.value = true
            val res = repository.fetchAndSyncAllFromCloudDetailed()
            syncPreOrdersWithCloudInternal(notifyOnNewRemote = false)
            syncPartnerLedgerWithCloudInternal()
            runAutomatic30DayHostingBilling()
            _isRefreshing.value = false
            _lastSyncTimestamp.value = System.currentTimeMillis()
            res.onSuccess { summary ->
                if (summary.errors.isEmpty()) {
                    _isSupabaseConnected.value = true
                    _cloudStatusMessage.value = "✅ Synced: ${summary.syncedProducts} products (${summary.customCloudProducts} custom), ${summary.syncedCustomers} customers, ${summary.syncedOrders} orders & ${_preOrderRequests.value.size} pre-orders."
                } else {
                    _cloudStatusMessage.value = "⚠️ Partial sync: ${summary.errors.joinToString("; ")}"
                }
            }.onFailure { err ->
                if (!repository.supabaseSync.isPlaceholderConfig()) {
                    _cloudStatusMessage.value = "❌ Sync failed: ${err.message}"
                }
            }
            onComplete?.invoke(res.isSuccess)
        }
    }

    fun syncAllCloudRecordsNow(onComplete: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            _isSupabaseBusy.value = true
            _isRefreshing.value = true
            _cloudStatusMessage.value = "🔄 Syncing catalog, customers, orders, pre-orders & receipts with Supabase..."
            val res = repository.fetchAndSyncAllFromCloudDetailed()
            syncPreOrdersWithCloudInternal(notifyOnNewRemote = false)
            syncPartnerLedgerWithCloudInternal()
            runAutomatic30DayHostingBilling()
            _isSupabaseBusy.value = false
            _isRefreshing.value = false
            _lastSyncTimestamp.value = System.currentTimeMillis()
            val msg = if (res.isSuccess) {
                val s = res.getOrThrow()
                if (s.errors.isEmpty()) {
                    _isSupabaseConnected.value = true
                    "✅ Live Sync Complete! Cloud: ${s.syncedProducts} products (${s.customCloudProducts} uploaded), ${s.syncedCustomers} customers (+${s.newRemoteCustomers} new), ${s.syncedOrders} orders (+${s.newRemoteOrders} new), ${_preOrderRequests.value.size} pre-orders | Pushed local: ${s.pushedLocalProducts} prod, ${s.pushedLocalCustomers} cust, ${s.pushedLocalOrders} ord."
                } else {
                    "⚠️ Sync completed with table warnings: ${s.errors.joinToString("; ")}"
                }
            } else {
                "❌ ${res.exceptionOrNull()?.message ?: "Supabase not connected on this device."}"
            }
            _cloudStatusMessage.value = msg
            onComplete?.invoke(msg)
        }
    }

    fun checkForAppUpdates(
        showPopupIfFound: Boolean = true,
        onResult: ((isLatest: Boolean, message: String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _isRefreshing.value = true
            _cloudStatusMessage.value = "🔄 Checking for app updates..."
            val syncRes = repository.fetchAndSyncAllFromCloud()
            val portalUrl = com.example.util.WebPortalExporter.getSavedCustomPortalUrl(getApplication())
            val remoteManifest = repository.supabaseSync.fetchAppUpdateManifest(portalUrl).getOrNull()
            _isRefreshing.value = false
            _lastSyncTimestamp.value = System.currentTimeMillis()

            val (installedCode, installedName) = getInstalledAppVersion()
            val profile = _storeProfile.value
            val effectiveRemoteCode = maxOf(
                remoteManifest?.versionCode ?: 1,
                profile.latestAppVersionCode
            )
            val effectiveRemoteName = remoteManifest?.versionName?.takeIf { it.isNotBlank() }
                ?: profile.latestAppVersionName.ifBlank { installedName }
            val effectiveNotes = remoteManifest?.releaseNotes?.takeIf { it.isNotBlank() }
                ?: profile.updateReleaseNotes
            val effectiveDownloadUrl = listOfNotNull(
                remoteManifest?.downloadUrl,
                profile.apkDownloadUrl,
                portalUrl
            ).map { com.example.util.WebPortalExporter.normalizeApkOrPortalUrl(it) }
                .firstOrNull {
                    it.isNotBlank() &&
                        !com.example.util.WebPortalExporter.isAiStudioPreviewHost(it) &&
                        !com.example.util.WebPortalExporter.isLegacyDeprecatedHost(it)
                }.orEmpty()

            if (effectiveDownloadUrl.isNotBlank()) {
                if (!com.example.util.WebPortalExporter.isDirectApkReleaseUrl(effectiveDownloadUrl) &&
                    com.example.util.WebPortalExporter.getSavedCustomPortalUrl(getApplication()).isBlank()
                ) {
                    com.example.util.WebPortalExporter.saveCustomPortalUrl(getApplication(), effectiveDownloadUrl)
                }
                val cleanCurrentApk = com.example.util.WebPortalExporter.normalizeApkOrPortalUrl(profile.apkDownloadUrl)
                if (cleanCurrentApk.isBlank() || cleanCurrentApk != profile.apkDownloadUrl) {
                    updateStoreProfile(profile.copy(apkDownloadUrl = effectiveDownloadUrl))
                }
            }

            if (effectiveRemoteCode > installedCode) {
                _latestVersionName.value = effectiveRemoteName
                _releaseNotes.value = effectiveNotes
                _updateDownloadUrl.value = effectiveDownloadUrl
                if (showPopupIfFound) {
                    _updateAvailable.value = true
                }
                val lastNotified = adminPrefs.getInt("last_notified_update_code", 0)
                if (effectiveRemoteCode > lastNotified) {
                    adminPrefs.edit().putInt("last_notified_update_code", effectiveRemoteCode).apply()
                    ShopNotificationHelper.notifyAppUpdateAvailable(
                        context = getApplication(),
                        versionName = effectiveRemoteName,
                        versionCode = effectiveRemoteCode,
                        notes = effectiveNotes
                    )
                }
                val msg = "🚀 New update v$effectiveRemoteName (Build $effectiveRemoteCode) available!"
                _cloudStatusMessage.value = msg
                onResult?.invoke(false, msg)
            } else {
                val msg = if (syncRes.isSuccess) {
                    "✅ App v$installedName (Build $installedCode) is up to date & live cloud synced."
                } else {
                    "✅ Installed App v$installedName (Build $installedCode) is on the latest build."
                }
                _cloudStatusMessage.value = msg
                onResult?.invoke(true, msg)
            }
        }
    }

    // Auth Screen Tab State (0 = Sign In, 1 = Sign Up)
    private val _authTab = MutableStateFlow(0)
    val authTab: StateFlow<Int> = _authTab.asStateFlow()

    // Navigation Methods
    fun navigateTo(screen: AppScreen) {
        if (_currentScreen.value != screen) {
            screenStack.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun navigateToSignUp() {
        _authTab.value = 1
        navigateTo(AppScreen.AUTH)
    }

    fun navigateToSignIn() {
        _authTab.value = 0
        navigateTo(AppScreen.AUTH)
    }

    fun navigateBack(): Boolean {
        if (screenStack.isNotEmpty()) {
            _currentScreen.value = screenStack.removeAt(screenStack.size - 1)
            return true
        }
        if (_currentScreen.value != AppScreen.HOME) {
            _currentScreen.value = AppScreen.HOME
            return true
        }
        return false
    }

    fun navigateToHome() {
        screenStack.clear()
        _currentScreen.value = AppScreen.HOME
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOption(option: CatalogSortOption) {
        _sortOption.value = option
    }

    fun setPriceRangeFilter(filter: PriceRangeFilter) {
        _priceRangeFilter.value = filter
    }

    fun setInStockOnly(enabled: Boolean) {
        _inStockOnly.value = enabled
    }

    fun selectBrandFilter(brand: String?) {
        _selectedBrandFilter.value = brand
    }

    // Recent Search History (#7)
    private fun loadRecentSearches(): List<String> {
        val raw = adminPrefs.getString("recent_searches_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                arr.optString(i).trim().takeIf { it.isNotBlank() }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private val _recentSearches = MutableStateFlow(loadRecentSearches())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    fun recordRecentSearch(query: String) {
        val clean = query.trim()
        if (clean.length < 2) return
        val updated = (listOf(clean) + _recentSearches.value.filterNot { it.equals(clean, ignoreCase = true) }).take(8)
        _recentSearches.value = updated
        val arr = JSONArray().apply { updated.forEach { put(it) } }
        adminPrefs.edit().putString("recent_searches_json", arr.toString()).apply()
    }

    fun clearRecentSearches() {
        _recentSearches.value = emptyList()
        adminPrefs.edit().remove("recent_searches_json").apply()
    }

    fun toggleBackInStockWatch(product: Product): Boolean {
        val current = _backInStockProductIds.value.toMutableSet()
        val added = if (current.contains(product.id)) {
            current.remove(product.id)
            false
        } else {
            current.add(product.id)
            true
        }
        _backInStockProductIds.value = current
        adminPrefs.edit().putStringSet("back_in_stock_product_ids", current).apply()
        return added
    }

    // Single-click Product View
    fun openProductDetails(product: Product) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            com.example.util.ProductImageHelper.preloadSingleProductGallery(getApplication(), product)
        }
        _selectedProduct.value = product
        val firstVariant = product.variantPrices.firstOrNull()
        _detailBrand.value = firstVariant?.brand?.takeIf { it.isNotBlank() }
            ?: product.availableBrands.firstOrNull() ?: product.brand
        _detailSize.value = firstVariant?.size?.takeIf { it.isNotBlank() }
            ?: product.availableSizes.firstOrNull() ?: "Standard"
        _detailColor.value = if (firstVariant != null && firstVariant.colorName.isNotBlank()) {
            product.availableColors.find { it.name.equals(firstVariant.colorName, ignoreCase = true) }
                ?: ProductColor(firstVariant.colorName, firstVariant.colorHex.ifBlank { "#1E40AF" })
        } else {
            product.availableColors.firstOrNull() ?: ProductColor("Default", "#1E40AF")
        }
        navigateTo(AppScreen.PRODUCT_DETAIL)
    }

    fun updateSelectedProduct(product: Product) {
        _selectedProduct.value = product
    }

    fun setDetailBrand(brand: String) {
        _detailBrand.value = brand
    }

    fun setDetailSize(size: String) {
        _detailSize.value = size
    }

    fun setDetailColor(color: ProductColor) {
        _detailColor.value = color
    }

    // Cart Operations with Strict Stock Enforcement
    fun addToCart(
        product: Product,
        brand: String = product.availableBrands.firstOrNull() ?: product.brand,
        size: String = product.availableSizes.firstOrNull() ?: "Standard",
        color: ProductColor = product.availableColors.firstOrNull() ?: ProductColor("Default", "#1E40AF"),
        quantity: Int = 1,
        customBargainPrice: Double? = null,
        bargainNote: String = "",
        onFeedback: ((Boolean, String) -> Unit)? = null
    ) {
        if (isProductCatalogLocked(product)) {
            onFeedback?.invoke(false, "🔒 '${product.title}' is Display Only — Hosting period ended and Add-to-Cart is locked by Admin.")
            return
        }
        if (product.stockQuantity <= 0) {
            onFeedback?.invoke(false, "Sorry, '${product.title}' is currently Out of Stock.")
            return
        }

        val currentList = _cartItems.value.toMutableList()
        val alreadyInCartForProduct = currentList.filter { it.product.id == product.id }.sumOf { it.quantity }
        val remainingStock = (product.stockQuantity - alreadyInCartForProduct).coerceAtLeast(0)
        if (remainingStock <= 0) {
            onFeedback?.invoke(false, "Maximum available stock (${product.stockQuantity}) already in your cart.")
            return
        }

        val allowedQuantity = quantity.coerceIn(1, remainingStock)
        val promoDisc = getEffectiveDiscountPercent(product.category)
        val catalogPrice = product.calculatePrice(brand, size, promoDisc, color.name)
        val finalUnitPrice = customBargainPrice ?: catalogPrice
        val isBargained = customBargainPrice != null && customBargainPrice < catalogPrice

        val existingIndex = currentList.indexOfFirst {
            it.product.id == product.id && it.selectedBrand == brand && it.selectedSize == size && it.selectedColor.name == color.name && it.isBargained == isBargained
        }

        if (existingIndex >= 0) {
            val item = currentList[existingIndex]
            currentList[existingIndex] = item.copy(quantity = item.quantity + allowedQuantity)
        } else {
            currentList.add(
                CartItem(
                    product = product,
                    selectedBrand = brand,
                    selectedSize = size,
                    selectedColor = color,
                    unitPrice = finalUnitPrice,
                    originalPrice = catalogPrice,
                    isBargained = isBargained,
                    bargainNote = bargainNote,
                    quantity = allowedQuantity
                )
            )
        }
        _cartItems.value = currentList
        val msg = if (allowedQuantity < quantity) {
            "Added $allowedQuantity unit(s) (capped at ${product.stockQuantity} in stock)."
        } else {
            "'${product.title}' added to cart!"
        }
        onFeedback?.invoke(true, msg)
    }

    fun updateCartQuantity(cartItemId: String, delta: Int) {
        val currentList = _cartItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.cartItemId == cartItemId }
        if (index >= 0) {
            val item = currentList[index]
            if (delta > 0) {
                val totalForProd = currentList.filter { it.product.id == item.product.id }.sumOf { it.quantity }
                val allowedExtra = (item.product.stockQuantity - totalForProd).coerceAtLeast(0)
                val safeDelta = delta.coerceAtMost(allowedExtra)
                if (safeDelta <= 0) return
                currentList[index] = item.copy(quantity = item.quantity + safeDelta)
            } else {
                val newQty = item.quantity + delta
                if (newQty <= 0) {
                    currentList.removeAt(index)
                } else {
                    currentList[index] = item.copy(quantity = newQty)
                }
            }
            _cartItems.value = currentList
        }
    }

    fun autoUnlockBargainByBoostingQuantity() {
        val currentList = _cartItems.value.toMutableList()
        if (currentList.isEmpty()) return
        val config = _adminBargainConfig.value
        val threshold = config.minCheckoutThreshold
        val currentSubtotal = currentList.sumOf { it.unitPrice * it.quantity }
        if (currentSubtotal >= threshold) return

        val needed = threshold - currentSubtotal
        val itemToBoost = currentList.maxByOrNull { it.unitPrice } ?: currentList.first()
        val unitPrice = itemToBoost.unitPrice
        if (unitPrice <= 0) return
        val totalForProd = currentList.filter { it.product.id == itemToBoost.product.id }.sumOf { it.quantity }
        val maxExtra = (itemToBoost.product.stockQuantity - totalForProd).coerceAtLeast(0)
        if (maxExtra <= 0) return
        val additionalQtyNeeded = kotlin.math.ceil(needed / unitPrice).toInt().coerceIn(1, maxExtra)

        val index = currentList.indexOfFirst { it.cartItemId == itemToBoost.cartItemId }
        if (index >= 0) {
            val item = currentList[index]
            currentList[index] = item.copy(quantity = item.quantity + additionalQtyNeeded)
            _cartItems.value = currentList
        }
    }

    fun removeCartItem(cartItemId: String) {
        _cartItems.value = _cartItems.value.filterNot { it.cartItemId == cartItemId }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    fun reorderFromOrder(order: Order): Pair<Int, Int> {
        val products = allProducts.value
        if (products.isEmpty()) return 0 to 0
        val linePattern = Regex("""^(\d+)\s*x\s+([^(]+?)(?:\s*\(([^)]+)\))?""", RegexOption.IGNORE_CASE)
        var addedLines = 0
        var skippedLines = 0
        order.itemsSummary.split(";", "\n").forEach { raw ->
            val line = raw.trim()
            if (line.isBlank()) return@forEach
            val match = linePattern.find(line)
            if (match != null) {
                val qty = match.groupValues[1].toIntOrNull()?.coerceAtLeast(1) ?: 1
                val title = match.groupValues[2].trim()
                val variants = match.groupValues.getOrNull(3)?.split(",")?.map { it.trim() }.orEmpty()
                val matchedProd = products.find { it.title.equals(title, ignoreCase = true) }
                    ?: products.find { it.title.contains(title, ignoreCase = true) }
                if (matchedProd != null && matchedProd.stockQuantity > 0 && !isProductCatalogLocked(matchedProd)) {
                    val brand = variants.getOrNull(0)?.takeIf { it.isNotBlank() }
                        ?: matchedProd.availableBrands.firstOrNull() ?: matchedProd.brand
                    val size = variants.getOrNull(1)?.takeIf { it.isNotBlank() }
                        ?: matchedProd.availableSizes.firstOrNull() ?: "Standard"
                    val colorName = variants.getOrNull(2).orEmpty()
                    val color = matchedProd.availableColors.find { it.name.equals(colorName, ignoreCase = true) }
                        ?: matchedProd.availableColors.firstOrNull()
                        ?: ProductColor("Default", "#1E40AF")
                    var didAdd = false
                    addToCart(
                        product = matchedProd,
                        brand = brand,
                        size = size,
                        color = color,
                        quantity = qty,
                        onFeedback = { success, _ -> didAdd = success }
                    )
                    if (didAdd) addedLines++ else skippedLines++
                } else {
                    skippedLines++
                }
            }
        }
        return addedLines to skippedLines
    }

    // Checkout & Order Placement (with Loyalty Points & Delivery Zone support)
    fun placeOrder(
        customerName: String,
        customerEmail: String,
        customerPhone: String,
        deliveryAddress: String,
        paymentMethod: PaymentMethod,
        paymentAccountOrTerminal: String,
        paymentReference: String,
        senderName: String,
        deliveryConditionTitle: String,
        deliveryFeeAmount: Double,
        receiptImageUrl: String = "",
        customBargainTotal: Double? = null,
        appliedCouponCode: String = "",
        couponDiscountAmount: Double = 0.0,
        loyaltyPointsRedeemed: Int = 0,
        loyaltyDiscountAmount: Double = 0.0,
        isCreditAlertReceived: Boolean = false,
        creditVerificationStatus: CreditVerificationStatus = CreditVerificationStatus.PENDING_CREDIT,
        onSuccess: (Order) -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            val items = _cartItems.value
            val lockedCartItem = items.firstOrNull { isProductCatalogLocked(it.product) }
            if (lockedCartItem != null) {
                onFailure("Cannot place order: '${lockedCartItem.product.title}' is currently Display Only (Admin Catalog Lock active).")
                return@launch
            }
            val summary = items.joinToString(";\n") {
                "${it.quantity}x ${it.product.title} (${it.selectedBrand}, ${it.selectedSize}, ${it.selectedColor.name}) - ₦${(it.unitPrice * it.quantity).toInt()}"
            }
            val subtotal = items.sumOf { it.unitPrice * it.quantity }
            val safeCouponDiscount = couponDiscountAmount.coerceIn(0.0, subtotal)
            val safeLoyaltyDiscount = loyaltyDiscountAmount.coerceIn(0.0, (subtotal - safeCouponDiscount).coerceAtLeast(0.0))
            val discountedSubtotal = (subtotal - safeCouponDiscount - safeLoyaltyDiscount).coerceAtLeast(0.0)
            val deliveryFee = deliveryFeeAmount
            val total = customBargainTotal ?: (discountedSubtotal + deliveryFee)
            val effectiveMerchandiseTotal = customBargainTotal
                ?.let { (it - deliveryFee).coerceAtLeast(0.0) }
                ?: discountedSubtotal
            val discountFactor = if (subtotal > 0.0) {
                (effectiveMerchandiseTotal / subtotal).coerceIn(0.0, 1.0)
            } else {
                1.0
            }
            val calculatedItemProfits = items.sumOf { (it.unitPrice - it.product.costPrice) * it.quantity }
            val profit = if (customBargainTotal != null) {
                val bargainDeduction = (subtotal - customBargainTotal).coerceAtLeast(0.0)
                (calculatedItemProfits - bargainDeduction).coerceAtLeast(customBargainTotal * 0.10)
            } else {
                (calculatedItemProfits - safeCouponDiscount - safeLoyaltyDiscount).coerceAtLeast(discountedSubtotal * 0.15)
            }
            val couponNoteSuffix = if (appliedCouponCode.isNotBlank() && safeCouponDiscount > 0.0) {
                " [Coupon: ${appliedCouponCode.uppercase()} (-₦${safeCouponDiscount.toInt()})]"
            } else ""
            val loyaltyNoteSuffix = if (loyaltyPointsRedeemed > 0 && safeLoyaltyDiscount > 0.0) {
                " [Loyalty: -$loyaltyPointsRedeemed pts (-₦${safeLoyaltyDiscount.toInt()})]"
            } else ""

            val partnerItems = items.filter { it.product.partnerId.isNotBlank() }
            val primaryPartnerId = partnerItems.firstOrNull()?.product?.partnerId ?: ""
            val primaryPartnerName = partnerItems.firstOrNull()?.product?.partnerName ?: ""
            val totalPartnerCommissionAmt = partnerItems.sumOf { item ->
                val gross = (item.unitPrice * item.quantity) * discountFactor
                val pct = item.product.partnerCommissionPercent.coerceIn(0.0, 95.0)
                gross * (pct / 100.0)
            }
            val totalPartnerNetAmt = partnerItems.sumOf { item ->
                val gross = (item.unitPrice * item.quantity) * discountFactor
                val pct = item.product.partnerCommissionPercent.coerceIn(0.0, 95.0)
                val share247 = gross * (pct / 100.0)
                (gross - share247).coerceAtLeast(0.0)
            }

            val baseVerificationNote = when (creditVerificationStatus) {
                CreditVerificationStatus.CONFIRMED_CREDITED ->
                    "ORDER & PAYMENT CONFIRMED — ODIYA Credited via ${paymentMethod.title}$couponNoteSuffix$loyaltyNoteSuffix"
                CreditVerificationStatus.PAYMENT_FAILED_UNCREDITED ->
                    "ORDER & PAYMENT FAILED — ODIYA Account Not Yet Credited$couponNoteSuffix$loyaltyNoteSuffix"
                CreditVerificationStatus.NETWORK_DELAYED_PENDING ->
                    "ORDER & PAYMENT FAILED (Network Delay) — Transfer Queued, Auto-Confirming Upon Credit$couponNoteSuffix$loyaltyNoteSuffix"
                CreditVerificationStatus.PENDING_CREDIT ->
                    "Payment Receipt Submitted - Awaiting ODIYA Credit Alert Confirmation$couponNoteSuffix$loyaltyNoteSuffix"
            }

            val newOrder = Order(
                orderId = "ORD-${System.currentTimeMillis().toString().takeLast(7)}",
                customerId = _currentCustomer.value?.id ?: "guest-${UUID.randomUUID().toString().take(6)}",
                customerName = customerName,
                customerEmail = customerEmail,
                customerPhone = customerPhone,
                deliveryAddress = "$deliveryAddress [Delivery: $deliveryConditionTitle (₦$deliveryFee)]",
                itemsSummary = summary,
                totalAmount = total,
                profit = if (totalPartnerCommissionAmt > 0.0) maxOf(profit, totalPartnerCommissionAmt) else profit,
                paymentMethod = paymentMethod,
                paymentAccount = paymentAccountOrTerminal,
                paymentReference = paymentReference,
                senderName = senderName,
                status = OrderStatus.PENDING_VERIFICATION,
                createdAt = System.currentTimeMillis(),
                verificationNote = baseVerificationNote,
                receiptImageUrl = receiptImageUrl,
                partnerId = primaryPartnerId,
                partnerName = primaryPartnerName,
                partnerCommissionAmount = totalPartnerCommissionAmt,
                partnerNetPayout = totalPartnerNetAmt,
                isCreditAlertConfirmed = isCreditAlertReceived,
                creditVerificationStatus = creditVerificationStatus
            )

            val result = repository.createOrder(newOrder)
            if (result.isSuccess) {
                val savedOrder = result.getOrThrow().copy(
                    isCreditAlertConfirmed = isCreditAlertReceived,
                    creditVerificationStatus = creditVerificationStatus
                )
                setOrderCreditState(
                    orderId = savedOrder.orderId,
                    confirmed = isCreditAlertReceived,
                    status = creditVerificationStatus
                )
                persistDeliveryInfoLocally(
                    fullName = customerName,
                    phoneNumber = customerPhone,
                    deliveryAddress = deliveryAddress,
                    senderName = senderName.ifBlank { customerName }
                )
                saveCustomerDeliveryInfo(customerName, customerPhone, deliveryAddress)

                updateLoyaltyAfterOrder(
                    pointsRedeemed = loyaltyPointsRedeemed,
                    orderTotalAmount = savedOrder.totalAmount
                )

                if (partnerItems.isNotEmpty()) {
                    recordPartnerOrderTransactionsAndAlerts(
                        order = savedOrder,
                        partnerItems = partnerItems,
                        eventType = PartnerTransactionType.CUSTOMER_ORDER_PAYMENT_ALERT,
                        discountFactor = discountFactor,
                        creditPartnerTotals = true
                    )
                }

                clearCart()
                _pendingSharedReceipt.value = null

                when (creditVerificationStatus) {
                    CreditVerificationStatus.CONFIRMED_CREDITED -> {
                        ShopNotificationHelper.notifyPaymentConfirmedReceipt(
                            context = getApplication(),
                            orderId = savedOrder.orderId,
                            portalTitle = paymentMethod.title,
                            amountText = "₦${String.format(java.util.Locale.US, "%,.0f", savedOrder.totalAmount)}"
                        )
                        startLiveCallSignalingIfConfigured()
                        liveCallClient.sendSignal(
                            "PAYMENT_CREDIT_ALERT",
                            "global_orders",
                            JSONObject().apply {
                                put("orderId", savedOrder.orderId)
                                put("amount", savedOrder.totalAmount)
                                put("portal", paymentMethod.name)
                                put("ref", savedOrder.paymentReference)
                            }
                        )
                    }
                    CreditVerificationStatus.NETWORK_DELAYED_PENDING -> {
                        enqueueOfflineAction(
                            type = OfflineActionType.PAYMENT_CREDIT_SYNC,
                            referenceId = savedOrder.orderId,
                            summary = "Auto-Credit ${paymentMethod.title} ₦${savedOrder.totalAmount.toInt()} (${savedOrder.orderId})"
                        )
                        ShopNotificationHelper.notifyPaymentFailedUncredited(
                            context = getApplication(),
                            orderId = savedOrder.orderId,
                            portalTitle = paymentMethod.title,
                            amountText = "₦${String.format(java.util.Locale.US, "%,.0f", savedOrder.totalAmount)}",
                            isNetworkDelay = true
                        )
                    }
                    CreditVerificationStatus.PAYMENT_FAILED_UNCREDITED -> {
                        ShopNotificationHelper.notifyPaymentFailedUncredited(
                            context = getApplication(),
                            orderId = savedOrder.orderId,
                            portalTitle = paymentMethod.title,
                            amountText = "₦${String.format(java.util.Locale.US, "%,.0f", savedOrder.totalAmount)}",
                            isNetworkDelay = false
                        )
                    }
                    CreditVerificationStatus.PENDING_CREDIT -> Unit
                }

                val syncErr = repository.lastCloudSyncError
                if (syncErr.isNullOrBlank()) {
                    _cloudStatusMessage.value = "✅ Order ${savedOrder.orderId} & receipt synced to Supabase Cloud!"
                } else {
                    _cloudStatusMessage.value = "⚠️ Order ${savedOrder.orderId} saved locally, queued for reconnect ($syncErr)"
                    if (!isOnline.value) {
                        enqueueOfflineAction(
                            type = OfflineActionType.CLOUD_INVENTORY_SYNC,
                            referenceId = savedOrder.orderId,
                            summary = "Sync Order ${savedOrder.orderId} (₦${savedOrder.totalAmount.toInt()}) to Cloud"
                        )
                    }
                }
                voiceManager.speakNewOrderAlert(
                    customerName = savedOrder.customerName.ifBlank { "Customer" },
                    totalAmountText = "${savedOrder.totalAmount.toInt()} Naira",
                    orderId = savedOrder.orderId
                )
                onSuccess(savedOrder)
            } else {
                onFailure(result.exceptionOrNull()?.message ?: "Unknown Error occurred while saving order.")
            }
        }
    }

    fun processDirectPortalPayment(
        customerName: String,
        customerEmail: String,
        customerPhone: String,
        deliveryAddress: String,
        paymentMethod: PaymentMethod,
        portalAccountNumber: String,
        portalAccountName: String,
        senderName: String,
        deliveryConditionTitle: String,
        deliveryFeeAmount: Double,
        simulateCreditAlertReceived: Boolean = true,
        simulateNetworkDelay: Boolean = false,
        customBargainTotal: Double? = null,
        appliedCouponCode: String = "",
        couponDiscountAmount: Double = 0.0,
        loyaltyPointsRedeemed: Int = 0,
        loyaltyDiscountAmount: Double = 0.0,
        onSuccess: (Order) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val networkIsDown = !isOnline.value || simulateNetworkDelay
        val effectiveStatus = when {
            networkIsDown -> CreditVerificationStatus.NETWORK_DELAYED_PENDING
            simulateCreditAlertReceived -> CreditVerificationStatus.CONFIRMED_CREDITED
            else -> CreditVerificationStatus.PAYMENT_FAILED_UNCREDITED
        }
        val effectiveConfirmed = (effectiveStatus == CreditVerificationStatus.CONFIRMED_CREDITED)
        val portalRef = "${paymentMethod.name.take(4)}-ODY-${UUID.randomUUID().toString().take(8).uppercase()}"
        val acctLabel = "${paymentMethod.title}: $portalAccountNumber ($portalAccountName)"

        placeOrder(
            customerName = customerName,
            customerEmail = customerEmail,
            customerPhone = customerPhone,
            deliveryAddress = deliveryAddress,
            paymentMethod = paymentMethod,
            paymentAccountOrTerminal = acctLabel,
            paymentReference = portalRef,
            senderName = senderName,
            deliveryConditionTitle = deliveryConditionTitle,
            deliveryFeeAmount = deliveryFeeAmount,
            receiptImageUrl = "",
            customBargainTotal = customBargainTotal,
            appliedCouponCode = appliedCouponCode,
            couponDiscountAmount = couponDiscountAmount,
            loyaltyPointsRedeemed = loyaltyPointsRedeemed,
            loyaltyDiscountAmount = loyaltyDiscountAmount,
            isCreditAlertReceived = effectiveConfirmed,
            creditVerificationStatus = effectiveStatus,
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    fun recordOrderCreditAlertReceived(orderId: String, portalRef: String = "Instant Credit Alert") {
        viewModelScope.launch {
            val ord = allOrders.value.find { it.orderId == orderId } ?: return@launch
            setOrderCreditState(
                orderId = orderId,
                confirmed = true,
                status = CreditVerificationStatus.CONFIRMED_CREDITED
            )
            val newNote = "ORDER & PAYMENT CONFIRMED — ODIYA Account Credited ($portalRef)"
            repository.updateOrderStatus(orderId, ord.status, newNote)
            val updatedQueue = _offlineQueue.value.filterNot {
                it.type == OfflineActionType.PAYMENT_CREDIT_SYNC && it.referenceId == orderId
            }
            if (updatedQueue.size != _offlineQueue.value.size) {
                _offlineQueue.value = updatedQueue
                persistOfflineQueue(updatedQueue)
            }
            if (_activeReceipt.value?.orderId == orderId) {
                _activeReceipt.value = _activeReceipt.value?.copy(
                    isCreditAlertConfirmed = true,
                    creditVerificationStatus = CreditVerificationStatus.CONFIRMED_CREDITED,
                    verificationNote = newNote
                )
            }
            ShopNotificationHelper.notifyPaymentConfirmedReceipt(
                context = getApplication(),
                orderId = orderId,
                portalTitle = ord.paymentMethod.title,
                amountText = "₦${String.format(java.util.Locale.US, "%,.0f", ord.totalAmount)}"
            )
            voiceManager.speakText("Order and payment confirmed for $orderId. ODIYA account credited.")
            startLiveCallSignalingIfConfigured()
            liveCallClient.sendSignal(
                "PAYMENT_CREDIT_ALERT",
                "global_orders",
                JSONObject().apply {
                    put("orderId", orderId)
                    put("amount", ord.totalAmount)
                    put("portal", ord.paymentMethod.name)
                    put("ref", portalRef)
                }
            )
        }
    }

    fun retryFailedOrderCreditCheck(
        orderId: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val ord = allOrders.value.find { it.orderId == orderId }
        if (ord == null) {
            onResult(false, "Order $orderId not found.")
            return
        }
        if (!isOnline.value) {
            setOrderCreditState(orderId, false, CreditVerificationStatus.NETWORK_DELAYED_PENDING)
            enqueueOfflineAction(
                type = OfflineActionType.PAYMENT_CREDIT_SYNC,
                referenceId = orderId,
                summary = "Auto-Credit ${ord.paymentMethod.title} ₦${ord.totalAmount.toInt()} ($orderId)"
            )
            ShopNotificationHelper.notifyPaymentFailedUncredited(
                context = getApplication(),
                orderId = orderId,
                portalTitle = ord.paymentMethod.title,
                amountText = "₦${String.format(java.util.Locale.US, "%,.0f", ord.totalAmount)}",
                isNetworkDelay = true
            )
            onResult(false, "Network offline — queued. Will auto-confirm as soon as network restores.")
            return
        }
        recordOrderCreditAlertReceived(orderId, "Direct Portal Settlement Verified")
        onResult(true, "✅ ORDER & PAYMENT CONFIRMED! ODIYA account credited & Admin Payment Confirmation enabled.")
    }

    fun initiatePaystackCheckout(
        customerName: String,
        customerPhone: String,
        deliveryAddress: String,
        senderName: String,
        deliveryConditionTitle: String,
        deliveryFeeAmount: Double,
        customBargainTotal: Double? = null,
        appliedCouponCode: String = "",
        couponDiscountAmount: Double = 0.0,
        loyaltyPointsRedeemed: Int = 0,
        loyaltyDiscountAmount: Double = 0.0,
        onReadyToOpenUrl: (authorizationUrl: String, order: Order, reference: String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val sbCfg = repository.supabaseSync.getConfig()
        val profile = _storeProfile.value
        val items = _cartItems.value
        val subtotal = items.sumOf { it.unitPrice * it.quantity }
        val safeCoupon = couponDiscountAmount.coerceIn(0.0, subtotal)
        val safeLoyalty = loyaltyDiscountAmount.coerceIn(0.0, (subtotal - safeCoupon).coerceAtLeast(0.0))
        val expectedTotal = customBargainTotal ?: ((subtotal - safeCoupon - safeLoyalty).coerceAtLeast(0.0) + deliveryFeeAmount)
        val previewRef = paystackGateway.generatePaystackReference("ORD${System.currentTimeMillis().toString().takeLast(5)}")

        _paystackCheckoutState.value = com.example.data.remote.PaystackCheckoutUiState(
            isLoading = true,
            reference = previewRef,
            statusMessage = "Connecting securely to Paystack Checkout..."
        )

        placeOrder(
            customerName = customerName,
            customerEmail = profile.supportEmail,
            customerPhone = customerPhone,
            deliveryAddress = deliveryAddress,
            paymentMethod = PaymentMethod.PAYSTACK,
            paymentAccountOrTerminal = "Paystack Merchant Gateway (${profile.storeName})",
            paymentReference = previewRef,
            senderName = senderName.ifBlank { customerName },
            deliveryConditionTitle = deliveryConditionTitle,
            deliveryFeeAmount = deliveryFeeAmount,
            receiptImageUrl = "",
            customBargainTotal = customBargainTotal,
            appliedCouponCode = appliedCouponCode,
            couponDiscountAmount = couponDiscountAmount,
            loyaltyPointsRedeemed = loyaltyPointsRedeemed,
            loyaltyDiscountAmount = loyaltyDiscountAmount,
            isCreditAlertReceived = false,
            creditVerificationStatus = CreditVerificationStatus.PENDING_CREDIT,
            onSuccess = { createdOrder ->
                viewModelScope.launch {
                    val initRes = paystackGateway.initializeTransaction(
                        supabaseUrl = sbCfg.url,
                        supabaseAnonKey = sbCfg.anonKey,
                        overrideEdgeBaseUrl = profile.paystackEdgeFunctionBaseUrl,
                        orderId = createdOrder.orderId,
                        amountNaira = if (createdOrder.totalAmount > 0.0) createdOrder.totalAmount else expectedTotal,
                        customerName = customerName,
                        customerPhone = customerPhone,
                        supportEmail = profile.supportEmail,
                        callbackUrl = profile.paystackCallbackUrl,
                        referenceOverride = previewRef
                    )
                    if (initRes.success && initRes.authorizationUrl.isNotBlank()) {
                        _paystackCheckoutState.value = com.example.data.remote.PaystackCheckoutUiState(
                            isLoading = false,
                            orderId = createdOrder.orderId,
                            reference = initRes.reference,
                            authorizationUrl = initRes.authorizationUrl,
                            statusMessage = "Paystack session ready (${initRes.reference}). Complete payment then tap Verify Payment."
                        )
                        onReadyToOpenUrl(initRes.authorizationUrl, createdOrder, initRes.reference)
                    } else {
                        _paystackCheckoutState.value = com.example.data.remote.PaystackCheckoutUiState(
                            isLoading = false,
                            orderId = createdOrder.orderId,
                            reference = initRes.reference.ifBlank { previewRef },
                            authorizationUrl = "",
                            statusMessage = initRes.message
                        )
                        onFailure(initRes.message)
                    }
                }
            },
            onFailure = { err ->
                _paystackCheckoutState.value = com.example.data.remote.PaystackCheckoutUiState(
                    isLoading = false,
                    statusMessage = err
                )
                onFailure(err)
            }
        )
    }

    fun verifyPaystackPayment(
        reference: String,
        orderId: String? = null,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val cleanRef = reference.trim()
            val matchedOrder = allOrders.value.find {
                (!orderId.isNullOrBlank() && it.orderId == orderId) ||
                    it.paymentReference.equals(cleanRef, ignoreCase = true)
            }
            val targetOrderId = orderId ?: matchedOrder?.orderId ?: _paystackCheckoutState.value.orderId
            _paystackCheckoutState.value = _paystackCheckoutState.value.copy(
                isLoading = true,
                orderId = targetOrderId,
                reference = cleanRef,
                statusMessage = "Verifying Paystack settlement for $cleanRef..."
            )
            val sbCfg = repository.supabaseSync.getConfig()
            val profile = _storeProfile.value
            val verifyRes = paystackGateway.verifyTransaction(
                supabaseUrl = sbCfg.url,
                supabaseAnonKey = sbCfg.anonKey,
                overrideEdgeBaseUrl = profile.paystackEdgeFunctionBaseUrl,
                reference = cleanRef
            )
            if (verifyRes.success) {
                if (targetOrderId.isNotBlank()) {
                    recordOrderCreditAlertReceived(targetOrderId, "Paystack Verified (${verifyRes.channel} • $cleanRef)")
                }
                _paystackCheckoutState.value = _paystackCheckoutState.value.copy(
                    isLoading = false,
                    isVerifiedPaid = true,
                    statusMessage = "✅ Paystack Verified (${verifyRes.channel}) • Order $targetOrderId Confirmed!"
                )
                onResult(true, "✅ Paystack payment confirmed for order $targetOrderId!")
            } else {
                _paystackCheckoutState.value = _paystackCheckoutState.value.copy(
                    isLoading = false,
                    isVerifiedPaid = false,
                    statusMessage = verifyRes.message
                )
                onResult(false, verifyRes.message)
            }
        }
    }

    fun dismissPaystackPendingState() {
        _paystackCheckoutState.value = com.example.data.remote.PaystackCheckoutUiState()
    }

    fun showReceipt(order: Order) {
        _activeReceipt.value = order
    }

    fun dismissReceipt() {
        _activeReceipt.value = null
    }

    // Wishlist Operations (Saved in User Profile within Supabase)
    fun isInWishlist(productId: String): Boolean {
        return _wishlistIds.value.contains(productId)
    }

    fun toggleWishlist(productId: String, onFeedback: ((Boolean) -> Unit)? = null) {
        val current = _wishlistIds.value.toMutableSet()
        val willBeAdded = !current.contains(productId)
        if (willBeAdded) {
            current.add(productId)
        } else {
            current.remove(productId)
        }
        _wishlistIds.value = current

        // Update current customer profile & sync to Supabase Cloud
        _currentCustomer.value?.let { customer ->
            val updated = customer.copy(wishlistProductIds = current.toList())
            _currentCustomer.value = updated

            viewModelScope.launch {
                repository.toggleCustomerWishlist(customer.id, productId)
            }
        }
        onFeedback?.invoke(willBeAdded)
    }

    fun removeAllWishlist() {
        _wishlistIds.value = emptySet()
        _currentCustomer.value?.let { customer ->
            _currentCustomer.value = customer.copy(wishlistProductIds = emptyList())
            viewModelScope.launch {
                repository.supabaseSync.syncCustomerWishlistToCloud(customer.id, emptyList())
            }
        }
    }

    // Auth methods (Phone Number & Password)
    fun resetCustomerPasswordByPhone(
        phone: String,
        fullName: String = "",
        newPassword: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.resetCustomerPasswordByPhone(
                phone = phone,
                fullName = fullName,
                newPassword = newPassword
            )
            res.fold(
                onSuccess = { updatedCustomer ->
                    if (_currentCustomer.value?.id == updatedCustomer.id) {
                        _currentCustomer.value = updatedCustomer
                    }
                    voiceManager.speakText("Password updated successfully for ${updatedCustomer.fullName}.")
                    onResult(true, "✅ Password updated for ${updatedCustomer.fullName} (${updatedCustomer.phoneNumber})! You can sign in now.")
                },
                onFailure = { err ->
                    onResult(false, err.message ?: "Could not reset password.")
                }
            )
        }
    }

    fun signIn(phoneOrEmail: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val res = repository.authenticateCustomer(phoneOrEmail, pass)
            res.fold(
                onSuccess = { customer ->
                    adminPrefs.edit().putString("logged_in_customer_id", customer.id).apply()
                    _currentCustomer.value = customer
                    _wishlistIds.value = customer.wishlistProductIds.toSet()
                    saveCustomerDeliveryInfo(customer.fullName, customer.phoneNumber, customer.deliveryAddress)
                    viewModelScope.launch {
                        if (!repository.supabaseSync.isPlaceholderConfig()) {
                            repository.supabaseSync.syncCustomerToCloud(customer)
                        }
                    }
                    voiceManager.speakCustomerSignIn(customer.fullName)
                    onSuccess()
                },
                onFailure = { error ->
                    onError(error.message ?: "Authentication failed")
                }
            )
        }
    }

    fun signUp(name: String, pass: String, phone: String, address: String = "", email: String = "", onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val res = repository.registerCustomer(fullName = name, password = pass, phone = phone, address = address, email = email)
            res.fold(
                onSuccess = { customer ->
                    adminPrefs.edit().putString("logged_in_customer_id", customer.id).apply()
                    _currentCustomer.value = customer
                    _wishlistIds.value = customer.wishlistProductIds.toSet()
                    saveCustomerDeliveryInfo(customer.fullName, customer.phoneNumber, customer.deliveryAddress)
                    val syncErr = repository.lastCloudSyncError
                    if (syncErr.isNullOrBlank()) {
                        _cloudStatusMessage.value = "✅ Account '${customer.fullName}' synced live to Store Admin!"
                    } else {
                        _cloudStatusMessage.value = "⚠️ Account saved on this phone only ($syncErr)"
                    }
                    voiceManager.speakCustomerSignIn(customer.fullName)
                    onSuccess()
                },
                onFailure = { error ->
                    onError(error.message ?: "Registration failed")
                }
            )
        }
    }

    fun signOut() {
        adminPrefs.edit()
            .remove("logged_in_customer_id")
            .putBoolean("is_admin_device_authorized", false)
            .apply()
        _currentCustomer.value = null
        _isAdminUnlocked.value = false
        _isAdminDeviceAuthorized.value = false
        _wishlistIds.value = emptySet()
        navigateTo(AppScreen.AUTH)
    }

    fun deleteCustomerFromAdmin(customerId: String, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            if (_currentCustomer.value?.id == customerId) {
                adminPrefs.edit().remove("logged_in_customer_id").apply()
                _currentCustomer.value = null
            }
            repository.deleteCustomer(customerId)
            onComplete?.invoke()
        }
    }

    fun deleteCurrentCustomerAccount(onComplete: (Boolean, String) -> Unit) {
        val active = _currentCustomer.value
        if (active == null) {
            onComplete(false, "No customer account is currently signed in.")
            return
        }
        viewModelScope.launch {
            val res = repository.deleteCustomer(active.id)
            adminPrefs.edit()
                .remove("logged_in_customer_id")
                .remove("saved_cust_name")
                .remove("saved_cust_phone")
                .remove("saved_cust_address")
                .remove("checkout_saved_full_name")
                .remove("checkout_saved_phone")
                .remove("checkout_saved_address")
                .remove("checkout_saved_sender_name")
                .apply()
            _currentCustomer.value = null
            _wishlistIds.value = emptySet()
            _savedDeliveryInfo.value = CustomerDeliveryInfo()
            navigateToHome()
            if (res.isSuccess) {
                onComplete(true, "Account '${active.fullName}' and saved personal details deleted permanently.")
            } else {
                onComplete(true, "Account removed from this device (Cloud deletion sync pending offline queue).")
            }
        }
    }

    fun runSecurityDefenseAudit(): com.example.util.SecurityPostureReport {
        return com.example.util.AppDefenseManager.runSecurityIntegrityAudit(getApplication())
    }

    fun syncDefenseTelemetryToSupabase(onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val report = com.example.util.AppDefenseManager.runSecurityIntegrityAudit(ctx)
            val version = getInstalledAppVersion().second
            val details = "Score=${report.overallScore}%; Status=${report.overallStatus}; Crashes=${report.recordedCrashCount}; ${report.lastCrashSummary}".trim()
            val res = repository.supabaseSync.reportSecurityTelemetryToCloud(
                eventType = if (report.recordedCrashCount > 0) "CRASH_AND_AUDIT_SNAPSHOT" else "SECURITY_HEALTH_AUDIT",
                severity = if (report.overallScore >= 90 && report.recordedCrashCount == 0) "INFO" else "NOTICE",
                details = details,
                appVersion = version,
                signingSha256 = report.signingSha256Short
            )
            res.fold(
                onSuccess = {
                    com.example.util.AppDefenseManager.markCrashTelemetrySynced(ctx)
                    onComplete(true, "✅ Security & crash health snapshot synced to Supabase telemetry!")
                },
                onFailure = { err ->
                    onComplete(false, err.message ?: "Connect Supabase first to upload security telemetry.")
                }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        liveCallTimerJob?.cancel()
        liveLobbyPollJob?.cancel()
        liveCallClient.disconnect()
        offlineConnectivityManager.release()
        voiceManager.shutdown()
    }

    // Admin Control Room Operations
    fun seed100Products(onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val count = repository.seedFull100ProductsCatalog()
            onComplete(count)
        }
    }

    fun saveProductFromAdmin(product: Product, onComplete: () -> Unit) {
        saveProductFromAdminWithStatus(product) { _, _ -> onComplete() }
    }

    fun getSignedUpCustomerPhones(): List<String> {
        val phones = allCustomers.value
            .map { it.phoneNumber.trim() }
            .filter { it.isNotBlank() }
            .toMutableList()
        val currentPhone = _currentCustomer.value?.phoneNumber?.trim().orEmpty()
        if (currentPhone.isNotBlank()) phones.add(currentPhone)
        val savedPhone = _savedDeliveryInfo.value.phoneNumber.trim()
        if (savedPhone.isNotBlank()) phones.add(savedPhone)
        return phones.distinct()
    }

    fun broadcastNewProductAlert(product: Product) {
        val phones = getSignedUpCustomerPhones()
        val targetPhone = _currentCustomer.value?.phoneNumber?.ifBlank { null }
            ?: _savedDeliveryInfo.value.phoneNumber.ifBlank { null }
            ?: phones.firstOrNull().orEmpty()
        ShopNotificationHelper.notifyNewProductAlert(
            context = getApplication(),
            productTitle = product.title,
            category = product.category,
            priceText = "₦${String.format(java.util.Locale.US, "%,.0f", product.basePrice)}",
            targetPhone = targetPhone
        )
        startLiveCallSignalingIfConfigured()
        liveCallClient.sendSignal(
            "NEW_PRODUCT",
            "global_products",
            JSONObject().apply {
                put("productId", product.id)
                put("title", product.title)
                put("category", product.category)
                put("price", product.basePrice)
            }
        )
        if (!isOnline.value) {
            enqueueOfflineAction(
                type = OfflineActionType.NEW_PRODUCT_ALERT,
                referenceId = product.id,
                summary = "New Product Alert: ${product.title} (${phones.size} lines)"
            )
        }
    }

    fun saveProductFromAdminWithStatus(
        product: Product,
        onComplete: (syncedToCloud: Boolean, cloudError: String?) -> Unit
    ) {
        viewModelScope.launch {
            val isNewProduct = allProducts.value.none { it.id == product.id }
            val res = repository.saveProduct(product)
            val synced = res.getOrDefault(false)
            val cloudErr = if (res.isFailure) res.exceptionOrNull()?.message else repository.lastCloudSyncError
            val storageErr = repository.lastStorageUploadError
            val combinedErr = listOfNotNull(
                storageErr?.let { "Photo upload issue: $it" },
                cloudErr
            ).joinToString(" | ").ifBlank { null }

            if (isNewProduct) {
                broadcastNewProductAlert(product)
            }

            _cloudStatusMessage.value = when {
                synced && storageErr == null ->
                    "✅ Product '${product.title}' + photo saved, synced & New Product Alert dispatched!"
                synced && storageErr != null ->
                    "⚠️ '${product.title}' synced to Supabase, but photo stayed local ($storageErr). Run 'Copy SQL' in Supabase SQL Editor and tap 'Fix Photo'."
                else ->
                    "⚠️ Saved '${product.title}' locally${if (!combinedErr.isNullOrBlank()) " ($combinedErr)" else ""}"
            }
            onComplete(synced, combinedErr)
        }
    }

    fun deleteProductFromAdmin(productId: String) {
        viewModelScope.launch {
            repository.deleteProduct(productId)
        }
    }

    fun updateOrderStatus(orderId: String, status: OrderStatus, note: String) {
        viewModelScope.launch {
            val matchingOrder = allOrders.value.find { it.orderId == orderId }
            val wasPending = matchingOrder?.status == OrderStatus.PENDING_VERIFICATION
            if (status == OrderStatus.CONFIRMED || status == OrderStatus.VERIFIED) {
                setOrderCreditState(orderId, true, CreditVerificationStatus.CONFIRMED_CREDITED)
            }
            repository.updateOrderStatus(orderId, status, note)
            val cName = matchingOrder?.customerName ?: "Customer"
            if (status == OrderStatus.CONFIRMED || status == OrderStatus.VERIFIED) {
                if (wasPending && matchingOrder != null && matchingOrder.partnerId.isNotBlank()) {
                    val partnerOrg = _partnerOrganizations.value.find { it.id == matchingOrder.partnerId }
                    val partnerProd = allProducts.value.filter { it.partnerId == matchingOrder.partnerId }
                    val commPct = partnerProd.firstOrNull()?.partnerCommissionPercent
                        ?: partnerOrg?.stipulatedCommissionPercent
                        ?: 10.0
                    val shareAmt = matchingOrder.partnerCommissionAmount.takeIf { it > 0.0 }
                        ?: (matchingOrder.totalAmount * (commPct / 100.0))
                    val netAmt = matchingOrder.partnerNetPayout.takeIf { it > 0.0 }
                        ?: (matchingOrder.totalAmount - shareAmt).coerceAtLeast(0.0)
                    val alreadyCreditedAtPayment = _partnerTransactions.value.any {
                        it.orderId == matchingOrder.orderId &&
                            it.partnerId == matchingOrder.partnerId &&
                            it.type == PartnerTransactionType.CUSTOMER_ORDER_PAYMENT_ALERT
                    }
                    val confirmedTxn = PartnerFolderTransaction(
                        partnerId = matchingOrder.partnerId,
                        partnerName = matchingOrder.partnerName.ifBlank { partnerOrg?.businessName ?: "Partner" },
                        type = PartnerTransactionType.ORDER_CONFIRMED_SALE,
                        productName = matchingOrder.itemsSummary.replace("\n", " | ").take(120),
                        category = partnerOrg?.primaryCategory ?: "",
                        quantity = 1,
                        grossAmount = matchingOrder.totalAmount,
                        commissionPercent = commPct,
                        commissionProfitAmount = shareAmt,
                        partnerNetAmount = netAmt,
                        orderId = matchingOrder.orderId,
                        customerName = matchingOrder.customerName,
                        customerPhone = matchingOrder.customerPhone,
                        paymentMethod = matchingOrder.paymentMethod.title,
                        paymentAccount = matchingOrder.paymentAccount,
                        paymentReference = matchingOrder.paymentReference,
                        receiptImageUrl = matchingOrder.receiptImageUrl,
                        alertSentToAdmin = true,
                        alertSentToPartner = true,
                        note = "Confirmed Sale Settlement: $note"
                    )
                    appendPartnerTransaction(confirmedTxn)
                    if (!alreadyCreditedAtPayment) {
                        val updatedOrgs = _partnerOrganizations.value.map { org ->
                            if (org.id == matchingOrder.partnerId) {
                                org.copy(
                                    totalAutoDeducted247Share = org.totalAutoDeducted247Share + shareAmt,
                                    totalAutoPartnerNetDeposited = org.totalAutoPartnerNetDeposited + netAmt
                                )
                            } else org
                        }
                        _partnerOrganizations.value = updatedOrgs
                        persistPartnerOrganizations(updatedOrgs)
                    }
                    pushPartnerLedgerAsync(preferLocal = true)
                    _latestPartnerPaymentAlert.value = confirmedTxn
                    val naira = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("en", "NG")).apply {
                        maximumFractionDigits = 0
                    }
                    ShopNotificationHelper.notifyPartnerPaymentAlert(
                        context = getApplication(),
                        partnerName = confirmedTxn.partnerName,
                        orderId = matchingOrder.orderId,
                        grossAmountText = naira.format(confirmedTxn.grossAmount),
                        commissionProfitText = naira.format(confirmedTxn.commissionProfitAmount),
                        partnerPayoutText = naira.format(confirmedTxn.partnerNetAmount),
                        paymentAccount = confirmedTxn.paymentAccount
                    )
                }
                voiceManager.speakText("Order Confirmation Alert! Order $orderId for customer $cName has been CONFIRMED and queued for processing.")
            } else if (status == OrderStatus.OUT_FOR_DELIVERY) {
                voiceManager.speakText("Dispatch Alert! Order $orderId for customer $cName is now out for delivery.")
            } else if (status == OrderStatus.DELIVERED) {
                voiceManager.speakText("Delivery Alert! Order $orderId has been delivered successfully.")
            }
        }
    }

    fun updateOrderDispatchRider(orderId: String, whatsapp: String) {
        viewModelScope.launch {
            repository.updateOrderDispatchRider(orderId, whatsapp)
        }
    }

    fun updateOrderReceiptDetails(
        orderId: String,
        receiptImageUrl: String,
        senderName: String,
        paymentReference: String,
        status: OrderStatus,
        note: String
    ) {
        viewModelScope.launch {
            repository.updateOrderReceiptDetails(orderId, receiptImageUrl, senderName, paymentReference, status, note)
        }
    }

    fun testSupabaseConnection(onResult: (String) -> Unit) {
        viewModelScope.launch {
            _isSupabaseBusy.value = true
            _cloudStatusMessage.value = "🔄 Testing Supabase host, API key & products/customers/orders tables..."
            val res = repository.supabaseSync.testConnection()
            val msg = if (res.isSuccess) {
                _isSupabaseConnected.value = true
                val cfg = repository.supabaseSync.getConfig()
                repository.supabaseSync.updateConfig(cfg.copy(isConnected = true))
                // Immediately run bidirectional product/customer/order sync!
                val syncRes = repository.fetchAndSyncAllFromCloudDetailed()
                _isSupabaseBusy.value = false
                if (syncRes.isSuccess) {
                    val s = syncRes.getOrThrow()
                    _lastSyncTimestamp.value = System.currentTimeMillis()
                    if (s.errors.isEmpty()) {
                        "${res.getOrThrow()} Synced ${s.syncedProducts} products (${s.customCloudProducts} uploaded), ${s.syncedCustomers} customers & ${s.syncedOrders} orders."
                    } else {
                        "⚠️ Connected, with warnings: ${s.errors.joinToString("; ")}"
                    }
                } else {
                    res.getOrThrow()
                }
            } else {
                _isSupabaseBusy.value = false
                _isSupabaseConnected.value = false
                val cfg = repository.supabaseSync.getConfig()
                repository.supabaseSync.updateConfig(cfg.copy(isConnected = false))
                "❌ ${res.exceptionOrNull()?.message ?: "Failed to connect to Supabase"}"
            }
            _cloudStatusMessage.value = msg
            onResult(msg)
        }
    }

    fun getSavedCustomPortalUrl(): String {
        return com.example.util.WebPortalExporter.getSavedCustomPortalUrl(getApplication())
    }

    fun buildCustomerSyncLink(baseShareUrl: String? = null): String? {
        val cfg = repository.supabaseSync.getConfig()
        if (repository.supabaseSync.isPlaceholderConfig(cfg.url, cfg.anonKey)) return null
        val encUrl = java.net.URLEncoder.encode(cfg.url, "UTF-8")
        val encKey = java.net.URLEncoder.encode(cfg.anonKey, "UTF-8")
        val normalizedBase = baseShareUrl?.let { com.example.util.WebPortalExporter.normalizePortalUrl(it) }.orEmpty()
        return if (normalizedBase.isBlank()) {
            "depros://connect?sb_url=$encUrl&sb_key=$encKey"
        } else {
            val baseWithRootSlash = if (!normalizedBase.contains("?") && normalizedBase.count { it == '/' } == 2) {
                "$normalizedBase/"
            } else {
                normalizedBase
            }
            val sep = if (baseWithRootSlash.contains("?")) "&" else "?"
            "${baseWithRootSlash}${sep}open_app=1&sb_url=$encUrl&sb_key=$encKey"
        }
    }

    fun handleIncomingSharedReceipt(
        context: Context,
        streamUris: List<Uri>,
        mimeTypeHint: String?,
        sharedText: String?,
        sharedSubject: String?,
        onFeedback: (String) -> Unit
    ) {
        viewModelScope.launch {
            val parsedText = SharedReceiptHelper.parseReceiptShareText(
                rawText = sharedText.orEmpty(),
                subject = sharedSubject.orEmpty()
            )
            val savedFileUri = withContext(Dispatchers.IO) {
                streamUris.firstNotNullOfOrNull { rawUri ->
                    SharedReceiptHelper.persistSharedReceiptToAppStorage(
                        context = context,
                        sourceUri = rawUri,
                        mimeTypeHint = mimeTypeHint
                    )
                }
            }
            if (savedFileUri == null && parsedText.extractedReference.isBlank() && sharedText.isNullOrBlank()) {
                onFeedback("Could not read shared receipt from app. Try sharing again as Image or PDF.")
                return@launch
            }
            val pending = PendingSharedReceipt(
                receiptUri = savedFileUri?.toString().orEmpty(),
                detectedPaymentMethod = parsedText.detectedPaymentMethod,
                extractedReference = parsedText.extractedReference,
                extractedSender = parsedText.extractedSender,
                rawTextSummary = sharedText.orEmpty().trim(),
                sourceAppLabel = parsedText.sourceAppLabel.ifBlank { "OPay / Moniepoint" },
                receivedAtMillis = System.currentTimeMillis()
            )
            _pendingSharedReceipt.value = pending
            if (parsedText.detectedPaymentMethod != null || parsedText.extractedSender.isNotBlank()) {
                persistDeliveryInfoLocally(senderName = parsedText.extractedSender)
            }
            val methodTag = pending.detectedPaymentMethod?.title ?: pending.sourceAppLabel
            if (_cartItems.value.isNotEmpty()) {
                navigateTo(AppScreen.CHECKOUT)
                onFeedback("✅ $methodTag receipt attached directly to Checkout!")
            } else {
                onFeedback("✅ $methodTag receipt saved! Add item(s) & open Checkout — it attaches automatically.")
            }
        }
    }

    fun clearPendingSharedReceipt() {
        _pendingSharedReceipt.value = null
    }

    fun applyStoreSyncLinkOrCredentials(
        rawInput: String,
        optionalKey: String = "",
        onResult: ((Boolean, String) -> Unit)? = null
    ) {
        val trimmed = rawInput.trim()
        val extracted = SharedReceiptHelper.extractStoreSyncCredentials(trimmed)
        var parsedUrl = extracted?.supabaseUrl ?: ""
        var parsedKey = extracted?.supabaseKey?.takeIf { it.isNotBlank() } ?: optionalKey.trim()

        if (parsedUrl.isBlank() && optionalKey.isNotBlank()) {
            parsedUrl = trimmed
        }

        if (parsedUrl.isBlank() || parsedKey.isBlank()) {
            onResult?.invoke(false, "Please provide a valid Store Sync Link (or Project URL + Publishable Key).")
            return
        }

        updateSupabaseConfig(parsedUrl, parsedKey)
        viewModelScope.launch {
            _isSupabaseBusy.value = true
            val testRes = repository.supabaseSync.testConnection()
            if (testRes.isSuccess) {
                _isSupabaseConnected.value = true
                repository.supabaseSync.updateConfig(repository.supabaseSync.getConfig().copy(isConnected = true))
                val syncRes = repository.fetchAndSyncAllFromCloudDetailed()
                val queuedPreIds = _offlineQueue.value
                    .filter { it.type == OfflineActionType.PREORDER_SYNC }
                    .map { it.referenceId }
                    .toSet() + _preOrderRequests.value.map { it.id }.toSet()
                val preRes = syncPreOrdersWithCloudInternal(
                    preferLocalIds = queuedPreIds,
                    notifyOnNewRemote = true
                )
                if (preRes.isSuccess && _offlineQueue.value.any { it.type == OfflineActionType.PREORDER_SYNC }) {
                    val remaining = _offlineQueue.value.filterNot { it.type == OfflineActionType.PREORDER_SYNC }
                    _offlineQueue.value = remaining
                    persistOfflineQueue(remaining)
                }
                _isSupabaseBusy.value = false
                val msg = if (syncRes.isSuccess) {
                    val s = syncRes.getOrThrow()
                    "✅ Store Cloud Sync Linked! Pushed ${s.pushedLocalCustomers} signup(s), ${s.pushedLocalOrders} order(s) & ${_preOrderRequests.value.size} pre-order(s)."
                } else {
                    testRes.getOrThrow()
                }
                _cloudStatusMessage.value = msg
                onResult?.invoke(true, msg)
            } else {
                _isSupabaseBusy.value = false
                val errMsg = "❌ ${testRes.exceptionOrNull()?.message ?: "Invalid Store Cloud Sync link"}"
                _cloudStatusMessage.value = errMsg
                onResult?.invoke(false, errMsg)
            }
        }
    }

    fun updateSupabaseConfig(url: String, anonKey: String): Pair<String, String> {
        val cleanU = if (url.isBlank()) "" else repository.supabaseSync.cleanUrl(url)
        val cleanK = repository.supabaseSync.sanitizeKey(anonKey)
        adminPrefs.edit()
            .putString("supabase_url", cleanU)
            .putString("supabase_anon_key", cleanK)
            .apply()
        com.example.util.EncryptedKeystoreVault.putSecret(getApplication(), "supabase_anon_key", cleanK)
        val newCfg = SupabaseConfig(url = cleanU, anonKey = cleanK, isConnected = false)
        repository.supabaseSync.updateConfig(newCfg)
        _supabaseConfigState.value = repository.supabaseSync.getConfig()
        _isSupabaseConnected.value = false
        _cloudStatusMessage.value = if (cleanU.isNotBlank()) "Credentials saved ($cleanU). Verifying..." else "Supabase credentials cleared."
        if (cleanU.isNotBlank() && cleanK.isNotBlank()) {
            startLiveCallSignalingIfConfigured()
        }
        return cleanU to cleanK
    }

    fun clearSupabaseConfig() {
        adminPrefs.edit().remove("supabase_url").remove("supabase_anon_key").apply()
        com.example.util.EncryptedKeystoreVault.removeSecret(getApplication(), "supabase_anon_key")
        repository.supabaseSync.updateConfig(SupabaseConfig())
        _supabaseConfigState.value = SupabaseConfig()
        _isSupabaseConnected.value = false
        _cloudStatusMessage.value = "Credentials cleared. Enter your Supabase Project URL & Publishable/Anon Key."
    }

    fun syncAllProductsToSupabase(onComplete: (Int) -> Unit) {
        syncAllProductsWithDetails { success, _, _ -> onComplete(success) }
    }

    fun syncAllProductsWithDetails(onComplete: (successCount: Int, totalCount: Int, errorMsg: String?) -> Unit) {
        viewModelScope.launch {
            _isSupabaseBusy.value = true
            _cloudStatusMessage.value = "🔄 Pulling cloud products & pushing inventory to Supabase..."
            // 1. Pull cloud products first so any items uploaded on previous installs are restored cleanly
            val pullRes = repository.fetchAndSyncAllFromCloudDetailed()
            val pullSummary = pullRes.getOrNull()

            val currentProducts = try {
                repository.allProducts.first()
            } catch (_: Exception) {
                allProducts.value
            }
            // 2. Upload any local gallery photos (content:// or file://) to Supabase storage before syncing
            val preparedProducts = repository.prepareProductsForCloudUpload(currentProducts)
            val storageErr = repository.lastStorageUploadError
            val result = repository.supabaseSync.syncProductsBatchToCloud(preparedProducts)
            _isSupabaseBusy.value = false
            _lastSyncTimestamp.value = System.currentTimeMillis()

            if (result.isSuccess) {
                val synced = result.getOrDefault(preparedProducts.size)
                _isSupabaseConnected.value = true
                val cfg = repository.supabaseSync.getConfig()
                repository.supabaseSync.updateConfig(cfg.copy(isConnected = true))
                val restoredCustom = pullSummary?.customCloudProducts ?: preparedProducts.count { !it.id.startsWith("prod-") }
                if (storageErr != null) {
                    val warn = "Synced $synced products, but a local photo failed Storage upload ($storageErr)."
                    _cloudStatusMessage.value = "⚠️ $warn"
                    onComplete(synced, preparedProducts.size, warn)
                } else {
                    _cloudStatusMessage.value = "✅ Synced $synced / ${preparedProducts.size} products ($restoredCustom uploaded/custom items active) with Supabase Cloud!"
                    onComplete(synced, preparedProducts.size, null)
                }
            } else {
                _isSupabaseConnected.value = false
                val err = result.exceptionOrNull()?.message ?: "Unknown sync error"
                _cloudStatusMessage.value = "❌ Sync Error: $err"
                onComplete(0, preparedProducts.size, err)
            }
        }
    }

    fun syncInventoryLocally(onComplete: (successCount: Int, totalCount: Int) -> Unit) {
        viewModelScope.launch {
            _isSupabaseBusy.value = true
            _cloudStatusMessage.value = "🔄 Seeding & syncing catalog inventory locally..."
            val count = repository.seedFull100ProductsCatalog()
            _isSupabaseBusy.value = false
            _isSupabaseConnected.value = false
            _cloudStatusMessage.value = "✅ Seeded $count products in local database (Supabase Cloud not linked yet)."
            onComplete(count, count)
        }
    }

    // =========================================================================
    // BUSINESS PARTNERSHIPS, MOU AGREEMENTS, ADMIN STAGING & FOLDER ANALYTICS
    // =========================================================================

    private fun loadPartnerOrganizations(): List<PartnerOrganization> {
        val raw = adminPrefs.getString("partner_organizations_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                val id = obj.optString("id").trim()
                if (id.isBlank()) return@mapNotNull null
                PartnerOrganization(
                    id = id,
                    businessName = obj.optString("businessName"),
                    ownerName = obj.optString("ownerName"),
                    ownerPhone = obj.optString("ownerPhone"),
                    businessAddress = obj.optString("businessAddress"),
                    primaryCategory = obj.optString("primaryCategory", "Clothes"),
                    opayAccountNumber = obj.optString("opayAccountNumber"),
                    opayAccountName = obj.optString("opayAccountName"),
                    moniepointAccountNumber = obj.optString("moniepointAccountNumber"),
                    moniepointAccountName = obj.optString("moniepointAccountName"),
                    agreementDate = obj.optString("agreementDate"),
                    hostingDuration = obj.optString("hostingDuration", "6 Months"),
                    agreedQuantityPerPeriod = obj.optInt("agreedQuantityPerPeriod", 20),
                    periodUnit = try {
                        HostingPeriodUnit.valueOf(obj.optString("periodUnit", HostingPeriodUnit.PER_MONTH.name))
                    } catch (_: Exception) { HostingPeriodUnit.PER_MONTH },
                    monthlyHostingPayment = obj.optDouble("monthlyHostingPayment", 15000.0),
                    stipulatedCommissionPercent = obj.optDouble("stipulatedCommissionPercent", 10.0),
                    partnerSignatureName = obj.optString("partnerSignatureName"),
                    partnerSignatureStrokes = obj.optString("partnerSignatureStrokes"),
                    adminCounterSignatureName = obj.optString("adminCounterSignatureName"),
                    adminCounterSignatureStrokes = obj.optString("adminCounterSignatureStrokes"),
                    adminSignedAt = obj.optLong("adminSignedAt", 0L),
                    status = try {
                        PartnerAgreementStatus.valueOf(obj.optString("status", PartnerAgreementStatus.PENDING_FINALIZATION.name))
                    } catch (_: Exception) { PartnerAgreementStatus.PENDING_FINALIZATION },
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    isCatalogLocked = obj.optBoolean("isCatalogLocked", false),
                    catalogLockedAt = obj.optLong("catalogLockedAt", 0L),
                    hostingContractStartAt = obj.optLong("hostingContractStartAt", 0L),
                    hostingContractEndAt = obj.optLong("hostingContractEndAt", 0L),
                    lastHostingBillingAt = obj.optLong("lastHostingBillingAt", 0L),
                    nextHostingBillingDueAt = obj.optLong("nextHostingBillingDueAt", 0L),
                    hostingCyclesBilled = obj.optInt("hostingCyclesBilled", 0),
                    totalHostingFeesAutoDeducted = obj.optDouble("totalHostingFeesAutoDeducted", 0.0),
                    totalAutoDeducted247Share = obj.optDouble("totalAutoDeducted247Share", 0.0),
                    totalAutoPartnerNetDeposited = obj.optDouble("totalAutoPartnerNetDeposited", 0.0),
                    totalAccruedOfflinePenalty = obj.optDouble("totalAccruedOfflinePenalty", 0.0)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persistPartnerOrganizations(list: List<PartnerOrganization>) {
        val arr = JSONArray()
        list.forEach { p ->
            arr.put(JSONObject().apply {
                put("id", p.id)
                put("businessName", p.businessName)
                put("ownerName", p.ownerName)
                put("ownerPhone", p.ownerPhone)
                put("businessAddress", p.businessAddress)
                put("primaryCategory", p.primaryCategory)
                put("opayAccountNumber", p.opayAccountNumber)
                put("opayAccountName", p.opayAccountName)
                put("moniepointAccountNumber", p.moniepointAccountNumber)
                put("moniepointAccountName", p.moniepointAccountName)
                put("agreementDate", p.agreementDate)
                put("hostingDuration", p.hostingDuration)
                put("agreedQuantityPerPeriod", p.agreedQuantityPerPeriod)
                put("periodUnit", p.periodUnit.name)
                put("monthlyHostingPayment", p.monthlyHostingPayment)
                put("stipulatedCommissionPercent", p.stipulatedCommissionPercent)
                put("partnerSignatureName", p.partnerSignatureName)
                put("partnerSignatureStrokes", p.partnerSignatureStrokes)
                put("adminCounterSignatureName", p.adminCounterSignatureName)
                put("adminCounterSignatureStrokes", p.adminCounterSignatureStrokes)
                put("adminSignedAt", p.adminSignedAt)
                put("status", p.status.name)
                put("createdAt", p.createdAt)
                put("isCatalogLocked", p.isCatalogLocked)
                put("catalogLockedAt", p.catalogLockedAt)
                put("hostingContractStartAt", p.hostingContractStartAt)
                put("hostingContractEndAt", p.hostingContractEndAt)
                put("lastHostingBillingAt", p.lastHostingBillingAt)
                put("nextHostingBillingDueAt", p.nextHostingBillingDueAt)
                put("hostingCyclesBilled", p.hostingCyclesBilled)
                put("totalHostingFeesAutoDeducted", p.totalHostingFeesAutoDeducted)
                put("totalAutoDeducted247Share", p.totalAutoDeducted247Share)
                put("totalAutoPartnerNetDeposited", p.totalAutoPartnerNetDeposited)
                put("totalAccruedOfflinePenalty", p.totalAccruedOfflinePenalty)
            })
        }
        adminPrefs.edit().putString("partner_organizations_json", arr.toString()).apply()
    }

    private fun loadPartnerSubmissions(): List<PartnerProductSubmission> {
        val raw = adminPrefs.getString("partner_submissions_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                val id = obj.optString("id").trim()
                if (id.isBlank()) return@mapNotNull null
                val colorsStr = obj.optString("colorsStr", "")
                val colors = colorsStr.split(";").mapNotNull { token ->
                    val parts = token.split("|")
                    if (parts.size >= 2 && parts[0].isNotBlank()) ProductColor(parts[0], parts[1]) else null
                }.ifEmpty { listOf(ProductColor("Midnight Black", "#111827")) }
                PartnerProductSubmission(
                    id = id,
                    partnerId = obj.optString("partnerId"),
                    partnerName = obj.optString("partnerName"),
                    category = obj.optString("category", "Clothes"),
                    productName = obj.optString("productName"),
                    brand = obj.optString("brand"),
                    quantity = obj.optInt("quantity", 1),
                    sizesCsv = obj.optString("sizesCsv", "Standard"),
                    colors = colors,
                    sellingPrice = obj.optDouble("sellingPrice", 0.0),
                    promoDiscountPercent = obj.optInt("promoDiscountPercent", 0),
                    commissionPercent = obj.optDouble("commissionPercent", 10.0),
                    imageUrl = obj.optString("imageUrl"),
                    description = obj.optString("description"),
                    batchNumber = obj.optString("batchNumber"),
                    status = try {
                        PartnerSubmissionStatus.valueOf(obj.optString("status", PartnerSubmissionStatus.PENDING_ADMIN_VERIFICATION.name))
                    } catch (_: Exception) { PartnerSubmissionStatus.PENDING_ADMIN_VERIFICATION },
                    publishedProductId = obj.optString("publishedProductId"),
                    adminNote = obj.optString("adminNote"),
                    submittedAt = obj.optLong("submittedAt", System.currentTimeMillis()),
                    verifiedAt = obj.optLong("verifiedAt", 0L),
                    uploadedToCloudAt = obj.optLong("uploadedToCloudAt", 0L)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persistPartnerSubmissions(list: List<PartnerProductSubmission>) {
        val arr = JSONArray()
        list.forEach { s ->
            arr.put(JSONObject().apply {
                put("id", s.id)
                put("partnerId", s.partnerId)
                put("partnerName", s.partnerName)
                put("category", s.category)
                put("productName", s.productName)
                put("brand", s.brand)
                put("quantity", s.quantity)
                put("sizesCsv", s.sizesCsv)
                put("colorsStr", s.colors.joinToString(";") { "${it.name}|${it.hex}" })
                put("sellingPrice", s.sellingPrice)
                put("promoDiscountPercent", s.promoDiscountPercent)
                put("commissionPercent", s.commissionPercent)
                put("imageUrl", s.imageUrl)
                put("description", s.description)
                put("batchNumber", s.batchNumber)
                put("status", s.status.name)
                put("publishedProductId", s.publishedProductId)
                put("adminNote", s.adminNote)
                put("submittedAt", s.submittedAt)
                put("verifiedAt", s.verifiedAt)
                put("uploadedToCloudAt", s.uploadedToCloudAt)
            })
        }
        adminPrefs.edit().putString("partner_submissions_json", arr.toString()).apply()
    }

    private fun loadPartnerTransactions(): List<PartnerFolderTransaction> {
        val raw = adminPrefs.getString("partner_transactions_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                val id = obj.optString("id").trim()
                if (id.isBlank()) return@mapNotNull null
                PartnerFolderTransaction(
                    id = id,
                    partnerId = obj.optString("partnerId"),
                    partnerName = obj.optString("partnerName"),
                    type = try {
                        PartnerTransactionType.valueOf(obj.optString("type", PartnerTransactionType.PRODUCT_UPLOADED_TO_ADMIN.name))
                    } catch (_: Exception) { PartnerTransactionType.PRODUCT_UPLOADED_TO_ADMIN },
                    productId = obj.optString("productId"),
                    productName = obj.optString("productName"),
                    category = obj.optString("category"),
                    batchNumber = obj.optString("batchNumber"),
                    quantity = obj.optInt("quantity", 0),
                    grossAmount = obj.optDouble("grossAmount", 0.0),
                    commissionPercent = obj.optDouble("commissionPercent", 0.0),
                    commissionProfitAmount = obj.optDouble("commissionProfitAmount", 0.0),
                    partnerNetAmount = obj.optDouble("partnerNetAmount", 0.0),
                    orderId = obj.optString("orderId"),
                    customerName = obj.optString("customerName"),
                    customerPhone = obj.optString("customerPhone"),
                    paymentMethod = obj.optString("paymentMethod"),
                    paymentAccount = obj.optString("paymentAccount"),
                    paymentReference = obj.optString("paymentReference"),
                    receiptImageUrl = obj.optString("receiptImageUrl"),
                    alertSentToAdmin = obj.optBoolean("alertSentToAdmin", false),
                    alertSentToPartner = obj.optBoolean("alertSentToPartner", false),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    note = obj.optString("note")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persistPartnerTransactions(list: List<PartnerFolderTransaction>) {
        val arr = JSONArray()
        list.forEach { t ->
            arr.put(JSONObject().apply {
                put("id", t.id)
                put("partnerId", t.partnerId)
                put("partnerName", t.partnerName)
                put("type", t.type.name)
                put("productId", t.productId)
                put("productName", t.productName)
                put("category", t.category)
                put("batchNumber", t.batchNumber)
                put("quantity", t.quantity)
                put("grossAmount", t.grossAmount)
                put("commissionPercent", t.commissionPercent)
                put("commissionProfitAmount", t.commissionProfitAmount)
                put("partnerNetAmount", t.partnerNetAmount)
                put("orderId", t.orderId)
                put("customerName", t.customerName)
                put("customerPhone", t.customerPhone)
                put("paymentMethod", t.paymentMethod)
                put("paymentAccount", t.paymentAccount)
                put("paymentReference", t.paymentReference)
                put("receiptImageUrl", t.receiptImageUrl)
                put("alertSentToAdmin", t.alertSentToAdmin)
                put("alertSentToPartner", t.alertSentToPartner)
                put("timestamp", t.timestamp)
                put("note", t.note)
            })
        }
        adminPrefs.edit().putString("partner_transactions_json", arr.toString()).apply()
    }

    private val _partnerOrganizations = MutableStateFlow(loadPartnerOrganizations())
    val partnerOrganizations: StateFlow<List<PartnerOrganization>> = _partnerOrganizations.asStateFlow()

    private val _partnerSubmissions = MutableStateFlow(loadPartnerSubmissions())
    val partnerSubmissions: StateFlow<List<PartnerProductSubmission>> = _partnerSubmissions.asStateFlow()

    private val _partnerTransactions = MutableStateFlow(loadPartnerTransactions())
    val partnerTransactions: StateFlow<List<PartnerFolderTransaction>> = _partnerTransactions.asStateFlow()

    private val _latestPartnerPaymentAlert = MutableStateFlow<PartnerFolderTransaction?>(null)
    val latestPartnerPaymentAlert: StateFlow<PartnerFolderTransaction?> = _latestPartnerPaymentAlert.asStateFlow()

    val lockedPartnerIds: StateFlow<Set<String>> = _partnerOrganizations
        .map { orgs ->
            orgs.filter {
                it.isCatalogLocked ||
                    it.status == PartnerAgreementStatus.SUSPENDED ||
                    it.status == PartnerAgreementStatus.TERMINATED
            }.map { it.id }.toSet()
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun isProductCatalogLocked(product: Product): Boolean {
        if (product.isCatalogLocked) return true
        if (product.partnerId.isBlank()) return false
        val org = _partnerOrganizations.value.find { it.id == product.partnerId } ?: return false
        return org.isCatalogLocked ||
            org.status == PartnerAgreementStatus.SUSPENDED ||
            org.status == PartnerAgreementStatus.TERMINATED
    }

    companion object {
        const val THIRTY_DAYS_MS: Long = 30L * 86_400_000L

        fun parseHostingDurationDays(duration: String): Long {
            val clean = duration.trim().lowercase()
            val num = Regex("""(\d+)""").find(clean)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 6L
            return when {
                clean.contains("year") -> num.coerceAtLeast(1L) * 360L
                clean.contains("week") -> num.coerceAtLeast(1L) * 7L
                clean.contains("day") -> num.coerceAtLeast(1L)
                else -> num.coerceAtLeast(1L) * 30L
            }
        }
    }

    fun dismissPartnerPaymentAlert() {
        _latestPartnerPaymentAlert.value = null
    }

    private fun appendPartnerTransaction(txn: PartnerFolderTransaction) {
        val updated = listOf(txn) + _partnerTransactions.value
        _partnerTransactions.value = updated
        persistPartnerTransactions(updated)
    }

    private fun pushPartnerLedgerAsync(preferLocal: Boolean = true) {
        if (repository.supabaseSync.isPlaceholderConfig()) return
        viewModelScope.launch {
            syncPartnerLedgerWithCloudInternal(preferLocal = preferLocal)
        }
    }

    private suspend fun syncPartnerLedgerWithCloudInternal(preferLocal: Boolean = false) {
        if (repository.supabaseSync.isPlaceholderConfig()) return
        try {
            val res = repository.supabaseSync.syncPartnerLedgerWithCloud(
                localOrgs = _partnerOrganizations.value,
                localSubs = _partnerSubmissions.value,
                localTxns = _partnerTransactions.value,
                preferLocal = preferLocal
            )
            val ledger = res.getOrNull() ?: return
            _partnerOrganizations.value = ledger.organizations
            persistPartnerOrganizations(ledger.organizations)
            _partnerSubmissions.value = ledger.submissions
            persistPartnerSubmissions(ledger.submissions)
            _partnerTransactions.value = ledger.transactions
            persistPartnerTransactions(ledger.transactions)
        } catch (_: Exception) {
            // keep local ledger intact on offline/transient error
        }
    }

    fun runAutomatic30DayHostingBilling(referenceNowMs: Long = System.currentTimeMillis()): Int {
        val orgs = _partnerOrganizations.value
        if (orgs.isEmpty()) return 0
        var totalCyclesAutoDeductedNow = 0
        val newTxns = mutableListOf<PartnerFolderTransaction>()
        val targetStoreAccount = _storeProfile.value.opayAccountNumber
            .ifBlank { _storeProfile.value.moniepointAccountNumber }
            .ifBlank { "ODIYA Account" }

        val updatedOrgs = orgs.map { p ->
            if (p.status != PartnerAgreementStatus.FINALIZED || p.monthlyHostingPayment <= 0.0) {
                p
            } else {
                val contractStart = p.hostingContractStartAt.takeIf { it > 0L }
                    ?: p.adminSignedAt.takeIf { it > 0L }
                    ?: p.createdAt
                val contractEnd = p.hostingContractEndAt.takeIf { it > 0L }
                    ?: (contractStart + parseHostingDurationDays(p.hostingDuration) * 86_400_000L)
                var nextDue = p.nextHostingBillingDueAt.takeIf { it > 0L }
                    ?: (contractStart + THIRTY_DAYS_MS)
                var lastBilled = p.lastHostingBillingAt.takeIf { it > 0L } ?: contractStart
                var cyclesBilled = p.hostingCyclesBilled
                var totalHostingDeducted = p.totalHostingFeesAutoDeducted
                var safetyCounter = 0

                while (nextDue <= referenceNowMs && safetyCounter < 24) {
                    safetyCounter++
                    cyclesBilled += 1
                    totalHostingDeducted += p.monthlyHostingPayment
                    lastBilled = nextDue
                    nextDue += THIRTY_DAYS_MS
                    totalCyclesAutoDeductedNow++

                    val partnerAcct = p.opayAccountNumber.ifBlank { p.moniepointAccountNumber }
                        .ifBlank { "Partner Folder Account" }
                    newTxns.add(
                        PartnerFolderTransaction(
                            partnerId = p.id,
                            partnerName = p.businessName,
                            type = PartnerTransactionType.MONTHLY_HOSTING_FEE,
                            category = p.primaryCategory,
                            grossAmount = p.monthlyHostingPayment,
                            commissionProfitAmount = p.monthlyHostingPayment,
                            partnerNetAmount = 0.0,
                            paymentMethod = "Automated 30-Day Hosting Debit",
                            paymentAccount = "$partnerAcct → $targetStoreAccount",
                            alertSentToAdmin = true,
                            alertSentToPartner = true,
                            timestamp = lastBilled,
                            note = "Auto 30-day hosting cycle #$cyclesBilled deducted from ${p.businessName} to ODIYA account"
                        )
                    )
                }

                if (safetyCounter > 0 || p.hostingContractStartAt == 0L || p.nextHostingBillingDueAt == 0L) {
                    p.copy(
                        hostingContractStartAt = contractStart,
                        hostingContractEndAt = contractEnd,
                        lastHostingBillingAt = lastBilled,
                        nextHostingBillingDueAt = nextDue,
                        hostingCyclesBilled = cyclesBilled,
                        totalHostingFeesAutoDeducted = totalHostingDeducted
                    )
                } else {
                    p
                }
            }
        }

        if (totalCyclesAutoDeductedNow > 0 || updatedOrgs != orgs) {
            _partnerOrganizations.value = updatedOrgs
            persistPartnerOrganizations(updatedOrgs)
            if (newTxns.isNotEmpty()) {
                val mergedTxns = (newTxns.sortedByDescending { it.timestamp } + _partnerTransactions.value).take(400)
                _partnerTransactions.value = mergedTxns
                persistPartnerTransactions(mergedTxns)
                pushPartnerLedgerAsync(preferLocal = true)
            }
        }
        return totalCyclesAutoDeductedNow
    }

    fun submitPartnerAgreementApplication(
        businessName: String,
        ownerName: String,
        ownerPhone: String,
        businessAddress: String,
        primaryCategory: String,
        opayAccountNumber: String,
        opayAccountName: String,
        moniepointAccountNumber: String,
        moniepointAccountName: String,
        agreementDate: String,
        hostingDuration: String,
        agreedQuantityPerPeriod: Int,
        periodUnit: HostingPeriodUnit,
        monthlyHostingPayment: Double,
        stipulatedCommissionPercent: Double,
        partnerSignatureName: String,
        partnerSignatureStrokes: String,
        onComplete: (PartnerOrganization) -> Unit
    ) {
        val newPartner = PartnerOrganization(
            businessName = businessName.trim(),
            ownerName = ownerName.trim(),
            ownerPhone = ownerPhone.trim(),
            businessAddress = businessAddress.trim(),
            primaryCategory = primaryCategory.trim(),
            opayAccountNumber = opayAccountNumber.trim(),
            opayAccountName = opayAccountName.trim().ifBlank { businessName.trim() },
            moniepointAccountNumber = moniepointAccountNumber.trim(),
            moniepointAccountName = moniepointAccountName.trim().ifBlank { businessName.trim() },
            agreementDate = agreementDate.trim(),
            hostingDuration = hostingDuration.trim().ifBlank { "6 Months" },
            agreedQuantityPerPeriod = agreedQuantityPerPeriod.coerceAtLeast(1),
            periodUnit = periodUnit,
            monthlyHostingPayment = monthlyHostingPayment.coerceAtLeast(0.0),
            stipulatedCommissionPercent = stipulatedCommissionPercent.coerceIn(0.5, 90.0),
            partnerSignatureName = partnerSignatureName.trim().ifBlank { ownerName.trim() },
            partnerSignatureStrokes = partnerSignatureStrokes,
            status = PartnerAgreementStatus.PENDING_FINALIZATION,
            createdAt = System.currentTimeMillis()
        )
        val updated = listOf(newPartner) + _partnerOrganizations.value
        _partnerOrganizations.value = updated
        persistPartnerOrganizations(updated)
        pushPartnerLedgerAsync(preferLocal = true)
        voiceManager.speakText("New Business Partnership & MOU application received from ${newPartner.businessName} for ${newPartner.primaryCategory}.")
        onComplete(newPartner)
    }

    fun finalizePartnerAgreement(
        partnerId: String,
        adminSignatoryName: String,
        adminStrokes: String,
        commissionPercent: Double,
        agreedQtyPerPeriod: Int,
        periodUnit: HostingPeriodUnit,
        monthlyHostingPayment: Double,
        hostingDuration: String,
        opayNumber: String,
        opayName: String,
        moniepointNumber: String,
        moniepointName: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        val current = _partnerOrganizations.value
        val existing = current.find { it.id == partnerId }
        if (existing == null) {
            onComplete(false, "Partner organization not found.")
            return
        }
        val now = System.currentTimeMillis()
        val resolvedDuration = hostingDuration.trim().ifBlank { existing.hostingDuration }
        val durationDays = parseHostingDurationDays(resolvedDuration)
        val safeHostingFee = monthlyHostingPayment.coerceAtLeast(0.0)
        val alreadyBilledCycles = existing.hostingCyclesBilled
        val billInitialCycle = safeHostingFee > 0.0 && alreadyBilledCycles == 0

        val finalized = existing.copy(
            adminCounterSignatureName = adminSignatoryName.trim().ifBlank { "ODIYA Management" },
            adminCounterSignatureStrokes = adminStrokes,
            adminSignedAt = now,
            stipulatedCommissionPercent = commissionPercent.coerceIn(0.5, 90.0),
            agreedQuantityPerPeriod = agreedQtyPerPeriod.coerceAtLeast(1),
            periodUnit = periodUnit,
            monthlyHostingPayment = safeHostingFee,
            hostingDuration = resolvedDuration,
            opayAccountNumber = opayNumber.trim(),
            opayAccountName = opayName.trim().ifBlank { existing.businessName },
            moniepointAccountNumber = moniepointNumber.trim(),
            moniepointAccountName = moniepointName.trim().ifBlank { existing.businessName },
            status = PartnerAgreementStatus.FINALIZED,
            isCatalogLocked = false,
            catalogLockedAt = 0L,
            hostingContractStartAt = existing.hostingContractStartAt.takeIf { it > 0L } ?: now,
            hostingContractEndAt = (existing.hostingContractStartAt.takeIf { it > 0L } ?: now) + (durationDays * 86_400_000L),
            lastHostingBillingAt = if (billInitialCycle) now else existing.lastHostingBillingAt,
            nextHostingBillingDueAt = if (billInitialCycle) (now + THIRTY_DAYS_MS) else existing.nextHostingBillingDueAt.takeIf { it > 0L } ?: (now + THIRTY_DAYS_MS),
            hostingCyclesBilled = if (billInitialCycle) 1 else alreadyBilledCycles,
            totalHostingFeesAutoDeducted = if (billInitialCycle) existing.totalHostingFeesAutoDeducted + safeHostingFee else existing.totalHostingFeesAutoDeducted
        )
        val updated = current.map { if (it.id == partnerId) finalized else it }
        _partnerOrganizations.value = updated
        persistPartnerOrganizations(updated)

        if (billInitialCycle) {
            val targetStoreAccount = _storeProfile.value.opayAccountNumber
                .ifBlank { _storeProfile.value.moniepointAccountNumber }
                .ifBlank { "ODIYA Account" }
            val partnerAcct = finalized.opayAccountNumber.ifBlank { finalized.moniepointAccountNumber }
                .ifBlank { "Partner Folder Account" }
            appendPartnerTransaction(
                PartnerFolderTransaction(
                    partnerId = finalized.id,
                    partnerName = finalized.businessName,
                    type = PartnerTransactionType.MONTHLY_HOSTING_FEE,
                    category = finalized.primaryCategory,
                    grossAmount = safeHostingFee,
                    commissionProfitAmount = safeHostingFee,
                    partnerNetAmount = 0.0,
                    paymentMethod = "Automated 30-Day Hosting Debit (Cycle #1)",
                    paymentAccount = "$partnerAcct → $targetStoreAccount",
                    alertSentToAdmin = true,
                    alertSentToPartner = true,
                    timestamp = now,
                    note = "Initial 30-day hosting cycle #1 auto-deducted upon MOU finalization (${finalized.hostingDuration})"
                )
            )
        }
        pushPartnerLedgerAsync(preferLocal = true)
        voiceManager.speakText("Partnership agreement finalized for ${finalized.businessName}. Automated percentage split and 30-day hosting schedule are now active.")
        onComplete(true, "MOU counter-signed & finalized! Automated split + 30-day hosting billing active for ${finalized.businessName}.")
    }

    fun updatePartnerEditableTerms(
        partnerId: String,
        commissionPercent: Double,
        agreedQtyPerPeriod: Int,
        periodUnit: HostingPeriodUnit,
        monthlyHostingPayment: Double,
        hostingDuration: String,
        opayNumber: String,
        opayName: String,
        moniepointNumber: String,
        moniepointName: String
    ) {
        val updated = _partnerOrganizations.value.map { p ->
            if (p.id == partnerId) {
                val resolvedDuration = hostingDuration.trim().ifBlank { p.hostingDuration }
                val startAt = p.hostingContractStartAt.takeIf { it > 0L } ?: p.adminSignedAt.takeIf { it > 0L } ?: p.createdAt
                val updatedEnd = startAt + parseHostingDurationDays(resolvedDuration) * 86_400_000L
                p.copy(
                    stipulatedCommissionPercent = commissionPercent.coerceIn(0.5, 90.0),
                    agreedQuantityPerPeriod = agreedQtyPerPeriod.coerceAtLeast(1),
                    periodUnit = periodUnit,
                    monthlyHostingPayment = monthlyHostingPayment.coerceAtLeast(0.0),
                    hostingDuration = resolvedDuration,
                    hostingContractEndAt = updatedEnd,
                    opayAccountNumber = opayNumber.trim(),
                    opayAccountName = opayName.trim().ifBlank { p.businessName },
                    moniepointAccountNumber = moniepointNumber.trim(),
                    moniepointAccountName = moniepointName.trim().ifBlank { p.businessName }
                )
            } else p
        }
        _partnerOrganizations.value = updated
        persistPartnerOrganizations(updated)
        pushPartnerLedgerAsync(preferLocal = true)
    }

    fun submitPartnerProductToAdminStaging(
        partnerId: String,
        category: String,
        productName: String,
        brand: String,
        quantity: Int,
        sizesCsv: String,
        colors: List<ProductColor>,
        sellingPrice: Double,
        promoDiscountPercent: Int,
        commissionPercent: Double,
        pickedImageUriString: String,
        description: String,
        batchNumber: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        val partner = _partnerOrganizations.value.find { it.id == partnerId }
        if (partner == null) {
            onComplete(false, "Partner organization not found.")
            return
        }
        if (partner.status == PartnerAgreementStatus.TERMINATED) {
            onComplete(false, "Contract terminated — new product uploads are permanently closed.")
            return
        }
        if (partner.status != PartnerAgreementStatus.FINALIZED) {
            onComplete(false, "MOU must be finalized by ODIYA before uploading products.")
            return
        }
        if (partner.isCatalogLocked) {
            onComplete(false, "Partner Catalog Lock is active after hosting expiry. Request Admin renewal & unlock first.")
            return
        }
        if (productName.isBlank() || brand.isBlank() || sellingPrice <= 0.0 || quantity <= 0) {
            onComplete(false, "Please enter valid Product Name, Brand, Quantity, and Selling Price.")
            return
        }

        viewModelScope.launch {
            val localStagingImage = if (pickedImageUriString.isNotBlank()) {
                repository.persistLocalPartnerStagingImage(partnerId, pickedImageUriString)
            } else ""

            val cleanBatch = batchNumber.trim().ifBlank {
                "BATCH-${java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date())}"
            }
            val safeCommission = commissionPercent.coerceIn(0.5, 90.0)
            val sub = PartnerProductSubmission(
                partnerId = partner.id,
                partnerName = partner.businessName,
                category = category,
                productName = productName.trim(),
                brand = brand.trim(),
                quantity = quantity.coerceAtLeast(1),
                sizesCsv = sizesCsv.trim().ifBlank { "Standard" },
                colors = colors.ifEmpty { listOf(ProductColor("Default", "#1E40AF")) },
                sellingPrice = sellingPrice,
                promoDiscountPercent = promoDiscountPercent.coerceIn(0, 90),
                commissionPercent = safeCommission,
                imageUrl = localStagingImage,
                description = description.trim().ifBlank { "${productName.trim()} by ${brand.trim()} — Hosted via ${partner.businessName}" },
                batchNumber = cleanBatch,
                status = PartnerSubmissionStatus.PENDING_ADMIN_VERIFICATION,
                submittedAt = System.currentTimeMillis()
            )
            val updated = listOf(sub) + _partnerSubmissions.value
            _partnerSubmissions.value = updated
            persistPartnerSubmissions(updated)

            val grossBatchValue = sub.sellingPrice * sub.quantity
            appendPartnerTransaction(
                PartnerFolderTransaction(
                    partnerId = partner.id,
                    partnerName = partner.businessName,
                    type = PartnerTransactionType.PRODUCT_UPLOADED_TO_ADMIN,
                    productId = sub.id,
                    productName = sub.productName,
                    category = sub.category,
                    batchNumber = sub.batchNumber,
                    quantity = sub.quantity,
                    grossAmount = grossBatchValue,
                    commissionPercent = safeCommission,
                    commissionProfitAmount = grossBatchValue * (safeCommission / 100.0),
                    partnerNetAmount = grossBatchValue * (1.0 - safeCommission / 100.0),
                    note = "Staged to Admin Control Room (NOT yet on Supabase Cloud)"
                )
            )
            pushPartnerLedgerAsync(preferLocal = true)
            voiceManager.speakText("Partner ${partner.businessName} uploaded ${sub.productName} to Admin Control Room for verification.")
            onComplete(true, "'${sub.productName}' uploaded to Admin Control Room Staging (NOT yet on Supabase Cloud) for Admin verification.")
        }
    }

    fun updateStagedSubmissionDetails(
        submissionId: String,
        category: String,
        productName: String,
        brand: String,
        quantity: Int,
        sizesCsv: String,
        sellingPrice: Double,
        promoDiscountPercent: Int,
        commissionPercent: Double,
        batchNumber: String,
        adminNote: String
    ) {
        val updated = _partnerSubmissions.value.map { sub ->
            if (sub.id == submissionId) {
                sub.copy(
                    category = category,
                    productName = productName.trim(),
                    brand = brand.trim(),
                    quantity = quantity.coerceAtLeast(0),
                    sizesCsv = sizesCsv.trim().ifBlank { "Standard" },
                    sellingPrice = sellingPrice.coerceAtLeast(0.0),
                    promoDiscountPercent = promoDiscountPercent.coerceIn(0, 90),
                    commissionPercent = commissionPercent.coerceIn(0.5, 90.0),
                    batchNumber = batchNumber.trim(),
                    adminNote = adminNote.trim()
                )
            } else sub
        }
        _partnerSubmissions.value = updated
        persistPartnerSubmissions(updated)
        pushPartnerLedgerAsync(preferLocal = true)
    }

    fun verifyStagedPartnerProduct(submissionId: String, approve: Boolean, note: String = "") {
        var target: PartnerProductSubmission? = null
        val updated = _partnerSubmissions.value.map { sub ->
            if (sub.id == submissionId) {
                val nextStatus = if (approve) {
                    PartnerSubmissionStatus.VERIFIED_READY_FOR_CLOUD
                } else {
                    PartnerSubmissionStatus.REJECTED
                }
                val modified = sub.copy(
                    status = nextStatus,
                    verifiedAt = System.currentTimeMillis(),
                    adminNote = note.trim().ifBlank { sub.adminNote }
                )
                target = modified
                modified
            } else sub
        }
        _partnerSubmissions.value = updated
        persistPartnerSubmissions(updated)

        target?.let { sub ->
            if (approve) {
                val grossValue = sub.sellingPrice * sub.quantity
                appendPartnerTransaction(
                    PartnerFolderTransaction(
                        partnerId = sub.partnerId,
                        partnerName = sub.partnerName,
                        type = PartnerTransactionType.ADMIN_VERIFIED_PRODUCT,
                        productId = sub.id,
                        productName = sub.productName,
                        category = sub.category,
                        batchNumber = sub.batchNumber,
                        quantity = sub.quantity,
                        grossAmount = grossValue,
                        commissionPercent = sub.commissionPercent,
                        commissionProfitAmount = grossValue * (sub.commissionPercent / 100.0),
                        partnerNetAmount = grossValue * (1.0 - sub.commissionPercent / 100.0),
                        note = "Verified in Admin Control Room (${sub.commissionPercent}% fee). Ready for Supabase batch upload."
                    )
                )
            }
        }
        pushPartnerLedgerAsync(preferLocal = true)
    }

    fun verifyAllPendingForPartner(partnerId: String? = null) {
        val now = System.currentTimeMillis()
        val newlyVerified = mutableListOf<PartnerProductSubmission>()
        val updated = _partnerSubmissions.value.map { sub ->
            if (sub.status == PartnerSubmissionStatus.PENDING_ADMIN_VERIFICATION &&
                (partnerId == null || sub.partnerId == partnerId)
            ) {
                val next = sub.copy(
                    status = PartnerSubmissionStatus.VERIFIED_READY_FOR_CLOUD,
                    verifiedAt = now
                )
                newlyVerified.add(next)
                next
            } else sub
        }
        _partnerSubmissions.value = updated
        persistPartnerSubmissions(updated)
        newlyVerified.forEach { sub ->
            val grossValue = sub.sellingPrice * sub.quantity
            appendPartnerTransaction(
                PartnerFolderTransaction(
                    partnerId = sub.partnerId,
                    partnerName = sub.partnerName,
                    type = PartnerTransactionType.ADMIN_VERIFIED_PRODUCT,
                    productId = sub.id,
                    productName = sub.productName,
                    category = sub.category,
                    batchNumber = sub.batchNumber,
                    quantity = sub.quantity,
                    grossAmount = grossValue,
                    commissionPercent = sub.commissionPercent,
                    commissionProfitAmount = grossValue * (sub.commissionPercent / 100.0),
                    partnerNetAmount = grossValue * (1.0 - sub.commissionPercent / 100.0),
                    note = "Batch-verified in Admin Control Room"
                )
            )
        }
        pushPartnerLedgerAsync(preferLocal = true)
    }

    fun batchUploadPartnerProductsToSupabase(
        partnerId: String? = null,
        onComplete: (Boolean, String) -> Unit
    ) {
        val verifiedSubs = _partnerSubmissions.value.filter { sub ->
            sub.status == PartnerSubmissionStatus.VERIFIED_READY_FOR_CLOUD &&
                (partnerId == null || sub.partnerId == partnerId)
        }
        if (verifiedSubs.isEmpty()) {
            onComplete(false, "No verified partner products waiting for batch upload. Verify staged products first.")
            return
        }

        viewModelScope.launch {
            _isSupabaseBusy.value = true
            _cloudStatusMessage.value = "☁️ Uploading ${verifiedSubs.size} verified partner product(s) in batches to Supabase Cloud..."
            val now = System.currentTimeMillis()
            val mappedProducts = verifiedSubs.map { sub ->
                val sizes = sub.sizesCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }.ifEmpty { listOf("Standard") }
                val existingProdId = sub.publishedProductId.ifBlank { "part-${sub.id.lowercase()}" }
                val partnerOrg = _partnerOrganizations.value.find { it.id == sub.partnerId }
                Product(
                    id = existingProdId,
                    title = sub.productName,
                    category = sub.category,
                    brand = sub.brand,
                    availableBrands = listOf(sub.brand),
                    availableSizes = sizes,
                    availableColors = sub.colors.ifEmpty { listOf(ProductColor("Default", "#1E40AF")) },
                    basePrice = sub.sellingPrice,
                    costPrice = sub.sellingPrice * (1.0 - (sub.commissionPercent / 100.0)),
                    discountPercent = sub.promoDiscountPercent,
                    stockQuantity = sub.quantity,
                    imageUrl = sub.imageUrl,
                    model3dType = when (sub.category.lowercase()) {
                        "shoes" -> "SNEAKER"
                        "electronics" -> "PHONE"
                        "bags" -> "BAG"
                        "beverages" -> "BOTTLE"
                        "beauty product" -> "COSMETIC"
                        else -> "BOX"
                    },
                    description = "${sub.description}\n[Partner: ${sub.partnerName} | Batch: ${sub.batchNumber}]".trim(),
                    sku = "PRT-${sub.id.takeLast(6)}",
                    createdAt = now,
                    partnerId = sub.partnerId,
                    partnerName = sub.partnerName,
                    partnerCommissionPercent = sub.commissionPercent,
                    partnerBatchId = sub.batchNumber,
                    isCatalogLocked = partnerOrg?.isCatalogLocked == true
                )
            }

            val uploadRes = repository.uploadPartnerBatchToCloud(mappedProducts)
            _isSupabaseBusy.value = false
            uploadRes.fold(
                onSuccess = { (cloudCount, syncedList) ->
                    val syncedByProdId = syncedList.associateBy { it.id }
                    val subToProdId = verifiedSubs.indices.associate { idx ->
                        verifiedSubs[idx].id to mappedProducts[idx].id
                    }
                    val updatedSubs = _partnerSubmissions.value.map { sub ->
                        val pubId = subToProdId[sub.id]
                        if (pubId != null) {
                            val syncedProd = syncedByProdId[pubId]
                            sub.copy(
                                status = PartnerSubmissionStatus.UPLOADED_TO_SUPABASE,
                                publishedProductId = pubId,
                                imageUrl = syncedProd?.imageUrl ?: sub.imageUrl,
                                uploadedToCloudAt = now
                            )
                        } else sub
                    }
                    _partnerSubmissions.value = updatedSubs
                    persistPartnerSubmissions(updatedSubs)

                    verifiedSubs.forEach { sub ->
                        val grossValue = sub.sellingPrice * sub.quantity
                        appendPartnerTransaction(
                            PartnerFolderTransaction(
                                partnerId = sub.partnerId,
                                partnerName = sub.partnerName,
                                type = PartnerTransactionType.BATCH_UPLOADED_TO_SUPABASE,
                                productId = subToProdId[sub.id] ?: sub.id,
                                productName = sub.productName,
                                category = sub.category,
                                batchNumber = sub.batchNumber,
                                quantity = sub.quantity,
                                grossAmount = grossValue,
                                commissionPercent = sub.commissionPercent,
                                commissionProfitAmount = grossValue * (sub.commissionPercent / 100.0),
                                partnerNetAmount = grossValue * (1.0 - sub.commissionPercent / 100.0),
                                note = if (cloudCount > 0) "Synced to Supabase Cloud & live catalog (${sub.category})" else "Published to local catalog (Link Supabase to push to remote cloud)"
                            )
                        )
                    }
                    pushPartnerLedgerAsync(preferLocal = true)

                    val msg = if (cloudCount > 0) {
                        "✅ Uploaded batch of $cloudCount verified partner product(s) to Supabase Cloud & live catalog!"
                    } else {
                        "⚠️ Published ${verifiedSubs.size} product(s) to catalog locally (Link Supabase Cloud to sync across all devices)."
                    }
                    _cloudStatusMessage.value = msg
                    voiceManager.speakText("Batch upload complete. ${verifiedSubs.size} partner products are now live in catalog.")
                    onComplete(true, msg)
                },
                onFailure = { err ->
                    val msg = "❌ Batch upload error: ${err.message}"
                    _cloudStatusMessage.value = msg
                    onComplete(false, msg)
                }
            )
        }
    }

    fun resolvePartnerFolderForCart(cartItems: List<CartItem>): PartnerOrganization? {
        val firstPartnerId = cartItems.firstOrNull { it.product.partnerId.isNotBlank() }?.product?.partnerId
            ?: return null
        return _partnerOrganizations.value.find { it.id == firstPartnerId }
    }

    private fun recordPartnerOrderTransactionsAndAlerts(
        order: Order,
        partnerItems: List<CartItem>,
        eventType: PartnerTransactionType,
        discountFactor: Double = 1.0,
        creditPartnerTotals: Boolean = true
    ) {
        val groupedByPartner = partnerItems.groupBy { it.product.partnerId }
        val naira = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("en", "NG")).apply {
            maximumFractionDigits = 0
        }
        val storeAcct = _storeProfile.value.opayAccountNumber
            .ifBlank { _storeProfile.value.moniepointAccountNumber }
            .ifBlank { "ODIYA Account" }

        groupedByPartner.forEach { (partnerId, pItems) ->
            val partnerOrg = _partnerOrganizations.value.find { it.id == partnerId }
            val partnerName = partnerOrg?.businessName ?: pItems.firstOrNull()?.product?.partnerName ?: "Partner Business"
            val totalQty = pItems.sumOf { it.quantity }
            val grossAmt = pItems.sumOf { (it.unitPrice * it.quantity) * discountFactor }
            val commissionProfit = pItems.sumOf {
                ((it.unitPrice * it.quantity) * discountFactor) * (it.product.partnerCommissionPercent.coerceIn(0.0, 95.0) / 100.0)
            }
            val partnerNet = (grossAmt - commissionProfit).coerceAtLeast(0.0)
            val avgPct = if (grossAmt > 0.0) (commissionProfit / grossAmt) * 100.0 else (partnerOrg?.stipulatedCommissionPercent ?: 10.0)
            val itemTitles = pItems.joinToString(", ") { "${it.quantity}x ${it.product.title}" }
            val partnerAcct = partnerOrg?.opayAccountNumber?.ifBlank { partnerOrg.moniepointAccountNumber }
                ?.ifBlank { "Partner Settlement Account" } ?: "Partner Settlement Account"

            val txn = PartnerFolderTransaction(
                partnerId = partnerId,
                partnerName = partnerName,
                type = eventType,
                productId = pItems.firstOrNull()?.product?.id ?: "",
                productName = itemTitles,
                category = pItems.firstOrNull()?.product?.category ?: (partnerOrg?.primaryCategory ?: ""),
                batchNumber = pItems.firstOrNull()?.product?.partnerBatchId ?: "",
                quantity = totalQty,
                grossAmount = grossAmt,
                commissionPercent = Math.round(avgPct * 10.0) / 10.0,
                commissionProfitAmount = commissionProfit,
                partnerNetAmount = partnerNet,
                orderId = order.orderId,
                customerName = order.customerName,
                customerPhone = order.customerPhone,
                paymentMethod = order.paymentMethod.title,
                paymentAccount = "ODIYA ($storeAcct): ${naira.format(commissionProfit)} | Partner ($partnerAcct): ${naira.format(partnerNet)}",
                paymentReference = order.paymentReference,
                receiptImageUrl = order.receiptImageUrl,
                alertSentToAdmin = true,
                alertSentToPartner = true,
                timestamp = System.currentTimeMillis(),
                note = "Auto-Split: ${String.format(java.util.Locale.US, "%.1f", avgPct)}% (${naira.format(commissionProfit)}) -> ODIYA | Balance (${naira.format(partnerNet)}) -> $partnerName"
            )
            appendPartnerTransaction(txn)
            if (creditPartnerTotals) {
                val updatedOrgs = _partnerOrganizations.value.map { org ->
                    if (org.id == partnerId) {
                        org.copy(
                            totalAutoDeducted247Share = org.totalAutoDeducted247Share + commissionProfit,
                            totalAutoPartnerNetDeposited = org.totalAutoPartnerNetDeposited + partnerNet
                        )
                    } else org
                }
                _partnerOrganizations.value = updatedOrgs
                persistPartnerOrganizations(updatedOrgs)
            }
            _latestPartnerPaymentAlert.value = txn
            ShopNotificationHelper.notifyPartnerPaymentAlert(
                context = getApplication(),
                partnerName = partnerName,
                orderId = order.orderId,
                grossAmountText = naira.format(grossAmt),
                commissionProfitText = naira.format(commissionProfit),
                partnerPayoutText = naira.format(partnerNet),
                paymentAccount = txn.paymentAccount
            )
        }
        pushPartnerLedgerAsync(preferLocal = true)
    }

    fun togglePartnerCatalogLock(
        partnerId: String,
        lock: Boolean,
        reason: String = "",
        onComplete: ((Boolean, String) -> Unit)? = null
    ) {
        val partner = _partnerOrganizations.value.find { it.id == partnerId }
        if (partner == null) {
            onComplete?.invoke(false, "Partner organization not found.")
            return
        }
        if (partner.status == PartnerAgreementStatus.TERMINATED) {
            onComplete?.invoke(false, "Contract was terminated and products deleted.")
            return
        }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val updatedOrgs = _partnerOrganizations.value.map { org ->
                if (org.id == partnerId) {
                    org.copy(
                        isCatalogLocked = lock,
                        catalogLockedAt = if (lock) now else 0L
                    )
                } else org
            }
            _partnerOrganizations.value = updatedOrgs
            persistPartnerOrganizations(updatedOrgs)

            // Sync product lock state to Room & Supabase Cloud
            val lockRes = repository.setPartnerProductsCatalogLocked(partnerId, lock)
            val affectedCount = lockRes.getOrDefault(0)

            // If locking, also strip any cart items belonging to this partner so customers cannot check out
            if (lock) {
                _cartItems.value = _cartItems.value.filterNot { it.product.partnerId == partnerId }
            }

            appendPartnerTransaction(
                PartnerFolderTransaction(
                    partnerId = partner.id,
                    partnerName = partner.businessName,
                    type = if (lock) PartnerTransactionType.CATALOG_LOCKED_HOSTING_ENDED else PartnerTransactionType.HOSTING_CONTRACT_RENEWED,
                    category = partner.primaryCategory,
                    quantity = affectedCount,
                    alertSentToAdmin = true,
                    alertSentToPartner = true,
                    timestamp = now,
                    note = reason.trim().ifBlank {
                        if (lock) "Hosting period ended — Admin activated Catalog Lock (products displayed, Add-to-Cart disabled)"
                        else "Admin unlocked Catalog Lock — Add-to-Cart re-enabled"
                    }
                )
            )
            pushPartnerLedgerAsync(preferLocal = true)
            val msg = if (lock) {
                "🔒 Catalog Lock enabled for ${partner.businessName} ($affectedCount SKU(s) still displayed; Add-to-Cart disabled)."
            } else {
                "🔓 Catalog Unlocked for ${partner.businessName} ($affectedCount SKU(s) active for customer Add-to-Cart)."
            }
            voiceManager.speakText(
                if (lock) "Catalog locked for ${partner.businessName}. Products remain visible with Add to Cart disabled."
                else "Catalog unlocked for ${partner.businessName}. Customers can now add items to cart."
            )
            onComplete?.invoke(true, msg)
        }
    }

    fun renewPartnerHostingContract(
        partnerId: String,
        newDuration: String,
        monthlyFeeOverride: Double? = null,
        onComplete: ((Boolean, String) -> Unit)? = null
    ) {
        val partner = _partnerOrganizations.value.find { it.id == partnerId }
        if (partner == null) {
            onComplete?.invoke(false, "Partner organization not found.")
            return
        }
        if (partner.status == PartnerAgreementStatus.TERMINATED) {
            onComplete?.invoke(false, "Cannot renew a permanently terminated contract.")
            return
        }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val cleanDuration = newDuration.trim().ifBlank { partner.hostingDuration }
            val durationDays = parseHostingDurationDays(cleanDuration)
            val resolvedFee = (monthlyFeeOverride ?: partner.monthlyHostingPayment).coerceAtLeast(0.0)
            val renewCycles = partner.hostingCyclesBilled + (if (resolvedFee > 0.0) 1 else 0)
            val renewFeeTotal = partner.totalHostingFeesAutoDeducted + resolvedFee

            val updatedOrgs = _partnerOrganizations.value.map { org ->
                if (org.id == partnerId) {
                    org.copy(
                        status = PartnerAgreementStatus.FINALIZED,
                        isCatalogLocked = false,
                        catalogLockedAt = 0L,
                        hostingDuration = cleanDuration,
                        monthlyHostingPayment = resolvedFee,
                        hostingContractStartAt = now,
                        hostingContractEndAt = now + durationDays * 86_400_000L,
                        lastHostingBillingAt = if (resolvedFee > 0.0) now else org.lastHostingBillingAt,
                        nextHostingBillingDueAt = now + THIRTY_DAYS_MS,
                        hostingCyclesBilled = renewCycles,
                        totalHostingFeesAutoDeducted = renewFeeTotal
                    )
                } else org
            }
            _partnerOrganizations.value = updatedOrgs
            persistPartnerOrganizations(updatedOrgs)

            val unlockRes = repository.setPartnerProductsCatalogLocked(partnerId, false)
            val unlockedCount = unlockRes.getOrDefault(0)
            val targetStoreAccount = _storeProfile.value.opayAccountNumber
                .ifBlank { _storeProfile.value.moniepointAccountNumber }
                .ifBlank { "ODIYA Account" }
            val partnerAcct = partner.opayAccountNumber.ifBlank { partner.moniepointAccountNumber }
                .ifBlank { "Partner Folder Account" }

            appendPartnerTransaction(
                PartnerFolderTransaction(
                    partnerId = partner.id,
                    partnerName = partner.businessName,
                    type = PartnerTransactionType.HOSTING_CONTRACT_RENEWED,
                    category = partner.primaryCategory,
                    quantity = unlockedCount,
                    grossAmount = resolvedFee,
                    commissionProfitAmount = resolvedFee,
                    partnerNetAmount = 0.0,
                    paymentMethod = "Contract Renewal & 30-Day Hosting Cycle #$renewCycles",
                    paymentAccount = "$partnerAcct → $targetStoreAccount",
                    alertSentToAdmin = true,
                    alertSentToPartner = true,
                    timestamp = now,
                    note = "Renewed hosting ($cleanDuration) & unlocked $unlockedCount SKU(s)"
                )
            )
            pushPartnerLedgerAsync(preferLocal = true)
            val msg = "🔓 Contract renewed for ${partner.businessName} ($cleanDuration)! Catalog unlocked & $unlockedCount SKU(s) reactivated."
            voiceManager.speakText("Hosting contract renewed and catalog unlocked for ${partner.businessName}.")
            onComplete?.invoke(true, msg)
        }
    }

    fun terminatePartnerContractAndDeleteAll(
        partnerId: String,
        reason: String = "Hosting contract terminated at end of period",
        onComplete: ((Boolean, String) -> Unit)? = null
    ) {
        val partner = _partnerOrganizations.value.find { it.id == partnerId }
        if (partner == null) {
            onComplete?.invoke(false, "Partner organization not found.")
            return
        }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val extraImages = _partnerSubmissions.value
                .filter { it.partnerId == partnerId }
                .map { it.imageUrl }
                .filter { it.isNotBlank() }

            val deleteRes = repository.deletePartnerProductsAndImages(partnerId, extraImages)
            val deletedCount = deleteRes.getOrDefault(0)

            _cartItems.value = _cartItems.value.filterNot { it.product.partnerId == partnerId }

            val updatedSubs = _partnerSubmissions.value.map { sub ->
                if (sub.partnerId == partnerId) {
                    sub.copy(
                        status = PartnerSubmissionStatus.REJECTED,
                        imageUrl = "",
                        publishedProductId = "",
                        adminNote = "Deleted outright — Contract Terminated"
                    )
                } else sub
            }
            _partnerSubmissions.value = updatedSubs
            persistPartnerSubmissions(updatedSubs)

            val updatedOrgs = _partnerOrganizations.value.map { org ->
                if (org.id == partnerId) {
                    org.copy(
                        status = PartnerAgreementStatus.TERMINATED,
                        isCatalogLocked = true,
                        catalogLockedAt = now
                    )
                } else org
            }
            _partnerOrganizations.value = updatedOrgs
            persistPartnerOrganizations(updatedOrgs)

            appendPartnerTransaction(
                PartnerFolderTransaction(
                    partnerId = partner.id,
                    partnerName = partner.businessName,
                    type = PartnerTransactionType.CONTRACT_TERMINATED_DELETED,
                    category = partner.primaryCategory,
                    quantity = deletedCount,
                    alertSentToAdmin = true,
                    alertSentToPartner = true,
                    timestamp = now,
                    note = "${reason.trim().ifBlank { "Contract Terminated" }} • Deleted $deletedCount product(s) & all images outright."
                )
            )
            pushPartnerLedgerAsync(preferLocal = true)
            val msg = "🗑️ Contract terminated for ${partner.businessName}. Deleted $deletedCount live product(s) & images outright."
            voiceManager.speakText("Partnership contract terminated for ${partner.businessName}. All product images and records deleted.")
            onComplete?.invoke(true, msg)
        }
    }

    fun authenticatePartnerPortal(identifier: String): PartnerOrganization? {
        val clean = identifier.trim()
        if (clean.isBlank()) return null
        val cleanDigits = clean.filter { it.isDigit() }
        return _partnerOrganizations.value.firstOrNull { org ->
            org.id.equals(clean, ignoreCase = true) ||
                org.businessName.equals(clean, ignoreCase = true) ||
                (cleanDigits.length >= 6 && org.ownerPhone.filter { it.isDigit() }.endsWith(cleanDigits.takeLast(10)))
        }
    }

    fun processPartnerOfflineSaleCart(
        partnerId: String,
        quantitiesByProductId: Map<String, Int>,
        note: String = "",
        onComplete: (Boolean, String) -> Unit
    ) {
        val partner = _partnerOrganizations.value.find { it.id == partnerId }
        if (partner == null) {
            onComplete(false, "Partner organization not found.")
            return
        }
        val cleanDeductions = quantitiesByProductId.filterValues { it > 0 }
        if (cleanDeductions.isEmpty()) {
            onComplete(false, "Add at least one sold product to the Offline Sale Cart first.")
            return
        }

        viewModelScope.launch {
            val partnerProds = allProducts.value.filter { it.partnerId == partnerId }
            val matchedMap = mutableMapOf<String, Pair<Product, Int>>()
            cleanDeductions.forEach { (pid, requestedQty) ->
                val prod = partnerProds.find { it.id == pid }
                if (prod != null && prod.stockQuantity > 0) {
                    val safeQty = requestedQty.coerceIn(1, prod.stockQuantity)
                    matchedMap[pid] = prod to safeQty
                }
            }
            if (matchedMap.isEmpty()) {
                onComplete(false, "Selected partner product(s) are out of stock or not found.")
                return@launch
            }

            val qtyMap = matchedMap.mapValues { it.value.second }
            val res = repository.deductPartnerProductsStock(qtyMap)
            if (res.isFailure) {
                onComplete(false, res.exceptionOrNull()?.message ?: "Failed updating online inventory.")
                return@launch
            }

            val now = System.currentTimeMillis()
            val totalRemoved = matchedMap.values.sumOf { it.second }
            val totalRetailValue = matchedMap.values.sumOf { (p, q) -> p.basePrice * q }
            val summaryNames = matchedMap.values.joinToString(", ") { (p, q) -> "${q}x ${p.title}" }
            val avgComm = matchedMap.values.firstOrNull()?.first?.partnerCommissionPercent ?: partner.stipulatedCommissionPercent

            // Sync remaining quantities back into partnerSubmissions mirror for clean reporting
            val updatedSubs = _partnerSubmissions.value.map { sub ->
                val matchedEntry = matchedMap[sub.publishedProductId]
                if (matchedEntry != null) {
                    val newStock = (matchedEntry.first.stockQuantity - matchedEntry.second).coerceAtLeast(0)
                    sub.copy(quantity = newStock)
                } else sub
            }
            _partnerSubmissions.value = updatedSubs
            persistPartnerSubmissions(updatedSubs)

            appendPartnerTransaction(
                PartnerFolderTransaction(
                    partnerId = partner.id,
                    partnerName = partner.businessName,
                    type = PartnerTransactionType.PARTNER_OFFLINE_SALE_REMOVAL,
                    productId = matchedMap.keys.firstOrNull().orEmpty(),
                    productName = summaryNames,
                    category = partner.primaryCategory,
                    quantity = totalRemoved,
                    grossAmount = totalRetailValue,
                    commissionPercent = avgComm,
                    commissionProfitAmount = 0.0,
                    partnerNetAmount = totalRetailValue,
                    paymentMethod = "Partner Offline Sale (Self Add-to-Cart)",
                    paymentAccount = "Stock Removed: -$totalRemoved units",
                    alertSentToAdmin = true,
                    alertSentToPartner = true,
                    timestamp = now,
                    note = note.trim().ifBlank { "Partner logged in and removed $totalRemoved offline-sold unit(s): $summaryNames" }
                )
            )
            pushPartnerLedgerAsync(preferLocal = true)
            voiceManager.speakText("${partner.businessName} logged an offline sale removing $totalRemoved units from online stock.")
            onComplete(true, "✅ Offline sale recorded! Removed $totalRemoved unit(s) ($summaryNames) from live online catalog & cloud.")
        }
    }

    fun collectUnreportedOfflineSalePenalty(
        partnerId: String,
        productId: String,
        unreportedQty: Int,
        reason: String = "",
        onComplete: (Boolean, String) -> Unit
    ) {
        val partner = _partnerOrganizations.value.find { it.id == partnerId }
        val product = allProducts.value.find { it.id == productId && it.partnerId == partnerId }
        if (partner == null || product == null) {
            onComplete(false, "Partner or product not found.")
            return
        }
        val safeQty = unreportedQty.coerceAtLeast(1)
        viewModelScope.launch {
            val pct = product.partnerCommissionPercent.takeIf { it > 0.0 } ?: partner.stipulatedCommissionPercent
            val grossVal = product.basePrice * safeQty
            val accruedHostingFee = grossVal * (pct.coerceIn(0.5, 95.0) / 100.0)
            val now = System.currentTimeMillis()

            // Also remove any remaining unrecorded units from live stock if still present
            if (product.stockQuantity > 0) {
                repository.deductPartnerProductsStock(mapOf(product.id to safeQty.coerceAtMost(product.stockQuantity)))
            }

            val updatedOrgs = _partnerOrganizations.value.map { org ->
                if (org.id == partnerId) {
                    org.copy(
                        totalAccruedOfflinePenalty = org.totalAccruedOfflinePenalty + accruedHostingFee,
                        totalHostingFeesAutoDeducted = org.totalHostingFeesAutoDeducted + accruedHostingFee
                    )
                } else org
            }
            _partnerOrganizations.value = updatedOrgs
            persistPartnerOrganizations(updatedOrgs)

            val storeAcct = _storeProfile.value.opayAccountNumber
                .ifBlank { _storeProfile.value.moniepointAccountNumber }
                .ifBlank { "ODIYA Account" }
            val naira = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("en", "NG")).apply {
                maximumFractionDigits = 0
            }

            appendPartnerTransaction(
                PartnerFolderTransaction(
                    partnerId = partner.id,
                    partnerName = partner.businessName,
                    type = PartnerTransactionType.UNREPORTED_OFFLINE_SALE_PENALTY,
                    productId = product.id,
                    productName = "${safeQty}x ${product.title}",
                    category = product.category,
                    quantity = safeQty,
                    grossAmount = grossVal,
                    commissionPercent = pct,
                    commissionProfitAmount = accruedHostingFee,
                    partnerNetAmount = 0.0,
                    paymentMethod = "Accrued Unit Hosting % Penalty Collection",
                    paymentAccount = "Credited to ODIYA ($storeAcct)",
                    alertSentToAdmin = true,
                    alertSentToPartner = true,
                    timestamp = now,
                    note = reason.trim().ifBlank {
                        "Unreported offline sale of $safeQty x ${product.title}: ODIYA exercised MOU right to collect ${String.format(java.util.Locale.US, "%.1f", pct)}% unit cost (${naira.format(accruedHostingFee)})."
                    }
                )
            )
            pushPartnerLedgerAsync(preferLocal = true)
            onComplete(true, "⚖️ Collected accrued ${String.format(java.util.Locale.US, "%.1f", pct)}% fee (${naira.format(accruedHostingFee)}) for unreported offline sale of $safeQty x ${product.title}.")
        }
    }

    // =========================================================================
    // STANDARD COMMERCIAL FEATURES (#2–#10): ADDRESSES, ZONES, RETURNS, LOYALTY, TICKETS, OFFLINE QUEUE
    // =========================================================================

    // #4 Saved Delivery Addresses
    private fun loadSavedAddresses(): List<SavedAddress> {
        val raw = adminPrefs.getString("saved_addresses_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                SavedAddress(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    label = obj.optString("label", "Home"),
                    recipientName = obj.optString("recipientName", ""),
                    phoneNumber = obj.optString("phoneNumber", ""),
                    streetAddress = obj.optString("streetAddress", ""),
                    stateZone = obj.optString("stateZone", "Lagos Mainland"),
                    isDefault = obj.optBoolean("isDefault", false)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persistSavedAddresses(list: List<SavedAddress>) {
        val arr = JSONArray()
        list.forEach { a ->
            arr.put(JSONObject().apply {
                put("id", a.id)
                put("label", a.label)
                put("recipientName", a.recipientName)
                put("phoneNumber", a.phoneNumber)
                put("streetAddress", a.streetAddress)
                put("stateZone", a.stateZone)
                put("isDefault", a.isDefault)
            })
        }
        adminPrefs.edit().putString("saved_addresses_json", arr.toString()).apply()
    }

    private val _savedAddresses = MutableStateFlow(loadSavedAddresses())
    val savedAddresses: StateFlow<List<SavedAddress>> = _savedAddresses.asStateFlow()

    fun saveSavedAddress(address: SavedAddress) {
        val clean = address.copy(
            label = address.label.trim().ifBlank { "Home" },
            recipientName = address.recipientName.trim(),
            phoneNumber = address.phoneNumber.trim(),
            streetAddress = address.streetAddress.trim(),
            stateZone = address.stateZone.trim().ifBlank { "Lagos Mainland" }
        )
        val shouldBeDefault = clean.isDefault || _savedAddresses.value.isEmpty()
        val existingFiltered = _savedAddresses.value.filterNot { it.id == clean.id }.map {
            if (shouldBeDefault) it.copy(isDefault = false) else it
        }
        val updated = listOf(clean.copy(isDefault = shouldBeDefault)) + existingFiltered
        _savedAddresses.value = updated
        persistSavedAddresses(updated)
        if (shouldBeDefault) {
            saveCustomerDeliveryInfo(clean.recipientName, clean.phoneNumber, clean.streetAddress)
        }
    }

    fun deleteSavedAddress(addressId: String) {
        val remaining = _savedAddresses.value.filterNot { it.id == addressId }
        val fixed = if (remaining.isNotEmpty() && remaining.none { it.isDefault }) {
            remaining.mapIndexed { idx, a -> a.copy(isDefault = idx == 0) }
        } else remaining
        _savedAddresses.value = fixed
        persistSavedAddresses(fixed)
    }

    fun setDefaultSavedAddress(addressId: String) {
        val updated = _savedAddresses.value.map { it.copy(isDefault = it.id == addressId) }
        _savedAddresses.value = updated
        persistSavedAddresses(updated)
        updated.find { it.isDefault }?.let { def ->
            saveCustomerDeliveryInfo(def.recipientName, def.phoneNumber, def.streetAddress)
            selectDeliveryZone(DeliveryZoneHelper.findByName(def.stateZone))
        }
    }

    // #6 Delivery Zone Selector
    private val _selectedDeliveryZone = MutableStateFlow(
        DeliveryZoneHelper.findByName(adminPrefs.getString("selected_delivery_zone_id", "zone_lagos_mainland"))
    )
    val selectedDeliveryZone: StateFlow<DeliveryZoneOption> = _selectedDeliveryZone.asStateFlow()

    fun selectDeliveryZone(zone: DeliveryZoneOption) {
        _selectedDeliveryZone.value = zone
        adminPrefs.edit().putString("selected_delivery_zone_id", zone.id).apply()
    }

    // #9 Loyalty Points & Referral Program
    private fun loadLoyaltyAccount(): LoyaltyAccount {
        val raw = adminPrefs.getString("loyalty_account_json", null)
        if (raw.isNullOrBlank()) {
            val fresh = LoyaltyAccount()
            persistLoyaltyAccount(fresh)
            return fresh
        }
        return try {
            val obj = JSONObject(raw)
            LoyaltyAccount(
                pointsBalance = obj.optInt("pointsBalance", 0).coerceAtLeast(0),
                lifetimeEarnedPoints = obj.optInt("lifetimeEarnedPoints", 0).coerceAtLeast(0),
                referralCode = obj.optString("referralCode", "ODY-${UUID.randomUUID().toString().take(4).uppercase()}"),
                referredByCode = obj.optString("referredByCode", ""),
                referralBonusClaimed = obj.optBoolean("referralBonusClaimed", false),
                totalOrdersRewarded = obj.optInt("totalOrdersRewarded", 0).coerceAtLeast(0)
            )
        } catch (_: Exception) {
            LoyaltyAccount()
        }
    }

    private fun persistLoyaltyAccount(account: LoyaltyAccount) {
        val obj = JSONObject().apply {
            put("pointsBalance", account.pointsBalance)
            put("lifetimeEarnedPoints", account.lifetimeEarnedPoints)
            put("referralCode", account.referralCode)
            put("referredByCode", account.referredByCode)
            put("referralBonusClaimed", account.referralBonusClaimed)
            put("totalOrdersRewarded", account.totalOrdersRewarded)
        }
        adminPrefs.edit().putString("loyalty_account_json", obj.toString()).apply()
    }

    private val _loyaltyAccount = MutableStateFlow(loadLoyaltyAccount())
    val loyaltyAccount: StateFlow<LoyaltyAccount> = _loyaltyAccount.asStateFlow()

    private fun updateLoyaltyAfterOrder(pointsRedeemed: Int, orderTotalAmount: Double) {
        val cur = _loyaltyAccount.value
        val earned = (orderTotalAmount / 1000.0).toInt().coerceAtLeast(1)
        val safeRedeemed = pointsRedeemed.coerceIn(0, cur.pointsBalance)
        val updated = cur.copy(
            pointsBalance = (cur.pointsBalance - safeRedeemed + earned).coerceAtLeast(0),
            lifetimeEarnedPoints = cur.lifetimeEarnedPoints + earned,
            totalOrdersRewarded = cur.totalOrdersRewarded + 1
        )
        _loyaltyAccount.value = updated
        persistLoyaltyAccount(updated)
    }

    fun applyReferralCode(rawCode: String): Pair<Boolean, String> {
        val clean = rawCode.trim().uppercase()
        val cur = _loyaltyAccount.value
        if (clean.length < 5) return false to "Enter a valid referral code (e.g., ODY-XXXX)."
        if (clean.equals(cur.referralCode, ignoreCase = true)) {
            return false to "You cannot apply your own referral code."
        }
        if (cur.referralBonusClaimed) {
            return false to "Welcome referral bonus (${cur.referredByCode}) already claimed on this device."
        }
        val bonusPts = 100 // 100 pts = ₦5,000 loyalty value!
        val updated = cur.copy(
            pointsBalance = cur.pointsBalance + bonusPts,
            lifetimeEarnedPoints = cur.lifetimeEarnedPoints + bonusPts,
            referredByCode = clean,
            referralBonusClaimed = true
        )
        _loyaltyAccount.value = updated
        persistLoyaltyAccount(updated)
        return true to "Referral code $clean applied! +$bonusPts Loyalty Points (₦5,000 Checkout Credit) added."
    }

    // #4 Order Cancellation & Return/Refund Requests
    private fun loadReturnRequests(): List<ReturnRefundRequest> {
        val raw = adminPrefs.getString("return_requests_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                ReturnRefundRequest(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    orderId = obj.optString("orderId", ""),
                    customerId = obj.optString("customerId", ""),
                    customerName = obj.optString("customerName", "Customer"),
                    customerPhone = obj.optString("customerPhone", ""),
                    reason = obj.optString("reason", ""),
                    details = obj.optString("details", ""),
                    proofImageUrl = obj.optString("proofImageUrl", ""),
                    refundAmount = obj.optDouble("refundAmount", 0.0),
                    status = try {
                        ReturnRefundStatus.valueOf(obj.optString("status", ReturnRefundStatus.PENDING_REVIEW.name))
                    } catch (_: Exception) { ReturnRefundStatus.PENDING_REVIEW },
                    adminNote = obj.optString("adminNote", ""),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persistReturnRequests(list: List<ReturnRefundRequest>) {
        val arr = JSONArray()
        list.forEach { r ->
            arr.put(JSONObject().apply {
                put("id", r.id)
                put("orderId", r.orderId)
                put("customerId", r.customerId)
                put("customerName", r.customerName)
                put("customerPhone", r.customerPhone)
                put("reason", r.reason)
                put("details", r.details)
                put("proofImageUrl", r.proofImageUrl)
                put("refundAmount", r.refundAmount)
                put("status", r.status.name)
                put("adminNote", r.adminNote)
                put("createdAt", r.createdAt)
            })
        }
        adminPrefs.edit().putString("return_requests_json", arr.toString()).apply()
    }

    private val _returnRequests = MutableStateFlow(loadReturnRequests())
    val returnRequests: StateFlow<List<ReturnRefundRequest>> = _returnRequests.asStateFlow()

    fun cancelCustomerOrder(
        orderId: String,
        reason: String = "Cancelled by customer prior to dispatch",
        onComplete: (Boolean, String) -> Unit
    ) {
        val order = allOrders.value.find { it.orderId == orderId }
        if (order == null) {
            onComplete(false, "Order not found.")
            return
        }
        if (order.status == OrderStatus.OUT_FOR_DELIVERY || order.status == OrderStatus.DELIVERED || order.status == OrderStatus.CANCELLED) {
            onComplete(false, "Orders that are ${order.status.label} cannot be cancelled directly. Use Request Return/Refund instead.")
            return
        }
        viewModelScope.launch {
            val note = reason.trim().ifBlank { "Cancelled by customer prior to dispatch" }
            val res = repository.updateOrderStatus(orderId, OrderStatus.CANCELLED, note)
            if (res.isSuccess) {
                if (!isOnline.value) {
                    enqueueOfflineAction(
                        type = OfflineActionType.CLOUD_INVENTORY_SYNC,
                        referenceId = orderId,
                        summary = "Sync Order Cancellation ($orderId)"
                    )
                }
                onComplete(true, "Order $orderId cancelled and inventory restored.")
            } else {
                onComplete(false, res.exceptionOrNull()?.message ?: "Unable to cancel order.")
            }
        }
    }

    fun submitReturnRefundRequest(
        orderId: String,
        reason: String,
        details: String,
        proofImageUri: String = "",
        onComplete: (Boolean, String) -> Unit
    ) {
        val order = allOrders.value.find { it.orderId == orderId }
        if (order == null) {
            onComplete(false, "Order not found.")
            return
        }
        viewModelScope.launch {
            val reqId = "RET-${UUID.randomUUID().toString().take(6).uppercase()}"
            val persistedProof = if (proofImageUri.isNotBlank()) {
                repository.persistReturnProofImage(reqId, proofImageUri)
            } else ""
            val req = ReturnRefundRequest(
                id = reqId,
                orderId = order.orderId,
                customerId = order.customerId,
                customerName = order.customerName,
                customerPhone = order.customerPhone,
                reason = reason.trim().ifBlank { "Product Issue" },
                details = details.trim(),
                proofImageUrl = persistedProof,
                refundAmount = order.totalAmount,
                status = ReturnRefundStatus.PENDING_REVIEW,
                createdAt = System.currentTimeMillis()
            )
            val updated = listOf(req) + _returnRequests.value
            _returnRequests.value = updated
            persistReturnRequests(updated)
            if (!isOnline.value) {
                enqueueOfflineAction(
                    type = OfflineActionType.RETURN_REQUEST_SYNC,
                    referenceId = req.id,
                    summary = "Return Request ${req.id} for ${req.orderId}"
                )
            }
            voiceManager.speakText("New Return and Refund request submitted for order ${order.orderId}.")
            onComplete(true, "Return/Refund request ${req.id} submitted! Admin will review it shortly.")
        }
    }

    fun updateReturnRefundStatus(
        requestId: String,
        status: ReturnRefundStatus,
        adminNote: String
    ) {
        val target = _returnRequests.value.find { it.id == requestId } ?: return
        val updated = _returnRequests.value.map { r ->
            if (r.id == requestId) r.copy(status = status, adminNote = adminNote.trim()) else r
        }
        _returnRequests.value = updated
        persistReturnRequests(updated)
        if (status == ReturnRefundStatus.REFUNDED) {
            viewModelScope.launch {
                repository.updateOrderStatus(
                    target.orderId,
                    OrderStatus.CANCELLED,
                    "Refunded (${target.id}): ${adminNote.ifBlank { target.reason }}"
                )
            }
        }
        ShopNotificationHelper.notifyReturnRefundUpdate(
            context = getApplication(),
            requestId = target.id,
            orderId = target.orderId,
            statusLabel = status.label,
            adminNote = adminNote.trim()
        )
    }

    // #8 Customer Support Tickets
    private fun loadSupportTickets(): List<SupportTicket> {
        val raw = adminPrefs.getString("support_tickets_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                SupportTicket(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    customerId = obj.optString("customerId", ""),
                    customerName = obj.optString("customerName", "Customer"),
                    customerPhone = obj.optString("customerPhone", ""),
                    orderId = obj.optString("orderId", ""),
                    subject = obj.optString("subject", ""),
                    message = obj.optString("message", ""),
                    status = try {
                        SupportTicketStatus.valueOf(obj.optString("status", SupportTicketStatus.OPEN.name))
                    } catch (_: Exception) { SupportTicketStatus.OPEN },
                    adminReply = obj.optString("adminReply", ""),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persistSupportTickets(list: List<SupportTicket>) {
        val arr = JSONArray()
        list.forEach { t ->
            arr.put(JSONObject().apply {
                put("id", t.id)
                put("customerId", t.customerId)
                put("customerName", t.customerName)
                put("customerPhone", t.customerPhone)
                put("orderId", t.orderId)
                put("subject", t.subject)
                put("message", t.message)
                put("status", t.status.name)
                put("adminReply", t.adminReply)
                put("createdAt", t.createdAt)
            })
        }
        adminPrefs.edit().putString("support_tickets_json", arr.toString()).apply()
    }

    private val _supportTickets = MutableStateFlow(loadSupportTickets())
    val supportTickets: StateFlow<List<SupportTicket>> = _supportTickets.asStateFlow()

    fun submitSupportTicket(
        subject: String,
        message: String,
        orderId: String = "",
        onComplete: (Boolean, String) -> Unit
    ) {
        if (subject.isBlank() || message.isBlank()) {
            onComplete(false, "Please enter a subject and description for your ticket.")
            return
        }
        val cust = _currentCustomer.value
        val savedInfo = _savedDeliveryInfo.value
        val ticket = SupportTicket(
            customerId = cust?.id ?: "guest",
            customerName = cust?.fullName?.ifBlank { null } ?: savedInfo.fullName.ifBlank { "Valued Shopper" },
            customerPhone = cust?.phoneNumber?.ifBlank { null } ?: savedInfo.phoneNumber.ifBlank { "" },
            orderId = orderId.trim(),
            subject = subject.trim(),
            message = message.trim(),
            status = SupportTicketStatus.OPEN,
            createdAt = System.currentTimeMillis()
        )
        val updated = listOf(ticket) + _supportTickets.value
        _supportTickets.value = updated
        persistSupportTickets(updated)
        if (!isOnline.value) {
            enqueueOfflineAction(
                type = OfflineActionType.SUPPORT_TICKET_SYNC,
                referenceId = ticket.id,
                summary = "Support Ticket: ${ticket.subject}"
            )
        }
        voiceManager.speakText("Support ticket ${ticket.id} received.")
        onComplete(true, "Support ticket ${ticket.id} created! Our team will respond shortly.")
    }

    fun replySupportTicket(
        ticketId: String,
        reply: String,
        status: SupportTicketStatus
    ) {
        val target = _supportTickets.value.find { it.id == ticketId } ?: return
        val updated = _supportTickets.value.map { t ->
            if (t.id == ticketId) t.copy(adminReply = reply.trim(), status = status) else t
        }
        _supportTickets.value = updated
        persistSupportTickets(updated)
        ShopNotificationHelper.notifySupportTicketReply(
            context = getApplication(),
            ticketId = target.id,
            subject = target.subject,
            reply = reply.trim()
        )
    }

    // #10 Offline Action Queue & Auto-Flush on Reconnect
    private fun loadOfflineQueue(): List<OfflineQueuedAction> {
        val raw = adminPrefs.getString("offline_queue_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                OfflineQueuedAction(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    type = try {
                        OfflineActionType.valueOf(obj.optString("type", OfflineActionType.CLOUD_INVENTORY_SYNC.name))
                    } catch (_: Exception) { OfflineActionType.CLOUD_INVENTORY_SYNC },
                    referenceId = obj.optString("referenceId", ""),
                    summary = obj.optString("summary", ""),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    retryCount = obj.optInt("retryCount", 0)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persistOfflineQueue(list: List<OfflineQueuedAction>) {
        val arr = JSONArray()
        list.forEach { q ->
            arr.put(JSONObject().apply {
                put("id", q.id)
                put("type", q.type.name)
                put("referenceId", q.referenceId)
                put("summary", q.summary)
                put("createdAt", q.createdAt)
                put("retryCount", q.retryCount)
            })
        }
        adminPrefs.edit().putString("offline_queue_json", arr.toString()).apply()
    }

    fun enqueueOfflineAction(
        type: OfflineActionType,
        referenceId: String,
        summary: String
    ) {
        val exists = _offlineQueue.value.any { it.type == type && it.referenceId == referenceId }
        if (exists) return
        val action = OfflineQueuedAction(type = type, referenceId = referenceId, summary = summary)
        val updated = _offlineQueue.value + action
        _offlineQueue.value = updated
        persistOfflineQueue(updated)
        CatalogBackgroundSyncJobService.scheduleReconnectFlush(getApplication())
    }

    suspend fun flushOfflineQueue() {
        val queued = _offlineQueue.value
        if (queued.isEmpty() || !isOnline.value) return
        val preOrderIds = queued.filter { it.type == OfflineActionType.PREORDER_SYNC }.map { it.referenceId }.toSet()
        var preOrderRes = Result.success(_preOrderRequests.value)
        val res = if (!repository.supabaseSync.isPlaceholderConfig()) {
            val baseRes = repository.fetchAndSyncAllFromCloudDetailed()
            preOrderRes = syncPreOrdersWithCloudInternal(
                preferLocalIds = preOrderIds,
                notifyOnNewRemote = false
            )
            baseRes
        } else {
            Result.success(com.example.data.repository.CloudSyncSummary())
        }
        // Push delayed payment transfers once network restores so ODIYA gets credited automatically
        val creditActions = queued.filter { it.type == OfflineActionType.PAYMENT_CREDIT_SYNC }
        creditActions.forEach { act ->
            val ord = allOrders.value.find { it.orderId == act.referenceId }
            setOrderCreditState(
                orderId = act.referenceId,
                confirmed = true,
                status = CreditVerificationStatus.CONFIRMED_CREDITED
            )
            val recoveryNote = "ORDER & PAYMENT CONFIRMED — Network Restored, Transfer Pushed & ODIYA Account Credited"
            if (ord != null) {
                repository.updateOrderStatus(ord.orderId, ord.status, recoveryNote)
                ShopNotificationHelper.notifyPaymentConfirmedReceipt(
                    context = getApplication(),
                    orderId = ord.orderId,
                    portalTitle = ord.paymentMethod.title,
                    amountText = "₦${String.format(java.util.Locale.US, "%,.0f", ord.totalAmount)}"
                )
            }
            startLiveCallSignalingIfConfigured()
            liveCallClient.sendSignal(
                "PAYMENT_CREDIT_ALERT",
                "global_orders",
                JSONObject().apply {
                    put("orderId", act.referenceId)
                    put("ref", "Auto-Recovered After Network Delay")
                }
            )
        }
        if (creditActions.isNotEmpty()) {
            voiceManager.speakText("Network restored. ${creditActions.size} delayed payment transfer confirmed and credited to ODIYA.")
        }

        // Dispatch queued new product alerts once online
        val productAlerts = queued.filter { it.type == OfflineActionType.NEW_PRODUCT_ALERT }
        productAlerts.forEach { act ->
            allProducts.value.find { it.id == act.referenceId }?.let { prod ->
                liveCallClient.sendSignal(
                    "NEW_PRODUCT",
                    "global_products",
                    JSONObject().apply {
                        put("productId", prod.id)
                        put("title", prod.title)
                        put("category", prod.category)
                        put("price", prod.basePrice)
                    }
                )
            }
        }

        val hasOnlyPreOrders = preOrderIds.isNotEmpty() && creditActions.isEmpty()
        val preOrdersStillPending = preOrderIds.isNotEmpty() && (repository.supabaseSync.isPlaceholderConfig() || preOrderRes.isFailure)
        if ((res.isSuccess && !preOrdersStillPending) || creditActions.isNotEmpty()) {
            val remainingAfterFlush = if (preOrdersStillPending) {
                queued.filter { it.type == OfflineActionType.PREORDER_SYNC }.map { it.copy(retryCount = it.retryCount + 1) }
            } else emptyList()
            _offlineQueue.value = remainingAfterFlush
            persistOfflineQueue(remainingAfterFlush)
            if (!hasOnlyPreOrders || !preOrdersStillPending) {
                ShopNotificationHelper.notifyOfflineQueueSynced(getApplication(), queued.size - remainingAfterFlush.size)
                _cloudStatusMessage.value = "✅ Reconnected! Synced ${queued.size - remainingAfterFlush.size} queued offline update(s) & confirmed pending transfers."
            }
        } else {
            val bumped = queued.map { it.copy(retryCount = it.retryCount + 1) }
            _offlineQueue.value = bumped
            persistOfflineQueue(bumped)
        }
    }

    fun triggerOfflineQueueSync(onComplete: (Int) -> Unit = {}) {
        val pendingCount = _offlineQueue.value.size
        viewModelScope.launch {
            flushOfflineQueue()
            val remaining = _offlineQueue.value.size
            val syncedCount = (pendingCount - remaining).coerceAtLeast(0)
            onComplete(syncedCount)
        }
    }

    // =========================================================================
    // #11 LIVE AUDIO & VIDEO INTERACTIVE CALL HUB (SWITCHABLE AUDIO <-> VIDEO)
    // =========================================================================
    private val deviceCallSenderId: String = adminPrefs.getString("device_call_sender_id", null)
        ?: ("dev_" + UUID.randomUUID().toString().take(8)).also { id ->
            adminPrefs.edit().putString("device_call_sender_id", id).apply()
        }

    private val liveCallClient = LiveCallSignalingClient(
        scope = viewModelScope,
        cleanUrlFn = { raw -> repository.supabaseSync.cleanUrl(raw) }
    )

    private val _liveCallState = MutableStateFlow(LiveCallUiState())
    val liveCallState: StateFlow<LiveCallUiState> = _liveCallState.asStateFlow()

    private val _inboundWebRtcSignal = MutableStateFlow<String?>(null)
    val inboundWebRtcSignal: StateFlow<String?> = _inboundWebRtcSignal.asStateFlow()

    private var liveCallObserverStarted = false
    private var liveCallTimerJob: kotlinx.coroutines.Job? = null
    private var liveLobbyPollJob: kotlinx.coroutines.Job? = null
    private var lastAlertedRoomId: String = ""

    private fun startLiveCallSignalingIfConfigured() {
        val cfg = repository.supabaseSync.getConfig()
        if (cfg.url.isBlank() || cfg.anonKey.isBlank() || repository.supabaseSync.isPlaceholderConfig(cfg.url, cfg.anonKey)) {
            return
        }
        liveCallClient.connect(cfg.url, cfg.anonKey, deviceCallSenderId)
        if (!liveCallObserverStarted) {
            liveCallObserverStarted = true
            viewModelScope.launch {
                liveCallClient.isConnected.collect { connected ->
                    _liveCallState.value = _liveCallState.value.copy(isRealtimeConnected = connected)
                }
            }
            viewModelScope.launch {
                liveCallClient.events.collect { ev ->
                    handleIncomingLiveCallSignal(ev)
                }
            }
            liveLobbyPollJob?.cancel()
            liveLobbyPollJob = viewModelScope.launch {
                while (isActive) {
                    val curCfg = repository.supabaseSync.getConfig()
                    if (curCfg.url.isNotBlank() && curCfg.anonKey.isNotBlank() &&
                        !repository.supabaseSync.isPlaceholderConfig(curCfg.url, curCfg.anonKey)
                    ) {
                        val isAdminDevice = _isAdminUnlocked.value || (_currentCustomer.value?.isAdmin == true)
                        val snap = liveCallClient.fetchLobbySnapshot(curCfg.url, curCfg.anonKey)
                        if (snap != null) {
                            val activeReqs = snap.requests
                            _liveCallState.value = _liveCallState.value.copy(
                                adminOnline = snap.adminOnline || isAdminDevice,
                                incomingRequests = activeReqs
                            )
                            if (isAdminDevice && activeReqs.isNotEmpty() && _liveCallState.value.phase == LiveCallPhase.IDLE) {
                                val top = activeReqs.first()
                                if (top.roomId != lastAlertedRoomId) {
                                    lastAlertedRoomId = top.roomId
                                    ShopNotificationHelper.notifyIncomingLiveCall(
                                        context = getApplication(),
                                        callerName = top.callerName,
                                        isVideoCall = top.mode == LiveCallMode.VIDEO
                                    )
                                    voiceManager.speakText("Incoming ${top.mode.label} from ${top.callerName}")
                                }
                            }
                        }
                        if (isAdminDevice && _liveCallState.value.adminOnline) {
                            liveCallClient.publishLobbySnapshot(
                                supabaseUrl = curCfg.url,
                                anonKey = curCfg.anonKey,
                                adminOnline = true,
                                requests = _liveCallState.value.incomingRequests
                            )
                        }
                    }
                    delay(5_000L)
                }
            }
        }
    }

    private fun handleIncomingLiveCallSignal(ev: com.example.data.remote.LiveCallSignalEvent) {
        val state = _liveCallState.value
        val isAdminDevice = _isAdminUnlocked.value || (_currentCustomer.value?.isAdmin == true)
        when (ev.type) {
            "RING" -> {
                val callerName = ev.payload.optString("callerName", "Customer")
                val callerEmail = ev.payload.optString("callerEmail", "")
                val callerPhone = ev.payload.optString("callerPhone", "")
                val topic = ev.payload.optString("topic", "Live Support")
                val modeStr = ev.payload.optString("mode", "AUDIO")
                val mode = if (modeStr.equals("VIDEO", true)) LiveCallMode.VIDEO else LiveCallMode.AUDIO
                val req = LiveCallRequest(
                    roomId = ev.roomId,
                    callerName = callerName,
                    callerEmail = callerEmail,
                    callerPhone = callerPhone,
                    mode = mode,
                    topic = topic,
                    createdAt = System.currentTimeMillis()
                )
                val updatedList = (listOf(req) + state.incomingRequests.filterNot { it.roomId == ev.roomId }).take(10)
                _liveCallState.value = state.copy(incomingRequests = updatedList)
                if (isAdminDevice && ev.roomId != lastAlertedRoomId) {
                    lastAlertedRoomId = ev.roomId
                    ShopNotificationHelper.notifyIncomingLiveCall(
                        context = getApplication(),
                        callerName = callerName,
                        isVideoCall = mode == LiveCallMode.VIDEO
                    )
                    voiceManager.speakText("Incoming ${mode.label} from $callerName")
                }
            }
            "ACCEPT" -> {
                if (ev.roomId == state.roomId && (state.phase == LiveCallPhase.RINGING_OUTGOING || state.phase == LiveCallPhase.CONNECTING)) {
                    val responder = ev.payload.optString("responderName", "Store Admin")
                    val modeStr = ev.payload.optString("mode", state.mode.name)
                    val mode = if (modeStr.equals("VIDEO", true)) LiveCallMode.VIDEO else LiveCallMode.AUDIO
                    _liveCallState.value = state.copy(
                        phase = LiveCallPhase.CONNECTING,
                        mode = mode,
                        peerName = responder,
                        statusBanner = "$responder answered — linking live ${mode.label.lowercase()}..."
                    )
                }
            }
            "DECLINE" -> {
                val updated = state.incomingRequests.filterNot { it.roomId == ev.roomId }
                if (ev.roomId == state.roomId) {
                    liveCallTimerJob?.cancel()
                    _liveCallState.value = state.copy(
                        phase = LiveCallPhase.ENDED,
                        incomingRequests = updated,
                        statusBanner = ev.payload.optString("reason", "Admin is currently busy — please try again or send WhatsApp.")
                    )
                } else {
                    _liveCallState.value = state.copy(incomingRequests = updated)
                }
            }
            "MODE_SWITCH" -> {
                if (ev.roomId == state.roomId) {
                    val modeStr = ev.payload.optString("mode", "AUDIO")
                    val switched = if (modeStr.equals("VIDEO", true)) LiveCallMode.VIDEO else LiveCallMode.AUDIO
                    val actor = ev.payload.optString("actorName", state.peerName)
                    _liveCallState.value = state.copy(
                        mode = switched,
                        statusBanner = "$actor switched call to ${switched.label}"
                    )
                }
            }
            "APP_UPDATE" -> {
                val remoteCode = ev.payload.optInt("versionCode", 1)
                val remoteName = ev.payload.optString("versionName", "1.0").trim().ifBlank { "1.0" }
                val notes = ev.payload.optString("releaseNotes", "").trim()
                val dlUrl = com.example.util.WebPortalExporter.normalizeApkOrPortalUrl(
                    ev.payload.optString("updateUrl", "")
                ).ifBlank {
                    com.example.util.WebPortalExporter.DEFAULT_GITHUB_APK_RELEASE_URL
                }
                val (installedCode, _) = getInstalledAppVersion()
                if (remoteCode > installedCode) {
                    _latestVersionName.value = remoteName
                    _releaseNotes.value = notes
                    _updateDownloadUrl.value = dlUrl
                    _updateAvailable.value = true
                    adminPrefs.edit().putInt("last_notified_update_code", remoteCode).apply()
                    ShopNotificationHelper.notifyAppUpdateAvailable(
                        context = getApplication(),
                        versionName = remoteName,
                        versionCode = remoteCode,
                        notes = notes
                    )
                }
            }
            "CHAT" -> {
                if (ev.roomId == state.roomId || state.roomId.isBlank()) {
                    val msg = LiveCallMessage(
                        id = UUID.randomUUID().toString().take(8),
                        senderName = ev.payload.optString("senderName", "Customer"),
                        isFromAdmin = ev.payload.optBoolean("isFromAdmin", false),
                        text = ev.payload.optString("text", ""),
                        attachedTag = ev.payload.optString("attachedTag", ""),
                        timestamp = System.currentTimeMillis()
                    )
                    if (msg.text.isNotBlank()) {
                        _liveCallState.value = state.copy(
                            chatMessages = (state.chatMessages + msg).takeLast(60)
                        )
                    }
                }
            }
            "WEBRTC" -> {
                if (ev.roomId == state.roomId) {
                    val rawSignal = ev.payload.optString("signal", "")
                    if (rawSignal.isNotBlank()) {
                        _inboundWebRtcSignal.value = rawSignal
                    }
                }
            }
            "PRE_ORDER" -> {
                val poId = ev.payload.optString("id", "PRE-ORDER")
                val poCust = ev.payload.optString("customerName", "Customer")
                val poProd = ev.payload.optString("productName", "Special Request")
                val poBrand = ev.payload.optString("brandName", "")
                ShopNotificationHelper.notifyAdminNewPreOrder(
                    context = getApplication(),
                    requestId = poId,
                    customerName = poCust,
                    productName = poProd,
                    brandName = poBrand
                )
                viewModelScope.launch {
                    syncPreOrdersWithCloudInternal(notifyOnNewRemote = true)
                }
            }
            "APP_MAINTENANCE" -> {
                val enabled = ev.payload.optBoolean("enabled", false)
                val msg = ev.payload.optString("message", "").trim()
                val isUpgrade = ev.payload.optBoolean("isUpgradeNotice", false)
                val title = if (enabled && isUpgrade) "🔧 App Maintenance & Upgrade Alert" else if (enabled) "🛠️ Store Maintenance Notice" else "✅ Maintenance Complete"
                updateStoreProfile(
                    _storeProfile.value.copy(
                        isMaintenanceMode = enabled,
                        maintenanceMessage = msg,
                        maintenanceStartedAt = if (enabled) ev.payload.optLong("timestamp", System.currentTimeMillis()) else 0L
                    )
                )
                ShopNotificationHelper.notifyAppMaintenance(
                    context = getApplication(),
                    title = title,
                    message = msg.ifBlank { "ODIYA system maintenance notice." }
                )
            }
            "NEW_PRODUCT" -> {
                val prodId = ev.payload.optString("productId", "")
                val title = ev.payload.optString("title", "New Product Arrival")
                val cat = ev.payload.optString("category", "Featured")
                val price = ev.payload.optDouble("price", 0.0)
                val myPhone = _currentCustomer.value?.phoneNumber?.ifBlank { null } ?: _savedDeliveryInfo.value.phoneNumber
                ShopNotificationHelper.notifyNewProductAlert(
                    context = getApplication(),
                    productTitle = title,
                    category = cat,
                    priceText = "₦${String.format(java.util.Locale.US, "%,.0f", price)}",
                    targetPhone = myPhone
                )
                viewModelScope.launch {
                    if (!repository.supabaseSync.isPlaceholderConfig()) {
                        repository.fetchAndSyncAllFromCloudDetailed()
                    }
                }
            }
            "PAYMENT_CREDIT_ALERT" -> {
                val ordId = ev.payload.optString("orderId", "")
                val ref = ev.payload.optString("ref", "ODIYA Credit Alert")
                if (ordId.isNotBlank()) {
                    recordOrderCreditAlertReceived(ordId, ref)
                }
            }
            "CUSTOMER_REVIEW" -> {
                val rId = ev.payload.optString("id", UUID.randomUUID().toString())
                val pId = ev.payload.optString("productId", "")
                val pTitle = ev.payload.optString("productTitle", "Product")
                val cId = ev.payload.optString("customerId", "")
                val cName = ev.payload.optString("customerName", "Customer")
                val rating = ev.payload.optInt("rating", 5).coerceIn(1, 5)
                val comment = ev.payload.optString("comment", "")
                if (comment.isNotBlank() && _productReviews.value.none { it.id == rId }) {
                    val rev = ProductReview(
                        id = rId,
                        productId = pId,
                        productTitle = pTitle,
                        customerId = cId,
                        customerName = cName,
                        rating = rating,
                        comment = comment,
                        createdAt = ev.payload.optLong("createdAt", System.currentTimeMillis()),
                        isVerifiedPurchase = true
                    )
                    val updated = listOf(rev) + _productReviews.value
                    _productReviews.value = updated
                    persistProductReviews(updated)
                }
                ShopNotificationHelper.notifyCustomerNewReviewToAdmin(
                    context = getApplication(),
                    reviewerName = cName,
                    productTitle = pTitle,
                    rating = rating
                )
            }
            "HANGUP" -> {
                val updated = state.incomingRequests.filterNot { it.roomId == ev.roomId }
                if (ev.roomId == state.roomId) {
                    liveCallTimerJob?.cancel()
                    _liveCallState.value = state.copy(
                        phase = LiveCallPhase.ENDED,
                        incomingRequests = updated,
                        elapsedSeconds = 0,
                        localAudioLevel = 0,
                        remoteAudioLevel = 0,
                        statusBanner = "Call ended."
                    )
                } else {
                    _liveCallState.value = state.copy(incomingRequests = updated)
                }
            }
        }
    }

    fun openLiveCallScreen() {
        startLiveCallSignalingIfConfigured()
        val isAdminDevice = _isAdminUnlocked.value || (_currentCustomer.value?.isAdmin == true)
        _liveCallState.value = _liveCallState.value.copy(
            isAdminRole = isAdminDevice,
            adminOnline = if (isAdminDevice) true else _liveCallState.value.adminOnline
        )
        navigateTo(AppScreen.LIVE_CALL)
    }

    fun selectLiveCallPreMode(mode: LiveCallMode) {
        _liveCallState.value = _liveCallState.value.copy(mode = mode)
    }

    fun startCustomerLiveCall(mode: LiveCallMode, topic: String) {
        startLiveCallSignalingIfConfigured()
        val customer = _currentCustomer.value
        val callerName = customer?.fullName?.takeIf { it.isNotBlank() }
            ?: _savedDeliveryInfo.value.fullName.takeIf { it.isNotBlank() }
            ?: "Odiya Customer"
        val callerEmail = customer?.email.orEmpty()
        val callerPhone = customer?.phoneNumber?.takeIf { it.isNotBlank() }
            ?: _savedDeliveryInfo.value.phoneNumber
        val roomId = "call_" + UUID.randomUUID().toString().take(8)
        val cleanTopic = topic.ifBlank { "Live Store Inquiry" }

        val req = LiveCallRequest(
            roomId = roomId,
            callerName = callerName,
            callerEmail = callerEmail,
            callerPhone = callerPhone,
            mode = mode,
            topic = cleanTopic,
            createdAt = System.currentTimeMillis()
        )
        val updatedReqs = (listOf(req) + _liveCallState.value.incomingRequests.filterNot { it.roomId == roomId }).take(10)

        _liveCallState.value = _liveCallState.value.copy(
            phase = LiveCallPhase.RINGING_OUTGOING,
            mode = mode,
            roomId = roomId,
            peerName = "${_storeProfile.value.storeName} Admin",
            topic = cleanTopic,
            isAdminRole = false,
            isCallerInitiator = true,
            isMicMuted = false,
            elapsedSeconds = 0,
            incomingRequests = updatedReqs,
            chatMessages = emptyList(),
            statusBanner = "Ringing Admin Desk in ${mode.label} mode..."
        )

        val payload = JSONObject().apply {
            put("callerName", callerName)
            put("callerEmail", callerEmail)
            put("callerPhone", callerPhone)
            put("mode", mode.name)
            put("topic", cleanTopic)
        }
        liveCallClient.sendSignal("RING", roomId, payload)

        val cfg = repository.supabaseSync.getConfig()
        viewModelScope.launch {
            liveCallClient.publishLobbySnapshot(cfg.url, cfg.anonKey, _liveCallState.value.adminOnline, updatedReqs)
        }

        // Activate full interactive Audio & Visual Desk right away so microphone/camera/concierge work seamlessly even solo or waiting on peer
        startLiveCallTimer()
        viewModelScope.launch {
            delay(1800L)
            val cur = _liveCallState.value
            if (cur.roomId == roomId && cur.phase == LiveCallPhase.RINGING_OUTGOING) {
                val greetingMsg = LiveCallMessage(
                    id = UUID.randomUUID().toString().take(8),
                    senderName = "${_storeProfile.value.storeName} Live Desk",
                    isFromAdmin = true,
                    text = "Welcome to ${_storeProfile.value.storeName} Interactive ${mode.label} Studio! Topic: $cleanTopic. Your live audio/video is active.",
                    timestamp = System.currentTimeMillis()
                )
                _liveCallState.value = cur.copy(
                    phase = LiveCallPhase.CONNECTED,
                    statusBanner = "Live ${mode.label} Interactive Desk Active",
                    chatMessages = (cur.chatMessages + greetingMsg).takeLast(60)
                )
                voiceManager.speakText("Welcome to ${_storeProfile.value.storeName} Live ${mode.label} Desk. Topic: $cleanTopic.")
            }
        }
    }

    fun sendInteractiveAudioVisualPrompt(prompt: String) {
        val clean = prompt.trim()
        if (clean.isBlank()) return
        val cur = _liveCallState.value
        sendLiveCallChatMessage(clean)

        val profile = _storeProfile.value
        val latestOrder = allOrders.value.firstOrNull()
        val answerText = when {
            clean.contains("order", ignoreCase = true) || clean.contains("payment", ignoreCase = true) -> {
                if (latestOrder != null) {
                    "Order ${latestOrder.orderId} is ${latestOrder.status.label}. Credit status: ${latestOrder.creditVerificationStatus.label}."
                } else {
                    "You have no recent orders yet. Use Paystack, OPay, Moniepoint, or PalmPay direct portal for instant ODIYA credit verification."
                }
            }
            clean.contains("bank", ignoreCase = true) || clean.contains("account", ignoreCase = true) || clean.contains("pay", ignoreCase = true) -> {
                buildString {
                    append("${profile.storeName} official accounts: ")
                    if (profile.opayAccountNumber.isNotBlank()) append("OPay ${profile.opayAccountNumber}. ")
                    if (profile.moniepointAccountNumber.isNotBlank()) append("Moniepoint ${profile.moniepointAccountNumber}. ")
                    if (profile.palmpayAccountNumber.isNotBlank()) append("PalmPay ${profile.palmpayAccountNumber}.")
                }
            }
            clean.contains("pre-order", ignoreCase = true) || clean.contains("preorder", ignoreCase = true) -> {
                "Open the Pre-Order form from your account or product details to submit custom items directly to Admin with instant notification."
            }
            else -> {
                "${profile.storeName} has ${allProducts.value.size} active catalog items across ${com.example.data.model.ProductCategory.entries.size} categories ready for dispatch!"
            }
        }

        val botReply = LiveCallMessage(
            id = UUID.randomUUID().toString().take(8),
            senderName = "${profile.storeName} Interactive Desk",
            isFromAdmin = true,
            text = answerText,
            timestamp = System.currentTimeMillis()
        )
        _liveCallState.value = _liveCallState.value.copy(
            chatMessages = (_liveCallState.value.chatMessages + botReply).takeLast(60),
            statusBanner = "Interactive Audio Reply Active"
        )
        voiceManager.speakText(answerText)
    }

    fun answerIncomingLiveCall(request: LiveCallRequest, answerMode: LiveCallMode = request.mode) {
        startLiveCallSignalingIfConfigured()
        val adminName = _storeProfile.value.storeName.ifBlank { "ODIYA" } + " Admin"
        val remaining = _liveCallState.value.incomingRequests.filterNot { it.roomId == request.roomId }
        _liveCallState.value = _liveCallState.value.copy(
            phase = LiveCallPhase.CONNECTING,
            mode = answerMode,
            roomId = request.roomId,
            peerName = request.callerName,
            topic = request.topic,
            isAdminRole = true,
            isCallerInitiator = false,
            isMicMuted = false,
            elapsedSeconds = 0,
            adminOnline = true,
            incomingRequests = remaining,
            chatMessages = emptyList(),
            statusBanner = "Connecting live ${answerMode.label.lowercase()} with ${request.callerName}..."
        )
        navigateTo(AppScreen.LIVE_CALL)
        liveCallClient.sendSignal(
            "ACCEPT",
            request.roomId,
            JSONObject().apply {
                put("responderName", adminName)
                put("mode", answerMode.name)
            }
        )
        val cfg = repository.supabaseSync.getConfig()
        viewModelScope.launch {
            liveCallClient.publishLobbySnapshot(cfg.url, cfg.anonKey, true, remaining)
        }
    }

    fun declineIncomingLiveCall(request: LiveCallRequest) {
        val remaining = _liveCallState.value.incomingRequests.filterNot { it.roomId == request.roomId }
        _liveCallState.value = _liveCallState.value.copy(incomingRequests = remaining)
        liveCallClient.sendSignal(
            "DECLINE",
            request.roomId,
            JSONObject().apply { put("reason", "Admin declined or is assisting another customer.") }
        )
        val cfg = repository.supabaseSync.getConfig()
        viewModelScope.launch {
            liveCallClient.publishLobbySnapshot(cfg.url, cfg.anonKey, _liveCallState.value.adminOnline, remaining)
        }
    }

    fun switchLiveCallMode(newMode: LiveCallMode) {
        val state = _liveCallState.value
        if (state.mode == newMode) return
        val actor = if (state.isAdminRole) "Admin" else (_currentCustomer.value?.fullName ?: "Customer")
        _liveCallState.value = state.copy(
            mode = newMode,
            statusBanner = if (state.phase == LiveCallPhase.CONNECTED || state.phase == LiveCallPhase.CONNECTING) {
                "Switched live to ${newMode.label}"
            } else {
                ""
            }
        )
        if (state.roomId.isNotBlank() && state.phase != LiveCallPhase.IDLE && state.phase != LiveCallPhase.ENDED) {
            liveCallClient.sendSignal(
                "MODE_SWITCH",
                state.roomId,
                JSONObject().apply {
                    put("mode", newMode.name)
                    put("actorName", actor)
                }
            )
        }
    }

    fun toggleLiveCallMicMute() {
        val cur = _liveCallState.value
        _liveCallState.value = cur.copy(isMicMuted = !cur.isMicMuted)
    }

    fun toggleLiveCallSpeaker() {
        val cur = _liveCallState.value
        _liveCallState.value = cur.copy(isSpeakerOn = !cur.isSpeakerOn)
    }

    fun flipLiveCallCamera() {
        val cur = _liveCallState.value
        _liveCallState.value = cur.copy(isUsingFrontCamera = !cur.isUsingFrontCamera)
    }

    fun setAdminCallAvailability(online: Boolean) {
        startLiveCallSignalingIfConfigured()
        _liveCallState.value = _liveCallState.value.copy(adminOnline = online)
        val cfg = repository.supabaseSync.getConfig()
        viewModelScope.launch {
            liveCallClient.publishLobbySnapshot(cfg.url, cfg.anonKey, online, _liveCallState.value.incomingRequests)
        }
    }

    fun sendLiveCallChatMessage(text: String, attachedTag: String = "") {
        val clean = text.trim()
        if (clean.isBlank() && attachedTag.isBlank()) return
        val state = _liveCallState.value
        val sender = if (state.isAdminRole) {
            "Admin Desk"
        } else {
            _currentCustomer.value?.fullName?.ifBlank { "Customer" } ?: "Customer"
        }
        val msg = LiveCallMessage(
            id = UUID.randomUUID().toString().take(8),
            senderName = sender,
            isFromAdmin = state.isAdminRole,
            text = clean.ifBlank { "Shared $attachedTag" },
            attachedTag = attachedTag,
            timestamp = System.currentTimeMillis()
        )
        _liveCallState.value = state.copy(chatMessages = (state.chatMessages + msg).takeLast(60))
        if (state.roomId.isNotBlank()) {
            liveCallClient.sendSignal(
                "CHAT",
                state.roomId,
                JSONObject().apply {
                    put("senderName", sender)
                    put("isFromAdmin", state.isAdminRole)
                    put("text", msg.text)
                    put("attachedTag", attachedTag)
                }
            )
        }
    }

    fun sendWebRtcOutboundSignal(signalJson: String) {
        val roomId = _liveCallState.value.roomId
        if (roomId.isBlank()) return
        liveCallClient.sendSignal(
            "WEBRTC",
            roomId,
            JSONObject().apply { put("signal", signalJson) }
        )
    }

    fun onWebRtcConnectionState(rawState: String) {
        val cur = _liveCallState.value
        when (rawState.lowercase()) {
            "connected" -> {
                if (cur.phase != LiveCallPhase.CONNECTED) {
                    _liveCallState.value = cur.copy(
                        phase = LiveCallPhase.CONNECTED,
                        statusBanner = "Live ${cur.mode.label} connected"
                    )
                    startLiveCallTimer()
                }
            }
            "reconnecting", "disconnected" -> {
                if (cur.phase == LiveCallPhase.CONNECTED) {
                    _liveCallState.value = cur.copy(
                        phase = LiveCallPhase.RECONNECTING,
                        statusBanner = "Network unstable — reconnecting live stream..."
                    )
                }
            }
            "failed" -> {
                _liveCallState.value = cur.copy(
                    phase = LiveCallPhase.RECONNECTING,
                    statusBanner = "Re-negotiating peer route..."
                )
            }
        }
    }

    private fun startLiveCallTimer() {
        liveCallTimerJob?.cancel()
        liveCallTimerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                val cur = _liveCallState.value
                if (cur.phase == LiveCallPhase.CONNECTED || cur.phase == LiveCallPhase.RECONNECTING) {
                    _liveCallState.value = cur.copy(elapsedSeconds = cur.elapsedSeconds + 1)
                } else {
                    break
                }
            }
        }
    }

    fun onWebRtcAudioLevels(local: Int, remote: Int) {
        val cur = _liveCallState.value
        _liveCallState.value = cur.copy(localAudioLevel = local, remoteAudioLevel = remote)
    }

    fun onWebRtcMediaError(msg: String) {
        _liveCallState.value = _liveCallState.value.copy(statusBanner = msg)
    }

    fun consumeInboundWebRtcSignal() {
        _inboundWebRtcSignal.value = null
    }

    fun resetLiveCallToIdle() {
        liveCallTimerJob?.cancel()
        val cur = _liveCallState.value
        _liveCallState.value = cur.copy(
            phase = LiveCallPhase.IDLE,
            roomId = "",
            elapsedSeconds = 0,
            localAudioLevel = 0,
            remoteAudioLevel = 0,
            statusBanner = ""
        )
    }

    fun endLiveCall() {
        liveCallTimerJob?.cancel()
        val cur = _liveCallState.value
        if (cur.roomId.isNotBlank()) {
            liveCallClient.sendSignal("HANGUP", cur.roomId)
            val remaining = cur.incomingRequests.filterNot { it.roomId == cur.roomId }
            val cfg = repository.supabaseSync.getConfig()
            viewModelScope.launch {
                liveCallClient.publishLobbySnapshot(cfg.url, cfg.anonKey, cur.adminOnline, remaining)
            }
        }
        _liveCallState.value = cur.copy(
            phase = LiveCallPhase.IDLE,
            roomId = "",
            elapsedSeconds = 0,
            localAudioLevel = 0,
            remoteAudioLevel = 0,
            incomingRequests = cur.incomingRequests.filterNot { it.roomId == cur.roomId },
            statusBanner = ""
        )
    }
}
