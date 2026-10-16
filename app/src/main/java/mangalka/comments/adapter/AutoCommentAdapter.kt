package mangalka.comments.adapter

import mangalka.comments.CommentAdapter
import mangalka.comments.CommentHttp
import mangalka.comments.model.CommentAuthor
import mangalka.comments.model.CommentFlag
import mangalka.comments.model.CommentNode
import mangalka.comments.model.CommentPage
import mangalka.comments.model.CommentSort
import mangalka.comments.model.CommentTarget
import mangalka.comments.model.CommentVotes
import mangalka.comments.model.UnifiedComment
import mangalka.comments.util.CommentDates
import mangalka.comments.util.CommentHtml
import mangalka.comments.util.buildCommentTree
import mangalka.comments.util.stableCommentId
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

/**
 * Универсальный парсер комментариев для сайтов без своего адаптера.
 *
 * Работает по HTML страницы и ничего не требует от расширения.
 *
 * ## Два прохода
 *
 * 1. **Микроданные schema.org** — если на странице есть `itemtype="…/Comment"`,
 *    используем только их. Это не «ещё одна эвристика», а разметка, которую
 *    движок объявил сам, поэтому она даёт точное разделение треда и мусора.
 *
 *    Пример, на котором это проверено (Grouple/selfmanga): на странице 76
 *    вхождений класса `comments-item`, из которых 70 — виджет «Последние
 *    комментарии» в сайдбаре, не имеющий отношения к треду. А `schema.org/Comment`
 *    встречается ровно 6 раз и ровно в треде. Эвристика по классам тут обязана
 *    ошибиться, микроданные — нет.
 *
 * 2. **Эвристика по классам/структуре** — для движков без микроданных (DLE,
 *    старые форумные движки). Кандидаты ранжируются [scoreContainer], контейнеры
 *    внутри сайдбарных панелей штрафуются.
 *
 * ## Чего парсер намеренно не делает
 *
 * Не выдумывает данные, которых на странице нет. Если движок не выводит в треде
 * дату и рейтинг (а Grouple именно так и делает — они есть только в сайдбаре),
 * поля останутся `null`. Лучше пустое поле, чем правдоподобная ложь.
 */
