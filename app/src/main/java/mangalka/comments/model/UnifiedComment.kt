package mangalka.comments.model

/**
 * Единая модель комментария Mangalka.
 *
 * Полностью независима от расширений: расширения НЕ модифицируются и ничего
 * про эту модель не знают. Заполняется адаптерами [mangalka.comments.CommentAdapter].
 *
 * Дерево ответов хранится в [children]. Плоские источники (а таких большинство)
 * приводятся к дереву через [mangalka.comments.util.buildCommentTree].
 */
data class UnifiedComment(
    /** Стабильный id внутри источника. Уникален в пределах одного треда. */
    val id: String,
    /** id прямого родителя, `null` для корневого комментария. */
    val parentId: String? = null,
    val author: CommentAuthor,
    /** Тело комментария в нормализованном виде — список inline/блочных узлов. */
    val body: List<CommentNode>,
    /** Unix epoch millis. `null`, если источник не отдаёт дату. */
    val createdAt: Long? = null,
    val votes: CommentVotes = CommentVotes.EMPTY,
    /** Прямые ответы. Порядок — как отдал источник (обычно по возрастанию даты). */
    val children: List<UnifiedComment> = emptyList(),
    /** Глубина: 0 — корень. Заполняется построителем дерева. */
    val depth: Int = 0,
    /** Номер страницы главы, к которой привязан комментарий (MangaLib отдаёт `post_page`). */
    val pageNumber: Int? = null,
    /** Признаки: закреплён, удалён, автор — модератор и т.п. */
    val flags: Set<CommentFlag> = emptySet(),
) {
    /** Полное число потомков на всех уровнях. */
    val descendantCount: Int by lazy(LazyThreadSafetyMode.NONE) {
        children.sumOf { 1 + it.descendantCount }
    }

    /** Плоский текст без разметки — для превью, поиска и тестов. */
    val plainText: String by lazy(LazyThreadSafetyMode.NONE) {
        buildString { body.forEach { it.appendPlainText(this) } }.trim()
    }

    /** true, если комментарий целиком или частично скрыт под спойлером. */
    val hasSpoiler: Boolean by lazy(LazyThreadSafetyMode.NONE) {
        body.any { it.containsSpoiler() }
    }
}

data class CommentAuthor(
    val name: String,
    val id: String? = null,
    val avatarUrl: String? = null,
    /** Значок/титул рядом с ником: «Premium», «Модератор», «Переводчик». */
    val badge: String? = null,
    val profileUrl: String? = null,
) {
    companion object {
        val UNKNOWN = CommentAuthor(name = "")
    }
}

/**
 * Лайки и дизлайки раздельно. Источники, отдающие только суммарный рейтинг,
 * кладут его в [score] и оставляют [up]/[down] равными `null`.
 */
data class CommentVotes(
    val up: Int? = null,
    val down: Int? = null,
    /** Суммарный рейтинг. Если не задан явно, вычисляется как `up - down`. */
    val score: Int? = null,
    /** Голос текущего пользователя: `+1`, `-1` или `null`. */
    val userVote: Int? = null,
) {
    val effectiveScore: Int?
        get() = score ?: if (up != null || down != null) (up ?: 0) - (down ?: 0) else null

    companion object {
        val EMPTY = CommentVotes()
    }
}

enum class CommentFlag { PINNED, DELETED, EDITED, MODERATOR, AUTHOR, TRANSLATOR }

/**
 * Нормализованное содержимое комментария.
 *
 * HTML и markdown источников приводятся к этому дереву один раз в адаптере,
 * поэтому UI-слой не занимается ни парсингом HTML, ни санитайзингом.
 */
sealed interface CommentNode {

    data class Text(val text: String) : CommentNode

    data class Styled(
        val children: List<CommentNode>,
        val bold: Boolean = false,
        val italic: Boolean = false,
        val strikethrough: Boolean = false,
        val underline: Boolean = false,
        val code: Boolean = false,
    ) : CommentNode

    data class Link(val children: List<CommentNode>, val url: String) : CommentNode

    data class Mention(val name: String, val userId: String? = null) : CommentNode

    data class Image(val url: String, val alt: String? = null) : CommentNode

    /** Скрытый блок. UI раскрывает по тапу. */
    data class Spoiler(val children: List<CommentNode>, val title: String? = null) : CommentNode

    data class Quote(val children: List<CommentNode>, val author: String? = null) : CommentNode

    data class Paragraph(val children: List<CommentNode>) : CommentNode

    data object LineBreak : CommentNode
}

private fun CommentNode.appendPlainText(sb: StringBuilder) {
    when (this) {
        is CommentNode.Text -> sb.append(text)
        is CommentNode.Styled -> children.forEach { it.appendPlainText(sb) }
        is CommentNode.Link -> children.forEach { it.appendPlainText(sb) }
        is CommentNode.Mention -> sb.append('@').append(name)
        is CommentNode.Image -> alt?.let { sb.append(it) }
        is CommentNode.Spoiler -> children.forEach { it.appendPlainText(sb) }
        is CommentNode.Quote -> children.forEach { it.appendPlainText(sb) }
        is CommentNode.Paragraph -> {
            children.forEach { it.appendPlainText(sb) }
            sb.append('\n')
        }
        CommentNode.LineBreak -> sb.append('\n')
    }
}

private fun CommentNode.containsSpoiler(): Boolean = when (this) {
    is CommentNode.Spoiler -> true
    is CommentNode.Styled -> children.any { it.containsSpoiler() }
    is CommentNode.Link -> children.any { it.containsSpoiler() }
    is CommentNode.Quote -> children.any { it.containsSpoiler() }
    is CommentNode.Paragraph -> children.any { it.containsSpoiler() }
    else -> false
}
