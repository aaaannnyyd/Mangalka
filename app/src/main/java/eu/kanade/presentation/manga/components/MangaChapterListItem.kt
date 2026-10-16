package eu.kanade.presentation.manga.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.BookmarkRemove
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FileDownloadOff
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.RemoveDone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.data.download.model.Download
import me.saket.swipe.SwipeableActionsBox
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkGlassIconButton
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun MangaChapterListItem(
    title: String,
    date: String?,
    readProgress: String?,
    scanlator: String?,
    read: Boolean,
    bookmark: Boolean,
    selected: Boolean,
    downloadIndicatorEnabled: Boolean,
    downloadStateProvider: () -> Download.State,
    downloadProgressProvider: () -> Int,
    chapterSwipeStartAction: LibraryPreferences.ChapterSwipeAction,
    chapterSwipeEndAction: LibraryPreferences.ChapterSwipeAction,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    onDownloadClick: ((ChapterDownloadAction) -> Unit)?,
    onChapterSwipe: (LibraryPreferences.ChapterSwipeAction) -> Unit,
    onCommentsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val swipeBackground = InkGlassTokens.AccentPressed.copy(alpha = 0.58f)
    val start = getSwipeAction(
        action = chapterSwipeStartAction,
        read = read,
        bookmark = bookmark,
        downloadState = downloadStateProvider(),
        background = swipeBackground,
        onSwipe = { onChapterSwipe(chapterSwipeStartAction) },
    )
    val end = getSwipeAction(
        action = chapterSwipeEndAction,
        read = read,
        bookmark = bookmark,
        downloadState = downloadStateProvider(),
        background = swipeBackground,
        onSwipe = { onChapterSwipe(chapterSwipeEndAction) },
    )

    SwipeableActionsBox(
        modifier = Modifier.clipToBounds(),
        startActions = listOfNotNull(start),
        endActions = listOfNotNull(end),
        swipeThreshold = swipeActionThreshold,
        backgroundUntilSwipeThreshold = InkGlassTokens.BackgroundPrimary,
    ) {
        InkGlassSurface(
            modifier = modifier.padding(
                horizontal = InkGlassTokens.ScreenHorizontal,
                vertical = InkGlassTokens.Space4,
            ),
            level = if (selected || readProgress != null) InkGlassLevel.Accent else InkGlassLevel.Soft,
            contentPadding = PaddingValues(
                start = InkGlassTokens.Space16,
                top = InkGlassTokens.Space12,
                end = InkGlassTokens.Space8,
                bottom = InkGlassTokens.Space12,
            ),
            shadowElevation = 0.dp,
            onClick = onClick,
            onLongClick = onLongClick,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space4),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        var textHeight by remember { mutableIntStateOf(0) }
                        if (read) {
                            Icon(
                                imageVector = Icons.Outlined.Done,
                                contentDescription = stringResource(MR.strings.action_mark_as_read),
                                modifier = Modifier.size(16.dp),
                                tint = InkGlassTokens.TextTertiary,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Circle,
                                contentDescription = stringResource(MR.strings.unread),
                                modifier = Modifier.size(7.dp),
                                tint = InkGlassTokens.Accent,
                            )
                        }
                        if (bookmark) {
                            Icon(
                                imageVector = Icons.Filled.Bookmark,
                                contentDescription = stringResource(MR.strings.action_filter_bookmarked),
                                modifier = Modifier.sizeIn(
                                    maxHeight = with(LocalDensity.current) {
                                        textHeight.toDp() -
                                            2.dp
                                    },
                                ),
                                tint = InkGlassTokens.Accent,
                            )
                        }
                        Text(
                            text = title,
                            style = InkTypography.TitleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = { textHeight = it.size.height },
                            color = if (read) InkGlassTokens.TextTertiary else InkGlassTokens.TextPrimary,
                        )
                    }

                    val metadata = listOfNotNull(
                        date,
                        readProgress,
                        scanlator?.takeIf { it.isNotBlank() },
                    ).joinToString(" · ")
                    if (metadata.isNotBlank()) {
                        Text(
                            text = metadata,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = InkTypography.BodySmall,
                            color = when {
                                read -> InkGlassTokens.TextTertiary
                                readProgress != null -> InkGlassTokens.Accent
                                else -> InkGlassTokens.TextSecondary
                            },
                        )
                    }
                }

                if (onCommentsClick != null) {
                    InkGlassIconButton(
                        imageVector = Icons.Outlined.Forum,
                        contentDescription = stringResource(MR.strings.ml_g3_chapter_comments),
                        onClick = onCommentsClick,
                    )
                }

                ChapterDownloadIndicator(
                    enabled = downloadIndicatorEnabled,
                    modifier = Modifier.padding(start = InkGlassTokens.Space4),
                    downloadStateProvider = downloadStateProvider,
                    downloadProgressProvider = downloadProgressProvider,
                    onClick = { onDownloadClick?.invoke(it) },
                )
            }
        }
    }
}

private fun getSwipeAction(
    action: LibraryPreferences.ChapterSwipeAction,
    read: Boolean,
    bookmark: Boolean,
    downloadState: Download.State,
    background: Color,
    onSwipe: () -> Unit,
): me.saket.swipe.SwipeAction? {
    return when (action) {
        LibraryPreferences.ChapterSwipeAction.ToggleRead -> swipeAction(
            icon = if (!read) Icons.Outlined.Done else Icons.Outlined.RemoveDone,
            background = background,
            isUndo = read,
            onSwipe = onSwipe,
        )
        LibraryPreferences.ChapterSwipeAction.ToggleBookmark -> swipeAction(
            icon = if (!bookmark) Icons.Outlined.BookmarkAdd else Icons.Outlined.BookmarkRemove,
            background = background,
            isUndo = bookmark,
            onSwipe = onSwipe,
        )
        LibraryPreferences.ChapterSwipeAction.Download -> swipeAction(
            icon = when (downloadState) {
                Download.State.NOT_DOWNLOADED, Download.State.ERROR -> Icons.Outlined.Download
                Download.State.QUEUE, Download.State.DOWNLOADING -> Icons.Outlined.FileDownloadOff
                Download.State.DOWNLOADED -> Icons.Outlined.Delete
            },
            background = background,
            onSwipe = onSwipe,
        )
        LibraryPreferences.ChapterSwipeAction.Disabled -> null
    }
}

private fun swipeAction(
    onSwipe: () -> Unit,
    icon: ImageVector,
    background: Color,
    isUndo: Boolean = false,
): me.saket.swipe.SwipeAction {
    return me.saket.swipe.SwipeAction(
        icon = {
            Icon(
                modifier = Modifier.padding(16.dp),
                imageVector = icon,
                tint = InkGlassTokens.TextPrimary,
                contentDescription = null,
            )
        },
        background = background,
        onSwipe = onSwipe,
        isUndo = isUndo,
    )
}

private val swipeActionThreshold = 56.dp
