package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DeprosDao {
    @Query("SELECT * FROM products ORDER BY CASE WHEN id NOT LIKE 'prod-%' THEN 0 ELSE 1 END, createdAt DESC, title ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY CASE WHEN id NOT LIKE 'prod-%' THEN 0 ELSE 1 END, createdAt DESC, title ASC")
    suspend fun getAllProductsList(): List<ProductEntity>

    @Query("SELECT COUNT(*) FROM products")
    suspend fun getProductCount(): Int

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): ProductEntity?

    @Query("SELECT * FROM products WHERE category = :category ORDER BY title ASC")
    fun getProductsByCategory(category: String): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProduct(id: String)

    // Orders
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    suspend fun getAllOrdersList(): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun getOrdersForCustomer(customerId: String): Flow<List<OrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Query("SELECT * FROM orders WHERE orderId = :orderId LIMIT 1")
    suspend fun getOrderById(orderId: String): OrderEntity?

    @Query("UPDATE orders SET status = :status, verificationNote = :note WHERE orderId = :orderId")
    suspend fun updateOrderStatus(orderId: String, status: String, note: String)

    @Query("UPDATE orders SET receiptImageUrl = :receiptUrl, senderName = :sender, paymentReference = :ref, status = :status, verificationNote = :note WHERE orderId = :orderId")
    suspend fun updateOrderReceiptDetails(orderId: String, receiptUrl: String, sender: String, ref: String, status: String, note: String)

    // Customers
    @Query("SELECT * FROM customers ORDER BY createdAt DESC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers ORDER BY createdAt DESC")
    suspend fun getAllCustomersList(): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE email = :email LIMIT 1")
    suspend fun getCustomerByEmail(email: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE phoneNumber = :phone LIMIT 1")
    suspend fun getCustomerByPhone(phone: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: String): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomer(id: String)

    @Query("UPDATE customers SET wishlistProductIds = :wishlistStr WHERE id = :customerId")
    suspend fun updateCustomerWishlist(customerId: String, wishlistStr: String)

    @Query("SELECT wishlistProductIds FROM customers WHERE id = :customerId LIMIT 1")
    fun getCustomerWishlist(customerId: String): Flow<String?>
}
