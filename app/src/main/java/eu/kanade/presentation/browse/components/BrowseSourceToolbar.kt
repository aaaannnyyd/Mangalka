package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.Source
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkGlassDropdownMenu
import tachiyomi.presentation.core.components.InkGlassIconButton
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.source.local.LocalSource

@Composable
fun BrowseSourceToolbar(
    searchQuery: String?,
    onSearchQueryChange: (String?) -> Unit,
    source: Source?,
    displayMode: LibraryDisplayMode,
    onDisplayModeChange: (LibraryDisplayMode) -> Unit,
    navigateUp: () -> Unit,
    onWebViewClick: () -> Unit,
    onHelpClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSearch: (String) -> Unit,
    @Suppress("UNUSED_PARAMETER") scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val title = source?.name.orEmpty()
    val isLocalSource = source is LocalSource
    val isConfigurableSource = source is ConfigurableSource
    var selectingDisplayMode by remember { mutableStateOf(false) }
    var showingMore by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(
                start = InkGlassTokens.ScreenHorizontal,
                end = InkGlassTokens.ScreenHorizontal,
                top = InkGlassTokens.Space12,
                bottom = InkGlassTokens.Space8,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
    ) {
        InkGlassIconButton(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Назад",
            onClick = navigateUp,
        )

        if (searchQuery != null) {
            InkGlassSurface(
                modifier = Modifier.weight(1f),
                level = InkGlassLevel.Strong,
                contentPadding = PaddingValues(
                    horizontal = InkGlassTokens.Space16,
                    vertical = InkGlassTokens.Space12,
                ),
                shadowElevation = 0.dp,
            ) {
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = InkTypography.Body.copy(color = InkGlassTokens.TextPrimary),
                    cursorBrush = SolidColor(InkGlassTokens.Accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch(searchQuery) }),
                    decorationBox = { inner ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Поиск в $title…",
                                style = InkTypography.Body,
                                color = InkGlassTokens.TextTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        inner()
                    },
                )
            }
            InkGlassIconButton(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Закрыть поиск",
                onClick = navigateUp,
            )
            return@Row
        }

        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = InkTypography.Headline,
            color = InkGlassTokens.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        InkGlassIconButton(
            imageVector = Icons.Outlined.Search,
            contentDescription = "Поиск",
            onClick = { onSearchQueryChange("") },
        )

        Box {
            InkGlassIconButton(
                imageVector = if (displayMode == LibraryDisplayMode.List) {
                    Icons.AutoMirrored.Filled.ViewList
                } else {
                    Icons.Filled.ViewModule
                },
                contentDescription = stringResource(MR.strings.action_display_mode),
                onClick = { selectingDisplayMode = true },
            )
            InkGlassDropdownMenu(
                expanded = selectingDisplayMode,
                onDismissRequest = { selectingDisplayMode = false },
            ) {
                DisplayModeItem(
                    label = stringResource(MR.strings.action_display_comfortable_grid),
                    selected = displayMode == LibraryDisplayMode.ComfortableGrid,
                    onClick = {
                        selectingDisplayMode = false
                        onDisplayModeChange(LibraryDisplayMode.ComfortableGrid)
                    },
                )
                DisplayModeItem(
                    label = stringResource(MR.strings.action_display_grid),
                    selected = displayMode == LibraryDisplayMode.CompactGrid,
                    onClick = {
                        selectingDisplayMode = false
                        onDisplayModeChange(LibraryDisplayMode.CompactGrid)
                    },
                )
                DisplayModeItem(
                    label = stringResource(MR.strings.action_display_list),
                    selected = displayMode == LibraryDisplayMode.List,
                    onClick = {
                        selectingDisplayMode = false
                        onDisplayModeChange(LibraryDisplayMode.List)
                    },
                )
            }
        }

        Box {
            InkGlassIconButton(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = "Ещё",
                onClick = { showingMore = true },
            )
            InkGlassDropdownMenu(
                expanded = showingMore,
                onDismissRequest = { showingMore = false },
            ) {
                if (isLocalSource) {
                    DropdownMenuItem(
                        text = { Text(stringResource(MR.strings.label_help), color = InkGlassTokens.TextPrimary) },
                        leadingIcon = {
                            Icon(Icons.Outlined.HelpOutline, null, tint = InkGlassTokens.TextSecondary)
                        },
                        onClick = {
                            showingMore = false
                            onHelpClick()
                        },
                    )
                } else {
                    DropdownMenuItem(
                        text = {
                            Text(stringResource(MR.strings.action_open_in_web_view), color = InkGlassTokens.TextPrimary)
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.Public, null, tint = InkGlassTokens.TextSecondary)
                        },
                        onClick = {
                            showingMore = false
                            onWebViewClick()
                        },
                    )
                }
                if (isConfigurableSource) {
                    DropdownMenuItem(
                        text = { Text(stringResource(MR.strings.action_settings), color = InkGlassTokens.TextPrimary) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Settings, null, tint = InkGlassTokens.TextSecondary)
                        },
                        onClick = {
                            showingMore = false
                            onSettingsClick()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DisplayModeItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                color = if (selected) InkGlassTokens.Accent else InkGlassTokens.TextPrimary,
            )
        },
        onClick = onClick,
    )
}
