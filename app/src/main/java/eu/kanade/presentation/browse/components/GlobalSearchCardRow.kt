package eu.kanade.presentation.browse.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.domain.manga.model.sourceRating
import eu.kanade.presentation.manga.components.MangaCover
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.asMangaCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.i18n.stringResource

private const val SEARCH_PREVIEW_COLUMNS = 3
private const val SEARCH_PREVIEW_LIMIT = 6

@Composable
fun GlobalSearchCardRow(
    titles: List<Manga>,
    getManga: @Composable (Manga) -> State<Manga>,
    onClick: (Manga) -> Unit,
    onLongClick: (Manga) -> Unit,
) {
    if (titles.isEmpty()) {
        EmptyResultItem()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = InkGlassTokens.ScreenHorizontal),
        verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12),
    ) {
        titles
            .take(SEARCH_PREVIEW_LIMIT)
            .chunked(SEARCH_PREVIEW_COLUMNS)
            .forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12),
                ) {
                    rowItems.forEach { item ->
                        val manga by getManga(item)
                        SearchMangaCard(
                            manga = manga,
                            modifier = Modifier.weight(1f),
                            onClick = { onClick(manga) },
                            onLongClick = { onLongClick(manga) },
                        )
                    }
                    repeat(SEARCH_PREVIEW_COLUMNS - rowItems.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
    }
}

@Composable
private fun SearchMangaCard(
    manga: Manga,
    modifier: Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(
        modifier = modifier.combinedClickable(
            role = Role.Button,
            onClick = onClick,
            onLongClick = onLongClick,
        ),
        verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            MangaCover.Book(
                data = manga.asMangaCover(),
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (manga.favorite) 0.72f else 1f),
                shape = RoundedCornerShape(InkGlassTokens.CoverRadius),
            )

            manga.sourceRating()?.displayValue?.let { rating ->
                InkGlassSurface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(InkGlassTokens.Space4),
                    level = InkGlassLevel.Strong,
                    shape = RoundedCornerShape(InkGlassTokens.RadiusChip),
                    contentPadding = PaddingValues(
                        horizontal = InkGlassTokens.Space8,
                        vertical = InkGlassTokens.Space4,
                    ),
                    shadowElevation = 0.dp,
                ) {
                    Text(
                        text = "★ $rating",
                        style = InkTypography.Eyebrow,
                        color = InkGlassTokens.Accent,
                    )
                }
            }

            if (manga.favorite) {
                InkGlassSurface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(InkGlassTokens.Space4),
                    level = InkGlassLevel.Accent,
                    shape = RoundedCornerShape(InkGlassTokens.RadiusChip),
                    contentPadding = PaddingValues(
                        horizontal = InkGlassTokens.Space8,
                        vertical = InkGlassTokens.Space4,
                    ),
                    shadowElevation = 0.dp,
                ) {
                    Text(
                        text = stringResource(MR.strings.ml_g4_in_library),
                        style = InkTypography.Eyebrow,
                        color = InkGlassTokens.Accent,
                    )
                }
            }
        }

        Text(
            text = manga.title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = InkTypography.TitleSmall,
            color = InkGlassTokens.TextPrimary,
        )
    }
}

@Composable
private fun EmptyResultItem() {
    InkGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = InkGlassTokens.ScreenHorizontal),
        level = InkGlassLevel.Soft,
        contentPadding = PaddingValues(InkGlassTokens.Space16),
        shadowElevation = 0.dp,
    ) {
        Text(
            text = stringResource(MR.strings.ml_g4_nothing_found),
            style = InkTypography.Body,
            color = InkGlassTokens.TextSecondary,
        )
    }
}
