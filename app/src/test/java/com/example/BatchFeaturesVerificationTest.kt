package com.example

import android.app.Application
import android.net.Uri
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CreditVerificationStatus
import com.example.data.model.OfflineActionType
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.ProductColor
import com.example.data.model.ProductVariantPrice
import com.example.data.remote.LiveCallMode
import com.example.data.remote.LiveCallPhase
import com.example.data.repository.CatalogDataSeeder
import com.example.util.PdfInvoiceExporter
import com.example.util.ProductImageHelper
import com.example.util.SharedReceiptHelper
import com.example.util.WordInvoiceExporter
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BatchFeaturesVerificationTest {

    private lateinit var app: Application
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        app.getSharedPreferences("depros_admin_settings", android.content.Context.MODE_PRIVATE)
            .edit().clear().commit()
        val cm = app.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        val activeNet = cm?.activeNetwork
        if (cm != null && activeNet != null) {
            val caps = org.robolectric.shadows.ShadowNetworkCapabilities.newInstance()
            Shadows.shadowOf(caps).addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
            Shadows.shadowOf(cm).setNetworkCapabilities(activeNet, caps)
        }
        if (FirebaseApp.getApps(app).isEmpty()) {
            FirebaseApp.initializeApp(
                app,
                FirebaseOptions.Builder()
                    .setApplicationId("1:1234567890:android:abcdef123456")
                    .setProjectId("test-project")
                    .setApiKey("fake-api-key")
                    .build()
            )
        }
        viewModel = MainViewModel(app)
        pumpLooper()
    }

    @org.junit.After
    fun tearDown() {
        try {
            viewModel.endLiveCall()
        } catch (_: Exception) {
        }
        com.example.data.local.DeprosDatabase.resetInstanceForTests(app)
    }

    private fun pumpLooper() = runBlocking {
        repeat(8) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            delay(40)
        }
    }

    @Test
    fun expanded23Categories_allHaveSeededCatalogAndBundledImages() {
        val expectedNewCategories = listOf(
            "Men", "Women", "Babies", "Kids", "Household", "Musicals",
            "Educational", "Jewelry", "Stationaries", "Phone Accessories",
            "Upholstery", "Rubber/Plastic", "Automobile Spare-Parts",
            "Clothing & Sewing Materials", "Health/Medical Equipment", "Pharmaceuticals"
        )
        assertEquals(23, ProductCategory.entries.size)
        val displayNames = ProductCategory.entries.map { it.displayName }
        expectedNewCategories.forEach { cat ->
            assertTrue("Missing category $cat", displayNames.contains(cat))
            assertNotEquals("Missing drawable for $cat", 0, ProductImageHelper.getFallbackDrawableRes(cat))
        }

        val catalog = CatalogDataSeeder.generate100Products()
        expectedNewCategories.forEach { cat ->
            assertTrue(
                "Expected seeded products for $cat",
                catalog.any { it.category.equals(cat, ignoreCase = true) }
            )
        }
    }

    @Test
    fun customerCanChangeForgottenPasswordByPhoneNumber() = runBlocking {
        var signedUp = false
        viewModel.signUp(
            name = "Chidi Nwosu",
            pass = "OldPass123",
            phone = "+2348011223344",
            address = "12 Broad St, Lagos",
            onSuccess = { signedUp = true },
            onError = {}
        )
        pumpLooper()
        assertTrue(signedUp)

        var resetOk = false
        viewModel.resetCustomerPasswordByPhone(
            phone = "+2348011223344",
            fullName = "Chidi Nwosu",
            newPassword = "NewPass456"
        ) { ok, _ -> resetOk = ok }
        pumpLooper()
        assertTrue(resetOk)

        // Old password should now fail; new password should succeed
        var oldPassOk = true
        viewModel.signIn("+2348011223344", "OldPass123", onSuccess = { oldPassOk = true }, onError = { oldPassOk = false })
        pumpLooper()
        assertFalse(oldPassOk)

        var newPassOk = false
        viewModel.signIn("+2348011223344", "NewPass456", onSuccess = { newPassOk = true }, onError = {})
        pumpLooper()
        assertTrue(newPassOk)
    }

    @Test
    fun palmPayPortalDetectedAndWordPdfReceiptsInclude247ShoppingRecipient() {
        val text = """
            PalmPay Official Receipt
            Amount: ₦35,500.00
            Sender Name: Fatima Bello
            Session ID: PALM-88221100
        """.trimIndent()
        val parsed = SharedReceiptHelper.parseReceiptShareText(text, "PalmPay")
        assertEquals(PaymentMethod.PALMPAY, parsed.detectedPaymentMethod)
        assertEquals("PALM-88221100", parsed.extractedReference)
        assertEquals("Fatima Bello", parsed.extractedSender)

        val order = Order(
            orderId = "ORD-247-PALM",
            customerId = "c-1",
            customerName = "Fatima Bello",
            customerEmail = "",
            customerPhone = "+2348099887766",
            deliveryAddress = "Wuse II, Abuja",
            itemsSummary = "1x Luxury Gold Analog Watch",
            totalAmount = 35500.0,
            paymentMethod = PaymentMethod.PALMPAY,
            paymentAccount = "PalmPay: 8012345678 (24/7 SHOPPING)",
            paymentReference = "PALM-88221100",
            senderName = "Fatima Bello",
            status = OrderStatus.CONFIRMED,
            isCreditAlertConfirmed = true,
            creditVerificationStatus = CreditVerificationStatus.CONFIRMED_CREDITED
        )

        val wordFile = File(app.cacheDir, "test_receipt.doc")
        val wordOk = WordInvoiceExporter.writeToUri(app, Uri.fromFile(wordFile), order, viewModel.storeProfile.value)
        assertTrue(wordOk)
        val wordHtml = wordFile.readText()
        assertTrue(wordHtml.contains("24/7 SHOPPING OFFICIAL RECIPIENT"))
        assertTrue(wordHtml.contains("ORD-247-PALM"))

        val pdfFile = File(app.cacheDir, "test_receipt.pdf")
        val pdfOk = PdfInvoiceExporter.writeToUri(app, Uri.fromFile(pdfFile), order, viewModel.storeProfile.value)
        assertTrue(pdfOk)
        assertTrue(pdfFile.length() > 100L)
    }

    @Test
    fun directPortalPaymentHandlesConfirmedFailedAndNetworkDelayedAutoRecovery() = runBlocking {
        var prod = viewModel.allProducts.value.firstOrNull()
        repeat(15) {
            if (prod != null) return@repeat
            pumpLooper()
            prod = viewModel.allProducts.value.firstOrNull()
        }
        val item = prod ?: error("Catalog required")

        // 1) Instant Credit Alert Confirmed -> Admin Payment Confirmation automatically ON
        viewModel.addToCart(item)
        var confirmedOrder: Order? = null
        viewModel.processDirectPortalPayment(
            customerName = "Kelechi Eze",
            customerEmail = "",
            customerPhone = "+2348033445566",
            deliveryAddress = "Enugu",
            paymentMethod = PaymentMethod.PALMPAY,
            portalAccountNumber = "8012345678",
            portalAccountName = "24/7 SHOPPING",
            senderName = "Kelechi Eze",
            deliveryConditionTitle = "Standard",
            deliveryFeeAmount = 1000.0,
            simulateCreditAlertReceived = true,
            simulateNetworkDelay = false,
            onSuccess = { confirmedOrder = it },
            onFailure = {}
        )
        pumpLooper()
        assertNotNull(confirmedOrder)
        assertTrue(confirmedOrder!!.isCreditAlertConfirmed)
        assertEquals(CreditVerificationStatus.CONFIRMED_CREDITED, confirmedOrder!!.creditVerificationStatus)

        // 2) Uncredited Payment -> ORDER & PAYMENT FAILED until 24/7 receives credit alert
        viewModel.addToCart(item)
        var failedOrder: Order? = null
        viewModel.processDirectPortalPayment(
            customerName = "Kelechi Eze",
            customerEmail = "",
            customerPhone = "+2348033445566",
            deliveryAddress = "Enugu",
            paymentMethod = PaymentMethod.OPAY,
            portalAccountNumber = "9012345678",
            portalAccountName = "24/7 SHOPPING",
            senderName = "Kelechi Eze",
            deliveryConditionTitle = "Standard",
            deliveryFeeAmount = 1000.0,
            simulateCreditAlertReceived = false,
            simulateNetworkDelay = false,
            onSuccess = { failedOrder = it },
            onFailure = {}
        )
        pumpLooper()
        assertNotNull(failedOrder)
        assertFalse(failedOrder!!.isCreditAlertConfirmed)
        assertEquals(CreditVerificationStatus.PAYMENT_FAILED_UNCREDITED, failedOrder!!.creditVerificationStatus)

        // When 24/7 SHOPPING receives credit alert -> Admin button turns ON & receipt confirmed
        viewModel.recordOrderCreditAlertReceived(failedOrder!!.orderId)
        pumpLooper()
        val recoveredOrder = viewModel.allOrders.value.first { it.orderId == failedOrder!!.orderId }
        assertTrue(recoveredOrder.isCreditAlertConfirmed)
        assertEquals(CreditVerificationStatus.CONFIRMED_CREDITED, recoveredOrder.creditVerificationStatus)

        // 3) Network Delay -> Queued & Auto-Confirmed when Network Restores (flushOfflineQueue)
        viewModel.addToCart(item)
        var delayedOrder: Order? = null
        viewModel.processDirectPortalPayment(
            customerName = "Kelechi Eze",
            customerEmail = "",
            customerPhone = "+2348033445566",
            deliveryAddress = "Enugu",
            paymentMethod = PaymentMethod.MONIEPOINT,
            portalAccountNumber = "5012345678",
            portalAccountName = "24/7 SHOPPING",
            senderName = "Kelechi Eze",
            deliveryConditionTitle = "Standard",
            deliveryFeeAmount = 1000.0,
            simulateCreditAlertReceived = true,
            simulateNetworkDelay = true,
            onSuccess = { delayedOrder = it },
            onFailure = {}
        )
        pumpLooper()
        assertNotNull(delayedOrder)
        assertFalse(delayedOrder!!.isCreditAlertConfirmed)
        assertEquals(CreditVerificationStatus.NETWORK_DELAYED_PENDING, delayedOrder!!.creditVerificationStatus)
        assertTrue(viewModel.offlineQueue.value.any { it.type == OfflineActionType.PAYMENT_CREDIT_SYNC && it.referenceId == delayedOrder!!.orderId })

        // Simulate network restoration flushing queue
        var flushed = 0
        viewModel.triggerOfflineQueueSync { flushed = it }
        pumpLooper()
        assertTrue(flushed >= 1)
        val syncedOrder = viewModel.allOrders.value.first { it.orderId == delayedOrder!!.orderId }
        assertTrue(syncedOrder.isCreditAlertConfirmed)
        assertEquals(CreditVerificationStatus.CONFIRMED_CREDITED, syncedOrder.creditVerificationStatus)
    }

    @Test
    fun adminReceivesCustomerReviewsAndReplies_maintenanceAndPreOrderAndAudioVisualInteractiveWork() = runBlocking {
        var prod = viewModel.allProducts.value.firstOrNull()
        repeat(15) {
            if (prod != null) return@repeat
            pumpLooper()
            prod = viewModel.allProducts.value.firstOrNull()
        }
        val item = prod ?: error("Catalog required")

        // Pre-Order submission stored for Admin
        val submittedPreOrder = viewModel.submitPreOrderRequest(
            productName = "Custom Medical Stethoscope",
            brandName = "Littmann",
            category = "Health/Medical Equipment",
            customerNameOverride = "Dr. Bello",
            customerPhoneOverride = "+2348055443322"
        )
        assertTrue(submittedPreOrder)
        assertTrue(viewModel.preOrderRequests.value.any { it.productName == "Custom Medical Stethoscope" })

        // Maintenance & Upgrade Notice propagates to StoreProfileConfig
        viewModel.publishAppMaintenanceNotice(
            enabled = true,
            message = "Upgrading 24/7 SHOPPING checkout nodes",
            isUpgradeNotice = true,
            versionName = "2.5"
        )
        pumpLooper()
        assertTrue(viewModel.storeProfile.value.isMaintenanceMode)
        assertEquals("Upgrading 24/7 SHOPPING checkout nodes", viewModel.storeProfile.value.maintenanceMessage)

        // Signed-up phone line new product alert
        viewModel.persistDeliveryInfoLocally(phoneNumber = "+2348077665544")
        assertTrue(viewModel.getSignedUpCustomerPhones().contains("+2348077665544"))
        viewModel.broadcastNewProductAlert(item)

        // Delivered order review -> Admin Reply
        viewModel.repository.createOrder(
            Order(
                orderId = "ORD-REV-ADMIN",
                customerId = "cust-rev",
                customerName = "Dr. Bello",
                customerEmail = "",
                customerPhone = "+2348055443322",
                deliveryAddress = "Abuja",
                itemsSummary = "1x ${item.title}",
                totalAmount = item.basePrice,
                paymentMethod = PaymentMethod.OPAY,
                paymentAccount = "OPay",
                paymentReference = "REF-REV",
                senderName = "Dr. Bello",
                status = OrderStatus.DELIVERED
            )
        )
        repeat(12) {
            if (viewModel.allOrders.value.any { it.orderId == "ORD-REV-ADMIN" }) return@repeat
            pumpLooper()
        }
        var reviewOk = false
        viewModel.submitProductReview(item.id, 5, "Top class delivery!") { ok, _ -> reviewOk = ok }
        pumpLooper()
        assertTrue(reviewOk)
        val createdReview = viewModel.productReviews.value.first { it.productId == item.id }
        viewModel.replyToProductReview(createdReview.id, "Thank you Dr. Bello!")
        assertEquals("Thank you Dr. Bello!", viewModel.productReviews.value.first { it.id == createdReview.id }.adminReply)

        // Live Audio/Visual Interactive Feature activation
        viewModel.startCustomerLiveCall(LiveCallMode.AUDIO, "Check Order Status")
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(2000L, java.util.concurrent.TimeUnit.MILLISECONDS)
        pumpLooper()
        assertEquals(LiveCallPhase.CONNECTED, viewModel.liveCallState.value.phase)
        viewModel.sendInteractiveAudioVisualPrompt("Store Bank Accounts")
        pumpLooper()
        assertTrue(viewModel.liveCallState.value.chatMessages.any { it.text.contains("Store Bank Accounts") })
        assertTrue(viewModel.liveCallState.value.chatMessages.any { it.isFromAdmin && it.text.contains("official accounts") })
        viewModel.endLiveCall()
        pumpLooper()
    }

    @Test
    fun brandSizeAmountColourPricing_variesBySizeAndColorAndNormalizesShoeTokens() {
        val variants = listOf(
            ProductVariantPrice(brand = "Brand A", size = "42", colorName = "Black", amount = 50.0, colorHex = "#111827"),
            ProductVariantPrice(brand = "Brand A", size = "43", colorName = "Black", amount = 60.0, colorHex = "#111827"),
            ProductVariantPrice(brand = "Brand A", size = "42", colorName = "Red", amount = 75.0, colorHex = "#DC2626"),
            ProductVariantPrice(brand = "Brand B", size = "42", colorName = "Black", amount = 90.0, colorHex = "#111827")
        )
        val product = Product(
            id = "variant-test-shoe",
            title = "Classic Runner Pro",
            category = "Shoes",
            brand = "Brand A",
            availableBrands = listOf("Brand A", "Brand B"),
            availableSizes = listOf("42", "43"),
            availableColors = listOf(ProductColor("Black", "#111827"), ProductColor("Red", "#DC2626")),
            basePrice = 50.0,
            stockQuantity = 20,
            imageUrl = "",
            description = "Variant matrix test product",
            variantPrices = variants
        )

        assertEquals(50.0, product.calculatePrice("Brand A", "42", selectedColor = "Black"), 0.01)
        assertEquals(60.0, product.calculatePrice("Brand A", "43", selectedColor = "Black"), 0.01)
        assertEquals(75.0, product.calculatePrice("Brand A", "42", selectedColor = "Red"), 0.01)
        assertEquals(90.0, product.calculatePrice("Brand B", "42", selectedColor = "Black"), 0.01)
        // Normalized shoe token ("EU 42" -> "42")
        assertEquals(50.0, product.calculatePrice("Brand A", "EU 42", selectedColor = "Black"), 0.01)

        // JSON encode/decode round-trip
        val json = ProductVariantPrice.encodeListToJson(variants)
        val decoded = ProductVariantPrice.decodeListFromJson(json)
        assertEquals(4, decoded.size)
        assertEquals(60.0, decoded[1].amount, 0.01)
        assertEquals("Red", decoded[2].colorName)
    }

    @Test
    fun brandSizeAmountColourProduct_persistsInRoomV8AndUpdatesDetailAndCartPrice() = runBlocking {
        val variants = listOf(
            ProductVariantPrice(brand = "Brand A", size = "42", colorName = "Black", amount = 50.0, colorHex = "#111827"),
            ProductVariantPrice(brand = "Brand A", size = "43", colorName = "Black", amount = 60.0, colorHex = "#111827"),
            ProductVariantPrice(brand = "Brand A", size = "42", colorName = "Red", amount = 75.0, colorHex = "#DC2626")
        )
        val customProduct = Product(
            id = "room-variant-item-1",
            title = "Heritage Oxford Shoe",
            category = "Shoes",
            brand = "Brand A",
            availableBrands = listOf("Brand A"),
            availableSizes = listOf("42", "43"),
            availableColors = listOf(ProductColor("Black", "#111827"), ProductColor("Red", "#DC2626")),
            basePrice = 50.0,
            stockQuantity = 15,
            imageUrl = "",
            description = "Admin entered variant product",
            variantPrices = variants
        )

        viewModel.repository.saveProduct(customProduct)
        val reloaded = viewModel.repository.getProductById("room-variant-item-1")
        assertNotNull(reloaded)
        assertEquals(3, reloaded!!.variantPrices.size)

        viewModel.openProductDetails(reloaded)
        assertEquals("Brand A", viewModel.detailBrand.value)
        assertEquals("42", viewModel.detailSize.value)
        assertEquals("Black", viewModel.detailColor.value.name)

        // Select Size 43 Black -> 60 Naira in Cart
        viewModel.setDetailSize("43")
        viewModel.addToCart(
            product = reloaded,
            brand = viewModel.detailBrand.value,
            size = viewModel.detailSize.value,
            color = viewModel.detailColor.value
        )
        val size43Item = viewModel.cartItems.value.first { it.product.id == reloaded.id && it.selectedSize == "43" }
        assertEquals(60.0, size43Item.unitPrice, 0.01)

        // Select Size 42 Red -> 75 Naira in Cart
        viewModel.setDetailSize("42")
        viewModel.setDetailColor(ProductColor("Red", "#DC2626"))
        viewModel.addToCart(
            product = reloaded,
            brand = viewModel.detailBrand.value,
            size = viewModel.detailSize.value,
            color = viewModel.detailColor.value
        )
        val redItem = viewModel.cartItems.value.first { it.product.id == reloaded.id && it.selectedSize == "42" && it.selectedColor.name == "Red" }
        assertEquals(75.0, redItem.unitPrice, 0.01)
    }
}
