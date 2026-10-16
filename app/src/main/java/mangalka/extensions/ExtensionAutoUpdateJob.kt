package mangalka.extensions

import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import dev.zacsweers.metro.Inject
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.extension.api.ExtensionUpdateNotifier
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.util.system.notify
import eu.kanade.tachiyomi.util.system.workManager
import logcat.LogPriority
import mihon.app.di.AppGraph
import mihon.app.di.appGraph
import mihon.core.metro.metroGraph
import tachiyomi.core.common.util.system.logcat
import java.util.concurrent.TimeUnit

/**
 * Periodically installs extension updates. Sites change their markup often and the fix usually ships as an
 * extension update within a day, so this keeps sources working without the user visiting the Extensions tab.
 * Updates that would need a system confirmation are left to the regular "updates available" notification.
 */
class ExtensionAutoUpdateJob(private val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private val graph: AppGraph = context.metroGraph()

    @Inject lateinit var updater: SourceExtensionUpdater

    @Inject lateinit var basePreferences: BasePreferences

    @Inject lateinit var sourcePreferences: SourcePreferences

    @Inject lateinit var securityPreferences: SecurityPreferences

    @Inject lateinit var updateNotifier: ExtensionUpdateNotifier

    override suspend fun doWork(): Result {
        graph.inject(this)
        if (!sourcePreferences.autoUpdateExtensions.get()) return Result.success()

        val updates = try {
            updater.findUpdates() ?: return Result.retry()
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            return Result.retry()
        }
        if (updates.isEmpty()) return Result.success()

        val (silent, needsConfirmation) = updates.partition { it.canUpdateSilently() }
        val updated = mutableListOf<String>()
        val failed = mutableListOf<Extension.Installed>()
        for (extension in silent) {
            val result = runCatching { updater.install(extension) }.getOrDefault(SourceExtensionUpdater.Result.Failed)
            if (result == SourceExtensionUpdater.Result.Updated) updated += extension.name else failed += extension
        }

        if (updated.isNotEmpty()) notifyUpdated(updated)
        val remaining = needsConfirmation + failed
        if (remaining.isNotEmpty()) updateNotifier.promptUpdates(remaining.map { it.name })
        return Result.success()
    }

    /**
     * Private installs live inside the app; Shizuku installs without prompts; the system package installer can
     * update without a prompt on Android 12+ when this app installed the extension.
     */
    private fun Extension.Installed.canUpdateSilently(): Boolean {
        if (!isShared) return true
        return when (basePreferences.extensionInstaller.get()) {
            BasePreferences.ExtensionInstaller.PRIVATE, BasePreferences.ExtensionInstaller.SHIZUKU -> true
            BasePreferences.ExtensionInstaller.PACKAGEINSTALLER -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            else -> false
        }
    }

    private fun notifyUpdated(names: List<String>) {
        context.notify(ID_AUTO_UPDATED, Notifications.CHANNEL_EXTENSIONS_UPDATE) {
            setSmallIcon(R.drawable.ic_extension_24dp)
            setContentTitle(if (names.size == 1) "Расширение обновлено" else "Обновлено расширений: ${names.size}")
            if (!securityPreferences.hideNotificationContent.get()) {
                val text = names.joinToString(", ")
                setContentText(text)
                setStyle(NotificationCompat.BigTextStyle().bigText(text))
            }
            setContentIntent(NotificationReceiver.openExtensionsPendingActivity(context))
            setAutoCancel(true)
        }
    }

    companion object {
        private const val TAG = "ExtensionAutoUpdate"
        private const val ID_AUTO_UPDATED = -402

        fun setupTask(context: Context, enabled: Boolean? = null) {
            val isEnabled = enabled ?: context.appGraph.sourcePreferences.autoUpdateExtensions.get()
            if (!isEnabled) {
                context.workManager.cancelUniqueWork(TAG)
                return
            }
            val request = PeriodicWorkRequestBuilder<ExtensionAutoUpdateJob>(12, TimeUnit.HOURS, 2, TimeUnit.HOURS)
                .setConstraints(Constraints(requiredNetworkType = NetworkType.UNMETERED, requiresBatteryNotLow = true))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .addTag(TAG)
                .build()
            context.workManager.enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }
}
