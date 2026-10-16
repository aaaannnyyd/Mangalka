package mangalka.recommendations

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import logcat.LogPriority
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.manga.repository.MangaRepository
import kotlin.time.Clock

data class HistoryRecommendation(
    val shikimoriId: Long,
    val title: String,
    val originalTitle: String,
    val coverUrl: String?,
    val score: String?,
    /** The title from the user's history this one is similar to. */
    val because: String,
)

/**
 * "Similar to what you read": matches recently read titles on Shikimori, which knows Russian and original names
 * across all sources, and ranks its similar-title lists. Titles already in the library are left out.
 */
@Inject
@SingleIn(AppScope::class)
class GetHistoryRecommendations(
    private val networkHelper: NetworkHelper,
    private val mangaRepository: MangaRepository,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val headers = Headers.headersOf("User-Agent", "Mangalka")
    private val mutex = Mutex()
    private var cachedSeeds: List<String> = emptyList()
    private var cachedAt = 0L
    private var cached: List<HistoryRecommendation> = emptyList()

    suspend fun await(recentTitles: List<String>, forceRefresh: Boolean = false): List<HistoryRecommendation> =
        mutex.withLock {
            val seeds = recentTitles.map(String::trim).filter(String::isNotEmpty).distinct().take(MAX_SEEDS)
            val now = Clock.System.now().toEpochMilliseconds()
            if (!forceRefresh && seeds == cachedSeeds && now - cachedAt < CACHE_TTL_MILLIS) return@withLock cached
            if (seeds.isEmpty()) return@withLock emptyList()

            val result = try {
                withIOContext { compute(seeds) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                logcat(LogPriority.WARN, e) { "History recommendations unavailable" }
                return@withLock cached
            }
            cachedSeeds = seeds
            cachedAt = now
            cached = result
            result
        }

    private suspend fun compute(seeds: List<String>): List<HistoryRecommendation> {
        val library = mangaRepository.getFavorites().mapTo(hashSetOf()) { it.title.normalized() }
        val seedIds = hashSetOf<Long>()
        val scores = linkedMapOf<Long, Double>()
        val items = hashMapOf<Long, Pair<ShikimoriManga, String>>()

        for (seed in seeds) {
            val match = search(seed).firstOrNull { it.matches(seed) } ?: continue
            seedIds += match.id
            delay(REQUEST_SPACING_MILLIS)
            similar(match.id).take(SIMILAR_PER_SEED).forEachIndexed { index, item ->
                scores[item.id] = (scores[item.id] ?: 0.0) + 1.0 / (1 + index * 0.15)
                items.putIfAbsent(item.id, item to seed)
            }
            delay(REQUEST_SPACING_MILLIS)
        }

        return scores.entries
            .asSequence()
            .filter { (id, _) -> id !in seedIds }
            .mapNotNull { (id, score) -> items[id]?.let { Triple(it.first, it.second, score) } }
            .filterNot { (item) -> item.russian.normalized() in library || item.name.normalized() in library }
            .sortedWith(
                compareByDescending<Triple<ShikimoriManga, String, Double>> { it.third }
                    .thenByDescending { it.first.score?.toDoubleOrNull() ?: 0.0 },
            )
            .take(TOTAL_LIMIT)
            .map { (item, because) ->
                HistoryRecommendation(
                    shikimoriId = item.id,
                    title = item.russian?.takeIf { it.isNotBlank() } ?: item.name.orEmpty(),
                    originalTitle = item.name.orEmpty(),
                    coverUrl = item.image?.original
                        ?.takeUnless { "missing" in it }
                        ?.let { if (it.startsWith("http")) it else "$BASE_URL$it" },
                    score = item.score?.takeUnless { it == "0.0" },
                    because = because,
                )
            }
            .toList()
    }

    private suspend fun search(title: String): List<ShikimoriManga> {
        val url = "$BASE_URL/api/mangas".toHttpUrl().newBuilder()
            .addQueryParameter("search", title)
            .addQueryParameter("limit", "5")
            .build()
        return get(url.toString())
    }

    private suspend fun similar(id: Long): List<ShikimoriManga> = get("$BASE_URL/api/mangas/$id/similar")

    private suspend fun get(url: String): List<ShikimoriManga> {
        val body = networkHelper.client.newCall(GET(url, headers)).awaitSuccess().use { it.body.string() }
        return json.decodeFromString(body)
    }

    private fun ShikimoriManga.matches(seed: String): Boolean {
        val target = seed.normalized()
        return listOfNotNull(russian, name).any { candidate ->
            val normalized = candidate.normalized()
            normalized == target ||
                (
                    normalized.length >= MIN_PARTIAL_MATCH && target.length >= MIN_PARTIAL_MATCH &&
                        (normalized in target || target in normalized)
                    )
        }
    }

    private fun String?.normalized(): String = orEmpty().lowercase().replace(NON_ALPHANUMERIC, "")

    @Serializable
    private data class ShikimoriManga(
        val id: Long,
        val name: String? = null,
        val russian: String? = null,
        val image: Image? = null,
        val score: String? = null,
    ) {
        @Serializable
        data class Image(val original: String? = null)
    }

    private companion object {
        const val BASE_URL = "https://shikimori.io"
        const val MAX_SEEDS = 6
        const val SIMILAR_PER_SEED = 10
        const val TOTAL_LIMIT = 18
        const val MIN_PARTIAL_MATCH = 6
        const val REQUEST_SPACING_MILLIS = 250L
        const val CACHE_TTL_MILLIS = 6L * 60L * 60L * 1000L
        val NON_ALPHANUMERIC = Regex("[^\\p{L}\\p{N}]+")
    }
}
