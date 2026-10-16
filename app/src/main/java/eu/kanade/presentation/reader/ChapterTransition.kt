package eu.kanade.presentation.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.tachiyomi.data.database.models.toDomainChapter
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.service.calculateChapterGap
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkAccentButton
import tachiyomi.presentation.core.components.InkGlassButton
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ChapterTransition(
    transition: ChapterTransition,
    currChapterDownloaded: Boolean,
    goingToChapterDownloaded: Boolean,
    onNextChapter: (() -> Unit)? = null,
    onChapterComments: (() -> Unit)? = null,
) {
    val currChapter = transition.from.chapter.toDomainChapter()
    val goingToChapter = transition.to?.chapter?.toDomainChapter()

    when (transition) {
        is ChapterTransition.Prev -> {
            TransitionText(
                topLabel = stringResource(MR.strings.transition_previous),
                topChapter = goingToChapter,
                topChapterDownloaded = goingToChapterDownloaded,
                bottomLabel = stringResource(MR.strings.transition_current),
                bottomChapter = currChapter,
                bottomChapterDownloaded = currChapterDownloaded,
                fallbackLabel = stringResource(MR.strings.transition_no_previous),
                chapterGap = calculateChapterGap(currChapter, goingToChapter),
                onTopChapterComments = null,
            )
        }
        is ChapterTransition.Next -> {
            EndOfChapterCard(
                currentChapter = currChapter,
                nextChapter = goingToChapter,
                nextChapterDownloaded = goingToChapterDownloaded,
                fallbackLabel = stringResource(MR.strings.transition_no_next),
                chapterGap = calculateChapterGap(goingToChapter, currChapter),
                onNextChapter = onNextChapter,
                onChapterComments = onChapterComments,
            )
        }
    }
}

@Composable
private fun TransitionText(
    topLabel: String,
    topChapter: Chapter?,
    topChapterDownloaded: Boolean,
    bottomLabel: String,
    bottomChapter: Chapter?,
    bottomChapterDownloaded: Boolean,
    fallbackLabel: String,
    chapterGap: Int,
    onTopChapterComments: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .widthIn(max = 460.dp)
            .fillMaxWidth()
            .padding(horizontal = InkGlassTokens.Space8),
        verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12),
    ) {
        if (topChapter == null) {
            NoChapterNotification(
                text = fallbackLabel,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            return@Column
        }

        InkGlassSurface(
            modifier = Modifier.fillMaxWidth(),
            level = InkGlassLevel.Strong,
            shape = RoundedCornerShape(InkGlassTokens.RadiusCard),
            contentPadding = PaddingValues(InkGlassTokens.Space20),
            shadowElevation = InkGlassTokens.Space4,
            prominent = true,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12)) {
                Text(
                    text = topLabel.uppercase(),
                    style = InkTypography.Eyebrow,
                    color = InkGlassTokens.Accent,
                )
                ChapterIdentity(
                    name = topChapter.name,
                    scanlator = topChapter.scanlator,
                    downloaded = topChapterDownloaded,
                )

                if (onTopChapterComments != null) {
                    InkGlassButton(
                        label = stringResource(MR.strings.ml_g3_chapter_comments),
                        icon = Icons.Outlined.Forum,
                        onClick = onTopChapterComments,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (bottomChapter != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(InkGlassTokens.GlassBorderSoft),
                    )
                    Text(
                        text = bottomLabel.uppercase(),
                        style = InkTypography.Eyebrow,
                        color = InkGlassTokens.TextTertiary,
                    )
                    ChapterIdentity(
                        name = bottomChapter.name,
                        scanlator = bottomChapter.scanlator,
                        downloaded = bottomChapterDownloaded,
                        compact = true,
                    )
                } else {
                    Text(
                        text = fallbackLabel,
                        style = InkTypography.BodySmall,
                        color = InkGlassTokens.TextTertiary,
                    )
                }
            }
        }

        if (bottomChapter != null && chapterGap > 0) {
            ChapterGapWarning(
                gapCount = chapterGap,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun ChapterIdentity(
    name: String,
    scanlator: String?,
    downloaded: Boolean,
    compact: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space4)) {
        Text(
            text = buildAnnotatedString {
                if (downloaded) {
                    appendInlineContent(DOWNLOADED_ICON_ID)
                    append(' ')
                }
                append(name)
            },
            maxLines = if (compact) 2 else 3,
            overflow = TextOverflow.Ellipsis,
            style = if (compact) InkTypography.TitleSmall else InkTypography.Title,
            color = InkGlassTokens.TextPrimary,
            inlineContent = mapOf(
                DOWNLOADED_ICON_ID to InlineTextContent(
                    Placeholder(
                        width = 18.sp,
                        height = 18.sp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = stringResource(MR.strings.label_downloaded),
                        tint = InkGlassTokens.Success,
                        modifier = Modifier.size(18.dp),
                    )
                },
            ),
        )
        scanlator?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = stringResource(MR.strings.ml_g3_translation_by, it),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = InkTypography.BodySmall,
                color = InkGlassTokens.TextSecondary,
            )
        }
    }
}

