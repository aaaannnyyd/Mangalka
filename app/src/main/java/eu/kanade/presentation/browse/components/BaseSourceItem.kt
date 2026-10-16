package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.tachiyomi.util.system.LocaleHelper
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun BaseSourceItem(
    source: Source,
    modifier: Modifier = Modifier,
    showLanguageInContent: Boolean = true,
    onClickItem: () -> Unit = {},
    onLongClickItem: () -> Unit = {},
    icon: @Composable RowScope.(Source) -> Unit = defaultIcon,
    action: @Composable RowScope.(Source) -> Unit = {},
    content: @Composable RowScope.(Source, String?) -> Unit = defaultContent,
) {
    val otherLabel = stringResource(MR.strings.other_source)
    val sourceLangString = LocaleHelper.getSourceDisplayName(source.lang, LocalContext.current)
        .let { if (it.equals("Other", ignoreCase = true)) otherLabel else it }
        .takeIf { showLanguageInContent }
    BaseBrowseItem(
        modifier = modifier,
        onClickItem = onClickItem,
        onLongClickItem = onLongClickItem,
        icon = { icon.invoke(this, source) },
        action = { action.invoke(this, source) },
        content = { content.invoke(this, source, sourceLangString) },
    )
}

private val defaultIcon: @Composable RowScope.(Source) -> Unit = { source ->
    SourceIcon(source = source)
}

private val defaultContent: @Composable RowScope.(Source, String?) -> Unit = { source, sourceLangString ->
    Column(
        modifier = Modifier
            .padding(horizontal = InkGlassTokens.Space12)
            .weight(1f),
    ) {
        Text(
            text = if (source.name == "Local source") stringResource(MR.strings.ml_g1_local_source) else source.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = InkTypography.TitleSmall,
            color = InkGlassTokens.TextPrimary,
        )
        if (sourceLangString != null) {
            Text(
                text = sourceLangString,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = InkTypography.BodySmall,
                color = InkGlassTokens.TextSecondary,
            )
        }
    }
}
