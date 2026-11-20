package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Email
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChipDefaults
import com.example.ui.components.QuickEditProductDialog
import com.example.ui.components.SizeSelectionBox
import com.example.util.WhatsAppHelper
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.AppScreen
import com.example.MainViewModel
import com.example.data.model.AdminBargainConfig
import com.example.data.model.Customer
import com.example.data.model.Order
import com.example.data.model.StatusUpdate
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.PreOrderRequest
import com.example.data.model.PreOrderStatus
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.ProductColor
import com.example.data.model.ReturnRefundRequest
import com.example.data.model.ReturnRefundStatus
import com.example.data.model.StoreProfileConfig
import com.example.data.model.SupportTicket
import com.example.data.model.SupportTicketStatus
import com.example.util.BiometricLockHelper
import com.example.util.ProductImageHelper
import com.example.ui.components.DualAxisScrollContainer
import com.example.ui.components.QrCodeView
import com.example.ui.components.ShareAppDialog
import com.example.ui.components.ShoeAudience
import com.example.ui.components.ShoeStandard
import com.example.ui.components.TickableColourPicker
import com.example.ui.components.ZoomableImageDialog
import com.example.ui.components.shareCleanApkFile
import com.example.ui.components.shareQrCodeImage
import com.example.ui.theme.AccentGold
import com.example.ui.theme.CodOrange
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.MoniepointBlue
import com.example.ui.theme.OPayGreen
import com.example.ui.theme.PosPurple
import com.example.ui.theme.SuccessGreen
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminControlRoomScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val initialAdminTab by viewModel.adminSelectedTab.collectAsState()
    var selectedTab by remember(initialAdminTab) { mutableIntStateOf(initialAdminTab) }
    // 0 = Products Form, 1 = Customer Identity, 2 = Payment Receipts, 4 = Supabase Cloud

    val isAdminUnlocked by viewModel.isAdminUnlocked.collectAsState()

    val products by viewModel.allProducts.collectAsState()
    val customers by viewModel.allCustomers.collectAsState()
    val orders by viewModel.allOrders.collectAsState()
    val preOrderRequests by viewModel.preOrderRequests.collectAsState()
    val partnerOrganizations by viewModel.partnerOrganizations.collectAsState()
    val returnRequests by viewModel.returnRequests.collectAsState()
    val supportTickets by viewModel.supportTickets.collectAsState()
    val cloudStatusMsg by viewModel.cloudStatusMessage.collectAsState()
    val isWideDualAxisMode by viewModel.isWideDualAxisMode.collectAsState()

    var selectedCustomerFile by remember { mutableStateOf<Customer?>(null) }

    BackHandler {
        viewModel.navigateBack()
    }

    if (!isAdminUnlocked) {
        AdminSecurityLockScreen(viewModel = viewModel)
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AccentGold.copy(alpha = 0.2f),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "ADMIN",
                                color = AccentGold,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text("Control Room", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("admin_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Previous Page")
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.navigateToHome() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("admin_enter_store_front_topbar_button")
                    ) {
                        Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ENTER STORE FRONT", fontWeight = FontWeight.Black, fontSize = 10.5.sp)
                    }
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.AUTH) },
                        modifier = Modifier.testTag("admin_account_shortcut_button")
                    ) {
                        Icon(Icons.Default.Person, contentDescription = "Admin Account & Customer Shortcuts", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(
                        onClick = { viewModel.lockAdmin() },
                        modifier = Modifier.testTag("admin_lock_button")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = "Lock Admin", tint = AccentGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Prominent Direct ENTER STORE FRONT Switch Banner (Compact size)
            Surface(
                color = CyberCyan.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .testTag("admin_storefront_shortcut_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("STORE FRONT & CONTACT", fontWeight = FontWeight.Black, fontSize = 11.sp, color = CyberCyan)
                            Text("Switch catalog or edit store contact", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    OutlinedButton(
                        onClick = { selectedTab = 8 },
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.7f)),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 3.dp),
                        modifier = Modifier.testTag("admin_banner_edit_store_info_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = AccentGold, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("INFO", fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = AccentGold)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Button(
                        onClick = { viewModel.navigateToHome() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp),
                        modifier = Modifier.testTag("admin_banner_enter_storefront_button")
                    ) {
                        Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("FRONT", fontWeight = FontWeight.Black, fontSize = 9.5.sp)
                    }
                }
            }

            // Tabs Bar
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0; viewModel.selectAdminTab(0) },
                    text = { Text("Product Form (${products.size})") },
                    icon = { Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1; viewModel.selectAdminTab(1) },
                    text = { Text("Customer Order Files (${customers.size})") },
                    icon = { Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2; viewModel.selectAdminTab(2) },
                    text = { Text("Payment Receipts (${orders.size})") },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3; viewModel.selectAdminTab(3) },
                    text = { Text("Sales Analytics") },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4; viewModel.selectAdminTab(4) },
                    text = { Text("Supabase Cloud") },
                    icon = { Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 5,
                    onClick = { selectedTab = 5; viewModel.selectAdminTab(5) },
                    text = { Text("Delivery Options") },
                    icon = { Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 6,
                    onClick = { selectedTab = 6; viewModel.selectAdminTab(6) },
                    text = { Text("Promo Activation") },
                    icon = { Icon(Icons.Default.Sell, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 7,
                    onClick = { selectedTab = 7; viewModel.selectAdminTab(7) },
                    text = { Text("Pre-Orders (${preOrderRequests.size})") },
                    icon = { Icon(Icons.Default.NewReleases, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 8,
                    onClick = { selectedTab = 8; viewModel.selectAdminTab(8) },
                    text = { Text("Store & Security") },
                    icon = { Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 9,
                    onClick = { selectedTab = 9; viewModel.selectAdminTab(9) },
                    text = { Text("Partners (${partnerOrganizations.size})") },
                    icon = { Icon(Icons.Default.Handshake, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("admin_partners_hub_tab")
                )
                Tab(
                    selected = selectedTab == 10,
                    onClick = { selectedTab = 10; viewModel.selectAdminTab(10) },
                    text = { Text("Returns & Support (${returnRequests.size + supportTickets.size})") },
                    icon = { Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("admin_returns_support_tab")
                )
                val liveCallState by viewModel.liveCallState.collectAsState()
                Tab(
                    selected = selectedTab == 11,
                    onClick = { selectedTab = 11; viewModel.selectAdminTab(11) },
                    text = {
                        val badge = if (liveCallState.incomingRequests.isNotEmpty()) " (${liveCallState.incomingRequests.size})" else ""
                        Text("Live Audio/Video$badge")
                    },
                    icon = { Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("admin_live_call_tab")
                )
            }

            DualAxisScrollContainer(
                wideMode = isWideDualAxisMode,
                minWideWidth = 740.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { _, _ ->
                when (selectedTab) {
                    0 -> ProductManagementTab(viewModel, products)
                    1 -> CustomerIdentityTab(
                        viewModel = viewModel,
                        customers = customers,
                        orders = orders,
                        onCustomerClick = { selectedCustomerFile = it },
                        onOpenPreOrdersTab = {
                            selectedTab = 7
                            viewModel.selectAdminTab(7)
                        }
                    )
                    2 -> PaymentReceiptsTab(viewModel, orders)
                    3 -> SalesAnalyticsTab(products, orders)
                    4 -> SupabaseCloudTab(viewModel, cloudStatusMsg)
                    5 -> DeliveryOptionsTab(viewModel)
                    6 -> PromoActivationTab(viewModel)
                    7 -> AdminPreOrdersTab(viewModel)
                    8 -> AdminSecuritySettingsTab(
                        viewModel = viewModel,
                        onNavigateToTab = { tab ->
                            selectedTab = tab
                            viewModel.selectAdminTab(tab)
                        }
                    )
                    9 -> AdminPartnerHubTab(viewModel = viewModel)
                    10 -> ReturnsAndSupportAdminTab(viewModel = viewModel)
                    11 -> LiveCallsAdminTab(viewModel = viewModel)
                }
            }
        }

        selectedCustomerFile?.let { customer ->
            val liveCustomer = customers.find { it.id == customer.id } ?: customer
            CustomerFileModal(
                customer = liveCustomer,
                orders = orders.filter { matchesCustomerFolder(it, liveCustomer) },
                onDismiss = { selectedCustomerFile = null },
                viewModel = viewModel
            )
        }
    }
}

private fun matchesCustomerFolder(order: Order, customer: Customer): Boolean {
    if (order.customerId == customer.id) return true
    if (customer.email.isNotBlank() && order.customerEmail.isNotBlank() &&
        order.customerEmail.equals(customer.email, ignoreCase = true)
    ) return true
    val custDigits = customer.phoneNumber.filter { it.isDigit() }
    val ordDigits = order.customerPhone.filter { it.isDigit() }
    if (custDigits.length >= 7 && ordDigits.length >= 7) {
        val len = minOf(10, minOf(custDigits.length, ordDigits.length))
        if (custDigits.takeLast(len) == ordDigits.takeLast(len)) return true
    }
    return false
}

private fun matchesCustomerPreOrder(req: PreOrderRequest, customer: Customer): Boolean {
    if (req.customerId == customer.id) return true
    val custDigits = customer.phoneNumber.filter { it.isDigit() }
    val reqDigits = req.customerPhone.filter { it.isDigit() }
    if (custDigits.length >= 7 && reqDigits.length >= 7) {
        val len = minOf(10, minOf(custDigits.length, reqDigits.length))
        if (custDigits.takeLast(len) == reqDigits.takeLast(len)) return true
    }
    if (customer.fullName.isNotBlank() && req.customerName.isNotBlank() &&
        req.customerName != "Valued Customer" &&
        req.customerName.equals(customer.fullName, ignoreCase = true)
    ) return true
    return false
}

@Composable
private fun CredentialDetailRow(
    label: String,
    value: String,
    context: Context,
    isMonospace: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default
            )
        }
        IconButton(
            onClick = {
                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cb.setPrimaryClip(ClipData.newPlainText(label, value))
                Toast.makeText(context, "$label copied!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy $label", modifier = Modifier.size(15.dp), tint = CyberCyan)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerFileModal(
    customer: Customer,
    orders: List<Order>,
    onDismiss: () -> Unit,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val storeProfile by viewModel.storeProfile.collectAsState()
    val allPreOrders by viewModel.preOrderRequests.collectAsState()
    val customerPreOrders = remember(allPreOrders, customer) {
        allPreOrders.filter { matchesCustomerPreOrder(it, customer) }
    }
    val nairaFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "NG")).apply {
            maximumFractionDigits = 0
        }
    }
    val fullDateTimeFormat = remember { SimpleDateFormat("EEEE, dd MMMM yyyy 'at' hh:mm:ss a", Locale.getDefault()) }
    val shortDateTimeFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val timeOnlyFormat = remember { SimpleDateFormat("hh:mm:ss a", Locale.getDefault()) }

    var folderSectionTab by remember { mutableIntStateOf(0) } // 0 = All, 1 = Sign-Up Credentials, 2 = Orders, 3 = Pre-Orders
    var showPasswordPlain by remember { mutableStateOf(false) }
    var selectedOrderFilter by remember { mutableStateOf<OrderStatus?>(null) }
    var viewingReceiptUrl by remember { mutableStateOf<String?>(null) }

    val filteredOrders = if (selectedOrderFilter == null) orders else orders.filter { it.status == selectedOrderFilter }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, CyberCyan.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Dossier Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = CyberCyan.copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "OFFICIAL CUSTOMER FILE",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = AccentGold.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "#CUST-${customer.id.take(6).uppercase()}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentGold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${storeProfile.storeName} Customer & Order Dossier",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close Customer File")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Folder Internal Divider Tabs: All / Credentials / Orders / Pre-Orders
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = folderSectionTab == 0,
                        onClick = { folderSectionTab = 0 },
                        label = { Text("📁 Full Folder", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    FilterChip(
                        selected = folderSectionTab == 1,
                        onClick = { folderSectionTab = 1 },
                        label = { Text("🔐 Sign-Up Credentials", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    FilterChip(
                        selected = folderSectionTab == 2,
                        onClick = { folderSectionTab = 2 },
                        label = { Text("📦 Orders (${orders.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    FilterChip(
                        selected = folderSectionTab == 3,
                        onClick = { folderSectionTab = 3 },
                        label = { Text("✨ Pre-Orders (${customerPreOrders.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ==========================================
                    // 1. SIGN-UP CREDENTIALS & CUSTOMER IDENTITY SHEET
                    // ==========================================
                    if (folderSectionTab == 0 || folderSectionTab == 1) {
                        item {
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                                border = BorderStroke(1.2.dp, CyberCyan.copy(alpha = 0.45f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "CUSTOMER MASTER IDENTITY",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    if (customer.isAdmin) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = AccentGold.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "ADMIN USER",
                                                color = AccentGold,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = SuccessGreen.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "VERIFIED SHOPPER",
                                                color = SuccessGreen,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Full Name
                                Text(
                                    text = customer.fullName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // Phone & Direct Communication
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = customer.phoneNumber.ifBlank { "No phone recorded" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    if (customer.phoneNumber.isNotBlank()) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            // Call Button
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                modifier = Modifier.clickable {
                                                    try {
                                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.phoneNumber.trim()}"))
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Could not launch dialer: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Icon(Icons.Default.Call, contentDescription = "Call Customer", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Call", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                }
                                            }

                                            // WhatsApp Customer Button
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = SuccessGreen.copy(alpha = 0.15f),
                                                modifier = Modifier.clickable {
                                                    try {
                                                        val clean = customer.phoneNumber.replace("+", "").replace(" ", "").replace("-", "")
                                                        val waUri = Uri.parse("https://api.whatsapp.com/send?phone=$clean&text=${Uri.encode("Hello ${customer.fullName}, this is ${storeProfile.storeName} regarding your account and orders.")}")
                                                        context.startActivity(Intent(Intent.ACTION_VIEW, waUri))
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Could not open WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Icon(Icons.Default.Chat, contentDescription = "WhatsApp Customer", modifier = Modifier.size(14.dp), tint = SuccessGreen)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // SIGN-UP DATE AND EXACT TIME
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "ACCOUNT SIGN-UP DATE & TIME",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (customer.createdAt > 0L) fullDateTimeFormat.format(Date(customer.createdAt)) else "Legacy / Pre-Sync Record",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (customer.createdAt > 0L) "Timestamp ID: ${customer.createdAt} ms" else "Created before cloud timestamps were enabled",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Delivery Address
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "SAVED DELIVERY ADDRESS",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = customer.deliveryAddress.ifBlank { "No default delivery address set yet." },
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        if (customer.deliveryAddress.isNotBlank()) {
                                            IconButton(
                                                onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    clipboard.setPrimaryClip(ClipData.newPlainText("Customer Address", customer.deliveryAddress))
                                                    Toast.makeText(context, "Address copied to clipboard!", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Address", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Quick Metrics Grid
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("ORDERS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("${orders.size}", fontSize = 15.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("PRE-ORDERS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("${customerPreOrders.size}", fontSize = 15.sp, fontWeight = FontWeight.Black, color = AccentGold)
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.weight(1.4f)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("SPEND", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(nairaFormat.format(orders.sumOf { it.totalAmount }), fontSize = 13.sp, fontWeight = FontWeight.Black, color = SuccessGreen)
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("DELIVERED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("${orders.count { it.status == OrderStatus.DELIVERED }}", fontSize = 15.sp, fontWeight = FontWeight.Black, color = CyberCyan)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // SIGN-UP CREDENTIALS VAULT CARD INSIDE CUSTOMER FOLDER
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.2.dp, AccentGold.copy(alpha = 0.55f)),
                                    modifier = Modifier.fillMaxWidth().testTag("folder_signup_credentials_card")
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Key, contentDescription = null, tint = AccentGold, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "SIGN-UP CREDENTIALS & LOGIN",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Black,
                                                    color = AccentGold
                                                )
                                            }
                                            TextButton(
                                                onClick = {
                                                    val credDump = """
                                                        24/7 SHOPPING — CUSTOMER SIGN-UP CREDENTIALS
                                                        Folder ID: #CUST-${customer.id.take(8).uppercase()}
                                                        Full Name: ${customer.fullName}
                                                        Login Phone: ${customer.phoneNumber.ifBlank { "N/A" }}
                                                        Sign-Up Password: ${customer.signUpPassword.ifBlank { "(Synced/Set on Customer Device)" }}
                                                        Delivery Address: ${customer.deliveryAddress.ifBlank { "N/A" }}
                                                        Signed Up: ${if (customer.createdAt > 0L) fullDateTimeFormat.format(Date(customer.createdAt)) else "Legacy Record"}
                                                    """.trimIndent()
                                                    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    cb.setPrimaryClip(ClipData.newPlainText("Sign-Up Credentials", credDump))
                                                    Toast.makeText(context, "Sign-Up Credentials copied!", Toast.LENGTH_SHORT).show()
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Copy Credentials", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                                        CredentialDetailRow("Customer Account ID", customer.id, context, isMonospace = true)
                                        CredentialDetailRow("Full Name", customer.fullName, context)
                                        CredentialDetailRow("Login Phone Number", customer.phoneNumber.ifBlank { "Not Provided" }, context)

                                        // Sign-Up Password / Passcode Row with Reveal Toggle
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Sign-Up Password / Access Code",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = when {
                                                        customer.signUpPassword.isBlank() -> "Set on customer device (re-sync or sign in after SQL update to mirror)"
                                                        showPasswordPlain -> customer.signUpPassword
                                                        else -> "•".repeat(customer.signUpPassword.length.coerceIn(6, 14))
                                                    },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = if (customer.signUpPassword.isNotBlank()) FontFamily.Monospace else FontFamily.Default,
                                                    color = if (customer.signUpPassword.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (customer.signUpPassword.isNotBlank()) {
                                                Row {
                                                    IconButton(
                                                        onClick = { showPasswordPlain = !showPasswordPlain },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (showPasswordPlain) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                            contentDescription = if (showPasswordPlain) "Hide Password" else "Reveal Password",
                                                            modifier = Modifier.size(16.dp),
                                                            tint = AccentGold
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                            cb.setPrimaryClip(ClipData.newPlainText("Customer Password", customer.signUpPassword))
                                                            Toast.makeText(context, "Password copied!", Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Password", modifier = Modifier.size(15.dp), tint = CyberCyan)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        }
                    }

                    // ==========================================
                    // 2. ORDER HISTORY & LIFECYCLE CONTROLS INSIDE THIS CUSTOMER'S FOLDER
                    // ==========================================
                    if (folderSectionTab == 0 || folderSectionTab == 2) {
                        item {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Orders in ${customer.fullName.substringBefore(" ")}'s Folder (${orders.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black
                                    )
                                }

                            // Filter Chips Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = selectedOrderFilter == null,
                                    onClick = { selectedOrderFilter = null },
                                    label = { Text("All (${orders.size})", fontSize = 11.sp) }
                                )
                                OrderStatus.values().forEach { st ->
                                    val count = orders.count { it.status == st }
                                    if (count > 0 || selectedOrderFilter == st) {
                                        FilterChip(
                                            selected = selectedOrderFilter == st,
                                            onClick = { selectedOrderFilter = if (selectedOrderFilter == st) null else st },
                                            label = { Text("${st.label} ($count)", fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                        if (filteredOrders.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = if (orders.isEmpty()) "No orders placed yet inside this customer's folder." else "No orders matching selected filter.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        items(filteredOrders, key = { it.orderId }) { order ->
                        var isExpanded by remember { mutableStateOf(true) }
                        var dispatchWhatsappInput by remember { mutableStateOf(order.dispatchRiderWhatsapp) }
                        var statusNoteInput by remember { mutableStateOf("") }
                        var selectedTargetStatus by remember { mutableStateOf(order.status) }
                        var statusDropdownOpen by remember { mutableStateOf(false) }

                        Card(
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(
                                1.5.dp,
                                when (order.status) {
                                    OrderStatus.DELIVERED -> SuccessGreen
                                    OrderStatus.OUT_FOR_DELIVERY -> CyberCyan
                                    OrderStatus.VERIFIED -> MoniepointBlue
                                    OrderStatus.CANCELLED -> MaterialTheme.colorScheme.error
                                    else -> AccentGold
                                }
                            ),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                // Order Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = order.orderId,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "Placed: ${shortDateTimeFormat.format(Date(order.createdAt))}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = when (order.status) {
                                            OrderStatus.DELIVERED -> SuccessGreen.copy(alpha = 0.15f)
                                            OrderStatus.OUT_FOR_DELIVERY -> CyberCyan.copy(alpha = 0.15f)
                                            OrderStatus.VERIFIED -> MoniepointBlue.copy(alpha = 0.15f)
                                            OrderStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer
                                            else -> AccentGold.copy(alpha = 0.15f)
                                        }
                                    ) {
                                        Text(
                                            text = order.status.label,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = when (order.status) {
                                                OrderStatus.DELIVERED -> SuccessGreen
                                                OrderStatus.OUT_FOR_DELIVERY -> CyberCyan
                                                OrderStatus.VERIFIED -> MoniepointBlue
                                                OrderStatus.CANCELLED -> MaterialTheme.colorScheme.error
                                                else -> AccentGold
                                            }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Order Financials & Payment Information
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Total Amount:",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = nairaFormat.format(order.totalAmount),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Payment Method: ${order.paymentMethod.title}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = "Ref / Slip: ${order.paymentReference.ifBlank { "Awaiting Reference" }}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (order.senderName.isNotBlank()) {
                                            Text(
                                                text = "Sender Name: ${order.senderName}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = CyberCyan
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Order Profit: +${nairaFormat.format(order.profit)}", color = SuccessGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    Text("10% from Profit: ${nairaFormat.format(order.profit * 0.10)}", color = AccentGold, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Ordered Items Breakdown with profit per item
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "TOTAL PROFIT MADE FROM EACH ITEM:",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Black,
                                                color = CyberCyan
                                            )
                                            Text(
                                                text = "+${nairaFormat.format(order.profit)}",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.sp,
                                                color = SuccessGreen
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        order.itemsSummary.split(";", "\n").filter { it.isNotBlank() }.forEach { line ->
                                            Text(
                                                text = "• ${line.trim()}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 11.sp,
                                                lineHeight = 15.sp
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Destination Address
                                Text(
                                    text = "Delivery Address: ${order.deliveryAddress}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // Assigned Rider Display Banner
                                if (order.dispatchRiderWhatsapp.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = SuccessGreen.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.LocalShipping, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Column {
                                                    Text("ASSIGNED DISPATCH RIDER", fontSize = 9.sp, fontWeight = FontWeight.Black, color = SuccessGreen)
                                                    Text("WhatsApp: ${order.dispatchRiderWhatsapp}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            // Direct WhatsApp Rider Button
                                            IconButton(
                                                onClick = {
                                                    try {
                                                        val cleanRider = order.dispatchRiderWhatsapp.replace("+", "").replace(" ", "").replace("-", "")
                                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$cleanRider"))
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Cannot open WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Chat, contentDescription = "Chat Rider", tint = SuccessGreen, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }

                                // Receipt Thumbnail Inspection
                                if (order.receiptImageUrl.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = { viewingReceiptUrl = order.receiptImageUrl },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().height(36.dp)
                                    ) {
                                        Icon(Icons.Default.Verified, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Inspect Uploaded Payment Receipt Photo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Toggle Action Panel
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = {
                                            // Copy complete order summary
                                            val summary = """
                                                *${storeProfile.storeName} ORDER DOSSIER*
                                                Order ID: ${order.orderId}
                                                Customer: ${customer.fullName} (${customer.phoneNumber})
                                                Placed: ${shortDateTimeFormat.format(Date(order.createdAt))}
                                                Total: ${nairaFormat.format(order.totalAmount)}
                                                Status: ${order.status.label}
                                                Delivery Address: ${order.deliveryAddress}
                                                Items: ${order.itemsSummary}
                                                Rider WhatsApp: ${order.dispatchRiderWhatsapp.ifBlank { "Unassigned" }}
                                            """.trimIndent()
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Order File", summary))
                                            Toast.makeText(context, "Order summary copied to clipboard!", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Copy Order Details", fontSize = 11.sp)
                                    }

                                    TextButton(onClick = { isExpanded = !isExpanded }) {
                                        Text(if (isExpanded) "Hide Actions & History" else "Manage Order & Actions")
                                        Icon(if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null)
                                    }
                                }

                                if (isExpanded) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                    // ==========================================
                                    // A. LIFECYCLE STATUS UPDATES LOG (DATE & TIME)
                                    // ==========================================
                                    Text(
                                        text = "STATUS UPDATE HISTORY (DATE & TIME LOG)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            // Initial placement record
                                            Row(verticalAlignment = Alignment.Top) {
                                                Text(
                                                    text = "• ${shortDateTimeFormat.format(Date(order.createdAt))}: ",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                                Text(
                                                    text = "[ORDER CREATED] Order placed by customer with ${order.paymentMethod.title}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            // Subsequent admin updates
                                            order.statusUpdates.forEach { update ->
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(verticalAlignment = Alignment.Top) {
                                                    Text(
                                                        text = "• ${shortDateTimeFormat.format(Date(update.timestamp))}: ",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        text = "[${update.status.label}] ${update.note.ifBlank { "Status updated by admin" }}",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // ==========================================
                                    // B. ADMIN EXECUTION: STATUS TRANSITIONS
                                    // ==========================================
                                    Text(
                                        text = "EXECUTE ORDER PROCESSING STATUS",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    // One-Tap Quick Action Buttons
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // 1. Confirm & Process
                                        Button(
                                            onClick = {
                                                viewModel.updateOrderStatus(
                                                    order.orderId,
                                                    OrderStatus.CONFIRMED,
                                                    "Payment verified by admin; order queued for packaging & processing"
                                                )
                                                Toast.makeText(context, "Order Confirmed & In Processing!", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MoniepointBlue),
                                            modifier = Modifier.weight(1f).height(38.dp)
                                        ) {
                                            Text("CONFIRM & PROCESS", fontSize = 9.sp, fontWeight = FontWeight.Black)
                                        }

                                        // 2. Dispatch / Out for Delivery
                                        Button(
                                            onClick = {
                                                val riderNote = if (dispatchWhatsappInput.isNotBlank()) "Dispatched with rider ($dispatchWhatsappInput)" else "Dispatched for customer delivery"
                                                viewModel.updateOrderStatus(
                                                    order.orderId,
                                                    OrderStatus.OUT_FOR_DELIVERY,
                                                    statusNoteInput.ifBlank { riderNote }
                                                )
                                                Toast.makeText(context, "Order Marked Out for Delivery!", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                            modifier = Modifier.weight(1f).height(38.dp)
                                        ) {
                                            Text("DISPATCH", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Black)
                                        }

                                        // 3. Mark Delivered
                                        Button(
                                            onClick = {
                                                viewModel.updateOrderStatus(
                                                    order.orderId,
                                                    OrderStatus.DELIVERED,
                                                    statusNoteInput.ifBlank { "Order delivered successfully to customer address" }
                                                )
                                                Toast.makeText(context, "Order Confirmed Delivered!", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                            modifier = Modifier.weight(1f).height(38.dp)
                                        ) {
                                            Text("DELIVERED", fontSize = 10.sp, fontWeight = FontWeight.Black)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Custom Status & Note Form
                                    ExposedDropdownMenuBox(
                                        expanded = statusDropdownOpen,
                                        onExpandedChange = { statusDropdownOpen = !statusDropdownOpen }
                                    ) {
                                        OutlinedTextField(
                                            value = selectedTargetStatus.label,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Set Custom Order Status") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusDropdownOpen) },
                                            modifier = Modifier.menuAnchor().fillMaxWidth()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = statusDropdownOpen,
                                            onDismissRequest = { statusDropdownOpen = false }
                                        ) {
                                            OrderStatus.values().forEach { st ->
                                                DropdownMenuItem(
                                                    text = { Text(st.label, fontWeight = if (st == selectedTargetStatus) FontWeight.Bold else FontWeight.Normal) },
                                                    onClick = {
                                                        selectedTargetStatus = st
                                                        statusDropdownOpen = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    OutlinedTextField(
                                        value = statusNoteInput,
                                        onValueChange = { statusNoteInput = it },
                                        label = { Text("Status Update Note / Remarks") },
                                        placeholder = { Text("e.g. Package cleared warehouse sorting") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Button(
                                        onClick = {
                                            val note = statusNoteInput.ifBlank { "Status updated to ${selectedTargetStatus.label}" }
                                            viewModel.updateOrderStatus(order.orderId, selectedTargetStatus, note)
                                            statusNoteInput = ""
                                            Toast.makeText(context, "Status & Note Saved to Order Lifecycle!", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().height(38.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Record Status Update with Current Time", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // ==========================================
                                    // C. ASSIGNED DISPATCH RIDER WHATSAPP ATTACHMENT
                                    // ==========================================
                                    Text(
                                        text = "ASSIGN DISPATCH RIDER WHATSAPP NUMBER",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = SuccessGreen
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    OutlinedTextField(
                                        value = dispatchWhatsappInput,
                                        onValueChange = { dispatchWhatsappInput = it },
                                        label = { Text("Dispatch Rider WhatsApp #") },
                                        placeholder = { Text("e.g. +234 801 234 5678") },
                                        leadingIcon = { Icon(Icons.Default.LocalShipping, contentDescription = null, tint = SuccessGreen) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Button(
                                        onClick = {
                                            if (dispatchWhatsappInput.isBlank()) {
                                                Toast.makeText(context, "Please enter a valid WhatsApp number", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            viewModel.updateOrderDispatchRider(order.orderId, dispatchWhatsappInput.trim())
                                            Toast.makeText(context, "Dispatch Rider WhatsApp Attached to Order!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().height(38.dp)
                                    ) {
                                        Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Save & Attach Rider WhatsApp to Order", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // When rider WhatsApp is attached, enable instant WhatsApp dispatch triggers
                                    if (order.dispatchRiderWhatsapp.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            // Send Waybill to Rider
                                            OutlinedButton(
                                                onClick = {
                                                    try {
                                                        val cleanRider = order.dispatchRiderWhatsapp.replace("+", "").replace(" ", "").replace("-", "")
                                                        val waybill = """
                                                            *${storeProfile.storeName} DISPATCH ASSIGNMENT*
                                                            📦 *Order ID:* ${order.orderId}
                                                            👤 *Customer Name:* ${customer.fullName}
                                                            📞 *Customer Phone:* ${customer.phoneNumber}
                                                            📍 *Delivery Address:* ${order.deliveryAddress}
                                                            🛍️ *Items:* ${order.itemsSummary}
                                                            💰 *Total Amount:* ${nairaFormat.format(order.totalAmount)}
                                                            💳 *Payment Status:* ${order.paymentMethod.title}
                                                            Please confirm package pickup and proceed to delivery. Thank you!
                                                        """.trimIndent()
                                                        val waUri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanRider&text=${Uri.encode(waybill)}")
                                                        context.startActivity(Intent(Intent.ACTION_VIEW, waUri))
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Error opening WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f).height(38.dp)
                                            ) {
                                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp), tint = SuccessGreen)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Send Waybill to Rider", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }

                                            // Notify Customer of Dispatch & Rider Contact
                                            if (customer.phoneNumber.isNotBlank()) {
                                                OutlinedButton(
                                                    onClick = {
                                                        try {
                                                            val cleanCust = customer.phoneNumber.replace("+", "").replace(" ", "").replace("-", "")
                                                            val msg = """
                                                                Hello ${customer.fullName}! Your ${storeProfile.storeName} order *${order.orderId}* has been dispatched!
                                                                🏍️ *Assigned Dispatch Rider:* ${order.dispatchRiderWhatsapp}
                                                                📦 *Items:* ${order.itemsSummary}
                                                                📍 *Destination:* ${order.deliveryAddress}
                                                                Our dispatch rider will arrive shortly. Thank you for choosing ${storeProfile.storeName}!
                                                            """.trimIndent()
                                                            val waUri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanCust&text=${Uri.encode(msg)}")
                                                            context.startActivity(Intent(Intent.ACTION_VIEW, waUri))
                                                        } catch (e: Exception) {
                                                            Toast.makeText(context, "Error notifying customer: ${e.message}", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f).height(38.dp)
                                                ) {
                                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp), tint = CyberCyan)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Notify Customer", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    }

                    // ==========================================
                    // 3. PRE-ORDERS & SPECIAL BRAND REQUESTS INSIDE THIS CUSTOMER'S FOLDER
                    // ==========================================
                    if (folderSectionTab == 0 || folderSectionTab == 3) {
                        item {
                            Text(
                                text = "Pre-Orders & Brand Requests (${customerPreOrders.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black
                            )
                        }

                        if (customerPreOrders.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No Pre-Order or Special Brand requests submitted by this customer yet.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            items(customerPreOrders, key = { "folder_po_${it.id}" }) { req ->
                                AdminPreOrderItemCard(
                                    req = req,
                                    viewModel = viewModel,
                                    nairaFormat = nairaFormat,
                                    dateFormat = shortDateTimeFormat,
                                    context = context
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close Customer Order File", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Receipt Full Image Pinch-to-Zoom Inspector Dialog
        viewingReceiptUrl?.let { url ->
            ZoomableImageDialog(
                imageUrl = url,
                title = "Payment Receipt • ${customer.fullName}",
                onDismiss = { viewingReceiptUrl = null }
            )
        }
    }
}

// ==========================================
// TAB 1: PRODUCT MANAGEMENT & FORM
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductManagementTab(
    viewModel: MainViewModel,
    products: List<Product>
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val adminBargainConfig by viewModel.adminBargainConfig.collectAsState()

    val nairaFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "NG")).apply {
            maximumFractionDigits = 0
        }
    }

    var isEditing by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf("") }

    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ProductCategory.CLOTHES.displayName) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var brand by remember { mutableStateOf("") }
    var availableBrands by remember { mutableStateOf("") }
    var availableSizes by remember { mutableStateOf("") }
    var adminShoeAudience by remember { mutableStateOf(ShoeAudience.MEN) }
    var adminShoeStandard by remember { mutableStateOf(ShoeStandard.EU) }
    var selectedColors by remember { mutableStateOf<List<ProductColor>>(listOf(ProductColor("Black", "#000000"), ProductColor("Cyber Cyan", "#06B6D4"), ProductColor("Crimson Red", "#DC2626"))) }
    var basePrice by remember { mutableStateOf("") }
    var costPrice by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("25") }
    var discountPercent by remember { mutableStateOf("0") }
    var imageUrl by remember { mutableStateOf("") }
    var galleryUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var model3dType by remember { mutableStateOf("SNEAKER") }
    var description by remember { mutableStateOf("") }

    var adminSearchQuery by remember { mutableStateOf("") }
    var activeAdminSubView by remember { mutableStateOf("CATALOG") } // "CATALOG", "ALERTS", "ADD_FORM"
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var showUploadedOnly by remember { mutableStateOf(false) }
    var quickEditProduct by remember { mutableStateOf<Product?>(null) }

    val customUploadedProducts = remember(products) { products.filter { !it.id.startsWith("prod-") } }
    val expiredProducts = remember(products) { products.filter { it.expiryMonthsRemaining <= 0 } }
    val shortDatedProducts = remember(products) { products.filter { it.expiryMonthsRemaining in 1..3 } }
    val lowStockProducts = remember(products) { products.filter { it.stockQuantity in 0..2 } }
    val totalAlertsCount = expiredProducts.size + shortDatedProducts.size + lowStockProducts.size

    val adminFilteredProducts = products.filter {
        (!showUploadedOnly || !it.id.startsWith("prod-")) &&
        (selectedCategoryFilter == "All" || it.category.equals(selectedCategoryFilter, ignoreCase = true)) &&
        (adminSearchQuery.isEmpty() ||
                it.title.contains(adminSearchQuery, ignoreCase = true) ||
                it.brand.contains(adminSearchQuery, ignoreCase = true) ||
                it.category.contains(adminSearchQuery, ignoreCase = true) ||
                it.sku.contains(adminSearchQuery, ignoreCase = true))
    }

    fun resetForm() {
        isEditing = false
        editingId = ""
        title = ""
        category = ProductCategory.CLOTHES.displayName
        brand = ""
        availableBrands = ""
        availableSizes = ""
        selectedColors = listOf(ProductColor("Black", "#000000"), ProductColor("Cyber Cyan", "#06B6D4"), ProductColor("Crimson Red", "#DC2626"))
        basePrice = ""
        costPrice = ""
        quantity = "25"
        discountPercent = "0"
        imageUrl = ""
        galleryUrls = emptyList()
        description = ""
    }

    if (quickEditProduct != null) {
        QuickEditProductDialog(
            product = quickEditProduct!!,
            onDismiss = { quickEditProduct = null },
            onSave = { updated ->
                viewModel.saveProductFromAdminWithStatus(updated) { syncedToCloud, cloudErr ->
                    val msg = when {
                        syncedToCloud && cloudErr.isNullOrBlank() ->
                            "✅ Saved & uploaded photo for '${updated.title}' to Supabase Cloud!"
                        syncedToCloud && !cloudErr.isNullOrBlank() ->
                            "⚠️ Synced '${updated.title}' text, but $cloudErr"
                        else ->
                            "⚠️ Saved '${updated.title}' locally (${cloudErr ?: "Link Supabase to sync"})"
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
                quickEditProduct = null
            }
        )
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TOP SUB-VIEW SWITCHER & VOICE CONTROLS
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = activeAdminSubView == "CATALOG" && !showUploadedOnly,
                            onClick = {
                                showUploadedOnly = false
                                activeAdminSubView = "CATALOG"
                            },
                            label = { Text("📦 Live Store Catalog (${products.size})", fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )

                        FilterChip(
                            selected = activeAdminSubView == "CATALOG" && showUploadedOnly,
                            onClick = {
                                showUploadedOnly = true
                                selectedCategoryFilter = "All"
                                activeAdminSubView = "CATALOG"
                            },
                            label = { Text("✨ Uploaded (${customUploadedProducts.size})", fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )

                        FilterChip(
                            selected = activeAdminSubView == "ALERTS",
                            onClick = { activeAdminSubView = "ALERTS" },
                            label = {
                                Text(
                                    "⚠️ Expiry & Low Stock ($totalAlertsCount)",
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalAlertsCount > 0) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = if (totalAlertsCount > 0) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = if (totalAlertsCount > 0) FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFDC2626).copy(alpha = 0.15f)
                            ) else FilterChipDefaults.filterChipColors()
                        )

                        FilterChip(
                            selected = activeAdminSubView == "ADD_FORM",
                            onClick = { activeAdminSubView = "ADD_FORM" },
                            label = { Text(if (isEditing) "✏️ Edit Product Form" else "➕ Add New Product", fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }

                    // Voice Notification & Inventory Audit Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val text = "Attention Admin! Inventory audit report: You have ${expiredProducts.size} expired items, ${shortDatedProducts.size} short-dated products within 3 months, and ${lowStockProducts.size} low stock products with 2 or fewer units remaining."
                                viewModel.voiceManager.speakText(text)
                                Toast.makeText(context, text, Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("🔊 Voice Inventory Audit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.voiceManager.speakNewOrderAlert("Ibrahim Abubakar", "₦185,000", "#DEP-7824")
                                Toast.makeText(context, "Triggered test fresh order voice-over alert!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("🔔 Test Order Voice Alert", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // CONDITIONAL FORM CARD
        if (activeAdminSubView == "ADD_FORM" || isEditing) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isEditing) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = if (isEditing) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isEditing) "✏️ Edit Product in Cloud & DB" else "➕ Add New 3D Catalog Product",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isEditing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (isEditing) {
                                Text(
                                    text = "Editing ID: ${editingId.take(12)}...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        if (isEditing) {
                            OutlinedButton(onClick = { resetForm() }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Product")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Product Title") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_product_title_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Category Selector Dropdown
                    ExposedDropdownMenuBox(
                        expanded = categoryDropdownExpanded,
                        onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Product Category (7 Available)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false }
                        ) {
                            ProductCategory.values().forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.displayName) },
                                    onClick = {
                                        category = cat.displayName
                                        categoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = brand,
                            onValueChange = { brand = it },
                            label = { Text("Primary Brand") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_brand_input")
                        )
                        OutlinedTextField(
                            value = availableBrands,
                            onValueChange = { availableBrands = it },
                            label = { Text("Available Brands (csv)") },
                            placeholder = { Text("Nike, Adidas, Puma") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // DEDICATED SIZE SELECTION BOX (Category-Adaptive: Clothes, Shoes, Bags, Beauty, Drinks, Electronics, etc.)
                    SizeSelectionBox(
                        category = category,
                        availableSizesCsv = availableSizes,
                        onSizesChanged = { availableSizes = it }
                    )

                    // UNIVERSAL SHOE SIZE BUILDER HELPER (Active when category is Shoes / Footwear)
                    if (category.contains("Shoe", ignoreCase = true) || category.contains("Footwear", ignoreCase = true)) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "👞 Universal Shoe Size Builder",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Select audience & standard, then tap numbers to add universal shoe sizes automatically.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // Audience Selector Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    ShoeAudience.values().forEach { aud ->
                                        val isSelected = adminShoeAudience == aud
                                        OutlinedButton(
                                            onClick = { adminShoeAudience = aud },
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).height(36.dp)
                                        ) {
                                            Text(aud.label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Standard Selector Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    ShoeStandard.values().forEach { std ->
                                        val isSelected = adminShoeStandard == std
                                        OutlinedButton(
                                            onClick = { adminShoeStandard = std },
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = if (isSelected) CyberCyan else Color.Transparent,
                                                contentColor = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).height(32.dp)
                                        ) {
                                            Text(std.label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Quick Number Pills
                                val sampleSizes = when (adminShoeAudience) {
                                    ShoeAudience.MEN -> listOf(38, 39, 40, 41, 42, 43, 44, 45, 46)
                                    ShoeAudience.WOMEN -> listOf(35, 36, 37, 38, 39, 40, 41, 42)
                                    ShoeAudience.CHILDREN -> listOf(24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36)
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    sampleSizes.forEach { n ->
                                        val formatted = com.example.ui.components.formatShoeSizeWithAudienceAndStandard("$n", adminShoeAudience, adminShoeStandard)
                                        OutlinedButton(
                                            onClick = {
                                                val existing = availableSizes.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
                                                if (!existing.contains(formatted)) {
                                                    existing.add(formatted)
                                                    availableSizes = existing.joinToString(", ")
                                                    Toast.makeText(context, "Added $formatted", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text(formatted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    TextButton(
                                        onClick = {
                                            val fullSet = sampleSizes.map { com.example.ui.components.formatShoeSizeWithAudienceAndStandard("$it", adminShoeAudience, adminShoeStandard) }
                                            availableSizes = fullSet.joinToString(", ")
                                            Toast.makeText(context, "Added full ${adminShoeAudience.label} range (${adminShoeStandard.label})", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Text("Add Full ${adminShoeAudience.label} Range", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    TextButton(
                                        onClick = { availableSizes = "" }
                                    ) {
                                        Text("Clear Sizes", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TickableColourPicker(
                        selectedColors = selectedColors,
                        onColorsChanged = { selectedColors = it }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = basePrice,
                            onValueChange = { basePrice = it },
                            label = { Text("Base Price (₦)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_price_input")
                        )
                        OutlinedTextField(
                            value = costPrice,
                            onValueChange = { costPrice = it },
                            label = { Text("Cost Price (₦)") },
                            placeholder = { Text("Default 70%") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_cost_price_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { quantity = it },
                            label = { Text("Quantity in Stock") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_quantity_input")
                        )
                        OutlinedTextField(
                            value = discountPercent,
                            onValueChange = { discountPercent = it },
                            label = { Text("Promo Discount (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_discount_input")
                        )
                    }

                    // REAL-TIME PRICE FIXING & PROFIT FEASIBILITY SIMULATION BOX (ADMIN EXCLUSIVE)
                    val formPriceVal = basePrice.toDoubleOrNull() ?: 0.0
                    val formCostVal = costPrice.toDoubleOrNull() ?: (formPriceVal * 0.70)
                    val activeCategoryPromo = viewModel.getEffectiveDiscountPercent(category)
                    val effectivePromoPercent = if (discountPercent.toIntOrNull() != null && (discountPercent.toIntOrNull() ?: 0) > 0) discountPercent.toInt() else activeCategoryPromo
                    val adminBargainLimit = adminBargainConfig.maxBargainDiscountPercent

                    if (formPriceVal > 0.0) {
                        val stdProfit = formPriceVal - formCostVal
                        val stdMargin = (stdProfit / formPriceVal) * 100.0

                        val promoPrice = formPriceVal * (1.0 - (effectivePromoPercent / 100.0))
                        val promoProfit = promoPrice - formCostVal
                        val promoMargin = if (promoPrice > 0) (promoProfit / promoPrice) * 100.0 else 0.0

                        val bargainPrice = formPriceVal * (1.0 - (adminBargainLimit / 100.0))
                        val bargainProfit = bargainPrice - formCostVal
                        val bargainMargin = if (bargainPrice > 0) (bargainProfit / bargainPrice) * 100.0 else 0.0

                        val formQty = quantity.toIntOrNull() ?: 25
                        val batchTotalProfit = stdProfit * formQty
                        val tenPercentBatch = batchTotalProfit * 0.10

                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.5.dp, CyberCyan),
                            modifier = Modifier.fillMaxWidth().testTag("admin_price_fixing_profit_box")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "PRICE FIXING PROFIT ANALYSIS",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp,
                                            color = CyberCyan
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = AccentGold.copy(alpha = 0.15f)
                                    ) {
                                        Text("ADMIN FEASIBILITY", color = AccentGold, fontSize = 8.5.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Selling Price: ${nairaFormat.format(formPriceVal)} | Cost Price: ${nairaFormat.format(formCostVal)}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "• Standard Profit: +${nairaFormat.format(stdProfit)} (${String.format(Locale.US, "%.1f", stdMargin)}% margin)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessGreen
                                )
                                Text(
                                    text = "• Under Promo ($effectivePromoPercent% off): Selling: ${nairaFormat.format(promoPrice)} -> Resultant Profit: +${nairaFormat.format(promoProfit)} (${String.format(Locale.US, "%.1f", promoMargin)}%)",
                                    fontSize = 10.5.sp,
                                    color = if (promoProfit >= 0) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFFDC2626)
                                )
                                Text(
                                    text = "• Under Price Bargain ($adminBargainLimit% cap): Selling: ${nairaFormat.format(bargainPrice)} -> Resultant Profit: +${nairaFormat.format(bargainProfit)} (${String.format(Locale.US, "%.1f", bargainMargin)}%)",
                                    fontSize = 10.5.sp,
                                    color = if (bargainProfit >= 0) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFFDC2626)
                                )
                                Text(
                                    text = "• Expected Batch Profit ($formQty units): +${nairaFormat.format(batchTotalProfit)} | 10% Reserve: ${nairaFormat.format(tenPercentBatch)}",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentGold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Analysis: Fixing retail price at ${nairaFormat.format(formPriceVal)} against wholesale cost of ${nairaFormat.format(formCostVal)} provides strong resilience. Even under simultaneous promo ($effectivePromoPercent%) or maximum allowed bargain ($adminBargainLimit%), minimum net profit remains protected at +${nairaFormat.format(minOf(promoProfit, bargainProfit))} per unit.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 9.5.sp,
                                    lineHeight = 13.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Image URL or Local File Upload
                    val photoPicker = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.PickVisualMedia()
                    ) { uri ->
                        if (uri != null) {
                            imageUrl = uri.toString()
                        }
                    }

                    Button(
                        onClick = {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (imageUrl.isBlank()) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primaryContainer,
                            contentColor = if (imageUrl.isBlank()) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Pick Product Photo from Gallery", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (imageUrl.isBlank()) "Pick Product Photo from Gallery (Required)" else "Change Product Photo from Gallery",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = imageUrl,
                            onValueChange = { imageUrl = it },
                            label = { Text("Image URL or Selected Gallery Photo") },
                            placeholder = { Text("Pick photo above or paste https:// image URL") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Select Image")
                        }
                    }

                    if (imageUrl.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                .padding(8.dp)
                        ) {
                            AsyncImage(
                                model = ProductImageHelper.buildImageRequest(
                                    context = context,
                                    imageUrl = imageUrl,
                                    category = category,
                                    title = title
                                ),
                                contentDescription = "Uploaded Image Preview",
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (ProductImageHelper.isLocalOnlyUri(imageUrl)) "Gallery Photo Selected" else "Cloud Image URL Linked",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (ProductImageHelper.isLocalOnlyUri(imageUrl)) "Uploads permanently to Supabase Storage when saved" else "Ready to display in store catalog",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val galleryPicker = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
                    ) { uris ->
                        if (uris.isNotEmpty()) {
                            galleryUrls = (galleryUrls + uris.map { it.toString() })
                                .distinct()
                                .take(5)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            galleryPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_add_gallery_angles_button")
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (galleryUrls.isEmpty()) "Add Extra Gallery Angles (Optional, up to 5)"
                            else "Add More Angles (${galleryUrls.size}/5 selected)",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (galleryUrls.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(galleryUrls) { extraUrl ->
                                Box(modifier = Modifier.size(64.dp)) {
                                    AsyncImage(
                                        model = ProductImageHelper.buildImageRequest(
                                            context = context,
                                            imageUrl = extraUrl,
                                            category = category,
                                            title = title
                                        ),
                                        contentDescription = "Extra product angle",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White)
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFDC2626),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(2.dp)
                                            .size(20.dp)
                                            .clickable { galleryUrls = galleryUrls.filterNot { it == extraUrl } }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove extra photo",
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Product Description") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (title.isBlank() || brand.isBlank() || basePrice.isBlank()) {
                                Toast.makeText(context, "Please enter title, brand, and price", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (imageUrl.isBlank()) {
                                Toast.makeText(context, "Please pick the product photo from gallery or paste an image URL", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            val priceVal = basePrice.toDoubleOrNull() ?: 0.0
                            val costVal = costPrice.toDoubleOrNull() ?: (priceVal * 0.70)
                            val qtyVal = quantity.toIntOrNull() ?: 10
                            val cleanMainImage = imageUrl.trim()
                            val cleanGallery = galleryUrls.map { it.trim() }
                                .filter { it.isNotBlank() && it != cleanMainImage }
                                .distinct()

                            val newProd = Product(
                                id = if (isEditing && editingId.isNotBlank()) editingId else UUID.randomUUID().toString(),
                                title = title.trim(),
                                category = category,
                                brand = brand.trim(),
                                availableBrands = if (availableBrands.isNotBlank()) availableBrands.split(",").map { it.trim() } else listOf(brand.trim()),
                                availableSizes = if (availableSizes.isNotBlank()) availableSizes.split(",").map { it.trim() } else listOf("Standard"),
                                availableColors = if (selectedColors.isNotEmpty()) selectedColors else listOf(ProductColor("Black", "#000000")),
                                basePrice = priceVal,
                                costPrice = costVal,
                                discountPercent = discountPercent.toIntOrNull() ?: 0,
                                stockQuantity = qtyVal,
                                imageUrl = cleanMainImage,
                                galleryUrls = cleanGallery,
                                model3dType = model3dType,
                                description = description.ifBlank { "High-end product with full 3D interactive preview." },
                                createdAt = System.currentTimeMillis()
                            )

                            val wasEditing = isEditing
                            viewModel.saveProductFromAdminWithStatus(newProd) { syncedToCloud, cloudErr ->
                                val msg = when {
                                    syncedToCloud && cloudErr.isNullOrBlank() ->
                                        if (wasEditing) "✅ Updated & Synced '${newProd.title}' + photo to Supabase Cloud!"
                                        else "✅ Saved & Synced '${newProd.title}' + photo to Supabase Cloud!"
                                    syncedToCloud && !cloudErr.isNullOrBlank() ->
                                        "⚠️ Synced '${newProd.title}' details, but $cloudErr"
                                    else ->
                                        "⚠️ Saved '${newProd.title}' locally (${cloudErr ?: "Link Supabase to sync"})"
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                resetForm()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("admin_save_product_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isEditing) "Update & Sync Product to Cloud" else "Save & Sync Product to Supabase Cloud", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        } // End of conditional form card

        if (activeAdminSubView == "CATALOG") {
            // DIRECT STORE CATALOG VIEW (NO STOREFRONT SWITCHING NEEDED)
            item {
                OutlinedTextField(
                    value = adminSearchQuery,
                    onValueChange = { adminSearchQuery = it },
                    label = { Text("Search Catalog (Title, Brand, SKU or Category)") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (adminSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { adminSearchQuery = "" }) {
                                Icon(Icons.Default.Check, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Category & Uploaded Filter Pills
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !showUploadedOnly && selectedCategoryFilter == "All",
                        onClick = {
                            showUploadedOnly = false
                            selectedCategoryFilter = "All"
                        },
                        label = { Text("All (${products.size})", fontWeight = FontWeight.Bold) }
                    )
                    FilterChip(
                        selected = showUploadedOnly,
                        onClick = {
                            showUploadedOnly = !showUploadedOnly
                            selectedCategoryFilter = "All"
                        },
                        label = { Text("✨ Uploaded Only (${customUploadedProducts.size})", fontWeight = FontWeight.Bold) }
                    )
                    ProductCategory.values().forEach { cat ->
                        val count = products.count { it.category.equals(cat.displayName, ignoreCase = true) }
                        FilterChip(
                            selected = !showUploadedOnly && selectedCategoryFilter.equals(cat.displayName, ignoreCase = true),
                            onClick = {
                                showUploadedOnly = false
                                selectedCategoryFilter = cat.displayName
                            },
                            label = { Text("${cat.displayName} ($count)") }
                        )
                    }
                }
            }

            // Status Summary Chips
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Listed", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${products.size} Items (${customUploadedProducts.size} Uploaded)", fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                        Column(modifier = Modifier.clickable { activeAdminSubView = "ALERTS" }) {
                            Text("Low Stock (≤2)", fontSize = 11.sp, color = if (lowStockProducts.isNotEmpty()) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${lowStockProducts.size} Items", fontWeight = FontWeight.Black, fontSize = 14.sp, color = if (lowStockProducts.isNotEmpty()) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface)
                        }
                        Column(modifier = Modifier.clickable { activeAdminSubView = "ALERTS" }) {
                            Text("Expired (≤0mo)", fontSize = 11.sp, color = if (expiredProducts.isNotEmpty()) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${expiredProducts.size} Items", fontWeight = FontWeight.Black, fontSize = 14.sp, color = if (expiredProducts.isNotEmpty()) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface)
                        }
                        Column(modifier = Modifier.clickable { activeAdminSubView = "ALERTS" }) {
                            Text("Short-Dated (≤3mo)", fontSize = 11.sp, color = if (shortDatedProducts.isNotEmpty()) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${shortDatedProducts.size} Items", fontWeight = FontWeight.Black, fontSize = 14.sp, color = if (shortDatedProducts.isNotEmpty()) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            // Direct Catalog Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                showUploadedOnly -> "Uploaded Products (${adminFilteredProducts.size} Items)"
                                selectedCategoryFilter == "All" -> "Direct Store Catalog (${adminFilteredProducts.size} Items)"
                                else -> "$selectedCategoryFilter Catalog (${adminFilteredProducts.size} Items)"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Uploaded & newest products appear first at the top",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = {
                                viewModel.syncAllCloudRecordsNow { msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Pull Cloud Products", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pull Cloud", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.seed100Products { count ->
                                    Toast.makeText(context, "Loaded $count catalog products across all 7 categories!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("100 Items", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            val brokenPhotoUploadedProducts = customUploadedProducts.filter {
                ProductImageHelper.isUnreadableImageUri(context, it.imageUrl)
            }
            if (brokenPhotoUploadedProducts.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFDC2626).copy(alpha = 0.08f)),
                        border = BorderStroke(1.5.dp, Color(0xFFDC2626).copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "📷 ${brokenPhotoUploadedProducts.size} Uploaded Product(s) Need Real Photo Re-upload",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = Color(0xFFDC2626)
                            )
                            Text(
                                text = "Those items were synced with an old phone link that expired after reinstalling, so they currently display a fallback category image instead of your photo. Tap 'Fix Photo' on each item below to upload its real photo to Supabase Storage.",
                                fontSize = 11.5.sp,
                                lineHeight = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            if (adminFilteredProducts.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No products match '$selectedCategoryFilter' or search query.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(adminFilteredProducts, key = { it.id }) { item ->
                    AdminProductItemCard(
                        product = item,
                        adminBargainConfig = adminBargainConfig,
                        onEdit = {
                            quickEditProduct = item
                        },
                        onDelete = {
                            viewModel.deleteProductFromAdmin(item.id)
                            Toast.makeText(context, "Deleted '${item.title}'", Toast.LENGTH_SHORT).show()
                        },
                        onStockAdjust = { delta ->
                            val updatedQty = (item.stockQuantity + delta).coerceAtLeast(0)
                            val updatedProd = item.copy(stockQuantity = updatedQty)
                            viewModel.saveProductFromAdmin(updatedProd) {
                                Toast.makeText(context, "Updated stock for '${item.title}' to $updatedQty", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        } else if (activeAdminSubView == "ALERTS") {
            // EXPIRED DRUGS & PRODUCTS ALERT SECTION
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (expiredProducts.isNotEmpty()) Color(0xFFDC2626).copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(1.5.dp, if (expiredProducts.isNotEmpty()) Color(0xFFDC2626) else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = Color(0xFFDC2626), modifier = Modifier.size(32.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("⛔ Expired Drugs & Products Alert (${expiredProducts.size})", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color(0xFFDC2626))
                                Text("Critical health & safety alert. Product names and images displayed in RED. Immediate removal or expiry date update required.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        if (expiredProducts.isEmpty()) {
                            Text("✅ No expired drugs or products in inventory.", fontSize = 12.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
                        } else {
                            expiredProducts.forEach { prod ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(2.dp, Color(0xFFDC2626)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Product Image in RED border
                                        Box(
                                            modifier = Modifier
                                                .size(60.dp)
                                                .border(BorderStroke(2.5.dp, Color(0xFFDC2626)), RoundedCornerShape(10.dp))
                                                .clip(RoundedCornerShape(10.dp))
                                        ) {
                                            AsyncImage(
                                                model = ProductImageHelper.buildImageRequest(
                                                    context = LocalContext.current,
                                                    imageUrl = prod.imageUrl,
                                                    category = prod.category,
                                                    title = prod.title
                                                ),
                                                contentDescription = prod.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            // Product Title in RED
                                            Text(prod.title, fontWeight = FontWeight.ExtraBold, color = Color(0xFFDC2626), fontSize = 13.sp, maxLines = 1)
                                            Text("${prod.category} • ${prod.brand}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("⛔ Status: EXPIRED (${prod.expiryMonthsRemaining}mo) • Remaining: ${prod.stockQuantity} units", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                        }

                                        IconButton(onClick = { quickEditProduct = prod }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Expiry", tint = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(onClick = {
                                            viewModel.deleteProductFromAdmin(prod.id)
                                            Toast.makeText(context, "Removed expired product '${prod.title}'", Toast.LENGTH_SHORT).show()
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Expired", tint = Color(0xFFDC2626))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SHORT-DATED PRODUCTS ALERT SECTION (WITHIN 3 MONTHS)
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (shortDatedProducts.isNotEmpty()) Color(0xFFDC2626).copy(alpha = 0.06f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(1.5.dp, if (shortDatedProducts.isNotEmpty()) Color(0xFFDC2626) else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = Color(0xFFDC2626), modifier = Modifier.size(32.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("⚠️ Short-Dated Products (< 3 Months to Expiry) (${shortDatedProducts.size})", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color(0xFFDC2626))
                                Text("Expiring within 3 months. Displayed with RED image border and RED name. Sell fast with promotion or prepare replacement stock.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        if (shortDatedProducts.isEmpty()) {
                            Text("✅ No short-dated products within 3 months of expiry.", fontSize = 12.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
                        } else {
                            shortDatedProducts.forEach { prod ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(2.dp, Color(0xFFDC2626)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Product Image in RED border
                                        Box(
                                            modifier = Modifier
                                                .size(60.dp)
                                                .border(BorderStroke(2.5.dp, Color(0xFFDC2626)), RoundedCornerShape(10.dp))
                                                .clip(RoundedCornerShape(10.dp))
                                        ) {
                                            AsyncImage(
                                                model = ProductImageHelper.buildImageRequest(
                                                    context = LocalContext.current,
                                                    imageUrl = prod.imageUrl,
                                                    category = prod.category,
                                                    title = prod.title
                                                ),
                                                contentDescription = prod.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            // Product Title in RED
                                            Text(prod.title, fontWeight = FontWeight.ExtraBold, color = Color(0xFFDC2626), fontSize = 13.sp, maxLines = 1)
                                            Text("${prod.category} • ${prod.brand}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("⚠️ Expires in: ${prod.expiryMonthsRemaining} month(s) • Remaining: ${prod.stockQuantity} units", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                        }

                                        IconButton(onClick = { quickEditProduct = prod }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Product", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // LOW STOCK INVENTORY ALERT SECTION (THRESHOLD <= 2)
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (lowStockProducts.isNotEmpty()) Color(0xFFF59E0B).copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(1.5.dp, if (lowStockProducts.isNotEmpty()) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = Color(0xFFF59E0B), modifier = Modifier.size(32.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Inventory, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("📦 Low Stock Alert: Remaining Quantity <= 2 (${lowStockProducts.size})", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color(0xFFB45309))
                                Text("Stock level is at or below the default threshold of 2 units. Remaining quantity is indicated and instant restock is enabled.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        if (lowStockProducts.isEmpty()) {
                            Text("✅ All inventory items have adequate stock (> 2 units).", fontSize = 12.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
                        } else {
                            lowStockProducts.forEach { prod ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AsyncImage(
                                                model = ProductImageHelper.buildImageRequest(
                                                    context = LocalContext.current,
                                                    imageUrl = prod.imageUrl,
                                                    category = prod.category,
                                                    title = prod.title
                                                ),
                                                contentDescription = prod.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(50.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            )

                                            Spacer(modifier = Modifier.width(10.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(prod.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                                Text("${prod.category} • ${prod.brand}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(
                                                    text = if (prod.stockQuantity == 0) "⛔ OUT OF STOCK (0 remaining)" else "⚠️ REMAINING QUANTITY: ONLY ${prod.stockQuantity} UNITS (Threshold <= 2)",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = if (prod.stockQuantity == 0) Color.Black else Color(0xFFEF4444)
                                                )
                                            }

                                            IconButton(onClick = { quickEditProduct = prod }) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit Product", tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            AssistChip(
                                                onClick = {
                                                    val updatedProd = prod.copy(stockQuantity = prod.stockQuantity + 10)
                                                    viewModel.saveProductFromAdmin(updatedProd) {
                                                        Toast.makeText(context, "Restocked '${prod.title}' (+10)", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                label = { Text("+10 Restock", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                            )
                                            AssistChip(
                                                onClick = {
                                                    val updatedProd = prod.copy(stockQuantity = prod.stockQuantity + 25)
                                                    viewModel.saveProductFromAdmin(updatedProd) {
                                                        Toast.makeText(context, "Restocked '${prod.title}' (+25)", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                label = { Text("+25 Restock", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                            )
                                            AssistChip(
                                                onClick = {
                                                    val updatedProd = prod.copy(stockQuantity = prod.stockQuantity + 50)
                                                    viewModel.saveProductFromAdmin(updatedProd) {
                                                        Toast.makeText(context, "Restocked '${prod.title}' (+50)", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                label = { Text("+50 Bulk", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (activeAdminSubView == "ADD_FORM") {
        // RECENTLY UPLOADED / CUSTOM PRODUCTS SECTION (ALWAYS PINNED ABOVE CATEGORY LIST)
        if (customUploadedProducts.isNotEmpty()) {
            item(key = "add_form_custom_uploaded_section") {
                AdminCategoryExpandableCard(
                    categoryName = "Uploaded Products (${customUploadedProducts.size})",
                    products = customUploadedProducts,
                    onAddProductToCategory = {
                        resetForm()
                        isEditing = false
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
                    onQuickAddSave = { newProd ->
                        val stamped = newProd.copy(createdAt = System.currentTimeMillis())
                        viewModel.saveProductFromAdminWithStatus(stamped) { synced, err ->
                            val msg = if (synced) "✅ Added & Synced '${stamped.title}'!" else "⚠️ Added locally (${err ?: "Link Supabase"})"
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    onEdit = { item ->
                        isEditing = true
                        editingId = item.id
                        title = item.title
                        category = item.category
                        brand = item.brand
                        availableBrands = item.availableBrands.joinToString(", ")
                        availableSizes = item.availableSizes.joinToString(", ")
                        selectedColors = item.availableColors
                        basePrice = item.basePrice.toInt().toString()
                        costPrice = item.costPrice.toInt().toString()
                        quantity = item.stockQuantity.toString()
                        discountPercent = item.discountPercent.toString()
                        imageUrl = item.imageUrl
                        galleryUrls = item.galleryUrls
                        description = item.description
                        coroutineScope.launch { listState.animateScrollToItem(0) }
                        Toast.makeText(context, "Loaded '${item.title}' into Edit Form at top", Toast.LENGTH_SHORT).show()
                    },
                    onDelete = { item ->
                        viewModel.deleteProductFromAdmin(item.id)
                        Toast.makeText(context, "Deleted '${item.title}' from local DB & Supabase Cloud", Toast.LENGTH_SHORT).show()
                    },
                    onStockAdjust = { item, delta ->
                        val updatedQty = (item.stockQuantity + delta).coerceAtLeast(0)
                        val updatedProd = item.copy(stockQuantity = updatedQty)
                        viewModel.saveProductFromAdmin(updatedProd) {
                            Toast.makeText(context, "Updated stock for '${item.title}' to $updatedQty", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }

        // INVENTORY LIST HEADER
        item(key = "add_form_inventory_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Expandable Product Catalog (${products.size} Items)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Across 7 product categories",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = {
                        viewModel.seed100Products { count ->
                            Toast.makeText(context, "Loaded $count catalog products across all 7 categories!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Load 100 Catalog Items", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // EXPANDABLE CATALOG SECTIONS REFLECTING ALL 7 PRODUCT CATEGORIES
        ProductCategory.values().forEach { cat ->
            val catProducts = products.filter { it.category.equals(cat.displayName, ignoreCase = true) }
            item(key = "add_form_cat_${cat.name}") {
                AdminCategoryExpandableCard(
                    categoryName = cat.displayName,
                    products = catProducts,
                    onAddProductToCategory = { catName ->
                        resetForm()
                        category = catName
                        isEditing = false
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                        Toast.makeText(context, "Prefilled creation form for '$catName' category at top", Toast.LENGTH_SHORT).show()
                    },
                    onQuickAddSave = { newProd ->
                        val stamped = newProd.copy(createdAt = System.currentTimeMillis())
                        viewModel.saveProductFromAdminWithStatus(stamped) { synced, err ->
                            val msg = if (synced) {
                                "✅ Added & Synced '${stamped.title}' to ${stamped.category}!"
                            } else {
                                "⚠️ Added '${stamped.title}' locally (${err ?: "Link Supabase"})"
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    onEdit = { item ->
                        isEditing = true
                        editingId = item.id
                        title = item.title
                        category = item.category
                        brand = item.brand
                        availableBrands = item.availableBrands.joinToString(", ")
                        availableSizes = item.availableSizes.joinToString(", ")
                        selectedColors = item.availableColors
                        basePrice = item.basePrice.toInt().toString()
                        costPrice = item.costPrice.toInt().toString()
                        quantity = item.stockQuantity.toString()
                        discountPercent = item.discountPercent.toString()
                        imageUrl = item.imageUrl
                        galleryUrls = item.galleryUrls
                        description = item.description

                        // Smooth scroll to top form
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                        Toast.makeText(context, "Loaded '${item.title}' into Edit Form at top", Toast.LENGTH_SHORT).show()
                    },
                    onDelete = { item ->
                        viewModel.deleteProductFromAdmin(item.id)
                        Toast.makeText(context, "Deleted '${item.title}' from local DB & Supabase Cloud", Toast.LENGTH_SHORT).show()
                    },
                    onStockAdjust = { item, delta ->
                        val updatedQty = (item.stockQuantity + delta).coerceAtLeast(0)
                        val updatedProd = item.copy(stockQuantity = updatedQty)
                        viewModel.saveProductFromAdmin(updatedProd) {
                            Toast.makeText(context, "Updated stock for '${item.title}' to $updatedQty", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }

        // UNCATEGORIZED SECTION
        val standardCategoryNames = ProductCategory.values().map { it.displayName.lowercase() }
        val uncategorizedProducts = products.filter {
            it.category.lowercase() !in standardCategoryNames
        }
        if (uncategorizedProducts.isNotEmpty()) {
            item(key = "add_form_uncategorized") {
                AdminCategoryExpandableCard(
                    categoryName = "Uncategorized",
                    products = uncategorizedProducts,
                    onAddProductToCategory = { catName ->
                        resetForm()
                        category = "Other"
                        isEditing = false
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                        Toast.makeText(context, "Prefilled creation form for Uncategorized at top", Toast.LENGTH_SHORT).show()
                    },
                    onQuickAddSave = { newProd ->
                        viewModel.saveProductFromAdmin(newProd) {
                            Toast.makeText(context, "Added '${newProd.title}' to Uncategorized!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onEdit = { item ->
                        isEditing = true
                        editingId = item.id
                        title = item.title
                        category = item.category
                        brand = item.brand
                        availableBrands = item.availableBrands.joinToString(", ")
                        availableSizes = item.availableSizes.joinToString(", ")
                        selectedColors = item.availableColors
                        basePrice = item.basePrice.toInt().toString()
                        costPrice = item.costPrice.toInt().toString()
                        quantity = item.stockQuantity.toString()
                        discountPercent = item.discountPercent.toString()
                        imageUrl = item.imageUrl
                        galleryUrls = item.galleryUrls
                        description = item.description

                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                        Toast.makeText(context, "Loaded '${item.title}' into Edit Form", Toast.LENGTH_SHORT).show()
                    },
                    onDelete = { item ->
                        viewModel.deleteProductFromAdmin(item.id)
                        Toast.makeText(context, "Deleted '${item.title}'", Toast.LENGTH_SHORT).show()
                    },
                    onStockAdjust = { item, delta ->
                        val updatedQty = (item.stockQuantity + delta).coerceAtLeast(0)
                        val updatedProd = item.copy(stockQuantity = updatedQty)
                        viewModel.saveProductFromAdmin(updatedProd) {
                            Toast.makeText(context, "Updated stock for '${item.title}' to $updatedQty", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
        } // End of activeAdminSubView == ADD_FORM
    }
}

@Composable
private fun AdminCategoryExpandableCard(
    categoryName: String,
    products: List<Product>,
    onAddProductToCategory: (String) -> Unit,
    onQuickAddSave: (Product) -> Unit,
    onEdit: (Product) -> Unit,
    onDelete: (Product) -> Unit,
    onStockAdjust: (Product, Int) -> Unit
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(true) }
    var showQuickAddDialog by remember { mutableStateOf(false) }

    val nairaFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "NG")).apply {
            maximumFractionDigits = 0
        }
    }
    val totalStock = products.sumOf { it.stockQuantity }
    val totalEstRevenue = products.sumOf { it.basePrice * it.stockQuantity }
    val totalEstProfit = totalEstRevenue * 0.10

    // Quick Add Dialog state
    var quickTitle by remember { mutableStateOf("") }
    var quickBrand by remember { mutableStateOf("") }
    var quickPrice by remember { mutableStateOf("") }
    var quickQty by remember { mutableStateOf("20") }
    var quickImage by remember { mutableStateOf("") }
    var quickSizes by remember { mutableStateOf("M, L, XL") }

    val quickPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            quickImage = uri.toString()
        }
    }

    if (showQuickAddDialog) {
        AlertDialog(
            onDismissRequest = { showQuickAddDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Product to $categoryName", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quickTitle,
                        onValueChange = { quickTitle = it },
                        label = { Text("Product Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = quickBrand,
                        onValueChange = { quickBrand = it },
                        label = { Text("Brand Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = quickPrice,
                            onValueChange = { quickPrice = it },
                            label = { Text("Price (₦)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = quickQty,
                            onValueChange = { quickQty = it },
                            label = { Text("Stock Qty") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = quickSizes,
                        onValueChange = { quickSizes = it },
                        label = { Text("Available Sizes (csv)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = {
                            quickPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Pick Photo", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (quickImage.isBlank()) "Pick Photo from Gallery" else "Change Selected Photo")
                    }
                    OutlinedTextField(
                        value = quickImage,
                        onValueChange = { quickImage = it },
                        label = { Text("Image URL or Selected Photo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (quickTitle.isNotBlank() && quickPrice.isNotBlank()) {
                            if (quickImage.isBlank()) {
                                Toast.makeText(context, "Please pick a product photo or enter an image URL", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val pVal = quickPrice.toDoubleOrNull() ?: 0.0
                            val qVal = quickQty.toIntOrNull() ?: 10
                            val newProduct = Product(
                                id = UUID.randomUUID().toString(),
                                title = quickTitle.trim(),
                                category = categoryName,
                                brand = quickBrand.ifBlank { "Generic" }.trim(),
                                availableBrands = listOf(quickBrand.ifBlank { "Generic" }.trim()),
                                availableSizes = if (quickSizes.isNotBlank()) quickSizes.split(",").map { it.trim() } else listOf("Standard"),
                                availableColors = listOf(ProductColor("Black", "#000000")),
                                basePrice = pVal,
                                stockQuantity = qVal,
                                imageUrl = quickImage.trim(),
                                description = "New catalog product in $categoryName."
                            )
                            onQuickAddSave(newProduct)
                            showQuickAddDialog = false
                            quickTitle = ""
                            quickBrand = ""
                            quickPrice = ""
                            quickImage = ""
                        }
                    }
                ) {
                    Text("Save to Catalog")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { isExpanded = !isExpanded }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = categoryName.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "${products.size} Products • Total Stock: $totalStock units",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Category Add Button (Prefills form & scrolls to top)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .clickable { onAddProductToCategory(categoryName) }
                            .padding(end = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add to Category", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "+ Add Item",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(10.dp))
                if (products.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "No items currently listed in $categoryName category.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { showQuickAddDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add First $categoryName Product", fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        products.forEach { item ->
                            AdminProductItemCard(
                                product = item,
                                onEdit = { onEdit(item) },
                                onDelete = { onDelete(item) },
                                onStockAdjust = { delta -> onStockAdjust(item, delta) }
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { onAddProductToCategory(categoryName) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add to $categoryName (Top Form)", fontSize = 11.sp)
                            }
                            Button(
                                onClick = { showQuickAddDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Quick Add Dialog", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminProductItemCard(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStockAdjust: (Int) -> Unit,
    adminBargainConfig: AdminBargainConfig = AdminBargainConfig(),
    defaultPromoPercent: Int = 0
) {
    val nairaFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "NG")).apply {
            maximumFractionDigits = 0
        }
    }
    var expanded by remember { mutableStateOf(false) }

    val isExpired = product.expiryMonthsRemaining <= 0
    val isShortDated = product.expiryMonthsRemaining in 1..3
    val isDanger = isExpired || isShortDated
    val isLowStock = product.stockQuantity in 1..2
    val isOutOfStock = product.stockQuantity == 0
    val alertRedColor = Color(0xFFDC2626)
    val itemContext = LocalContext.current
    val isPhotoMissingOrExpired = remember(product.imageUrl) {
        ProductImageHelper.isUnreadableImageUri(itemContext, product.imageUrl)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDanger) alertRedColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = if (isDanger) BorderStroke(1.5.dp, alertRedColor.copy(alpha = 0.7f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Product Image with RED border if expired or short-dated (< 3 months)
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .border(
                            border = if (isDanger || isPhotoMissingOrExpired) BorderStroke(2.5.dp, alertRedColor) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = ProductImageHelper.buildImageRequest(
                            context = itemContext,
                            imageUrl = product.imageUrl,
                            category = product.category,
                            title = product.title
                        ),
                        contentDescription = product.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (isDanger) {
                        Surface(
                            color = alertRedColor,
                            shape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
                            modifier = Modifier.align(Alignment.TopStart)
                        ) {
                            Text(
                                text = if (isExpired) "EXP" else "≤3MO",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    } else if (isPhotoMissingOrExpired) {
                        Surface(
                            color = alertRedColor,
                            shape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
                            modifier = Modifier.align(Alignment.TopStart)
                        ) {
                            Text(
                                text = "FIX PIC",
                                color = Color.White,
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Product Title in RED if expired or short-dated
                        Text(
                            text = product.title,
                            fontWeight = FontWeight.Bold,
                            color = if (isDanger) alertRedColor else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        if (isExpired) {
                            Surface(shape = RoundedCornerShape(6.dp), color = alertRedColor) {
                                Text("⛔ EXPIRED", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        } else if (isShortDated) {
                            Surface(shape = RoundedCornerShape(6.dp), color = alertRedColor) {
                                Text("⚠️ SHORT-DATED (${product.expiryMonthsRemaining}mo)", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        } else if (isOutOfStock) {
                            Surface(shape = RoundedCornerShape(6.dp), color = Color.Black) {
                                Text("OUT OF STOCK", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        } else if (isLowStock) {
                            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFEF4444)) {
                                Text("LOW STOCK: ${product.stockQuantity} LEFT", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                    }

                    // Remaining Quantity & Category Information
                    Text(
                        text = buildString {
                            append("${product.category} • ${product.brand} • ")
                            if (isOutOfStock) {
                                append("⛔ 0 remaining")
                            } else if (isLowStock) {
                                append("⚠️ Low Stock: Only ${product.stockQuantity} remaining")
                            } else {
                                append("Remaining: ${product.stockQuantity} in stock")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isLowStock || isOutOfStock) alertRedColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isLowStock || isOutOfStock) FontWeight.Bold else FontWeight.Normal
                    )

                    if (isPhotoMissingOrExpired) {
                        Text(
                            text = "⚠️ Photo link expired (showing fallback) — Tap Fix Photo",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = alertRedColor
                        )
                    }

                    Text(
                        nairaFormat.format(product.basePrice),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Product", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Product", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Quick Stock Adjusters & Details
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    AssistChip(
                        onClick = { onStockAdjust(5) },
                        label = { Text("+5", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    )
                    AssistChip(
                        onClick = { onStockAdjust(20) },
                        label = { Text("+20 Restock", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    )
                    AssistChip(
                        onClick = { onStockAdjust(1) },
                        label = { Text("+1", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    )
                    AssistChip(
                        onClick = { onStockAdjust(-1) },
                        label = { Text("-1", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(8.dp),
                    border = if (isPhotoMissingOrExpired) BorderStroke(1.2.dp, alertRedColor) else ButtonDefaults.outlinedButtonBorder,
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        imageVector = if (isPhotoMissingOrExpired) Icons.Default.AddPhotoAlternate else Icons.Default.Edit,
                        contentDescription = null,
                        tint = if (isPhotoMissingOrExpired) alertRedColor else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isPhotoMissingOrExpired) "Fix Photo" else "Edit Fields",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPhotoMissingOrExpired) alertRedColor else MaterialTheme.colorScheme.primary
                    )
                }
            }

            // =========================================================================
            // RECTANGULAR ANALYTIC BOX BELOW EACH PRODUCT CATALOG (RESTRICTED ONLY TO ADMIN)
            // Analyzes in writing:
            // - Cost Price & Selling Price
            // - Default Promo Activation %
            // - Price Bargain Default Percentage Activation %
            // - Effect of all these on Cost Price
            // - Resultant Profit at each price level
            // =========================================================================
            val costPrice = product.costPrice
            val basePrice = product.basePrice
            val promoPercent = if (defaultPromoPercent > 0) defaultPromoPercent else product.discountPercent
            val bargainPercent = adminBargainConfig.maxBargainDiscountPercent

            val standardProfit = basePrice - costPrice
            val standardMarginPercent = if (basePrice > 0) (standardProfit / basePrice) * 100.0 else 0.0

            val promoPrice = basePrice * (1.0 - (promoPercent / 100.0))
            val promoProfit = promoPrice - costPrice
            val promoMarginPercent = if (promoPrice > 0) (promoProfit / promoPrice) * 100.0 else 0.0

            val bargainPrice = basePrice * (1.0 - (bargainPercent / 100.0))
            val bargainProfit = bargainPrice - costPrice
            val bargainMarginPercent = if (bargainPrice > 0) (bargainProfit / bargainPrice) * 100.0 else 0.0

            val totalStockValuation = basePrice * product.stockQuantity
            val totalBatchCost = costPrice * product.stockQuantity
            val totalStockProfit = standardProfit * product.stockQuantity
            val tenPercentOfStockProfit = totalStockProfit * 0.10

            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.2.dp, CyberCyan.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth().testTag("admin_catalog_analytics_box_${product.id}")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BarChart, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PRODUCT PRICING & PROFIT ANALYTICS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = CyberCyan
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = AccentGold.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "ADMIN RESTRICTED",
                                color = AccentGold,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Base Selling Price: ${nairaFormat.format(basePrice)}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        Text("Wholesale Cost Price: ${nairaFormat.format(costPrice)}", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(3.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Standard Profit (Base - Cost):", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("+${nairaFormat.format(standardProfit)} (${String.format(Locale.US, "%.1f", standardMarginPercent)}%)", fontWeight = FontWeight.Black, fontSize = 11.sp, color = SuccessGreen)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Default Promo Activation ($promoPercent% off):", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Selling: ${nairaFormat.format(promoPrice)}", fontWeight = FontWeight.SemiBold, fontSize = 10.5.sp, color = Color(0xFFDC2626))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("• Resultant Profit with Promo:", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("+${nairaFormat.format(promoProfit)} (${String.format(Locale.US, "%.1f", promoMarginPercent)}%)", fontWeight = FontWeight.Bold, fontSize = 10.5.sp, color = if (promoProfit >= 0) SuccessGreen else Color(0xFFDC2626))
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Default Bargain Limit ($bargainPercent% cap):", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Selling: ${nairaFormat.format(bargainPrice)}", fontWeight = FontWeight.SemiBold, fontSize = 10.5.sp, color = CyberCyan)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("• Resultant Profit with Bargain:", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("+${nairaFormat.format(bargainProfit)} (${String.format(Locale.US, "%.1f", bargainMarginPercent)}%)", fontWeight = FontWeight.Bold, fontSize = 10.5.sp, color = if (bargainProfit >= 0) SuccessGreen else Color(0xFFDC2626))
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Stock Profit (${product.stockQuantity} units):", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        Text("+${nairaFormat.format(totalStockProfit)}", fontWeight = FontWeight.Black, fontSize = 11.5.sp, color = SuccessGreen)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("10% Reserve from Profit:", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = AccentGold)
                        Text(nairaFormat.format(tenPercentOfStockProfit), fontWeight = FontWeight.Black, fontSize = 11.sp, color = AccentGold)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Analysis: Cost price is ${nairaFormat.format(costPrice)} vs fixed base price of ${nairaFormat.format(basePrice)}, yielding +${nairaFormat.format(standardProfit)} standard margin. Default promo ($promoPercent%) delivers +${nairaFormat.format(promoProfit)} resultant profit. Maximum default bargain ($bargainPercent%) preserves +${nairaFormat.format(bargainProfit)} resultant profit, maintaining positive return above cost price.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 9.5.sp,
                            lineHeight = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))
                Text("SKU: ${product.sku}", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                Text("Available Sizes: ${product.availableSizes.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                Text("Model Type: ${product.model3dType} | Rating: ${product.rating} ⭐", style = MaterialTheme.typography.bodySmall)
                Text("Estimated Profit Margin (10%): ${nairaFormat.format(product.basePrice * 0.10)}", style = MaterialTheme.typography.bodySmall, color = SuccessGreen, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// TAB 2: CUSTOMER FOLDERS, CREDENTIALS & ORDERS
// ==========================================
@Composable
private fun CustomerIdentityTab(
    viewModel: MainViewModel,
    customers: List<Customer>,
    orders: List<Order>,
    onCustomerClick: (Customer) -> Unit,
    onOpenPreOrdersTab: () -> Unit = {}
) {
    val context = LocalContext.current
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val cloudStatus by viewModel.cloudStatusMessage.collectAsState()
    val preOrders by viewModel.preOrderRequests.collectAsState()
    val nairaFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "NG")).apply {
            maximumFractionDigits = 0
        }
    }
    val dateTimeFormat = remember { SimpleDateFormat("dd MMM yyyy 'at' hh:mm a", Locale.getDefault()) }
    val nowMillis = remember(customers.size) { System.currentTimeMillis() }

    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("ALL") } // ALL, RECENT, ACTIVE, DELIVERED, NO_ORDERS, PREORDERS

    val unmatchedPreOrdersCount = remember(preOrders, customers) {
        preOrders.count { req -> customers.none { cust -> matchesCustomerPreOrder(req, cust) } }
    }

    val filteredCustomers = customers.filter { cust ->
        val custOrders = orders.filter { matchesCustomerFolder(it, cust) }
        val custPreOrders = preOrders.filter { matchesCustomerPreOrder(it, cust) }
        val isRecentSignUp = cust.createdAt > 0L && (nowMillis - cust.createdAt) < 172_800_000L

        val matchesSearch = searchQuery.isBlank() ||
                cust.fullName.contains(searchQuery, ignoreCase = true) ||
                cust.phoneNumber.contains(searchQuery, ignoreCase = true) ||
                cust.email.contains(searchQuery, ignoreCase = true) ||
                cust.id.contains(searchQuery, ignoreCase = true) ||
                "CUST-${cust.id.take(6)}".contains(searchQuery, ignoreCase = true) ||
                custOrders.any { it.orderId.contains(searchQuery, ignoreCase = true) } ||
                custPreOrders.any {
                    it.id.contains(searchQuery, ignoreCase = true) ||
                        it.productName.contains(searchQuery, ignoreCase = true) ||
                        it.brandName.contains(searchQuery, ignoreCase = true)
                }

        val matchesFilter = when (filterType) {
            "RECENT" -> isRecentSignUp || custOrders.isEmpty()
            "ACTIVE" -> custOrders.any {
                it.status == OrderStatus.PENDING_VERIFICATION ||
                        it.status == OrderStatus.VERIFIED ||
                        it.status == OrderStatus.CONFIRMED ||
                        it.status == OrderStatus.OUT_FOR_DELIVERY
            }
            "DELIVERED" -> custOrders.any { it.status == OrderStatus.DELIVERED }
            "NO_ORDERS" -> custOrders.isEmpty()
            "PREORDERS" -> custPreOrders.isNotEmpty()
            else -> true
        }

        matchesSearch && matchesFilter
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Customer Folders System Banner Header
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Customer Folders & Credentials",
                                    fontWeight = FontWeight.Black,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "${customers.size} Customer Folder(s) • ${orders.size} Orders • ${preOrders.size} Pre-Orders",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Every registered customer automatically gets a separate folder containing their Sign-Up Credentials (Name, Phone, Password, Address & Timestamp), all Customer Orders & Receipts, and Customer Pre-Orders.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                    )

                    if (unmatchedPreOrdersCount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            onClick = onOpenPreOrdersTab,
                            shape = RoundedCornerShape(10.dp),
                            color = AccentGold.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, AccentGold),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "✨ $unmatchedPreOrdersCount Guest / Direct Pre-Order(s) waiting in Pre-Orders File",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = AccentGold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "OPEN →",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = AccentGold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        enabled = !isRefreshing,
                        onClick = {
                            viewModel.syncAllCloudRecordsNow { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("admin_sync_customers_now_button")
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Syncing Customer Folders & Orders...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sync Customer Folders, Orders & Pre-Orders", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (!cloudStatus.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = cloudStatus!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        // Search Bar for Customer Folders
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Folders by Name, Phone, Order # or Pre-Order") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_customer_folder_search")
            )
        }

        // Filter Pills
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterType == "ALL",
                    onClick = { filterType = "ALL" },
                    label = { Text("All Folders (${customers.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                FilterChip(
                    selected = filterType == "PREORDERS",
                    onClick = { filterType = "PREORDERS" },
                    label = { Text("Has Pre-Orders", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                FilterChip(
                    selected = filterType == "RECENT",
                    onClick = { filterType = "RECENT" },
                    label = { Text("New Sign-Ups", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                FilterChip(
                    selected = filterType == "ACTIVE",
                    onClick = { filterType = "ACTIVE" },
                    label = { Text("Active Orders", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                FilterChip(
                    selected = filterType == "DELIVERED",
                    onClick = { filterType = "DELIVERED" },
                    label = { Text("Delivered", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                FilterChip(
                    selected = filterType == "NO_ORDERS",
                    onClick = { filterType = "NO_ORDERS" },
                    label = { Text("No Orders Yet", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        if (filteredCustomers.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (customers.isEmpty()) "No customer folders created yet. New sign-ups automatically create their own folder here." else "No customer folders match your search criteria.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Separate Customer Folder Cards (Each Customer = 1 Distinct Folder)
        items(filteredCustomers, key = { it.id }) { customer ->
            val customerOrders = orders.filter { matchesCustomerFolder(it, customer) }
            val customerPreOrders = preOrders.filter { matchesCustomerPreOrder(it, customer) }
            val totalSpend = customerOrders.sumOf { it.totalAmount }
            val activeOrders = customerOrders.filter { it.status != OrderStatus.DELIVERED && it.status != OrderStatus.CANCELLED }
            val isNewSignUp = customer.createdAt > 0L && (nowMillis - customer.createdAt) < 172_800_000L
            val isLegacyDemo = customer.id == "cust-01" ||
                customer.email.equals("promiseanyanwu218@gmail.com", ignoreCase = true) ||
                (customer.fullName.equals("Promise Anyanwu", ignoreCase = true) && customer.isAdmin)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("customer_folder_card_${customer.id}")
            ) {
                // Sculpted Protruding Folder Tab Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Surface(
                        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                        color = if (activeOrders.isNotEmpty()) CyberCyan.copy(alpha = 0.18f) else AccentGold.copy(alpha = 0.16f),
                        border = BorderStroke(
                            1.dp,
                            if (activeOrders.isNotEmpty()) CyberCyan.copy(alpha = 0.6f) else AccentGold.copy(alpha = 0.55f)
                        ),
                        modifier = Modifier.clickable { onCustomerClick(customer) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (activeOrders.isNotEmpty()) CyberCyan else AccentGold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "FOLDER #CUST-${customer.id.take(6).uppercase()}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = if (activeOrders.isNotEmpty()) CyberCyan else AccentGold
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        if (isNewSignUp && !isLegacyDemo) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF06B6D4).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "NEW",
                                    color = CyberCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (isLegacyDemo) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFF59E0B).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "DEMO",
                                    color = Color(0xFFB45309),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else if (customer.isAdmin) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AccentGold.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "ADMIN",
                                    color = AccentGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SuccessGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "CUSTOMER",
                                    color = SuccessGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (isLegacyDemo) {
                            TextButton(
                                onClick = {
                                    viewModel.deleteCustomerFromAdmin(customer.id) {
                                        Toast.makeText(context, "Removed old demo customer '${customer.fullName}'.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove Demo Customer", tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Remove Demo", fontSize = 10.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Folder Main Body Card
                Card(
                    onClick = { onCustomerClick(customer) },
                    shape = RoundedCornerShape(topStart = 0.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                    border = BorderStroke(
                        1.2.dp,
                        if (activeOrders.isNotEmpty()) CyberCyan else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                    ),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Customer Name & Quick Communication Icons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = customer.fullName.ifBlank { "Unnamed Customer" },
                                    fontWeight = FontWeight.Black,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = if (customer.createdAt > 0L) "Signed Up: ${dateTimeFormat.format(Date(customer.createdAt))}" else "Signed Up: Legacy / Pre-Sync",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (customer.phoneNumber.isNotBlank()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    IconButton(
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.phoneNumber.trim()}"))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Cannot open dialer: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = "Call Customer", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(
                                        onClick = {
                                            try {
                                                val clean = customer.phoneNumber.replace("+", "").replace(" ", "").replace("-", "")
                                                val waUri = Uri.parse("https://api.whatsapp.com/send?phone=$clean")
                                                context.startActivity(Intent(Intent.ACTION_VIEW, waUri))
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Cannot open WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.Default.Chat, contentDescription = "WhatsApp Customer", modifier = Modifier.size(18.dp), tint = SuccessGreen)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Sign-Up Credentials Preview Strip on Folder Cover
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(14.dp), tint = AccentGold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "SIGN-UP CREDENTIALS",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = AccentGold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Login Phone: ${customer.phoneNumber.ifBlank { "N/A" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (customer.signUpPassword.isNotBlank()) {
                                        "Password / Access Code: ${"•".repeat(customer.signUpPassword.length.coerceIn(6, 10))} (Open folder to reveal & copy)"
                                    } else {
                                        "Password / Access Code: Open folder for account ID & login details"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Orders Summary & Active Status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${customerOrders.size} Order(s) • ${customerPreOrders.size} Pre-Order(s)",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (customerPreOrders.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = AccentGold.copy(alpha = 0.2f),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Text(
                                            text = "✨ ${customerPreOrders.size} Pre-Order Request(s) Inside Folder",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = AccentGold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                if (activeOrders.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = CyberCyan.copy(alpha = 0.15f),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Text(
                                            text = "${activeOrders.size} Active in Progress",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = CyberCyan,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                } else if (customerOrders.isNotEmpty()) {
                                    Text(
                                        text = "All Orders Delivered / Closed",
                                        fontSize = 10.sp,
                                        color = SuccessGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Text(
                                        text = "Registered — Awaiting First Order",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Lifetime Spend",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = nairaFormat.format(totalSpend),
                                    fontWeight = FontWeight.Black,
                                    color = SuccessGreen,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Prominent Open Folder Action Button
                        Button(
                            onClick = { onCustomerClick(customer) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("open_customer_folder_${customer.id}"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "📂 Open Folder (${customerOrders.size} Orders • ${customerPreOrders.size} Pre-Orders)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 3: PAYMENT RECEIPTS CONTROL
// ==========================================
@Composable
private fun PaymentReceiptsTab(
    viewModel: MainViewModel,
    orders: List<Order>
) {
    val context = LocalContext.current
    var filterStatus by remember { mutableStateOf<OrderStatus?>(null) }

    var viewingReceiptImageOrder by remember { mutableStateOf<Order?>(null) }
    var fullZoomReceiptUrl by remember { mutableStateOf<String?>(null) }

    val filteredOrders = if (filterStatus == null) orders else orders.filter { it.status == filterStatus }

    val nairaFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "NG")).apply {
            maximumFractionDigits = 0
        }
    }
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    // Full High-Res Receipt Image Inspector Modal for Admin
    viewingReceiptImageOrder?.let { inspectOrder ->
        Dialog(onDismissRequest = { viewingReceiptImageOrder = null }) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Uploaded Payment Receipt", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        }
                        IconButton(onClick = { viewingReceiptImageOrder = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Order ID: ${inspectOrder.orderId}", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    Text("Customer: ${inspectOrder.customerName} (${inspectOrder.customerPhone})", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Text("Delivery: ${inspectOrder.deliveryAddress}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Total Amount: ${nairaFormat.format(inspectOrder.totalAmount)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                    Text("Sender/Payer: ${inspectOrder.senderName.ifBlank { inspectOrder.customerName }}", style = MaterialTheme.typography.bodySmall, color = CyberCyan, fontWeight = FontWeight.SemiBold)
                    Text("Transfer Ref: ${inspectOrder.paymentReference}", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall)
                    Text("Items: ${inspectOrder.itemsSummary}", style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(alpha = 0.08f))
                            .border(1.5.dp, CyberCyan, RoundedCornerShape(14.dp))
                            .clickable {
                                if (inspectOrder.receiptImageUrl.isNotBlank()) {
                                    fullZoomReceiptUrl = inspectOrder.receiptImageUrl
                                }
                            }
                    ) {
                        if (inspectOrder.receiptImageUrl.isNotBlank()) {
                            AsyncImage(
                                model = ProductImageHelper.buildImageRequest(
                                    context = context,
                                    imageUrl = inspectOrder.receiptImageUrl,
                                    title = "Receipt ${inspectOrder.orderId}",
                                    highRes = true
                                ),
                                contentDescription = "Full Uploaded Payment Receipt Photo (Tap to zoom)",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit,
                                filterQuality = FilterQuality.High
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.Black.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "Tap to Pinch-Zoom",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = AccentGold, modifier = Modifier.size(52.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Payment Reference Recorded", fontWeight = FontWeight.Bold)
                                Text("STAN / Ref: ${inspectOrder.paymentReference}", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (inspectOrder.status == OrderStatus.PENDING_VERIFICATION) {
                            Button(
                                onClick = {
                                    viewModel.updateOrderStatus(inspectOrder.orderId, OrderStatus.CONFIRMED, "Order confirmed & approved by Admin Cashier after receipt review")
                                    Toast.makeText(context, "Order ${inspectOrder.orderId} Confirmed! Status changed to ORDER CONFIRMED.", Toast.LENGTH_SHORT).show()
                                    viewingReceiptImageOrder = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Confirm & Process Order", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = { viewingReceiptImageOrder = null },
                            modifier = Modifier.weight(0.8f)
                        ) {
                            Text("Close Inspector", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    fullZoomReceiptUrl?.let { zoomUrl ->
        ZoomableImageDialog(
            imageUrl = zoomUrl,
            title = "Payment Receipt • ${viewingReceiptImageOrder?.orderId ?: ""}".trimEnd(' ', '•'),
            onDismiss = { fullZoomReceiptUrl = null }
        )
    }

    val isRefreshing by viewModel.isRefreshing.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Live Sync Orders & Receipts Button Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Live Orders & Receipts",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black
            )
            OutlinedButton(
                enabled = !isRefreshing,
                onClick = {
                    viewModel.syncAllCloudRecordsNow { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(36.dp).testTag("admin_sync_orders_now_button")
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(15.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sync Cloud Orders", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filter Chips Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filterStatus == null,
                onClick = { filterStatus = null },
                label = { Text("All (${orders.size})") }
            )
            OrderStatus.values().forEach { st ->
                FilterChip(
                    selected = filterStatus == st,
                    onClick = { filterStatus = st },
                    label = { Text(st.name.replace("_", " ")) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val totalRevenue = orders.sumOf { it.totalAmount }
        val totalProfit = orders.sumOf { it.profit }
        val tenPercentOfTotalProfit = totalProfit * 0.10

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Total Revenue (Sales)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(nairaFormat.format(totalRevenue), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("SUM TOTAL PROFIT (ALL ORDERS)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = SuccessGreen)
                        Text(nairaFormat.format(totalProfit), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = SuccessGreen)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("10% MADE FROM TOTAL PROFIT:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = AccentGold)
                    Text(nairaFormat.format(tenPercentOfTotalProfit), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black, color = AccentGold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredOrders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No payment receipts in this category.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredOrders, key = { it.orderId }) { order ->
                    val paymentColor = when (order.paymentMethod) {
                        PaymentMethod.OPAY -> OPayGreen
                        PaymentMethod.MONIEPOINT -> MoniepointBlue
                        PaymentMethod.POS_TRANSFER -> PosPurple
                        PaymentMethod.CASH_ON_DELIVERY -> CodOrange
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(order.orderId, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = paymentColor.copy(alpha = 0.15f)
                                ) {
                                    Text(order.paymentMethod.title, color = paymentColor, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Customer: ${order.customerName} (${order.customerPhone})", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text("Delivery: ${order.deliveryAddress}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Total Settlement: ${nairaFormat.format(order.totalAmount)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Profit from Order: ${nairaFormat.format(order.profit)}", style = MaterialTheme.typography.bodySmall, color = SuccessGreen, fontWeight = FontWeight.Bold)
                                Text("10% from Profit: ${nairaFormat.format(order.profit * 0.10)}", style = MaterialTheme.typography.bodySmall, color = AccentGold, fontWeight = FontWeight.Bold)
                            }
                            Text("Payment Ref / STAN: ${order.paymentReference}", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)

                            // TOTAL PROFIT MADE FROM EACH ITEM BREAKDOWN
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "TOTAL PROFIT MADE FROM EACH ITEM:",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            color = CyberCyan
                                        )
                                        Text(
                                            text = "+${nairaFormat.format(order.profit)}",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp,
                                            color = SuccessGreen
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    order.itemsSummary.split(";", "\n").filter { it.isNotBlank() }.forEach { line ->
                                        Text(
                                            text = "• ${line.trim()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("10% Made from Order Profit:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = AccentGold)
                                        Text(nairaFormat.format(order.profit * 0.10), fontSize = 11.sp, fontWeight = FontWeight.Black, color = AccentGold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Uploaded Payment Receipt Photo Card Section
                            Card(
                                onClick = { viewingReceiptImageOrder = order },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, if (order.receiptImageUrl.isNotBlank()) SuccessGreen.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        if (order.receiptImageUrl.isNotBlank()) {
                                            Box(
                                                modifier = Modifier
                                                    .size(54.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .border(1.dp, SuccessGreen, RoundedCornerShape(8.dp))
                                            ) {
                                                AsyncImage(
                                                    model = ProductImageHelper.buildImageRequest(
                                                        context = context,
                                                        imageUrl = order.receiptImageUrl,
                                                        title = "Receipt ${order.orderId}"
                                                    ),
                                                    contentDescription = "Receipt Photo Thumbnail",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                )
                                            }
                                        } else {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = AccentGold.copy(alpha = 0.15f),
                                                modifier = Modifier.size(54.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = AccentGold, modifier = Modifier.size(26.dp))
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = if (order.receiptImageUrl.isNotBlank()) "📷 Payment Receipt Photo Attached" else "📄 Transfer Reference Logged",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (order.receiptImageUrl.isNotBlank()) SuccessGreen else MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "Payer: ${order.senderName.ifBlank { order.customerName }} • Ref: ${order.paymentReference.ifBlank { "N/A" }}",
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text("Tap to view full receipt photo", fontSize = 10.sp, color = CyberCyan, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (order.status == OrderStatus.VERIFIED || order.status == OrderStatus.DELIVERED) SuccessGreen.copy(alpha = 0.12f) else AccentGold.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "Status: ${order.status.label}",
                                    color = if (order.status == OrderStatus.VERIFIED || order.status == OrderStatus.DELIVERED) SuccessGreen else AccentGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Action buttons to update receipt status
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewingReceiptImageOrder = order },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("View Receipt", fontSize = 12.sp)
                                }

                                if (order.status == OrderStatus.PENDING_VERIFICATION) {
                                    Button(
                                        onClick = {
                                            viewModel.updateOrderStatus(order.orderId, OrderStatus.CONFIRMED, "Order confirmed & verified by Admin Cashier")
                                            Toast.makeText(context, "Order ${order.orderId} Confirmed! Customer status changed to ORDER CONFIRMED.", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                        modifier = Modifier.weight(1.2f)
                                    ) {
                                        Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Confirm & Process Order", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else if (order.status == OrderStatus.CONFIRMED || order.status == OrderStatus.VERIFIED) {
                                    Button(
                                        onClick = {
                                            viewModel.updateOrderStatus(order.orderId, OrderStatus.OUT_FOR_DELIVERY, "Dispatched via Express Courier")
                                            Toast.makeText(context, "Dispatched for Delivery!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1.2f)
                                    ) {
                                        Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Dispatch", fontSize = 12.sp)
                                    }
                                } else if (order.status == OrderStatus.OUT_FOR_DELIVERY) {
                                    Button(
                                        onClick = {
                                            viewModel.updateOrderStatus(order.orderId, OrderStatus.DELIVERED, "Delivered and Signed")
                                            Toast.makeText(context, "Marked as Delivered!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                        modifier = Modifier.weight(1.2f)
                                    ) {
                                        Text("Delivered", fontSize = 12.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            val adminStoreProfile by viewModel.storeProfile.collectAsState()
                            OutlinedButton(
                                onClick = {
                                    WhatsAppHelper.sendAdminOrderStatusToCustomer(
                                        context = context,
                                        order = order,
                                        storeProfile = adminStoreProfile
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_whatsapp_customer_${order.orderId}")
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("WhatsApp Customer (${order.status.label})", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 4: SUPABASE CLOUD CONTROL
// ==========================================
@Composable
private fun SupabaseCloudTab(
    viewModel: MainViewModel,
    cloudStatusMsg: String?
) {
    val context = LocalContext.current
    val storeProfile by viewModel.storeProfile.collectAsState()
    val liveSupabaseConfig by viewModel.supabaseConfigState.collectAsState()
    val currentConfig = if (liveSupabaseConfig.url.isNotBlank() || liveSupabaseConfig.anonKey.isNotBlank()) {
        liveSupabaseConfig
    } else {
        viewModel.repository.supabaseSync.getConfig()
    }
    val isBusy by viewModel.isSupabaseBusy.collectAsState()
    val isConnected by viewModel.isSupabaseConnected.collectAsState()

    LaunchedEffect(Unit) {
        com.example.util.WebPortalExporter.purgeLegacyApkUrlPref(context)
    }

    val hasValidSaved = !viewModel.repository.supabaseSync.isPlaceholderConfig(currentConfig.url, currentConfig.anonKey)
    var supabaseUrl by remember {
        mutableStateOf(if (hasValidSaved) currentConfig.url else "")
    }
    var supabaseAnonKey by remember {
        mutableStateOf(if (hasValidSaved) currentConfig.anonKey else "")
    }
    LaunchedEffect(liveSupabaseConfig.url, liveSupabaseConfig.anonKey) {
        if (!viewModel.repository.supabaseSync.isPlaceholderConfig(liveSupabaseConfig.url, liveSupabaseConfig.anonKey)) {
            supabaseUrl = liveSupabaseConfig.url
            supabaseAnonKey = liveSupabaseConfig.anonKey
        }
    }
    var testResult by remember {
        mutableStateOf(
            cloudStatusMsg ?: if (hasValidSaved) {
                "Saved Supabase URL ready. Tap 'Link Up Real-Time' to verify live connection."
            } else {
                "Not linked yet. Paste your Supabase Project URL and Publishable / Anon Key below."
            }
        )
    }

    LaunchedEffect(cloudStatusMsg) {
        if (!cloudStatusMsg.isNullOrBlank()) {
            testResult = cloudStatusMsg
        }
    }

    val isErrorStatus = testResult.contains("❌") ||
        testResult.contains("Error", ignoreCase = true) ||
        testResult.contains("Unauthorized", ignoreCase = true) ||
        testResult.contains("Unable to resolve", ignoreCase = true) ||
        testResult.contains("missing", ignoreCase = true) ||
        testResult.contains("Blocked", ignoreCase = true) ||
        testResult.contains("Placeholder", ignoreCase = true) ||
        testResult.contains("Mismatch", ignoreCase = true) ||
        testResult.contains("Secret Key", ignoreCase = true)

    val isSuccessStatus = isConnected || testResult.startsWith("✅")

    val statusTint = when {
        isErrorStatus -> Color(0xFFDC2626)
        isSuccessStatus -> SuccessGreen
        else -> Color(0xFFD97706)
    }
    val badgeText = when {
        isBusy -> "● CHECKING..."
        isSuccessStatus -> "● CONNECTED"
        isErrorStatus -> "● SYNC ERROR"
        else -> "● SETUP REQUIRED"
    }

    val copyToClipboard: (String, String, String) -> Unit = { label, text, toastMsg ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, toastMsg, Toast.LENGTH_LONG).show()
    }

    val normalizeOrExtractCredentials: () -> Pair<String, String> = {
        val rawUrl = supabaseUrl.trim()
        val rawKey = supabaseAnonKey.trim()
        if (rawUrl.contains("sb_url=") || rawUrl.contains("sb_key=")) {
            try {
                val uri = Uri.parse(
                    if (rawUrl.startsWith("http", ignoreCase = true) || rawUrl.startsWith("depros", ignoreCase = true)) {
                        rawUrl
                    } else {
                        "depros://connect?$rawUrl"
                    }
                )
                val extU = uri.getQueryParameter("sb_url")?.trim().orEmpty()
                val extK = uri.getQueryParameter("sb_key")?.trim().orEmpty()
                if (extU.isNotBlank() && extK.isNotBlank()) {
                    viewModel.updateSupabaseConfig(extU, extK)
                } else {
                    viewModel.updateSupabaseConfig(rawUrl, rawKey)
                }
            } catch (_: Exception) {
                viewModel.updateSupabaseConfig(rawUrl, rawKey)
            }
        } else {
            viewModel.updateSupabaseConfig(rawUrl, rawKey)
        }
    }

    val onLinkUpClick: () -> Unit = {
        val (cleanU, cleanK) = normalizeOrExtractCredentials()
        supabaseUrl = cleanU
        supabaseAnonKey = cleanK
        testResult = "🔄 Testing Supabase host, API key & products table..."
        viewModel.testSupabaseConnection { msg ->
            testResult = msg
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    val onSyncAllClick: () -> Unit = {
        val (cleanU, cleanK) = viewModel.updateSupabaseConfig(supabaseUrl, supabaseAnonKey)
        supabaseUrl = cleanU
        supabaseAnonKey = cleanK
        testResult = "🔄 Syncing inventory to Supabase Cloud..."
        Toast.makeText(context, "🔄 Starting cloud inventory sync...", Toast.LENGTH_SHORT).show()
        viewModel.syncAllProductsWithDetails { successCount, totalCount, errorMsg ->
            if (errorMsg != null) {
                testResult = "❌ Sync Error: $errorMsg"
                Toast.makeText(context, "❌ Sync Error: $errorMsg", Toast.LENGTH_LONG).show()
            } else {
                testResult = "✅ Successfully synced $successCount / $totalCount products to Supabase!"
                Toast.makeText(context, "✅ Successfully synced $successCount / $totalCount products!", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            border = BorderStroke(1.dp, statusTint.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Cloud, contentDescription = null, tint = CyberCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Supabase Cloud Board", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusTint.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = badgeText,
                            color = statusTint,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Connect your Supabase project (Project Settings -> Data API URL & Publishable/Anon Key) to sync catalog inventory, customer profiles, wishlists, and orders.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                val pasteFromClipboard: ((String) -> Unit) -> Unit = { onText ->
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clipText = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.trim().orEmpty()
                    if (clipText.isNotBlank()) {
                        onText(clipText)
                        Toast.makeText(context, "Pasted from clipboard!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                    }
                }

                OutlinedTextField(
                    value = supabaseUrl,
                    onValueChange = { supabaseUrl = it },
                    label = { Text("Supabase Project URL") },
                    placeholder = { Text("https://your-project-ref.supabase.co") },
                    trailingIcon = {
                        IconButton(
                            onClick = { pasteFromClipboard { supabaseUrl = it } },
                            modifier = Modifier.testTag("supabase_paste_url_button")
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste Supabase URL", tint = CyberCyan)
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("supabase_url_input")
                )

                val previewUrl = remember(supabaseUrl) {
                    if (supabaseUrl.isBlank()) "" else viewModel.repository.supabaseSync.cleanUrl(supabaseUrl)
                }
                if (previewUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Resolved URL: $previewUrl",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (previewUrl.endsWith(".supabase.co")) CyberCyan else Color(0xFFD97706)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = supabaseAnonKey,
                    onValueChange = { supabaseAnonKey = it },
                    label = { Text("Supabase Publishable / Public Anon Key") },
                    placeholder = { Text("sb_publishable_... or eyJ...") },
                    trailingIcon = {
                        IconButton(
                            onClick = { pasteFromClipboard { supabaseAnonKey = it } },
                            modifier = Modifier.testTag("supabase_paste_key_button")
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste Anon Key", tint = CyberCyan)
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("supabase_key_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        enabled = !isBusy,
                        onClick = {
                            supabaseUrl = ""
                            supabaseAnonKey = ""
                            viewModel.clearSupabaseConfig()
                            testResult = "Cleared. Paste your real Supabase Project URL & Publishable Key."
                            Toast.makeText(context, "Cleared Supabase credentials", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(0.7f)
                            .height(48.dp)
                            .testTag("supabase_clear_button")
                    ) {
                        Text("Clear", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        enabled = !isBusy && (supabaseUrl.isNotBlank() && (supabaseAnonKey.isNotBlank() || supabaseUrl.contains("sb_key="))),
                        onClick = {
                            val (cleanU, cleanK) = normalizeOrExtractCredentials()
                            supabaseUrl = cleanU
                            supabaseAnonKey = cleanK
                            testResult = "✅ Saved URL & Anon Key locally. Tap 'Link Up Real-Time' to verify and sync."
                            Toast.makeText(context, "Supabase URL & Anon Key saved!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(0.95f)
                            .height(48.dp)
                            .testTag("supabase_save_key_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Key", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        enabled = !isBusy,
                        onClick = onLinkUpClick,
                        modifier = Modifier
                            .weight(1.35f)
                            .height(48.dp)
                            .testTag("supabase_link_button")
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(15.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Link Up Real-Time", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = statusTint.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, statusTint.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Connection Status",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = statusTint
                            )
                            TextButton(
                                onClick = {
                                    copyToClipboard("Supabase Status", testResult, "Status message copied!")
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Status", modifier = Modifier.size(13.dp), tint = statusTint)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy", fontSize = 10.sp, color = statusTint)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        SelectionContainer {
                            Text(
                                text = testResult,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isErrorStatus) statusTint else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Action: Push local inventory & sync Customers + Orders bidirectionally
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Sync Inventory, Customers & Orders", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Pushes catalog items and pulls/pushes all customer signups, orders, and payment receipts in real time.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    enabled = !isBusy,
                    onClick = onSyncAllClick,
                    modifier = Modifier.fillMaxWidth().testTag("supabase_sync_all_button")
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.CloudSync, contentDescription = null)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sync All Products to Supabase Cloud")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    enabled = !isBusy,
                    onClick = {
                        val (cleanU, cleanK) = viewModel.updateSupabaseConfig(supabaseUrl, supabaseAnonKey)
                        supabaseUrl = cleanU
                        supabaseAnonKey = cleanK
                        testResult = "🔄 Syncing customers, orders, receipts & catalog..."
                        viewModel.syncAllCloudRecordsNow { msg ->
                            testResult = msg
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth().testTag("supabase_sync_customers_orders_button")
                ) {
                    Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sync Customers, Orders & Receipts Now", fontWeight = FontWeight.Black, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    enabled = !isBusy,
                    onClick = {
                        testResult = "🔄 Syncing inventory locally..."
                        viewModel.syncInventoryLocally { successCount, totalCount ->
                            testResult = "✅ Successfully synced $successCount / $totalCount products locally (Offline Ready)!"
                            Toast.makeText(context, "✅ Successfully synced $successCount products locally!", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("supabase_local_sync_button")
                ) {
                    Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("⚡ Instant Local Sync (Bypass Network)", fontWeight = FontWeight.Bold)
                }
            }
        }

        // UNIFIED QR CODE, DOWNLOAD LINKS & SHARE HUB
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CyberCyan.copy(alpha = 0.08f)),
            border = BorderStroke(1.5.dp, CyberCyan.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val adminStoreProfile by viewModel.storeProfile.collectAsState()
                var customPortalUrl by remember {
                    mutableStateOf(
                        com.example.util.WebPortalExporter.getSavedCustomPortalUrl(context)
                            .ifBlank {
                                com.example.util.WebPortalExporter.normalizePortalUrl(adminStoreProfile.apkDownloadUrl)
                                    .takeUnless { com.example.util.WebPortalExporter.isDirectApkReleaseUrl(it) }
                                    .orEmpty()
                            }
                            .ifBlank { com.example.ui.components.DEPROS_DOWNLOAD_URL }
                    )
                }
                LaunchedEffect(adminStoreProfile.apkDownloadUrl) {
                    val savedPortal = com.example.util.WebPortalExporter.getSavedCustomPortalUrl(context)
                    val cleanFallback = com.example.util.WebPortalExporter.normalizePortalUrl(adminStoreProfile.apkDownloadUrl)
                        .takeUnless { com.example.util.WebPortalExporter.isDirectApkReleaseUrl(it) }
                        .orEmpty()
                    if (customPortalUrl.isBlank()) {
                        customPortalUrl = savedPortal.ifBlank { cleanFallback }.ifBlank { com.example.ui.components.DEPROS_DOWNLOAD_URL }
                    }
                }
                var showWebPortalPreview by remember { mutableStateOf(false) }
                var showShareHubDialog by remember { mutableStateOf(false) }

                val cleanU = remember(supabaseUrl) { viewModel.repository.supabaseSync.cleanUrl(supabaseUrl) }
                val cleanK = remember(supabaseAnonKey) { viewModel.repository.supabaseSync.sanitizeKey(supabaseAnonKey) }
                val hasValidCloud = cleanU.isNotBlank() && cleanK.isNotBlank() &&
                    !viewModel.repository.supabaseSync.isPlaceholderConfig(cleanU, cleanK)

                val savedLiveUrl = com.example.util.WebPortalExporter.normalizePortalUrl(customPortalUrl)
                    .takeIf { it.isNotBlank() && !com.example.util.WebPortalExporter.isDirectApkReleaseUrl(it) }
                    ?: com.example.ui.components.DEPROS_DOWNLOAD_URL

                val directAppLink = remember(cleanU, cleanK, hasValidCloud) {
                    if (hasValidCloud) {
                        viewModel.buildCustomerSyncLink(null)
                            ?: "depros://connect?sb_url=${Uri.encode(cleanU)}&sb_key=${Uri.encode(cleanK)}"
                    } else null
                }

                val liveWebCustomerLink = remember(savedLiveUrl, cleanU, cleanK, hasValidCloud) {
                    if (hasValidCloud && savedLiveUrl.isNotBlank()) {
                        viewModel.buildCustomerSyncLink(savedLiveUrl)
                    } else null
                }

                val qrTarget = liveWebCustomerLink ?: savedLiveUrl
                val activeShareTarget = liveWebCustomerLink ?: savedLiveUrl

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "QR Code, Download Links & Share Hub",
                            fontWeight = FontWeight.Black,
                            color = CyberCyan,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "All ${storeProfile.storeName} download links, share buttons, web portal & QR code in one place.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { showShareHubDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("admin_open_share_hub_button")
                    ) {
                        Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp), tint = CyberCyan)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Full Hub", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Unified Scannable QR Code + Download / Share Buttons Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        QrCodeView(
                            content = qrTarget,
                            modifier = Modifier
                                .size(176.dp)
                                .testTag("admin_qr_code_view")
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (liveWebCustomerLink != null) "QR Target: Live Store Web Link" else "QR Target: 24/7 SHOPPING Direct App Link",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    shareQrCodeImage(context, qrTarget, storeTitle = storeProfile.storeName)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("admin_share_qr_poster_button")
                            ) {
                                Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share QR Poster", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    shareCleanApkFile(context, activeShareTarget)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("admin_send_clean_apk_button")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Send Clean APK", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                enabled = (liveWebCustomerLink ?: directAppLink) != null,
                                onClick = {
                                    val target = liveWebCustomerLink ?: directAppLink
                                    target?.let {
                                        copyToClipboard(
                                            "Customer Cloud Connect Link",
                                            it,
                                            if (liveWebCustomerLink != null) {
                                                "Copied Live Store Web Link!"
                                            } else {
                                                "Copied ${storeProfile.storeName} Direct App Sync Link!"
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("copy_customer_connect_link_button")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (liveWebCustomerLink != null) "Copy Web Link" else "Copy App Link",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                enabled = directAppLink != null,
                                onClick = {
                                    val appLink = directAppLink ?: return@Button
                                    val shareMsg = buildString {
                                        append("🛍️ ${storeProfile.storeName} — Store Live Sync & Catalog\n")
                                        if (storeProfile.phoneNumber.isNotBlank()) append("📞 ${storeProfile.phoneNumber}\n")
                                        if (storeProfile.storeAddress.isNotBlank()) append("📍 ${storeProfile.storeAddress}\n")
                                        append("\n")
                                        if (liveWebCustomerLink != null) {
                                            append("🌐 Open Store Web Portal & App Link:\n$liveWebCustomerLink\n\n")
                                        }
                                        append("📲 Direct App Connect Link:\n$appLink\n\n")
                                        append("💡 Already installed ${storeProfile.storeName}? Copy this message & paste it in the app → Account → Link Now!")
                                    }
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareMsg)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Send Customer Cloud Sync Link"))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("share_customer_connect_link_button")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share Links", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { showWebPortalPreview = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("preview_web_portal_button")
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Preview Portal", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                enabled = directAppLink != null,
                                onClick = {
                                    val target = directAppLink ?: return@OutlinedButton
                                    try {
                                        val testIntent = Intent(Intent.ACTION_VIEW, Uri.parse(target)).apply {
                                            setPackage(context.packageName)
                                            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                        }
                                        context.startActivity(testIntent)
                                    } catch (e: Exception) {
                                        viewModel.applyStoreSyncLinkOrCredentials(target) { _, msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("test_depros_deeplink_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test App Link", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            enabled = hasValidCloud,
                            onClick = {
                                com.example.util.WebPortalExporter.exportAndSharePortalHtml(
                                    context = context,
                                    sbUrl = cleanU,
                                    sbKey = cleanK,
                                    storeName = storeProfile.storeName,
                                    storePhone = storeProfile.phoneNumber,
                                    storeAddress = storeProfile.storeAddress,
                                    storeEmail = storeProfile.supportEmail,
                                    apkDownloadUrl = adminStoreProfile.apkDownloadUrl.ifBlank {
                                        com.example.util.WebPortalExporter.DEFAULT_GITHUB_APK_RELEASE_URL
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("export_web_portal_html_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export Live Web Page (index.html)", fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    com.example.util.WebPortalExporter.openExternalUrl(context, "https://github.com/new")
                                },
                                modifier = Modifier.weight(1f).height(48.dp).testTag("admin_github_pages_button")
                            ) {
                                Text("GitHub Pages", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    com.example.util.WebPortalExporter.openExternalUrl(context, "https://github.com")
                                },
                                modifier = Modifier.weight(1f).height(48.dp).testTag("admin_github_releases_button")
                            ) {
                                Text("GitHub Releases", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CyberCyan.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = "MANUAL GITHUB HOSTING STEPS (\$0 FREE)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = CyberCyan
                                )
                                Text(
                                    text = "1. Export APK from AI Studio & export index.html above.\n" +
                                        "2. Create a public repo on GitHub (tap 'GitHub Pages' above) & upload index.html.\n" +
                                        "3. In GitHub repo → Settings → Pages → Branch: main → Save to get your live https://USER.github.io/REPO/ URL.\n" +
                                        "4. In GitHub repo → Releases → Draft a new release → attach your .apk & copy its download URL into Store & Security below.",
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Save Custom Hosted Web Address (GitHub Pages)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "HOSTED WEB ADDRESS (FROM GITHUB PAGES)",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Black,
                            color = CyberCyan
                        )

                        OutlinedTextField(
                            value = customPortalUrl,
                            onValueChange = { customPortalUrl = it },
                            label = { Text("Your GitHub Pages Web URL (Optional)") },
                            placeholder = { Text("https://yourusername.github.io/247-shopping/") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_live_portal_url_input")
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val rawTrimmed = customPortalUrl.trim()
                                    val hadSavedUrl = com.example.util.WebPortalExporter.getSavedCustomPortalUrl(context).isNotBlank()
                                    val candidate = com.example.util.WebPortalExporter.normalizeWebUrl(rawTrimmed)
                                    when {
                                        rawTrimmed.isBlank() -> {
                                            com.example.util.WebPortalExporter.saveCustomPortalUrl(context, "")
                                            customPortalUrl = ""
                                            val msg = if (hadSavedUrl) {
                                                "Cleared custom URL. Direct app link mode is active."
                                            } else {
                                                "⚠️ Paste your GitHub Pages https://username.github.io/... link first, then tap Save."
                                            }
                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        }
                                        com.example.util.WebPortalExporter.isAiStudioPreviewHost(candidate) -> {
                                            Toast.makeText(
                                                context,
                                                "⚠️ Upload index.html to GitHub Pages and paste THAT https://username.github.io/... link here.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                        candidate.equals("https://github.com", ignoreCase = true) ||
                                            candidate.equals("https://github.com/", ignoreCase = true) ||
                                            candidate.equals("https://github.com/new", ignoreCase = true) -> {
                                            Toast.makeText(
                                                context,
                                                "⚠️ Publish index.html in your GitHub repo (Settings → Pages), then paste the live https://username.github.io/... URL.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                        else -> {
                                            val savedClean = viewModel.savePermanentShareUrl(candidate)
                                            customPortalUrl = savedClean.ifBlank {
                                                com.example.util.WebPortalExporter.getSavedCustomPortalUrl(context)
                                            }
                                            Toast.makeText(
                                                context,
                                                "✅ Saved permanently! QR Codes & Share Hub now auto-fill $customPortalUrl",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text("Save Web Address", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                enabled = customPortalUrl.trim().isNotBlank(),
                                onClick = {
                                    val normalized = com.example.util.WebPortalExporter.normalizePortalUrl(customPortalUrl)
                                        .ifBlank { com.example.ui.components.DEPROS_DOWNLOAD_URL }
                                    val testTarget = if (hasValidCloud) {
                                        viewModel.buildCustomerSyncLink(normalized) ?: normalized
                                    } else normalized
                                    com.example.util.WebPortalExporter.openExternalUrl(context, testTarget)
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text("Test in Browser", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (showWebPortalPreview) {
                    com.example.ui.components.WebPortalPreviewDialog(
                        sbUrl = cleanU,
                        sbKey = cleanK,
                        storeName = storeProfile.storeName,
                        storePhone = storeProfile.phoneNumber,
                        storeAddress = storeProfile.storeAddress,
                        storeEmail = storeProfile.supportEmail,
                        apkDownloadUrl = adminStoreProfile.apkDownloadUrl,
                        onDismiss = { showWebPortalPreview = false }
                    )
                }

                if (showShareHubDialog) {
                    ShareAppDialog(
                        onDismiss = { showShareHubDialog = false },
                        viewModel = viewModel
                    )
                }
            }
        }

        // Action: Copy SQL Schema Script, RLS Policies & Bucket Configurations
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Supabase SQL Setup, RLS & Storage Buckets", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Creates/upgrades all tables (with ADD COLUMN IF NOT EXISTS), applies open sync RLS policies, creates storage buckets, and reloads schema cache.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("1. Tap 'Copy SQL' below & run it once in Supabase SQL Editor", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                        Text("2. Paste your Project URL (https://xxxx.supabase.co) & Publishable Key", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("3. Tap 'Link Up Real-Time' then 'Sync All Products'", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val schema = viewModel.repository.supabaseSync.generateSupabaseSqlSchema()
                            copyToClipboard("Supabase Schema", schema, "Copied Complete Supabase RLS & Upgrade SQL Script!")
                        },
                        modifier = Modifier.weight(1f).testTag("supabase_copy_sql_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy SQL", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://supabase.com/dashboard"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open Web", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                var showFullscreen by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick = { showFullscreen = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("🔍 Open Fullscreen Supabase Manager", fontWeight = FontWeight.Bold)
                }

                if (showFullscreen) {
                    androidx.compose.ui.window.Dialog(
                        onDismissRequest = { showFullscreen = false },
                        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Cloud, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(28.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Fullscreen Supabase Manager", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                                    }
                                    IconButton(onClick = { showFullscreen = false }) {
                                        Icon(Icons.Default.Close, contentDescription = "Close Fullscreen")
                                    }
                                }

                                Text("Direct access to your Supabase PostgreSQL backend, schema upgrade SQL script, RLS policies, and live inventory synchronization.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                OutlinedTextField(
                                    value = supabaseUrl,
                                    onValueChange = { supabaseUrl = it },
                                    label = { Text("Supabase Project URL") },
                                    placeholder = { Text("https://your-project-ref.supabase.co") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = supabaseAnonKey,
                                    onValueChange = { supabaseAnonKey = it },
                                    label = { Text("Supabase Publishable / Public Anon Key") },
                                    placeholder = { Text("sb_publishable_... or eyJ...") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        enabled = !isBusy,
                                        onClick = onLinkUpClick,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Test & Save Connection", fontSize = 11.sp)
                                    }

                                    Button(
                                        enabled = !isBusy,
                                        onClick = onSyncAllClick,
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Sync All Inventory", fontSize = 11.sp)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = statusTint.copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, statusTint.copy(alpha = 0.45f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Status ($badgeText)", fontWeight = FontWeight.Bold, color = statusTint, fontSize = 12.sp)
                                            TextButton(
                                                onClick = { copyToClipboard("Supabase Status", testResult, "Status message copied!") },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Status", modifier = Modifier.size(13.dp), tint = statusTint)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Copy", fontSize = 11.sp, color = statusTint)
                                            }
                                        }
                                        SelectionContainer {
                                            Text(
                                                text = testResult,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = if (isErrorStatus) statusTint else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Text("🛡️ Tables: products, customers, orders (with RLS & Schema Upgrade)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("🪣 Buckets: shop-receipts, product-media, customer-avatars", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Button(
                                    onClick = {
                                        val schema = viewModel.repository.supabaseSync.generateSupabaseSqlSchema()
                                        copyToClipboard("Supabase Schema", schema, "Copied Complete Supabase RLS & Upgrade SQL Script!")
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("📋 Copy Complete SQL & Upgrade Script", fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://supabase.com/dashboard"))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A), contentColor = Color.White),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Icon(Icons.Default.Cloud, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("🚀 Open Supabase Dashboard in Browser", fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.NewReleases, contentDescription = null, tint = AccentGold, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "📱 NATIVE APK IN-APP UPDATE NOTICE",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 13.sp,
                                                color = AccentGold
                                            )
                                        }
                                        Text(
                                            text = "When you make underlying changes to app code or layouts, native APK users receive an automatic 'UPDATE APP' popup notice. Clicking it opens the download portal instantly.",
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 16.sp
                                        )
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = {
                                                    viewModel.checkForAppUpdates()
                                                    Toast.makeText(context, "🔄 Checking for app updates...", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.weight(1f).height(44.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                            ) {
                                                Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Check Updates", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Button(
                                                onClick = {
                                                    viewModel.simulateNewUpdateAvailable()
                                                    Toast.makeText(context, "🚀 Simulated 'UPDATE APP' notice triggered!", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.weight(1.1f).height(44.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                                            ) {
                                                Icon(Icons.Default.NewReleases, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Test Update Notice", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SalesAnalyticsTab(products: List<Product>, orders: List<Order>) {
    val nairaFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "NG")).apply {
            maximumFractionDigits = 0
        }
    }
    val totalRevenue = orders.sumOf { it.totalAmount }
    val totalProfit = orders.sumOf { it.profit }
    val totalOrders = orders.size
    val totalStock = products.sumOf { it.stockQuantity }
    val averageOrderValue = if (totalOrders > 0) totalRevenue / totalOrders else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Financial Performance", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                title = "Total Revenue",
                value = nairaFormat.format(totalRevenue),
                icon = Icons.Default.TrendingUp,
                color = SuccessGreen,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "SUM TOTAL PROFIT",
                value = nairaFormat.format(totalProfit),
                icon = Icons.Default.BarChart,
                color = AccentGold,
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                title = "10% from Profit",
                value = nairaFormat.format(totalProfit * 0.10),
                icon = Icons.Default.Sell,
                color = CyberCyan,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Orders Recv.",
                value = totalOrders.toString(),
                icon = Icons.Default.ReceiptLong,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Inventory Overview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Inventory, contentDescription = null, tint = CyberCyan)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Live Stock Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                AnalyticsRow("Total Catalog Items", products.size.toString())
                AnalyticsRow("Total Units in Stock", totalStock.toString())
                AnalyticsRow("Estimated Inventory Value", nairaFormat.format(products.sumOf { it.basePrice * it.stockQuantity }))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // REAL-TIME PRODUCT CATALOG ANALYTIC TABLE
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF10B981),
                            modifier = Modifier.size(8.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "REAL-TIME PRODUCT ANALYTIC TABLE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = CyberCyan,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SuccessGreen.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "● LIVE UPDATES ACTIVE",
                            color = SuccessGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Inventory Valuation & Stock Table",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Every newly uploaded or modified product automatically updates valuation, margins, and stock metrics here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Table Filters & Search
                var tableSearchQuery by remember { mutableStateOf("") }
                var selectedTableCategory by remember { mutableStateOf<String?>(null) }

                OutlinedTextField(
                    value = tableSearchQuery,
                    onValueChange = { tableSearchQuery = it },
                    placeholder = { Text("Filter table by product name or brand...", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category filter pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedTableCategory == null,
                        onClick = { selectedTableCategory = null },
                        label = { Text("All (${products.size})", fontSize = 11.sp) }
                    )
                    ProductCategory.values().forEach { cat ->
                        val count = products.count { it.category.equals(cat.displayName, ignoreCase = true) }
                        FilterChip(
                            selected = selectedTableCategory == cat.displayName,
                            onClick = {
                                selectedTableCategory = if (selectedTableCategory == cat.displayName) null else cat.displayName
                            },
                            label = { Text("${cat.displayName} ($count)", fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Filtered products for table
                val tableFilteredProducts = remember(products, tableSearchQuery, selectedTableCategory) {
                    products.filter { p ->
                        val matchesCategory = selectedTableCategory == null || p.category.equals(selectedTableCategory, ignoreCase = true)
                        val matchesSearch = tableSearchQuery.isBlank() ||
                            p.title.contains(tableSearchQuery, ignoreCase = true) ||
                            p.brand.contains(tableSearchQuery, ignoreCase = true)
                        matchesCategory && matchesSearch
                    }
                }

                // Table Summary Metrics Bar
                val tableTotalValuation = tableFilteredProducts.sumOf { it.basePrice * it.stockQuantity }
                val tableTotalStock = tableFilteredProducts.sumOf { it.stockQuantity }
                val tableTotalEstProfit = tableTotalValuation * 0.15

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Items", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${tableFilteredProducts.size}", fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Units", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$tableTotalStock", fontWeight = FontWeight.Black, fontSize = 13.sp, color = CyberCyan)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Valuation", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(nairaFormat.format(tableTotalValuation), fontWeight = FontWeight.Black, fontSize = 13.sp, color = AccentGold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("15% Margin", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(nairaFormat.format(tableTotalEstProfit), fontWeight = FontWeight.Black, fontSize = 13.sp, color = SuccessGreen)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Table Rows
                if (tableFilteredProducts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        Text("No uploaded products match the filter.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        tableFilteredProducts.forEach { item ->
                            val itemValuation = item.basePrice * item.stockQuantity
                            val itemEstProfit = itemValuation * 0.15

                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = item.imageUrl,
                                        contentDescription = item.title,
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    )

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = CyberCyan.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = item.category.uppercase(),
                                                    color = CyberCyan,
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }

                                            Text(
                                                text = item.brand,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Available ticked colors preview swatches
                                        if (item.availableColors.isNotEmpty()) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                item.availableColors.take(6).forEach { color ->
                                                    val parsedColor = try {
                                                        Color(android.graphics.Color.parseColor(color.hex))
                                                    } catch (e: Exception) {
                                                        Color.Gray
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .size(10.dp)
                                                            .clip(CircleShape)
                                                            .background(parsedColor)
                                                            .border(0.5.dp, Color.Black.copy(alpha = 0.3f), CircleShape)
                                                    )
                                                }
                                                if (item.availableColors.size > 6) {
                                                    Text("+${item.availableColors.size - 6}", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Metrics Column
                                    Column(horizontalAlignment = Alignment.End) {
                                        // Stock status badge
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = when {
                                                item.stockQuantity == 0 -> Color(0xFFDC2626).copy(alpha = 0.15f)
                                                item.stockQuantity <= 3 -> AccentGold.copy(alpha = 0.15f)
                                                else -> SuccessGreen.copy(alpha = 0.15f)
                                            }
                                        ) {
                                            Text(
                                                text = when {
                                                    item.stockQuantity == 0 -> "OUT OF STOCK"
                                                    item.stockQuantity <= 3 -> "LOW (${item.stockQuantity})"
                                                    else -> "${item.stockQuantity} in stock"
                                                },
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when {
                                                    item.stockQuantity == 0 -> Color(0xFFDC2626)
                                                    item.stockQuantity <= 3 -> AccentGold
                                                    else -> SuccessGreen
                                                },
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Text(
                                            text = "Unit: ${nairaFormat.format(item.basePrice)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Text(
                                            text = "Val: ${nairaFormat.format(itemValuation)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold
                                        )

                                        Text(
                                            text = "+${nairaFormat.format(itemEstProfit)}",
                                            fontSize = 9.5.sp,
                                            color = SuccessGreen,
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

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Order-by-Order Profit Breakdown", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AccentGold.copy(alpha = 0.2f)
            ) {
                Text(
                    text = "${orders.size} ORDERS",
                    color = AccentGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        if (orders.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No orders received yet. Order profit calculations will be shown here in real-time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                orders.forEach { order ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = SuccessGreen.copy(alpha = 0.15f),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.TrendingUp,
                                                contentDescription = null,
                                                tint = SuccessGreen,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = order.orderId,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "${order.customerName} • ${order.paymentMethod.title}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = SuccessGreen.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "+ Profit: ${nairaFormat.format(order.profit)}",
                                            color = SuccessGreen,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "10%: ${nairaFormat.format(order.profit * 0.10)}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentGold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Order Total: ${nairaFormat.format(order.totalAmount)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Status: ${order.status.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AccentGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = "TOTAL PROFIT MADE FROM EACH ITEM:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 9.5.sp,
                                        color = CyberCyan
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    order.itemsSummary.split(";", "\n").filter { it.isNotBlank() }.forEach { line ->
                                        Text(
                                            text = "• ${line.trim()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 10.5.sp,
                                            lineHeight = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = color)
        }
    }
}

@Composable
private fun AnalyticsRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DeliveryOptionsTab(viewModel: MainViewModel) {
    val context = LocalContext.current
    val deliveryOptions by viewModel.deliveryOptions.collectAsState()
    val nairaFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "NG")).apply {
            maximumFractionDigits = 0
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Manage Delivery Conditions & Fees",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Edit delivery titles and fee amounts in real-time. Changes apply instantly at checkout.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        items(deliveryOptions, key = { it.id }) { option ->
            var titleInput by remember(option.title) { mutableStateOf(option.title) }
            var priceInput by remember(option.price) { mutableStateOf(option.price.toInt().toString()) }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = "Condition ID: ${option.id}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("Delivery Condition Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = { priceInput = it },
                        label = { Text("Fee Amount (₦)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val priceVal = priceInput.toDoubleOrNull() ?: 0.0
                            viewModel.updateDeliveryOption(option.id, titleInput, priceVal)
                            Toast.makeText(context, "Delivery Condition Updated Successfully!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Changes", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// ADMIN SECURITY LOCK SCREEN
// ==========================================
@Composable
private fun AdminSecurityLockScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val initialTab by viewModel.adminSelectedTab.collectAsState()
    val isCloudLinked by viewModel.isSupabaseConnected.collectAsState()
    val hasCloudCfg = remember(isCloudLinked) {
        !viewModel.repository.supabaseSync.isPlaceholderConfig()
    }
    var selectedEntryTab by remember(initialTab) { mutableIntStateOf(if (initialTab == 4) 4 else 0) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            border = BorderStroke(1.5.dp, AccentGold.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 430.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = AccentGold.copy(alpha = 0.2f),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Admin Security Lock",
                            tint = AccentGold,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (hasCloudCfg) SuccessGreen.copy(alpha = 0.16f) else Color(0xFFF59E0B).copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, if (hasCloudCfg) SuccessGreen.copy(alpha = 0.5f) else Color(0xFFF59E0B).copy(alpha = 0.5f))
                ) {
                    Text(
                        text = if (hasCloudCfg) "RESTRICTED AREA • SUPABASE CLOUD ACTIVE" else "RESTRICTED AREA • CLOUD SETUP AVAILABLE INSIDE",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        color = if (hasCloudCfg) SuccessGreen else AccentGold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Admin Entry & Supabase Cloud",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = AccentGold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Protected store owner area to enter Supabase URL, upload your Anon Key, and manage product forms, receipts & settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Destination selector inside the restricted Admin entrance
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedEntryTab == 0,
                        onClick = { selectedEntryTab = 0 },
                        leadingIcon = {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = {
                            Text("Admin Panel", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentGold,
                            selectedLabelColor = Color.Black,
                            selectedLeadingIconColor = Color.Black
                        ),
                        modifier = Modifier.weight(1f).testTag("admin_lock_select_panel_chip")
                    )
                    FilterChip(
                        selected = selectedEntryTab == 4,
                        onClick = { selectedEntryTab = 4 },
                        leadingIcon = {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = {
                            Text("Supabase & Key", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberCyan,
                            selectedLabelColor = Color.Black,
                            selectedLeadingIconColor = Color.Black
                        ),
                        modifier = Modifier.weight(1f).testTag("admin_lock_select_supabase_chip")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Admin Security Password") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Security, contentDescription = null, tint = AccentGold) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide Password" else "Show Password"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_password_input")
                )

                Spacer(modifier = Modifier.height(18.dp))

                val canUseBiometrics = remember(context) { BiometricLockHelper.isBiometricAvailable(context) }

                Button(
                    onClick = {
                        val success = viewModel.verifyAdminPassword(passwordInput)
                        if (success) {
                            viewModel.selectAdminTab(selectedEntryTab)
                            Toast.makeText(
                                context,
                                if (selectedEntryTab == 4) "Unlocked! Opening Supabase Cloud..." else "Admin Unlocked Successfully!",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(context, "Invalid Admin Password! Default password is 'admin123'", Toast.LENGTH_LONG).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedEntryTab == 4) CyberCyan else AccentGold
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("unlock_admin_button")
                ) {
                    Icon(
                        imageVector = if (selectedEntryTab == 4) Icons.Default.Cloud else Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedEntryTab == 4) "Unlock & Open Supabase Cloud" else "Unlock Admin Control Room",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                if (canUseBiometrics) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            BiometricLockHelper.authenticateAdmin(
                                context = context,
                                onSuccess = {
                                    viewModel.selectAdminTab(selectedEntryTab)
                                    viewModel.unlockAdminWithBiometric()
                                    Toast.makeText(context, "Admin unlocked with Biometrics!", Toast.LENGTH_SHORT).show()
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("unlock_admin_biometric_button")
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Unlock with Biometrics / Device PIN", fontWeight = FontWeight.Bold, color = CyberCyan)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Tip: Long-press the 24/7 SHOPPING logo badge on the Home front view anytime to open this restricted portal.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = { viewModel.navigateBack() }) {
                    Text("Return to Store", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ==========================================
// TAB 6: PROMO ACTIVATION
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PromoActivationTab(viewModel: MainViewModel) {
    val context = LocalContext.current
    val promo by viewModel.adminPromo.collectAsState()

    var isActive by remember(promo.isActive) { mutableStateOf(promo.isActive) }
    var discountPercentInput by remember(promo.discountPercent) { mutableStateOf(promo.discountPercent.toString()) }
    var titleInput by remember(promo.title) { mutableStateOf(promo.title) }
    var targetCategoryInput by remember(promo.targetCategory) { mutableStateOf(promo.targetCategory ?: "All Products") }
    var durationHours by remember { mutableIntStateOf(24) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    // Lock state for editing inputs: By default, inputs are LOCKED until admin clicks "OPEN & UNLOCK"
    var isEditingUnlocked by remember { mutableStateOf(false) }

    val categoryOptions = listOf("All Products") + ProductCategory.values().map { it.displayName }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isActive) AccentGold.copy(alpha = 0.08f) else Color(0xFF450A0A)
                ),
                border = BorderStroke(2.dp, if (isActive) AccentGold else Color(0xFFEF4444)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_promo_activation_box")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Header with Open / Lock Status Indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isActive && isEditingUnlocked) SuccessGreen.copy(alpha = 0.2f) else if (isActive) AccentGold.copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isEditingUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = if (isEditingUnlocked) SuccessGreen else if (isActive) AccentGold else Color(0xFFEF4444),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "PROMO DISCOUNT OFFER CONTROL",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isEditingUnlocked) SuccessGreen else if (isActive) AccentGold else Color(0xFFEF4444)
                                    ) {
                                        Text(
                                            text = if (isEditingUnlocked) "🔓 UNLOCKED FOR EDITING" else if (isActive) "🔒 PROMO ACTIVE (PERCENTAGE LOCKED)" else "🔒 LOCKED & INACTIVE",
                                            color = if (isEditingUnlocked || !isActive) Color.White else Color.Black,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isEditingUnlocked) "Editing promo parameters active" else if (isActive) "Shoppers get discount (parameters locked)" else "Promo locked for all shoppers",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Switch(
                            checked = isActive,
                            onCheckedChange = { isOpen ->
                                isActive = isOpen
                                if (!isOpen) {
                                    isEditingUnlocked = false
                                }
                                viewModel.togglePromo(isOpen)
                                Toast.makeText(
                                    context,
                                    if (isOpen) "🔓 Promo Discount Campaign OPENED!" else "🔒 Promo Discount Campaign LOCKED & DEACTIVATED!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentGold,
                                checkedTrackColor = AccentGold.copy(alpha = 0.4f),
                                uncheckedThumbColor = Color(0xFFEF4444),
                                uncheckedTrackColor = Color(0xFFEF4444).copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.testTag("promo_active_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Open / Lock Quick Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                isActive = true
                                isEditingUnlocked = true
                                viewModel.togglePromo(true)
                                Toast.makeText(context, "🔓 Promo Discount OPENED & UNLOCKED for editing!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isEditingUnlocked) SuccessGreen else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isEditingUnlocked) Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("OPEN & UNLOCK", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        }

                        Button(
                            onClick = {
                                isActive = false
                                isEditingUnlocked = false
                                viewModel.togglePromo(false)
                                Toast.makeText(context, "🔒 Promo Discount LOCKED & DEACTIVATED!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isActive && !isEditingUnlocked) Color(0xFFEF4444) else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (!isActive && !isEditingUnlocked) Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("LOCK PROMO", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "When activated, every item in the 24/7 SHOPPING store (or selected category) gets this discount percentage automatically calculated at checkout without hardcoded product edits.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Configure Promo Campaign Parameters",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Status banner when fields are locked
                    if (!isEditingUnlocked) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "🔒 PROMO PERCENTAGE IS LOCKED & UNEDITABLE. Click 'OPEN & UNLOCK' above to edit parameters.",
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        enabled = isEditingUnlocked,
                        label = { Text("Promo Campaign Title") },
                        placeholder = { Text("e.g. 24/7 SHOPPING Flash Sale / Holiday Discount") },
                        singleLine = true,
                        trailingIcon = {
                            Icon(
                                imageVector = if (isEditingUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = if (isEditingUnlocked) "Unlocked" else "Locked",
                                tint = if (isEditingUnlocked) SuccessGreen else Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("promo_title_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = discountPercentInput,
                        onValueChange = { discountPercentInput = it },
                        enabled = isEditingUnlocked,
                        label = { Text("Promo Discount Percentage (%)") },
                        placeholder = { Text("e.g. 15") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon = {
                            Icon(
                                imageVector = if (isEditingUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = if (isEditingUnlocked) "Unlocked" else "Locked",
                                tint = if (isEditingUnlocked) SuccessGreen else Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("promo_percent_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Quick Percentage Presets:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(5, 10, 15, 20, 25, 50).forEach { pct ->
                            FilterChip(
                                selected = discountPercentInput == pct.toString(),
                                onClick = { if (isEditingUnlocked) discountPercentInput = pct.toString() },
                                enabled = isEditingUnlocked,
                                label = { Text("$pct%") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Category Scope
                    ExposedDropdownMenuBox(
                        expanded = categoryDropdownExpanded,
                        onExpandedChange = { if (isEditingUnlocked) categoryDropdownExpanded = !categoryDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = targetCategoryInput,
                            onValueChange = {},
                            readOnly = true,
                            enabled = isEditingUnlocked,
                            label = { Text("Target Category Scope") },
                            trailingIcon = {
                                if (isEditingUnlocked) {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        if (isEditingUnlocked) {
                            ExposedDropdownMenu(
                                expanded = categoryDropdownExpanded,
                                onDismissRequest = { categoryDropdownExpanded = false }
                            ) {
                                categoryOptions.forEach { opt ->
                                    DropdownMenuItem(
                                        text = { Text(opt) },
                                        onClick = {
                                            targetCategoryInput = opt
                                            categoryDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Flash Sale Countdown Duration:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        listOf(
                            12 to "12h",
                            24 to "24h",
                            48 to "48h",
                            72 to "72h",
                            168 to "7 Days"
                        ).forEach { (hrs, label) ->
                            FilterChip(
                                selected = durationHours == hrs,
                                onClick = { if (isEditingUnlocked) durationHours = hrs },
                                enabled = isEditingUnlocked,
                                label = { Text(label) }
                            )
                        }
                    }

                    if (promo.isActive && promo.expiresAtMillis > System.currentTimeMillis()) {
                        val remainingHrs = ((promo.expiresAtMillis - System.currentTimeMillis()) / 3_600_000L).coerceAtLeast(0L)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "⏱ Active live countdown expires in ~${remainingHrs}h",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AccentGold
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (!isEditingUnlocked) {
                                Toast.makeText(context, "🔒 Please click 'OPEN & UNLOCK' first before saving or editing!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val pct = discountPercentInput.toIntOrNull() ?: 10
                            val targetCat = if (targetCategoryInput == "All Products") null else targetCategoryInput
                            viewModel.updatePromo(
                                isActive = isActive,
                                discountPercent = pct,
                                title = titleInput.ifBlank { "24/7 SHOPPING Promo Discount" },
                                targetCategory = targetCat,
                                durationHours = durationHours
                            )
                            isEditingUnlocked = false // Automatically lock after saving percentage
                            Toast.makeText(
                                context,
                                "🔒 Saved & Locked! Promo set to $pct% for ${durationHours}h (${targetCat ?: "Storewide"})",
                                Toast.LENGTH_LONG
                            ).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEditingUnlocked) AccentGold else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isEditingUnlocked) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_promo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (isEditingUnlocked) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEditingUnlocked) "SAVE & LOCK PROMO PERCENTAGE" else "🔒 PROMO SAVED & LOCKED",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ==========================================
        // ADMIN PRICE BARGAIN SPECIAL OFFER CONTROL BOX (OPEN & LOCKABLE TYPE)
        // ==========================================
        item {
            val bargainConfig by viewModel.adminBargainConfig.collectAsState()
            var bargainActive by remember(bargainConfig.isActive) { mutableStateOf(bargainConfig.isActive) }
            var defaultAmountInput by remember(bargainConfig.defaultBargainAmount) { mutableStateOf(bargainConfig.defaultBargainAmount.toLong().toString()) }
            var selectedDesignatedCategory by remember(bargainConfig.designatedCategory) { mutableStateOf(bargainConfig.designatedCategory) }
            var maxDiscountInput by remember(bargainConfig.maxBargainDiscountPercent) { mutableStateOf(bargainConfig.maxBargainDiscountPercent.toString()) }
            var minThresholdInput by remember(bargainConfig.minCheckoutThreshold) { mutableStateOf(bargainConfig.minCheckoutThreshold.toLong().toString()) }
            
            // Lock state for editing inputs: By default, inputs are LOCKED until admin clicks "OPEN & UNLOCK"
            var isEditingUnlocked by remember { mutableStateOf(false) }

            val nairaFormat = remember {
                java.text.NumberFormat.getCurrencyInstance(java.util.Locale("en", "NG")).apply {
                    maximumFractionDigits = 0
                }
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (bargainActive) CyberCyan.copy(alpha = 0.08f) else Color(0xFFFEF2F2)
                ),
                border = BorderStroke(2.dp, if (bargainActive) CyberCyan else Color(0xFFEF4444)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_price_bargain_box")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Header with Open / Lock Status Indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (bargainActive && isEditingUnlocked) SuccessGreen.copy(alpha = 0.2f) else if (bargainActive) CyberCyan.copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isEditingUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = if (isEditingUnlocked) SuccessGreen else if (bargainActive) CyberCyan else Color(0xFFEF4444),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "PRICE BARGAIN OFFER CONTROL",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isEditingUnlocked) SuccessGreen else if (bargainActive) CyberCyan else Color(0xFFEF4444)
                                    ) {
                                        Text(
                                            text = if (isEditingUnlocked) "🔓 UNLOCKED FOR EDITING" else if (bargainActive) "🔒 BARGAIN ACTIVE (LIMIT LOCKED)" else "🔒 LOCKED & INACTIVE",
                                            color = Color.White,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isEditingUnlocked) "Editing limit active" else "Percentage limit locked",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Switch(
                            checked = bargainActive,
                            onCheckedChange = { isOpen ->
                                bargainActive = isOpen
                                if (!isOpen) {
                                    isEditingUnlocked = false
                                }
                                viewModel.toggleBargainActive(isOpen)
                                Toast.makeText(
                                    context,
                                    if (isOpen) "🔓 Price Bargain System OPENED!" else "🔒 Price Bargain Box LOCKED & DEACTIVATED!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SuccessGreen,
                                checkedTrackColor = SuccessGreen.copy(alpha = 0.3f),
                                uncheckedThumbColor = Color(0xFFEF4444),
                                uncheckedTrackColor = Color(0xFFEF4444).copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.testTag("bargain_active_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Open / Lock Quick Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                bargainActive = true
                                isEditingUnlocked = true
                                viewModel.toggleBargainActive(true)
                                Toast.makeText(context, "🔓 Bargain Box OPENED & UNLOCKED for editing!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isEditingUnlocked) SuccessGreen else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isEditingUnlocked) Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("OPEN & UNLOCK", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        }

                        Button(
                            onClick = {
                                bargainActive = false
                                isEditingUnlocked = false
                                viewModel.toggleBargainActive(false)
                                Toast.makeText(context, "🔒 Bargain Box LOCKED & DEACTIVATED!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!bargainActive && !isEditingUnlocked) Color(0xFFEF4444) else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (!bargainActive && !isEditingUnlocked) Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("LOCK BARGAIN", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(14.dp))

                    // Status banner when fields are locked
                    if (!isEditingUnlocked) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "🔒 PERCENTAGE IS LOCKED & UNEDITABLE. Click 'OPEN & UNLOCK' above to edit limits.",
                                    color = Color(0xFFDC2626),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // 1. EDITABLE DEFAULT PRICE BARGAIN AMOUNT (₦)
                    Text(
                        text = "💵 Editable Default Price Bargain Amount (₦)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Adjust the default bargain discount amount in Naira (e.g. ₦2,000, ₦5,000, ₦10,000, ₦20,000). The bargain box must be UNLOCKED before editing.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = defaultAmountInput,
                        onValueChange = { defaultAmountInput = it },
                        enabled = isEditingUnlocked,
                        label = { Text("Default Bargain Amount (₦)") },
                        placeholder = { Text("e.g. 5000 or 10000") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon = {
                            Icon(
                                imageVector = if (isEditingUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = if (isEditingUnlocked) "Unlocked" else "Locked",
                                tint = if (isEditingUnlocked) SuccessGreen else Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bargain_default_amount_input")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Quick Preset Amount Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1000L, 2000L, 5000L, 10000L, 15000L, 20000L, 50000L).forEach { amt ->
                            val isChosen = defaultAmountInput == amt.toString()
                            FilterChip(
                                selected = isChosen,
                                onClick = {
                                    if (isEditingUnlocked) {
                                        defaultAmountInput = amt.toString()
                                    } else {
                                        Toast.makeText(context, "Click 'OPEN & UNLOCK' to edit amount", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                label = { Text("₦${java.text.NumberFormat.getIntegerInstance().format(amt)}", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberCyan,
                                    selectedLabelColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. DESIGNATE OFFER TO ANY PRODUCT CATEGORY
                    Text(
                        text = "🎯 Designate Offer to Product Category",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Designate this default bargain offer to ALL products or specifically to ANY single category at will. Active designation: '$selectedDesignatedCategory'.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val allCategoryOptions = listOf("All") + com.example.data.model.ProductCategory.values().map { it.displayName }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allCategoryOptions.forEach { catName ->
                            val isSelectedCat = selectedDesignatedCategory.equals(catName, ignoreCase = true)
                            FilterChip(
                                selected = isSelectedCat,
                                onClick = {
                                    if (isEditingUnlocked) {
                                        selectedDesignatedCategory = catName
                                    } else {
                                        Toast.makeText(context, "Click 'OPEN & UNLOCK' to change designated category", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                label = {
                                    Text(
                                        text = if (catName == "All") "🌟 All Categories" else catName,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelectedCat) FontWeight.Black else FontWeight.Medium
                                    )
                                },
                                leadingIcon = if (isSelectedCat) {
                                    {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberCyan,
                                    selectedLabelColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. MAXIMUM DEFAULT PRICE BARGAIN LIMIT (%)
                    Text(
                        text = "Maximum Default Price Bargain Limit (%)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Set percentage cap limit (e.g. 5%, 10%, 15%, 20%). Prevents excessive discounts beyond profitability.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = maxDiscountInput,
                        onValueChange = { maxDiscountInput = it },
                        enabled = isEditingUnlocked,
                        label = { Text("Default Price Bargain Limit (%)") },
                        placeholder = { Text("e.g. 10.0 or 15.0") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon = {
                            Icon(
                                imageVector = if (isEditingUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = if (isEditingUnlocked) "Unlocked" else "Locked",
                                tint = if (isEditingUnlocked) SuccessGreen else Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bargain_max_discount_input")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Quick Preset Percentage Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(5.0, 10.0, 15.0, 20.0, 25.0).forEach { pct ->
                            val isChosen = maxDiscountInput == pct.toString()
                            FilterChip(
                                selected = isChosen,
                                onClick = {
                                    if (isEditingUnlocked) {
                                        maxDiscountInput = pct.toString()
                                    } else {
                                        Toast.makeText(context, "Click 'OPEN & UNLOCK' to edit percentage", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                label = { Text("${pct.toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberCyan,
                                    selectedLabelColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4. MINIMUM CHECKOUT THRESHOLD (₦)
                    Text(
                        text = "Default Price Bargaining Limit Threshold (₦)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Set minimum checkout total required to unlock price bargaining. Set to ₦0 to allow bargaining on ANY product without minimum restriction!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = minThresholdInput,
                        onValueChange = { minThresholdInput = it },
                        enabled = isEditingUnlocked,
                        label = { Text("Price Bargain Limit Threshold (₦)") },
                        placeholder = { Text("e.g. 0 or 200000") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon = {
                            Icon(
                                imageVector = if (isEditingUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = if (isEditingUnlocked) "Unlocked" else "Locked",
                                tint = if (isEditingUnlocked) SuccessGreen else Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bargain_min_threshold_input")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Quick Threshold Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0L, 50000L, 100000L, 200000L).forEach { th ->
                            val isChosen = minThresholdInput == th.toString()
                            FilterChip(
                                selected = isChosen,
                                onClick = {
                                    if (isEditingUnlocked) {
                                        minThresholdInput = th.toString()
                                    } else {
                                        Toast.makeText(context, "Click 'OPEN & UNLOCK' to edit threshold", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                label = { Text(if (th == 0L) "₦0 (Any Order)" else "₦${java.text.NumberFormat.getIntegerInstance().format(th)}", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberCyan,
                                    selectedLabelColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (!isEditingUnlocked) {
                                Toast.makeText(context, "🔒 Please click 'OPEN & UNLOCK' first before saving or editing!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val defAmt = defaultAmountInput.toDoubleOrNull() ?: 5000.0
                            val maxPct = maxDiscountInput.toDoubleOrNull() ?: 10.0
                            val threshold = minThresholdInput.toDoubleOrNull() ?: 0.0

                            viewModel.updateBargainConfig(
                                isActive = bargainActive,
                                maxDiscountPercent = maxPct,
                                defaultAmount = defAmt,
                                designatedCategory = selectedDesignatedCategory,
                                minThreshold = threshold
                            )
                            isEditingUnlocked = false // Automatically lock after saving
                            Toast.makeText(
                                context,
                                "🔒 Saved & Locked! Default amount of ${nairaFormat.format(defAmt)} designated to '$selectedDesignatedCategory' (Max ${maxPct}% cap, Min: ${nairaFormat.format(threshold)})",
                                Toast.LENGTH_LONG
                            ).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEditingUnlocked) CyberCyan else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isEditingUnlocked) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("save_bargain_config_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (isEditingUnlocked) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEditingUnlocked) "SAVE & LOCK BARGAIN CONFIG" else "🔒 BARGAIN CONFIG SAVED & LOCKED",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ==========================================
        // ADMIN PROMO COUPON CODE GENERATOR
        // ==========================================
        item {
            val coupons by viewModel.promoCoupons.collectAsState()
            var newCodeInput by remember { mutableStateOf("") }
            var newDiscountInput by remember { mutableStateOf("10") }
            var newMinOrderInput by remember { mutableStateOf("0") }
            var newDescInput by remember { mutableStateOf("") }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, AccentGold.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_coupon_manager_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Sell, contentDescription = null, tint = AccentGold, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Promo Coupon Codes (${coupons.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Create custom discount codes customers can apply at checkout.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = newCodeInput,
                        onValueChange = { newCodeInput = it.uppercase().replace(" ", "") },
                        label = { Text("Coupon Code (e.g. SAVE15)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_coupon_code_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newDiscountInput,
                            onValueChange = { newDiscountInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Discount (%)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_coupon_percent_input")
                        )
                        OutlinedTextField(
                            value = newMinOrderInput,
                            onValueChange = { newMinOrderInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Min Order (₦)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_coupon_min_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newDescInput,
                        onValueChange = { newDescInput = it },
                        label = { Text("Campaign Note (Optional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val pct = newDiscountInput.toIntOrNull() ?: 10
                            val minOrd = newMinOrderInput.toDoubleOrNull() ?: 0.0
                            val ok = viewModel.savePromoCoupon(newCodeInput, pct, minOrd, newDescInput)
                            if (ok) {
                                Toast.makeText(context, "Coupon '$newCodeInput' ($pct% OFF) created!", Toast.LENGTH_SHORT).show()
                                newCodeInput = ""
                                newDescInput = ""
                            } else {
                                Toast.makeText(context, "Enter at least 3 characters for code.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_create_coupon_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Coupon Code", fontWeight = FontWeight.Black)
                    }

                    if (coupons.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        coupons.forEach { cp ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${cp.code} • ${cp.discountPercent}% OFF",
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (cp.isActive) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Min Order: ₦${cp.minOrderAmount.toLong()}" + if (cp.description.isNotBlank()) " • ${cp.description}" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = cp.isActive,
                                        onCheckedChange = { viewModel.togglePromoCoupon(cp.code, it) }
                                    )
                                    IconButton(onClick = { viewModel.deletePromoCoupon(cp.code) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Coupon", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 7: ADMIN PRE-ORDERS & CUSTOM REQUESTS
// ==========================================
@Composable
private fun AdminPreOrdersTab(viewModel: MainViewModel) {
    val context = LocalContext.current
    val preOrders by viewModel.preOrderRequests.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val nairaFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "NG")).apply { maximumFractionDigits = 0 }
    }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    LaunchedEffect(Unit) {
        viewModel.syncPreOrdersNow()
    }

    var filterStatus by remember { mutableStateOf<PreOrderStatus?>(null) }
    val filteredPreOrders = remember(preOrders, filterStatus) {
        if (filterStatus == null) preOrders else preOrders.filter { it.status == filterStatus }
    }

    val pendingCount = preOrders.count { it.status == PreOrderStatus.PENDING }
    val processingCount = preOrders.count { it.status == PreOrderStatus.PROCESSING || it.status == PreOrderStatus.CONFIRMED_AVAILABLE }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary Header Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = CyberCyan.copy(alpha = 0.2f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.NewReleases, contentDescription = null, tint = CyberCyan)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Customer Pre-Orders & Special Requests",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Respond, quote prices, and update status for custom requests",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("TOTAL REQUESTS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${preOrders.size}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AccentGold.copy(alpha = 0.15f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("PENDING ACTION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AccentGold)
                                Text("$pendingCount", fontSize = 18.sp, fontWeight = FontWeight.Black, color = AccentGold)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CyberCyan.copy(alpha = 0.15f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("IN PROGRESS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                                Text("$processingCount", fontSize = 18.sp, fontWeight = FontWeight.Black, color = CyberCyan)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        enabled = !isRefreshing,
                        onClick = {
                            viewModel.syncPreOrdersNow { _, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("admin_sync_preorders_button")
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Syncing Pre-Orders from Cloud...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sync Customer Pre-Orders from Cloud Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterStatus == null,
                    onClick = { filterStatus = null },
                    label = { Text("All (${preOrders.size})") }
                )
                PreOrderStatus.values().forEach { st ->
                    val cnt = preOrders.count { it.status == st }
                    FilterChip(
                        selected = filterStatus == st,
                        onClick = { filterStatus = if (filterStatus == st) null else st },
                        label = { Text("${st.label} ($cnt)") }
                    )
                }
            }
        }

        if (filteredPreOrders.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.NewReleases, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No pre-order requests match the selected filter.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        items(filteredPreOrders, key = { it.id }) { req ->
            AdminPreOrderItemCard(
                req = req,
                viewModel = viewModel,
                nairaFormat = nairaFormat,
                dateFormat = dateFormat,
                context = context
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminPreOrderItemCard(
    req: PreOrderRequest,
    viewModel: MainViewModel,
    nairaFormat: NumberFormat,
    dateFormat: SimpleDateFormat,
    context: Context
) {
    var selectedStatus by remember(req) { mutableStateOf(req.status) }
    var quotedPriceInput by remember(req) { mutableStateOf(if (req.adminQuotedPrice > 0) req.adminQuotedPrice.toInt().toString() else "") }
    var adminResponseText by remember(req) { mutableStateOf(req.adminResponse) }
    var statusDropdownExpanded by remember { mutableStateOf(false) }

    val statusColor = when (selectedStatus) {
        PreOrderStatus.CONFIRMED_AVAILABLE -> SuccessGreen
        PreOrderStatus.PROCESSING -> CyberCyan
        PreOrderStatus.PENDING -> AccentGold
        PreOrderStatus.COMPLETED -> SuccessGreen
        PreOrderStatus.DECLINED -> Color(0xFFEF4444)
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.5.dp, statusColor),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Product Name & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = req.productName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Brand: ${req.brandName} • Category: ${req.category}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = req.status.label,
                        color = statusColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(10.dp))

            // Specs Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (req.preferredSize.isNotBlank()) {
                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text("Size: ${req.preferredSize}", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                    }
                }
                if (req.preferredColor.isNotBlank()) {
                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text("Color: ${req.preferredColor}", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                    }
                }
                if (req.targetBudget > 0) {
                    Surface(shape = RoundedCornerShape(6.dp), color = AccentGold.copy(alpha = 0.15f)) {
                        Text("Target: ${nairaFormat.format(req.targetBudget)}", fontSize = 11.sp, fontWeight = FontWeight.Black, color = AccentGold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                    }
                }
            }

            if (req.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Customer Details: \"${req.description}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (req.photoUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(req.photoUrl)).apply {
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open attached media", Toast.LENGTH_SHORT).show()
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        AsyncImage(
                            model = req.photoUrl,
                            contentDescription = "Attached media",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play/View Media", tint = Color.White)
                            }
                        }
                    }
                }
            }

            if (req.customerReply.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AccentGold.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = AccentGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("CUSTOMER FEEDBACK REPLY", fontWeight = FontWeight.Black, fontSize = 10.5.sp, color = AccentGold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(req.customerReply, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Customer Info & Communication Row
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Customer: ${req.customerName}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Phone: ${req.customerPhone.ifBlank { "Not provided" }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Requested: ${dateFormat.format(Date(req.createdAt))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (req.customerPhone.isNotBlank()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Dial Phone
                            IconButton(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${req.customerPhone.trim()}"))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not open dialer", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = "Call Customer", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }

                            // WhatsApp Customer
                            IconButton(
                                onClick = {
                                    try {
                                        val clean = req.customerPhone.replace("+", "").replace(" ", "").replace("-", "")
                                        val waUri = Uri.parse("https://api.whatsapp.com/send?phone=$clean&text=${Uri.encode("Hello ${req.customerName}, regarding your pre-order request for ${req.productName} (${req.brandName}).")}")
                                        context.startActivity(Intent(Intent.ACTION_VIEW, waUri))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not open WhatsApp", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = "WhatsApp Customer", tint = SuccessGreen, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Admin Response Form
            Text(
                text = "ADMIN RESPONSE & PRICE QUOTATION",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                // Status Dropdown
                ExposedDropdownMenuBox(
                    expanded = statusDropdownExpanded,
                    onExpandedChange = { statusDropdownExpanded = !statusDropdownExpanded },
                    modifier = Modifier.weight(1.2f)
                ) {
                    OutlinedTextField(
                        value = selectedStatus.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Status") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = statusDropdownExpanded,
                        onDismissRequest = { statusDropdownExpanded = false },
                        containerColor = Color.White
                    ) {
                        PreOrderStatus.entries.forEach { st ->
                            val isSelected = selectedStatus == st
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = st.label,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                        color = if (isSelected) Color(0xFFB91C1C) else Color(0xFF111827)
                                    )
                                },
                                onClick = {
                                    selectedStatus = st
                                    statusDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Quoted Price Input
                OutlinedTextField(
                    value = quotedPriceInput,
                    onValueChange = { quotedPriceInput = it },
                    label = { Text("Quoted Price (₦)") },
                    placeholder = { Text("e.g. 85000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = adminResponseText,
                onValueChange = { adminResponseText = it },
                label = { Text("Admin Note / Response to Customer") },
                placeholder = { Text("e.g. We have located this Gucci pair! It will arrive in 2 business days.") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    val price = quotedPriceInput.toDoubleOrNull() ?: 0.0
                    viewModel.updatePreOrderResponse(
                        requestId = req.id,
                        status = selectedStatus,
                        adminResponseText = adminResponseText,
                        quotedPrice = price
                    )
                    Toast.makeText(context, "✅ Response saved & sent to customer!", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("SAVE & RESPOND TO CUSTOMER", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
            }
        }
    }
}

// ==========================================
// TAB 8: STORE PROFILE & ADMIN SECURITY SETTINGS
// ==========================================
@Composable
private fun AdminSecuritySettingsTab(
    viewModel: MainViewModel,
    onNavigateToTab: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val currentAdminPassword by viewModel.adminPassword.collectAsState()
    val storeProfile by viewModel.storeProfile.collectAsState()
    val isCloudLinked by viewModel.isSupabaseConnected.collectAsState()
    val isCloudBusy by viewModel.isSupabaseBusy.collectAsState()
    val hasCloudCfg = remember(isCloudLinked) {
        !viewModel.repository.supabaseSync.isPlaceholderConfig()
    }
    var showRestrictedLinkBox by remember { mutableStateOf(false) }
    var pastedRestrictedSyncLink by remember { mutableStateOf("") }

    var storeNameInput by remember(storeProfile.storeName) { mutableStateOf(storeProfile.storeName) }
    var taglineInput by remember(storeProfile.tagline) { mutableStateOf(storeProfile.tagline) }
    var phoneInput by remember(storeProfile.phoneNumber) { mutableStateOf(storeProfile.phoneNumber) }
    var secondaryPhoneInput by remember(storeProfile.secondaryPhoneNumber) { mutableStateOf(storeProfile.secondaryPhoneNumber) }
    var whatsappInput by remember(storeProfile.whatsappNumber) { mutableStateOf(storeProfile.whatsappNumber) }
    var emailInput by remember(storeProfile.supportEmail) { mutableStateOf(storeProfile.supportEmail) }
    var addressInput by remember(storeProfile.storeAddress) { mutableStateOf(storeProfile.storeAddress) }
    var opayNumberInput by remember(storeProfile.opayAccountNumber) { mutableStateOf(storeProfile.opayAccountNumber) }
    var opayNameInput by remember(storeProfile.opayAccountName) { mutableStateOf(storeProfile.opayAccountName) }
    var monieNumberInput by remember(storeProfile.moniepointAccountNumber) { mutableStateOf(storeProfile.moniepointAccountNumber) }
    var monieNameInput by remember(storeProfile.moniepointAccountName) { mutableStateOf(storeProfile.moniepointAccountName) }
    var posTerminalIdInput by remember(storeProfile.posTerminalId) { mutableStateOf(storeProfile.posTerminalId) }
    var posTerminalNameInput by remember(storeProfile.posTerminalName) { mutableStateOf(storeProfile.posTerminalName) }
    var extraBankNameInput by remember(storeProfile.extraBankName) { mutableStateOf(storeProfile.extraBankName) }
    var extraBankNumberInput by remember(storeProfile.extraBankAccountNumber) { mutableStateOf(storeProfile.extraBankAccountNumber) }
    var extraBankHolderInput by remember(storeProfile.extraBankAccountName) { mutableStateOf(storeProfile.extraBankAccountName) }

    val installedApkInfo = remember(viewModel) { viewModel.getInstalledAppVersion() }
    var apkDownloadUrlInput by remember(storeProfile.apkDownloadUrl) {
        mutableStateOf(
            com.example.util.WebPortalExporter.normalizeApkOrPortalUrl(storeProfile.apkDownloadUrl)
                .ifBlank { com.example.util.WebPortalExporter.DEFAULT_GITHUB_APK_RELEASE_URL }
        )
    }
    var latestVersionCodeInput by remember(storeProfile.latestAppVersionCode, installedApkInfo.first) {
        mutableStateOf(maxOf(storeProfile.latestAppVersionCode, installedApkInfo.first).toString())
    }
    var latestVersionNameInput by remember(storeProfile.latestAppVersionName, installedApkInfo.first) {
        val defaultName = if (installedApkInfo.first >= storeProfile.latestAppVersionCode) {
            installedApkInfo.second
        } else {
            storeProfile.latestAppVersionName
        }
        mutableStateOf(defaultName)
    }
    var updateNotesInput by remember(storeProfile.updateReleaseNotes) { mutableStateOf(storeProfile.updateReleaseNotes) }
    var isPublishingUpdate by remember { mutableStateOf(false) }

    var currentPassInput by remember { mutableStateOf("") }
    var newPassInput by remember { mutableStateOf("") }
    var confirmPassInput by remember { mutableStateOf("") }

    var passVisible by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Store Contact, App Email Address, Physical Address & ALL Payment Accounts Editor
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, CyberCyan.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth().testTag("admin_store_profile_editor_card")
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = CyberCyan.copy(alpha = 0.2f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Storefront, contentDescription = null, tint = CyberCyan)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Store Phones, Address, App Email & Accounts",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Edit all phone numbers, real store address, official app email address, and all account numbers in one place.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider()

                    OutlinedTextField(
                        value = storeNameInput,
                        onValueChange = { storeNameInput = it },
                        label = { Text("Store Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("admin_store_name_input")
                    )

                    OutlinedTextField(
                        value = taglineInput,
                        onValueChange = { taglineInput = it },
                        label = { Text("Store Slogan / Tagline") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("admin_store_tagline_input")
                    )

                    HorizontalDivider()
                    Text(
                        text = "ALL STORE PHONE NUMBERS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = CyberCyan
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            label = { Text("Primary Phone No.") },
                            placeholder = { Text("+234 ...") },
                            leadingIcon = { Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("admin_store_phone_input")
                        )

                        OutlinedTextField(
                            value = secondaryPhoneInput,
                            onValueChange = { secondaryPhoneInput = it },
                            label = { Text("Hotline / Phone 2") },
                            placeholder = { Text("+234 ...") },
                            leadingIcon = { Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("admin_store_secondary_phone_input")
                        )
                    }

                    OutlinedTextField(
                        value = whatsappInput,
                        onValueChange = { whatsappInput = it },
                        label = { Text("WhatsApp Business Phone No.") },
                        placeholder = { Text("+234 ...") },
                        leadingIcon = { Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("admin_store_whatsapp_input")
                    )

                    HorizontalDivider()
                    Text(
                        text = "OFFICIAL APP EMAIL ADDRESS (ADMIN ONLY)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = AccentGold
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AccentGold.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Write the official email address for the 24/7 SHOPPING app below (customers will never be asked for an email address).",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                label = { Text("App Email Address") },
                                placeholder = { Text("e.g. store@247shopping.com") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("admin_store_email_input")
                            )
                        }
                    }

                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = { addressInput = it },
                        label = { Text("Real Store Address") },
                        placeholder = { Text("Shop number, Street, City, State") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("admin_store_address_input")
                    )

                    HorizontalDivider()
                    Text(
                        text = "ALL PAYMENT ACCOUNT NUMBERS (SHOWN AT CHECKOUT)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = SuccessGreen
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = opayNumberInput,
                            onValueChange = { opayNumberInput = it },
                            label = { Text("OPay Account No.") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("admin_opay_number_input")
                        )
                        OutlinedTextField(
                            value = opayNameInput,
                            onValueChange = { opayNameInput = it },
                            label = { Text("OPay Account Name") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("admin_opay_name_input")
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = monieNumberInput,
                            onValueChange = { monieNumberInput = it },
                            label = { Text("Moniepoint Account No.") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("admin_monie_number_input")
                        )
                        OutlinedTextField(
                            value = monieNameInput,
                            onValueChange = { monieNameInput = it },
                            label = { Text("Moniepoint Account Name") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("admin_monie_name_input")
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = posTerminalIdInput,
                            onValueChange = { posTerminalIdInput = it },
                            label = { Text("POS Terminal ID") },
                            placeholder = { Text("e.g. POS-...") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("admin_pos_terminal_id_input")
                        )
                        OutlinedTextField(
                            value = posTerminalNameInput,
                            onValueChange = { posTerminalNameInput = it },
                            label = { Text("POS Location / Label") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("admin_pos_terminal_name_input")
                        )
                    }

                    OutlinedTextField(
                        value = extraBankNameInput,
                        onValueChange = { extraBankNameInput = it },
                        label = { Text("Additional Bank Name (Optional)") },
                        placeholder = { Text("e.g. Access Bank, Zenith, GTBank, UBA") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("admin_extra_bank_name_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = extraBankNumberInput,
                            onValueChange = { extraBankNumberInput = it },
                            label = { Text("Additional Account No.") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("admin_extra_bank_number_input")
                        )
                        OutlinedTextField(
                            value = extraBankHolderInput,
                            onValueChange = { extraBankHolderInput = it },
                            label = { Text("Additional Account Name") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("admin_extra_bank_holder_input")
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.updateStoreProfile(
                                StoreProfileConfig(
                                    storeName = storeNameInput.trim().ifBlank { "24/7 SHOPPING" },
                                    tagline = taglineInput.trim().ifBlank { "Shopping with ease and comfort" },
                                    phoneNumber = phoneInput.trim(),
                                    secondaryPhoneNumber = secondaryPhoneInput.trim(),
                                    whatsappNumber = whatsappInput.trim(),
                                    supportEmail = emailInput.trim(),
                                    storeAddress = addressInput.trim(),
                                    opayAccountNumber = opayNumberInput.trim(),
                                    opayAccountName = opayNameInput.trim().ifBlank { "24/7 SHOPPING" },
                                    moniepointAccountNumber = monieNumberInput.trim(),
                                    moniepointAccountName = monieNameInput.trim().ifBlank { "24/7 SHOPPING" },
                                    posTerminalId = posTerminalIdInput.trim(),
                                    posTerminalName = posTerminalNameInput.trim(),
                                    extraBankName = extraBankNameInput.trim(),
                                    extraBankAccountNumber = extraBankNumberInput.trim(),
                                    extraBankAccountName = extraBankHolderInput.trim(),
                                    apkDownloadUrl = com.example.util.WebPortalExporter.normalizeWebUrl(apkDownloadUrlInput),
                                    latestAppVersionCode = latestVersionCodeInput.toIntOrNull()?.coerceAtLeast(1) ?: storeProfile.latestAppVersionCode,
                                    latestAppVersionName = latestVersionNameInput.trim().ifBlank { storeProfile.latestAppVersionName },
                                    updateReleaseNotes = updateNotesInput.trim()
                                )
                            )
                            Toast.makeText(
                                context,
                                "✅ All phone numbers, address, app email & accounts updated everywhere!",
                                Toast.LENGTH_LONG
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_store_profile_button")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save All Phones, Address, Email & Accounts", fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        // NATIVE APK RELEASE & UPDATE MANAGER (Push code/design APK updates to downloaded phones)
        item {
            val installedApk = remember(viewModel) { viewModel.getInstalledAppVersion() }
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, SuccessGreen.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth().testTag("admin_apk_update_publisher_card")
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "NATIVE APK RELEASE & UPDATE LINK",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                color = SuccessGreen
                            )
                            Text(
                                text = "Installed build: v${installedApk.second} (Build ${installedApk.first})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "Products, prices, promos & orders already update 100% silently in the background. When you install a newer APK build, tap below to push a live popup & notification to every customer phone for 1-tap app upgrade (Android OS requires 1 customer tap to install new APK code outside Google Play).",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        enabled = !isPublishingUpdate,
                        onClick = {
                            isPublishingUpdate = true
                            latestVersionCodeInput = installedApk.first.toString()
                            latestVersionNameInput = installedApk.second
                            val cleanApkUrl = com.example.util.WebPortalExporter.normalizeApkOrPortalUrl(apkDownloadUrlInput)
                                .ifBlank { com.example.util.WebPortalExporter.DEFAULT_GITHUB_APK_RELEASE_URL }
                            apkDownloadUrlInput = cleanApkUrl
                            viewModel.publishInstalledBuildAsUpdate(
                                releaseNotes = updateNotesInput,
                                apkDownloadUrl = cleanApkUrl
                            ) { _, msg ->
                                isPublishingUpdate = false
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color(0xFF052E16)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("push_installed_version_button")
                    ) {
                        Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isPublishingUpdate) "Pushing Update..." else "Push My Installed Build (v${installedApk.second}) Live Now",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.5.sp
                        )
                    }

                    OutlinedTextField(
                        value = apkDownloadUrlInput,
                        onValueChange = { apkDownloadUrlInput = it },
                        label = { Text("GitHub Releases APK Download URL (https://...)") },
                        placeholder = { Text("https://github.com/USER/REPO/releases/download/v1.0/app.apk") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("admin_apk_download_url_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = latestVersionCodeInput,
                            onValueChange = { latestVersionCodeInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("New Build Code") },
                            placeholder = { Text("e.g. 2") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("admin_apk_version_code_input")
                        )
                        OutlinedTextField(
                            value = latestVersionNameInput,
                            onValueChange = { latestVersionNameInput = it },
                            label = { Text("New Version Name") },
                            placeholder = { Text("e.g. 2.0") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("admin_apk_version_name_input")
                        )
                    }

                    OutlinedTextField(
                        value = updateNotesInput,
                        onValueChange = { updateNotesInput = it },
                        label = { Text("What's New (Shown on Customer Update Popup)") },
                        placeholder = { Text("New design upgrades, faster checkout & bug fixes") },
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("admin_apk_release_notes_input")
                    )

                    Button(
                        enabled = !isPublishingUpdate,
                        onClick = {
                            isPublishingUpdate = true
                            val targetCode = latestVersionCodeInput.toIntOrNull()?.coerceAtLeast(1) ?: 1
                            val cleanApkUrl = com.example.util.WebPortalExporter.normalizeApkOrPortalUrl(apkDownloadUrlInput)
                            if (apkDownloadUrlInput.trim().isNotEmpty() && cleanApkUrl.isEmpty()) {
                                apkDownloadUrlInput = ""
                                Toast.makeText(context, "⚠️ Invalid HTML/link removed from APK URL field.", Toast.LENGTH_SHORT).show()
                            } else if (cleanApkUrl != apkDownloadUrlInput.trim() && cleanApkUrl.isNotEmpty()) {
                                apkDownloadUrlInput = cleanApkUrl
                            }
                            viewModel.publishAppUpdateNotice(
                                versionCode = targetCode,
                                versionName = latestVersionNameInput,
                                releaseNotes = updateNotesInput,
                                apkDownloadUrl = cleanApkUrl
                            ) { _, msg ->
                                isPublishingUpdate = false
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("publish_apk_update_button")
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isPublishingUpdate) "Publishing Update..." else "Publish APK Update to All Customers",
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // Relocated from Customer Account page: Admin Entry & Supabase Cloud Control
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, AccentGold.copy(alpha = 0.65f)),
                colors = CardDefaults.cardColors(containerColor = AccentGold.copy(alpha = 0.09f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_restricted_supabase_entry_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(AccentGold.copy(alpha = 0.22f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Admin Entry and Supabase Cloud",
                                tint = AccentGold,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Admin Entry & Supabase Cloud",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = AccentGold
                            )
                            Text(
                                text = "Protected store owner controls to enter Supabase URL, upload Anon Key, link another device & manage inventory.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (hasCloudCfg) SuccessGreen.copy(alpha = 0.12f) else Color(0xFFF59E0B).copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, if (hasCloudCfg) SuccessGreen.copy(alpha = 0.45f) else Color(0xFFF59E0B).copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (hasCloudCfg) "🟢 Store Cloud Sync Active" else "⚠️ Phone Not Linked to Store Cloud Yet",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (hasCloudCfg) SuccessGreen else AccentGold
                                )
                                Text(
                                    text = if (hasCloudCfg) "Sign-ups, catalog & orders sync live to Supabase Cloud." else "Configure Supabase URL & Anon Key or paste Store Sync Link below.",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            AssistChip(
                                onClick = { showRestrictedLinkBox = !showRestrictedLinkBox },
                                label = { Text(if (showRestrictedLinkBox) "Hide" else "Link Device", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }

                    if (showRestrictedLinkBox) {
                        OutlinedTextField(
                            value = pastedRestrictedSyncLink,
                            onValueChange = { pastedRestrictedSyncLink = it },
                            label = { Text("Paste Store Link or Full WhatsApp Invite") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            enabled = !isCloudBusy && pastedRestrictedSyncLink.isNotBlank(),
                            onClick = {
                                viewModel.applyStoreSyncLinkOrCredentials(pastedRestrictedSyncLink) { ok, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    if (ok) showRestrictedLinkBox = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isCloudBusy) "Linking & Uploading..." else "Connect & Sync Store Cloud Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onNavigateToTab(4) },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color.Black),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.1f)
                                .height(48.dp)
                                .testTag("admin_restricted_supabase_entry_button")
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Supabase & Key", fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { onNavigateToTab(0) },
                            border = BorderStroke(1.2.dp, AccentGold.copy(alpha = 0.7f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGold),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("admin_restricted_panel_entry_button")
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Admin Panel", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "CUSTOMER DOWNLOAD PRIVACY MODE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = AccentGold
                    )
                    Text(
                        text = "Customer installs hide the Admin Control button automatically. Tap below to lock Admin and hide the Control button on this device too (you can always re-open Admin by long-pressing the 24/7 SHOPPING logo badge).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = {
                            viewModel.hideAdminControlForCustomerMode()
                            Toast.makeText(context, "Control button hidden (Customer View active)", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("hide_admin_control_button")
                    ) {
                        Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Hide Control Button (Customer Mode)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = AccentGold.copy(alpha = 0.2f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = AccentGold)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Admin Control Room Security Settings",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Create or update security password for admin access",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = currentPassInput,
                        onValueChange = { currentPassInput = it },
                        label = { Text("Current Admin Password") },
                        singleLine = true,
                        visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("current_admin_password_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = newPassInput,
                        onValueChange = { newPassInput = it },
                        label = { Text("New Security Password") },
                        singleLine = true,
                        visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("new_admin_password_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = confirmPassInput,
                        onValueChange = { confirmPassInput = it },
                        label = { Text("Confirm New Security Password") },
                        singleLine = true,
                        visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passVisible = !passVisible }) {
                                Icon(
                                    imageVector = if (passVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passVisible) "Hide Password" else "Show Password"
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("confirm_admin_password_input")
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (currentPassInput.trim() != currentAdminPassword) {
                                Toast.makeText(context, "Current admin password is incorrect!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (newPassInput.isBlank()) {
                                Toast.makeText(context, "New password cannot be empty!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (newPassInput.trim() != confirmPassInput.trim()) {
                                Toast.makeText(context, "New passwords do not match!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            viewModel.setAdminPassword(newPassInput)
                            currentPassInput = ""
                            newPassInput = ""
                            confirmPassInput = ""
                            Toast.makeText(context, "Admin Security Password Updated Successfully!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("update_admin_password_button")
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Update Admin Security Password", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            viewModel.lockAdmin()
                            Toast.makeText(context, "Admin Control Room Locked", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, AccentGold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("lock_admin_control_room_button")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = AccentGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Lock Control Room Now", fontWeight = FontWeight.Bold, color = AccentGold)
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 10: RETURNS, REFUNDS & SUPPORT TICKETS
// ==========================================
@Composable
private fun ReturnsAndSupportAdminTab(viewModel: MainViewModel) {
    val context = LocalContext.current
    val returnRequests by viewModel.returnRequests.collectAsState()
    val supportTickets by viewModel.supportTickets.collectAsState()
    var filterMode by remember { mutableIntStateOf(0) } // 0 = All, 1 = Returns, 2 = Tickets
    var inspectedProofUrl by remember { mutableStateOf<String?>(null) }

    val nairaFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "NG")).apply {
            maximumFractionDigits = 0
        }
    }
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    inspectedProofUrl?.let { proofUrl ->
        ZoomableImageDialog(
            imageUrl = proofUrl,
            title = "Return Proof Evidence",
            category = "Proof",
            onDismiss = { inspectedProofUrl = null }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterMode == 0,
                    onClick = { filterMode = 0 },
                    label = { Text("All (${returnRequests.size + supportTickets.size})") }
                )
                FilterChip(
                    selected = filterMode == 1,
                    onClick = { filterMode = 1 },
                    leadingIcon = { Icon(Icons.Default.AssignmentReturn, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("Returns & Refunds (${returnRequests.size})") }
                )
                FilterChip(
                    selected = filterMode == 2,
                    onClick = { filterMode = 2 },
                    leadingIcon = { Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("Support Tickets (${supportTickets.size})") }
                )
            }
        }

        if (filterMode != 2) {
            item {
                Text(
                    text = "Return & Refund Requests (${returnRequests.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = CyberCyan
                )
            }
            if (returnRequests.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No return or refund requests submitted yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(returnRequests, key = { "ret_${it.id}" }) { req ->
                    var noteInput by remember(req.id, req.adminNote) { mutableStateOf(req.adminNote) }
                    val statusColor = when (req.status) {
                        ReturnRefundStatus.PENDING_REVIEW -> AccentGold
                        ReturnRefundStatus.APPROVED_RETURN -> CyberCyan
                        ReturnRefundStatus.REFUNDED -> SuccessGreen
                        ReturnRefundStatus.DECLINED -> Color(0xFFEF4444)
                    }
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${req.id} • Order #${req.orderId.take(8)}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = statusColor.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = req.status.label,
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Customer: ${req.customerName} (${req.customerPhone.ifBlank { "No phone" }})",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Amount: ${nairaFormat.format(req.refundAmount)} • Submitted ${dateFormat.format(Date(req.createdAt))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentGold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Reason: ${req.reason}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (req.details.isNotBlank()) {
                                Text(
                                    text = req.details,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (req.proofImageUrl.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { inspectedProofUrl = req.proofImageUrl }
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(6.dp)
                                ) {
                                    AsyncImage(
                                        model = req.proofImageUrl,
                                        contentDescription = "Customer return proof photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Tap to inspect proof photo",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = CyberCyan
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = noteInput,
                                onValueChange = { noteInput = it },
                                label = { Text("Admin Resolution Note") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.updateReturnRefundStatus(
                                            requestId = req.id,
                                            status = ReturnRefundStatus.APPROVED_RETURN,
                                            adminNote = noteInput.ifBlank { "Return approved — please ship item back." }
                                        )
                                        Toast.makeText(context, "Approved return ${req.id}", Toast.LENGTH_SHORT).show()
                                    }
                                ) { Text("Approve Return", fontSize = 11.sp) }
                                Button(
                                    onClick = {
                                        viewModel.updateReturnRefundStatus(
                                            requestId = req.id,
                                            status = ReturnRefundStatus.REFUNDED,
                                            adminNote = noteInput.ifBlank { "Refund processed successfully." }
                                        )
                                        Toast.makeText(context, "Marked ${req.id} as Refunded", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                                ) { Text("Mark Refunded", color = Color.White, fontSize = 11.sp) }
                                OutlinedButton(
                                    onClick = {
                                        viewModel.updateReturnRefundStatus(
                                            requestId = req.id,
                                            status = ReturnRefundStatus.DECLINED,
                                            adminNote = noteInput.ifBlank { "Request did not meet return policy criteria." }
                                        )
                                        Toast.makeText(context, "Declined ${req.id}", Toast.LENGTH_SHORT).show()
                                    }
                                ) { Text("Decline", color = Color(0xFFEF4444), fontSize = 11.sp) }
                            }
                        }
                    }
                }
            }
        }

        if (filterMode != 1) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Customer Support Tickets (${supportTickets.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = AccentGold
                )
            }
            if (supportTickets.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No customer support tickets submitted yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(supportTickets, key = { "tkt_${it.id}" }) { ticket ->
                    var replyInput by remember(ticket.id, ticket.adminReply) { mutableStateOf(ticket.adminReply) }
                    val statusColor = when (ticket.status) {
                        SupportTicketStatus.OPEN -> AccentGold
                        SupportTicketStatus.IN_PROGRESS -> CyberCyan
                        SupportTicketStatus.RESOLVED -> SuccessGreen
                    }
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${ticket.id} • ${ticket.subject}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = statusColor.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = ticket.status.label,
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "From: ${ticket.customerName} (${ticket.customerPhone.ifBlank { "No phone" }}) • ${dateFormat.format(Date(ticket.createdAt))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (ticket.orderId.isNotBlank()) {
                                Text(
                                    text = "Order Ref: ${ticket.orderId}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberCyan
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = ticket.message, style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = replyInput,
                                onValueChange = { replyInput = it },
                                label = { Text("Admin Reply to Customer") },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.replySupportTicket(
                                            ticketId = ticket.id,
                                            reply = replyInput.ifBlank { "We are reviewing your request now." },
                                            status = SupportTicketStatus.IN_PROGRESS
                                        )
                                        Toast.makeText(context, "Ticket ${ticket.id} marked In Progress", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Reviewing", fontSize = 11.sp) }
                                Button(
                                    onClick = {
                                        viewModel.replySupportTicket(
                                            ticketId = ticket.id,
                                            reply = replyInput.ifBlank { "Your issue has been resolved. Thank you for shopping with 24/7 Shopping!" },
                                            status = SupportTicketStatus.RESOLVED
                                        )
                                        Toast.makeText(context, "Reply sent & Ticket ${ticket.id} Resolved!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                    modifier = Modifier.weight(1f)
                                ) { Text("Reply & Resolve", color = Color.White, fontSize = 11.sp) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveCallsAdminTab(viewModel: MainViewModel) {
    val callState by viewModel.liveCallState.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B)),
            border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Live Audio & Video Customer Desk",
                            color = AccentGold,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (callState.adminOnline) "ONLINE • Customers see live availability" else "Toggle ON to broadcast Admin availability to customers",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                    }
                    androidx.compose.material3.Switch(
                        checked = callState.adminOnline,
                        onCheckedChange = { viewModel.setAdminCallAvailability(it) }
                    )
                }
                Button(
                    onClick = { viewModel.openLiveCallScreen() },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color(0xFF052E16)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("admin_open_live_studio_button")
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Open Live Audio/Video Studio", fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
            }
        }

        Text(
            text = "Incoming Customer Calls (${callState.incomingRequests.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = CyberCyan
        )

        if (callState.incomingRequests.isEmpty()) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No customer is currently ringing. When a customer taps Live Audio/Video Call, they will appear right here and trigger an instant call alert.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            callState.incomingRequests.forEach { req ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${req.callerName} • Requested ${req.mode.label}",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Topic: ${req.topic}${if (req.callerPhone.isNotBlank()) " • ${req.callerPhone}" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.answerIncomingLiveCall(req, com.example.data.remote.LiveCallMode.AUDIO) },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Answer Audio", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { viewModel.answerIncomingLiveCall(req, com.example.data.remote.LiveCallMode.VIDEO) },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color(0xFF052E16)),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Answer Video", fontSize = 11.5.sp, fontWeight = FontWeight.Black)
                            }
                            IconButton(
                                onClick = { viewModel.declineIncomingLiveCall(req) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF991B1B))
                            ) {
                                Icon(Icons.Default.CallEnd, contentDescription = "Decline", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}


