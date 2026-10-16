package eu.kanade.presentation.history.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.util.formatChapterNumber
import eu.kanade.tachiyomi.util.lang.toTimestampString
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkGlassIconButton
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun HistoryItem(
    history: HistoryWithRelations,
    onClickCover: () -> Unit,
    onClickResume: () -> Unit,
    onClickDelete: () -> Unit,
    onClickFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InkGlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = InkGlassTokens.ScreenHorizontal, vertical = InkGlassTokens.Space4),
        level = InkGlassLevel.Soft,
        contentPadding = PaddingValues(InkGlassTokens.Space12),
        shadowElevation = 0.dp,
        onClick = onClickResume,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12),
        ) {
            MangaCover.Book(
                modifier = Modifier.height(82.dp),
                data = history.coverData,
                onClick = onClickCover,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
            ) {
                Text(
                    text = history.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = InkTypography.TitleSmall,
                    color = InkGlassTokens.TextPrimary,
                )
                val readAt = remember { history.readAt?.toTimestampString().orEmpty() }
                Text(
                    text = if (history.chapterNumber > -1) {
                        stringResource(
                            MR.strings.recent_manga_time,
                            formatChapterNumber(history.chapterNumber),
                            readAt,
                        )
                    } else {
                        readAt
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = InkTypography.BodySmall,
                    color = InkGlassTokens.TextSecondary,
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (!history.coverData.isMangaFavorite) {
                    InkGlassIconButton(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = stringResource(MR.strings.add_to_library),
                        onClick = onClickFavorite,
                    )
                }
                InkGlassIconButton(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(MR.strings.action_delete),
                    onClick = onClickDelete,
                )
            }
        }
    }
}
