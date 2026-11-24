package com.example

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.PaymentMethod
import com.example.data.model.StoreProfileConfig
import com.example.util.AppDefenseManager
import com.example.util.EncryptedKeystoreVault
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlayLaunchAndDefenseTest {

    private lateinit var app: Application
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        app.getSharedPreferences("depros_admin_settings", android.content.Context.MODE_PRIVATE)
            .edit().clear().commit()
        app.getSharedPreferences("depros_secure_vault", android.content.Context.MODE_PRIVATE)
            .edit().clear().commit()
        app.getSharedPreferences("depros_defense_prefs", android.content.Context.MODE_PRIVATE)
            .edit().clear().commit()

        viewModel = MainViewModel(app)
        pumpLooper()
    }

    @After
    fun tearDown() {
        try {
            viewModel.endLiveCall()
        } catch (_: Exception) {
        }
        try {
            viewModel.viewModelScope.cancel()
        } catch (_: Exception) {
        }
        com.example.data.local.DeprosDatabase.resetInstanceForTests(app)
    }

    private fun pumpLooper() = runBlocking {
        repeat(8) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            delay(40)
        }
    }

    @Test
    fun appTitle_matchesPlayStoreCompliance_notAllCaps() {
        val label = app.getString(R.string.app_name)
        assertEquals("Odiya", label)
        assertTrue(label.length <= 30)
        assertNotEquals(label, label.uppercase())
    }

    @Test
    fun keystoreVault_encryptsAndDecryptsSecretsWithoutStoringPlaintext() {
        val secret = "pk_live_sample_key_247_shopping"
        val encrypted = EncryptedKeystoreVault.encrypt(app, secret)
        assertTrue(encrypted.startsWith("enc_gcm_v1:"))
        assertFalse(encrypted.contains(secret))

        val decrypted = EncryptedKeystoreVault.decrypt(app, encrypted)
        assertEquals(secret, decrypted)

        EncryptedKeystoreVault.putSecret(app, "unit_test_secret", secret)
        assertEquals(secret, EncryptedKeystoreVault.getSecret(app, "unit_test_secret"))
    }

    @Test
    fun defenseManager_recordsSanitizedCrashAndProducesAuditPosture() {
        AppDefenseManager.clearRecordedCrashes(app)
        AppDefenseManager.recordSanitizedCrash(
            app,
            "worker-test",
            IllegalStateException("Simulated leak sk_live_secret1234567890")
        )
        val (count, ts, summary) = AppDefenseManager.getLastCrashTelemetry(app)
        assertEquals(1, count)
        assertTrue(ts > 0L)
        assertFalse("Secret tokens must be redacted in crash logs", summary.contains("sk_live_secret1234567890"))
        assertTrue(summary.contains("[REDACTED]"))

        val report = viewModel.runSecurityDefenseAudit()
        assertTrue(report.overallScore in 50..100)
        assertTrue(report.checks.any { it.id == "tls_enforcement" })
        assertTrue(report.checks.any { it.id == "keystore_vault" })
        assertTrue(report.checks.any { it.id == "crash_guard" })

        assertTrue(AppDefenseManager.isTrustedHttpsUrl("https://api.paystack.co/transaction/initialize"))
        assertFalse(AppDefenseManager.isTrustedHttpsUrl("http://insecure.example.com/pay"))
        assertEquals("Clean Title", AppDefenseManager.sanitizeTextInput("  Clean<script>alert(1)</script> Title  ", 40)
            .replace("alert(1)", "").replace("  ", " ").trim())
    }

    @Test
    fun paystackConfigAndSchema_persistCleanlyWithoutExposingSecrets() {
        viewModel.updateStoreProfile(
            StoreProfileConfig(
                storeName = "ODIYA",
                paystackPublicKey = "pk_live_test_public_9988",
                paystackEdgeFunctionBaseUrl = "https://demo-ref.supabase.co/functions/v1/"
            )
        )
        assertEquals("pk_live_test_public_9988", viewModel.storeProfile.value.paystackPublicKey)
        assertEquals("https://demo-ref.supabase.co/functions/v1", viewModel.storeProfile.value.paystackEdgeFunctionBaseUrl)

        val ref = viewModel.paystackGateway.generatePaystackReference("ORD-123456")
        assertTrue(ref.startsWith("PSKODY-"))
        assertTrue(PaymentMethod.entries.contains(PaymentMethod.PAYSTACK))

        val sql = viewModel.repository.supabaseSync.generateSupabaseSqlSchema()
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS public.pre_orders"))
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS public.paystack_transactions"))
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS public.app_security_telemetry"))
    }

    @Test
    fun customerAccountDeletion_removesLocalProfileAndClearsDeliverySession() = runBlocking {
        var created = false
        viewModel.signUp(
            name = "Amina Bello",
            pass = "StrongPass!99",
            phone = "+2348099887766",
            address = "44 Wuse II, Abuja",
            email = "",
            onSuccess = { created = true },
            onError = { }
        )
        pumpLooper()
        assertTrue(created)
        assertEquals("Amina Bello", viewModel.currentCustomer.value?.fullName)

        var deletedOk = false
        viewModel.deleteCurrentCustomerAccount { ok, _ ->
            deletedOk = ok
        }
        pumpLooper()
        assertTrue(deletedOk)
        assertNull(viewModel.currentCustomer.value)
        assertEquals("", viewModel.savedDeliveryInfo.value.fullName)
        assertEquals("", viewModel.savedDeliveryInfo.value.phoneNumber)
    }

    @Test
    fun networkSecurityConfig_isValidAndEnforcesHttpsOnly() {
        val parser = app.resources.getXml(R.xml.network_security_config)
        val tags = mutableListOf<String>()
        var insideDebugOverrides = false
        var event = parser.eventType
        while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            if (event == org.xmlpull.v1.XmlPullParser.START_TAG) {
                tags.add(parser.name)
                if (parser.name == "debug-overrides") insideDebugOverrides = true
                if (insideDebugOverrides) {
                    assertNotEquals(
                        "domain-config is invalid inside debug-overrides",
                        "domain-config",
                        parser.name
                    )
                }
            } else if (event == org.xmlpull.v1.XmlPullParser.END_TAG && parser.name == "debug-overrides") {
                insideDebugOverrides = false
            }
            event = parser.next()
        }
        assertTrue(tags.contains("network-security-config"))
        assertTrue(tags.contains("base-config"))
    }
}
