package eu.kanade.presentation.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.browse.components.BaseSourceItem
import eu.kanade.tachiyomi.ui.browse.source.SourcesViewModel
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceViewModel.Listing
import eu.kanade.tachiyomi.util.system.LocaleHelper
import tachiyomi.domain.source.model.Pin
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkAlertDialog
import tachiyomi.presentation.core.components.InkEmptyState
import tachiyomi.presentation.core.components.InkGlassChip
import tachiyomi.presentation.core.components.InkGlassIconButton
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkLoadingState
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus
import tachiyomi.source.local.isLocal

@Composable
fun SourcesScreen(
    state: SourcesViewModel.State,
    contentPadding: PaddingValues,
    onClickItem: (Source, Listing) -> Unit,
    onClickPin: (Source) -> Unit,
    onLongClickItem: (Source) -> Unit,
) {
    when {
        state.isLoading -> InkLoadingState(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding)
                .padding(top = InkGlassTokens.Space48),
            label = stringResource(MR.strings.ml_g4_sources_loading),
        )
        state.isEmpty -> InkEmptyState(
            title = stringResource(MR.strings.ml_g4_sources_empty_title),
            message = stringResource(MR.strings.ml_g4_sources_empty_message),
            modifier = Modifier
                .padding(contentPadding)
                .padding(horizontal = InkGlassTokens.ScreenHorizontal, vertical = InkGlassTokens.Space32),
        )
        else -> {
            ScrollbarLazyColumn(
                contentPadding = contentPadding + PaddingValues(
                    top = InkGlassTokens.Space8,
                    bottom = InkGlassTokens.Space24,
                ),
            ) {
                items(
                    items = state.items,
                    contentType = {
                        when (it) {
                            is SourceUiModel.Header -> "header"
                            is SourceUiModel.Item -> "item"
                        }
                    },
                    key = {
                        when (it) {
                            is SourceUiModel.Header -> it.hashCode()
                            is SourceUiModel.Item -> "source-${it.source.key()}"
                        }
                    },
                ) { model ->
                    when (model) {
                        is SourceUiModel.Header -> {
                            SourceHeader(
                                modifier = Modifier.animateItem(),
                                language = model.language,
                            )
                        }
                        is SourceUiModel.Item -> SourceItem(
                            modifier = Modifier.animateItem(),
                            source = model.source,
                            onClickItem = onClickItem,
                            onLongClickItem = onLongClickItem,
                            onClickPin = onClickPin,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceHeader(
    language: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val otherLabel = stringResource(MR.strings.ml_g4_sources_other)
    val displayLanguage = LocaleHelper.getSourceDisplayName(language, context).let {
        if (it.equals("Other", ignoreCase = true)) otherLabel else it
    }
    Text(
        text = displayLanguage.uppercase(),
        modifier = modifier.padding(
            start = InkGlassTokens.ScreenHorizontal + InkGlassTokens.Space4,
            end = InkGlassTokens.ScreenHorizontal,
            top = InkGlassTokens.Space16,
            bottom = InkGlassTokens.Space8,
        ),
        style = InkTypography.Eyebrow,
        color = InkGlassTokens.TextTertiary,
    )
}

@Composable
private fun SourceItem(
    source: Source,
    onClickItem: (Source, Listing) -> Unit,
    onLongClickItem: (Source) -> Unit,
    onClickPin: (Source) -> Unit,
    modifier: Modifier = Modifier,
) {
    BaseSourceItem(
        modifier = modifier,
        source = source,
        onClickItem = { onClickItem(source, Listing.Popular) },
        onLongClickItem = { onLongClickItem(source) },
        action = {
            if (source.supportsLatest) {
                InkGlassChip(
                    label = stringResource(MR.strings.ml_g4_sources_latest),
                    selected = false,
                    onClick = { onClickItem(source, Listing.Latest) },
                )
            }
            SourcePinButton(
                isPinned = Pin.Pinned in source.pin,
                onClick = { onClickPin(source) },
            )
        },
    )
}

@Composable
private fun SourcePinButton(
    isPinned: Boolean,
    onClick: () -> Unit,
) {
    val icon = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin
    val description = if (isPinned) MR.strings.action_unpin else MR.strings.action_pin
    InkGlassIconButton(
        imageVector = icon,
        contentDescription = stringResource(description),
        onClick = onClick,
        selected = isPinned,
    )
}

@Composable
fun SourceOptionsDialog(
    source: Source,
    onClickPin: () -> Unit,
    onClickDisable: () -> Unit,
    onDismiss: () -> Unit,
) {
    InkAlertDialog(
        title = {
            Text(text = source.visualName)
        },
        text = {
            Column {
                val textId = if (Pin.Pinned in source.pin) MR.strings.action_unpin else MR.strings.action_pin
                Text(
                    text = stringResource(textId),
                    modifier = Modifier
                        .clickable(onClick = onClickPin)
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                )
                if (!source.isLocal()) {
                    Text(
                        text = stringResource(MR.strings.action_disable),
                        modifier = Modifier
                            .clickable(onClick = onClickDisable)
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                    )
                }
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {},
    )
}

sealed interface SourceUiModel {
    data class Item(val source: Source) : SourceUiModel
    data class Header(val language: String) : SourceUiModel
}
