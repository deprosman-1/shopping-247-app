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
    val isVerifiedPaid: Boolean = false,
    val purpose: String = "ORDER_CHECKOUT",
    val partnerId: String = "",
    val hostingOffsetAppliedNaira: Double = 0.0
)

data class PaystackBankOption(
    val name: String,
    val code: String
)

data class PartnerPaystackSplitAllocation(
    val partnerId: String,
    val partnerName: String,
    val subaccountCode: String,
    val grossNaira: Double,
    val commissionPercent: Double,
    val commissionNaira: Double,
    val hostingOffsetNaira: Double,
    val partnerNetNaira: Double
) {
    val partnerNetKobo: Long get() = Math.round(partnerNetNaira * 100.0).coerceAtLeast(0L)
    val commissionKobo: Long get() = Math.round(commissionNaira * 100.0).coerceAtLeast(0L)
    val hostingOffsetKobo: Long get() = Math.round(hostingOffsetNaira * 100.0).coerceAtLeast(0L)
}

data class PaystackSplitPlan(
    val splitMode: String = "STANDARD",
    val totalAmountNaira: Double = 0.0,
    val odiyaStoreItemsAndDeliveryNaira: Double = 0.0,
    val odiyaCommissionNaira: Double = 0.0,
    val hostingOffsetNaira: Double = 0.0,
    val odiyaTotalShareNaira: Double = 0.0,
    val totalPartnerNetNaira: Double = 0.0,
    val allocations: List<PartnerPaystackSplitAllocation> = emptyList()
) {
    val hasActiveSubaccountSplit: Boolean
        get() = allocations.any { it.subaccountCode.startsWith("ACCT_") && it.partnerNetKobo >= 100L }

    val hasPartnerItems: Boolean
        get() = allocations.isNotEmpty() || odiyaCommissionNaira > 0.0 || hostingOffsetNaira > 0.0

    val odiyaTotalShareKobo: Long
        get() = Math.round(odiyaTotalShareNaira * 100.0).coerceAtLeast(0L)

    val odiyaCommissionKobo: Long
        get() = Math.round(odiyaCommissionNaira * 100.0).coerceAtLeast(0L)

    val hostingOffsetKobo: Long
        get() = Math.round(hostingOffsetNaira * 100.0).coerceAtLeast(0L)

    val totalPartnerNetKobo: Long
        get() = Math.round(totalPartnerNetNaira * 100.0).coerceAtLeast(0L)
}

data class PaystackSubaccountResult(
    val success: Boolean,
    val subaccountCode: String = "",
    val accountName: String = "",
    val bankName: String = "",
    val message: String = ""
)

/**
 * Supabase Edge Function-backed Paystack Merchant Gateway client.
 * Security Architecture:
 * - Secret Key (`sk_live_...` / `sk_test_...`) NEVER leaves Supabase Edge Functions.
 * - Client only communicates over HTTPS with Supabase Edge Functions (`paystack-init`, `paystack-verify`, `paystack-subaccount`)
 *   and opens the Paystack Hosted Checkout (`authorization_url`) in the system browser / Custom Tab.
 */
class PaystackGatewayService {
    companion object {
        val SUPPORTED_NIGERIAN_SETTLEMENT_BANKS = listOf(
            PaystackBankOption("OPay Digital Services (OPay)", "999992"),
            PaystackBankOption("Moniepoint MFB", "50515"),
            PaystackBankOption("PalmPay", "999991"),
            PaystackBankOption("Kuda Bank", "50211"),
            PaystackBankOption("Access Bank", "044"),
            PaystackBankOption("Guaranty Trust Bank (GTBank)", "058"),
            PaystackBankOption("First Bank of Nigeria", "011"),
            PaystackBankOption("Zenith Bank", "057"),
            PaystackBankOption("United Bank for Africa (UBA)", "033"),
            PaystackBankOption("Fidelity Bank", "070"),
            PaystackBankOption("Wema Bank", "035"),
            PaystackBankOption("Stanbic IBTC Bank", "221"),
            PaystackBankOption("Sterling Bank", "232"),
            PaystackBankOption("Polaris Bank", "076"),
            PaystackBankOption("Union Bank of Nigeria", "032")
        )
    }

