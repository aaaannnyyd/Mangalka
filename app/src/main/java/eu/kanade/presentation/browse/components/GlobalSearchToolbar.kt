package eu.kanade.presentation.browse.components

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SourceFilter
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkGlassChip
import tachiyomi.presentation.core.components.InkGlassIconButton
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassProgressBar
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun GlobalSearchToolbar(
    searchQuery: String?,
    progress: Int,
    total: Int,
    navigateUp: () -> Unit,
    onChangeSearchQuery: (String?) -> Unit,
    onSearch: (String) -> Unit,
    hideSourceFilter: Boolean,
    sourceFilter: SourceFilter,
    onChangeSearchFilter: (SourceFilter) -> Unit,
    onlyShowHasResults: Boolean,
    onToggleResults: () -> Unit,
    @Suppress("UNUSED_PARAMETER") scrollBehavior: TopAppBarScrollBehavior,
) {
    val query = searchQuery.orEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .padding(
                start = InkGlassTokens.ScreenHorizontal,
                top = InkGlassTokens.Space16,
                end = InkGlassTokens.ScreenHorizontal,
                bottom = InkGlassTokens.Space12,
            ),
        verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12),
        ) {
            InkGlassIconButton(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(MR.strings.ml_g4_back),
                onClick = navigateUp,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(MR.strings.ml_g4_brand_eyebrow),
                    style = InkTypography.Eyebrow,
                    color = InkGlassTokens.Accent,
                )
                Text(
                    stringResource(MR.strings.ml_g4_global_search_title),
                    style = InkTypography.Headline,
                    color = InkGlassTokens.TextPrimary,
                )
            }
        }

        InkGlassSurface(
            modifier = Modifier.fillMaxWidth(),
            level = InkGlassLevel.Strong,
            contentPadding = PaddingValues(
                horizontal = InkGlassTokens.Space16,
                vertical = InkGlassTokens.Space12,
            ),
            shadowElevation = 0.dp,
            prominent = true,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = InkGlassTokens.TextTertiary,
                )
                BasicTextField(
                    value = query,
                    onValueChange = { onChangeSearchQuery(it) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = InkTypography.Body.copy(color = InkGlassTokens.TextPrimary),
                    cursorBrush = SolidColor(InkGlassTokens.Accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (query.isNotBlank()) onSearch(query)
                        },
                    ),
                    decorationBox = { inner ->
                        if (query.isBlank()) {
                            Text(
                                text = stringResource(MR.strings.action_search_hint),
                                style = InkTypography.Body,
                                color = InkGlassTokens.TextTertiary,
                            )
                        }
                        inner()
                    },
                )
                if (query.isNotEmpty()) {
                    InkGlassIconButton(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(MR.strings.ml_g4_clear_search),
                        onClick = { onChangeSearchQuery("") },
                    )
                }
            }
        }

        if (progress in 1..<total) {
            InkGlassProgressBar(
                progress = progress / total.toFloat(),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!hideSourceFilter) {
                InkGlassChip(
                    label = stringResource(MR.strings.all),
                    selected = sourceFilter == SourceFilter.All,
                    onClick = { onChangeSearchFilter(SourceFilter.All) },
                )
                InkGlassChip(
                    label = stringResource(MR.strings.pinned_sources),
                    selected = sourceFilter == SourceFilter.PinnedOnly,
                    onClick = { onChangeSearchFilter(SourceFilter.PinnedOnly) },
                )
            }
            InkGlassChip(
                label = stringResource(MR.strings.has_results),
                selected = onlyShowHasResults,
                onClick = onToggleResults,
            )
        }
    }
}
