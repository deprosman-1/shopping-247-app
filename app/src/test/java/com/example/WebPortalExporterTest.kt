package com.example

import com.example.util.WebPortalExporter
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WebPortalExporterTest {

    @Test
    fun normalizePortalUrlConvertsGitHubRepoDashboardToPagesUrl() {
        assertEquals(
            "https://deprosman-1.github.io/shopping-247-app/",
            WebPortalExporter.normalizePortalUrl("https://github.com/deprosman-1/shopping-247-app")
        )
        assertEquals(
            "https://deprosman-1.github.io/shopping-247-app/",
            WebPortalExporter.normalizePortalUrl("https://github.com/deprosman-1/shopping-247-app/tree/main")
        )
        assertEquals(
            "https://deprosman-1.github.io/shopping-247-app/",
            WebPortalExporter.normalizePortalUrl("https://github.com/deprosman-1/shopping-247-app.git")
        )
    }

    @Test
    fun normalizePortalUrlStripsSyncParamsAndPreviewHosts() {
        assertEquals(
            "https://deprosman-1.github.io/shopping-247-app/",
            WebPortalExporter.normalizePortalUrl(
                "https://deprosman-1.github.io/shopping-247-app/?open_app=1&sb_url=https%3A%2F%2Ffoo.supabase.co&sb_key=abc"
            )
        )
        assertEquals(
            "",
            WebPortalExporter.normalizePortalUrl(
                "https://ais-pre-gjs6srxqc2prdx42l343un-645163211794.europe-west2.run.app/"
            )
        )
    }

    @Test
    fun normalizeApkOrPortalUrlPreservesDirectApkReleaseAndConvertsDashboard() {
        val apkRelease = "https://github.com/deprosman-1/shopping-247-app/releases/download/v1.0-latest/247-shopping.apk"
        assertEquals(apkRelease, WebPortalExporter.normalizeApkOrPortalUrl(apkRelease))
        assertEquals(
            "https://deprosman-1.github.io/shopping-247-app/",
            WebPortalExporter.normalizeApkOrPortalUrl("https://github.com/deprosman-1/shopping-247-app")
        )
    }

    @Test
    fun buildStorePortalHtmlIncludesCrossPlatformIosWebFeaturesAndNoSecrets() {
        val html = WebPortalExporter.buildStorePortalHtml(
            sbUrl = "https://example.supabase.co",
            sbKey = "sb_publishable_demo_key_123",
            storeName = "ODIYA"
        )
        org.junit.Assert.assertTrue(html.contains("viewport-fit=cover"))
        org.junit.Assert.assertTrue(html.contains("apple-mobile-web-app-capable"))
        org.junit.Assert.assertTrue(html.contains("safe-area-inset-top"))
        org.junit.Assert.assertTrue(html.contains("offlineBanner"))
        org.junit.Assert.assertTrue(html.contains("/rest/v1/products"))
        org.junit.Assert.assertTrue(html.contains("/rest/v1/orders"))
        org.junit.Assert.assertTrue(html.contains("/rest/v1/pre_orders"))
        org.junit.Assert.assertTrue(html.contains("/functions/v1/paystack-init"))
        org.junit.Assert.assertTrue(html.contains("/functions/v1/paystack-verify"))
        org.junit.Assert.assertTrue(html.contains("if (navigator.clipboard && navigator.clipboard.writeText) {"))
        org.junit.Assert.assertFalse(html.contains("sk_live_"))
        org.junit.Assert.assertFalse(html.contains("PAYSTACK_SECRET_KEY"))
    }
}
