package com.example.data.model

import androidx.compose.runtime.Immutable
import java.util.UUID

enum class ProductCategory(val displayName: String) {
    CLOTHES("Clothes"),
    SHOES("Shoes"),
    BAGS("Bags"),
    BEAUTY_PRODUCT("Beauty Product"),
    ELECTRONICS("Electronics"),
    FOOD("Food"),
    BEVERAGES("Beverages")
}

@Immutable
data class ProductColor(
    val name: String,
    val hex: String
)

@Immutable
data class Product(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val category: String, // One of ProductCategory displayName
    val brand: String,
    val availableBrands: List<String> = listOf(brand),
    val availableSizes: List<String>,
    val availableColors: List<ProductColor>,
    val basePrice: Double,
    val costPrice: Double = (basePrice * 0.70), // Purchase/Wholesale Cost Price
    val discountPercent: Int = 0,
    val stockQuantity: Int,
    val imageUrl: String,
    val galleryUrls: List<String> = emptyList(),
    val model3dType: String = "CUBE", // SNEAKER, PHONE, BAG, BOTTLE, BOX, COSMETIC
    val description: String,
    val rating: Double = 4.8,
    val reviewCount: Int = 120,
    val sku: String = "SKU-${UUID.randomUUID().toString().take(6).uppercase()}",
    val expiryMonthsRemaining: Int = 6, // < 5 months triggers short-dated red alert, <= 0 triggers expired alert
    val createdAt: Long = 0L,
    val partnerId: String = "",
    val partnerName: String = "",
    val partnerCommissionPercent: Double = 0.0,
    val partnerBatchId: String = "",
    val isCatalogLocked: Boolean = false
) {
    val allImageUrls: List<String>
        get() = buildList {
            if (imageUrl.isNotBlank()) add(imageUrl.trim())
            galleryUrls.forEach { url ->
                val clean = url.trim()
                if (clean.isNotBlank() && !contains(clean)) add(clean)
            }
        }
    fun calculatePrice(
        selectedBrand: String = brand,
        selectedSize: String = availableSizes.firstOrNull() ?: "",
        promoDiscountPercent: Int = 0
    ): Double {
        var multiplier = 1.0
        // Brand multiplier
        if (selectedBrand.contains("Pro", ignoreCase = true) || selectedBrand.contains("Luxury", ignoreCase = true) || selectedBrand.contains("Apple", ignoreCase = true) || selectedBrand.contains("Gucci", ignoreCase = true)) {
            multiplier += 0.25
        } else if (selectedBrand.contains("Plus", ignoreCase = true) || selectedBrand.contains("Nike", ignoreCase = true)) {
            multiplier += 0.15
        }

        // Size multiplier (e.g. larger storage, bigger shoe, larger quantity, or shoe size number scaling)
        val cleanSizeDigits = selectedSize.filter { it.isDigit() }
        val sizeNum = cleanSizeDigits.toIntOrNull()
        val firstSizeNum = availableSizes.firstOrNull()?.filter { it.isDigit() }?.toIntOrNull()
        if (sizeNum != null && firstSizeNum != null) {
            if (sizeNum > firstSizeNum) {
                val diff = sizeNum - firstSizeNum
                multiplier += (diff * 0.015).coerceIn(0.0, 0.35)
            }
        } else if (sizeNum != null) {
            val baseSizeRef = 35.0
            val diff = sizeNum - baseSizeRef
            multiplier += (diff * 0.015).coerceIn(-0.15, 0.35)
        } else if (selectedSize.contains("XL") || selectedSize.contains("256") || selectedSize.contains("1kg")) {
            multiplier += 0.15
        } else if (selectedSize.contains("XXL") || selectedSize.contains("512") || selectedSize.contains("2kg")) {
            multiplier += 0.30
        }

        val calculated = basePrice * multiplier
        val effectiveDiscount = if (promoDiscountPercent > 0) promoDiscountPercent else discountPercent
        return if (effectiveDiscount > 0) {
            calculated * (1.0 - (effectiveDiscount / 100.0))
        } else {
            calculated
        }
    }
}

