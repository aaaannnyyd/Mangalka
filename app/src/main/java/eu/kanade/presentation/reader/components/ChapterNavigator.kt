package eu.kanade.presentation.reader.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalSlider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.presentation.util.isTabletUi
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkGlassIconButton
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.roundToInt

enum class ChapterNavigatorType {
    HORIZONTAL_LTR,
    HORIZONTAL_RTL,
    VERTICAL_LEFT,
    VERTICAL_RIGHT,
    ;

    fun isHorizontal() = this in setOf(HORIZONTAL_LTR, HORIZONTAL_RTL)
}

@Composable
fun ChapterNavigator(
    type: ChapterNavigatorType,
    onNextChapter: () -> Unit,
    enabledNext: Boolean,
    onPreviousChapter: () -> Unit,
    enabledPrevious: Boolean,
    currentPage: Int,
    totalPages: Int,
    onPageIndexChange: (Int) -> Unit,
    onPageIndexChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val state = remember(totalPages) {
        SliderState(
            value = currentPage.toFloat(),
            steps = (totalPages - 2).coerceAtLeast(0),
            valueRange = 1f..totalPages.coerceAtLeast(1).toFloat(),
        )
    }
    state.value = currentPage.coerceAtLeast(1).toFloat()
    state.onValueChange = { onPageIndexChange(it.roundToInt() - 1) }
    state.onValueChangeFinished = onPageIndexChangeFinished

    val interactionSource = remember { MutableInteractionSource() }
    val sliderDragged by interactionSource.collectIsDraggedAsState()
    LaunchedEffect(currentPage) {
        if (sliderDragged) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    val mainAxisPadding = if (isTabletUi()) 24.dp else 8.dp
    if (type.isHorizontal()) {
        HorizontalChapterNavigator(
            isRtl = type == ChapterNavigatorType.HORIZONTAL_RTL,
            state = state,
            onNextChapter = onNextChapter,
            enabledNext = enabledNext,
            onPreviousChapter = onPreviousChapter,
            enabledPrevious = enabledPrevious,
            currentPage = currentPage,
            totalPages = totalPages,
            interactionSource = interactionSource,
            mainAxisPadding = mainAxisPadding,
            modifier = modifier,
        )
    } else {
        VerticalChapterNavigator(
            state = state,
            onNextChapter = onNextChapter,
            enabledNext = enabledNext,
            onPreviousChapter = onPreviousChapter,
            enabledPrevious = enabledPrevious,
            currentPage = currentPage,
            totalPages = totalPages,
            interactionSource = interactionSource,
            mainAxisPadding = mainAxisPadding,
            modifier = modifier,
        )
    }
}

@Composable
private fun HorizontalChapterNavigator(
    isRtl: Boolean,
    state: SliderState,
    onNextChapter: () -> Unit,
    enabledNext: Boolean,
    onPreviousChapter: () -> Unit,
    enabledPrevious: Boolean,
    currentPage: Int,
    totalPages: Int,
    interactionSource: MutableInteractionSource,
    mainAxisPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = mainAxisPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InkGlassIconButton(
                imageVector = Icons.Outlined.SkipPrevious,
                contentDescription = stringResource(
                    if (isRtl) MR.strings.action_next_chapter else MR.strings.action_previous_chapter,
                ),
                enabled = if (isRtl) enabledNext else enabledPrevious,
                onClick = if (isRtl) onNextChapter else onPreviousChapter,
            )

            if (totalPages > 1) {
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    InkGlassSurface(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = InkGlassTokens.Space8),
                        level = InkGlassLevel.Strong,
                        contentPadding = PaddingValues(
                            horizontal = InkGlassTokens.Space12,
                            vertical = InkGlassTokens.Space4,
                        ),
                        shadowElevation = 0.dp,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(contentAlignment = Alignment.CenterEnd) {
                                Text(
                                    text = currentPage.toString(),
                                    style = InkTypography.BodySmall,
                                    color = InkGlassTokens.TextPrimary,
                                )
                                Text(text = totalPages.toString(), color = Color.Transparent)
                            }
                            Slider(
                                state = state,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = InkGlassTokens.Space8),
                                interactionSource = interactionSource,
                                colors = SliderDefaults.colors(
                                    thumbColor = InkGlassTokens.Accent,
                                    activeTrackColor = InkGlassTokens.Accent,
                                    inactiveTrackColor = InkGlassTokens.GlassBorder,
                                ),
                            )
                            Text(
                                text = totalPages.toString(),
                                style = InkTypography.BodySmall,
                                color = InkGlassTokens.TextPrimary,
                            )
                        }
                    }
                }
            } else {
                Spacer(Modifier.weight(1f))
            }

            InkGlassIconButton(
                imageVector = Icons.Outlined.SkipNext,
                contentDescription = stringResource(
                    if (isRtl) MR.strings.action_previous_chapter else MR.strings.action_next_chapter,
                ),
                enabled = if (isRtl) enabledPrevious else enabledNext,
                onClick = if (isRtl) onPreviousChapter else onNextChapter,
            )
        }
    }
}

@Composable
private fun VerticalChapterNavigator(
    state: SliderState,
    onNextChapter: () -> Unit,
    enabledNext: Boolean,
    onPreviousChapter: () -> Unit,
    enabledPrevious: Boolean,
    currentPage: Int,
    totalPages: Int,
    interactionSource: MutableInteractionSource,
    mainAxisPadding: Dp,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = mainAxisPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        InkGlassIconButton(
            imageVector = Icons.Outlined.KeyboardArrowUp,
            contentDescription = stringResource(MR.strings.action_previous_chapter),
            enabled = enabledPrevious,
            onClick = onPreviousChapter,
        )

        if (totalPages > 1) {
            InkGlassSurface(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = InkGlassTokens.Space8),
                level = InkGlassLevel.Strong,
                contentPadding = PaddingValues(
                    horizontal = InkGlassTokens.Space8,
                    vertical = InkGlassTokens.Space12,
                ),
                shadowElevation = 0.dp,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = currentPage.toString(),
                        style = InkTypography.BodySmall,
                        color = InkGlassTokens.TextPrimary,
                    )
                    VerticalSlider(
                        state = state,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = InkGlassTokens.Space8),
                        interactionSource = interactionSource,
                        colors = SliderDefaults.colors(
                            thumbColor = InkGlassTokens.Accent,
                            activeTrackColor = InkGlassTokens.Accent,
                            inactiveTrackColor = InkGlassTokens.GlassBorder,
                        ),
                    )
                    Text(
                        text = totalPages.toString(),
                        style = InkTypography.BodySmall,
                        color = InkGlassTokens.TextPrimary,
                    )
                }
            }
        } else {
            Spacer(Modifier.weight(1f))
        }

        InkGlassIconButton(
            imageVector = Icons.Outlined.KeyboardArrowDown,
            contentDescription = stringResource(MR.strings.action_next_chapter),
            enabled = enabledNext,
            onClick = onNextChapter,
        )
    }
}

@Preview
@Composable
private fun ChapterNavigatorPreview() {
    var currentPage by remember { mutableIntStateOf(1) }
    TachiyomiPreviewTheme {
        ChapterNavigator(
            type = ChapterNavigatorType.VERTICAL_RIGHT,
            onNextChapter = {},
            enabledNext = true,
            onPreviousChapter = {},
            enabledPrevious = true,
            currentPage = currentPage,
            totalPages = 10,
            onPageIndexChange = { currentPage = (it + 1) },
            onPageIndexChangeFinished = {},
        )
    }
}
