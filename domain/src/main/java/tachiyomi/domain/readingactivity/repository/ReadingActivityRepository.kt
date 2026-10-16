package tachiyomi.domain.readingactivity.repository

import tachiyomi.domain.readingactivity.model.ReadingActivityBackup
import tachiyomi.domain.readingactivity.model.ReadingActivityDay
import tachiyomi.domain.readingactivity.model.ReadingActivityEntry
import java.time.LocalDate

interface ReadingActivityRepository {
    suspend fun record(entries: List<ReadingActivityEntry>)

    suspend fun getDays(from: LocalDate, until: LocalDate): List<ReadingActivityDay>

    suspend fun getCompletedChapterCount(from: LocalDate, until: LocalDate): Long

    suspend fun getFirstRecordedDay(): LocalDate?

    suspend fun export(): List<ReadingActivityBackup>

    /** Remaps portable URLs, merges with max rather than adding, and ignores missing chapters. */
    suspend fun restore(entries: List<ReadingActivityBackup>)
}
