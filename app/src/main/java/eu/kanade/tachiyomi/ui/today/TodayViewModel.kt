package eu.kanade.tachiyomi.ui.today

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import eu.kanade.domain.today.interactor.GetPinnedSourceRecommendations
import eu.kanade.domain.today.interactor.PinnedSourceRecommendation
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.data.download.DownloadManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import logcat.LogPriority
import mangalka.recommendations.GetHistoryRecommendations
import mangalka.recommendations.HistoryRecommendation
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.interactor.UpdateChapter
import tachiyomi.domain.chapter.model.ChapterUpdate
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.today.interactor.GetTodayData
import tachiyomi.domain.today.model.TodayData

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class TodayViewModel(
    private val getTodayData: GetTodayData,
    private val updateChapter: UpdateChapter,
    private val chapterCache: ChapterCache,
    private val downloadManager: DownloadManager,
    private val sourceManager: SourceManager,
    private val getPinnedSourceRecommendations: GetPinnedSourceRecommendations,
    private val getHistoryRecommendations: GetHistoryRecommendations,
) : ViewModel() {

    private val bookmarkOverrides = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    private val refreshRequest = MutableStateFlow(0)

    val state: StateFlow<State> = refreshRequest
        .flatMapLatest { request ->
            val todayData = getTodayData.subscribe(System.currentTimeMillis() - UPDATES_WINDOW_MILLIS)
                .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), replay = 1)
            combine(
                todayData,
                bookmarkOverrides,
                flow {
                    // Do not make Today wait on slow network sources: render history/updates first,
                    // then populate the recommendation rail when pinned sources return.
                    emit(emptyList<PinnedSourceRecommendation>())
                    emit(getPinnedSourceRecommendations.await(forceRefresh = request > 0))
                },
                todayData
                    .map { data -> data.history.distinctBy { it.mangaId }.map { it.title } }
                    .distinctUntilChanged()
                    .transformLatest { seeds ->
                        emit(emptyList())
                        emit(getHistoryRecommendations.await(seeds, forceRefresh = request > 0))
                    },
            ) { data, overrides, recommendations, similar ->
                val continueReading = data.continueReading
                State(
                    isLoading = false,
                    data = data,
                    totalPages = continueReading?.let(::findKnownPageCount),
                    bookmarkOverride = continueReading?.chapter?.id?.let(overrides::get),
                    recommendations = recommendations,
                    similar = similar,
                )
            }
                .onStart { emit(State()) }
                .catch { error ->
                    logcat(LogPriority.ERROR, error)
                    emit(State(isLoading = false, hasError = true))
                }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), State())

    fun setBookmark(chapterId: Long, bookmarked: Boolean) {
        bookmarkOverrides.update { it + (chapterId to bookmarked) }
        viewModelScope.launchIO {
            updateChapter.await(ChapterUpdate(id = chapterId, bookmark = bookmarked))
        }
    }

    fun retry() {
        refreshRequest.update { it + 1 }
    }

    private fun findKnownPageCount(data: tachiyomi.domain.today.model.ContinueReading): Int? {
        val cachedCount = runCatching { chapterCache.getPageListFromCache(data.chapter).size }
            .getOrNull()
            ?.takeIf { it > 0 }
        if (cachedCount != null) return cachedCount

        val chapter = data.chapter
        val manga = data.manga
        val isDownloaded = downloadManager.isChapterDownloaded(
            chapter.name,
            chapter.scanlator,
            chapter.url,
            manga.title,
            manga.source,
        )
        if (!isDownloaded) return null
        return runCatching {
            downloadManager.buildPageList(sourceManager.getOrStub(manga.source), manga, chapter).size
        }.getOrNull()?.takeIf { it > 0 }
    }

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val data: TodayData? = null,
        val totalPages: Int? = null,
        val bookmarkOverride: Boolean? = null,
        val recommendations: List<PinnedSourceRecommendation> = emptyList(),
        val similar: List<HistoryRecommendation> = emptyList(),
        val hasError: Boolean = false,
    )

    private companion object {
        const val UPDATES_WINDOW_MILLIS = 90L * 24L * 60L * 60L * 1000L
    }
}
