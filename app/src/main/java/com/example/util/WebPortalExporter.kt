package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
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

    fun openExternalUrl(context: Context, url: String) {
        val target = normalizeWebUrl(url)
        if (target.isBlank()) return
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target)).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open browser: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Builds a complete, standalone, mobile-friendly HTTPS-ready `index.html` storefront & deep-link page
     * with the store's Supabase URL & Publishable Key baked directly inside.
     * Hosting this single `index.html` on GitHub Pages creates a live 200 OK web address that never returns 404.
     */
    fun buildStorePortalHtml(
        sbUrl: String,
        sbKey: String,
        storeName: String = "24/7 SHOPPING",
        storePhone: String = "",
        storeAddress: String = "",
        storeEmail: String = "",
        apkDownloadUrl: String = ""
    ): String {
        val escapedSbUrl = JSONObject.quote(sbUrl.trim())
        val escapedSbKey = JSONObject.quote(sbKey.trim())
        val escapedPkg = JSONObject.quote(APP_PACKAGE)
        val cleanApkUrl = normalizeWebUrl(apkDownloadUrl)
        val escapedApkUrl = JSONObject.quote(cleanApkUrl)
        val cleanStoreName = storeName.trim().ifBlank { "24/7 SHOPPING" }
        val safeHtmlName = cleanStoreName
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
        val contactParts = listOfNotNull(
            storePhone.trim().takeIf { it.isNotBlank() }?.let { "📞 $it" },
            storeAddress.trim().takeIf { it.isNotBlank() }?.let { "📍 $it" },
            storeEmail.trim().takeIf { it.isNotBlank() }?.let { "✉️ $it" }
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
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <meta name="theme-color" content="#064E3B" />
  <meta name="apple-mobile-web-app-capable" content="yes" />
  <meta name="apple-mobile-web-app-title" content="24/7 SHOPPING" />
  <title>24/7 SHOPPING — Official Store Portal & App Link</title>
  <link rel="manifest" id="pwaManifestLink" href="" />
  <style>
    :root {
      --bg: #052E16;
      --card: rgba(6, 78, 59, 0.94);
      --border: rgba(74, 222, 128, 0.4);
      --cyan: #4ADE80;
      --gold: #F59E0B;
      --green: #16A34A;
      --text: #FFFFFF;
      --muted: #FFFFFF;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
    html, body { max-width: 100%; overflow-x: hidden; }
    img, video, iframe, embed, object { max-width: 100%; height: auto; }
    body {
      background: radial-gradient(circle at top right, #15803D 0%, #064E3B 48%, #052E16 100%);
      color: #FFFFFF;
      min-height: 100vh;
      padding: 16px 10px 48px;
    }
    .wrap { max-width: 920px; width: 100%; margin: 0 auto; min-width: 0; }
    .hero {
      background: var(--card);
      border: 1.5px solid var(--border);
      border-radius: 24px;
      padding: 24px 20px;
      text-align: center;
      box-shadow: 0 20px 50px rgba(0, 0, 0, 0.4);
      margin-bottom: 18px;
      max-width: 100%;
      min-width: 0;
    }
    .pill {
      display: inline-block;
      background: rgba(22, 163, 74, 0.35);
      border: 1px solid rgba(255, 255, 255, 0.45);
      color: #FFFFFF;
      font-size: 13px;
      font-weight: 800;
      padding: 6px 14px;
      border-radius: 999px;
      margin-bottom: 10px;
      max-width: 100%;
      word-break: break-word;
    }
    h1 { font-size: 26px; font-weight: 900; letter-spacing: 0.4px; margin-bottom: 6px; color: #FFFFFF; }
    .sub { color: #FFFFFF; font-size: 15px; font-weight: 600; margin-bottom: 16px; overflow-wrap: anywhere; }
    .actions {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(min(220px, 100%), 1fr));
      gap: 10px;
      margin-top: 12px;
      width: 100%;
      max-width: 100%;
      min-width: 0;
    }
    .btn {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      min-height: 48px;
      min-width: 0;
      max-width: 100%;
      padding: 12px 16px;
      border-radius: 14px;
      border: none;
      font-size: 15px;
      font-weight: 800;
      color: #FFFFFF;
      text-decoration: none;
      text-align: center;
      white-space: normal;
      word-break: break-word;
      overflow-wrap: anywhere;
      cursor: pointer;
      transition: transform 0.15s ease, opacity 0.15s ease;
    }
    .btn:active { transform: scale(0.98); }
    .btn-cyan { background: linear-gradient(135deg, #22C55E, #15803D); color: #FFFFFF; }
    .btn-green { background: linear-gradient(135deg, #10B981, #047857); color: #FFFFFF; }
    .btn-gold { background: linear-gradient(135deg, #16A34A, #166534); color: #FFFFFF; border: 1px solid rgba(255, 255, 255, 0.35); }
    .btn-dark { background: rgba(255, 255, 255, 0.12); color: #FFFFFF; border: 1px solid rgba(255, 255, 255, 0.3); }
    .status-note {
      margin-top: 12px;
      padding: 12px 14px;
      border-radius: 12px;
      background: rgba(22, 163, 74, 0.22);
      border: 1px solid rgba(255, 255, 255, 0.3);
      font-size: 14px;
      color: #FFFFFF;
      line-height: 1.5;
      text-align: left;
      overflow-wrap: anywhere;
    }
    .catalog-card {
      background: var(--card);
      border: 1px solid rgba(255, 255, 255, 0.22);
      border-radius: 22px;
      padding: 18px;
      max-width: 100%;
      min-width: 0;
    }
    .catalog-head {
      display: flex;
      flex-wrap: wrap;
      gap: 10px;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 14px;
      min-width: 0;
    }
    .catalog-head h2 { font-size: 18px; font-weight: 900; color: #FFFFFF; }
    .search {
      width: 100%;
      max-width: 340px;
      min-height: 44px;
      padding: 10px 14px;
      border-radius: 12px;
      border: 1px solid rgba(255, 255, 255, 0.35);
      background: rgba(5, 46, 22, 0.85);
      color: #FFFFFF;
      font-size: 16px;
      outline: none;
    }
    .search::placeholder { color: rgba(255, 255, 255, 0.8); }
    .grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(min(175px, 100%), 1fr));
      gap: 12px;
      width: 100%;
      max-width: 100%;
      min-width: 0;
    }
    .prod {
      background: rgba(4, 120, 87, 0.55);
      border: 1px solid rgba(255, 255, 255, 0.2);
      border-radius: 16px;
      overflow: hidden;
      display: flex;
      flex-direction: column;
      min-width: 0;
    }
    .prod img {
      width: 100%;
      max-width: 100%;
      height: 150px;
      object-fit: cover;
      background: #052E16;
      display: block;
    }
    .prod-body { padding: 12px; flex: 1; display: flex; flex-direction: column; justify-content: space-between; min-width: 0; }
    .prod-cat { font-size: 13px; color: #FFFFFF; opacity: 0.9; font-weight: 800; text-transform: uppercase; margin-bottom: 4px; overflow-wrap: anywhere; }
    .prod-title { font-size: 15px; color: #FFFFFF; font-weight: 800; line-height: 1.3; margin-bottom: 6px; overflow-wrap: anywhere; }
    .prod-price { font-size: 15px; font-weight: 900; color: #FFFFFF; overflow-wrap: anywhere; }
    .empty { text-align: center; padding: 28px 12px; color: #FFFFFF; font-size: 15px; overflow-wrap: anywhere; }
    .foot { text-align: center; margin-top: 20px; font-size: 14px; color: #FFFFFF; overflow-wrap: anywhere; }

    @media (max-width: 639px) {
      body { padding: 10px 8px 36px; font-size: 16px; }
      .hero { padding: 18px 14px; border-radius: 20px; }
      .catalog-card { padding: 14px 12px; border-radius: 18px; }
      .pill, .sub, .status-note, .prod-cat, .prod-title, .prod-price, .empty, .foot, .search, .btn { font-size: 16px; }
      .actions { grid-template-columns: minmax(0, 1fr); }
      .btn { font-size: 16px; min-height: 48px; padding: 12px 14px; }
      .search { width: 100%; max-width: 100%; min-height: 44px; font-size: 16px; }
      .grid { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
      .prod img { height: 140px; }
    }
    @media (max-width: 479px) {
      .grid { grid-template-columns: minmax(0, 1fr); gap: 12px; }
      .prod img { height: 180px; }
    }
  </style>
</head>
<body>
  <div class="wrap">
    <div class="hero">
      <div class="pill">✅ $safeHtmlName • OFFICIAL LIVE STORE PORTAL</div>
      <h1>$safeHtmlName</h1>
      <div class="sub">Open Installed App, Connect Store Sync, or Browse Live Storefront Catalog</div>

      <div class="actions">
        <a id="openAppBtn" href="#" class="btn btn-cyan">⚡ Open $safeHtmlName App & Connect</a>
        <button id="copySyncBtn" class="btn btn-green" onclick="copySyncLink()">📋 Copy Store Sync Link</button>
        <a id="downloadApkBtn" href="#" class="btn btn-gold" style="display:none;">⬇️ Download Android APK</a>
        <button id="installWebBtn" class="btn btn-dark" onclick="installWebApp()">💻 Install as Web App</button>
      </div>

      <div class="status-note" id="syncStatusNote">
        📱 <strong>Already have the $safeHtmlName APK installed?</strong> Tap <strong>"Open $safeHtmlName App & Connect"</strong> above. If your phone browser asks for confirmation or doesn't auto-switch, tap <strong>"Copy Store Sync Link"</strong>, open the <strong>$safeHtmlName</strong> app → <strong>Account</strong> → <strong>Link Now</strong> → <strong>Paste</strong>.
      </div>
    </div>

    <div class="catalog-card">
      <div class="catalog-head">
        <h2>🛍️ Live Storefront Catalog</h2>
        <input class="search" id="searchBox" type="search" placeholder="Search shoes, clothes, gadgets..." oninput="renderProducts()" />
      </div>
      <div id="catalogContainer" class="empty">Connecting to live store inventory...</div>
    </div>

    <div class="foot">© $safeHtmlName • $safeHtmlContact</div>
  </div>

  <script>
    const EMBEDDED_SB_URL = $escapedSbUrl;
    const EMBEDDED_SB_KEY = $escapedSbKey;
    const ANDROID_PACKAGE = $escapedPkg;
    const APK_DOWNLOAD_URL = $escapedApkUrl;

    const params = new URLSearchParams(window.location.search);
    const sbUrl = (params.get("sb_url") || EMBEDDED_SB_URL || "").trim().replace(/\/+$/, "");
    const sbKey = (params.get("sb_key") || EMBEDDED_SB_KEY || "").trim();

    const apkBtn = document.getElementById("downloadApkBtn");
    const cleanApkUrl = (typeof APK_DOWNLOAD_URL === "string" ? APK_DOWNLOAD_URL.trim() : "");
    if (cleanApkUrl && /^https?:\/\/[^\s<>]+$/i.test(cleanApkUrl) && !cleanApkUrl.includes("<")) {
      apkBtn.href = cleanApkUrl;
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

    const openBtn = document.getElementById("openAppBtn");
    const ua = navigator.userAgent || "";
    const isAndroid = /Android/i.test(ua);
    const isAltBrowser = /OPR|Opera|Opera Mini|UCBrowser|PHX|SamsungBrowser|MiuiBrowser|VivoBrowser|HeyTapBrowser|YaBrowser|Firefox|FxiOS|EdgA|wv\b/i.test(ua);
    const isChromeAndroid = isAndroid && /\bChrome\/\d+/i.test(ua) && !isAltBrowser;
    openBtn.href = isChromeAndroid ? androidIntentLink : customSchemeLink;

    openBtn.addEventListener("click", function(e) {
      if (isAndroid && !isChromeAndroid) {
        e.preventDefault();
        let leftPage = false;
        const onHide = function() {
          if (document.hidden) leftPage = true;
        };
        document.addEventListener("visibilitychange", onHide, { once: true });
        try {
          window.location.href = customSchemeLink;
        } catch (_) {}
        setTimeout(function() {
          document.removeEventListener("visibilitychange", onHide);
          if (!leftPage && !document.hidden) {
            const note = document.getElementById("syncStatusNote");
            if (note) {
              note.innerHTML = "⚠️ <strong>App not installed yet (or browser blocked auto-open)?</strong><br/>&bull; New customer? Tap <strong>⬇️ Download Android APK</strong> above to install first.<br/>&bull; Already installed? Tap <strong>⋮ Menu &rarr; Open in Chrome</strong> (Opera Mini/in-app browsers block direct app switching), or tap <strong>📋 Copy Store Sync Link</strong> and paste inside <strong>Account &rarr; Link Now</strong>.";
            }
          }
        }, 1200);
      }
    });

    const stayOnWeb = params.get("web") === "1";
    const isHttpsOrigin = window.location.protocol === "https:" || window.location.protocol === "http:";
    if (isChromeAndroid && isHttpsOrigin && !window.AndroidBridge && !stayOnWeb && (params.get("open_app") === "1" || params.has("sb_url"))) {
      setTimeout(function() {
        try {
          window.location.href = androidIntentLink;
        } catch (_) {}
      }, 120);
    }

    if (!sbUrl || !sbKey) {
      document.getElementById("copySyncBtn").style.display = "none";
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
      name: "24/7 SHOPPING",
      short_name: "24/7 Shop",
      start_url: window.location.href,
      display: "standalone",
      background_color: "#052E16",
      theme_color: "#16A34A"
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
      } else {
        alert("Open Chrome/Edge menu (⋮) and tap 'Add to Home screen' or 'Install App' to save this storefront.");
      }
    }

    let allProducts = [];
    async function loadLiveCatalog() {
      const container = document.getElementById("catalogContainer");
      if (!sbUrl || !sbKey) {
        container.innerHTML = '<div class="empty">Open or install the 24/7 SHOPPING Android app to browse full 3D inventory and place orders.</div>';
        return;
      }
      try {
        const res = await fetch(sbUrl + "/rest/v1/products?select=id,title,category,brand,base_price,discount_percent,image_url&order=created_at.desc.nullslast&limit=40", {
          headers: { "apikey": sbKey, "Authorization": "Bearer " + sbKey }
        });
        if (!res.ok) throw new Error("HTTP " + res.status);
        allProducts = await res.json();
        renderProducts();
      } catch (err) {
        container.innerHTML = '<div class="empty">Live preview could not load (' + err.message + '). Tap "Open 24/7 Shopping App & Connect" above to shop directly inside the app!</div>';
      }
    }

    function escapeHtml(s) {
      return String(s || "").replace(/[&<>"']/g, function(c) {
        return { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c];
      });
    }

    function renderProducts() {
      const container = document.getElementById("catalogContainer");
      const q = (document.getElementById("searchBox").value || "").trim().toLowerCase();
      const filtered = allProducts.filter(function(p) {
        return !q || (p.title || "").toLowerCase().includes(q) || (p.category || "").toLowerCase().includes(q) || (p.brand || "").toLowerCase().includes(q);
      });
      if (!filtered.length) {
        container.className = "empty";
        container.innerText = allProducts.length ? "No products match your search." : "No products uploaded in catalog yet.";
        return;
      }
      container.className = "grid";
      container.innerHTML = filtered.map(function(p) {
        const price = Number(p.base_price || 0);
        const disc = Number(p.discount_percent || 0);
        const finalPrice = disc > 0 ? Math.round(price * (1 - disc / 100)) : Math.round(price);
        const rawUrl = (p.image_url || "").trim();
        const hasHttpImg = rawUrl.startsWith("http://") || rawUrl.startsWith("https://");
        const mediaHtml = hasHttpImg
          ? '<img src="' + escapeHtml(rawUrl) + '" alt="' + escapeHtml(p.title) + '" loading="lazy" onerror="this.onerror=null;this.style.visibility=\'hidden\';" />'
          : '<div style="width:100%;aspect-ratio:1/1;background:#064E3B;display:flex;align-items:center;justify-content:center;color:rgba(255,255,255,0.65);font-size:13px;font-weight:700;">No Photo</div>';
        return '<div class="prod">' +
          mediaHtml +
          '<div class="prod-body">' +
            '<div>' +
              '<div class="prod-cat">' + escapeHtml(p.category || "24/7 Shop") + '</div>' +
              '<div class="prod-title">' + escapeHtml(p.title || "Item") + '</div>' +
            '</div>' +
            '<div class="prod-price">₦' + finalPrice.toLocaleString("en-NG") + '</div>' +
          '</div>' +
        '</div>';
      }).join("");
    }

    loadLiveCatalog();
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
        storeName: String = "24/7 SHOPPING",
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
     * Saves index.html directly to the phone's public Downloads/247_shopping_portal/ folder on Android 10+
     * with zero storage permissions so the user can find it immediately in the Files app -> Downloads.
     */
    fun savePortalToDownloads(
        context: Context,
        sbUrl: String,
        sbKey: String,
        storeName: String = "24/7 SHOPPING",
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
                val relPath = "${Environment.DIRECTORY_DOWNLOADS}/247_shopping_portal"
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
                    return "Downloads/247_shopping_portal/index.html"
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
        storeName: String = "24/7 SHOPPING",
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
                putExtra(Intent.EXTRA_SUBJECT, "24/7 SHOPPING Live Web Portal (index.html)")
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
