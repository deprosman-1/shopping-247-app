package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.model.PaymentMethod
import java.text.NumberFormat
import java.util.Locale

data class MerchantLaunchResult(
    val launchedInstalledApp: Boolean,
    val installedPackageName: String?,
    val playStoreUrl: String,
    val clipboardSet: Boolean
)

object MerchantAppLauncher {

    fun candidatePackages(method: PaymentMethod): List<String> = when (method) {
        PaymentMethod.OPAY -> listOf("team.opay.pay", "team.opay.pay.merchant")
        PaymentMethod.MONIEPOINT -> listOf("com.moniepoint.personal", "com.moniepoint.business")
        PaymentMethod.PALMPAY -> listOf("com.transsnet.palmpay", "com.palmpay.business")
        else -> emptyList()
    }

    fun getGooglePlayStoreUrl(method: PaymentMethod): String = when (method) {
        PaymentMethod.OPAY -> "https://play.google.com/store/apps/details?id=team.opay.pay"
        PaymentMethod.MONIEPOINT -> "https://play.google.com/store/apps/details?id=com.moniepoint.personal"
        PaymentMethod.PALMPAY -> "https://play.google.com/store/apps/details?id=com.transsnet.palmpay"
        PaymentMethod.PAYSTACK -> "https://paystack.com/"
        else -> "https://play.google.com/store"
    }

    fun getGooglePlayMarketUri(method: PaymentMethod): String = when (method) {
        PaymentMethod.OPAY -> "market://details?id=team.opay.pay"
        PaymentMethod.MONIEPOINT -> "market://details?id=com.moniepoint.personal"
        PaymentMethod.PALMPAY -> "market://details?id=com.transsnet.palmpay"
        else -> getGooglePlayStoreUrl(method)
    }

    fun findInstalledMerchantPackage(context: Context, method: PaymentMethod): String? {
        val pm = context.packageManager ?: return null
        for (pkg in candidatePackages(method)) {
            val launchIntent = runCatching { pm.getLaunchIntentForPackage(pkg) }.getOrNull()
            if (launchIntent != null) return pkg
            val hasPkg = runCatching { pm.getPackageInfo(pkg, 0) }.getOrNull() != null
            if (hasPkg) return pkg
        }
        return null
    }

    fun copyAccountToClipboard(context: Context, accountNumber: String): Boolean {
        if (accountNumber.isBlank()) return false
        return runCatching {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: return false
            cm.setPrimaryClip(ClipData.newPlainText("ODIYA Merchant Account", accountNumber))
            true
        }.getOrDefault(false)
    }

    fun launchInstalledMerchantApp(
        context: Context,
        method: PaymentMethod,
        accountNumber: String,
        accountName: String,
        storeName: String,
        payableAmount: Double
    ): MerchantLaunchResult {
        val clipboardOk = copyAccountToClipboard(context, accountNumber)
        val naira = NumberFormat.getCurrencyInstance(Locale("en", "NG")).apply {
            maximumFractionDigits = 2
        }
        val formattedAmt = naira.format(payableAmount)
        ShopNotificationHelper.notifyActiveMerchantPaymentDetails(
            context = context,
            merchantTitle = method.title,
            accountNumber = accountNumber,
            accountName = accountName,
            formattedAmount = formattedAmt
        )

        val installedPkg = findInstalledMerchantPackage(context, method)
        if (installedPkg != null) {
            val intent = runCatching {
                context.packageManager?.getLaunchIntentForPackage(installedPkg)
            }.getOrNull()?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("odiya_account_number", accountNumber)
                putExtra("odiya_account_name", accountName)
                putExtra("odiya_store_name", storeName)
                putExtra("odiya_payable_amount", String.format(Locale.US, "%.2f", payableAmount))
                putExtra("odiya_formatted_amount", formattedAmt)
            }
            val started = if (intent != null) {
                runCatching {
                    context.startActivity(intent)
                    true
                }.getOrDefault(false)
            } else {
                false
            }
            return MerchantLaunchResult(
                launchedInstalledApp = started,
                installedPackageName = installedPkg,
                playStoreUrl = getGooglePlayStoreUrl(method),
                clipboardSet = clipboardOk
            )
        }

        // Merchant app not installed — do NOT force-open Google Play; let customer pay in embedded portal or tap install if they have no account.
        return MerchantLaunchResult(
            launchedInstalledApp = false,
            installedPackageName = null,
            playStoreUrl = getGooglePlayStoreUrl(method),
            clipboardSet = clipboardOk
        )
    }

    fun openGooglePlayForNewCustomer(context: Context, method: PaymentMethod): Boolean {
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse(getGooglePlayMarketUri(method))).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val marketOk = runCatching {
            context.startActivity(marketIntent)
            true
        }.getOrDefault(false)
        if (marketOk) return true

        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(getGooglePlayStoreUrl(method))).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(webIntent)
            true
        }.getOrDefault(false)
    }
}
