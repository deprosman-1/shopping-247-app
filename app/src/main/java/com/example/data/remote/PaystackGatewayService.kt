package com.example.data.remote

import com.example.util.AppDefenseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

data class PaystackInitResult(
    val success: Boolean,
    val authorizationUrl: String = "",
    val accessCode: String = "",
    val reference: String = "",
    val message: String = ""
)

data class PaystackVerifyResult(
    val success: Boolean,
    val status: String = "",
    val amountNaira: Double = 0.0,
    val reference: String = "",
    val channel: String = "",
    val message: String = ""
)

data class PaystackCheckoutUiState(
    val isLoading: Boolean = false,
    val orderId: String = "",
    val reference: String = "",
    val authorizationUrl: String = "",
    val statusMessage: String = "",
    val isVerifiedPaid: Boolean = false
)

/**
 * Supabase Edge Function-backed Paystack Merchant Gateway client.
 * Security Architecture:
 * - Secret Key (`sk_live_...` / `sk_test_...`) NEVER leaves Supabase Edge Functions.
 * - Client only communicates over HTTPS with Supabase Edge Functions (`paystack-init`, `paystack-verify`)
 *   and opens the Paystack Hosted Checkout (`authorization_url`) in the system browser / Custom Tab.
 */
class PaystackGatewayService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun generatePaystackReference(orderId: String): String {
        val cleanOrd = orderId.replace(Regex("[^A-Za-z0-9]"), "").takeLast(7).ifBlank { "ORDER" }
        val token = UUID.randomUUID().toString().replace("-", "").take(8).uppercase()
        return "PSKODY-$cleanOrd-$token"
    }

    fun resolveEdgeBaseUrl(supabaseUrl: String, overrideEdgeBaseUrl: String = ""): String {
        val cleanOverride = overrideEdgeBaseUrl.trim().trimEnd('/')
        if (cleanOverride.startsWith("https://", ignoreCase = true)) {
            return cleanOverride
        }
        val cleanSb = supabaseUrl.trim().trimEnd('/')
        return if (cleanSb.startsWith("https://", ignoreCase = true)) {
            "$cleanSb/functions/v1"
        } else {
            ""
        }
    }

    fun buildCustomerReceiptEmail(customerPhone: String, supportEmail: String, reference: String): String {
        val cleanSupport = supportEmail.trim()
        if (cleanSupport.contains("@") && cleanSupport.contains(".")) {
            return cleanSupport
        }
        val digits = customerPhone.filter { it.isDigit() }.takeLast(11).ifBlank { reference.take(10).lowercase() }
        return "customer.$digits@odiya.store"
    }

    suspend fun initializeTransaction(
        supabaseUrl: String,
        supabaseAnonKey: String,
        overrideEdgeBaseUrl: String,
        orderId: String,
        amountNaira: Double,
        customerName: String,
        customerPhone: String,
        supportEmail: String,
        callbackUrl: String = "depros://paystack-callback",
        referenceOverride: String = ""
    ): PaystackInitResult = withContext(Dispatchers.IO) {
        if (amountNaira <= 0.0) {
            return@withContext PaystackInitResult(false, message = "Order amount must be greater than ₦0.")
        }
        val edgeBase = resolveEdgeBaseUrl(supabaseUrl, overrideEdgeBaseUrl)
        if (edgeBase.isBlank() || supabaseAnonKey.isBlank()) {
            return@withContext PaystackInitResult(
                success = false,
                message = "Connect Supabase Cloud URL & Anon Key first, then deploy Paystack Edge Functions ('paystack-init') in Supabase."
            )
        }
        val ref = referenceOverride.ifBlank { generatePaystackReference(orderId) }
        val amountKobo = (amountNaira * 100.0).toLong().coerceAtLeast(100L)
        val safeEmail = buildCustomerReceiptEmail(customerPhone, supportEmail, ref)

        val payload = JSONObject().apply {
            put("order_id", orderId)
            put("reference", ref)
            put("amount", amountKobo)
            put("amount_naira", amountNaira)
            put("currency", "NGN")
            put("email", safeEmail)
            put("callback_url", callbackUrl.ifBlank { "depros://paystack-callback" })
            put("metadata", JSONObject().apply {
                put("order_id", orderId)
                put("customer_name", AppDefenseManager.sanitizeTextInput(customerName, 80))
                put("customer_phone", AppDefenseManager.sanitizeTextInput(customerPhone, 32))
                put("store_brand", "Odiya")
            })
        }

        val initUrl = "$edgeBase/paystack-init"
        if (!AppDefenseManager.isTrustedHttpsUrl(initUrl)) {
            return@withContext PaystackInitResult(false, message = "Paystack endpoint must use strict HTTPS.")
        }

        try {
            val req = Request.Builder()
                .url(initUrl)
                .header("Authorization", "Bearer ${supabaseAnonKey.trim()}")
                .header("apikey", supabaseAnonKey.trim())
                .header("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            client.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    val hint = when (resp.code) {
                        404 -> "Supabase Edge Function 'paystack-init' is not deployed yet. Copy the Edge Function code from Admin -> Supabase Cloud Tab."
                        401, 403 -> "Paystack Edge Function rejected auth. Check PAYSTACK_SECRET_KEY in Supabase Edge Function Secrets."
                        else -> "Paystack init HTTP ${resp.code}: ${raw.take(180)}"
                    }
                    return@withContext PaystackInitResult(false, reference = ref, message = hint)
                }
                val json = JSONObject(raw.ifBlank { "{}" })
                val dataObj = json.optJSONObject("data") ?: json
                val authUrl = dataObj.optString("authorization_url").trim()
                val accessCode = dataObj.optString("access_code").trim()
                val returnedRef = dataObj.optString("reference").ifBlank { ref }

                if (!AppDefenseManager.isTrustedHttpsUrl(authUrl)) {
                    return@withContext PaystackInitResult(
                        success = false,
                        reference = returnedRef,
                        message = json.optString("message").ifBlank { "Invalid authorization URL returned by Paystack Edge Function." }
                    )
                }
                PaystackInitResult(
                    success = true,
                    authorizationUrl = authUrl,
                    accessCode = accessCode,
                    reference = returnedRef,
                    message = "Paystack checkout session ready."
                )
            }
        } catch (e: Exception) {
            PaystackInitResult(
                success = false,
                reference = ref,
                message = "Network timeout reaching Paystack Edge Function: ${e.message ?: "Check internet connection"}"
            )
        }
    }

    suspend fun verifyTransaction(
        supabaseUrl: String,
        supabaseAnonKey: String,
        overrideEdgeBaseUrl: String,
        reference: String
    ): PaystackVerifyResult = withContext(Dispatchers.IO) {
        val cleanRef = AppDefenseManager.sanitizeTextInput(reference, 64)
        if (cleanRef.isBlank()) {
            return@withContext PaystackVerifyResult(false, message = "Missing Paystack payment reference.")
        }
        val cleanSb = supabaseUrl.trim().trimEnd('/')
        val edgeBase = resolveEdgeBaseUrl(cleanSb, overrideEdgeBaseUrl)
        if (edgeBase.isBlank() || supabaseAnonKey.isBlank()) {
            return@withContext PaystackVerifyResult(false, reference = cleanRef, message = "Supabase connection required to verify Paystack payment.")
        }

        // 1. Primary verification via live Paystack Verify Edge Function
        try {
            val verifyUrl = "$edgeBase/paystack-verify"
            if (AppDefenseManager.isTrustedHttpsUrl(verifyUrl)) {
                val payload = JSONObject().put("reference", cleanRef).toString()
                val req = Request.Builder()
                    .url(verifyUrl)
                    .header("Authorization", "Bearer ${supabaseAnonKey.trim()}")
                    .header("apikey", supabaseAnonKey.trim())
                    .header("Content-Type", "application/json")
                    .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()

                client.newCall(req).execute().use { resp ->
                    val raw = resp.body?.string().orEmpty()
                    if (resp.isSuccessful && raw.isNotBlank()) {
                        val json = JSONObject(raw)
                        val dataObj = json.optJSONObject("data") ?: json
                        val status = dataObj.optString("status", "").lowercase()
                        val amountKobo = dataObj.optDouble("amount", 0.0)
                        val channel = dataObj.optString("channel", "paystack")
                        if (status == "success") {
                            return@withContext PaystackVerifyResult(
                                success = true,
                                status = "success",
                                amountNaira = amountKobo / 100.0,
                                reference = cleanRef,
                                channel = channel,
                                message = "Paystack payment verified ($channel)!"
                            )
                        }
                        if (status.isNotBlank()) {
                            return@withContext PaystackVerifyResult(
                                success = false,
                                status = status,
                                reference = cleanRef,
                                channel = channel,
                                message = "Paystack reports status '$status' for $cleanRef."
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }

        // 2. Fallback check against paystack_transactions table updated by paystack-webhook
        try {
            val tableUrl = "$cleanSb/rest/v1/paystack_transactions?reference=eq.$cleanRef&select=reference,status,amount_naira,channel&limit=1"
            if (AppDefenseManager.isTrustedHttpsUrl(tableUrl)) {
                val req = Request.Builder()
                    .url(tableUrl)
                    .header("Authorization", "Bearer ${supabaseAnonKey.trim()}")
                    .header("apikey", supabaseAnonKey.trim())
                    .get()
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val arr = JSONArray(resp.body?.string().orEmpty().ifBlank { "[]" })
                        if (arr.length() > 0) {
                            val row = arr.getJSONObject(0)
                            val st = row.optString("status").lowercase()
                            val amt = row.optDouble("amount_naira", 0.0)
                            val ch = row.optString("channel", "paystack")
                            if (st == "success") {
                                return@withContext PaystackVerifyResult(
                                    success = true,
                                    status = "success",
                                    amountNaira = amt,
                                    reference = cleanRef,
                                    channel = ch,
                                    message = "Verified via Paystack Webhook Ledger ($ch)."
                                )
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }

        PaystackVerifyResult(
            success = false,
            status = "pending",
            reference = cleanRef,
            message = "Payment not yet confirmed credited on Paystack ($cleanRef). Complete checkout in browser then tap Verify."
        )
    }

    fun buildEdgeFunctionSetupGuide(): String {
        return """
            === ODIYA PAYSTACK SUPABASE EDGE FUNCTIONS & WEBHOOK ===
            1. In Supabase Dashboard -> Edge Functions -> Secrets (or CLI):
               supabase secrets set PAYSTACK_SECRET_KEY=sk_live_xxxxxxxxx
               (NEVER put sk_live_... inside GitHub or Android source code!)

            2. Deploy the 3 Edge Functions (found in repo supabase/functions/):
               supabase functions deploy paystack-init --no-verify-jwt
               supabase functions deploy paystack-verify --no-verify-jwt
               supabase functions deploy paystack-webhook --no-verify-jwt

            3. Set Paystack Webhook URL in Paystack Dashboard -> Settings -> API Keys & Webhooks:
               https://<YOUR_PROJECT_REF>.supabase.co/functions/v1/paystack-webhook
               ('paystack-webhook' verifies x-paystack-signature via HMAC-SHA512 using PAYSTACK_SECRET_KEY,
                upserts public.paystack_transactions, and marks public.orders status = 'CONFIRMED'.)
        """.trimIndent()
    }
}
