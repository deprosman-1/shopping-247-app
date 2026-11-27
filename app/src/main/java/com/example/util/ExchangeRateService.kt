package com.example.util

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class UsdNgnExchangeSnapshot(
    val rateNgnPerUsd: Double,
    val source: String,
    val fetchedAtMillis: Long
)

/**
 * Fetches live USD -> NGN currency exchange rates using real public HTTPS FX endpoints.
 * Returns null if offline or unreachable so callers can retain the last cached real rate.
 */
object ExchangeRateService {
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    fun fetchLiveUsdToNgnRate(): UsdNgnExchangeSnapshot? {
        // Primary public endpoint: open.er-api.com
        try {
            val req = Request.Builder()
                .url("https://open.er-api.com/v6/latest/USD")
                .header("Accept", "application/json")
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    val root = JSONObject(body)
                    val ngn = root.optJSONObject("rates")?.optDouble("NGN", 0.0) ?: 0.0
                    if (ngn > 100.0 && ngn < 100_000.0) {
                        return UsdNgnExchangeSnapshot(
                            rateNgnPerUsd = ngn,
                            source = "Live FX (open.er-api.com)",
                            fetchedAtMillis = System.currentTimeMillis()
                        )
                    }
                }
            }
        } catch (_: Exception) {
        }

        // Secondary fallback endpoint: fawazahmed0 currency-api on jsDelivr
        try {
            val req = Request.Builder()
                .url("https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.json")
                .header("Accept", "application/json")
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    val root = JSONObject(body)
                    val ngn = root.optJSONObject("usd")?.optDouble("ngn", 0.0) ?: 0.0
                    if (ngn > 100.0 && ngn < 100_000.0) {
                        return UsdNgnExchangeSnapshot(
                            rateNgnPerUsd = ngn,
                            source = "Live FX (Currency API)",
                            fetchedAtMillis = System.currentTimeMillis()
                        )
                    }
                }
            }
        } catch (_: Exception) {
        }

        return null
    }
}
