package eu.kanade.presentation.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import eu.kanade.presentation.manga.components.MangaCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkAccentButton
import tachiyomi.presentation.core.components.InkEmptyState
import tachiyomi.presentation.core.components.InkErrorState
import tachiyomi.presentation.core.components.InkGlassIconButton
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassProgressBar
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkLoadingState
import tachiyomi.presentation.core.components.InkMangaBackdrop
import tachiyomi.presentation.core.components.InkMangaCard
import tachiyomi.presentation.core.components.InkSectionHeader
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.i18n.stringResource

data class TodayHeroUiModel(
    val mangaId: Long,
    val chapterId: Long,
    val title: String,
    val chapterLabel: String,
    val currentPage: Long,
    val totalPages: Int?,
    val progress: Float?,
    val coverData: Any?,
    val bookmarked: Boolean = false,
    val bookmarkAvailable: Boolean = false,
)

data class TodayRecommendationUiModel(
    val mangaId: Long,
    val title: String,
    val sourceName: String,
    val coverData: Any?,
    val rating: String? = null,
)

data class TodaySimilarUiModel(
    val id: Long,
    val title: String,
    val because: String,
    val coverUrl: String?,
    val score: String?,
)

data class TodayUpdateUiModel(
    val mangaId: Long,
    val chapterId: Long,
    val title: String,
    val chapterLabel: String,
    val relativeTime: String,
    val coverData: Any?,
)

data class TodayUiModel(
    val isLoading: Boolean,
    val hasError: Boolean,
    val hero: TodayHeroUiModel?,
    val recommendations: List<TodayRecommendationUiModel>,
    val similar: List<TodaySimilarUiModel> = emptyList(),
    val updates: List<TodayUpdateUiModel>,
    val history: List<TodayUpdateUiModel>,
)

@Composable
fun TodayScreenContent(
    state: TodayUiModel,
    onSearch: () -> Unit,
    onReadHero: (Long, Long) -> Unit,
    onToggleHeroBookmark: (Long, Long, Boolean) -> Unit,
    onOpenManga: (Long) -> Unit,
    onSearchTitle: (String) -> Unit,
    onOpenUpdates: () -> Unit,
    onOpenHistory: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().background(InkGlassTokens.BackgroundPrimary),
        contentPadding = PaddingValues(bottom = InkGlassTokens.NavigationReservedSpace + InkGlassTokens.Space32),
        verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space32),
    ) {
        item(key = "today_hero") {
            val heroFraction = when {
                state.hero != null -> 0.72f
                state.updates.isEmpty() && state.history.isEmpty() -> 0.82f
                else -> 0.56f
            }
            TodayHero(
                modifier = Modifier.fillParentMaxHeight(heroFraction),
                hero = state.hero,
                isLoading = state.isLoading,
                hasError = state.hasError,
                onSearch = onSearch,
                onRead = onReadHero,
                onBookmark = onToggleHeroBookmark,
                onBrowse = onSearch,
                onRetry = onRetry,
            )
        }

        if (state.recommendations.isNotEmpty()) {
            item(key = "recommendations_header") {
                InkSectionHeader(
                    title = stringResource(MR.strings.ml_g4_today_pinned_sources),
                    modifier = Modifier.padding(horizontal = InkGlassTokens.ScreenHorizontal),
                )
            }
            item(key = "recommendations_row") {
                TodayRecommendationRow(state.recommendations, onOpenManga)
            }
        }

        if (state.similar.isNotEmpty()) {
            item(key = "similar_header") {
                InkSectionHeader(
                    title = stringResource(MR.strings.ml_g4_today_similar),
                    modifier = Modifier.padding(horizontal = InkGlassTokens.ScreenHorizontal),
                )
            }
            item(key = "similar_row") {
                TodaySimilarRow(state.similar, onSearchTitle)
            }
        }

        if (state.updates.isNotEmpty()) {
            item(key = "updates_header") {
                InkSectionHeader(
                    title = stringResource(MR.strings.ml_g4_today_new_chapters),
                    modifier = Modifier.padding(horizontal = InkGlassTokens.ScreenHorizontal),
                    trailing = { SectionAction(stringResource(MR.strings.ml_g4_today_all_updates), onOpenUpdates) },
                )
            }
            item(key = "updates_row") {
                TodayMangaRow(state.updates, onOpenManga)
            }
        }

        if (state.history.isNotEmpty()) {
            item(key = "history_header") {
                InkSectionHeader(
                    title = stringResource(MR.strings.ml_g4_today_recently_read),
                    modifier = Modifier.padding(horizontal = InkGlassTokens.ScreenHorizontal),
                    trailing = { SectionAction(stringResource(MR.strings.history), onOpenHistory) },
                )
            }
            item(key = "history_row") {
                TodayMangaRow(state.history, onOpenManga)
            }
        }
    }
}

