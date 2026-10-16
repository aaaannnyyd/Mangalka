package mangalka.comments

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** User-facing classification for failures in the website comment layer. */
enum class CommentFailureKind {
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
    RATE_LIMITED,
    TIMEOUT,
    NETWORK,
    TARGET_RESOLUTION,
    SCHEMA_CHANGED,
    UNSUPPORTED,
    HTTP,
    UNKNOWN,
}

data class CommentDiagnostic(
    val kind: CommentFailureKind,
    val message: String,
    val technicalDetail: String? = null,
    val retryable: Boolean = true,
)

/** The adapter knows the source, but cannot map the Mihon entity to the site's numeric/API id. */
class CommentTargetResolutionException(
    val providerId: String,
    detail: String,
) : RuntimeException(detail)

/** The endpoint answered successfully, but its payload no longer has the expected shape. */
class UnexpectedCommentSchemaException(
    val providerId: String,
    detail: String,
) : RuntimeException(detail)

class CommentSourceUnavailableException(
    val providerName: String,
    val httpCode: Int? = null,
    val endpoint: String? = null,
    cause: Throwable? = null,
) : RuntimeException(
    buildString {
        append(providerName).append(" comments unavailable")
        httpCode?.let { append(" (HTTP ").append(it).append(')') }
    },
    cause,
)

fun Throwable.toCommentDiagnostic(): CommentDiagnostic = when (this) {
    is CommentHttpException -> when (code) {
        401 -> CommentDiagnostic(
            CommentFailureKind.UNAUTHORIZED,
            "Нужна авторизация в источнике. Откройте сайт через WebView источника и войдите в аккаунт.",
            technicalDetail = "HTTP 401 · $url",
        )
        403 -> CommentDiagnostic(
            CommentFailureKind.FORBIDDEN,
            "Источник отклонил запрос комментариев. Обычно помогает авторизация/WebView или обновление сессии Cloudflare.",
            technicalDetail = "HTTP 403 · $url",
        )
        404 -> CommentDiagnostic(
            CommentFailureKind.NOT_FOUND,
            "Сайт не нашёл endpoint или тред комментариев. Возможно, API источника изменился.",
            technicalDetail = "HTTP 404 · $url",
        )
        429 -> CommentDiagnostic(
            CommentFailureKind.RATE_LIMITED,
            "Источник временно ограничил частоту запросов. Повторите позже.",
            technicalDetail = "HTTP 429 · $url",
        )
        else -> CommentDiagnostic(
            CommentFailureKind.HTTP,
            "Сайт вернул ошибку HTTP $code при загрузке комментариев.",
            technicalDetail = "HTTP $code · $url",
        )
    }
    is CommentSourceUnavailableException -> when {
        httpCode != null -> CommentHttpException(httpCode, endpoint.orEmpty()).toCommentDiagnostic()
        cause != null -> cause!!.toCommentDiagnostic()
        else -> CommentDiagnostic(
            CommentFailureKind.UNKNOWN,
            "Источник комментариев временно недоступен.",
            technicalDetail = message,
        )
    }
    is CommentTargetResolutionException -> CommentDiagnostic(
        CommentFailureKind.TARGET_RESOLUTION,
        "Не удалось определить точный ID главы для комментариев. Обновите список глав; если у тайтла несколько переводов, проверьте выбранную ветку.",
        technicalDetail = "$providerId · ${message.orEmpty()}",
    )
    is UnexpectedCommentSchemaException -> CommentDiagnostic(
        CommentFailureKind.SCHEMA_CHANGED,
        "Ответ сайта изменился, и текущий адаптер комментариев больше не понимает его схему.",
        technicalDetail = "$providerId · ${message.orEmpty()}",
    )
    is NoCommentAdapterException -> CommentDiagnostic(
        CommentFailureKind.UNSUPPORTED,
        "Для этого источника пока нет адаптера комментариев.",
        technicalDetail = message,
        retryable = false,
    )
    is TimeoutCancellationException,
    is SocketTimeoutException,
    -> CommentDiagnostic(
        CommentFailureKind.TIMEOUT,
        "Источник слишком долго отвечает. Повторите загрузку комментариев.",
        technicalDetail = message,
    )
    is UnknownHostException -> CommentDiagnostic(
        CommentFailureKind.NETWORK,
        "Не удалось найти сервер комментариев. Проверьте сеть или доступность домена источника.",
        technicalDetail = message,
    )
    is SerializationException -> CommentDiagnostic(
        CommentFailureKind.SCHEMA_CHANGED,
        "Сайт изменил формат данных комментариев.",
        technicalDetail = message,
    )
    is IOException -> CommentDiagnostic(
        CommentFailureKind.NETWORK,
        "Сетевая ошибка при загрузке комментариев.",
        technicalDetail = message,
    )
    else -> CommentDiagnostic(
        CommentFailureKind.UNKNOWN,
        message?.takeIf { it.isNotBlank() } ?: "Неизвестная ошибка комментариев.",
        technicalDetail = this::class.qualifiedName,
    )
}
