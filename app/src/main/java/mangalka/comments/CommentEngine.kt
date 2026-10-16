package mangalka.comments

import mangalka.comments.model.CommentPage
import mangalka.comments.model.CommentSort
import mangalka.comments.model.CommentTarget

/**
 * Точка входа слоя комментариев Mangalka.
 *
 * ```
 * Extension (не изменён)
 *        |  SManga.url / SChapter.url / Source.id / Source.baseUrl
 *        v
 * CommentEngine  --resolve-->  CommentAdapter (custom | auto)
 *        v
 * Unified Comment Model (дерево)
 *        v
 * Mangalka UI
 * ```
 *
 * Порядок разрешения адаптера:
 *  1. кастомные адаптеры в порядке регистрации — точное совпадение по `Source.id`
 *     или по домену `baseUrl`;
 *  2. [fallback] (auto-parser), если он задан и включён.
 *
 * Никакой адаптер не требует изменений в APK расширения.
 */
class CommentEngine(
    private val adapters: List<CommentAdapter>,
    private val http: CommentHttp,
    private val fallback: CommentAdapter? = null,
) {

    /** Есть ли вообще чем читать комментарии для этого источника. */
    fun isSupported(sourceId: Long, baseUrl: String?): Boolean =
        resolve(sourceId, baseUrl) != null

    fun adapterFor(sourceId: Long, baseUrl: String?): CommentAdapter? = resolve(sourceId, baseUrl)

    /**
     * Загружает страницу треда.
     *
     * Ошибки сети/парсинга НЕ пробрасываются наружу как исключения адаптера:
     * страница тайтла не должна падать из-за комментариев. Вместо этого
     * возвращается [Result], и вызывающий сам решает, показать ошибку или скрыть блок.
     */
    suspend fun fetch(
        target: CommentTarget,
        page: Int = 1,
        sort: CommentSort = CommentSort.NEWEST,
    ): Result<CommentPage> {
        val adapter = resolve(target.sourceId, target.absoluteUrl)
            ?: return Result.failure(NoCommentAdapterException(target.sourceId))

        if (target is CommentTarget.Chapter && !adapter.supportsChapterComments) {
            return Result.success(CommentPage.empty(adapter.id, page))
        }

        return runCatching { adapter.fetch(target, page, sort, http) }
    }

    private fun resolve(sourceId: Long, baseUrl: String?): CommentAdapter? =
        adapters.firstOrNull { it.supports(sourceId, baseUrl) }
            ?: fallback?.takeIf { it.supports(sourceId, baseUrl) }
}

class NoCommentAdapterException(sourceId: Long) :
    Exception("No comment adapter for source $sourceId")