@Composable
private fun TodayHero(
    hero: TodayHeroUiModel?,
    isLoading: Boolean,
    hasError: Boolean,
    onSearch: () -> Unit,
    onRead: (Long, Long) -> Unit,
    onBookmark: (Long, Long, Boolean) -> Unit,
    onBrowse: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxWidth()) {
        AtmosphericBackdrop(hero?.coverData)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .padding(
                    start = InkGlassTokens.ScreenHorizontal,
                    end = InkGlassTokens.ScreenHorizontal,
                    top = InkGlassTokens.Space32,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(MR.strings.ml_g4_brand_eyebrow),
                    style = InkTypography.Eyebrow,
                    color = InkGlassTokens.Accent,
                )
                Text(
                    stringResource(MR.strings.ml_g4_today_title),
                    style = InkTypography.Display,
                    color = InkGlassTokens.TextPrimary,
                )
            }
            InkGlassIconButton(Icons.Outlined.Search, stringResource(MR.strings.ml_g4_search), onSearch)
        }

        when {
            isLoading -> InkLoadingState(
                modifier = Modifier.align(Alignment.Center),
            )
            hasError -> InkErrorState(
                title = stringResource(MR.strings.ml_g4_today_error_title),
                message = stringResource(MR.strings.ml_g4_today_error_message),
                onRetry = onRetry,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = InkGlassTokens.Space32),
            )
            hero == null -> InkEmptyState(
                title = stringResource(MR.strings.ml_g4_today_empty_title),
                message = stringResource(MR.strings.ml_g4_today_empty_message),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = InkGlassTokens.ScreenHorizontal,
                        end = InkGlassTokens.ScreenHorizontal,
                        bottom = InkGlassTokens.Space24,
                    ),
                actionLabel = stringResource(MR.strings.ml_g4_today_open_catalog),
                onAction = onBrowse,
            )
            else -> InkGlassSurface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = InkGlassTokens.Space16),
                level = InkGlassLevel.Strong,
                shape = RoundedCornerShape(InkGlassTokens.RadiusLargeCard),
                contentPadding = PaddingValues(InkGlassTokens.Space24),
                shadowElevation = InkGlassTokens.Space16,
                prominent = true,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space16)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(MR.strings.ml_g4_today_continue_reading),
                                style = InkTypography.Eyebrow,
                                color = InkGlassTokens.Accent,
                            )
                            Spacer(Modifier.height(InkGlassTokens.Space8))
                            Text(
                                hero.title,
                                style = InkTypography.Headline,
                                color = InkGlassTokens.TextPrimary,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.width(InkGlassTokens.Space12))
                        if (hero.bookmarkAvailable) {
                            InkGlassIconButton(
                                imageVector = if (hero.bookmarked) {
                                    Icons.Outlined.Bookmark
                                } else {
                                    Icons.Outlined.BookmarkBorder
                                },
                                contentDescription = stringResource(MR.strings.ml_g4_bookmark),
                                onClick = { onBookmark(hero.mangaId, hero.chapterId, !hero.bookmarked) },
                                selected = hero.bookmarked,
                                modifier = Modifier.size(44.dp),
                            )
                        }
                    }

                    Text(
                        listOf(
                            hero.chapterLabel,
                            if (hero.totalPages != null) {
                                stringResource(MR.strings.ml_g4_today_page_of, hero.currentPage, hero.totalPages)
                            } else {
                                stringResource(MR.strings.ml_g4_today_page, hero.currentPage)
                            },
                        ).filter { it.isNotBlank() }.joinToString("  ·  "),
                        style = InkTypography.Body,
                        color = InkGlassTokens.TextSecondary,
                    )
                    hero.progress?.let { InkGlassProgressBar(it) }
                    InkAccentButton(
                        label = stringResource(MR.strings.ml_g4_today_read),
                        onClick = { onRead(hero.mangaId, hero.chapterId) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun AtmosphericBackdrop(coverData: Any?) {
    Box(
        modifier = Modifier.fillMaxSize().background(InkGlassTokens.BackgroundPrimary),
    ) {
        InkMangaBackdrop(intensity = if (coverData == null) 1.15f else 0.55f)
        if (coverData != null) {
            HeroArtwork(coverData, Modifier.fillMaxSize())
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to InkGlassTokens.Scrim.copy(alpha = 0.46f),
                            0.32f to InkGlassTokens.Scrim.copy(alpha = 0.08f),
                            0.64f to InkGlassTokens.Scrim.copy(alpha = 0.18f),
                            0.84f to InkGlassTokens.BackgroundPrimary.copy(alpha = 0.82f),
                            1f to InkGlassTokens.BackgroundPrimary,
                        ),
                    ),
                ),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to InkGlassTokens.BackgroundPrimary.copy(alpha = 0.76f),
                        0.22f to InkGlassTokens.Scrim.copy(alpha = 0.08f),
                        0.72f to InkGlassTokens.Scrim.copy(alpha = 0.04f),
                        1f to InkGlassTokens.BackgroundPrimary.copy(alpha = 0.74f),
                    ),
                ),
        )
    }
}

