package mangalka.comments

import android.content.Context
import dev.icerock.moko.resources.StringResource
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.serialization.SerializationException
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
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
            localized(MR.strings.ml_g1_comment_error_unauthorized),
            technicalDetail = "HTTP 401 · $url",
        )
        403 -> CommentDiagnostic(
            CommentFailureKind.FORBIDDEN,
            localized(MR.strings.ml_g1_comment_error_forbidden),
            technicalDetail = "HTTP 403 · $url",
        )
        404 -> CommentDiagnostic(
            CommentFailureKind.NOT_FOUND,
            localized(MR.strings.ml_g1_comment_error_not_found),
            technicalDetail = "HTTP 404 · $url",
        )
        429 -> CommentDiagnostic(
            CommentFailureKind.RATE_LIMITED,
            localized(MR.strings.ml_g1_comment_error_rate_limited),
            technicalDetail = "HTTP 429 · $url",
        )
        else -> CommentDiagnostic(
            CommentFailureKind.HTTP,
            localized(MR.strings.ml_g1_comment_error_http, code),
            technicalDetail = "HTTP $code · $url",
        )
    }
    is CommentSourceUnavailableException -> when {
        httpCode != null -> CommentHttpException(httpCode, endpoint.orEmpty()).toCommentDiagnostic()
        cause != null -> cause!!.toCommentDiagnostic()
        else -> CommentDiagnostic(
            CommentFailureKind.UNKNOWN,
            localized(MR.strings.ml_g1_comment_error_unavailable),
            technicalDetail = message,
        )
    }
    is CommentTargetResolutionException -> CommentDiagnostic(
        CommentFailureKind.TARGET_RESOLUTION,
        localized(MR.strings.ml_g1_comment_error_target_resolution),
        technicalDetail = "$providerId · ${message.orEmpty()}",
    )
    is UnexpectedCommentSchemaException -> CommentDiagnostic(
        CommentFailureKind.SCHEMA_CHANGED,
        localized(MR.strings.ml_g1_comment_error_schema_changed),
        technicalDetail = "$providerId · ${message.orEmpty()}",
    )
    is NoCommentAdapterException -> CommentDiagnostic(
        CommentFailureKind.UNSUPPORTED,
        localized(MR.strings.ml_g1_comment_error_unsupported),
        technicalDetail = message,
        retryable = false,
    )
    is TimeoutCancellationException,
    is SocketTimeoutException,
    -> CommentDiagnostic(
        CommentFailureKind.TIMEOUT,
        localized(MR.strings.ml_g1_comment_error_timeout),
        technicalDetail = message,
    )
    is UnknownHostException -> CommentDiagnostic(
        CommentFailureKind.NETWORK,
        localized(MR.strings.ml_g1_comment_error_unknown_host),
        technicalDetail = message,
    )
    is SerializationException -> CommentDiagnostic(
        CommentFailureKind.SCHEMA_CHANGED,
        localized(MR.strings.ml_g1_comment_error_serialization),
        technicalDetail = message,
    )
    is IOException -> CommentDiagnostic(
        CommentFailureKind.NETWORK,
        localized(MR.strings.ml_g1_comment_error_network),
        technicalDetail = message,
    )
    else -> CommentDiagnostic(
        CommentFailureKind.UNKNOWN,
        message?.takeIf { it.isNotBlank() } ?: localized(MR.strings.ml_g1_comment_error_unknown),
        technicalDetail = this::class.qualifiedName,
    )
}

/** Diagnostics are built in ViewModels without a Context at hand, so resolve via the app Context. */
private fun localized(resource: StringResource, vararg args: Any): String =
    Injekt.get<Context>().stringResource(resource, *args)