    fun calculatePaystackSplitPlan(
        cartItems: List<com.example.data.model.CartItem>,
        partners: List<com.example.data.model.PartnerOrganization>,
        deliveryFeeNaira: Double,
        couponDiscountNaira: Double = 0.0,
        loyaltyDiscountNaira: Double = 0.0,
        customBargainTotal: Double? = null,
        applyDueHostingFeeOffset: Boolean = true
    ): PaystackSplitPlan {
        val rawSubtotal = cartItems.sumOf { it.unitPrice * it.quantity }.coerceAtLeast(0.0)
        val safeCoupon = couponDiscountNaira.coerceIn(0.0, rawSubtotal)
        val safeLoyalty = loyaltyDiscountNaira.coerceIn(0.0, (rawSubtotal - safeCoupon).coerceAtLeast(0.0))
        val discountedItemsTotal = if (customBargainTotal != null) {
            (customBargainTotal - deliveryFeeNaira).coerceAtLeast(0.0)
        } else {
            (rawSubtotal - safeCoupon - safeLoyalty).coerceAtLeast(0.0)
        }
        val discountFactor = if (rawSubtotal > 0.0) {
            (discountedItemsTotal / rawSubtotal).coerceIn(0.0, 2.0)
        } else 1.0

        val totalPayableNaira = customBargainTotal ?: (discountedItemsTotal + deliveryFeeNaira.coerceAtLeast(0.0))
        val storeOwnItemsNaira = cartItems
            .filter { it.product.partnerId.isBlank() }
            .sumOf { (it.unitPrice * it.quantity) * discountFactor }
        val odiyaStoreAndDeliveryNaira = (storeOwnItemsNaira + deliveryFeeNaira.coerceAtLeast(0.0)).coerceAtLeast(0.0)

        val partnerGroups = cartItems.filter { it.product.partnerId.isNotBlank() }.groupBy { it.product.partnerId }
        val allocations = mutableListOf<PartnerPaystackSplitAllocation>()
        var totalCommission = 0.0
        var totalHostingOffset = 0.0
        var totalPartnerNet = 0.0

        partnerGroups.forEach { (partnerId, pItems) ->
            val org = partners.find { it.id == partnerId }
            val partnerName = org?.businessName ?: pItems.firstOrNull()?.product?.partnerName ?: "Partner Business"
            val grossNaira = pItems.sumOf { (it.unitPrice * it.quantity) * discountFactor }
            val commissionNaira = pItems.sumOf { item ->
                val pct = item.product.partnerCommissionPercent.takeIf { it > 0.0 }
                    ?: org?.stipulatedCommissionPercent
                    ?: 10.0
                ((item.unitPrice * item.quantity) * discountFactor) * (pct.coerceIn(0.0, 95.0) / 100.0)
            }
            val rawPartnerNet = (grossNaira - commissionNaira).coerceAtLeast(0.0)
            val dueHosting = if (applyDueHostingFeeOffset && org?.autoDeductHostingFromPaystackSplit == true) {
                org.pendingHostingFeeBalance.coerceAtLeast(0.0)
            } else 0.0
            val hostingOffset = minOf(rawPartnerNet, dueHosting)
            val finalPartnerNet = (rawPartnerNet - hostingOffset).coerceAtLeast(0.0)
            val effPct = if (grossNaira > 0.0) Math.round((commissionNaira / grossNaira) * 1000.0) / 10.0 else (org?.stipulatedCommissionPercent ?: 10.0)

            totalCommission += commissionNaira
            totalHostingOffset += hostingOffset
            totalPartnerNet += finalPartnerNet

            allocations.add(
                PartnerPaystackSplitAllocation(
                    partnerId = partnerId,
                    partnerName = partnerName,
                    subaccountCode = org?.paystackSubaccountCode?.trim().orEmpty(),
                    grossNaira = grossNaira,
                    commissionPercent = effPct,
                    commissionNaira = commissionNaira,
                    hostingOffsetNaira = hostingOffset,
                    partnerNetNaira = finalPartnerNet
                )
            )
        }

        val activeSubAllocations = allocations.filter { it.subaccountCode.startsWith("ACCT_") && it.partnerNetKobo >= 100L }
        val splitMode = when {
            activeSubAllocations.size >= 2 -> "PARTNER_MULTI_SPLIT"
            activeSubAllocations.size == 1 -> "PARTNER_SUBACCOUNT"
            else -> "STANDARD"
        }

        val odiyaTotalShare = (totalPayableNaira - totalPartnerNet).coerceAtLeast(0.0)
        return PaystackSplitPlan(
            splitMode = splitMode,
            totalAmountNaira = totalPayableNaira,
            odiyaStoreItemsAndDeliveryNaira = odiyaStoreAndDeliveryNaira,
            odiyaCommissionNaira = totalCommission,
            hostingOffsetNaira = totalHostingOffset,
            odiyaTotalShareNaira = odiyaTotalShare,
            totalPartnerNetNaira = totalPartnerNet,
            allocations = allocations
        )
    }
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
        referenceOverride: String = "",
        splitPlan: PaystackSplitPlan? = null,
        purpose: String = "ORDER_CHECKOUT",
        partnerId: String = "",
        partnerName: String = "",
        hostingFeeNaira: Double = 0.0
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
        val activeAllocations = splitPlan?.allocations
            ?.filter { it.subaccountCode.startsWith("ACCT_") && it.partnerNetKobo >= 100L }
            .orEmpty()
        val resolvedPurpose = purpose.ifBlank { "ORDER_CHECKOUT" }
        val resolvedSplitMode = when {
            resolvedPurpose == "PARTNER_HOSTING_FEE" -> "HOSTING_FEE_100_ODIYA"
            splitPlan != null && activeAllocations.isNotEmpty() -> splitPlan.splitMode
            else -> "STANDARD"
        }

