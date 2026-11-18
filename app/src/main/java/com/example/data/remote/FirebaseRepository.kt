package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.CartItem
import com.example.data.model.Customer
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Throwable, operationType: OperationType, path: String?): String {
    val causes = generateSequence(exception) { it.cause }
    val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
        ?: causes.firstOrNull { it !is CancellationException }
        ?: exception

    val auth = try { FirebaseAuth.getInstance() } catch (_: Exception) { null }
    val currentUser = auth?.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", rootError.message ?: rootError.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", jsonString, exception)
    return jsonString
}

class FirebaseRepository(private val db: FirebaseFirestore?) {

    constructor(context: Context) : this(
        try {
            if (FirebaseApp.getApps(context.applicationContext).isNotEmpty()) {
                FirebaseFirestore.getInstance(
                    context.applicationContext.getString(R.string.firestore_database_id)
                )
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    )

    val auth: FirebaseAuth? = try {
        if (db != null) Firebase.auth else null
    } catch (_: Exception) {
        null
    }

    private val _currentUserFlow = MutableStateFlow<FirebaseUser?>(auth?.currentUser)
    val currentUserFlow: StateFlow<FirebaseUser?> = _currentUserFlow.asStateFlow()

    init {
        auth?.addAuthStateListener { firebaseAuth ->
            _currentUserFlow.value = firebaseAuth.currentUser
        }
    }

    fun requireUserId(): String {
        return auth?.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    suspend fun signInWithGoogleCredential(idToken: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        val activeAuth = auth
            ?: return@withContext Result.failure(IllegalStateException("Firebase Auth is not initialized"))
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = activeAuth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw IllegalStateException("Google Sign-In returned null user")
            _currentUserFlow.value = user
            Result.success(user)
        } catch (e: Exception) {
            Log.e("FirebaseAuth", "Sign in with Google credential failed", e)
            Result.failure(e)
        }
    }

    fun signOut() {
        auth?.signOut()
        _currentUserFlow.value = null
    }

    // -------------------------------------------------------------
    // Customer User Profile
    // -------------------------------------------------------------
    suspend fun syncUserProfile(customer: Customer): Result<Unit> = withContext(Dispatchers.IO) {
        val activeDb = db
            ?: return@withContext Result.failure(IllegalStateException("Firestore is not initialized"))
        val uid = auth?.currentUser?.uid ?: customer.id
        val docRef = activeDb.collection("users").document(uid)
        val payload = mapOf(
            "userId" to uid,
            "displayName" to customer.fullName,
            "phoneNumber" to customer.phoneNumber,
            "defaultAddress" to customer.deliveryAddress,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // Customer Orders Sync & Realtime Stream
    // -------------------------------------------------------------
    suspend fun syncOrder(order: Order): Result<Unit> = withContext(Dispatchers.IO) {
        val activeDb = db
            ?: return@withContext Result.failure(IllegalStateException("Firestore is not initialized"))
        val uid = auth?.currentUser?.uid ?: order.customerId
        val docRef = activeDb.collection("users").document(uid).collection("orders").document(order.orderId)
        val payload = mapOf(
            "orderId" to order.orderId,
            "userId" to uid,
            "customerName" to order.customerName,
            "customerEmail" to order.customerEmail,
            "customerPhone" to order.customerPhone,
            "deliveryAddress" to order.deliveryAddress,
            "itemsSummary" to order.itemsSummary,
            "paymentMethod" to order.paymentMethod.name,
            "paymentAccount" to order.paymentAccount,
            "paymentReference" to order.paymentReference,
            "senderName" to order.senderName,
            "totalAmount" to order.totalAmount,
            "deliveryStatus" to order.status.name,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeUserOrders(uid: String): Flow<List<Order>> {
        val activeDb = db ?: return emptyFlow()
        val colRef = activeDb.collection("users").document(uid).collection("orders")
        return colRef.snapshots().map { snapshot ->
            snapshot.documents.mapNotNull { doc ->
                val orderId = doc.getString("orderId") ?: doc.id
                val customerName = doc.getString("customerName") ?: "Customer"
                val customerPhone = doc.getString("customerPhone") ?: ""
                val address = doc.getString("deliveryAddress") ?: ""
                val total = doc.getDouble("totalAmount") ?: 0.0
                val statusStr = doc.getString("deliveryStatus") ?: OrderStatus.PENDING_VERIFICATION.name
                val status = try { OrderStatus.valueOf(statusStr) } catch (e: Exception) { OrderStatus.PENDING_VERIFICATION }
                val payMethodStr = doc.getString("paymentMethod") ?: PaymentMethod.OPAY.name
                val payMethod = try { PaymentMethod.valueOf(payMethodStr) } catch (e: Exception) { PaymentMethod.OPAY }

                Order(
                    orderId = orderId,
                    customerId = uid,
                    customerName = customerName,
                    customerEmail = doc.getString("customerEmail") ?: "",
                    customerPhone = customerPhone,
                    deliveryAddress = address,
                    itemsSummary = doc.getString("itemsSummary") ?: "",
                    totalAmount = total,
                    paymentMethod = payMethod,
                    paymentAccount = doc.getString("paymentAccount") ?: "",
                    paymentReference = doc.getString("paymentReference") ?: "",
                    senderName = doc.getString("senderName") ?: "",
                    status = status,
                    createdAt = System.currentTimeMillis()
                )
            }
        }.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            if (auth?.currentUser?.uid == uid) {
                handleFirestoreError(firestoreError ?: error, OperationType.LIST, colRef.path)
            }
            throw error
        }
    }

    // -------------------------------------------------------------
    // Wishlist Sync & Stream
    // -------------------------------------------------------------
    suspend fun setWishlistProduct(productId: String, isWishlisted: Boolean, customerId: String = ""): Result<Unit> = withContext(Dispatchers.IO) {
        val activeDb = db
            ?: return@withContext Result.failure(IllegalStateException("Firestore is not initialized"))
        val uid = auth?.currentUser?.uid ?: customerId.ifBlank { "guest_user" }
        val docRef = activeDb.collection("users").document(uid).collection("wishlist").document(productId)
        try {
            if (isWishlisted) {
                val payload = mapOf(
                    "productId" to productId,
                    "userId" to uid,
                    "addedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                docRef.delete().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, if (isWishlisted) OperationType.CREATE else OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeUserWishlist(uid: String): Flow<Set<String>> {
        val activeDb = db ?: return emptyFlow()
        val colRef = activeDb.collection("users").document(uid).collection("wishlist")
        return colRef.snapshots().map { snapshot ->
            snapshot.documents.mapNotNull { it.getString("productId") ?: it.id }.toSet()
        }.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            if (auth?.currentUser?.uid == uid) {
                handleFirestoreError(firestoreError ?: error, OperationType.LIST, colRef.path)
            }
            throw error
        }
    }
}
