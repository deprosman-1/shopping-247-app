package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.MainViewModel
import com.example.data.model.StoreProfileConfig
import com.example.ui.theme.AccentGold
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.SuccessGreen
import java.io.File
import java.io.FileOutputStream

// Live Public GitHub Pages Store Portal (opens installed ODIYA app automatically, or shows live catalog + APK download)
const val DEPROS_DOWNLOAD_URL = com.example.util.WebPortalExporter.DEFAULT_LIVE_PORTAL_URL
const val DEPROS_DEV_URL = com.example.util.WebPortalExporter.DEFAULT_LIVE_PORTAL_URL

@Composable
fun ShareAppDialog(
    onDismiss: () -> Unit,
    viewModel: MainViewModel? = null
) {
    val context = LocalContext.current
    val storeProfile = viewModel?.storeProfile?.collectAsState()?.value ?: StoreProfileConfig()
    val storeName = storeProfile.storeName.ifBlank { "ODIYA" }

    LaunchedEffect(Unit) {
        com.example.util.WebPortalExporter.purgeLegacyApkUrlPref(context)
    }

    val sharePrefs = remember { context.getSharedPreferences("smart_shop_share_prefs", Context.MODE_PRIVATE) }
    val adminPrefs = remember { context.getSharedPreferences("depros_admin_settings", Context.MODE_PRIVATE) }

    val savedSbUrl = remember(viewModel) {
        viewModel?.repository?.supabaseSync?.getConfig()?.url?.takeIf { it.isNotBlank() }
            ?: adminPrefs.getString("supabase_url", "")?.trim().orEmpty()
    }
    val savedSbKey = remember(viewModel) {
        viewModel?.repository?.supabaseSync?.getConfig()?.anonKey?.takeIf { it.isNotBlank() }
            ?: adminPrefs.getString("supabase_anon_key", "")?.trim().orEmpty()
    }
    val hasCloudCreds = remember(savedSbUrl, savedSbKey) {
        savedSbUrl.isNotBlank() && savedSbKey.isNotBlank() &&
            !(viewModel?.repository?.supabaseSync?.isPlaceholderConfig(savedSbUrl, savedSbKey) ?: false)
    }

    val fallbackSavedUrl = remember(storeProfile.apkDownloadUrl) {
        com.example.util.WebPortalExporter.purgeLegacyApkUrlPref(context)
        val savedPortal = com.example.util.WebPortalExporter.getSavedCustomPortalUrl(context)
        if (savedPortal.isNotBlank()) {
            savedPortal
        } else {
            com.example.util.WebPortalExporter.normalizePortalUrl(storeProfile.apkDownloadUrl)
                .takeIf { it.isNotBlank() && !com.example.util.WebPortalExporter.isDirectApkReleaseUrl(it) }
                ?: DEPROS_DOWNLOAD_URL
        }
    }

    var customUrl by remember {
        val prefFallback = com.example.util.WebPortalExporter.normalizePortalUrl(
            sharePrefs.getString("custom_share_url", "") ?: ""
        ).takeIf { !com.example.util.WebPortalExporter.isDirectApkReleaseUrl(it) }.orEmpty()
        mutableStateOf(
            if (prefFallback.isNotBlank()) prefFallback else fallbackSavedUrl
        )
    }

    LaunchedEffect(storeProfile.apkDownloadUrl) {
        val cleanPortal = com.example.util.WebPortalExporter.normalizePortalUrl(storeProfile.apkDownloadUrl)
        if (customUrl.isBlank() &&
            cleanPortal.isNotBlank() &&
            !com.example.util.WebPortalExporter.isDirectApkReleaseUrl(cleanPortal)
        ) {
            customUrl = cleanPortal
            if (viewModel != null) {
                viewModel.savePermanentShareUrl(cleanPortal)
            } else {
                com.example.util.WebPortalExporter.saveCustomPortalUrl(context, cleanPortal)
            }
        }
    }

    var linkMode by remember {
        val savedMode = sharePrefs.getInt("share_link_mode", -1)
        val initialMode = when {
            savedMode in 0..2 -> savedMode
            else -> 2
        }
        mutableIntStateOf(initialMode)
    }
    var installGuideTab by remember { mutableIntStateOf(0) } // 0 = Android, 1 = Computers
    var includeCloudCreds by remember(hasCloudCreds) { mutableStateOf(hasCloudCreds) }
    var showWebPortalPreview by remember { mutableStateOf(false) }

    val directConnectUrl = remember(hasCloudCreds, savedSbUrl, savedSbKey) {
        if (hasCloudCreds) {
            "depros://connect?sb_url=${Uri.encode(savedSbUrl)}&sb_key=${Uri.encode(savedSbKey)}"
        } else {
            "depros://open"
        }
    }

    val rawShareUrl = remember(linkMode, customUrl, directConnectUrl) {
        when (linkMode) {
            1 -> directConnectUrl
            0 -> DEPROS_DOWNLOAD_URL
            else -> {
                val normalized = com.example.util.WebPortalExporter.normalizePortalUrl(customUrl)
                normalized.ifBlank { DEPROS_DOWNLOAD_URL }
            }
        }
    }

    val activeShareUrl = remember(rawShareUrl, includeCloudCreds, hasCloudCreds, savedSbUrl, savedSbKey, linkMode) {
        val isDirectApkLink = com.example.util.WebPortalExporter.isDirectApkReleaseUrl(rawShareUrl)
        if (linkMode == 1 || !rawShareUrl.startsWith("http", ignoreCase = true) || isDirectApkLink) {
            rawShareUrl
        } else {
            val baseWithSlash = if (!rawShareUrl.contains("?") && rawShareUrl.count { it == '/' } == 2) {
                "$rawShareUrl/"
            } else {
                rawShareUrl
            }
            val sep = if (baseWithSlash.contains("?")) "&" else "?"
            if (includeCloudCreds && hasCloudCreds) {
                "${baseWithSlash}${sep}open_app=1&sb_url=${Uri.encode(savedSbUrl)}&sb_key=${Uri.encode(savedSbKey)}"
            } else {
                "${baseWithSlash}${sep}open_app=1"
            }
        }
    }

    val shareTextPayload = remember(activeShareUrl, directConnectUrl, linkMode, storeProfile) {
        val isLiveWeb = activeShareUrl.startsWith("http", ignoreCase = true)
        buildString {
            append("🛍️ ${storeName.uppercase()}\n${storeProfile.tagline}\n")
            val allPhones = listOf(storeProfile.phoneNumber, storeProfile.secondaryPhoneNumber).filter { it.isNotBlank() }.joinToString(" / ")
            if (allPhones.isNotBlank()) append("📞 Phone: $allPhones\n")
            if (storeProfile.storeAddress.isNotBlank()) append("📍 Address: ${storeProfile.storeAddress}\n")
            append("\n")
            if (isLiveWeb) {
                append("📲 Open $storeName App or Web Storefront:\n$activeShareUrl\n\n")
            }
            if (directConnectUrl.contains("sb_url=")) {
                append("🔗 Direct App Sync Link (for installed APK):\n$directConnectUrl\n\n")
            }
            append("📱 ALREADY HAVE $storeName INSTALLED?\n")
            append("Tap the link above to open the app automatically — or copy this message and paste it into: $storeName → Account → Link Now!\n\n")
            append("📦 NEW CUSTOMER?\n")
            append("Tap the link above to browse live or download the official Android APK.")
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
                .testTag("share_app_dialog"),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AccentGold.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Store,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("INSTALL & SHARE", color = AccentGold, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = storeName.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "QR Code, Download Links & Share Hub",
                    style = MaterialTheme.typography.bodySmall,
                    color = CyberCyan,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                val allHeaderPhones = listOf(storeProfile.phoneNumber, storeProfile.secondaryPhoneNumber).filter { it.isNotBlank() }.joinToString(" / ")
                if (allHeaderPhones.isNotBlank() || storeProfile.storeAddress.isNotBlank()) {
                    val infoLine = listOfNotNull(
                        allHeaderPhones.takeIf { it.isNotBlank() }?.let { "📞 $it" },
                        storeProfile.storeAddress.takeIf { it.isNotBlank() }?.let { "📍 $it" }
                    ).joinToString(" • ")
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = infoLine,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Verified Banner
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SuccessGreen.copy(alpha = 0.12f),
                    border = BorderStroke(1.5.dp, SuccessGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("verified_safe_download_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "Verified Safe",
                            tint = SuccessGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "UNIVERSAL ANDROID & DESKTOP READY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = SuccessGreen
                            )
                            Text(
                                text = "Android 7.0–16 • Windows • macOS • Linux • Chromebook",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Unified QR Code + Download Links + Share Container
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)),
                    border = BorderStroke(1.2.dp, SuccessGreen.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Link Target Switcher (Updates QR Code & All Download/Share Links Live)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = linkMode == 2,
                                onClick = {
                                    linkMode = 2
                                    sharePrefs.edit().putInt("share_link_mode", 2).apply()
                                },
                                label = { Text("Live Web URL (Custom)", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = linkMode == 1,
                                onClick = {
                                    linkMode = 1
                                    sharePrefs.edit().putInt("share_link_mode", 1).apply()
                                },
                                label = { Text("Direct App Link", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = linkMode == 0,
                                onClick = {
                                    linkMode = 0
                                    sharePrefs.edit().putInt("share_link_mode", 0).apply()
                                },
                                label = { Text("Odiya Cloud Web Link", fontSize = 11.sp) }
                            )
                        }

                        if (linkMode == 2) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = customUrl,
                                onValueChange = {
                                    customUrl = it
                                    com.example.util.WebPortalExporter.saveCustomPortalUrl(context, it)
                                },
                                label = { Text("Your GitHub Pages URL (or Custom https://)") },
                                placeholder = { Text("https://yourusername.github.io/odiya-store/") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("custom_share_url_input")
                            )
                            val savedNorm = com.example.util.WebPortalExporter.normalizeWebUrl(customUrl)
                            val suggestedPagesUrl = remember(savedNorm) {
                                com.example.util.WebPortalExporter.suggestGitHubPagesUrlIfRepoDashboard(savedNorm)
                            }
                            if (suggestedPagesUrl != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = AccentGold.copy(alpha = 0.14f),
                                    border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.6f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "⚠️ That opens GitHub's code dashboard, not your store!",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Black,
                                            color = AccentGold
                                        )
                                        Text(
                                            text = "Upload index.html, turn on GitHub Pages (Settings → Pages), then use:\n$suggestedPagesUrl",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        OutlinedButton(
                                            onClick = {
                                                customUrl = suggestedPagesUrl
                                                if (viewModel != null) {
                                                    viewModel.savePermanentShareUrl(suggestedPagesUrl)
                                                } else {
                                                    com.example.util.WebPortalExporter.saveCustomPortalUrl(context, suggestedPagesUrl)
                                                }
                                                Toast.makeText(
                                                    context,
                                                    "✅ Switched to GitHub Pages link: $suggestedPagesUrl",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            },
                                            modifier = Modifier.fillMaxWidth().height(48.dp)
                                        ) {
                                            Text("Use GitHub Pages Link Instead", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else if (savedNorm.isNotBlank() && !com.example.util.WebPortalExporter.isAiStudioPreviewHost(savedNorm)) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "✅ Link saved permanently — ready for QR, WhatsApp & Updates",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessGreen
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = {
                                    val normalized = if (viewModel != null) {
                                        viewModel.savePermanentShareUrl(customUrl)
                                    } else {
                                        com.example.util.WebPortalExporter.saveCustomPortalUrl(context, customUrl)
                                        com.example.util.WebPortalExporter.getSavedCustomPortalUrl(context)
                                    }
                                    if (normalized.isNotBlank()) {
                                        customUrl = normalized
                                        linkMode = 2
                                        Toast.makeText(
                                            context,
                                            "✅ Saved permanently! It will stay inside this box automatically.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "⚠️ Paste a valid https:// GitHub link first.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("save_permanent_share_url_button")
                            ) {
                                Text("Save Link Permanently", fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    enabled = hasCloudCreds,
                                    onClick = {
                                        com.example.util.WebPortalExporter.exportAndSharePortalHtml(
                                            context = context,
                                            sbUrl = savedSbUrl,
                                            sbKey = savedSbKey,
                                            storeName = storeName,
                                            storePhone = storeProfile.phoneNumber,
                                            storeAddress = storeProfile.storeAddress,
                                            storeEmail = storeProfile.supportEmail,
                                            apkDownloadUrl = storeProfile.apkDownloadUrl
                                        )
                                    },
                                    modifier = Modifier.weight(1f).height(44.dp)
                                ) {
                                    Text("Export index.html", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = {
                                        com.example.util.WebPortalExporter.openExternalUrl(context, "https://github.com/new")
                                    },
                                    modifier = Modifier.weight(1f).height(44.dp)
                                ) {
                                    Text("Open GitHub", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else if (linkMode == 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "💡 Tip: For permanent $0 hosting on external phones, choose 'Live Web URL', upload index.html to GitHub Pages, or use 'Direct App Link'.",
                                fontSize = 10.sp,
                                color = CyberCyan
                            )
                        }

                if (hasCloudCreds) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CyberCyan.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🔗 Auto-Link Customer Phones to Store Cloud",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = CyberCyan
                                )
                                Text(
                                    text = if (includeCloudCreds) {
                                        "ON: Auto-links new phones to cloud. Tip: Turn OFF for a simpler, faster-scanning QR code."
                                    } else {
                                        "OFF: Short portal link & ultra-fast camera QR scan (uses embedded store cloud)."
                                    },
                                    fontSize = 9.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            androidx.compose.material3.Switch(
                                checked = includeCloudCreds,
                                onCheckedChange = { includeCloudCreds = it }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // High-Precision Scannable QR Code (ISO/IEC 18004)
                Box(
                    modifier = Modifier
                        .size(224.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .border(2.dp, SuccessGreen.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
                        .padding(4.dp)
                ) {
                    QrCodeView(
                        content = activeShareUrl.trim(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Point any QR Code Scanner, Phone Camera, or Google Lens here to open & install instantly",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Share / Save Scannable QR Code Poster Image Button
                OutlinedButton(
                    onClick = { shareQrCodeImage(context, activeShareUrl, storeTitle = storeName) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("share_qr_image_button")
                ) {
                    Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp), tint = CyberCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share / Save Scannable QR Poster Image", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                // Shareable URL Box with Copy URL & Copy Invite actions
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Shareable Link (QR Target)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            SelectionContainer {
                                Text(
                                    text = activeShareUrl,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 2,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("$storeName Link", activeShareUrl))
                                Toast.makeText(context, "✅ Shareable Link Copied!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("copy_url_only_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Shareable Link", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 1-CLICK SHARE & INSTALL ACTIONS
                Text(
                    text = "SHARE OR INSTALL DIRECTLY",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // SEND CLEAN APK FILE DIRECTLY (Android Phones, Tablets, Chromebook, Windows WSA)
                Button(
                    onClick = { shareCleanApkFile(context, activeShareUrl) },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("share_direct_apk_file_button")
                ) {
                    Icon(Icons.Default.InstallMobile, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text("SEND CLEAN APK FILE DIRECTLY", fontWeight = FontWeight.Black, fontSize = 12.sp)
                        Text("WhatsApp • Bluetooth • Quick Share • Drive • PC", fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // BROADCAST APP INVITE TO ADMIN, CUSTOMER WHATSAPP & SOCIALS
                Button(
                    onClick = {
                        val broadcastText = "🚀 *INVITATION TO JOIN $storeName*\n\n" +
                            "Hello friends, customers & partners! You're invited to install and use *${storeName}* — your always-on digital store with live catalog browsing, instant pre-orders, secure offline/online payments (Paystack, OPay, Moniepoint, PalmPay, POS), and real-time delivery tracking.\n\n" +
                            "📲 *Download & Open Portal:* $activeShareUrl\n\n" +
                            "Tap the link above or open the app to shop anytime!"
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "$storeName App Invite & Summary")
                            putExtra(Intent.EXTRA_TEXT, broadcastText)
                        }
                        try {
                            context.startActivity(Intent.createChooser(sendIntent, "Broadcast App Invite to WhatsApp & Socials"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open share chooser: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("broadcast_whatsapp_socials_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text("INVITE ADMIN, CUSTOMERS & SOCIALS", fontWeight = FontWeight.Black, fontSize = 11.5.sp)
                        Text("WhatsApp • Social Media • SMS • Direct Invite", fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // WhatsApp Share (handles WhatsApp & WhatsApp Business + Web fallback)
                    Button(
                        onClick = {
                            val waIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                `package` = "com.whatsapp"
                                putExtra(Intent.EXTRA_TEXT, shareTextPayload)
                            }
                            try {
                                context.startActivity(waIntent)
                            } catch (_: Exception) {
                                try {
                                    val w4bIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        `package` = "com.whatsapp.w4b"
                                        putExtra(Intent.EXTRA_TEXT, shareTextPayload)
                                    }
                                    context.startActivity(w4bIntent)
                                } catch (_: Exception) {
                                    try {
                                        context.startActivity(
                                            Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(shareTextPayload)}")
                                            )
                                        )
                                    } catch (_: Exception) {
                                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, shareTextPayload)
                                        }, "Share via WhatsApp"))
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("share_whatsapp_button")
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = "WhatsApp", tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("WhatsApp", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    // Copy Full Invite Message
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("$storeName Invite", shareTextPayload))
                            Toast.makeText(context, "✅ Storefront Invite Copied!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("share_copy_invite_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Invite", tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Invite", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "$storeName - Official App Link")
                                putExtra(Intent.EXTRA_TEXT, shareTextPayload)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Link & Guide"))
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("share_link_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("More Apps")
                    }

                    Button(
                        onClick = {
                            val isPreviewHost = com.example.util.WebPortalExporter.isAiStudioPreviewHost(activeShareUrl)
                            val isAppScheme = activeShareUrl.startsWith("depros://", ignoreCase = true) ||
                                activeShareUrl.startsWith("deprosboutique://", ignoreCase = true)
                            when {
                                isAppScheme -> {
                                    try {
                                        val deepIntent = Intent(Intent.ACTION_VIEW, Uri.parse(activeShareUrl)).apply {
                                            setPackage(context.packageName)
                                            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                        }
                                        context.startActivity(deepIntent)
                                        onDismiss()
                                    } catch (e: Exception) {
                                        openInWebBrowser(context, activeShareUrl)
                                    }
                                }
                                isPreviewHost || activeShareUrl.isBlank() -> {
                                    Toast.makeText(context, "🌐 Opening Built-In Live Web Portal Preview...", Toast.LENGTH_SHORT).show()
                                    showWebPortalPreview = true
                                }
                                else -> {
                                    Toast.makeText(context, "🌐 Opening Live Store Web Portal...", Toast.LENGTH_SHORT).show()
                                    openInWebBrowser(context, activeShareUrl)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.1f)
                            .height(48.dp)
                            .testTag("download_apk_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Portal", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { showWebPortalPreview = true },
                    border = BorderStroke(1.2.dp, CyberCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("share_preview_web_portal_button")
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Preview Live Store Web Portal Here", color = CyberCyan, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // UPDATE APP BUTTON FOR NATIVE APK DOWNLOADS
                val installedVerPair = remember(viewModel) { viewModel?.getInstalledAppVersion() ?: (1 to "1.0") }
                var isCheckingApkUpdate by remember { mutableStateOf(false) }
                Button(
                    onClick = {
                        val configuredApkUrl = com.example.util.WebPortalExporter.normalizeApkOrPortalUrl(storeProfile.apkDownloadUrl)
                            .ifBlank { com.example.util.WebPortalExporter.getSavedCustomPortalUrl(context) }
                            .ifBlank { DEPROS_DOWNLOAD_URL }
                        if (viewModel != null) {
                            isCheckingApkUpdate = true
                            viewModel.checkForAppUpdates(showPopupIfFound = true) { isLatest, _ ->
                                isCheckingApkUpdate = false
                                if (!isLatest) {
                                    onDismiss()
                                } else {
                                    Toast.makeText(context, "⬇️ Opening Live Storefront & APK Download...", Toast.LENGTH_SHORT).show()
                                    openInWebBrowser(context, configuredApkUrl)
                                }
                            }
                        } else {
                            openInWebBrowser(context, configuredApkUrl)
                        }
                    },
                    enabled = !isCheckingApkUpdate,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("update_app_button")
                ) {
                    if (isCheckingApkUpdate) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Checking for App Update...", fontWeight = FontWeight.Black, fontSize = 12.5.sp)
                    } else {
                        Icon(Icons.Default.Update, contentDescription = "Update App", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Update App (Installed v${installedVerPair.second})",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.5.sp
                        )
                    }
                }

                if (showWebPortalPreview) {
                    WebPortalPreviewDialog(
                        sbUrl = savedSbUrl,
                        sbKey = savedSbKey,
                        storeName = storeName,
                        storePhone = storeProfile.phoneNumber,
                        storeAddress = storeProfile.storeAddress,
                        storeEmail = storeProfile.supportEmail,
                        apkDownloadUrl = storeProfile.apkDownloadUrl,
                        onDismiss = { showWebPortalPreview = false }
                    )
                }
                    }
                } // End unified QR Code + Links + Share Container

                Spacer(modifier = Modifier.height(14.dp))

                // CROSS-PLATFORM EASY INSTALLATION GUIDE (ANDROID + COMPUTERS)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("install_troubleshooting_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "EASY INSTALL GUIDE BY DEVICE",
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = CyberCyan
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = installGuideTab == 0,
                                onClick = { installGuideTab = 0 },
                                leadingIcon = { Icon(Icons.Default.Smartphone, contentDescription = null, modifier = Modifier.size(15.dp)) },
                                label = { Text("All Android", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = installGuideTab == 1,
                                onClick = { installGuideTab = 1 },
                                leadingIcon = { Icon(Icons.Default.Computer, contentDescription = null, modifier = Modifier.size(15.dp)) },
                                label = { Text("Computers", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (installGuideTab == 0) {
                            Text(
                                text = "📱 Any Android Phone or Tablet (Samsung, Tecno, Infinix, Itel, Xiaomi, Pixel):",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = SuccessGreen
                            )
                            Text(
                                text = "1. Tap Shareable Link or 'SEND CLEAN APK FILE DIRECTLY'.\n" +
                                    "2. Open APK and allow installation from Chrome / WhatsApp / Files.\n" +
                                    "3. Google Play Protect Notice: Tap 'More details' ➡️ 'Install anyway'.\n" +
                                    "4. Tecno / Infinix / Itel Phone Master Security Scan Warning:\n" +
                                    "   ⚠️ Why it shows: Phone Master flags any direct APK download not from Google Play as 'Virus scan not clarified' / 'Installation source standard'.\n" +
                                    "   ✅ How to bypass: Tap 'Continue Anyway', 'Trust App', 'Ignore Risk', or 'Install Anyway'. The app is 100% clean and verified!\n" +
                                    "   💡 Tip: Use 'Open Portal' in Chrome to install the PWA instantly with zero APK warnings.",
                                fontSize = 10.5.sp,
                                lineHeight = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "💻 Windows, Mac, Linux & Chromebook:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = CyberCyan
                            )
                            Text(
                                text = "1. Open the Shareable Link above in Chrome or Microsoft Edge on your computer.\n" +
                                    "2. Click the Install icon (⊕) inside the browser address bar (or Menu ⋮ ➡️ 'Install $storeName') to add it as a standalone Desktop App with a Start Menu / Dock / Desktop icon.\n" +
                                    "3. To run the Android APK directly on a computer: use Chromebook Play/Linux installer, Windows Subsystem for Android, or any Android Emulator—just send the APK file via 'SEND CLEAN APK FILE DIRECTLY' above!",
                                fontSize = 10.5.sp,
                                lineHeight = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // SECURITY & COMPATIBILITY CARD
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SECURITY & COMPATIBILITY GUARANTEE",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = SuccessGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• Android Phones & Tablets (Samsung, Tecno, Infinix, Itel, Pixel, Xiaomi, Android 7.0–16)\n" +
                                "• Desktops & Laptops (Windows, macOS, Linux, Chromebook via Installable Web Portal or APK)\n" +
                                "• Universal ISO/IEC 18004 Level-H QR Code for fast scanning from cameras & scanners",
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val installedInfo = remember(viewModel) { viewModel?.getInstalledAppVersion() ?: (1 to "1.0") }
                var isCheckingUpdate by remember { mutableStateOf(false) }
                var updateStatusMessage by remember {
                    mutableStateOf("Installed Native APK: v${installedInfo.second} (Build ${installedInfo.first})")
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("realtime_update_sync_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberCyan.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Sync, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "APP UPDATES & LIVE SYNC",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = SuccessGreen
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SuccessGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "v${installedInfo.second}",
                                    color = SuccessGreen,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = updateStatusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Catalog & prices sync live automatically. Tap below to check for new APK design/code updates.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                isCheckingUpdate = true
                                if (viewModel != null) {
                                    viewModel.checkForAppUpdates(showPopupIfFound = true) { isLatest, msg ->
                                        isCheckingUpdate = false
                                        updateStatusMessage = msg
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        if (!isLatest) {
                                            onDismiss()
                                        }
                                    }
                                } else {
                                    isCheckingUpdate = false
                                    updateStatusMessage = "Installed v${installedInfo.second} (Build ${installedInfo.first}) is up to date."
                                    Toast.makeText(context, updateStatusMessage, Toast.LENGTH_LONG).show()
                                }
                            },
                            enabled = !isCheckingUpdate,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("check_for_updates_button")
                        ) {
                            if (isCheckingUpdate) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Checking for App Updates...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.Update, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Check & Update App Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Official Customer Support",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                val footerPhones = listOf(storeProfile.phoneNumber, storeProfile.secondaryPhoneNumber).filter { it.isNotBlank() }.joinToString(" / ")
                if (footerPhones.isNotBlank() || storeProfile.storeAddress.isNotBlank()) {
                    val footerContact = listOfNotNull(
                        footerPhones.takeIf { it.isNotBlank() }?.let { "📞 $it" },
                        storeProfile.storeAddress.takeIf { it.isNotBlank() }?.let { "📍 $it" }
                    ).joinToString(" • ")
                    Text(
                        text = footerContact,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Generates and shares a crisp printable/scannable QR Poster PNG via FileProvider
 * so users can send the QR code to WhatsApp, printers, Instagram, or flyers.
 */
fun shareQrCodeImage(context: Context, qrTargetUrl: String, storeTitle: String = "ODIYA") {
    try {
        val posterBitmap = generateShareableQrPosterBitmap(qrTargetUrl, title = storeTitle)
        if (posterBitmap == null) {
            Toast.makeText(context, "Could not generate QR image", Toast.LENGTH_SHORT).show()
            return
        }
        val qrFolder = File(context.cacheDir, "qr_share")
        if (!qrFolder.exists()) qrFolder.mkdirs()
        val qrFile = File(qrFolder, "Odiya_QR_Poster.png")
        FileOutputStream(qrFile).use { out ->
            posterBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val qrUri = FileProvider.getUriForFile(
            context,
            "com.aistudio.deprosboutique.kqwmxp.fileprovider",
            qrFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, qrUri)
            putExtra(Intent.EXTRA_SUBJECT, "$storeTitle - Scan QR Code to Install")
            putExtra(
                Intent.EXTRA_TEXT,
                "🛍️ Scan this QR Code with your phone camera or QR scanner to install $storeTitle:\n$qrTargetUrl"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Scannable QR Code Poster"))
    } catch (e: Exception) {
        Toast.makeText(context, "Could not share QR image: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Opens the URL in the system browser or presents a browser chooser modal (Chrome, Opera Mini, etc.)
 * with fail-safe error handling and clipboard copy fallback.
 */
fun openInWebBrowser(context: Context, url: String) {
    val cleanUrl = if (
        url.startsWith("depros://", ignoreCase = true) ||
        url.startsWith("deprosboutique://", ignoreCase = true) ||
        url.startsWith("http://", ignoreCase = true) ||
        url.startsWith("https://", ignoreCase = true)
    ) {
        url.trim()
    } else {
        "https://${url.trim()}"
    }
    if (cleanUrl.startsWith("http://", ignoreCase = true) ||
        cleanUrl.startsWith("https://", ignoreCase = true)
    ) {
        com.example.util.WebPortalExporter.openInExternalBrowserOnly(
            context = context,
            url = cleanUrl,
            forceStayOnWebIfPortal = true
        )
        return
    }
    val uri = Uri.parse(cleanUrl)
    try {
        val directIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(directIntent)
    } catch (e2: Exception) {
        Toast.makeText(context, "Link copied to clipboard! Paste into App → Account → Link Now.", Toast.LENGTH_LONG).show()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("App Link", cleanUrl)
        clipboard.setPrimaryClip(clip)
    }
}

/**
 * Exports and shares the intact, uncorrupted APK file directly from this app's storage
 * using FileProvider so recipients on Android, Chromebook, or Windows/Mac emulators receive a valid, installable APK.
 */
fun shareCleanApkFile(context: Context, fallbackUrl: String = DEPROS_DOWNLOAD_URL) {
    try {
        val appInfo = context.applicationInfo
        val sourceApk = File(appInfo.sourceDir)
        if (!sourceApk.exists()) {
            Toast.makeText(context, "Extracting download link...", Toast.LENGTH_SHORT).show()
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(
                    Intent.EXTRA_TEXT,
                    "🛍️ Download ODIYA:\n$fallbackUrl\n\n📌 In Play Protect, tap 'More details' -> 'Install anyway'!"
                )
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Download Link"))
            return
        }

        val shareFolder = File(context.cacheDir, "apk_share")
        if (!shareFolder.exists()) shareFolder.mkdirs()
        val targetApk = File(shareFolder, "Odiya_Official.apk")

        sourceApk.copyTo(targetApk, overwrite = true)

        val apkUri = FileProvider.getUriForFile(
            context,
            "com.aistudio.deprosboutique.kqwmxp.fileprovider",
            targetApk
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.android.package-archive"
            putExtra(Intent.EXTRA_STREAM, apkUri)
            putExtra(Intent.EXTRA_SUBJECT, "ODIYA - Official App APK")
            putExtra(
                Intent.EXTRA_TEXT,
                "🛍️ ODIYA Official App APK\n\n" +
                    "📌 EASY INSTALLATION (Android & Computer Emulators):\n" +
                    "1. Tap to open the APK file.\n" +
                    "2. When prompted 'This app may be harmful', tap 'More details' ➡️ 'Install anyway'.\n" +
                    "3. Or open the web portal on any phone/computer: $fallbackUrl"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Send Official APK via WhatsApp / Drive"))
    } catch (e: Exception) {
        Toast.makeText(context, "Sharing download link instead: ${e.message}", Toast.LENGTH_LONG).show()
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                "🛍️ Download ODIYA Official App:\n$fallbackUrl\n\nTap 'More details' -> 'Install anyway'!"
            )
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Official Download Link"))
    }
}