data class Customer(
    val id: String = UUID.randomUUID().toString(),
    val fullName: String,
    val email: String,
    val phoneNumber: String,
    val deliveryAddress: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isAdmin: Boolean = false,
    val wishlistProductIds: List<String> = emptyList(),
    val signUpPassword: String = ""
)

data class AdminBargainConfig(
    val isActive: Boolean = true,
    val maxBargainDiscountPercent: Double = 10.0, // Admin set max discount percentage (e.g., 0.1%, 0.2%, 0.5%, 1%, 10%, 15%, 20%)
    val defaultBargainAmount: Double = 5000.0, // Default editable bargain discount amount in Naira (e.g. ₦5,000, ₦10,000, etc.)
    val designatedCategory: String = "All", // Designated offer category ("All" or specific: "Clothes", "Shoes", "Bags", "Beauty Product", "Electronics", "Food", "Beverages")
    val categoryBargainAmounts: Map<String, Double> = emptyMap(), // Specific custom default amounts designated per category
    val minCheckoutThreshold: Double = 0.0 // Minimum checkout total required to activate bargaining (Default ₦0 for instant bargaining)
)

@Immutable
data class CartItem(
    val cartItemId: String = UUID.randomUUID().toString(),
    val product: Product,
    val selectedBrand: String,
    val selectedSize: String,
    val selectedColor: ProductColor,
    val unitPrice: Double,
    val originalPrice: Double = unitPrice,
    val isBargained: Boolean = false,
    val bargainNote: String = "",
    val quantity: Int
) {
    val totalCost: Double get() = product.costPrice * quantity
    val totalSelling: Double get() = unitPrice * quantity
    val itemProfit: Double get() = (totalSelling - totalCost).coerceAtLeast(totalSelling * 0.10)
    val tenPercentOfProfit: Double get() = itemProfit * 0.10
}

enum class PaymentMethod(val title: String, val subtitle: String) {
    OPAY("OPay Transfer", "Direct transfer to official OPay Merchant Account"),
    MONIEPOINT("Moniepoint MFB", "Fast transfer to Moniepoint Commercial Account"),
    POS_TRANSFER("POS Terminal Transfer", "Offline POS card swipe or terminal slip transfer"),
    CASH_ON_DELIVERY("Payment on Delivery", "Pay cash or card when parcel arrives at your doorstep")
}

enum class OrderStatus(val label: String) {
    PENDING_VERIFICATION("Awaiting Admin Confirmation"),
    CONFIRMED("ORDER CONFIRMED & PROCESSING"),
    VERIFIED("ORDER CONFIRMED & PROCESSING"),
    OUT_FOR_DELIVERY("Out for Delivery"),
    DELIVERED("Delivered Successfully"),
    CANCELLED("Cancelled")
}

@Immutable
data class StatusUpdate(
    val status: OrderStatus,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = ""
)

@Immutable
data class Order(
    val orderId: String = "ORD-${System.currentTimeMillis().toString().takeLast(7)}",
    val customerId: String,
    val customerName: String,
    val customerEmail: String,
    val customerPhone: String,
    val deliveryAddress: String,
    val itemsSummary: String,
    val totalAmount: Double,
    val profit: Double = totalAmount * 0.15, // Estimated 15% profit margin
    val paymentMethod: PaymentMethod,
    val paymentAccount: String,
    val paymentReference: String,
    val senderName: String,
    val status: OrderStatus = OrderStatus.PENDING_VERIFICATION,
    val createdAt: Long = System.currentTimeMillis(),
    val verificationNote: String = "Awaiting admin confirmation",
    val receiptImageUrl: String = "",
    val statusUpdates: List<StatusUpdate> = emptyList(),
    val dispatchRiderWhatsapp: String = "",
    val partnerId: String = "",
    val partnerName: String = "",
    val partnerCommissionAmount: Double = 0.0,
    val partnerNetPayout: Double = 0.0
)

