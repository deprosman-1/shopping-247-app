package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import com.example.ui.theme.AccentGold
import com.example.ui.theme.CyberCyan
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AppScreen
import com.example.MainViewModel
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.ui.theme.CodOrange
import com.example.ui.theme.MoniepointBlue
import com.example.ui.theme.OPayGreen
import com.example.ui.theme.PosPurple
import com.example.ui.theme.SuccessGreen
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.ReturnRefundRequest
import com.example.data.model.ReturnRefundStatus
import com.example.ui.components.CancelOrderConfirmDialog
import com.example.ui.components.CustomerPreOrdersList
import com.example.ui.components.DualAxisScrollContainer
import com.example.ui.components.OrderProgressStepper
import com.example.ui.components.PreOrderRequestCard
import com.example.ui.components.PreOrderRequestDialog
import com.example.ui.components.ReturnRefundRequestDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersReceiptsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val orders by viewModel.allOrders.collectAsState()
    val currentCustomer by viewModel.currentCustomer.collectAsState()
    val storeProfile by viewModel.storeProfile.collectAsState()
    val returnRequests by viewModel.returnRequests.collectAsState()
    val isWideDualAxisMode by viewModel.isWideDualAxisMode.collectAsState()
    var showPreOrderDialog by remember { mutableStateOf(false) }
    var cancellingOrder by remember { mutableStateOf<Order?>(null) }
    var returningOrder by remember { mutableStateOf<Order?>(null) }

    val nairaFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "NG")).apply {
            maximumFractionDigits = 0
        }
    }
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    BackHandler {
        viewModel.navigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Orders & Pre-Orders", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("orders_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Previous Page")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showPreOrderDialog = true },
                        modifier = Modifier.testTag("new_preorder_appbar_button")
                    ) {
                        Icon(Icons.Default.NewReleases, contentDescription = "New Pre-Order Request", tint = CyberCyan)
                    }
                    IconButton(
                        onClick = { viewModel.navigateToHome() },
                        modifier = Modifier.testTag("orders_home_button")
                    ) {
                        Icon(Icons.Default.Home, contentDescription = "Back to Home Page", tint = CyberCyan)
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (isWideDualAxisMode) Modifier else Modifier.widthIn(max = 500.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Store Contact & Customer Support Card (Dynamically updated by Admin)
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth().testTag("orders_store_support_card")
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Storefront, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${storeProfile.storeName} • Customer Support",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black,
                                    color = CyberCyan
                                )
                            }
                            if (storeProfile.storeAddress.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(storeProfile.storeAddress, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            if (storeProfile.supportEmail.isNotBlank()) {
                                Text(
                                    text = "Email: ${storeProfile.supportEmail}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val callNumber = storeProfile.phoneNumber
                            val secondaryNumber = storeProfile.secondaryPhoneNumber
                            val waNumber = storeProfile.whatsappNumber.ifBlank { storeProfile.phoneNumber }
                            if (secondaryNumber.isNotBlank()) {
                                Text(
                                    text = "Hotline: $secondaryNumber",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (callNumber.isNotBlank() || waNumber.isNotBlank()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                                    if (callNumber.isNotBlank()) {
                                        OutlinedButton(
                                            onClick = {
                                                try {
                                                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$callNumber")))
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Dialer unavailable", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Call $callNumber", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (waNumber.isNotBlank()) {
                                        OutlinedButton(
                                            onClick = {
                                                try {
                                                    val cleanWa = waNumber.replace("+", "").replace(" ", "").replace("-", "")
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$cleanWa")))
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "WhatsApp unavailable", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Chat, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("WhatsApp", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                                        }
                                    }
                                }
                            }
                            OutlinedButton(
                                onClick = { viewModel.navigateTo(AppScreen.HELP_SUPPORT) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                                    .testTag("orders_help_support_button")
                            ) {
                                Icon(Icons.Default.HelpOutline, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Help Desk, Tickets, Privacy Policy & Terms", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                            }
                        }
                    }
                }

                // Section 1: Customer Pre-Orders & Custom Requests
                item {
                    CustomerPreOrdersList(viewModel = viewModel)
                }

                item {
                    PreOrderRequestCard(viewModel = viewModel)
                }

                // Section 2: Order Receipts History
                item {
                    Text(
                        text = "ORDER & PAYMENT RECEIPTS (${orders.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (orders.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "No Orders Placed Yet",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Your offline payment receipts will show here once confirmed.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(
                                        onClick = { viewModel.navigateToHome() },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.testTag("orders_empty_browse_button")
                                    ) {
                                        Text("Browse ${storeProfile.storeName}", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(
                        items = orders,
                        key = { it.orderId },
                        contentType = { "order_receipt_card" }
                    ) { order ->
                        val existingReturn = returnRequests.find { it.orderId == order.orderId }
                        OrderReceiptCard(
                            order = order,
                            existingReturn = existingReturn,
                            onViewReceipt = { viewModel.showReceipt(order) },
                            onReorder = {
                                val (added, skipped) = viewModel.reorderFromOrder(order)
                                val msg = when {
                                    added > 0 && skipped == 0 -> "Added $added item(s) to Cart!"
                                    added > 0 -> "Added $added item(s) ($skipped out of stock)"
                                    else -> "Items from this order are currently out of stock"
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                if (added > 0) {
                                    viewModel.navigateTo(AppScreen.CART)
                                }
                            },
                            onCancelOrder = { cancellingOrder = order },
                            onRequestReturn = { returningOrder = order },
                            onRateOrder = {
                                viewModel.openCustomerAccountShortcut(
                                    com.example.CustomerAccountDestination.RATINGS_AND_REVIEWS
                                )
                            }
                        )
                    }
                }
            }

            if (showPreOrderDialog) {
                PreOrderRequestDialog(viewModel = viewModel, onDismiss = { showPreOrderDialog = false })
            }

            cancellingOrder?.let { targetOrder ->
                CancelOrderConfirmDialog(
                    order = targetOrder,
                    onDismiss = { cancellingOrder = null },
                    onConfirmCancel = { reason ->
                        viewModel.cancelCustomerOrder(targetOrder.orderId, reason) { _, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            cancellingOrder = null
                        }
                    }
                )
            }

            returningOrder?.let { targetOrder ->
                ReturnRefundRequestDialog(
                    order = targetOrder,
                    onDismiss = { returningOrder = null },
                    onSubmit = { reason, details, proofUri ->
                        viewModel.submitReturnRefundRequest(
                            orderId = targetOrder.orderId,
                            reason = reason,
                            details = details,
                            proofImageUri = proofUri
                        ) { _, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            returningOrder = null
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun OrderReceiptCard(
    order: Order,
    existingReturn: ReturnRefundRequest? = null,
    onViewReceipt: () -> Unit,
    onReorder: () -> Unit = {},
    onCancelOrder: () -> Unit = {},
    onRequestReturn: () -> Unit = {},
    onRateOrder: () -> Unit = {}
) {
    val nairaFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "NG")).apply {
            maximumFractionDigits = 0
        }
    }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    val paymentColor = when (order.paymentMethod) {
        PaymentMethod.OPAY -> OPayGreen
        PaymentMethod.MONIEPOINT -> MoniepointBlue
        PaymentMethod.POS_TRANSFER -> PosPurple
        PaymentMethod.CASH_ON_DELIVERY -> CodOrange
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("order_card_${order.orderId}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Row 1: Order ID and Payment Method Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = order.orderId,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = paymentColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = order.paymentMethod.title,
                        color = paymentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dateFormat.format(Date(order.createdAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // Items Summary
            val cleanSummary = order.itemsSummary.replace(Regex("""\s*\[Sold:[^\]]+\]"""), "").trim()
            Text(
                text = cleanSummary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Normal,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Reference
            Text(
                text = "Ref: ${order.paymentReference}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))
            OrderProgressStepper(status = order.status)
            Spacer(modifier = Modifier.height(10.dp))

            // Status and Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = nairaFormat.format(order.totalAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = order.status.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (order.status == OrderStatus.CONFIRMED || order.status == OrderStatus.VERIFIED || order.status == OrderStatus.DELIVERED) SuccessGreen else paymentColor,
                        fontWeight = FontWeight.Black
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    val canCancel = order.status in listOf(
                        OrderStatus.PENDING_VERIFICATION,
                        OrderStatus.CONFIRMED,
                        OrderStatus.VERIFIED
                    )
                    if (canCancel) {
                        OutlinedButton(
                            onClick = onCancelOrder,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.6f)),
                            modifier = Modifier.testTag("cancel_order_button_${order.orderId}")
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cancel", fontSize = 11.5.sp, color = Color(0xFFDC2626))
                        }
                    }

                    if (order.status == OrderStatus.DELIVERED) {
                        OutlinedButton(
                            onClick = onRateOrder,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, AccentGold),
                            modifier = Modifier.testTag("rate_order_button_${order.orderId}")
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = AccentGold, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rate & Review", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = AccentGold)
                        }
                    }

                    if (order.status == OrderStatus.DELIVERED && existingReturn == null) {
                        OutlinedButton(
                            onClick = onRequestReturn,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.7f)),
                            modifier = Modifier.testTag("return_order_button_${order.orderId}")
                        ) {
                            Icon(Icons.Default.AssignmentReturn, contentDescription = null, tint = AccentGold, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Return", fontSize = 11.5.sp, color = AccentGold)
                        }
                    }

                    OutlinedButton(
                        onClick = onReorder,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("reorder_button_${order.orderId}")
                    ) {
                        Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Buy Again", fontSize = 11.5.sp)
                    }

                    OutlinedButton(
                        onClick = onViewReceipt,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("view_receipt_button_${order.orderId}")
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View Slip", fontSize = 11.5.sp)
                    }
                }
            }

            if (existingReturn != null) {
                Spacer(modifier = Modifier.height(10.dp))
                val retColor = when (existingReturn.status) {
                    ReturnRefundStatus.REFUNDED, ReturnRefundStatus.APPROVED_RETURN -> SuccessGreen
                    ReturnRefundStatus.DECLINED -> Color(0xFFDC2626)
                    ReturnRefundStatus.PENDING_REVIEW -> AccentGold
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = retColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, retColor.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Return / Refund: ${existingReturn.status.label}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            color = retColor
                        )
                        Text(
                            text = "Reason: ${existingReturn.reason}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (existingReturn.adminNote.isNotBlank()) {
                            Text(
                                text = "Store Note: ${existingReturn.adminNote}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
