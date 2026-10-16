package eu.kanade.presentation.library.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkGlassButton
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun GlobalSearchItem(
    searchQuery: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InkGlassButton(
        label = stringResource(MR.strings.action_global_search_query, searchQuery),
        onClick = onClick,
        modifier = modifier,
    )
}
