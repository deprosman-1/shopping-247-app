package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Loyalty
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AppScreen
import com.example.MainViewModel
import com.example.data.model.Customer
import com.example.data.model.OrderStatus
import com.example.data.model.Product
import com.example.ui.theme.AccentGold
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.SuccessGreen
import com.example.util.WebPortalExporter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WelcomeCustomerOnlyPage(
    customer: Customer,
    viewModel: MainViewModel,
    onShowQr: () -> Unit
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Strictly Customer Name & Phone Number ONLY
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            border = BorderStroke(1.2.dp, AccentGold.copy(alpha = 0.65f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("welcome_profile_only_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF052E16))
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Customer Profile",
                        tint = AccentGold,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Text(
                    text = "WELCOME",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.4.sp,
                    color = AccentGold
                )

                Text(
                    text = customer.fullName.ifBlank { "Customer" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("welcome_customer_name_only")
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Customer Phone Number",
                        tint = CyberCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = customer.phoneNumber.ifBlank { "No phone number" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        modifier = Modifier.testTag("welcome_customer_phone_only")
                    )
                }
            }
        }

        Button(
            onClick = {
                viewModel.playSignInAudio(customer.fullName)
                Toast.makeText(context, "Playing Live Welcome Voice Message...", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Replay Live Audio Welcome", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = onShowQr,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = SuccessGreen),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(Icons.Default.QrCode2, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Share App & Official QR Code", color = SuccessGreen, fontWeight = FontWeight.SemiBold)
        }

        OutlinedButton(
            onClick = {
                viewModel.signOut()
                viewModel.navigateToSignUp()
                Toast.makeText(context, "Opening New Customer Sign Up Form...", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("new_customer_signup_page_shortcut_button")
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Register Another Customer Account", fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = {
                viewModel.signOut()
                Toast.makeText(context, "Signed out successfully", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
            border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("welcome_sign_out_button")
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Sign Out", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
        }
    }
}

@Composable
fun CustomerInboxPage(viewModel: MainViewModel) {
    val allOrders by viewModel.allOrders.collectAsState()
    val preOrders by viewModel.preOrderRequests.collectAsState()
    val supportTickets by viewModel.supportTickets.collectAsState()
    val adminPromo by viewModel.adminPromo.collectAsState()
    val cloudStatusState by viewModel.cloudStatusMessage.collectAsState()
    val cloudStatus = cloudStatusState.orEmpty()
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    val hasAnyItem = adminPromo.isActive ||
        allOrders.isNotEmpty() ||
        preOrders.isNotEmpty() ||
        supportTickets.isNotEmpty() ||
        cloudStatus.isNotBlank()

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("account_inbox_page")
    ) {
        if (adminPromo.isActive) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFB91C1C).copy(alpha = 0.14f)),
                border = BorderStroke(1.2.dp, AccentGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.LocalOffer, contentDescription = null, tint = AccentGold, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "LIVE STORE PROMO • ${adminPromo.discountPercent}% OFF",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = AccentGold
                        )
                        Text(
                            text = adminPromo.title.ifBlank { "Limited-time discount active on 24/7 SHOPPING." },
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        if (preOrders.any { it.adminResponse.isNotBlank() || it.adminQuotedPrice > 0.0 }) {
            Text(
                text = "PRE-ORDER ADMIN RESPONSES",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Black,
                color = CyberCyan
            )
            preOrders.forEach { req ->
                Card(
                    onClick = { viewModel.navigateTo(AppScreen.ORDERS) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(req.productName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(req.status.name, fontSize = 11.sp, fontWeight = FontWeight.Black, color = AccentGold)
                        }
                        if (req.adminResponse.isNotBlank()) {
                            Text("Admin Note: ${req.adminResponse}", fontSize = 12.sp)
                        }
                        Text(dateFormat.format(Date(req.createdAt)), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (allOrders.isNotEmpty()) {
            Text(
                text = "ORDER STATUS UPDATES (${allOrders.size})",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Black,
                color = SuccessGreen
            )
            allOrders.take(10).forEach { ord ->
                Card(
                    onClick = { viewModel.navigateTo(AppScreen.ORDERS) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (ord.status == OrderStatus.DELIVERED) Icons.Default.CheckCircle else Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = if (ord.status == OrderStatus.DELIVERED) SuccessGreen else AccentGold,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Order ${ord.orderId} • ${ord.status.label}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(ord.itemsSummary, fontSize = 11.5.sp, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (ord.verificationNote.isNotBlank()) {
                                Text("Note: ${ord.verificationNote}", fontSize = 11.sp, color = CyberCyan)
                            }
                            Text(dateFormat.format(Date(ord.createdAt)), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        if (supportTickets.isNotEmpty()) {
            Text(
                text = "SUPPORT & HELP DESK MESSAGES (${supportTickets.size})",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Black,
                color = AccentGold
            )
            supportTickets.forEach { tkt ->
                Card(
                    onClick = { viewModel.navigateTo(AppScreen.HELP_SUPPORT) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${tkt.subject} (${tkt.status.label})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(tkt.message, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (tkt.adminReply.isNotBlank()) {
                            Text("Store Reply: ${tkt.adminReply}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SuccessGreen)
                        }
                    }
                }
            }
        }

        if (cloudStatus.isNotBlank()) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(cloudStatus, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (!hasAnyItem) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Inbox, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(44.dp))
                    Text("Your Inbox is Up to Date", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        "Order notifications, Pre-Order quotes, Promo alerts, and Support replies appear here automatically.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        OutlinedButton(
            onClick = { viewModel.navigateTo(AppScreen.HELP_SUPPORT) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Message Support Desk", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DeliveredRatingsAndReviewsPage(viewModel: MainViewModel) {
    val context = LocalContext.current
    val allOrders by viewModel.allOrders.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val allReviews by viewModel.productReviews.collectAsState()
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    val deliveredProducts = remember(allOrders, allProducts) {
        val delivered = allOrders.filter { it.status == OrderStatus.DELIVERED }
        if (delivered.isEmpty()) emptyList()
        else allProducts.filter { prod ->
            prod.title.isNotBlank() && delivered.any { ord ->
                ord.itemsSummary.contains(prod.title, ignoreCase = true)
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("account_ratings_reviews_page")
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = CyberCyan.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Verified, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ratings & review views are active ONLY on delivered ordered products (${deliveredProducts.size} unlocked).",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (deliveredProducts.isEmpty()) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("account_ratings_locked_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = AccentGold, modifier = Modifier.size(44.dp))
                    Text("No Delivered Ordered Products Yet", fontWeight = FontWeight.Black, fontSize = 15.sp, color = AccentGold)
                    Text(
                        text = "Once your ordered product is marked DELIVERED, rating stars, review posting, and review viewing become active here.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { viewModel.navigateTo(AppScreen.ORDERS) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open My Orders", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            deliveredProducts.forEach { prod ->
                DeliveredProductReviewCard(
                    product = prod,
                    reviews = allReviews.filter { it.productId == prod.id },
                    dateFormat = dateFormat,
                    onSubmitReview = { stars, comment, onDone ->
                        viewModel.submitProductReview(
                            productId = prod.id,
                            rating = stars,
                            comment = comment
                        ) { ok, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            onDone(ok)
                        }
                    },
                    onOpenProduct = { viewModel.openProductDetails(prod) }
                )
            }
        }
    }
}

@Composable
private fun DeliveredProductReviewCard(
    product: Product,
    reviews: List<com.example.data.model.ProductReview>,
    dateFormat: SimpleDateFormat,
    onSubmitReview: (Int, String, (Boolean) -> Unit) -> Unit,
    onOpenProduct: () -> Unit
) {
    var stars by remember(product.id) { mutableIntStateOf(5) }
    var comment by remember(product.id) { mutableStateOf("") }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.2.dp, SuccessGreen.copy(alpha = 0.55f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("delivered_review_card_${product.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(product.title, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Text(
                        text = "${product.brand} • ★ ${product.rating} (${product.reviewCount} reviews)",
                        fontSize = 11.5.sp,
                        color = AccentGold,
                        fontWeight = FontWeight.Bold
                    )
                }
                OutlinedButton(onClick = onOpenProduct, shape = RoundedCornerShape(8.dp)) {
                    Text("View Item", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                (1..5).forEach { s ->
                    IconButton(
                        onClick = { stars = s },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (s <= stars) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "$s Stars",
                            tint = if (s <= stars) AccentGold else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Text("$stars / 5", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AccentGold)
            }

            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text("Write a review for ${product.title}") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    onSubmitReview(stars, comment) { ok ->
                        if (ok) comment = ""
                    }
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Submit Review & Rating", fontWeight = FontWeight.Bold)
            }

            if (reviews.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                Text("Delivered Customer Reviews:", fontSize = 11.sp, fontWeight = FontWeight.Black, color = SuccessGreen)
                reviews.take(5).forEach { rev ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${rev.customerName} (${rev.rating}★)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(dateFormat.format(Date(rev.createdAt)), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(rev.comment, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        if (rev.adminReply.isNotBlank()) {
                            Text(
                                text = "Store Admin Reply: ${rev.adminReply}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerVouchersPage(viewModel: MainViewModel) {
    val context = LocalContext.current
    val coupons by viewModel.promoCoupons.collectAsState()
    val adminPromo by viewModel.adminPromo.collectAsState()
    val loyaltyAccount by viewModel.loyaltyAccount.collectAsState()
    var referralInput by remember { mutableStateOf("") }
    val activeCoupons = remember(coupons) { coupons.filter { it.isActive } }
    val nairaFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "NG")).apply {
            maximumFractionDigits = 0
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("account_vouchers_page")
    ) {
        if (adminPromo.isActive) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFB91C1C).copy(alpha = 0.14f)),
                border = BorderStroke(1.2.dp, AccentGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("ACTIVE STOREWIDE VOUCHER OFFER", fontSize = 11.sp, fontWeight = FontWeight.Black, color = AccentGold)
                    Text("${adminPromo.title} — ${adminPromo.discountPercent}% OFF automatically applied at checkout!", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Text("ACTIVE PROMO VOUCHERS (${activeCoupons.size})", fontSize = 12.sp, fontWeight = FontWeight.Black, color = CyberCyan)
        if (activeCoupons.isEmpty()) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = AccentGold, modifier = Modifier.size(34.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("No active promo coupon codes right now.", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Check back soon or redeem Loyalty Points & Referral credits below!", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            activeCoupons.forEach { coupon ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.6f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${coupon.code} • ${coupon.discountPercent}% OFF", fontWeight = FontWeight.Black, fontSize = 14.sp, color = AccentGold)
                            if (coupon.minOrderAmount > 0.0) {
                                Text("Min. spend: ${nairaFormat.format(coupon.minOrderAmount)}", fontSize = 11.sp, color = CyberCyan)
                            }
                            if (coupon.description.isNotBlank()) {
                                Text(coupon.description, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cb.setPrimaryClip(ClipData.newPlainText("Voucher Code", coupon.code))
                                Toast.makeText(context, "Copied voucher '${coupon.code}'!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy voucher", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Loyalty & Referral Voucher Card
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
                        Text("Loyalty Points Voucher", fontWeight = FontWeight.Black, fontSize = 14.sp, color = AccentGold)
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
                        Text("Your Referral Voucher Code:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(loyaltyAccount.referralCode, fontWeight = FontWeight.Black, fontSize = 15.sp, color = CyberCyan)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = {
                                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cb.setPrimaryClip(ClipData.newPlainText("Referral Code", loyaltyAccount.referralCode))
                                Toast.makeText(context, "Referral code copied!", Toast.LENGTH_SHORT).show()
                            }
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
                            }
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
    }
}

@Composable
fun FollowStorePage(
    viewModel: MainViewModel,
    onShowQr: () -> Unit
) {
    val context = LocalContext.current
    val isFollowing by viewModel.isFollowingStore.collectAsState()
    val storeProfile by viewModel.storeProfile.collectAsState()
    val webUrl = remember(storeProfile) {
        val raw = WebPortalExporter.getSavedCustomPortalUrl(context).ifBlank { storeProfile.apkDownloadUrl }
        WebPortalExporter.normalizePortalUrl(raw)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("account_follow_store_page")
    ) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF052E16)),
            border = BorderStroke(1.5.dp, AccentGold),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (isFollowing) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Follow 24/7 SHOPPING",
                    tint = AccentGold,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = storeProfile.storeName.uppercase(),
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = AccentGold
                )
                Text(
                    text = if (isFollowing) {
                        "You are now following 24/7 SHOPPING for new arrivals, flash promos, and restock alerts."
                    } else {
                        "Follow 24/7 SHOPPING to receive fresh catalog drops, promo offers, and restock updates."
                    },
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Button(
                    onClick = {
                        val nowFollowing = viewModel.toggleFollowStore()
                        Toast.makeText(
                            context,
                            if (nowFollowing) "✓ Following 24/7 SHOPPING!" else "Unfollowed 24/7 SHOPPING",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFollowing) SuccessGreen else AccentGold,
                        contentColor = Color(0xFF052E16)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("toggle_follow_store_button")
                ) {
                    Text(
                        text = if (isFollowing) "FOLLOWING 24/7 SHOPPING ✓" else "FOLLOW 24/7 SHOPPING",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }
            }
        }

        if (storeProfile.whatsappNumber.isNotBlank()) {
            OutlinedButton(
                onClick = {
                    try {
                        val cleanWa = storeProfile.whatsappNumber.replace("+", "").replace(" ", "").replace("-", "")
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$cleanWa")))
                    } catch (_: Exception) {
                        Toast.makeText(context, "WhatsApp unavailable on this device", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.HelpOutline, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Follow on WhatsApp (${storeProfile.whatsappNumber})", fontWeight = FontWeight.Bold, color = SuccessGreen)
            }
        }

        if (storeProfile.phoneNumber.isNotBlank()) {
            OutlinedButton(
                onClick = {
                    try {
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${storeProfile.phoneNumber}")))
                    } catch (_: Exception) {
                        Toast.makeText(context, "Dialer unavailable", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Call, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Call Store Hotline (${storeProfile.phoneNumber})", fontWeight = FontWeight.Bold, color = CyberCyan)
            }
        }

        if (webUrl.isNotBlank()) {
            OutlinedButton(
                onClick = {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)))
                    } catch (_: Exception) {
                        Toast.makeText(context, "Browser unavailable", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Language, contentDescription = null, tint = AccentGold, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Official Store Link", fontWeight = FontWeight.Bold, color = AccentGold)
            }
        }

        OutlinedButton(
            onClick = onShowQr,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Share 24/7 SHOPPING QR & Invite", fontWeight = FontWeight.Bold)
        }
    }
}
