package eu.kanade.presentation.more

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import eu.kanade.presentation.manga.components.MarkdownRender
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.tachiyomi.ui.more.NewUpdateScreenModel
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkGlassButton
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.InfoScreen

@Composable
fun NewUpdateScreen(
    versionName: String,
    changelogInfo: String,
    stage: NewUpdateScreenModel.Stage,
    downloadProgress: () -> Int,
    onOpenInBrowser: () -> Unit,
    onAcceptUpdate: () -> Unit,
    onRejectUpdate: () -> Unit,
) {
    InfoScreen(
        icon = Icons.Outlined.NewReleases,
        headingText = stringResource(MR.strings.update_check_notification_update_available),
        subtitleText = versionName,
        acceptText = when (stage) {
            NewUpdateScreenModel.Stage.Available -> stringResource(MR.strings.update_check_confirm)
            NewUpdateScreenModel.Stage.Downloading -> stringResource(
                MR.strings.downloading_with_progress,
                downloadProgress(),
            )
            NewUpdateScreenModel.Stage.Downloaded -> stringResource(MR.strings.action_install)
            NewUpdateScreenModel.Stage.Failed -> stringResource(MR.strings.action_retry)
        },
        onAcceptClick = onAcceptUpdate,
        canAccept = stage != NewUpdateScreenModel.Stage.Downloading,
        rejectText = stringResource(MR.strings.action_not_now),
        onRejectClick = onRejectUpdate,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = InkGlassTokens.Space16),
        ) {
            MarkdownRender(
                content = changelogInfo,
                flavour = remember { GFMFlavourDescriptor() },
            )
            InkGlassButton(
                label = stringResource(MR.strings.update_check_open),
                icon = Icons.AutoMirrored.Outlined.OpenInNew,
                onClick = onOpenInBrowser,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = InkGlassTokens.Space16),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun NewUpdateScreenPreview() {
    TachiyomiPreviewTheme {
        NewUpdateScreen(
            versionName = "v0.99.9",
            changelogInfo = """
                ## Yay
                Foobar

                ### More info
                - Hello
                - World
            """.trimIndent(),
            stage = NewUpdateScreenModel.Stage.Available,
            downloadProgress = { 0 },
            onOpenInBrowser = {},
            onAcceptUpdate = {},
            onRejectUpdate = {},
        )
    }
}
