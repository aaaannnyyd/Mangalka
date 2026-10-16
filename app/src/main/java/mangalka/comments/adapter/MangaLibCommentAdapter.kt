package mangalka.comments.adapter

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
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
import mangalka.comments.util.CommentHtml
import mangalka.comments.util.buildCommentTree

/**
 * Адаптер комментариев для семейства MangaLib / HentaiLib / YaoiLib / SlashLib.
 * Движок lib.social использует общий comments API и различает сайты заголовком `Site-Id`.
 *
 * Схема проверена на живом API:
 * ```
 * GET https://api.cdnlibs.org/api/comments?post_id={id}&post_type={manga|chapter}&page={n}
 * Site-Id: 1            # MangaLib; HentaiLib = 4
 * Referer: https://mangalib.me/
 *
 * {"data":{"root":[..],"replies":[..]},"meta":{"has_next_page":bool,"page":int,"per_page":int}}
 * ```
 * `root[]` — комментарии уровня 0, `replies[]` — ВСЕ ответы плоским списком
 * с `parent_comment` и `comment_level` (наблюдалась вложенность до 13 уровней),
 * поэтому дерево собирается на нашей стороне через [buildCommentTree].
 *
 * Расширение при этом не модифицируется: нужен только url тайтла, из которого
 * достаётся числовой id (`{id}--{slug}`).
 */
