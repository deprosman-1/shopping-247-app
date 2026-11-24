package com.example

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.SupabaseConfig
import com.example.data.remote.SupabaseSyncService
import com.sun.net.httpserver.HttpServer
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CustomerOrderTrackingTest {

    private lateinit var app: Application
    private lateinit var viewModel: MainViewModel
    private lateinit var server: HttpServer
    private var serverPort: Int = 0
    private val capturedPaths = CopyOnWriteArrayList<String>()
    private var ordersResponseJson: String = "[]"

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        app.getSharedPreferences("depros_admin_settings", android.content.Context.MODE_PRIVATE)
            .edit().clear().commit()

        server = HttpServer.create(InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0)
        serverPort = server.address.port
        server.createContext("/rest/v1/orders") { exchange ->
            val rawUri = exchange.requestURI.toString()
            capturedPaths.add(rawUri)
            val bytes = ordersResponseJson.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()

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
            server.stop(0)
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
    fun supabaseSyncService_tracksOrdersFromOrdersTable() = runBlocking {
        val now = System.currentTimeMillis()
        ordersResponseJson = """
            [
              {
                "order_id": "ORD-9012345",
                "customer_id": "cust-track-901",
                "customer_name": "Chinedu Okafor",
                "customer_email": "chinedu@example.com",
                "customer_phone": "+2348011223344",
                "delivery_address": "12 Admiralty Way, Lekki Phase 1, Lagos",
                "items_summary": "2x Luxury Sneakers (42, Black)",
                "total_amount": 85000.0,
                "payment_method": "PAYSTACK",
                "payment_account": "Paystack Merchant Gateway",
                "payment_reference": "PSKODY-901",
                "sender_name": "Chinedu Okafor",
                "status": "OUT_FOR_DELIVERY",
                "verification_note": "Rider En Route via Lekki Express",
                "status_updates_json": "PENDING_VERIFICATION|${now - 7200000}|Order received;CONFIRMED|${now - 3600000}|Paystack verified;OUT_FOR_DELIVERY|${now - 900000}|Rider picked up parcel",
                "dispatch_rider_whatsapp": "+2348099001122",
                "created_at": ${now - 7200000}
              }
            ]
        """.trimIndent()

        val syncService = SupabaseSyncService(
            SupabaseConfig(
                url = "http://127.0.0.1:$serverPort",
                anonKey = "sb_publishable_test_key_247",
                isConnected = true
            )
        )

        val trackedRes = syncService.trackOrdersFromCloud("ORD-9012345")
        assertTrue(trackedRes.isSuccess)
        val orders = trackedRes.getOrThrow()
        assertEquals(1, orders.size)
        val tracked = orders.first()
        assertEquals("ORD-9012345", tracked.orderId)
        assertEquals(OrderStatus.OUT_FOR_DELIVERY, tracked.status)
        assertEquals("Rider En Route via Lekki Express", tracked.verificationNote)
        assertEquals("+2348099001122", tracked.dispatchRiderWhatsapp)
        assertEquals(3, tracked.statusUpdates.size)
        assertTrue(capturedPaths.any { it.contains("/rest/v1/orders") && it.contains("ORD-9012345") })

        capturedPaths.clear()
        val custRes = syncService.fetchCustomerOrdersFromCloud(
            customerId = "cust-track-901",
            customerPhone = "08011223344"
        )
        assertTrue(custRes.isSuccess)
        assertTrue(capturedPaths.any { it.contains("customer_id.eq.cust-track-901") && it.contains("8011223344") })
    }

    @Test
    fun viewModel_searchAndRefreshOrderTracking_updatesLocalStatusFromSupabase() = runBlocking {
        val now = System.currentTimeMillis()
        viewModel.repository.createOrder(
            Order(
                orderId = "ORD-7788990",
                customerId = "cust-local-778",
                customerName = "Zainab Musa",
                customerEmail = "zainab@example.com",
                customerPhone = "08055566677",
                deliveryAddress = "50 Aminu Kano Cres, Wuse II, Abuja",
                itemsSummary = "1x Designer Handbag",
                totalAmount = 64000.0,
                paymentMethod = PaymentMethod.OPAY,
                paymentAccount = "OPay ODIYA",
                paymentReference = "OPAY-778",
                senderName = "Zainab Musa",
                status = OrderStatus.PENDING_VERIFICATION,
                createdAt = now - 5000000L,
                verificationNote = "Awaiting admin confirmation"
            )
        )
        pumpLooper()

        viewModel.repository.supabaseSync.updateConfig(
            SupabaseConfig(
                url = "http://127.0.0.1:$serverPort",
                anonKey = "sb_publishable_live_test_key",
                isConnected = true
            )
        )
        ordersResponseJson = """
            [
              {
                "order_id": "ORD-7788990",
                "customer_id": "cust-local-778",
                "customer_name": "Zainab Musa",
                "customer_email": "zainab@example.com",
                "customer_phone": "08055566677",
                "delivery_address": "50 Aminu Kano Cres, Wuse II, Abuja",
                "items_summary": "1x Designer Handbag",
                "total_amount": 64000.0,
                "payment_method": "OPAY",
                "payment_account": "OPay ODIYA",
                "payment_reference": "OPAY-778",
                "sender_name": "Zainab Musa",
                "status": "DELIVERED",
                "verification_note": "Delivered & signed at reception",
                "status_updates_json": "PENDING_VERIFICATION|${now - 5000000L}|Order submitted;DELIVERED|$now|Delivered & signed at reception",
                "dispatch_rider_whatsapp": "+2348091234567",
                "created_at": ${now - 5000000L}
              }
            ]
        """.trimIndent()

        viewModel.openOrderTracking("ORD-7788990")
        repeat(25) {
            if (!viewModel.orderTrackingState.value.isLoading &&
                viewModel.orderTrackingState.value.searchedOrders.isNotEmpty()
            ) return@repeat
            pumpLooper()
        }

        assertEquals(AppScreen.AUTH, viewModel.currentScreen.value)
        assertEquals(CustomerAccountDestination.ORDER_TRACKING, viewModel.customerAccountDestination.value)

        val state = viewModel.orderTrackingState.value
        assertTrue(state.isLiveSynced)
        assertTrue(state.hasActiveSearch)
        assertEquals(1, state.searchedOrders.size)
        val pulledOrder = state.searchedOrders.first()
        assertEquals(OrderStatus.DELIVERED, pulledOrder.status)
        assertEquals("Delivered & signed at reception", pulledOrder.verificationNote)
        assertEquals("+2348091234567", pulledOrder.dispatchRiderWhatsapp)

        val inRoom = viewModel.allOrders.value.find { it.orderId == "ORD-7788990" }
        assertEquals(OrderStatus.DELIVERED, inRoom?.status)
    }
}
