package com.example.ui.components

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.AccentGold
import com.example.ui.theme.CyberCyan
import com.example.util.WebPortalExporter

private class WebPortalJsBridge(private val context: Context) {
    @JavascriptInterface
    fun copySyncLink(link: String) {
        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cb.setPrimaryClip(ClipData.newPlainText("Odiya Store Sync Link", link))
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            Toast.makeText(context, "✅ Store Sync Link copied!", Toast.LENGTH_SHORT).show()
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebPortalPreviewDialog(
    sbUrl: String,
    sbKey: String,
    storeName: String = "ODIYA",
    storePhone: String = "",
    storeAddress: String = "",
    storeEmail: String = "",
    apkDownloadUrl: String = "",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val saveHtmlLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/html")
    ) { uri ->
        if (uri != null) {
            val ok = WebPortalExporter.writePortalToUri(
                context = context,
                targetUri = uri,
                sbUrl = sbUrl,
                sbKey = sbKey,
                storeName = storeName,
                storePhone = storePhone,
                storeAddress = storeAddress,
                storeEmail = storeEmail,
                apkDownloadUrl = apkDownloadUrl
            )
            Toast.makeText(
                context,
                if (ok) "✅ index.html saved! Upload it to your GitHub repository & enable GitHub Pages." else "❌ Could not save file.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    val portalHtml = remember(sbUrl, sbKey, storeName, storePhone, storeAddress, storeEmail, apkDownloadUrl) {
        val raw = WebPortalExporter.buildStorePortalHtml(
            sbUrl = sbUrl,
            sbKey = sbKey,
            storeName = storeName,
            storePhone = storePhone,
            storeAddress = storeAddress,
            storeEmail = storeEmail,
            apkDownloadUrl = apkDownloadUrl
        )
        // Enhance clipboard button inside in-app preview WebView via safe JS bridge
        raw.replace(
            "if (navigator.clipboard && navigator.clipboard.writeText) {",
            "if (window.AndroidBridge && window.AndroidBridge.copySyncLink) { window.AndroidBridge.copySyncLink(customSchemeLink); btn.innerText = '✅ Copied! Paste in App -> Account -> Link Now'; setTimeout(function(){ btn.innerText = '📋 Copy Store Sync Link'; }, 3000); return; } if (navigator.clipboard && navigator.clipboard.writeText) {"
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, CyberCyan.copy(alpha = 0.55f)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .testTag("web_portal_preview_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Live Web Portal Preview",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Exact webpage customers see after hosting index.html",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close Preview")
                    }
                }

                HorizontalDivider()

                // Interactive WebView showing live portal & catalog
                Surface(
                    color = Color(0xFF052E16),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                addJavascriptInterface(WebPortalJsBridge(ctx), "AndroidBridge")
                                webChromeClient = WebChromeClient()
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        val urlStr = request?.url?.toString() ?: return false
                                        if (urlStr.startsWith("intent://", ignoreCase = true)) {
                                            return try {
                                                val parsed = Intent.parseUri(urlStr, Intent.URI_INTENT_SCHEME).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                                }
                                                ctx.startActivity(parsed)
                                                onDismiss()
                                                true
                                            } catch (e: Exception) {
                                                Toast.makeText(ctx, "App link ready: ${e.message}", Toast.LENGTH_SHORT).show()
                                                true
                                            }
                                        }
                                        if (urlStr.startsWith("depros://", ignoreCase = true) ||
                                            urlStr.startsWith("deprosboutique://", ignoreCase = true)
                                        ) {
                                            return try {
                                                val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse(urlStr)).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                                }
                                                ctx.startActivity(appIntent)
                                                onDismiss()
                                                true
                                            } catch (e: Exception) {
                                                Toast.makeText(ctx, "Could not launch app link: ${e.message}", Toast.LENGTH_SHORT).show()
                                                true
                                            }
                                        }
                                        return false
                                    }
                                }
                                loadDataWithBaseURL(
                                    sbUrl.ifBlank { "https://localhost/" },
                                    portalHtml,
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                            }
                        }
                    )
                }

                HorizontalDivider()

                // Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cb.setPrimaryClip(ClipData.newPlainText("Odiya index.html", portalHtml))
                            Toast.makeText(context, "✅ HTML copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f).height(48.dp).testTag("preview_copy_html_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Copy", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { saveHtmlLauncher.launch("index.html") },
                        border = BorderStroke(1.dp, CyberCyan),
                        modifier = Modifier.weight(1.1f).height(48.dp).testTag("preview_save_file_button")
                    ) {
                        Icon(Icons.Default.SaveAlt, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Save File", fontSize = 11.sp, color = CyberCyan, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            WebPortalExporter.exportAndSharePortalHtml(
                                context = context,
                                sbUrl = sbUrl,
                                sbKey = sbKey,
                                storeName = storeName,
                                storePhone = storePhone,
                                storeAddress = storeAddress,
                                storeEmail = storeEmail,
                                apkDownloadUrl = apkDownloadUrl
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color.Black),
                        modifier = Modifier.weight(1.2f).height(48.dp).testTag("preview_export_html_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Export", fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
