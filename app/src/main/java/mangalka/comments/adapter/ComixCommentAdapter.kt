package mangalka.comments.adapter

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import mangalka.comments.CommentAdapter
import mangalka.comments.CommentHttp
import mangalka.comments.CommentHttpException
import mangalka.comments.CommentTargetResolutionException
import mangalka.comments.UnexpectedCommentSchemaException
import mangalka.comments.model.CommentAuthor
import mangalka.comments.model.CommentFlag
import mangalka.comments.model.CommentPage
import mangalka.comments.model.CommentSort
import mangalka.comments.model.CommentTarget
import mangalka.comments.model.CommentVotes
import mangalka.comments.model.UnifiedComment
import mangalka.comments.util.CommentDates
import mangalka.comments.util.CommentHtml

/**
 * Адаптер комментариев Comix (`comix.to` / зеркало `comix.ws`).
 *
 * ## Почему это вообще работает без обфускации
 *
 * Comix защищает часть API протектором (чанк `secure-*.js` с control-flow
 * flattening; `/api/v1/manga` отвечает `{"message":"Missing token."}`).
 * Комментарии в эту защиту не попали и доступны обычным GET без токена и
 * подписи — проверено живыми запросами. Поэтому WebView не нужен.
 *
 * ## Схема (снята с живого API)
 *
 * ```
 * GET /api/v1/threads/lookup?page_identifier={pi}&page_url={pu}
 *   -> {status, result:{thread:{id, pageIdentifier, commentCount, mainCommentCount, isClosed}}}
 *
 * GET /api/v1/threads/{threadId}/comments?sort={best|newest|oldest}[&cursor=]
 *   -> {status, result:{thread, count, items:[Comment], cursor}}
 * ```
 *
 * `items[]` — уже готовое дерево: корни с вложенным `replies[]`, поэтому
 * пересобирать его не нужно, достаточно рекурсивно отобразить.
 *
 * `page_identifier`:
 *  - тайтл: `manga{mangaId}`
 *  - глава: `manga{mangaId}_chap{chapterNumber}_vol{volume}` (`_vol` обязателен)
 *
 * ## Две ловушки, которые здесь обработаны
 *
 * 1. `threads/lookup` работает как **create-or-get**: при неизвестном
 *    `page_identifier` он не возвращает ошибку, а создаёт новый пустой тред.
 *    То есть опечатка в идентификаторе выглядит как «комментариев нет».
 *    Поэтому идентификатор строится только из данных, полученных с самого
 *    сайта, а не угадывается.
 *
 * 2. Расширение хранит `SManga.url` как `/{hid}-{slug}` (без `/title`), а API
 *    комментариев требует **числовой** `mangaId`. Числовой id отдаёт только
 *    `/api/v1/manga/{hid}`, который закрыт токеном. Поэтому id берётся из
 *    `<script id="initial-data">` на HTML-странице тайтла — тот же путь,
 *    которым идёт сам сайт. Результат кэшируется.
 */
