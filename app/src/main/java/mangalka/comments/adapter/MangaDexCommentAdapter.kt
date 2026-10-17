package mangalka.comments.adapter

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mangalka.comments.CommentAdapter
import mangalka.comments.CommentHttp
import mangalka.comments.CommentHttpException
import mangalka.comments.model.CommentAuthor
import mangalka.comments.model.CommentFlag
import mangalka.comments.model.CommentPage
import mangalka.comments.model.CommentSort
import mangalka.comments.model.CommentTarget
import mangalka.comments.model.CommentVotes
import mangalka.comments.model.UnifiedComment
import mangalka.comments.util.CommentHtml
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.util.concurrent.ConcurrentHashMap

/**
 * Read-only comments for MangaDex.
 *
 * MangaDex keeps discussions on its XenForo forum. The public statistics API maps a title or chapter UUID to the
 * forum thread and its reply count; the thread pages are plain HTML, oldest first, [POSTS_PER_PAGE] posts each.
 * The first post is the MangaDex bot's announcement and is skipped. Threads are created on the first comment,
 * so a title or chapter without one has no comments yet.
 */
class MangaDexCommentAdapter(
    private val apiBase: String = DEFAULT_API_BASE,
    private val forumBase: String = DEFAULT_FORUM_BASE,
) : CommentAdapter {

    override val id = "mangadex"
    override val name = "MangaDex"
    override val supportsChapterComments = true

    private val json = Json { ignoreUnknownKeys = true }
    private val threads = ConcurrentHashMap<String, Thread>()

    private data class Thread(val id: Long, val replies: Int)

    override fun supports(sourceId: Long, baseUrl: String?): Boolean =
        baseUrl?.contains("mangadex.org", ignoreCase = true) == true

    override suspend fun fetch(
        target: CommentTarget,
        page: Int,
        sort: CommentSort,
        http: CommentHttp,
    ): CommentPage {
        val (kind, uuid) = when (target) {
            is CommentTarget.Manga -> "manga" to uuidOf(target.mangaUrl)
            is CommentTarget.Chapter -> "chapter" to uuidOf(target.chapterUrl)
        }
        uuid ?: return CommentPage.empty(id, page)
        val thread = (if (page == 1) null else threads[uuid]) ?: thread(kind, uuid, http)
            ?: return CommentPage.empty(id, page)

        // Post #1 is the bot's announcement, so the thread holds replies + 1 posts.
        val forumPages = maxOf(1, (thread.replies + POSTS_PER_PAGE) / POSTS_PER_PAGE)
        if (page > forumPages) return CommentPage.empty(id, page)
        val newestFirst = sort != CommentSort.OLDEST
        val forumPage = if (newestFirst) forumPages - page + 1 else page

        val url = "$forumBase/threads/${thread.id}/page-$forumPage"
        val response = http.get(url, mapOf("Accept" to "text/html"))
        if (!response.isSuccess) throw CommentHttpException(response.code, url)

        val posts = parsePosts(response.body, response.finalUrl).let { if (newestFirst) it.reversed() else it }
        return CommentPage(
            comments = posts,
            page = page,
            hasNextPage = page < forumPages,
            totalCount = thread.replies,
            providerId = id,
        )
    }

    private suspend fun thread(kind: String, uuid: String, http: CommentHttp): Thread? {
        val url = "$apiBase/statistics/$kind/$uuid"
        val response = http.get(url, mapOf("Accept" to "application/json"))
        if (!response.isSuccess) throw CommentHttpException(response.code, url)
        val comments = json.parseToJsonElement(response.body).jsonObject["statistics"]
            ?.jsonObject?.get(uuid)?.jsonObject?.get("comments")
            ?.takeIf { it is kotlinx.serialization.json.JsonObject }?.jsonObject
            ?: return null
        val thread = Thread(
            id = comments["threadId"]?.jsonPrimitive?.int?.toLong() ?: return null,
            replies = comments["repliesCount"]?.jsonPrimitive?.intOrNull ?: 0,
        )
        threads[uuid] = thread
        return thread
    }

    internal fun parsePosts(html: String, baseUrl: String): List<UnifiedComment> {
        val doc = Jsoup.parse(html, baseUrl)
        return doc.select("article.message--post")
            .filterNot { it.hasClass("is-first") && it.attr("data-author") == BOT_NAME }
            .mapNotNull(::parsePost)
    }

    private fun parsePost(post: Element): UnifiedComment? {
        val postId = post.attr("data-content").removePrefix("post-").ifBlank { return null }
        val wrapper = post.selectFirst(".message-body .bbWrapper") ?: return null
        wrapper.select(".bbCodeBlock--quote").forEach { quote ->
            // Quoting the bot's thread opener only repeats the chapter banner.
            if (quote.attr("data-quote") == BOT_NAME) {
                quote.remove()
                return@forEach
            }
            quote.attr("data-author", quote.attr("data-quote"))
            quote.select(".bbCodeBlock-title, .bbCodeBlock-expandLink").remove()
            quote.select(".bbCodeBlock-expandContent").forEach { it.unwrap() }
            quote.select(".bbCodeBlock-content").forEach { it.unwrap() }
        }
        wrapper.select(".bbCodeSpoiler-button, .bbCodeInlineSpoiler-button").remove()

        val userLink = post.selectFirst(".message-name a.username")
        val name = post.attr("data-author").ifBlank { userLink?.text().orEmpty() }
        val banner = post.select(".message-userBanner strong").firstOrNull()?.text()
            ?: post.selectFirst(".message-userTitle")?.text()
        val staff = post.selectFirst(".username--staff, .username--moderator") != null
        return UnifiedComment(
            id = postId,
            author = CommentAuthor(
                name = name,
                id = userLink?.attr("data-user-id")?.ifBlank { null },
                avatarUrl = post.selectFirst(".message-avatar img")?.attr("abs:src")?.ifBlank { null },
                badge = banner?.takeIf { staff },
                profileUrl = userLink?.attr("abs:href")?.ifBlank { null },
            ),
            body = CommentHtml.fromHtml(wrapper.html(), post.baseUri()),
            createdAt = post.selectFirst("time.u-dt")?.attr("data-time")?.toLongOrNull()?.times(1000),
            votes = CommentVotes(up = reactionCount(post).takeIf { it > 0 }),
            flags = buildSet {
                if (staff) add(CommentFlag.MODERATOR)
                if (post.selectFirst(".message-lastEdit") != null) add(CommentFlag.EDITED)
            },
        )
    }

    /** "A, B and 3 others" → 5; XenForo lists up to three names, then a remainder count. */
    private fun reactionCount(post: Element): Int {
        val text = post.selectFirst(".reactionsBar-link")?.text()?.trim().orEmpty()
        if (text.isEmpty()) return 0
        val others = OTHERS.find(text)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val named = text.replace(OTHERS, "").split(", ", " and ").count { it.isNotBlank() }
        return named + others
    }

    private fun uuidOf(url: String): String? = UUID.find(url)?.value

    private companion object {
        const val DEFAULT_API_BASE = "https://api.mangadex.org"
        const val DEFAULT_FORUM_BASE = "https://forums.mangadex.org"
        const val POSTS_PER_PAGE = 20
        const val BOT_NAME = "MangaDex"
        val OTHERS = Regex("""\s*and (\d+) others?$""")
        val UUID = Regex("""[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}""", RegexOption.IGNORE_CASE)
    }
}
