package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.model.Order
import com.example.data.model.StoreProfileConfig
import java.net.URLEncoder
import java.text.NumberFormat
import java.util.Locale

object WhatsAppHelper {

    fun formatWhatsAppNumber(rawPhone: String): String {
        val digits = rawPhone.filter { it.isDigit() }
        return when {
            digits.startsWith("0") && digits.length == 11 -> "234${digits.drop(1)}"
            digits.startsWith("234") -> digits
            digits.length >= 10 -> digits
            else -> digits
        }
    }

    fun openWhatsAppChat(
        context: Context,
        rawPhone: String,
        message: String
    ) {
        val formatted = formatWhatsAppNumber(rawPhone)
        val encoded = try {
            URLEncoder.encode(message, "UTF-8")
        } catch (_: Exception) {
            message
        }
        val url = if (formatted.isNotBlank()) {
            "https://api.whatsapp.com/send?phone=$formatted&text=$encoded"
        } else {
            "https://api.whatsapp.com/send?text=$encoded"
        }
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            Toast.makeText(context, "WhatsApp is not installed on this device.", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendCustomerOrderReceiptToStore(
        context: Context,
        order: Order,
        storeProfile: StoreProfileConfig
    ) {
        val naira = NumberFormat.getCurrencyInstance(Locale("en", "NG")).apply {
            maximumFractionDigits = 0
        }
        val targetPhone = storeProfile.whatsappNumber
            .ifBlank { storeProfile.phoneNumber }
            .ifBlank { storeProfile.secondaryPhoneNumber }
        val cleanItems = order.itemsSummary.replace(Regex("""\s*\[Sold:[^\]]+\]"""), "").trim()
        val msg = """
            Hello ${storeProfile.storeName}, I just submitted my payment receipt!
            
            *Order ID:* ${order.orderId}
            *Name:* ${order.customerName}
            *Phone:* ${order.customerPhone}
            *Delivery:* ${order.deliveryAddress}
            *Method:* ${order.paymentMethod.title}
            *Sender Name:* ${order.senderName}
            *Ref / STAN:* ${order.paymentReference}
            *Total Paid:* ${naira.format(order.totalAmount)}
            
            *Items:*
            $cleanItems
        """.trimIndent()
        openWhatsAppChat(context, targetPhone, msg)
    }

    fun sendAdminOrderStatusToCustomer(
        context: Context,
        order: Order,
        storeProfile: StoreProfileConfig
    ) {
        val naira = NumberFormat.getCurrencyInstance(Locale("en", "NG")).apply {
            maximumFractionDigits = 0
        }
        val msg = """
            Hello ${order.customerName.ifBlank { "Valued Customer" }}, update from *${storeProfile.storeName}*!
            
            *Order ID:* ${order.orderId}
            *Current Status:* ${order.status.label}
            *Amount:* ${naira.format(order.totalAmount)}
            ${if (order.verificationNote.isNotBlank()) "*Note:* ${order.verificationNote}\n" else ""}
            Thank you for shopping with us 24/7!
        """.trimIndent()
        openWhatsAppChat(context, order.customerPhone, msg)
    }

    fun sendPartnerPaymentAlert(
        context: Context,
        recipientPhone: String,
        partnerName: String,
        orderId: String,
        customerName: String,
        customerPhone: String,
        productDetails: String,
        grossAmount: Double,
        commissionPercent: Double,
        commissionProfit: Double,
        partnerNetPayout: Double,
        paymentMethodLabel: String,
        paymentAccount: String,
        paymentRef: String,
        storeProfile: StoreProfileConfig
    ) {
        val naira = NumberFormat.getCurrencyInstance(Locale("en", "NG")).apply {
            maximumFractionDigits = 0
        }
        val msg = """
            *AUTOMATED PARTNER PAYMENT ALERT*
            *Store:* ${storeProfile.storeName}
            *Partner Folder:* $partnerName
            
            *Order ID:* $orderId
            *Customer:* $customerName ($customerPhone)
            *Items:* $productDetails
            *Gross Sale Amount:* ${naira.format(grossAmount)}
            *24/7 SHOPPING Commission (${String.format(Locale.US, "%.1f", commissionPercent)}%):* ${naira.format(commissionProfit)}
            *Partner Net Payout:* ${naira.format(partnerNetPayout)}
            
            *Payment Channel:* $paymentMethodLabel
            *Settlement Account:* $paymentAccount
            *Reference:* $paymentRef
            
            Both ${storeProfile.storeName} and $partnerName have been notified.
        """.trimIndent()
        openWhatsAppChat(context, recipientPhone, msg)
    }

    fun sendPartnerSalesAndStockReport(
        context: Context,
        recipientPhone: String,
        reportText: String
    ) {
        openWhatsAppChat(context, recipientPhone, reportText)
    }
}
