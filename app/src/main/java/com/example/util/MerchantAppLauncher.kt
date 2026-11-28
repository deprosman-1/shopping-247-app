package com.example.util

import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.util.Rational
import androidx.annotation.RequiresApi
import com.example.R
import com.example.data.model.PaymentMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.NumberFormat
import java.util.Locale

data class ActiveMerchantPaymentOverlay(
    val merchantTitle: String,
    val packageName: String?,
    val accountNumber: String,
    val accountName: String,
    val storeName: String,
    val payableAmount: Double,
    val formattedAmount: String,
    val plainAmount: String,
    val activatedAtMillis: Long = System.currentTimeMillis()
)

object MerchantPaymentOverlayManager {
    private val _activeOverlay = MutableStateFlow<ActiveMerchantPaymentOverlay?>(null)
    val activeOverlay: StateFlow<ActiveMerchantPaymentOverlay?> = _activeOverlay.asStateFlow()

    @Volatile
    private var autoPipPending: Boolean = false

    fun startOverlaySession(
        overlay: ActiveMerchantPaymentOverlay,
        requestAutoPip: Boolean
    ) {
        _activeOverlay.value = overlay
        autoPipPending = requestAutoPip
    }

    fun consumeAutoPip(): Boolean {
        val current = autoPipPending
        autoPipPending = false
        return current
    }

    fun markPipEntered() {
        autoPipPending = false
    }

    fun clearOverlay(context: Context? = null) {
        autoPipPending = false
        _activeOverlay.value = null
        if (context != null) {
            ShopNotificationHelper.cancelActiveMerchantPaymentDetails(context)
        }
    }
}

