package mangalka.comments.adapter

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import mangalka.comments.CommentAdapter
import mangalka.comments.CommentHttp
import mangalka.comments.CommentSourceUnavailableException
import mangalka.comments.model.CommentAuthor
import mangalka.comments.model.CommentFlag
import mangalka.comments.model.CommentNode
import mangalka.comments.model.CommentPage
import mangalka.comments.model.CommentSort
import mangalka.comments.model.CommentTarget
import mangalka.comments.model.CommentVotes
import mangalka.comments.model.UnifiedComment
import mangalka.comments.util.CommentHtml
import org.jsoup.parser.Parser

/**
 * Read-only comments adapter for ReManga.
 *
 * The comments API is public: authentication is not required for reading title/chapter
 * threads or replies. The extension APK is not modified; requests are sent through its
 * own OkHttp client by [mangalka.comments.android.OkHttpCommentHttp].
 *
 * Live API shape (2026-08):
 * - title:   GET /api/v2/activity/comments/?title_id={id}&page={n}&ordering={...}
 * - chapter: GET /api/v2/activity/comments/?chapter_id={id}&page={n}&ordering={...}
 * - replies: add reply_to={parentCommentId}
 *
 * ReManga returns replies as separate lists. We recursively hydrate only comments that
 * report count_replies > 0, preserving the website thread structure.
 */
