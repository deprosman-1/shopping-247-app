package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.MainViewModel
import com.example.data.model.PreOrderRequest
import com.example.data.model.PreOrderStatus
import com.example.data.model.ProductCategory
import com.example.ui.theme.AccentGold
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.SuccessGreen
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

/**
 * Rectangular Box Component for Customers to Pre-Order & Request
 * any Product or Brand not currently available in the 24/7 SHOPPING Catalog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreOrderRequestCard(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onOpenDialog: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentCustomer by viewModel.currentCustomer.collectAsState()
    val savedDeliveryInfo by viewModel.savedDeliveryInfo.collectAsState()

    var customerNameInput by remember(currentCustomer?.id, savedDeliveryInfo.fullName) {
        mutableStateOf(
            currentCustomer?.fullName?.takeIf { it.isNotBlank() }
                ?: savedDeliveryInfo.fullName.takeIf { it.isNotBlank() }
                ?: ""
        )
    }
    var customerPhoneInput by remember(currentCustomer?.id, savedDeliveryInfo.phoneNumber) {
        mutableStateOf(
            currentCustomer?.phoneNumber?.takeIf { it.isNotBlank() }
                ?: savedDeliveryInfo.phoneNumber.takeIf { it.isNotBlank() }
                ?: ""
        )
    }
    var productName by remember { mutableStateOf("") }
    var brandName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ProductCategory.CLOTHES.displayName) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var sizeInput by remember { mutableStateOf("") }
    var colorInput by remember { mutableStateOf("") }
    var budgetInput by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf("") }
    var isSubmitted by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        border = BorderStroke(1.5.dp, CyberCyan),
        modifier = modifier
            .fillMaxWidth()
            .testTag("customer_preorder_request_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = CyberCyan.copy(alpha = 0.2f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.NewReleases, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(22.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "PRE-ORDER & SPECIAL BRAND REQUEST",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = CyberCyan
                        )
                        Text(
                            text = "Can't find a product or brand? Tell us & we will source it!",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AccentGold.copy(alpha = 0.2f)
                ) {
                    Text("LIVE REQUEST", color = AccentGold, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(14.dp))

            // Customer Contact Identity Row (pre-filled when signed in, editable for guests)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = customerNameInput,
                    onValueChange = { customerNameInput = it },
                    label = { Text("Your Full Name *") },
                    placeholder = { Text("Customer name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("preorder_customer_name_input")
                )
                OutlinedTextField(
                    value = customerPhoneInput,
                    onValueChange = { customerPhoneInput = it },
                    label = { Text("Phone / WhatsApp *") },
                    placeholder = { Text("+234...") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("preorder_customer_phone_input")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Form Inputs
            OutlinedTextField(
                value = productName,
                onValueChange = { productName = it },
                label = { Text("Product Name / Description *") },
                placeholder = { Text("e.g. Gucci Signature Sneakers or Cashmere Suit") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("preorder_product_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = brandName,
                    onValueChange = { brandName = it },
                    label = { Text("Brand Name *") },
                    placeholder = { Text("Gucci, Nike, Zara...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("preorder_brand_input")
                )

                // Category Dropdown (Bold Black & Bold Red on Crisp White Popup)
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("preorder_category_selector")
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                        containerColor = Color.White
                    ) {
                        ProductCategory.entries.forEach { cat ->
                            val isSelected = category == cat.displayName
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = getCategoryIcon(cat),
                                        contentDescription = cat.displayName,
                                        tint = if (isSelected) Color(0xFFB91C1C) else Color(0xFF111827),
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                text = {
                                    Text(
                                        text = cat.displayName,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                        color = if (isSelected) Color(0xFFB91C1C) else Color(0xFF111827)
                                    )
                                },
                                trailingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFFB91C1C),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                onClick = {
                                    category = cat.displayName
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = sizeInput,
                    onValueChange = { sizeInput = it },
                    label = { Text("Preferred Size") },
                    placeholder = { Text("e.g. EU 43 / Medium") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = colorInput,
                    onValueChange = { colorInput = it },
                    label = { Text("Color Preference") },
                    placeholder = { Text("Black, Gold, Camel...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = budgetInput,
                onValueChange = { budgetInput = it },
                label = { Text("Target Budget (₦)") },
                placeholder = { Text("e.g. 50000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("preorder_budget_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Special Request Details / Notes") },
                placeholder = { Text("Provide any model details, year, or specific requirements...") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth().testTag("preorder_description_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            val mediaPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia()
            ) { uri: Uri? ->
                if (uri != null) {
                    photoUrl = uri.toString()
                    Toast.makeText(context, "✅ Media Attached Successfully!", Toast.LENGTH_SHORT).show()
                }
            }

            OutlinedButton(
                onClick = {
                    mediaPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (photoUrl.isBlank()) "Attach Photo or Video of Item" else "✅ Media Attached (Tap to change)")
            }

            if (photoUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(photoUrl)).apply {
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
                            model = photoUrl,
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

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    if (productName.isBlank() || brandName.isBlank()) {
                        Toast.makeText(context, "Please enter product name and brand", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (customerNameInput.isBlank() || customerPhoneInput.isBlank()) {
                        Toast.makeText(context, "Please enter your Name & Phone/WhatsApp so Admin can reach you", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val budget = budgetInput.toDoubleOrNull() ?: 0.0
                    val success = viewModel.submitPreOrderRequest(
                        productName = productName,
                        brandName = brandName,
                        category = category,
                        preferredSize = sizeInput,
                        preferredColor = colorInput,
                        targetBudget = budget,
                        description = description,
                        photoUrl = photoUrl,
                        customerNameOverride = customerNameInput.trim(),
                        customerPhoneOverride = customerPhoneInput.trim()
                    )
                    if (success) {
                        Toast.makeText(context, "✅ Pre-Order Request Sent & Synced to Admin File!", Toast.LENGTH_LONG).show()
                        productName = ""
                        brandName = ""
                        sizeInput = ""
                        colorInput = ""
                        budgetInput = ""
                        description = ""
                        photoUrl = ""
                        isSubmitted = true
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("submit_preorder_button")
            ) {
                Icon(Icons.Default.Send, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("SUBMIT PRE-ORDER REQUEST TO ADMIN", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 12.sp)
            }
        }
    }
}

/**
 * Modal Dialog for Pre-Order & Special Brand Request
 */
