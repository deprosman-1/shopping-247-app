package com.example.data.model

import androidx.compose.runtime.Immutable
import java.util.UUID

@Immutable
data class SavedAddress(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "Home",
    val recipientName: String = "",
    val phoneNumber: String = "",
    val streetAddress: String = "",
    val stateZone: String = "Lagos Mainland",
    val isDefault: Boolean = false
)

@Immutable
data class DeliveryZoneOption(
    val id: String,
    val zoneName: String,
    val statesSummary: String,
    val fee: Double,
    val etaLabel: String
)

enum class ReturnRefundStatus(val label: String) {
    PENDING_REVIEW("Pending Admin Review"),
    APPROVED_RETURN("Return Approved — Ship Back"),
    REFUNDED("Refund Completed"),
    DECLINED("Declined")
}

@Immutable
data class ReturnRefundRequest(
    val id: String = "RET-${UUID.randomUUID().toString().take(6).uppercase()}",
    val orderId: String,
    val customerId: String,
    val customerName: String,
    val customerPhone: String,
    val reason: String,
    val details: String,
    val proofImageUrl: String = "",
    val refundAmount: Double,
    val status: ReturnRefundStatus = ReturnRefundStatus.PENDING_REVIEW,
    val adminNote: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

enum class SupportTicketStatus(val label: String) {
    OPEN("Open"),
    IN_PROGRESS("Admin Reviewing"),
    RESOLVED("Resolved")
}

@Immutable
data class SupportTicket(
    val id: String = "TKT-${UUID.randomUUID().toString().take(6).uppercase()}",
    val customerId: String = "",
    val customerName: String,
    val customerPhone: String,
    val orderId: String = "",
    val subject: String,
    val message: String,
    val status: SupportTicketStatus = SupportTicketStatus.OPEN,
    val adminReply: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Immutable
data class LoyaltyAccount(
    val pointsBalance: Int = 0,
    val lifetimeEarnedPoints: Int = 0,
    val referralCode: String = "247-${UUID.randomUUID().toString().take(4).uppercase()}",
    val referredByCode: String = "",
    val referralBonusClaimed: Boolean = false,
    val totalOrdersRewarded: Int = 0
)

enum class OfflineActionType(val label: String) {
    CLOUD_INVENTORY_SYNC("Cloud Inventory & Orders Sync"),
    SUPPORT_TICKET_SYNC("Customer Support Ticket"),
    RETURN_REQUEST_SYNC("Return/Refund Request"),
    PREORDER_SYNC("Custom Pre-Order Request")
}

@Immutable
data class OfflineQueuedAction(
    val id: String = UUID.randomUUID().toString(),
    val type: OfflineActionType,
    val referenceId: String,
    val summary: String,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
)
