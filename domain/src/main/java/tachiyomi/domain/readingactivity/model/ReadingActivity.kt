package tachiyomi.domain.readingactivity.model

import java.time.LocalDate
import java.time.ZoneId

data class ReadingActivityDay(
    val date: LocalDate,
    val durationMillis: Long,
    val completedChapters: Long,
)

data class ReadingActivityEntry(
    val day: Long,
    val chapterId: Long,
    val durationMillis: Long,
    val completed: Boolean = false,
)

/** Portable identity: database IDs are deliberately excluded from backups. */
data class ReadingActivityBackup(
    val sourceId: Long,
    val mangaUrl: String,
    val chapterUrl: String,
    val day: Long,
    val durationMillis: Long,
    val completed: Boolean,
)

/** Split measured intervals at local midnight, including daylight-saving boundaries. */
fun splitReadingInterval(
    chapterId: Long,
    startedAtMillis: Long,
    durationMillis: Long,
    zone: ZoneId,
): List<ReadingActivityEntry> {
    if (chapterId < 0 || durationMillis <= 0) return emptyList()
    val entries = mutableListOf<ReadingActivityEntry>()
    var cursor = java.time.Instant.ofEpochMilli(startedAtMillis)
    var remaining = durationMillis
    while (remaining > 0) {
        val day = cursor.atZone(zone).toLocalDate()
        val midnight = day.plusDays(1).atStartOfDay(zone).toInstant()
        val part = minOf(remaining, midnight.toEpochMilli() - cursor.toEpochMilli())
        entries += ReadingActivityEntry(day.toEpochDay(), chapterId, part)
        cursor = cursor.plusMillis(part)
        remaining -= part
    }
    return entries
}

fun readingWeek(
    today: LocalDate,
    recorded: List<ReadingActivityDay>,
): List<ReadingActivityDay> {
    val monday = today.minusDays(today.dayOfWeek.value.toLong() - 1)
    val byDate = recorded.associateBy { it.date }
    return (0L..6L).map { offset ->
        val date = monday.plusDays(offset)
        byDate[date] ?: ReadingActivityDay(date, 0, 0)
    }
}
