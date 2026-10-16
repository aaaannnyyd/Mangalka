package eu.kanade.presentation.today

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import eu.kanade.tachiyomi.R

@Preview(name = "Today golden", widthDp = 412, heightDp = 892, showBackground = true)
@Composable
private fun TodayScreenPreview() {
    TodayScreenContent(
        state = TodayUiModel(
            isLoading = false,
            hasError = false,
            hero = TodayHeroUiModel(
                mangaId = 1,
                chapterId = 11,
                title = "Пепел над последним городом",
                chapterLabel = "Глава 42 · Затмение",
                currentPage = 18,
                totalPages = 29,
                progress = 18f / 29f,
                coverData = R.drawable.inkflow_today_preview_art,
                bookmarked = true,
                bookmarkAvailable = true,
            ),
            recommendations = listOf(
                TodayRecommendationUiModel(
                    mangaId = 5,
                    title = "Край ночи",
                    sourceName = "MangaLib",
                    coverData = R.drawable.inkflow_today_preview_art,
                    rating = "8.9/10",
                ),
                TodayRecommendationUiModel(
                    mangaId = 6,
                    title = "Пепельный охотник",
                    sourceName = "Remanga",
                    coverData = R.drawable.inkflow_today_preview_art,
                    rating = "9.1/10",
                ),
            ),
            updates = listOf(
                TodayUpdateUiModel(
                    2,
                    21,
                    "Город без рассвета",
                    "Глава 18",
                    "2 ч",
                    R.drawable.inkflow_today_preview_art,
                ),
                TodayUpdateUiModel(
                    3,
                    31,
                    "Стеклянное море",
                    "Глава 67",
                    "Сегодня",
                    R.drawable.inkflow_today_preview_art,
                ),
            ),
            history = listOf(
                TodayUpdateUiModel(4, 41, "Белый шум", "Глава 23", "Вчера", R.drawable.inkflow_today_preview_art),
            ),
        ),
        onSearch = {},
        onReadHero = { _, _ -> },
        onToggleHeroBookmark = { _, _, _ -> },
        onOpenManga = {},
        onSearchTitle = {},
        onOpenUpdates = {},
        onOpenHistory = {},
        onRetry = {},
    )
}
