package eu.kanade.presentation.util

import android.content.Context
import eu.kanade.tachiyomi.network.HttpException
import eu.kanade.tachiyomi.util.system.isOnline
import kotlinx.serialization.SerializationException
import tachiyomi.data.source.NoResultsException
import tachiyomi.domain.source.model.SourceNotInstalledException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.SSLException

/**
 * What went wrong with a source, in words a reader understands, plus whether updating the extension or
 * switching to another edition is likely to help.
 */
data class SourceProblem(
    val kind: Kind,
    val title: String,
    val hint: String,
) {
    enum class Kind {
        Offline,
        Timeout,
        Unreachable,

        /** Anti-bot protection or HTTP 401/403/429. */
        Blocked,
        NotFound,
        ServerDown,

        /** The site changed and the extension can no longer parse it. */
        SourceChanged,

        /** The extension itself reported a failure that is not a network problem. */
        SourceError,
        Other,
        ;

        /** Problems on the source side, where an extension update or another source can help. */
        val isSourceSide: Boolean
            get() = this == Blocked || this == NotFound || this == ServerDown || this == SourceChanged ||
                this == SourceError

        val extensionUpdateMayHelp: Boolean
            get() = this == SourceChanged || this == Blocked || this == SourceError
    }
}

context(context: Context)
fun Throwable.toSourceProblem(): SourceProblem {
    val chain = generateSequence(this) { it.cause.takeIf { cause -> cause !== it } }.take(8).toList()
    val http = chain.firstNotNullOfOrNull { it as? HttpException }
    return when {
        chain.any { it is UnknownHostException || it is ConnectException } && !context.isOnline() ->
            SourceProblem(SourceProblem.Kind.Offline, "Нет интернета", "Проверьте подключение и повторите.")
        chain.any { it is SocketTimeoutException || it is InterruptedIOException } ->
            SourceProblem(SourceProblem.Kind.Timeout, "Сайт не ответил вовремя", "Повторите чуть позже.")
        chain.any { it::class.simpleName == "CloudflareBypassException" } ->
            SourceProblem(
                SourceProblem.Kind.Blocked,
                "Сайт закрылся защитой от ботов",
                "Откройте его в WebView, пройдите проверку и вернитесь.",
            )
        http != null -> when (http.code) {
            401, 403, 429 -> SourceProblem(
                SourceProblem.Kind.Blocked,
                "Сайт не пускает приложение (ошибка ${http.code})",
                "Часто чинится обновлением расширения или входом через WebView.",
            )
            404, 410 -> SourceProblem(
                SourceProblem.Kind.NotFound,
                "На сайте этого больше нет",
                "Тайтл или глава удалены — попробуйте другой источник.",
            )
            in 500..599 -> SourceProblem(
                SourceProblem.Kind.ServerDown,
                "Сайт сейчас не работает (ошибка ${http.code})",
                "Подождите или читайте в другом источнике.",
            )
            else -> SourceProblem(SourceProblem.Kind.Other, "Ошибка сайта ${http.code}", "Проверьте сайт в WebView.")
        }
        chain.any { it is UnknownHostException || it is ConnectException || it is SSLException } ->
            SourceProblem(
                SourceProblem.Kind.Unreachable,
                "Сайт недоступен",
                "Возможно, сменился адрес или он заблокирован в вашей сети.",
            )
        chain.any { it.looksLikeParsingFailure() } ->
            SourceProblem(
                SourceProblem.Kind.SourceChanged,
                "Сайт изменился, расширение его не понимает",
                "Обычно помогает обновление расширения.",
            )
        isExtensionReportedFailure() ->
            SourceProblem(
                SourceProblem.Kind.SourceError,
                "Источник не смог загрузить данные",
                message?.takeIf { it.isNotBlank() }?.let { "Расширение сообщает: $it" }
                    ?: "Попробуйте обновить расширение или открыть другой источник.",
            )
        else -> SourceProblem(SourceProblem.Kind.Other, plainMessage(), "")
    }
}

/** Plain exceptions thrown by extension code itself, e.g. `Exception("Could not find ...")`. */
private fun Throwable.isExtensionReportedFailure(): Boolean =
    this !is IOException &&
        this !is NoResultsException &&
        this !is SourceNotInstalledException &&
        (this::class == Exception::class || this is IllegalStateException || this is IllegalArgumentException)

private fun Throwable.looksLikeParsingFailure(): Boolean = when (this) {
    is SerializationException,
    is NullPointerException,
    is IndexOutOfBoundsException,
    is NoSuchElementException,
    is NumberFormatException,
    is ClassCastException,
    -> true
    else -> this::class.qualifiedName.orEmpty().let { name ->
        // Matched by name too: an extension may ship its own copy of these classes.
        name.startsWith("org.jsoup.") || name.startsWith("kotlinx.serialization.")
    }
}

/**
 * Last source-side failure per source, shared between the reader and manga details so a chapter that failed in
 * the reader can be explained on the details screen the user lands on.
 */
object SourceHealth {
    private data class Entry(val problem: SourceProblem, val atMillis: Long)

    private val problems = ConcurrentHashMap<Long, Entry>()

    fun report(sourceId: Long, problem: SourceProblem) {
        if (problem.kind.isSourceSide) problems[sourceId] = Entry(problem, System.currentTimeMillis())
    }

    fun recent(sourceId: Long): SourceProblem? {
        val entry = problems[sourceId] ?: return null
        if (System.currentTimeMillis() - entry.atMillis > RECENT_MILLIS) {
            problems.remove(sourceId)
            return null
        }
        return entry.problem
    }

    fun clear(sourceId: Long) {
        problems.remove(sourceId)
    }

    private const val RECENT_MILLIS = 30L * 60L * 1000L
}
