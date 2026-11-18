package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.MainViewModel
import com.example.data.model.HostingPeriodUnit
import com.example.data.model.PartnerAgreementStatus
import com.example.data.model.PartnerFolderTransaction
import com.example.data.model.PartnerOrganization
import com.example.data.model.PartnerProductSubmission
import com.example.data.model.PartnerSubmissionStatus
import com.example.data.model.PartnerTransactionType
import com.example.data.model.ProductCategory
import com.example.data.model.SalesReportPeriod
import com.example.ui.components.SignaturePadBox
import com.example.ui.components.SignaturePreviewBox
import com.example.ui.components.ZoomableImageDialog
import com.example.ui.theme.AccentGold
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.MoniepointBlue
import com.example.ui.theme.OPayGreen
import com.example.ui.theme.SuccessGreen
import com.example.util.PartnerDocumentExporter
import com.example.util.ProductImageHelper
import com.example.util.WhatsAppHelper
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminPartnerHubTab(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val partners by viewModel.partnerOrganizations.collectAsState()
    val submissions by viewModel.partnerSubmissions.collectAsState()
    val transactions by viewModel.partnerTransactions.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val storeProfile by viewModel.storeProfile.collectAsState()
    val isSupabaseBusy by viewModel.isSupabaseBusy.collectAsState()

    var section by rememberSaveable { mutableIntStateOf(0) } // 0=MOU Agreements, 1=Staging & Batch Cloud, 2=Folders & Analytics
    var selectedFolderId by rememberSaveable { mutableStateOf<String?>(null) }

    val nairaFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "NG")).apply {
            maximumFractionDigits = 0
        }
    }
    val dateTimeFmt = remember { SimpleDateFormat("dd MMM yyyy • hh:mm:ss a", Locale.getDefault()) }

    val pendingMouCount = remember(partners) { partners.count { it.status == PartnerAgreementStatus.PENDING_FINALIZATION } }
    val pendingStageCount = remember(submissions) { submissions.count { it.status == PartnerSubmissionStatus.PENDING_ADMIN_VERIFICATION } }
    val verifiedStageCount = remember(submissions) { submissions.count { it.status == PartnerSubmissionStatus.VERIFIED_READY_FOR_CLOUD } }

    // PDF Save Launchers
    var pendingMouForSave by remember { mutableStateOf<PartnerOrganization?>(null) }
    val saveMouPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val p = pendingMouForSave
        if (uri != null && p != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    PartnerDocumentExporter.writePartnerMouPdf(out, p, storeProfile)
                }
                Toast.makeText(context, "MOU PDF saved to device!", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(context, "Could not save MOU PDF.", Toast.LENGTH_SHORT).show()
            }
        }
        pendingMouForSave = null
    }

    var pendingReportForSave by remember { mutableStateOf<Pair<PartnerOrganization, SalesReportPeriod>?>(null) }
    val saveReportPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val target = pendingReportForSave
        if (uri != null && target != null) {
            val (partner, period) = target
            val pProds = allProducts.filter { it.partnerId == partner.id }
            val pTxns = transactions.filter { it.partnerId == partner.id }
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    PartnerDocumentExporter.writeSalesReportPdf(out, partner, period, pProds, pTxns, storeProfile)
                }
                Toast.makeText(context, "${period.shortLabel} Sales Report PDF saved!", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(context, "Could not save report PDF.", Toast.LENGTH_SHORT).show()
            }
        }
        pendingReportForSave = null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Sub-navigation Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = section == 0,
                onClick = { section = 0 },
                leadingIcon = { Icon(Icons.Default.Handshake, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = {
                    Text(
                        text = if (pendingMouCount > 0) "MOU Agreements ($pendingMouCount Pending)" else "MOU Agreements (${partners.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp
                    )
                },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AccentGold, selectedLabelColor = Color.Black),
                modifier = Modifier.testTag("admin_partner_section_mou")
            )
            FilterChip(
                selected = section == 1,
                onClick = { section = 1 },
                leadingIcon = { Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = {
                    Text(
                        text = "Staging (${pendingStageCount + verifiedStageCount})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp
                    )
                },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CyberCyan, selectedLabelColor = Color.Black),
                modifier = Modifier.testTag("admin_partner_section_staging")
            )
            FilterChip(
                selected = section == 2,
                onClick = { section = 2 },
                leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text("Folders & Analytics (${partners.size})", fontWeight = FontWeight.Bold, fontSize = 11.5.sp) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SuccessGreen, selectedLabelColor = Color.Black),
                modifier = Modifier.testTag("admin_partner_section_folders")
            )
        }

        when (section) {
            0 -> AdminPartnerAgreementsSection(
                partners = partners,
                nairaFormat = nairaFormat,
                onCounterSignAndFinalize = { partnerId, adminSigName, adminStrokes, comm, qty, unit, fee, dur, opayN, opayNm, monieN, monieNm ->
                    viewModel.finalizePartnerAgreement(
                        partnerId = partnerId,
                        adminSignatoryName = adminSigName,
                        adminStrokes = adminStrokes,
                        commissionPercent = comm,
                        agreedQtyPerPeriod = qty,
                        periodUnit = unit,
                        monthlyHostingPayment = fee,
                        hostingDuration = dur,
                        opayNumber = opayN,
                        opayName = opayNm,
                        moniepointNumber = monieN,
                        moniepointName = monieNm
                    ) { _, msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                },
                onUpdateTerms = { partnerId, comm, qty, unit, fee, dur, opayN, opayNm, monieN, monieNm ->
                    viewModel.updatePartnerEditableTerms(
                        partnerId, comm, qty, unit, fee, dur, opayN, opayNm, monieN, monieNm
                    )
                    Toast.makeText(context, "Partner MOU editable terms & payment accounts updated!", Toast.LENGTH_SHORT).show()
                },
                onShareMouPdf = { p -> PartnerDocumentExporter.sharePartnerMouPdf(context, p, storeProfile) },
                onSaveMouPdf = { p ->
                    pendingMouForSave = p
                    saveMouPdfLauncher.launch("${p.id}_Signed_MOU.pdf")
                }
            )

            1 -> AdminPartnerStagingBatchSection(
                partners = partners,
                submissions = submissions,
                isBusy = isSupabaseBusy,
                nairaFormat = nairaFormat,
                onVerifyAll = { partnerId ->
                    viewModel.verifyAllPendingForPartner(partnerId)
                    Toast.makeText(context, "Pending staged items marked Verified for batch cloud upload!", Toast.LENGTH_SHORT).show()
                },
                onVerifyOne = { subId, approve, note ->
                    viewModel.verifyStagedPartnerProduct(subId, approve, note)
                    Toast.makeText(context, if (approve) "Product Verified!" else "Product Declined.", Toast.LENGTH_SHORT).show()
                },
                onUpdateSubmission = { subId, cat, name, brand, qty, sizes, price, promo, comm, batch, note ->
                    viewModel.updateStagedSubmissionDetails(subId, cat, name, brand, qty, sizes, price, promo, comm, batch, note)
                    Toast.makeText(context, "Staged product updated!", Toast.LENGTH_SHORT).show()
                },
                onBatchUploadToCloud = { partnerId ->
                    viewModel.batchUploadPartnerProductsToSupabase(partnerId) { _, msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                }
            )

            2 -> {
                val activeFolder = partners.find { it.id == selectedFolderId }
                if (activeFolder == null) {
                    AdminPartnerFolderListSection(
                        partners = partners,
                        submissions = submissions,
                        transactions = transactions,
                        allProducts = allProducts,
                        nairaFormat = nairaFormat,
                        onOpenFolder = { selectedFolderId = it.id }
                    )
                } else {
                    AdminSinglePartnerFolderAnalytics(
                        folder = activeFolder,
                        folderProducts = allProducts.filter { it.partnerId == activeFolder.id },
                        folderSubmissions = submissions.filter { it.partnerId == activeFolder.id },
                        folderTransactions = transactions.filter { it.partnerId == activeFolder.id },
                        storeProfile = storeProfile,
                        nairaFormat = nairaFormat,
                        dateTimeFmt = dateTimeFmt,
                        onBackToFolders = { selectedFolderId = null },
                        onUpdateAccounts = { opayN, opayNm, monieN, monieNm ->
                            viewModel.updatePartnerEditableTerms(
                                partnerId = activeFolder.id,
                                commissionPercent = activeFolder.stipulatedCommissionPercent,
                                agreedQtyPerPeriod = activeFolder.agreedQuantityPerPeriod,
                                periodUnit = activeFolder.periodUnit,
                                monthlyHostingPayment = activeFolder.monthlyHostingPayment,
                                hostingDuration = activeFolder.hostingDuration,
                                opayNumber = opayN,
                                opayName = opayNm,
                                moniepointNumber = monieN,
                                moniepointName = monieNm
                            )
                            Toast.makeText(context, "Folder OPay/Moniepoint accounts saved!", Toast.LENGTH_SHORT).show()
                        },
                        onSaveReportPdf = { period ->
                            pendingReportForSave = activeFolder to period
                            saveReportPdfLauncher.launch("${activeFolder.id}_${period.shortLabel}_Sales_Stock_Report.pdf")
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun AdminPartnerAgreementsSection(
    partners: List<PartnerOrganization>,
    nairaFormat: NumberFormat,
    onCounterSignAndFinalize: (String, String, String, Double, Int, HostingPeriodUnit, Double, String, String, String, String, String) -> Unit,
    onUpdateTerms: (String, Double, Int, HostingPeriodUnit, Double, String, String, String, String, String) -> Unit,
    onShareMouPdf: (PartnerOrganization) -> Unit,
    onSaveMouPdf: (PartnerOrganization) -> Unit
) {
    val context = LocalContext.current
    if (partners.isEmpty()) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Handshake, contentDescription = null, tint = AccentGold, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("No Business Partnership MOUs Yet", fontWeight = FontWeight.Bold)
                Text(
                    "Businesses can submit & sign categorized partnership applications via the Business Partner Portal.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    var expandedPartnerId by rememberSaveable { mutableStateOf(partners.firstOrNull()?.id ?: "") }

    partners.forEach { partner ->
        val isPending = partner.status == PartnerAgreementStatus.PENDING_FINALIZATION
        val isExpanded = expandedPartnerId == partner.id

        var editCommission by remember(partner.id, partner.stipulatedCommissionPercent) { mutableStateOf(partner.stipulatedCommissionPercent.toString()) }
        var editQty by remember(partner.id, partner.agreedQuantityPerPeriod) { mutableStateOf(partner.agreedQuantityPerPeriod.toString()) }
        var editPeriodUnit by remember(partner.id, partner.periodUnit) { mutableStateOf(partner.periodUnit) }
        var editMonthlyPay by remember(partner.id, partner.monthlyHostingPayment) { mutableStateOf(partner.monthlyHostingPayment.toInt().toString()) }
        var editDuration by remember(partner.id, partner.hostingDuration) { mutableStateOf(partner.hostingDuration) }
        var editOpayNum by remember(partner.id, partner.opayAccountNumber) { mutableStateOf(partner.opayAccountNumber) }
        var editOpayName by remember(partner.id, partner.opayAccountName) { mutableStateOf(partner.opayAccountName) }
        var editMonieNum by remember(partner.id, partner.moniepointAccountNumber) { mutableStateOf(partner.moniepointAccountNumber) }
        var editMonieName by remember(partner.id, partner.moniepointAccountName) { mutableStateOf(partner.moniepointAccountName) }
        var adminSignatoryName by remember(partner.id) { mutableStateOf(partner.adminCounterSignatureName.ifBlank { "24/7 SHOPPING Management" }) }
        var adminSigStrokes by remember(partner.id) { mutableStateOf(partner.adminCounterSignatureStrokes) }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, if (isPending) AccentGold else SuccessGreen.copy(alpha = 0.65f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedPartnerId = if (isExpanded) "" else partner.id },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(partner.businessName, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        Text(
                            "${partner.id} • Owner: ${partner.ownerName} (${partner.ownerPhone})",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Cat: ${partner.primaryCategory} • Fee: ${partner.stipulatedCommissionPercent}% • Monthly: ${nairaFormat.format(partner.monthlyHostingPayment)}",
                            fontSize = 11.sp,
                            color = CyberCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isPending) AccentGold.copy(alpha = 0.2f) else SuccessGreen.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = if (isPending) "SIGN & FINALIZE" else "FINALIZED",
                            color = if (isPending) AccentGold else SuccessGreen,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (isExpanded) {
                    HorizontalDivider()
                    Text("Date: ${partner.agreementDate} • Address: ${partner.businessAddress.ifBlank { "On file" }}", fontSize = 11.5.sp)

                    // Editable Commercial Terms
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editCommission,
                            onValueChange = { editCommission = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("24/7 % Per Product (Edit)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editQty,
                            onValueChange = { editQty = it.filter { c -> c.isDigit() } },
                            label = { Text("Agreed Qty/Period") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(0.9f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editMonthlyPay,
                            onValueChange = { editMonthlyPay = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Monthly Hosting ₦") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editDuration,
                            onValueChange = { editDuration = it },
                            label = { Text("Hosting Duration") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        HostingPeriodUnit.entries.forEach { u ->
                            FilterChip(
                                selected = editPeriodUnit == u,
                                onClick = { editPeriodUnit = u },
                                label = { Text(u.label, fontSize = 10.5.sp) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editOpayNum,
                            onValueChange = { editOpayNum = it.filter { c -> c.isDigit() } },
                            label = { Text("Folder OPay No.") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editOpayName,
                            onValueChange = { editOpayName = it },
                            label = { Text("OPay Name") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editMonieNum,
                            onValueChange = { editMonieNum = it.filter { c -> c.isDigit() } },
                            label = { Text("Folder Moniepoint No.") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editMonieName,
                            onValueChange = { editMonieName = it },
                            label = { Text("Moniepoint Name") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    SignaturePreviewBox(
                        strokesData = partner.partnerSignatureStrokes,
                        signerName = partner.partnerSignatureName.ifBlank { partner.ownerName },
                        roleLabel = "PARTNER BUSINESS OWNER SIGNATURE"
                    )

                    if (isPending) {
                        OutlinedTextField(
                            value = adminSignatoryName,
                            onValueChange = { adminSignatoryName = it },
                            label = { Text("24/7 SHOPPING Admin Signatory Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        SignaturePadBox(
                            strokesData = adminSigStrokes,
                            onStrokesChanged = { adminSigStrokes = it },
                            title = "24/7 SHOPPING Counter-Signature *",
                            testTagPrefix = "admin_mou_counter_sig_${partner.id}"
                        )
                        Button(
                            onClick = {
                                if (adminSigStrokes.isBlank()) {
                                    Toast.makeText(context, "Please draw 24/7 SHOPPING Admin counter-signature.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                onCounterSignAndFinalize(
                                    partner.id,
                                    adminSignatoryName,
                                    adminSigStrokes,
                                    editCommission.toDoubleOrNull() ?: partner.stipulatedCommissionPercent,
                                    editQty.toIntOrNull() ?: partner.agreedQuantityPerPeriod,
                                    editPeriodUnit,
                                    editMonthlyPay.toDoubleOrNull() ?: partner.monthlyHostingPayment,
                                    editDuration,
                                    editOpayNum,
                                    editOpayName,
                                    editMonieNum,
                                    editMonieName
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.Black),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Counter-Sign & Finalize MOU", fontWeight = FontWeight.Black)
                        }
                    } else {
                        SignaturePreviewBox(
                            strokesData = partner.adminCounterSignatureStrokes,
                            signerName = partner.adminCounterSignatureName,
                            roleLabel = "24/7 SHOPPING ADMIN COUNTER-SIGNATURE (FINALIZED)"
                        )
                        OutlinedButton(
                            onClick = {
                                onUpdateTerms(
                                    partner.id,
                                    editCommission.toDoubleOrNull() ?: partner.stipulatedCommissionPercent,
                                    editQty.toIntOrNull() ?: partner.agreedQuantityPerPeriod,
                                    editPeriodUnit,
                                    editMonthlyPay.toDoubleOrNull() ?: partner.monthlyHostingPayment,
                                    editDuration,
                                    editOpayNum,
                                    editOpayName,
                                    editMonieNum,
                                    editMonieName
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Updated Terms & Folder Accounts", fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onShareMouPdf(partner) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share MOU PDF", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { onSaveMouPdf(partner) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save MOU PDF", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminPartnerStagingBatchSection(
    partners: List<PartnerOrganization>,
    submissions: List<PartnerProductSubmission>,
    isBusy: Boolean,
    nairaFormat: NumberFormat,
    onVerifyAll: (String?) -> Unit,
    onVerifyOne: (String, Boolean, String) -> Unit,
    onUpdateSubmission: (String, String, String, String, Int, String, Double, Int, Double, String, String) -> Unit,
    onBatchUploadToCloud: (String?) -> Unit
) {
    val context = LocalContext.current
    var partnerFilterId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingSub by remember { mutableStateOf<PartnerProductSubmission?>(null) }
    var zoomingImageUrl by remember { mutableStateOf<String?>(null) }

    val filteredSubs = remember(submissions, partnerFilterId) {
        if (partnerFilterId == null) submissions else submissions.filter { it.partnerId == partnerFilterId }
    }
    val pendingCount = filteredSubs.count { it.status == PartnerSubmissionStatus.PENDING_ADMIN_VERIFICATION }
    val verifiedCount = filteredSubs.count { it.status == PartnerSubmissionStatus.VERIFIED_READY_FOR_CLOUD }

    // Partner Filter Chips
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FilterChip(
            selected = partnerFilterId == null,
            onClick = { partnerFilterId = null },
            label = { Text("All Partners (${submissions.size})", fontSize = 11.sp) }
        )
        partners.forEach { p ->
            val cnt = submissions.count { it.partnerId == p.id }
            FilterChip(
                selected = partnerFilterId == p.id,
                onClick = { partnerFilterId = p.id },
                label = { Text("${p.businessName} ($cnt)", fontSize = 11.sp) }
            )
        }
    }

    // Batch Control Actions Card
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberCyan.copy(alpha = 0.1f)),
        border = BorderStroke(1.5.dp, CyberCyan.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "ADMIN STAGING QUEUE → ONE-CLICK SUPABASE BATCH SYNC",
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                color = CyberCyan
            )
            Text(
                text = "Pending Admin Review: $pendingCount  |  Verified Ready for Cloud: $verifiedCount",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    enabled = pendingCount > 0 && !isBusy,
                    onClick = { onVerifyAll(partnerFilterId) },
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Verify All ($pendingCount)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    enabled = verifiedCount > 0 && !isBusy,
                    onClick = { onBatchUploadToCloud(partnerFilterId) },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.Black),
                    modifier = Modifier.weight(1.25f).height(48.dp).testTag("admin_batch_upload_supabase_button")
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(17.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Batch Upload ($verifiedCount)", fontSize = 11.5.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }

    if (filteredSubs.isEmpty()) {
        Text("No staged partner products in this view yet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        filteredSubs.forEach { sub ->
            val badgeColor = when (sub.status) {
                PartnerSubmissionStatus.PENDING_ADMIN_VERIFICATION -> AccentGold
                PartnerSubmissionStatus.VERIFIED_READY_FOR_CLOUD -> CyberCyan
                PartnerSubmissionStatus.UPLOADED_TO_SUPABASE -> SuccessGreen
                PartnerSubmissionStatus.REJECTED -> Color(0xFFEF4444)
            }
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = ProductImageHelper.buildImageRequest(context, sub.imageUrl, sub.category, sub.productName),
                            contentDescription = sub.productName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(66.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.1f))
                                .clickable { if (sub.imageUrl.isNotBlank()) zoomingImageUrl = sub.imageUrl }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(sub.productName, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${sub.partnerName} • ${sub.category} • ${sub.brand}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Qty: ${sub.quantity} • Sizes: ${sub.sizesCsv} • ${nairaFormat.format(sub.sellingPrice)}" +
                                    if (sub.promoDiscountPercent > 0) " (${sub.promoDiscountPercent}% Promo)" else "",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "24/7 Fee: ${sub.commissionPercent}% • Batch: ${sub.batchNumber}",
                                fontSize = 11.sp,
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = sub.status.label,
                            color = badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { editingSub = sub },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit % / Details", fontSize = 11.sp)
                        }
                        if (sub.status == PartnerSubmissionStatus.PENDING_ADMIN_VERIFICATION) {
                            Button(
                                onClick = { onVerifyOne(sub.id, true, "") },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Verify", fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                            OutlinedButton(
                                onClick = { onVerifyOne(sub.id, false, "Declined in staging") },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Text("Decline", fontSize = 11.sp, color = Color(0xFFEF4444))
                            }
                        }
                    }
                }
            }
        }
    }

    editingSub?.let { target ->
        EditStagedProductDialog(
            submission = target,
            onDismiss = { editingSub = null },
            onSave = { cat, name, brand, qty, sizes, price, promo, comm, batch, note ->
                onUpdateSubmission(target.id, cat, name, brand, qty, sizes, price, promo, comm, batch, note)
                editingSub = null
            }
        )
    }

    zoomingImageUrl?.let { url ->
        ZoomableImageDialog(imageUrl = url, title = "Staged Partner Product", onDismiss = { zoomingImageUrl = null })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditStagedProductDialog(
    submission: PartnerProductSubmission,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Int, String, Double, Int, Double, String, String) -> Unit
) {
    var cat by remember { mutableStateOf(submission.category) }
    var name by remember { mutableStateOf(submission.productName) }
    var brand by remember { mutableStateOf(submission.brand) }
    var qty by remember { mutableStateOf(submission.quantity.toString()) }
    var sizes by remember { mutableStateOf(submission.sizesCsv) }
    var price by remember { mutableStateOf(submission.sellingPrice.toInt().toString()) }
    var promo by remember { mutableStateOf(submission.promoDiscountPercent.toString()) }
    var comm by remember { mutableStateOf(submission.commissionPercent.toString()) }
    var batch by remember { mutableStateOf(submission.batchNumber) }
    var note by remember { mutableStateOf(submission.adminNote) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Staged Item & % Fee", fontWeight = FontWeight.Black, fontSize = 16.sp) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ProductCategory.entries.forEach { c ->
                        FilterChip(selected = cat == c.displayName, onClick = { cat = c.displayName }, label = { Text(c.displayName, fontSize = 10.sp) })
                    }
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product Name") }, singleLine = true)
                OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = qty, onValueChange = { qty = it.filter { ch -> ch.isDigit() } }, label = { Text("Qty") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = price, onValueChange = { price = it.filter { ch -> ch.isDigit() || ch == '.' } }, label = { Text("Price ₦") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = comm, onValueChange = { comm = it.filter { ch -> ch.isDigit() || ch == '.' } }, label = { Text("24/7 % Fee") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = promo, onValueChange = { promo = it.filter { ch -> ch.isDigit() } }, label = { Text("Promo %") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(value = sizes, onValueChange = { sizes = it }, label = { Text("Sizes CSV") }, singleLine = true)
                OutlinedTextField(value = batch, onValueChange = { batch = it }, label = { Text("Batch Tag") }, singleLine = true)
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Admin Verification Note") })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        cat, name, brand,
                        qty.toIntOrNull() ?: submission.quantity,
                        sizes,
                        price.toDoubleOrNull() ?: submission.sellingPrice,
                        promo.toIntOrNull() ?: submission.promoDiscountPercent,
                        comm.toDoubleOrNull() ?: submission.commissionPercent,
                        batch, note
                    )
                }
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AdminPartnerFolderListSection(
    partners: List<PartnerOrganization>,
    submissions: List<PartnerProductSubmission>,
    transactions: List<PartnerFolderTransaction>,
    allProducts: List<com.example.data.model.Product>,
    nairaFormat: NumberFormat,
    onOpenFolder: (PartnerOrganization) -> Unit
) {
    if (partners.isEmpty()) {
        Text("No Partner Folders exist yet. Finalize a Partner MOU first.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }

    partners.forEach { folder ->
        val fProds = allProducts.filter { it.partnerId == folder.id }
        val fTxns = transactions.filter { it.partnerId == folder.id }
        val fSales = fTxns.filter {
            it.type == PartnerTransactionType.CUSTOMER_ORDER_PAYMENT_ALERT ||
                it.type == PartnerTransactionType.ORDER_CONFIRMED_SALE
        }
        val gross = fSales.sumOf { it.grossAmount }
        val profit = fSales.sumOf { it.commissionProfitAmount }
        val stockTotal = fProds.sumOf { it.stockQuantity }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, AccentGold.copy(alpha = 0.55f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenFolder(folder) }
                .testTag("partner_folder_card_${folder.id}")
        ) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AccentGold.copy(alpha = 0.16f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = AccentGold, modifier = Modifier.size(28.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(folder.businessName, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    Text(
                        "${folder.primaryCategory} • ${fProds.size} Live SKUs ($stockTotal units) • ${submissions.count { it.partnerId == folder.id }} Submissions",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Gross: ${nairaFormat.format(gross)} • 24/7 Profit: ${nairaFormat.format(profit)}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SuccessGreen
                    )
                }
                Icon(Icons.Default.Analytics, contentDescription = "Open Folder Analytics", tint = CyberCyan)
            }
        }
    }
}

@Composable
private fun AdminSinglePartnerFolderAnalytics(
    folder: PartnerOrganization,
    folderProducts: List<com.example.data.model.Product>,
    folderSubmissions: List<PartnerProductSubmission>,
    folderTransactions: List<PartnerFolderTransaction>,
    storeProfile: com.example.data.model.StoreProfileConfig,
    nairaFormat: NumberFormat,
    dateTimeFmt: SimpleDateFormat,
    onBackToFolders: () -> Unit,
    onUpdateAccounts: (String, String, String, String) -> Unit,
    onSaveReportPdf: (SalesReportPeriod) -> Unit
) {
    val context = LocalContext.current
    var reportPeriod by remember { mutableStateOf(SalesReportPeriod.DAILY) }
    var opayNum by remember(folder.id) { mutableStateOf(folder.opayAccountNumber) }
    var opayNm by remember(folder.id) { mutableStateOf(folder.opayAccountName) }
    var monieNum by remember(folder.id) { mutableStateOf(folder.moniepointAccountNumber) }
    var monieNm by remember(folder.id) { mutableStateOf(folder.moniepointAccountName) }
    var zoomReceipt by remember { mutableStateOf<String?>(null) }

    val saleTxns = remember(folderTransactions) {
        folderTransactions.filter {
            it.type == PartnerTransactionType.CUSTOMER_ORDER_PAYMENT_ALERT ||
                it.type == PartnerTransactionType.ORDER_CONFIRMED_SALE
        }
    }
    val grossSales = saleTxns.sumOf { it.grossAmount }
    val profitEarned = saleTxns.sumOf { it.commissionProfitAmount }
    val partnerNet = saleTxns.sumOf { it.partnerNetAmount }
    val unitsSold = saleTxns.sumOf { it.quantity }
    val liveStockBalance = folderProducts.sumOf { it.stockQuantity }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = onBackToFolders) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Partner Folders")
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(folder.businessName, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Text("Folder ${folder.id} • ${folder.primaryCategory} • ${folder.ownerPhone}", fontSize = 11.sp, color = CyberCyan)
        }
    }

    // Folder Financial & Stock Balance Analytics Card
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF052E16)),
        border = BorderStroke(1.5.dp, AccentGold.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("FOLDER ANALYTICS DASHBOARD", fontWeight = FontWeight.Black, color = AccentGold, fontSize = 12.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Gross Sales", fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.7f))
                    Text(nairaFormat.format(grossSales), fontWeight = FontWeight.Black, color = Color.White, fontSize = 15.sp)
                }
                Column {
                    Text("24/7 SHOPPING Profit", fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.7f))
                    Text(nairaFormat.format(profitEarned), fontWeight = FontWeight.Black, color = SuccessGreen, fontSize = 15.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Partner Net Payout", fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.7f))
                    Text(nairaFormat.format(partnerNet), fontWeight = FontWeight.Black, color = CyberCyan, fontSize = 15.sp)
                }
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Live SKUs: ${folderProducts.size}", fontSize = 11.sp, color = Color.White)
                Text("Units Sold: $unitsSold", fontSize = 11.sp, color = AccentGold, fontWeight = FontWeight.Bold)
                Text("Stock Balance: $liveStockBalance", fontSize = 11.sp, color = CyberCyan, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Separate Folder Payment Accounts (OPay & Moniepoint) + Auto Alert Status
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = OPayGreen, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Folder Payment Accounts (OPay & Moniepoint)", fontWeight = FontWeight.Black, fontSize = 13.sp)
            }
            Text(
                text = "Customer checkout for ${folder.businessName}'s items routes to these accounts & triggers automated payment alerts to both 24/7 SHOPPING and ${folder.businessName}.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = opayNum, onValueChange = { opayNum = it.filter { c -> c.isDigit() } }, label = { Text("OPay Account No.") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = opayNm, onValueChange = { opayNm = it }, label = { Text("OPay Account Name") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = monieNum, onValueChange = { monieNum = it.filter { c -> c.isDigit() } }, label = { Text("Moniepoint Account No.") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = monieNm, onValueChange = { monieNm = it }, label = { Text("Moniepoint Name") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Button(
                onClick = { onUpdateAccounts(opayNum, opayNm, monieNum, monieNm) },
                colors = ButtonDefaults.buttonColors(containerColor = OPayGreen, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Text("Save Folder Settlement Accounts", fontWeight = FontWeight.Bold)
            }
        }
    }

    // Daily / Weekly / Monthly Sales Report & Stock Balance Sender
    val reportPreviewText = remember(reportPeriod, folder, folderProducts, folderTransactions, storeProfile) {
        PartnerDocumentExporter.buildSalesAndStockReportText(folder, reportPeriod, folderProducts, folderTransactions, storeProfile)
    }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, CyberCyan.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Assessment, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Daily / Weekly / Monthly Sales & Stock Report", fontWeight = FontWeight.Black, fontSize = 13.sp, color = CyberCyan)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SalesReportPeriod.entries.forEach { period ->
                    FilterChip(
                        selected = reportPeriod == period,
                        onClick = { reportPeriod = period },
                        label = { Text(period.shortLabel, fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CyberCyan, selectedLabelColor = Color.Black)
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(reportPreviewText, fontSize = 10.5.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(10.dp))
            }
            Button(
                onClick = {
                    WhatsAppHelper.sendPartnerSalesAndStockReport(context, folder.ownerPhone, reportPreviewText)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("send_partner_sales_report_button")
            ) {
                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Send ${reportPeriod.shortLabel} Sales & Stock Report to Partner", fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        PartnerDocumentExporter.shareSalesReportPdf(context, folder, reportPeriod, folderProducts, folderTransactions, storeProfile)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share Report PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { onSaveReportPdf(reportPeriod) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save Report PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Complete Transaction Analytics Ledger (Upload -> Sale & Receipt)
    Text(
        text = "Transaction Lifecycle Ledger (${folderTransactions.size})",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Black
    )
    if (folderTransactions.isEmpty()) {
        Text("No folder lifecycle events recorded yet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        folderTransactions.forEach { txn ->
            val isSaleEvent = txn.type == PartnerTransactionType.CUSTOMER_ORDER_PAYMENT_ALERT ||
                txn.type == PartnerTransactionType.ORDER_CONFIRMED_SALE
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, if (isSaleEvent) SuccessGreen.copy(alpha = 0.6f) else CyberCyan.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(txn.type.label, fontWeight = FontWeight.Black, fontSize = 11.5.sp, color = if (isSaleEvent) SuccessGreen else CyberCyan)
                        Text(dateTimeFmt.format(Date(txn.timestamp)), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (txn.productName.isNotBlank()) {
                        Text("${txn.productName} (${txn.category}) • Qty: ${txn.quantity}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Amount: ${nairaFormat.format(txn.grossAmount)}", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "24/7 Profit (${txn.commissionPercent}%): ${nairaFormat.format(txn.commissionProfitAmount)}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            color = AccentGold
                        )
                    }
                    if (txn.customerName.isNotBlank() || txn.orderId.isNotBlank()) {
                        Text(
                            "Order: ${txn.orderId} • Customer: ${txn.customerName} (${txn.customerPhone})",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (txn.paymentAccount.isNotBlank()) {
                        Text(
                            "Settlement: ${txn.paymentMethod} • ${txn.paymentAccount} • Ref: ${txn.paymentReference}",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (txn.receiptImageUrl.isNotBlank() || isSaleEvent) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            if (txn.receiptImageUrl.isNotBlank()) {
                                AssistChip(
                                    onClick = { zoomReceipt = txn.receiptImageUrl },
                                    label = { Text("View Receipt", fontSize = 10.5.sp) }
                                )
                            }
                            if (isSaleEvent) {
                                Button(
                                    onClick = {
                                        WhatsAppHelper.sendPartnerPaymentAlert(
                                            context = context,
                                            recipientPhone = folder.ownerPhone,
                                            partnerName = folder.businessName,
                                            orderId = txn.orderId,
                                            customerName = txn.customerName,
                                            customerPhone = txn.customerPhone,
                                            productDetails = txn.productName,
                                            grossAmount = txn.grossAmount,
                                            commissionPercent = txn.commissionPercent,
                                            commissionProfit = txn.commissionProfitAmount,
                                            partnerNetPayout = txn.partnerNetAmount,
                                            paymentMethodLabel = txn.paymentMethod,
                                            paymentAccount = txn.paymentAccount,
                                            paymentRef = txn.paymentReference,
                                            storeProfile = storeProfile
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color.Black),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Send Payment Alert", fontSize = 10.5.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                    if (txn.note.isNotBlank()) {
                        Text(txn.note, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    zoomReceipt?.let { url ->
        ZoomableImageDialog(imageUrl = url, title = "Transaction Payment Receipt", onDismiss = { zoomReceipt = null })
    }
}
