package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

sealed class ApkDownloadResult {
    data class ReadyToInstall(val apkFile: File) : ApkDownloadResult()
    data class NotAnApk(val url: String, val reason: String) : ApkDownloadResult()
    data class Failed(val message: String) : ApkDownloadResult()
}

sealed class ApkInstallLaunchResult {
    data object LaunchedInstaller : ApkInstallLaunchResult()
    data object NeedsInstallPermission : ApkInstallLaunchResult()
    data class Failed(val message: String) : ApkInstallLaunchResult()
}

object ApkUpdateInstaller {
    private const val FILE_PROVIDER_AUTHORITY = "com.aistudio.deprosboutique.kqwmxp.fileprovider"
    private const val UPDATE_DIR = "apk_share"
    private const val UPDATE_FILENAME = "odiya-update.apk"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .build()
    }

    suspend fun downloadApk(
        context: Context,
        rawUrl: String,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit
    ): ApkDownloadResult = withContext(Dispatchers.IO) {
        val normalized = WebPortalExporter.normalizeApkOrPortalUrl(rawUrl)
            .ifBlank { WebPortalExporter.DEFAULT_GITHUB_APK_RELEASE_URL }

        if (!WebPortalExporter.isDirectApkReleaseUrl(normalized)) {
            return@withContext ApkDownloadResult.NotAnApk(
                url = normalized,
                reason = "Update link points to a web storefront page instead of a direct .apk binary."
            )
        }

        val targetDir = File(context.cacheDir, UPDATE_DIR).apply { mkdirs() }
        val tempFile = File(targetDir, "$UPDATE_FILENAME.part")
        val finalFile = File(targetDir, UPDATE_FILENAME)
        if (tempFile.exists()) tempFile.delete()

        try {
            val request = Request.Builder()
                .url(normalized)
                .header("Accept", "application/vnd.android.package-archive, application/octet-stream, */*")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    if (code == 404) {
                        return@withContext ApkDownloadResult.NotAnApk(
                            url = normalized,
                            reason = "APK file not published at that release URL yet (HTTP 404). Open Live Storefront or browser to download."
                        )
                    }
                    return@withContext ApkDownloadResult.Failed("Download failed (HTTP $code). Tap 'Open in Browser / Storefront' below.")
                }

                val body = response.body ?: return@withContext ApkDownloadResult.Failed("Empty download response.")
                val contentType = response.header("Content-Type").orEmpty().lowercase()
                if (contentType.contains("text/html")) {
                    return@withContext ApkDownloadResult.NotAnApk(
                        url = normalized,
                        reason = "Server returned a web page instead of an APK package."
                    )
                }

                val totalBytes = body.contentLength()
                val stream = body.byteStream()
                val header = ByteArray(4)
                var headerRead = 0
                while (headerRead < 4) {
                    val r = stream.read(header, headerRead, 4 - headerRead)
                    if (r < 0) break
                    headerRead += r
                }

                // Every Android APK is a ZIP archive starting with 'P', 'K' (0x50, 0x4B)
                if (headerRead < 2 || header[0] != 0x50.toByte() || header[1] != 0x4B.toByte()) {
                    return@withContext ApkDownloadResult.NotAnApk(
                        url = normalized,
                        reason = "Downloaded file is not a valid Android APK package."
                    )
                }

                var downloaded = headerRead.toLong()
                FileOutputStream(tempFile).use { out ->
                    out.write(header, 0, headerRead)
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val read = stream.read(buffer)
                        if (read == -1) break
                        out.write(buffer, 0, read)
                        downloaded += read
                        onProgress(downloaded, totalBytes)
                    }
                    out.flush()
                }

                if (finalFile.exists()) finalFile.delete()
                if (!tempFile.renameTo(finalFile)) {
                    tempFile.copyTo(finalFile, overwrite = true)
                    tempFile.delete()
                }
                ApkDownloadResult.ReadyToInstall(finalFile)
            }
        } catch (e: Exception) {
            if (tempFile.exists()) tempFile.delete()
            ApkDownloadResult.Failed(e.message ?: "Network error while downloading update APK.")
        }
    }

    fun launchApkInstall(context: Context, apkFile: File): ApkInstallLaunchResult {
        if (!apkFile.exists() || apkFile.length() < 1024L) {
            return ApkInstallLaunchResult.Failed("Downloaded APK file is missing or incomplete.")
        }
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                !context.packageManager.canRequestPackageInstalls()
            ) {
                val settingsIntent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
                return ApkInstallLaunchResult.NeedsInstallPermission
            }

            val apkUri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, apkFile)
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(installIntent)
            ApkInstallLaunchResult.LaunchedInstaller
        } catch (e: Exception) {
            ApkInstallLaunchResult.Failed(e.message ?: "Unable to launch Android package installer.")
        }
    }
}
