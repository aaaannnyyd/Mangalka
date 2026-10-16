package tachiyomi.data.release

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import kotlinx.coroutines.CancellationException
import okhttp3.CacheControl
import tachiyomi.domain.release.interactor.GetApplicationRelease
import tachiyomi.domain.release.model.Release
import tachiyomi.domain.release.service.ReleaseService

/**
 * Releases are published to a public Telegram channel (see `work/tg-release.ps1`):
 * one post per APK, caption starting with `#release mangalka <versionName> (<versionCode>) <flavor>`.
 *
 * The app reads the channel's public web preview (`https://t.me/s/<channel>`), so no bot token
 * ships with the app. [GetApplicationRelease.Arguments.repository] carries the channel name.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ReleaseServiceImpl(
    private val networkService: NetworkHelper,
) : ReleaseService {

    override suspend fun latest(arguments: GetApplicationRelease.Arguments): Release? {
        val channel = arguments.repository
        // The channel is shared with other apps and the preview only shows ~20 recent posts, so
        // the tag search is needed for older posts. Search indexing lags behind new posts though,
        // so both pages are read and the highest versionCode wins.
        val urls = listOf(
            "https://t.me/s/$channel",
            "https://t.me/s/$channel?q=%23release+${TelegramReleaseParser.APP_ID}",
        )
        var lastError: Exception? = null
        val releases = urls.flatMap { url ->
            try {
                val html = networkService.client
                    .newCall(GET(url, cache = CacheControl.FORCE_NETWORK))
                    .awaitSuccess()
                    .use { it.body.string() }
                TelegramReleaseParser.parse(html, channel)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                emptyList()
            }
        }
        if (releases.isEmpty()) lastError?.let { throw it }
        return releases.maxByOrNull { it.versionCode }?.release
    }
}

internal object TelegramReleaseParser {

    const val APP_ID = "mangalka"

    data class Parsed(val versionCode: Int, val release: Release)

    private val POST = Regex("""data-post="([^"/]+)/(\d+)"([\s\S]*?)(?=data-post="|\z)""")
    private val TEXT = Regex("""<div class="tgme_widget_message_text[^"]*"[^>]*>([\s\S]*?)</div>""")

    /** The machine line must open the caption; the pinned rules post only quotes examples later. */
    private val HEADER = Regex(
        """\A\s*#release\s+$APP_ID\s+([0-9][0-9A-Za-z.\-]*)\s+\((\d+)\)[^\n]*""",
        RegexOption.IGNORE_CASE,
    )
    private val APK_LINK = Regex("""https?://\S+?\.apk\b""")

    fun parse(html: String, channel: String): List<Parsed> =
        POST.findAll(html).mapNotNull { match ->
            val body = TEXT.find(match.groupValues[3])?.groupValues?.get(1) ?: return@mapNotNull null
            val text = htmlToText(body)
            val header = HEADER.find(text) ?: return@mapNotNull null
            val postChannel = match.groupValues[1].ifBlank { channel }
            val postId = match.groupValues[2]
            val postUrl = "https://t.me/$postChannel/$postId"
            val notes = text.substring(header.range.last + 1)
                .lines()
                .map(String::trim)
                .dropWhile(String::isEmpty)
                // Second line is the human tagline «Mangalka 1.0.1 — …»; keep only change items.
                .let { lines -> if (lines.firstOrNull()?.startsWith("—") == false) lines.drop(1) else lines }
                .joinToString("\n")
                .trim()
            Parsed(
                versionCode = header.groupValues[2].toInt(),
                release = Release(
                    version = header.groupValues[1],
                    info = notes,
                    releaseLink = postUrl,
                    // A direct mirror link in the caption is downloaded in-app; otherwise the
                    // update screen opens the post in Telegram, where the APK is attached.
                    downloadLink = APK_LINK.find(text)?.value ?: postUrl,
                ),
            )
        }.toList()

    private fun htmlToText(s: String): String = s
        .replace(Regex("<br\\s*/?>"), "\n")
        .replace(Regex("<[^>]+>"), "")
        .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'")
        .replace("&nbsp;", " ").replace("&amp;", "&")
        .trim()
}
