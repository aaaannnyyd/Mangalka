package eu.kanade.presentation.track

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.DatePicker
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkAccentButton
import tachiyomi.presentation.core.components.InkGlassButton
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkMangaBackdrop
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.WheelNumberPicker
import tachiyomi.presentation.core.components.WheelTextPicker
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun TrackStatusSelector(
    selection: Long,
    onSelectionChange: (Long) -> Unit,
    selections: Map<Long, StringResource?>,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    BaseSelector(
        title = stringResource(MR.strings.status),
        content = {
            val state = rememberLazyListState()
            ScrollbarLazyColumn(
                state = state,
                verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
            ) {
                selections.forEach { (key, value) ->
                    val isSelected = selection == key
                    item(key = key) {
                        InkGlassSurface(
                            modifier = Modifier.fillMaxWidth(),
                            level = if (isSelected) InkGlassLevel.Accent else InkGlassLevel.Soft,
                            contentPadding = PaddingValues(
                                horizontal = InkGlassTokens.Space16,
                                vertical = InkGlassTokens.Space12,
                            ),
                            shadowElevation = 0.dp,
                            onClick = { onSelectionChange(key) },
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12),
                            ) {
                                Text(
                                    text = value?.let { stringResource(it) } ?: "—",
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    style = InkTypography.Body,
                                    color = if (isSelected) {
                                        InkGlassTokens.TextPrimary
                                    } else {
                                        InkGlassTokens.TextSecondary
                                    },
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = InkGlassTokens.Accent,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        onConfirm = onConfirm,
        onDismissRequest = onDismissRequest,
    )
}

@Composable
fun TrackChapterSelector(
    selection: Int,
    onSelectionChange: (Int) -> Unit,
    range: Iterable<Int>,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    BaseSelector(
        title = stringResource(MR.strings.chapters),
        content = {
            WheelNumberPicker(
                items = range.toList(),
                modifier = Modifier.align(Alignment.Center),
                startIndex = selection,
                onSelectionChanged = { onSelectionChange(it) },
            )
        },
        onConfirm = onConfirm,
        onDismissRequest = onDismissRequest,
    )
}

@Composable
fun TrackScoreSelector(
    selection: String,
    onSelectionChange: (String) -> Unit,
    selections: List<String>,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    BaseSelector(
        title = stringResource(MR.strings.score),
        content = {
            WheelTextPicker(
                items = selections,
                modifier = Modifier.align(Alignment.Center),
                startIndex = selections.indexOf(selection).takeIf { it > 0 } ?: (selections.size / 2),
                onSelectionChanged = { onSelectionChange(selections[it]) },
            )
        },
        onConfirm = onConfirm,
        onDismissRequest = onDismissRequest,
    )
}

@Composable
fun TrackDateSelector(
    title: String,
    initialSelectedDateMillis: Long,
    selectableDates: SelectableDates,
    onConfirm: (Long) -> Unit,
    onRemove: (() -> Unit)?,
    onDismissRequest: () -> Unit,
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialSelectedDateMillis,
        selectableDates = selectableDates,
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        InkMangaBackdrop(intensity = 0.4f)
        InkGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(InkGlassTokens.Space20)
                .align(Alignment.Center),
            level = InkGlassLevel.Strong,
            contentPadding = PaddingValues(InkGlassTokens.Space20),
            shadowElevation = InkGlassTokens.Space12,
            prominent = true,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space16)) {
                Text(
                    stringResource(MR.strings.ml_g3_tracking_eyebrow),
                    style = InkTypography.Eyebrow,
                    color = InkGlassTokens.Accent,
                )
                Text(title, style = InkTypography.Title, color = InkGlassTokens.TextPrimary)

                DatePicker(
                    state = pickerState,
                    title = null,
                    headline = null,
                    showModeToggle = false,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onRemove != null) {
                        InkGlassButton(
                            label = stringResource(MR.strings.action_remove),
                            onClick = onRemove,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    InkGlassButton(
                        label = stringResource(MR.strings.action_cancel),
                        onClick = onDismissRequest,
                    )
                    InkAccentButton(
                        label = stringResource(MR.strings.action_ok),
                        onClick = { pickerState.selectedDateMillis?.let(onConfirm) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BaseSelector(
    title: String,
    content: @Composable BoxScope.() -> Unit,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    thirdButton: @Composable (RowScope.() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        InkMangaBackdrop(intensity = 0.4f)
        InkGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(InkGlassTokens.Space20)
                .align(Alignment.Center),
            level = InkGlassLevel.Strong,
            contentPadding = PaddingValues(InkGlassTokens.Space20),
            shadowElevation = InkGlassTokens.Space12,
            prominent = true,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space16)) {
                Column(verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space4)) {
                    Text(
                        stringResource(MR.strings.ml_g3_tracking_eyebrow),
                        style = InkTypography.Eyebrow,
                        color = InkGlassTokens.Accent,
                    )
                    Text(title, style = InkTypography.Headline, color = InkGlassTokens.TextPrimary)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 220.dp, max = 460.dp),
                    content = content,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (thirdButton != null) {
                        thirdButton()
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    InkGlassButton(
                        label = stringResource(MR.strings.action_cancel),
                        onClick = onDismissRequest,
                    )
                    InkAccentButton(
                        label = stringResource(MR.strings.action_ok),
                        onClick = onConfirm,
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun TrackStatusSelectorPreviews() {
    TachiyomiPreviewTheme {
        TrackStatusSelector(
            selection = 1,
            onSelectionChange = {},
            selections = mapOf(
                1L to MR.strings.reading,
                2L to MR.strings.plan_to_read,
                3L to MR.strings.completed,
                4L to MR.strings.on_hold,
                5L to MR.strings.dropped,
                6L to MR.strings.repeating,
            ),
            onConfirm = {},
            onDismissRequest = {},
        )
    }
}
