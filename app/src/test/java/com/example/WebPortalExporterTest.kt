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
        org.junit.Assert.assertTrue(html.contains("mirrorPreOrderToStorage"))
        org.junit.Assert.assertTrue(html.contains("androidIntentLink"))
        org.junit.Assert.assertTrue(html.contains("Odiya_Official.apk"))
        org.junit.Assert.assertTrue(html.replace("\\/", "/").contains(WebPortalExporter.DEFAULT_GITHUB_APK_RELEASE_URL))
        org.junit.Assert.assertFalse(html.contains("sk_live_"))
        org.junit.Assert.assertFalse(html.contains("PAYSTACK_SECRET_KEY"))
        org.junit.Assert.assertFalse(html.contains("coEmail"))
        org.junit.Assert.assertFalse(html.contains("Email Address (for Receipt)"))
        org.junit.Assert.assertFalse(html.contains("odiya_customer_email"))
        org.junit.Assert.assertTrue(html.contains("customerCredentialsPanel"))
        org.junit.Assert.assertTrue(html.contains("applyPermanentCustomerCredentials"))
        org.junit.Assert.assertTrue(html.contains("data-tab=\"wishlist\""))
        org.junit.Assert.assertTrue(html.contains("id=\"panel-wishlist\""))
        org.junit.Assert.assertTrue(html.contains("odiya_web_wishlist"))
        org.junit.Assert.assertTrue(html.contains("toggleWishlistProduct"))
        org.junit.Assert.assertTrue(html.contains("Add to Wishlist"))
        org.junit.Assert.assertTrue(html.contains("id=\"catalogQrCard\""))
        org.junit.Assert.assertTrue(html.contains("id=\"catalogQrCanvas\""))
        org.junit.Assert.assertTrue(html.contains("function generateQrMatrix(text)"))
        org.junit.Assert.assertTrue(html.contains("function updateDynamicCatalogQr()"))
        org.junit.Assert.assertTrue(html.contains("function buildAppDeepLinks(catFilter, queryFilter)"))
        org.junit.Assert.assertTrue(html.contains("function launchAndroidApkDownload()"))
        org.junit.Assert.assertTrue(html.contains("urlCatParam"))
        org.junit.Assert.assertTrue(html.contains("urlSearchParam"))
        org.junit.Assert.assertTrue(html.contains("encrypted &amp; well secured"))
        val htmlBodyOnly = html.substringBefore("<script>")
        org.junit.Assert.assertFalse(htmlBodyOnly.contains("Supabase", ignoreCase = true))
    }

    @Test
    fun withStayOnWebParamStripsOpenAppAndAppendsWebFlagOnlyOnPortals() {
        val portal = WebPortalExporter.withStayOnWebParam(
            "https://deprosman-1.github.io/shopping-247-app/?open_app=1&sb_url=https%3A%2F%2Ffoo.supabase.co&sb_key=abc"
        )
        org.junit.Assert.assertTrue(portal.contains("web=1"))
        org.junit.Assert.assertTrue(portal.contains("sb_url="))
        org.junit.Assert.assertFalse(portal.contains("open_app="))

        val apk = WebPortalExporter.DEFAULT_GITHUB_APK_RELEASE_URL
        assertEquals(apk, WebPortalExporter.withStayOnWebParam(apk))
    }

    @Test
    fun qrCodeBitmapGenerationProducesCrispBitmapsForShortAndLongUrls() {
        org.junit.Assert.assertNull(com.example.ui.components.generateQrBitmap("   "))

        val shortQr = com.example.ui.components.generateQrBitmap("depros://open", 720)
        org.junit.Assert.assertNotNull(shortQr)
        org.junit.Assert.assertTrue((shortQr?.width ?: 0) >= 240)
        assertEquals(shortQr?.width, shortQr?.height)

        val longSyncUrl = "https://deprosman-1.github.io/shopping-247-app/?open_app=1&sb_url=https%3A%2F%2Fexampleproject.supabase.co&sb_key=sb_publishable_abcdefghijklmnopqrstuvwxyz0123456789"
        val longQr = com.example.ui.components.generateQrBitmap(longSyncUrl, 720)
        org.junit.Assert.assertNotNull(longQr)
        org.junit.Assert.assertTrue((longQr?.width ?: 0) >= 240)

        val poster = com.example.ui.components.generateShareableQrPosterBitmap(longSyncUrl, "ODIYA")
        org.junit.Assert.assertNotNull(poster)
        assertEquals(960, poster?.width)
        assertEquals(1160, poster?.height)
    }
}
