package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.MainViewModel
import com.example.data.model.HostingPeriodUnit
import com.example.data.model.PartnerAgreementStatus
import com.example.data.model.PartnerOrganization
import com.example.data.model.PartnerSubmissionStatus
import com.example.data.model.ProductCategory
import com.example.data.model.ProductColor
import com.example.ui.components.DualAxisScrollContainer
import com.example.ui.components.SignaturePadBox
import com.example.ui.components.SignaturePreviewBox
import com.example.ui.components.SizeSelectionBox
import com.example.ui.components.TickableColourPicker
import com.example.ui.theme.AccentGold
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.MoniepointBlue
import com.example.ui.theme.OPayGreen
import com.example.ui.theme.SuccessGreen
import com.example.util.PartnerDocumentExporter
import com.example.util.ProductImageHelper
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PartnerPortalScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val partners by viewModel.partnerOrganizations.collectAsState()
    val submissions by viewModel.partnerSubmissions.collectAsState()
    val storeProfile by viewModel.storeProfile.collectAsState()
    val isWideDualAxisMode by viewModel.isWideDualAxisMode.collectAsState()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0 = MOU Form, 1 = Upload Products to Admin Control Room
    var selectedPartnerId by rememberSaveable { mutableStateOf("") }

    val activePartner = remember(partners, selectedPartnerId) {
        partners.find { it.id == selectedPartnerId } ?: partners.firstOrNull()
    }

    val nairaFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "NG")).apply {
            maximumFractionDigits = 0
        }
    }
    val todayDateStr = remember {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
    }

    // Shared sample/pre-fill fields from MOU Form -> Staging Product Upload Tab
    var uploadCategory by rememberSaveable { mutableStateOf(ProductCategory.CLOTHES.displayName) }
    var uploadProductName by rememberSaveable { mutableStateOf("") }
    var uploadBrand by rememberSaveable { mutableStateOf("") }
    var uploadQuantity by rememberSaveable { mutableStateOf("20") }
    var uploadSizesCsv by rememberSaveable { mutableStateOf("M, L, XL") }
    var uploadColors by remember { mutableStateOf(listOf(ProductColor("Black", "#000000"), ProductColor("Navy Blue", "#1E3A8A"))) }
    var uploadSellingPrice by rememberSaveable { mutableStateOf("") }
    var uploadPromoPercent by rememberSaveable { mutableStateOf("0") }
    var uploadCommissionPercent by rememberSaveable { mutableStateOf("10.0") }

    // PDF Save Launcher for MOU
    var pendingPdfPartner by remember { mutableStateOf<PartnerOrganization?>(null) }
    val saveMouPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val p = pendingPdfPartner
        if (uri != null && p != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    PartnerDocumentExporter.writePartnerMouPdf(out, p, storeProfile)
                }
                Toast.makeText(context, "Signed MOU PDF saved!", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(context, "Could not save MOU PDF.", Toast.LENGTH_SHORT).show()
            }
        }
        pendingPdfPartner = null
    }

    BackHandler {
        viewModel.navigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Handshake, contentDescription = null, tint = AccentGold, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Business Partner Portal", fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Text("Categorized Hosting & MOU", fontSize = 10.sp, color = CyberCyan)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("partner_portal_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.navigateToHome() },
                        modifier = Modifier.testTag("partner_portal_home_button")
                    ) {
                        Icon(Icons.Default.Home, contentDescription = "Home", tint = CyberCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        DualAxisScrollContainer(
            wideMode = isWideDualAxisMode,
            minWideWidth = 700.dp,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { _, _ ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (isWideDualAxisMode) Modifier else Modifier.widthIn(max = 560.dp))
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("1. MOU & Partnership Form", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("partner_tab_mou")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("2. Upload to Admin Room", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("partner_tab_upload")
                    )
                }

                if (selectedTab == 0) {
                    // =============================================================
                    // TAB 0: CATEGORIZED PARTNERSHIP & MOU APPLICATION FORM
                    // =============================================================
                    var businessName by rememberSaveable { mutableStateOf("") }
                    var ownerName by rememberSaveable { mutableStateOf("") }
                    var ownerPhone by rememberSaveable { mutableStateOf("") }
                    var businessAddress by rememberSaveable { mutableStateOf("") }
                    var primaryCategory by rememberSaveable { mutableStateOf(ProductCategory.CLOTHES.displayName) }

                    // Schedule / Initial Product Spec inside MOU
                    var sampleProductName by rememberSaveable { mutableStateOf("") }
                    var sampleBrand by rememberSaveable { mutableStateOf("") }
                    var sampleQuantity by rememberSaveable { mutableStateOf("20") }
                    var sampleSizesCsv by rememberSaveable { mutableStateOf("M, L, XL") }
                    var sampleSellingPrice by rememberSaveable { mutableStateOf("") }
                    var samplePromoPercent by rememberSaveable { mutableStateOf("0") }
                    var sampleColors by remember { mutableStateOf(listOf(ProductColor("Black", "#000000"), ProductColor("Accent Gold", "#F59E0B"))) }

                    // Agreement & Editable Commercial Terms
                    var agreementDate by rememberSaveable { mutableStateOf(todayDateStr) }
                    var hostingDuration by rememberSaveable { mutableStateOf("6 Months") }
                    var agreedQtyPerPeriod by rememberSaveable { mutableStateOf("20") }
                    var periodUnit by remember { mutableStateOf(HostingPeriodUnit.PER_MONTH) }
                    var monthlyHostingPayment by rememberSaveable { mutableStateOf("15000") }
                    var commissionPercent by rememberSaveable { mutableStateOf("10.0") }

                    // Dedicated Folder Settlement Accounts
                    var opayNumber by rememberSaveable { mutableStateOf("") }
                    var opayName by rememberSaveable { mutableStateOf("") }
                    var monieNumber by rememberSaveable { mutableStateOf("") }
                    var monieName by rememberSaveable { mutableStateOf("") }

                    // Dual Signatures
                    var partnerSignatoryName by rememberSaveable { mutableStateOf("") }
                    var partnerSignatureStrokes by rememberSaveable { mutableStateOf("") }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Hero Gradient Banner
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.5.dp, AccentGold.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF052E16), Color(0xFF065F46), Color(0xFF047857))
                                        )
                                    )
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "PARTNER WITH ${storeProfile.storeName.uppercase()}",
                                        color = AccentGold,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        letterSpacing = 0.6.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Host your categorized products inside a dedicated Partner Folder with automated OPay / Moniepoint alerts, live stock tracking, and signed MOU terms.",
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        // Existing Partner Applications Quick Status Selector
                        if (partners.isNotEmpty()) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Registered Partner Organisations (${partners.size})",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Black,
                                        color = CyberCyan
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    partners.forEach { p ->
                                        val isFinal = p.status == PartnerAgreementStatus.FINALIZED
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(
                                                1.dp,
                                                if (activePartner?.id == p.id) AccentGold else MaterialTheme.colorScheme.outlineVariant
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .clickable {
                                                    selectedPartnerId = p.id
                                                    uploadCategory = p.primaryCategory
                                                    uploadCommissionPercent = p.stipulatedCommissionPercent.toString()
                                                }
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(p.businessName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                        Text(
                                                            "${p.id} • ${p.primaryCategory} • ${p.stipulatedCommissionPercent}% fee",
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = if (isFinal) SuccessGreen.copy(alpha = 0.16f) else AccentGold.copy(alpha = 0.18f)
                                                    ) {
                                                        Text(
                                                            text = if (isFinal) "MOU FINALIZED" else "PENDING ADMIN",
                                                            color = if (isFinal) SuccessGreen else AccentGold,
                                                            fontSize = 9.5.sp,
                                                            fontWeight = FontWeight.Black,
                                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    OutlinedButton(
                                                        onClick = { PartnerDocumentExporter.sharePartnerMouPdf(context, p, storeProfile) },
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                        modifier = Modifier.height(34.dp)
                                                    ) {
                                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Share MOU PDF", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    OutlinedButton(
                                                        onClick = {
                                                            pendingPdfPartner = p
                                                            saveMouPdfLauncher.launch("${p.id}_Signed_MOU.pdf")
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                        modifier = Modifier.height(34.dp)
                                                    ) {
                                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Save PDF", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    if (isFinal) {
                                                        Button(
                                                            onClick = {
                                                                selectedPartnerId = p.id
                                                                uploadCategory = p.primaryCategory
                                                                uploadCommissionPercent = p.stipulatedCommissionPercent.toString()
                                                                selectedTab = 1
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                            modifier = Modifier.height(34.dp)
                                                        ) {
                                                            Text("Upload Stock", fontSize = 10.5.sp, fontWeight = FontWeight.Black)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 1. Business Organisation Identity
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Business, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("1. Business Organisation & Product Category", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                                }

                                OutlinedTextField(
                                    value = businessName,
                                    onValueChange = { businessName = it },
                                    label = { Text("Partner Business / Organisation Name *") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("partner_business_name_input")
                                )
                                OutlinedTextField(
                                    value = ownerName,
                                    onValueChange = {
                                        ownerName = it
                                        if (partnerSignatoryName.isBlank()) partnerSignatoryName = it
                                    },
                                    label = { Text("Business Owner / Authorised Director *") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("partner_owner_name_input")
                                )
                                OutlinedTextField(
                                    value = ownerPhone,
                                    onValueChange = { ownerPhone = it },
                                    label = { Text("Owner WhatsApp / Phone Number (For Payment Alerts & Reports) *") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("partner_owner_phone_input")
                                )
                                OutlinedTextField(
                                    value = businessAddress,
                                    onValueChange = { businessAddress = it },
                                    label = { Text("Business Warehouse / Office Address") },
                                    minLines = 2,
                                    modifier = Modifier.fillMaxWidth().testTag("partner_address_input")
                                )

                                Text("Select Sold Product Category *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = CyberCyan)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    ProductCategory.entries.forEach { cat ->
                                        val isSel = primaryCategory == cat.displayName
                                        FilterChip(
                                            selected = isSel,
                                            onClick = { primaryCategory = cat.displayName },
                                            label = { Text(cat.displayName, fontSize = 11.5.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = CyberCyan,
                                                selectedLabelColor = Color.Black
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Categorized Product Details Schedule
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Category, contentDescription = null, tint = AccentGold, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("2. Categorized Product Schedule (Sample / Opening Line)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                                }
                                Text(
                                    text = "Declare product details covered by this partnership MOU (automatically carried over once Admin finalizes your agreement).",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedTextField(
                                    value = sampleProductName,
                                    onValueChange = { sampleProductName = it },
                                    label = { Text("Product Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = sampleBrand,
                                        onValueChange = { sampleBrand = it },
                                        label = { Text("Brand") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = sampleQuantity,
                                        onValueChange = { sampleQuantity = it.filter { c -> c.isDigit() } },
                                        label = { Text("Quantity") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(0.7f)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = sampleSellingPrice,
                                        onValueChange = { sampleSellingPrice = it.filter { c -> c.isDigit() || c == '.' } },
                                        label = { Text("Selling Price (₦)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = samplePromoPercent,
                                        onValueChange = { samplePromoPercent = it.filter { c -> c.isDigit() } },
                                        label = { Text("Promo % (Optional)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(0.8f)
                                    )
                                }

                                SizeSelectionBox(
                                    category = primaryCategory,
                                    availableSizesCsv = sampleSizesCsv,
                                    onSizesChanged = { sampleSizesCsv = it }
                                )

                                TickableColourPicker(
                                    selectedColors = sampleColors,
                                    onColorsChanged = { sampleColors = it }
                                )
                            }
                        }

                        // 3. Editable Agreement Duration, Quota, Hosting Payment & Stipulated % Offer
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("3. Hosting Duration, Editable Quota & % Fee", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = agreementDate,
                                        onValueChange = { agreementDate = it },
                                        label = { Text("Date of Partnership Agreement *") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = hostingDuration,
                                        onValueChange = { hostingDuration = it },
                                        label = { Text("Duration of Hosting Time *") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("3 Months", "6 Months", "12 Months", "24 Months").forEach { dur ->
                                        AssistChip(
                                            onClick = { hostingDuration = dur },
                                            label = { Text(dur, fontSize = 11.sp) }
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = agreedQtyPerPeriod,
                                        onValueChange = { agreedQtyPerPeriod = it.filter { c -> c.isDigit() } },
                                        label = { Text("Agreed Product Qty (Editable)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = monthlyHostingPayment,
                                        onValueChange = { monthlyHostingPayment = it.filter { c -> c.isDigit() || c == '.' } },
                                        label = { Text("Monthly Hosting Fee ₦ (Editable)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Text("Supply Quota Period:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    HostingPeriodUnit.entries.forEach { unit ->
                                        FilterChip(
                                            selected = periodUnit == unit,
                                            onClick = { periodUnit = unit },
                                            label = { Text(unit.label, fontSize = 11.sp) }
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = commissionPercent,
                                    onValueChange = { commissionPercent = it.filter { c -> c.isDigit() || c == '.' } },
                                    label = { Text("Stipulated % Offer Paid to 24/7 SHOPPING per Product (Editable)") },
                                    supportingText = {
                                        Text("Example: 10% paid to 24/7 SHOPPING per unit sold; editable per product during upload or Admin verification.")
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("partner_commission_percent_input")
                                )
                            }
                        }

                        // 4. Dedicated Partner Folder Payment Accounts (OPay & Moniepoint)
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = OPayGreen, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("4. Partner Folder Settlement Accounts", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                                }
                                Text(
                                    text = "Each Partner Folder holds separate OPay & Moniepoint accounts with automated payment alerts to both 24/7 SHOPPING & your business.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = opayNumber,
                                        onValueChange = { opayNumber = it.filter { c -> c.isDigit() } },
                                        label = { Text("OPay Account No.") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = opayName,
                                        onValueChange = { opayName = it },
                                        label = { Text("OPay Account Name") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = monieNumber,
                                        onValueChange = { monieNumber = it.filter { c -> c.isDigit() } },
                                        label = { Text("Moniepoint Account No.") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = monieName,
                                        onValueChange = { monieName = it },
                                        label = { Text("Moniepoint Account Name") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // 5. Memorandum of Understanding (MOU) & Dual Signatures
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.5.dp, AccentGold.copy(alpha = 0.55f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "5. MEMORANDUM OF UNDERSTANDING (MOU)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black,
                                    color = AccentGold
                                )
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val feeAmt = monthlyHostingPayment.toDoubleOrNull() ?: 0.0
                                    val commVal = commissionPercent.toDoubleOrNull() ?: 10.0
                                    Text(
                                        text = """
                                            THIS MEMORANDUM OF UNDERSTANDING is entered on $agreementDate between ${storeProfile.storeName} ("Host Platform") and ${businessName.ifBlank { "[Partner Business]" }} ("Partner").
                                            
                                            1. Category & Supply: Partner supplies authentic '$primaryCategory' items at ${agreedQtyPerPeriod.ifBlank { "20" }} units ${periodUnit.label} for a hosting duration of $hostingDuration.
                                            2. Commission & Hosting Fee: ${storeProfile.storeName} earns $commVal% stipulated commission per unit sold (editable per product), plus Monthly Hosting Payment of ${nairaFormat.format(feeAmt)}.
                                            3. Staging & Verification: Once finalized by both signatories, Partner uploads products & images strictly to 24/7 SHOPPING Admin Control Room (not Supabase Cloud). Admin verifies & batch-uploads approved products to Supabase Cloud under Partner's dedicated Folder.
                                            4. Automated Alerts & Reporting: Customer payments trigger automated alerts to both ${storeProfile.storeName} & Partner, with Daily / Weekly / Monthly sales & stock balance reports.
                                        """.trimIndent(),
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }

                                OutlinedTextField(
                                    value = partnerSignatoryName,
                                    onValueChange = { partnerSignatoryName = it },
                                    label = { Text("Partner Business Owner Signatory Name *") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                SignaturePadBox(
                                    strokesData = partnerSignatureStrokes,
                                    onStrokesChanged = { partnerSignatureStrokes = it },
                                    title = "Partnering Business Owner Signature *",
                                    testTagPrefix = "partner_mou_signature_pad"
                                )

                                SignaturePreviewBox(
                                    strokesData = activePartner?.adminCounterSignatureStrokes ?: "",
                                    signerName = activePartner?.adminCounterSignatureName?.ifBlank { "24/7 SHOPPING Management (Signs in Admin Control Room)" }
                                        ?: "24/7 SHOPPING Management (Counter-signs upon review)",
                                    roleLabel = "24/7 SHOPPING COUNTER-SIGNATURE STATUS"
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (businessName.isBlank() || ownerName.isBlank() || ownerPhone.isBlank()) {
                                    Toast.makeText(context, "Enter Business Name, Owner Name, and Phone Number.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (partnerSignatureStrokes.isBlank()) {
                                    Toast.makeText(context, "Please draw your Partner Business Owner signature on the MOU pad.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val comm = commissionPercent.toDoubleOrNull() ?: 10.0
                                val qty = agreedQtyPerPeriod.toIntOrNull() ?: 20
                                val hostingFee = monthlyHostingPayment.toDoubleOrNull() ?: 0.0

                                viewModel.submitPartnerAgreementApplication(
                                    businessName = businessName,
                                    ownerName = ownerName,
                                    ownerPhone = ownerPhone,
                                    businessAddress = businessAddress,
                                    primaryCategory = primaryCategory,
                                    opayAccountNumber = opayNumber,
                                    opayAccountName = opayName,
                                    moniepointAccountNumber = monieNumber,
                                    moniepointAccountName = monieName,
                                    agreementDate = agreementDate,
                                    hostingDuration = hostingDuration,
                                    agreedQuantityPerPeriod = qty,
                                    periodUnit = periodUnit,
                                    monthlyHostingPayment = hostingFee,
                                    stipulatedCommissionPercent = comm,
                                    partnerSignatureName = partnerSignatoryName.ifBlank { ownerName },
                                    partnerSignatureStrokes = partnerSignatureStrokes
                                ) { created ->
                                    selectedPartnerId = created.id
                                    uploadCategory = primaryCategory
                                    uploadProductName = sampleProductName
                                    uploadBrand = sampleBrand
                                    uploadQuantity = sampleQuantity
                                    uploadSizesCsv = sampleSizesCsv
                                    uploadColors = sampleColors
                                    uploadSellingPrice = sampleSellingPrice
                                    uploadPromoPercent = samplePromoPercent
                                    uploadCommissionPercent = commissionPercent
                                    Toast.makeText(
                                        context,
                                        "MOU Signed & Submitted (${created.id})! Awaiting 24/7 SHOPPING counter-signature in Admin Control Room.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    selectedTab = 1
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color.Black),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("submit_partner_mou_button")
                        ) {
                            Icon(Icons.Default.Handshake, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign MOU & Submit Partnership Application", fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                } else {
                    // =============================================================
                    // TAB 1: UPLOAD PRODUCTS & IMAGE TO ADMIN CONTROL ROOM STAGING
                    // (Gated strictly until Agreement is FINALIZED; NEVER pushes directly to Supabase Cloud)
                    // =============================================================
                    var uploadDescription by rememberSaveable { mutableStateOf("") }
                    var uploadBatchNumber by rememberSaveable { mutableStateOf("") }
                    var pickedImageUri by remember { mutableStateOf<Uri?>(null) }

                    val imagePickerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.PickVisualMedia()
                    ) { uri ->
                        if (uri != null) {
                            pickedImageUri = uri
                            Toast.makeText(context, "Product image selected for Admin Staging!", Toast.LENGTH_SHORT).show()
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Select Active Partner Profile
                        Text("Select Partner Organisation Folder", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                        if (partners.isEmpty()) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.FolderShared, contentDescription = null, tint = AccentGold, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No Partner Application Found", fontWeight = FontWeight.Bold)
                                    Text(
                                        "Complete & sign the Categorized Partnership MOU form first. Once 24/7 SHOPPING counter-signs & finalizes it, product upload unlocks here.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(onClick = { selectedTab = 0 }) {
                                        Text("Open Partnership & MOU Form", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                partners.forEach { p ->
                                    val isSelected = activePartner?.id == p.id
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedPartnerId = p.id
                                            uploadCategory = p.primaryCategory
                                            uploadCommissionPercent = p.stipulatedCommissionPercent.toString()
                                        },
                                        label = {
                                            Text(
                                                text = "${p.businessName} (${if (p.status == PartnerAgreementStatus.FINALIZED) "FINALIZED" else "PENDING"})",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.5.sp
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = if (p.status == PartnerAgreementStatus.FINALIZED) SuccessGreen else AccentGold,
                                            selectedLabelColor = Color.Black
                                        )
                                    )
                                }
                            }

                            val partner = activePartner!!
                            val isFinalized = partner.status == PartnerAgreementStatus.FINALIZED

                            // Agreement Status Gate Card
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isFinalized) SuccessGreen.copy(alpha = 0.12f) else AccentGold.copy(alpha = 0.14f)
                                ),
                                border = BorderStroke(1.5.dp, if (isFinalized) SuccessGreen else AccentGold),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isFinalized) Icons.Default.Verified else Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = if (isFinalized) SuccessGreen else AccentGold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isFinalized) "MOU FINALIZED — ADMIN CONTROL ROOM UPLOAD ACTIVE" else "LOCKED: AWAITING 24/7 SHOPPING COUNTER-SIGNATURE",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp,
                                            color = if (isFinalized) SuccessGreen else AccentGold
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CloudOff, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Important: Uploaded products & images go ONLY to Admin Control Room Staging (NOT Supabase Cloud) until Admin verifies and clicks Batch Upload.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { PartnerDocumentExporter.sharePartnerMouPdf(context, partner, storeProfile) },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Text("Share MOU PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Text(
                                            text = "Stipulated Fee: ${partner.stipulatedCommissionPercent}% • Quota: ${partner.agreedQuantityPerPeriod} ${partner.periodUnit.label}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.align(Alignment.CenterVertically)
                                        )
                                    }
                                }
                            }

                            if (isFinalized) {
                                // Product Upload Form -> Admin Staging Only
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(
                                            text = "Upload Categorized Product to Admin Control Room",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Black,
                                            color = CyberCyan
                                        )

                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            ProductCategory.entries.forEach { cat ->
                                                val sel = uploadCategory == cat.displayName
                                                FilterChip(
                                                    selected = sel,
                                                    onClick = { uploadCategory = cat.displayName },
                                                    label = { Text(cat.displayName, fontSize = 11.sp) }
                                                )
                                            }
                                        }

                                        OutlinedTextField(
                                            value = uploadProductName,
                                            onValueChange = { uploadProductName = it },
                                            label = { Text("Product Name *") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth().testTag("partner_upload_name_input")
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = uploadBrand,
                                                onValueChange = { uploadBrand = it },
                                                label = { Text("Brand *") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f).testTag("partner_upload_brand_input")
                                            )
                                            OutlinedTextField(
                                                value = uploadQuantity,
                                                onValueChange = { uploadQuantity = it.filter { c -> c.isDigit() } },
                                                label = { Text("Quantity *") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                modifier = Modifier.weight(0.75f).testTag("partner_upload_qty_input")
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = uploadSellingPrice,
                                                onValueChange = { uploadSellingPrice = it.filter { c -> c.isDigit() || c == '.' } },
                                                label = { Text("Selling Price (₦) *") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                singleLine = true,
                                                modifier = Modifier.weight(1f).testTag("partner_upload_price_input")
                                            )
                                            OutlinedTextField(
                                                value = uploadPromoPercent,
                                                onValueChange = { uploadPromoPercent = it.filter { c -> c.isDigit() } },
                                                label = { Text("Promo % (Opt)") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                modifier = Modifier.weight(0.75f)
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = uploadCommissionPercent,
                                                onValueChange = { uploadCommissionPercent = it.filter { c -> c.isDigit() || c == '.' } },
                                                label = { Text("% Offer to 24/7 SHOPPING (Editable)") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = uploadBatchNumber,
                                                onValueChange = { uploadBatchNumber = it },
                                                label = { Text("Batch Tag (Opt)") },
                                                placeholder = { Text("e.g. BATCH-01") },
                                                singleLine = true,
                                                modifier = Modifier.weight(0.9f)
                                            )
                                        }

                                        SizeSelectionBox(
                                            category = uploadCategory,
                                            availableSizesCsv = uploadSizesCsv,
                                            onSizesChanged = { uploadSizesCsv = it }
                                        )

                                        TickableColourPicker(
                                            selectedColors = uploadColors,
                                            onColorsChanged = { uploadColors = it }
                                        )

                                        OutlinedTextField(
                                            value = uploadDescription,
                                            onValueChange = { uploadDescription = it },
                                            label = { Text("Product Description & Highlights") },
                                            minLines = 2,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        // Local Image Picker -> Held in Admin Staging ONLY
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, if (pickedImageUri != null) SuccessGreen else MaterialTheme.colorScheme.outlineVariant),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                if (pickedImageUri != null) {
                                                    AsyncImage(
                                                        model = pickedImageUri,
                                                        contentDescription = "Staged Partner Product Image",
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(170.dp)
                                                            .clip(RoundedCornerShape(10.dp))
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                }
                                                OutlinedButton(
                                                    onClick = {
                                                        imagePickerLauncher.launch(
                                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                        )
                                                    },
                                                    modifier = Modifier.fillMaxWidth().testTag("partner_pick_image_button")
                                                ) {
                                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(if (pickedImageUri != null) "Change Product Image" else "Pick Product Image (Admin Staging Only)")
                                                }
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                val price = uploadSellingPrice.toDoubleOrNull() ?: 0.0
                                                val qty = uploadQuantity.toIntOrNull() ?: 0
                                                val promo = uploadPromoPercent.toIntOrNull() ?: 0
                                                val comm = uploadCommissionPercent.toDoubleOrNull() ?: partner.stipulatedCommissionPercent
                                                viewModel.submitPartnerProductToAdminStaging(
                                                    partnerId = partner.id,
                                                    category = uploadCategory,
                                                    productName = uploadProductName,
                                                    brand = uploadBrand,
                                                    quantity = qty,
                                                    sizesCsv = uploadSizesCsv,
                                                    colors = uploadColors,
                                                    sellingPrice = price,
                                                    promoDiscountPercent = promo,
                                                    commissionPercent = comm,
                                                    pickedImageUriString = pickedImageUri?.toString() ?: "",
                                                    description = uploadDescription,
                                                    batchNumber = uploadBatchNumber
                                                ) { ok, msg ->
                                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                    if (ok) {
                                                        uploadProductName = ""
                                                        uploadSellingPrice = ""
                                                        uploadDescription = ""
                                                        pickedImageUri = null
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(52.dp)
                                                .testTag("partner_stage_product_submit_button")
                                        ) {
                                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Send to Admin Control Room Staging", fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                            }

                            // Partner's Submitted Batches & Status List
                            val mySubs = submissions.filter { it.partnerId == partner.id }
                            Text(
                                text = "Uploaded Batches in ${partner.businessName} (${mySubs.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black
                            )
                            if (mySubs.isEmpty()) {
                                Text(
                                    text = "No products staged yet for this partner.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                mySubs.forEach { sub ->
                                    val statusColor = when (sub.status) {
                                        PartnerSubmissionStatus.PENDING_ADMIN_VERIFICATION -> AccentGold
                                        PartnerSubmissionStatus.VERIFIED_READY_FOR_CLOUD -> CyberCyan
                                        PartnerSubmissionStatus.UPLOADED_TO_SUPABASE -> SuccessGreen
                                        PartnerSubmissionStatus.REJECTED -> Color(0xFFEF4444)
                                    }
                                    Card(
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.55f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AsyncImage(
                                                model = ProductImageHelper.buildImageRequest(context, sub.imageUrl, sub.category, sub.productName),
                                                contentDescription = sub.productName,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color.Black.copy(alpha = 0.1f))
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(sub.productName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(
                                                    "${sub.category} • ${sub.brand} • Qty: ${sub.quantity} • ${nairaFormat.format(sub.sellingPrice)}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    "24/7 Fee: ${sub.commissionPercent}% • Batch: ${sub.batchNumber}",
                                                    fontSize = 10.5.sp,
                                                    color = CyberCyan,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = statusColor.copy(alpha = 0.16f)
                                                ) {
                                                    Text(
                                                        text = sub.status.label,
                                                        color = statusColor,
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Black,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}
