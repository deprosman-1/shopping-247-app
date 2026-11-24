package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.util.SecurityUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MainViewModelStartupTest {

    @Test
    fun mainViewModelInitializesWithoutNullStateFlowCrash() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(app)

        // Pump main looper so StateFlow collectors inside MainViewModel.init execute
        repeat(5) {
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            delay(50)
        }

        assertNotNull(viewModel.allProducts.value)
        assertNotNull(viewModel.backInStockProductIds.value)
        assertNotNull(viewModel.offlineQueue.value)
        assertNotNull(viewModel.lastSyncTimestamp.value)
        assertNotNull(viewModel.loyaltyAccount.value)
    }

    @Test
    fun securityUtilsHashesAndVerifiesSaltedPasswords() {
        val hash1 = SecurityUtils.hashPassword("secret123")
        val hash2 = SecurityUtils.hashPassword("secret123")
        assertTrue(hash1.startsWith("sha256$"))
        assertTrue(hash1 != hash2)
        assertTrue(SecurityUtils.verifyPassword("secret123", hash1))
        assertFalse(SecurityUtils.verifyPassword("wrong", hash1))
    }
}
