package mangalka.statistics

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.domain.readingactivity.model.ReadingActivityDay
import tachiyomi.domain.readingactivity.model.readingWeek
import tachiyomi.domain.readingactivity.model.splitReadingInterval
import java.time.LocalDate
import java.time.ZoneId

class ReadingActivityTest {
    @Test
    fun `measured intervals split across local midnight`() {
        val zone = ZoneId.of("Asia/Yakutsk")
        val start = LocalDate.of(2026, 10, 5).atTime(23, 59, 30).atZone(zone).toInstant().toEpochMilli()
        val entries = splitReadingInterval(7, start, 90_000, zone)
        assertEquals(listOf(30_000L, 60_000L), entries.map { it.durationMillis })
        assertEquals(
            listOf(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 6)),
            entries.map {
                LocalDate.ofEpochDay(it.day)
            },
        )
    }

    @Test
    fun `daylight saving day is measured by elapsed milliseconds`() {
        val zone = ZoneId.of("Europe/Berlin")
        val day = LocalDate.of(2026, 3, 29)
        val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val entries = splitReadingInterval(7, start, 24 * 60 * 60_000L, zone)
        assertEquals(listOf(23 * 60 * 60_000L, 60 * 60_000L), entries.map { it.durationMillis })
        assertEquals(day.plusDays(1).toEpochDay(), entries.last().day)
    }

    @Test
    fun `week starts on Monday across year boundary and fills missing days`() {
        val today = LocalDate.of(2027, 1, 1)
        val recorded = listOf(ReadingActivityDay(today, 60_000, 2))
        val week = readingWeek(today, recorded)
        assertEquals(LocalDate.of(2026, 12, 28), week.first().date)
        assertEquals(LocalDate.of(2027, 1, 3), week.last().date)
        assertEquals(7, week.size)
        assertEquals(60_000L, week.sumOf { it.durationMillis })
        assertEquals(2L, week.sumOf { it.completedChapters })
    }

    @Test
    fun `invalid or zero intervals do not invent history`() {
        assertTrue(splitReadingInterval(-1, 0, 60_000, ZoneId.of("UTC")).isEmpty())
        assertTrue(splitReadingInterval(1, 0, 0, ZoneId.of("UTC")).isEmpty())
        assertTrue(splitReadingInterval(1, 0, -1, ZoneId.of("UTC")).isEmpty())
    }
}