@Composable
fun PreOrderRequestDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(10.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp).verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pre-Order / Special Request", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                PreOrderRequestCard(viewModel = viewModel)
            }
        }
    }
}

/**
 * Customer Live Pre-Order Status List
 */
@Composable
fun CustomerPreOrdersList(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val requests by viewModel.preOrderRequests.collectAsState()
    val currentCust by viewModel.currentCustomer.collectAsState()
    val savedInfo by viewModel.savedDeliveryInfo.collectAsState()
    val myRequests = remember(requests, currentCust, savedInfo) {
        val custPhoneDigits = (currentCust?.phoneNumber ?: savedInfo.phoneNumber).filter { it.isDigit() }
        val custName = (currentCust?.fullName ?: savedInfo.fullName).trim()
        requests.filter { req ->
            val reqPhoneDigits = req.customerPhone.filter { it.isDigit() }
            val phoneMatch = custPhoneDigits.length >= 7 && reqPhoneDigits.length >= 7 &&
                custPhoneDigits.takeLast(minOf(10, custPhoneDigits.length)) == reqPhoneDigits.takeLast(minOf(10, reqPhoneDigits.length))
            val nameMatch = custName.isNotBlank() && req.customerName.equals(custName, ignoreCase = true)
            when {
                currentCust != null -> req.customerId == currentCust!!.id || phoneMatch || nameMatch
                custPhoneDigits.length >= 7 -> phoneMatch || nameMatch
                else -> req.customerId == "guest"
            }
        }
    }
    val nairaFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "NG")).apply { maximumFractionDigits = 0 }
    }
    val dateFormat = remember { java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val context = LocalContext.current
    var replyInputs = remember { androidx.compose.runtime.mutableStateMapOf<String, String>() }

    if (myRequests.isNotEmpty()) {
        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "YOUR PRE-ORDER & BRAND REQUESTS",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )

            myRequests.forEach { req ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(req.productName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when (req.status) {
                                    PreOrderStatus.CONFIRMED_AVAILABLE -> SuccessGreen
                                    PreOrderStatus.PROCESSING -> CyberCyan
                                    PreOrderStatus.PENDING -> AccentGold
                                    PreOrderStatus.COMPLETED -> SuccessGreen
                                    PreOrderStatus.DECLINED -> Color(0xFFEF4444)
                                }
                            ) {
                                Text(
                                    text = req.status.label,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Brand: ${req.brandName} • Size: ${req.preferredSize} • Color: ${req.preferredColor}", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (req.targetBudget > 0) {
                            Text("Target Budget: ${nairaFormat.format(req.targetBudget)}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }

                        if (req.photoUrl.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
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
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Play/View Media", tint = Color.White)
                                        }
                                    }
                                }
                            }
                        }

                        if (req.adminResponse.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = CyberCyan.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Storefront, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("ADMIN RESPONSE", fontWeight = FontWeight.Black, fontSize = 10.5.sp, color = CyberCyan)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(req.adminResponse, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    if (req.adminQuotedPrice > 0) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Admin Quoted Price: ${nairaFormat.format(req.adminQuotedPrice)}", fontSize = 11.5.sp, fontWeight = FontWeight.Black, color = SuccessGreen)
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
                                        Icon(Icons.Default.Send, contentDescription = null, tint = AccentGold, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("YOUR FEEDBACK REPLY", fontWeight = FontWeight.Black, fontSize = 10.5.sp, color = AccentGold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(req.customerReply, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        // Rectangular Box for Admin and Customer Feedback Reply
                        Spacer(modifier = Modifier.height(10.dp))
                        val currentReplyText = replyInputs[req.id] ?: ""
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = currentReplyText,
                                onValueChange = { replyInputs[req.id] = it },
                                placeholder = { Text("Reply to admin feedback without new request...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("customer_reply_input_${req.id}")
                            )
                            Button(
                                onClick = {
                                    if (currentReplyText.isNotBlank()) {
                                        viewModel.submitCustomerPreOrderReply(req.id, currentReplyText)
                                        replyInputs[req.id] = ""
                                        Toast.makeText(context, "✅ Feedback reply sent to Admin!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(50.dp).testTag("send_customer_reply_button_${req.id}")
                            ) {
                                Text("Reply", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
