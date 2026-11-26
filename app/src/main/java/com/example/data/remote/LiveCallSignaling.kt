package com.example.data.remote

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

enum class LiveCallMode(val label: String) {
    CHAT("Live Chat"),
    AUDIO("Audio Talk"),
    VIDEO("Video Call");

    companion object {
        fun fromWire(raw: String, fallback: LiveCallMode = AUDIO): LiveCallMode {
            return entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) } ?: fallback
        }
    }
}

enum class LiveCallPhase(val label: String) {
    IDLE("Ready"),
    RINGING_OUTGOING("Ringing Admin..."),
    INCOMING_RING("Incoming Call"),
    CONNECTING("Connecting Media..."),
    CONNECTED("Live Connected"),
    RECONNECTING("Reconnecting..."),
    ENDED("Call Ended")
}

data class LiveCallRequest(
    val roomId: String,
    val callerName: String,
    val callerEmail: String = "",
    val callerPhone: String = "",
    val mode: LiveCallMode = LiveCallMode.AUDIO,
    val topic: String = "General Inquiry",
    val createdAt: Long = System.currentTimeMillis()
)

data class LiveCallMessage(
    val id: String,
    val senderName: String,
    val isFromAdmin: Boolean,
    val text: String,
    val attachedTag: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class LiveCallUiState(
    val phase: LiveCallPhase = LiveCallPhase.IDLE,
    val mode: LiveCallMode = LiveCallMode.AUDIO,
    val roomId: String = "",
    val peerName: String = "Admin Desk",
    val topic: String = "Product & Order Inquiry",
    val isAdminRole: Boolean = false,
    val isCallerInitiator: Boolean = true,
    val isMicMuted: Boolean = false,
    val isSpeakerOn: Boolean = true,
    val isUsingFrontCamera: Boolean = true,
    val localAudioLevel: Int = 0,
    val remoteAudioLevel: Int = 0,
    val elapsedSeconds: Int = 0,
    val adminOnline: Boolean = false,
    val isRealtimeConnected: Boolean = false,
    val incomingRequests: List<LiveCallRequest> = emptyList(),
    val chatMessages: List<LiveCallMessage> = emptyList(),
    val statusBanner: String = ""
)

data class LiveCallSignalEvent(
    val type: String,
    val roomId: String,
    val senderId: String,
    val payload: JSONObject
)

data class CustomerOnlinePresence(
    val sessionId: String,
    val customerId: String = "",
    val customerName: String = "Customer",
    val customerPhone: String = "",
    val currentScreen: String = "HOME",
    val isOnline: Boolean = true,
    val lastSeenAt: Long = System.currentTimeMillis()
)

data class LiveCallLobbySnapshot(
    val adminOnline: Boolean = false,
    val updatedAt: Long = 0L,
    val requests: List<LiveCallRequest> = emptyList()
)

class LiveCallSignalingClient(
    private val scope: CoroutineScope,
    private val cleanUrlFn: (String) -> String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private val refCounter = AtomicInteger(1)
    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null

    private var currentUrl: String = ""
    private var currentKey: String = ""
    private var currentSenderId: String = ""

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _events = MutableSharedFlow<LiveCallSignalEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<LiveCallSignalEvent> = _events.asSharedFlow()

    fun connect(supabaseUrl: String, anonKey: String, senderId: String) {
        val cleanUrl = cleanUrlFn(supabaseUrl)
        val cleanKey = anonKey.trim()
        if (cleanUrl.isBlank() || cleanKey.isBlank() || cleanUrl.contains("YOUR_SUPABASE", true)) {
            return
        }
        if (cleanUrl == currentUrl && cleanKey == currentKey && webSocket != null && _isConnected.value) {
            return
        }
        currentUrl = cleanUrl
        currentKey = cleanKey
        currentSenderId = senderId
        openSocket()
    }

    private fun openSocket() {
        reconnectJob?.cancel()
        heartbeatJob?.cancel()
        try {
            webSocket?.close(1000, "reconnect")
        } catch (_: Exception) {
        }
        webSocket = null

        val wsBase = currentUrl
            .replaceFirst("https://", "wss://", ignoreCase = true)
            .replaceFirst("http://", "ws://", ignoreCase = true)
            .trimEnd('/')
        val wsUrl = "$wsBase/realtime/v1/websocket?apikey=$currentKey&vsn=1.0.0"

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("LiveCallSignaling", "Supabase Realtime WebSocket connected")
                _isConnected.value = true
                val joinMsg = JSONObject().apply {
                    put("topic", "realtime:depros_live_calls")
                    put("event", "phx_join")
                    put("payload", JSONObject().apply {
                        put("config", JSONObject().apply {
                            put("broadcast", JSONObject().apply {
                                put("self", false)
                                put("ack", false)
                            })
                        })
                    })
                    put("ref", refCounter.getAndIncrement().toString())
                }.toString()
                webSocket.send(joinMsg)
                startHeartbeat()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val root = JSONObject(text)
                    val event = root.optString("event")
                    if (event == "broadcast") {
                        val outerPayload = root.optJSONObject("payload") ?: return
                        val innerPayload = outerPayload.optJSONObject("payload") ?: outerPayload
                        val signalType = innerPayload.optString("type").ifBlank { outerPayload.optString("event") }
                        val roomId = innerPayload.optString("roomId")
                        val sender = innerPayload.optString("senderId")
                        if (sender.isNotBlank() && sender == currentSenderId) return
                        if (signalType.isNotBlank()) {
                            _events.tryEmit(
                                LiveCallSignalEvent(
                                    type = signalType,
                                    roomId = roomId,
                                    senderId = sender,
                                    payload = innerPayload
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.w("LiveCallSignaling", "Parse WS message failed: ${e.message}")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w("LiveCallSignaling", "WS failure: ${t.message}")
                _isConnected.value = false
                scheduleReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _isConnected.value = false
                if (code != 1000) {
                    scheduleReconnect()
                }
            }
        })
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch(Dispatchers.IO) {
            while (isActive && _isConnected.value) {
                delay(24_000L)
                try {
                    val hb = JSONObject().apply {
                        put("topic", "phoenix")
                        put("event", "heartbeat")
                        put("payload", JSONObject())
                        put("ref", refCounter.getAndIncrement().toString())
                    }.toString()
                    webSocket?.send(hb)
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun scheduleReconnect() {
        if (currentUrl.isBlank() || currentKey.isBlank()) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch(Dispatchers.IO) {
            delay(3_500L)
            if (isActive && !_isConnected.value) {
                openSocket()
            }
        }
    }

    fun sendSignal(type: String, roomId: String, extra: JSONObject = JSONObject()): Boolean {
        val ws = webSocket ?: return false
        val bodyPayload = JSONObject(extra.toString()).apply {
            put("type", type)
            put("roomId", roomId)
            put("senderId", currentSenderId)
            put("ts", System.currentTimeMillis())
        }
        val frame = JSONObject().apply {
            put("topic", "realtime:depros_live_calls")
            put("event", "broadcast")
            put("payload", JSONObject().apply {
                put("type", "broadcast")
                put("event", "call_signal")
                put("payload", bodyPayload)
            })
            put("ref", refCounter.getAndIncrement().toString())
        }.toString()
        return ws.send(frame)
    }

    suspend fun publishLobbySnapshot(
        supabaseUrl: String,
        anonKey: String,
        adminOnline: Boolean,
        requests: List<LiveCallRequest>
    ) = withContext(Dispatchers.IO) {
        val baseUrl = cleanUrlFn(supabaseUrl)
        val cleanKey = anonKey.trim()
        if (baseUrl.isBlank() || cleanKey.isBlank()) return@withContext
        val now = System.currentTimeMillis()
        val freshRequests = requests.filter { now - it.createdAt <= 180_000L }.take(12)
        val json = JSONObject().apply {
            put("adminOnline", adminOnline)
            put("updatedAt", now)
            put("requests", JSONArray().apply {
                freshRequests.forEach { req ->
                    put(JSONObject().apply {
                        put("roomId", req.roomId)
                        put("callerName", req.callerName)
                        put("callerEmail", req.callerEmail)
                        put("callerPhone", req.callerPhone)
                        put("mode", req.mode.name)
                        put("topic", req.topic)
                        put("createdAt", req.createdAt)
                    })
                }
            })
        }.toString()

        val bytes = json.toByteArray(Charsets.UTF_8)
        val mediaType = "application/json".toMediaType()
        for (bucket in listOf("product-media", "shop-receipts")) {
            try {
                val req = Request.Builder()
                    .url("$baseUrl/storage/v1/object/$bucket/system/live_calls_lobby.json")
                    .header("apikey", cleanKey)
                    .header("Authorization", "Bearer $cleanKey")
                    .header("Content-Type", "application/json")
                    .header("x-upsert", "true")
                    .post(bytes.toRequestBody(mediaType))
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful || resp.code in 200..201) return@withContext
                }
            } catch (_: Exception) {
            }
        }
    }

    suspend fun fetchLobbySnapshot(
        supabaseUrl: String,
        anonKey: String
    ): LiveCallLobbySnapshot? = withContext(Dispatchers.IO) {
        val baseUrl = cleanUrlFn(supabaseUrl)
        val cleanKey = anonKey.trim()
        if (baseUrl.isBlank() || cleanKey.isBlank()) return@withContext null
        val ts = System.currentTimeMillis()
        for (bucket in listOf("product-media", "shop-receipts")) {
            try {
                val req = Request.Builder()
                    .url("$baseUrl/storage/v1/object/public/$bucket/system/live_calls_lobby.json?t=$ts")
                    .header("apikey", cleanKey)
                    .get()
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val raw = resp.body?.string()?.trim().orEmpty()
                        if (raw.startsWith("{")) {
                            val obj = JSONObject(raw)
                            val updatedAt = obj.optLong("updatedAt", 0L)
                            val onlineFlag = obj.optBoolean("adminOnline", false) && (ts - updatedAt <= 90_000L)
                            val arr = obj.optJSONArray("requests") ?: JSONArray()
                            val list = mutableListOf<LiveCallRequest>()
                            for (i in 0 until arr.length()) {
                                val item = arr.optJSONObject(i) ?: continue
                                val created = item.optLong("createdAt", 0L)
                                if (ts - created <= 120_000L) {
                                    val modeStr = item.optString("mode", "AUDIO")
                                    val mode = LiveCallMode.fromWire(modeStr, LiveCallMode.AUDIO)
                                    list.add(
                                        LiveCallRequest(
                                            roomId = item.optString("roomId"),
                                            callerName = item.optString("callerName", "Customer"),
                                            callerEmail = item.optString("callerEmail", ""),
                                            callerPhone = item.optString("callerPhone", ""),
                                            mode = mode,
                                            topic = item.optString("topic", "Live Call"),
                                            createdAt = created
                                        )
                                    )
                                }
                            }
                            return@withContext LiveCallLobbySnapshot(
                                adminOnline = onlineFlag,
                                updatedAt = updatedAt,
                                requests = list.filter { it.roomId.isNotBlank() }
                            )
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
        null
    }

    suspend fun fetchOnlineCustomersPresence(
        supabaseUrl: String,
        anonKey: String,
        ttlMillis: Long = 90_000L
    ): List<CustomerOnlinePresence> = withContext(Dispatchers.IO) {
        val baseUrl = cleanUrlFn(supabaseUrl)
        val cleanKey = anonKey.trim()
        if (baseUrl.isBlank() || cleanKey.isBlank()) return@withContext emptyList()
        val ts = System.currentTimeMillis()
        for (bucket in listOf("product-media", "shop-receipts")) {
            try {
                val req = Request.Builder()
                    .url("$baseUrl/storage/v1/object/public/$bucket/system/online_customers_presence.json?t=$ts")
                    .header("apikey", cleanKey)
                    .get()
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val raw = resp.body?.string()?.trim().orEmpty()
                        if (raw.startsWith("{")) {
                            val obj = JSONObject(raw)
                            val arr = obj.optJSONArray("customers") ?: JSONArray()
                            val list = mutableListOf<CustomerOnlinePresence>()
                            for (i in 0 until arr.length()) {
                                val item = arr.optJSONObject(i) ?: continue
                                val lastSeen = item.optLong("lastSeenAt", 0L)
                                val isOnlineFlag = item.optBoolean("isOnline", true)
                                val sessId = item.optString("sessionId").trim()
                                if (isOnlineFlag && sessId.isNotBlank() && ts - lastSeen <= ttlMillis) {
                                    list.add(
                                        CustomerOnlinePresence(
                                            sessionId = sessId,
                                            customerId = item.optString("customerId", ""),
                                            customerName = item.optString("customerName", "Customer"),
                                            customerPhone = item.optString("customerPhone", ""),
                                            currentScreen = item.optString("currentScreen", "HOME"),
                                            isOnline = true,
                                            lastSeenAt = lastSeen
                                        )
                                    )
                                }
                            }
                            return@withContext list.sortedByDescending { it.lastSeenAt }
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
        emptyList()
    }

    suspend fun publishCustomerPresenceSnapshot(
        supabaseUrl: String,
        anonKey: String,
        presence: CustomerOnlinePresence,
        ttlMillis: Long = 90_000L
    ) = withContext(Dispatchers.IO) {
        val baseUrl = cleanUrlFn(supabaseUrl)
        val cleanKey = anonKey.trim()
        if (baseUrl.isBlank() || cleanKey.isBlank()) return@withContext
        val now = System.currentTimeMillis()
        val existing = try {
            fetchOnlineCustomersPresence(baseUrl, cleanKey, ttlMillis)
        } catch (_: Exception) {
            emptyList()
        }
        val merged = buildList {
            existing.filterNot { it.sessionId == presence.sessionId }.forEach {
                if (it.isOnline && now - it.lastSeenAt <= ttlMillis) add(it)
            }
            if (presence.isOnline) {
                add(presence.copy(lastSeenAt = now))
            }
        }.sortedByDescending { it.lastSeenAt }.take(100)

        val json = JSONObject().apply {
            put("updatedAt", now)
            put("customers", JSONArray().apply {
                merged.forEach { c ->
                    put(JSONObject().apply {
                        put("sessionId", c.sessionId)
                        put("customerId", c.customerId)
                        put("customerName", c.customerName)
                        put("customerPhone", c.customerPhone)
                        put("currentScreen", c.currentScreen)
                        put("isOnline", c.isOnline)
                        put("lastSeenAt", c.lastSeenAt)
                    })
                }
            })
        }.toString()

        val bytes = json.toByteArray(Charsets.UTF_8)
        val mediaType = "application/json".toMediaType()
        for (bucket in listOf("product-media", "shop-receipts")) {
            try {
                val req = Request.Builder()
                    .url("$baseUrl/storage/v1/object/$bucket/system/online_customers_presence.json")
                    .header("apikey", cleanKey)
                    .header("Authorization", "Bearer $cleanKey")
                    .header("Content-Type", "application/json")
                    .header("x-upsert", "true")
                    .post(bytes.toRequestBody(mediaType))
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful || resp.code in 200..201) return@withContext
                }
            } catch (_: Exception) {
            }
        }
    }

    fun disconnect() {
        reconnectJob?.cancel()
        heartbeatJob?.cancel()
        _isConnected.value = false
        try {
            webSocket?.close(1000, "closed")
        } catch (_: Exception) {
        }
        webSocket = null
    }
}
