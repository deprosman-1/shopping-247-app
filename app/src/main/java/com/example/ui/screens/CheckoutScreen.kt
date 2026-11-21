package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Loyalty
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.AppScreen
import com.example.MainViewModel
import com.example.data.model.Customer
import com.example.data.model.CustomerDeliveryInfo
import com.example.data.model.DeliveryZoneOption
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.PartnerOrganization
import com.example.data.model.PaymentMethod
import com.example.data.model.SavedAddress
import com.example.data.model.StoreProfileConfig
import com.example.ui.components.DualAxisScrollContainer
import com.example.ui.components.OrderProgressStepper
import com.example.ui.components.ZoomableImageDialog
import com.example.ui.theme.AccentGold
import com.example.ui.theme.CodOrange
import com.example.ui.theme.CyberCyan
import com.example.util.DeliveryZoneHelper
import com.example.util.PdfInvoiceExporter
import com.example.util.ProductImageHelper
import com.example.util.WhatsAppHelper
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Switch
import androidx.compose.ui.graphics.FilterQuality
import com.example.ui.theme.MoniepointBlue
import com.example.ui.theme.OPayGreen
import com.example.ui.theme.PosPurple
import com.example.ui.theme.SuccessGreen
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CheckoutStep {
    SELECT_DELIVERY_CONDITION,                          // Step 1: SELECT DELIVERY CONDITIONS
    UPLOAD_PAYMENT_RECEIPT_ORDER_CONFIRMATION_AND_TOTAL,// Step 2: UPLOAD PAYMENT RECEIPT ORDER CONFIRMATION AND TOTAL
    PAYMENT_CONFIRMED_STATUS                            // Step 3: Payment Confirmed & Delivery Status (Then, appears the next page)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentCustomer by viewModel.currentCustomer.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val subtotal by viewModel.cartSubtotal.collectAsState()
    
    val deliveryOptions by viewModel.deliveryOptions.collectAsState()
    val storeProfile by viewModel.storeProfile.collectAsState()
    val partnerOrganizations by viewModel.partnerOrganizations.collectAsState()
    val activePartnerFolder = remember(cartItems, partnerOrganizations) {
        viewModel.resolvePartnerFolderForCart(cartItems)
    }
    val savedDeliveryInfo by viewModel.savedDeliveryInfo.collectAsState()
    val savedSenderName by viewModel.savedSenderName.collectAsState()
    val selectedDeliveryZone by viewModel.selectedDeliveryZone.collectAsState()
    val savedAddresses by viewModel.savedAddresses.collectAsState()
    val loyaltyAccount by viewModel.loyaltyAccount.collectAsState()
    val isWideDualAxisMode by viewModel.isWideDualAxisMode.collectAsState()
    var selectedDeliveryOption by remember { mutableStateOf<com.example.data.model.DeliveryOption?>(null) }

    LaunchedEffect(deliveryOptions) {
        if (selectedDeliveryOption == null && deliveryOptions.isNotEmpty()) {
            selectedDeliveryOption = deliveryOptions.first()
        }
    }

    val baseOptionFee = selectedDeliveryOption?.price ?: 0.0
    val zoneFee = selectedDeliveryZone.fee
    val deliveryFee = baseOptionFee + zoneFee
    val total = subtotal + deliveryFee

    var hasManuallyEditedDelivery by rememberSaveable { mutableStateOf(false) }
    var fullName by remember {
        mutableStateOf(currentCustomer?.fullName?.takeIf { it.isNotBlank() } ?: savedDeliveryInfo.fullName)
    }
    var phone by remember {
        mutableStateOf(currentCustomer?.phoneNumber?.takeIf { it.isNotBlank() } ?: savedDeliveryInfo.phoneNumber)
    }
    var address by remember {
        mutableStateOf(currentCustomer?.deliveryAddress?.takeIf { it.isNotBlank() } ?: savedDeliveryInfo.deliveryAddress)
    }

    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.OPAY) }
    var senderName by remember {
        mutableStateOf(
            savedSenderName.ifBlank {
                currentCustomer?.fullName?.takeIf { it.isNotBlank() } ?: savedDeliveryInfo.fullName
            }
        )
    }
    var paymentReference by rememberSaveable { mutableStateOf("REF-${System.currentTimeMillis().toString().takeLast(6)}") }
    var receiptImageUri by remember { mutableStateOf<Uri?>(null) }

    var currentStepName by rememberSaveable { mutableStateOf(CheckoutStep.SELECT_DELIVERY_CONDITION.name) }
    val currentStep = CheckoutStep.entries.firstOrNull { it.name == currentStepName } ?: CheckoutStep.SELECT_DELIVERY_CONDITION
    var isPlacingOrder by remember { mutableStateOf(false) }
    var showNoReceiptDialog by remember { mutableStateOf(false) }
    var appliedCheckoutBargainPrice by remember { mutableStateOf<Double?>(null) }
    var proposedCheckoutBargainInput by remember { mutableStateOf("") }
    var couponCodeInput by rememberSaveable { mutableStateOf("") }
    var appliedCouponCode by rememberSaveable { mutableStateOf("") }
    var couponDiscountAmount by remember { mutableStateOf(0.0) }
    var redeemLoyaltyPoints by rememberSaveable { mutableStateOf(false) }
    val maxRedeemablePts = remember(loyaltyAccount.pointsBalance, subtotal, couponDiscountAmount) {
        val remainingSub = (subtotal - couponDiscountAmount).coerceAtLeast(0.0)
        kotlin.math.floor(remainingSub / 50.0).toInt().coerceAtMost(loyaltyAccount.pointsBalance).coerceAtLeast(0)
    }
    val appliedLoyaltyPoints = if (redeemLoyaltyPoints) maxRedeemablePts else 0
    val loyaltyDiscountAmount = appliedLoyaltyPoints * 50.0
    var placedOrderId by rememberSaveable { mutableStateOf<String?>(null) }
    var placedOrderSnapshot by remember { mutableStateOf<Order?>(null) }
    val allOrders by viewModel.allOrders.collectAsState()
    val liveOrder = remember(placedOrderId, placedOrderSnapshot, allOrders) {
        placedOrderId?.let { id -> allOrders.find { it.orderId == id } } ?: placedOrderSnapshot
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            receiptImageUri = uri
            Toast.makeText(context, "Payment Receipt Selected!", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(currentCustomer, savedDeliveryInfo, savedSenderName) {
        val autoName = currentCustomer?.fullName?.takeIf { it.isNotBlank() } ?: savedDeliveryInfo.fullName
        val autoPhone = currentCustomer?.phoneNumber?.takeIf { it.isNotBlank() } ?: savedDeliveryInfo.phoneNumber
        val autoAddress = currentCustomer?.deliveryAddress?.takeIf { it.isNotBlank() } ?: savedDeliveryInfo.deliveryAddress
        val autoSender = savedSenderName.ifBlank { autoName }
        if (!hasManuallyEditedDelivery) {
            if (fullName.isBlank() && autoName.isNotBlank()) fullName = autoName
            if (phone.isBlank() && autoPhone.isNotBlank()) phone = autoPhone
            if (address.isBlank() && autoAddress.isNotBlank()) address = autoAddress
            if (senderName.isBlank() && autoSender.isNotBlank()) senderName = autoSender
        }
    }

    val nairaFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "NG")).apply {
            maximumFractionDigits = 0
        }
    }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    BackHandler {
        if (currentStep == CheckoutStep.UPLOAD_PAYMENT_RECEIPT_ORDER_CONFIRMATION_AND_TOTAL) {
            currentStepName = CheckoutStep.SELECT_DELIVERY_CONDITION.name
        } else if (currentStep == CheckoutStep.PAYMENT_CONFIRMED_STATUS) {
            viewModel.navigateToHome()
        } else {
            viewModel.navigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentStep) {
                            CheckoutStep.SELECT_DELIVERY_CONDITION -> "SELECT DELIVERY CONDITIONS"
                            CheckoutStep.UPLOAD_PAYMENT_RECEIPT_ORDER_CONFIRMATION_AND_TOTAL -> "Order Confirmation & Total"
                            CheckoutStep.PAYMENT_CONFIRMED_STATUS -> "Payment & Delivery Status"
                        },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (currentStep == CheckoutStep.UPLOAD_PAYMENT_RECEIPT_ORDER_CONFIRMATION_AND_TOTAL) {
                                currentStepName = CheckoutStep.SELECT_DELIVERY_CONDITION.name
                            } else if (currentStep == CheckoutStep.PAYMENT_CONFIRMED_STATUS) {
                                viewModel.navigateToHome()
                            } else {
                                viewModel.navigateBack()
                            }
                        },
                        modifier = Modifier.testTag("checkout_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.navigateToHome() },
                        modifier = Modifier.testTag("checkout_home_button")
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
                    .padding(16.dp)
            ) {
                when (currentStep) {
                    CheckoutStep.SELECT_DELIVERY_CONDITION -> {
                        SelectDeliveryConditionStep(
                            deliveryOptions = deliveryOptions,
                            selectedOption = selectedDeliveryOption,
                            onSelectOption = { selectedDeliveryOption = it },
                            deliveryZones = DeliveryZoneHelper.zones,
                            selectedZone = selectedDeliveryZone,
                            onSelectZone = { viewModel.selectDeliveryZone(it) },
                            savedAddresses = savedAddresses,
                            onQuickPickAddress = { addr ->
                                hasManuallyEditedDelivery = true
                                fullName = addr.recipientName
                                phone = addr.phoneNumber
                                address = addr.streetAddress
                                viewModel.selectDeliveryZone(DeliveryZoneHelper.findByName(addr.stateZone))
                                viewModel.persistDeliveryInfoLocally(
                                    fullName = addr.recipientName,
                                    phoneNumber = addr.phoneNumber,
                                    deliveryAddress = addr.streetAddress
                                )
                                Toast.makeText(context, "Loaded ${addr.label} address & zone!", Toast.LENGTH_SHORT).show()
                            },
                            onContinue = { currentStepName = CheckoutStep.UPLOAD_PAYMENT_RECEIPT_ORDER_CONFIRMATION_AND_TOTAL.name },
                            subtotal = subtotal,
                            nairaFormat = nairaFormat,
                            autoFullName = fullName,
                            autoPhone = phone,
                            autoAddress = address
                        )
                    }

                    CheckoutStep.UPLOAD_PAYMENT_RECEIPT_ORDER_CONFIRMATION_AND_TOTAL -> {
                        UploadPaymentReceiptOrderConfirmationAndTotalStep(
                            cartItems = cartItems,
                            subtotal = subtotal,
                            selectedDeliveryOption = selectedDeliveryOption,
                            deliveryFee = deliveryFee,
                            total = total,
                            fullName = fullName,
                            onFullNameChange = {
                                hasManuallyEditedDelivery = true
                                fullName = it
                                viewModel.persistDeliveryInfoLocally(fullName = it)
                            },
                            phone = phone,
                            onPhoneChange = {
                                hasManuallyEditedDelivery = true
                                phone = it
                                viewModel.persistDeliveryInfoLocally(phoneNumber = it)
                            },
                            address = address,
                            onAddressChange = {
                                hasManuallyEditedDelivery = true
                                address = it
                                viewModel.persistDeliveryInfoLocally(deliveryAddress = it)
                            },
                            selectedPaymentMethod = selectedPaymentMethod,
                            onSelectPaymentMethod = { selectedPaymentMethod = it },
                            senderName = senderName,
                            onSenderNameChange = {
                                senderName = it
                                viewModel.persistDeliveryInfoLocally(senderName = it)
                            },
                            paymentReference = paymentReference,
                            onPaymentReferenceChange = { paymentReference = it },
                            receiptImageUri = receiptImageUri,
                            onChooseReceipt = { photoPickerLauncher.launch("image/*") },
                            currentCustomer = currentCustomer,
                            hasSavedDelivery = (currentCustomer != null) || savedDeliveryInfo.fullName.isNotBlank() || savedDeliveryInfo.phoneNumber.isNotBlank() || savedDeliveryInfo.deliveryAddress.isNotBlank(),
                            nairaFormat = nairaFormat,
                            onSubmitOrder = {
                                if (isPlacingOrder) return@UploadPaymentReceiptOrderConfirmationAndTotalStep
                                
                                if (fullName.isBlank() || phone.isBlank() || address.isBlank()) {
                                    Toast.makeText(context, "Please enter your Name, Phone and Delivery Address", Toast.LENGTH_SHORT).show()
                                    return@UploadPaymentReceiptOrderConfirmationAndTotalStep
                                }
                                if (receiptImageUri == null) {
                                    showNoReceiptDialog = true
                                    return@UploadPaymentReceiptOrderConfirmationAndTotalStep
                                }
                                if (cartItems.isEmpty() && liveOrder == null) {
                                    Toast.makeText(context, "Your cart is empty", Toast.LENGTH_SHORT).show()
                                    return@UploadPaymentReceiptOrderConfirmationAndTotalStep
                                }

                                val opayNum = activePartnerFolder?.opayAccountNumber?.takeIf { it.isNotBlank() }
                                    ?: storeProfile.opayAccountNumber.ifBlank { "Contact Store for Account" }
                                val opayName = activePartnerFolder?.opayAccountName?.takeIf { it.isNotBlank() }
                                    ?: storeProfile.opayAccountName.ifBlank { storeProfile.storeName }
                                val monieNum = activePartnerFolder?.moniepointAccountNumber?.takeIf { it.isNotBlank() }
                                    ?: storeProfile.moniepointAccountNumber.ifBlank { "Contact Store for Account" }
                                val monieName = activePartnerFolder?.moniepointAccountName?.takeIf { it.isNotBlank() }
                                    ?: storeProfile.moniepointAccountName.ifBlank { storeProfile.storeName }
                                val posId = storeProfile.posTerminalId.ifBlank { storeProfile.storeName }
                                val folderTag = if (activePartnerFolder != null) " [Folder: ${activePartnerFolder.businessName}]" else ""
                                val accountOrTerminal = when (selectedPaymentMethod) {
                                    PaymentMethod.OPAY -> "OPay: $opayNum ($opayName)$folderTag"
                                    PaymentMethod.MONIEPOINT -> "Moniepoint: $monieNum ($monieName)$folderTag"
                                    PaymentMethod.POS_TRANSFER -> "POS Terminal: $posId"
                                    PaymentMethod.CASH_ON_DELIVERY -> "Cash / Card at Doorstep"
                                }

                                val receiptUrl = receiptImageUri?.toString() ?: ""

                                isPlacingOrder = true
                                viewModel.placeOrder(
                                    customerName = fullName,
                                    customerEmail = "",
                                    customerPhone = phone,
                                    deliveryAddress = address,
                                    paymentMethod = selectedPaymentMethod,
                                    paymentAccountOrTerminal = accountOrTerminal,
                                    paymentReference = paymentReference,
                                    senderName = senderName.ifBlank { fullName },
                                    deliveryConditionTitle = "${selectedDeliveryOption?.title ?: "Standard Delivery"} (${selectedDeliveryZone.zoneName} • ${selectedDeliveryZone.etaLabel})",
                                    deliveryFeeAmount = deliveryFee,
                                    receiptImageUrl = receiptUrl,
                                    customBargainTotal = appliedCheckoutBargainPrice,
                                    appliedCouponCode = appliedCouponCode,
                                    couponDiscountAmount = couponDiscountAmount,
                                    loyaltyPointsRedeemed = appliedLoyaltyPoints,
                                    loyaltyDiscountAmount = loyaltyDiscountAmount,
                                    onSuccess = { order ->
                                        isPlacingOrder = false
                                        placedOrderSnapshot = order
                                        placedOrderId = order.orderId
                                        currentStepName = CheckoutStep.PAYMENT_CONFIRMED_STATUS.name
                                        val syncMsg = viewModel.cloudStatusMessage.value
                                        if (!syncMsg.isNullOrBlank()) {
                                            Toast.makeText(context, syncMsg, Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    onFailure = { error ->
                                        isPlacingOrder = false
                                        Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            onAutofill = {
                                val autoName = currentCustomer?.fullName?.takeIf { it.isNotBlank() } ?: savedDeliveryInfo.fullName
                                val autoPhone = currentCustomer?.phoneNumber?.takeIf { it.isNotBlank() } ?: savedDeliveryInfo.phoneNumber
                                val autoAddress = currentCustomer?.deliveryAddress?.takeIf { it.isNotBlank() } ?: savedDeliveryInfo.deliveryAddress
                                val autoSender = savedSenderName.ifBlank { autoName }
                                fullName = autoName
                                phone = autoPhone
                                address = autoAddress
                                if (senderName.isBlank() && autoSender.isNotBlank()) senderName = autoSender
                                hasManuallyEditedDelivery = false
                                viewModel.persistDeliveryInfoLocally(
                                    fullName = autoName,
                                    phoneNumber = autoPhone,
                                    deliveryAddress = autoAddress,
                                    senderName = autoSender
                                )
                                Toast.makeText(context, "Delivery information auto-filled!", Toast.LENGTH_SHORT).show()
                            },
                            onSavePermanent = {
                                viewModel.persistDeliveryInfoLocally(
                                    fullName = fullName,
                                    phoneNumber = phone,
                                    deliveryAddress = address,
                                    senderName = senderName
                                )
                                viewModel.saveCustomerDeliveryInfo(fullName, phone, address)
                                Toast.makeText(context, "Customer delivery details permanently saved!", Toast.LENGTH_SHORT).show()
                            },
                            isPlacingOrder = isPlacingOrder,
                            appliedBargainPrice = appliedCheckoutBargainPrice,
                            onBargainPriceApplied = { approved, msg, isCapped ->
                                appliedCheckoutBargainPrice = approved
                            },
                            proposedBargainInput = proposedCheckoutBargainInput,
                            onProposedBargainInputChange = { proposedCheckoutBargainInput = it },
                            couponCodeInput = couponCodeInput,
                            onCouponCodeInputChange = { couponCodeInput = it },
                            appliedCouponCode = appliedCouponCode,
                            couponDiscountAmount = couponDiscountAmount,
                            onApplyCoupon = { rawCode ->
                                val (coupon, feedback) = viewModel.validateCoupon(rawCode, subtotal)
                                if (coupon != null) {
                                    appliedCouponCode = coupon.code
                                    couponDiscountAmount = subtotal * (coupon.discountPercent / 100.0)
                                } else {
                                    appliedCouponCode = ""
                                    couponDiscountAmount = 0.0
                                }
                                Toast.makeText(context, feedback, Toast.LENGTH_LONG).show()
                            },
                            onClearCoupon = {
                                couponCodeInput = ""
                                appliedCouponCode = ""
                                couponDiscountAmount = 0.0
                            },
                            loyaltyPointsBalance = loyaltyAccount.pointsBalance,
                            redeemLoyaltyPoints = redeemLoyaltyPoints,
                            onToggleRedeemLoyalty = { redeemLoyaltyPoints = it },
                            loyaltyPointsRedeemed = appliedLoyaltyPoints,
                            loyaltyDiscountAmount = loyaltyDiscountAmount,
                            selectedDeliveryZone = selectedDeliveryZone,
                            savedAddresses = savedAddresses,
                            onQuickPickAddress = { addr ->
                                hasManuallyEditedDelivery = true
                                fullName = addr.recipientName
                                phone = addr.phoneNumber
                                address = addr.streetAddress
                                viewModel.selectDeliveryZone(DeliveryZoneHelper.findByName(addr.stateZone))
                                viewModel.persistDeliveryInfoLocally(
                                    fullName = addr.recipientName,
                                    phoneNumber = addr.phoneNumber,
                                    deliveryAddress = addr.streetAddress
                                )
                                Toast.makeText(context, "Loaded ${addr.label} address!", Toast.LENGTH_SHORT).show()
                            },
                            storeProfile = storeProfile,
                            partnerFolder = activePartnerFolder,
                            viewModel = viewModel
                        )
                    }

                    CheckoutStep.PAYMENT_CONFIRMED_STATUS -> {
                        PaymentConfirmedStatusStep(
                            order = liveOrder,
                            receiptImageUri = receiptImageUri,
                            storeProfile = storeProfile,
                            savedDeliveryInfo = savedDeliveryInfo,
                            currentCustomer = currentCustomer,
                            nairaFormat = nairaFormat,
                            dateFormat = dateFormat,
                            viewModel = viewModel,
                            onDone = {
                                viewModel.navigateTo(AppScreen.ORDERS)
                            },
                            onContinueShopping = {
                                viewModel.navigateToHome()
                            }
                        )
                    }
                }
            }
        }
    }

    if (showNoReceiptDialog) {
        AlertDialog(
            onDismissRequest = { showNoReceiptDialog = false },
            title = {
                Text(
                    text = "ORDER DECLINED",
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFDC2626)
                )
            },
            text = {
                Text(
                    text = "attach payment receipt to complete order process",
                    color = Color(0xFFDC2626),
                    fontWeight = FontWeight.Bold
                )
            },
            confirmButton = {
                TextButton(onClick = { showNoReceiptDialog = false }) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }
}

// ==============================================================
// STEP 0: SELECT DELIVERY CONDITION
// ==============================================================
@Composable
private fun SelectDeliveryConditionStep(
    deliveryOptions: List<com.example.data.model.DeliveryOption>,
    selectedOption: com.example.data.model.DeliveryOption?,
    onSelectOption: (com.example.data.model.DeliveryOption) -> Unit,
    deliveryZones: List<DeliveryZoneOption>,
    selectedZone: DeliveryZoneOption,
    onSelectZone: (DeliveryZoneOption) -> Unit,
    savedAddresses: List<SavedAddress> = emptyList(),
    onQuickPickAddress: (SavedAddress) -> Unit = {},
    onContinue: () -> Unit,
    subtotal: Double,
    nairaFormat: NumberFormat,
    autoFullName: String = "",
    autoPhone: String = "",
    autoAddress: String = ""
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalShipping, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SELECT DELIVERY CONDITIONS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Choose your preferred delivery location or VIP package option. The fee will be added to your order subtotal.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Automatically display saved Customer Delivery Information on the delivery page
        if (autoFullName.isNotBlank() || autoPhone.isNotBlank() || autoAddress.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.08f)),
                border = BorderStroke(1.2.dp, SuccessGreen.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auto_customer_delivery_info_card")
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CUSTOMER DELIVERY INFORMATION (AUTO-FILLED)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = SuccessGreen
                        )
                    }
                    if (autoFullName.isNotBlank()) {
                        Text(text = "Recipient: $autoFullName", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                    if (autoPhone.isNotBlank()) {
                        Text(text = "Phone: $autoPhone", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                    if (autoAddress.isNotBlank()) {
                        Text(text = "Address: $autoAddress", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        deliveryOptions.forEach { option ->
            val isSelected = selectedOption?.id == option.id
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { onSelectOption(option) }
                    .testTag("delivery_option_${option.id}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelectOption(option) }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = option.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (option.price == 0.0) "FREE" else nairaFormat.format(option.price),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Black,
                        color = if (option.price == 0.0) SuccessGreen else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (savedAddresses.isNotEmpty()) {
            Text(
                text = "Quick-Pick Saved Address:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = CyberCyan
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                savedAddresses.forEach { addr ->
                    FilterChip(
                        selected = autoAddress.equals(addr.streetAddress, ignoreCase = true),
                        onClick = { onQuickPickAddress(addr) },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(15.dp)) },
                        label = { Text("${addr.label} (${addr.stateZone})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = CyberCyan.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DELIVERY ZONE & ETA CALCULATOR",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = CyberCyan
                    )
                    Text(
                        text = selectedZone.etaLabel,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SuccessGreen
                    )
                }
                deliveryZones.forEach { zone ->
                    val zoneSelected = selectedZone.id == zone.id
                    Surface(
                        onClick = { onSelectZone(zone) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (zoneSelected) CyberCyan.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(if (zoneSelected) 1.5.dp else 1.dp, if (zoneSelected) CyberCyan else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delivery_zone_${zone.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = zoneSelected, onClick = { onSelectZone(zone) })
                            Column(modifier = Modifier.weight(1f)) {
                                Text(zone.zoneName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("${zone.statesSummary} • ETA: ${zone.etaLabel}", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = if (zone.fee == 0.0) "FREE" else nairaFormat.format(zone.fee),
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = if (zone.fee == 0.0) SuccessGreen else CyberCyan
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val combinedDeliveryFee = (selectedOption?.price ?: 0.0) + selectedZone.fee
        Button(
            onClick = onContinue,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("continue_to_summary_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = "Proceed with Delivery (${if (combinedDeliveryFee == 0.0) "FREE" else nairaFormat.format(combinedDeliveryFee)} • ${selectedZone.etaLabel})",
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}



// ==============================================================
// STEP 3: PAYMENT RECEIPT UPLOAD & ORDER INFORMATION
// ==============================================================
@Composable
private fun PaymentReceiptUploadStep(
    order: Order?,
    selectedPaymentMethod: PaymentMethod,
    onSelectPaymentMethod: (PaymentMethod) -> Unit,
    senderName: String,
    onSenderNameChange: (String) -> Unit,
    paymentReference: String,
    onPaymentReferenceChange: (String) -> Unit,
    receiptImageUri: Uri?,
    onChooseReceipt: () -> Unit,
    onSendPaymentReceipt: () -> Unit,
    nairaFormat: NumberFormat,
    storeProfile: StoreProfileConfig = StoreProfileConfig()
) {
    // Top banner
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = CyberCyan.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = CyberCyan)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Order Logged: ${order?.orderId ?: "ORD-PENDING"}",
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan
                )
                Text(
                    text = "Upload your proof of payment so admin can process delivery dispatch.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Order Information Recap
    Text("Order Information & Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(6.dp))
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Recipient:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(order?.customerName ?: "Customer", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Items:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val cleanShortSummary = order?.itemsSummary?.replace(Regex("""\s*\[Sold:[^\]]+\]"""), "")?.trim()
                Text(cleanShortSummary?.take(30) ?: "Products", fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Payable:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(nairaFormat.format(order?.totalAmount ?: 0.0), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    Spacer(modifier = Modifier.height(18.dp))

    // Payment Channel Details
    Text("Payment Account Transfer Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))

    // Payment Option 1: OPAY
    PaymentOptionCard(
        title = "OPay Transfer",
        subtitle = "Instant settlement via OPay App / USSD",
        brandColor = OPayGreen,
        icon = Icons.Default.PhoneAndroid,
        isSelected = selectedPaymentMethod == PaymentMethod.OPAY,
        onSelect = { onSelectPaymentMethod(PaymentMethod.OPAY) }
    ) {
        BankTransferDetailsBox(
            bankName = "OPay (Paycom)",
            accountNumber = storeProfile.opayAccountNumber.ifBlank { "Contact Store for Account" },
            accountName = storeProfile.opayAccountName.ifBlank { storeProfile.storeName },
            brandColor = OPayGreen
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Payment Option 2: MONIEPOINT
    PaymentOptionCard(
        title = "Moniepoint MFB",
        subtitle = "Direct bank transfer to Moniepoint Account",
        brandColor = MoniepointBlue,
        icon = Icons.Default.AccountBalance,
        isSelected = selectedPaymentMethod == PaymentMethod.MONIEPOINT,
        onSelect = { onSelectPaymentMethod(PaymentMethod.MONIEPOINT) }
    ) {
        BankTransferDetailsBox(
            bankName = "Moniepoint Microfinance Bank",
            accountNumber = storeProfile.moniepointAccountNumber.ifBlank { "Contact Store for Account" },
            accountName = storeProfile.moniepointAccountName.ifBlank { storeProfile.storeName },
            brandColor = MoniepointBlue
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Payment Option 3: POS TRANSFER
    PaymentOptionCard(
        title = "POS Terminal Transfer",
        subtitle = "Terminal debit swipe or agent slip transfer",
        brandColor = PosPurple,
        icon = Icons.Default.CreditCard,
        isSelected = selectedPaymentMethod == PaymentMethod.POS_TRANSFER,
        onSelect = { onSelectPaymentMethod(PaymentMethod.POS_TRANSFER) }
    ) {
        PosTransferDetailsBox(storeProfile = storeProfile)
    }

    Spacer(modifier = Modifier.height(16.dp))

    // UPLOAD PAYMENT RECEIPT BOX
    Text("Upload Payment Receipt Slip / Screenshot", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Text("Select photo or screenshot of your successful transaction", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

    Spacer(modifier = Modifier.height(10.dp))

    var previewZoomOpen by remember { mutableStateOf(false) }
    val previewCtx = LocalContext.current

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        border = BorderStroke(1.5.dp, if (receiptImageUri != null) SuccessGreen else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (receiptImageUri != null) {
                // Image preview (tap to zoom)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.05f))
                        .clickable { previewZoomOpen = true }
                ) {
                    AsyncImage(
                        model = ProductImageHelper.buildImageRequest(
                            context = previewCtx,
                            imageUrl = receiptImageUri.toString(),
                            title = "Uploaded Payment Receipt",
                            highRes = true
                        ),
                        contentDescription = "Uploaded Receipt Preview (Tap to zoom)",
                        contentScale = ContentScale.Fit,
                        filterQuality = FilterQuality.High,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (previewZoomOpen) {
                    ZoomableImageDialog(
                        imageUrl = receiptImageUri.toString(),
                        title = "Uploaded Payment Receipt",
                        onDismiss = { previewZoomOpen = false }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Payment Receipt Attached & Visible", fontWeight = FontWeight.Bold, color = SuccessGreen, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onChooseReceipt, shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Change Receipt Photo")
                }
            } else {
                // Empty state prompt to choose photo
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("No receipt selected yet", fontWeight = FontWeight.Bold)
                Text("Tap button below to select receipt from gallery/files", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onChooseReceipt,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("upload_receipt_picker_button")
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Payment Receipt Image")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = senderName,
                onValueChange = onSenderNameChange,
                label = { Text("Sender / Account Holder Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("receipt_sender_name_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = paymentReference,
                onValueChange = onPaymentReferenceChange,
                label = { Text("Payment Session ID / STAN / Reference") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("receipt_reference_input")
            )
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // BUTTON: SEND PAYMENT RECEIPT TO ADMIN
    Button(
        onClick = onSendPaymentReceipt,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("send_payment_receipt_button"),
        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
    ) {
        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Send Payment Receipt", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
    }

    Spacer(modifier = Modifier.height(30.dp))
}

// ==============================================================
// STEP 3: PAYMENT CONFIRMATION & OFFICIAL RECORD (TWO-PHASE LIVE)
// ==============================================================
@Composable
private fun PaymentConfirmedStatusStep(
    order: Order?,
    receiptImageUri: Uri?,
    storeProfile: StoreProfileConfig = StoreProfileConfig(),
    savedDeliveryInfo: CustomerDeliveryInfo = CustomerDeliveryInfo(),
    currentCustomer: Customer? = null,
    nairaFormat: NumberFormat,
    dateFormat: SimpleDateFormat,
    viewModel: MainViewModel,
    onDone: () -> Unit,
    onContinueShopping: () -> Unit
) {
    val context = LocalContext.current
    var viewingSlipZoom by remember { mutableStateOf(false) }
    val slipModel = receiptImageUri ?: order?.receiptImageUrl?.takeIf { it.isNotBlank() }

    val isConfirmed = order?.status == OrderStatus.VERIFIED ||
            order?.status == OrderStatus.OUT_FOR_DELIVERY ||
            order?.status == OrderStatus.DELIVERED

    val themeColor = if (isConfirmed) SuccessGreen else Color(0xFFD97706)
    val statusBg = if (isConfirmed) SuccessGreen.copy(alpha = 0.1f) else Color(0xFFD97706).copy(alpha = 0.12f)
    val statusBorder = if (isConfirmed) SuccessGreen.copy(alpha = 0.5f) else Color(0xFFD97706).copy(alpha = 0.5f)

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        border = BorderStroke(1.5.dp, statusBorder),
        modifier = Modifier.fillMaxWidth().testTag("payment_confirmed_status_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Official Store Label
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(
                    text = "${storeProfile.storeName} • OFFICIAL ORDER RECORD",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    letterSpacing = 0.5.sp
                )
            }

            // Status Icon Circle
            Surface(
                shape = CircleShape,
                color = statusBg,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isConfirmed) Icons.Default.Verified else Icons.Default.Schedule,
                        contentDescription = null,
                        tint = themeColor,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Two-phase Live Title
            Text(
                text = if (isConfirmed) "Payment Received & Confirmed!" else "Payment Receipt Sent — Awaiting Confirmation",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                color = if (isConfirmed) SuccessGreen else Color(0xFFD97706)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isConfirmed)
                    "Physical bank alert has been confirmed by Admin Cashier. Your order is processing and preparing for delivery dispatch."
                else
                    "Your payment receipt has been delivered to Admin Cashier. Awaiting confirmation of physical bank alert.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Delivery Status Badge
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = statusBg,
                border = BorderStroke(1.dp, statusBorder)
            ) {
                Text(
                    text = if (isConfirmed) "● PAYMENT CONFIRMED & ORDER IS PROCESSING" else "● AWAITING ADMIN PHYSICAL ALERT CONFIRMATION",
                    color = themeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            OrderProgressStepper(status = order?.status ?: OrderStatus.PENDING_VERIFICATION)

            if (!isConfirmed) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFD97706).copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Real-Time Verification Pending", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFD97706))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${storeProfile.storeName} Cashier is currently confirming the credit alert on our bank terminal. As soon as admin confirms the alert and clicks 'Payment Confirmed', this page changes instantly to 'Payment Confirmed & Order is Processing'.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (order?.verificationNote.isNullOrBlank().not()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SuccessGreen.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = order?.verificationNote ?: "Verified by Admin Cashier",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SuccessGreen
                        )
                    }
                }
            }

            val isCloudConnected by viewModel.isSupabaseConnected.collectAsState()
            val isCloudBusy by viewModel.isSupabaseBusy.collectAsState()
            val needsCloudLink = remember(isCloudConnected, order) {
                viewModel.repository.supabaseSync.isPlaceholderConfig()
            }
            var checkoutSyncLink by remember { mutableStateOf("") }
            if (needsCloudLink) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEF4444).copy(alpha = 0.08f),
                    border = BorderStroke(1.5.dp, Color(0xFFEF4444).copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "⚠️ Saved Locally — Phone Not Linked to Store Cloud",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = Color(0xFFB91C1C)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Paste the Seller's Store Sync Link below (or open their depros://connect link) to upload this order & your sign-up straight to the Admin Control Room.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = checkoutSyncLink,
                            onValueChange = { checkoutSyncLink = it },
                            label = { Text("Paste Store Link or Full WhatsApp Invite") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            enabled = !isCloudBusy && checkoutSyncLink.isNotBlank(),
                            onClick = {
                                viewModel.applyStoreSyncLinkOrCredentials(checkoutSyncLink) { _, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isCloudBusy) "Uploading Order to Admin..." else "Connect & Send Order to Admin Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ==============================================================
            // TRANSFERRED INVOICE DATA: ORDER DETAILS & TIMESTAMP
            // ==============================================================
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Order Reference Number", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(order?.orderId ?: "ORD-SUCCESS", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                            IconButton(
                                onClick = {
                                    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cb.setPrimaryClip(ClipData.newPlainText("Order ID", order?.orderId ?: ""))
                                    Toast.makeText(context, "Copied Order Number!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp).padding(start = 4.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Order ID", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Order Date & Time", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(dateFormat.format(Date(order?.createdAt ?: System.currentTimeMillis())), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ==============================================================
            // TRANSFERRED INVOICE DATA: CUSTOMER PROFILE & SHIPPING
            // ==============================================================
            val displayCustomerName = order?.customerName?.takeIf { it.isNotBlank() }
                ?: currentCustomer?.fullName?.takeIf { it.isNotBlank() }
                ?: savedDeliveryInfo.fullName.ifBlank { "Customer" }
            val displayCustomerPhone = order?.customerPhone?.takeIf { it.isNotBlank() }
                ?: currentCustomer?.phoneNumber?.takeIf { it.isNotBlank() }
                ?: savedDeliveryInfo.phoneNumber.ifBlank { "Verified Phone" }
            val displayCustomerAddress = order?.deliveryAddress?.takeIf { it.isNotBlank() }
                ?: currentCustomer?.deliveryAddress?.takeIf { it.isNotBlank() }
                ?: savedDeliveryInfo.deliveryAddress.ifBlank { "Address" }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Customer Profile & Delivery Details", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SuccessGreen.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = "SAVED",
                                color = SuccessGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Customer Name", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(displayCustomerName, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Contact Phone", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(displayCustomerPhone, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Delivery Destination", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = displayCustomerAddress,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End,
                            modifier = Modifier.widthIn(max = 220.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Estimated Timeline", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("24 - 48 Hours Express Dispatch", fontWeight = FontWeight.Bold, color = SuccessGreen)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ==============================================================
            // OFFICIAL PAYMENT SETTLEMENT DETAILS
            // ==============================================================
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Payment & Settlement Details", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Payment Channel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(order?.paymentMethod?.title ?: "OPay Transfer", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Destination Account", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(order?.paymentAccount ?: storeProfile.storeName, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Sender Account Name", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(order?.senderName ?: "Customer", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Session / STAN / Ref", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(order?.paymentReference ?: "REF-OFFICIAL", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                    if (!order?.partnerName.isNullOrBlank()) {
                        val partnersList by viewModel.partnerOrganizations.collectAsState()
                        val orderPartner = partnersList.find { it.id == order?.partnerId }
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = AccentGold.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Automated Payment Alerts Issued (${order?.partnerName})",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.5.sp,
                                    color = AccentGold
                                )
                                Text(
                                    text = "24/7 SHOPPING & ${order?.partnerName} received automated alert logs for this order.",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (orderPartner != null && orderPartner.ownerPhone.isNotBlank() && order != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedButton(
                                        onClick = {
                                            WhatsAppHelper.sendPartnerPaymentAlert(
                                                context = context,
                                                recipientPhone = orderPartner.ownerPhone,
                                                partnerName = orderPartner.businessName,
                                                orderId = order.orderId,
                                                customerName = order.customerName,
                                                customerPhone = order.customerPhone,
                                                productDetails = order.itemsSummary,
                                                grossAmount = order.totalAmount,
                                                commissionPercent = orderPartner.stipulatedCommissionPercent,
                                                commissionProfit = order.partnerCommissionAmount,
                                                partnerNetPayout = order.partnerNetPayout,
                                                paymentMethodLabel = order.paymentMethod.title,
                                                paymentAccount = order.paymentAccount,
                                                paymentRef = order.paymentReference,
                                                storeProfile = storeProfile
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Send Automated Alert Copy via WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ==============================================================
            // TRANSFERRED INVOICE DATA: ORDERED ITEMS BREAKDOWN
            // ==============================================================
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Purchased Items Summary", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(8.dp))

                    val cleanSummary = order?.itemsSummary?.replace(Regex("""\s*\[Sold:[^\]]+\]"""), "")?.trim()
                    Text(
                        text = cleanSummary.takeIf { !it.isNullOrBlank() } ?: "Registered in order archive.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ==============================================================
            // TRANSFERRED INVOICE DATA: FINANCIAL TOTAL BREAKDOWN
            // ==============================================================
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("SUM TOTAL PAID", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            Text("Official Payable Amount", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }
                        Text(
                            text = nairaFormat.format(order?.totalAmount ?: 0.0),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = SuccessGreen
                        )
                    }
                }
            }

            // ==============================================================
            // UPLOADED PAYMENT RECEIPT SLIP PREVIEW
            // ==============================================================
            if (slipModel != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Uploaded Payment Receipt Slip", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SuccessGreen.copy(alpha = 0.15f)
                            ) {
                                Text("TRANSMITTED TO CASHIER", color = SuccessGreen, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.06f))
                                .clickable { viewingSlipZoom = true }
                        ) {
                            AsyncImage(
                                model = slipModel,
                                contentDescription = "Uploaded Receipt Slip",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap image to view full receipt slip",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            val savePdfLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/pdf")
            ) { targetUri ->
                val targetOrder = order
                if (targetUri != null && targetOrder != null) {
                    val saved = PdfInvoiceExporter.writeToUri(context, targetUri, targetOrder, storeProfile)
                    Toast.makeText(
                        context,
                        if (saved) "PDF Invoice saved to device!" else "Could not save PDF invoice",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            if (order != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { PdfInvoiceExporter.sharePdfInvoice(context, order, storeProfile) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("checkout_share_pdf_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { savePdfLauncher.launch("${order.orderId}_Invoice.pdf") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("checkout_save_pdf_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { WhatsAppHelper.sendCustomerOrderReceiptToStore(context, order, storeProfile) },
                    colors = ButtonDefaults.buttonColors(containerColor = OPayGreen, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("checkout_whatsapp_receipt_button")
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send Receipt via WhatsApp", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Action Buttons
            Button(
                onClick = onDone,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("view_orders_status_button")
            ) {
                Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("View All My Orders & Receipts", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onContinueShopping,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("payment_confirmation_continue_shopping_button")
            ) {
                Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Continue Shopping", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    // Modal to zoom uploaded slip with pinch-to-zoom & high resolution
    if (viewingSlipZoom && slipModel != null) {
        ZoomableImageDialog(
            imageUrl = slipModel.toString(),
            title = "Payment Slip • ${order?.orderId ?: ""}".trimEnd(' ', '•'),
            onDismiss = { viewingSlipZoom = false }
        )
    }
}

// ==============================================================
// HELPER PAYMENT UI COMPONENTS
// ==============================================================
@Composable
private fun PaymentOptionCard(
    title: String,
    subtitle: String,
    brandColor: Color,
    icon: ImageVector,
    isSelected: Boolean,
    onSelect: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onSelect)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) brandColor else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) brandColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = brandColor.copy(alpha = 0.2f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = icon, contentDescription = null, tint = brandColor, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                RadioButton(
                    selected = isSelected,
                    onClick = onSelect
                )
            }

            if (isSelected) {
                Spacer(modifier = Modifier.height(10.dp))
                content()
            }
        }
    }
}

@Composable
private fun BankTransferDetailsBox(
    bankName: String,
    accountNumber: String,
    accountName: String,
    brandColor: Color
) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Bank Name", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(bankName, fontWeight = FontWeight.Bold, color = brandColor)
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = brandColor.copy(alpha = 0.15f)
                ) {
                    Text("OFFICIAL MERCH", color = brandColor, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Account Number", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = accountNumber,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Account Number", accountNumber.replace(" ", "")))
                        Toast.makeText(context, "Copied Account Number!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Account Number", tint = brandColor)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text("Beneficiary: $accountName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PosTransferDetailsBox(storeProfile: StoreProfileConfig = StoreProfileConfig()) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("POS Terminal Merchant", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = "Terminal ID: ${storeProfile.posTerminalId.ifBlank { "Ask Store for Terminal ID" }}",
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Location: ${storeProfile.posTerminalName.ifBlank { storeProfile.storeAddress.ifBlank { storeProfile.storeName } }}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("After transfer, attach slip screenshot or enter 6-digit RRN reference.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun UploadPaymentReceiptOrderConfirmationAndTotalStep(
    cartItems: List<com.example.data.model.CartItem>,
    subtotal: Double,
    selectedDeliveryOption: com.example.data.model.DeliveryOption?,
    deliveryFee: Double,
    total: Double,
    fullName: String,
    onFullNameChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    address: String,
    onAddressChange: (String) -> Unit,
    selectedPaymentMethod: PaymentMethod,
    onSelectPaymentMethod: (PaymentMethod) -> Unit,
    senderName: String,
    onSenderNameChange: (String) -> Unit,
    paymentReference: String,
    onPaymentReferenceChange: (String) -> Unit,
    receiptImageUri: Uri?,
    onChooseReceipt: () -> Unit,
    currentCustomer: com.example.data.model.Customer?,
    hasSavedDelivery: Boolean,
    nairaFormat: NumberFormat,
    onSubmitOrder: () -> Unit,
    onAutofill: () -> Unit,
    onSavePermanent: () -> Unit = {},
    isPlacingOrder: Boolean,
    appliedBargainPrice: Double?,
    onBargainPriceApplied: (Double, String, Boolean) -> Unit,
    proposedBargainInput: String,
    onProposedBargainInputChange: (String) -> Unit,
    couponCodeInput: String = "",
    onCouponCodeInputChange: (String) -> Unit = {},
    appliedCouponCode: String = "",
    couponDiscountAmount: Double = 0.0,
    onApplyCoupon: (String) -> Unit = {},
    onClearCoupon: () -> Unit = {},
    loyaltyPointsBalance: Int = 0,
    redeemLoyaltyPoints: Boolean = false,
    onToggleRedeemLoyalty: (Boolean) -> Unit = {},
    loyaltyPointsRedeemed: Int = 0,
    loyaltyDiscountAmount: Double = 0.0,
    selectedDeliveryZone: DeliveryZoneOption? = null,
    savedAddresses: List<SavedAddress> = emptyList(),
    onQuickPickAddress: (SavedAddress) -> Unit = {},
    storeProfile: StoreProfileConfig = StoreProfileConfig(),
    partnerFolder: PartnerOrganization? = null,
    viewModel: MainViewModel
) {
    val effectivePayableTotal = (total - couponDiscountAmount - loyaltyDiscountAmount).coerceAtLeast(0.0)
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 1. SUM PAYABLE AMOUNT & CHECKOUT TOTAL BREAKDOWN
        Text(
            text = "1. Sum Payable Amount & Breakdown",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Products Subtotal (${cartItems.sumOf { it.quantity }} items):", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(nairaFormat.format(subtotal), fontWeight = FontWeight.Bold)
                }
                if (couponDiscountAmount > 0.0 && appliedCouponCode.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Promo Coupon ($appliedCouponCode):", style = MaterialTheme.typography.bodySmall, color = SuccessGreen, fontWeight = FontWeight.Bold)
                        Text("-${nairaFormat.format(couponDiscountAmount)}", fontWeight = FontWeight.Black, color = SuccessGreen)
                    }
                }
                if (loyaltyDiscountAmount > 0.0 && loyaltyPointsRedeemed > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Loyalty Points ($loyaltyPointsRedeemed pts):", style = MaterialTheme.typography.bodySmall, color = AccentGold, fontWeight = FontWeight.Bold)
                        Text("-${nairaFormat.format(loyaltyDiscountAmount)}", fontWeight = FontWeight.Black, color = AccentGold)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Delivery & Zone ETA:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = buildString {
                                append(selectedDeliveryOption?.title ?: "Standard Delivery")
                                if (selectedDeliveryZone != null) {
                                    append(" • ${selectedDeliveryZone.zoneName} (${selectedDeliveryZone.etaLabel})")
                                }
                            },
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = if (deliveryFee == 0.0) "FREE" else nairaFormat.format(deliveryFee),
                        fontWeight = FontWeight.Bold,
                        color = if (deliveryFee == 0.0) SuccessGreen else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (loyaltyPointsBalance > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AccentGold.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, if (redeemLoyaltyPoints) SuccessGreen else AccentGold.copy(alpha = 0.55f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("checkout_loyalty_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Loyalty, contentDescription = null, tint = AccentGold, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Redeem Loyalty Points", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AccentGold)
                            Text(
                                text = "Balance: $loyaltyPointsBalance pts (Value: ${nairaFormat.format(loyaltyPointsBalance * 50.0)})",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = redeemLoyaltyPoints,
                        onCheckedChange = onToggleRedeemLoyalty
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Promo / Coupon Code Input Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
            border = BorderStroke(1.dp, if (appliedCouponCode.isNotBlank()) SuccessGreen else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("checkout_coupon_card")
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalOffer, contentDescription = null, tint = AccentGold, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Promo / Coupon Code", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = couponCodeInput,
                        onValueChange = onCouponCodeInputChange,
                        placeholder = { Text("Enter promo code") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("checkout_coupon_input")
                    )
                    if (appliedCouponCode.isNotBlank()) {
                        OutlinedButton(onClick = onClearCoupon, modifier = Modifier.height(52.dp)) {
                            Text("Remove", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { onApplyCoupon(couponCodeInput) },
                            enabled = couponCodeInput.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("checkout_apply_coupon_button")
                        ) {
                            Text("APPLY", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (appliedCouponCode.isNotBlank() && couponDiscountAmount > 0.0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "✓ Code $appliedCouponCode active — Saved ${nairaFormat.format(couponDiscountAmount)}!",
                        color = SuccessGreen,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ==============================================================
        // CHECKOUT PRICE BARGAIN BOX (FOR HIGH-VALUE ORDERS >= ₦200,000)
        // ==============================================================
        val bargainConfig by viewModel.adminBargainConfig.collectAsState()
        val isEligible = bargainConfig.isActive && (bargainConfig.minCheckoutThreshold <= 0.0 || effectivePayableTotal >= bargainConfig.minCheckoutThreshold)
        
        var localBargainMsg by remember { mutableStateOf("") }
        var localIsCapped by remember { mutableStateOf(false) }
        val context = LocalContext.current

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isEligible) CyberCyan.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            border = BorderStroke(
                width = 1.5.dp,
                color = if (isEligible) CyberCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            ),
            modifier = Modifier.fillMaxWidth().testTag("checkout_bargain_box")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = if (isEligible) CyberCyan.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isEligible) Icons.Default.Sell else Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (isEligible) CyberCyan else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "CHECKOUT PRICE BARGAIN",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isEligible) CyberCyan else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isEligible) "High-value order privilege UNLOCKED" else "Bargain privilege LOCKED (Min ${nairaFormat.format(bargainConfig.minCheckoutThreshold)})",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = if (isEligible) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (!isEligible) {
                    val remainingAmount = (bargainConfig.minCheckoutThreshold - total).coerceAtLeast(0.0)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Unlock the bargain offer by adding the remaining amount (${nairaFormat.format(remainingAmount)}) to unlock the offer!",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDC2626),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Text(
                            text = "Price bargaining is exclusive to orders of ${nairaFormat.format(bargainConfig.minCheckoutThreshold)} and above. Add ${nairaFormat.format(remainingAmount)} more to your cart to unlock custom price bargaining!",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                viewModel.autoUnlockBargainByBoostingQuantity()
                                Toast.makeText(context, "Added remaining quantity to Cart! Price Bargain Offer UNLOCKED!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("checkout_add_quantity_unlock_bargain_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "➕ Add Quantity to Cart to Unlock Bargain Offer (${nairaFormat.format(remainingAmount)})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color.Black
                            )
                        }
                    }
                } else {
                    val defaultDiscountedTotal = (total - bargainConfig.defaultBargainAmount).coerceAtLeast(total * 0.10)
                    Button(
                        onClick = {
                            onProposedBargainInputChange(defaultDiscountedTotal.toLong().toString())
                            val res = viewModel.evaluateBargain(total, defaultDiscountedTotal)
                            localBargainMsg = res.message
                            localIsCapped = res.isCappedAtMax
                            onBargainPriceApplied(res.approvedPrice, res.message, res.isCappedAtMax)
                            Toast.makeText(context, "Applied Admin Default Bargain Offer!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("apply_default_bargain_checkout_button")
                    ) {
                        Icon(Icons.Default.Sell, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "⚡ Apply Default Offer: Save ${nairaFormat.format(bargainConfig.defaultBargainAmount)} (Pay ${nairaFormat.format(defaultDiscountedTotal)})",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Or propose your custom total amount (₦):",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = proposedBargainInput,
                            onValueChange = onProposedBargainInputChange,
                            placeholder = { Text("e.g. 190000") },
                            label = { Text("Choice Amount (₦)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("checkout_bargain_input")
                        )

                        Button(
                            onClick = {
                                val proposed = proposedBargainInput.toDoubleOrNull() ?: 0.0
                                if (proposed <= 0.0) {
                                    Toast.makeText(context, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val res = viewModel.evaluateBargain(total, proposed)
                                localBargainMsg = res.message
                                localIsCapped = res.isCappedAtMax
                                onBargainPriceApplied(res.approvedPrice, res.message, res.isCappedAtMax)
                                Toast.makeText(context, if (res.isCappedAtMax) "Bargain Capped!" else "Bargain Applied!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(54.dp)
                        ) {
                            Text("APPLY", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (localBargainMsg.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (localIsCapped) AccentGold.copy(alpha = 0.12f) else SuccessGreen.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, if (localIsCapped) AccentGold else SuccessGreen)
                        ) {
                            Text(
                                text = localBargainMsg,
                                modifier = Modifier.padding(10.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Updated Summary Card showing Bargained Total
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (appliedBargainPrice != null) "BARGAINED TOTAL" else "CHECKOUT TOTAL",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = if (appliedBargainPrice != null) SuccessGreen else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Final Payable Amount",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        if (appliedBargainPrice != null || couponDiscountAmount > 0.0 || loyaltyDiscountAmount > 0.0) {
                            Text(
                                text = nairaFormat.format(total),
                                style = MaterialTheme.typography.bodySmall,
                                textDecoration = TextDecoration.LineThrough,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        Text(
                            text = nairaFormat.format(appliedBargainPrice ?: effectivePayableTotal),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = if (appliedBargainPrice != null || couponDiscountAmount > 0.0 || loyaltyDiscountAmount > 0.0) SuccessGreen else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. PAYMENT CHANNELS & OFFICIAL ACCOUNTS
        Text(
            text = "2. Official Payment Accounts",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        val partnerCartItems = remember(cartItems) { cartItems.filter { it.product.partnerId.isNotBlank() } }
        val partnerSplitBreakdown = remember(partnerCartItems, subtotal, effectivePayableTotal, appliedBargainPrice, deliveryFee, couponDiscountAmount, loyaltyDiscountAmount) {
            if (partnerCartItems.isEmpty()) null
            else {
                val effMerch = appliedBargainPrice?.let { (it - deliveryFee).coerceAtLeast(0.0) }
                    ?: (subtotal - couponDiscountAmount - loyaltyDiscountAmount).coerceAtLeast(0.0)
                val factor = if (subtotal > 0.0) (effMerch / subtotal).coerceIn(0.0, 1.0) else 1.0
                var gross = 0.0
                var share247 = 0.0
                var partnerNet = 0.0
                partnerCartItems.forEach { item ->
                    val itemGross = (item.unitPrice * item.quantity) * factor
                    val pct = item.product.partnerCommissionPercent.coerceIn(0.0, 95.0)
                    val cut = itemGross * (pct / 100.0)
                    gross += itemGross
                    share247 += cut
                    partnerNet += (itemGross - cut).coerceAtLeast(0.0)
                }
                Triple(gross, share247, partnerNet)
            }
        }
        if (partnerFolder != null || partnerSplitBreakdown != null) {
            val hostStoreAcct = storeProfile.opayAccountNumber.ifBlank { storeProfile.moniepointAccountNumber }.ifBlank { storeProfile.storeName }
            val partnerDestAcct = partnerFolder?.let {
                it.opayAccountNumber.ifBlank { it.moniepointAccountNumber }.ifBlank { it.businessName }
            } ?: (partnerCartItems.firstOrNull()?.product?.partnerName ?: "Partner Account")
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AccentGold.copy(alpha = 0.14f),
                border = BorderStroke(1.2.dp, AccentGold.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .testTag("checkout_partner_mou_split_card")
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "AUTOMATED MOU SPLIT SETTLEMENT (${partnerFolder?.businessName ?: partnerCartItems.firstOrNull()?.product?.partnerName ?: "Partner"})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = AccentGold
                    )
                    if (partnerSplitBreakdown != null) {
                        val (pGross, pShare247, pNet) = partnerSplitBreakdown
                        Text(
                            text = "Partner Items Gross: ${nairaFormat.format(pGross)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "• Auto-Deducted to ${storeProfile.storeName} Account ($hostStoreAcct): ${nairaFormat.format(pShare247)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                        Text(
                            text = "• Remaining Balance to Partner Account ($partnerDestAcct): ${nairaFormat.format(pNet)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan
                        )
                    }
                    Text(
                        text = "Automated payment alerts notify both ${storeProfile.storeName} & the Partner upon payment.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Text(
            text = "Transfer the sum payable amount to any of the accounts below",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        val effectiveOpayNum = partnerFolder?.opayAccountNumber?.takeIf { it.isNotBlank() }
            ?: storeProfile.opayAccountNumber.ifBlank { "Contact Store for Account Number" }
        val effectiveOpayName = partnerFolder?.opayAccountName?.takeIf { it.isNotBlank() }
            ?: storeProfile.opayAccountName.ifBlank { storeProfile.storeName }
        val effectiveMonieNum = partnerFolder?.moniepointAccountNumber?.takeIf { it.isNotBlank() }
            ?: storeProfile.moniepointAccountNumber.ifBlank { "Contact Store for Account Number" }
        val effectiveMonieName = partnerFolder?.moniepointAccountName?.takeIf { it.isNotBlank() }
            ?: storeProfile.moniepointAccountName.ifBlank { storeProfile.storeName }

        PaymentOptionCard(
            title = "OPay Transfer",
            subtitle = if (partnerFolder?.opayAccountNumber?.isNotBlank() == true)
                "Partner Folder OPay Account (${partnerFolder.businessName})"
            else
                "Instant settlement via OPay App / USSD",
            brandColor = OPayGreen,
            icon = Icons.Default.PhoneAndroid,
            isSelected = selectedPaymentMethod == PaymentMethod.OPAY,
            onSelect = { onSelectPaymentMethod(PaymentMethod.OPAY) }
        ) {
            BankTransferDetailsBox(
                bankName = "OPay (Paycom)",
                accountNumber = effectiveOpayNum,
                accountName = effectiveOpayName,
                brandColor = OPayGreen
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        PaymentOptionCard(
            title = "Moniepoint MFB",
            subtitle = if (partnerFolder?.moniepointAccountNumber?.isNotBlank() == true)
                "Partner Folder Moniepoint Account (${partnerFolder.businessName})"
            else
                "Direct bank transfer to Moniepoint Account",
            brandColor = MoniepointBlue,
            icon = Icons.Default.AccountBalance,
            isSelected = selectedPaymentMethod == PaymentMethod.MONIEPOINT,
            onSelect = { onSelectPaymentMethod(PaymentMethod.MONIEPOINT) }
        ) {
            BankTransferDetailsBox(
                bankName = "Moniepoint Microfinance Bank",
                accountNumber = effectiveMonieNum,
                accountName = effectiveMonieName,
                brandColor = MoniepointBlue
            )
        }

        if (storeProfile.extraBankAccountNumber.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            BankTransferDetailsBox(
                bankName = storeProfile.extraBankName.ifBlank { "Additional Bank Transfer" },
                accountNumber = storeProfile.extraBankAccountNumber,
                accountName = storeProfile.extraBankAccountName.ifBlank { storeProfile.storeName },
                brandColor = CyberCyan
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        PaymentOptionCard(
            title = "POS Terminal Transfer",
            subtitle = "Terminal debit swipe or agent slip transfer",
            brandColor = PosPurple,
            icon = Icons.Default.CreditCard,
            isSelected = selectedPaymentMethod == PaymentMethod.POS_TRANSFER,
            onSelect = { onSelectPaymentMethod(PaymentMethod.POS_TRANSFER) }
        ) {
            PosTransferDetailsBox(storeProfile = storeProfile)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3. UPLOAD PAYMENT RECEIPT SLIP / SCREENSHOT
        Text(
            text = "3. Upload Payment Receipt",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Attach photo of payment slip or enter transfer reference",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        var receiptPreviewZoomOpen by remember { mutableStateOf(false) }

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            border = BorderStroke(1.5.dp, if (receiptImageUri != null) SuccessGreen else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (receiptImageUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.05f))
                            .clickable { receiptPreviewZoomOpen = true }
                    ) {
                        AsyncImage(
                            model = ProductImageHelper.buildImageRequest(
                                context = context,
                                imageUrl = receiptImageUri.toString(),
                                title = "Uploaded Receipt Preview",
                                highRes = true
                            ),
                            contentDescription = "Uploaded Receipt Preview (Tap to zoom)",
                            contentScale = ContentScale.Fit,
                            filterQuality = FilterQuality.High,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    if (receiptPreviewZoomOpen) {
                        ZoomableImageDialog(
                            imageUrl = receiptImageUri.toString(),
                            title = "Uploaded Payment Receipt",
                            onDismiss = { receiptPreviewZoomOpen = false }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Payment Receipt Attached & Visible", fontWeight = FontWeight.Bold, color = SuccessGreen, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = onChooseReceipt, shape = RoundedCornerShape(10.dp)) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Change Receipt Photo")
                    }
                } else {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("No receipt selected yet", fontWeight = FontWeight.Bold)
                    Text("Select payment screenshot or camera receipt slip", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onChooseReceipt,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("upload_receipt_picker_button")
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Payment Receipt Image")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = senderName,
                    onValueChange = onSenderNameChange,
                    label = { Text("Sender / Account Holder Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("receipt_sender_name_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = paymentReference,
                    onValueChange = onPaymentReferenceChange,
                    label = { Text("Payment Session ID / STAN / Reference") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("receipt_reference_input")
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4. CUSTOMER DELIVERY INFORMATION
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "4. Customer Delivery Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (fullName.isNotBlank() || phone.isNotBlank() || address.isNotBlank())
                        "✓ Permanently saved across all future orders"
                    else
                        "Enter details once — saved permanently for future orders",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (fullName.isNotBlank() || phone.isNotBlank() || address.isNotBlank()) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (hasSavedDelivery) {
                    AssistChip(
                        onClick = onAutofill,
                        label = { Text("Refresh", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }
                if (fullName.isNotBlank() || phone.isNotBlank() || address.isNotBlank()) {
                    AssistChip(
                        onClick = onSavePermanent,
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Save permanently",
                                modifier = Modifier.size(14.dp),
                                tint = SuccessGreen
                            )
                        },
                        label = { Text("Save Info", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SuccessGreen) },
                        modifier = Modifier.testTag("save_customer_info_chip")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (savedAddresses.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                savedAddresses.forEach { addr ->
                    FilterChip(
                        selected = address.equals(addr.streetAddress, ignoreCase = true),
                        onClick = { onQuickPickAddress(addr) },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        label = { Text("${addr.label} • ${addr.stateZone}", fontSize = 11.sp) }
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = onFullNameChange,
                    label = { Text("Full Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("checkout_name_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    label = { Text("Phone Number") },
                    placeholder = { Text("e.g. +234 800 000 0000") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("checkout_phone_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = onAddressChange,
                    label = { Text("Complete Delivery Address") },
                    placeholder = { Text("Street number, Area, Landmark, City/State") },
                    leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("checkout_address_input")
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 5. ORDERED ITEMS SUMMARY
        Text(
            text = "5. Ordered Items (${cartItems.sumOf { it.quantity }} items)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (cartItems.isEmpty()) {
                    Text("No items in cart.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    cartItems.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.05f))
                            ) {
                                AsyncImage(
                                    model = ProductImageHelper.buildImageRequest(
                                        context = LocalContext.current,
                                        imageUrl = item.product.imageUrl,
                                        category = item.product.category,
                                        title = item.product.title
                                    ),
                                    contentDescription = item.product.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.product.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                                Text("${item.quantity}x • ${item.selectedBrand} • ${item.selectedSize} • ${item.selectedColor.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            }
                            Text(nairaFormat.format(item.unitPrice * item.quantity), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        if (index < cartItems.size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 6. ACTION BUTTON: SEND ORDERS AND PAYMENT RECEIPT
        Button(
            onClick = {
                if (!isPlacingOrder) {
                    onSubmitOrder()
                }
            },
            enabled = !isPlacingOrder,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("send_orders_and_receipt_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isPlacingOrder) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary
            )
        ) {
            if (isPlacingOrder) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = "SENDING ORDER...", fontWeight = FontWeight.Black)
            } else {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Send Orders & Payment Receipt (${nairaFormat.format(appliedBargainPrice ?: effectivePayableTotal)})",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))
    }
}
