package com.example.data.remote

import android.util.Log
import com.example.data.model.AppUpdateManifest
import com.example.data.model.Customer
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.data.model.SupabaseConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Dns
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class SupabaseSyncService(
    private var config: SupabaseConfig = SupabaseConfig()
) {
    private val dohCache = ConcurrentHashMap<String, List<InetAddress>>()

    private val dohClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private fun resolveViaDoh(hostname: String): List<InetAddress> {
        dohCache[hostname]?.let { if (it.isNotEmpty()) return it }
        val endpoints = listOf(
            "https://8.8.8.8/resolve?name=$hostname&type=A",
            "https://1.1.1.1/dns-query?name=$hostname&type=A",
            "https://dns.google/resolve?name=$hostname&type=A"
        )
        for (url in endpoints) {
            try {
                val req = Request.Builder()
                    .url(url)
                    .header("Accept", "application/dns-json")
                    .get()
                    .build()
                dohClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) continue
                    val body = resp.body?.string() ?: continue
                    val json = JSONObject(body)
                    val answers = json.optJSONArray("Answer") ?: continue
                    val addrs = mutableListOf<InetAddress>()
                    for (i in 0 until answers.length()) {
                        val ans = answers.getJSONObject(i)
                        if (ans.optInt("type") == 1) {
                            val ip = ans.optString("data").trim()
                            if (ip.isNotBlank()) {
                                addrs.add(InetAddress.getByName(ip))
                            }
                        }
                    }
                    if (addrs.isNotEmpty()) {
                        dohCache[hostname] = addrs
                        return addrs
                    }
                }
            } catch (e: Exception) {
                Log.w("SupabaseSyncService", "DoH lookup failed for $hostname via $url: ${e.message}")
            }
        }
        throw UnknownHostException("Unable to resolve host '$hostname' (System DNS & DoH lookup failed)")
    }

    private val smartDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            return try {
                Dns.SYSTEM.lookup(hostname)
            } catch (e: UnknownHostException) {
                Log.w("SupabaseSyncService", "System DNS failed for $hostname, trying DoH fallback...")
                resolveViaDoh(hostname)
            }
        }
    }

    private val client = OkHttpClient.Builder()
        .dns(smartDns)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    fun updateConfig(newConfig: SupabaseConfig) {
        val cleaned = newConfig.copy(
            url = if (newConfig.url.isBlank()) "" else cleanUrl(newConfig.url),
            anonKey = sanitizeKey(newConfig.anonKey)
        )
        this.config = cleaned
    }

    fun getConfig(): SupabaseConfig = config

    fun sanitizeKey(rawKey: String): String {
        return rawKey
            .replace("\u200B", "")
            .replace("\uFEFF", "")
            .trim()
            .trim('"', '\'', '`')
            .replace("\\s+".toRegex(), "")
    }

    fun isPlaceholderConfig(url: String = config.url, key: String = config.anonKey): Boolean {
        val u = url.trim().lowercase()
        val k = key.trim().lowercase()
        return u.contains("gjs6srxqc2prdx42l343un.supabase.co") ||
            u.contains("your-project") ||
            k.contains("depros_supabase_public_anon_key") ||
            k.contains("dummy") ||
            u.isBlank() ||
            k.isBlank()
    }

    private fun extractKeyType(key: String): String {
        return when {
            key.startsWith("sb_publishable_") -> "Publishable Key"
            key.startsWith("sb_secret_") -> "Secret Key (UNSAFE)"
            key.startsWith("eyJ") && key.count { it == '.' } == 2 -> "Legacy Anon JWT"
            else -> "Custom Key"
        }
    }

    private fun extractJwtProjectRef(jwt: String): String? {
        return try {
            val parts = jwt.split(".")
            if (parts.size != 3) return null
            val decoded = android.util.Base64.decode(
                parts[1],
                android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
            )
            val json = JSONObject(String(decoded, Charsets.UTF_8))
            json.optString("ref").takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    private fun validateCredentialsBeforeRequest(): String? {
        val u = cleanUrl(config.url)
        val k = sanitizeKey(config.anonKey)
        if (u.isBlank()) {
            return "Supabase Project URL is empty. Copy it from Supabase Dashboard -> Project Settings -> Data API (https://xxxx.supabase.co)."
        }
        if (k.isBlank()) {
            return "Supabase API Key is empty. Copy your Publishable Key (sb_publishable_...) or Legacy anon key (eyJ...) from Project Settings -> API Keys."
        }
        if (isPlaceholderConfig(u, k)) {
            return "Placeholder credentials detected. Clear and paste your real Supabase Project URL (https://xxxx.supabase.co) & Publishable/Anon Key."
        }
        if (k.startsWith("sb_secret_")) {
            return "You entered a Secret Key (sb_secret_...). Supabase blocks Secret keys in client apps! Use the Publishable key (sb_publishable_...) or Legacy anon public key instead."
        }
        val host = try { java.net.URI(u).host ?: "" } catch (_: Exception) { "" }
        if (host.isBlank() || !host.contains(".")) {
            return "Invalid Project URL '$u'. Expected format: https://yourprojectref.supabase.co"
        }
        if (k.startsWith("eyJ") && k.count { it == '.' } == 2) {
            val keyRef = extractJwtProjectRef(k)
            val urlRef = host.substringBefore(".")
            if (keyRef != null && urlRef.isNotBlank() && keyRef != urlRef && host.endsWith(".supabase.co")) {
                return "Project Mismatch: Your URL points to '$urlRef', but your Legacy JWT key belongs to project '$keyRef'. Make sure URL and Key are from the same Supabase project."
            }
        }
        return null
    }

    private fun Request.Builder.addSupabaseAuthHeaders(): Request.Builder {
        val cleanKey = sanitizeKey(config.anonKey)
        header("apikey", cleanKey)
        header("Accept", "application/json")
        // Legacy anon keys are 3-part JWTs (eyJ...); new sb_publishable_ keys must NOT be sent as Bearer JWTs
        if (cleanKey.startsWith("eyJ") && cleanKey.count { it == '.' } == 2) {
            header("Authorization", "Bearer $cleanKey")
        }
        return this
    }

    private fun JSONObject.optCleanString(key: String, default: String = ""): String {
        if (isNull(key)) return default
        val v = optString(key, default)
        return if (v == "null") default else v
    }

    private fun toIsoTimestamp(millis: Long): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return sdf.format(java.util.Date(millis))
    }

    private fun parseCreatedAtMillis(obj: JSONObject): Long {
        if (obj.isNull("created_at")) return 0L
        val rawNum = obj.optLong("created_at", -1L)
        if (rawNum > 0L) return rawNum
        val str = obj.optCleanString("created_at")
        if (str.isBlank()) return 0L
        str.toLongOrNull()?.let { if (it > 0L) return it }
        // Parse ISO-8601 strings such as 2026-11-14T08:40:00.123456+00:00 or ...Z
        val normalized = str
            .replace("Z", "+0000")
            .replace(Regex("([+-]\\d{2}):(\\d{2})$"), "$1$2")
            .replace(Regex("\\.(\\d{3})\\d+"), ".$1")
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss"
        )
        for (pattern in patterns) {
            try {
                val sdf = java.text.SimpleDateFormat(pattern, java.util.Locale.US)
                sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                val parsed = sdf.parse(normalized)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {}
        }
        return 0L
    }

    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val validationErr = validateCredentialsBeforeRequest()
            if (validationErr != null) {
                return@withContext Result.failure(Exception(validationErr))
            }

            val baseUrl = cleanUrl(config.url)
            val cleanKey = sanitizeKey(config.anonKey)
            val host = try { java.net.URI(baseUrl).host ?: baseUrl } catch (_: Exception) { baseUrl }
            val keyType = extractKeyType(cleanKey)
            val projectRef = host.substringBefore(".")

            fun probeTable(table: String, selectCol: String): Pair<Int, String> {
                val req = Request.Builder()
                    .url("$baseUrl/rest/v1/$table?select=$selectCol&limit=1")
                    .addSupabaseAuthHeaders()
                    .get()
                    .build()
                return client.newCall(req).execute().use { resp ->
                    resp.code to (resp.body?.string() ?: "")
                }
            }

            val (pCode, pBody) = probeTable("products", "id")
            if (pCode in 200..299) {
                val (cCode, cBody) = probeTable("customers", "id")
                val (oCode, oBody) = probeTable("orders", "order_id")
                val issues = mutableListOf<String>()
                if (cCode !in 200..299) {
                    issues.add("customers (HTTP $cCode: ${cBody.ifBlank { "missing/RLS blocked" }.take(80)})")
                }
                if (oCode !in 200..299) {
                    issues.add("orders (HTTP $oCode: ${oBody.ifBlank { "missing/RLS blocked" }.take(80)})")
                }
                return@withContext if (issues.isEmpty()) {
                    Result.success("✅ Connected to '$host' ($keyType)! 'products', 'customers' & 'orders' tables + RLS ready (HTTP $pCode).")
                } else {
                    Result.failure(
                        Exception("Host '$host' & API Key are valid, and 'products' is ready, BUT these tables need setup/RLS in project '$projectRef': ${issues.joinToString("; ")}. Tap 'Copy SQL' and run it in Supabase SQL Editor.")
                    )
                }
            }

            when {
                pCode == 404 || pBody.contains("PGRST205") || pBody.contains("42P01") || pBody.contains("does not exist", ignoreCase = true) -> {
                    val singularFound = try { probeTable("product", "id").first in 200..299 } catch (_: Exception) { false }
                    val customersFound = try { probeTable("customers", "id").first in 200..299 } catch (_: Exception) { false }

                    val extraHint = when {
                        singularFound -> "Found singular 'product' table instead of 'products'. Tap 'Copy SQL' and run it in SQL Editor for project '$projectRef'."
                        customersFound -> "'customers' exists, so only 'products' is missing or needs schema reload in '$projectRef'."
                        else -> "None of the tables were visible on project '$projectRef'. Confirm: (1) SQL ran inside project '$projectRef' (check top-left switcher), (2) SQL Editor said 'Success', (3) Data API -> Exposed schemas includes 'public', (4) Click Reload schema cache."
                    }
                    Result.failure(
                        Exception("Table 'products' missing on '$host' (HTTP $pCode): $extraHint | Raw: ${pBody.take(140)}")
                    )
                }
                pBody.contains("42501") || pBody.contains("row-level security", ignoreCase = true) -> {
                    Result.failure(
                        Exception("Host '$host' & API Key ($keyType) are VALID, but RLS blocked reading 'products' (HTTP $pCode). Tap 'Copy SQL' and run the RLS policy script.")
                    )
                }
                pCode == 401 || pCode == 403 -> {
                    Result.failure(
                        Exception("Host '$host' reachable, but API Key ($keyType) was rejected (HTTP $pCode). Verify you copied the Publishable (sb_publishable_...) or Legacy anon public key from Project Settings -> API. ${pBody.take(180)}")
                    )
                }
                else -> {
                    Result.failure(
                        Exception("Table check HTTP $pCode: ${pBody.take(200)}")
                    )
                }
            }
        } catch (e: java.net.UnknownHostException) {
            val host = try { java.net.URI(cleanUrl(config.url)).host ?: config.url } catch (_: Exception) { config.url }
            Result.failure(Exception("Unable to resolve host '$host'. Check your internet connection & verify your Project URL in Supabase -> Settings -> Data API."))
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(Exception("Connection timed out reaching Supabase. Check internet access or retry in a few seconds."))
        } catch (e: Exception) {
            Log.e("SupabaseSyncService", "Connection test error", e)
            Result.failure(Exception(e.message ?: "Network error connecting to Supabase"))
        }
    }

    private fun productToJson(product: Product, includeExtendedCols: Boolean = true): JSONObject = JSONObject().apply {
        put("id", product.id)
        put("title", product.title)
        put("category", product.category)
        put("brand", product.brand)
        put("available_brands", product.availableBrands.joinToString(","))
        put("available_sizes", product.availableSizes.joinToString(","))
        put("base_price", product.basePrice)
        put("stock_quantity", product.stockQuantity)
        put("image_url", product.imageUrl)
        put("model_3d_type", product.model3dType)
        put("description", product.description)
        put("sku", product.sku)
        if (includeExtendedCols) {
            put("cost_price", product.costPrice)
            put("discount_percent", product.discountPercent)
            put("available_colors", product.availableColors.joinToString(";") { "${it.name}|${it.hex}" })
            put("expiry_months_remaining", product.expiryMonthsRemaining)
            if (product.partnerId.isNotBlank()) {
                put("partner_id", product.partnerId)
                put("partner_name", product.partnerName)
                put("partner_commission_percent", product.partnerCommissionPercent)
                put("partner_batch_id", product.partnerBatchId)
            }
        }
    }

    private fun formatSyncHttpError(code: Int, respBody: String, fallbackMsg: String): String {
        return when {
            respBody.contains("PGRST204") -> {
                val colMatch = Regex("Could not find the '([^']+)' column").find(respBody)
                val colName = colMatch?.groupValues?.getOrNull(1) ?: "unknown"
                "Schema mismatch: 'products' table is missing column '$colName' (PGRST204). Tap 'Copy SQL' and run it in Supabase SQL Editor to add missing columns safely."
            }
            code == 404 || respBody.contains("PGRST205") || respBody.contains("42P01") ->
                "Table 'products' not found on Supabase (HTTP $code): ${respBody.ifBlank { fallbackMsg }.take(160)}. Run 'Copy SQL' in the matching project's SQL Editor & reload schema cache."
            respBody.contains("42501") || (code == 403 && respBody.contains("row-level security", ignoreCase = true)) ->
                "Blocked by Row Level Security (RLS) policy (42501). Tap 'Copy SQL' and run it in Supabase SQL Editor to allow INSERT/UPDATE on 'products'."
            code == 401 || code == 403 ->
                "Unauthorized (HTTP $code): Check your Publishable/Anon Key and RLS policy. ${respBody.take(150)}"
            code == 400 ->
                "Bad Request (HTTP 400): ${respBody.ifBlank { fallbackMsg }.take(220)}"
            else ->
                "HTTP $code: ${respBody.ifBlank { fallbackMsg }.take(220)}"
        }
    }

    suspend fun syncProductsBatchToCloud(products: List<Product>): Result<Int> = withContext(Dispatchers.IO) {
        if (products.isEmpty()) {
            return@withContext Result.failure(Exception("No local products found to sync."))
        }
        val validationErr = validateCredentialsBeforeRequest()
        if (validationErr != null) {
            return@withContext Result.failure(Exception(validationErr))
        }
        try {
            var syncedCount = 0
            val baseUrl = cleanUrl(config.url)
            var useExtended = true
            val chunks = products.chunked(50)
            for (chunk in chunks) {
                fun postBatch(extended: Boolean): Pair<Int, String> {
                    val jsonArray = JSONArray()
                    chunk.forEach { jsonArray.put(productToJson(it, extended)) }
                    val body = jsonArray.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                    val url = "$baseUrl/rest/v1/products?on_conflict=id"
                    val request = Request.Builder()
                        .url(url)
                        .addSupabaseAuthHeaders()
                        .header("Content-Type", "application/json")
                        .header("Prefer", "resolution=merge-duplicates,return=minimal")
                        .post(body)
                        .build()
                    return client.newCall(request).execute().use { response ->
                        response.code to (response.body?.string() ?: "")
                    }
                }

                var (code, respBody) = postBatch(useExtended)
                if (code == 400 && useExtended && respBody.contains("PGRST204")) {
                    Log.w("SupabaseSyncService", "Extended product column missing in Supabase, retrying core columns: $respBody")
                    useExtended = false
                    val retry = postBatch(false)
                    code = retry.first
                    respBody = retry.second
                }

                if (code in 200..299) {
                    syncedCount += chunk.size
                } else {
                    Log.e("SupabaseSyncService", "Batch sync failed HTTP $code: $respBody")
                    return@withContext Result.failure(
                        Exception(formatSyncHttpError(code, respBody, "HTTP $code"))
                    )
                }
            }
            Result.success(syncedCount)
        } catch (e: java.net.UnknownHostException) {
            val host = try { java.net.URI(cleanUrl(config.url)).host ?: config.url } catch (_: Exception) { config.url }
            Result.failure(Exception("Unable to resolve host '$host'. Verify your Project URL and internet connection."))
        } catch (e: Exception) {
            Log.e("SupabaseSyncService", "Batch product sync exception", e)
            Result.failure(Exception(e.message ?: "Network error during sync"))
        }
    }

    suspend fun syncProductToCloud(product: Product): Result<Boolean> = withContext(Dispatchers.IO) {
        val validationErr = validateCredentialsBeforeRequest()
        if (validationErr != null) {
            return@withContext Result.failure(Exception(validationErr))
        }
        try {
            val baseUrl = cleanUrl(config.url)
            if (baseUrl.isBlank()) return@withContext Result.failure(Exception("Supabase URL is empty"))
            val url = "$baseUrl/rest/v1/products?on_conflict=id"

            fun postSingle(extended: Boolean): Pair<Int, String> {
                val body = productToJson(product, extended).toString()
                    .toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .addSupabaseAuthHeaders()
                    .header("Content-Type", "application/json")
                    .header("Prefer", "resolution=merge-duplicates,return=minimal")
                    .post(body)
                    .build()
                return client.newCall(request).execute().use { response ->
                    response.code to (response.body?.string() ?: "")
                }
            }

            var (code, respBody) = postSingle(true)
            if (code == 400 && respBody.contains("PGRST204")) {
                Log.w("SupabaseSyncService", "Retrying single product sync with core columns: $respBody")
                val retry = postSingle(false)
                code = retry.first
                respBody = retry.second
            }

            if (code in 200..299) {
                Result.success(true)
            } else {
                Log.e("SupabaseSyncService", "Sync failed HTTP $code: $respBody")
                Result.failure(Exception(formatSyncHttpError(code, respBody, "HTTP $code")))
            }
        } catch (e: Exception) {
            Log.e("SupabaseSyncService", "Product sync exception", e)
            Result.failure(e)
        }
    }

    suspend fun deleteProductFromCloud(productId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (isPlaceholderConfig()) {
            return@withContext Result.failure(Exception("Supabase not configured"))
        }
        try {
            val baseUrl = cleanUrl(config.url)
            if (baseUrl.isBlank()) return@withContext Result.failure(Exception("Supabase URL is empty"))
            val url = "$baseUrl/rest/v1/products?id=eq.$productId"
            val request = Request.Builder()
                .url(url)
                .addSupabaseAuthHeaders()
                .delete()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 200 || response.code == 204) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("Supabase Delete Product HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildOrderJson(
        order: Order,
        includeExtendedCols: Boolean,
        useIsoTimestamp: Boolean
    ): JSONObject = JSONObject().apply {
        put("order_id", order.orderId)
        put("customer_id", order.customerId)
        put("customer_name", order.customerName)
        put("customer_email", order.customerEmail)
        put("customer_phone", order.customerPhone)
        put("delivery_address", order.deliveryAddress)
        put("items_summary", order.itemsSummary)
        put("total_amount", order.totalAmount)
        put("profit", order.profit)
        put("payment_method", order.paymentMethod.name)
        put("payment_account", order.paymentAccount)
        put("payment_reference", order.paymentReference)
        put("sender_name", order.senderName)
        put("status", order.status.name)
        put("created_at", if (useIsoTimestamp) toIsoTimestamp(order.createdAt) else order.createdAt)

        val receiptNoteSuffix = if (!includeExtendedCols && order.receiptImageUrl.startsWith("http", ignoreCase = true) && !order.verificationNote.contains(order.receiptImageUrl)) {
            " [Receipt: ${order.receiptImageUrl}]"
        } else ""
        put("verification_note", "${order.verificationNote}$receiptNoteSuffix".trim())

        if (includeExtendedCols) {
            put("receipt_image_url", order.receiptImageUrl)
            put("status_updates_json", order.statusUpdates.joinToString(";") { "${it.status.name}|${it.timestamp}|${it.note}" })
            put("dispatch_rider_whatsapp", order.dispatchRiderWhatsapp)
            if (order.partnerId.isNotBlank()) {
                put("partner_id", order.partnerId)
                put("partner_name", order.partnerName)
                put("partner_commission_amount", order.partnerCommissionAmount)
                put("partner_net_payout", order.partnerNetPayout)
            }
        }
    }

    suspend fun syncOrderToCloud(order: Order): Result<Boolean> = withContext(Dispatchers.IO) {
        val validationErr = validateCredentialsBeforeRequest()
        if (validationErr != null) {
            return@withContext Result.failure(Exception(validationErr))
        }
        try {
            val baseUrl = cleanUrl(config.url)
            if (baseUrl.isBlank()) return@withContext Result.failure(Exception("Supabase URL is empty"))
            val url = "$baseUrl/rest/v1/orders?on_conflict=order_id"

            val payloadObj = buildOrderJson(order, includeExtendedCols = true, useIsoTimestamp = false)
            var switchedToIso = false

            fun executeOrderPost(jsonObj: JSONObject): Pair<Int, String> {
                val body = jsonObj.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .addSupabaseAuthHeaders()
                    .header("Content-Type", "application/json")
                    .header("Prefer", "resolution=merge-duplicates,return=minimal")
                    .post(body)
                    .build()
                return client.newCall(request).execute().use { resp ->
                    resp.code to (resp.body?.string() ?: "")
                }
            }

            var (code, respBody) = executeOrderPost(payloadObj)
            var retries = 0
            while (code == 400 && retries < 10) {
                retries++
                val badTimestamp = !switchedToIso && (
                    respBody.contains("timestamp", ignoreCase = true) ||
                        respBody.contains("22007") ||
                        respBody.contains("22008")
                    )
                if (badTimestamp) {
                    switchedToIso = true
                    payloadObj.put("created_at", toIsoTimestamp(if (order.createdAt > 0L) order.createdAt else System.currentTimeMillis()))
                    val next = executeOrderPost(payloadObj)
                    code = next.first
                    respBody = next.second
                    continue
                }

                if (respBody.contains("PGRST204")) {
                    val colMatch = Regex("Could not find the '([^']+)' column").find(respBody)
                    val missingCol = colMatch?.groupValues?.getOrNull(1)
                    if (!missingCol.isNullOrBlank() && payloadObj.has(missingCol)) {
                        if (missingCol == "receipt_image_url" &&
                            order.receiptImageUrl.startsWith("http", ignoreCase = true) &&
                            payloadObj.has("verification_note")
                        ) {
                            val existingNote = payloadObj.optString("verification_note", "")
                            if (!existingNote.contains(order.receiptImageUrl)) {
                                payloadObj.put("verification_note", "$existingNote [Receipt: ${order.receiptImageUrl}]".trim())
                            }
                        }
                        payloadObj.remove(missingCol)
                        val next = executeOrderPost(payloadObj)
                        code = next.first
                        respBody = next.second
                        continue
                    } else {
                        // Fallback: strip all optional/extended columns
                        payloadObj.remove("receipt_image_url")
                        payloadObj.remove("status_updates_json")
                        payloadObj.remove("dispatch_rider_whatsapp")
                        payloadObj.remove("profit")
                        val next = executeOrderPost(payloadObj)
                        code = next.first
                        respBody = next.second
                        continue
                    }
                }
                break
            }

            if (code in 200..299) {
                Result.success(true)
            } else {
                Log.e("SupabaseSyncService", "Order sync failed HTTP $code: $respBody")
                Result.failure(Exception("Supabase Order Sync HTTP $code: ${respBody.take(180)}"))
            }
        } catch (e: Exception) {
            Log.e("SupabaseSyncService", "Order sync exception", e)
            Result.failure(e)
        }
    }

    private fun buildCustomerJson(
        customer: Customer,
        includeExtendedCols: Boolean,
        useIsoTimestamp: Boolean
    ): JSONObject = JSONObject().apply {
        put("id", customer.id)
        put("full_name", customer.fullName)
        put("email", customer.email)
        put("phone_number", customer.phoneNumber)
        put("delivery_address", customer.deliveryAddress)
        val ts = if (customer.createdAt > 0L) customer.createdAt else System.currentTimeMillis()
        put("created_at", if (useIsoTimestamp) toIsoTimestamp(ts) else ts)
        if (includeExtendedCols) {
            put("is_admin", customer.isAdmin)
            put("wishlist_product_ids", customer.wishlistProductIds.joinToString(","))
            if (customer.signUpPassword.isNotBlank()) {
                put("signup_credential", customer.signUpPassword)
            }
        }
    }

    suspend fun syncCustomerToCloud(customer: Customer): Result<Boolean> = withContext(Dispatchers.IO) {
        val validationErr = validateCredentialsBeforeRequest()
        if (validationErr != null) {
            return@withContext Result.failure(Exception(validationErr))
        }
        try {
            val baseUrl = cleanUrl(config.url)
            if (baseUrl.isBlank()) return@withContext Result.failure(Exception("Supabase URL is empty"))

            val payloadObj = buildCustomerJson(customer, includeExtendedCols = true, useIsoTimestamp = false)
            var switchedToIso = false

            fun postCustomerJson(jsonObj: JSONObject): Pair<Int, String> {
                val body = jsonObj.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val url = "$baseUrl/rest/v1/customers?on_conflict=id"
                val request = Request.Builder()
                    .url(url)
                    .addSupabaseAuthHeaders()
                    .header("Content-Type", "application/json")
                    .header("Prefer", "resolution=merge-duplicates,return=minimal")
                    .post(body)
                    .build()
                return client.newCall(request).execute().use { resp ->
                    resp.code to (resp.body?.string() ?: "")
                }
            }

            var (code, respBody) = postCustomerJson(payloadObj)
            var retries = 0
            while (code == 400 && retries < 8) {
                retries++
                val badTimestamp = !switchedToIso && (
                    respBody.contains("timestamp", ignoreCase = true) ||
                        respBody.contains("22007") ||
                        respBody.contains("22008")
                    )
                if (badTimestamp) {
                    switchedToIso = true
                    val ts = if (customer.createdAt > 0L) customer.createdAt else System.currentTimeMillis()
                    payloadObj.put("created_at", toIsoTimestamp(ts))
                    val next = postCustomerJson(payloadObj)
                    code = next.first
                    respBody = next.second
                    continue
                }
                if (respBody.contains("PGRST204")) {
                    val colMatch = Regex("Could not find the '([^']+)' column").find(respBody)
                    val missingCol = colMatch?.groupValues?.getOrNull(1)
                    if (!missingCol.isNullOrBlank() && payloadObj.has(missingCol)) {
                        payloadObj.remove(missingCol)
                        val next = postCustomerJson(payloadObj)
                        code = next.first
                        respBody = next.second
                        continue
                    } else {
                        payloadObj.remove("is_admin")
                        payloadObj.remove("wishlist_product_ids")
                        val next = postCustomerJson(payloadObj)
                        code = next.first
                        respBody = next.second
                        continue
                    }
                }
                break
            }

            // Handle email or phone_number UNIQUE conflict (HTTP 409 / 23505) if customer exists under another row id
            if (code == 409 || respBody.contains("23505")) {
                val patchJson = JSONObject().apply {
                    put("full_name", customer.fullName)
                    if (payloadObj.has("phone_number")) put("phone_number", customer.phoneNumber)
                    if (payloadObj.has("delivery_address")) put("delivery_address", customer.deliveryAddress)
                    if (payloadObj.has("wishlist_product_ids")) put("wishlist_product_ids", customer.wishlistProductIds.joinToString(","))
                    if (payloadObj.has("signup_credential") && customer.signUpPassword.isNotBlank()) {
                        put("signup_credential", customer.signUpPassword)
                    }
                }
                val encodedEmail = java.net.URLEncoder.encode(customer.email, "UTF-8")
                val patchByEmailReq = Request.Builder()
                    .url("$baseUrl/rest/v1/customers?email=eq.$encodedEmail")
                    .addSupabaseAuthHeaders()
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=representation")
                    .patch(patchJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()
                var emailMatched = false
                client.newCall(patchByEmailReq).execute().use { pResp ->
                    code = pResp.code
                    respBody = pResp.body?.string() ?: ""
                    if (pResp.isSuccessful && respBody.trim().startsWith("[") && respBody.trim() != "[]") {
                        emailMatched = true
                    }
                }
                if (!emailMatched && customer.phoneNumber.isNotBlank()) {
                    val encodedPhone = java.net.URLEncoder.encode(customer.phoneNumber, "UTF-8")
                    val patchByPhoneReq = Request.Builder()
                        .url("$baseUrl/rest/v1/customers?phone_number=eq.$encodedPhone")
                        .addSupabaseAuthHeaders()
                        .header("Content-Type", "application/json")
                        .patch(patchJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                        .build()
                    client.newCall(patchByPhoneReq).execute().use { phResp ->
                        code = phResp.code
                        respBody = phResp.body?.string() ?: ""
                    }
                }
            }

            if (code in 200..299) {
                Result.success(true)
            } else {
                Log.e("SupabaseSyncService", "Customer sync failed HTTP $code: $respBody")
                Result.failure(Exception("Supabase Customer Sync HTTP $code: ${respBody.take(180)}"))
            }
        } catch (e: Exception) {
            Log.e("SupabaseSyncService", "Customer sync exception", e)
            Result.failure(e)
        }
    }

    suspend fun deleteCustomerFromCloud(customerId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (isPlaceholderConfig()) return@withContext Result.success(true)
        try {
            val baseUrl = cleanUrl(config.url)
            if (baseUrl.isBlank()) return@withContext Result.success(true)
            val encodedId = java.net.URLEncoder.encode(customerId, "UTF-8")
            val req = Request.Builder()
                .url("$baseUrl/rest/v1/customers?id=eq.$encodedId")
                .addSupabaseAuthHeaders()
                .delete()
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful || resp.code == 200 || resp.code == 204) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("Delete Customer HTTP ${resp.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncCustomerWishlistToCloud(customerId: String, wishlistIds: List<String>): Result<Boolean> = withContext(Dispatchers.IO) {
        if (isPlaceholderConfig()) {
            return@withContext Result.failure(Exception("Supabase not configured"))
        }
        try {
            val baseUrl = cleanUrl(config.url)
            if (baseUrl.isBlank()) return@withContext Result.failure(Exception("Supabase URL is empty"))
            val json = JSONObject().apply {
                put("wishlist_product_ids", wishlistIds.joinToString(","))
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val url = "$baseUrl/rest/v1/customers?id=eq.$customerId"
            val request = Request.Builder()
                .url(url)
                .addSupabaseAuthHeaders()
                .patch(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 200 || response.code == 204) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("Supabase Wishlist Sync HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadReceiptToSupabaseStorage(
        fileName: String,
        fileBytes: ByteArray,
        bucketId: String = "shop-receipts"
    ): Result<String> = withContext(Dispatchers.IO) {
        val validationErr = validateCredentialsBeforeRequest()
        if (validationErr != null) {
            return@withContext Result.failure(Exception(validationErr))
        }
        try {
            val baseUrl = cleanUrl(config.url)
            if (baseUrl.isBlank()) return@withContext Result.failure(Exception("Supabase URL is empty"))
            val cleanName = fileName.replace(" ", "_").replace(Regex("[^A-Za-z0-9._/-]"), "_")
            val mediaType = "image/jpeg".toMediaType()

            suspend fun attemptUpload(objectPath: String, useUpsert: Boolean): Result<String> {
                val url = "$baseUrl/storage/v1/object/$bucketId/$objectPath"
                val requestBody = fileBytes.toRequestBody(mediaType)
                val reqBuilder = Request.Builder()
                    .url(url)
                    .addSupabaseAuthHeaders()
                    .header("Content-Type", "image/jpeg")
                if (useUpsert) {
                    reqBuilder.header("x-upsert", "true")
                }
                val request = reqBuilder.post(requestBody).build()

                return client.newCall(request).execute().use { response ->
                    val respBody = response.body?.string() ?: ""
                    if (response.isSuccessful || response.code == 200 || response.code == 201) {
                        val publicUrl = "$baseUrl/storage/v1/object/public/$bucketId/$objectPath"
                        Result.success(publicUrl)
                    } else {
                        Log.e("SupabaseSyncService", "Storage upload ($bucketId/$objectPath) failed HTTP ${response.code}: $respBody")
                        Result.failure(Exception("Storage '$bucketId' HTTP ${response.code}: ${respBody.ifBlank { response.message }.take(150)}"))
                    }
                }
            }

            val firstTry = attemptUpload(cleanName, useUpsert = true)
            if (firstTry.isSuccess) return@withContext firstTry

            // If UPDATE RLS policy isn't applied yet on an older SQL setup, x-upsert can trigger 400/403/409.
            // Retry with a fresh unique filename using pure INSERT (no x-upsert).
            val dotIdx = cleanName.lastIndexOf('.')
            val uniqueRetryName = if (dotIdx > 0) {
                "${cleanName.substring(0, dotIdx)}_${System.nanoTime() % 100000}${cleanName.substring(dotIdx)}"
            } else {
                "${cleanName}_${System.nanoTime() % 100000}"
            }
            val retryTry = attemptUpload(uniqueRetryName, useUpsert = false)
            if (retryTry.isSuccess) retryTry else firstTry
        } catch (e: Exception) {
            Log.e("SupabaseSyncService", "Storage upload failed", e)
            Result.failure(e)
        }
    }

    /**
     * Uploads product photo to 'product-media' bucket; if 'product-media' bucket/policy hasn't
     * been created yet in SQL Editor, falls back to 'shop-receipts' (under products/) if available.
     */
    suspend fun uploadProductMediaWithFallback(
        fileName: String,
        fileBytes: ByteArray
    ): Result<String> {
        val primary = uploadReceiptToSupabaseStorage(
            fileName = fileName,
            fileBytes = fileBytes,
            bucketId = "product-media"
        )
        if (primary.isSuccess) return primary

        val cleanName = fileName.replace(" ", "_").replace("/", "_")
        val fallback = uploadReceiptToSupabaseStorage(
            fileName = "products/$cleanName",
            fileBytes = fileBytes,
            bucketId = "shop-receipts"
        )
        return if (fallback.isSuccess) fallback else primary
    }

    fun generateSupabaseSqlSchema(): String {
        return """
            -- 24/7 SHOPPING Complete Supabase Schema, RLS & Storage Bucket Policy Script
            -- Execute this entire script in your Supabase SQL Editor:

            -- 1. TABLES CREATION & SAFE COLUMN UPGRADES
            CREATE TABLE IF NOT EXISTS public.products (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                category TEXT NOT NULL,
                brand TEXT NOT NULL,
                available_brands TEXT,
                available_sizes TEXT,
                available_colors TEXT,
                base_price NUMERIC NOT NULL,
                cost_price NUMERIC,
                discount_percent INT DEFAULT 0,
                stock_quantity INT DEFAULT 0,
                expiry_months_remaining INT DEFAULT 6,
                image_url TEXT,
                model_3d_type TEXT DEFAULT 'CUBE',
                description TEXT,
                sku TEXT,
                created_at TIMESTAMPTZ DEFAULT NOW()
            );

            -- Safely upgrade an existing partial products table without dropping data
            ALTER TABLE public.products ADD COLUMN IF NOT EXISTS category TEXT DEFAULT 'General';
            ALTER TABLE public.products ADD COLUMN IF NOT EXISTS brand TEXT DEFAULT '24/7 SHOPPING';
            ALTER TABLE public.products ADD COLUMN IF NOT EXISTS available_brands TEXT;
            ALTER TABLE public.products ADD COLUMN IF NOT EXISTS available_sizes TEXT;
            ALTER TABLE public.products ADD COLUMN IF NOT EXISTS available_colors TEXT;
            ALTER TABLE public.products ADD COLUMN IF NOT EXISTS base_price NUMERIC DEFAULT 0;
            ALTER TABLE public.products ADD COLUMN IF NOT EXISTS cost_price NUMERIC;
            ALTER TABLE public.products ADD COLUMN IF NOT EXISTS discount_percent INT DEFAULT 0;
            ALTER TABLE public.products ADD COLUMN IF NOT EXISTS stock_quantity INT DEFAULT 0;
            ALTER TABLE public.products ADD COLUMN IF NOT EXISTS expiry_months_remaining INT DEFAULT 6;
            ALTER TABLE public.products ADD COLUMN IF NOT EXISTS image_url TEXT;
            ALTER TABLE public.products ADD COLUMN IF NOT EXISTS model_3d_type TEXT DEFAULT 'CUBE';
            ALTER TABLE public.products ADD COLUMN IF NOT EXISTS description TEXT;
            ALTER TABLE public.products ADD COLUMN IF NOT EXISTS sku TEXT;
            ALTER TABLE public.products ADD COLUMN IF NOT EXISTS partner_id TEXT DEFAULT '';
            ALTER TABLE public.products ADD COLUMN IF NOT EXISTS partner_name TEXT DEFAULT '';
            ALTER TABLE public.products ADD COLUMN IF NOT EXISTS partner_commission_percent NUMERIC DEFAULT 0;
            ALTER TABLE public.products ADD COLUMN IF NOT EXISTS partner_batch_id TEXT DEFAULT '';
            ALTER TABLE public.products ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW();

            CREATE TABLE IF NOT EXISTS public.customers (
                id TEXT PRIMARY KEY,
                full_name TEXT NOT NULL,
                email TEXT UNIQUE NOT NULL,
                phone_number TEXT,
                delivery_address TEXT,
                wishlist_product_ids TEXT DEFAULT '',
                signup_credential TEXT DEFAULT '',
                is_admin BOOLEAN DEFAULT FALSE,
                created_at BIGINT
            );

            ALTER TABLE public.customers ADD COLUMN IF NOT EXISTS full_name TEXT DEFAULT 'Customer';
            ALTER TABLE public.customers ADD COLUMN IF NOT EXISTS email TEXT;
            ALTER TABLE public.customers ADD COLUMN IF NOT EXISTS phone_number TEXT DEFAULT '';
            ALTER TABLE public.customers ADD COLUMN IF NOT EXISTS delivery_address TEXT DEFAULT '';
            ALTER TABLE public.customers ADD COLUMN IF NOT EXISTS wishlist_product_ids TEXT DEFAULT '';
            ALTER TABLE public.customers ADD COLUMN IF NOT EXISTS signup_credential TEXT DEFAULT '';
            ALTER TABLE public.customers ADD COLUMN IF NOT EXISTS is_admin BOOLEAN DEFAULT FALSE;
            ALTER TABLE public.customers ADD COLUMN IF NOT EXISTS created_at BIGINT;

            CREATE TABLE IF NOT EXISTS public.orders (
                order_id TEXT PRIMARY KEY,
                customer_id TEXT NOT NULL,
                customer_name TEXT NOT NULL,
                customer_email TEXT NOT NULL,
                customer_phone TEXT,
                delivery_address TEXT NOT NULL,
                items_summary TEXT,
                total_amount NUMERIC NOT NULL,
                profit NUMERIC DEFAULT 0,
                payment_method TEXT NOT NULL,
                payment_account TEXT,
                payment_reference TEXT,
                sender_name TEXT,
                status TEXT NOT NULL,
                verification_note TEXT,
                receipt_image_url TEXT DEFAULT '',
                status_updates_json TEXT DEFAULT '',
                dispatch_rider_whatsapp TEXT DEFAULT '',
                created_at BIGINT
            );

            ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS customer_phone TEXT DEFAULT '';
            ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS payment_account TEXT DEFAULT '';
            ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS payment_reference TEXT DEFAULT '';
            ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS sender_name TEXT DEFAULT '';
            ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS verification_note TEXT DEFAULT '';
            ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS receipt_image_url TEXT DEFAULT '';
            ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS status_updates_json TEXT DEFAULT '';
            ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS dispatch_rider_whatsapp TEXT DEFAULT '';
            ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS partner_id TEXT DEFAULT '';
            ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS partner_name TEXT DEFAULT '';
            ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS partner_commission_amount NUMERIC DEFAULT 0;
            ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS partner_net_payout NUMERIC DEFAULT 0;
            ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS created_at BIGINT;

            -- 2. GRANTS & ROW LEVEL SECURITY (RLS) POLICIES FOR TABLES
            GRANT USAGE ON SCHEMA public TO anon, authenticated;
            GRANT ALL ON TABLE public.products TO anon, authenticated;
            GRANT ALL ON TABLE public.customers TO anon, authenticated;
            GRANT ALL ON TABLE public.orders TO anon, authenticated;

            ALTER TABLE public.products ENABLE ROW LEVEL SECURITY;
            DROP POLICY IF EXISTS "Public Read & Sync Access on products" ON public.products;
            CREATE POLICY "Public Read & Sync Access on products" ON public.products FOR ALL TO anon, authenticated USING (true) WITH CHECK (true);

            ALTER TABLE public.customers ENABLE ROW LEVEL SECURITY;
            DROP POLICY IF EXISTS "Public Read & Sync Access on customers" ON public.customers;
            CREATE POLICY "Public Read & Sync Access on customers" ON public.customers FOR ALL TO anon, authenticated USING (true) WITH CHECK (true);

            ALTER TABLE public.orders ENABLE ROW LEVEL SECURITY;
            DROP POLICY IF EXISTS "Public Read & Sync Access on orders" ON public.orders;
            CREATE POLICY "Public Read & Sync Access on orders" ON public.orders FOR ALL TO anon, authenticated USING (true) WITH CHECK (true);

            -- 3. SUPABASE STORAGE BUCKETS INITIALIZATION
            INSERT INTO storage.buckets (id, name, public) 
            VALUES ('shop-receipts', 'shop-receipts', true)
            ON CONFLICT (id) DO UPDATE SET public = true;

            INSERT INTO storage.buckets (id, name, public) 
            VALUES ('product-media', 'product-media', true)
            ON CONFLICT (id) DO UPDATE SET public = true;

            INSERT INTO storage.buckets (id, name, public) 
            VALUES ('customer-avatars', 'customer-avatars', true)
            ON CONFLICT (id) DO UPDATE SET public = true;

            -- 4. SUPABASE STORAGE BUCKET POLICIES (storage.objects)
            -- Receipts Bucket Policies
            DROP POLICY IF EXISTS "Public Read on shop-receipts" ON storage.objects;
            CREATE POLICY "Public Read on shop-receipts" ON storage.objects FOR SELECT USING (bucket_id = 'shop-receipts');

            DROP POLICY IF EXISTS "Public Insert on shop-receipts" ON storage.objects;
            CREATE POLICY "Public Insert on shop-receipts" ON storage.objects FOR INSERT WITH CHECK (bucket_id = 'shop-receipts');

            DROP POLICY IF EXISTS "Public Update on shop-receipts" ON storage.objects;
            CREATE POLICY "Public Update on shop-receipts" ON storage.objects FOR UPDATE USING (bucket_id = 'shop-receipts');

            -- Product Media Bucket Policies
            DROP POLICY IF EXISTS "Public Read on product-media" ON storage.objects;
            CREATE POLICY "Public Read on product-media" ON storage.objects FOR SELECT USING (bucket_id = 'product-media');

            DROP POLICY IF EXISTS "Public Insert on product-media" ON storage.objects;
            CREATE POLICY "Public Insert on product-media" ON storage.objects FOR INSERT WITH CHECK (bucket_id = 'product-media');

            DROP POLICY IF EXISTS "Public Update on product-media" ON storage.objects;
            CREATE POLICY "Public Update on product-media" ON storage.objects FOR UPDATE USING (bucket_id = 'product-media');

            -- Customer Avatars Bucket Policies
            DROP POLICY IF EXISTS "Public Read on customer-avatars" ON storage.objects;
            CREATE POLICY "Public Read on customer-avatars" ON storage.objects FOR SELECT USING (bucket_id = 'customer-avatars');

            DROP POLICY IF EXISTS "Public Insert on customer-avatars" ON storage.objects;
            CREATE POLICY "Public Insert on customer-avatars" ON storage.objects FOR INSERT WITH CHECK (bucket_id = 'customer-avatars');

            DROP POLICY IF EXISTS "Public Update on customer-avatars" ON storage.objects;
            CREATE POLICY "Public Update on customer-avatars" ON storage.objects FOR UPDATE USING (bucket_id = 'customer-avatars');

            -- 5. FORCE POSTGREST SCHEMA CACHE RELOAD
            NOTIFY pgrst, 'reload schema';
        """.trimIndent()
    }

    suspend fun fetchProductsFromCloud(): Result<List<Product>> = withContext(Dispatchers.IO) {
        val validationErr = validateCredentialsBeforeRequest()
        if (validationErr != null) {
            return@withContext Result.failure(Exception(validationErr))
        }
        try {
            val baseUrl = cleanUrl(config.url)
            val productsList = mutableListOf<Product>()
            val pageSize = 500
            var offset = 0

            while (true) {
                val url = "$baseUrl/rest/v1/products?select=*&limit=$pageSize&offset=$offset"
                val request = Request.Builder()
                    .url(url)
                    .addSupabaseAuthHeaders()
                    .get()
                    .build()

                var pageCount = 0
                client.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        Log.e("SupabaseSyncService", "Fetch products failed HTTP ${response.code}: $bodyStr")
                        return@withContext Result.failure(
                            Exception(formatSyncHttpError(response.code, bodyStr, response.message))
                        )
                    }
                    val jsonArray = JSONArray(bodyStr.ifBlank { "[]" })
                    pageCount = jsonArray.length()
                    for (i in 0 until jsonArray.length()) {
                        try {
                            val obj = jsonArray.getJSONObject(i)
                            val id = obj.optCleanString("id")
                            val title = obj.optCleanString("title")
                            if (id.isBlank() || title.isBlank()) continue

                            val category = obj.optCleanString("category", "Clothes").ifBlank { "Clothes" }
                            val brand = obj.optCleanString("brand", "24/7 SHOPPING").ifBlank { "24/7 SHOPPING" }
                            val availableBrandsStr = obj.optCleanString("available_brands")
                            val availableSizesStr = obj.optCleanString("available_sizes")
                            val availableColorsStr = obj.optCleanString("available_colors")
                            val rawBasePrice = obj.optDouble("base_price", 0.0)
                            val basePrice = if (rawBasePrice.isNaN()) 0.0 else rawBasePrice
                            val rawCostPrice = if (obj.isNull("cost_price")) Double.NaN else obj.optDouble("cost_price", Double.NaN)
                            val costPrice = if (rawCostPrice.isNaN() || rawCostPrice < 0.0) (basePrice * 0.70) else rawCostPrice
                            val discountPercent = obj.optInt("discount_percent", 0).coerceIn(0, 95)
                            val stockQty = obj.optInt("stock_quantity", 0)
                            val expiryMonths = if (obj.isNull("expiry_months_remaining")) 6 else obj.optInt("expiry_months_remaining", 6)
                            val imageUrl = obj.optCleanString("image_url")
                            val model3dType = obj.optCleanString("model_3d_type", "SNEAKER").ifBlank { "SNEAKER" }
                            val description = obj.optCleanString("description")
                            val sku = obj.optCleanString("sku").ifBlank { "SKU-${id.take(6).uppercase()}" }
                            val parsedCreatedAt = if (!id.startsWith("prod-")) parseCreatedAtMillis(obj) else 0L
                            val partnerId = obj.optCleanString("partner_id")
                            val partnerName = obj.optCleanString("partner_name")
                            val rawCommPct = if (obj.isNull("partner_commission_percent")) 0.0 else obj.optDouble("partner_commission_percent", 0.0)
                            val partnerCommissionPercent = if (rawCommPct.isNaN()) 0.0 else rawCommPct
                            val partnerBatchId = obj.optCleanString("partner_batch_id")

                            val parsedBrands = availableBrandsStr.split(",")
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                                .ifEmpty { listOf(brand) }
                            val parsedSizes = availableSizesStr.split(",")
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                                .ifEmpty { listOf("Standard") }
                            val parsedColors = availableColorsStr.split(";").mapNotNull { entry ->
                                val parts = entry.split("|")
                                if (parts.size >= 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                                    com.example.data.model.ProductColor(parts[0].trim(), parts[1].trim())
                                } else null
                            }.ifEmpty {
                                listOf(
                                    com.example.data.model.ProductColor("Midnight Black", "#111827"),
                                    com.example.data.model.ProductColor("Cyber Cyan", "#06B6D4")
                                )
                            }

                            productsList.add(
                                Product(
                                    id = id,
                                    title = title,
                                    category = category,
                                    brand = brand,
                                    availableBrands = parsedBrands,
                                    availableSizes = parsedSizes,
                                    availableColors = parsedColors,
                                    basePrice = basePrice,
                                    costPrice = costPrice,
                                    discountPercent = discountPercent,
                                    stockQuantity = stockQty,
                                    imageUrl = imageUrl,
                                    model3dType = model3dType,
                                    description = description,
                                    sku = sku,
                                    expiryMonthsRemaining = expiryMonths,
                                    createdAt = parsedCreatedAt,
                                    partnerId = partnerId,
                                    partnerName = partnerName,
                                    partnerCommissionPercent = partnerCommissionPercent,
                                    partnerBatchId = partnerBatchId
                                )
                            )
                        } catch (rowEx: Exception) {
                            Log.w("SupabaseSyncService", "Skipping malformed product row at index $i: ${rowEx.message}")
                        }
                    }
                }
                if (pageCount < pageSize) break
                offset += pageCount
            }
            Result.success(productsList)
        } catch (e: Exception) {
            Log.e("SupabaseSyncService", "Fetch products exception", e)
            Result.failure(e)
        }
    }

    suspend fun fetchCustomersFromCloud(): Result<List<Customer>> = withContext(Dispatchers.IO) {
        if (isPlaceholderConfig()) {
            return@withContext Result.failure(Exception("Supabase not configured"))
        }
        try {
            val baseUrl = cleanUrl(config.url)
            val list = mutableListOf<Customer>()
            val pageSize = 500
            var offset = 0
            var useOrder = true

            while (offset < 5000) {
                val orderParam = if (useOrder) "&order=created_at.desc.nullslast" else ""
                val request = Request.Builder()
                    .url("$baseUrl/rest/v1/customers?select=*$orderParam&limit=$pageSize&offset=$offset")
                    .addSupabaseAuthHeaders()
                    .get()
                    .build()

                var pageCount = 0
                var shouldRetryWithoutOrder = false
                client.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        val jsonArray = JSONArray(bodyStr.ifBlank { "[]" })
                        pageCount = jsonArray.length()
                        for (i in 0 until pageCount) {
                            val obj = jsonArray.getJSONObject(i)
                            val id = obj.optCleanString("id")
                            val fullName = obj.optCleanString("full_name", "Customer")
                            val email = obj.optCleanString("email")
                            val phone = obj.optCleanString("phone_number")
                            val address = obj.optCleanString("delivery_address")
                            val createdAt = parseCreatedAtMillis(obj)
                            val isAdmin = obj.optBoolean("is_admin", false)
                            val signUpCred = obj.optCleanString("signup_credential")
                            val wishlistStr = obj.optCleanString("wishlist_product_ids")
                            val wishlist = if (wishlistStr.isBlank()) emptyList() else wishlistStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }

                            if (id.isNotBlank()) {
                                list.add(
                                    Customer(
                                        id = id,
                                        fullName = fullName.ifBlank { "Valued Customer" },
                                        email = email,
                                        phoneNumber = phone,
                                        deliveryAddress = address,
                                        createdAt = createdAt,
                                        isAdmin = isAdmin,
                                        wishlistProductIds = wishlist,
                                        signUpPassword = signUpCred
                                    )
                                )
                            }
                        }
                    } else if (useOrder && response.code == 400 && (bodyStr.contains("created_at") || bodyStr.contains("42703") || bodyStr.contains("PGRST"))) {
                        useOrder = false
                        shouldRetryWithoutOrder = true
                    } else {
                        Log.e("SupabaseSyncService", "Fetch customers failed HTTP ${response.code}: $bodyStr")
                        return@withContext Result.failure(Exception("Supabase Fetch Customers HTTP ${response.code}: ${bodyStr.take(150)}"))
                    }
                }
                if (shouldRetryWithoutOrder) continue
                if (pageCount < pageSize) break
                offset += pageCount
            }
            Result.success(list.sortedByDescending { it.createdAt })
        } catch (e: Exception) {
            Log.e("SupabaseSyncService", "Fetch customers exception", e)
            Result.failure(e)
        }
    }

    suspend fun fetchOrdersFromCloud(): Result<List<Order>> = withContext(Dispatchers.IO) {
        if (isPlaceholderConfig()) {
            return@withContext Result.failure(Exception("Supabase not configured"))
        }
        try {
            val baseUrl = cleanUrl(config.url)
            val ordersList = mutableListOf<Order>()
            val pageSize = 500
            var offset = 0
            var useOrder = true

            while (offset < 5000) {
                val orderParam = if (useOrder) "&order=created_at.desc.nullslast" else ""
                val url = "$baseUrl/rest/v1/orders?select=*$orderParam&limit=$pageSize&offset=$offset"
                val request = Request.Builder()
                    .url(url)
                    .addSupabaseAuthHeaders()
                    .get()
                    .build()

                var pageCount = 0
                var shouldRetryWithoutOrder = false
                client.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        val jsonArray = JSONArray(bodyStr.ifBlank { "[]" })
                        pageCount = jsonArray.length()
                        for (i in 0 until pageCount) {
                            val obj = jsonArray.getJSONObject(i)
                            val orderId = obj.optCleanString("order_id")
                            val customerId = obj.optCleanString("customer_id")
                            val customerName = obj.optCleanString("customer_name")
                            val customerEmail = obj.optCleanString("customer_email")
                            val customerPhone = obj.optCleanString("customer_phone")
                            val deliveryAddress = obj.optCleanString("delivery_address")
                            val itemsSummary = obj.optCleanString("items_summary")
                            val totalAmount = obj.optDouble("total_amount", 0.0)
                            val profit = obj.optDouble("profit", totalAmount * 0.15)
                            val paymentMethodStr = obj.optCleanString("payment_method", "OPAY")
                            val paymentAccount = obj.optCleanString("payment_account")
                            val paymentReference = obj.optCleanString("payment_reference")
                            val senderName = obj.optCleanString("sender_name")
                            val statusStr = obj.optCleanString("status", "PENDING_VERIFICATION")
                            val createdAt = parseCreatedAtMillis(obj)
                            val rawNote = obj.optCleanString("verification_note")
                            val directReceiptUrl = obj.optCleanString("receipt_image_url")
                            val extractedNoteUrl = Regex("\\[Receipt:\\s*(https?://[^\\]\\s]+)\\]").find(rawNote)?.groupValues?.getOrNull(1) ?: ""
                            val receiptUrl = directReceiptUrl.ifBlank { extractedNoteUrl }
                            val dispatchWhatsapp = obj.optCleanString("dispatch_rider_whatsapp")
                            val statusUpdatesRaw = obj.optCleanString("status_updates_json")
                            val updates = statusUpdatesRaw.split(";").mapNotNull { entry ->
                                val parts = entry.split("|")
                                if (parts.size >= 3) {
                                    com.example.data.model.StatusUpdate(
                                        status = try { com.example.data.model.OrderStatus.valueOf(parts[0]) } catch (_: Exception) { com.example.data.model.OrderStatus.PENDING_VERIFICATION },
                                        timestamp = parts[1].toLongOrNull() ?: createdAt,
                                        note = parts[2]
                                    )
                                } else null
                            }

                            val pMethod = try { com.example.data.model.PaymentMethod.valueOf(paymentMethodStr) } catch (e: Exception) { com.example.data.model.PaymentMethod.OPAY }
                            val oStatus = try { com.example.data.model.OrderStatus.valueOf(statusStr) } catch (e: Exception) { com.example.data.model.OrderStatus.PENDING_VERIFICATION }
                            val partnerId = obj.optCleanString("partner_id")
                            val partnerName = obj.optCleanString("partner_name")
                            val partnerCommAmt = if (obj.isNull("partner_commission_amount")) 0.0 else obj.optDouble("partner_commission_amount", 0.0)
                            val partnerNetPayout = if (obj.isNull("partner_net_payout")) 0.0 else obj.optDouble("partner_net_payout", 0.0)

                            if (orderId.isNotBlank()) {
                                ordersList.add(
                                    Order(
                                        orderId = orderId,
                                        customerId = customerId,
                                        customerName = customerName,
                                        customerEmail = customerEmail,
                                        customerPhone = customerPhone,
                                        deliveryAddress = deliveryAddress,
                                        itemsSummary = itemsSummary,
                                        totalAmount = totalAmount,
                                        profit = profit,
                                        paymentMethod = pMethod,
                                        paymentAccount = paymentAccount,
                                        paymentReference = paymentReference,
                                        senderName = senderName,
                                        status = oStatus,
                                        createdAt = createdAt,
                                        verificationNote = rawNote,
                                        receiptImageUrl = receiptUrl,
                                        statusUpdates = updates,
                                        dispatchRiderWhatsapp = dispatchWhatsapp,
                                        partnerId = partnerId,
                                        partnerName = partnerName,
                                        partnerCommissionAmount = if (partnerCommAmt.isNaN()) 0.0 else partnerCommAmt,
                                        partnerNetPayout = if (partnerNetPayout.isNaN()) 0.0 else partnerNetPayout
                                    )
                                )
                            }
                        }
                    } else if (useOrder && response.code == 400 && (bodyStr.contains("created_at") || bodyStr.contains("42703") || bodyStr.contains("PGRST"))) {
                        useOrder = false
                        shouldRetryWithoutOrder = true
                    } else {
                        Log.e("SupabaseSyncService", "Fetch orders failed HTTP ${response.code}: $bodyStr")
                        return@withContext Result.failure(Exception("Supabase Fetch Orders HTTP ${response.code}: ${bodyStr.take(150)}"))
                    }
                }
                if (shouldRetryWithoutOrder) continue
                if (pageCount < pageSize) break
                offset += pageCount
            }
            Result.success(ordersList.sortedByDescending { it.createdAt })
        } catch (e: Exception) {
            Log.e("SupabaseSyncService", "Fetch orders exception", e)
            Result.failure(e)
        }
    }

    fun cleanUrl(rawUrl: String): String {
        var trimmed = rawUrl
            .replace("\u200B", "")
            .replace("\uFEFF", "")
            .trim()
            .trim('"', '\'', '`')
            .replace("\\s+".toRegex(), "")
            .trimEnd('/')

        if (trimmed.isBlank()) return ""

        // 1. Extract Supabase Dashboard URL if pasted directly:
        // e.g. https://supabase.com/dashboard/project/abcdefghij/settings/api -> https://abcdefghij.supabase.co
        val dashboardMatch = Regex("supabase\\.com/dashboard/project/([a-zA-Z0-9-]+)").find(trimmed)
        if (dashboardMatch != null) {
            val ref = dashboardMatch.groupValues[1].lowercase()
            return "https://$ref.supabase.co"
        }

        // 2. Extract embedded *.supabase.co or *.supabase.com host even if preceded/followed by pasted label text
        val supabaseHostMatch = Regex("([a-zA-Z0-9-]+)\\.supabase\\.com?", RegexOption.IGNORE_CASE).find(trimmed)
        if (supabaseHostMatch != null) {
            val ref = supabaseHostMatch.groupValues[1].lowercase()
            if (ref != "www") {
                return "https://$ref.supabase.co"
            }
        }

        // 3. Strip common API path suffixes
        for (suffix in listOf("/rest/v1", "/storage/v1", "/auth/v1", "/realtime/v1")) {
            if (trimmed.endsWith(suffix, ignoreCase = true)) {
                trimmed = trimmed.dropLast(suffix.length).trimEnd('/')
            }
        }

        // 4. Normalize scheme & handle bare project ref (e.g. "http://jcgdxxxx" or "jcgdxxxx" without .supabase.co)
        val withoutScheme = trimmed
            .replaceFirst(Regex("^https?://", RegexOption.IGNORE_CASE), "")
            .trim('/')
        if (!withoutScheme.contains(".") && withoutScheme.matches(Regex("^[a-zA-Z0-9]{12,32}$"))) {
            return "https://${withoutScheme.lowercase()}.supabase.co"
        }

        if (trimmed.startsWith("http://", ignoreCase = true) && trimmed.contains(".supabase.co", ignoreCase = true)) {
            trimmed = "https://" + trimmed.substring(7)
        } else if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
            trimmed = "https://$trimmed"
        }
        return trimmed.trimEnd('/')
    }

    suspend fun publishAppUpdateManifest(manifest: AppUpdateManifest): Result<String> = withContext(Dispatchers.IO) {
        val validationErr = validateCredentialsBeforeRequest()
        if (validationErr != null) {
            return@withContext Result.failure(Exception(validationErr))
        }
        try {
            val baseUrl = cleanUrl(config.url)
            val json = JSONObject().apply {
                put("versionCode", manifest.versionCode)
                put("versionName", manifest.versionName)
                put("releaseNotes", manifest.releaseNotes)
                put("updateUrl", manifest.downloadUrl)
                put("publishedAt", System.currentTimeMillis())
            }.toString()
            val bytes = json.toByteArray(Charsets.UTF_8)
            val mediaType = "application/json".toMediaType()
            val objectPath = "system/app_update.json"

            suspend fun uploadToBucket(bucketId: String): Result<String> {
                val url = "$baseUrl/storage/v1/object/$bucketId/$objectPath"
                val request = Request.Builder()
                    .url(url)
                    .addSupabaseAuthHeaders()
                    .header("Content-Type", "application/json")
                    .header("x-upsert", "true")
                    .post(bytes.toRequestBody(mediaType))
                    .build()
                return client.newCall(request).execute().use { response ->
                    val respBody = response.body?.string() ?: ""
                    if (response.isSuccessful || response.code in 200..201) {
                        Result.success("$baseUrl/storage/v1/object/public/$bucketId/$objectPath")
                    } else {
                        Result.failure(Exception("Storage '$bucketId' HTTP ${response.code}: ${respBody.take(120)}"))
                    }
                }
            }

            val primary = uploadToBucket("product-media")
            if (primary.isSuccess) return@withContext primary
            val fallback = uploadToBucket("shop-receipts")
            if (fallback.isSuccess) fallback else primary
        } catch (e: Exception) {
            Log.e("SupabaseSyncService", "publishAppUpdateManifest failed", e)
            Result.failure(e)
        }
    }

    suspend fun fetchAppUpdateManifest(customPortalBaseUrl: String = ""): Result<AppUpdateManifest?> = withContext(Dispatchers.IO) {
        val candidates = mutableListOf<String>()
        if (!isPlaceholderConfig()) {
            val baseUrl = cleanUrl(config.url)
            if (baseUrl.isNotBlank()) {
                val ts = System.currentTimeMillis()
                candidates.add("$baseUrl/storage/v1/object/public/product-media/system/app_update.json?t=$ts")
                candidates.add("$baseUrl/storage/v1/object/public/shop-receipts/system/app_update.json?t=$ts")
            }
        }
        val cleanPortal = customPortalBaseUrl.trim()
            .substringBefore("?")
            .trimEnd('/')
            .removeSuffix("/index.html")
        if (cleanPortal.startsWith("http", ignoreCase = true) &&
            !cleanPortal.contains("ais-pre-gjs6srxqc2prdx42l343un") &&
            !cleanPortal.contains("ais-dev-gjs6srxqc2prdx42l343un")
        ) {
            candidates.add("$cleanPortal/version.json?t=${System.currentTimeMillis()}")
        }

        for (candidateUrl in candidates) {
            try {
                val req = Request.Builder().url(candidateUrl).get().build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string()?.trim().orEmpty()
                        if (body.startsWith("{")) {
                            val obj = JSONObject(body)
                            val vCode = obj.optInt("versionCode", 1)
                            val vName = obj.optString("versionName", "1.0").trim().ifBlank { "1.0" }
                            val notes = obj.optString("releaseNotes", "").trim()
                            val dlUrl = com.example.util.WebPortalExporter.normalizeWebUrl(
                                obj.optString("updateUrl", "")
                            )
                            return@withContext Result.success(
                                AppUpdateManifest(
                                    versionCode = vCode,
                                    versionName = vName,
                                    releaseNotes = notes,
                                    downloadUrl = dlUrl
                                )
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                // Try next source
            }
        }
        Result.success(null)
    }
}
