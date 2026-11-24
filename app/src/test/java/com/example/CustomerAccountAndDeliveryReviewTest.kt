package com.example

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CustomerAccountAndDeliveryReviewTest {

    private lateinit var app: Application
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        app.getSharedPreferences("depros_admin_settings", android.content.Context.MODE_PRIVATE)
            .edit().clear().commit()
        viewModel = MainViewModel(app)
        pumpLooper()
    }

    @After
    fun tearDown() {
        try {
            viewModel.endLiveCall()
        } catch (_: Exception) {}
        try {
            viewModel.viewModelScope.cancel()
        } catch (_: Exception) {}
        com.example.data.local.DeprosDatabase.resetInstanceForTests(app)
    }

    private fun pumpLooper() = runBlocking {
        repeat(8) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            delay(50)
        }
    }

    @Test
    fun customerAccountShortcutNavigatesToVerticalMenuAndSubDestinations() {
        viewModel.openCustomerAccountShortcut()
        assertEquals(AppScreen.AUTH, viewModel.currentScreen.value)
        assertEquals(CustomerAccountDestination.MENU, viewModel.customerAccountDestination.value)

        viewModel.selectCustomerAccountDestination(CustomerAccountDestination.WELCOME_PROFILE)
        assertEquals(CustomerAccountDestination.WELCOME_PROFILE, viewModel.customerAccountDestination.value)

        viewModel.selectCustomerAccountDestination(CustomerAccountDestination.INBOX)
        assertEquals(CustomerAccountDestination.INBOX, viewModel.customerAccountDestination.value)

        viewModel.selectCustomerAccountDestination(CustomerAccountDestination.ORDER_TRACKING)
        assertEquals(CustomerAccountDestination.ORDER_TRACKING, viewModel.customerAccountDestination.value)

        viewModel.selectCustomerAccountDestination(CustomerAccountDestination.RATINGS_AND_REVIEWS)
        assertEquals(CustomerAccountDestination.RATINGS_AND_REVIEWS, viewModel.customerAccountDestination.value)

        viewModel.selectCustomerAccountDestination(CustomerAccountDestination.VOUCHERS)
        assertEquals(CustomerAccountDestination.VOUCHERS, viewModel.customerAccountDestination.value)

        viewModel.selectCustomerAccountDestination(CustomerAccountDestination.FOLLOW_STORE)
        assertEquals(CustomerAccountDestination.FOLLOW_STORE, viewModel.customerAccountDestination.value)
    }

    @Test
    fun followStoreTogglePersistsAcrossViewModelReads() {
        assertFalse(viewModel.isFollowingStore.value)
        val nowFollowing = viewModel.toggleFollowStore()
        assertTrue(nowFollowing)
        assertTrue(viewModel.isFollowingStore.value)

        val unfollowed = viewModel.toggleFollowStore()
        assertFalse(unfollowed)
        assertFalse(viewModel.isFollowingStore.value)
    }

    @Test
    fun ratingsAndReviewsLockedUntilOrderIsDelivered() = runBlocking {
        var sampleProduct = viewModel.allProducts.value.firstOrNull()
        repeat(20) {
            if (sampleProduct != null) return@repeat
            pumpLooper()
            sampleProduct = viewModel.allProducts.value.firstOrNull()
        }
        val product = sampleProduct ?: error("Expected catalog products to load")
        var rejectSuccess = true
        var rejectMessage = ""

        // Attempt before any delivered order exists
        viewModel.submitProductReview(
            productId = product.id,
            rating = 5,
            comment = "Great product!"
        ) { ok, msg ->
            rejectSuccess = ok
            rejectMessage = msg
        }
        assertFalse(rejectSuccess)
        assertTrue(rejectMessage.contains("delivered", ignoreCase = true))

        // Insert a non-delivered order (PENDING_VERIFICATION) containing this product
        val pendingOrder = Order(
            orderId = "ORD-PENDING-1",
            customerId = "cust-1",
            customerName = "Ada Okafor",
            customerEmail = "ada@example.com",
            customerPhone = "08012345678",
            deliveryAddress = "Lagos",
            itemsSummary = "1x ${product.title}",
            totalAmount = product.basePrice,
            paymentMethod = PaymentMethod.OPAY,
            paymentAccount = "ODIYA OPay",
            paymentReference = "REF-1",
            senderName = "Ada Okafor",
            status = OrderStatus.PENDING_VERIFICATION
        )
        viewModel.repository.createOrder(pendingOrder)
        repeat(15) {
            if (viewModel.allOrders.value.any { it.orderId == pendingOrder.orderId }) return@repeat
            pumpLooper()
        }

        assertFalse(viewModel.hasVerifiedDeliveredPurchase(product.title))
        var pendingRejectSuccess = true
        viewModel.submitProductReview(
            productId = product.id,
            rating = 4,
            comment = "Should still be blocked before delivery"
        ) { ok, _ -> pendingRejectSuccess = ok }
        assertFalse(pendingRejectSuccess)
        assertTrue(viewModel.getDeliveredOrderedProducts().none { it.id == product.id })

        // Mark order as DELIVERED -> ratings and review views become active!
        viewModel.repository.updateOrderStatus(pendingOrder.orderId, OrderStatus.DELIVERED, "Delivered to customer")
        repeat(15) {
            if (viewModel.allOrders.value.any { it.orderId == pendingOrder.orderId && it.status == OrderStatus.DELIVERED }) return@repeat
            pumpLooper()
        }

        assertTrue(viewModel.hasVerifiedDeliveredPurchase(product.title))
        assertTrue(viewModel.getDeliveredOrderedProducts().any { it.id == product.id })

        var accepted = false
        var acceptMessage = ""
        viewModel.submitProductReview(
            productId = product.id,
            rating = 5,
            comment = "Delivered fast and authentic!"
        ) { ok, msg ->
            accepted = ok
            acceptMessage = msg
        }
        assertTrue(accepted)
        assertTrue(acceptMessage.contains("published", ignoreCase = true))
        assertTrue(viewModel.productReviews.value.any { it.productId == product.id && it.isVerifiedPurchase })
    }

    @Test
    fun mainActivityLaunchesWithoutDrawableResourceCrash() {
        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java)
        controller.setup()
        pumpLooper()
        val activity = controller.get()
        org.junit.Assert.assertNotNull(activity)
        activity.finish()
        controller.pause().stop().destroy()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }
}