@Composable
private fun EndOfChapterCard(
    currentChapter: Chapter?,
    nextChapter: Chapter?,
    nextChapterDownloaded: Boolean,
    fallbackLabel: String,
    chapterGap: Int,
    onNextChapter: (() -> Unit)?,
    onChapterComments: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .widthIn(max = 460.dp)
            .fillMaxWidth()
            .padding(horizontal = InkGlassTokens.Space16),
        verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12),
    ) {
        InkGlassSurface(
            modifier = Modifier.fillMaxWidth(),
            level = InkGlassLevel.Strong,
            shape = RoundedCornerShape(InkGlassTokens.RadiusCard),
            contentPadding = PaddingValues(InkGlassTokens.Space20),
            shadowElevation = InkGlassTokens.Space4,
            prominent = true,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12)) {
                Text(
                    text = stringResource(MR.strings.ml_g3_chapter_finished_eyebrow),
                    style = InkTypography.Eyebrow,
                    color = InkGlassTokens.Accent,
                )
                Text(
                    text = currentChapter?.name ?: stringResource(MR.strings.ml_g3_chapter_finished),
                    style = InkTypography.Title,
                    color = InkGlassTokens.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                currentChapter?.scanlator?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = stringResource(MR.strings.ml_g3_translation_by, it),
                        style = InkTypography.BodySmall,
                        color = InkGlassTokens.TextSecondary,
                    )
                }

                if (nextChapter != null && onNextChapter != null) {
                    InkAccentButton(
                        label = stringResource(MR.strings.action_next_chapter),
                        onClick = onNextChapter,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (onChapterComments != null) {
                    InkGlassButton(
                        label = stringResource(MR.strings.ml_g3_chapter_comments),
                        icon = Icons.Outlined.Forum,
                        onClick = onChapterComments,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (chapterGap > 0) {
                    ChapterGapWarning(gapCount = chapterGap)
                }

                if (nextChapter != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(MR.strings.ml_g3_up_next_eyebrow),
                                style = InkTypography.Eyebrow,
                                color = InkGlassTokens.TextTertiary,
                            )
                            Text(
                                text = nextChapter.name,
                                style = InkTypography.TitleSmall,
                                color = InkGlassTokens.TextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (nextChapterDownloaded) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = stringResource(MR.strings.label_downloaded),
                                tint = InkGlassTokens.Success,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                } else {
                    Text(
                        text = fallbackLabel,
                        style = InkTypography.Body,
                        color = InkGlassTokens.TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun NoChapterNotification(
    text: String,
    modifier: Modifier = Modifier,
) {
    InkGlassSurface(
        modifier = modifier.fillMaxWidth(),
        level = InkGlassLevel.Soft,
        shape = RoundedCornerShape(InkGlassTokens.RadiusMedium),
        contentPadding = PaddingValues(InkGlassTokens.Space16),
        shadowElevation = 0.dp,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                tint = InkGlassTokens.Accent,
                contentDescription = null,
            )
            Text(text = text, style = InkTypography.Body, color = InkGlassTokens.TextSecondary)
        }
    }
}

@Composable
private fun ChapterGapWarning(
    gapCount: Int,
    modifier: Modifier = Modifier,
) {
    InkGlassSurface(
        modifier = modifier.fillMaxWidth(),
        level = InkGlassLevel.Accent,
        shape = RoundedCornerShape(InkGlassTokens.RadiusSmall),
        contentPadding = PaddingValues(horizontal = InkGlassTokens.Space16, vertical = InkGlassTokens.Space12),
        shadowElevation = 0.dp,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                tint = InkGlassTokens.Error,
                contentDescription = null,
            )
            Text(
                text = pluralStringResource(MR.plurals.missing_chapters_warning, count = gapCount, gapCount),
                style = InkTypography.Body,
                color = InkGlassTokens.TextPrimary,
            )
        }
    }
}

@Composable
private fun ChapterText(
    header: String,
    name: String,
    scanlator: String?,
    downloaded: Boolean,
) {
    InkGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        level = InkGlassLevel.Strong,
        shape = RoundedCornerShape(InkGlassTokens.RadiusCard),
        contentPadding = PaddingValues(InkGlassTokens.Space20),
        shadowElevation = InkGlassTokens.Space4,
        prominent = true,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8)) {
            Text(
                text = header.uppercase(),
                style = InkTypography.Eyebrow,
                color = InkGlassTokens.Accent,
            )
            Text(
                text = buildAnnotatedString {
                    if (downloaded) {
                        appendInlineContent(DOWNLOADED_ICON_ID)
                        append(' ')
                    }
                    append(name)
                },
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
                style = InkTypography.Title,
                color = InkGlassTokens.TextPrimary,
                inlineContent = mapOf(
                    DOWNLOADED_ICON_ID to InlineTextContent(
                        Placeholder(
                            width = 20.sp,
                            height = 20.sp,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = stringResource(MR.strings.label_downloaded),
                            tint = InkGlassTokens.Success,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                ),
            )
            scanlator?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = stringResource(MR.strings.ml_g3_translation_by, it),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = InkTypography.BodySmall,
                    color = InkGlassTokens.TextSecondary,
                )
            }
        }
    }
}

private const val DOWNLOADED_ICON_ID = "downloaded"

private fun previewChapter(name: String, scanlator: String, chapterNumber: Double) = Chapter.create().copy(
    id = 0L,
    mangaId = 0L,
    url = "",
    name = name,
    scanlator = scanlator,
    chapterNumber = chapterNumber,
)

private val FakeChapter = previewChapter(
    name = "Vol.1, Ch.1 - Fake Chapter Title",
    scanlator = "Scanlator Name",
    chapterNumber = 1.0,
)
private val FakeGapChapter = previewChapter(
    name = "Vol.5, Ch.44 - Fake Gap Chapter Title",
    scanlator = "Scanlator Name",
    chapterNumber = 44.0,
)

@PreviewLightDark
@Composable
private fun TransitionTextPreview() {
    TachiyomiPreviewTheme {
        Box(
            modifier = Modifier
                .background(InkGlassTokens.BackgroundPrimary)
                .padding(InkGlassTokens.Space32),
        ) {
            ChapterTransition(
                transition = ChapterTransition.Next(ReaderChapter(FakeChapter), ReaderChapter(FakeGapChapter)),
                currChapterDownloaded = true,
                goingToChapterDownloaded = false,
            )
        }
    }
}
