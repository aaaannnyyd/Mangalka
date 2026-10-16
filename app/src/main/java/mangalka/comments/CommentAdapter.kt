package mangalka.comments

import mangalka.comments.model.CommentPage
import mangalka.comments.model.CommentSort
import mangalka.comments.model.CommentTarget

/**
 * Минимальный HTTP-порт.
 *
 * Ядро намеренно НЕ зависит от OkHttp: в приложении подставляется реализация
 * поверх `Source.client` (что даёт нам бесплатно Cloudflare-интерцептор,
 * User-Agent и cookie-jar расширения), а в тестах — фикстуры с диска.
 */
interface CommentHttp {
    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): HttpText
}

data class HttpText(val code: Int, val body: String, val finalUrl: String) {
    val isSuccess: Boolean get() = code in 200..299
}

class CommentHttpException(val code: Int, val url: String) :
    Exception("HTTP $code for $url")

/**
 * Адаптер комментариев одного сайта (или семейства сайтов).
 *
 * Реализации не трогают APK расширений и не требуют от них ничего:
 * они работают по [CommentTarget] + собственному знанию про сайт.
 */
interface CommentAdapter {

    /** Стабильный id для логов, настроек и отображения в UI. */
    val id: String

    /** Человекочитаемое имя. */
    val name: String

    /**
     * Может ли адаптер обслужить цель.
     *
     * @param sourceId  `Source.id` из Mihon
     * @param baseUrl   базовый url источника, если удалось получить
     */
    fun supports(sourceId: Long, baseUrl: String?): Boolean

    /** Поддерживает ли комментарии к отдельной главе. */
    val supportsChapterComments: Boolean get() = false

    /**
     * Отдаёт ли источник привязку комментария к конкретной странице главы.
     * Сейчас это гарантировано для LibGroup через `post_page`.
     */
    val supportsPageComments: Boolean get() = false

    suspend fun fetch(
        target: CommentTarget,
        page: Int,
        sort: CommentSort,
        http: CommentHttp,
    ): CommentPage
}
