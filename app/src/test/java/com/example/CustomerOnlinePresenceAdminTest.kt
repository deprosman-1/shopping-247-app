package com.example

import android.app.Application
import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Customer
import com.example.data.model.SupabaseConfig
import com.example.ui.screens.AdminControlRoomScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.sun.net.httpserver.HttpServer
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
import java.net.InetAddress
import java.net.InetSocketAddress

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w480dp-h2400dp")
class CustomerOnlinePresenceAdminTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var app: Application
    private lateinit var viewModel: MainViewModel
    private var server: HttpServer? = null

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
        try {
            server?.stop(0)
        } catch (_: Exception) {}
        com.example.data.local.DeprosDatabase.resetInstanceForTests(app)
    }

    private fun pumpLooper() = runBlocking {
        repeat(8) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            delay(40)
        }
    }

    @Test
    fun customerHeartbeat_tracksOnlinePresenceAndSignOutClearsIt() = runBlocking {
        var signedUp = false
        viewModel.signUp(
            name = "Bimbo Adeyemi",
            pass = "pass1234",
            phone = "+2348099887766",
            address = "14 Lekki Phase 1",
            onSuccess = { signedUp = true },
            onError = {}
        )
        repeat(20) {
            if (signedUp) return@repeat
            pumpLooper()
        }
        assertTrue(signedUp)

        val signedInCustomer = viewModel.currentCustomer.value!!
        viewModel.reportCustomerOnlineHeartbeat(isOnlineNow = true, screenName = AppScreen.HOME.name)
        pumpLooper()

        assertTrue(viewModel.isCustomerOnline(signedInCustomer))
        val matched = viewModel.findCustomerOnlinePresence(signedInCustomer)
        assertNotNull(matched)
        assertEquals("Bimbo Adeyemi", matched?.customerName)

        // Non-admin device must NOT receive Admin online notice popups
        assertNull(viewModel.latestCustomerOnlineNotice.value)

        viewModel.signOut()
        pumpLooper()
        assertFalse(viewModel.isCustomerOnline(signedInCustomer))
    }

    @Test
    fun remoteOnlinePresence_alertsAdminControlRoomOnlyAndCanBeDismissed() = runBlocking {
        val now = System.currentTimeMillis()
        val remotePresencePayload = """
            {
              "updatedAt": $now,
              "customers": [
                {
                  "sessionId": "cust-session-remote-99",
                  "customerId": "cust-remote-99",
                  "customerName": "Kemi Oladipo",
                  "customerPhone": "+2348055443322",
                  "currentScreen": "CART",
                  "isOnline": true,
                  "lastSeenAt": $now
                }
              ]
            }
        """.trimIndent()

        val http = HttpServer.create(InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0)
        server = http
        http.createContext("/storage/v1/object/public/product-media/system/online_customers_presence.json") { ex ->
            val bytes = remotePresencePayload.toByteArray(Charsets.UTF_8)
            ex.responseHeaders.add("Content-Type", "application/json")
            ex.sendResponseHeaders(200, bytes.size.toLong())
            ex.responseBody.use { it.write(bytes) }
        }
        http.createContext("/storage/v1/object") { ex ->
            val bytes = "{}".toByteArray(Charsets.UTF_8)
            ex.responseHeaders.add("Content-Type", "application/json")
            ex.sendResponseHeaders(200, bytes.size.toLong())
            ex.responseBody.use { it.write(bytes) }
        }
        http.start()

        viewModel.repository.supabaseSync.updateConfig(
            SupabaseConfig(
                url = "http://127.0.0.1:${http.address.port}",
                anonKey = "sb_publishable_test_key_odiya",
                isConnected = true
            )
        )

        // While customer (not Admin), remote presence does NOT raise Admin notice
        viewModel.refreshOnlineCustomersNow()
        pumpLooper()
        assertNull(viewModel.latestCustomerOnlineNotice.value)

        // Unlock Admin -> refreshing reveals live notice & Admin Control Room card
        assertTrue(viewModel.verifyAdminPassword(viewModel.adminPassword.value))
        viewModel.refreshOnlineCustomersNow()
        repeat(20) {
            if (viewModel.latestCustomerOnlineNotice.value != null) return@repeat
            pumpLooper()
        }

        val notice = viewModel.latestCustomerOnlineNotice.value
        assertNotNull(notice)
        assertTrue(notice!!.contains("Kemi Oladipo"))

        val mockCustomer = Customer(
            id = "cust-remote-99",
            fullName = "Kemi Oladipo",
            email = "kemi@example.com",
            phoneNumber = "+2348055443322",
            deliveryAddress = "10 Victoria Island, Lagos"
        )
        assertTrue(viewModel.isCustomerOnline(mockCustomer))

        composeRule.setContent {
            MyApplicationTheme {
                AdminControlRoomScreen(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("admin_customer_online_notice_card").assertIsDisplayed()
        composeRule.onNodeWithText("1 Customer(s) Online Now").assertIsDisplayed()
        composeRule.onNodeWithText(notice).assertIsDisplayed()

        viewModel.dismissCustomerOnlineNotice()
        composeRule.waitForIdle()
        assertNull(viewModel.latestCustomerOnlineNotice.value)
    }

    @Test
    fun customerHomeScreen_neverDisplaysActiveSavedOrOfflineNotices() {
        viewModel.saveCustomerDeliveryInfo("Tunde Lawal", "+2348011122233", "5 Marina Road, Lagos")
        pumpLooper()

        composeRule.setContent {
            MyApplicationTheme {
                HomeScreen(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("home_customer_credentials_bar").assertDoesNotExist()
        composeRule.onNodeWithTag("official_installed_horizontal_bar").assertDoesNotExist()
        assertEquals(0, composeRule.onAllNodesWithTag("home_offline_status_banner").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("ACTIVE").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("SAVED").fetchSemanticsNodes().size)
    }
}