class ReMangaCommentAdapter(
    private val domains: List<String> = DEFAULT_DOMAINS,
    private val apiRoot: String = DEFAULT_API_ROOT,
    private val siteBase: String = DEFAULT_SITE_BASE,
) : CommentAdapter {

    override val id = "remanga"
    override val name = "ReManga"
    override val supportsChapterComments = true

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val titleCache = java.util.concurrent.ConcurrentHashMap<String, TitleInfo>()

    override fun supports(sourceId: Long, baseUrl: String?): Boolean =
        domains.any { domain -> baseUrl?.contains(domain, ignoreCase = true) == true }

    override suspend fun fetch(
        target: CommentTarget,
        page: Int,
        sort: CommentSort,
        http: CommentHttp,
    ): CommentPage {
        val scope = when (target) {
            is CommentTarget.Manga -> {
                val title = resolveTitle(target, http) ?: return CommentPage.empty(id, page)
                ThreadScope("title_id", title.id, title.commentCount)
            }
            is CommentTarget.Chapter -> {
                // ReManga v115 stores the real API chapter id directly in SChapter.url
                // as `<manga-url>/ch12345`. Prefer that exact id and avoid re-resolving
                // the chapter by number/branch, which can select a different translation
                // branch on popular titles.
                val directId = directChapterId(target.chapterUrl)
                val chapterId = if (directId != null) {
                    directId
                } else {
                    val title = resolveTitle(target, http) ?: return CommentPage.empty(id, page)
                    resolveChapterId(target, title, http) ?: return CommentPage.empty(id, page)
                }
                ThreadScope("chapter_id", chapterId, null)
            }
        }

        val roots = fetchCommentObjects(
            scope = scope,
            page = page,
            ordering = sortKey(sort),
            replyTo = null,
            http = http,
            failIfUnavailable = true,
        ).orEmpty()

        val comments = roots.mapNotNull { root ->
            hydrateComment(
                raw = root,
                parentId = null,
                depth = 0,
                scope = scope,
                http = http,
            )
        }

        return CommentPage(
            comments = comments,
            page = page,
            // ReManga does not expose a root-page count. A full page means another
            // request may exist; an exact-multiple thread can produce one harmless
            // final empty page instead of hiding comments prematurely.
            hasNextPage = roots.size >= PAGE_SIZE,
            totalCount = scope.totalCount,
            providerId = id,
        )
    }

    private suspend fun hydrateComment(
        raw: JsonObject,
        parentId: String?,
        depth: Int,
        scope: ThreadScope,
        http: CommentHttp,
    ): UnifiedComment? {
        val parsed = parseComment(raw, parentId, depth) ?: return null
        val replyCount = raw["count_replies"]?.int() ?: 0
        if (replyCount <= 0 || depth >= MAX_REPLY_DEPTH) return parsed

        val children = fetchAllReplies(
            scope = scope,
            parentId = parsed.id,
            expectedCount = replyCount,
            depth = depth + 1,
            http = http,
        )
        return parsed.copy(children = children)
    }

    private suspend fun fetchAllReplies(
        scope: ThreadScope,
        parentId: String,
        expectedCount: Int,
        depth: Int,
        http: CommentHttp,
    ): List<UnifiedComment> {
        val out = mutableListOf<UnifiedComment>()
        val pagesToFetch = ((expectedCount + PAGE_SIZE - 1) / PAGE_SIZE)
            .coerceIn(1, MAX_REPLY_PAGES)
        for (page in 1..pagesToFetch) {
            val raw = fetchCommentObjects(
                scope = scope,
                page = page,
                ordering = "id",
                replyTo = parentId,
                http = http,
                failIfUnavailable = false,
            ) ?: break

            raw.mapNotNullTo(out) { child ->
                hydrateComment(
                    raw = child,
                    parentId = parentId,
                    depth = depth,
                    scope = scope,
                    http = http,
                )
            }
            if (raw.size < PAGE_SIZE) break
        }
        return out.distinctBy { it.id }
    }

    private suspend fun fetchCommentObjects(
        scope: ThreadScope,
        page: Int,
        ordering: String,
        replyTo: String?,
        http: CommentHttp,
        failIfUnavailable: Boolean,
    ): List<JsonObject>? {
        val query = buildString {
            append(scope.key).append('=').append(scope.id)
            append("&page=").append(page)
            append("&ordering=").append(ordering)
            replyTo?.let { append("&reply_to=").append(it) }
        }
        val urls = listOf(
            "$apiRoot/v2/activity/comments/?$query",
            "$apiRoot/activity/comments?$query",
        )

        var lastFailure: Throwable? = null
        var lastHttpCode: Int? = null
        for (url in urls) {
            val response = runCatching { http.get(url, headers()) }
                .onFailure { lastFailure = it }
                .getOrNull() ?: continue
            lastHttpCode = response.code
            if (!response.isSuccess) continue

            val comments = runCatching {
                when (val root = json.parseToJsonElement(response.body)) {
                    is JsonArray -> root.mapNotNull { it as? JsonObject }
                    is JsonObject -> (root["content"] as? JsonArray)
                        ?.mapNotNull { it as? JsonObject }
                        .orEmpty()
                    else -> emptyList()
                }
            }
                .onFailure { lastFailure = it }
                .getOrNull() ?: continue
            return comments
        }

        if (failIfUnavailable) {
            throw CommentSourceUnavailableException(
                providerName = "ReManga",
                httpCode = lastHttpCode,
                endpoint = urls.first(),
                cause = lastFailure,
            )
        }
        return null
    }

    private fun parseComment(raw: JsonObject, parentId: String?, depth: Int): UnifiedComment? {
        val commentId = raw["id"]?.long()?.toString() ?: return null
        val user = raw["user"] as? JsonObject

        val encodedBody = raw["text"]?.str().orEmpty()
        val decodedBody = Parser.unescapeEntities(encodedBody, false)
        val normalizedBody = CommentHtml.fromHtml(decodedBody, "$siteBase/")
        val body = if (raw["is_spoiler"]?.bool() == true) {
            listOf(CommentNode.Spoiler(normalizedBody))
        } else {
            normalizedBody
        }

        val avatar = user?.get("avatar") as? JsonObject
        val avatarUrl = sequenceOf("high", "mid", "low")
            .mapNotNull { key -> avatar?.get(key)?.str()?.takeIf(String::isNotBlank) }
            .firstOrNull()
            ?.let(::absolutize)

        val rank = raw["rank"]?.str()?.trim().orEmpty()
        val premium = user?.get("is_premium")?.bool() == true
        val badge = rank.takeIf(String::isNotBlank) ?: if (premium) "Premium" else null

        val ageMs = raw["date"]?.long()
        val createdAt = ageMs
            ?.takeIf { it >= 0L }
            ?.let { (System.currentTimeMillis() - it).coerceAtLeast(0L) }

        val flags = buildSet {
            if (raw["is_pinned"]?.bool() == true) add(CommentFlag.PINNED)
        }

        return UnifiedComment(
            id = commentId,
            parentId = parentId,
            author = CommentAuthor(
                name = user?.get("username")?.str().orEmpty(),
                id = user?.get("id")?.long()?.toString(),
                avatarUrl = avatarUrl,
                badge = badge,
            ),
            body = body,
            createdAt = createdAt,
            votes = CommentVotes(score = raw["score"]?.int()),
            depth = depth,
            flags = flags,
        )
    }

    private suspend fun resolveTitle(target: CommentTarget, http: CommentHttp): TitleInfo? {
        val slug = titleSlug(target.mangaUrl, target.absoluteUrl) ?: return null
        titleCache[slug]?.let { return it }

        val candidates = listOf(
            "$apiRoot/v2/titles/$slug/",
            "$apiRoot/titles/$slug/",
        )
        for (url in candidates) {
            val response = runCatching { http.get(url, headers()) }.getOrNull() ?: continue
            if (!response.isSuccess) continue
            val info = runCatching {
                val root = json.parseToJsonElement(response.body).jsonObject
                val value = (root["content"] as? JsonObject) ?: root
                val id = value["id"]?.long() ?: return@runCatching null
                val branches = (value["branches"] as? JsonArray)
                    ?.mapNotNull { (it as? JsonObject)?.get("id")?.long() }
                    .orEmpty()
                TitleInfo(
                    id = id,
                    commentCount = value["count_comments"]?.int(),
                    branchIds = branches,
                )
            }.getOrNull() ?: continue
            titleCache[slug] = info
            return info
        }
        return null
    }

    private suspend fun resolveChapterId(
        target: CommentTarget.Chapter,
        title: TitleInfo,
        http: CommentHttp,
    ): Long? {
        directChapterId(target.chapterUrl)?.let { return it }

        val requestedBranch = BRANCH_ID.find(target.chapterUrl)?.groupValues?.get(1)?.toLongOrNull()
        val branches = buildList {
            requestedBranch?.let(::add)
            title.branchIds.filterNot { it == requestedBranch }.forEach(::add)
        }
        if (branches.isEmpty()) return null

        val targetNumber = target.chapterNumber?.toDouble()
        val targetName = target.chapterName?.trim()?.takeIf(String::isNotBlank)

        for (branchId in branches) {
            var url: String? = "$apiRoot/v2/titles/chapters/" +
                "?branch_id=$branchId&ordering=-index&page=1&count=$CHAPTER_PAGE_SIZE"
            for (attempt in 0 until MAX_CHAPTER_PAGES) {
                val currentUrl = url ?: break
                // A failed page must stop this branch: retrying the same URL up to
                // MAX_CHAPTER_PAGES times only burns the source's rate limit.
                val response = runCatching { http.get(currentUrl, headers()) }.getOrNull() ?: break
                if (!response.isSuccess) break

                val root = runCatching { json.parseToJsonElement(response.body).jsonObject }.getOrNull()
                    ?: break
                val results = root["results"] as? JsonArray ?: break
                val matched = results.firstNotNullOfOrNull { element ->
                    val chapter = element as? JsonObject ?: return@firstNotNullOfOrNull null
                    val number = chapter["chapter"]?.str()?.toDoubleOrNull()
                    val name = chapter["name"]?.str()?.trim().orEmpty()
                    val numberMatches = targetNumber != null && number != null && number == targetNumber
                    val nameMatches =
                        targetNumber == null && targetName != null && name.equals(targetName, ignoreCase = true)
                    if (numberMatches || nameMatches) chapter["id"]?.long() else null
                }
                if (matched != null) return matched

                url = root["next"]?.str()?.takeIf(String::isNotBlank)
            }
        }
        return null
    }

    private fun directChapterId(chapterUrl: String): Long? {
        CHAPTER_ID_QUERY.find(chapterUrl)?.groupValues?.get(1)?.toLongOrNull()?.let { return it }
        CHAPTER_ID_PATH.find(chapterUrl)?.groupValues?.get(1)?.toLongOrNull()?.let { return it }
        return null
    }

    private fun titleSlug(mangaUrl: String, absoluteUrl: String?): String? {
        listOfNotNull(mangaUrl, absoluteUrl).forEach { raw ->
            val clean = raw.substringBefore('?').substringBefore('#').trimEnd('/')
            TITLE_SEGMENT.find(clean)?.groupValues?.get(1)?.takeIf(String::isNotBlank)?.let { return it }
            MANGA_SEGMENT.find(clean)?.groupValues?.get(1)?.takeIf(String::isNotBlank)?.let { return it }
        }

        return mangaUrl
            .substringBefore('?')
            .substringBefore('#')
            .trim('/')
            .split('/')
            .asReversed()
            .firstOrNull { segment ->
                segment.isNotBlank() &&
                    segment !in IGNORED_PATH_SEGMENTS &&
                    segment.toLongOrNull() == null
            }
    }

    private fun sortKey(sort: CommentSort): String = when (sort) {
        CommentSort.NEWEST -> "-id"
        CommentSort.OLDEST -> "id"
        CommentSort.TOP -> "-score"
    }

    private fun headers() = mapOf(
        "Accept" to "application/json",
        "Referer" to "$siteBase/",
        "Origin" to siteBase,
    )

    private fun absolutize(url: String): String? = when {
        url.isBlank() -> null
        url.startsWith("http", ignoreCase = true) -> url
        url.startsWith("//") -> "https:$url"
        else -> siteBase.trimEnd('/') + "/" + url.trimStart('/')
    }

    private data class TitleInfo(
        val id: Long,
        val commentCount: Int?,
        val branchIds: List<Long>,
    )

    private data class ThreadScope(
        val key: String,
        val id: Long,
        val totalCount: Int?,
    )

    private companion object {
        const val DEFAULT_API_ROOT = "https://api.remanga.org/api"
        const val DEFAULT_SITE_BASE = "https://remanga.org"
        val DEFAULT_DOMAINS = listOf("remanga.org", "xn--80aaig9ahr.xn--c1avg")

        const val PAGE_SIZE = 20
        const val MAX_REPLY_PAGES = 20
        const val MAX_REPLY_DEPTH = 20
        const val CHAPTER_PAGE_SIZE = 100
        const val MAX_CHAPTER_PAGES = 30

        val TITLE_SEGMENT = Regex("""/titles?/([^/?#]+)""", RegexOption.IGNORE_CASE)
        val MANGA_SEGMENT = Regex("""/manga/([^/?#]+)""", RegexOption.IGNORE_CASE)
        val CHAPTER_ID_QUERY = Regex("""[?&]chapter_id=(\d+)""", RegexOption.IGNORE_CASE)

        // ReManga v115 stores SChapter.url as `<manga-url>/ch{id}` (no slash
        // between `ch` and the numeric API id). `/chapter/1` remains ambiguous:
        // it can be a human-facing chapter number, so only `ch{id}` and explicit
        // plural `/chapters/{id}` routes are accepted as direct ids.
        val CHAPTER_ID_PATH = Regex(
            """/(?:ch|chapters/)(\d+)(?:/|$|[?#])""",
            RegexOption.IGNORE_CASE,
        )
        val BRANCH_ID = Regex("""[?&]branch_id=(\d+)""", RegexOption.IGNORE_CASE)
        val IGNORED_PATH_SEGMENTS = setOf("main", "chapters", "chapter", "manga", "title", "titles")
    }
}