data class MerchantLaunchResult(
    val launchedInstalledApp: Boolean,
    val installedPackageName: String?,
    val playStoreUrl: String,
    val clipboardSet: Boolean,
    val embeddedAccountNumber: String = "",
    val embeddedFormattedAmount: String = "",
    val embeddedPlainAmount: String = "",
    val pipEntered: Boolean = false
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

    fun copyAmountToClipboard(
        context: Context,
        plainAmount: String
    ): Boolean {
        if (plainAmount.isBlank()) return false
        return runCatching {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: return false
            val cleanAmt = plainAmount.trim()
            cm.setPrimaryClip(ClipData.newPlainText("ODIYA Payable Amount", cleanAmt))
            true
        }.getOrDefault(false)
    }

    fun copyAccountToClipboard(
        context: Context,
        accountNumber: String,
        plainAmount: String = ""
    ): Boolean {
        val cleanAccount = accountNumber.replace(" ", "").trim()
        if (cleanAccount.isBlank()) return false
        return runCatching {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: return false
            // Issue two distinct setPrimaryClip calls so Android keyboard clipboard trays
            // (Gboard / Samsung / SwiftKey) record BOTH the payable amount in figures AND
            // the ODIYA account number as separate pasteable clipboard tiles instead of
            // dropping getItemAt(1).
            if (plainAmount.isNotBlank()) {
                cm.setPrimaryClip(ClipData.newPlainText("ODIYA Payable Amount", plainAmount.trim()))
            }
            cm.setPrimaryClip(ClipData.newPlainText("ODIYA Account Number", cleanAccount))
            true
        }.getOrDefault(false)
    }

    private fun findActivity(context: Context): Activity? {
        var current: Context? = context
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun buildPaymentPipParams(
        context: Context,
        overlay: ActiveMerchantPaymentOverlay
    ): PictureInPictureParams {
        val icon = Icon.createWithResource(context, R.drawable.shopping_logo_icon_1794752250458)

        val copyAcctPi = PendingIntent.getBroadcast(
            context,
            24911,
            Intent(context, MerchantClipboardActionReceiver::class.java).apply {
                action = MerchantClipboardActionReceiver.ACTION_COPY_MERCHANT_DETAIL
                putExtra(MerchantClipboardActionReceiver.EXTRA_COPY_LABEL, "ODIYA Account")
                putExtra(MerchantClipboardActionReceiver.EXTRA_COPY_VALUE, overlay.accountNumber)
                putExtra(
                    MerchantClipboardActionReceiver.EXTRA_TOAST_MESSAGE,
                    "Copied ODIYA Account: ${overlay.accountNumber}"
                )
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val copyAmtPi = PendingIntent.getBroadcast(
            context,
            24912,
            Intent(context, MerchantClipboardActionReceiver::class.java).apply {
                action = MerchantClipboardActionReceiver.ACTION_COPY_MERCHANT_DETAIL
                putExtra(MerchantClipboardActionReceiver.EXTRA_COPY_LABEL, "Payable Amount")
                putExtra(MerchantClipboardActionReceiver.EXTRA_COPY_VALUE, overlay.plainAmount)
                putExtra(
                    MerchantClipboardActionReceiver.EXTRA_TOAST_MESSAGE,
                    "Copied Payable Amount: ${overlay.formattedAmount}"
                )
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val actions = listOf(
            RemoteAction(
                icon,
                "Copy Acct",
                "Copy ODIYA Account ${overlay.accountNumber}",
                copyAcctPi
            ),
            RemoteAction(
                icon,
                "Copy Amount",
                "Copy Amount ${overlay.formattedAmount}",
                copyAmtPi
            )
        )

        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .setActions(actions)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setSeamlessResizeEnabled(true)
        }
        return builder.build()
    }

    fun tryEnterPaymentPipMode(
        context: Context,
        overlay: ActiveMerchantPaymentOverlay
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val activity = findActivity(context) ?: return false
        val hasPipFeature = runCatching {
            activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        }.getOrDefault(false)
        if (!hasPipFeature || activity.isFinishing || activity.isDestroyed) return false

        return runCatching {
            val params = buildPaymentPipParams(activity, overlay)
            val entered = activity.enterPictureInPictureMode(params)
            if (entered) {
                MerchantPaymentOverlayManager.markPipEntered()
            }
            entered
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
        val naira = NumberFormat.getCurrencyInstance(Locale("en", "NG")).apply {
            maximumFractionDigits = 2
        }
        val formattedAmt = naira.format(payableAmount)
        val plainAmt = String.format(Locale.US, "%.2f", payableAmount)
        val clipboardOk = copyAccountToClipboard(context, accountNumber, plainAmt)

        val installedPkg = findInstalledMerchantPackage(context, method)
        val overlay = ActiveMerchantPaymentOverlay(
            merchantTitle = method.title,
            packageName = installedPkg,
            accountNumber = accountNumber,
            accountName = accountName,
            storeName = storeName,
            payableAmount = payableAmount,
            formattedAmount = formattedAmt,
            plainAmount = plainAmt
        )
        MerchantPaymentOverlayManager.startOverlaySession(
            overlay = overlay,
            requestAutoPip = installedPkg != null
        )

        ShopNotificationHelper.notifyActiveMerchantPaymentDetails(
            context = context,
            merchantTitle = method.title,
            accountNumber = accountNumber,
            accountName = accountName,
            formattedAmount = formattedAmt,
            plainAmount = plainAmt
        )

        if (installedPkg != null) {
            val intent = runCatching {
                context.packageManager?.getLaunchIntentForPackage(installedPkg)
            }.getOrNull()?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("odiya_account_number", accountNumber)
                putExtra("odiya_account_name", accountName)
                putExtra("odiya_store_name", storeName)
                putExtra("odiya_payable_amount", plainAmt)
                putExtra("odiya_formatted_amount", formattedAmt)
                putExtra("account_number", accountNumber)
                putExtra("accountNumber", accountNumber)
                putExtra("account_name", accountName)
                putExtra("amount", plainAmt)
                putExtra("AMOUNT", plainAmt)
                putExtra("formatted_amount", formattedAmt)
                putExtra(Intent.EXTRA_TEXT, "$accountNumber | $formattedAmt | $accountName")
            }

            // Activate floating PiP embedded payment card before bringing up the merchant app
            // so ODIYA Account Number & Payable Amount remain visible right inside/over the opened app.
            val pipEntered = if (intent != null) tryEnterPaymentPipMode(context, overlay) else false

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
                clipboardSet = clipboardOk,
                embeddedAccountNumber = accountNumber,
                embeddedFormattedAmount = formattedAmt,
                embeddedPlainAmount = plainAmt,
                pipEntered = pipEntered
            )
        }

        // Merchant app not installed — do NOT force-open Google Play; let customer pay in embedded portal or tap install if they have no account.
        return MerchantLaunchResult(
            launchedInstalledApp = false,
            installedPackageName = null,
            playStoreUrl = getGooglePlayStoreUrl(method),
            clipboardSet = clipboardOk,
            embeddedAccountNumber = accountNumber,
            embeddedFormattedAmount = formattedAmt,
            embeddedPlainAmount = plainAmt,
            pipEntered = false
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
