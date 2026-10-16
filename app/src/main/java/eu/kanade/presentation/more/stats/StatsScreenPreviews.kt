package eu.kanade.presentation.more.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import eu.kanade.presentation.more.stats.data.StatsData
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import tachiyomi.domain.readingactivity.model.ReadingActivityDay
import tachiyomi.domain.readingactivity.model.readingWeek
import tachiyomi.presentation.core.components.InkGlassTokens
import java.time.LocalDate

private val previewToday = LocalDate.of(2026, 10, 7)

@Preview(name = "Статистика — неделя", widthDp = 390, heightDp = 844)
@Composable
private fun StatsWeeklyPreview() {
    StatsPreview(
        StatsData.Weekly(
            today = previewToday,
            days = readingWeek(
                previewToday,
                listOf(
                    ReadingActivityDay(previewToday.minusDays(2), 25 * 60_000L, 4),
                    ReadingActivityDay(previewToday.minusDays(1), 45 * 60_000L, 7),
                    ReadingActivityDay(previewToday, 12 * 60_000L, 2),
                ),
            ),
            completedChapters = 13,
            measuredSince = previewToday.minusDays(2),
        ),
    )
}

@Preview(name = "Статистика — новый журнал", widthDp = 390, heightDp = 844)
@Composable
private fun StatsWeeklyEmptyPreview() {
    StatsPreview(StatsData.Weekly(previewToday, readingWeek(previewToday, emptyList()), 0, null))
}

@Preview(name = "Статистика — журнал недоступен", widthDp = 390, heightDp = 844)
@Composable
private fun StatsWeeklyErrorPreview() {
    StatsPreview(StatsData.Weekly(previewToday, readingWeek(previewToday, emptyList()), 0, null, unavailable = true))
}

@Composable
private fun StatsPreview(weekly: StatsData.Weekly) {
    TachiyomiPreviewTheme {
        Box(Modifier.fillMaxSize().background(InkGlassTokens.BackgroundPrimary)) {
            StatsScreenContent(
                state = StatsScreenState.Success(
                    overview = StatsData.Overview(42, 8, 120 * 60_000L),
                    titles = StatsData.Titles(30, 18, 2),
                    chapters = StatsData.Chapters(1_204, 510, 63),
                    trackers = StatsData.Trackers(12, 8.5, 1),
                    weekly = weekly,
                ),
                paddingValues = PaddingValues(vertical = InkGlassTokens.Space16),
            )
        }
    }
}
