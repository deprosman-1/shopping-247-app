package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import com.example.data.model.SupportTicketStatus
import com.example.data.remote.LiveCallMode
import com.example.ui.theme.AccentGold
import com.example.ui.theme.SuccessGreen
import com.example.util.WhatsAppHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class FaqEntry(val question: String, val answer: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSupportScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val storeProfile by viewModel.storeProfile.collectAsState()
    val tickets by viewModel.supportTickets.collectAsState()
    val allOrders by viewModel.allOrders.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0=FAQ & Tickets, 1=Privacy & Terms
    var faqSearch by remember { mutableStateOf("") }
    var expandedFaqIndex by remember { mutableIntStateOf(0) }

    var ticketOrderId by remember { mutableStateOf("") }
    var ticketSubject by remember { mutableStateOf("") }
    var ticketMessage by remember { mutableStateOf("") }

    val faqs = remember {
        listOf(
            FaqEntry(
                "How do I pay and confirm my order?",
                "At Checkout, pick your delivery zone, select OPay, Moniepoint MFB, POS Transfer, or Payment on Delivery, transfer to the official 24/7 SHOPPING account, and attach your transfer receipt photo. Admin verifies payments promptly and sends an instant notification."
            ),
            FaqEntry(
                "How do I track or cancel my order?",
                "Open Receipts & Tracking in the bottom bar to view real-time status (Awaiting Confirmation -> Confirmed -> Out for Delivery -> Delivered). Pre-dispatch orders can be cancelled immediately with one tap, automatically restoring reserved stock."
            ),
            FaqEntry(
                "What is the Return & Refund policy?",
                "Delivered items with wrong sizing, defects, or transport damage are eligible for Return or Refund within 7 days. Open Receipts -> Request Return/Refund, select your reason, and attach photo proof via the Android Photo Picker."
            ),
            FaqEntry(
                "How do Loyalty Points & Referral Credits work?",
                "You earn 1 Loyalty Point for every ₦1,000 spent (1 point = ₦50 discount credit at Checkout). Share your personal 247-XXXX referral code on your Account page — friends applying a referral code receive +100 bonus points (₦5,000 value)."
            ),
            FaqEntry(
                "Can I shop when my internet is offline?",
                "Yes! Catalog products, cart items, receipts, and orders work offline using local Room storage. Any actions queued offline automatically sync to the cloud the moment internet connection is restored."
            )
        )
    }

    val filteredFaqs = remember(faqSearch, faqs) {
        if (faqSearch.isBlank()) faqs
        else faqs.filter {
            it.question.contains(faqSearch, ignoreCase = true) ||
                it.answer.contains(faqSearch, ignoreCase = true)
        }
    }

    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help Center & Legal", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("help_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize().testTag("help_support_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Direct Contact Actions
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "${storeProfile.storeName} Support Desk",
                            color = AccentGold,
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Need immediate assistance with an order, delivery rider, or partner store? Reach our support desk directly:",
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val phone = storeProfile.phoneNumber.ifBlank { storeProfile.whatsappNumber }
                                    if (phone.isBlank()) {
                                        Toast.makeText(context, "Store phone number not configured yet.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Call Store", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    val wa = storeProfile.whatsappNumber.ifBlank { storeProfile.phoneNumber }
                                    WhatsAppHelper.openWhatsAppChat(
                                        context = context,
                                        rawPhone = wa,
                                        message = "Hello ${storeProfile.storeName} Support, I need assistance with my account/order."
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color(0xFF052E16)),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("WhatsApp", fontWeight = FontWeight.Bold)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.selectLiveCallPreMode(LiveCallMode.AUDIO)
                                    viewModel.openLiveCallScreen()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.White),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("help_live_audio_button")
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Live Audio Talk", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    viewModel.selectLiveCallPreMode(LiveCallMode.VIDEO)
                                    viewModel.openLiveCallScreen()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF052E16)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("help_live_video_button")
                            ) {
                                Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Live Video Call", fontSize = 12.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }

            // Tab Switcher
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        leadingIcon = { Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("FAQ & Support Tickets") },
                        modifier = Modifier.testTag("help_tab_faq_tickets")
                    )
                    FilterChip(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        leadingIcon = { Icon(Icons.Default.PrivacyTip, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Privacy & Terms") },
                        modifier = Modifier.testTag("help_tab_legal")
                    )
                }
            }

            if (selectedTab == 0) {
                // FAQ Search & Accordion
                item {
                    OutlinedTextField(
                        value = faqSearch,
                        onValueChange = { faqSearch = it },
                        label = { Text("Search Help & FAQ") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                items(filteredFaqs.size) { index ->
                    val faq = filteredFaqs[index]
                    val expanded = expandedFaqIndex == index
                    Card(
                        onClick = { expandedFaqIndex = if (expanded) -1 else index },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = faq.question,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (expanded) "Collapse" else "Expand"
                                )
                            }
                            if (expanded) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = faq.answer,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Submit Support Ticket Form
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Open a Support Ticket",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            if (allOrders.isNotEmpty()) {
                                val latestOrder = allOrders.first()
                                OutlinedTextField(
                                    value = ticketOrderId,
                                    onValueChange = { ticketOrderId = it },
                                    label = { Text("Order ID (optional, e.g. ${latestOrder.orderId})") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                OutlinedTextField(
                                    value = ticketOrderId,
                                    onValueChange = { ticketOrderId = it },
                                    label = { Text("Order ID (optional)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            OutlinedTextField(
                                value = ticketSubject,
                                onValueChange = { ticketSubject = it },
                                label = { Text("Subject (e.g., Delivery inquiry / Payment verification)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("support_ticket_subject_input")
                            )
                            OutlinedTextField(
                                value = ticketMessage,
                                onValueChange = { ticketMessage = it },
                                label = { Text("Describe how we can help") },
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier.fillMaxWidth().testTag("support_ticket_message_input")
                            )
                            Button(
                                onClick = {
                                    viewModel.submitSupportTicket(
                                        subject = ticketSubject,
                                        message = ticketMessage,
                                        orderId = ticketOrderId
                                    ) { ok, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        if (ok) {
                                            ticketSubject = ""
                                            ticketMessage = ""
                                            ticketOrderId = ""
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("submit_support_ticket_button")
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Submit Support Ticket", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Existing Support Tickets History
                if (tickets.isNotEmpty()) {
                    item {
                        Text(
                            text = "Your Support Tickets (${tickets.size})",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    items(tickets, key = { it.id }) { ticket ->
                        val statusColor = when (ticket.status) {
                            SupportTicketStatus.OPEN -> Color(0xFFD97706)
                            SupportTicketStatus.IN_PROGRESS -> Color(0xFF2563EB)
                            SupportTicketStatus.RESOLVED -> Color(0xFF16A34A)
                        }
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${ticket.id} • ${ticket.subject}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = statusColor.copy(alpha = 0.14f)
                                    ) {
                                        Text(
                                            text = ticket.status.label,
                                            color = statusColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                if (ticket.orderId.isNotBlank()) {
                                    Text("Order: ${ticket.orderId}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                                Text(ticket.message, style = MaterialTheme.typography.bodySmall)
                                if (ticket.adminReply.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF16A34A).copy(alpha = 0.1f)
                                    ) {
                                        Text(
                                            text = "Admin Reply: ${ticket.adminReply}",
                                            color = Color(0xFF15803D),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = dateFormatter.format(Date(ticket.createdAt)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                // Privacy Policy & Terms of Service (Play Policy Compliant)
                item {
                    Card(shape = RoundedCornerShape(14.dp)) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text("Privacy Policy & Data Protection", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            }
                            Text(
                                text = "${storeProfile.storeName} collects only the minimum information required to process and deliver your orders (name, phone number, delivery address, and payment receipt proofs you explicitly choose to upload). Password credentials are protected using salted SHA-256 cryptographic hashes. Photos are selected solely via the privacy-preserving Android Photo Picker without accessing broad device storage.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "You may request data deletion or account updates at any time through Support Tickets above or by contacting store management.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                item {
                    Card(shape = RoundedCornerShape(14.dp)) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Gavel, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text("Terms of Sale & Partner Hosting", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            }
                            Text(
                                text = "1. Order Confirmation: Bank/POS transfers are verified before dispatch; Payment on Delivery orders are confirmed via phone/WhatsApp.\n" +
                                    "2. Cancellations: Orders may be cancelled prior to dispatch from Receipts & Tracking.\n" +
                                    "3. Returns & Refunds: Eligible items may be submitted for return/refund within 7 days of delivery with clear photo documentation.\n" +
                                    "4. Partner Sellers: Verified partner business items hosted on 24/7 SHOPPING follow identical warranty, delivery ETA, and customer protection standards.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
