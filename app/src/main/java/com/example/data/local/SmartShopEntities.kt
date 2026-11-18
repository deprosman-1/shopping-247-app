package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val brand: String,
    val availableBrands: String, // Comma-separated
    val availableSizes: String,  // Comma-separated
    val availableColorsJson: String, // name|hex;name|hex
    val basePrice: Double,
    val discountPercent: Int,
    val stockQuantity: Int,
    val imageUrl: String,
    val model3dType: String,
    val description: String,
    val rating: Double,
    val reviewCount: Int,
    val sku: String,
    val costPrice: Double = -1.0,
    val expiryMonthsRemaining: Int = 6,
    val createdAt: Long = 0L,
    val partnerId: String = "",
    val partnerName: String = "",
    val partnerCommissionPercent: Double = 0.0,
    val partnerBatchId: String = "",
    val galleryUrlsJson: String = ""
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: String,
    val fullName: String,
    val email: String,
    val passwordHash: String,
    val phoneNumber: String,
    val deliveryAddress: String,
    val createdAt: Long,
    val isAdmin: Boolean,
    val wishlistProductIds: String = ""
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val orderId: String,
    val customerId: String,
    val customerName: String,
    val customerEmail: String,
    val customerPhone: String,
    val deliveryAddress: String,
    val itemsSummary: String,
    val totalAmount: Double,
    val profit: Double,
    val paymentMethod: String,
    val paymentAccount: String,
    val paymentReference: String,
    val senderName: String,
    val status: String,
    val createdAt: Long,
    val verificationNote: String,
    val receiptImageUrl: String = "",
    val statusUpdatesJson: String = "", // status|timestamp|note;status|timestamp|note
    val dispatchRiderWhatsapp: String = "",
    val partnerId: String = "",
    val partnerName: String = "",
    val partnerCommissionAmount: Double = 0.0,
    val partnerNetPayout: Double = 0.0
)
