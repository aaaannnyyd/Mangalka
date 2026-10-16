package mangalka.statistics

/** Uses monotonic time. Each checkpoint charges only the foreground interval before the idle cutoff. */
class ReadingSessionTimer(
    private val idleTimeoutMillis: Long = 120_000L,
) {
    data class Interval(val chapterId: Long, val startedAtMillis: Long, val durationMillis: Long)

    private var chapterId: Long? = null
    private var foreground = false
    private var lastInteraction = 0L
    private var checkpoint = 0L
    private var checkpointWall = 0L

    @Synchronized
    fun checkpoint(now: Long, wall: Long): Interval? {
        val end = minOf(now, lastInteraction + idleTimeoutMillis)
        val duration = if (foreground && chapterId != null) (end - checkpoint).coerceAtLeast(0) else 0
        val interval = chapterId?.takeIf { duration > 0 }?.let { Interval(it, checkpointWall, duration) }
        checkpoint = now
        checkpointWall = wall
        return interval
    }

    @Synchronized
    fun setForeground(active: Boolean, now: Long, wall: Long): Interval? {
        val interval = checkpoint(now, wall)
        if (active && !foreground) lastInteraction = now
        foreground = active
        return interval
    }

    @Synchronized
    fun interact(now: Long, wall: Long): Interval? {
        val interval = checkpoint(now, wall)
        lastInteraction = now
        return interval
    }

    @Synchronized
    fun selectChapter(id: Long, now: Long, wall: Long): Interval? {
        val interval = checkpoint(now, wall)
        if (chapterId != id) lastInteraction = now
        chapterId = id
        return interval
    }
}
