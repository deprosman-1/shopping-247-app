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
                "Out for Delivery ($orderId)" to (note.ifBlank { "Your 24/7 SHOPPING order has been dispatched and is on the way!" })
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
            body = title.ifBlank { "24/7 SHOPPING Promo Discount is active now." }
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
            body = "Paid $grossAmountText via $paymentAccount. 24/7 Profit: $commissionProfitText | Partner Payout: $partnerPayoutText."
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
            body = "Good news! $productTitle is available again on 24/7 SHOPPING. Tap to grab yours before it sells out."
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
            body = "$syncedCount queued offline update(s) synced safely to 24/7 SHOPPING Cloud."
        )
    }

    fun notifyIncomingLiveCall(
        context: Context,
        callerName: String,
        isVideoCall: Boolean
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        val kind = if (isVideoCall) "Live Video Call" else "Live Audio Call"
        postNotification(
            context = context,
            channelId = CHANNEL_ORDERS,
            notificationId = 24805,
            title = "Incoming $kind • $callerName",
            body = "$callerName is calling Admin live on 24/7 SHOPPING. Tap to answer in Audio or Video mode."
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
