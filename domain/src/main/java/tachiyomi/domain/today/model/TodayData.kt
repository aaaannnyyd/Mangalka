package tachiyomi.domain.today.model

import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.updates.model.UpdatesWithRelations

data class TodayData(
    val continueReading: ContinueReading?,
    val updates: List<UpdatesWithRelations>,
    val history: List<HistoryWithRelations>,
)

data class ContinueReading(
    val history: HistoryWithRelations,
    val manga: Manga,
    val chapter: Chapter,
)
