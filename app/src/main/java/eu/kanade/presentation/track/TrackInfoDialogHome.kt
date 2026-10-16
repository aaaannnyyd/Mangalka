package eu.kanade.presentation.track

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.presentation.track.components.TrackLogoIcon
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.ui.manga.track.TrackItem
import eu.kanade.tachiyomi.util.lang.toJavaLocalDate
import eu.kanade.tachiyomi.util.lang.toLocalDate
import eu.kanade.tachiyomi.util.system.copyToClipboard
import kotlinx.datetime.toJavaLocalDate
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkAccentButton
import tachiyomi.presentation.core.components.InkGlassIconButton
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkMangaBackdrop
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.i18n.stringResource
import java.time.format.DateTimeFormatter

@Composable
fun TrackInfoDialogHome(
    trackItems: List<TrackItem>,
    dateFormat: DateTimeFormatter,
    onStatusClick: (TrackItem) -> Unit,
    onChapterClick: (TrackItem) -> Unit,
    onScoreClick: (TrackItem) -> Unit,
    onStartDateEdit: (TrackItem) -> Unit,
    onEndDateEdit: (TrackItem) -> Unit,
    onNewSearch: (TrackItem) -> Unit,
    onOpenInBrowser: (TrackItem) -> Unit,
    onRemoved: (TrackItem) -> Unit,
    onCopyLink: (TrackItem) -> Unit,
    onTogglePrivate: (TrackItem) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = InkGlassTokens.Space8),
    ) {
        InkMangaBackdrop(intensity = 0.45f)
        Column(
            modifier = Modifier
                .animateContentSize()
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(InkGlassTokens.Space20)
                .windowInsetsPadding(WindowInsets.systemBars),
            verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space16),
        ) {
            Text("ТРЕКИНГ", style = InkTypography.Eyebrow, color = InkGlassTokens.Accent)
            Text("Прогресс чтения", style = InkTypography.Headline, color = InkGlassTokens.TextPrimary)

            trackItems.forEach { item ->
                if (item.track != null) {
                    val supportsScoring = item.tracker.getScoreList().isNotEmpty()
                    val supportsReadingDates = item.tracker.supportsReadingDates
                    val supportsPrivate = item.tracker.supportsPrivateTracking
                    TrackInfoItem(
                        title = item.track.title,
                        tracker = item.tracker,
                        status = item.tracker.getStatus(item.track.status),
                        onStatusClick = { onStatusClick(item) },
                        chapters = "${item.track.lastChapterRead.toInt()}".let {
                            val totalChapters = item.track.totalChapters
                            if (totalChapters > 0) "$it / $totalChapters" else it
                        },
                        onChaptersClick = { onChapterClick(item) },
                        score = item.tracker.displayScore(item.track)
                            .takeIf { supportsScoring && item.track.score != 0.0 },
                        onScoreClick = { onScoreClick(item) }.takeIf { supportsScoring },
                        startDate = remember(item.track.startDate) {
                            dateFormat.format(item.track.startDate.toLocalDate().toJavaLocalDate())
                        }.takeIf { supportsReadingDates && item.track.startDate != 0L },
                        onStartDateClick = { onStartDateEdit(item) }.takeIf { supportsReadingDates },
                        endDate = dateFormat.format(item.track.finishDate.toJavaLocalDate())
                            .takeIf { supportsReadingDates && item.track.finishDate != 0L },
                        onEndDateClick = { onEndDateEdit(item) }.takeIf { supportsReadingDates },
                        onNewSearch = { onNewSearch(item) },
                        onOpenInBrowser = { onOpenInBrowser(item) },
                        onRemoved = { onRemoved(item) },
                        onCopyLink = { onCopyLink(item) },
                        private = item.track.private,
                        onTogglePrivate = { onTogglePrivate(item) }.takeIf { supportsPrivate },
                    )
                } else {
                    TrackInfoItemEmpty(
                        tracker = item.tracker,
                        onNewSearch = { onNewSearch(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackInfoItem(
    title: String,
    tracker: Tracker,
    status: StringResource?,
    onStatusClick: () -> Unit,
    chapters: String,
    onChaptersClick: () -> Unit,
    score: String?,
    onScoreClick: (() -> Unit)?,
    startDate: String?,
    onStartDateClick: (() -> Unit)?,
    endDate: String?,
    onEndDateClick: (() -> Unit)?,
    onNewSearch: () -> Unit,
    onOpenInBrowser: () -> Unit,
    onRemoved: () -> Unit,
    onCopyLink: () -> Unit,
    private: Boolean,
    onTogglePrivate: (() -> Unit)?,
) {
    val context = LocalContext.current
    InkGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        level = InkGlassLevel.Strong,
        shape = RoundedCornerShape(InkGlassTokens.RadiusCard),
        contentPadding = PaddingValues(InkGlassTokens.Space16),
        shadowElevation = InkGlassTokens.Space4,
        prominent = true,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12),
            ) {
                Box {
                    TrackLogoIcon(
                        tracker = tracker,
                        onClick = onOpenInBrowser,
                        onLongClick = onCopyLink,
                    )
                    if (private) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(19.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            InkGlassSurface(
                                modifier = Modifier.fillMaxSize(),
                                level = InkGlassLevel.Accent,
                                shape = CircleShape,
                                contentPadding = PaddingValues(0.dp),
                                shadowElevation = 0.dp,
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.VisibilityOff,
                                        contentDescription = stringResource(MR.strings.tracked_privately),
                                        modifier = Modifier.size(11.dp),
                                        tint = InkGlassTokens.Accent,
                                    )
                                }
                            }
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .combinedClickable(
                            role = Role.Button,
                            onClick = onNewSearch,
                            onLongClick = { context.copyToClipboard(title, title) },
                        ),
                    verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space4),
                ) {
                    Text(
                        text = title,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = InkTypography.Title,
                        color = InkGlassTokens.TextPrimary,
                    )
                    Text(
                        text = tracker.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = InkTypography.BodySmall,
                        color = InkGlassTokens.TextSecondary,
                    )
                }
                TrackInfoItemMenu(
                    onOpenInBrowser = onOpenInBrowser,
                    onRemoved = onRemoved,
                    onCopyLink = onCopyLink,
                    private = private,
                    onTogglePrivate = onTogglePrivate,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
            ) {
                TrackDetailsItem(
                    modifier = Modifier.weight(1f),
                    label = "Статус",
                    text = status?.let { stringResource(it) } ?: "—",
                    onClick = onStatusClick,
                )
                TrackDetailsItem(
                    modifier = Modifier.weight(1f),
                    label = "Главы",
                    text = chapters,
                    onClick = onChaptersClick,
                )
                if (onScoreClick != null) {
                    TrackDetailsItem(
                        modifier = Modifier.weight(1f),
                        label = "Оценка",
                        text = score ?: "—",
                        onClick = onScoreClick,
                    )
                }
            }

            if (onStartDateClick != null && onEndDateClick != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
                ) {
                    TrackDetailsItem(
                        modifier = Modifier.weight(1f),
                        label = "Начато",
                        text = startDate ?: "—",
                        onClick = onStartDateClick,
                    )
                    TrackDetailsItem(
                        modifier = Modifier.weight(1f),
                        label = "Завершено",
                        text = endDate ?: "—",
                        onClick = onEndDateClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackDetailsItem(
    label: String,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InkGlassSurface(
        modifier = modifier.fillMaxHeight(),
        level = InkGlassLevel.Soft,
        shape = RoundedCornerShape(InkGlassTokens.RadiusSmall),
        contentPadding = PaddingValues(
            horizontal = InkGlassTokens.Space8,
            vertical = InkGlassTokens.Space12,
        ),
        shadowElevation = 0.dp,
        onClick = onClick,
        role = Role.Button,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space4),
        ) {
            Text(
                text = label.uppercase(),
                maxLines = 1,
                style = InkTypography.Eyebrow,
                color = InkGlassTokens.TextTertiary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = InkTypography.Label,
                color = if (text == "—") InkGlassTokens.TextTertiary else InkGlassTokens.TextPrimary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun TrackInfoItemEmpty(
    tracker: Tracker,
    onNewSearch: () -> Unit,
) {
    InkGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        level = InkGlassLevel.Soft,
        contentPadding = PaddingValues(InkGlassTokens.Space16),
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space16),
        ) {
            TrackLogoIcon(tracker)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space4),
            ) {
                Text(tracker.name, style = InkTypography.TitleSmall, color = InkGlassTokens.TextPrimary)
                Text("Не подключено", style = InkTypography.BodySmall, color = InkGlassTokens.TextTertiary)
            }
            InkAccentButton(
                label = stringResource(MR.strings.add_tracking),
                onClick = onNewSearch,
            )
        }
    }
}

@Composable
private fun TrackInfoItemMenu(
    onOpenInBrowser: () -> Unit,
    onRemoved: () -> Unit,
    onCopyLink: () -> Unit,
    private: Boolean,
    onTogglePrivate: (() -> Unit)?,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        InkGlassIconButton(
            imageVector = Icons.Default.MoreVert,
            contentDescription = stringResource(MR.strings.label_more),
            onClick = { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(MR.strings.action_open_in_browser)) },
                onClick = {
                    onOpenInBrowser()
                    expanded = false
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(MR.strings.action_copy_link)) },
                onClick = {
                    onCopyLink()
                    expanded = false
                },
            )
            if (onTogglePrivate != null) {
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                if (private) {
                                    MR.strings.action_toggle_private_off
                                } else {
                                    MR.strings.action_toggle_private_on
                                },
                            ),
                        )
                    },
                    onClick = {
                        onTogglePrivate()
                        expanded = false
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(MR.strings.action_remove), color = InkGlassTokens.Accent) },
                onClick = {
                    onRemoved()
                    expanded = false
                },
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun TrackInfoDialogHomePreviews(
    @PreviewParameter(TrackInfoDialogHomePreviewProvider::class)
    content: @Composable () -> Unit,
) {
    TachiyomiPreviewTheme {
        Box(Modifier.fillMaxSize()) {
            content()
        }
    }
}
