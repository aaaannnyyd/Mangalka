package tachiyomi.data.release

import android.os.Build
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.CacheControl
import okhttp3.Headers
import tachiyomi.domain.release.interactor.GetApplicationRelease
import tachiyomi.domain.release.model.Release
import tachiyomi.domain.release.service.ReleaseService

/**
 * Releases come from GitHub Releases of [GetApplicationRelease.Arguments.repository] (`owner/name`).
 * Each release carries `Mangalka-<version>-<abi>.apk` assets; the one matching the device ABI is
 * downloaded and installed in-app, with the universal APK as a fallback.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ReleaseServiceImpl(
    private val networkService: NetworkHelper,
) : ReleaseService {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun latest(arguments: GetApplicationRelease.Arguments): Release? {
        val body = networkService.client
            .newCall(
                GET(
                    "https://api.github.com/repos/${arguments.repository}/releases/latest",
                    Headers.headersOf("Accept", "application/vnd.github+json"),
                    CacheControl.FORCE_NETWORK,
                ),
            )
            .awaitSuccess()
            .use { it.body.string() }
        val release = json.decodeFromString<GithubRelease>(body)
        val apk = GithubApkPicker.pick(release.assets.map { it.name to it.downloadUrl }, Build.SUPPORTED_ABIS.toList())
        return Release(
            version = release.tagName,
            info = release.body.orEmpty().trim(),
            releaseLink = release.htmlUrl,
            downloadLink = apk ?: release.htmlUrl,
        )
    }

    @Serializable
    private data class GithubRelease(
        @SerialName("tag_name") val tagName: String,
        @SerialName("html_url") val htmlUrl: String,
        val body: String? = null,
        val assets: List<Asset> = emptyList(),
    )

    @Serializable
    private data class Asset(
        val name: String,
        @SerialName("browser_download_url") val downloadUrl: String,
    )
}

internal object GithubApkPicker {

    /** Prefers the first device ABI that has its own APK, then the universal build, then any APK. */
    fun pick(assets: List<Pair<String, String>>, deviceAbis: List<String>): String? {
        val apks = assets.filter { (name, _) -> name.endsWith(".apk", ignoreCase = true) }
        return deviceAbis.firstNotNullOfOrNull { abi -> apks.firstOrNull { (name, _) -> "-$abi." in name }?.second }
            ?: apks.firstOrNull { (name, _) -> "universal" in name }?.second
            ?: apks.firstOrNull()?.second
    }
}
