package mangalka.comments.model

/**
 * К чему относится запрос комментариев.
 *
 * Собирается в app-слое из Mihon-сущностей ([eu.kanade.tachiyomi.source.model.SManga] /
 * `SChapter`), но сам класс о них не знает — ядро остаётся чистым JVM-модулем.
 */
sealed interface CommentTarget {

    /** Идентификатор источника Mihon (`Source.id`). Нужен для выбора адаптера. */
    val sourceId: Long

    /** Относительный url тайтла в источнике, как его отдаёт расширение. */
    val mangaUrl: String

    /** Абсолютный url страницы, если адаптеру нужно скрести HTML. */
    val absoluteUrl: String?

    data class Manga(
        override val sourceId: Long,
        override val mangaUrl: String,
        override val absoluteUrl: String? = null,
        val title: String? = null,
    ) : CommentTarget

    data class Chapter(
        override val sourceId: Long,
        override val mangaUrl: String,
        val chapterUrl: String,
        override val absoluteUrl: String? = null,
        val chapterName: String? = null,
        val chapterNumber: Float? = null,
    ) : CommentTarget
}

/** Страница треда: список корневых комментариев + признак наличия следующей. */
data class CommentPage(
    val comments: List<UnifiedComment>,
    val page: Int,
    val hasNextPage: Boolean,
    /** Общее число комментариев в треде, если источник его сообщает. */
    val totalCount: Int? = null,
    /** Какой адаптер отработал — показывается в UI как «источник комментариев». */
    val providerId: String,
) {
    companion object {
        fun empty(providerId: String, page: Int = 1) =
            CommentPage(emptyList(), page, hasNextPage = false, totalCount = 0, providerId = providerId)
    }
}

/** Порядок сортировки, который просит UI. Адаптер применяет его, если умеет. */
enum class CommentSort { NEWEST, OLDEST, TOP }
