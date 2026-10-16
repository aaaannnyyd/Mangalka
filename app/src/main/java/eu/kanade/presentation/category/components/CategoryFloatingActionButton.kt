package eu.kanade.presentation.category.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkAccentButton
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.shouldExpandFAB

@Composable
fun CategoryFloatingActionButton(
    lazyListState: LazyListState,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InkAccentButton(
        label = if (lazyListState.shouldExpandFAB()) stringResource(MR.strings.action_add) else "+",
        icon = Icons.Outlined.Add.takeIf { lazyListState.shouldExpandFAB() },
        onClick = onCreate,
        modifier = modifier,
    )
}
