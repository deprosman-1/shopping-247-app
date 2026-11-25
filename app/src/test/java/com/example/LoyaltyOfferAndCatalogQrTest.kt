package com.example

import android.app.Application
import android.os.Looper
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CartItem
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.ProductColor
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
class LoyaltyOfferAndCatalogQrTest {

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
            delay(35)
        }
    }

    @Test
    fun regularCustomerLoyaltyOffer_activatesAndDeductsOnFifthPatronageWithin60Days() = runBlocking {
        val blackColor = ProductColor("Black", "#000000")
        val shoeProduct = Product(
            id = "prod-shoe-1",
            title = "Odiya Luxury Runner",
            category = ProductCategory.SHOES.displayName,
            brand = "ODIYA",
            availableSizes = listOf("42"),
            availableColors = listOf(blackColor),
            basePrice = 110_000.0,
            costPrice = 65_000.0,
            stockQuantity = 15,
            imageUrl = "",
            description = "Premium sneaker"
        )
        val phone = "08012345678"
        val now = System.currentTimeMillis()
        viewModel.repository.saveProduct(shoeProduct)
        pumpLooper()

        // Admin configures and activates 5-visit / 60-day / ₦100,000 loyalty offer scoped to Shoes
        viewModel.updateAdminLoyaltyOfferConfig(
            isActive = true,
            title = "Regular Buyer ₦2,500 Reward",
            designatedCategory = ProductCategory.SHOES.displayName,
            minPatronages = 5,
            windowDays = 60,
            minRecurrentCheckoutAmount = 100_000.0,
            rewardPerPatronage = 500.0,
            claimableTotalReward = 2_500.0
        )

        // Create 4 qualifying prior orders (>= ₦100,000, within 60 days, matching category)
        val priorIds = (1..4).map { idx ->
            val ordId = "ORD-PRIOR-$idx"
            viewModel.repository.createOrder(
                Order(
                    orderId = ordId,
                    customerId = "cust-loyalty-1",
                    customerName = "Amina Bello",
                    customerEmail = "",
                    customerPhone = phone,
                    deliveryAddress = "14 Admiralty Way, Lekki",
                    itemsSummary = "1x Odiya Luxury Runner (ODIYA, 42, Black) - ₦110000",
                    totalAmount = 110_000.0,
                    paymentMethod = PaymentMethod.OPAY,
                    paymentAccount = "OPay 9012345678",
                    paymentReference = "REF-$idx",
                    senderName = "Amina Bello",
                    status = OrderStatus.CONFIRMED,
                    createdAt = now - (idx * 7L * 86_400_000L)
                )
            )
            ordId
        }
        pumpLooper()

        val cartItems = listOf(
            CartItem(
                product = shoeProduct,
                selectedBrand = "ODIYA",
                selectedSize = "42",
                selectedColor = blackColor,
                quantity = 1,
                unitPrice = 110_000.0
            )
        )

        // Evaluate 5th checkout -> Claim Box MUST activate for ₦2,500
        val status = viewModel.evaluateCustomerLoyaltyOffer(
            customerPhone = phone,
            customerName = "Amina Bello",
            cartItems = cartItems,
            currentCheckoutSubtotalBeforeLoyalty = 110_000.0,
            nowMillis = now
        )
        assertTrue(status.isClaimBoxActivated)
        assertEquals(2_500.0, status.claimableAmount, 0.01)
        assertEquals(4, status.qualifyingPriorVisitsInWindow)
        assertEquals(priorIds.sorted(), status.qualifyingPriorOrderIds.sorted())

        // Wrong category should NOT activate
        val electronicsCart = listOf(
            cartItems.first().copy(
                product = shoeProduct.copy(id = "prod-elec-1", title = "Smart Earbuds", category = ProductCategory.ELECTRONICS.displayName)
            )
        )
        val wrongCatStatus = viewModel.evaluateCustomerLoyaltyOffer(
            customerPhone = phone,
            customerName = "Amina Bello",
            cartItems = electronicsCart,
            currentCheckoutSubtotalBeforeLoyalty = 110_000.0,
            nowMillis = now
        )
        assertFalse(wrongCatStatus.isClaimBoxActivated)

        // Add shoe product to cart and place 5th order claiming the ₦2,500 reward
        viewModel.addToCart(shoeProduct) { _, _ -> }
        var createdOrder: Order? = null
        viewModel.placeOrder(
            customerName = "Amina Bello",
            customerEmail = "",
            customerPhone = phone,
            deliveryAddress = "14 Admiralty Way, Lekki",
            paymentMethod = PaymentMethod.OPAY,
            paymentAccountOrTerminal = "OPay 9012345678",
            paymentReference = "REF-5",
            senderName = "Amina Bello",
            deliveryConditionTitle = "Standard Delivery",
            deliveryFeeAmount = 0.0,
            loyaltyDiscountAmount = 2_500.0,
            specialLoyaltyOfferClaimed = true,
            specialLoyaltyPriorOrderIds = status.qualifyingPriorOrderIds,
            onSuccess = { createdOrder = it },
            onFailure = {}
        )
        pumpLooper()

        assertEquals(107_500.0, createdOrder?.totalAmount ?: 0.0, 0.01)

        // After claiming, those 4 prior orders + the 5th order are consumed so 6th checkout resets
        val afterClaimStatus = viewModel.evaluateCustomerLoyaltyOffer(
            customerPhone = phone,
            customerName = "Amina Bello",
            cartItems = cartItems,
            currentCheckoutSubtotalBeforeLoyalty = 110_000.0,
            nowMillis = now + 10_000L
        )
        assertFalse(afterClaimStatus.isClaimBoxActivated)
        assertEquals(0, afterClaimStatus.qualifyingPriorVisitsInWindow)
    }

    @Test
    fun catalogShareLink_and_customerPreOrderDestination_workProperly() {
        val link = viewModel.buildCatalogShareLink(
            baseShareUrl = "https://deprosman-1.github.io/shopping-247-app/",
            category = "Shoes",
            query = "Air Max"
        )
        assertTrue(link.contains("open_app=1"))
        assertTrue(link.contains("cat=Shoes"))
        assertTrue(link.contains("q=Air+Max"))

        viewModel.openCustomerAccountShortcut(CustomerAccountDestination.PRE_ORDER)
        assertEquals(AppScreen.AUTH, viewModel.currentScreen.value)
        assertEquals(CustomerAccountDestination.PRE_ORDER, viewModel.customerAccountDestination.value)
    }
}
