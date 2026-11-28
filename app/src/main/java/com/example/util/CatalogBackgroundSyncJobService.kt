package com.example.util

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import com.example.data.local.DeprosDatabase
import com.example.data.model.SupabaseConfig
import com.example.data.remote.SupabaseSyncService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Background JobService that syncs cloud updates (orders, stock restocks, and active promos)
 * even when the app is closed, posting push-style OS notifications via ShopNotificationHelper.
 */
class CatalogBackgroundSyncJobService : JobService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartJob(params: JobParameters?): Boolean {
        val appContext = applicationContext
        serviceScope.launch {
            try {
                val prefs = appContext.getSharedPreferences("depros_admin_settings", Context.MODE_PRIVATE)
                val sbUrl = prefs.getString("supabase_url", "") ?: ""
                val sbKey = prefs.getString("supabase_anon_key", "") ?: ""
                if (sbUrl.isNotBlank() && sbKey.isNotBlank()) {
                    val syncService = SupabaseSyncService()
                    syncService.updateConfig(SupabaseConfig(url = sbUrl, anonKey = sbKey, isConnected = true))

                    val dao = DeprosDatabase.getDatabase(appContext).dao()
                    val previousOrders = dao.getAllOrdersList().associate { it.orderId to it.status }
                    val remoteOrdersRes = syncService.fetchOrdersFromCloud()
                    if (remoteOrdersRes.isSuccess) {
                        val remoteOrders = remoteOrdersRes.getOrDefault(emptyList())
                        remoteOrders.forEach { ord ->
                            val oldStatus = previousOrders[ord.orderId]
                            if (oldStatus != null && oldStatus != ord.status.name) {
                                ShopNotificationHelper.notifyOrderStatusChange(
                                    context = appContext,
                                    orderId = ord.orderId,
                                    newStatus = ord.status,
                                    note = ord.verificationNote
                                )
                            }
                        }
                    }

                    // Check back-in-stock wishlist/watch alerts
                    val watchedIds = prefs.getStringSet("back_in_stock_product_ids", emptySet()).orEmpty()
                    if (watchedIds.isNotEmpty()) {
                        val remoteProds = syncService.fetchProductsFromCloud().getOrDefault(emptyList())
                        val restocked = remoteProds.filter { watchedIds.contains(it.id) && it.stockQuantity > 0 }
                        if (restocked.isNotEmpty()) {
                            restocked.forEach { p ->
                                ShopNotificationHelper.notifyBackInStock(appContext, p.id, p.title)
                            }
                            val remaining = watchedIds - restocked.map { it.id }.toSet()
                            prefs.edit().putStringSet("back_in_stock_product_ids", remaining).apply()
                        }
                    }

                    // Check if Admin published a newer APK version on Cloud (manifest or store_profile)
                    val manifest = syncService.fetchAppUpdateManifest().getOrNull()
                    val remoteProfile = syncService.syncStoreProfileWithCloud(
                        localProfile = com.example.data.model.StoreProfileConfig(),
                        preferLocal = false
                    ).getOrNull()
                    val effectiveCode = maxOf(
                        manifest?.versionCode ?: 1,
                        remoteProfile?.latestAppVersionCode ?: 1
                    )
                    val effectiveName = manifest?.versionName?.takeIf { it.isNotBlank() }
                        ?: remoteProfile?.latestAppVersionName?.takeIf { it.isNotBlank() }
                        ?: "1.0"
                    val effectiveNotes = manifest?.releaseNotes?.takeIf { it.isNotBlank() }
                        ?: remoteProfile?.updateReleaseNotes.orEmpty()

                    val pkgInfo = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
                    val installedCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        pkgInfo.longVersionCode.toInt()
                    } else {
                        @Suppress("DEPRECATION")
                        pkgInfo.versionCode
                    }
                    val installedName = pkgInfo.versionName?.ifBlank { "1.0" } ?: "1.0"
                    val isNewer = effectiveCode > installedCode ||
                        (effectiveCode >= installedCode && !effectiveName.equals(installedName, ignoreCase = true))
                    val versionSig = "$effectiveCode:$effectiveName"
                    val lastNotifiedSig = prefs.getString("last_notified_update_sig", "").orEmpty()
                    val lastNotified = prefs.getInt("last_notified_update_code", 0)
                    if (isNewer && (lastNotifiedSig != versionSig || effectiveCode > lastNotified)) {
                        prefs.edit()
                            .putInt("last_notified_update_code", effectiveCode)
                            .putString("last_notified_update_sig", versionSig)
                            .apply()
                        ShopNotificationHelper.notifyAppUpdateAvailable(
                            context = appContext,
                            versionName = effectiveName,
                            versionCode = effectiveCode,
                            notes = effectiveNotes
                        )
                    }
                }
            } catch (_: Exception) {
            } finally {
                jobFinished(params, false)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean = true

    companion object {
        private const val PERIODIC_JOB_ID = 24701
        private const val RECONNECT_JOB_ID = 24702

        fun schedulePeriodicSync(context: Context) {
            try {
                val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
                if (scheduler.getPendingJob(PERIODIC_JOB_ID) != null) return
                val component = ComponentName(context, CatalogBackgroundSyncJobService::class.java)
                val info = JobInfo.Builder(PERIODIC_JOB_ID, component)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPeriodic(15 * 60 * 1000L) // Minimum 15 min interval supported by JobScheduler
                    .setPersisted(false)
                    .build()
                scheduler.schedule(info)
            } catch (_: Exception) {
            }
        }

        fun scheduleReconnectFlush(context: Context) {
            try {
                val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
                val component = ComponentName(context, CatalogBackgroundSyncJobService::class.java)
                val info = JobInfo.Builder(RECONNECT_JOB_ID, component)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setMinimumLatency(2000L)
                    .setOverrideDeadline(60 * 60 * 1000L)
                    .build()
                scheduler.schedule(info)
            } catch (_: Exception) {
            }
        }
    }
}
