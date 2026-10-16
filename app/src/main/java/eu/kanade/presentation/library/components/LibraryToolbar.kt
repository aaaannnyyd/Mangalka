package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkGlassIconButton
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun LibraryToolbar(
    hasActiveFilters: Boolean,
    selectedCount: Int,
    title: LibraryToolbarTitle,
    onClickUnselectAll: () -> Unit,
    onClickSelectAll: () -> Unit,
    onClickInvertSelection: () -> Unit,
    onClickFilter: () -> Unit,
    onClickRefresh: () -> Unit,
    onClickGlobalUpdate: () -> Unit,
    onClickOpenRandomManga: () -> Unit,
    searchQuery: String?,
    onSearchQueryChange: (String?) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior?,
) = when {
    selectedCount > 0 -> LibrarySelectionToolbar(
        selectedCount = selectedCount,
        onClickUnselectAll = onClickUnselectAll,
        onClickSelectAll = onClickSelectAll,
        onClickInvertSelection = onClickInvertSelection,
    )
    else -> LibraryRegularToolbar(
        title = title,
        hasFilters = hasActiveFilters,
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchQueryChange,
        onClickFilter = onClickFilter,
        onClickRefresh = onClickRefresh,
        onClickGlobalUpdate = onClickGlobalUpdate,
        onClickOpenRandomManga = onClickOpenRandomManga,
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun LibraryRegularToolbar(
    title: LibraryToolbarTitle,
    hasFilters: Boolean,
    searchQuery: String?,
    onSearchQueryChange: (String?) -> Unit,
    onClickFilter: () -> Unit,
    onClickRefresh: () -> Unit,
    onClickGlobalUpdate: () -> Unit,
    onClickOpenRandomManga: () -> Unit,
    @Suppress("UNUSED_PARAMETER") scrollBehavior: TopAppBarScrollBehavior?,
) {
    if (searchQuery != null) {
        InkGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .padding(
                    start = InkGlassTokens.ScreenHorizontal,
                    end = InkGlassTokens.ScreenHorizontal,
                    top = InkGlassTokens.Space20,
                    bottom = InkGlassTokens.Space12,
                ),
            level = InkGlassLevel.Strong,
            contentPadding = PaddingValues(horizontal = InkGlassTokens.Space16, vertical = InkGlassTokens.Space12),
            shadowElevation = 0.dp,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { onSearchQueryChange(it) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = InkTypography.Body.copy(color = InkGlassTokens.TextPrimary),
                    cursorBrush = SolidColor(InkGlassTokens.Accent),
                    decorationBox = { inner ->
                        if (searchQuery.isEmpty()) {
                            Text("Поиск в библиотеке…", style = InkTypography.Body, color = InkGlassTokens.TextTertiary)
                        }
                        inner()
                    },
                )
                InkGlassIconButton(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Закрыть поиск",
                    onClick = { onSearchQueryChange(null) },
                )
            }
        }
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .padding(
                start = InkGlassTokens.ScreenHorizontal,
                end = InkGlassTokens.ScreenHorizontal,
                top = InkGlassTokens.Space20,
                bottom = InkGlassTokens.Space12,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
    ) {
        Column(Modifier.weight(1f)) {
            Text("МАНГАЛКА", style = InkTypography.Eyebrow, color = InkGlassTokens.Accent)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = title.text,
                    style = InkTypography.Headline.copy(fontSize = 24.sp, lineHeight = 28.sp),
                    color = InkGlassTokens.TextPrimary,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, false),
                    overflow = TextOverflow.Ellipsis,
                )
                title.numberOfManga?.let {
                    Text(
                        text = "  $it",
                        style = InkTypography.BodySmall,
                        color = InkGlassTokens.TextTertiary,
                    )
                }
            }
        }
        InkGlassIconButton(
            imageVector = Icons.Outlined.Search,
            contentDescription = "Поиск",
            onClick = { onSearchQueryChange("") },
        )
        InkGlassIconButton(
            imageVector = Icons.Outlined.FilterList,
            contentDescription = stringResource(MR.strings.action_filter),
            onClick = onClickFilter,
            selected = hasFilters,
        )
        AppBarActions(
            listOf(
                AppBar.OverflowAction(
                    title = stringResource(MR.strings.action_update_library),
                    onClick = onClickGlobalUpdate,
                ),
                AppBar.OverflowAction(
                    title = stringResource(MR.strings.action_update_category),
                    onClick = onClickRefresh,
                ),
                AppBar.OverflowAction(
                    title = stringResource(MR.strings.action_open_random_manga),
                    onClick = onClickOpenRandomManga,
                ),
            ),
        )
    }
}

@Composable
private fun LibrarySelectionToolbar(
    selectedCount: Int,
    onClickUnselectAll: () -> Unit,
    onClickSelectAll: () -> Unit,
    onClickInvertSelection: () -> Unit,
) {
    AppBar(
        titleContent = { Text(text = "$selectedCount") },
        actions = {
            AppBarActions(
                listOf(
                    AppBar.Action(
                        title = stringResource(MR.strings.action_select_all),
                        icon = Icons.Outlined.SelectAll,
                        onClick = onClickSelectAll,
                    ),
                    AppBar.Action(
                        title = stringResource(MR.strings.action_select_inverse),
                        icon = Icons.Outlined.FlipToBack,
                        onClick = onClickInvertSelection,
                    ),
                ),
            )
        },
        isActionMode = true,
        onCancelActionMode = onClickUnselectAll,
    )
}

@Immutable
data class LibraryToolbarTitle(
    val text: String,
    val numberOfManga: Int? = null,
)
