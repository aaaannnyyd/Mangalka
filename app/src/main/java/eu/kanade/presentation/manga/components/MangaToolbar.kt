package eu.kanade.presentation.manga.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.presentation.components.DownloadDropdownMenu
import eu.kanade.presentation.manga.DownloadAction
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkGlassDropdownMenu
import tachiyomi.presentation.core.components.InkGlassIconButton
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun MangaToolbar(
    title: String,
    hasFilters: Boolean,
    navigateUp: () -> Unit,
    onClickFilter: () -> Unit,
    onClickShare: (() -> Unit)?,
    onClickDownload: ((DownloadAction) -> Unit)?,
    onClickEditCategory: (() -> Unit)?,
    onClickRefresh: () -> Unit,
    onClickMigrate: (() -> Unit)?,
    onClickEditNotes: () -> Unit,
    onClickLinkedVersions: () -> Unit,
    actionModeCounter: Int,
    onCancelActionMode: () -> Unit,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    titleAlphaProvider: () -> Float,
    @Suppress("UNUSED_PARAMETER") backgroundAlphaProvider: () -> Float,
    modifier: Modifier = Modifier,
) {
    val isActionMode = actionModeCounter > 0
    if (isActionMode) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .padding(
                    start = InkGlassTokens.Space16,
                    end = InkGlassTokens.Space12,
                    top = InkGlassTokens.Space8,
                    bottom = InkGlassTokens.Space8,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
        ) {
            InkGlassIconButton(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(MR.strings.action_cancel),
                onClick = onCancelActionMode,
                selected = true,
            )
            Text(
                text = actionModeCounter.toString(),
                modifier = Modifier.weight(1f),
                style = InkTypography.Title,
                color = InkGlassTokens.TextPrimary,
            )
            InkGlassIconButton(
                imageVector = Icons.Outlined.SelectAll,
                contentDescription = stringResource(MR.strings.action_select_all),
                onClick = onSelectAll,
            )
            InkGlassIconButton(
                imageVector = Icons.Outlined.FlipToBack,
                contentDescription = stringResource(MR.strings.action_select_inverse),
                onClick = onInvertSelection,
            )
        }
        return
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .padding(
                start = InkGlassTokens.Space16,
                end = InkGlassTokens.Space12,
                top = InkGlassTokens.Space8,
                bottom = InkGlassTokens.Space8,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
    ) {
        InkGlassIconButton(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = stringResource(MR.strings.ml_g2_back),
            onClick = navigateUp,
        )
        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .alpha(titleAlphaProvider()),
            style = InkTypography.TitleSmall,
            color = InkGlassTokens.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (onClickShare != null) {
            InkGlassIconButton(
                imageVector = Icons.Outlined.Share,
                contentDescription = stringResource(MR.strings.action_share),
                onClick = onClickShare,
            )
        }

        var downloadExpanded by remember { mutableStateOf(false) }
        if (onClickDownload != null) {
            DownloadDropdownMenu(
                expanded = downloadExpanded,
                onDismissRequest = { downloadExpanded = false },
                onDownloadClicked = onClickDownload,
            )
            InkGlassIconButton(
                imageVector = Icons.Outlined.Download,
                contentDescription = stringResource(MR.strings.manga_download),
                onClick = { downloadExpanded = !downloadExpanded },
            )
        }
        var overflowExpanded by remember { mutableStateOf(false) }
        Box {
            InkGlassIconButton(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = stringResource(MR.strings.action_menu_overflow_description),
                onClick = { overflowExpanded = true },
                selected = hasFilters,
            )
            InkGlassDropdownMenu(
                expanded = overflowExpanded,
                onDismissRequest = { overflowExpanded = false },
            ) {
                @Composable
                fun Item(label: String, action: () -> Unit) {
                    DropdownMenuItem(
                        text = { Text(label, color = InkGlassTokens.TextPrimary) },
                        onClick = {
                            action()
                            overflowExpanded = false
                        },
                    )
                }

                Item(stringResource(MR.strings.action_filter), onClickFilter)
                Item(stringResource(MR.strings.action_webview_refresh), onClickRefresh)
                if (onClickEditCategory != null) {
                    Item(stringResource(MR.strings.action_edit_categories), onClickEditCategory)
                }
                if (onClickMigrate != null) {
                    Item(stringResource(MR.strings.action_migrate), onClickMigrate)
                }
                Item(stringResource(MR.strings.action_notes), onClickEditNotes)
                Item(stringResource(MR.strings.ml_g2_versions_and_sources), onClickLinkedVersions)
            }
        }
    }
}
