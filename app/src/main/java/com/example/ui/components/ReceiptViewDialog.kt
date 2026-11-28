package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.CreditVerificationStatus
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.StoreProfileConfig
import com.example.ui.theme.AccentGold
import com.example.ui.theme.CodOrange
import com.example.ui.theme.MoniepointBlue
import com.example.ui.theme.OPayGreen
import com.example.ui.theme.PalmPayPurple
import com.example.ui.theme.PaystackTeal
import com.example.ui.theme.PosPurple
import com.example.ui.theme.SuccessGreen
import com.example.util.OrderSlipFormatter
import com.example.util.PdfInvoiceExporter
import com.example.util.WhatsAppHelper
import com.example.util.WordInvoiceExporter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReceiptViewDialog(
    order: Order,
    storeProfile: StoreProfileConfig = StoreProfileConfig(),
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showZoomedReceipt by remember { mutableStateOf(false) }
    val storeTitle = storeProfile.storeName.ifBlank { "ODIYA" }.uppercase()
    val nairaFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "NG")).apply {
            maximumFractionDigits = 0
        }
    }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    val paymentColor = when (order.paymentMethod) {
        PaymentMethod.PAYSTACK -> PaystackTeal
        PaymentMethod.OPAY -> OPayGreen
        PaymentMethod.MONIEPOINT -> MoniepointBlue
        PaymentMethod.PALMPAY -> PalmPayPurple
        PaymentMethod.POS_TRANSFER -> PosPurple
        PaymentMethod.CASH_ON_DELIVERY -> CodOrange
    }
    var isTargetRecipientSelected by remember(order.orderId) { mutableStateOf(true) }
    var isExpandedView by rememberSaveable { mutableStateOf(false) }
    val parsedSlip = remember(order.itemsSummary, order.deliveryAddress) {
        OrderSlipFormatter.parseOrderSlipDetails(order)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(if (isExpandedView) 0.98f else 0.92f)
                .then(if (isExpandedView) Modifier.fillMaxHeight(0.95f) else Modifier)
                .padding(if (isExpandedView) 2.dp else 8.dp)
                .testTag("receipt_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(if (isExpandedView) 22.dp else 18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = paymentColor.copy(alpha = 0.15f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = paymentColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "$storeTitle ORDER INVOICE",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "$storeTitle • Purchase Invoice",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val phonesLine = listOf(storeProfile.phoneNumber, storeProfile.secondaryPhoneNumber).filter { it.isNotBlank() }.joinToString(" / ")
                            if (phonesLine.isNotBlank() || storeProfile.storeAddress.isNotBlank() || storeProfile.supportEmail.isNotBlank()) {
                                val storeMeta = listOfNotNull(
                                    phonesLine.takeIf { it.isNotBlank() }?.let { "📞 $it" },
                                    storeProfile.supportEmail.takeIf { it.isNotBlank() }?.let { "✉️ $it" },
                                    storeProfile.storeAddress.takeIf { it.isNotBlank() }?.let { "📍 $it" }
                                ).joinToString(" • ")
                                Text(
                                    text = storeMeta,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { isExpandedView = !isExpandedView },
                            modifier = Modifier.testTag("receipt_expand_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isExpandedView) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = if (isExpandedView) "Compact Receipt View" else "Expand Receipt View",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("receipt_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Page"
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Clickable ODIYA Official Target Recipient Logo Card
                Surface(
                    onClick = {
                        isTargetRecipientSelected = true
                        Toast.makeText(context, "✓ $storeTitle selected as official receipt target recipient", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isTargetRecipientSelected) SuccessGreen.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.5.dp, if (isTargetRecipientSelected) SuccessGreen else AccentGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("receipt_target_recipient_logo_button")
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.shopping_logo_icon_1794752250458),
                            contentDescription = "ODIYA Target Recipient Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, AccentGold, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "TARGET RECIPIENT: $storeTitle ✓",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = SuccessGreen
                            )
                            Text(
                                text = "Official Verified Recipient for Word & PDF Invoice Exports",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Verification & Credit-Alert Stamp Banner
                val creditBannerColor = when (order.creditVerificationStatus) {
                    CreditVerificationStatus.CONFIRMED_CREDITED -> SuccessGreen
                    CreditVerificationStatus.PAYMENT_FAILED_UNCREDITED -> Color(0xFFDC2626)
                    CreditVerificationStatus.NETWORK_DELAYED_PENDING -> Color(0xFFF59E0B)
                    CreditVerificationStatus.PENDING_CREDIT -> paymentColor
                }
                val creditTitle = when (order.creditVerificationStatus) {
                    CreditVerificationStatus.CONFIRMED_CREDITED -> "ORDER & PAYMENT CONFIRMATION RECEIPT ✓"
                    CreditVerificationStatus.PAYMENT_FAILED_UNCREDITED -> "ORDER & PAYMENT FAILED (Not Yet Credited)"
                    CreditVerificationStatus.NETWORK_DELAYED_PENDING -> "ORDER & PAYMENT FAILED (Network Delay — Auto-Syncing)"
                    CreditVerificationStatus.PENDING_CREDIT -> order.status.label
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = creditBannerColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, creditBannerColor.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = creditTitle,
                            tint = creditBannerColor
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = creditTitle,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.5.sp,
                                color = creditBannerColor
                            )
                            Text(
                                text = order.verificationNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(14.dp))

                // Receipt Meta (Customer & Delivery info, no order profit summary)
                ReceiptRow(label = "Order Reference", value = order.orderId, isMono = true)
                ReceiptRow(label = "Date & Time", value = dateFormat.format(Date(order.createdAt)))
                ReceiptRow(label = "Order Status", value = order.status.label, highlight = true)
                ReceiptRow(label = "Customer Name", value = order.customerName)
                ReceiptRow(label = "Customer Phone", value = order.customerPhone)
                ReceiptRow(label = "Delivery Destination", value = parsedSlip.cleanDeliveryAddress)
                if (parsedSlip.deliveryConditionAndFee.isNotBlank()) {
                    ReceiptRow(label = "Shipping / Delivery Fee", value = parsedSlip.deliveryConditionAndFee, highlight = true)
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))

                // Payment Meta
                ReceiptRow(label = "Payment Option", value = order.paymentMethod.title, highlight = true)
                ReceiptRow(label = "Merchant Destination", value = order.paymentAccount)
                ReceiptRow(label = "Sender / Payer", value = order.senderName)
                ReceiptRow(label = "Transaction Ref / STAN", value = order.paymentReference, isMono = true)

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))

                // Purchased Items & Individual Prices Summary
                Text(
                    text = "Purchased Items & Prices (${parsedSlip.items.size})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("receipt_items_breakdown")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (parsedSlip.items.isEmpty()) {
                            Text(
                                text = order.itemsSummary.ifBlank { "Items recorded in order archive" },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            parsedSlip.items.forEachIndexed { idx, lineItem ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(
                                            text = "${idx + 1}. ${lineItem.titleAndSpecs}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (lineItem.unitPriceText.isNotBlank()) {
                                            Text(
                                                text = "Unit price: ${lineItem.unitPriceText}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (lineItem.lineTotalText.isNotBlank()) {
                                        Text(
                                            text = lineItem.lineTotalText,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                if (idx < parsedSlip.items.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                                }
                            }
                        }
                    }
                }

                if (order.receiptImageUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "📷 Attached Payment Receipt Slip (Tap to Zoom)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.05f))
                            .clickable { showZoomedReceipt = true }
                            .testTag("receipt_dialog_slip_image")
                    ) {
                        AsyncImage(
                            model = com.example.util.ProductImageHelper.buildImageRequest(
                                context = context,
                                imageUrl = order.receiptImageUrl,
                                title = "Receipt ${order.orderId}",
                                highRes = true
                            ),
                            contentDescription = "Uploaded Receipt Slip",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                            filterQuality = FilterQuality.High
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.65f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Icon(Icons.Default.ZoomIn, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Zoom", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (showZoomedReceipt) {
                        ZoomableImageDialog(
                            imageUrl = order.receiptImageUrl,
                            title = "Payment Receipt • ${order.orderId}",
                            onDismiss = { showZoomedReceipt = false }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Total Amount Paid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Settlement:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = nairaFormat.format(order.totalAmount),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                OrderProgressStepper(status = order.status)
                Spacer(modifier = Modifier.height(14.dp))

                val savePdfLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("application/pdf")
                ) { targetUri ->
                    if (targetUri != null) {
                        val ok = PdfInvoiceExporter.writeToUri(context, targetUri, order, storeProfile)
                        Toast.makeText(
                            context,
                            if (ok) "PDF Invoice ${order.orderId} saved for $storeTitle!" else "Failed to save PDF",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                val saveWordLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("application/msword")
                ) { targetUri ->
                    if (targetUri != null) {
                        val ok = WordInvoiceExporter.writeToUri(context, targetUri, order, storeProfile)
                        Toast.makeText(
                            context,
                            if (ok) "Word Invoice ${order.orderId} (.doc) saved for $storeTitle!" else "Failed to save Word doc",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                // Word Format (.doc) Export Buttons targeting ODIYA
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            isTargetRecipientSelected = true
                            WordInvoiceExporter.shareWordInvoice(context, order, storeProfile)
                        },
                        border = BorderStroke(1.dp, MoniepointBlue),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("receipt_share_word_button")
                    ) {
                        Icon(Icons.Default.Description, contentDescription = "Word Format", tint = MoniepointBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Word Format", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MoniepointBlue)
                    }

                    Button(
                        onClick = {
                            isTargetRecipientSelected = true
                            saveWordLauncher.launch("${order.orderId}_ODIYA.doc")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MoniepointBlue, contentColor = Color.White),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("receipt_save_word_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Save Word", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // PDF Format (.pdf) Export Buttons targeting ODIYA
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            isTargetRecipientSelected = true
                            PdfInvoiceExporter.sharePdfInvoice(context, order, storeProfile)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("receipt_share_pdf_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "PDF Format", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PDF Format", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            isTargetRecipientSelected = true
                            savePdfLauncher.launch("${order.orderId}_ODIYA.pdf")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = paymentColor),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("receipt_save_pdf_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save PDF", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Order Receipt Ref", order.paymentReference)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Reference Copied to Clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Copy Ref", fontSize = 12.sp)
                    }

                    Button(
                        onClick = { WhatsAppHelper.sendCustomerOrderReceiptToStore(context, order, storeProfile) },
                        colors = ButtonDefaults.buttonColors(containerColor = OPayGreen, contentColor = Color.White),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("receipt_whatsapp_store_button")
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("WhatsApp", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptRow(
    label: String,
    value: String,
    isMono: Boolean = false,
    highlight: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.42f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium,
            fontFamily = if (isMono) FontFamily.Monospace else FontFamily.Default,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.58f)
        )
    }
}
