package eu.kanade.presentation.more.stats.data

import tachiyomi.domain.readingactivity.model.ReadingActivityDay
import java.time.LocalDate

sealed interface StatsData {

    data class Weekly(
        val today: LocalDate,
        val days: List<ReadingActivityDay>,
        val completedChapters: Long,
        val measuredSince: LocalDate?,
        val unavailable: Boolean = false,
    ) : StatsData {
        val durationMillis: Long get() = days.sumOf { it.durationMillis }
    }

    data class Overview(
        val libraryMangaCount: Int,
        val completedMangaCount: Int,
        val totalReadDuration: Long,
    ) : StatsData

    data class Titles(
        val globalUpdateItemCount: Int,
        val startedMangaCount: Int,
        val localMangaCount: Int,
    ) : StatsData

    data class Chapters(
        val totalChapterCount: Int,
        val readChapterCount: Int,
        val downloadCount: Int,
    ) : StatsData

    data class Trackers(
        val trackedTitleCount: Int,
        val meanScore: Double,
        val trackerCount: Int,
    ) : StatsData
}