class MangaLibCommentAdapter(
    private val sites: List<Site> = DEFAULT_SITES,
    private val apiHost: String = API_HOST,
) : CommentAdapter {

    /** Один сайт семейства. [siteId] уходит в заголовок `Site-Id`. */
    data class Site(
        val sourceId: Long,
        val siteId: Int,
        val domain: String,
        val label: String,
    )

    override val id = "mangalib"
    override val name = "MangaLib / HentaiLib"
    override val supportsChapterComments = true
    override val supportsPageComments = true

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override fun supports(sourceId: Long, baseUrl: String?): Boolean = resolveSite(sourceId, baseUrl) != null

    private fun resolveSite(sourceId: Long, baseUrl: String?): Site? =
        sites.firstOrNull { it.sourceId == sourceId }
            ?: baseUrl?.let { url -> sites.firstOrNull { url.contains(it.domain, ignoreCase = true) } }

    override suspend fun fetch(
        target: CommentTarget,
        page: Int,
        sort: CommentSort,
        http: CommentHttp,
    ): CommentPage {
        val site = resolveSite(target.sourceId, target.absoluteUrl)
            ?: return CommentPage.empty(id, page)

        val postType: String
        val postId: Long
        when (target) {
            is CommentTarget.Manga -> {
                postType = "manga"
                postId = resolveMangaId(target.mangaUrl, site, http) ?: return CommentPage.empty(id, page)
            }
            is CommentTarget.Chapter -> {
                // Комментарии к главе требуют числовой id главы. LibGroup хранит
                // volume/number и, при наличии, branch_id прямо в SChapter.url.
                // Резолвим точную запись главы через тот же авторизованный client,
                // иначе на тайтлах с несколькими ветками легко выбрать чужую главу.
                postType = "chapter"
                postId = resolveChapterId(target, site, http)
                    ?: throw CommentTargetResolutionException(
                        id,
                        "chapterUrl=${target.chapterUrl}; mangaUrl=${target.mangaUrl}",
                    )
            }
        }

        val url = "$apiHost/api/comments?post_id=$postId&post_type=$postType&page=$page" +
            when (sort) {
                CommentSort.TOP -> "&sort_by=rating"
                CommentSort.OLDEST -> "&sort_by=id&sort_type=asc"
                CommentSort.NEWEST -> ""
            }

        val res = http.get(url, headers(site))
        if (!res.isSuccess) throw CommentHttpException(res.code, res.finalUrl)

        val root = json.parseToJsonElement(res.body).jsonObject
        val data = root["data"]?.jsonObject
            ?: throw UnexpectedCommentSchemaException(id, "Missing data object in comments response")
        val meta = root["meta"]?.jsonObject

        val flat = buildList {
            data["root"]?.asArray()?.forEach { addIfComment(it) }
            data["replies"]?.asArray()?.forEach { addIfComment(it) }
        }

        return CommentPage(
            comments = buildCommentTree(flat),
            page = meta?.get("page")?.int() ?: page,
            hasNextPage = meta?.get("has_next_page")?.bool() ?: false,
            totalCount = null, // API не сообщает общее число
            providerId = id,
        )
    }

    private fun MutableList<UnifiedComment>.addIfComment(el: JsonElement) {
        (el as? JsonObject)?.let { obj -> parseComment(obj)?.let { add(it) } }
    }

    private fun parseComment(o: JsonObject): UnifiedComment? {
        val cid = o["id"]?.long() ?: return null
        val user = o["user"]?.jsonObject
        val votes = o["votes"]?.jsonObject

        val flags = buildSet {
            if (user?.get("premium")?.jsonObject?.get("enabled")?.bool() == true) add(CommentFlag.AUTHOR)
        }

        return UnifiedComment(
            id = cid.toString(),
            // parent_comment — прямой родитель; root_id — только вершина ветки,
            // использовать его как parentId означало бы схлопнуть дерево в 2 уровня.
            parentId = o["parent_comment"]?.long()?.toString(),
            author = CommentAuthor(
                name = user?.get("username")?.str().orEmpty(),
                id = user?.get("id")?.long()?.toString(),
                avatarUrl = user?.get("avatar")?.jsonObject?.get("url")?.str(),
                badge = if (user?.get("premium")?.jsonObject?.get("enabled")?.bool() == true) "Premium" else null,
            ),
            body = CommentHtml.fromHtml(o["comment"]?.str().orEmpty()),
            createdAt = o["created_at_ts"]?.long(),
            votes = CommentVotes(
                up = votes?.get("up")?.int(),
                down = votes?.get("down")?.int(),
                userVote = votes?.get("user")?.int(),
            ),
            pageNumber = o["post_page"]?.int(),
            flags = flags,
        )
    }

    /** `/ru/manga/7580--i-alone-level-up` -> `7580`. */
    private suspend fun resolveMangaId(mangaUrl: String, site: Site, http: CommentHttp): Long? {
        SLUG_ID.find(mangaUrl)?.groupValues?.get(1)?.toLongOrNull()?.let { return it }

        val slug = mangaUrl.trim('/').substringAfterLast('/').ifBlank { return null }
        val res = runCatching { http.get("$apiHost/api/manga/$slug", headers(site)) }.getOrNull() ?: return null
        if (!res.isSuccess) return null
        return runCatching {
            json.parseToJsonElement(res.body).jsonObject["data"]?.jsonObject?.get("id")?.long()
        }.getOrNull()
    }

    private suspend fun resolveChapterId(
        target: CommentTarget.Chapter,
        site: Site,
        http: CommentHttp,
    ): Long? {
        CHAPTER_ID.find(target.chapterUrl)?.groupValues?.get(1)?.toLongOrNull()?.let { return it }

        val mangaSlug = SLUG_FULL.find(target.mangaUrl)?.value
            ?: target.mangaUrl.trim('/').substringAfterLast('/').ifBlank { return null }
        val volume = VOLUME_PATH.find(target.chapterUrl)?.groupValues?.get(1)
            ?: VOLUME_QUERY.find(target.chapterUrl)?.groupValues?.get(1)
        val number = NUMBER_PATH.find(target.chapterUrl)?.groupValues?.get(1)
            ?: NUMBER_QUERY.find(target.chapterUrl)?.groupValues?.get(1)
            ?: return null
        val requestedBranchId = BRANCH_QUERY.find(target.chapterUrl)
            ?.groupValues
            ?.get(1)
            ?.toLongOrNull()

        // LibGroup can be configured to use one of several API mirrors. Its client
        // injects the bearer token only for the currently configured API domain,
        // therefore try every official mirror until the authenticated chapter list
        // yields the requested chapter. Public comment requests can stay on apiHost.
        for (host in chapterApiHosts()) {
            val res = runCatching {
                http.get("$host/api/manga/$mangaSlug/chapters", headers(site))
            }.getOrNull() ?: continue
            if (!res.isSuccess) continue

            val list = runCatching {
                json.parseToJsonElement(res.body).jsonObject["data"]?.jsonArray
            }.getOrNull() ?: continue

            val chapterId = list.firstNotNullOfOrNull { el ->
                val o = el as? JsonObject ?: return@firstNotNullOfOrNull null
                val n = o["number"]?.str() ?: o["number"]?.int()?.toString()
                val v = o["volume"]?.str() ?: o["volume"]?.int()?.toString()
                val numberMatches = n != null && n.toFloatOrNull() == number.toFloatOrNull()
                val volumeMatches = volume == null || v == null || v.toFloatOrNull() == volume.toFloatOrNull()
                val branchMatches = requestedBranchId == null || chapterContainsBranch(o, requestedBranchId)
                if (numberMatches && volumeMatches && branchMatches) o["id"]?.long() else null
            }
            if (chapterId != null) return chapterId
        }
        return null
    }

    private fun chapterContainsBranch(chapter: JsonObject, requestedBranchId: Long): Boolean {
        chapter["branch_id"]?.long()?.let { if (it == requestedBranchId) return true }
        return (chapter["branches"] as? JsonArray)
            ?.any { branch ->
                (branch as? JsonObject)?.get("branch_id")?.long() == requestedBranchId
            }
            ?: false
    }

    private fun chapterApiHosts(): List<String> =
        if (apiHost == API_HOST) DEFAULT_API_HOSTS else listOf(apiHost)

    private fun headers(site: Site) = mapOf(
        "Site-Id" to site.siteId.toString(),
        "Referer" to "https://${site.domain}/",
        "Accept" to "application/json",
    )

    private companion object {
        const val API_HOST = "https://api.cdnlibs.org"
        val DEFAULT_API_HOSTS = listOf(
            API_HOST,
            "https://api2.mangalib.me",
            "https://hapi.hentaicdn.org",
            "https://api.imglib.info",
        )

        /** id тайтла в slug_url вида `7580--i-alone-level-up`. */
        val SLUG_ID = Regex("""(\d+)--""")
        val SLUG_FULL = Regex("""\d+--[a-z0-9-]+""", RegexOption.IGNORE_CASE)
        val CHAPTER_ID = Regex("""[?&]chapter_id=(\d+)""")
        val VOLUME_PATH = Regex("""/v([\d.]+)""")
        val NUMBER_PATH = Regex("""/c([\d.]+)""")
        val VOLUME_QUERY = Regex("""[?&]volume=([\d.]+)""")
        val NUMBER_QUERY = Regex("""[?&]number=([\d.]+)""")
        val BRANCH_QUERY = Regex("""[?&]branch_id=(\d+)""")

        /**
         * sourceId и Site-Id считаны из самих APK расширений
         * (`ExtensionGenerated.getId()` и поле, уходящее в заголовок `Site-Id`).
         */
        val DEFAULT_SITES = listOf(
            Site(6111047689498497237L, 1, "mangalib.me", "MangaLib"),
            Site(6425650164840473547L, 4, "hentailib.me", "HentaiLib"),
            Site(2730544188738947015L, 2, "slashlib.me", "SlashLib"),
        )
    }
}

// --- мелкие хелперы для JsonElement без @Serializable-классов -----------------
// Схема API семейства lib.social меняется без предупреждения, поэтому читаем
// поля вручную: неизвестное/переехавшее поле даёт null, а не падение парсера.

internal fun JsonElement.asArray(): JsonArray? = this as? JsonArray
internal fun JsonElement.str(): String? = (this as? JsonPrimitive)?.contentOrNull
internal fun JsonElement.long(): Long? = (this as? JsonPrimitive)?.longOrNull
internal fun JsonElement.int(): Int? = (this as? JsonPrimitive)?.intOrNull
internal fun JsonElement.bool(): Boolean? =
    (this as? JsonPrimitive)?.contentOrNull?.let { it == "true" || it == "1" }
