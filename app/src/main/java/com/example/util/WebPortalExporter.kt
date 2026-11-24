package com.example.util

import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File

object WebPortalExporter {
    private const val PREFS_NAME = "smart_shop_share_prefs"
    private const val KEY_SHARE_MODE = "share_link_mode"
    private const val KEY_CUSTOM_PORTAL_URL = "custom_share_url"
    private const val APP_PACKAGE = "com.aistudio.deprosboutique.kqwmxp"

    const val DEFAULT_LIVE_PORTAL_URL = "https://deprosman-1.github.io/shopping-247-app/"
    const val DEFAULT_GITHUB_APK_RELEASE_URL = "https://github.com/deprosman-1/shopping-247-app/releases/download/v1.0-latest/247-shopping.apk"

    fun normalizeWebUrl(raw: String): String {
        val cleaned = raw.trim().replace("\u200B", "")
        if (cleaned.isBlank() ||
            cleaned.length > 500 ||
            cleaned.contains("<") ||
            cleaned.contains(">") ||
            cleaned.contains("\n") ||
            cleaned.contains("\r") ||
            cleaned.contains("<!DOCTYPE", ignoreCase = true) ||
            cleaned.contains("<html", ignoreCase = true)
        ) {
            return ""
        }
        val extracted = Regex("""https?://[^\s<>"']+""", RegexOption.IGNORE_CASE).find(cleaned)?.value
        val withScheme = if (!extracted.isNullOrBlank()) {
            extracted.trimEnd('.', ',', ';', ')', ']')
        } else {
            "https://${cleaned.replace(" ", "")}"
        }
        if (withScheme.contains("<") || withScheme.contains(">")) return ""
        // Auto-repair accidental paste typo where 'https' or 'http' got glued onto the end of the domain (e.g. .github.iohttps)
        return withScheme.replace(
            Regex("""(\.(?:github\.io|github\.com|pages\.dev|vercel\.app|com|org|net|io|app|site))https?$""", RegexOption.IGNORE_CASE),
            "$1"
        )
    }

    fun isAiStudioPreviewHost(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("ais-pre-gjs6srxqc2prdx42l343un") ||
            lower.contains("ais-dev-gjs6srxqc2prdx42l343un")
    }

    fun isLegacyDeprecatedHost(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("netlify.app") ||
            lower.contains("netlify.com") ||
            lower.contains("tiiny.site") ||
            lower.contains("tiiny.host")
    }

    fun isDirectApkReleaseUrl(url: String): Boolean {
        val lower = url.trim().lowercase()
        return lower.endsWith(".apk") || lower.contains("/releases/download/")
    }

    fun suggestGitHubPagesUrlIfRepoDashboard(url: String): String? {
        val cleaned = url.trim()
        val match = Regex("""^https?://(?:www\.)?github\.com/([^/\s?#]+)/([^/\s?#]+)(?:[/?#].*)?$""", RegexOption.IGNORE_CASE)
            .find(cleaned) ?: return null
        val owner = match.groupValues[1]
        val repo = match.groupValues[2].removeSuffix(".git")
        val lowerUrl = cleaned.lowercase()
        val reservedOwners = setOf("new", "settings", "login", "signup", "features", "marketplace", "explore", "topics", "orgs", "apps")
        if (owner.lowercase() in reservedOwners ||
            lowerUrl.contains("/releases/download/") ||
            lowerUrl.endsWith(".apk")
        ) {
            return null
        }
        return "https://${owner.lowercase()}.github.io/${repo}/"
    }

    private fun stripSyncQueryParams(url: String): String {
        val qIdx = url.indexOf('?')
        if (qIdx < 0) return url
        val base = url.substring(0, qIdx)
        val query = url.substring(qIdx + 1)
        val kept = query.split("&").filter { param ->
            val key = param.substringBefore("=").lowercase()
            key.isNotBlank() && key !in setOf("sb_url", "sb_key", "open_app", "web")
        }
        return if (kept.isEmpty()) base else "$base?${kept.joinToString("&")}"
    }

    /**
     * Normalizes a storefront/share web portal URL:
     * - Repairs scheme & typos
     * - Strips accidental appended sync query params (`sb_url`, `sb_key`, `open_app`, `web`)
     * - Blocks deprecated hosts, AI Studio preview hosts, and bare github.com/new
     * - Auto-converts any GitHub repository dashboard URL (`https://github.com/USER/REPO`) into
     *   its live GitHub Pages storefront URL (`https://user.github.io/REPO/`) so WhatsApp shares
     *   never open a GitHub code dashboard.
     */
    fun normalizePortalUrl(raw: String): String {
        val normalized = normalizeWebUrl(raw)
        if (normalized.isBlank()) return ""
        val withoutSyncParams = stripSyncQueryParams(normalized)
        if (isAiStudioPreviewHost(withoutSyncParams) || isLegacyDeprecatedHost(withoutSyncParams)) {
            return ""
        }
        if (withoutSyncParams.equals("https://github.com", ignoreCase = true) ||
            withoutSyncParams.equals("https://github.com/", ignoreCase = true) ||
            withoutSyncParams.equals("https://github.com/new", ignoreCase = true)
        ) {
            return ""
        }
        val githubPages = suggestGitHubPagesUrlIfRepoDashboard(withoutSyncParams)
        if (githubPages != null) {
            return githubPages
        }
        return withoutSyncParams
    }

    /**
     * Normalizes an APK update/download URL: preserves direct `.apk` / `/releases/download/` URLs,
     * while auto-converting accidental GitHub repository dashboard URLs into GitHub Pages URLs
     * and stripping legacy/preview hosts.
     */
    fun normalizeApkOrPortalUrl(raw: String): String {
        val normalized = normalizeWebUrl(raw)
        if (normalized.isBlank()) return ""
        if (isDirectApkReleaseUrl(normalized)) return normalized
        return normalizePortalUrl(normalized)
    }

