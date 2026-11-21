package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.data.local.CustomerEntity
import com.example.data.local.DeprosDao
import com.example.data.local.DeprosDatabase
import com.example.data.local.OrderEntity
import com.example.data.local.ProductEntity
import com.example.data.model.Customer
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.ProductColor
import com.example.data.model.StatusUpdate
import com.example.data.model.SupabaseConfig
import com.example.data.remote.SupabaseSyncService
import com.example.util.ProductImageHelper
import com.example.util.SecurityUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

data class CloudSyncSummary(
    val syncedProducts: Int = 0,
    val customCloudProducts: Int = 0,
    val syncedCustomers: Int = 0,
    val syncedOrders: Int = 0,
    val newRemoteCustomers: Int = 0,
    val newRemoteOrders: Int = 0,
    val pushedLocalProducts: Int = 0,
    val pushedLocalCustomers: Int = 0,
    val pushedLocalOrders: Int = 0,
    val latestNewCustomerName: String? = null,
    val latestNewCustomerCreatedAt: Long = 0L,
    val latestNewOrder: Order? = null,
    val errors: List<String> = emptyList()
)

private fun normalizePhoneKey(phone: String): String {
    val digits = phone.filter { it.isDigit() }
    return if (digits.length >= 10) digits.takeLast(10) else digits
}

