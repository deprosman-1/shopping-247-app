package com.example.data.model

import java.util.UUID

enum class PartnerAgreementStatus(val label: String) {
    PENDING_FINALIZATION("Awaiting 24/7 SHOPPING Signature"),
    FINALIZED("Agreement Finalized & Active"),
    SUSPENDED("Paused / Expired")
}

enum class HostingPeriodUnit(val label: String) {
    PER_WEEK("Per Week"),
    PER_MONTH("Per Month"),
    PER_QUARTER("Per Quarter"),
    PER_DURATION("Full Hosting Duration")
}

enum class PartnerSubmissionStatus(val label: String) {
    PENDING_ADMIN_VERIFICATION("In Admin Control Room (Not on Supabase)"),
    VERIFIED_READY_FOR_CLOUD("Verified • Ready for Batch Cloud Upload"),
    UPLOADED_TO_SUPABASE("Live on Supabase Cloud & Catalog"),
    REJECTED("Declined by Admin")
}

enum class PartnerTransactionType(val label: String) {
    PRODUCT_UPLOADED_TO_ADMIN("Uploaded to Admin Control Room"),
    ADMIN_VERIFIED_PRODUCT("Verified by Admin"),
    BATCH_UPLOADED_TO_SUPABASE("Batch Uploaded to Supabase Cloud"),
    CUSTOMER_ORDER_PAYMENT_ALERT("Customer Order Payment Alert"),
    ORDER_CONFIRMED_SALE("Confirmed Sale & Profit Split"),
    MONTHLY_HOSTING_FEE("Monthly Hosting Payment")
}

enum class SalesReportPeriod(val label: String, val shortLabel: String, val lookbackDays: Long) {
    DAILY("Daily Report (24 Hours)", "Daily", 1L),
    WEEKLY("Weekly Report (7 Days)", "Weekly", 7L),
    MONTHLY("Monthly Report (30 Days)", "Monthly", 30L)
}

data class PartnerOrganization(
    val id: String = "PRT-${UUID.randomUUID().toString().take(6).uppercase()}",
    val businessName: String,
    val ownerName: String,
    val ownerPhone: String,
    val businessAddress: String,
    val primaryCategory: String,
    val opayAccountNumber: String = "",
    val opayAccountName: String = "",
    val moniepointAccountNumber: String = "",
    val moniepointAccountName: String = "",
    val agreementDate: String,
    val hostingDuration: String = "6 Months",
    val agreedQuantityPerPeriod: Int = 20,
    val periodUnit: HostingPeriodUnit = HostingPeriodUnit.PER_MONTH,
    val monthlyHostingPayment: Double = 15000.0,
    val stipulatedCommissionPercent: Double = 10.0,
    val partnerSignatureName: String = "",
    val partnerSignatureStrokes: String = "",
    val adminCounterSignatureName: String = "",
    val adminCounterSignatureStrokes: String = "",
    val adminSignedAt: Long = 0L,
    val status: PartnerAgreementStatus = PartnerAgreementStatus.PENDING_FINALIZATION,
    val createdAt: Long = System.currentTimeMillis()
)

data class PartnerProductSubmission(
    val id: String = "PSUB-${UUID.randomUUID().toString().take(8).uppercase()}",
    val partnerId: String,
    val partnerName: String,
    val category: String,
    val productName: String,
    val brand: String,
    val quantity: Int,
    val sizesCsv: String,
    val colors: List<ProductColor>,
    val sellingPrice: Double,
    val promoDiscountPercent: Int = 0,
    val commissionPercent: Double = 10.0,
    val imageUrl: String,
    val description: String = "",
    val batchNumber: String = "",
    val status: PartnerSubmissionStatus = PartnerSubmissionStatus.PENDING_ADMIN_VERIFICATION,
    val publishedProductId: String = "",
    val adminNote: String = "",
    val submittedAt: Long = System.currentTimeMillis(),
    val verifiedAt: Long = 0L,
    val uploadedToCloudAt: Long = 0L
)

data class PartnerFolderTransaction(
    val id: String = "PTXN-${UUID.randomUUID().toString().take(8).uppercase()}",
    val partnerId: String,
    val partnerName: String,
    val type: PartnerTransactionType,
    val productId: String = "",
    val productName: String = "",
    val category: String = "",
    val batchNumber: String = "",
    val quantity: Int = 0,
    val grossAmount: Double = 0.0,
    val commissionPercent: Double = 0.0,
    val commissionProfitAmount: Double = 0.0,
    val partnerNetAmount: Double = 0.0,
    val orderId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val paymentMethod: String = "",
    val paymentAccount: String = "",
    val paymentReference: String = "",
    val receiptImageUrl: String = "",
    val alertSentToAdmin: Boolean = false,
    val alertSentToPartner: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = ""
)
