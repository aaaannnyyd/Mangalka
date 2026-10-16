package mangalka.comments.util

import mangalka.comments.model.CommentNode
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Разбор дат комментариев.
 *
 * Форматов много, потому что каждый движок отдаёт своё:
 *  - ISO 8601 (`2026-08-01T10:00:00Z`) — MangaLib, WordPress, `<time datetime>`;
 *  - `dd.mm.yyyy` и `dd.mm.yyyy HH:mm` — Grouple, DLE;
 *  - unix-время в секундах или миллисекундах — `data-time`;
 *  - относительное (`6mos ago`, `3 часа назад`) — Comix и большинство SPA.
 *
 * Относительные даты восстанавливаются приблизительно, от «сейчас».
 * Это осознанный компромисс: точного времени в таком ответе просто нет,
 * а показать «6 месяцев назад» полезнее, чем пустоту. Для сортировки такие
 * значения использовать нельзя, поэтому адаптеры не сортируют по ним сами.
 */
object CommentDates {

    fun parse(raw: String?, now: () -> Long = System::currentTimeMillis): Long? {
        val s = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null

        // Чистое число — unix-время.
        s.toLongOrNull()?.let { return if (it < 100_000_000_000L) it * 1000 else it }

        parseIso(s)?.let { return it }
        parseNumericDate(s)?.let { return it }
        parseNamedMonth(s, now)?.let { return it }
        parseRelative(s, now)?.let { return it }
        return null
    }

    private fun parseIso(s: String): Long? = runCatching { Instant.parse(s).toEpochMilli() }
        .recoverCatching { OffsetDateTime.parse(s).toInstant().toEpochMilli() }
        .recoverCatching { LocalDateTime.parse(s).toInstant(ZoneOffset.UTC).toEpochMilli() }
        .recoverCatching { LocalDate.parse(s).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() }
        .getOrNull()

    /** `26.08.2026`, `26.08.2026 14:31`, `26/08/2026`, `2026-08-26 14:31`. */
    private fun parseNumericDate(s: String): Long? {
        DMY.find(s)?.let { m ->
            val (d, mo, y) = m.destructured
            val year = y.toInt().let { if (it < 100) 2000 + it else it }
            val time = HM.find(s)
            return runCatching {
                LocalDateTime.of(
                    year,
                    mo.toInt(),
                    d.toInt(),
                    time?.groupValues?.get(1)?.toInt() ?: 0,
                    time?.groupValues?.get(2)?.toInt() ?: 0,
                ).toInstant(ZoneOffset.UTC).toEpochMilli()
            }.getOrNull()
        }
        YMD.find(s)?.let { m ->
            val (y, mo, d) = m.destructured
            val time = HM.find(s)
            return runCatching {
                LocalDateTime.of(
                    y.toInt(),
                    mo.toInt(),
                    d.toInt(),
                    time?.groupValues?.get(1)?.toInt() ?: 0,
                    time?.groupValues?.get(2)?.toInt() ?: 0,
                ).toInstant(ZoneOffset.UTC).toEpochMilli()
            }.getOrNull()
        }
        return null
    }

    /**
     * DLE и подобные движки печатают дату словами: `26 августа 2026 12:34`,
     * `5 января 2026, 09:00`. Год может отсутствовать — тогда берём текущий.
     */
    private fun parseNamedMonth(s: String, now: () -> Long): Long? {
        val lower = s.lowercase()
        val m = NAMED_MONTH.find(lower) ?: return null
        val day = m.groupValues[1].toIntOrNull() ?: return null
        val monthWord = m.groupValues[2]
        val month = RU_MONTHS.entries.firstOrNull { monthWord.startsWith(it.key) }?.value
            ?: EN_MONTHS.entries.firstOrNull { monthWord.startsWith(it.key) }?.value
            ?: return null
        val year = YEAR_IN.find(lower)?.value?.toIntOrNull()
            ?: Instant.ofEpochMilli(now()).atZone(ZoneOffset.UTC).year
        val time = HM.find(lower)
        return runCatching {
            LocalDateTime.of(
                year,
                month,
                day,
                time?.groupValues?.get(1)?.toInt() ?: 0,
                time?.groupValues?.get(2)?.toInt() ?: 0,
            ).toInstant(ZoneOffset.UTC).toEpochMilli()
        }.getOrNull()
    }

    /** `6mos ago`, `3s ago`, `2 days ago`, `5 минут назад`, `Сегодня, 12:34`, `вчера`. */
    private fun parseRelative(s: String, now: () -> Long): Long? {
        val lower = s.lowercase()

        // DLE: «Сегодня, 12:34» / «Вчера, 23:11» — день относительный, время точное.
        val dayOffset = when {
            TODAY.any { it in lower } -> 0L
            YESTERDAY.any { it in lower } -> 1L
            else -> null
        }
        if (dayOffset != null) {
            val time = HM.find(lower)
            val base = Instant.ofEpochMilli(now()).atZone(ZoneOffset.UTC).toLocalDate().minusDays(dayOffset)
            return runCatching {
                base.atTime(
                    time?.groupValues?.get(1)?.toInt() ?: 0,
                    time?.groupValues?.get(2)?.toInt() ?: 0,
                ).toInstant(ZoneOffset.UTC).toEpochMilli()
            }.getOrNull()
        }

        if (lower in JUST_NOW) return now()

        val m = RELATIVE.find(lower) ?: return null
        val value = m.groupValues[1].toLongOrNull() ?: return null
        val unitRaw = m.groupValues[2]
        val millis = UNITS.entries.firstOrNull { (key, _) -> unitRaw.startsWith(key) }?.value
            ?: return null
        return now() - value * millis
    }

