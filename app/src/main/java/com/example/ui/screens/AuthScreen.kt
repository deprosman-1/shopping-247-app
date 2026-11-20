package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Loyalty
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import com.example.data.model.SavedAddress
import com.example.ui.components.DualAxisScrollContainer
import com.example.ui.components.GoogleSignInButton
import com.example.ui.components.ShareAppDialog
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.SuccessGreen
import com.example.util.DeliveryZoneHelper
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AppScreen
import com.example.MainViewModel
import com.example.ui.theme.AccentGold

@Composable
fun ProfileDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentCustomer by viewModel.currentCustomer.collectAsState()
    val initialAuthTab by viewModel.authTab.collectAsState()
    val isWideDualAxisMode by viewModel.isWideDualAxisMode.collectAsState()
    val loyaltyAccount by viewModel.loyaltyAccount.collectAsState()
    val savedAddresses by viewModel.savedAddresses.collectAsState()
    var selectedTab by remember(initialAuthTab) { mutableIntStateOf(initialAuthTab) } // 0 = Sign In, 1 = Sign Up

    var signInPhone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var signUpFullName by remember { mutableStateOf("") }
    var signUpPhone by remember { mutableStateOf("") }
    var signUpPassword by remember { mutableStateOf("") }
    var signUpAddress by remember { mutableStateOf("") }
    var referralInput by remember { mutableStateOf("") }
    var showAddAddressDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }

    BackHandler {
        viewModel.navigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (currentCustomer != null) "Customer Account & Access" else "Customer Access", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("auth_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Previous Page")
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
                            onClick = { selectedTab = 1 },
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
                    .then(if (isWideDualAxisMode) Modifier else Modifier.widthIn(max = 500.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // IF ALREADY LOGGED IN vs NOT LOGGED IN
                if (currentCustomer != null) {
                    val customer = currentCustomer!!
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(
                                    imageVector = if (customer.isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = customer.fullName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = customer.phoneNumber,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )

                            if (customer.isAdmin) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = AccentGold.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "ADMINISTRATOR PRIVILEGES",
                                        color = AccentGold,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(14.dp))

                            ProfileDetailRow("Phone", customer.phoneNumber)
                            ProfileDetailRow("Delivery Address", customer.deliveryAddress)
                            ProfileDetailRow("Account ID", customer.id.take(8))

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = { viewModel.navigateTo(AppScreen.ORDERS) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("order_tracker_button")
                            ) {
                                Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Order Tracker")
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = { showQrDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SuccessGreen),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Download Official App & QR Code (100% Safe)", color = SuccessGreen, fontWeight = FontWeight.SemiBold)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    viewModel.playSignInAudio(customer.fullName)
                                    Toast.makeText(context, "Playing Live Welcome Voice Message...", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Replay Live Audio Welcome", color = Color.Black, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = {
                                    viewModel.signOut()
                                    Toast.makeText(context, "Signed out successfully", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sign Out")
                            }
                        }
                    }



                    // NEW CUSTOMER SIGN UP PAGE ACCOUNT SHORTCUT CARD
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.5.dp, CyberCyan),
                        colors = CardDefaults.cardColors(
                            containerColor = CyberCyan.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_customer_signup_account_shortcut_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(CyberCyan),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = "New Customer Sign Up Shortcut",
                                        tint = Color.Black,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "NEW CUSTOMER SIGN UP SHORTCUT",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = CyberCyan
                                    )
                                    Text(
                                        text = "Direct Shortcut to Customer Registration Page",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Create a new customer account to register phone details, delivery address, and place 24/7 SHOPPING orders with real-time status updates.",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 12.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    viewModel.signOut()
                                    viewModel.navigateToSignUp()
                                    Toast.makeText(context, "Navigating to New Customer Sign Up Page...", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("new_customer_signup_page_shortcut_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("OPEN NEW CUSTOMER SIGN UP FORM", fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                    }
                } else {
                    // NOT LOGGED IN: SIGN UP SHORTCUT BANNER & AUTH TABS
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, CyberCyan),
                        colors = CardDefaults.cardColors(
                            containerColor = CyberCyan.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                            .testTag("logged_out_signup_shortcut_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "NEW CUSTOMER SIGN UP SHORTCUT",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = CyberCyan
                                    )
                                    Text(
                                        text = "Tap for quick access to sign up form",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Button(
                                onClick = { selectedTab = 1 },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("banner_go_to_signup_button")
                            ) {
                                Text("SIGN UP", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.clip(RoundedCornerShape(14.dp))
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Sign In", fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Sign Up", fontWeight = FontWeight.Bold) }
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    if (selectedTab == 0) {
                        Text(
                            text = "Welcome Back",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sign in with your phone number and password to sync your cart and cloud orders.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = signInPhone,
                            onValueChange = { signInPhone = it },
                            label = { Text("Phone Number") },
                            placeholder = { Text("e.g. +234 800 000 0000") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_signin_phone_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_signin_password_input")
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                if (signInPhone.isBlank() || password.isBlank()) {
                                    Toast.makeText(context, "Please enter phone number and password", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.signIn(
                                    phoneOrEmail = signInPhone,
                                    pass = password,
                                    onSuccess = {
                                        Toast.makeText(context, "Welcome back!", Toast.LENGTH_SHORT).show()
                                        viewModel.navigateTo(AppScreen.HOME)
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("auth_signin_submit_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        GoogleSignInButton(
                            onAuthSuccess = {
                                viewModel.onGoogleSignInSuccess {
                                    Toast.makeText(context, "Signed in with Google!", Toast.LENGTH_SHORT).show()
                                    viewModel.navigateTo(AppScreen.HOME)
                                }
                            },
                            onAuthError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    } else {
                        // SIGN UP FORM
                        Text(
                            text = "Create Customer Account",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Register with your name, phone number, and password to start shopping.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = signUpFullName,
                            onValueChange = { signUpFullName = it },
                            label = { Text("Full Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_signup_name_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = signUpPhone,
                            onValueChange = { signUpPhone = it },
                            label = { Text("Phone Number") },
                            placeholder = { Text("e.g. +234 800 000 0000") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_signup_phone_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = signUpPassword,
                            onValueChange = { signUpPassword = it },
                            label = { Text("Create Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_signup_password_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = signUpAddress,
                            onValueChange = { signUpAddress = it },
                            label = { Text("Delivery Address (Optional)") },
                            leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                            minLines = 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_signup_address_input")
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                if (signUpFullName.isBlank() || signUpPhone.isBlank() || signUpPassword.isBlank()) {
                                    Toast.makeText(context, "Please enter your Name, Phone Number, and Password", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.signUp(
                                    name = signUpFullName,
                                    pass = signUpPassword,
                                    phone = signUpPhone,
                                    address = signUpAddress,
                                    email = "",
                                    onSuccess = {
                                        val syncErr = viewModel.repository.lastCloudSyncError
                                        val msg = if (syncErr.isNullOrBlank()) {
                                            "✅ Account Created & Synced Live to Store Admin!"
                                        } else {
                                            "⚠️ Account saved on this phone ($syncErr). Link to Store Cloud to send to Admin!"
                                        }
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        if (syncErr.isNullOrBlank()) {
                                            viewModel.navigateTo(AppScreen.HOME)
                                        }
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("auth_signup_submit_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Create Account", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Handshake, contentDescription = null, tint = AccentGold, modifier = Modifier.size(26.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Business Partner Portal (MOU)", fontWeight = FontWeight.Black, fontSize = 13.sp, color = AccentGold)
                                Text("Sign hosting MOU & upload categorized products to Admin", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Loyalty Points & Referral Rewards Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.2.dp, AccentGold.copy(alpha = 0.55f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_loyalty_rewards_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Loyalty, contentDescription = null, tint = AccentGold, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Loyalty Rewards & Referrals", fontWeight = FontWeight.Black, fontSize = 14.sp, color = AccentGold)
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AccentGold.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = "${loyaltyAccount.pointsBalance} pts (₦${loyaltyAccount.pointsBalance * 50})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = AccentGold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(
                            text = "Earn 1 point per ₦1,000 spent (1 pt = ₦50 discount at checkout). Lifetime earned: ${loyaltyAccount.lifetimeEarnedPoints} pts.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Your Referral Code:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(loyaltyAccount.referralCode, fontWeight = FontWeight.Black, fontSize = 15.sp, color = CyberCyan)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cb.setPrimaryClip(ClipData.newPlainText("Referral Code", loyaltyAccount.referralCode))
                                        Toast.makeText(context, "Referral code copied!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy code", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy", fontSize = 11.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        val share = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, "Shop on 24/7 Shopping with my referral code ${loyaltyAccount.referralCode} for 10 bonus points (₦500 value)!")
                                        }
                                        context.startActivity(Intent.createChooser(share, "Share Referral Code"))
                                    },
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Share code", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Invite", fontSize = 11.sp)
                                }
                            }
                        }
                        if (!loyaltyAccount.referralBonusClaimed) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = referralInput,
                                    onValueChange = { referralInput = it },
                                    placeholder = { Text("Enter friend's code (247-XXXX)", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        val (ok, msg) = viewModel.applyReferralCode(referralInput)
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        if (ok) referralInput = ""
                                    },
                                    enabled = referralInput.isNotBlank()
                                ) {
                                    Text("Claim", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Text(
                                text = "✓ Referral bonus claimed (${loyaltyAccount.referredByCode})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Saved Delivery Addresses Book
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
                                text = "Save Home, Office, or alternate addresses for one-tap checkout autofill.",
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

                Spacer(modifier = Modifier.height(14.dp))

                // Help Center, Support Tickets, Privacy Policy & Terms
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
            } // end Column

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
        } // end Box
    } // end Scaffold content
}
