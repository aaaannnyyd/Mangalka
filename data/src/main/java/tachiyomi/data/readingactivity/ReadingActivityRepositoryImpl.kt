package tachiyomi.data.readingactivity

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CancellationException
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.Database
import tachiyomi.domain.readingactivity.model.ReadingActivityBackup
import tachiyomi.domain.readingactivity.model.ReadingActivityDay
import tachiyomi.domain.readingactivity.model.ReadingActivityEntry
import tachiyomi.domain.readingactivity.repository.ReadingActivityRepository
import java.time.LocalDate

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ReadingActivityRepositoryImpl(
    private val database: Database,
) : ReadingActivityRepository {
    override suspend fun record(entries: List<ReadingActivityEntry>) {
        if (entries.isEmpty()) return
        try {
            database.transaction {
                entries.forEach { entry ->
                    database.reading_activityQueries.record(
                        localDay = entry.day,
                        chapterId = entry.chapterId,
                        durationMillis = entry.durationMillis.coerceAtLeast(0),
                        completed = entry.completed,
                    )
                }
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            // Statistics must never interrupt reading or chapter completion.
            logcat(LogPriority.ERROR, error)
        }
    }

    override suspend fun getDays(from: LocalDate, until: LocalDate): List<ReadingActivityDay> {
        return database.reading_activityQueries.getDays(from.toEpochDay(), until.toEpochDay()) {
                day,
                duration,
                chapters,
            ->
            ReadingActivityDay(LocalDate.ofEpochDay(day), duration ?: 0L, chapters ?: 0L)
        }.awaitAsList()
    }

    override suspend fun getCompletedChapterCount(from: LocalDate, until: LocalDate): Long {
        return database.reading_activityQueries.getCompletedChapterCount(from.toEpochDay(), until.toEpochDay())
            .awaitAsOne()
    }

    override suspend fun getFirstRecordedDay(): LocalDate? {
        return database.reading_activityQueries.getFirstRecordedDay().awaitAsOne().MIN?.let(LocalDate::ofEpochDay)
    }

    override suspend fun export(): List<ReadingActivityBackup> {
        return database.reading_activityQueries.export { source, mangaUrl, chapterUrl, day, duration, completed ->
            ReadingActivityBackup(source, mangaUrl, chapterUrl, day, duration, completed)
        }.awaitAsList()
    }

    override suspend fun restore(entries: List<ReadingActivityBackup>) {
        database.transaction {
            entries.forEach { entry ->
                if (entry.mangaUrl.isBlank() || entry.chapterUrl.isBlank() || entry.durationMillis < 0) return@forEach
                if (entry.day !in LocalDate.MIN.toEpochDay()..LocalDate.MAX.toEpochDay()) return@forEach
                database.reading_activityQueries.restore(
                    localDay = entry.day,
                    durationMillis = entry.durationMillis,
                    completed = entry.completed,
                    sourceId = entry.sourceId,
                    mangaUrl = entry.mangaUrl,
                    chapterUrl = entry.chapterUrl,
                )
            }
        }
    }
}