    private const val SECOND = 1_000L
    private const val MINUTE = 60 * SECOND
    private const val HOUR = 60 * MINUTE
    private const val DAY = 24 * HOUR
    private const val WEEK = 7 * DAY
    private const val MONTH = 30 * DAY
    private const val YEAR = 365 * DAY

    private val DMY = Regex("""\b(\d{1,2})[.\-/](\d{1,2})[.\-/](\d{2,4})\b""")
    private val YMD = Regex("""\b(\d{4})[.\-/](\d{1,2})[.\-/](\d{1,2})\b""")
    private val HM = Regex("""\b(\d{1,2}):(\d{2})\b""")

    /**
     * Порядок в [UNITS] важен: проверка идёт по `startsWith`, поэтому более
     * длинные префиксы должны стоять раньше более коротких (`mos` перед `m`,
     * `мин` перед `м`), иначе «6mos» опознается как минуты.
     */
    private val UNITS: Map<String, Long> = linkedMapOf(
        "mos" to MONTH, "mon" to MONTH, "мес" to MONTH,
        "sec" to SECOND, "сек" to SECOND,
        "min" to MINUTE, "мин" to MINUTE,
        "hour" to HOUR, "hr" to HOUR, "час" to HOUR,
        "day" to DAY, "дн" to DAY, "ден" to DAY, "сут" to DAY,
        "week" to WEEK, "wk" to WEEK, "нед" to WEEK,
        "year" to YEAR, "yr" to YEAR, "год" to YEAR, "лет" to YEAR,
        "s" to SECOND, "h" to HOUR, "d" to DAY, "w" to WEEK, "y" to YEAR, "m" to MINUTE,
    )

    private val RELATIVE = Regex("""(\d+)\s*([a-zа-я]+)""")
    private val JUST_NOW = setOf("now", "just now", "сейчас", "только что")
    private val TODAY = listOf("сегодня", "today")
    private val YESTERDAY = listOf("вчера", "yesterday")

    private val NAMED_MONTH = Regex("""\b(\d{1,2})\s+([a-zа-я]{3,})""")
    private val YEAR_IN = Regex("""\b(19|20)\d{2}\b""")

    private val RU_MONTHS: Map<String, Int> = linkedMapOf(
        "янв" to 1, "фев" to 2, "мар" to 3, "апр" to 4, "ма" to 5, "июн" to 6,
        "июл" to 7, "авг" to 8, "сен" to 9, "окт" to 10, "ноя" to 11, "дек" to 12,
    )
    private val EN_MONTHS: Map<String, Int> = linkedMapOf(
        "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4, "may" to 5, "jun" to 6,
        "jul" to 7, "aug" to 8, "sep" to 9, "oct" to 10, "nov" to 11, "dec" to 12,
    )
}

/**
 * Стабильный id для комментария, у которого движок id не выводит.
 *
 * Grouple в треде не печатает ни `id`, ни анкор — а id нужен как ключ для
 * `LazyColumn`, для связи ответа с родителем и для запоминания раскрытых
 * спойлеров. Порядковый номер («auto-3») для этого не годится: он меняется
 * при появлении нового комментария, и Compose теряет состояние, а раскрытые
 * спойлеры «перескакивают» на другие комментарии.
 *
 * Поэтому id считается от содержимого: автор + текст. Устойчив к вставке
 * новых комментариев и к смене порядка сортировки.
 */
fun stableCommentId(author: String, body: List<CommentNode>): String {
    val text = buildString {
        append(author)
        append('\u0000')
        body.forEach { appendPlain(it) }
    }
    val digest = MessageDigest.getInstance("SHA-1").digest(text.toByteArray())
    return "s" + digest.take(8).joinToString("") { "%02x".format(it) }
}

private fun StringBuilder.appendPlain(node: CommentNode) {
    when (node) {
        is CommentNode.Text -> append(node.text)
        is CommentNode.Styled -> node.children.forEach { appendPlain(it) }
        is CommentNode.Link -> {
            node.children.forEach { appendPlain(it) }
            append(node.url)
        }
        is CommentNode.Mention -> append('@').append(node.name)
        is CommentNode.Image -> append(node.url)
        is CommentNode.Spoiler -> node.children.forEach { appendPlain(it) }
        is CommentNode.Quote -> node.children.forEach { appendPlain(it) }
        is CommentNode.Paragraph -> {
            node.children.forEach { appendPlain(it) }
            append('\n')
        }
        CommentNode.LineBreak -> append('\n')
    }
}
