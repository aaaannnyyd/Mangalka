package eu.kanade.tachiyomi.ui.more

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import eu.kanade.presentation.more.NewUpdateScreen
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.util.system.openInBrowser

class NewUpdateScreen(
    private val versionName: String,
    private val changelogInfo: String,
    private val releaseLink: String,
    private val downloadLink: String,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val viewModel =
            assistedMetroViewModel<NewUpdateScreenModel, NewUpdateScreenModel.Factory> {
                create(changelogInfo = changelogInfo, downloadLink = downloadLink)
            }

        val state by viewModel.state.collectAsState()

        NewUpdateScreen(
            versionName = versionName,
            stage = state.stage,
            downloadProgress = { state.downloadProgress },
            changelogInfo = state.changelogInfo,
            onOpenInBrowser = { context.openInBrowser(releaseLink) },
            onAcceptUpdate = {
                // Without an APK asset for this device the release page is opened instead.
                when {
                    !downloadLink.isDirectApk() -> context.openInBrowser(releaseLink)
                    state.stage == NewUpdateScreenModel.Stage.Available ||
                        state.stage == NewUpdateScreenModel.Stage.Failed -> viewModel.startDownload()
                    state.stage == NewUpdateScreenModel.Stage.Downloaded -> viewModel.installUpdate()
                    else -> Unit
                }
            },
            onRejectUpdate = navigator::pop,
        )
    }
}

private fun String.isDirectApk(): Boolean = substringBefore('?').endsWith(".apk", ignoreCase = true)
