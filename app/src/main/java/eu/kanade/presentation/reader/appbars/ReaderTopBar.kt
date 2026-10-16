package eu.kanade.presentation.reader.appbars

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkGlassDropdownMenu
import tachiyomi.presentation.core.components.InkGlassIconButton
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ReaderTopBar(
    mangaTitle: String?,
    chapterTitle: String?,
    navigateUp: () -> Unit,
    bookmarked: Boolean,
    onToggleBookmarked: () -> Unit,
    onOpenInWebView: (() -> Unit)?,
    onOpenInBrowser: (() -> Unit)?,
    onShare: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val hasOverflow = onOpenInWebView != null || onOpenInBrowser != null || onShare != null

    InkGlassSurface(
        modifier = modifier.fillMaxWidth(),
        level = InkGlassLevel.Strong,
        shape = RoundedCornerShape(InkGlassTokens.RadiusLargeCard),
        contentPadding = PaddingValues(
            start = InkGlassTokens.Space8,
            top = InkGlassTokens.Space8,
            end = InkGlassTokens.Space8,
            bottom = InkGlassTokens.Space8,
        ),
        shadowElevation = InkGlassTokens.Space8,
        prominent = true,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InkGlassIconButton(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Назад",
                onClick = navigateUp,
            )
            Spacer(Modifier.width(InkGlassTokens.Space12))
            Column(Modifier.weight(1f)) {
                Text(
                    text = mangaTitle.orEmpty(),
                    style = InkTypography.TitleSmall,
                    color = InkGlassTokens.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!chapterTitle.isNullOrBlank()) {
                    Text(
                        text = chapterTitle,
                        style = InkTypography.BodySmall,
                        color = InkGlassTokens.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            InkGlassIconButton(
                imageVector = if (bookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = stringResource(
                    if (bookmarked) MR.strings.action_remove_bookmark else MR.strings.action_bookmark,
                ),
                onClick = onToggleBookmarked,
                selected = bookmarked,
            )
            if (hasOverflow) {
                Spacer(Modifier.width(InkGlassTokens.Space4))
                androidx.compose.foundation.layout.Box {
                    InkGlassIconButton(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "Ещё",
                        onClick = { menuExpanded = true },
                    )
                    InkGlassDropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        onOpenInWebView?.let { action ->
                            DropdownMenuItem(
                                text = { Text(stringResource(MR.strings.action_open_in_web_view)) },
                                onClick = {
                                    menuExpanded = false
                                    action()
                                },
                            )
                        }
                        onOpenInBrowser?.let { action ->
                            DropdownMenuItem(
                                text = { Text(stringResource(MR.strings.action_open_in_browser)) },
                                onClick = {
                                    menuExpanded = false
                                    action()
                                },
                            )
                        }
                        onShare?.let { action ->
                            DropdownMenuItem(
                                text = { Text(stringResource(MR.strings.action_share)) },
                                onClick = {
                                    menuExpanded = false
                                    action()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
