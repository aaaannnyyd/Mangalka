package eu.kanade.domain.today.interactor

import dev.zacsweers.metro.Inject
import eu.kanade.domain.source.interactor.GetEnabledSources
import eu.kanade.tachiyomi.source.CatalogueSource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import mihon.domain.manga.model.toDomainManga
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.model.Pin
import tachiyomi.domain.source.service.SourceManager
import kotlin.time.Clock

data class PinnedSourceRecommendation(
    val manga: Manga,
    val sourceName: String,
)

@Inject
class GetPinnedSourceRecommendations(
    private val getEnabledSources: GetEnabledSources,
    private val sourceManager: SourceManager,
    private val networkToLocalManga: NetworkToLocalManga,
) {
    private val cacheMutex = Mutex()
    private var cachedSourceIds: Set<Long> = emptySet()
    private var cachedAtMillis: Long = 0L
    private var cached: List<PinnedSourceRecommendation> = emptyList()

    suspend fun await(forceRefresh: Boolean = false): List<PinnedSourceRecommendation> = cacheMutex.withLock {
        val pinned = getEnabledSources.subscribe().first()
            // The "last used" duplicate has its pin flags flipped (Pins.minus is xor), so it is not a pin.
            .filter { !it.isUsedLast && Pin.Actual in it.pin }
            .distinctBy { it.id }
        val sourceIds = pinned.mapTo(linkedSetOf()) { it.id }
        val now = Clock.System.now().toEpochMilliseconds()
        val cacheFresh = sourceIds == cachedSourceIds && now - cachedAtMillis < CACHE_TTL_MILLIS
        if (!forceRefresh && cacheFresh) return@withLock cached
        if (sourceIds.isEmpty()) {
            cachedSourceIds = emptySet()
            cachedAtMillis = now
            cached = emptyList()
            return@withLock emptyList()
        }

        val perSource = supervisorScope {
            pinned.map { domainSource ->
                async {
                    val source = sourceManager.get(domainSource.id) as? CatalogueSource
                        ?: return@async emptyList()
                    withTimeoutOrNull(SOURCE_TIMEOUT_MILLIS) {
                        val popular =
                            async { runCatching { source.getPopularManga(1).mangas }.getOrDefault(emptyList()) }
                        val latest = async {
                            if (source.supportsLatest) {
                                runCatching { source.getLatestUpdates(1).mangas }.getOrDefault(emptyList())
                            } else {
                                emptyList()
                            }
                        }
                        val remote = (popular.await() + latest.await())
                            .distinctBy { it.url }
                            .take(PER_SOURCE_LIMIT)
                            .map { it.toDomainManga(source.id) }
                        if (remote.isEmpty()) return@withTimeoutOrNull emptyList()
                        networkToLocalManga(remote)
                            .filterNot { it.favorite }
                            .map { PinnedSourceRecommendation(it, source.name) }
                    }.orEmpty()
                }
            }.awaitAll()
        }

        val seenTitles = hashSetOf<String>()
        val result = buildList {
            val maxSize = perSource.maxOfOrNull { it.size } ?: 0
            for (index in 0 until maxSize) {
                for (sourceItems in perSource) {
                    val item = sourceItems.getOrNull(index) ?: continue
                    val key = item.manga.title
                        .lowercase()
                        .replace(NON_ALPHANUMERIC, "")
                    if (key.isNotBlank() && seenTitles.add(key)) add(item)
                    if (size >= TOTAL_LIMIT) break
                }
                if (size >= TOTAL_LIMIT) break
            }
        }

        cachedSourceIds = sourceIds
        cachedAtMillis = now
        cached = result
        result
    }

    private companion object {
        const val PER_SOURCE_LIMIT = 8
        const val TOTAL_LIMIT = 18
        const val SOURCE_TIMEOUT_MILLIS = 8_000L
        const val CACHE_TTL_MILLIS = 20L * 60L * 1000L
        val NON_ALPHANUMERIC = Regex("[^\\p{L}\\p{N}]+")
    }
}