class EcommerceRepository(
    context: Context,
    private val scope: CoroutineScope
) {
    private val appContext = context.applicationContext
    private val db = DeprosDatabase.getDatabase(appContext)
    private val dao: DeprosDao = db.dao()
    val supabaseSync = SupabaseSyncService()
    val firebaseRepo = com.example.data.remote.FirebaseRepository(appContext)
    @Volatile
    var lastCloudSyncError: String? = null
        private set

    @Volatile
    var lastStorageUploadError: String? = null
        private set

    init {
        scope.launch(Dispatchers.IO) {
            seedDefaultProductsIfEmpty()
        }
    }

    // Products Flow
    val allProducts: Flow<List<Product>> = dao.getAllProducts().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun getProductById(id: String): Product? = withContext(Dispatchers.IO) {
        dao.getProductById(id)?.toDomain()
    }

    private fun persistUriToInternalFileAndCompress(
        subFolder: String,
        filePrefix: String,
        uriString: String
    ): Pair<String, ByteArray?> {
        return try {
            val uri = Uri.parse(uriString)
            val rawBytes = if (uriString.startsWith("file://", ignoreCase = true)) {
                val f = uri.path?.let { File(it) }
                if (f != null && f.exists() && f.length() > 0L) f.readBytes() else null
            } else {
                appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } ?: return uriString to null

            val compressedBytes = try {
                val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
                if (decoded != null) {
                    val maxDim = 1280
                    val scaled = if (decoded.width > maxDim || decoded.height > maxDim) {
                        val ratio = maxDim.toFloat() / maxOf(decoded.width, decoded.height).toFloat()
                        Bitmap.createScaledBitmap(
                            decoded,
                            (decoded.width * ratio).toInt().coerceAtLeast(1),
                            (decoded.height * ratio).toInt().coerceAtLeast(1),
                            true
                        )
                    } else decoded
                    val out = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, 84, out)
                    if (scaled !== decoded) decoded.recycle()
                    scaled.recycle()
                    out.toByteArray()
                } else {
                    rawBytes
                }
            } catch (_: Exception) {
                rawBytes
            }

            // Copy to persistent internal storage so gallery URI never expires across restarts
            val dir = File(appContext.filesDir, subFolder).apply { mkdirs() }
            val safePrefix = filePrefix.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val destFile = if (uriString.startsWith("file://", ignoreCase = true) &&
                uriString.contains("/$subFolder/") &&
                File(uri.path ?: "").exists()
            ) {
                File(uri.path!!)
            } else {
                File(dir, "${safePrefix}_${System.currentTimeMillis()}.jpg").also {
                    it.writeBytes(compressedBytes)
                }
            }

            Uri.fromFile(destFile).toString() to compressedBytes
        } catch (e: Exception) {
            Log.w("EcommerceRepository", "Could not persist/read URI ($subFolder): ${e.message}")
            uriString to null
        }
    }

    private fun compressToCompactBase64(rawBytes: ByteArray): String? {
        return try {
            val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size) ?: return null
            val maxDim = 640
            val scaled = if (decoded.width > maxDim || decoded.height > maxDim) {
                val ratio = maxDim.toFloat() / maxOf(decoded.width, decoded.height).toFloat()
                Bitmap.createScaledBitmap(
                    decoded,
                    (decoded.width * ratio).toInt().coerceAtLeast(1),
                    (decoded.height * ratio).toInt().coerceAtLeast(1),
                    true
                )
            } else decoded

            var quality = 65
            var resultBytes: ByteArray
            do {
                val out = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                resultBytes = out.toByteArray()
                quality -= 15
            } while (resultBytes.size > 85_000 && quality >= 20)

            if (scaled !== decoded) decoded.recycle()
            scaled.recycle()

            val b64 = android.util.Base64.encodeToString(resultBytes, android.util.Base64.NO_WRAP)
            "data:image/jpeg;base64,$b64"
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun uploadImageUriToBucketIfNeeded(
        bucketId: String,
        filePrefix: String,
        uriString: String
    ): String {
        val trimmed = uriString.trim()
        if (trimmed.isBlank() ||
            trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("data:image/", ignoreCase = true) ||
            trimmed.startsWith("android.resource://", ignoreCase = true)
        ) {
            lastStorageUploadError = null
            return trimmed
        }
        if (!trimmed.startsWith("content://", ignoreCase = true) && !trimmed.startsWith("file://", ignoreCase = true)) {
            return trimmed
        }

        val subFolder = if (bucketId == "product-media") "product_images" else "receipt_images"
        val (persistentLocalUri, uploadBytes) = persistUriToInternalFileAndCompress(
            subFolder = subFolder,
            filePrefix = filePrefix,
            uriString = trimmed
        )

        if (uploadBytes == null) {
            lastStorageUploadError = "Selected photo file could not be read from device storage. Please pick the photo again."
            return persistentLocalUri
        }
        if (supabaseSync.isPlaceholderConfig()) {
            lastStorageUploadError = null
            return persistentLocalUri
        }

        val safeFileName = "${filePrefix.replace(Regex("[^A-Za-z0-9._-]"), "_")}_${System.currentTimeMillis()}.jpg"
        val uploadRes = if (bucketId == "product-media") {
            supabaseSync.uploadProductMediaWithFallback(
                fileName = safeFileName,
                fileBytes = uploadBytes
            )
        } else {
            val primary = supabaseSync.uploadReceiptToSupabaseStorage(
                fileName = safeFileName,
                fileBytes = uploadBytes,
                bucketId = bucketId
            )
            if (primary.isSuccess) {
                primary
            } else {
                supabaseSync.uploadReceiptToSupabaseStorage(
                    fileName = "receipts/$safeFileName",
                    fileBytes = uploadBytes,
                    bucketId = "product-media"
                )
            }
        }

        return uploadRes.fold(
            onSuccess = { publicUrl ->
                lastStorageUploadError = null
                publicUrl
            },
            onFailure = { err ->
                val msg = err.message ?: "Storage upload failed"
                lastStorageUploadError = msg
                Log.w("EcommerceRepository", "Storage upload ($bucketId) failed: $msg")
                if (bucketId == "shop-receipts") {
                    compressToCompactBase64(uploadBytes) ?: persistentLocalUri
                } else {
                    persistentLocalUri
                }
            }
        )
    }

    suspend fun saveProduct(product: Product): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            lastStorageUploadError = null
            val stampedProduct = if (product.createdAt > 0L) product else product.copy(createdAt = System.currentTimeMillis())

            // 1. Persist local gallery photo(s) to app filesDir & upload to Supabase Storage (product-media)
            val resolvedImageUrl = uploadImageUriToBucketIfNeeded(
                bucketId = "product-media",
                filePrefix = "prod_${stampedProduct.id.take(12)}",
                uriString = stampedProduct.imageUrl
            )
            val resolvedGallery = stampedProduct.galleryUrls.mapIndexed { idx, extraUrl ->
                val clean = extraUrl.trim()
                if (clean.isBlank() || clean == stampedProduct.imageUrl) {
                    resolvedImageUrl
                } else {
                    uploadImageUriToBucketIfNeeded(
                        bucketId = "product-media",
                        filePrefix = "prod_${stampedProduct.id.take(10)}_g$idx",
                        uriString = clean
                    )
                }
            }.filter { it.isNotBlank() }.distinct()
            val finalProduct = stampedProduct.copy(
                imageUrl = resolvedImageUrl,
                galleryUrls = resolvedGallery
            )

            // 2. Save in local Room DB with persistent file:// or permanent https:// URL
            dao.insertProduct(finalProduct.toEntity())

            // 3. Sync product to Supabase Cloud & report actual status
            if (!supabaseSync.isPlaceholderConfig()) {
                val cloudRes = supabaseSync.syncProductToCloud(finalProduct)
                lastCloudSyncError = if (cloudRes.isFailure) cloudRes.exceptionOrNull()?.message else null
                Result.success(cloudRes.isSuccess)
            } else {
                lastCloudSyncError = "Supabase Cloud not linked on this device yet."
                Result.success(false)
            }
        } catch (e: Exception) {
            lastCloudSyncError = e.message
            Result.failure(e)
        }
    }

    suspend fun persistLocalPartnerStagingImage(
        partnerId: String,
        uriString: String
    ): String = withContext(Dispatchers.IO) {
        val trimmed = uriString.trim()
        if (trimmed.isBlank()) return@withContext ""
        val (localUri, _) = persistUriToInternalFileAndCompress(
            subFolder = "partner_staging",
            filePrefix = "partner_${partnerId.take(10)}",
            uriString = trimmed
        )
        localUri
    }

    suspend fun uploadPartnerBatchToCloud(
        batchProducts: List<Product>
    ): Result<Pair<Int, List<Product>>> = withContext(Dispatchers.IO) {
        if (batchProducts.isEmpty()) {
            return@withContext Result.failure(Exception("No verified partner products in this batch."))
        }
        try {
            lastStorageUploadError = null
            lastCloudSyncError = null
            dao.insertProducts(batchProducts.map { it.toEntity() })

            if (supabaseSync.isPlaceholderConfig()) {
                lastCloudSyncError = "Supabase Cloud not linked on this device yet."
                return@withContext Result.success(0 to batchProducts)
            }

            val prepared = prepareProductsForCloudUpload(batchProducts)
            val syncRes = supabaseSync.syncProductsBatchToCloud(prepared)
            if (syncRes.isSuccess) {
                Result.success(syncRes.getOrDefault(prepared.size) to prepared)
            } else {
                val err = syncRes.exceptionOrNull()?.message ?: "Batch sync failed"
                lastCloudSyncError = err
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            lastCloudSyncError = e.message
            Result.failure(e)
        }
    }

    suspend fun prepareProductsForCloudUpload(products: List<Product>): List<Product> = withContext(Dispatchers.IO) {
        if (supabaseSync.isPlaceholderConfig()) return@withContext products
        var latestUploadErr: String? = null
        val mapped = products.map { prod ->
            val resolvedMain = if (prod.imageUrl.startsWith("content://", ignoreCase = true) || prod.imageUrl.startsWith("file://", ignoreCase = true)) {
                uploadImageUriToBucketIfNeeded(
                    bucketId = "product-media",
                    filePrefix = "prod_${prod.id.take(12)}",
                    uriString = prod.imageUrl
                ).also {
                    if (lastStorageUploadError != null) latestUploadErr = lastStorageUploadError
                }
            } else prod.imageUrl

            val resolvedGallery = prod.galleryUrls.mapIndexed { idx, g ->
                if (g.startsWith("content://", ignoreCase = true) || g.startsWith("file://", ignoreCase = true)) {
                    uploadImageUriToBucketIfNeeded(
                        bucketId = "product-media",
                        filePrefix = "prod_${prod.id.take(10)}_g$idx",
                        uriString = g
                    ).also {
                        if (lastStorageUploadError != null) latestUploadErr = lastStorageUploadError
                    }
                } else g
            }.filter { it.isNotBlank() }.distinct()

            if (resolvedMain != prod.imageUrl || resolvedGallery != prod.galleryUrls) {
                prod.copy(imageUrl = resolvedMain, galleryUrls = resolvedGallery).also { dao.insertProduct(it.toEntity()) }
            } else prod
        }
        lastStorageUploadError = latestUploadErr
        mapped
    }

    suspend fun deleteProduct(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.deleteProduct(id)
            supabaseSync.deleteProductFromCloud(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setPartnerProductsCatalogLocked(partnerId: String, locked: Boolean): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val entities = dao.getProductsByPartnerId(partnerId)
            if (entities.isEmpty()) return@withContext Result.success(0)
            val updated = entities.map { it.toDomain().copy(isCatalogLocked = locked) }
            dao.insertProducts(updated.map { it.toEntity() })
            if (!supabaseSync.isPlaceholderConfig()) {
                updated.forEach { prod ->
                    try { supabaseSync.syncProductToCloud(prod) } catch (_: Exception) {}
                }
            }
            Result.success(updated.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePartnerProductsAndImages(
        partnerId: String,
        extraImageUrls: List<String> = emptyList()
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val partnerEntities = dao.getProductsByPartnerId(partnerId)
            val imageUrls = buildSet {
                partnerEntities.forEach { entity ->
                    val prod = entity.toDomain()
                    if (prod.imageUrl.isNotBlank()) add(prod.imageUrl)
                    addAll(prod.galleryUrls)
                }
                extraImageUrls.filter { it.isNotBlank() }.forEach { add(it) }
            }

            // 1. Delete local cached / staged image files
            imageUrls.forEach { url ->
                try {
                    val filePath = when {
                        url.startsWith("file://") -> url.removePrefix("file://")
                        url.startsWith("/") -> url
                        else -> null
                    }
                    if (!filePath.isNullOrBlank()) {
                        val file = File(filePath)
                        if (file.exists() && (file.absolutePath.contains("product_images") || file.absolutePath.contains("partner_staging"))) {
                            file.delete()
                        }
                    }
                } catch (_: Exception) {}
            }

            // 2. Delete product rows from Room & Supabase DB, plus Supabase Storage objects
            dao.deleteProductsByPartner(partnerId)
            if (!supabaseSync.isPlaceholderConfig()) {
                try { supabaseSync.deletePartnerProductsFromCloud(partnerId) } catch (_: Exception) {}
                partnerEntities.forEach { ent ->
                    try { supabaseSync.deleteProductFromCloud(ent.id) } catch (_: Exception) {}
                }
                imageUrls.filter { it.startsWith("http", ignoreCase = true) }.forEach { remoteUrl ->
                    try { supabaseSync.deleteStorageObjectByPublicUrl(remoteUrl) } catch (_: Exception) {}
                }
            }
            Result.success(partnerEntities.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deductPartnerProductsStock(deductionsByProductId: Map<String, Int>): Result<List<Product>> = withContext(Dispatchers.IO) {
        try {
            val updated = mutableListOf<Product>()
            deductionsByProductId.forEach { (productId, qtySold) ->
                if (qtySold > 0) {
                    val existing = dao.getProductById(productId)?.toDomain()
                    if (existing != null) {
                        val nextQty = (existing.stockQuantity - qtySold).coerceAtLeast(0)
                        val saved = existing.copy(stockQuantity = nextQty)
                        dao.insertProduct(saved.toEntity())
                        updated.add(saved)
                        if (!supabaseSync.isPlaceholderConfig()) {
                            try { supabaseSync.syncProductToCloud(saved) } catch (_: Exception) {}
                        }
                    }
                }
            }
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Orders & Receipts Flow
    val allOrders: Flow<List<Order>> = dao.getAllOrders().map { entities ->
        entities.map { it.toDomain() }
    }

    fun getOrdersForCustomer(customerId: String): Flow<List<Order>> {
        return dao.getOrdersForCustomer(customerId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    private suspend fun uploadReceiptUriIfNeeded(orderId: String, uriString: String): String {
        return uploadImageUriToBucketIfNeeded(
            bucketId = "shop-receipts",
            filePrefix = "receipt_${orderId}",
            uriString = uriString
        )
    }

    suspend fun persistReturnProofImage(requestId: String, uriString: String): String = withContext(Dispatchers.IO) {
        if (uriString.isBlank()) return@withContext ""
        uploadImageUriToBucketIfNeeded(
            bucketId = "shop-receipts",
            filePrefix = "return_${requestId}",
            uriString = uriString
        )
    }

    suspend fun createOrder(order: Order): Result<Order> = withContext(Dispatchers.IO) {
        try {
            // 1. Save immediately in local Room DB so order is never lost
            dao.insertOrder(order.toEntity())

            // 2. Upload receipt photo if it is a local gallery URI
            val resolvedReceiptUrl = uploadReceiptUriIfNeeded(order.orderId, order.receiptImageUrl)
            val finalOrder = if (resolvedReceiptUrl != order.receiptImageUrl) {
                order.copy(receiptImageUrl = resolvedReceiptUrl).also {
                    dao.insertOrder(it.toEntity())
                }
            } else {
                order
            }

            // 3. Ensure customer profile exists in Room + Supabase before/alongside order sync
            if (!supabaseSync.isPlaceholderConfig()) {
                val localCust = dao.getCustomerById(finalOrder.customerId)?.toDomain()
                    ?: Customer(
                        id = finalOrder.customerId,
                        fullName = finalOrder.customerName.ifBlank { "Valued Customer" },
                        email = finalOrder.customerEmail.ifBlank {
                            "cust_${finalOrder.customerPhone.filter { it.isDigit() }.ifBlank { finalOrder.customerId.take(6) }}@247shopping.local"
                        },
                        phoneNumber = finalOrder.customerPhone,
                        deliveryAddress = finalOrder.deliveryAddress
                    )
                if (dao.getCustomerById(localCust.id) == null) {
                    dao.insertCustomer(
                        CustomerEntity(
                            id = localCust.id,
                            fullName = localCust.fullName,
                            email = localCust.email,
                            passwordHash = "",
                            phoneNumber = localCust.phoneNumber,
                            deliveryAddress = localCust.deliveryAddress,
                            createdAt = localCust.createdAt,
                            isAdmin = localCust.isAdmin,
                            wishlistProductIds = ""
                        )
                    )
                }
                val custSyncRes = supabaseSync.syncCustomerToCloud(localCust)
                if (custSyncRes.isFailure) {
                    Log.w("EcommerceRepository", "Customer pre-order sync issue: ${custSyncRes.exceptionOrNull()?.message}")
                }

                val orderSyncRes = supabaseSync.syncOrderToCloud(finalOrder)
                lastCloudSyncError = if (orderSyncRes.isFailure) {
                    orderSyncRes.exceptionOrNull()?.message
                } else {
                    null
                }
            } else {
                lastCloudSyncError = "Store Cloud Sync link is not configured on this device yet."
            }

            scope.launch {
                try {
                    firebaseRepo.syncOrder(finalOrder)
                } catch (e: Exception) {
                    Log.e("EcommerceRepository", "Firebase mirror sync skipped for order ${finalOrder.orderId}: ${e.message}")
                }
            }
            Result.success(finalOrder)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun deductStockForOrder(order: Order) {
        try {
            val allProds = dao.getAllProductsList().map { it.toDomain() }
            if (allProds.isEmpty()) return
            val linePattern = Regex("""^(\d+)\s*x\s+([^(]+?)(?:\s*\(|\s*-\s*₦|$)""", RegexOption.IGNORE_CASE)
            val quantitiesByTitle = mutableMapOf<String, Int>()
            order.itemsSummary.split(";", "\n").forEach { rawLine ->
                val line = rawLine.trim()
                if (line.isNotBlank()) {
                    val match = linePattern.find(line)
                    if (match != null) {
                        val qty = match.groupValues[1].toIntOrNull() ?: 1
                        val titleKey = match.groupValues[2].trim().lowercase()
                        quantitiesByTitle[titleKey] = (quantitiesByTitle[titleKey] ?: 0) + qty
                    }
                }
            }
            if (quantitiesByTitle.isEmpty()) return

            val updatedProducts = mutableListOf<Product>()
            allProds.forEach { prod ->
                val orderedQty = quantitiesByTitle[prod.title.trim().lowercase()] ?: 0
                if (orderedQty > 0) {
                    val newStock = (prod.stockQuantity - orderedQty).coerceAtLeast(0)
                    if (newStock != prod.stockQuantity) {
                        val updated = prod.copy(stockQuantity = newStock)
                        dao.insertProduct(updated.toEntity())
                        updatedProducts.add(updated)
                    }
                }
            }

            if (updatedProducts.isNotEmpty() && !supabaseSync.isPlaceholderConfig()) {
                updatedProducts.forEach { prod ->
                    try {
                        supabaseSync.syncProductToCloud(prod)
                    } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            Log.w("EcommerceRepository", "Stock deduction skipped for order ${order.orderId}: ${e.message}")
        }
    }

    private suspend fun restoreStockForOrder(order: Order) {
        try {
            val allProds = dao.getAllProductsList().map { it.toDomain() }
            if (allProds.isEmpty()) return
            val linePattern = Regex("""^(\d+)\s*x\s+([^(]+?)(?:\s*\(|\s*-\s*₦|$)""", RegexOption.IGNORE_CASE)
            val quantitiesByTitle = mutableMapOf<String, Int>()
            order.itemsSummary.split(";", "\n").forEach { rawLine ->
                val line = rawLine.trim()
                if (line.isNotBlank()) {
                    val match = linePattern.find(line)
                    if (match != null) {
                        val qty = match.groupValues[1].toIntOrNull() ?: 1
                        val titleKey = match.groupValues[2].trim().lowercase()
                        quantitiesByTitle[titleKey] = (quantitiesByTitle[titleKey] ?: 0) + qty
                    }
                }
            }
            if (quantitiesByTitle.isEmpty()) return

            val updatedProducts = mutableListOf<Product>()
            allProds.forEach { prod ->
                val orderedQty = quantitiesByTitle[prod.title.trim().lowercase()] ?: 0
                if (orderedQty > 0) {
                    val updated = prod.copy(stockQuantity = prod.stockQuantity + orderedQty)
                    dao.insertProduct(updated.toEntity())
                    updatedProducts.add(updated)
                }
            }

            if (updatedProducts.isNotEmpty() && !supabaseSync.isPlaceholderConfig()) {
                updatedProducts.forEach { prod ->
                    try {
                        supabaseSync.syncProductToCloud(prod)
                    } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            Log.w("EcommerceRepository", "Stock restore skipped for order ${order.orderId}: ${e.message}")
        }
    }

    suspend fun updateOrderStatus(orderId: String, status: OrderStatus, note: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val order = dao.getOrderById(orderId)?.toDomain() ?: return@withContext Result.failure(Exception("Order not found"))
            val wasPending = order.status == OrderStatus.PENDING_VERIFICATION
            val wasStockDeducted = order.status == OrderStatus.CONFIRMED ||
                order.status == OrderStatus.VERIFIED ||
                order.status == OrderStatus.OUT_FOR_DELIVERY ||
                order.status == OrderStatus.DELIVERED
            val isNowConfirmed = status == OrderStatus.CONFIRMED || status == OrderStatus.VERIFIED
            val newUpdate = StatusUpdate(status, System.currentTimeMillis(), note)
            val updatedOrder = order.copy(
                status = status,
                verificationNote = note,
                statusUpdates = order.statusUpdates + newUpdate
            )
            dao.insertOrder(updatedOrder.toEntity())

            if (wasPending && isNowConfirmed) {
                deductStockForOrder(updatedOrder)
            } else if (wasStockDeducted && status == OrderStatus.CANCELLED) {
                restoreStockForOrder(updatedOrder)
            }

            // Cloud sync
            scope.launch {
                try {
                    val res = supabaseSync.syncOrderToCloud(updatedOrder)
                    if (res.isFailure) {
                        Log.e("EcommerceRepository", "Supabase order status sync failed: ${res.exceptionOrNull()?.message}")
                    }
                    firebaseRepo.syncOrder(updatedOrder)
                } catch (e: Exception) {
                    Log.e("EcommerceRepository", "Cloud sync failed for order update", e)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateOrderDispatchRider(orderId: String, whatsapp: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val order = dao.getOrderById(orderId)?.toDomain() ?: return@withContext Result.failure(Exception("Order not found"))
            val updatedOrder = order.copy(dispatchRiderWhatsapp = whatsapp)
            dao.insertOrder(updatedOrder.toEntity())

            scope.launch {
                try {
                    supabaseSync.syncOrderToCloud(updatedOrder)
                    firebaseRepo.syncOrder(updatedOrder)
                } catch (e: Exception) {
                    Log.e("EcommerceRepository", "Cloud sync failed for dispatch rider update", e)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchAndSyncAllFromCloudDetailed(): Result<CloudSyncSummary> = withContext(Dispatchers.IO) {
        if (supabaseSync.isPlaceholderConfig()) {
            return@withContext Result.failure(Exception("Supabase not configured on this device"))
        }
        try {
            val errors = mutableListOf<String>()
            var syncedProducts = 0
            var customCloudProducts = 0
            var syncedCustomers = 0
            var syncedOrders = 0
            var newRemoteCustomers = 0
            var newRemoteOrders = 0
            var pushedLocalProducts = 0
            var pushedLocalCustomers = 0
            var pushedLocalOrders = 0
            var latestNewCustomerName: String? = null
            var latestNewOrder: Order? = null

            val preSyncCustomers = dao.getAllCustomersList()
            val existingCustomerIds = preSyncCustomers.map { it.id }.toSet()
            val existingOrderIds = dao.getAllOrdersList().map { it.orderId }.toSet()
            val newRemoteCustomerList = mutableListOf<Customer>()
            val newRemoteOrderList = mutableListOf<Order>()

            // 1. Fetch Products & preserve valid local extended fields if cloud lacks them
            val pRes = supabaseSync.fetchProductsFromCloud()
            val cloudProducts = pRes.getOrNull() ?: emptyList()
            if (pRes.isSuccess) {
                val localEntitiesById = dao.getAllProductsList().associateBy { it.id }
                val changedEntities = mutableListOf<ProductEntity>()
                cloudProducts.forEach { product ->
                    val localEntity = localEntitiesById[product.id]
                    val localExisting = localEntity?.toDomain()
                    val resolvedImage = when {
                        product.imageUrl.isBlank() && localExisting != null -> localExisting.imageUrl
                        localExisting != null &&
                            ProductImageHelper.isUnreadableImageUri(appContext, product.imageUrl) &&
                            !ProductImageHelper.isUnreadableImageUri(appContext, localExisting.imageUrl) -> localExisting.imageUrl
                        else -> product.imageUrl
                    }
                    val mergedProduct = if (localExisting != null) {
                        product.copy(
                            imageUrl = resolvedImage,
                            galleryUrls = if (product.galleryUrls.isNotEmpty()) product.galleryUrls else localExisting.galleryUrls,
                            rating = if (localExisting.rating > 0.0) localExisting.rating else product.rating,
                            reviewCount = if (localExisting.reviewCount > 0) localExisting.reviewCount else product.reviewCount,
                            createdAt = if (localExisting.createdAt > 0L) localExisting.createdAt else product.createdAt,
                            partnerId = product.partnerId.ifBlank { localExisting.partnerId },
                            partnerName = product.partnerName.ifBlank { localExisting.partnerName },
                            partnerCommissionPercent = if (product.partnerCommissionPercent > 0.0) product.partnerCommissionPercent else localExisting.partnerCommissionPercent,
                            partnerBatchId = product.partnerBatchId.ifBlank { localExisting.partnerBatchId },
                            isCatalogLocked = product.isCatalogLocked || localExisting.isCatalogLocked
                        )
                    } else {
                        product
                    }
                    val mergedEntity = mergedProduct.toEntity()
                    if (localEntity != mergedEntity) {
                        changedEntities.add(mergedEntity)
                    }
                    syncedProducts++
                    if (!mergedProduct.id.startsWith("prod-")) {
                        customCloudProducts++
                    }
                }
                if (changedEntities.isNotEmpty()) {
                    dao.insertProducts(changedEntities)
                }
            } else {
                pRes.exceptionOrNull()?.message?.let { errors.add("Products: $it") }
            }

            // 2. Fetch Customers & merge cleanly (preserving local passwordHash and true earliest timestamp)
            val cRes = supabaseSync.fetchCustomersFromCloud()
            val cloudCustomers = cRes.getOrNull() ?: emptyList()
            if (cRes.isSuccess) {
                val allLocalCusts = dao.getAllCustomersList()
                cloudCustomers.forEach { cloudCust ->
                    val cloudPhoneKey = normalizePhoneKey(cloudCust.phoneNumber)
                    val existing = dao.getCustomerById(cloudCust.id)
                        ?: if (cloudCust.email.isNotBlank()) dao.getCustomerByEmail(cloudCust.email.lowercase()) else null
                        ?: if (cloudPhoneKey.isNotBlank()) allLocalCusts.find { normalizePhoneKey(it.phoneNumber) == cloudPhoneKey } else null

                    val mergedWishlist = if (cloudCust.wishlistProductIds.isNotEmpty()) {
                        cloudCust.wishlistProductIds.joinToString(",")
                    } else {
                        existing?.wishlistProductIds ?: ""
                    }
                    val resolvedCreatedAt = when {
                        existing != null && existing.createdAt > 0L && cloudCust.createdAt > 0L -> minOf(existing.createdAt, cloudCust.createdAt)
                        cloudCust.createdAt > 0L -> cloudCust.createdAt
                        existing != null && existing.createdAt > 0L -> existing.createdAt
                        else -> 0L
                    }
                    val mergedEntity = CustomerEntity(
                        id = existing?.id ?: cloudCust.id,
                        fullName = cloudCust.fullName.ifBlank { existing?.fullName ?: "Customer" },
                        email = cloudCust.email.ifBlank { existing?.email ?: "" },
                        passwordHash = existing?.passwordHash?.ifBlank { cloudCust.signUpPassword } ?: cloudCust.signUpPassword,
                        phoneNumber = cloudCust.phoneNumber.ifBlank { existing?.phoneNumber ?: "" },
                        deliveryAddress = cloudCust.deliveryAddress.ifBlank { existing?.deliveryAddress ?: "" },
                        createdAt = resolvedCreatedAt,
                        isAdmin = cloudCust.isAdmin || (existing?.isAdmin == true),
                        wishlistProductIds = mergedWishlist
                    )
                    dao.insertCustomer(mergedEntity)
                    syncedCustomers++
                    if (cloudCust.id !in existingCustomerIds && existing == null) {
                        newRemoteCustomers++
                        newRemoteCustomerList.add(cloudCust.copy(createdAt = resolvedCreatedAt))
                    }
                }
            } else {
                cRes.exceptionOrNull()?.message?.let { errors.add("Customers: $it") }
            }

            // 3. Fetch Orders & merge cleanly (preserving local receipt/statusUpdates/rider if cloud empty)
            val oRes = supabaseSync.fetchOrdersFromCloud()
            val cloudOrders = oRes.getOrNull() ?: emptyList()
            if (oRes.isSuccess) {
                cloudOrders.forEach { cloudOrder ->
                    val localExisting = dao.getOrderById(cloudOrder.orderId)?.toDomain()
                    val resolvedCreatedAt = when {
                        localExisting != null && localExisting.createdAt > 0L && cloudOrder.createdAt > 0L -> minOf(localExisting.createdAt, cloudOrder.createdAt)
                        cloudOrder.createdAt > 0L -> cloudOrder.createdAt
                        localExisting != null && localExisting.createdAt > 0L -> localExisting.createdAt
                        else -> 0L
                    }
                    val mergedOrder = if (localExisting != null) {
                        cloudOrder.copy(
                            customerName = cloudOrder.customerName.ifBlank { localExisting.customerName },
                            customerEmail = cloudOrder.customerEmail.ifBlank { localExisting.customerEmail },
                            customerPhone = cloudOrder.customerPhone.ifBlank { localExisting.customerPhone },
                            deliveryAddress = cloudOrder.deliveryAddress.ifBlank { localExisting.deliveryAddress },
                            senderName = cloudOrder.senderName.ifBlank { localExisting.senderName },
                            receiptImageUrl = cloudOrder.receiptImageUrl.ifBlank { localExisting.receiptImageUrl },
                            statusUpdates = cloudOrder.statusUpdates.ifEmpty { localExisting.statusUpdates },
                            dispatchRiderWhatsapp = cloudOrder.dispatchRiderWhatsapp.ifBlank { localExisting.dispatchRiderWhatsapp },
                            createdAt = resolvedCreatedAt
                        )
                    } else {
                        cloudOrder.copy(createdAt = resolvedCreatedAt)
                    }
                    dao.insertOrder(mergedOrder.toEntity())
                    syncedOrders++
                    if (cloudOrder.orderId !in existingOrderIds) {
                        newRemoteOrders++
                        newRemoteOrderList.add(mergedOrder)
                    }
                }
            } else {
                oRes.exceptionOrNull()?.message?.let { errors.add("Orders: $it") }
            }

            // 4. Push any local-only custom products OR custom products with readable local photos awaiting cloud upload
            if (pRes.isSuccess) {
                val cloudProdIds = cloudProducts.map { it.id }.toSet()
                val localCustomProducts = dao.getAllProductsList().filter { !it.id.startsWith("prod-") }
                localCustomProducts.forEach { lp ->
                    val domainProd = lp.toDomain()
                    val hasReadableLocalImage = ProductImageHelper.isLocalOnlyUri(domainProd.imageUrl) &&
                        !ProductImageHelper.isUnreadableImageUri(appContext, domainProd.imageUrl)
                    if (lp.id !in cloudProdIds || hasReadableLocalImage) {
                        val uploadedImg = uploadImageUriToBucketIfNeeded(
                            bucketId = "product-media",
                            filePrefix = "prod_${domainProd.id.take(12)}",
                            uriString = domainProd.imageUrl
                        )
                        val prodToPush = if (uploadedImg != domainProd.imageUrl) {
                            domainProd.copy(imageUrl = uploadedImg).also { dao.insertProduct(it.toEntity()) }
                        } else domainProd
                        val pushRes = supabaseSync.syncProductToCloud(prodToPush)
                        if (pushRes.isSuccess && lp.id !in cloudProdIds) pushedLocalProducts++
                    }
                }
            }

            if (cRes.isSuccess) {
                val localCustomers = dao.getAllCustomersList()
                localCustomers.forEach { lc ->
                    val emailKey = lc.email.trim().lowercase()
                    val phoneKey = normalizePhoneKey(lc.phoneNumber)
                    val matchedCloud = cloudCustomers.find { cc ->
                        cc.id == lc.id ||
                            (emailKey.isNotBlank() && cc.email.trim().lowercase() == emailKey) ||
                            (phoneKey.isNotBlank() && normalizePhoneKey(cc.phoneNumber) == phoneKey)
                    }
                    val needsCredentialOrFieldBackfill = matchedCloud != null && (
                        (lc.passwordHash.isNotBlank() && matchedCloud.signUpPassword.isBlank()) ||
                            (lc.phoneNumber.isNotBlank() && matchedCloud.phoneNumber.isBlank()) ||
                            (lc.deliveryAddress.isNotBlank() && matchedCloud.deliveryAddress.isBlank())
                        )
                    if (matchedCloud == null || needsCredentialOrFieldBackfill) {
                        val pushRes = supabaseSync.syncCustomerToCloud(lc.toDomain())
                        if (pushRes.isSuccess) {
                            if (matchedCloud == null) pushedLocalCustomers++
                        } else {
                            pushRes.exceptionOrNull()?.message?.let { errors.add("Push signup (${lc.fullName}): $it") }
                        }
                    }
                }
            }

            if (oRes.isSuccess) {
                val localOrders = dao.getAllOrdersList()
                localOrders.forEach { lo ->
                    val matchedCloudOrder = cloudOrders.find { it.orderId == lo.orderId }
                    val domainOrder = lo.toDomain()
                    val hasUploadableLocalReceipt = domainOrder.receiptImageUrl.isNotBlank() &&
                        !domainOrder.receiptImageUrl.startsWith("http", ignoreCase = true) &&
                        !domainOrder.receiptImageUrl.startsWith("data:image/", ignoreCase = true) &&
                        !ProductImageHelper.isUnreadableImageUri(appContext, domainOrder.receiptImageUrl)
                    val needsReceiptBackfill = (matchedCloudOrder != null &&
                        matchedCloudOrder.receiptImageUrl.isBlank() &&
                        (hasUploadableLocalReceipt || domainOrder.receiptImageUrl.startsWith("http", ignoreCase = true) || domainOrder.receiptImageUrl.startsWith("data:image/", ignoreCase = true))) ||
                        hasUploadableLocalReceipt

                    if (matchedCloudOrder == null || needsReceiptBackfill) {
                        val uploadedUrl = uploadReceiptUriIfNeeded(domainOrder.orderId, domainOrder.receiptImageUrl)
                        val orderToPush = if (uploadedUrl != domainOrder.receiptImageUrl) {
                            domainOrder.copy(receiptImageUrl = uploadedUrl).also { dao.insertOrder(it.toEntity()) }
                        } else domainOrder
                        val pushRes = supabaseSync.syncOrderToCloud(orderToPush)
                        if (pushRes.isSuccess) {
                            if (matchedCloudOrder == null) pushedLocalOrders++
                        } else {
                            pushRes.exceptionOrNull()?.message?.let { errors.add("Push order (${lo.orderId}): $it") }
                        }
                    }
                }
            }

            // 5. Synthesize customer dossiers for any cloud orders whose customer record isn't in Room yet
            val currentCustomers = dao.getAllCustomersList().toMutableList()
            dao.getAllOrdersList().forEach { ord ->
                val ordPhoneKey = normalizePhoneKey(ord.customerPhone)
                val matchesExisting = currentCustomers.any { cust ->
                    cust.id == ord.customerId ||
                        (ord.customerEmail.isNotBlank() && cust.email.equals(ord.customerEmail, ignoreCase = true)) ||
                        (ordPhoneKey.isNotBlank() && normalizePhoneKey(cust.phoneNumber) == ordPhoneKey)
                }
                if (!matchesExisting && (ord.customerName.isNotBlank() || ord.customerPhone.isNotBlank())) {
                    val synthId = ord.customerId.ifBlank { "cust-${ord.orderId}" }
                    val synthEmail = ord.customerEmail.ifBlank {
                        "cust_${ord.customerPhone.filter { it.isDigit() }.ifBlank { synthId.take(6) }}@247shopping.local"
                    }
                    val synthEntity = CustomerEntity(
                        id = synthId,
                        fullName = ord.customerName.ifBlank { "Customer" },
                        email = synthEmail,
                        passwordHash = "",
                        phoneNumber = ord.customerPhone,
                        deliveryAddress = ord.deliveryAddress,
                        createdAt = ord.createdAt,
                        isAdmin = false,
                        wishlistProductIds = ""
                    )
                    dao.insertCustomer(synthEntity)
                    currentCustomers.add(synthEntity)
                    if (ord.orderId !in existingOrderIds && ord.createdAt > 0L) {
                        newRemoteCustomers++
                        newRemoteCustomerList.add(synthEntity.toDomain())
                    }
                }
            }

            val newestCust = newRemoteCustomerList.maxByOrNull { it.createdAt }
            latestNewCustomerName = newestCust?.fullName
            val latestNewCustomerCreatedAt = newestCust?.createdAt ?: 0L
            latestNewOrder = newRemoteOrderList.maxByOrNull { it.createdAt }

            lastCloudSyncError = if (errors.isNotEmpty()) errors.joinToString(" | ") else null

            if (pRes.isFailure && cRes.isFailure && oRes.isFailure) {
                Result.failure(Exception(errors.joinToString(" | ").ifBlank { "Cloud sync unavailable" }))
            } else {
                Result.success(
                    CloudSyncSummary(
                        syncedProducts = syncedProducts,
                        customCloudProducts = customCloudProducts,
                        syncedCustomers = syncedCustomers,
                        syncedOrders = syncedOrders,
                        newRemoteCustomers = newRemoteCustomers,
                        newRemoteOrders = newRemoteOrders,
                        pushedLocalProducts = pushedLocalProducts,
                        pushedLocalCustomers = pushedLocalCustomers,
                        pushedLocalOrders = pushedLocalOrders,
                        latestNewCustomerName = latestNewCustomerName,
                        latestNewCustomerCreatedAt = latestNewCustomerCreatedAt,
                        latestNewOrder = latestNewOrder,
                        errors = errors
                    )
                )
            }
        } catch (e: Exception) {
            lastCloudSyncError = e.message
            Result.failure(e)
        }
    }

    suspend fun fetchAndSyncAllFromCloud(): Result<Int> {
        val res = fetchAndSyncAllFromCloudDetailed()
        return res.map { it.syncedProducts + it.syncedCustomers + it.syncedOrders }
    }

    // Customers Flow & Auth
    val allCustomers: Flow<List<Customer>> = dao.getAllCustomers().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun registerCustomer(fullName: String, password: String, phone: String, address: String = "", email: String = ""): Result<Customer> = withContext(Dispatchers.IO) {
        try {
            val cleanPhone = phone.trim()
            val phoneKey = normalizePhoneKey(cleanPhone)
            val allLocal = dao.getAllCustomersList()
            val existing = dao.getCustomerByPhone(cleanPhone)
                ?: (if (phoneKey.isNotBlank()) allLocal.find { normalizePhoneKey(it.phoneNumber) == phoneKey } else null)
                ?: if (email.isNotBlank()) dao.getCustomerByEmail(email.trim().lowercase()) else null
            if (existing != null) {
                return@withContext Result.failure(Exception("An account with this phone number already exists. Please use Sign In instead."))
            }
            val effectiveEmail = email.ifBlank { "cust_${cleanPhone.filter { it.isDigit() }.ifBlank { UUID.randomUUID().toString().take(6) }}@247shopping.local" }
            val hashedPassword = SecurityUtils.hashPassword(password)
            val customer = Customer(
                id = UUID.randomUUID().toString(),
                fullName = fullName.trim(),
                email = effectiveEmail.trim().lowercase(),
                phoneNumber = cleanPhone,
                deliveryAddress = address.trim(),
                createdAt = System.currentTimeMillis(),
                isAdmin = fullName.trim().lowercase().contains("admin") || effectiveEmail.contains("admin"),
                signUpPassword = hashedPassword
            )
            val entity = CustomerEntity(
                id = customer.id,
                fullName = customer.fullName,
                email = customer.email,
                passwordHash = hashedPassword, // Salted SHA-256 hash
                phoneNumber = customer.phoneNumber,
                deliveryAddress = customer.deliveryAddress,
                createdAt = customer.createdAt,
                isAdmin = customer.isAdmin
            )
            dao.insertCustomer(entity)
            if (!supabaseSync.isPlaceholderConfig()) {
                val cloudRes = supabaseSync.syncCustomerToCloud(customer)
                lastCloudSyncError = if (cloudRes.isFailure) cloudRes.exceptionOrNull()?.message else null
            } else {
                lastCloudSyncError = "Store Cloud Sync link is not configured on this device yet."
            }
            Result.success(customer)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCustomer(customerId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.deleteCustomer(customerId)
            supabaseSync.deleteCustomerFromCloud(customerId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun upsertExternalCustomer(customer: Customer): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val existing = dao.getCustomerById(customer.id)
                ?: if (customer.email.isNotBlank()) dao.getCustomerByEmail(customer.email.lowercase()) else null
            val merged = CustomerEntity(
                id = existing?.id ?: customer.id,
                fullName = customer.fullName.ifBlank { existing?.fullName ?: "Customer" },
                email = customer.email.ifBlank { existing?.email ?: "" },
                passwordHash = existing?.passwordHash ?: "",
                phoneNumber = customer.phoneNumber.ifBlank { existing?.phoneNumber ?: "" },
                deliveryAddress = customer.deliveryAddress.ifBlank { existing?.deliveryAddress ?: "" },
                createdAt = existing?.createdAt ?: customer.createdAt,
                isAdmin = customer.isAdmin || (existing?.isAdmin == true),
                wishlistProductIds = existing?.wishlistProductIds?.ifBlank { customer.wishlistProductIds.joinToString(",") }
                    ?: customer.wishlistProductIds.joinToString(",")
            )
            dao.insertCustomer(merged)
            val cloudRes = supabaseSync.syncCustomerToCloud(merged.toDomain())
            lastCloudSyncError = if (cloudRes.isFailure) cloudRes.exceptionOrNull()?.message else null
            cloudRes
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCustomerDeliveryDetails(
        customerId: String,
        fullName: String,
        phone: String,
        address: String
    ) = withContext(Dispatchers.IO) {
        try {
            val existing = dao.getCustomerById(customerId) ?: return@withContext
            val updated = existing.copy(
                fullName = fullName.trim().ifBlank { existing.fullName },
                phoneNumber = phone.trim().ifBlank { existing.phoneNumber },
                deliveryAddress = address.trim().ifBlank { existing.deliveryAddress }
            )
            if (updated != existing) {
                dao.insertCustomer(updated)
                if (!supabaseSync.isPlaceholderConfig()) {
                    supabaseSync.syncCustomerToCloud(updated.toDomain())
                }
            }
        } catch (_: Exception) {}
    }

    suspend fun authenticateCustomer(identifier: String, password: String): Result<Customer> = withContext(Dispatchers.IO) {
        try {
            val cleanId = identifier.trim()
            val phoneKey = normalizePhoneKey(cleanId)
            val allLocal = dao.getAllCustomersList()
            // Look up by phone number first (including normalized last-10 digits), or by email
            val entity = dao.getCustomerByPhone(cleanId)
                ?: (if (phoneKey.isNotBlank()) allLocal.find { normalizePhoneKey(it.phoneNumber) == phoneKey } else null)
                ?: dao.getCustomerByEmail(cleanId.lowercase())
                ?: return@withContext Result.failure(Exception("No account found with this phone number or credential."))
            if (entity.passwordHash.isBlank()) {
                // Cloud-pulled customer signing back in on a new install sets/confirms their device password cleanly
                val updated = entity.copy(passwordHash = SecurityUtils.hashPassword(password))
                dao.insertCustomer(updated)
                return@withContext Result.success(updated.toDomain())
            }
            if (!SecurityUtils.verifyPassword(password, entity.passwordHash)) {
                return@withContext Result.failure(Exception("Incorrect password. Please try again."))
            }
            if (SecurityUtils.needsHashUpgrade(entity.passwordHash)) {
                val upgraded = entity.copy(passwordHash = SecurityUtils.hashPassword(password))
                dao.insertCustomer(upgraded)
                return@withContext Result.success(upgraded.toDomain())
            }
            Result.success(entity.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Mappers
    private fun ProductEntity.toDomain(): Product {
        val colors = availableColorsJson.split(";").mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size >= 2) ProductColor(parts[0], parts[1]) else null
        }
        val parsedGallery = if (galleryUrlsJson.isNotBlank()) {
            galleryUrlsJson.split("|").map { it.trim() }.filter { it.isNotBlank() }
        } else emptyList()
        return Product(
            id = id,
            title = title,
            category = category,
            brand = brand,
            availableBrands = if (availableBrands.isNotBlank()) availableBrands.split(",") else listOf(brand),
            availableSizes = if (availableSizes.isNotBlank()) availableSizes.split(",") else listOf("Standard"),
            availableColors = if (colors.isNotEmpty()) colors else listOf(ProductColor("Default", "#1E40AF")),
            basePrice = basePrice,
            costPrice = if (costPrice >= 0.0) costPrice else (basePrice * 0.70),
            discountPercent = discountPercent,
            stockQuantity = stockQuantity,
            imageUrl = imageUrl,
            galleryUrls = parsedGallery,
            model3dType = model3dType,
            description = description,
            rating = rating,
            reviewCount = reviewCount,
            sku = sku,
            expiryMonthsRemaining = expiryMonthsRemaining,
            createdAt = createdAt,
            partnerId = partnerId,
            partnerName = partnerName,
            partnerCommissionPercent = partnerCommissionPercent,
            partnerBatchId = partnerBatchId,
            isCatalogLocked = isCatalogLocked
        )
    }

    private fun Product.toEntity(): ProductEntity {
        val colorsStr = availableColors.joinToString(";") { "${it.name}|${it.hex}" }
        return ProductEntity(
            id = id,
            title = title,
            category = category,
            brand = brand,
            availableBrands = availableBrands.joinToString(","),
            availableSizes = availableSizes.joinToString(","),
            availableColorsJson = colorsStr,
            basePrice = basePrice,
            discountPercent = discountPercent,
            stockQuantity = stockQuantity,
            imageUrl = imageUrl,
            model3dType = model3dType,
            description = description,
            rating = rating,
            reviewCount = reviewCount,
            sku = sku,
            costPrice = costPrice,
            expiryMonthsRemaining = expiryMonthsRemaining,
            createdAt = createdAt,
            partnerId = partnerId,
            partnerName = partnerName,
            partnerCommissionPercent = partnerCommissionPercent,
            partnerBatchId = partnerBatchId,
            galleryUrlsJson = galleryUrls.filter { it.isNotBlank() }.distinct().joinToString("|"),
            isCatalogLocked = isCatalogLocked
        )
    }

    private fun OrderEntity.toDomain(): Order {
        val method = try {
            PaymentMethod.valueOf(paymentMethod)
        } catch (e: Exception) {
            PaymentMethod.OPAY
        }
        val orderStatus = try {
            OrderStatus.valueOf(status)
        } catch (e: Exception) {
            OrderStatus.PENDING_VERIFICATION
        }
        val updates = statusUpdatesJson.split(";").mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size >= 3) {
                StatusUpdate(
                    status = try { OrderStatus.valueOf(parts[0]) } catch(e: Exception) { OrderStatus.PENDING_VERIFICATION },
                    timestamp = parts[1].toLongOrNull() ?: System.currentTimeMillis(),
                    note = parts[2]
                )
            } else null
        }
        return Order(
            orderId = orderId,
            customerId = customerId,
            customerName = customerName,
            customerEmail = customerEmail,
            customerPhone = customerPhone,
            deliveryAddress = deliveryAddress,
            itemsSummary = itemsSummary,
            totalAmount = totalAmount,
            profit = profit,
            paymentMethod = method,
            paymentAccount = paymentAccount,
            paymentReference = paymentReference,
            senderName = senderName,
            status = orderStatus,
            createdAt = createdAt,
            verificationNote = verificationNote,
            receiptImageUrl = receiptImageUrl,
            statusUpdates = updates,
            dispatchRiderWhatsapp = dispatchRiderWhatsapp,
            partnerId = partnerId,
            partnerName = partnerName,
            partnerCommissionAmount = partnerCommissionAmount,
            partnerNetPayout = partnerNetPayout
        )
    }

    private fun Order.toEntity(): OrderEntity {
        val updatesStr = statusUpdates.joinToString(";") { "${it.status.name}|${it.timestamp}|${it.note}" }
        return OrderEntity(
            orderId = orderId,
            customerId = customerId,
            customerName = customerName,
            customerEmail = customerEmail,
            customerPhone = customerPhone,
            deliveryAddress = deliveryAddress,
            itemsSummary = itemsSummary,
            totalAmount = totalAmount,
            profit = profit,
            paymentMethod = paymentMethod.name,
            paymentAccount = paymentAccount,
            paymentReference = paymentReference,
            senderName = senderName,
            status = status.name,
            createdAt = createdAt,
            verificationNote = verificationNote,
            receiptImageUrl = receiptImageUrl,
            statusUpdatesJson = updatesStr,
            dispatchRiderWhatsapp = dispatchRiderWhatsapp,
            partnerId = partnerId,
            partnerName = partnerName,
            partnerCommissionAmount = partnerCommissionAmount,
            partnerNetPayout = partnerNetPayout
        )
    }

    suspend fun updateOrderReceiptDetails(
        orderId: String,
        receiptImageUrl: String,
        senderName: String,
        paymentReference: String,
        status: OrderStatus,
        note: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.updateOrderReceiptDetails(orderId, receiptImageUrl, senderName, paymentReference, status.name, note)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCustomerWishlist(customerId: String): Flow<List<String>> {
        return dao.getCustomerWishlist(customerId).map { raw ->
            if (raw.isNullOrBlank()) emptyList() else raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
    }

    suspend fun toggleCustomerWishlist(customerId: String, productId: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val customer = dao.getCustomerById(customerId) ?: return@withContext Result.failure(Exception("Customer not found"))
            val currentList = if (customer.wishlistProductIds.isBlank()) mutableListOf() else customer.wishlistProductIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
            if (currentList.contains(productId)) {
                currentList.remove(productId)
            } else {
                currentList.add(productId)
            }
            val updatedStr = currentList.joinToString(",")
            dao.updateCustomerWishlist(customerId, updatedStr)
            // Sync with Supabase Cloud user profile in database
            supabaseSync.syncCustomerWishlistToCloud(customerId, currentList)
            firebaseRepo.setWishlistProduct(productId, currentList.contains(productId))
            Result.success(currentList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun CustomerEntity.toDomain(): Customer {
        val wishlist = if (wishlistProductIds.isNotBlank()) {
            wishlistProductIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else emptyList()
        return Customer(
            id = id,
            fullName = fullName,
            email = email,
            phoneNumber = phoneNumber,
            deliveryAddress = deliveryAddress,
            createdAt = createdAt,
            isAdmin = isAdmin,
            wishlistProductIds = wishlist,
            signUpPassword = passwordHash
        )
    }


    suspend fun seedFull100ProductsCatalog(): Int = withContext(Dispatchers.IO) {
        val items = CatalogDataSeeder.generate100Products()
        dao.insertProducts(items.map { it.toEntity() })
        if (!supabaseSync.isPlaceholderConfig()) {
            supabaseSync.syncProductsBatchToCloud(items)
        }
        items.size
    }

    private suspend fun seedDefaultProductsIfEmpty() {
        try {
            if (dao.getProductCount() == 0) {
                val sampleItems = CatalogDataSeeder.generate100Products()
                dao.insertProducts(sampleItems.map { it.toEntity() })
                if (!supabaseSync.isPlaceholderConfig()) {
                    scope.launch {
                        try {
                            supabaseSync.syncProductsBatchToCloud(sampleItems)
                        } catch (_: Exception) {}
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("EcommerceRepository", "Error seeding default products", e)
        }
    }
}
