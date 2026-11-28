package com.example

import android.app.Application
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.ResolveInfo
import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.DeprosDatabase
import com.example.data.local.ProductEntity
import com.example.data.model.PaymentMethod
import com.example.data.model.ProductColor
import com.example.ui.screens.CheckoutScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.ActiveMerchantPaymentOverlay
import com.example.util.MerchantAppLauncher
import com.example.util.MerchantClipboardActionReceiver
import com.example.util.MerchantPaymentOverlayManager
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowPackageManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w480dp-h2400dp")
class MerchantAppLauncherTest {

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
        MerchantPaymentOverlayManager.clearOverlay(app)
        try {
            viewModel.endLiveCall()
        } catch (_: Exception) {}
        try {
            viewModel.viewModelScope.cancel()
        } catch (_: Exception) {}
        DeprosDatabase.resetInstanceForTests(app)
    }

    private suspend fun pumpLooper(cycles: Int = 6) {
        repeat(cycles) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            delay(35)
        }
    }

    private fun installFakeMerchantApp(shadowPm: ShadowPackageManager, packageName: String, activityName: String) {
        val pkgInfo = PackageInfo().apply {
            this.packageName = packageName
            applicationInfo = ApplicationInfo().apply {
                this.packageName = packageName
            }
        }
        shadowPm.installPackage(pkgInfo)
        val comp = ComponentName(packageName, activityName)
        val activityInfo = ActivityInfo().apply {
            this.packageName = packageName
            this.name = activityName
            applicationInfo = pkgInfo.applicationInfo
            exported = true
        }
        shadowPm.addOrUpdateActivity(activityInfo)
        val mainIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setPackage(packageName)
        }
        val resolveInfo = ResolveInfo().apply {
            this.activityInfo = activityInfo
        }
        @Suppress("DEPRECATION")
        shadowPm.addResolveInfoForIntent(mainIntent, resolveInfo)
    }

    @Test
    fun installedMerchantApps_launchDirectlyWithEmbeddedOdiyaDetailsAndNeverOpenGooglePlay() {
        val shadowPm = Shadows.shadowOf(app.packageManager)
        installFakeMerchantApp(shadowPm, "team.opay.pay", "team.opay.pay.MainActivity")
        installFakeMerchantApp(shadowPm, "com.moniepoint.personal", "com.moniepoint.personal.MainActivity")
        installFakeMerchantApp(shadowPm, "com.transsnet.palmpay", "com.transsnet.palmpay.MainActivity")

        val cm = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val capturedClips = mutableListOf<String>()
        val listener = ClipboardManager.OnPrimaryClipChangedListener {
            cm.primaryClip?.getItemAt(0)?.text?.toString()?.let { capturedClips.add(it) }
        }
        cm.addPrimaryClipChangedListener(listener)

        val opayResult = MerchantAppLauncher.launchInstalledMerchantApp(
            context = app,
            method = PaymentMethod.OPAY,
            accountNumber = "8123456789",
            accountName = "ODIYA ENTERPRISE",
            storeName = "ODIYA",
            payableAmount = 45000.0
        )
        cm.removePrimaryClipChangedListener(listener)
        assertTrue(opayResult.launchedInstalledApp)
        assertEquals("team.opay.pay", opayResult.installedPackageName)
        assertTrue(opayResult.clipboardSet)
        assertEquals("8123456789", opayResult.embeddedAccountNumber)
        assertEquals("45000.00", opayResult.embeddedPlainAmount)

        assertTrue(capturedClips.contains("45000.00"))
        assertTrue(capturedClips.contains("8123456789"))
        assertEquals("8123456789", cm.primaryClip?.getItemAt(0)?.text?.toString())

        val overlay = MerchantPaymentOverlayManager.activeOverlay.value
        assertNotNull(overlay)
        assertEquals("8123456789", overlay?.accountNumber)
        assertEquals("45000.00", overlay?.plainAmount)
        assertTrue(overlay?.formattedAmount?.contains("45,000") == true)

        val launchedIntent = Shadows.shadowOf(app).nextStartedActivity
        assertNotNull(launchedIntent)
        assertEquals("team.opay.pay", launchedIntent.component?.packageName ?: launchedIntent.`package`)
        assertEquals("8123456789", launchedIntent.getStringExtra("odiya_account_number"))
        assertEquals("45000.00", launchedIntent.getStringExtra("odiya_payable_amount"))
        assertEquals("8123456789", launchedIntent.getStringExtra("account_number"))
        assertEquals("45000.00", launchedIntent.getStringExtra("amount"))

        assertEquals(
            "com.moniepoint.personal",
            MerchantAppLauncher.findInstalledMerchantPackage(app, PaymentMethod.MONIEPOINT)
        )
        assertEquals(
            "com.transsnet.palmpay",
            MerchantAppLauncher.findInstalledMerchantPackage(app, PaymentMethod.PALMPAY)
        )
    }

    @Test
    fun pipEmbeddedOverlayCard_andClipboardActionReceiver_exposeAccountNumberAndAmountInFigures() {
        val overlay = ActiveMerchantPaymentOverlay(
            merchantTitle = "OPay",
            packageName = "team.opay.pay",
            accountNumber = "9012345678",
            accountName = "ODIYA ENTERPRISE",
            storeName = "ODIYA",
            payableAmount = 27500.0,
            formattedAmount = "₦27,500.00",
            plainAmount = "27500.00"
        )

        composeRule.setContent {
            MyApplicationTheme {
                EmbeddedMerchantPipOverlayCard(overlay = overlay)
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("pip_embedded_payment_card").assertIsDisplayed()
        composeRule.onNodeWithText("9012345678").assertIsDisplayed()
        composeRule.onNodeWithText("₦27,500.00").assertIsDisplayed()

        val receiver = MerchantClipboardActionReceiver()
        val copyAmtIntent = Intent(MerchantClipboardActionReceiver.ACTION_COPY_MERCHANT_DETAIL).apply {
            putExtra(MerchantClipboardActionReceiver.EXTRA_COPY_LABEL, "Payable Amount")
            putExtra(MerchantClipboardActionReceiver.EXTRA_COPY_VALUE, "27500.00")
            putExtra(MerchantClipboardActionReceiver.EXTRA_TOAST_MESSAGE, "Copied Payable Amount: ₦27,500.00")
        }
        receiver.onReceive(app, copyAmtIntent)
        val cm = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        assertEquals("27500.00", cm.primaryClip?.getItemAt(0)?.text?.toString())
    }

    @Test
    fun uninstalledMerchantApp_doesNotAutoLaunchGooglePlayOnClick() {
        assertNull(MerchantAppLauncher.findInstalledMerchantPackage(app, PaymentMethod.OPAY))
        val res = MerchantAppLauncher.launchInstalledMerchantApp(
            context = app,
            method = PaymentMethod.OPAY,
            accountNumber = "8123456789",
            accountName = "ODIYA",
            storeName = "ODIYA",
            payableAmount = 19500.0
        )
        assertFalse(res.launchedInstalledApp)
        assertNull(res.installedPackageName)
        assertNull(Shadows.shadowOf(app).nextStartedActivity)

        // Only explicit Google Play button triggers Play URL
        val explicitOpened = MerchantAppLauncher.openGooglePlayForNewCustomer(app, PaymentMethod.OPAY)
        assertTrue(explicitOpened)
        val playIntent = Shadows.shadowOf(app).nextStartedActivity
        assertNotNull(playIntent)
        assertTrue(playIntent.data.toString().contains("team.opay.pay"))
    }

    @Test
    fun checkoutScreen_displaysMerchantInstructionAndOpensPortalDialogWithoutGooglePlayRedirect() {
        runBlocking {
            dao.insertProduct(
                ProductEntity(
                    id = "merch-test-prod",
                    title = "Organic Honey Jar",
                    category = "Processed Foods/Pastries",
                    brand = "ODIYA Foods",
                    availableBrands = "ODIYA Foods",
                    availableSizes = "500ml",
                    availableColorsJson = "Amber|#D97706",
                    basePrice = 8500.0,
                    discountPercent = 0,
                    stockQuantity = 10,
                    imageUrl = "",
                    model3dType = "BOTTLE",
                    description = "Pure honey",
                    rating = 4.9,
                    reviewCount = 4,
                    sku = "SKU-HONEY-1",
                    costPrice = 5000.0,
                    expiryMonthsRemaining = 12,
                    createdAt = System.currentTimeMillis()
                )
            )
            pumpLooper(6)
            val prod = viewModel.allProducts.value.first { it.id == "merch-test-prod" }
            viewModel.addToCart(prod, "ODIYA Foods", "500ml", ProductColor("Amber", "#D97706"), 1) { _, _ -> }
            pumpLooper(4)
        }

        composeRule.setContent {
            MyApplicationTheme {
                CheckoutScreen(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Proceed with Delivery", substring = true).performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("checkout_merchant_instruction_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("direct_portal_opay_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        // Confirm NO Google Play intent was auto-fired and embedded ODIYA checkout dialog opened instead
        assertNull(Shadows.shadowOf(app).nextStartedActivity)
        composeRule.onNodeWithTag("direct_portal_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("direct_portal_app_redirect_card").assertIsDisplayed()
        composeRule.onNodeWithTag("direct_portal_copy_both_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("direct_portal_copy_amount_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("direct_portal_receipt_upload_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("direct_portal_return_to_app_button").assertExists()
        composeRule.onNodeWithTag("direct_portal_bottom_return_button").performScrollTo().assertIsDisplayed()

        // Return to Complete Payment cleanly closes dialog back to CheckoutScreen with submit order + receipt button
        composeRule.onNodeWithTag("direct_portal_bottom_return_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("checkout_merchant_instruction_card").assertExists()
        composeRule.onNodeWithTag("send_orders_and_receipt_button")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("Submit Order and Payment Receipt", substring = true)
    }
}
