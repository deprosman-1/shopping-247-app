package com.example

import android.app.Application
import android.os.Looper
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.OfflineActionType
import com.example.data.model.PreOrderRequest
import com.example.data.model.PreOrderStatus
import com.example.data.model.SupabaseConfig
import com.example.data.remote.SupabaseSyncService
import com.sun.net.httpserver.HttpServer
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
class PreOrderSyncAndQueueTest {

    private lateinit var app: Application
    private lateinit var viewModel: MainViewModel
    private lateinit var server: HttpServer
    private var serverPort: Int = 0
    private val capturedTablePosts = CopyOnWriteArrayList<String>()
    private var tableGetStatus: Int = 200
    private var tableGetBody: String = "[]"
    private var tablePostStatus: Int = 201
    private var tablePostBody: String = "[]"
    private var storageGetStatus: Int = 404
    private var storagePostStatus: Int = 404

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

        server = HttpServer.create(InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0)
        serverPort = server.address.port
        server.createContext("/rest/v1/pre_orders") { exchange ->
            val method = exchange.requestMethod.uppercase()
            val reqBody = exchange.requestBody.bufferedReader().use { it.readText() }
            if (method == "POST") {
                capturedTablePosts.add(reqBody)
                val out = tablePostBody.toByteArray(Charsets.UTF_8)
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(tablePostStatus, out.size.toLong())
                exchange.responseBody.use { it.write(out) }
            } else {
                val out = tableGetBody.toByteArray(Charsets.UTF_8)
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(tableGetStatus, out.size.toLong())
                exchange.responseBody.use { it.write(out) }
            }
        }
        server.createContext("/rest/v1/products") { exchange ->
            val out = "[]".toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, out.size.toLong())
            exchange.responseBody.use { it.write(out) }
        }
        server.createContext("/rest/v1/customers") { exchange ->
            val out = "[]".toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, out.size.toLong())
            exchange.responseBody.use { it.write(out) }
        }
        server.createContext("/rest/v1/orders") { exchange ->
            val out = "[]".toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, out.size.toLong())
            exchange.responseBody.use { it.write(out) }
        }
        server.createContext("/storage/v1/object") { exchange ->
            val method = exchange.requestMethod.uppercase()
            val status = if (method == "GET") storageGetStatus else storagePostStatus
            val msg = """{"error":"not_found"}""".toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(status, msg.size.toLong())
            exchange.responseBody.use { it.write(msg) }
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
    fun syncPreOrdersWithCloud_postsToPreOrdersTableAndMergesRemoteRequestsForAdmin() = runBlocking {
        val now = System.currentTimeMillis()
        tableGetBody = """
            [
              {
                "id": "REQ-REM01",
                "customer_id": "cust-remote",
                "customer_name": "Ngozi Eze",
                "customer_phone": "+2348099112233",
                "product_name": "Gold Designer Watch",
                "brand_name": "Rolex",
                "category": "Jewelry",
                "preferred_size": "Standard",
                "preferred_color": "Gold",
                "target_budget": 120000.0,
                "description": "Need gold bezel",
                "photo_url": "",
                "status": "PENDING",
                "created_at": ${now - 10000},
                "admin_response": "",
                "admin_quoted_price": 0.0,
                "customer_reply": ""
              }
            ]
        """.trimIndent()

        val localReq = PreOrderRequest(
            id = "REQ-LOC02",
            customerId = "cust-local",
            customerName = "Musa Abdullahi",
            customerPhone = "+2348033445566",
            productName = "Italian Leather Loafers",
            brandName = "Gucci",
            category = "Shoes",
            preferredSize = "43",
            preferredColor = "Black",
            targetBudget = 65000.0,
            description = "Size 43 black suede",
            status = PreOrderStatus.PENDING,
            createdAt = now
        )

        val syncService = SupabaseSyncService(
            SupabaseConfig(
                url = "http://127.0.0.1:$serverPort",
                anonKey = "sb_publishable_test_key_odiya",
                isConnected = true
            )
        )

        val res = syncService.syncPreOrdersWithCloud(
            localList = listOf(localReq),
            preferLocalIds = setOf("REQ-LOC02")
        )

        assertTrue(res.isSuccess)
        val merged = res.getOrThrow()
        assertEquals(2, merged.size)
        assertTrue(merged.any { it.id == "REQ-REM01" && it.productName == "Gold Designer Watch" })
        assertTrue(merged.any { it.id == "REQ-LOC02" && it.productName == "Italian Leather Loafers" })
        assertTrue(capturedTablePosts.any { it.contains("REQ-LOC02") && it.contains("Italian Leather Loafers") })
    }

    @Test
    fun syncPreOrdersWithCloud_returnsFailureWhenTableAndStorageBothRejectWrites() = runBlocking {
        tableGetStatus = 404
        tableGetBody = """{"code":"PGRST205","message":"Could not find table public.pre_orders"}"""
        tablePostStatus = 404
        tablePostBody = """{"code":"PGRST205","message":"Could not find table public.pre_orders"}"""
        storageGetStatus = 404
        storagePostStatus = 404

        val localReq = PreOrderRequest(
            id = "REQ-FAIL01",
            customerId = "cust-fail",
            customerName = "Test Buyer",
            customerPhone = "+2348000000001",
            productName = "Custom Bag",
            brandName = "Hermes"
        )

        val syncService = SupabaseSyncService(
            SupabaseConfig(
                url = "http://127.0.0.1:$serverPort",
                anonKey = "sb_publishable_test_key_odiya",
                isConnected = true
            )
        )

        val res = syncService.syncPreOrdersWithCloud(listOf(localReq), preferLocalIds = setOf("REQ-FAIL01"))
        assertTrue(res.isFailure)
    }

    @Test
    fun submitPreOrderRequest_enqueuesOfflineSyncWhenCloudUnconfiguredAndDeliversOnceConnected() = runBlocking {
        val ok = viewModel.submitPreOrderRequest(
            productName = "Custom Ankara Agbada",
            brandName = "Odiya Bespoke",
            category = "Clothes",
            preferredSize = "XL",
            preferredColor = "Emerald Green",
            targetBudget = 55000.0,
            customerNameOverride = "Seyi Ogunleye",
            customerPhoneOverride = "+2348077889900"
        )
        assertTrue(ok)
        pumpLooper()

        assertTrue(viewModel.preOrderRequests.value.any { it.productName == "Custom Ankara Agbada" })
        assertTrue(
            "Pre-order submitted while Supabase is unconfigured must be queued for auto-sync",
            viewModel.offlineQueue.value.any { it.type == OfflineActionType.PREORDER_SYNC }
        )

        // Now connect Supabase config and flush offline queue -> reaches Admin server endpoint
        viewModel.repository.supabaseSync.updateConfig(
            SupabaseConfig(
                url = "http://127.0.0.1:$serverPort",
                anonKey = "sb_publishable_test_key_odiya",
                isConnected = true
            )
        )

        var flushed = 0
        viewModel.triggerOfflineQueueSync { flushed = it }
        pumpLooper()

        assertTrue(flushed >= 1)
        assertTrue(capturedTablePosts.any { it.contains("Custom Ankara Agbada") && it.contains("Seyi Ogunleye") })
    }

    @Test
    fun syncPreOrdersWithCloud_fallsBackToSingleRowUpsertWhenBatchFails() = runBlocking {
        // Reject batch array payloads, accept single-object row upsert
        val customServer = HttpServer.create(InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0)
        val port = customServer.address.port
        val singleRowBodies = CopyOnWriteArrayList<String>()
        customServer.createContext("/rest/v1/pre_orders") { exchange ->
            val method = exchange.requestMethod.uppercase()
            val body = exchange.requestBody.bufferedReader().use { it.readText() }
            if (method == "POST") {
                if (body.trimStart().startsWith("[")) {
                    val err = """{"message":"batch payload rejected"}""".toByteArray(Charsets.UTF_8)
                    exchange.responseHeaders.add("Content-Type", "application/json")
                    exchange.sendResponseHeaders(400, err.size.toLong())
                    exchange.responseBody.use { it.write(err) }
                } else {
                    singleRowBodies.add(body)
                    val ok = "[]".toByteArray(Charsets.UTF_8)
                    exchange.responseHeaders.add("Content-Type", "application/json")
                    exchange.sendResponseHeaders(201, ok.size.toLong())
                    exchange.responseBody.use { it.write(ok) }
                }
            } else {
                val ok = "[]".toByteArray(Charsets.UTF_8)
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(200, ok.size.toLong())
                exchange.responseBody.use { it.write(ok) }
            }
        }
        customServer.createContext("/storage/v1/object") { exchange ->
            val err = """{"error":"not_found"}""".toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(404, err.size.toLong())
            exchange.responseBody.use { it.write(err) }
        }
        customServer.start()
        try {
            val syncService = SupabaseSyncService(
                SupabaseConfig(
                    url = "http://127.0.0.1:$port",
                    anonKey = "sb_publishable_test_key_odiya",
                    isConnected = true
                )
            )
            val req = PreOrderRequest(
                id = "REQ-FALLBACK01",
                customerId = "cust-fb",
                customerName = "Adaeze Obi",
                customerPhone = "+2348055566677",
                productName = "Bespoke Lace Dress",
                brandName = "Odiya Couture",
                targetBudget = Double.NaN
            )
            val res = syncService.syncPreOrdersWithCloud(listOf(req), preferLocalIds = setOf("REQ-FALLBACK01"))
            assertTrue("Expected success, got ${res.exceptionOrNull()?.message}", res.isSuccess)
            assertTrue(
                singleRowBodies.any {
                    it.contains("REQ-FALLBACK01") &&
                        it.contains("Adaeze Obi") &&
                        (it.contains("\"target_budget\":0") || it.contains("\"target_budget\":0.0"))
                }
            )
        } finally {
            customServer.stop(0)
        }
    }

    @Test
    fun adminSyncPreOrdersNow_pullsRemoteCustomerPreOrderIntoAdminState() = runBlocking {
        val now = System.currentTimeMillis()
        tableGetBody = """
            [
              {
                "id": "REQ-WEB99",
                "customer_id": "web-guest",
                "customer_name": "Chidi Okonkwo",
                "customer_phone": "+2348011223344",
                "product_name": "Limited Velvet Loafers",
                "brand_name": "Prada",
                "category": "Shoes",
                "preferred_size": "44",
                "preferred_color": "Black",
                "target_budget": 95000.0,
                "description": "From Web Storefront",
                "photo_url": "",
                "status": "PENDING",
                "created_at": $now,
                "admin_response": "",
                "admin_quoted_price": 0.0,
                "customer_reply": ""
              }
            ]
        """.trimIndent()

        assertTrue(viewModel.verifyAdminPassword(viewModel.adminPassword.value))
        viewModel.repository.supabaseSync.updateConfig(
            SupabaseConfig(
                url = "http://127.0.0.1:$serverPort",
                anonKey = "sb_publishable_test_key_odiya",
                isConnected = true
            )
        )

        val latch = java.util.concurrent.CountDownLatch(1)
        var syncOk = false
        viewModel.syncPreOrdersNow { ok, _ ->
            syncOk = ok
            latch.countDown()
        }
        repeat(40) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            if (latch.count == 0L) return@repeat
            delay(50)
        }

        assertTrue(syncOk)
        assertTrue(viewModel.preOrderRequests.value.any { it.id == "REQ-WEB99" && it.customerName == "Chidi Okonkwo" })
    }
}
