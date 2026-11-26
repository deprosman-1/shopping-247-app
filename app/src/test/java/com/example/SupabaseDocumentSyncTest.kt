package com.example

import com.example.data.model.ForwardedReceiptLog
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.StoreProfileConfig
import com.example.data.remote.SupabaseSyncService
import com.example.util.PdfInvoiceExporter
import com.example.util.WordInvoiceExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SupabaseDocumentSyncTest {

    private val sampleProfile = StoreProfileConfig(
        storeName = "ODIYA",
        phoneNumber = "08012345678",
        whatsappNumber = "08012345678",
        supportEmail = "orders@odiya.store",
        storeAddress = "Lagos, Nigeria",
        opayAccountNumber = "8100001111",
        opayAccountName = "ODIYA OPAY",
        moniepointAccountNumber = "5200002222",
        moniepointAccountName = "ODIYA MONIEPOINT",
        palmpayAccountNumber = "9100003333",
        palmpayAccountName = "ODIYA PALMPAY",
        paystackPublicKey = "pk_test_safe_public_key_123",
        paystackCallbackUrl = "depros://paystack-callback",
        paystackEdgeFunctionBaseUrl = "https://demo.supabase.co/functions/v1",
        updatedAt = 1760000000000L
    )

    private val sampleOrder = Order(
        orderId = "ORD-SYNC-901",
        customerId = "cust_901",
        customerName = "Ada Okafor",
        customerEmail = "ada@example.com",
        customerPhone = "08098765432",
        deliveryAddress = "24 Marina Road, Lagos",
        itemsSummary = "2x Silk Gown - ₦50,000",
        totalAmount = 50000.0,
        profit = 8000.0,
        paymentMethod = PaymentMethod.OPAY,
        paymentAccount = "8100001111 (ODIYA OPAY)",
        paymentReference = "OPY-REF-901",
        senderName = "Ada Okafor",
        status = OrderStatus.PENDING_VERIFICATION,
        createdAt = 1760000005000L
    )

    @Test
    fun wordAndPdfDocumentBytesGenerateValidReceiptPayloads() {
        val wordBytes = WordInvoiceExporter.buildWordDocumentBytes(sampleOrder, sampleProfile)
        val wordText = String(wordBytes, Charsets.UTF_8)
        assertTrue(wordBytes.isNotEmpty())
        assertTrue(wordText.contains("ORD-SYNC-901"))
        assertTrue(wordText.contains("OPY-REF-901"))
        assertTrue(wordText.contains("8100001111"))

        val pdfBytes = PdfInvoiceExporter.buildPdfDocumentBytes(sampleOrder, sampleProfile)
        val pdfHeader = String(pdfBytes.copyOfRange(0, minOf(pdfBytes.size, 8)), Charsets.US_ASCII)
        assertTrue(pdfBytes.size > 100)
        assertTrue(pdfHeader.startsWith("%PDF-"))
    }

    @Test
    fun storeProfileJsonRoundTripsAllMerchantAccountsAndBlocksSecretKeys() {
        val sync = SupabaseSyncService()

        for (isSnakeCase in listOf(true, false)) {
            val json = sync.storeProfileToJson(sampleProfile, isSnakeCase = isSnakeCase)
            val parsed = sync.parseStoreProfileJson(json, isSnakeCase = isSnakeCase)
            assertEquals("8100001111", parsed.opayAccountNumber)
            assertEquals("5200002222", parsed.moniepointAccountNumber)
            assertEquals("9100003333", parsed.palmpayAccountNumber)
            assertEquals("pk_test_safe_public_key_123", parsed.paystackPublicKey)
            assertEquals("https://demo.supabase.co/functions/v1", parsed.paystackEdgeFunctionBaseUrl)
            assertEquals(1760000000000L, parsed.updatedAt)
        }

        val secretProfile = sampleProfile.copy(paystackPublicKey = "sk_live_never_sync_secret")
        val sanitizedJson = sync.storeProfileToJson(secretProfile, isSnakeCase = true)
        assertEquals("", sanitizedJson.optString("paystack_public_key"))
        assertFalse(sanitizedJson.toString().contains("sk_live_never_sync_secret"))
    }

    @Test
    fun forwardedReceiptJsonRoundTripsCloudDocumentUrlAndReference() {
        val sync = SupabaseSyncService()
        val entry = ForwardedReceiptLog(
            id = "FWD12345",
            orderId = "ORD-SYNC-901",
            customerId = "cust_901",
            customerName = "Ada Okafor",
            customerPhone = "08098765432",
            portalTitle = "OPay Wallet / Bank App",
            format = "WORD",
            amount = 50000.0,
            accountLabel = "8100001111 (ODIYA OPAY)",
            paymentReference = "OPY-REF-901",
            cloudDocumentUrl = "https://demo.supabase.co/storage/v1/object/public/shop-receipts/forwarded_docs/ORD-SYNC-901_doc_1760000005000.doc",
            forwardedAt = 1760000005000L
        )

        for (isSnakeCase in listOf(true, false)) {
            val json = sync.forwardedReceiptToJson(entry, isSnakeCase = isSnakeCase)
            val parsed = sync.parseForwardedReceiptJson(json, isSnakeCase = isSnakeCase)
            assertNotNull(parsed)
            assertEquals(entry.id, parsed?.id)
            assertEquals(entry.orderId, parsed?.orderId)
            assertEquals(entry.format, parsed?.format)
            assertEquals(entry.paymentReference, parsed?.paymentReference)
            assertEquals(entry.cloudDocumentUrl, parsed?.cloudDocumentUrl)
            assertEquals(entry.amount, parsed?.amount ?: 0.0, 0.01)
        }
    }

    @Test
    fun supabaseSchemaSqlIncludesStoreProfileAndForwardedReceiptsTables() {
        val sql = SupabaseSyncService().generateSupabaseSqlSchema()
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS public.store_profile"))
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS public.forwarded_receipts"))
        assertTrue(sql.contains("cloud_document_url TEXT"))
        assertTrue(sql.contains("StoreProfile Read & Sync Access"))
        assertTrue(sql.contains("ForwardedReceipts Read & Sync Access"))
    }
}
