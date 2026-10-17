package mangalka.comments.android

import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mangalka.comments.CommentAdapter
import mangalka.comments.CommentEngine
import mangalka.comments.CommentHttp
import mangalka.comments.HttpText
import mangalka.comments.adapter.AutoCommentAdapter
import mangalka.comments.adapter.ComixCommentAdapter
import mangalka.comments.adapter.HenChanCommentAdapter
import mangalka.comments.adapter.MangaDexCommentAdapter
import mangalka.comments.adapter.MangaLibCommentAdapter
import mangalka.comments.adapter.MangaShiCommentAdapter
import mangalka.comments.adapter.ReMangaCommentAdapter
import mangalka.comments.model.CommentPage
import mangalka.comments.model.CommentSort
import mangalka.comments.model.CommentTarget
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga

/**
 * Android-интеграция comment engine внутри app-модуля Mangalka.
 * Этот код компилируется и проверяется обычными app Gradle-задачами вместе с Mihon.
 *
 * Реализация [CommentHttp] поверх OkHttp-клиента самого источника.
 *
 * Почему клиент берётся у [HttpSource], а не создаётся свой: у расширения уже
 * настроены Cloudflare-интерцептор, User-Agent, rate limit и cookie-jar. Свой
 * клиент почти гарантированно упрётся в 403 на тех же сайтах, где расширение
 * работает. Расширение при этом не модифицируется — мы только переиспользуем
 * его сеть.
 */
class OkHttpCommentHttp(
    private val client: OkHttpClient,
    private val defaultHeaders: Headers = Headers.headersOf(),
) : CommentHttp {

    override suspend fun get(url: String, headers: Map<String, String>): HttpText =
        withContext(Dispatchers.IO) {
            val builder = Request.Builder().url(url).get()
            defaultHeaders.forEach { (k, v) -> builder.header(k, v) }
            headers.forEach { (k, v) -> builder.header(k, v) }

            client.newCall(builder.build()).await().use { response ->
                HttpText(
                    code = response.code,
                    body = response.body.string(),
                    finalUrl = response.request.url.toString(),
                )
            }
        }
}

/** Набор адаптеров по умолчанию. Порядок важен: auto — только как fallback. */
object CommentEngineDefaults {

    // Адаптеры живут столько же, сколько процесс: это сохраняет дешёвые runtime-кэши
    // (numeric manga id и cursor pagination Comix) между соседними запросами страниц.
    private val defaultAdapters: List<CommentAdapter> by lazy {
        listOf(
            MangaLibCommentAdapter(),
            ComixCommentAdapter(),
            ReMangaCommentAdapter(),
            MangaShiCommentAdapter(),
            HenChanCommentAdapter(),
            MangaDexCommentAdapter(),
            // сюда добавляются новые кастомные адаптеры
        )
    }
    private val defaultFallback: CommentAdapter by lazy { AutoCommentAdapter() }

    fun adapters(): List<CommentAdapter> = defaultAdapters

    fun fallback(): CommentAdapter = defaultFallback

    fun engineFor(source: Source): CommentEngine? {
        val http = source.commentHttp() ?: return null
        return CommentEngine(adapters(), http, fallback())
    }

    private fun Source.commentHttp(): CommentHttp? = when (this) {
        is HttpSource -> OkHttpCommentHttp(client, headers)
        else -> null // LocalSource и прочие оффлайн-источники комментариев не имеют
    }
}

/**
 * Единственная точка, из которой app-слой берёт комментарии.
 *
 * Заменяет мёртвый путь alpha15: там `MangaViewModel` делал
 * `source as? CommentableSource`, а реализаций этого интерфейса не существует —
 * ни одно стоковое расширение его не реализует, потому что интерфейс
 * объявлен внутри приложения, а не в `extensions-lib`.
 */
class GetComments(
    private val engineProvider: (Source) -> CommentEngine? = CommentEngineDefaults::engineFor,
) {

    fun isSupported(source: Source): Boolean {
        val engine = engineProvider(source) ?: return false
        return engine.isSupported(source.id, (source as? HttpSource)?.baseUrl)
    }

    /** Имя провайдера для подписи в UI: «Комментарии MangaLib». */
    fun providerName(source: Source): String? {
        val engine = engineProvider(source) ?: return null
        return engine.adapterFor(source.id, (source as? HttpSource)?.baseUrl)?.name
    }

    suspend fun forManga(
        source: Source,
        manga: Manga,
        page: Int = 1,
        sort: CommentSort = CommentSort.NEWEST,
    ): Result<CommentPage> {
        val engine = engineProvider(source) ?: return Result.failure(UnsupportedOperationException())
        return engine.fetch(
            CommentTarget.Manga(
                sourceId = source.id,
                mangaUrl = manga.url,
                absoluteUrl = source.absoluteUrl(manga.url),
                title = manga.title,
            ),
            page = page,
            sort = sort,
        )
    }

    suspend fun forChapter(
        source: Source,
        manga: Manga,
        chapter: Chapter,
        page: Int = 1,
        sort: CommentSort = CommentSort.NEWEST,
    ): Result<CommentPage> {
        val engine = engineProvider(source) ?: return Result.failure(UnsupportedOperationException())
        return engine.fetch(
            CommentTarget.Chapter(
                sourceId = source.id,
                mangaUrl = manga.url,
                chapterUrl = chapter.url,
                absoluteUrl = source.absoluteUrl(chapter.url),
                chapterName = chapter.name,
                chapterNumber = chapter.chapterNumber.toFloat(),
            ),
            page = page,
            sort = sort,
        )
    }

    fun supportsChapterComments(source: Source): Boolean {
        val engine = engineProvider(source) ?: return false
        return engine.adapterFor(source.id, (source as? HttpSource)?.baseUrl)?.supportsChapterComments == true
    }

    fun supportsPageComments(source: Source): Boolean {
        val engine = engineProvider(source) ?: return false
        return engine.adapterFor(source.id, (source as? HttpSource)?.baseUrl)?.supportsPageComments == true
    }

    private fun Source.absoluteUrl(path: String): String? {
        val base = (this as? HttpSource)?.baseUrl ?: return null
        return when {
            path.startsWith("http", ignoreCase = true) -> path
            path.startsWith("/") -> base.trimEnd('/') + path
            else -> base.trimEnd('/') + "/" + path
        }
    }
}

/** Состояние блока комментариев для экрана тайтла и для конца главы. */
sealed interface CommentsUiState {

    data object Unsupported : CommentsUiState

    data object Loading : CommentsUiState

    data class Error(val message: String?) : CommentsUiState

    data class Success(
        val page: CommentPage,
        val providerName: String?,
        val sort: CommentSort,
        val loadingMore: Boolean = false,
        /** id ветвей, свёрнутых пользователем. */
        val collapsed: Set<String> = emptySet(),
        /** id комментариев, у которых спойлер раскрыт. */
        val revealedSpoilers: Set<String> = emptySet(),
    ) : CommentsUiState {
        val isEmpty: Boolean get() = page.comments.isEmpty()
    }
}

@Suppress("unused")
private fun CatalogueSource.unusedMarker() = Unit
