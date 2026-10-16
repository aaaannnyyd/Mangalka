package eu.kanade.tachiyomi.ui.more

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
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
                // Telegram posts carry the APK as an attachment that cannot be fetched without a
                // bot token, so without a direct mirror link the post is opened in Telegram.
                when {
                    !downloadLink.isDirectApk() -> context.openTelegramPost(downloadLink)
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

private val TELEGRAM_POST = Regex("""^https?://t\.me/([A-Za-z0-9_]+)/(\d+)""")

/** Opens a `https://t.me/<channel>/<post>` link in the Telegram app, falling back to the browser. */
private fun Context.openTelegramPost(url: String) {
    val match = TELEGRAM_POST.find(url) ?: return openInBrowser(url)
    val (channel, post) = match.destructured
    val intent = Intent(Intent.ACTION_VIEW, "tg://resolve?domain=$channel&post=$post".toUri())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        openInBrowser(url)
    }
}
