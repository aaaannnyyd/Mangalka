package tachiyomi.domain.today.interactor

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import tachiyomi.domain.chapter.interactor.GetChapter
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.today.model.ContinueReading
import tachiyomi.domain.today.model.TodayData
import tachiyomi.domain.updates.interactor.GetUpdates

@Inject
class GetTodayData(
    private val getHistory: GetHistory,
    private val getChapter: GetChapter,
    private val getManga: GetManga,
    private val getUpdates: GetUpdates,
) {

    fun subscribe(updatesAfter: Long): Flow<TodayData> {
        return combine(
            getHistory.subscribe(""),
            getUpdates.subscribe(read = false, after = updatesAfter),
        ) { history, updates ->
            history to updates
        }.mapLatest { (history, updates) ->
            var continueReading: ContinueReading? = null
            for (historyEntry in history) {
                val chapter = getChapter.await(historyEntry.chapterId)
                val manga = getManga.await(historyEntry.mangaId)
                if (chapter != null && manga != null) {
                    continueReading = ContinueReading(historyEntry, manga, chapter)
                    break
                }
            }
            TodayData(
                continueReading = continueReading,
                updates = updates,
                history = history,
            )
        }
    }
}