    fun getSavedCustomPortalUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_CUSTOM_PORTAL_URL, "") ?: ""
        if (raw.isBlank()) return ""
        val clean = normalizePortalUrl(raw)
        val sanitized = if (isDirectApkReleaseUrl(clean)) "" else clean
        if (sanitized != raw.trim()) {
            prefs.edit().putString(KEY_CUSTOM_PORTAL_URL, sanitized).apply()
        }
        return sanitized
    }

    fun saveCustomPortalUrl(context: Context, rawUrl: String) {
        val cleanToSave = normalizePortalUrl(rawUrl).let {
            if (isDirectApkReleaseUrl(it)) "" else it
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit().putString(KEY_CUSTOM_PORTAL_URL, cleanToSave)
        if (cleanToSave.isNotBlank()) {
            editor.putInt(KEY_SHARE_MODE, 2)
        }
        editor.apply()
    }

    fun purgeLegacyApkUrlPref(context: Context) {
        val sharePrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val shareEditor = sharePrefs.edit().remove("custom_apk_download_url")
        val savedCustom = sharePrefs.getString(KEY_CUSTOM_PORTAL_URL, "") ?: ""
        if (savedCustom.isNotBlank()) {
            val cleanedCustom = normalizePortalUrl(savedCustom).let {
                if (isDirectApkReleaseUrl(it)) "" else it
            }
            if (cleanedCustom != savedCustom) {
                if (cleanedCustom.isBlank()) {
                    shareEditor.remove(KEY_CUSTOM_PORTAL_URL)
                } else {
                    shareEditor.putString(KEY_CUSTOM_PORTAL_URL, cleanedCustom)
                }
            }
        }
        shareEditor.apply()

        val adminPrefs = context.getSharedPreferences("depros_admin_settings", Context.MODE_PRIVATE)
        val savedApk = adminPrefs.getString("apk_download_url", "") ?: ""
        if (savedApk.isNotBlank()) {
            val cleanedApk = normalizeApkOrPortalUrl(savedApk)
            if (cleanedApk != savedApk) {
                adminPrefs.edit().putString("apk_download_url", cleanedApk).apply()
            }
        }
    }

    /**
     * Appends `web=1` (and strips `open_app`) on HTTP(S) portal URLs so opening the
     * Live Storefront in an external browser (e.g., to download a new APK update or preview)
     * never bounces straight back into the same old installed Android app.
     */
    fun withStayOnWebParam(url: String): String {
        val clean = normalizeWebUrl(url)
        if (clean.isBlank()) return ""
        if (isDirectApkReleaseUrl(clean)) return clean
        val qIdx = clean.indexOf('?')
        val base = if (qIdx >= 0) clean.substring(0, qIdx) else clean
        val baseWithSlash = if (!base.contains("?") && base.count { it == '/' } == 2) "$base/" else base
        val keptParams = if (qIdx >= 0) {
            clean.substring(qIdx + 1).split("&").filter { param ->
                val key = param.substringBefore("=").lowercase()
                key.isNotBlank() && key != "open_app" && key != "web"
            }
        } else {
            emptyList()
        }
        val finalParams = keptParams + "web=1"
        return "$baseWithSlash?${finalParams.joinToString("&")}"
    }

    /**
     * Opens an HTTP/HTTPS URL strictly inside an external web browser (Chrome, Samsung Browser,
     * Firefox, Opera, Edge, etc.), explicitly excluding this app's own MainActivity so Android
     * deep-link filters NEVER hijack an APK Update or Live Storefront request back into the app.
     */
    fun openInExternalBrowserOnly(
        context: Context,
        url: String,
        forceStayOnWebIfPortal: Boolean = true
    ) {
        val rawNorm = normalizeApkOrPortalUrl(url).ifBlank { normalizeWebUrl(url) }
        if (rawNorm.isBlank()) return
        val target = if (forceStayOnWebIfPortal && !isDirectApkReleaseUrl(rawNorm)) {
            withStayOnWebParam(rawNorm)
        } else {
            rawNorm
        }
        val targetUri = Uri.parse(target)
        try {
            val probeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://deprosman-1.github.io/")).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
            }
            @Suppress("DEPRECATION")
            val browserMatches = context.packageManager.queryIntentActivities(
                probeIntent,
                PackageManager.MATCH_DEFAULT_ONLY
            ).filter { info ->
                val pkg = info.activityInfo?.packageName.orEmpty()
                pkg.isNotBlank() &&
                    pkg != context.packageName &&
                    pkg != APP_PACKAGE &&
                    pkg != "android"
            }
            val preferredPackages = listOf(
                "com.android.chrome",
                "com.sec.android.app.sbrowser",
                "com.brave.browser",
                "com.microsoft.emmx",
                "com.opera.browser",
                "com.opera.mini.native",
                "org.mozilla.firefox"
            )
            val chosenBrowserPkg = preferredPackages.firstOrNull { pref ->
                browserMatches.any { it.activityInfo?.packageName == pref }
            } ?: browserMatches.firstOrNull()?.activityInfo?.packageName

            if (!chosenBrowserPkg.isNullOrBlank()) {
                val directBrowserIntent = Intent(Intent.ACTION_VIEW, targetUri).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    `package` = chosenBrowserPkg
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(directBrowserIntent)
                return
            }

            val baseIntent = Intent(Intent.ACTION_VIEW, targetUri).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(baseIntent, "Open in Web Browser").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    putExtra(
                        Intent.EXTRA_EXCLUDE_COMPONENTS,
                        arrayOf(
                            ComponentName(context.packageName, "com.example.MainActivity"),
                            ComponentName(APP_PACKAGE, "com.example.MainActivity")
                        )
                    )
                }
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open external browser: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openExternalUrl(context: Context, url: String) {
        val trimmed = url.trim()
        if (trimmed.startsWith("depros://", ignoreCase = true) ||
            trimmed.startsWith("deprosboutique://", ignoreCase = true)
        ) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(trimmed)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open app link: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            return
        }
        openInExternalBrowserOnly(context, trimmed, forceStayOnWebIfPortal = true)
    }

    /**
     * Builds a complete, standalone, mobile-friendly HTTPS-ready `index.html` storefront & deep-link page
     * with the store's Supabase URL & Publishable Key baked directly inside.
     * Hosting this single `index.html` on GitHub Pages creates a live 200 OK web address that never returns 404.
     */
    fun buildStorePortalHtml(
        sbUrl: String,
        sbKey: String,
        storeName: String = "ODIYA",
        storePhone: String = "",
        storeAddress: String = "",
        storeEmail: String = "",
        apkDownloadUrl: String = ""
    ): String {
        val escapedSbUrl = JSONObject.quote(sbUrl.trim())
        val escapedSbKey = JSONObject.quote(sbKey.trim())
        val escapedPkg = JSONObject.quote(APP_PACKAGE)
        val rawApkNorm = normalizeWebUrl(apkDownloadUrl)
        val cleanApkUrl = if (isDirectApkReleaseUrl(rawApkNorm)) {
            rawApkNorm
        } else {
            DEFAULT_GITHUB_APK_RELEASE_URL
        }
        val escapedApkUrl = JSONObject.quote(cleanApkUrl)
        val cleanStoreName = storeName.trim().ifBlank { "ODIYA" }
        val safeHtmlName = cleanStoreName
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
        val contactParts = listOfNotNull(
            storePhone.trim().takeIf { it.isNotBlank() }?.let { "📞 $it" },
            storeAddress.trim().takeIf { it.isNotBlank() }?.let { "📍 $it" }
        ).joinToString(" • ").ifBlank { "Real-Time Cloud Storefront" }
        val safeHtmlContact = contactParts
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")

        return """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover" />
  <meta name="theme-color" content="#064E3B" />
  <meta name="apple-mobile-web-app-capable" content="yes" />
  <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent" />
  <meta name="apple-mobile-web-app-title" content="$safeHtmlName" />
  <title>$safeHtmlName — Live Storefront, Paystack Checkout, Pre-Orders & Order Tracking</title>
  <link rel="manifest" id="pwaManifestLink" href="" />
  <style>
    :root {
      --bg: #052E16;
      --card: rgba(6, 78, 59, 0.94);
      --card-soft: rgba(4, 120, 87, 0.55);
      --border: rgba(74, 222, 128, 0.42);
      --green: #16A34A;
      --accent: #22C55E;
      --amber: #F59E0B;
      --danger: #EF4444;
      --text: #FFFFFF;
      --muted: rgba(255, 255, 255, 0.82);
    }
    * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; -webkit-tap-highlight-color: transparent; }
    html, body { max-width: 100%; overflow-x: hidden; }
    img { max-width: 100%; height: auto; }
    body {
      background: radial-gradient(circle at top right, #15803D 0%, #064E3B 48%, #052E16 100%);
      color: #FFFFFF;
      min-height: 100vh;
      padding: max(14px, env(safe-area-inset-top)) max(10px, env(safe-area-inset-right)) max(96px, calc(76px + env(safe-area-inset-bottom))) max(10px, env(safe-area-inset-left));
    }
    .wrap { max-width: 960px; width: 100%; margin: 0 auto; min-width: 0; }
    .offline-banner {
      display: none;
      background: #B91C1C;
      color: #FFFFFF;
      padding: 12px 16px;
      border-radius: 14px;
      margin-bottom: 14px;
      font-weight: 800;
      font-size: 14px;
      text-align: center;
      border: 1px solid rgba(255, 255, 255, 0.35);
    }
    .offline-banner.show { display: block; }
    .hero {
      background: var(--card);
      border: 1.5px solid var(--border);
      border-radius: 22px;
      padding: 20px 16px;
      text-align: center;
      box-shadow: 0 20px 50px rgba(0, 0, 0, 0.35);
      margin-bottom: 14px;
    }
    .pill {
      display: inline-block;
      background: rgba(22, 163, 74, 0.38);
      border: 1px solid rgba(255, 255, 255, 0.45);
      color: #FFFFFF;
      font-size: 12px;
      font-weight: 800;
      padding: 5px 12px;
      border-radius: 999px;
      margin-bottom: 8px;
      word-break: break-word;
    }
    h1 { font-size: 26px; font-weight: 900; letter-spacing: 0.6px; margin-bottom: 4px; color: #FFFFFF; }
    .sub { color: var(--muted); font-size: 14px; font-weight: 600; margin-bottom: 14px; overflow-wrap: anywhere; }
    .actions {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(min(200px, 100%), 1fr));
      gap: 8px;
      margin-top: 10px;
    }
    .btn {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      min-height: 46px;
      padding: 11px 14px;
      border-radius: 13px;
      border: none;
      font-size: 14px;
      font-weight: 800;
      color: #FFFFFF;
      text-decoration: none;
      text-align: center;
      cursor: pointer;
      transition: transform 0.12s ease, opacity 0.12s ease;
      overflow-wrap: anywhere;
    }
    .btn:active { transform: scale(0.98); }
    .btn:disabled { opacity: 0.55; cursor: not-allowed; }
    .btn-cyan { background: linear-gradient(135deg, #22C55E, #15803D); }
    .btn-green { background: linear-gradient(135deg, #10B981, #047857); }
    .btn-gold { background: linear-gradient(135deg, #D97706, #B45309); }
    .btn-dark { background: rgba(255, 255, 255, 0.14); border: 1px solid rgba(255, 255, 255, 0.3); }
    .btn-wish { background: rgba(255, 255, 255, 0.10); border: 1px solid rgba(255, 255, 255, 0.28); color: #FFFFFF; }
    .btn-wish.saved { background: rgba(239, 68, 68, 0.26); border-color: #F87171; color: #FECACA; }
    .btn-sm { min-height: 38px; padding: 8px 12px; font-size: 13px; border-radius: 10px; }
    .status-note {
      margin-top: 12px;
      padding: 11px 13px;
      border-radius: 12px;
      background: rgba(22, 163, 74, 0.22);
      border: 1px solid rgba(255, 255, 255, 0.28);
      font-size: 13px;
      color: #FFFFFF;
      line-height: 1.5;
      text-align: left;
      overflow-wrap: anywhere;
    }
    .tabs {
      display: grid;
      grid-template-columns: repeat(5, minmax(0, 1fr));
      gap: 8px;
      margin-bottom: 14px;
      position: sticky;
      top: max(6px, env(safe-area-inset-top));
      z-index: 20;
      background: rgba(5, 46, 22, 0.92);
      backdrop-filter: blur(10px);
      padding: 8px;
      border-radius: 16px;
      border: 1px solid var(--border);
    }
    .tab-btn {
      min-height: 44px;
      padding: 8px 6px;
      border-radius: 11px;
      border: none;
      background: transparent;
      color: var(--muted);
      font-size: 13px;
      font-weight: 800;
      cursor: pointer;
    }
    .tab-btn.active {
      background: linear-gradient(135deg, #22C55E, #15803D);
      color: #FFFFFF;
      box-shadow: 0 4px 12px rgba(22, 163, 74, 0.4);
    }
    .panel { display: none; }
    .panel.active { display: block; }
    .card {
      background: var(--card);
      border: 1px solid rgba(255, 255, 255, 0.22);
      border-radius: 20px;
      padding: 16px;
      margin-bottom: 14px;
    }
    .card-head {
      display: flex;
      flex-wrap: wrap;
      gap: 10px;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 12px;
    }
    .card-head h2 { font-size: 18px; font-weight: 900; color: #FFFFFF; }
    .input, .select, .textarea {
      width: 100%;
      min-height: 44px;
      padding: 10px 13px;
      border-radius: 12px;
      border: 1px solid rgba(255, 255, 255, 0.32);
      background: rgba(5, 46, 22, 0.9);
      color: #FFFFFF;
      font-size: 15px;
      outline: none;
    }
    .textarea { min-height: 88px; resize: vertical; }
    .input::placeholder, .textarea::placeholder { color: rgba(255, 255, 255, 0.65); }
    .search { max-width: 340px; }
    .cats {
      display: flex;
      gap: 8px;
      overflow-x: auto;
      padding-bottom: 8px;
      margin-bottom: 12px;
      scrollbar-width: none;
    }
    .cats::-webkit-scrollbar { display: none; }
    .cat-chip {
      flex: 0 0 auto;
      padding: 8px 14px;
      border-radius: 999px;
      border: 1px solid rgba(255, 255, 255, 0.3);
      background: rgba(255, 255, 255, 0.08);
      color: #FFFFFF;
      font-size: 13px;
      font-weight: 700;
      cursor: pointer;
    }
    .cat-chip.active { background: #22C55E; border-color: #4ADE80; color: #052E16; font-weight: 900; }
    .grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(min(175px, 100%), 1fr));
      gap: 12px;
    }
    .prod {
      background: var(--card-soft);
      border: 1px solid rgba(255, 255, 255, 0.22);
      border-radius: 16px;
      overflow: hidden;
      display: flex;
      flex-direction: column;
    }
    .prod img {
      width: 100%;
      height: 150px;
      object-fit: cover;
      background: #052E16;
      display: block;
      cursor: pointer;
    }
    .prod-body { padding: 11px; flex: 1; display: flex; flex-direction: column; justify-content: space-between; gap: 8px; }
    .prod-actions { display: flex; flex-direction: column; gap: 6px; }
    .prod-cat { font-size: 11px; color: #A7F3D0; font-weight: 800; text-transform: uppercase; margin-bottom: 2px; }
    .prod-title { font-size: 14px; color: #FFFFFF; font-weight: 800; line-height: 1.3; margin-bottom: 4px; overflow-wrap: anywhere; cursor: pointer; }
    .prod-price { font-size: 15px; font-weight: 900; color: #FDE68A; }
    .prod-old { font-size: 12px; color: rgba(255, 255, 255, 0.6); text-decoration: line-through; margin-left: 6px; }
    .field-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(220px, 100%), 1fr)); gap: 10px; margin-bottom: 10px; }
    .label { display: block; font-size: 12px; font-weight: 800; color: #A7F3D0; margin-bottom: 5px; }
    .cart-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 10px;
      padding: 12px;
      border-radius: 14px;
      background: rgba(5, 46, 22, 0.72);
      border: 1px solid rgba(255, 255, 255, 0.18);
      margin-bottom: 10px;
      flex-wrap: wrap;
    }
    .qty-box { display: flex; align-items: center; gap: 8px; }
    .qty-btn {
      width: 34px; height: 34px; border-radius: 9px; border: 1px solid rgba(255,255,255,0.3);
      background: rgba(255,255,255,0.14); color: #FFFFFF; font-size: 16px; font-weight: 900; cursor: pointer;
    }
    .summary-box {
      padding: 14px;
      border-radius: 14px;
      background: rgba(22, 163, 74, 0.22);
      border: 1px solid var(--border);
      margin: 12px 0;
    }
    .order-card {
      padding: 14px;
      border-radius: 14px;
      background: rgba(5, 46, 22, 0.78);
      border: 1px solid rgba(255, 255, 255, 0.22);
      margin-top: 10px;
    }
    .badge {
      display: inline-block;
      padding: 4px 10px;
      border-radius: 999px;
      font-size: 11px;
      font-weight: 900;
      background: #15803D;
      color: #FFFFFF;
    }
    .badge-warn { background: #B45309; }
    .badge-ok { background: #16A34A; }
    .modal-backdrop {
      display: none;
      position: fixed;
      inset: 0;
      background: rgba(0, 0, 0, 0.76);
      z-index: 60;
      align-items: center;
      justify-content: center;
      padding: 16px;
    }
    .modal-backdrop.open { display: flex; }
    .modal {
      background: #064E3B;
      border: 1.5px solid var(--border);
      border-radius: 20px;
      max-width: 480px;
      width: 100%;
      max-height: 88vh;
      overflow-y: auto;
      padding: 18px;
      color: #FFFFFF;
    }
    .floating-cart {
      display: none;
      position: fixed;
      left: 50%;
      transform: translateX(-50%);
      bottom: max(14px, env(safe-area-inset-bottom));
      z-index: 40;
      width: calc(100% - 24px);
      max-width: 520px;
      background: linear-gradient(135deg, #22C55E, #15803D);
      color: #FFFFFF;
      border: 1.5px solid rgba(255,255,255,0.45);
      border-radius: 16px;
      padding: 12px 16px;
      font-weight: 900;
      font-size: 15px;
      box-shadow: 0 12px 28px rgba(0,0,0,0.45);
      cursor: pointer;
      justify-content: space-between;
      align-items: center;
    }
    .floating-cart.show { display: flex; }
    .empty { text-align: center; padding: 26px 12px; color: var(--muted); font-size: 14px; }
    .msg-banner {
      display: none;
      padding: 12px 14px;
      border-radius: 12px;
      margin-bottom: 12px;
      font-size: 13px;
      font-weight: 700;
      line-height: 1.45;
      overflow-wrap: anywhere;
    }
    .msg-banner.ok { display: block; background: rgba(22, 163, 74, 0.35); border: 1px solid #4ADE80; }
    .msg-banner.err { display: block; background: rgba(220, 38, 38, 0.35); border: 1px solid #FCA5A5; }
    .foot { text-align: center; margin-top: 18px; font-size: 13px; color: var(--muted); }

    @media (max-width: 639px) {
      .search { max-width: 100%; }
      .grid { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
      .prod img { height: 135px; }
      .tabs { gap: 6px; padding: 6px; }
      .tab-btn { font-size: 11.5px; padding: 6px 4px; }
    }
    @media (max-width: 480px) {
      .tabs { grid-template-columns: repeat(3, minmax(0, 1fr)); }
    }
    @media (max-width: 390px) {
      .grid { grid-template-columns: minmax(0, 1fr); }
    }
  </style>
</head>
<body>
  <div class="wrap">
    <div id="offlineBanner" class="offline-banner">
      ⚠️ No Internet Connection — Reconnect to browse live stock, complete Paystack checkout, or track orders.
    </div>

    <div class="hero">
      <div class="pill">✅ $safeHtmlName • ANDROID &amp; IPHONE LIVE STOREFRONT</div>
      <h1>$safeHtmlName</h1>
      <div class="sub">Shop Live Catalog, Pay Securely via Paystack, Submit Pre-Orders &amp; Track Delivery on Any Phone</div>

      <div class="actions">
        <a id="openAppBtn" href="#" class="btn btn-cyan">⚡ Open $safeHtmlName App &amp; Connect</a>
        <button id="copySyncBtn" class="btn btn-green" onclick="copySyncLink()">📋 Copy Store Sync Link</button>
        <a id="downloadApkBtn" href="#" class="btn btn-gold" style="display:none;">⬇️ Download Android APK</a>
        <button id="installWebBtn" class="btn btn-dark" onclick="installWebApp()">📱 Add to Home Screen</button>
      </div>

      <div class="status-note" id="syncStatusNote">
        📱 <strong>iPhone (Safari) Users:</strong> Tap <strong>Share (⬆️)</strong> in Safari &rarr; <strong>Add to Home Screen</strong> to install $safeHtmlName on your iPhone, and shop, pay with Paystack, pre-order, or track orders below.<br/>
        🤖 <strong>Android Users:</strong> Tap <strong>Open $safeHtmlName App &amp; Connect</strong> above or shop directly here in your browser!
      </div>
    </div>

    <div id="globalAlert" class="msg-banner"></div>

    <div id="customerCredentialsPanel" class="card" style="padding:14px 16px;margin-bottom:12px;border-color:rgba(245,158,11,0.55);background:rgba(5,46,22,0.92);">
      <div style="display:flex;justify-content:space-between;align-items:center;gap:10px;flex-wrap:wrap;">
        <div style="min-width:0;flex:1;">
          <div style="font-size:11px;font-weight:900;letter-spacing:0.6px;color:#FDE68A;text-transform:uppercase;margin-bottom:4px;">
            👤 Permanent Customer Credentials
          </div>
          <div id="savedCredentialsSummary" style="font-size:13px;font-weight:700;color:#FFFFFF;line-height:1.4;word-break:break-word;">
            Tap Edit to save your Name, Phone Number &amp; Delivery Address permanently.
          </div>
        </div>
        <button id="toggleCredEditBtn" type="button" class="btn btn-gold btn-sm" onclick="toggleCredentialsEditor()">✏️ Edit</button>
      </div>
      <div id="credentialsEditorForm" style="display:none;margin-top:12px;border-top:1px solid rgba(255,255,255,0.16);padding-top:12px;">
        <div class="field-grid">
          <div>
            <label class="label" for="credName">Full Name *</label>
            <input class="input" id="credName" placeholder="e.g. Chinedu Okafor" />
          </div>
          <div>
            <label class="label" for="credPhone">Phone Number (WhatsApp) *</label>
            <input class="input" id="credPhone" type="tel" placeholder="e.g. 08012345678" />
          </div>
        </div>
        <div style="margin-bottom:10px;">
          <label class="label" for="credAddress">Delivery Address (Street, City, State) *</label>
          <input class="input" id="credAddress" placeholder="e.g. 14 Admiralty Way, Lekki Phase 1, Lagos" />
        </div>
        <button id="saveCredentialsBtn" type="button" class="btn btn-green btn-sm" style="width:100%;" onclick="savePermanentCredentialsForm()">
          💾 Save Customer Credentials Permanently
        </button>
      </div>
    </div>

    <nav class="tabs" aria-label="Storefront Sections">
      <button class="tab-btn active" data-tab="shop" onclick="switchTab('shop')">🛍️ Shop</button>
      <button class="tab-btn" data-tab="wishlist" onclick="switchTab('wishlist')">❤️ Wishlist (<span id="wishlistTabCount">0</span>)</button>
      <button class="tab-btn" data-tab="cart" onclick="switchTab('cart')">🛒 Cart (<span id="cartTabCount">0</span>)</button>
      <button class="tab-btn" data-tab="track" onclick="switchTab('track')">📦 Track</button>
      <button class="tab-btn" data-tab="preorder" onclick="switchTab('preorder')">✨ Pre-Order</button>
    </nav>

    <section id="panel-shop" class="panel active">
      <div class="card">
        <div class="card-head">
          <h2>🛍️ Live Storefront Catalog</h2>
          <input class="input search" id="searchBox" type="search" placeholder="Search shoes, clothes, gadgets..." oninput="renderProducts()" />
        </div>
        <div id="categoryChips" class="cats"></div>
        <div id="catalogContainer" class="empty">Connecting to live store inventory...</div>
      </div>
    </section>

    <section id="panel-wishlist" class="panel">
      <div class="card">
        <div class="card-head">
          <h2>❤️ Saved Wishlist Items</h2>
          <button class="btn btn-dark btn-sm" onclick="clearWishlist()">Clear Wishlist</button>
        </div>
        <div id="wishlistContainer" class="empty">Your wishlist is empty. Tap &ldquo;Add to Wishlist&rdquo; on any product card!</div>
      </div>
    </section>

    <section id="panel-cart" class="panel">
      <div class="card">
        <div class="card-head">
          <h2>🛒 Your Shopping Cart</h2>
          <button class="btn btn-dark btn-sm" onclick="clearCart()">Clear Cart</button>
        </div>
        <div id="cartItemsContainer"></div>
        <div id="checkoutSection" style="display:none;">
          <div class="summary-box">
            <div style="display:flex;justify-content:space-between;font-weight:900;font-size:17px;">
              <span>Total Payable:</span>
              <span id="cartTotalText">₦0</span>
            </div>
            <div style="font-size:12px;color:#A7F3D0;margin-top:4px;">Instant sync to $safeHtmlName Admin &amp; verified Paystack checkout</div>
            <div id="webSplitBanner" style="display:none;margin-top:6px;font-size:11.5px;color:#FDE68A;font-weight:700;"></div>
          </div>

          <div class="field-grid">
            <div>
              <label class="label" for="coName">Full Name *</label>
              <input class="input" id="coName" placeholder="e.g. Chinedu Okafor" oninput="syncPermanentCustomerCredentials('co')" />
            </div>
            <div>
              <label class="label" for="coPhone">Phone Number (WhatsApp) *</label>
              <input class="input" id="coPhone" type="tel" placeholder="e.g. 08012345678" oninput="syncPermanentCustomerCredentials('co')" />
            </div>
            <div>
              <label class="label" for="coPayment">Payment Method *</label>
              <select class="select" id="coPayment" onchange="toggleTransferHint()">
                <option value="PAYSTACK">Paystack (Card / Bank / Transfer / USSD)</option>
                <option value="OPAY">OPay Wallet Transfer</option>
                <option value="MONIEPOINT">Moniepoint Bank Transfer</option>
              </select>
            </div>
          </div>

          <div style="margin-bottom:10px;">
            <label class="label" for="coAddress">Delivery Address (Street, City, State) *</label>
            <input class="input" id="coAddress" placeholder="e.g. 14 Admiralty Way, Lekki Phase 1, Lagos" oninput="syncPermanentCustomerCredentials('co')" />
          </div>

          <div id="transferSenderRow" style="display:none;margin-bottom:10px;">
            <label class="label" for="coSender">Sender Account Name / Transfer Reference</label>
            <input class="input" id="coSender" placeholder="Name on bank transfer or reference ID" />
          </div>

          <button id="placeOrderBtn" class="btn btn-cyan" style="width:100%;" onclick="submitCheckout()">
            🔒 Complete Checkout &amp; Pay Now
          </button>
        </div>
      </div>
    </section>

    <section id="panel-track" class="panel">
      <div class="card">
        <div class="card-head">
          <h2>📦 Live Order Tracking</h2>
        </div>
        <div class="field-grid">
          <div>
            <label class="label" for="trackQuery">Phone Number or Order ID</label>
            <input class="input" id="trackQuery" placeholder="e.g. 08012345678 or ORD-..." />
          </div>
          <div style="display:flex;align-items:flex-end;">
            <button id="trackSearchBtn" class="btn btn-cyan" style="width:100%;" onclick="trackOrders()">🔍 Track My Orders</button>
          </div>
        </div>
        <div id="trackResults" class="empty">Enter your phone number or Order ID above to view real-time status from Supabase.</div>
      </div>
    </section>

    <section id="panel-preorder" class="panel">
      <div class="card">
        <div class="card-head">
          <h2>✨ Request Custom Pre-Order</h2>
        </div>
        <p style="font-size:13px;color:#A7F3D0;margin-bottom:12px;">
          Can't find your exact brand, size, or model? Send a pre-order request straight to $safeHtmlName Admin!
        </p>
        <div class="field-grid">
          <div>
            <label class="label" for="poName">Your Full Name *</label>
            <input class="input" id="poName" placeholder="e.g. Amina Bello" oninput="syncPermanentCustomerCredentials('po')" />
          </div>
          <div>
            <label class="label" for="poPhone">Your Phone Number *</label>
            <input class="input" id="poPhone" type="tel" placeholder="e.g. 08099887766" oninput="syncPermanentCustomerCredentials('po')" />
          </div>
          <div>
            <label class="label" for="poProduct">Product Name *</label>
            <input class="input" id="poProduct" placeholder="e.g. Air Jordan 4 Retro Military Blue" />
          </div>
          <div>
            <label class="label" for="poBrand">Preferred Brand</label>
            <input class="input" id="poBrand" placeholder="e.g. Nike, Gucci, Apple" />
          </div>
          <div>
            <label class="label" for="poCategory">Category</label>
            <select class="select" id="poCategory">
              <option value="Clothes">Clothes</option>
              <option value="Shoes">Shoes</option>
              <option value="Bags">Bags</option>
              <option value="Beauty Product">Beauty Product</option>
              <option value="Electronics">Electronics</option>
              <option value="Food">Food</option>
              <option value="Beverages">Beverages</option>
              <option value="Jewelry & Watches">Jewelry &amp; Watches</option>
              <option value="Phones & Accessories">Phones &amp; Accessories</option>
              <option value="Home & Kitchen">Home &amp; Kitchen</option>
              <option value="General">General</option>
            </select>
          </div>
          <div>
            <label class="label" for="poBudget">Target Budget (₦)</label>
            <input class="input" id="poBudget" type="number" min="0" placeholder="e.g. 45000" />
          </div>
          <div>
            <label class="label" for="poSize">Preferred Size</label>
            <input class="input" id="poSize" placeholder="e.g. EU 43 / XL / 256GB" />
          </div>
          <div>
            <label class="label" for="poColor">Preferred Color</label>
            <input class="input" id="poColor" placeholder="e.g. Black, Gold" />
          </div>
        </div>
        <div style="margin-bottom:10px;">
          <label class="label" for="poDesc">Additional Notes / Specs</label>
          <textarea class="textarea" id="poDesc" placeholder="Describe quantity, packaging, or delivery deadline..."></textarea>
        </div>
        <button id="submitPreOrderBtn" class="btn btn-green" style="width:100%;" onclick="submitPreOrder()">
          🚀 Send Pre-Order Request to Admin
        </button>
      </div>

      <div class="card">
        <div class="card-head">
          <h2>💬 Check My Pre-Order Replies</h2>
        </div>
        <div class="field-grid">
          <div>
            <input class="input" id="poLookupPhone" type="tel" placeholder="Enter your phone number to check quotes" />
          </div>
          <div>
            <button class="btn btn-dark" style="width:100%;" onclick="loadMyPreOrders()">🔄 Check Admin Quotes</button>
          </div>
        </div>
        <div id="preOrderResults" class="empty">Submitted requests and Admin price quotes appear here.</div>
      </div>
    </section>

    <div class="foot">© $safeHtmlName • $safeHtmlContact</div>
  </div>

  <div id="productModal" class="modal-backdrop" onclick="if(event.target===this) closeProductModal()">
    <div class="modal">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:10px;">
        <h3 id="pmTitle" style="font-size:18px;font-weight:900;"></h3>
        <button class="qty-btn" onclick="closeProductModal()">✕</button>
      </div>
      <img id="pmImg" src="" alt="" style="width:100%;height:200px;object-fit:cover;border-radius:14px;margin-bottom:10px;display:none;" />
      <div id="pmPrice" style="font-size:20px;font-weight:900;color:#FDE68A;margin-bottom:6px;"></div>
      <div id="pmStock" style="font-size:12px;color:#A7F3D0;margin-bottom:8px;"></div>
      <p id="pmDesc" style="font-size:13px;line-height:1.45;color:#FFFFFF;margin-bottom:12px;"></p>
      <div class="field-grid">
        <div>
          <label class="label" for="pmBrand">Brand</label>
          <select class="select" id="pmBrand"></select>
        </div>
        <div>
          <label class="label" for="pmSize">Size</label>
          <select class="select" id="pmSize"></select>
        </div>
        <div>
          <label class="label" for="pmColor">Color</label>
          <select class="select" id="pmColor"></select>
        </div>
        <div>
          <label class="label" for="pmQty">Quantity</label>
          <input class="input" id="pmQty" type="number" min="1" max="50" value="1" />
        </div>
      </div>
      <button id="pmAddBtn" class="btn btn-cyan" style="width:100%;" onclick="confirmAddFromModal()">🛒 Add to Cart</button>
      <button id="pmWishBtn" class="btn btn-wish" style="width:100%;margin-top:8px;" onclick="toggleWishlistFromModal()">🤍 Add to Wishlist</button>
    </div>
  </div>

  <button id="floatingCartBtn" class="floating-cart" onclick="switchTab('cart')">
    <span>🛒 <span id="fcCount">0</span> item(s) in Cart</span>
    <span id="fcTotal">₦0 &rarr;</span>
  </button>

  <script>
    const EMBEDDED_SB_URL = $escapedSbUrl;
    const EMBEDDED_SB_KEY = $escapedSbKey;
    const ANDROID_PACKAGE = $escapedPkg;
    const APK_DOWNLOAD_URL = $escapedApkUrl;

    const params = new URLSearchParams(window.location.search);
    const sbUrl = (params.get("sb_url") || EMBEDDED_SB_URL || "").trim().replace(/\/+$/, "");
    const sbKey = (params.get("sb_key") || EMBEDDED_SB_KEY || "").trim();

    const ua = navigator.userAgent || "";
    const isAndroid = /Android/i.test(ua);
    const isIOS = /iPhone|iPad|iPod/i.test(ua) || (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1);
    const isAltBrowser = /OPR|Opera|Opera Mini|UCBrowser|PHX|SamsungBrowser|MiuiBrowser|VivoBrowser|HeyTapBrowser|YaBrowser|Firefox|FxiOS|EdgA/i.test(ua);
    const isWebView = /\bwv\b|Instagram|FBAN|FBAV|WhatsApp/i.test(ua);
    const isChromeAndroid = isAndroid && /\bChrome\/\d+/i.test(ua) && !isAltBrowser && !isWebView;

    const apkBtn = document.getElementById("downloadApkBtn");
    const openBtn = document.getElementById("openAppBtn");
    const copyBtn = document.getElementById("copySyncBtn");
    const installBtn = document.getElementById("installWebBtn");
    const statusNote = document.getElementById("syncStatusNote");
    const cleanApkUrl = (typeof APK_DOWNLOAD_URL === "string" ? APK_DOWNLOAD_URL.trim() : "");

    if (isIOS) {
      apkBtn.style.display = "none";
      openBtn.style.display = "none";
      copyBtn.style.display = "none";
      installBtn.innerText = "📱 Install on iPhone (Safari)";
    } else if (cleanApkUrl && /^https?:\/\/[^\s<>]+$/i.test(cleanApkUrl) && !cleanApkUrl.includes("<")) {
      apkBtn.href = cleanApkUrl;
      apkBtn.setAttribute("download", "Odiya_Official.apk");
      apkBtn.style.display = "flex";
    } else {
      apkBtn.removeAttribute("href");
      apkBtn.style.display = "none";
    }

    const customSchemeLink = (sbUrl && sbKey)
      ? ("depros://connect?sb_url=" + encodeURIComponent(sbUrl) + "&sb_key=" + encodeURIComponent(sbKey))
      : "depros://open";

    let webFallbackUrl = "";
    try {
      const fb = new URL(window.location.href);
      fb.searchParams.delete("open_app");
      fb.searchParams.set("web", "1");
      webFallbackUrl = fb.toString();
    } catch (_) {}
    const fbSuffix = webFallbackUrl ? (";S.browser_fallback_url=" + encodeURIComponent(webFallbackUrl)) : "";

    const androidIntentLink = (sbUrl && sbKey)
      ? ("intent://connect?sb_url=" + encodeURIComponent(sbUrl) + "&sb_key=" + encodeURIComponent(sbKey) + "#Intent;scheme=depros;package=" + ANDROID_PACKAGE + fbSuffix + ";end")
      : ("intent://open#Intent;scheme=depros;package=" + ANDROID_PACKAGE + fbSuffix + ";end");

    openBtn.href = (isChromeAndroid || isWebView) ? androidIntentLink : customSchemeLink;

    openBtn.addEventListener("click", function(e) {
      if (isAndroid) {
        e.preventDefault();
        let leftPage = false;
        const onHide = function() { if (document.hidden) leftPage = true; };
        document.addEventListener("visibilitychange", onHide, { once: true });
        try {
          window.location.href = (isChromeAndroid || isWebView) ? androidIntentLink : customSchemeLink;
        } catch (_) {}
        if (!isChromeAndroid) {
          setTimeout(function() {
            if (!leftPage && !document.hidden) {
              try { window.location.href = customSchemeLink; } catch (_) {}
            }
          }, 260);
        }
        setTimeout(function() {
          document.removeEventListener("visibilitychange", onHide);
          if (!leftPage && !document.hidden && statusNote) {
            statusNote.innerHTML = "⚠️ <strong>App not opening from this browser?</strong> Tap <strong>Copy Store Sync Link</strong> above &rarr; paste inside ODIYA &rarr; <strong>Account &rarr; Link Now</strong>, or tap <strong>Download Android APK</strong>!";
          }
        }, 1500);
      }
    });

    const stayOnWeb = params.get("web") === "1";
    const isHttpsOrigin = window.location.protocol === "https:" || window.location.protocol === "http:";
    if (stayOnWeb && isAndroid && statusNote) {
      statusNote.innerHTML = "⬇️ <strong>Update / APK Storefront Mode:</strong> Tap <strong>Download Android APK</strong> above to install the newest ODIYA build cleanly, or tap <strong>Open $safeHtmlName App</strong> anytime.";
    }
    const wantsAutoLaunch = params.get("open_app") === "1" || params.has("sb_url") || params.get("open") === "1";
    if (isAndroid && isHttpsOrigin && !window.AndroidBridge && !stayOnWeb && wantsAutoLaunch) {
      setTimeout(function() {
        try {
          window.location.href = (isChromeAndroid || isWebView) ? androidIntentLink : customSchemeLink;
        } catch (_) {}
      }, 80);
    }

    if (!sbUrl || !sbKey) {
      copyBtn.style.display = "none";
    }

    function copySyncLink() {
      const btn = document.getElementById("copySyncBtn");
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(customSchemeLink).then(function() {
          btn.innerText = "✅ Copied! Paste in App -> Account -> Link Now";
          setTimeout(function() { btn.innerText = "📋 Copy Store Sync Link"; }, 3000);
        }).catch(function() {
          window.prompt("Copy Store Sync Link:", customSchemeLink);
        });
      } else {
        window.prompt("Copy Store Sync Link:", customSchemeLink);
      }
    }

    const manifestObj = {
      name: "ODIYA",
      short_name: "Odiya",
      start_url: window.location.href.split("?")[0],
      display: "standalone",
      background_color: "#052E16",
      theme_color: "#064E3B"
    };
    const manifestBlob = new Blob([JSON.stringify(manifestObj)], { type: "application/manifest+json" });
    document.getElementById("pwaManifestLink").href = URL.createObjectURL(manifestBlob);

    let deferredPrompt = null;
    window.addEventListener("beforeinstallprompt", function(e) {
      e.preventDefault();
      deferredPrompt = e;
    });

    function installWebApp() {
      if (deferredPrompt) {
        deferredPrompt.prompt();
        deferredPrompt.userChoice.then(function() { deferredPrompt = null; });
      } else if (isIOS) {
        showGlobalAlert("On iPhone Safari: tap the Share button (⬆️ at the bottom) and choose 'Add to Home Screen' to save ODIYA like a native app!", true);
      } else {
        showGlobalAlert("Open your browser menu (⋮) and tap 'Add to Home screen' or 'Install App' to save ODIYA.", true);
      }
    }

    function updateOnlineState() {
      const banner = document.getElementById("offlineBanner");
      if (navigator.onLine === false) {
        banner.classList.add("show");
      } else {
        banner.classList.remove("show");
      }
    }
    window.addEventListener("online", updateOnlineState);
    window.addEventListener("offline", updateOnlineState);
    updateOnlineState();

    function sbHeaders(extra) {
      const h = { "apikey": sbKey, "Accept": "application/json" };
      if (sbKey.startsWith("eyJ") && sbKey.split(".").length === 3) {
        h["Authorization"] = "Bearer " + sbKey;
      }
      if (extra) {
        Object.keys(extra).forEach(function(k) { h[k] = extra[k]; });
      }
      return h;
    }

    function escapeHtml(s) {
      return String(s || "").replace(/[&<>"']/g, function(c) {
        return { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c];
      });
    }

    function showGlobalAlert(msg, isOk) {
      const el = document.getElementById("globalAlert");
      el.className = "msg-banner " + (isOk ? "ok" : "err");
      el.innerText = msg;
      el.scrollIntoView({ behavior: "smooth", block: "nearest" });
    }

    function switchTab(tabName) {
      document.querySelectorAll(".tab-btn").forEach(function(b) {
        b.classList.toggle("active", b.getAttribute("data-tab") === tabName);
      });
      document.querySelectorAll(".panel").forEach(function(p) {
        p.classList.toggle("active", p.id === "panel-" + tabName);
      });
      if (tabName === "cart") renderCart();
      if (tabName === "wishlist") renderWishlist();
      if (tabName === "track" && !document.getElementById("trackQuery").value) {
        const savedPhone = localStorage.getItem("odiya_customer_phone") || "";
        const savedOrd = localStorage.getItem("odiya_last_order_id") || "";
        if (savedPhone || savedOrd) {
          document.getElementById("trackQuery").value = savedPhone || savedOrd;
          trackOrders();
        }
      }
      if (tabName === "preorder" && !document.getElementById("poLookupPhone").value) {
        const savedPhone = localStorage.getItem("odiya_customer_phone") || "";
        if (savedPhone) {
          document.getElementById("poLookupPhone").value = savedPhone;
          loadMyPreOrders();
        }
      }
    }

    let wishlist = [];
    try {
      wishlist = JSON.parse(localStorage.getItem("odiya_web_wishlist") || "[]");
      if (!Array.isArray(wishlist)) wishlist = [];
    } catch (_) { wishlist = []; }

    function saveWishlist() {
      try { localStorage.setItem("odiya_web_wishlist", JSON.stringify(wishlist)); } catch (_) {}
      updateWishlistBadge();
      if (allProducts.length) renderProducts();
      renderWishlist();
      syncModalWishlistButton();
    }

    function updateWishlistBadge() {
      const countEl = document.getElementById("wishlistTabCount");
      if (countEl) countEl.innerText = String(wishlist.length);
    }

    function isInWishlist(productId) {
      return wishlist.some(function(item) { return item && item.id === productId; });
    }

    function findKnownProduct(productId) {
      return allProducts.find(function(x) { return x.id === productId; }) ||
        wishlist.find(function(x) { return x && x.id === productId; }) ||
        null;
    }

    function toggleWishlistProduct(productId) {
      const idx = wishlist.findIndex(function(item) { return item && item.id === productId; });
      if (idx >= 0) {
        const removed = wishlist[idx];
        wishlist.splice(idx, 1);
        saveWishlist();
        showGlobalAlert("Removed '" + ((removed && removed.title) || "Item") + "' from Wishlist.", true);
        return;
      }
      const p = findKnownProduct(productId);
      if (!p) return;
      wishlist.unshift({
        id: p.id,
        title: p.title || "Item",
        category: p.category || "ODIYA",
        brand: p.brand || "ODIYA",
        available_brands: p.available_brands || "",
        available_sizes: p.available_sizes || "",
        available_colors: p.available_colors || "",
        base_price: Number(p.base_price || 0),
        discount_percent: Number(p.discount_percent || 0),
        stock_quantity: Number(p.stock_quantity || 0),
        image_url: p.image_url || "",
        description: p.description || "",
        is_catalog_locked: p.is_catalog_locked === true,
        partner_id: p.partner_id || "",
        partner_name: p.partner_name || "",
        partner_commission_percent: Number(p.partner_commission_percent || 0),
        savedAt: Date.now()
      });
      saveWishlist();
      showGlobalAlert("❤️ Saved '" + (p.title || "Item") + "' to your Wishlist!", true);
    }

    function toggleWishlistFromModal() {
      if (!modalProduct || !modalProduct.id) return;
      toggleWishlistProduct(modalProduct.id);
    }

    function syncModalWishlistButton() {
      const btn = document.getElementById("pmWishBtn");
      if (!btn || !modalProduct) return;
      const saved = isInWishlist(modalProduct.id);
      btn.className = "btn btn-wish" + (saved ? " saved" : "");
      btn.innerText = saved ? "❤️ Saved in Wishlist (Tap to Remove)" : "🤍 Add to Wishlist";
    }

    function clearWishlist() {
      wishlist = [];
      saveWishlist();
      showGlobalAlert("Wishlist cleared.", true);
    }

    function renderWishlist() {
      const box = document.getElementById("wishlistContainer");
      if (!box) return;
      if (!wishlist.length) {
        box.className = "empty";
        box.innerText = "Your wishlist is empty. Tap \"Add to Wishlist\" on any product card!";
        return;
      }
      box.className = "grid";
      box.innerHTML = wishlist.map(function(savedItem) {
        const p = allProducts.find(function(x) { return x.id === savedItem.id; }) || savedItem;
        const base = Math.round(Number(p.base_price || 0));
        const finalPrice = effectivePrice(p);
        const disc = Number(p.discount_percent || 0);
        const stock = Number(p.stock_quantity || 0);
        const locked = p.is_catalog_locked === true || stock <= 0;
        const rawUrl = (p.image_url || "").trim();
        const hasHttpImg = rawUrl.startsWith("http://") || rawUrl.startsWith("https://");
        const safeId = escapeHtml(p.id).replace(/'/g, "\\'");
        const mediaHtml = hasHttpImg
          ? '<img src="' + escapeHtml(rawUrl) + '" alt="' + escapeHtml(p.title) + '" loading="lazy" onclick="openProductModal(\'' + safeId + '\')" onerror="this.onerror=null;this.style.visibility=\'hidden\';" />'
          : '<div onclick="openProductModal(\'' + safeId + '\')" style="width:100%;height:140px;background:#052E16;display:flex;align-items:center;justify-content:center;color:rgba(255,255,255,0.65);font-size:13px;font-weight:700;cursor:pointer;">ODIYA Item</div>';
        const priceHtml = '₦' + finalPrice.toLocaleString("en-NG") + (disc > 0 ? '<span class="prod-old">₦' + base.toLocaleString("en-NG") + '</span>' : '');
        const cartBtnHtml = locked
          ? '<button class="btn btn-dark btn-sm" disabled>Unavailable</button>'
          : '<button class="btn btn-cyan btn-sm" onclick="quickAddProduct(\'' + safeId + '\')">+ Add to Cart</button>';
        return '<div class="prod">' +
          mediaHtml +
          '<div class="prod-body">' +
            '<div>' +
              '<div class="prod-cat">' + escapeHtml(p.category || "Odiya") + (disc > 0 ? ' • -' + disc + '%' : '') + '</div>' +
              '<div class="prod-title" onclick="openProductModal(\'' + safeId + '\')">' + escapeHtml(p.title || "Item") + '</div>' +
              '<div class="prod-price">' + priceHtml + '</div>' +
            '</div>' +
            '<div class="prod-actions">' +
              cartBtnHtml +
              '<button class="btn btn-wish saved btn-sm" onclick="toggleWishlistProduct(\'' + safeId + '\')">✕ Remove</button>' +
            '</div>' +
          '</div>' +
        '</div>';
      }).join("");
    }

    let cart = [];
    try {
      cart = JSON.parse(localStorage.getItem("odiya_web_cart") || "[]");
      if (!Array.isArray(cart)) cart = [];
    } catch (_) { cart = []; }

    function saveCart() {
      try { localStorage.setItem("odiya_web_cart", JSON.stringify(cart)); } catch (_) {}
      updateCartBadge();
    }

    function updateCartBadge() {
      const totalQty = cart.reduce(function(sum, item) { return sum + Number(item.qty || 1); }, 0);
      const totalAmt = cart.reduce(function(sum, item) { return sum + Number(item.unitPrice || 0) * Number(item.qty || 1); }, 0);
      document.getElementById("cartTabCount").innerText = String(totalQty);
      const fc = document.getElementById("floatingCartBtn");
      document.getElementById("fcCount").innerText = String(totalQty);
      document.getElementById("fcTotal").innerText = "₦" + Math.round(totalAmt).toLocaleString("en-NG") + " →";
      fc.classList.toggle("show", totalQty > 0);
    }

    function effectivePrice(p) {
      const base = Number(p.base_price || 0);
      const disc = Number(p.discount_percent || 0);
      return disc > 0 ? Math.round(base * (1 - disc / 100)) : Math.round(base);
    }

    function parseList(csv, fallback) {
      const arr = String(csv || "").split(",").map(function(x) { return x.trim(); }).filter(Boolean);
      return arr.length ? arr : fallback;
    }

    function parseColors(raw) {
      const items = String(raw || "").split(";").map(function(part) {
        return part.split("|")[0].trim();
      }).filter(Boolean);
      return items.length ? items : ["Standard"];
    }

    let allProducts = [];
    let allPartners = [];
    let selectedCategory = "All";
    let modalProduct = null;

    async function loadLiveCatalog() {
      const container = document.getElementById("catalogContainer");
      if (!sbUrl || !sbKey) {
        container.innerHTML = '<div class="empty">Store Cloud Link not attached yet. Open from your store link to shop live inventory.</div>';
        return;
      }
      try {
        const res = await fetch(sbUrl + "/rest/v1/products?select=id,title,category,brand,available_brands,available_sizes,available_colors,base_price,discount_percent,stock_quantity,image_url,description,is_catalog_locked,partner_id,partner_name,partner_commission_percent&order=created_at.desc.nullslast&limit=100", {
          headers: sbHeaders()
        });
        if (!res.ok) throw new Error("HTTP " + res.status);
        allProducts = await res.json();
        try {
          const pRes = await fetch(sbUrl + "/storage/v1/object/public/shop-receipts/partners_ledger.json?t=" + Date.now());
          if (pRes.ok) {
            const pJson = await pRes.json();
            if (pJson && Array.isArray(pJson.organizations)) allPartners = pJson.organizations;
          }
        } catch (_) {}
        renderCategoryChips();
        renderProducts();
        renderWishlist();
        renderCart();
      } catch (err) {
        container.innerHTML = '<div class="empty">Could not load live catalog (' + escapeHtml(err.message) + '). Check internet connection and refresh.</div>';
      }
    }

    function renderCategoryChips() {
      const cats = ["All"];
      allProducts.forEach(function(p) {
        const c = (p.category || "").trim();
        if (c && cats.indexOf(c) === -1) cats.push(c);
      });
      document.getElementById("categoryChips").innerHTML = cats.map(function(c) {
        const active = c === selectedCategory ? "active" : "";
        return '<button class="cat-chip ' + active + '" onclick="selectCategory(\'' + escapeHtml(c).replace(/'/g, "\\'") + '\')">' + escapeHtml(c) + '</button>';
      }).join("");
    }

    function selectCategory(cat) {
      selectedCategory = cat;
      renderCategoryChips();
      renderProducts();
    }

    function renderProducts() {
      const container = document.getElementById("catalogContainer");
      const q = (document.getElementById("searchBox").value || "").trim().toLowerCase();
      const filtered = allProducts.filter(function(p) {
        const catMatch = selectedCategory === "All" || (p.category || "").trim() === selectedCategory;
        const textMatch = !q || (p.title || "").toLowerCase().includes(q) || (p.category || "").toLowerCase().includes(q) || (p.brand || "").toLowerCase().includes(q);
        return catMatch && textMatch;
      });
      if (!filtered.length) {
        container.className = "empty";
        container.innerText = allProducts.length ? "No products match your filter." : "No products uploaded in catalog yet.";
        return;
      }
      container.className = "grid";
      container.innerHTML = filtered.map(function(p) {
        const base = Math.round(Number(p.base_price || 0));
        const finalPrice = effectivePrice(p);
        const disc = Number(p.discount_percent || 0);
        const stock = Number(p.stock_quantity || 0);
        const locked = p.is_catalog_locked === true || stock <= 0;
        const rawUrl = (p.image_url || "").trim();
        const hasHttpImg = rawUrl.startsWith("http://") || rawUrl.startsWith("https://");
        const safeId = escapeHtml(p.id).replace(/'/g, "\\'");
        const inWish = isInWishlist(p.id);
        const mediaHtml = hasHttpImg
          ? '<img src="' + escapeHtml(rawUrl) + '" alt="' + escapeHtml(p.title) + '" loading="lazy" onclick="openProductModal(\'' + safeId + '\')" onerror="this.onerror=null;this.style.visibility=\'hidden\';" />'
          : '<div onclick="openProductModal(\'' + safeId + '\')" style="width:100%;height:140px;background:#052E16;display:flex;align-items:center;justify-content:center;color:rgba(255,255,255,0.65);font-size:13px;font-weight:700;cursor:pointer;">ODIYA Item</div>';
        const priceHtml = '₦' + finalPrice.toLocaleString("en-NG") + (disc > 0 ? '<span class="prod-old">₦' + base.toLocaleString("en-NG") + '</span>' : '');
        const btnHtml = locked
          ? '<button class="btn btn-dark btn-sm" disabled>Unavailable</button>'
          : '<button class="btn btn-cyan btn-sm" onclick="quickAddProduct(\'' + safeId + '\')">+ Add to Cart</button>';
        const wishBtnHtml = '<button class="btn btn-wish' + (inWish ? ' saved' : '') + ' btn-sm" onclick="toggleWishlistProduct(\'' + safeId + '\')">' +
          (inWish ? '❤️ Saved in Wishlist' : '🤍 Add to Wishlist') + '</button>';
        return '<div class="prod">' +
          mediaHtml +
          '<div class="prod-body">' +
            '<div>' +
              '<div class="prod-cat">' + escapeHtml(p.category || "Odiya") + (disc > 0 ? ' • -' + disc + '%' : '') + '</div>' +
              '<div class="prod-title" onclick="openProductModal(\'' + safeId + '\')">' + escapeHtml(p.title || "Item") + '</div>' +
              '<div class="prod-price">' + priceHtml + '</div>' +
            '</div>' +
            '<div class="prod-actions">' +
              btnHtml +
              wishBtnHtml +
            '</div>' +
          '</div>' +
        '</div>';
      }).join("");
    }

    function quickAddProduct(productId) {
      const p = findKnownProduct(productId);
      if (!p) return;
      const brand = parseList(p.available_brands, [p.brand || "ODIYA"])[0];
      const size = parseList(p.available_sizes, ["Standard"])[0];
      const color = parseColors(p.available_colors)[0];
      addItemToCart(p, brand, size, color, 1);
      showGlobalAlert("✅ Added '" + p.title + "' to cart!", true);
    }

    function openProductModal(productId) {
      const p = findKnownProduct(productId);
      if (!p) return;
      modalProduct = p;
      document.getElementById("pmTitle").innerText = p.title || "Product";
      const img = document.getElementById("pmImg");
      const rawUrl = (p.image_url || "").trim();
      if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
        img.src = rawUrl;
        img.style.display = "block";
      } else {
        img.style.display = "none";
      }
      document.getElementById("pmPrice").innerText = "₦" + effectivePrice(p).toLocaleString("en-NG");
      const stock = Number(p.stock_quantity || 0);
      const locked = p.is_catalog_locked === true || stock <= 0;
      document.getElementById("pmStock").innerText = locked ? "Currently out of stock / catalog locked" : ("In Stock: " + stock + " unit(s)");
      document.getElementById("pmDesc").innerText = p.description || "Verified quality item.";
      document.getElementById("pmBrand").innerHTML = parseList(p.available_brands, [p.brand || "ODIYA"]).map(function(v) { return '<option>' + escapeHtml(v) + '</option>'; }).join("");
      document.getElementById("pmSize").innerHTML = parseList(p.available_sizes, ["Standard"]).map(function(v) { return '<option>' + escapeHtml(v) + '</option>'; }).join("");
      document.getElementById("pmColor").innerHTML = parseColors(p.available_colors).map(function(v) { return '<option>' + escapeHtml(v) + '</option>'; }).join("");
      document.getElementById("pmQty").value = "1";
      document.getElementById("pmAddBtn").disabled = locked;
      syncModalWishlistButton();
      document.getElementById("productModal").classList.add("open");
    }

    function closeProductModal() {
      document.getElementById("productModal").classList.remove("open");
    }

    function confirmAddFromModal() {
      if (!modalProduct) return;
      const brand = document.getElementById("pmBrand").value;
      const size = document.getElementById("pmSize").value;
      const color = document.getElementById("pmColor").value;
      const qty = Math.max(1, Math.min(50, parseInt(document.getElementById("pmQty").value || "1", 10) || 1));
      addItemToCart(modalProduct, brand, size, color, qty);
      closeProductModal();
      showGlobalAlert("✅ Added " + qty + "x '" + modalProduct.title + "' to cart!", true);
    }

    function addItemToCart(p, brand, size, color, qty) {
      const unitPrice = effectivePrice(p);
      const existing = cart.find(function(i) {
        return i.id === p.id && i.brand === brand && i.size === size && i.color === color;
      });
      if (existing) {
        existing.qty = Number(existing.qty || 1) + qty;
        existing.partnerId = p.partner_id || existing.partnerId || "";
        existing.partnerName = p.partner_name || existing.partnerName || "";
        existing.commissionPct = Number(p.partner_commission_percent || existing.commissionPct || 0);
      } else {
        cart.push({
          id: p.id,
          title: p.title,
          brand: brand,
          size: size,
          color: color,
          unitPrice: unitPrice,
          qty: qty,
          partnerId: p.partner_id || "",
          partnerName: p.partner_name || "",
          commissionPct: Number(p.partner_commission_percent || 0)
        });
      }
      saveCart();
    }

    function computeWebPaystackSplit() {
      const totalAmountNaira = cart.reduce(function(s, i) { return s + Number(i.unitPrice || 0) * Number(i.qty || 1); }, 0);
      const groups = {};
      cart.forEach(function(item) {
        const prodMatch = allProducts.find(function(p) { return p.id === item.id; });
        const pid = String(item.partnerId || (prodMatch && prodMatch.partner_id) || "").trim();
        if (!pid) return;
        if (!groups[pid]) groups[pid] = [];
        groups[pid].push({ item: item, prod: prodMatch });
      });
      const allocations = [];
      let totalCommission = 0;
      let totalHostingOffset = 0;
      let totalPartnerNet = 0;

      Object.keys(groups).forEach(function(pid) {
        const org = allPartners.find(function(o) { return o.id === pid; });
        const entries = groups[pid];
        const partnerName = (org && org.businessName) || (entries[0].item.partnerName) || (entries[0].prod && entries[0].prod.partner_name) || "Partner";
        let grossNaira = 0;
        let commNaira = 0;
        entries.forEach(function(e) {
          const line = Number(e.item.unitPrice || 0) * Number(e.item.qty || 1);
          const pctRaw = Number(e.item.commissionPct || (e.prod && e.prod.partner_commission_percent) || (org && org.stipulatedCommissionPercent) || 10);
          const pct = Math.max(0, Math.min(95, pctRaw));
          grossNaira += line;
          commNaira += line * (pct / 100);
        });
        const rawNet = Math.max(0, grossNaira - commNaira);
        const dueHost = (org && org.autoDeductHostingFromPaystackSplit !== false) ? Math.max(0, Number(org.pendingHostingFeeBalance || 0)) : 0;
        const hostOffset = Math.min(rawNet, dueHost);
        const netNaira = Math.max(0, rawNet - hostOffset);
        const subCode = org && org.paystackSubaccountCode ? String(org.paystackSubaccountCode).trim() : "";

        totalCommission += commNaira;
        totalHostingOffset += hostOffset;
        totalPartnerNet += netNaira;

        allocations.push({
          partner_id: pid,
          partner_name: partnerName,
          subaccount: subCode,
          partner_net_kobo: Math.max(0, Math.round(netNaira * 100)),
          commission_kobo: Math.max(0, Math.round(commNaira * 100)),
          hosting_offset_kobo: Math.max(0, Math.round(hostOffset * 100)),
          partnerNetNaira: netNaira
        });
      });

      const active = allocations.filter(function(a) { return a.subaccount.indexOf("ACCT_") === 0 && a.partner_net_kobo >= 100; });
      const splitMode = active.length >= 2 ? "PARTNER_MULTI_SPLIT" : (active.length === 1 ? "PARTNER_SUBACCOUNT" : "STANDARD");
      const amountKobo = Math.max(100, Math.round(totalAmountNaira * 100));
      const odiyaShareNaira = Math.max(0, totalAmountNaira - totalPartnerNet);
      return {
        splitMode: splitMode,
        amountKobo: amountKobo,
        odiyaShareKobo: Math.max(0, Math.round(odiyaShareNaira * 100)),
        odiyaCommissionKobo: Math.max(0, Math.round(totalCommission * 100)),
        hostingFeeKobo: Math.max(0, Math.round(totalHostingOffset * 100)),
        partnerNetKobo: Math.max(0, Math.round(totalPartnerNet * 100)),
        odiyaShareNaira: odiyaShareNaira,
        odiyaCommissionNaira: totalCommission,
        hostingOffsetNaira: totalHostingOffset,
        totalPartnerNetNaira: totalPartnerNet,
        allocations: allocations,
        activeAllocations: active
      };
    }

    function changeCartQty(idx, delta) {
      if (!cart[idx]) return;
      cart[idx].qty = Number(cart[idx].qty || 1) + delta;
      if (cart[idx].qty <= 0) cart.splice(idx, 1);
      saveCart();
      renderCart();
    }

    function clearCart() {
      cart = [];
      saveCart();
      renderCart();
    }

    function toggleTransferHint() {
      const m = document.getElementById("coPayment").value;
      document.getElementById("transferSenderRow").style.display = (m === "PAYSTACK") ? "none" : "block";
    }

    function renderCart() {
      const c = document.getElementById("cartItemsContainer");
      const co = document.getElementById("checkoutSection");
      if (!cart.length) {
        c.innerHTML = '<div class="empty">Your cart is empty. Browse the Shop tab to add items!</div>';
        co.style.display = "none";
        return;
      }
      co.style.display = "block";
      let total = 0;
      c.innerHTML = cart.map(function(item, idx) {
        const line = Number(item.unitPrice) * Number(item.qty);
        total += line;
        return '<div class="cart-row">' +
          '<div style="flex:1;min-width:180px;">' +
            '<div style="font-weight:900;font-size:15px;">' + escapeHtml(item.title) + '</div>' +
            '<div style="font-size:12px;color:#A7F3D0;">' + escapeHtml(item.brand + ' • Size ' + item.size + ' • ' + item.color) + '</div>' +
            '<div style="font-size:14px;font-weight:800;color:#FDE68A;margin-top:2px;">₦' + Math.round(line).toLocaleString("en-NG") + '</div>' +
          '</div>' +
          '<div class="qty-box">' +
            '<button class="qty-btn" onclick="changeCartQty(' + idx + ', -1)">-</button>' +
            '<span style="font-weight:900;min-width:20px;text-align:center;">' + item.qty + '</span>' +
            '<button class="qty-btn" onclick="changeCartQty(' + idx + ', 1)">+</button>' +
          '</div>' +
        '</div>';
      }).join("");
      document.getElementById("cartTotalText").innerText = "₦" + Math.round(total).toLocaleString("en-NG");

      const splitBanner = document.getElementById("webSplitBanner");
      if (splitBanner) {
        const sp = computeWebPaystackSplit();
        if (sp.allocations.length > 0) {
          splitBanner.style.display = "block";
          splitBanner.innerText = (sp.activeAllocations.length > 0 ? "⚡ Paystack Auto-Split: " : "Partner Split Summary: ") +
            "ODIYA ₦" + Math.round(sp.odiyaShareNaira).toLocaleString("en-NG") +
            " • Partner Net ₦" + Math.round(sp.totalPartnerNetNaira).toLocaleString("en-NG");
        } else {
          splitBanner.style.display = "none";
        }
      }

      applyPermanentCustomerCredentials();
    }

    function getSavedCustomerCredentials() {
      return {
        name: (localStorage.getItem("odiya_customer_name") || "").trim(),
        phone: (localStorage.getItem("odiya_customer_phone") || "").trim(),
        address: (localStorage.getItem("odiya_customer_addr") || "").trim()
      };
    }

    function renderSavedCredentialsCard() {
      const creds = getSavedCustomerCredentials();
      const summary = document.getElementById("savedCredentialsSummary");
      const editor = document.getElementById("credentialsEditorForm");
      const toggleBtn = document.getElementById("toggleCredEditBtn");
      if (!summary) return;
      if (creds.name || creds.phone || creds.address) {
        const parts = [];
        if (creds.name) parts.push("👤 " + escapeHtml(creds.name));
        if (creds.phone) parts.push("📞 " + escapeHtml(creds.phone));
        if (creds.address) parts.push("📍 " + escapeHtml(creds.address));
        summary.innerHTML = parts.join(" &nbsp;•&nbsp; ");
        if (toggleBtn) toggleBtn.innerText = (editor && editor.style.display === "block") ? "Hide" : "✏️ Edit";
      } else {
        summary.innerHTML = 'No saved credentials yet — enter your <strong>Name</strong>, <strong>Phone</strong> &amp; <strong>Address</strong> once to keep them permanently on this storefront.';
        if (editor) editor.style.display = "block";
        if (toggleBtn) toggleBtn.innerText = "Hide";
      }
      const credName = document.getElementById("credName");
      const credPhone = document.getElementById("credPhone");
      const credAddress = document.getElementById("credAddress");
      if (credName && document.activeElement !== credName) credName.value = creds.name;
      if (credPhone && document.activeElement !== credPhone) credPhone.value = creds.phone;
      if (credAddress && document.activeElement !== credAddress) credAddress.value = creds.address;
    }

    function applyPermanentCustomerCredentials() {
      const creds = getSavedCustomerCredentials();
      const targets = [
        ["coName", creds.name],
        ["coPhone", creds.phone],
        ["coAddress", creds.address],
        ["poName", creds.name],
        ["poPhone", creds.phone],
        ["poLookupPhone", creds.phone],
        ["trackQuery", creds.phone]
      ];
      targets.forEach(function(pair) {
        const el = document.getElementById(pair[0]);
        if (el && !el.value && pair[1] && document.activeElement !== el) {
          el.value = pair[1];
        }
      });
      renderSavedCredentialsCard();
    }

    function syncPermanentCustomerCredentials(source) {
      const nameEl = document.getElementById(source === "po" ? "poName" : "coName");
      const phoneEl = document.getElementById(source === "po" ? "poPhone" : "coPhone");
      const addrEl = document.getElementById("coAddress");
      if (nameEl && nameEl.value.trim()) localStorage.setItem("odiya_customer_name", nameEl.value.trim());
      if (phoneEl && phoneEl.value.trim()) localStorage.setItem("odiya_customer_phone", phoneEl.value.trim());
      if (addrEl && addrEl.value.trim()) localStorage.setItem("odiya_customer_addr", addrEl.value.trim());
      renderSavedCredentialsCard();
    }

    function toggleCredentialsEditor() {
      const editor = document.getElementById("credentialsEditorForm");
      const toggleBtn = document.getElementById("toggleCredEditBtn");
      if (!editor) return;
      const open = editor.style.display !== "block";
      editor.style.display = open ? "block" : "none";
      if (toggleBtn) toggleBtn.innerText = open ? "Hide" : "✏️ Edit";
    }

    function savePermanentCredentialsForm() {
      const name = (document.getElementById("credName").value || "").trim();
      const phone = (document.getElementById("credPhone").value || "").trim();
      const address = (document.getElementById("credAddress").value || "").trim();
      if (!name || !phone) {
        showGlobalAlert("Please enter at least your Full Name and Phone Number.", false);
        return;
      }
      localStorage.setItem("odiya_customer_name", name);
      localStorage.setItem("odiya_customer_phone", phone);
      localStorage.setItem("odiya_customer_addr", address);
      const coName = document.getElementById("coName");
      const coPhone = document.getElementById("coPhone");
      const coAddr = document.getElementById("coAddress");
      const poName = document.getElementById("poName");
      const poPhone = document.getElementById("poPhone");
      const poLookup = document.getElementById("poLookupPhone");
      const trk = document.getElementById("trackQuery");
      if (coName) coName.value = name;
      if (coPhone) coPhone.value = phone;
      if (coAddr) coAddr.value = address;
      if (poName) poName.value = name;
      if (poPhone) poPhone.value = phone;
      if (poLookup) poLookup.value = phone;
      if (trk) trk.value = phone;
      const editor = document.getElementById("credentialsEditorForm");
      if (editor) editor.style.display = "none";
      renderSavedCredentialsCard();
      showGlobalAlert("✅ Customer credentials saved permanently on this storefront!", true);
    }

    async function submitCheckout() {
      if (navigator.onLine === false) {
        showGlobalAlert("You are offline. Reconnect to the internet to place your order.", false);
        return;
      }
      if (!cart.length) {
        showGlobalAlert("Your cart is empty.", false);
        return;
      }
      const name = document.getElementById("coName").value.trim();
      const phone = document.getElementById("coPhone").value.trim();
      const address = document.getElementById("coAddress").value.trim();
      const method = document.getElementById("coPayment").value;
      const sender = document.getElementById("coSender").value.trim();

      if (!name || !phone || !address) {
        showGlobalAlert("Please enter your full name, phone number, and delivery address.", false);
        return;
      }

      const total = cart.reduce(function(s, i) { return s + Number(i.unitPrice) * Number(i.qty); }, 0);
      const itemsSummary = cart.map(function(i) { return i.qty + "x " + i.title + " (" + i.brand + ", " + i.size + ", " + i.color + ")"; }).join("; ");
      const orderId = "ORD-WEB-" + Date.now().toString().slice(-8);
      const custId = "web-" + phone.replace(/\D/g, "").slice(-10);
      const phoneDigits = phone.replace(/\D/g, "").slice(-11) || orderId.slice(-10).toLowerCase();
      const paystackReceiptEmail = "customer." + phoneDigits + "@odiya.store";
      const ref = method === "PAYSTACK"
        ? ("PSKODY-" + orderId.slice(-7) + "-" + Math.random().toString(36).slice(2, 8).toUpperCase())
        : (sender || ("TRF-" + orderId.slice(-6)));

      const btn = document.getElementById("placeOrderBtn");
      btn.disabled = true;
      btn.innerText = "⏳ Submitting Order...";

      try {
        localStorage.setItem("odiya_customer_name", name);
        localStorage.setItem("odiya_customer_phone", phone);
        localStorage.setItem("odiya_customer_addr", address);
        localStorage.setItem("odiya_last_order_id", orderId);
        applyPermanentCustomerCredentials();

        await fetch(sbUrl + "/rest/v1/customers?on_conflict=id", {
          method: "POST",
          headers: sbHeaders({ "Content-Type": "application/json", "Prefer": "resolution=merge-duplicates,return=minimal" }),
          body: JSON.stringify({
            id: custId,
            full_name: name,
            email: "",
            phone_number: phone,
            delivery_address: address,
            created_at: Date.now()
          })
        });

        const orderPayload = {
          order_id: orderId,
          customer_id: custId,
          customer_name: name,
          customer_email: "",
          customer_phone: phone,
          delivery_address: address,
          items_summary: itemsSummary,
          total_amount: Math.round(total),
          payment_method: method,
          payment_account: method === "PAYSTACK" ? "Paystack Hosted Checkout" : (method + " Merchant"),
          payment_reference: ref,
          sender_name: sender || name,
          status: "PENDING_VERIFICATION",
          verification_note: method === "PAYSTACK" ? "Awaiting Paystack confirmation" : "Web transfer submitted — pending Admin verification",
          created_at: Date.now()
        };
        const ordRes = await fetch(sbUrl + "/rest/v1/orders?on_conflict=order_id", {
          method: "POST",
          headers: sbHeaders({ "Content-Type": "application/json", "Prefer": "resolution=merge-duplicates,return=minimal" }),
          body: JSON.stringify(orderPayload)
        });
        if (!ordRes.ok) {
          const errTxt = await ordRes.text();
          throw new Error("Order save failed (HTTP " + ordRes.status + "): " + errTxt.slice(0, 140));
        }

        if (method === "PAYSTACK") {
          btn.innerText = "🔒 Redirecting to Paystack...";
          const cleanBase = window.location.href.split("?")[0];
          const splitPlan = computeWebPaystackSplit();
          const primaryAlloc = splitPlan.activeAllocations[0] || splitPlan.allocations[0] || null;
          const psBody = {
            order_id: orderId,
            reference: ref,
            amount: splitPlan.amountKobo,
            amount_naira: total,
            currency: "NGN",
            email: paystackReceiptEmail,
            callback_url: cleanBase + "?paystack_ref=" + encodeURIComponent(ref) + "&order_id=" + encodeURIComponent(orderId),
            purpose: "ORDER_CHECKOUT",
            split_mode: splitPlan.splitMode,
            bearer: "account",
            metadata: {
              order_id: orderId,
              purpose: "ORDER_CHECKOUT",
              split_mode: splitPlan.splitMode,
              odiya_share_kobo: splitPlan.odiyaShareKobo,
              odiya_commission_kobo: splitPlan.odiyaCommissionKobo,
              hosting_fee_kobo: splitPlan.hostingFeeKobo,
              partner_net_kobo: splitPlan.partnerNetKobo,
              customer_name: name,
              customer_phone: phone,
              platform: "web_ios_safari"
            }
          };
          if (primaryAlloc) {
            psBody.partner_id = primaryAlloc.partner_id;
            psBody.partner_name = primaryAlloc.partner_name;
            psBody.metadata.partner_id = primaryAlloc.partner_id;
            psBody.metadata.partner_name = primaryAlloc.partner_name;
          }
          if (splitPlan.activeAllocations.length > 0) {
            psBody.odiya_share_kobo = splitPlan.odiyaShareKobo;
            psBody.odiya_commission_kobo = splitPlan.odiyaCommissionKobo;
            psBody.hosting_fee_kobo = splitPlan.hostingFeeKobo;
            psBody.split_allocations = splitPlan.activeAllocations;
            if (splitPlan.activeAllocations.length === 1) {
              const single = splitPlan.activeAllocations[0];
              psBody.subaccount = single.subaccount;
              const txCharge = Math.max(0, splitPlan.amountKobo - single.partner_net_kobo);
              if (txCharge > 0) psBody.transaction_charge = txCharge;
            }
          }
          const initRes = await fetch(sbUrl + "/functions/v1/paystack-init", {
            method: "POST",
            headers: sbHeaders({ "Content-Type": "application/json" }),
            body: JSON.stringify(psBody)
          });
          const initJson = await initRes.json().catch(function() { return {}; });
          const authUrl = (initJson.data && initJson.data.authorization_url) || initJson.authorization_url || "";
          if (initRes.ok && authUrl && authUrl.startsWith("https://")) {
            clearCart();
            window.location.href = authUrl;
            return;
          } else {
            clearCart();
            switchTab("track");
            showGlobalAlert("Order " + orderId + " saved, but Paystack checkout is not ready yet (" + (initJson.message || ("HTTP " + initRes.status)) + "). You can track your order below.", false);
            return;
          }
        } else {
          clearCart();
          switchTab("track");
          showGlobalAlert("✅ Order " + orderId + " sent to Admin! Track status anytime below.", true);
        }
      } catch (err) {
        showGlobalAlert("❌ " + err.message, false);
      } finally {
        btn.disabled = false;
        btn.innerText = "🔒 Complete Checkout & Pay Now";
      }
    }

    async function verifyPaystackCallbackIfPresent() {
      const ref = params.get("reference") || params.get("trxref") || params.get("paystack_ref");
      if (!ref || !sbUrl || !sbKey) return;
      try {
        const res = await fetch(sbUrl + "/functions/v1/paystack-verify", {
          method: "POST",
          headers: sbHeaders({ "Content-Type": "application/json" }),
          body: JSON.stringify({ reference: ref })
        });
        const json = await res.json().catch(function() { return {}; });
        const status = String((json.data && json.data.status) || "").toLowerCase();
        switchTab("track");
        if (status === "success") {
          showGlobalAlert("✅ Paystack payment verified (Ref: " + ref + ")! Your order is CONFIRMED.", true);
        } else {
          showGlobalAlert("ℹ️ Paystack verification status for " + ref + ": " + (status || "pending") + ". Refresh tracking below shortly.", false);
        }
      } catch (err) {
        showGlobalAlert("Could not auto-verify Paystack reference " + ref + ": " + err.message, false);
      }
    }

    async function trackOrders() {
      const q = document.getElementById("trackQuery").value.trim();
      const box = document.getElementById("trackResults");
      if (!q) {
        box.innerHTML = '<div class="empty">Enter your phone number or Order ID first.</div>';
        return;
      }
      box.innerHTML = '<div class="empty">Checking live order status...</div>';
      try {
        const digits = q.replace(/\D/g, "");
        const filter = q.toUpperCase().startsWith("ORD")
          ? ("order_id=ilike.*" + encodeURIComponent(q) + "*")
          : ("customer_phone=ilike.*" + encodeURIComponent(digits.length >= 7 ? digits.slice(-10) : q) + "*");
        const res = await fetch(sbUrl + "/rest/v1/orders?" + filter + "&select=order_id,customer_name,items_summary,total_amount,status,payment_method,payment_reference,verification_note,dispatch_rider_whatsapp,created_at&order=created_at.desc.nullslast&limit=20", {
          headers: sbHeaders()
        });
        if (!res.ok) throw new Error("HTTP " + res.status);
        const rows = await res.json();
        if (!rows.length) {
          box.innerHTML = '<div class="empty">No matching orders found for "' + escapeHtml(q) + '".</div>';
          return;
        }
        box.innerHTML = rows.map(function(o) {
          const st = String(o.status || "PENDING_VERIFICATION");
          const badgeClass = st === "DELIVERED" || st === "CONFIRMED" ? "badge-ok" : (st === "CANCELLED" ? "" : "badge-warn");
          const rider = (o.dispatch_rider_whatsapp || "").replace(/\D/g, "");
          const riderHtml = rider
            ? '<a class="btn btn-green btn-sm" style="margin-top:8px;" target="_blank" rel="noopener" href="https://wa.me/' + escapeHtml(rider) + '">💬 WhatsApp Dispatch Rider</a>'
            : '';
          return '<div class="order-card">' +
            '<div style="display:flex;justify-content:space-between;align-items:center;gap:8px;flex-wrap:wrap;margin-bottom:6px;">' +
              '<strong>' + escapeHtml(o.order_id) + '</strong>' +
              '<span class="badge ' + badgeClass + '">' + escapeHtml(st.replace(/_/g, " ")) + '</span>' +
            '</div>' +
            '<div style="font-size:13px;color:#A7F3D0;margin-bottom:4px;">' + escapeHtml(o.items_summary || "Items") + '</div>' +
            '<div style="font-size:14px;font-weight:900;color:#FDE68A;">₦' + Math.round(Number(o.total_amount || 0)).toLocaleString("en-NG") + ' • ' + escapeHtml(o.payment_method || "") + '</div>' +
            (o.verification_note ? '<div style="font-size:12px;color:#FFFFFF;margin-top:4px;">Note: ' + escapeHtml(o.verification_note) + '</div>' : '') +
            riderHtml +
          '</div>';
        }).join("");
      } catch (err) {
        box.innerHTML = '<div class="empty">Could not fetch orders: ' + escapeHtml(err.message) + '</div>';
      }
    }

    async function mirrorPreOrderToStorage(payloadSnake) {
      const camelObj = {
        id: payloadSnake.id,
        customerId: payloadSnake.customer_id || "guest",
        customerName: payloadSnake.customer_name || "Valued Customer",
        customerPhone: payloadSnake.customer_phone || "",
        productName: payloadSnake.product_name || "",
        brandName: payloadSnake.brand_name || "",
        category: payloadSnake.category || "Clothes",
        preferredSize: payloadSnake.preferred_size || "Standard",
        preferredColor: payloadSnake.preferred_color || "Any",
        targetBudget: Number(payloadSnake.target_budget || 0) || 0,
        description: payloadSnake.description || "",
        photoUrl: payloadSnake.photo_url || "",
        status: payloadSnake.status || "PENDING",
        createdAt: Number(payloadSnake.created_at || Date.now()),
        adminResponse: payloadSnake.admin_response || "",
        adminQuotedPrice: Number(payloadSnake.admin_quoted_price || 0) || 0,
        customerReply: payloadSnake.customer_reply || ""
      };
      const buckets = ["product-media", "shop-receipts"];
      let existing = [];
      for (let i = 0; i < buckets.length; i++) {
        try {
          const r = await fetch(sbUrl + "/storage/v1/object/public/" + buckets[i] + "/system/pre_orders.json?t=" + Date.now());
          if (r.ok) {
            const arr = await r.json();
            if (Array.isArray(arr)) { existing = arr; break; }
          }
        } catch (_) {}
      }
      const merged = [camelObj].concat(existing.filter(function(item) { return item && item.id !== camelObj.id; }));
      for (let i = 0; i < buckets.length; i++) {
        try {
          const up = await fetch(sbUrl + "/storage/v1/object/" + buckets[i] + "/system/pre_orders.json", {
            method: "POST",
            headers: sbHeaders({ "Content-Type": "application/json", "x-upsert": "true" }),
            body: JSON.stringify(merged)
          });
          if (up.ok) return true;
        } catch (_) {}
      }
      return false;
    }

    async function submitPreOrder() {
      if (navigator.onLine === false) {
        showGlobalAlert("You are offline. Reconnect to send your pre-order request.", false);
        return;
      }
      const name = document.getElementById("poName").value.trim();
      const phone = document.getElementById("poPhone").value.trim();
      const product = document.getElementById("poProduct").value.trim();
      if (!name || !phone || !product) {
        showGlobalAlert("Please fill in your Name, Phone Number, and Product Name.", false);
        return;
      }
      const btn = document.getElementById("submitPreOrderBtn");
      btn.disabled = true;
      btn.innerText = "⏳ Sending to Admin...";
      try {
        localStorage.setItem("odiya_customer_name", name);
        localStorage.setItem("odiya_customer_phone", phone);
        applyPermanentCustomerCredentials();
        const preId = "PRE-WEB-" + Date.now().toString().slice(-8);
        const payload = {
          id: preId,
          customer_id: "web-" + phone.replace(/\D/g, "").slice(-10),
          customer_name: name,
          customer_phone: phone,
          product_name: product,
          brand_name: document.getElementById("poBrand").value.trim(),
          category: document.getElementById("poCategory").value,
          preferred_size: document.getElementById("poSize").value.trim() || "Standard",
          preferred_color: document.getElementById("poColor").value.trim() || "Any",
          target_budget: Number(document.getElementById("poBudget").value || 0) || 0,
          description: document.getElementById("poDesc").value.trim(),
          photo_url: "",
          status: "PENDING",
          created_at: Date.now(),
          admin_response: "",
          admin_quoted_price: 0,
          customer_reply: ""
        };
        let tableOk = false;
        let tableErrMsg = "";
        try {
          const res = await fetch(sbUrl + "/rest/v1/pre_orders?on_conflict=id", {
            method: "POST",
            headers: sbHeaders({ "Content-Type": "application/json", "Prefer": "resolution=merge-duplicates,return=minimal" }),
            body: JSON.stringify(payload)
          });
          if (res.ok) {
            tableOk = true;
          } else {
            const errTxt = await res.text();
            tableErrMsg = "HTTP " + res.status + ": " + errTxt.slice(0, 120);
          }
        } catch (netErr) {
          tableErrMsg = netErr.message;
        }
        const storageOk = await mirrorPreOrderToStorage(payload);
        if (!tableOk && !storageOk) {
          throw new Error(tableErrMsg || "Pre-order cloud endpoint rejected write.");
        }
        document.getElementById("poProduct").value = "";
        document.getElementById("poDesc").value = "";
        document.getElementById("poLookupPhone").value = phone;
        showGlobalAlert("✅ Pre-Order (" + preId + ") sent directly to Admin!", true);
        loadMyPreOrders();
      } catch (err) {
        showGlobalAlert("❌ Pre-order failed: " + err.message, false);
      } finally {
        btn.disabled = false;
        btn.innerText = "🚀 Send Pre-Order Request to Admin";
      }
    }

    async function loadMyPreOrders() {
      const q = document.getElementById("poLookupPhone").value.trim();
      const box = document.getElementById("preOrderResults");
      if (!q) {
        box.innerHTML = '<div class="empty">Enter your phone number to view pre-order quotes.</div>';
        return;
      }
      box.innerHTML = '<div class="empty">Loading pre-order status...</div>';
      try {
        const digits = q.replace(/\D/g, "");
        const needle = digits.length >= 7 ? digits.slice(-10) : q;
        let rows = [];
        try {
          const res = await fetch(sbUrl + "/rest/v1/pre_orders?customer_phone=ilike.*" + encodeURIComponent(needle) + "*&select=id,product_name,brand_name,status,target_budget,admin_response,admin_quoted_price,created_at&order=created_at.desc.nullslast&limit=20", {
            headers: sbHeaders()
          });
          if (res.ok) rows = await res.json();
        } catch (_) {}
        if (!rows.length) {
          const buckets = ["product-media", "shop-receipts"];
          for (let i = 0; i < buckets.length; i++) {
            try {
              const sRes = await fetch(sbUrl + "/storage/v1/object/public/" + buckets[i] + "/system/pre_orders.json?t=" + Date.now());
              if (sRes.ok) {
                const arr = await sRes.json();
                if (Array.isArray(arr)) {
                  rows = arr.filter(function(item) {
                    const p = String((item && (item.customerPhone || item.customer_phone)) || "").replace(/\D/g, "");
                    return needle && p.indexOf(needle) >= 0;
                  }).map(function(item) {
                    return {
                      id: item.id,
                      product_name: item.productName || item.product_name,
                      brand_name: item.brandName || item.brand_name,
                      status: item.status || "PENDING",
                      target_budget: item.targetBudget || item.target_budget || 0,
                      admin_response: item.adminResponse || item.admin_response || "",
                      admin_quoted_price: item.adminQuotedPrice || item.admin_quoted_price || 0
                    };
                  });
                  if (rows.length) break;
                }
              }
            } catch (_) {}
          }
        }
        if (!rows.length) {
          box.innerHTML = '<div class="empty">No pre-orders found for this phone number yet.</div>';
          return;
        }
        box.innerHTML = rows.map(function(po) {
          const quote = Number(po.admin_quoted_price || 0);
          return '<div class="order-card">' +
            '<div style="display:flex;justify-content:space-between;align-items:center;gap:8px;flex-wrap:wrap;margin-bottom:4px;">' +
              '<strong>' + escapeHtml(po.product_name) + '</strong>' +
              '<span class="badge">' + escapeHtml(po.status || "PENDING") + '</span>' +
            '</div>' +
            (po.brand_name ? '<div style="font-size:12px;color:#A7F3D0;">Brand: ' + escapeHtml(po.brand_name) + '</div>' : '') +
            (quote > 0 ? '<div style="font-size:14px;font-weight:900;color:#FDE68A;margin-top:4px;">Admin Quoted Price: ₦' + Math.round(quote).toLocaleString("en-NG") + '</div>' : '') +
            (po.admin_response ? '<div style="font-size:13px;color:#FFFFFF;margin-top:4px;">Admin Reply: ' + escapeHtml(po.admin_response) + '</div>' : '<div style="font-size:12px;color:#A7F3D0;margin-top:4px;">Waiting for Admin quote...</div>') +
          '</div>';
        }).join("");
      } catch (err) {
        box.innerHTML = '<div class="empty">Could not load pre-orders: ' + escapeHtml(err.message) + '</div>';
      }
    }

    applyPermanentCustomerCredentials();
    updateWishlistBadge();
    renderWishlist();
    updateCartBadge();
    loadLiveCatalog();
    verifyPaystackCallbackIfPresent();
  </script>
</body>
</html>
        """.trimIndent()
    }

    fun writePortalToUri(
        context: Context,
        targetUri: Uri,
        sbUrl: String,
        sbKey: String,
        storeName: String = "ODIYA",
        storePhone: String = "",
        storeAddress: String = "",
        storeEmail: String = "",
        apkDownloadUrl: String = ""
    ): Boolean {
        return try {
            val html = buildStorePortalHtml(
                sbUrl = sbUrl,
                sbKey = sbKey,
                storeName = storeName,
                storePhone = storePhone,
                storeAddress = storeAddress,
                storeEmail = storeEmail,
                apkDownloadUrl = apkDownloadUrl
            )
            context.contentResolver.openOutputStream(targetUri, "wt")?.use { out ->
                out.write(html.toByteArray(Charsets.UTF_8))
                out.flush()
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Saves index.html directly to the phone's public Downloads/odiya_portal/ folder on Android 10+
     * with zero storage permissions so the user can find it immediately in the Files app -> Downloads.
     */
    fun savePortalToDownloads(
        context: Context,
        sbUrl: String,
        sbKey: String,
        storeName: String = "ODIYA",
        storePhone: String = "",
        storeAddress: String = "",
        storeEmail: String = "",
        apkDownloadUrl: String = ""
    ): String? {
        val html = buildStorePortalHtml(
            sbUrl = sbUrl,
            sbKey = sbKey,
            storeName = storeName,
            storePhone = storePhone,
            storeAddress = storeAddress,
            storeEmail = storeEmail,
            apkDownloadUrl = apkDownloadUrl
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val resolver = context.contentResolver
                val relPath = "${Environment.DIRECTORY_DOWNLOADS}/odiya_portal"
                val existingUri = resolver.query(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Downloads._ID),
                    "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH}=?",
                    arrayOf("index.html", "$relPath/"),
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(0)
                        Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id.toString())
                    } else null
                }

                val targetUri = existingUri ?: resolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, "index.html")
                        put(MediaStore.MediaColumns.MIME_TYPE, "text/html")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, relPath)
                    }
                )

                if (targetUri != null) {
                    resolver.openOutputStream(targetUri, "wt")?.use { out ->
                        out.write(html.toByteArray(Charsets.UTF_8))
                        out.flush()
                    }
                    return "Downloads/odiya_portal/index.html"
                }
            } catch (_: Exception) {
                // Fall back to app-scoped filesDir share
            }
        }
        return null
    }

    fun exportAndSharePortalHtml(
        context: Context,
        sbUrl: String,
        sbKey: String,
        storeName: String = "ODIYA",
        storePhone: String = "",
        storeAddress: String = "",
        storeEmail: String = "",
        apkDownloadUrl: String = ""
    ) {
        try {
            val html = buildStorePortalHtml(
                sbUrl = sbUrl,
                sbKey = sbKey,
                storeName = storeName,
                storePhone = storePhone,
                storeAddress = storeAddress,
                storeEmail = storeEmail,
                apkDownloadUrl = apkDownloadUrl
            )
            val savedPath = savePortalToDownloads(
                context = context,
                sbUrl = sbUrl,
                sbKey = sbKey,
                storeName = storeName,
                storePhone = storePhone,
                storeAddress = storeAddress,
                storeEmail = storeEmail,
                apkDownloadUrl = apkDownloadUrl
            )
            if (savedPath != null) {
                Toast.makeText(
                    context,
                    "✅ Saved to phone: $savedPath (open Files app → Downloads)",
                    Toast.LENGTH_LONG
                ).show()
            }

            val dir = File(context.filesDir, "web_portal").apply { if (!exists()) mkdirs() }
            val htmlFile = File(dir, "index.html")
            htmlFile.writeText(html, Charsets.UTF_8)

            val uri = FileProvider.getUriForFile(
                context,
                "$APP_PACKAGE.fileprovider",
                htmlFile
            )
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/html"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "ODIYA Live Web Portal (index.html)")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Upload this index.html file to your GitHub repository and enable GitHub Pages (Settings -> Pages) to get your instant free https://username.github.io/ store web address!"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(sendIntent, "Save or Upload index.html Web Portal"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not export Web Portal HTML: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
