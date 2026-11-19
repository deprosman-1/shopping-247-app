package com.example

import com.example.util.WebPortalExporter
import org.junit.Assert.assertEquals
import org.junit.Test

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
}
