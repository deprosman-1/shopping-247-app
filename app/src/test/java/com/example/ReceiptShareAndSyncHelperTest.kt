package com.example

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.PaymentMethod
import com.example.util.SharedReceiptHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReceiptShareAndSyncHelperTest {

    @Test
    fun parseReceiptShareText_extractsOPayDetailsCleanly() {
        val text = """
            OPay Transaction Receipt
            Amount: ₦45,000.00
            Sender Name: Adewale Johnson
            Session ID: OPAY-20261121-984312
            Status: Successful
        """.trimIndent()

        val parsed = SharedReceiptHelper.parseReceiptShareText(text, "OPay Receipt")
        assertEquals(PaymentMethod.OPAY, parsed.detectedPaymentMethod)
        assertEquals("OPAY-20261121-984312", parsed.extractedReference)
        assertEquals("Adewale Johnson", parsed.extractedSender)
        assertEquals("OPay", parsed.sourceAppLabel)
    }

    @Test
    fun parseReceiptShareText_extractsMoniepointDetailsCleanly() {
        val text = """
            Moniepoint MFB Payment Advice
            Transaction Reference: MP-STAN-77889900
            Payer: Chinedu Okafor
            Transfer Successful
        """.trimIndent()

        val parsed = SharedReceiptHelper.parseReceiptShareText(text)
        assertEquals(PaymentMethod.MONIEPOINT, parsed.detectedPaymentMethod)
        assertEquals("MP-STAN-77889900", parsed.extractedReference)
        assertEquals("Chinedu Okafor", parsed.extractedSender)
        assertEquals("Moniepoint", parsed.sourceAppLabel)
    }

    @Test
    fun extractStoreSyncCredentials_parsesWhatsAppInviteAndDeepLink() {
        val whatsappInvite = """
            🛍️ Welcome to ODIYA!
            📲 Direct App Connect Link:
            depros://connect?sb_url=https%3A%2F%2Fxyzstore.supabase.co&sb_key=sb_publishable_TestKey1234567890
            💡 Copy this message & paste it in the app!
        """.trimIndent()

        val extracted = SharedReceiptHelper.extractStoreSyncCredentials(whatsappInvite)
        assertNotNull(extracted)
        assertEquals("https://xyzstore.supabase.co", extracted?.supabaseUrl)
        assertEquals("sb_publishable_TestKey1234567890", extracted?.supabaseKey)
    }

    @Test
    fun extractStoreSyncCredentials_parsesPlainSupabaseUrlAndJwtKeyInInvite() {
        val plainInvite = """
            Store Cloud Sync Credentials:
            Project URL: https://shop247live.supabase.co
            Anon Key: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJyb2xlIjoiYW5vbiJ9.signatureToken12345
        """.trimIndent()

        val extracted = SharedReceiptHelper.extractStoreSyncCredentials(plainInvite)
        assertNotNull(extracted)
        assertEquals("https://shop247live.supabase.co", extracted?.supabaseUrl)
        assertTrue(extracted?.supabaseKey?.startsWith("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.") == true)

        assertNull(SharedReceiptHelper.extractStoreSyncCredentials("Hello friend how are you"))
    }

    @Test
    fun persistSharedReceiptToAppStorage_copiesImageToPersistentInternalFile() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val sourceTemp = File(app.cacheDir, "opay_temp_invoice.jpg")
        sourceTemp.writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 1, 2, 3, 4))

        val savedUri = SharedReceiptHelper.persistSharedReceiptToAppStorage(
            context = app,
            sourceUri = Uri.fromFile(sourceTemp),
            mimeTypeHint = "image/jpeg"
        )
        assertNotNull(savedUri)
        val savedPath = savedUri?.path
        assertNotNull(savedPath)
        val savedFile = File(savedPath!!)
        assertTrue(savedFile.exists())
        assertTrue(savedFile.parentFile?.name == "shared_receipts")
        assertEquals(8L, savedFile.length())
    }
}