class AutoCommentAdapter(
    /** Минимальный балл контейнера для эвристического прохода. */
    private val minScore: Int = 12,
    private val maxComments: Int = 400,
) : CommentAdapter {

    override val id = "auto"
    override val name = "Auto"
    override val supportsChapterComments = false

    /**
     * Exact next-page URLs discovered in server-rendered pagination.
     *
     * We deliberately do not guess `?page=2`: DLE/Grouple themes use different
     * schemes (`cstart`, `/page/2`, custom query params). The first page teaches
     * us the URL for the next one, and sequential UI pagination reuses it.
     */
    private val pageUrls = ConcurrentHashMap<PageKey, String>()

    override fun supports(sourceId: Long, baseUrl: String?): Boolean = true

    override suspend fun fetch(
        target: CommentTarget,
        page: Int,
        sort: CommentSort,
        http: CommentHttp,
    ): CommentPage {
        val firstUrl = target.absoluteUrl ?: return CommentPage.empty(id, page)
        val key = threadKey(target, firstUrl)
        val url = if (page <= 1) {
            firstUrl
        } else {
            pageUrls[PageKey(key, page)] ?: return CommentPage.empty(id, page)
        }

        val res = http.get(url, mapOf("Accept" to "text/html"))
        if (!res.isSuccess) return CommentPage.empty(id, page)

        val doc = Jsoup.parse(res.body, res.finalUrl)
        val result = parse(doc, res.finalUrl, page)
        nextPageUrl(doc, res.finalUrl)?.let { next ->
            pageUrls[PageKey(key, page + 1)] = next
        } ?: pageUrls.remove(PageKey(key, page + 1))
        return result
    }

    /** Отдельно от [fetch], чтобы гонять на офлайн-фикстурах и через AutoParserCli. */
    fun parse(html: String, baseUrl: String, page: Int = 1): CommentPage =
        parse(Jsoup.parse(html, baseUrl), baseUrl, page)

    private fun parse(doc: Document, baseUrl: String, page: Int): CommentPage {
        val flat = parseMicrodata(doc, baseUrl).ifEmpty { parseHeuristic(doc, baseUrl) }
        if (flat.isEmpty()) return CommentPage.empty(id, page)

        return CommentPage(
            comments = buildCommentTree(flat),
            page = page,
            hasNextPage = nextPageUrl(doc, baseUrl) != null,
            totalCount = flat.size,
            providerId = id,
        )
    }

    // --- проход 1: микроданные schema.org --------------------------------

    private fun parseMicrodata(doc: Document, baseUrl: String): List<UnifiedComment> {
        val items = doc.select(MICRODATA_ITEM)
        if (items.isEmpty()) return emptyList()

        val ids = HashMap<Element, String>(items.size)
        items.forEach { el -> ids[el] = explicitId(el) ?: "" }

        val result = ArrayList<UnifiedComment>(items.size)
        for (el in items.take(maxComments)) {
            val author = microdataAuthor(el, baseUrl)
            val body = microdataBody(el, baseUrl)
            if (body.isEmpty() && author.name.isBlank()) continue

            val cid = ids[el]!!.ifBlank {
                stableCommentId(author.name, body).also { ids[el] = it }
            }
            result += UnifiedComment(
                id = cid,
                parentId = ancestorId(el, ids),
                author = author,
                body = body,
                createdAt = microdataDate(el),
                votes = microdataVotes(el),
                flags = flagsOf(el),
            )
        }
        return result
    }

    private fun microdataAuthor(el: Element, baseUrl: String): CommentAuthor {
        val node: Element? = el.selectFirst("[itemprop=author]") ?: el.selectFirst(NAMED_AUTHORS)
        val name = (node?.selectFirst("[itemprop=name]")?.text() ?: node?.text()).orEmpty().trim()
        return CommentAuthor(
            name = name.removePrefix("@"),
            id = authorId(el, node),
            avatarUrl = avatarOf(el, baseUrl),
            badge = null,
            profileUrl = node?.takeIf { it.tagName() == "a" }?.attr("abs:href")
                ?: node?.selectFirst("a")?.attr("abs:href")?.takeIf(String::isNotBlank),
        )
    }

    private fun authorId(el: Element, authorNode: Element?): String? {
        el.selectFirst("[data-user-id]")?.attr("data-user-id")?.takeIf(String::isNotBlank)?.let { return it }
        val href = authorNode?.takeIf { it.tagName() == "a" }?.attr("href")
            ?: authorNode?.selectFirst("a")?.attr("href")
        return href?.let { USER_ID_IN_URL.find(it)?.groupValues?.get(1) }
    }

    private fun microdataBody(el: Element, baseUrl: String): List<CommentNode> {
        val node = el.selectFirst("[itemprop=description]")
            ?: el.selectFirst("[itemprop=text]")
            ?: el.selectFirst("[itemprop=commentText]")
            ?: el.selectFirst(NAMED_BODIES)
            ?: return strippedBody(el, baseUrl)
        return CommentHtml.fromHtml(node.html(), baseUrl)
    }

    private fun microdataDate(el: Element): Long? {
        el.selectFirst("[itemprop=dateCreated], [itemprop=datePublished]")?.let { n ->
            val raw = n.attr("datetime").ifBlank { n.attr("content") }.ifBlank { n.text() }
            CommentDates.parse(raw)?.let { return it }
        }
        return extractDate(el)
    }

    private fun microdataVotes(el: Element): CommentVotes {
        fun prop(name: String): Int? = el.selectFirst("[itemprop=$name]")?.let { n ->
            (n.attr("content").ifBlank { n.text() }).let { NUM.find(it)?.value?.toIntOrNull() }
        }
        val up = prop("upvoteCount")
        val down = prop("downvoteCount")
        if (up != null || down != null) return CommentVotes(up = up, down = down)
        return extractVotes(el)
    }

    // --- проход 2: эвристика ---------------------------------------------

    private fun parseHeuristic(doc: Document, baseUrl: String): List<UnifiedComment> {
        val container = findContainer(doc) ?: return emptyList()
        val items = commentElements(container)
        if (items.isEmpty()) return emptyList()

        val ids = HashMap<Element, String>(items.size)
        items.forEach { el -> ids[el] = explicitId(el) ?: "" }

        val out = ArrayList<UnifiedComment>(items.size)
        for (el in items.take(maxComments)) {
            val body = extractBody(el, baseUrl)
            if (body.isEmpty()) continue
            val author = extractAuthor(el, baseUrl)
            val cid = ids[el]!!.ifBlank {
                stableCommentId(author.name, body).also { ids[el] = it }
            }
            out += UnifiedComment(
                id = cid,
                parentId = explicitParentId(el) ?: ancestorId(el, ids),
                author = author,
                body = body,
                createdAt = extractDate(el),
                votes = extractVotes(el),
                flags = flagsOf(el),
            )
        }
        return out
    }

    private fun findContainer(doc: Document): Element? {
        val candidates = LinkedHashSet<Element>()
        candidates += doc.select(NAMED_CONTAINERS)
        doc.select(NAMED_ITEMS).mapNotNullTo(candidates) { it.parent() }

        return candidates
            .map { it to scoreContainer(it) }
            .filter { it.second >= minScore }
            .maxByOrNull { it.second }
            ?.first
    }

    private fun scoreContainer(el: Element): Int {
        val children = el.children()
        if (children.isEmpty()) return 0

        var score = 0
        val name = "${el.id()} ${el.className()}".lowercase()
        // A last pagination page can legitimately contain a single comment.
        // Keep the stricter two-child requirement only for unnamed containers.
        if (children.size < 2 && COMMENT_WORDS.none { it in name } && DISCUSSION_WORDS.none { it in name }) return 0
        if (COMMENT_WORDS.any { it in name }) score += 8
        if (DISCUSSION_WORDS.any { it in name }) score += 6

        val classes = children.mapNotNull { it.className().takeIf(String::isNotBlank) }
        val dominant = classes.groupingBy { it }.eachCount().maxByOrNull { it.value }?.value ?: 0
        if (dominant >= 2) score += minOf(dominant, 10)

        val sample = children.take(8)
        if (sample.any { looksLikeAuthor(it) }) score += 4
        if (sample.any { it.selectFirst("time[datetime]") != null || DATE_TEXT.containsMatchIn(it.text()) }) score += 3
        if (sample.count { it.text().length > 20 } >= 2) score += 3

        if (el.select("nav, ul.menu, .pagination").isNotEmpty()) score -= 4
        // Виджеты «последние комментарии» в сайдбаре структурно неотличимы от
        // треда, но тредом не являются: их содержимое относится к другим тайтлам.
        if (isInsideSidebarWidget(el)) score -= 12
        return score
    }

    private fun isInsideSidebarWidget(el: Element): Boolean {
        var cur: Element? = el
        while (cur != null) {
            val n = "${cur.id()} ${cur.className()}".lowercase()
            if (SIDEBAR_WORDS.any { it in n }) return true
            cur = cur.parent()
        }
        return false
    }

    private fun looksLikeAuthor(el: Element): Boolean =
        el.selectFirst(NAMED_AUTHORS) != null || el.selectFirst(AVATAR_SELECTOR) != null

    private fun commentElements(container: Element): List<Element> {
        val direct = container.children().filter { child ->
            val n = "${child.id()} ${child.className()}".lowercase()
            COMMENT_WORDS.any { it in n } || child.text().length > 20
        }
        if (direct.size >= 2) {
            return direct.flatMap { listOf(it) + it.select(NAMED_ITEMS) }.distinct()
        }
        // Jsoup's Element.select() can include the container itself; e.g.
        // `#dle-comments-list` also matches the broad `[id^=dle-comm]` selector.
        return container.select(NAMED_ITEMS).filterNot { it === container }
    }

    // --- общие извлекатели ------------------------------------------------

    private fun explicitId(el: Element): String? {
        ID_ATTRS.forEach { a -> el.attr(a).takeIf(String::isNotBlank)?.let { return it } }
        ID_IN_ATTR.find(el.id())?.groupValues?.get(1)?.let { return it }
        // DLE/Grouple прячут id комментария в ссылке-анкоре: `#comm_439958`, `#comment-123`.
        el.select("a[href*=#comm]").forEach { a ->
            ID_IN_ANCHOR.find(a.attr("href"))?.groupValues?.get(1)?.let { return it }
        }
        el.selectFirst("a[name~=^comment]")?.attr("name")?.let { n ->
            ID_IN_ATTR.find(n)?.groupValues?.get(1)?.let { return it }
        }
        return el.id().takeIf(String::isNotBlank)
    }

    private fun explicitParentId(el: Element): String? {
        PARENT_ATTRS.forEach { a ->
            el.attr(a).takeIf { it.isNotBlank() && it != "0" }?.let { return it }
        }
        return null
    }

    /** Ближайший предок, который сам является распознанным комментарием. */
    private fun ancestorId(el: Element, ids: Map<Element, String>): String? {
        var p = el.parent()
        while (p != null) {
            ids[p]?.takeIf(String::isNotBlank)?.let { return it }
            p = p.parent()
        }
        return null
    }

    private fun extractAuthor(el: Element, baseUrl: String): CommentAuthor {
        // DLE's `.com_user` also contains the date, while `.name` contains only
        // the author. Prefer the narrow child before the generic union selector.
        val node: Element? = el.selectFirst(".com_user > .name, .com_user .name")
            ?: el.selectFirst(NAMED_AUTHORS)
        val name = node?.text()?.trim()?.removePrefix("@").orEmpty()
        val avatar = avatarOf(el, baseUrl)
        val profileEl: Element? = when {
            node == null -> null
            node.tagName() == "a" -> node
            else -> node.selectFirst("a")
        }
        return if (name.isBlank() && avatar == null) {
            CommentAuthor.UNKNOWN
        } else {
            CommentAuthor(
                name = name,
                id = authorId(el, node),
                avatarUrl = avatar,
                profileUrl = profileEl?.attr("abs:href")?.takeIf(String::isNotBlank),
            )
        }
    }

    /**
     * Аватар с учётом ленивой загрузки.
     *
     * `src` у lazyload-картинок пустой или заглушка, реальный url лежит в
     * `data-src` / `data-original` / `data-avatar`. Без этого аватары теряются
     * целиком — именно так и происходило на Grouple.
     */
    private fun avatarOf(el: Element, baseUrl: String): String? {
        el.selectFirst("[data-avatar]")?.attr("data-avatar")?.let { v ->
            absolutize(v, baseUrl)?.let { return it }
        }
        el.selectFirst(AVATAR_BACKGROUND_SELECTOR)?.attr("style")?.let { style ->
            BACKGROUND_URL.find(style)?.groupValues?.get(2)?.let { value ->
                absolutize(value, baseUrl)?.let { return it }
            }
        }

        val img: Element = el.selectFirst(AVATAR_SELECTOR) ?: return null
        for (attr in AVATAR_ATTRS) {
            val v = img.attr(attr)
            if (v.isNotBlank() && !v.startsWith("data:")) {
                absolutize(v, baseUrl)?.let { return it }
            }
        }
        return null
    }

    private fun absolutize(url: String, baseUrl: String): String? = when {
        url.isBlank() -> null
        url.startsWith("http", true) -> url
        url.startsWith("//") -> "https:$url"
        else -> runCatching { java.net.URI(baseUrl).resolve(url).toString() }.getOrNull()
    }

    private fun extractBody(el: Element, baseUrl: String): List<CommentNode> {
        // Standard DLE wraps the actual comment in `.com_content > .text` and
        // may append a signature to `.com_content`; keep the signature out.
        val explicit = el.selectFirst(".com_content > .text, .com_content .text")
            ?: el.selectFirst(NAMED_BODIES)
        if (explicit != null) return CommentHtml.fromHtml(explicit.html(), baseUrl)
        return strippedBody(el, baseUrl)
    }

    /**
     * Тело, когда явного контейнера текста нет: клонируем элемент и вырезаем
     * служебные узлы. Без этого текст вложенных ответов утекает в родителя.
     */
    private fun strippedBody(el: Element, baseUrl: String): List<CommentNode> {
        val clone = el.clone()
        clone.select(MICRODATA_ITEM).remove()
        clone.select(NAMED_ITEMS).remove()
        clone.select(NAMED_AUTHORS).remove()
        clone.select(NAMED_META).remove()
        clone.select("script, style, form, textarea, button, img[class*=avatar]").remove()
        return CommentHtml.fromHtml(clone.html(), baseUrl)
    }

    private fun extractDate(el: Element): Long? {
        el.selectFirst("time[datetime]")?.attr("datetime")?.let { iso ->
            CommentDates.parse(iso)?.let { return it }
        }
        el.selectFirst("[data-time], [data-timestamp]")?.let { n: Element ->
            val v = n.attr("data-time").ifBlank { n.attr("data-timestamp") }
            v.toLongOrNull()?.let { return if (it < 100_000_000_000L) it * 1000 else it }
        }
        // Текстовая дата в мета-узле: `26.08.2026`, `26.08.2026 14:31`, `3 часа назад`.
        el.selectFirst(NAMED_DATES)?.text()?.let { CommentDates.parse(it)?.let { d -> return d } }
        return null
    }

    /**
     * Голоса. Число часто лежит НЕ внутри узла с классом-иконкой, а в его
     * соседе: `<i class="fas fa-thumbs-up"></i><span>12</span>`. Поэтому если
     * внутри узла числа нет, смотрим на соседей и на родителя.
     */
    private fun extractVotes(el: Element): CommentVotes {
        // Standard DLE themes expose one net rating next to plus/minus controls.
        // Treating the same number as both `up` and `down` would fabricate data.
        numberNear(el, DLE_SCORE_SELECTOR)?.let { return CommentVotes(score = it) }

        val up = numberNear(el, UP_SELECTOR)
        val down = numberNear(el, DOWN_SELECTOR)
        val score = numberNear(el, SCORE_SELECTOR)
        return if (up == null && down == null && score == null) {
            CommentVotes.EMPTY
        } else {
            CommentVotes(up = up, down = down, score = score)
        }
    }

    private fun numberNear(root: Element, selector: String): Int? {
        val node = root.selectFirst(selector) ?: return null
        NUM.find(node.ownText())?.value?.toIntOrNull()?.let { return it }
        NUM.find(node.text())?.value?.toIntOrNull()?.let { return it }
        node.nextElementSibling()?.text()?.let { t -> NUM.find(t)?.value?.toIntOrNull()?.let { return it } }
        node.parent()?.text()?.let { t -> NUM.find(t)?.value?.toIntOrNull()?.let { return it } }
        return null
    }

    private fun flagsOf(el: Element): Set<CommentFlag> {
        val marker = "${el.className()} ${el.attr("data-vip")}".lowercase()
        return buildSet {
            if ("pinned" in marker || el.selectFirst("[class*=pinned]") != null) add(CommentFlag.PINNED)
            if (el.selectFirst("[class*=moderator], [class*=is-mod]") != null) add(CommentFlag.MODERATOR)
            if (el.selectFirst("[class*=edited]") != null) add(CommentFlag.EDITED)
        }
    }

    private fun threadKey(target: CommentTarget, firstUrl: String): String =
        "${target.sourceId}|${target::class.simpleName}|$firstUrl"

    private fun nextPageUrl(doc: Document, baseUrl: String): String? {
        val link = doc.selectFirst(NEXT_PAGE_SELECTOR) ?: return null
        val href = link.attr("href").takeIf(String::isNotBlank) ?: return null
        return when {
            href.startsWith("http", ignoreCase = true) -> href
            href.startsWith("//") -> "https:$href"
            // java.net.URI.resolve("?cstart=2") drops the last path segment.
            // DLE frequently uses query-only pagination links, so preserve the page path.
            href.startsWith("?") -> baseUrl.substringBefore('#').substringBefore('?') + href
            else -> runCatching { URI(baseUrl).resolve(href).toString() }.getOrNull()
        }
    }

    private data class PageKey(val thread: String, val page: Int)

    private companion object {
        val COMMENT_WORDS = listOf("comment", "comm", "otziv", "otzyv", "review", "kommentar", "disqus")
        val DISCUSSION_WORDS = listOf("discussion", "obsuzhdenie", "thread")
        val SIDEBAR_WORDS = listOf(
            "info-panel",
            "sidebar",
            "aside",
            "latest-comment",
            "last-comments",
            "recent-comment",
            "widget",
        )

        /** schema.org/Comment и schema.org/UserComments. */
        const val MICRODATA_ITEM =
            "[itemtype*=schema.org/Comment], [itemtype*=schema.org/UserComments]"

        const val NAMED_CONTAINERS =
            "#comments, #comment, #dle-comments-list, #comments-list, #commentsList, " +
                "[id*=comment][id!=comment-form], [id*=discussion], [class*=comments-list], " +
                "[class*=comment-list], [class*=commentsList], [class*=comments__list], " +
                "[class*=b-comment], ol.commentlist"

        const val NAMED_ITEMS =
            "[id^=comment-], [id^=comment_], [id^=comment-id-], [id^=dle-comm], [class*=comment-item], " +
                "[class*=comments-item], [class*=comment_item], [class*=comment-body], " +
                "[class*=commentBody], li.comment, div.comment, article.comment, " +
                "[class*=b-comment__item]"

        const val NAMED_AUTHORS =
            "[itemprop=author], [class*=author], [class*=username], [class*=user-name], " +
                "[class*=nickname], [class*=comment-name], .com_user > .name, .com_user .name, .com_user, .fn"

        const val NAMED_BODIES =
            "[itemprop=description], [itemprop=text], [class*=comment-text], " +
                "[class*=comment_text], [class*=comment-content], [class*=comments-item__content], " +
                "[class*=commentText], [class*=comment__body], [class*=comments-item__body], " +
                ".com_content > .text, .com_content .text, .com_content, [class*=message]"

        const val NAMED_META =
            "[class*=date], [class*=time], [class*=meta], [class*=actions], [class*=toolbar], " +
                "[class*=reply-link], [class*=rating], [class*=header__extra]"

        const val NAMED_DATES =
            "[class*=meta], [class*=date], [class*=time], [class*=ago], .com_user .grey, .com_info .grey, time"

        const val AVATAR_SELECTOR =
            "img[class*=avatar], img[src*=avatar], img[data-src*=avatar], img[class*=user], " +
                "[class*=avatar] img"
        const val AVATAR_BACKGROUND_SELECTOR =
            "[class*=avatar] [style*=background-image], [class*=avatar][style*=background-image]"

        val AVATAR_ATTRS = listOf("data-src", "data-original", "data-lazy-src", "data-echo", "src")
        val BACKGROUND_URL = Regex("""url\((['"]?)(.*?)\1\)""", RegexOption.IGNORE_CASE)

        const val UP_SELECTOR =
            "[class*=thumbs-up], [class*=vote-up], [class*=like]:not([class*=dislike]), " +
                "[class*=plus], [class*=upvote]"
        const val DOWN_SELECTOR =
            "[class*=thumbs-down], [class*=vote-down], [class*=dislike], [class*=minus], " +
                "[class*=downvote]"
        const val DLE_SCORE_SELECTOR = ".rate_like, .rate_like-dislike, .rate_stars"
        const val SCORE_SELECTOR = "[class*=rating], [class*=score], [class*=karma]"

        const val NEXT_PAGE_SELECTOR =
            ".comments-navigation a[rel=next], .comments-navigation a[class*=next], " +
                ".comment-navigation a[rel=next], .comment-navigation a[class*=next], " +
                "#dle-comments-list ~ .navigation a[rel=next], #dle-comments-list ~ .navigation a[class*=next], " +
                ".pagination a[rel=next], .pagination a[class*=next], [rel=next], " +
                ".navigation a:contains(Вперед), .navigation a:contains(Далее), " +
                ".pagination a:contains(Вперед), .pagination a:contains(Далее)"

        val ID_ATTRS = listOf("data-comment-id", "data-id", "data-comm-id")
        val PARENT_ATTRS = listOf("data-parent", "data-parent-id", "data-reply-to", "data-answer-to")
        val ID_IN_ATTR = Regex("""(\d{2,})""")
        val ID_IN_ANCHOR = Regex("""#comm(?:ent)?[_-]?(\d+)""")
        val USER_ID_IN_URL = Regex("""/(?:user|users|u|profile)/(\d+)""")
        val NUM = Regex("""-?\d+""")
        val DATE_TEXT = Regex("""\d{1,2}[.\-/]\d{1,2}[.\-/]\d{2,4}|\d{1,2}:\d{2}""")
    }
}