data class SupabaseConfig(
    val url: String = "",
    val anonKey: String = "",
    val isConnected: Boolean = false
)

data class StoreProfileConfig(
    val storeName: String = "24/7 SHOPPING",
    val tagline: String = "Shopping with ease and comfort",
    val phoneNumber: String = "",
    val secondaryPhoneNumber: String = "",
    val whatsappNumber: String = "",
    val supportEmail: String = "",
    val storeAddress: String = "",
    val opayAccountNumber: String = "",
    val opayAccountName: String = "24/7 SHOPPING",
    val moniepointAccountNumber: String = "",
    val moniepointAccountName: String = "24/7 SHOPPING",
    val posTerminalId: String = "",
    val posTerminalName: String = "",
    val extraBankName: String = "",
    val extraBankAccountNumber: String = "",
    val extraBankAccountName: String = "",
    val apkDownloadUrl: String = "",
    val latestAppVersionCode: Int = 1,
    val latestAppVersionName: String = "1.0",
    val updateReleaseNotes: String = ""
)

data class AppUpdateManifest(
    val versionCode: Int,
    val versionName: String,
    val releaseNotes: String,
    val downloadUrl: String
)

data class CustomerDeliveryInfo(
    val fullName: String = "",
    val phoneNumber: String = "",
    val deliveryAddress: String = ""
)

data class DeliveryOption(
    val id: String,
    val title: String,
    val price: Double
)

enum class PreOrderStatus(val label: String) {
    PENDING("Awaiting Admin Review"),
    PROCESSING("Admin Processing & Sourcing"),
    CONFIRMED_AVAILABLE("Item Available & Confirmed"),
    COMPLETED("Order Completed / Fulfilled"),
    DECLINED("Unable to Source / Declined")
}

data class PreOrderRequest(
    val id: String = "REQ-${UUID.randomUUID().toString().take(6).uppercase()}",
    val customerId: String,
    val customerName: String,
    val customerPhone: String,
    val productName: String,
    val brandName: String,
    val category: String = "24/7 Shopping Item",
    val preferredSize: String = "Standard",
    val preferredColor: String = "Any",
    val targetBudget: Double = 0.0,
    val description: String = "",
    val photoUrl: String = "",
    val status: PreOrderStatus = PreOrderStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val adminResponse: String = "",
    val adminQuotedPrice: Double = 0.0,
    val customerReply: String = ""
)

enum class CatalogSortOption(val label: String) {
    FEATURED("Featured"),
    NEWEST("Newest"),
    PRICE_LOW_HIGH("Price: Low → High"),
    PRICE_HIGH_LOW("Price: High → Low"),
    TOP_RATED("Top Rated"),
    ON_PROMO("On Promo")
}

enum class PriceRangeFilter(val label: String, val minPrice: Double?, val maxPrice: Double?) {
    ALL("All Prices", null, null),
    UNDER_25K("Under ₦25k", null, 25000.0),
    RANGE_25K_100K("₦25k – ₦100k", 25000.0, 100000.0),
    OVER_100K("Over ₦100k", 100000.0, null)
}

data class PromoCoupon(
    val code: String,
    val discountPercent: Int,
    val minOrderAmount: Double = 0.0,
    val isActive: Boolean = true,
    val description: String = ""
)

data class ProductReview(
    val id: String = UUID.randomUUID().toString(),
    val productId: String,
    val customerId: String,
    val customerName: String,
    val rating: Int,
    val comment: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isVerifiedPurchase: Boolean = true
)

enum class CatalogBrowseMode(val label: String) {
    CAROUSEL_AND_GRID("Row + Grid"),
    CATEGORY_SHELVES("Shelves ↕↔"),
    WIDE_MATRIX_2D("2D Matrix ↕↔")
}


