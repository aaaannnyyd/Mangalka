package mangalka.comments.util

import mangalka.comments.model.CommentNode
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.parser.Parser

/**
 * Нормализация тела комментария в [CommentNode].
 *
 * Один вход для трёх форматов, которые реально встречаются у источников:
 *  - HTML (MangaLib, Grouple, DLE);
 *  - BBCode (старые DLE-движки, `[spoiler]`, `[b]`, `[quote]`);
 *  - markdown-подобная разметка (`**bold**`, `||spoiler||`, `>!spoiler!<`).
 *
 * Всё приводится к дереву узлов ОДИН раз здесь, поэтому Compose-слой
 * не парсит HTML на каждой рекомпозиции и не занимается санитайзингом.
 */
object CommentHtml {

    private val SPOILER_CLASS_MARKERS = listOf("spoiler", "spoile", "hidden-text", "hide-block")

    /** Обёртки, которые нужно «раскрыть», не создавая узел: `.spoiler-node__text` и подобные. */
    private val TRANSPARENT_CLASS_MARKERS = listOf(
        "spoiler-node__text",
        "spoiler__text",
        "spoiler-body",
        "spoiler_text",
        "spoiler-content",
    )

    fun fromHtml(html: String, baseUrl: String? = null): List<CommentNode> {
        if (html.isBlank()) return emptyList()
        val body = Jsoup.parseBodyFragment(html, baseUrl.orEmpty()).body()
        val nodes = body.childNodes().flatMap { convert(it, baseUrl) }
        return collapse(nodes)
    }

    fun fromBBCode(text: String, baseUrl: String? = null): List<CommentNode> =
        fromHtml(bbcodeToHtml(text), baseUrl)

    fun fromMarkdownish(text: String, baseUrl: String? = null): List<CommentNode> =
        fromHtml(markdownishToHtml(text), baseUrl)

    /** Обычный текст: экранируется, переводы строк превращаются в [CommentNode.LineBreak]. */
    fun fromPlainText(text: String): List<CommentNode> {
        val out = ArrayList<CommentNode>()
        val lines = text.split('\n')
        lines.forEachIndexed { i, line ->
            if (line.isNotEmpty()) out += CommentNode.Text(line)
            if (i != lines.lastIndex) out += CommentNode.LineBreak
        }
        return collapse(out)
    }

    // --- HTML -> узлы ------------------------------------------------------

    private fun convert(node: Node, baseUrl: String?): List<CommentNode> = when (node) {
        is TextNode -> {
            val t = node.wholeText.replace('\u00a0', ' ')
            if (t.isBlank() && !t.contains(' ')) emptyList() else listOf(CommentNode.Text(t))
        }
        is Element -> convertElement(node, baseUrl)
        else -> emptyList()
    }

    private fun convertElement(el: Element, baseUrl: String?): List<CommentNode> {
        val kids = { el.childNodes().flatMap { convert(it, baseUrl) } }
        val cls = el.className().lowercase()

        // Спойлер определяем по классу или data-атрибуту, а не по имени тега:
        // разные движки используют span/div/blockquote с одинаковой семантикой.
        if (isSpoiler(el, cls)) {
            val title = el.attr("data-spoiler-text").ifBlank { spoilerTitleFrom(el) }.ifBlank { null }
            val inner = collapse(unwrapSpoilerBody(el, baseUrl))
            return listOf(CommentNode.Spoiler(inner, title))
        }
        if (TRANSPARENT_CLASS_MARKERS.any { it in cls }) return kids()

        return when (el.tagName().lowercase()) {
            "p" -> listOf(CommentNode.Paragraph(collapse(kids())))
            "br" -> listOf(CommentNode.LineBreak)
            "b", "strong" -> listOf(CommentNode.Styled(collapse(kids()), bold = true))
            "i", "em" -> listOf(CommentNode.Styled(collapse(kids()), italic = true))
            "s", "strike", "del" -> listOf(CommentNode.Styled(collapse(kids()), strikethrough = true))
            "u", "ins" -> listOf(CommentNode.Styled(collapse(kids()), underline = true))
            "code", "kbd", "samp" -> listOf(CommentNode.Styled(collapse(kids()), code = true))
            "pre" -> listOf(CommentNode.Paragraph(listOf(CommentNode.Styled(collapse(kids()), code = true))))
            "a" -> {
                val href = el.attr("abs:href").ifBlank { el.attr("href") }
                val mention = mentionFrom(el, href)
                when {
                    mention != null -> listOf(mention)
                    href.isBlank() -> kids()
                    else -> listOf(CommentNode.Link(collapse(kids()), href))
                }
            }
            "img" -> {
                val src = el.attr("abs:src").ifBlank { el.attr("src") }
                if (src.isBlank()) {
                    emptyList()
                } else {
                    listOf(CommentNode.Image(src, el.attr("alt").ifBlank { null }))
                }
            }
            "blockquote", "q" -> listOf(
                CommentNode.Quote(collapse(kids()), el.attr("data-author").ifBlank { null }),
            )
            "li" -> listOf(CommentNode.Paragraph(collapse(listOf(CommentNode.Text("• ")) + kids())))
            "script", "style", "noscript", "iframe", "form", "button", "svg" -> emptyList()
            else -> kids()
        }
    }

    private fun isSpoiler(el: Element, cls: String): Boolean {
        if (el.hasAttr("data-spoiler-type") || el.hasAttr("data-spoiler-text")) return true
        if (TRANSPARENT_CLASS_MARKERS.any { it in cls }) return false
        return SPOILER_CLASS_MARKERS.any { it in cls }
    }

