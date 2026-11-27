package com.example

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.DeprosDatabase
import com.example.data.local.ProductEntity
import com.example.data.model.HostingPeriodUnit
import com.example.data.model.PartnerAgreementStatus
import com.example.data.model.PartnershipInterestStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.ProductColor
import com.example.data.remote.LiveCallMode
import com.example.data.remote.LiveCallPhase
import com.example.ui.screens.AdminControlRoomScreen
import com.example.ui.screens.CartScreen
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.CustomerPartnershipPage
import com.example.ui.screens.HelpSupportScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LiveCallScreen
import com.example.ui.screens.PartnerPortalScreen
import com.example.ui.screens.ProductDetailScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.PartnerDocumentExporter
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w480dp-h2400dp")
class FeatureUpgradeVerificationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var app: Application
    private lateinit var viewModel: MainViewModel
    private lateinit var dao: com.example.data.local.DeprosDao

    @Before
    fun setUp() = runBlocking {
        app = ApplicationProvider.getApplicationContext()
        app.getSharedPreferences("smartshop_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        app.getSharedPreferences("depros_admin_settings", Context.MODE_PRIVATE).edit().clear().commit()
        dao = DeprosDatabase.getDatabase(app).dao()
        viewModel = MainViewModel(app)
        pumpLooper()
    }

    @After
    fun tearDown() {
        try {
            viewModel.endLiveCall()
        } catch (_: Exception) {}
        try {
            viewModel.viewModelScope.cancel()
        } catch (_: Exception) {}
        DeprosDatabase.resetInstanceForTests(app)
    }

    private suspend fun pumpLooper(cycles: Int = 8) {
        repeat(cycles) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            delay(40)
        }
    }

    @Test
    fun liveChatMode_connectsWithoutMicPermissionAndArchivesChatHistory() = runBlocking {
        viewModel.saveCustomerDeliveryInfo("Adaeze Okafor", "+2348091112233", "10 Admiralty Way, Lekki")
        pumpLooper(4)

        viewModel.startCustomerLiveCall(
            mode = LiveCallMode.CHAT,
            topic = "Order & Payment Assistance"
        )
        pumpLooper(4)

        val stateAfterStart = viewModel.liveCallState.value
        assertEquals(LiveCallMode.CHAT, stateAfterStart.mode)
        assertEquals(LiveCallPhase.CONNECTED, stateAfterStart.phase)
        assertTrue(stateAfterStart.chatMessages.isNotEmpty())

        viewModel.sendLiveCallChatMessage("Hello ODIYA Admin, please confirm my Moniepoint transfer.")
        pumpLooper(4)

        assertTrue(
            viewModel.liveCallState.value.chatMessages.any {
                it.text.contains("Moniepoint transfer") && !it.isFromAdmin
            }
        )
        assertTrue(
            viewModel.customerChatHistory.value.any {
                it.text.contains("Moniepoint transfer")
            }
        )

        viewModel.openAdminLiveChatSession(customerName = "Adaeze Okafor", customerPhone = "+2348091112233")
        pumpLooper(4)
        val adminChatState = viewModel.liveCallState.value
        assertEquals(LiveCallMode.CHAT, adminChatState.mode)
        assertTrue(adminChatState.isAdminRole)
        assertEquals("Adaeze Okafor", adminChatState.peerName)
    }

    @Test
    fun partnershipInterestToMainMouWithNextOfKinAndPdfExport() = runBlocking {
        var signedUp = false
        viewModel.signUp(
            name = "Zainab Musa",
            pass = "secret123",
            phone = "+2348034445566",
            address = "15 Ahmadu Bello Way, Kaduna",
            onSuccess = { signedUp = true },
            onError = {}
        )
        repeat(15) {
            if (signedUp) return@repeat
            pumpLooper(4)
        }
        assertTrue(signedUp)

        viewModel.notifyPartnershipInterestClicked()
        var reqSubmitted = false
        viewModel.submitPartnershipInterestRequest(
            customerName = "Zainab Musa",
            customerPhone = "+2348034445566",
            customerEmail = "zainab@arewacraft.ng",
            businessName = "Arewa Craft Collections",
            officeAddress = "Shop 8 Ali Akilu Plaza, Kaduna",
            productCategories = listOf("Clothes", "Processed Foods/Pastries"),
            note = "Ready for weekly dispatch",
            onComplete = { ok, _ -> reqSubmitted = ok }
        )
        pumpLooper(6)
        assertTrue(reqSubmitted)

        val request = viewModel.partnershipInterestRequests.value.firstOrNull {
            it.businessName == "Arewa Craft Collections"
        }
        assertNotNull(request)
        assertEquals(PartnershipInterestStatus.PENDING, request!!.status)

        viewModel.replyPartnershipInterest(
            requestId = request.id,
            accept = true,
            adminReply = "Welcome onboard! Kindly sign the MOU and supply Next of Kin details."
        )
        pumpLooper(4)

        val accepted = viewModel.partnershipInterestRequests.value.first { it.id == request.id }
        assertEquals(PartnershipInterestStatus.ACCEPTED, accepted.status)

        viewModel.openAcceptedPartnershipForm(accepted)
        pumpLooper(2)
        assertEquals(accepted.id, viewModel.pendingPartnershipDraft.value?.id)
        assertEquals(AppScreen.PARTNER_PORTAL, viewModel.currentScreen.value)

        var createdPartnerId = ""
        viewModel.submitPartnerAgreementApplication(
            businessName = accepted.businessName,
            ownerName = accepted.customerName,
            ownerPhone = accepted.customerPhone,
            businessAddress = accepted.officeAddress,
            primaryCategory = accepted.productCategories.first(),
            opayAccountNumber = "8034445566",
            opayAccountName = "Arewa Craft Collections",
            moniepointAccountNumber = "9034445566",
            moniepointAccountName = "Arewa Craft Collections",
            agreementDate = "26 Nov 2026",
            hostingDuration = "6 Months",
            agreedQuantityPerPeriod = 25,
            periodUnit = HostingPeriodUnit.PER_MONTH,
            monthlyHostingPayment = 20000.0,
            stipulatedCommissionPercent = 15.0,
            partnerSignatureName = "Zainab Musa",
            partnerSignatureStrokes = "0.1,0.1;0.7,0.8",
            nextOfKinName = "Sadiq Musa",
            nextOfKinGender = "Male",
            nextOfKinPhone = "08022334455, 08099887766",
            customerId = viewModel.currentCustomer.value?.id.orEmpty()
        ) { created ->
            createdPartnerId = created.id
        }
        pumpLooper(6)
        assertTrue(createdPartnerId.isNotBlank())

        var finalized = false
        viewModel.finalizePartnerAgreement(
            partnerId = createdPartnerId,
            adminSignatoryName = "ODIYA Admin",
            adminStrokes = "0.2,0.2;0.8,0.7",
            commissionPercent = 15.0,
            agreedQtyPerPeriod = 25,
            periodUnit = HostingPeriodUnit.PER_MONTH,
            monthlyHostingPayment = 20000.0,
            hostingDuration = "6 Months",
            opayNumber = "8034445566",
            opayName = "Arewa Craft Collections",
            moniepointNumber = "9034445566",
            moniepointName = "Arewa Craft Collections"
        ) { ok, _ -> finalized = ok }
        pumpLooper(6)
        assertTrue(finalized)

        val partnerOrg = viewModel.partnerOrganizations.value.first { it.id == createdPartnerId }
        assertEquals(PartnerAgreementStatus.FINALIZED, partnerOrg.status)
        assertEquals("Sadiq Musa", partnerOrg.nextOfKinName)
        assertEquals("Male", partnerOrg.nextOfKinGender)
        assertEquals("08022334455, 08099887766", partnerOrg.nextOfKinPhone)

        val pdfStream = ByteArrayOutputStream()
        PartnerDocumentExporter.writePartnerMouPdf(
            outputStream = pdfStream,
            partner = partnerOrg,
            storeProfile = viewModel.storeProfile.value
        )
        val pdfBytes = pdfStream.toByteArray()
        val pdfText = String(pdfBytes)
        assertTrue(pdfBytes.size > 300)
        assertTrue(pdfText.startsWith("%PDF"))
        assertTrue(pdfText.contains("Sadiq Musa"))
    }

    @Test
    fun directPortalPaymentAndForwardedWordPdfReceiptLogsPersist() = runBlocking {
        val productEntity = ProductEntity(
            id = "portal-prod-1",
            title = "Fresh Strawberries Box",
            category = "Fruits/Vegetables",
            brand = "ODIYA Farms",
            availableBrands = "ODIYA Farms",
            availableSizes = "1Kg",
            availableColorsJson = "Red|#DC2626",
            basePrice = 12500.0,
            discountPercent = 0,
            stockQuantity = 10,
            imageUrl = "",
            model3dType = "FOOD",
            description = "Fresh farm strawberries",
            rating = 4.9,
            reviewCount = 12,
            sku = "SKU-FRUIT-1",
            costPrice = 8000.0,
            expiryMonthsRemaining = 2,
            createdAt = System.currentTimeMillis()
        )
        dao.insertProduct(productEntity)
        pumpLooper(6)

        val product = viewModel.allProducts.value.first { it.id == "portal-prod-1" }
        var added = false
        viewModel.addToCart(product, brand = "ODIYA Farms", size = "1Kg", color = ProductColor("Red", "#DC2626"), quantity = 1) { ok, _ ->
            added = ok
        }
        pumpLooper(4)
        assertTrue(added)

        var placedOrder: com.example.data.model.Order? = null
        viewModel.processDirectPortalPayment(
            customerName = "Chika Eze",
            customerEmail = "chika@example.com",
            customerPhone = "08088776655",
            deliveryAddress = "22 Awolowo Rd, Ikoyi",
            paymentMethod = PaymentMethod.OPAY,
            portalAccountNumber = "8100001122",
            portalAccountName = "ODIYA STORES",
            senderName = "Chika Eze",
            deliveryConditionTitle = "Express Delivery",
            deliveryFeeAmount = 1500.0,
            simulateCreditAlertReceived = true,
            simulateNetworkDelay = false,
            onSuccess = { placedOrder = it },
            onFailure = {}
        )
        pumpLooper(8)

        assertNotNull(placedOrder)
        assertEquals(PaymentMethod.OPAY, placedOrder!!.paymentMethod)

        viewModel.recordForwardedReceiptToOdiya(placedOrder!!, "WORD")
        viewModel.recordForwardedReceiptToOdiya(placedOrder!!, "PDF")
        pumpLooper(4)

        val logs = viewModel.forwardedReceipts.value
        assertEquals(2, logs.count { it.orderId == placedOrder!!.orderId })
        assertTrue(logs.any { it.orderId == placedOrder!!.orderId && it.format == "WORD" })
        assertTrue(logs.any { it.orderId == placedOrder!!.orderId && it.format == "PDF" })
    }

    @Test
    fun uiSmoke_liveChatStage_customerPartnershipPage_adminDropdown_andNextOfKinInputs() {
        runBlocking {
            viewModel.startCustomerLiveCall(LiveCallMode.CHAT, "General Inquiry")
            pumpLooper(4)
        }
        composeRule.setContent {
            MyApplicationTheme {
                LiveCallScreen(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("live_chat_stage_container").assertIsDisplayed()
        composeRule.onNodeWithTag("live_chat_message_input").assertIsDisplayed()
        composeRule.onNodeWithTag("live_chat_send_button").assertIsDisplayed()
    }

    @Test
    fun uiSmoke_customerPartnershipAmInterestedRevealsForm() {
        composeRule.setContent {
            MyApplicationTheme {
                CustomerPartnershipPage(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("customer_partnership_interested_button").assertIsDisplayed()
        composeRule.onNodeWithTag("customer_partnership_interested_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("partnership_interest_send_button").assertIsDisplayed()
    }

    @Test
    fun uiSmoke_partnerPortalDisplaysNextOfKinFields() {
        composeRule.setContent {
            MyApplicationTheme {
                PartnerPortalScreen(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("partner_nok_name_input").assertExists()
        composeRule.onNodeWithTag("partner_nok_phone_input").assertExists()
    }

    @Test
    fun uiSmoke_adminControlRoomDisplaysAlphabeticalCustomerDropdown() {
        runBlocking {
            assertTrue(viewModel.verifyAdminPassword(viewModel.adminPassword.value))
            pumpLooper(4)
        }
        composeRule.setContent {
            MyApplicationTheme {
                AdminControlRoomScreen(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Customer Order Files", substring = true).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("admin_customer_alphabetical_dropdown").assertExists()
    }

    @Test
    fun uiSmoke_homeScreenRemovesPartnershipFormNoticeFromAppfront() {
        composeRule.setContent {
            MyApplicationTheme {
                HomeScreen(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_partner_portal_banner").assertDoesNotExist()
        composeRule.onNodeWithText("Sign MOU & host categorized products", substring = true).assertDoesNotExist()
    }

    @Test
    fun uiSmoke_adminCounterSignaturePadActivatesAfterPartnerSignsAndSubmits() {
        var createdPartnerId = ""
        runBlocking {
            assertTrue(viewModel.verifyAdminPassword(viewModel.adminPassword.value))
            viewModel.submitPartnerAgreementApplication(
                businessName = "Zaria Silk Atelier",
                ownerName = "Amina Bello",
                ownerPhone = "08031112222",
                businessAddress = "Plot 9, Ahmadu Bello Way",
                primaryCategory = "Clothes",
                opayAccountNumber = "8031112222",
                opayAccountName = "Zaria Silk Atelier",
                moniepointAccountNumber = "9031112222",
                moniepointAccountName = "Zaria Silk Atelier",
                agreementDate = "26 Nov 2026",
                hostingDuration = "6 Months",
                agreedQuantityPerPeriod = 20,
                periodUnit = HostingPeriodUnit.PER_MONTH,
                monthlyHostingPayment = 15000.0,
                stipulatedCommissionPercent = 10.0,
                partnerSignatureName = "Amina Bello",
                partnerSignatureStrokes = "0.1,0.2;0.7,0.8",
                nextOfKinName = "Ibrahim Bello",
                nextOfKinGender = "Male",
                nextOfKinPhone = "08055566677",
                customerId = "cust_zaria"
            ) { created ->
                createdPartnerId = created.id
            }
            pumpLooper(6)
        }
        assertTrue(createdPartnerId.isNotBlank())

        composeRule.setContent {
            MyApplicationTheme {
                AdminControlRoomScreen(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("admin_partners_hub_tab")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Partner Signed & Submitted", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("admin_mou_counter_sig_$createdPartnerId")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun uiSmoke_homeScreenEyeCareControlAndRelocatedOfficialBar() {
        composeRule.setContent {
            MyApplicationTheme {
                HomeScreen(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("home_customer_credentials_bar").assertDoesNotExist()
        composeRule.onNodeWithTag("official_installed_horizontal_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("home_eye_care_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("eye_care_control_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("eye_care_switch").performClick()
        composeRule.waitForIdle()
        assertTrue(viewModel.eyeCareEnabled.value)

        viewModel.setEyeCareBrightness(0.55f)
        assertEquals(0.55f, viewModel.eyeCareBrightness.value, 0.01f)
    }

    @Test
    fun uiSmoke_categoriesScreenSelectsCategoryDirectly() {
        composeRule.setContent {
            MyApplicationTheme {
                CategoriesScreen(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("categories_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("category_page_item_clothes").performClick()
        composeRule.waitForIdle()

        assertEquals("Clothes", viewModel.selectedCategory.value)
        assertEquals(AppScreen.HOME, viewModel.currentScreen.value)
    }

    @Test
    fun uiSmoke_cartScreenShowsProceedToPaymentPageAndSummary() {
        runBlocking {
            val sampleProduct = viewModel.allProducts.value.first()
            viewModel.addToCart(sampleProduct) { _, _ -> }
            pumpLooper(4)
        }

        composeRule.setContent {
            MyApplicationTheme {
                CartScreen(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Proceed to Payment Page").assertIsDisplayed()
        composeRule.onNodeWithTag("cart_summary_breakdown_card").assertExists()
    }

    @Test
    fun uiSmoke_productDetailStrictlyGatesReviewToExactDeliveredProduct() {
        val watchProduct = com.example.data.model.Product(
            id = "p_watch_1",
            title = "Luxury Wristwatch",
            category = "Watches",
            brand = "Odiya",
            availableSizes = listOf("Standard"),
            availableColors = listOf(ProductColor("Gold", "#F59E0B")),
            basePrice = 45000.0,
            imageUrl = "",
            description = "Luxury watch",
            stockQuantity = 10
        )
        val bagProduct = com.example.data.model.Product(
            id = "p_bag_1",
            title = "Bag",
            category = "Bags",
            brand = "Odiya",
            availableSizes = listOf("Medium"),
            availableColors = listOf(ProductColor("Black", "#000000")),
            basePrice = 25000.0,
            imageUrl = "",
            description = "Classic bag",
            stockQuantity = 10
        )
        val leatherBagProduct = com.example.data.model.Product(
            id = "p_leather_bag_1",
            title = "Leather Bag",
            category = "Bags",
            brand = "Odiya",
            availableSizes = listOf("Large"),
            availableColors = listOf(ProductColor("Brown", "#78350F")),
            basePrice = 35000.0,
            imageUrl = "",
            description = "Leather bag",
            stockQuantity = 10
        )

        runBlocking {
            val order = com.example.data.local.OrderEntity(
                orderId = "ORD-STRICT-1",
                customerId = "",
                customerName = "Chidi Okafor",
                customerEmail = "chidi@example.com",
                customerPhone = "08012345678",
                deliveryAddress = "Lagos",
                itemsSummary = "1x Leather Bag (Black)",
                totalAmount = 35000.0,
                profit = 8000.0,
                paymentMethod = PaymentMethod.OPAY.title,
                paymentAccount = "8012345678",
                paymentReference = "REF-STRICT-1",
                senderName = "Chidi Okafor",
                status = com.example.data.model.OrderStatus.DELIVERED.name,
                createdAt = System.currentTimeMillis(),
                verificationNote = "Delivered"
            )
            dao.insertOrder(order)
            pumpLooper(4)
        }

        // Substring trap: "Bag" must NOT unlock when only "Leather Bag" or "Luxury Wristwatch" was delivered
        assertTrue(viewModel.hasVerifiedDeliveredPurchaseForProduct(leatherBagProduct))
        org.junit.Assert.assertFalse(viewModel.hasVerifiedDeliveredPurchaseForProduct(bagProduct))
        org.junit.Assert.assertFalse(viewModel.hasVerifiedDeliveredPurchaseForProduct(watchProduct))

        runBlocking {
            viewModel.openProductDetails(bagProduct)
            pumpLooper(2)
        }

        composeRule.setContent {
            MyApplicationTheme {
                ProductDetailScreen(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("reviews_locked_notice")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Ratings & Views Locked", substring = true)
            .assertIsDisplayed()

        // Logged-in different customer must NOT inherit a delivered order even if order.customerId was blank
        runBlocking {
            viewModel.signUp(
                name = "Bola Ade",
                pass = "Password1!",
                phone = "08099998888",
                email = "bola@example.com",
                onSuccess = {},
                onError = {}
            )
            pumpLooper(4)
        }
        org.junit.Assert.assertFalse(viewModel.hasVerifiedDeliveredPurchaseForProduct(leatherBagProduct))
    }

    @Test
    fun uiSmoke_helpSupportFaqReflectsCurrentLoyaltyTerms() {
        composeRule.setContent {
            MyApplicationTheme {
                HelpSupportScreen(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        composeRule.onNode(hasScrollToIndexAction())
            .performScrollToNode(hasText("How do Loyalty Points & Referral Credits work?", substring = true))
        composeRule.onNodeWithText("How do Loyalty Points & Referral Credits work?", substring = true)
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNode(hasScrollToIndexAction())
            .performScrollToNode(hasText("Regular Customer 5th-Patronage Loyalty Reward", substring = true))
        composeRule.onNodeWithText("Regular Customer 5th-Patronage Loyalty Reward", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun partnerPortal_supportsMultipleCategoriesAndMouSampleProductImage() {
        var createdPartner: com.example.data.model.PartnerOrganization? = null
        runBlocking {
            viewModel.submitPartnerAgreementApplication(
                businessName = "Kano Crafts Ltd",
                ownerName = "Yusuf Musa",
                ownerPhone = "08022233344",
                businessAddress = "Suite 3, Kano",
                primaryCategory = "Clothes, Bags",
                opayAccountNumber = "8022233344",
                opayAccountName = "Kano Crafts Ltd",
                moniepointAccountNumber = "9022233344",
                moniepointAccountName = "Kano Crafts Ltd",
                agreementDate = "27 Nov 2026",
                hostingDuration = "12 Months",
                agreedQuantityPerPeriod = 30,
                periodUnit = HostingPeriodUnit.PER_MONTH,
                monthlyHostingPayment = 15000.0,
                stipulatedCommissionPercent = 10.0,
                partnerSignatureName = "Yusuf Musa",
                partnerSignatureStrokes = "0.1,0.1;0.6,0.6",
                selectedCategories = listOf("Clothes", "Bags"),
                sampleProductName = "Handwoven Tote Bag",
                sampleProductImageUrl = "content://media/picker/109"
            ) { created ->
                createdPartner = created
            }
            pumpLooper(4)
        }
        assertNotNull(createdPartner)
        assertEquals(listOf("Clothes", "Bags"), createdPartner!!.selectedCategories)
        assertEquals("Handwoven Tote Bag", createdPartner!!.sampleProductName)
        assertEquals("content://media/picker/109", createdPartner!!.sampleProductImageUrl)

        composeRule.setContent {
            MyApplicationTheme {
                PartnerPortalScreen(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("partner_mou_pick_image_button")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun adminAnalytics_displaysUsdToNgnFxAnalysisCardAndUpdatesRate() {
        viewModel.setManualUsdToNgnRate(1625.0)
        runBlocking {
            assertTrue(viewModel.verifyAdminPassword(viewModel.adminPassword.value))
            pumpLooper(4)
        }

        composeRule.setContent {
            MyApplicationTheme {
                AdminControlRoomScreen(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Sales Analytics", substring = true)
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("analytics_usd_fx_card").assertIsDisplayed()
        composeRule.onNodeWithTag("analytics_refresh_fx_button").assertIsDisplayed()
        composeRule.onNodeWithTag("analytics_sim_discount_input").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("analytics_sim_promo_input").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("analytics_sim_bargain_input").performScrollTo().assertIsDisplayed()
        assertEquals(1625.0, viewModel.usdToNgnRate.value, 0.01)
    }

    @Test
    fun adminProductEntry_showsFxProfitAnalysisAndRecommendedPrice() {
        viewModel.setManualUsdToNgnRate(1600.0)
        runBlocking {
            assertTrue(viewModel.verifyAdminPassword(viewModel.adminPassword.value))
            pumpLooper(4)
        }

        composeRule.setContent {
            MyApplicationTheme {
                AdminControlRoomScreen(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Add New Product", substring = true)
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("admin_cost_usd_input")
            .performScrollTo()
            .performTextInput("10")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("admin_price_fixing_profit_box")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("admin_sim_discount_input")
            .performScrollTo()
            .performTextInput("5")
        composeRule.onNodeWithTag("admin_sim_bargain_input")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("admin_apply_recommended_price_button")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("profit_analysis_usd_summary")
            .performScrollTo()
            .assertIsDisplayed()
    }
}
