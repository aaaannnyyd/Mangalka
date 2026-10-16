package eu.kanade.presentation.track

import android.content.ClipData
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.util.system.openInBrowser
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkAccentButton
import tachiyomi.presentation.core.components.InkEmptyState
import tachiyomi.presentation.core.components.InkGlassIconButton
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkLoadingState
import tachiyomi.presentation.core.components.InkMangaBackdrop
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus
import tachiyomi.presentation.core.util.runOnEnterKeyPressed

@Composable
fun TrackerSearch(
    state: TextFieldState,
    onDispatchQuery: () -> Unit,
    queryResult: Result<List<TrackSearch>>?,
    selected: TrackSearch?,
    onSelectedChange: (TrackSearch) -> Unit,
    onConfirmSelection: (private: Boolean) -> Unit,
    onDismissRequest: () -> Unit,
    supportsPrivateTracking: Boolean,
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val dispatchQueryAndClearFocus: () -> Unit = {
        onDispatchQuery()
        focusManager.clearFocus()
    }

    Scaffold(
        containerColor = InkGlassTokens.BackgroundPrimary,
        topBar = {
            TrackerSearchTopBar(
                state = state,
                focusRequester = focusRequester,
                onDismissRequest = onDismissRequest,
                onSubmit = dispatchQueryAndClearFocus,
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = selected != null,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = slideOutVertically { it / 2 } + fadeOut(),
            ) {
                InkGlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = InkGlassTokens.Space16, vertical = InkGlassTokens.Space8)
                        .windowInsetsPadding(WindowInsets.navigationBars),
                    level = InkGlassLevel.Strong,
                    contentPadding = PaddingValues(InkGlassTokens.Space12),
                    shadowElevation = InkGlassTokens.Space12,
                    prominent = true,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
                    ) {
                        InkAccentButton(
                            label = stringResource(MR.strings.action_track),
                            onClick = { onConfirmSelection(false) },
                            modifier = Modifier.weight(1f),
                        )
                        if (supportsPrivateTracking) {
                            InkGlassIconButton(
                                imageVector = Icons.Filled.VisibilityOff,
                                contentDescription = stringResource(MR.strings.action_toggle_private_on),
                                onClick = { onConfirmSelection(true) },
                                selected = true,
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            InkMangaBackdrop(intensity = 0.55f)
            when {
                queryResult == null -> {
                    InkLoadingState(
                        modifier = Modifier.align(Alignment.Center),
                        label = stringResource(MR.strings.ml_g1_tracker_searching),
                    )
                }
                queryResult.isFailure -> {
                    InkEmptyState(
                        title = stringResource(MR.strings.ml_g1_tracker_search_failed),
                        message = queryResult.exceptionOrNull()?.message ?: stringResource(MR.strings.unknown_error),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(InkGlassTokens.Space20),
                        actionLabel = stringResource(MR.strings.action_retry),
                        onAction = onDispatchQuery,
                    )
                }
                queryResult.getOrNull().isNullOrEmpty() -> {
                    InkEmptyState(
                        title = stringResource(MR.strings.ml_g1_tracker_nothing_found),
                        message = stringResource(MR.strings.ml_g1_tracker_nothing_found_hint),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(InkGlassTokens.Space20),
                    )
                }
                else -> {
                    val availableTracks = queryResult.getOrThrow()
                    ScrollbarLazyColumn(
                        contentPadding = PaddingValues(
                            start = InkGlassTokens.Space20,
                            top = InkGlassTokens.Space16,
                            end = InkGlassTokens.Space20,
                            bottom = InkGlassTokens.Space32,
                        ),
                        verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12),
                    ) {
                        items(
                            items = availableTracks,
                            key = { it.hashCode() },
                        ) { track ->
                            SearchResultItem(
                                trackSearch = track,
                                selected = track == selected,
                                onClick = { onSelectedChange(track) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackerSearchTopBar(
    state: TextFieldState,
    focusRequester: FocusRequester,
    onDismissRequest: () -> Unit,
    onSubmit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
            .padding(
                start = InkGlassTokens.Space16,
                top = InkGlassTokens.Space12,
                end = InkGlassTokens.Space16,
                bottom = InkGlassTokens.Space8,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12),
    ) {
        InkGlassIconButton(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = null,
            onClick = onDismissRequest,
        )
        InkGlassSurface(
            modifier = Modifier.weight(1f),
            level = InkGlassLevel.Strong,
            contentPadding = PaddingValues(horizontal = InkGlassTokens.Space16, vertical = InkGlassTokens.Space12),
            shadowElevation = 0.dp,
            prominent = true,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
            ) {
                BasicTextField(
                    state = state,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .runOnEnterKeyPressed(action = onSubmit),
                    textStyle = InkTypography.Body.copy(color = InkGlassTokens.TextPrimary),
                    lineLimits = TextFieldLineLimits.SingleLine,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    onKeyboardAction = { onSubmit() },
                    cursorBrush = SolidColor(InkGlassTokens.Accent),
                    decorator = { inner ->
                        if (state.text.isEmpty()) {
                            Text(
                                text = stringResource(MR.strings.action_search_hint),
                                color = InkGlassTokens.TextTertiary,
                                style = InkTypography.Body,
                            )
                        }
                        inner()
                    },
                )
                if (state.text.isNotEmpty()) {
                    InkGlassIconButton(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        onClick = {
                            state.clearText()
                            focusRequester.requestFocus()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    trackSearch: TrackSearch,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard: Clipboard = LocalClipboard.current
    val focusManager = LocalFocusManager.current
    val type = trackSearch.publishing_type.toLowerCase(Locale.current).capitalize(Locale.current)
    val status = trackSearch.publishing_status.toLowerCase(Locale.current).capitalize(Locale.current)
    val description = trackSearch.summary.trim()
    var dropDownMenuExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    InkGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        level = if (selected) InkGlassLevel.Accent else InkGlassLevel.Soft,
        contentPadding = PaddingValues(InkGlassTokens.Space16),
        shadowElevation = 0.dp,
        onClick = {
            focusManager.clearFocus()
            onClick()
        },
        onLongClick = { dropDownMenuExpanded = true },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12)) {
            Row(horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12)) {
                MangaCover.Book(
                    data = trackSearch.cover_url,
                    modifier = Modifier.height(112.dp),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space4),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = trackSearch.title,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = InkTypography.Title,
                            color = InkGlassTokens.TextPrimary,
                        )
                        if (selected) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = InkGlassTokens.Accent,
                            )
                        }
                    }
                    val creators = (trackSearch.authors + trackSearch.artists).distinct().joinToString()
                    if (creators.isNotBlank()) {
                        Text(
                            text = creators,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = InkTypography.BodySmall,
                            color = InkGlassTokens.TextSecondary,
                        )
                    }
                    if (type.isNotBlank()) SearchResultItemDetails(stringResource(MR.strings.track_type), type)
                    if (trackSearch.start_date.isNotBlank()) {
                        SearchResultItemDetails(stringResource(MR.strings.ml_g1_tracker_start), trackSearch.start_date)
                    }
                    if (status.isNotBlank()) SearchResultItemDetails(stringResource(MR.strings.status), status)
                    if (trackSearch.score != -1.0) {
                        SearchResultItemDetails(stringResource(MR.strings.score), trackSearch.score.toString())
                    }
                }
            }
            if (description.isNotBlank()) {
                Text(
                    text = description,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    style = InkTypography.BodySmall,
                    color = InkGlassTokens.TextSecondary,
                )
            }
        }

        SearchResultItemDropDownMenu(
            expanded = dropDownMenuExpanded,
            onCollapseMenu = { dropDownMenuExpanded = false },
            onCopyName = {
                scope.launch {
                    val clipEntry = ClipData.newPlainText(trackSearch.title, trackSearch.title).toClipEntry()
                    clipboard.setClipEntry(clipEntry)
                }
            },
            onOpenInBrowser = {
                val url = trackSearch.tracking_url
                if (url.isNotBlank()) context.openInBrowser(url)
            },
        )
    }
}

@Composable
private fun SearchResultItemDropDownMenu(
    expanded: Boolean,
    onCollapseMenu: () -> Unit,
    onCopyName: () -> Unit,
    onOpenInBrowser: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onCollapseMenu,
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(MR.strings.action_copy_to_clipboard)) },
            onClick = {
                onCopyName()
                onCollapseMenu()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(MR.strings.action_open_in_browser)) },
            onClick = {
                onOpenInBrowser()
                onCollapseMenu()
            },
        )
    }
}

@Composable
private fun SearchResultItemDetails(
    title: String,
    text: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8)) {
        Text(
            text = title.uppercase(),
            maxLines = 1,
            style = InkTypography.Eyebrow,
            color = InkGlassTokens.TextTertiary,
        )
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = InkTypography.BodySmall,
            color = InkGlassTokens.TextSecondary,
        )
    }
}

@PreviewLightDark
@Composable
private fun TrackerSearchPreviews(
    @PreviewParameter(TrackerSearchPreviewProvider::class)
    content: @Composable () -> Unit,
) {
    TachiyomiPreviewTheme { content() }
}
