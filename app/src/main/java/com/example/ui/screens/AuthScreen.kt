package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AppScreen
import com.example.CustomerAccountDestination
import com.example.MainViewModel
import com.example.data.model.OrderStatus
import com.example.data.model.SavedAddress
import com.example.ui.components.DualAxisScrollContainer
import com.example.ui.components.GoogleSignInButton
import com.example.ui.components.ShareAppDialog
import com.example.ui.theme.AccentGold
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.SuccessGreen
import com.example.util.DeliveryZoneHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentCustomer by viewModel.currentCustomer.collectAsState()
    val accountDestination by viewModel.customerAccountDestination.collectAsState()
    val initialAuthTab by viewModel.authTab.collectAsState()
    val isWideDualAxisMode by viewModel.isWideDualAxisMode.collectAsState()
    val allOrders by viewModel.allOrders.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val wishlistCount by viewModel.wishlistCount.collectAsState()
    val preOrders by viewModel.preOrderRequests.collectAsState()
    val supportTickets by viewModel.supportTickets.collectAsState()
    val promoCoupons by viewModel.promoCoupons.collectAsState()
    val loyaltyAccount by viewModel.loyaltyAccount.collectAsState()
    val savedAddresses by viewModel.savedAddresses.collectAsState()
    val isFollowingStore by viewModel.isFollowingStore.collectAsState()

    var selectedTab by remember(initialAuthTab) { mutableIntStateOf(initialAuthTab) }
    var signInPhone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var signUpFullName by remember { mutableStateOf("") }
    var signUpPhone by remember { mutableStateOf("") }
    var signUpPassword by remember { mutableStateOf("") }
    var signUpAddress by remember { mutableStateOf("") }
    var showAddAddressDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }

    val deliveredOrderedCount = remember(allOrders, allProducts) {
        val delivered = allOrders.filter { it.status == OrderStatus.DELIVERED }
        if (delivered.isEmpty()) 0
        else allProducts.count { prod ->
            prod.title.isNotBlank() && delivered.any { ord ->
                ord.itemsSummary.contains(prod.title, ignoreCase = true)
            }
        }
    }

    val topTitle = when (accountDestination) {
        CustomerAccountDestination.MENU -> "Customer Account"
        CustomerAccountDestination.WELCOME_PROFILE -> "WELCOME"
        CustomerAccountDestination.INBOX -> "INBOX"
        CustomerAccountDestination.RATINGS_AND_REVIEWS -> "RATINGS AND REVIEWS"
        CustomerAccountDestination.VOUCHERS -> "VOUCHERS"
        CustomerAccountDestination.FOLLOW_STORE -> "FOLLOW 24/7 SHOPPING"
    }

    BackHandler {
        if (accountDestination != CustomerAccountDestination.MENU) {
            viewModel.selectCustomerAccountDestination(CustomerAccountDestination.MENU)
        } else {
            viewModel.navigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(topTitle, fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (accountDestination != CustomerAccountDestination.MENU) {
                                viewModel.selectCustomerAccountDestination(CustomerAccountDestination.MENU)
                            } else {
                                viewModel.navigateBack()
                            }
                        },
                        modifier = Modifier.testTag("auth_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.navigateToHome() },
                        modifier = Modifier.testTag("auth_home_button")
                    ) {
                        Icon(Icons.Default.Home, contentDescription = "Back to Home Page", tint = CyberCyan)
                    }
                    if (currentCustomer == null) {
                        IconButton(
                            onClick = {
                                selectedTab = 1
                                viewModel.selectCustomerAccountDestination(CustomerAccountDestination.WELCOME_PROFILE)
                            },
                            modifier = Modifier.testTag("auth_signup_shortcut_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = "New Customer Sign Up Shortcut",
                                tint = CyberCyan
                            )
                        }
                    }
                    IconButton(
                        onClick = { showQrDialog = true },
                        modifier = Modifier.testTag("auth_qr_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = "Share App & QR Code",
                            tint = AccentGold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier
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
                    .then(if (isWideDualAxisMode) Modifier else Modifier.widthIn(max = 520.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (accountDestination) {
                    CustomerAccountDestination.MENU -> {
                        // Top Compact Summary (Customer Name & Phone Number ONLY when signed in)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF052E16),
                            border = BorderStroke(1.2.dp, AccentGold.copy(alpha = 0.7f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(AccentGold.copy(alpha = 0.2f))
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = AccentGold, modifier = Modifier.size(26.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = currentCustomer?.fullName ?: "24/7 SHOPPING Customer",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = currentCustomer?.phoneNumber ?: "Tap WELCOME below to Sign In or Sign Up",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AccentGold
                                    )
                                }
                            }
                        }

                        // Required Exact Vertical List Arrangement
                        CustomerAccountMenuItem(
                            title = "WELCOME",
                            subtitle = if (currentCustomer != null) {
                                "${currentCustomer!!.fullName} • ${currentCustomer!!.phoneNumber}"
                            } else {
                                "Sign In / Sign Up (Name & Phone Number)"
                            },
                            icon = Icons.Default.Person,
                            accentColor = AccentGold,
                            onClick = { viewModel.selectCustomerAccountDestination(CustomerAccountDestination.WELCOME_PROFILE) },
                            testTag = "account_menu_welcome"
                        )

                        val inboxCount = allOrders.size + preOrders.size + supportTickets.size
                        CustomerAccountMenuItem(
                            title = "INBOX",
                            subtitle = if (inboxCount > 0) "$inboxCount notification(s) & order/ticket updates" else "Store alerts, order updates & messages",
                            icon = Icons.Default.Inbox,
                            accentColor = CyberCyan,
                            onClick = { viewModel.selectCustomerAccountDestination(CustomerAccountDestination.INBOX) },
                            testTag = "account_menu_inbox"
                        )

                        CustomerAccountMenuItem(
                            title = "ORDERS",
                            subtitle = "${allOrders.size} order(s) • Track receipts & Pre-Orders",
                            icon = Icons.Default.ReceiptLong,
                            accentColor = SuccessGreen,
                            onClick = { viewModel.navigateTo(AppScreen.ORDERS) },
                            testTag = "account_menu_orders"
                        )

                        CustomerAccountMenuItem(
                            title = "WISHLIST",
                            subtitle = "$wishlistCount saved product(s)",
                            icon = Icons.Default.Favorite,
                            accentColor = Color(0xFFEF4444),
                            onClick = { viewModel.navigateTo(AppScreen.WISHLIST) },
                            testTag = "account_menu_wishlist"
                        )

                        CustomerAccountMenuItem(
                            title = "RATINGS AND REVIEWS",
                            subtitle = if (deliveredOrderedCount > 0) {
                                "$deliveredOrderedCount delivered item(s) active for rating & reviews"
                            } else {
                                "Active ONLY on delivered ordered products"
                            },
                            icon = Icons.Default.Star,
                            accentColor = AccentGold,
                            onClick = { viewModel.selectCustomerAccountDestination(CustomerAccountDestination.RATINGS_AND_REVIEWS) },
                            testTag = "account_menu_ratings_reviews"
                        )

                        val activeCouponCount = promoCoupons.count { it.isActive }
                        CustomerAccountMenuItem(
                            title = "VOUCHERS",
                            subtitle = "$activeCouponCount active voucher(s) • ${loyaltyAccount.pointsBalance} loyalty pts",
                            icon = Icons.Default.ConfirmationNumber,
                            accentColor = CyberCyan,
                            onClick = { viewModel.selectCustomerAccountDestination(CustomerAccountDestination.VOUCHERS) },
                            testTag = "account_menu_vouchers"
                        )

                        CustomerAccountMenuItem(
                            title = "FOLLOW 24/7 SHOPPING",
                            subtitle = if (isFollowingStore) "Following 24/7 SHOPPING ✓" else "Follow official channels & updates",
                            icon = if (isFollowingStore) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            accentColor = SuccessGreen,
                            onClick = { viewModel.selectCustomerAccountDestination(CustomerAccountDestination.FOLLOW_STORE) },
                            testTag = "account_menu_follow"
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        Spacer(modifier = Modifier.height(4.dp))

                        // Secondary Tools below the 7 primary items
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.45f)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_saved_addresses_card")
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Saved Delivery Addresses (${savedAddresses.size})", fontWeight = FontWeight.Black, fontSize = 13.sp, color = CyberCyan)
                                    }
                                    AssistChip(
                                        onClick = { showAddAddressDialog = true },
                                        leadingIcon = { Icon(Icons.Default.AddLocationAlt, contentDescription = null, modifier = Modifier.size(15.dp)) },
                                        label = { Text("Add", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                        modifier = Modifier.testTag("add_saved_address_button")
                                    )
                                }
                                if (savedAddresses.isEmpty()) {
                                    Text(
                                        text = "Save Home or Office addresses for 1-tap checkout autofill.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    savedAddresses.forEach { addr ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, if (addr.isDefault) SuccessGreen else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { viewModel.setDefaultSavedAddress(addr.id) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(addr.label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                        if (addr.isDefault) {
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text("DEFAULT", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = SuccessGreen)
                                                        }
                                                    }
                                                    Text("${addr.recipientName} • ${addr.phoneNumber}", fontSize = 11.sp)
                                                    Text("${addr.streetAddress} (${addr.stateZone})", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                IconButton(onClick = { viewModel.deleteSavedAddress(addr.id) }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete Address", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Card(
                            onClick = { viewModel.navigateTo(AppScreen.PARTNER_PORTAL) },
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.2.dp, AccentGold.copy(alpha = 0.6f)),
                            colors = CardDefaults.cardColors(containerColor = AccentGold.copy(alpha = 0.1f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_partner_portal_card")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Handshake, contentDescription = null, tint = AccentGold, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Business Partner Portal (MOU)", fontWeight = FontWeight.Black, fontSize = 13.sp, color = AccentGold)
                                    Text("Sign hosting MOU & upload categorized products to Admin", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        Card(
                            onClick = { viewModel.navigateTo(AppScreen.HELP_SUPPORT) },
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.2.dp, CyberCyan.copy(alpha = 0.55f)),
                            colors = CardDefaults.cardColors(containerColor = CyberCyan.copy(alpha = 0.08f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_help_legal_card")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.HelpOutline, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Help Center, Privacy Policy & Terms", fontWeight = FontWeight.Black, fontSize = 13.sp, color = CyberCyan)
                                    Text("FAQs, Support Tickets, Delivery Policy & Data Privacy", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    CustomerAccountDestination.WELCOME_PROFILE -> {
                        if (currentCustomer != null) {
                            WelcomeCustomerOnlyPage(
                                customer = currentCustomer!!,
                                viewModel = viewModel,
                                onShowQr = { showQrDialog = true }
                            )
                        } else {
                            TabRow(
                                selectedTabIndex = selectedTab,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.clip(RoundedCornerShape(14.dp))
                            ) {
                                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Sign In", fontWeight = FontWeight.Bold) })
                                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Sign Up", fontWeight = FontWeight.Bold) })
                            }

                            if (selectedTab == 0) {
                                Text("Welcome Back", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Sign in with your phone number and password.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedTextField(
                                    value = signInPhone,
                                    onValueChange = { signInPhone = it },
                                    label = { Text("Phone Number") },
                                    placeholder = { Text("e.g. +234 800 000 0000") },
                                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    modifier = Modifier.fillMaxWidth().testTag("auth_signin_phone_input")
                                )
                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    label = { Text("Password") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                    trailingIcon = {
                                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = "Toggle Password Visibility")
                                        }
                                    },
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    modifier = Modifier.fillMaxWidth().testTag("auth_signin_password_input")
                                )
                                Button(
                                    onClick = {
                                        if (signInPhone.isBlank() || password.isBlank()) {
                                            Toast.makeText(context, "Please enter phone number and password", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        viewModel.signIn(
                                            phoneOrEmail = signInPhone,
                                            pass = password,
                                            onSuccess = { Toast.makeText(context, "Welcome back!", Toast.LENGTH_SHORT).show() },
                                            onError = { err -> Toast.makeText(context, err, Toast.LENGTH_LONG).show() }
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("auth_signin_submit_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }

                                var showForgotPassDialog by remember { mutableStateOf(false) }
                                TextButton(
                                    onClick = { showForgotPassDialog = true },
                                    modifier = Modifier
                                        .align(Alignment.End)
                                        .height(48.dp)
                                        .testTag("auth_forgot_password_button")
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(15.dp), tint = AccentGold)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Forgot / Change Password?", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = AccentGold)
                                }

                                if (showForgotPassDialog) {
                                    var fpPhone by remember { mutableStateOf(signInPhone) }
                                    var fpName by remember { mutableStateOf("") }
                                    var fpNewPass by remember { mutableStateOf("") }
                                    var fpConfirmPass by remember { mutableStateOf("") }
                                    AlertDialog(
                                        onDismissRequest = { showForgotPassDialog = false },
                                        title = { Text("Change Forgotten Password", fontWeight = FontWeight.Black) },
                                        text = {
                                            Column(
                                                modifier = Modifier
                                                    .verticalScroll(rememberScrollState())
                                                    .testTag("auth_forgot_password_dialog"),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = "Enter the phone line you signed up with and set a new sign-in password.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                OutlinedTextField(
                                                    value = fpPhone,
                                                    onValueChange = { fpPhone = it },
                                                    label = { Text("Registered Phone Number *") },
                                                    placeholder = { Text("e.g. 08012345678") },
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                                    modifier = Modifier.fillMaxWidth().testTag("forgot_password_phone_input")
                                                )
                                                OutlinedTextField(
                                                    value = fpName,
                                                    onValueChange = { fpName = it },
                                                    label = { Text("Full Name Verification (Optional)") },
                                                    singleLine = true,
                                                    modifier = Modifier.fillMaxWidth().testTag("forgot_password_name_input")
                                                )
                                                OutlinedTextField(
                                                    value = fpNewPass,
                                                    onValueChange = { fpNewPass = it },
                                                    label = { Text("New Password *") },
                                                    visualTransformation = PasswordVisualTransformation(),
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                                    modifier = Modifier.fillMaxWidth().testTag("forgot_password_new_pass_input")
                                                )
                                                OutlinedTextField(
                                                    value = fpConfirmPass,
                                                    onValueChange = { fpConfirmPass = it },
                                                    label = { Text("Confirm New Password *") },
                                                    visualTransformation = PasswordVisualTransformation(),
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                                    modifier = Modifier.fillMaxWidth().testTag("forgot_password_confirm_pass_input")
                                                )
                                            }
                                        },
                                        confirmButton = {
                                            Button(
                                                onClick = {
                                                    if (fpPhone.isBlank() || fpNewPass.isBlank()) {
                                                        Toast.makeText(context, "Enter phone number and new password.", Toast.LENGTH_SHORT).show()
                                                        return@Button
                                                    }
                                                    if (fpNewPass != fpConfirmPass) {
                                                        Toast.makeText(context, "New passwords do not match.", Toast.LENGTH_SHORT).show()
                                                        return@Button
                                                    }
                                                    viewModel.resetCustomerPasswordByPhone(
                                                        phone = fpPhone,
                                                        fullName = fpName,
                                                        newPassword = fpNewPass
                                                    ) { ok, msg ->
                                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                        if (ok) {
                                                            signInPhone = fpPhone.trim()
                                                            password = ""
                                                            showForgotPassDialog = false
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.testTag("forgot_password_submit_button")
                                            ) {
                                                Text("Update Password", fontWeight = FontWeight.Bold)
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showForgotPassDialog = false }) { Text("Cancel") }
                                        }
                                    )
                                }

                                GoogleSignInButton(
                                    onAuthSuccess = {
                                        viewModel.onGoogleSignInSuccess {
                                            Toast.makeText(context, "Signed in with Google!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onAuthError = { err -> Toast.makeText(context, err, Toast.LENGTH_LONG).show() }
                                )
                            } else {
                                Text("Create Customer Account", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Register with your name, phone number, and password.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedTextField(
                                    value = signUpFullName,
                                    onValueChange = { signUpFullName = it },
                                    label = { Text("Full Name") },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("auth_signup_name_input")
                                )
                                OutlinedTextField(
                                    value = signUpPhone,
                                    onValueChange = { signUpPhone = it },
                                    label = { Text("Phone Number") },
                                    placeholder = { Text("e.g. +234 800 000 0000") },
                                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    modifier = Modifier.fillMaxWidth().testTag("auth_signup_phone_input")
                                )
                                OutlinedTextField(
                                    value = signUpPassword,
                                    onValueChange = { signUpPassword = it },
                                    label = { Text("Create Password") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    modifier = Modifier.fillMaxWidth().testTag("auth_signup_password_input")
                                )
                                OutlinedTextField(
                                    value = signUpAddress,
                                    onValueChange = { signUpAddress = it },
                                    label = { Text("Delivery Address (Optional)") },
                                    leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                                    minLines = 2,
                                    modifier = Modifier.fillMaxWidth().testTag("auth_signup_address_input")
                                )
                                Button(
                                    onClick = {
                                        if (signUpFullName.isBlank() || signUpPhone.isBlank() || signUpPassword.isBlank()) {
                                            Toast.makeText(context, "Please enter Name, Phone Number, and Password", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        viewModel.signUp(
                                            name = signUpFullName,
                                            pass = signUpPassword,
                                            phone = signUpPhone,
                                            address = signUpAddress,
                                            email = "",
                                            onSuccess = {
                                                Toast.makeText(context, "✅ Account Created!", Toast.LENGTH_LONG).show()
                                            },
                                            onError = { err -> Toast.makeText(context, err, Toast.LENGTH_LONG).show() }
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("auth_signup_submit_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Create Account", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                    }

                    CustomerAccountDestination.INBOX -> CustomerInboxPage(viewModel = viewModel)
                    CustomerAccountDestination.RATINGS_AND_REVIEWS -> DeliveredRatingsAndReviewsPage(viewModel = viewModel)
                    CustomerAccountDestination.VOUCHERS -> CustomerVouchersPage(viewModel = viewModel)
                    CustomerAccountDestination.FOLLOW_STORE -> FollowStorePage(viewModel = viewModel, onShowQr = { showQrDialog = true })
                }
            }

            if (showAddAddressDialog) {
                var addrLabel by remember { mutableStateOf("Home") }
                var addrRecipient by remember { mutableStateOf(currentCustomer?.fullName.orEmpty()) }
                var addrPhone by remember { mutableStateOf(currentCustomer?.phoneNumber.orEmpty()) }
                var addrStreet by remember { mutableStateOf("") }
                var addrZone by remember { mutableStateOf(DeliveryZoneHelper.zones[1].zoneName) }

                AlertDialog(
                    onDismissRequest = { showAddAddressDialog = false },
                    title = { Text("Add Delivery Address", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(value = addrLabel, onValueChange = { addrLabel = it }, label = { Text("Label (Home / Work)") }, singleLine = true)
                            OutlinedTextField(value = addrRecipient, onValueChange = { addrRecipient = it }, label = { Text("Recipient Name") }, singleLine = true)
                            OutlinedTextField(value = addrPhone, onValueChange = { addrPhone = it }, label = { Text("Phone Number") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                            OutlinedTextField(value = addrStreet, onValueChange = { addrStreet = it }, label = { Text("Street / Area Address") }, minLines = 2)
                            Text("Delivery Zone:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                DeliveryZoneHelper.zones.forEach { z ->
                                    FilterChip(
                                        selected = addrZone == z.zoneName,
                                        onClick = { addrZone = z.zoneName },
                                        label = { Text(z.zoneName, fontSize = 10.sp) }
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (addrRecipient.isBlank() || addrPhone.isBlank() || addrStreet.isBlank()) {
                                Toast.makeText(context, "Please complete recipient, phone and address", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.saveSavedAddress(
                                SavedAddress(
                                    label = addrLabel.trim().ifBlank { "Home" },
                                    recipientName = addrRecipient.trim(),
                                    phoneNumber = addrPhone.trim(),
                                    streetAddress = addrStreet.trim(),
                                    stateZone = addrZone,
                                    isDefault = savedAddresses.isEmpty()
                                )
                            )
                            showAddAddressDialog = false
                            Toast.makeText(context, "Delivery address saved!", Toast.LENGTH_SHORT).show()
                        }) { Text("Save") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddAddressDialog = false }) { Text("Cancel") }
                    }
                )
            }

            if (showQrDialog) {
                ShareAppDialog(
                    onDismiss = { showQrDialog = false },
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun CustomerAccountMenuItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.16f))
            ) {
                Icon(icon, contentDescription = title, tint = accentColor, modifier = Modifier.size(21.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open $title",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
