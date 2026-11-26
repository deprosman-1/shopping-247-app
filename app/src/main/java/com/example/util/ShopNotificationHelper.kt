package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.OrderStatus

object ShopNotificationHelper {
    private const val CHANNEL_ORDERS = "orders_channel"
    private const val CHANNEL_PROMOS = "promos_channel"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ORDERS) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ORDERS,
                        "Order & Payment Updates",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Alerts when payment is confirmed, dispatched, or delivered."
                    }
                )
            }
            if (nm.getNotificationChannel(CHANNEL_PROMOS) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_PROMOS,
                        "Deals & Promo Alerts",
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply {
                        description = "Flash promo discounts and custom pre-order quotes."
                    }
                )
            }
        }
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun launchAppPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun notifyOrderStatusChange(
        context: Context,
        orderId: String,
        newStatus: OrderStatus,
        note: String = ""
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        val (title, body) = when (newStatus) {
            OrderStatus.CONFIRMED, OrderStatus.VERIFIED ->
                "Payment Confirmed ($orderId)" to (note.ifBlank { "Your payment was confirmed and your order is now processing." })
            OrderStatus.OUT_FOR_DELIVERY ->
                "Out for Delivery ($orderId)" to (note.ifBlank { "Your ODIYA order has been dispatched and is on the way!" })
            OrderStatus.DELIVERED ->
                "Delivered ($orderId)" to (note.ifBlank { "Your order has arrived. Enjoy your items and leave a review!" })
            OrderStatus.CANCELLED ->
                "Order Update ($orderId)" to (note.ifBlank { "Your order status changed to Cancelled." })
            OrderStatus.PENDING_VERIFICATION ->
                "Receipt Submitted ($orderId)" to (note.ifBlank { "Payment receipt submitted and awaiting verification." })
        }
        postNotification(context, CHANNEL_ORDERS, orderId.hashCode(), title, body)
    }

    fun notifyPromoActivated(
        context: Context,
        title: String,
        discountPercent: Int,
        category: String?
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        val scopeLabel = category?.let { "on $it" } ?: "Storewide"
        postNotification(
            context = context,
            channelId = CHANNEL_PROMOS,
            notificationId = ("promo_$title").hashCode(),
            title = "Flash Promo Live: $discountPercent% OFF $scopeLabel!",
            body = title.ifBlank { "ODIYA Promo Discount is active now." }
        )
    }

    fun notifyPreOrderReply(
        context: Context,
        requestId: String,
        productName: String,
        statusLabel: String
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        postNotification(
            context = context,
            channelId = CHANNEL_ORDERS,
            notificationId = requestId.hashCode(),
            title = "Pre-Order Update: $productName",
            body = "Status updated to: $statusLabel"
        )
    }

    fun notifyAdminNewPreOrder(
        context: Context,
        requestId: String,
        customerName: String,
        productName: String,
        brandName: String
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        postNotification(
            context = context,
            channelId = CHANNEL_ORDERS,
            notificationId = ("preorder_admin_$requestId").hashCode(),
            title = "New Customer Pre-Order ($requestId)",
            body = "${customerName.ifBlank { "Customer" }} requested \"$productName\" (${brandName.ifBlank { "Custom" }}). Open Admin File to quote & reply."
        )
    }

    fun notifyPartnerPaymentAlert(
        context: Context,
        partnerName: String,
        orderId: String,
        grossAmountText: String,
        commissionProfitText: String,
        partnerPayoutText: String,
        paymentAccount: String
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        postNotification(
            context = context,
            channelId = CHANNEL_ORDERS,
            notificationId = ("partner_${partnerName}_$orderId").hashCode(),
            title = "Partner Payment Alert • $partnerName ($orderId)",
            body = "Paid $grossAmountText via $paymentAccount. ODIYA Profit: $commissionProfitText | Partner Payout: $partnerPayoutText."
        )
    }

    fun notifyBackInStock(
        context: Context,
        productId: String,
        productTitle: String
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        postNotification(
            context = context,
            channelId = CHANNEL_PROMOS,
            notificationId = ("restock_$productId").hashCode(),
            title = "Back in Stock: $productTitle",
            body = "Good news! $productTitle is available again on ODIYA. Tap to grab yours before it sells out."
        )
    }

    fun notifyReturnRefundUpdate(
        context: Context,
        requestId: String,
        orderId: String,
        statusLabel: String,
        adminNote: String = ""
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        postNotification(
            context = context,
            channelId = CHANNEL_ORDERS,
            notificationId = ("ret_$requestId").hashCode(),
            title = "Return/Refund Update ($orderId)",
            body = "$statusLabel${if (adminNote.isNotBlank()) " — $adminNote" else ""}"
        )
    }

    fun notifySupportTicketReply(
        context: Context,
        ticketId: String,
        subject: String,
        reply: String
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        postNotification(
            context = context,
            channelId = CHANNEL_ORDERS,
            notificationId = ("tkt_$ticketId").hashCode(),
            title = "Support Reply • $subject",
            body = reply.ifBlank { "An admin has updated your support ticket ($ticketId)." }
        )
    }

    fun notifyOfflineQueueSynced(
        context: Context,
        syncedCount: Int
    ) {
        if (syncedCount <= 0 || !hasNotificationPermission(context)) return
        ensureChannels(context)
        postNotification(
            context = context,
            channelId = CHANNEL_ORDERS,
            notificationId = 24799,
            title = "Connection Restored • Synced",
            body = "$syncedCount queued offline update(s) synced safely to ODIYA Cloud."
        )
    }

    fun notifyIncomingLiveCall(
        context: Context,
        callerName: String,
        isVideoCall: Boolean,
        isLiveChat: Boolean = false
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        val kind = when {
            isLiveChat -> "Live Chat"
            isVideoCall -> "Live Video Call"
            else -> "Live Audio Call"
        }
        postNotification(
            context = context,
            channelId = CHANNEL_ORDERS,
            notificationId = 24805,
            title = "Incoming $kind • $callerName",
            body = "$callerName is reaching Admin live on ODIYA. Tap to answer via Live Chat, Audio, or Video."
        )
    }

    fun notifyAdminPartnershipInterest(
        context: Context,
        customerName: String,
        businessName: String,
        categories: List<String>
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        postNotification(
            context = context,
            channelId = CHANNEL_ORDERS,
            notificationId = ("partner_interest_${customerName}_$businessName").hashCode(),
            title = "Partnership Interest • ${customerName.ifBlank { "Customer" }}",
            body = "${businessName.ifBlank { customerName }} expressed partnership interest (${categories.joinToString(", ").ifBlank { "General" }}). Open Admin Partners tab to Accept or Reject."
        )
    }

    fun notifyCustomerPartnershipDecision(
        context: Context,
        accepted: Boolean,
        adminReply: String
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        postNotification(
            context = context,
            channelId = CHANNEL_ORDERS,
            notificationId = 24866,
            title = if (accepted) "Partnership Offer Accepted! 🎉" else "Partnership Offer Update",
            body = adminReply.ifBlank {
                if (accepted) "Admin accepted your partnership offer! Complete your MOU form & append signature."
                else "Admin reviewed your partnership offer. Open Partnership in your account to view details."
            }
        )
    }

    fun notifyAppUpdateAvailable(
        context: Context,
        versionName: String,
        versionCode: Int,
        notes: String = ""
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        postNotification(
            context = context,
            channelId = CHANNEL_ORDERS,
            notificationId = 24810 + versionCode,
            title = "New ODIYA Update Ready (v$versionName)",
            body = notes.ifBlank { "Tap to install the latest features and live upgrades on your phone." }
        )
    }

    fun notifyNewProductAlert(
        context: Context,
        productTitle: String,
        category: String,
        priceText: String,
        targetPhone: String = ""
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        val phoneSuffix = if (targetPhone.isNotBlank()) " • Sent to $targetPhone" else ""
        postNotification(
            context = context,
            channelId = CHANNEL_PROMOS,
            notificationId = ("new_prod_${productTitle}_$category").hashCode(),
            title = "New Arrival on ODIYA: $productTitle",
            body = "Category: $category • Price: $priceText$phoneSuffix. Tap to view & shop now!"
        )
    }

    fun notifyAppMaintenance(
        context: Context,
        title: String,
        message: String
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        postNotification(
            context = context,
            channelId = CHANNEL_ORDERS,
            notificationId = 24899,
            title = title.ifBlank { "ODIYA App Maintenance & Upgrade" },
            body = message.ifBlank { "Scheduled system upgrade in progress for faster shopping. Thank you for your patience." }
        )
    }

    fun notifyCustomerNewReviewToAdmin(
        context: Context,
        reviewerName: String,
        productTitle: String,
        rating: Int
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        postNotification(
            context = context,
            channelId = CHANNEL_ORDERS,
            notificationId = ("review_${reviewerName}_$productTitle").hashCode(),
            title = "New Customer Rating ($rating★) • $productTitle",
            body = "${reviewerName.ifBlank { "Customer" }} submitted a $rating-star rating & feedback. Open Admin Feedback tab to view or reply."
        )
    }

    fun notifyPaymentConfirmedReceipt(
        context: Context,
        orderId: String,
        portalTitle: String,
        amountText: String
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        postNotification(
            context = context,
            channelId = CHANNEL_ORDERS,
            notificationId = ("pay_ok_$orderId").hashCode(),
            title = "ORDER & PAYMENT CONFIRMED ($orderId)",
            body = "ODIYA account credited $amountText via $portalTitle. Your ORDER & PAYMENT CONFIRMATION RECEIPT is ready!"
        )
    }

    fun notifyPaymentFailedUncredited(
        context: Context,
        orderId: String,
        portalTitle: String,
        amountText: String,
        isNetworkDelay: Boolean = false
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        val title = if (isNetworkDelay) {
            "ORDER & PAYMENT PENDING NETWORK ($orderId)"
        } else {
            "ORDER & PAYMENT FAILED ($orderId)"
        }
        val body = if (isNetworkDelay) {
            "Network delay detected for $amountText via $portalTitle. Once network restores, transfer pushes automatically to credit ODIYA."
        } else {
            "ODIYA account has NOT received credit alert for $amountText ($portalTitle) yet. Status turns to ORDER & PAYMENT CONFIRMED once credited."
        }
        postNotification(
            context = context,
            channelId = CHANNEL_ORDERS,
            notificationId = ("pay_fail_$orderId").hashCode(),
            title = title,
            body = body
        )
    }

    fun notifyActiveMerchantPaymentDetails(
        context: Context,
        merchantTitle: String,
        accountNumber: String,
        accountName: String,
        formattedAmount: String
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        postNotification(
            context = context,
            channelId = CHANNEL_ORDERS,
            notificationId = 24899,
            title = "ODIYA Account Ready • $merchantTitle ($formattedAmount)",
            body = "Account: $accountNumber ($accountName) • Payable Amount: $formattedAmount. After completing transfer in $merchantTitle, tap here to return to ODIYA Checkout & forward your Word/PDF receipt."
        )
    }

    private fun postNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        body: String
    ) {
        try {
            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.shopping_logo_icon_1794752250458)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(launchAppPendingIntent(context))
                .setAutoCancel(true)

            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
        } catch (_: Exception) {
        }
    }
}