class ComixCommentAdapter(
    private val sourceIds: Set<Long> = DEFAULT_SOURCE_IDS,
    private val domains: List<String> = DEFAULT_DOMAINS,
    private val apiBase: String = DEFAULT_API_BASE,
    private val siteBase: String = DEFAULT_SITE_BASE,
) : CommentAdapter {

    override val id = "comix"
    override val name = "Comix"
    override val supportsChapterComments = true

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /** hid -> numeric mangaId. Экономит один HTML-запрос на каждую страницу треда. */
    private val mangaIdCache = java.util.concurrent.ConcurrentHashMap<String, Long>()

    /**
     * Comix использует cursor, тогда как общий API движка оперирует номером страницы.
     * Запоминаем cursor, полученный после N-й страницы, как токен для N+1.
     */
    private val cursorByPage = java.util.concurrent.ConcurrentHashMap<String, String>()

    override fun supports(sourceId: Long, baseUrl: String?): Boolean =
        sourceId in sourceIds || domains.any { d -> baseUrl?.contains(d, ignoreCase = true) == true }

    override suspend fun fetch(
        target: CommentTarget,
        page: Int,
        sort: CommentSort,
        http: CommentHttp,
    ): CommentPage {
        val mangaId = resolveMangaId(target, http)
            ?: throw CommentTargetResolutionException(
                id,
                "Cannot resolve Comix numeric manga id from ${target.mangaUrl}",
            )

        val pageIdentifier: String
        val pageUrl: String
        when (target) {
            is CommentTarget.Manga -> {
                pageIdentifier = "manga$mangaId"
                pageUrl = titlePath(target.mangaUrl)
            }
            is CommentTarget.Chapter -> {
                val number = chapterNumberOf(target) ?: return CommentPage.empty(id, page)
                pageIdentifier = "manga${mangaId}_chap${number}_vol$DEFAULT_VOLUME"
                pageUrl = chapterPath(target.chapterUrl)
            }
        }

        val thread = lookupThread(pageIdentifier, pageUrl, http) ?: return CommentPage.empty(id, page)
        val threadId = thread["id"]?.long() ?: return CommentPage.empty(id, page)
        val totalCount = thread["commentCount"]?.int()

        val cursorKeyPrefix = "$threadId:${sort.name}:"
        val cursor = if (page > 1) cursorByPage[cursorKeyPrefix + page] ?: return CommentPage.empty(id, page) else null
        val url = buildString {
            append(apiBase).append("/threads/").append(threadId).append("/comments")
            append("?sort=").append(sortKey(sort))
            cursor?.let { append("&cursor=").append(encode(it)) }
        }
        val res = http.get(url, headers())
        if (!res.isSuccess) throw CommentHttpException(res.code, res.finalUrl)

        val result = json.parseToJsonElement(res.body).jsonObject["result"]?.jsonObject
            ?: throw UnexpectedCommentSchemaException(id, "Missing result object in Comix comments response")
        val items = result["items"] as? JsonArray
            ?: throw UnexpectedCommentSchemaException(id, "Missing items array in Comix comments response")
        val nextCursor = result["cursor"]?.str().orEmpty()
        if (nextCursor.isNotBlank()) {
            cursorByPage[cursorKeyPrefix + (page + 1)] = nextCursor
        } else {
            cursorByPage.remove(cursorKeyPrefix + (page + 1))
        }

        return CommentPage(
            comments = items.mapNotNull { (it as? JsonObject)?.let { o -> parseComment(o, null, 0) } },
            page = page,
            // Пагинация курсорная: непустой cursor означает, что есть продолжение.
            hasNextPage = nextCursor.isNotBlank(),
            totalCount = totalCount,
            providerId = id,
        )
    }

    /** Рекурсивно: API отдаёт корни с вложенным `replies[]`. */
    private fun parseComment(o: JsonObject, parentId: String?, depth: Int): UnifiedComment? {
        val cid = o["id"]?.long()?.toString() ?: return null
        val user = o["user"]?.jsonObject

        val children = (o["replies"] as? JsonArray)
            ?.mapNotNull { (it as? JsonObject)?.let { r -> parseComment(r, cid, depth + 1) } }
            .orEmpty()

        val flags = buildSet {
            if (o["isPinned"]?.bool() == true) add(CommentFlag.PINNED)
            if (o["isEdited"]?.bool() == true) add(CommentFlag.EDITED)
            if (user?.get("isMod")?.bool() == true) add(CommentFlag.MODERATOR)
            if (o["status"]?.str() == "deleted") add(CommentFlag.DELETED)
        }

        return UnifiedComment(
            id = cid,
            parentId = parentId ?: o["parentId"]?.long()?.takeIf { it != 0L }?.toString(),
            author = CommentAuthor(
                name = (
                    user?.get("displayName")?.str()?.takeIf(String::isNotBlank)
                        ?: user?.get("username")?.str()
                    ).orEmpty(),
                id = user?.get("hashId")?.str() ?: user?.get("id")?.long()?.toString(),
                avatarUrl = user?.get("avatar")?.str()?.let { absolutize(it) },
                badge = if (user?.get("isMod")?.bool() == true) "Mod" else null,
                profileUrl = user?.get("url")?.str()?.let { absolutize(it) },
            ),
            body = CommentHtml.fromHtml(o["contentHtml"]?.str().orEmpty(), siteBase),
            // API отдаёт только относительное время («6mos ago»), абсолютного нет.
            // Восстанавливаем приблизительно, чтобы UI мог показать хоть что-то.
            createdAt = CommentDates.parse(o["createdAtFormatted"]?.str()),
            votes = CommentVotes(
                up = o["likeCount"]?.int(),
                down = o["dislikeCount"]?.int(),
                userVote = o["userReaction"]?.int(),
            ),
            children = children,
            depth = depth,
            flags = flags,
        )
    }

    private suspend fun lookupThread(
        pageIdentifier: String,
        pageUrl: String,
        http: CommentHttp,
    ): JsonObject? {
        val url = apiBase + "/threads/lookup?page_identifier=" + encode(pageIdentifier) +
            "&page_url=" + encode(pageUrl)
        val res = http.get(url, headers())
        if (!res.isSuccess) throw CommentHttpException(res.code, res.finalUrl)
        return json.parseToJsonElement(res.body).jsonObject["result"]?.jsonObject?.get("thread")?.jsonObject
            ?: throw UnexpectedCommentSchemaException(id, "Missing thread object in Comix lookup response")
    }

    /**
     * Числовой id тайтла.
     *
     * `/api/v1/manga/{hid}` закрыт токеном, поэтому читаем `initial-data` со
     * страницы тайтла: `queries["[\"manga\",\"detail\",\"{hid}\"]"].id`.
     */
    private suspend fun resolveMangaId(target: CommentTarget, http: CommentHttp): Long? {
        val hid = hidOf(target) ?: return null
        mangaIdCache[hid]?.let { return it }

        val res = http.get(siteBase + titlePath(target.mangaUrl), mapOf("Accept" to "text/html"))
        if (!res.isSuccess) throw CommentHttpException(res.code, res.finalUrl)

        val raw = INITIAL_DATA.find(res.body)?.groupValues?.get(1)
            ?: throw UnexpectedCommentSchemaException(id, "Comix initial-data script is missing")
        val queries = json.parseToJsonElement(raw).jsonObject["queries"]?.jsonObject
            ?: throw UnexpectedCommentSchemaException(id, "Comix initial-data queries are missing")
        val numericId = queries.entries
            .firstOrNull { it.key.contains("\"detail\"") && it.key.contains(hid) }
            ?.value?.jsonObject?.get("id")?.long()
            ?: throw CommentTargetResolutionException(
                this.id,
                "Comix detail query does not contain numeric manga id for $hid",
            )

        mangaIdCache[hid] = numericId
        return numericId
    }

    /** `/dvjg-playing-revenge-against-god` -> `dvjg`. */
    private fun hidOf(target: CommentTarget): String? {
        val segment = target.mangaUrl.trim('/').substringAfterLast('/')
        return HID.find(segment)?.groupValues?.get(1)
    }

    /**
     * Расширение хранит `SManga.url` без префикса `/title`
     * (`getBaseUrl() + "/title" + manga.url`), а API комментариев ждёт полный путь.
     */
    private fun titlePath(mangaUrl: String): String {
        val clean = "/" + mangaUrl.trim('/')
        return if (clean.startsWith("/title/")) clean else "/title$clean"
    }

    /** `SChapter.url` хранится без ведущего слэша (`getBaseUrl() + "/" + chapter.url`). */
    private fun chapterPath(chapterUrl: String): String = "/" + chapterUrl.trim('/')

    /**
     * Номер главы для `page_identifier`. Целые номера должны быть без `.0`,
     * иначе идентификатор не совпадёт и lookup создаст пустой тред.
     */
    private fun chapterNumberOf(target: CommentTarget.Chapter): String? {
        CHAPTER_IN_URL.find(target.chapterUrl)?.groupValues?.get(1)?.let { return it }
        val n = target.chapterNumber ?: return null
        return if (n % 1f == 0f) n.toInt().toString() else n.toString()
    }

    private fun sortKey(sort: CommentSort) = when (sort) {
        CommentSort.TOP -> "best"
        CommentSort.NEWEST -> "newest"
        CommentSort.OLDEST -> "oldest"
    }

    private fun headers() = mapOf(
        "Accept" to "application/json",
        "Referer" to "$siteBase/",
    )

    private fun absolutize(url: String): String? = when {
        url.isBlank() -> null
        url.startsWith("http", ignoreCase = true) -> url
        url.startsWith("//") -> "https:$url"
        else -> siteBase.trimEnd('/') + "/" + url.trimStart('/')
    }

    private fun encode(s: String): String = java.net.URLEncoder.encode(s, "UTF-8")

    private companion object {
        const val DEFAULT_SITE_BASE = "https://comix.to"
        const val DEFAULT_API_BASE = "https://comix.to/api/v1"

        /**
         * `_vol` в идентификаторе обязателен: `manga12278_chap60` даёт ошибку,
         * `manga12278_chap60_vol0` — рабочий тред. Тома в данных Mihon для Comix
         * нет, а у сайта для неразбитых на тома серий стоит 0.
         */
        const val DEFAULT_VOLUME = 0

        /** `Source.id` и зеркала считаны из tachiyomi-en.comix. */
        val DEFAULT_SOURCE_IDS = setOf(7537715367149829912L)
        val DEFAULT_DOMAINS = listOf("comix.to", "comix.ws")

        val INITIAL_DATA = Regex("""id="initial-data"[^>]*>(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
        val HID = Regex("""^([a-z0-9]+)-""", RegexOption.IGNORE_CASE)
        val CHAPTER_IN_URL = Regex("""chapter-([\d.]+)""")
    }
}
