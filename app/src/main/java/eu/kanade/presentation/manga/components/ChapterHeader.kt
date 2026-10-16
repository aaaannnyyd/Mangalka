package eu.kanade.presentation.manga.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkGlassIconButton
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ChapterHeader(
    enabled: Boolean,
    filterActive: Boolean,
    chapterCount: Int?,
    missingChapterCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InkGlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = InkGlassTokens.ScreenHorizontal,
                end = InkGlassTokens.ScreenHorizontal,
                top = InkGlassTokens.Space8,
            ),
        level = InkGlassLevel.Strong,
        contentPadding = PaddingValues(
            start = InkGlassTokens.Space16,
            top = InkGlassTokens.Space12,
            end = InkGlassTokens.Space12,
            bottom = InkGlassTokens.Space12,
        ),
        shadowElevation = 0.dp,
        prominent = true,
        onClick = onClick.takeIf { enabled },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space4),
            ) {
                Text(
                    text = "ГЛАВЫ",
                    style = InkTypography.Eyebrow,
                    color = InkGlassTokens.Accent,
                )
                Text(
                    text = if (chapterCount == null) {
                        stringResource(MR.strings.chapters)
                    } else {
                        pluralStringResource(MR.plurals.manga_num_chapters, count = chapterCount, chapterCount)
                    },
                    style = InkTypography.Title,
                    color = InkGlassTokens.TextPrimary,
                )
                MissingChaptersWarning(missingChapterCount)
            }
            InkGlassIconButton(
                imageVector = Icons.Outlined.FilterList,
                contentDescription = stringResource(MR.strings.action_filter),
                onClick = onClick,
                selected = filterActive,
            )
        }
    }
}

@Composable
private fun MissingChaptersWarning(count: Int) {
    if (count == 0) return
    Text(
        text = pluralStringResource(MR.plurals.missing_chapters, count = count, count),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = InkTypography.BodySmall,
        color = InkGlassTokens.Error,
    )
}