@Composable
private fun HeroArtwork(coverData: Any?, modifier: Modifier = Modifier) {
    val colorFilter = remember {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.72f) })
    }
    AsyncImage(
        model = coverData,
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Crop,
        colorFilter = colorFilter,
    )
}

@Composable
private fun TodayRecommendationRow(items: List<TodayRecommendationUiModel>, onOpenManga: (Long) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = InkGlassTokens.ScreenHorizontal),
        horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space16),
    ) {
        items(items, key = { "recommendation_${it.mangaId}" }) { item ->
            InkMangaCard(
                title = item.title,
                subtitle = item.sourceName,
                meta = item.rating?.let { "★ $it" },
                onClick = { onOpenManga(item.mangaId) },
                modifier = Modifier.width(154.dp),
            ) {
                MangaCover.Book(
                    data = item.coverData,
                    contentDescription = item.title,
                    shape = RoundedCornerShape(InkGlassTokens.CoverRadius),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Titles found on Shikimori, not yet in any source: tapping searches all sources for them. */
@Composable
private fun TodaySimilarRow(items: List<TodaySimilarUiModel>, onSearchTitle: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = InkGlassTokens.ScreenHorizontal),
        horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space16),
    ) {
        items(items, key = { "similar_${it.id}" }) { item ->
            InkMangaCard(
                title = item.title,
                subtitle = stringResource(MR.strings.ml_g4_today_similar_because, item.because),
                meta = item.score?.let { "★ $it" },
                onClick = { onSearchTitle(item.title) },
                modifier = Modifier.width(154.dp),
            ) {
                MangaCover.Book(
                    data = item.coverUrl,
                    contentDescription = item.title,
                    shape = RoundedCornerShape(InkGlassTokens.CoverRadius),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun TodayMangaRow(items: List<TodayUpdateUiModel>, onOpenManga: (Long) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = InkGlassTokens.ScreenHorizontal),
        horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space16),
    ) {
        items(items, key = { "today_${it.mangaId}_${it.chapterId}" }) { item ->
            InkMangaCard(
                title = item.title,
                subtitle = item.chapterLabel,
                meta = item.relativeTime,
                onClick = { onOpenManga(item.mangaId) },
                modifier = Modifier.width(154.dp),
            ) {
                MangaCover.Book(
                    data = item.coverData,
                    contentDescription = item.title,
                    shape = RoundedCornerShape(InkGlassTokens.CoverRadius),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun SectionAction(label: String, onClick: () -> Unit) {
    InkGlassSurface(
        level = InkGlassLevel.Soft,
        shape = RoundedCornerShape(InkGlassTokens.RadiusChip),
        contentPadding = PaddingValues(horizontal = InkGlassTokens.Space12, vertical = InkGlassTokens.Space8),
        shadowElevation = 0.dp,
        onClick = onClick,
    ) {
        Text(label, style = InkTypography.Label, color = InkGlassTokens.TextSecondary)
    }
}