        val payload = JSONObject().apply {
            put("order_id", orderId)
            put("reference", ref)
            put("amount", amountKobo)
            put("amount_naira", amountNaira)
            put("currency", "NGN")
            put("email", safeEmail)
            put("callback_url", callbackUrl.ifBlank { "depros://paystack-callback" })
            put("purpose", resolvedPurpose)
            put("split_mode", resolvedSplitMode)
            put("bearer", "account")

            if (resolvedPurpose == "PARTNER_HOSTING_FEE") {
                val hKobo = Math.round(hostingFeeNaira.coerceAtLeast(amountNaira) * 100.0).coerceAtLeast(amountKobo)
                put("partner_id", partnerId)
                put("partner_name", partnerName)
                put("odiya_share_kobo", amountKobo)
                put("hosting_fee_kobo", hKobo)
                put("odiya_commission_kobo", 0L)
            } else if (splitPlan != null && activeAllocations.isNotEmpty()) {
                val primaryPartner = activeAllocations.first()
                put("partner_id", partnerId.ifBlank { primaryPartner.partnerId })
                put("partner_name", partnerName.ifBlank { primaryPartner.partnerName })
                put("odiya_share_kobo", splitPlan.odiyaTotalShareKobo)
                put("odiya_commission_kobo", splitPlan.odiyaCommissionKobo)
                put("hosting_fee_kobo", splitPlan.hostingOffsetKobo)
                if (activeAllocations.size == 1) {
                    put("subaccount", primaryPartner.subaccountCode)
                    val odiyaCharge = (amountKobo - primaryPartner.partnerNetKobo).coerceAtLeast(0L)
                    if (odiyaCharge > 0L) {
                        put("transaction_charge", odiyaCharge)
                    }
                }
                put("split_allocations", JSONArray().apply {
                    activeAllocations.forEach { alloc ->
                        put(JSONObject().apply {
                            put("partner_id", alloc.partnerId)
                            put("partner_name", alloc.partnerName)
                            put("subaccount", alloc.subaccountCode)
                            put("partner_net_kobo", alloc.partnerNetKobo)
                            put("commission_kobo", alloc.commissionKobo)
                            put("hosting_offset_kobo", alloc.hostingOffsetKobo)
                        })
                    }
                })
            }

            put("metadata", JSONObject().apply {
                put("order_id", orderId)
                put("purpose", resolvedPurpose)
                put("split_mode", resolvedSplitMode)
                if (partnerId.isNotBlank() || activeAllocations.isNotEmpty()) {
                    put("partner_id", partnerId.ifBlank { activeAllocations.first().partnerId })
                    put("partner_name", partnerName.ifBlank { activeAllocations.first().partnerName })
                }
                if (splitPlan != null) {
                    put("odiya_share_kobo", splitPlan.odiyaTotalShareKobo)
                    put("odiya_commission_kobo", splitPlan.odiyaCommissionKobo)
                    put("hosting_fee_kobo", splitPlan.hostingOffsetKobo)
                    put("partner_net_kobo", splitPlan.totalPartnerNetKobo)
                } else if (resolvedPurpose == "PARTNER_HOSTING_FEE") {
                    put("odiya_share_kobo", amountKobo)
                    put("hosting_fee_kobo", amountKobo)
                    put("partner_net_kobo", 0L)
                }
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

    suspend fun createOrUpdatePartnerSubaccount(
        supabaseUrl: String,
        supabaseAnonKey: String,
        overrideEdgeBaseUrl: String,
        partnerId: String,
        businessName: String,
        bankCode: String,
        accountNumber: String,
        commissionPercent: Double,
        existingSubaccountCode: String = "",
        contactEmail: String = "",
        contactPhone: String = ""
    ): PaystackSubaccountResult = withContext(Dispatchers.IO) {
        val edgeBase = resolveEdgeBaseUrl(supabaseUrl, overrideEdgeBaseUrl)
        if (edgeBase.isBlank() || supabaseAnonKey.isBlank()) {
            return@withContext PaystackSubaccountResult(
                success = false,
                message = "Connect Supabase Cloud URL & Anon Key first to register Paystack subaccounts."
            )
        }
        val cleanAcc = accountNumber.filter { it.isDigit() }
        if (cleanAcc.length < 10 || bankCode.isBlank()) {
            return@withContext PaystackSubaccountResult(
                success = false,
                message = "Select settlement bank and enter a valid 10-digit account number."
            )
        }
        val endpoint = "$edgeBase/paystack-subaccount"
        if (!AppDefenseManager.isTrustedHttpsUrl(endpoint)) {
            return@withContext PaystackSubaccountResult(false, message = "Subaccount endpoint must use HTTPS.")
        }
        val payload = JSONObject().apply {
            put("action", "create")
            put("partner_id", partnerId)
            put("business_name", AppDefenseManager.sanitizeTextInput(businessName, 80))
            put("bank_code", bankCode.trim())
            put("account_number", cleanAcc)
            put("percentage_charge", commissionPercent.coerceIn(0.5, 95.0))
            if (existingSubaccountCode.startsWith("ACCT_")) {
                put("subaccount_code", existingSubaccountCode.trim())
            }
            if (contactEmail.contains("@")) put("primary_contact_email", contactEmail.trim())
            if (contactPhone.isNotBlank()) put("primary_contact_phone", contactPhone.trim())
        }
        try {
            val req = Request.Builder()
                .url(endpoint)
                .header("Authorization", "Bearer ${supabaseAnonKey.trim()}")
                .header("apikey", supabaseAnonKey.trim())
                .header("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
            client.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                val json = runCatching { JSONObject(raw.ifBlank { "{}" }) }.getOrDefault(JSONObject())
                val dataObj = json.optJSONObject("data") ?: JSONObject()
                val subCode = dataObj.optString("subaccount_code").ifBlank { existingSubaccountCode }.trim()
                if (!resp.isSuccessful || !json.optBoolean("status", false) || subCode.isBlank()) {
                    val hint = when (resp.code) {
                        404 -> "Deploy 'paystack-subaccount' Edge Function in Supabase, or paste an ACCT_... subaccount code manually."
                        else -> json.optString("message").ifBlank { "Paystack subaccount HTTP ${resp.code}" }
                    }
                    return@withContext PaystackSubaccountResult(false, message = hint)
                }
                PaystackSubaccountResult(
                    success = true,
                    subaccountCode = subCode,
                    accountName = dataObj.optString("account_name"),
                    bankName = dataObj.optString("settlement_bank"),
                    message = "Paystack Split Subaccount active: $subCode"
                )
            }
        } catch (e: Exception) {
            PaystackSubaccountResult(
                success = false,
                message = "Could not reach paystack-subaccount Edge Function: ${e.message ?: "Network error"}"
            )
        }
    }

    fun buildEdgeFunctionSetupGuide(): String {
        return """
            === ODIYA PAYSTACK SPLIT & SUBACCOUNT EDGE FUNCTIONS ===
            1. In Supabase Dashboard -> Edge Functions -> Secrets (or CLI):
               supabase secrets set PAYSTACK_SECRET_KEY=sk_live_xxxxxxxxx
               (NEVER put sk_live_... inside GitHub or Android source code!)

            2. Deploy the 4 Edge Functions (found in repo supabase/functions/):
               supabase functions deploy paystack-init --no-verify-jwt
               supabase functions deploy paystack-verify --no-verify-jwt
               supabase functions deploy paystack-webhook --no-verify-jwt
               supabase functions deploy paystack-subaccount --no-verify-jwt

            3. Set Paystack Webhook URL in Paystack Dashboard -> Settings -> API Keys & Webhooks:
               https://<YOUR_PROJECT_REF>.supabase.co/functions/v1/paystack-webhook
               ('paystack-init' routes ODIYA % commissions + due Hosting Fees to ODIYA's main account
                and Partner's net share directly to Partner ACCT_... subaccounts automatically!)
        """.trimIndent()
    }
}
