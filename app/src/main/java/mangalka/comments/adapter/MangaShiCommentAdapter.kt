package mangalka.comments.adapter

import mangalka.comments.CommentAdapter
import mangalka.comments.CommentHttp
import mangalka.comments.CommentHttpException
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
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URI
import java.net.URLDecoder
import java.util.concurrent.ConcurrentHashMap

/**
 * Read-only comments adapter for MangaShi (`manga-shi.org`).
 *
 * MangaShi renders both title and chapter comments server-side. The first page is
 * available without authentication, while further pages are exposed through the
 * same HTMX `hx-get` URL that the website uses for its "load more" button.
 *
 * We deliberately follow that URL instead of guessing manga/chapter numeric ids:
 * the public page already contains the exact endpoint and sort/page parameters.
 */
class MangaShiCommentAdapter(
    private val domains: List<String> = DEFAULT_DOMAINS,
) : CommentAdapter {

    override val id = "mangashi"
    override val name = "MangaShi"
    override val supportsChapterComments = true

    /** Exact HTMX URL supplied by page N for page N+1. */
    private val nextPageUrls = ConcurrentHashMap<String, String>()
    private val totalCounts = ConcurrentHashMap<String, Int>()

    override fun supports(sourceId: Long, baseUrl: String?): Boolean =
        domains.any { domain -> baseUrl?.contains(domain, ignoreCase = true) == true }

    override suspend fun fetch(
        target: CommentTarget,
        page: Int,
        sort: CommentSort,
        http: CommentHttp,
    ): CommentPage {
        if (page < 1) return CommentPage.empty(id, page)

        val key = threadKey(target, sort)
        val url = when {
            page == 1 -> target.absoluteUrl?.let { withSort(it, sort) }
            else -> nextPageUrls[pageKey(key, page)]
        } ?: return CommentPage.empty(id, page)

        val response = http.get(url, mapOf("Accept" to "text/html"))
        if (!response.isSuccess) throw CommentHttpException(response.code, url)

        val parsed = parse(response.body, response.finalUrl)
        parsed.totalCount?.let { totalCounts[key] = it }

        val nextKey = pageKey(key, page + 1)
        if (parsed.nextUrl != null) {
            nextPageUrls[nextKey] = parsed.nextUrl
        } else {
            nextPageUrls.remove(nextKey)
        }

        return CommentPage(
            comments = parsed.comments,
            page = page,
            hasNextPage = parsed.nextUrl != null,
            totalCount = parsed.totalCount ?: totalCounts[key],
            providerId = id,
        )
    }

    internal fun parse(html: String, baseUrl: String): ParsedPage {
        val doc = Jsoup.parse(html, baseUrl)
        val scope = doc.selectFirst("#comments-list") ?: doc
        val flat = scope.select("[data-comment-fragment]")
            .mapNotNull { parseComment(it, baseUrl) }

        return ParsedPage(
            comments = buildCommentTree(flat),
            nextUrl = nextUrl(doc, baseUrl),
            totalCount = totalCount(doc),
        )
    }

    private fun parseComment(element: Element, baseUrl: String): UnifiedComment? {
        val commentId = COMMENT_ID.find(element.id())?.groupValues?.get(1) ?: return null
        val parentId = element.parents()
            .firstOrNull { it.hasAttr("data-comment-fragment") }
            ?.id()
            ?.let { COMMENT_ID.find(it)?.groupValues?.get(1) }

        val row = element.children().firstOrNull { child ->
            "flex" in child.classNames() && "items-start" in child.classNames()
        } ?: return null
        val avatarLink = row.children().firstOrNull { child ->
            child.tagName() == "a" && child.attr("href").contains("/profile/")
        }
        val content = row.children().firstOrNull { "flex-1" in it.classNames() } ?: return null
        val header = content.children().firstOrNull { "flex-wrap" in it.classNames() }
        val authorLink = header?.selectFirst("a[href*=/profile/]")
        val profileUrl = authorLink?.attr("abs:href")?.takeIf(String::isNotBlank)

        val badge = header?.children()
            ?.asSequence()
            ?.filter { it.tagName() == "span" }
            ?.map { it.text().trim() }
            ?.firstOrNull { text ->
                text.isNotBlank() &&
                    !text.equals("Спойлер", ignoreCase = true) &&
                    !text.startsWith("в ответ", ignoreCase = true)
            }

        val bodyElement = content.children().firstOrNull { child ->
            "mt-2" in child.classNames() && child.classNames().any { it.startsWith("text-[13px]") }
        } ?: return null

        val spoiler = bodyElement.selectFirst("details")
        val body = if (spoiler != null) {
            val spoilerBody = spoiler.children().firstOrNull { it.tagName() != "summary" }
            val children = CommentHtml.fromHtml(spoilerBody?.html().orEmpty(), baseUrl)
            listOf(CommentNode.Spoiler(children, title = "Спойлер"))
        } else {
            CommentHtml.fromHtml(bodyElement.html(), baseUrl)
        }

        val reactions = content.selectFirst("#comment-reactions-$commentId")
        val up = reactionCount(reactions, "thumbs-up")
        val down = reactionCount(reactions, "thumbs-down")

        val flags = buildSet {
            val badgeLower = badge.orEmpty().lowercase()
            if ("модератор" in badgeLower || "moderator" in badgeLower) add(CommentFlag.MODERATOR)
            if ("автор" in badgeLower || "author" in badgeLower) add(CommentFlag.AUTHOR)
            if ("перевод" in badgeLower || "translator" in badgeLower) add(CommentFlag.TRANSLATOR)
            val marker = "${element.id()} ${element.className()}".lowercase()
            if ("pinned" in marker || element.selectFirst("[class*=pinned]") != null) add(CommentFlag.PINNED)
        }

        return UnifiedComment(
            id = commentId,
            parentId = parentId,
            author = CommentAuthor(
                name = authorLink?.text()?.trim().orEmpty(),
                id = profileUrl?.let(::profileId),
                avatarUrl = avatarLink?.selectFirst("img")?.attr("abs:src")?.takeIf(String::isNotBlank),
                badge = badge,
                profileUrl = profileUrl,
            ),
            body = body,
            createdAt = CREATED_AT.find(element.attr("x-data"))?.groupValues?.get(1)?.let(CommentDates::parse),
            votes = if (up == null && down == null) CommentVotes.EMPTY else CommentVotes(up = up, down = down),
            flags = flags,
        )
    }

    private fun reactionCount(root: Element?, iconMarker: String): Int? {
        if (root == null) return null
        root.children().forEach { item ->
            if (item.selectFirst("[class*=$iconMarker]") != null) {
                return item.select("span").lastOrNull()?.text()?.trim()?.toIntOrNull()
            }
        }
        return null
    }

    private fun totalCount(doc: Document): Int? {
        val text = doc.selectFirst("#comments")?.text() ?: return null
        return COMMENTS_COUNT.find(text)?.groupValues?.get(1)
            ?.replace(" ", "")
            ?.replace("\u00a0", "")
            ?.toIntOrNull()
    }

    private fun nextUrl(doc: Document, baseUrl: String): String? {
        val raw = doc.selectFirst("#comments-load-more [hx-get]")?.attr("hx-get")
            ?.takeIf(String::isNotBlank)
            ?: doc.selectFirst("[hx-get*=/comments/more/]")?.attr("hx-get")?.takeIf(String::isNotBlank)
            ?: return null
        return runCatching { URI(baseUrl).resolve(raw).toString() }.getOrNull()
    }

    private fun withSort(url: String, sort: CommentSort): String {
        val fragment = url.substringAfter('#', "").takeIf(String::isNotEmpty)
        val withoutFragment = url.substringBefore('#')
        val base = withoutFragment.substringBefore('?')
        val params = withoutFragment.substringAfter('?', "")
            .split('&')
            .filter { it.isNotBlank() && !it.substringBefore('=').equals("sort", ignoreCase = true) }
            .toMutableList()
        params += "sort=${sortKey(sort)}"
        return buildString {
            append(base).append('?').append(params.joinToString("&"))
            fragment?.let { append('#').append(it) }
        }
    }

    private fun sortKey(sort: CommentSort): String = when (sort) {
        CommentSort.NEWEST -> "new"
        CommentSort.OLDEST -> "old"
        CommentSort.TOP -> "best"
    }

    private fun threadKey(target: CommentTarget, sort: CommentSort): String = when (target) {
        is CommentTarget.Manga -> "${target.sourceId}:m:${target.mangaUrl}:${sort.name}"
        is CommentTarget.Chapter -> "${target.sourceId}:c:${target.chapterUrl}:${sort.name}"
    }

    private fun pageKey(threadKey: String, page: Int) = "$threadKey:$page"

    private fun profileId(url: String): String? = runCatching {
        val path = URI(url).path.trimEnd('/')
        URLDecoder.decode(path.substringAfterLast('/'), "UTF-8").takeIf(String::isNotBlank)
    }.getOrNull()

    internal data class ParsedPage(
        val comments: List<UnifiedComment>,
        val nextUrl: String?,
        val totalCount: Int?,
    )

    private companion object {
        val DEFAULT_DOMAINS = listOf("manga-shi.org")
        val COMMENT_ID = Regex("""comment-(\d+)""")
        val CREATED_AT = Regex("""new\s+Date\(['\"]([^'\"]+)['\"]\)""")
        val COMMENTS_COUNT = Regex("""([\d\s\u00a0]+)\s+комментар""", RegexOption.IGNORE_CASE)
    }
}
