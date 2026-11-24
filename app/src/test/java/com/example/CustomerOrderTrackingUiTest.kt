package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.StatusUpdate
import com.example.ui.screens.CustomerOrderTrackingContent
import com.example.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w480dp-h2400dp")
class CustomerOrderTrackingUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun orderTrackingContent_rendersTimelineAndFiltersCorrectly() {
        val now = System.currentTimeMillis()
        val sampleOrder = Order(
            orderId = "ORD-UI-101",
            customerId = "cust-ui",
            customerName = "Tunde Bakare",
            customerEmail = "tunde@example.com",
            customerPhone = "08033344455",
            deliveryAddress = "18 Allen Avenue, Ikeja, Lagos",
            itemsSummary = "1x Classic Watch",
            totalAmount = 42000.0,
            paymentMethod = PaymentMethod.MONIEPOINT,
            paymentAccount = "Moniepoint ODIYA",
            paymentReference = "MON-101",
            senderName = "Tunde Bakare",
            status = OrderStatus.OUT_FOR_DELIVERY,
            createdAt = now - 1_000_000L,
            verificationNote = "Rider dispatched to Ikeja",
            dispatchRiderWhatsapp = "+2348022233344",
            statusUpdates = listOf(
                StatusUpdate(OrderStatus.PENDING_VERIFICATION, now - 1_000_000L, "Order received"),
                StatusUpdate(OrderStatus.OUT_FOR_DELIVERY, now - 120_000L, "Rider dispatched to Ikeja")
            )
        )

        var state by mutableStateOf(
            OrderTrackingUiState(
                isLiveSynced = true,
                lastSyncedAtMillis = now,
                statusBanner = "Synced 1 order(s) live from Supabase 'orders' table."
            )
        )

        composeRule.setContent {
            MyApplicationTheme {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    CustomerOrderTrackingContent(
                        trackingState = state,
                        scopedOrders = listOf(sampleOrder),
                        isSupabaseConnected = true,
                        onQueryChange = { state = state.copy(query = it) },
                        onSearch = {},
                        onClearSearch = { state = state.copy(query = "", hasActiveSearch = false) },
                        onSelectFilter = { state = state.copy(selectedFilter = it) },
                        onRefreshAll = {},
                        onRefreshOrder = {},
                        onViewReceipt = {},
                        onBrowseStore = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("account_order_tracking_page").assertIsDisplayed()
        composeRule.onNodeWithTag("order_tracking_hero_card").assertIsDisplayed()
        composeRule.onNodeWithTag("tracked_order_card_ORD-UI-101").assertIsDisplayed()
        composeRule.onNodeWithText("Dispatch Rider Assigned").assertIsDisplayed()

        // Filter to Delivered -> card hides and empty state appears
        composeRule.onNodeWithTag("order_tracking_filter_delivered").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("order_tracking_empty_card").assertIsDisplayed()

        // Filter to In Progress -> card returns
        composeRule.onNodeWithTag("order_tracking_filter_active").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tracked_order_card_ORD-UI-101").assertIsDisplayed()
    }
}
