package mangalka.statistics

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ReadingSessionTimerTest {
    @Test
    fun `foreground sessions exclude background and preserve chapter boundaries`() {
        val timer = ReadingSessionTimer()
        timer.selectChapter(1, 0, 1_000_000)
        timer.setForeground(true, 0, 1_000_000)
        assertEquals(10_000L, timer.setForeground(false, 10_000, 1_010_000)?.durationMillis)
        assertNull(timer.checkpoint(100_000, 1_100_000))
        timer.setForeground(true, 100_000, 1_100_000)
        val previous = timer.selectChapter(2, 110_000, 1_110_000)
        assertEquals(1L, previous?.chapterId)
        assertEquals(10_000L, previous?.durationMillis)
        assertEquals(2L, timer.checkpoint(120_000, 1_120_000)?.chapterId)
    }

    @Test
    fun `idle cap does not charge idle gap after interaction resumes`() {
        val timer = ReadingSessionTimer()
        timer.selectChapter(1, 0, 0)
        timer.setForeground(true, 0, 0)
        assertEquals(120_000L, timer.checkpoint(300_000, 300_000)?.durationMillis)
        assertNull(timer.interact(400_000, 400_000))
        val resumed = timer.checkpoint(410_000, 410_000)
        assertEquals(400_000L, resumed?.startedAtMillis)
        assertEquals(10_000L, resumed?.durationMillis)
    }

    @Test
    fun `repeated lifecycle callbacks do not reset idle timeout or count twice`() {
        val timer = ReadingSessionTimer()
        timer.selectChapter(1, 0, 0)
        timer.setForeground(true, 0, 0)
        assertEquals(60_000L, timer.setForeground(true, 60_000, 60_000)?.durationMillis)
        assertEquals(60_000L, timer.setForeground(true, 180_000, 180_000)?.durationMillis)
        assertNull(timer.checkpoint(180_000, 180_000))
        assertNull(timer.setForeground(false, 190_000, 190_000))
        assertNull(timer.interact(200_000, 200_000))
        assertNull(timer.checkpoint(210_000, 210_000))
    }

    @Test
    fun `wall clock correction does not change measured duration`() {
        val timer = ReadingSessionTimer()
        timer.selectChapter(1, 0, 1_000_000)
        timer.setForeground(true, 0, 1_000_000)
        assertEquals(10_000L, timer.checkpoint(10_000, 1)?.durationMillis)
    }
}
