package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.remote.FirebaseRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirebaseRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun authenticatedUser_canCreateOrder() = runBlocking {
    val uid = signInTestUser("alice@depros.com")
    val repo = FirebaseRepository(firestore)

    val order = Order(
      orderId = "ORD-TEST-001",
      customerId = uid,
      customerName = "Alice Shopper",
      customerEmail = "alice@depros.com",
      customerPhone = "08012345678",
      deliveryAddress = "15 Allen Avenue, Ikeja",
      itemsSummary = "Gucci Polo (Size L)",
      totalAmount = 35000.0,
      paymentMethod = PaymentMethod.OPAY,
      paymentAccount = "9928172611",
      paymentReference = "REF-10029",
      senderName = "Alice Shopper",
      status = OrderStatus.PENDING_VERIFICATION
    )

    val result = repo.syncOrder(order)
    assertTrue("Order sync should succeed for owner", result.isSuccess)
  }

  @Test
  fun unauthenticatedUser_failsCreatingOrder() = runBlocking {
    auth.signOut()
    val repo = FirebaseRepository(firestore)

    val order = Order(
      orderId = "ORD-TEST-002",
      customerId = "random_uid",
      customerName = "Intruder",
      customerEmail = "intruder@bad.com",
      customerPhone = "08000000000",
      deliveryAddress = "Unknown",
      itemsSummary = "Item",
      totalAmount = 1000.0,
      paymentMethod = PaymentMethod.OPAY,
      paymentAccount = "000000",
      paymentReference = "REF-000",
      senderName = "Intruder",
      status = OrderStatus.PENDING_VERIFICATION
    )

    val result = repo.syncOrder(order)
    assertTrue("Unauthenticated order sync must fail", result.isFailure)
  }

  @Test
  fun authenticatedUser_canSetWishlist() = runBlocking {
    val uid = signInTestUser("bob@depros.com")
    val repo = FirebaseRepository(firestore)

    val result = repo.setWishlistProduct("prod_nike_air", true)
    assertTrue("Wishlist addition should succeed for owner", result.isSuccess)

    val removeResult = repo.setWishlistProduct("prod_nike_air", false)
    assertTrue("Wishlist deletion should succeed for owner", removeResult.isSuccess)
  }
}
