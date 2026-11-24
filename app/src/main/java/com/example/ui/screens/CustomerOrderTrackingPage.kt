package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import com.example.OrderTrackingFilter
import com.example.data.model.CreditVerificationStatus
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.StatusUpdate
import com.example.ui.components.OrderProgressStepper
import com.example.ui.theme.AccentGold
import com.example.ui.theme.CodOrange
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.MoniepointBlue
import com.example.ui.theme.OPayGreen
import com.example.ui.theme.PalmPayPurple
import com.example.ui.theme.PaystackTeal
import com.example.ui.theme.PosPurple
import com.example.ui.theme.SuccessGreen
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CustomerOrderTrackingPage(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val trackingState by viewModel.orderTrackingState.collectAsState()
    val scopedOrders by viewModel.customerScopedOrders.collectAsState()
    val isSupabaseConnected by viewModel.isSupabaseConnected.collectAsState()

    LaunchedEffect(Unit) {
        if (!trackingState.hasActiveSearch) {
            viewModel.refreshCustomerOrderTracking()
        }
    }

    CustomerOrderTrackingContent(
        trackingState = trackingState,
        scopedOrders = scopedOrders,
        isSupabaseConnected = isSupabaseConnected,
        onQueryChange = { viewModel.updateOrderTrackingQuery(it) },
        onSearch = { viewModel.searchAndTrackOrderFromSupabase() },
        onClearSearch = { viewModel.clearOrderTrackingSearch() },
        onSelectFilter = { viewModel.setOrderTrackingFilter(it) },
        onRefreshAll = {
            if (trackingState.hasActiveSearch) {
                viewModel.searchAndTrackOrderFromSupabase()
            } else {
                viewModel.refreshCustomerOrderTracking()
            }
        },
        onRefreshOrder = { orderId ->
            viewModel.refreshSingleOrderTracking(orderId) { _, msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        },
        onViewReceipt = { viewModel.showReceipt(it) },
        onBrowseStore = { viewModel.navigateToHome() },
        modifier = modifier
    )
}

@Composable
fun CustomerOrderTrackingContent(
    trackingState: com.example.OrderTrackingUiState,
    scopedOrders: List<Order>,
    isSupabaseConnected: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClearSearch: () -> Unit,
    onSelectFilter: (OrderTrackingFilter) -> Unit,
    onRefreshAll: () -> Unit,
    onRefreshOrder: (String) -> Unit,
    onViewReceipt: (Order) -> Unit,
    onBrowseStore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nairaFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "NG")).apply {
            maximumFractionDigits = 0
        }
    }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    val baseOrders = remember(trackingState.hasActiveSearch, trackingState.searchedOrders, scopedOrders) {
        if (trackingState.hasActiveSearch) trackingState.searchedOrders else scopedOrders
    }

    val displayedOrders = remember(baseOrders, trackingState.selectedFilter) {
        when (trackingState.selectedFilter) {
            OrderTrackingFilter.ALL -> baseOrders
            OrderTrackingFilter.ACTIVE -> baseOrders.filter {
                it.status == OrderStatus.PENDING_VERIFICATION ||
                    it.status == OrderStatus.CONFIRMED ||
                    it.status == OrderStatus.VERIFIED ||
                    it.status == OrderStatus.OUT_FOR_DELIVERY
            }
            OrderTrackingFilter.DELIVERED -> baseOrders.filter { it.status == OrderStatus.DELIVERED }
            OrderTrackingFilter.CANCELLED -> baseOrders.filter { it.status == OrderStatus.CANCELLED }
        }
    }

    val activeCount = remember(baseOrders) {
        baseOrders.count {
            it.status != OrderStatus.DELIVERED && it.status != OrderStatus.CANCELLED
        }
    }
    val deliveredCount = remember(baseOrders) {
        baseOrders.count { it.status == OrderStatus.DELIVERED }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("account_order_tracking_page")
    ) {
        // Hero Header Card showing Live Supabase 'orders' Sync Status
        Surface(
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.2.dp, AccentGold.copy(alpha = 0.65f)),
            tonalElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("order_tracking_hero_card")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF022C22),
                                Color(0xFF052E16),
                                Color(0xFF064E3B)
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = CircleShape,
                                color = AccentGold.copy(alpha = 0.2f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.LocalShipping,
                                        contentDescription = "Order Tracking",
                                        tint = AccentGold,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "LIVE ORDER TRACKING",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    color = AccentGold
                                )
                                Text(
                                    text = "Real-time delivery milestones from Supabase",
                                    fontSize = 11.5.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }

                        val liveBadge = trackingState.isLiveSynced || isSupabaseConnected
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = (if (liveBadge) SuccessGreen else AccentGold).copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, (if (liveBadge) SuccessGreen else AccentGold).copy(alpha = 0.6f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (liveBadge) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                    contentDescription = if (liveBadge) "Supabase Live Connected" else "Cached Status",
                                    tint = if (liveBadge) SuccessGreen else AccentGold,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (liveBadge) "LIVE" else "CACHE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (liveBadge) SuccessGreen else AccentGold
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TrackingStatPill(
                            label = "Tracked",
                            value = baseOrders.size.toString(),
                            accent = CyberCyan,
                            modifier = Modifier.weight(1f)
                        )
                        TrackingStatPill(
                            label = "In Transit",
                            value = activeCount.toString(),
                            accent = AccentGold,
                            modifier = Modifier.weight(1f)
                        )
                        TrackingStatPill(
                            label = "Delivered",
                            value = deliveredCount.toString(),
                            accent = SuccessGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (trackingState.lastSyncedAtMillis > 0L) {
                                "Updated ${dateFormat.format(Date(trackingState.lastSyncedAtMillis))}"
                            } else {
                                "Tap refresh to query Supabase 'orders' table"
                            },
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.78f),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = onRefreshAll,
                            enabled = !trackingState.isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentGold,
                                contentColor = Color(0xFF052E16)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("order_tracking_refresh_button")
                        ) {
                            if (trackingState.isLoading) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF052E16),
                                    modifier = Modifier.size(15.dp)
                                )
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (trackingState.isLoading) "Syncing..." else "Refresh Status",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    AnimatedVisibility(visible = trackingState.statusBanner.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Black.copy(alpha = 0.25f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = trackingState.statusBanner,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.92f),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Track by Order ID / Phone / Reference Search Row
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "TRACK ANY ORDER BY ID, PHONE OR PAYMENT REF",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = CyberCyan
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = trackingState.query,
                        onValueChange = onQueryChange,
                        placeholder = { Text("e.g. ORD-1234567 or 080...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyberCyan) },
                        trailingIcon = {
                            if (trackingState.query.isNotBlank() || trackingState.hasActiveSearch) {
                                IconButton(
                                    onClick = onClearSearch,
                                    modifier = Modifier.testTag("order_tracking_clear_button")
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("order_tracking_query_input")
                    )
                    Button(
                        onClick = onSearch,
                        enabled = !trackingState.isLoading,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("order_tracking_search_button")
                    ) {
                        Text("Track", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OrderTrackingFilter.entries.forEach { filter ->
                val selected = trackingState.selectedFilter == filter
                FilterChip(
                    selected = selected,
                    onClick = { onSelectFilter(filter) },
                    label = {
                        Text(
                            text = filter.label,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Black else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentGold.copy(alpha = 0.2f),
                        selectedLabelColor = AccentGold
                    ),
                    modifier = Modifier.testTag("order_tracking_filter_${filter.name.lowercase()}")
                )
            }
        }

        if (displayedOrders.isEmpty()) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("order_tracking_empty_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalShipping,
                        contentDescription = null,
                        tint = CyberCyan.copy(alpha = 0.6f),
                        modifier = Modifier.size(46.dp)
                    )
                    Text(
                        text = if (trackingState.hasActiveSearch) {
                            "No Matching Order Found"
                        } else {
                            "No Orders to Track Yet"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (trackingState.hasActiveSearch) {
                            "Double-check the Order ID (e.g. ORD-1234567), payment reference, or phone number and tap Track."
                        } else {
                            "Orders placed from your account or tracked by ID appear here with live milestones from Supabase 'orders'."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (trackingState.hasActiveSearch) {
                            OutlinedButton(
                                onClick = onClearSearch,
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("Show All My Orders", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = onBrowseStore,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Browse Catalog", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            displayedOrders.forEach { order ->
                TrackedOrderDetailCard(
                    order = order,
                    isHighlighted = trackingState.focusedOrderId == order.orderId,
                    isRefreshing = trackingState.isLoading && trackingState.focusedOrderId == order.orderId,
                    nairaFormat = nairaFormat,
                    dateFormat = dateFormat,
                    onRefreshOrder = { onRefreshOrder(order.orderId) },
                    onViewReceipt = { onViewReceipt(order) }
                )
            }
        }
    }
}

@Composable
private fun TrackingStatPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                color = accent
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun TrackedOrderDetailCard(
    order: Order,
    isHighlighted: Boolean,
    isRefreshing: Boolean,
    nairaFormat: NumberFormat,
    dateFormat: SimpleDateFormat,
    onRefreshOrder: () -> Unit,
    onViewReceipt: () -> Unit
) {
    val context = LocalContext.current
    val statusColor = when (order.status) {
        OrderStatus.PENDING_VERIFICATION -> AccentGold
        OrderStatus.CONFIRMED, OrderStatus.VERIFIED -> CyberCyan
        OrderStatus.OUT_FOR_DELIVERY -> MoniepointBlue
        OrderStatus.DELIVERED -> SuccessGreen
        OrderStatus.CANCELLED -> Color(0xFFDC2626)
    }
    val paymentColor = when (order.paymentMethod) {
        PaymentMethod.PAYSTACK -> PaystackTeal
        PaymentMethod.OPAY -> OPayGreen
        PaymentMethod.MONIEPOINT -> MoniepointBlue
        PaymentMethod.PALMPAY -> PalmPayPurple
        PaymentMethod.POS_TRANSFER -> PosPurple
        PaymentMethod.CASH_ON_DELIVERY -> CodOrange
    }

    val timeline = remember(order.statusUpdates, order.status, order.verificationNote, order.createdAt) {
        val base = order.statusUpdates.sortedBy { it.timestamp }.toMutableList()
        if (base.none { it.status == OrderStatus.PENDING_VERIFICATION } && order.createdAt > 0L) {
            base.add(
                0,
                StatusUpdate(
                    status = OrderStatus.PENDING_VERIFICATION,
                    timestamp = order.createdAt,
                    note = "Order placed & submitted to ODIYA"
                )
            )
        }
        if (base.none { it.status == order.status }) {
            base.add(
                StatusUpdate(
                    status = order.status,
                    timestamp = System.currentTimeMillis(),
                    note = order.verificationNote.ifBlank { order.status.label }
                )
            )
        }
        base.distinctBy { "${it.status.name}|${it.note}" }
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)
        ),
        border = BorderStroke(
            width = if (isHighlighted) 1.8.dp else 1.dp,
            color = if (isHighlighted) AccentGold else statusColor.copy(alpha = 0.45f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tracked_order_card_${order.orderId}")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.orderId,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = dateFormat.format(Date(order.createdAt)),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.55f))
                ) {
                    Text(
                        text = order.status.label,
                        color = statusColor,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = nairaFormat.format(order.totalAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = paymentColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = order.paymentMethod.title,
                        color = paymentColor,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (order.creditVerificationStatus != CreditVerificationStatus.PENDING_CREDIT) {
                val creditColor = when (order.creditVerificationStatus) {
                    CreditVerificationStatus.CONFIRMED_CREDITED -> SuccessGreen
                    CreditVerificationStatus.PAYMENT_FAILED_UNCREDITED -> Color(0xFFDC2626)
                    CreditVerificationStatus.NETWORK_DELAYED_PENDING -> AccentGold
                    CreditVerificationStatus.PENDING_CREDIT -> CyberCyan
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = creditColor.copy(alpha = 0.12f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = creditColor, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = order.creditVerificationStatus.label,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = creditColor
                        )
                    }
                }
            }

            OrderProgressStepper(status = order.status)

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Text(
                text = "STATUS TIMELINE (SUPABASE 'orders')",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
                color = CyberCyan
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                timeline.forEachIndexed { idx, step ->
                    val isLatest = idx == timeline.lastIndex
                    val nodeColor = when (step.status) {
                        OrderStatus.DELIVERED -> SuccessGreen
                        OrderStatus.CANCELLED -> Color(0xFFDC2626)
                        OrderStatus.OUT_FOR_DELIVERY -> MoniepointBlue
                        OrderStatus.CONFIRMED, OrderStatus.VERIFIED -> CyberCyan
                        OrderStatus.PENDING_VERIFICATION -> AccentGold
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(20.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (isLatest) 12.dp else 9.dp)
                                    .clip(CircleShape)
                                    .background(if (isLatest) nodeColor else nodeColor.copy(alpha = 0.65f))
                            )
                            if (idx < timeline.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(22.dp)
                                        .background(nodeColor.copy(alpha = 0.35f))
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = step.status.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isLatest) FontWeight.Black else FontWeight.Bold,
                                    color = if (isLatest) nodeColor else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = dateFormat.format(Date(step.timestamp)),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (step.note.isNotBlank()) {
                                Text(
                                    text = step.note,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (order.deliveryAddress.isNotBlank()) {
                val cleanAddr = order.deliveryAddress.replace(Regex("""\s*\[Delivery:[^\]]*\]"""), "").trim()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = cleanAddr,
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (order.dispatchRiderWhatsapp.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SuccessGreen.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Dispatch Rider Assigned", fontSize = 11.sp, fontWeight = FontWeight.Black, color = SuccessGreen)
                            Text(order.dispatchRiderWhatsapp, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${order.dispatchRiderWhatsapp}")))
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Dialer unavailable", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Default.Call, contentDescription = "Call Rider", modifier = Modifier.size(15.dp))
                            }
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val cleanWa = order.dispatchRiderWhatsapp.replace("+", "").replace(" ", "").replace("-", "")
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$cleanWa")))
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "WhatsApp unavailable", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = "WhatsApp Rider", tint = SuccessGreen, modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRefreshOrder,
                    enabled = !isRefreshing,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("refresh_tracked_order_${order.orderId}")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isRefreshing) "Checking..." else "Refresh Order", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onViewReceipt,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("tracked_order_receipt_${order.orderId}")
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("View Receipt", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