    /**
     * MangaLib кладёт текст спойлера во вложенный `.spoiler-node__text`, а заголовок —
     * в `data-spoiler-text`. Если внутри есть такой контейнер, берём только его,
     * иначе — всё содержимое, кроме узла-заголовка.
     */
    private fun unwrapSpoilerBody(el: Element, baseUrl: String?): List<CommentNode> {
        val explicitBody = el.children().firstOrNull { child ->
            TRANSPARENT_CLASS_MARKERS.any { it in child.className().lowercase() }
        }
        if (explicitBody != null) {
            return explicitBody.childNodes().flatMap { convert(it, baseUrl) }
        }
        return el.childNodes()
            .filterNot { it is Element && isSpoilerTitle(it) }
            .flatMap { convert(it, baseUrl) }
    }

    private fun isSpoilerTitle(el: Element): Boolean {
        val c = el.className().lowercase()
        return "title_spoiler" in c || "spoiler-title" in c || "spoiler__title" in c ||
            "spoiler-node__label" in c
    }

    private fun spoilerTitleFrom(el: Element): String =
        el.children().firstOrNull { isSpoilerTitle(it) }?.text().orEmpty()

    private fun mentionFrom(el: Element, href: String): CommentNode.Mention? {
        val text = el.text()
        if (!text.startsWith("@")) return null
        val id = Regex("/(?:user|users|profile)/(\\d+)").find(href)?.groupValues?.getOrNull(1)
        return CommentNode.Mention(text.removePrefix("@"), id)
    }

    // --- Схлопывание -----------------------------------------------------

    /** Склеивает соседние Text-узлы и убирает пустышки/висящие переводы строк. */
    private fun collapse(nodes: List<CommentNode>): List<CommentNode> {
        val out = ArrayList<CommentNode>(nodes.size)
        for (n in nodes) {
            val last = out.lastOrNull()
            when {
                n is CommentNode.Text && n.text.isEmpty() -> Unit
                n is CommentNode.Text && last is CommentNode.Text ->
                    out[out.lastIndex] = CommentNode.Text(last.text + n.text)
                n is CommentNode.Paragraph && n.children.isEmpty() -> Unit
                n == CommentNode.LineBreak && (last == null || last == CommentNode.LineBreak) -> Unit
                else -> out += n
            }
        }
        while (out.lastOrNull() == CommentNode.LineBreak) out.removeAt(out.lastIndex)
        // Единственный абзац — разворачиваем, чтобы не плодить вложенность.
        if (out.size == 1) {
            val only = out[0]
            if (only is CommentNode.Paragraph) return only.children
        }
        return out
    }

    // --- BBCode / markdown -> HTML ---------------------------------------

    private fun bbcodeToHtml(src: String): String {
        var s = Parser.unescapeEntities(src, false)
        s = s.replace(Regex("(?is)\\[spoiler(?:=([^\\]]*))?](.*?)\\[/spoiler]")) { m ->
            val title = m.groupValues[1].trim()
            val attr = if (title.isEmpty()) "" else " data-spoiler-text=\"${escape(title)}\""
            "<span class=\"spoiler\"$attr>${m.groupValues[2]}</span>"
        }
        s = s.replace(Regex("(?is)\\[quote(?:=([^\\]]*))?](.*?)\\[/quote]")) { m ->
            val a = m.groupValues[1].trim()
            val attr = if (a.isEmpty()) "" else " data-author=\"${escape(a)}\""
            "<blockquote$attr>${m.groupValues[2]}</blockquote>"
        }
        s = s.replace(Regex("(?is)\\[url=([^\\]]+)](.*?)\\[/url]"), "<a href=\"$1\">$2</a>")
        s = s.replace(Regex("(?is)\\[img](.*?)\\[/img]"), "<img src=\"$1\">")
        for ((bb, tag) in listOf("b" to "b", "i" to "i", "u" to "u", "s" to "s", "code" to "code")) {
            s = s.replace(Regex("(?is)\\[$bb](.*?)\\[/$bb]"), "<$tag>$1</$tag>")
        }
        return s.replace("\n", "<br>")
    }

    private fun markdownishToHtml(src: String): String {
        var s = escape(src)
        s = s.replace(Regex("(?s)\\|\\|(.+?)\\|\\|"), "<span class=\"spoiler\">$1</span>")
        s = s.replace(Regex("(?s)>!(.+?)!<"), "<span class=\"spoiler\">$1</span>")
        s = s.replace(Regex("(?s)\\*\\*(.+?)\\*\\*"), "<strong>$1</strong>")
        s = s.replace(Regex("(?s)~~(.+?)~~"), "<s>$1</s>")
        s = s.replace(Regex("(?s)(?<![*\\w])\\*(?!\\*)(.+?)(?<!\\*)\\*(?![*\\w])"), "<em>$1</em>")
        s = s.replace(Regex("(?s)`(.+?)`"), "<code>$1</code>")
        s = s.replace(Regex("\\[([^\\]]+)]\\((https?://[^)\\s]+)\\)"), "<a href=\"$2\">$1</a>")
        s = s.replace(Regex("(?m)^&gt;\\s?(.*)$"), "<blockquote>$1</blockquote>")
        return s.replace("\n", "<br>")
    }

    private fun escape(s: String): String = s
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
}
