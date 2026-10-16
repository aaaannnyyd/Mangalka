package mangalka.extensions

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withTimeoutOrNull

/** Updates installed extensions from their stores; shared by the manga-details recovery banner and the job. */
@Inject
@SingleIn(AppScope::class)
class SourceExtensionUpdater(
    private val extensionManager: ExtensionManager,
) {
    enum class Result { Updated, NoUpdate, NotInstalled, StoreUnavailable, Failed }

    suspend fun updateForSource(sourceId: Long, onStep: (InstallStep) -> Unit = {}): Result {
        if (!extensionManager.findAvailableExtensions(silent = true)) return Result.StoreUnavailable
        val extension = installed().firstOrNull { ext -> ext.sources.any { it.id == sourceId } }
            ?: return Result.NotInstalled
        if (!extension.hasUpdate) return Result.NoUpdate
        return install(extension, onStep)
    }

    /** Refreshes the stores and returns installed extensions that have a newer version. */
    suspend fun findUpdates(): List<Extension.Installed>? {
        if (!extensionManager.findAvailableExtensions(silent = true)) return null
        return installed().filter { it.hasUpdate }
    }

    suspend fun install(extension: Extension.Installed, onStep: (InstallStep) -> Unit = {}): Result {
        val last = withTimeoutOrNull(INSTALL_TIMEOUT_MILLIS) {
            extensionManager.updateExtension(extension)
                .onEach(onStep)
                // Empty when the store entry vanished in the meantime.
                .firstOrNull { it.isCompleted() }
        }
        if (last == null) extensionManager.cancelInstallUpdateExtension(extension)
        return if (last == InstallStep.Installed) Result.Updated else Result.Failed
    }

    private suspend fun installed() = extensionManager.installedExtensionsFlow.first()

    private companion object {
        const val INSTALL_TIMEOUT_MILLIS = 3L * 60L * 1000L
    }
}
