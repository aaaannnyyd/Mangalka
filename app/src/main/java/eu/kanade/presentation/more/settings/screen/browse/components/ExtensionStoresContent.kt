package eu.kanade.presentation.more.settings.screen.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mihon.domain.extension.model.ExtensionStore
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkGlassIconButton
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.icons.CustomIcons
import tachiyomi.presentation.core.icons.Discord

@Composable
fun ExtensionStoresContent(
    repos: List<ExtensionStore>,
    lazyListState: LazyListState,
    paddingValues: PaddingValues,
    onCopy: (ExtensionStore) -> Unit,
    onOpenWebsite: (ExtensionStore) -> Unit,
    onOpenDiscord: (ExtensionStore) -> Unit,
    onClickDelete: (ExtensionStore) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = lazyListState,
        contentPadding = paddingValues,
        verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
        modifier = modifier,
    ) {
        repos.forEach { store ->
            item {
                ExtensionStoresListItem(
                    modifier = Modifier.animateItem(),
                    store = store,
                    onOpenWebsite = { onOpenWebsite(store) },
                    onOpenDiscord = { onOpenDiscord(store) },
                    onCopy = { onCopy(store) },
                    onDelete = { onClickDelete(store) },
                )
            }
        }
    }
}

@Composable
private fun ExtensionStoresListItem(
    store: ExtensionStore,
    onOpenWebsite: () -> Unit,
    onOpenDiscord: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InkGlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = InkGlassTokens.ScreenHorizontal, vertical = InkGlassTokens.Space4),
        level = InkGlassLevel.Soft,
        contentPadding = PaddingValues(InkGlassTokens.Space16),
        shadowElevation = 0.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Label,
                    contentDescription = null,
                    tint = InkGlassTokens.Accent,
                )
                Text(
                    text = store.name,
                    modifier = Modifier.padding(start = InkGlassTokens.Space12),
                    style = InkTypography.TitleSmall,
                    color = InkGlassTokens.TextPrimary,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                InkGlassIconButton(
                    imageVector = Icons.Outlined.Public,
                    contentDescription = stringResource(MR.strings.action_open_in_browser),
                    onClick = onOpenWebsite,
                )

                if (store.contact.discord != null) {
                    InkGlassIconButton(
                        imageVector = CustomIcons.Discord,
                        contentDescription = null,
                        onClick = onOpenDiscord,
                    )
                }

                InkGlassIconButton(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(MR.strings.action_copy_to_clipboard),
                    onClick = onCopy,
                )

                InkGlassIconButton(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(MR.strings.action_delete),
                    onClick = onDelete,
                    selected = true,
                )
            }
        }
    }
}
