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
    BEVERAGES("Beverages"),
    MEN("Men"),
    WOMEN("Women"),
    BABIES("Babies"),
    FRUITS_VEGETABLES("Fruits/Vegetables"),
    HOUSEHOLD("Household"),
    MUSICALS("Musicals"),
    EDUCATIONAL("Educational"),
    JEWELRY("Jewelry"),
    STATIONARIES("Stationaries"),
    PHONE_ACCESSORIES("Phone Accessories"),
    UPHOLSTERY("Upholstery"),
    RUBBER_PLASTIC("Rubber/Plastic"),
    AUTOMOBILE_SPARE_PARTS("Automobile Spare-Parts"),
    CLOTHING_SEWING_MATERIALS("Clothing & Sewing Materials"),
    HEALTH_MEDICAL_EQUIPMENT("Health/Medical Equipment"),
    PROCESSED_FOODS_PASTRIES("Processed Foods/Pastries"),
    FROZEN_FOODS_MEAT("Frozen Foods/Meat")
}

@Immutable
data class ProductColor(
    val name: String,
    val hex: String
)

@Immutable
data class ProductVariantPrice(
    val brand: String,
    val size: String,
    val colorName: String,
    val amount: Double,
    val colorHex: String = "#111827"
) {
    companion object {
        fun encodeListToJson(list: List<ProductVariantPrice>): String {
            if (list.isEmpty()) return ""
            val arr = org.json.JSONArray()
            list.forEach { item ->
                arr.put(
                    org.json.JSONObject().apply {
                        put("b", item.brand.trim())
                        put("s", item.size.trim())
                        put("c", item.colorName.trim())
                        put("a", item.amount)
                        put("h", item.colorHex.trim().ifBlank { "#111827" })
                    }
                )
            }
            return arr.toString()
        }

        fun decodeListFromJson(raw: String?): List<ProductVariantPrice> {
            val clean = raw?.trim().orEmpty()
            if (clean.isBlank() || clean == "null" || clean == "[]") return emptyList()
            return try {
                val arr = org.json.JSONArray(clean)
                buildList {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val brand = obj.optString("b", obj.optString("brand", "")).trim()
                        val size = obj.optString("s", obj.optString("size", "")).trim()
                        val color = obj.optString("c", obj.optString("colorName", "")).trim()
                        val amt = if (obj.has("a")) obj.optDouble("a", 0.0) else obj.optDouble("amount", 0.0)
                        val hex = obj.optString("h", obj.optString("colorHex", "#111827")).trim().ifBlank { "#111827" }
                        if (!amt.isNaN() && amt > 0.0 && (brand.isNotBlank() || size.isNotBlank() || color.isNotBlank())) {
                            add(
                                ProductVariantPrice(
                                    brand = brand.ifBlank { "Standard" },
                                    size = size.ifBlank { "Standard" },
                                    colorName = color.ifBlank { "Default" },
                                    amount = amt,
                                    colorHex = hex
                                )
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}

private fun variantTokenMatches(candidate: String, selected: String, isSizeField: Boolean = false): Boolean {
    val a = candidate.trim()
    val b = selected.trim()
    if (a.equals(b, ignoreCase = true)) return true
    if (a.equals("Any", ignoreCase = true) || a == "*" || b.equals("Any", ignoreCase = true)) return true
    if (isSizeField) {
        // Normalize shoe/size strings like "42", "EU 42", "Size 42 (EU) (Size 42 (US 12.75)...)"
        fun extractShoeOrPlainNumber(raw: String): String? {
            val lower = raw.lowercase().trim()
            if (lower.all { it.isDigit() }) return lower
            val m = Regex("""^(?:size|eu|us|uk|no\.?|#)\s*(\d+(?:\.\d+)?)\b""").find(lower)
            return m?.groupValues?.getOrNull(1)
        }
        val nA = extractShoeOrPlainNumber(a)
        val nB = extractShoeOrPlainNumber(b)
        if (nA != null && nB != null && nA == nB) return true
    }
    return false
}

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
    val isCatalogLocked: Boolean = false,
    val variantPrices: List<ProductVariantPrice> = emptyList()
) {
    val allImageUrls: List<String>
        get() = buildList {
            if (imageUrl.isNotBlank()) add(imageUrl.trim())
            galleryUrls.forEach { url ->
                val clean = url.trim()
                if (clean.isNotBlank() && !contains(clean)) add(clean)
            }
        }

    fun resolveVariantPriceEntry(
        selectedBrand: String = brand,
        selectedSize: String = availableSizes.firstOrNull() ?: "",
        selectedColor: String = availableColors.firstOrNull()?.name ?: ""
    ): ProductVariantPrice? {
        if (variantPrices.isEmpty()) return null
        val cleanBrand = selectedBrand.trim().ifBlank { brand }
        val cleanSize = selectedSize.trim().ifBlank { availableSizes.firstOrNull() ?: "Standard" }
        val cleanColor = selectedColor.trim().ifBlank { availableColors.firstOrNull()?.name ?: "Default" }

        // 1. Exact Brand + Size + Color
        variantPrices.firstOrNull {
            variantTokenMatches(it.brand, cleanBrand) &&
                variantTokenMatches(it.size, cleanSize, isSizeField = true) &&
                variantTokenMatches(it.colorName, cleanColor)
        }?.let { return it }

        // 2. Brand + Size (any color fallback if exact color isn't listed for that size)
        variantPrices.firstOrNull {
            variantTokenMatches(it.brand, cleanBrand) &&
                variantTokenMatches(it.size, cleanSize, isSizeField = true)
        }?.let { return it }

        // 3. Brand + Color (for products differing primarily by Brand & Color)
        variantPrices.firstOrNull {
            variantTokenMatches(it.brand, cleanBrand) &&
                variantTokenMatches(it.colorName, cleanColor)
        }?.let { return it }

        // 4. Size + Color
        variantPrices.firstOrNull {
            variantTokenMatches(it.size, cleanSize, isSizeField = true) &&
                variantTokenMatches(it.colorName, cleanColor)
        }?.let { return it }

        // 5. Same Brand
        return variantPrices.firstOrNull { variantTokenMatches(it.brand, cleanBrand) }
    }

    fun minVariantAmount(): Double? = variantPrices.minOfOrNull { it.amount }
    fun maxVariantAmount(): Double? = variantPrices.maxOfOrNull { it.amount }

    fun calculatePrice(
        selectedBrand: String = brand,
        selectedSize: String = availableSizes.firstOrNull() ?: "",
        promoDiscountPercent: Int = 0,
        selectedColor: String = availableColors.firstOrNull()?.name ?: ""
    ): Double {
        val effectiveDiscount = if (promoDiscountPercent > 0) promoDiscountPercent else discountPercent
        val matchedVariant = resolveVariantPriceEntry(selectedBrand, selectedSize, selectedColor)
        if (matchedVariant != null && matchedVariant.amount > 0.0) {
            return if (effectiveDiscount > 0) {
                matchedVariant.amount * (1.0 - (effectiveDiscount / 100.0))
            } else {
                matchedVariant.amount
            }
        }

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
    PAYSTACK("Paystack Checkout", "Instant Card, Bank Transfer, USSD & OPay via Paystack"),
    OPAY("OPay Transfer", "Direct transfer to official OPay Merchant Account"),
    MONIEPOINT("Moniepoint MFB", "Fast transfer to Moniepoint Commercial Account"),
    PALMPAY("PalmPay Wallet", "Direct instant transfer to official ODIYA PalmPay Account"),
    POS_TRANSFER("POS Terminal Transfer", "Offline POS card swipe or terminal slip transfer"),
    CASH_ON_DELIVERY("Payment on Delivery", "Pay cash or card when parcel arrives at your doorstep")
}

enum class CreditVerificationStatus(val label: String) {
    PENDING_CREDIT("Awaiting ODIYA Account Credit Alert"),
    PAYMENT_FAILED_UNCREDITED("ORDER & PAYMENT FAILED — Not Yet Credited"),
    CONFIRMED_CREDITED("ORDER & PAYMENT CONFIRMED — Account Credited"),
    NETWORK_DELAYED_PENDING("Network Delayed — Queued for Auto-Confirmation")
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
    val partnerNetPayout: Double = 0.0,
    val isCreditAlertConfirmed: Boolean = false,
    val creditVerificationStatus: CreditVerificationStatus = CreditVerificationStatus.PENDING_CREDIT
)

data class SupabaseConfig(
    val url: String = "",
    val anonKey: String = "",
    val isConnected: Boolean = false
)

data class StoreProfileConfig(
    val storeName: String = "ODIYA",
    val tagline: String = "Shopping with ease and comfort",
    val phoneNumber: String = "",
    val secondaryPhoneNumber: String = "",
    val whatsappNumber: String = "",
    val supportEmail: String = "",
    val storeAddress: String = "",
    val opayAccountNumber: String = "",
    val opayAccountName: String = "ODIYA",
    val moniepointAccountNumber: String = "",
    val moniepointAccountName: String = "ODIYA",
    val palmpayAccountNumber: String = "",
    val palmpayAccountName: String = "ODIYA",
    val posTerminalId: String = "",
    val posTerminalName: String = "",
    val extraBankName: String = "",
    val extraBankAccountNumber: String = "",
    val extraBankAccountName: String = "",
    val apkDownloadUrl: String = "",
    val latestAppVersionCode: Int = 1,
    val latestAppVersionName: String = "1.0",
    val updateReleaseNotes: String = "",
    val isMaintenanceMode: Boolean = false,
    val maintenanceMessage: String = "",
    val maintenanceStartedAt: Long = 0L,
    val paystackPublicKey: String = "",
    val paystackCallbackUrl: String = "depros://paystack-callback",
    val paystackEdgeFunctionBaseUrl: String = "",
    val updatedAt: Long = 0L
)

data class ForwardedReceiptLog(
    val id: String = java.util.UUID.randomUUID().toString().take(8).uppercase(),
    val orderId: String,
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val portalTitle: String = "",
    val format: String = "PDF",
    val amount: Double = 0.0,
    val accountLabel: String = "",
    val paymentReference: String = "",
    val cloudDocumentUrl: String = "",
    val forwardedAt: Long = System.currentTimeMillis()
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
    val category: String = "Odiya Item",
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
    val isVerifiedPurchase: Boolean = true,
    val productTitle: String = "",
    val adminReply: String = "",
    val adminRepliedAt: Long = 0L
)

enum class CatalogBrowseMode(val label: String) {
    CAROUSEL_AND_GRID("Row + Grid"),
    CATEGORY_SHELVES("Shelves ↕↔"),
    WIDE_MATRIX_2D("2D Matrix ↕↔")
}


