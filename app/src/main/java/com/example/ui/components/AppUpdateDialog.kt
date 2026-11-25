package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.AccentGold
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.SuccessGreen
import com.example.util.ApkDownloadResult
import com.example.util.ApkInstallLaunchResult
import com.example.util.ApkUpdateInstaller
import com.example.util.WebPortalExporter
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun AppUpdateDialog(
    latestVersionName: String,
    releaseNotes: String,
    updateUrl: String,
    installedVersionName: String = "1.0",
    storefrontUrl: String = WebPortalExporter.DEFAULT_LIVE_PORTAL_URL,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var statusText by remember { mutableStateOf<String?>(null) }
    var isErrorStatus by remember { mutableStateOf(false) }
    var downloadedApkFile by remember { mutableStateOf<File?>(null) }
    var needsInstallPermission by remember { mutableStateOf(false) }

    val resolvedApkTarget = remember(updateUrl) {
        val normalized = WebPortalExporter.normalizeApkOrPortalUrl(updateUrl)
        if (WebPortalExporter.isDirectApkReleaseUrl(normalized)) {
            normalized
        } else {
            WebPortalExporter.DEFAULT_GITHUB_APK_RELEASE_URL
        }
    }

    Dialog(onDismissRequest = { if (!isDownloading) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("app_update_dialog"),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = AccentGold.copy(alpha = 0.2f),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "Update Available",
                            tint = AccentGold,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "🚀 NEW APP UPDATE AVAILABLE!",
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Installed: v$installedVersionName → New: v$latestVersionName",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = CyberCyan,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "What's New in this Update:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = SuccessGreen
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = releaseNotes.ifBlank { "Performance upgrades, instant cloud sync improvements, and enhanced UI design." },
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }

                if (isDownloading) {
                    Spacer(modifier = Modifier.height(14.dp))
                    if (downloadProgress > 0f) {
                        LinearProgressIndicator(
                            progress = { downloadProgress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = SuccessGreen
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = SuccessGreen
                        )
                    }
                }

                if (!statusText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = statusText.orEmpty(),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isErrorStatus) Color(0xFFEF4444) else SuccessGreen,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Primary Update Action -> downloads APK inside app and opens native Android installer directly
                Button(
                    enabled = !isDownloading,
                    onClick = {
                        val existingFile = downloadedApkFile
                        if (existingFile != null && existingFile.exists()) {
                            when (val launchRes = ApkUpdateInstaller.launchApkInstall(context, existingFile)) {
                                ApkInstallLaunchResult.LaunchedInstaller -> {
                                    needsInstallPermission = false
                                    isErrorStatus = false
                                    statusText = "✅ Installer opened — tap 'Update' on Android prompt to upgrade now."
                                }
                                ApkInstallLaunchResult.NeedsInstallPermission -> {
                                    needsInstallPermission = true
                                    isErrorStatus = false
                                    statusText = "Allow 'Install unknown apps' for ODIYA in Settings, then return & tap 'INSTALL DOWNLOADED APK'."
                                }
                                is ApkInstallLaunchResult.Failed -> {
                                    isErrorStatus = true
                                    statusText = launchRes.message
                                }
                            }
                            return@Button
                        }

                        isDownloading = true
                        downloadProgress = 0f
                        isErrorStatus = false
                        statusText = "Downloading ODIYA v$latestVersionName APK..."

                        scope.launch {
                            when (val dl = ApkUpdateInstaller.downloadApk(
                                context = context,
                                rawUrl = resolvedApkTarget,
                                onProgress = { bytes, total ->
                                    if (total > 0L) {
                                        val frac = bytes.toFloat() / total.toFloat()
                                        downloadProgress = frac
                                        statusText = "Downloading APK (${(frac * 100).toInt()}% • ${bytes / 1024} KB)..."
                                    } else {
                                        statusText = "Downloading APK (${bytes / 1024} KB)..."
                                    }
                                }
                            )) {
                                is ApkDownloadResult.ReadyToInstall -> {
                                    isDownloading = false
                                    downloadedApkFile = dl.apkFile
                                    when (val installRes = ApkUpdateInstaller.launchApkInstall(context, dl.apkFile)) {
                                        ApkInstallLaunchResult.LaunchedInstaller -> {
                                            needsInstallPermission = false
                                            isErrorStatus = false
                                            statusText = "✅ Download complete! Tap 'Update' on Android installer prompt."
                                        }
                                        ApkInstallLaunchResult.NeedsInstallPermission -> {
                                            needsInstallPermission = true
                                            isErrorStatus = false
                                            statusText = "Enable 'Allow from this source' in Android Settings, then come back and tap 'INSTALL DOWNLOADED APK'."
                                            Toast.makeText(
                                                context,
                                                "Allow ODIYA to install app updates, then return to finish installing!",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                        is ApkInstallLaunchResult.Failed -> {
                                            isErrorStatus = true
                                            statusText = installRes.message
                                        }
                                    }
                                }
                                is ApkDownloadResult.NotAnApk -> {
                                    isDownloading = false
                                    isErrorStatus = true
                                    statusText = "${dl.reason} Opening download link in browser..."
                                    WebPortalExporter.openInExternalBrowserOnly(
                                        context = context,
                                        url = dl.url,
                                        forceStayOnWebIfPortal = true
                                    )
                                }
                                is ApkDownloadResult.Failed -> {
                                    isDownloading = false
                                    isErrorStatus = true
                                    statusText = dl.message
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("update_app_action_button")
                ) {
                    Icon(
                        imageVector = if (downloadedApkFile != null) Icons.Default.DownloadDone else Icons.Default.Update,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            isDownloading -> "DOWNLOADING APK..."
                            needsInstallPermission || downloadedApkFile != null -> "INSTALL DOWNLOADED APK"
                            else -> "UPDATE APP NOW"
                        },
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Fallback: Direct Browser APK Download / Storefront Web
                OutlinedButton(
                    enabled = !isDownloading,
                    onClick = {
                        val directBrowserTarget = resolvedApkTarget.ifBlank {
                            WebPortalExporter.normalizePortalUrl(storefrontUrl)
                                .ifBlank { WebPortalExporter.DEFAULT_LIVE_PORTAL_URL }
                        }
                        WebPortalExporter.openInExternalBrowserOnly(
                            context = context,
                            url = directBrowserTarget,
                            forceStayOnWebIfPortal = true
                        )
                        onDismiss()
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("open_live_storefront_update_button")
                ) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Download APK in Phone Browser", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = CyberCyan)
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Downloads the latest ODIYA APK directly on your device and launches the Android installer.",
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    enabled = !isDownloading,
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("dismiss_update_button")
                ) {
                    Text("Later", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
