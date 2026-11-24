package com.example

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.DeprosDatabase
import com.example.data.local.ProductEntity
import com.example.data.model.HostingPeriodUnit
import com.example.data.model.PartnerAgreementStatus
import com.example.data.model.PartnerTransactionType
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.ProductColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PartnerMouLifecycleTest {

    private lateinit var app: Application
    private lateinit var viewModel: MainViewModel
    private lateinit var dao: com.example.data.local.DeprosDao

    @Before
    fun setup() = runBlocking {
        app = ApplicationProvider.getApplicationContext()
        app.getSharedPreferences("smartshop_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        dao = DeprosDatabase.getDatabase(app).dao()
        viewModel = MainViewModel(app)
        pumpLooper()
    }

    private suspend fun pumpLooper(cycles: Int = 10) {
        repeat(cycles) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            delay(45)
        }
    }

    private suspend fun createAndFinalizePartner(
        businessName: String = "Kano Heritage Textiles",
        phone: String = "08011223344",
        commissionPct: Double = 15.0,
        monthlyFee: Double = 25000.0,
        duration: String = "6 Months"
    ): String {
        var partnerId = ""
        viewModel.submitPartnerAgreementApplication(
            businessName = businessName,
            ownerName = "Amina Bello",
            ownerPhone = phone,
            businessAddress = "Plot 12 Market Rd",
            primaryCategory = "Clothes",
            opayAccountNumber = "8011223344",
            opayAccountName = businessName,
            moniepointAccountNumber = "9011223344",
            moniepointAccountName = businessName,
            agreementDate = "20 Nov 2026",
            hostingDuration = duration,
            agreedQuantityPerPeriod = 20,
            periodUnit = HostingPeriodUnit.PER_MONTH,
            monthlyHostingPayment = monthlyFee,
            stipulatedCommissionPercent = commissionPct,
            partnerSignatureName = "Amina Bello",
            partnerSignatureStrokes = "0.1,0.2;0.8,0.7"
        ) { created ->
            partnerId = created.id
        }
        pumpLooper(4)

        var finalizedOk = false
        viewModel.finalizePartnerAgreement(
            partnerId = partnerId,
            adminSignatoryName = "ODIYA Admin",
            adminStrokes = "0.2,0.2;0.7,0.8",
            commissionPercent = commissionPct,
            agreedQtyPerPeriod = 20,
            periodUnit = HostingPeriodUnit.PER_MONTH,
            monthlyHostingPayment = monthlyFee,
            hostingDuration = duration,
            opayNumber = "8011223344",
            opayName = businessName,
            moniepointNumber = "9011223344",
            moniepointName = businessName
        ) { ok, _ ->
            finalizedOk = ok
        }
        assertTrue(finalizedOk)
        pumpLooper(4)
        return partnerId
    }

    private suspend fun insertPartnerProduct(
        id: String,
        partnerId: String,
        partnerName: String = "Kano Heritage Textiles",
        price: Double = 20000.0,
        stock: Int = 10,
        commissionPct: Double = 15.0
    ): Product {
        val entity = ProductEntity(
            id = id,
            title = "Handwoven Gown $id",
            category = "Clothes",
            brand = "CraftStandard",
            availableBrands = "CraftStandard",
            availableSizes = "M",
            availableColorsJson = "Black|#000000",
            basePrice = price,
            discountPercent = 0,
            stockQuantity = stock,
            imageUrl = "",
            model3dType = "CLOTHES",
            description = "Authentic partner garment",
            rating = 4.9,
            reviewCount = 5,
            sku = "SKU-$id",
            costPrice = price * 0.6,
            expiryMonthsRemaining = 12,
            createdAt = System.currentTimeMillis(),
            partnerId = partnerId,
            partnerName = partnerName,
            partnerCommissionPercent = commissionPct,
            partnerBatchId = "BATCH-TEST",
            isCatalogLocked = false
        )
        dao.insertProduct(entity)
        pumpLooper(8)
        return viewModel.allProducts.value.first { it.id == id }
    }

    @Test
    fun mouPercentageSplitsAutomaticallyAtPaymentAndHostingDeductsEvery30Days() = runBlocking {
        val partnerId = createAndFinalizePartner(
            businessName = "Zaria Loom Co",
            commissionPct = 20.0,
            monthlyFee = 30000.0
        )

        // Cycle #1 deducted automatically on MOU finalization
        val afterFinalize = viewModel.partnerOrganizations.value.first { it.id == partnerId }
        assertEquals(PartnerAgreementStatus.FINALIZED, afterFinalize.status)
        assertEquals(1, afterFinalize.hostingCyclesBilled)
        assertEquals(30000.0, afterFinalize.totalHostingFeesAutoDeducted, 0.01)

        // Advance time by two additional 30-day cycles -> Cycles #2 and #3 auto-deducted
        val twoCyclesLater = afterFinalize.nextHostingBillingDueAt + MainViewModel.THIRTY_DAYS_MS
        val newlyBilled = viewModel.runAutomatic30DayHostingBilling(twoCyclesLater)
        assertEquals(2, newlyBilled)
        pumpLooper(4)

        val after30d = viewModel.partnerOrganizations.value.first { it.id == partnerId }
        assertEquals(3, after30d.hostingCyclesBilled)
        assertEquals(90000.0, after30d.totalHostingFeesAutoDeducted, 0.01)

        // Add partner product (₦25,000 x 2 = ₦50,000 gross; 20% = ₦10,000 to ODIYA, ₦40,000 to Partner)
        val prod = insertPartnerProduct(
            id = "ptr-prod-split-1",
            partnerId = partnerId,
            partnerName = "Zaria Loom Co",
            price = 25000.0,
            stock = 6,
            commissionPct = 20.0
        )

        var added = false
        viewModel.addToCart(prod, brand = "CraftStandard", size = "M", color = ProductColor("Black", "#000000"), quantity = 2) { ok, _ ->
            added = ok
        }
        pumpLooper(4)
        assertTrue(added)

        var placedOrderId = ""
        viewModel.placeOrder(
            customerName = "Chinedu Okafor",
            customerEmail = "chinedu@example.com",
            customerPhone = "08099887766",
            deliveryAddress = "14 Allen Ave, Ikeja",
            paymentMethod = PaymentMethod.OPAY,
            paymentAccountOrTerminal = "8011223344",
            paymentReference = "OPAY-998877",
            senderName = "Chinedu Okafor",
            deliveryConditionTitle = "Standard Delivery",
            deliveryFeeAmount = 2000.0,
            onSuccess = { placedOrderId = it.orderId },
            onFailure = { }
        )
        pumpLooper(8)

        assertTrue(placedOrderId.isNotBlank())
        val afterPayment = viewModel.partnerOrganizations.value.first { it.id == partnerId }
        assertEquals(10000.0, afterPayment.totalAutoDeducted247Share, 0.01)
        assertEquals(40000.0, afterPayment.totalAutoPartnerNetDeposited, 0.01)

        val saleTxn = viewModel.partnerTransactions.value.firstOrNull {
            it.partnerId == partnerId && it.orderId == placedOrderId
        }
        assertNotNull(saleTxn)
        assertEquals(50000.0, saleTxn!!.grossAmount, 0.01)
        assertEquals(10000.0, saleTxn.commissionProfitAmount, 0.01)
        assertEquals(40000.0, saleTxn.partnerNetAmount, 0.01)
    }

    @Test
    fun catalogLockPreventsCustomerAddToCartWhileKeepingDisplayUntilRenewed() = runBlocking {
        val partnerId = createAndFinalizePartner(businessName = "Abuja Atelier", commissionPct = 10.0, monthlyFee = 15000.0)
        val prod = insertPartnerProduct(
            id = "ptr-prod-lock-1",
            partnerId = partnerId,
            partnerName = "Abuja Atelier",
            price = 18000.0,
            stock = 5,
            commissionPct = 10.0
        )

        // Admin clicks Catalog Lock at end of hosting period
        var lockOk = false
        viewModel.togglePartnerCatalogLock(partnerId, true, "Hosting period ended") { ok, _ ->
            lockOk = ok
        }
        pumpLooper(8)
        assertTrue(lockOk)

        // Product remains displayed in catalog, marked isCatalogLocked = true
        val lockedProd = viewModel.allProducts.value.firstOrNull { it.id == prod.id }
        assertNotNull(lockedProd)
        assertTrue(lockedProd!!.isCatalogLocked)
        assertTrue(viewModel.lockedPartnerIds.value.contains(partnerId))

        // Customer Add-to-Cart MUST be prevented
        var addAllowedWhenLocked = true
        var blockedReason = ""
        viewModel.addToCart(lockedProd) { ok, msg ->
            addAllowedWhenLocked = ok
            blockedReason = msg
        }
        pumpLooper(4)
        assertFalse(addAllowedWhenLocked)
        assertTrue(blockedReason.contains("Display Only", ignoreCase = true))
        assertTrue(viewModel.cartItems.value.isEmpty())

        // Partner renews contract -> Admin unlocks Catalog Lock -> Add-to-Cart works again
        var renewOk = false
        viewModel.renewPartnerHostingContract(partnerId, "6 Months", 15000.0) { ok, _ ->
            renewOk = ok
        }
        pumpLooper(8)
        assertTrue(renewOk)
        assertFalse(viewModel.lockedPartnerIds.value.contains(partnerId))

        val unlockedProd = viewModel.allProducts.value.first { it.id == prod.id }
        assertFalse(unlockedProd.isCatalogLocked)

        var addAllowedAfterRenew = false
        viewModel.addToCart(unlockedProd) { ok, _ ->
            addAllowedAfterRenew = ok
        }
        pumpLooper(4)
        assertTrue(addAllowedAfterRenew)
        assertEquals(1, viewModel.cartItems.value.size)
    }

    @Test
    fun terminateContractDeletesPartnerProductsAndMarksTerminatedOutright() = runBlocking {
        val partnerId = createAndFinalizePartner(businessName = "Jos Silk Studio", commissionPct = 12.0, monthlyFee = 10000.0)
        val prod = insertPartnerProduct(
            id = "ptr-prod-term-1",
            partnerId = partnerId,
            partnerName = "Jos Silk Studio",
            price = 12000.0,
            stock = 4,
            commissionPct = 12.0
        )

        var termOk = false
        viewModel.terminatePartnerContractAndDeleteAll(partnerId, "Partner terminated hosting contract") { ok, _ ->
            termOk = ok
        }
        pumpLooper(10)

        assertTrue(termOk)
        assertNull(viewModel.allProducts.value.firstOrNull { it.id == prod.id })
        assertNull(dao.getProductById(prod.id))

        val partner = viewModel.partnerOrganizations.value.first { it.id == partnerId }
        assertEquals(PartnerAgreementStatus.TERMINATED, partner.status)
        assertTrue(partner.isCatalogLocked)

        val termTxn = viewModel.partnerTransactions.value.firstOrNull {
            it.partnerId == partnerId && it.type == PartnerTransactionType.CONTRACT_TERMINATED_DELETED
        }
        assertNotNull(termTxn)
    }

    @Test
    fun partnerLoginOfflineSaleCartRemovesQuantityAndAdminCanCollectAccruedUnitCost() = runBlocking {
        val partnerId = createAndFinalizePartner(
            businessName = "Ibadan Craft House",
            phone = "08055443322",
            commissionPct = 15.0,
            monthlyFee = 20000.0
        )
        val prod = insertPartnerProduct(
            id = "ptr-prod-offline-1",
            partnerId = partnerId,
            partnerName = "Ibadan Craft House",
            price = 10000.0,
            stock = 8,
            commissionPct = 15.0
        )

        // Partner authenticates via phone or partner ID
        assertNotNull(viewModel.authenticatePartnerPortal("08055443322"))
        assertNotNull(viewModel.authenticatePartnerPortal(partnerId))
        assertNull(viewModel.authenticatePartnerPortal("nonexistent-partner"))

        // Partner uses Offline Sale Add-to-Cart to remove 3 sold units
        var offlineOk = false
        viewModel.processPartnerOfflineSaleCart(
            partnerId = partnerId,
            quantitiesByProductId = mapOf(prod.id to 3),
            note = "Walk-in boutique sale #104"
        ) { ok, _ ->
            offlineOk = ok
        }
        pumpLooper(8)
        assertTrue(offlineOk)

        val updatedProd = dao.getProductById(prod.id)
        assertNotNull(updatedProd)
        assertEquals(5, updatedProd!!.stockQuantity)

        val removalTxn = viewModel.partnerTransactions.value.firstOrNull {
            it.partnerId == partnerId && it.type == PartnerTransactionType.PARTNER_OFFLINE_SALE_REMOVAL
        }
        assertNotNull(removalTxn)
        assertEquals(3, removalTxn!!.quantity)

        // If Partner fails to log 2 offline sales, Admin collects 15% of (2 * ₦10,000 = ₦20,000) = ₦3,000
        var penaltyOk = false
        viewModel.collectUnreportedOfflineSalePenalty(
            partnerId = partnerId,
            productId = prod.id,
            unreportedQty = 2,
            reason = "Audit found 2 unreported offline units"
        ) { ok, _ ->
            penaltyOk = ok
        }
        pumpLooper(8)
        assertTrue(penaltyOk)

        val afterPenaltyPartner = viewModel.partnerOrganizations.value.first { it.id == partnerId }
        assertEquals(3000.0, afterPenaltyPartner.totalAccruedOfflinePenalty, 0.01)
        val postAuditProd = dao.getProductById(prod.id)
        assertEquals(3, postAuditProd!!.stockQuantity)
    }

    @Test
    fun paystackSplitCalculatesCommissionDeliveryAndDueHostingOffsetAccurately() = runBlocking {
        val partnerAId = createAndFinalizePartner(
            businessName = "Abuja Velvet Hub",
            phone = "08022110099",
            commissionPct = 20.0,
            monthlyFee = 15000.0
        )
        viewModel.updatePartnerEditableTerms(
            partnerId = partnerAId,
            commissionPercent = 20.0,
            agreedQtyPerPeriod = 20,
            periodUnit = HostingPeriodUnit.PER_MONTH,
            monthlyHostingPayment = 15000.0,
            hostingDuration = "6 Months",
            opayNumber = "8022110099",
            opayName = "Abuja Velvet Hub",
            moniepointNumber = "",
            moniepointName = "",
            paystackBankCode = "999992",
            paystackAccountNumber = "8022110099",
            paystackAccountName = "Abuja Velvet Hub",
            paystackSubaccountCode = "ACCT_testpartnerA01",
            pendingHostingFeeBalance = 10000.0,
            autoDeductHostingFromPaystackSplit = true
        )

        val partnerBId = createAndFinalizePartner(
            businessName = "Kano Leather Works",
            phone = "08033221100",
            commissionPct = 10.0,
            monthlyFee = 12000.0
        )
        viewModel.updatePartnerEditableTerms(
            partnerId = partnerBId,
            commissionPercent = 10.0,
            agreedQtyPerPeriod = 15,
            periodUnit = HostingPeriodUnit.PER_MONTH,
            monthlyHostingPayment = 12000.0,
            hostingDuration = "6 Months",
            opayNumber = "",
            opayName = "",
            moniepointNumber = "9033221100",
            moniepointName = "Kano Leather Works",
            paystackBankCode = "50515",
            paystackAccountNumber = "9033221100",
            paystackAccountName = "Kano Leather Works",
            paystackSubaccountCode = "ACCT_testpartnerB02",
            pendingHostingFeeBalance = 0.0,
            autoDeductHostingFromPaystackSplit = true
        )

        val prodA = insertPartnerProduct(
            id = "psk-split-prod-a",
            partnerId = partnerAId,
            partnerName = "Abuja Velvet Hub",
            price = 50000.0,
            stock = 5,
            commissionPct = 20.0
        )
        val prodB = insertPartnerProduct(
            id = "psk-split-prod-b",
            partnerId = partnerBId,
            partnerName = "Kano Leather Works",
            price = 30000.0,
            stock = 5,
            commissionPct = 10.0
        )

        // Single-partner cart: ₦50,000 + ₦2,500 delivery = ₦52,500 total.
        // 20% commission = ₦10,000 -> ODIYA; remaining raw net = ₦40,000 minus ₦10,000 due hosting = ₦30,000 -> Partner A.
        // ODIYA total = ₦2,500 delivery + ₦10,000 comm + ₦10,000 hosting offset = ₦22,500.
        viewModel.addToCart(prodA, brand = "CraftStandard", size = "M", color = ProductColor("Black", "#000000"), quantity = 1) { _, _ -> }
        pumpLooper(4)
        val singlePlan = viewModel.calculateCartPaystackSplitPlan(deliveryFeeAmount = 2500.0)
        assertEquals("PARTNER_SUBACCOUNT", singlePlan.splitMode)
        assertTrue(singlePlan.hasActiveSubaccountSplit)
        assertEquals(52500.0, singlePlan.totalAmountNaira, 0.01)
        assertEquals(10000.0, singlePlan.odiyaCommissionNaira, 0.01)
        assertEquals(10000.0, singlePlan.hostingOffsetNaira, 0.01)
        assertEquals(22500.0, singlePlan.odiyaTotalShareNaira, 0.01)
        assertEquals(30000.0, singlePlan.totalPartnerNetNaira, 0.01)
        assertEquals("ACCT_testpartnerA01", singlePlan.allocations.first().subaccountCode)

        // Multi-partner cart: add Partner B (₦30,000, 10% comm = ₦3,000 ODIYA, ₦27,000 Partner B).
        viewModel.addToCart(prodB, brand = "CraftStandard", size = "M", color = ProductColor("Black", "#000000"), quantity = 1) { _, _ -> }
        pumpLooper(4)
        val multiPlan = viewModel.calculateCartPaystackSplitPlan(deliveryFeeAmount = 2500.0)
        assertEquals("PARTNER_MULTI_SPLIT", multiPlan.splitMode)
        assertEquals(82500.0, multiPlan.totalAmountNaira, 0.01)
        assertEquals(13000.0, multiPlan.odiyaCommissionNaira, 0.01)
        assertEquals(10000.0, multiPlan.hostingOffsetNaira, 0.01)
        assertEquals(25500.0, multiPlan.odiyaTotalShareNaira, 0.01)
        assertEquals(57000.0, multiPlan.totalPartnerNetNaira, 0.01)
        assertEquals(2, multiPlan.allocations.size)
    }

    @Test
    fun partnerPaystackSubaccountAndSqlSplitSchemaPersistProperly() = runBlocking {
        val partnerId = createAndFinalizePartner(
            businessName = "Enugu Loom Studio",
            phone = "08044556677",
            commissionPct = 15.0,
            monthlyFee = 18000.0
        )
        viewModel.updatePartnerEditableTerms(
            partnerId = partnerId,
            commissionPercent = 15.0,
            agreedQtyPerPeriod = 20,
            periodUnit = HostingPeriodUnit.PER_MONTH,
            monthlyHostingPayment = 18000.0,
            hostingDuration = "6 Months",
            opayNumber = "8044556677",
            opayName = "Enugu Loom Studio",
            moniepointNumber = "",
            moniepointName = "",
            paystackBankCode = "058",
            paystackAccountNumber = "0123456789",
            paystackAccountName = "ENUGU LOOM LTD",
            paystackSubaccountCode = "ACCT_enuguloom99",
            pendingHostingFeeBalance = 18000.0,
            autoDeductHostingFromPaystackSplit = false
        )
        pumpLooper(4)

        val reloadedViewModel = MainViewModel(app)
        pumpLooper(8)
        val restored = reloadedViewModel.partnerOrganizations.value.firstOrNull { it.id == partnerId }
        assertNotNull(restored)
        assertEquals("ACCT_enuguloom99", restored!!.paystackSubaccountCode)
        assertEquals("058", restored.paystackBankCode)
        assertEquals("0123456789", restored.paystackAccountNumber)
        assertEquals("ENUGU LOOM LTD", restored.paystackAccountName)
        assertEquals(18000.0, restored.pendingHostingFeeBalance, 0.01)
        assertFalse(restored.autoDeductHostingFromPaystackSplit)

        val sql = com.example.data.remote.SupabaseSyncService().generateSupabaseSqlSchema()
        assertTrue(sql.contains("split_mode TEXT"))
        assertTrue(sql.contains("subaccount_code TEXT"))
        assertTrue(sql.contains("odiya_share_kobo BIGINT"))
        assertTrue(sql.contains("hosting_fee_kobo BIGINT"))
        assertTrue(sql.contains("partner_net_kobo BIGINT"))
    }
}
