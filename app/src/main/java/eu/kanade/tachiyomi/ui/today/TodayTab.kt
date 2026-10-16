package eu.kanade.tachiyomi.ui.today

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.domain.manga.model.sourceRating
import eu.kanade.presentation.today.TodayHeroUiModel
import eu.kanade.presentation.today.TodayRecommendationUiModel
import eu.kanade.presentation.today.TodayScreenContent
import eu.kanade.presentation.today.TodaySimilarUiModel
import eu.kanade.presentation.today.TodayUiModel
import eu.kanade.presentation.today.TodayUpdateUiModel
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.history.HistoryTab
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.updates.UpdatesTab
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.manga.model.asMangaCover
import tachiyomi.domain.today.model.ContinueReading
import tachiyomi.domain.updates.model.UpdatesWithRelations
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

data object TodayTab : Tab {

    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 0u,
            title = stringResource(MR.strings.relative_time_today),
            icon = rememberVectorPainter(Icons.Outlined.Home),
        )

    override suspend fun onReselect(navigator: Navigator) = Unit

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val tabNavigator = LocalTabNavigator.current
        val viewModel = metroViewModel<TodayViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val data = state.data

        TodayScreenContent(
            state = TodayUiModel(
                isLoading = state.isLoading,
                hasError = state.hasError,
                hero = data?.continueReading?.toHero(
                    context = context,
                    totalPages = state.totalPages,
                    bookmarkOverride = state.bookmarkOverride,
                ),
                recommendations = state.recommendations.map { recommendation ->
                    TodayRecommendationUiModel(
                        mangaId = recommendation.manga.id,
                        title = recommendation.manga.title,
                        sourceName = recommendation.sourceName,
                        coverData = recommendation.manga.asMangaCover(),
                        rating = recommendation.manga.sourceRating()?.displayValue,
                    )
                },
                similar = state.similar.map {
                    TodaySimilarUiModel(it.shikimoriId, it.title, it.because, it.coverUrl, it.score)
                },
                updates = data?.updates.orEmpty().distinctBy {
                    it.mangaId
                }.take(10).map(UpdatesWithRelations::toTodayItem),
                history = data?.history.orEmpty().distinctBy {
                    it.mangaId
                }.take(10).map { it.toTodayItem(context) },
            ),
            onSearch = { navigator.push(GlobalSearchScreen()) },
            onReadHero = { mangaId, chapterId ->
                context.startActivity(ReaderActivity.newIntent(context, mangaId, chapterId))
            },
            onToggleHeroBookmark = { _, chapterId, bookmarked ->
                viewModel.setBookmark(chapterId, bookmarked)
            },
            onOpenManga = { navigator.push(MangaScreen(it)) },
            onSearchTitle = { navigator.push(GlobalSearchScreen(it)) },
            onOpenUpdates = { tabNavigator.current = UpdatesTab },
            onOpenHistory = { tabNavigator.current = HistoryTab },
            onRetry = viewModel::retry,
        )

        LaunchedEffect(state.isLoading) {
            if (!state.isLoading) {
                (context as? MainActivity)?.ready = true
            }
        }
    }
}

private fun ContinueReading.toHero(
    context: Context,
    totalPages: Int?,
    bookmarkOverride: Boolean?,
): TodayHeroUiModel {
    val currentPage = (chapter.lastPageRead + 1L)
        .coerceAtLeast(1L)
        .let { page -> totalPages?.let { page.coerceAtMost(it.toLong()) } ?: page }
    val progress = when {
        totalPages != null && totalPages > 0 -> currentPage.toFloat() / totalPages.toFloat()
        chapter.read -> 1f
        else -> null
    }
    return TodayHeroUiModel(
        mangaId = manga.id,
        chapterId = chapter.id,
        title = manga.title,
        chapterLabel = chapter.name.ifBlank { chapter.chapterNumber.toChapterLabel(context) },
        currentPage = currentPage,
        totalPages = totalPages,
        progress = progress,
        coverData = history.coverData,
        bookmarked = bookmarkOverride ?: chapter.bookmark,
        bookmarkAvailable = true,
    )
}

private fun UpdatesWithRelations.toTodayItem() = TodayUpdateUiModel(
    mangaId = mangaId,
    chapterId = chapterId,
    title = mangaTitle,
    chapterLabel = chapterName,
    relativeTime = relativeTime(dateFetch),
    coverData = coverData,
)

private fun HistoryWithRelations.toTodayItem(context: Context) = TodayUpdateUiModel(
    mangaId = mangaId,
    chapterId = chapterId,
    title = title,
    chapterLabel = chapterNumber.toChapterLabel(context),
    relativeTime = readAt?.let { relativeTime(it.time) }.orEmpty(),
    coverData = coverData,
)

private fun Double.toChapterLabel(context: Context): String {
    val value = if (this % 1.0 == 0.0) toLong().toString() else toString()
    return context.stringResource(MR.strings.display_mode_chapter, value)
}

private fun relativeTime(timestamp: Long): String = DateUtils.getRelativeTimeSpanString(
    timestamp,
    System.currentTimeMillis(),
    DateUtils.MINUTE_IN_MILLIS,
    DateUtils.FORMAT_ABBREV_RELATIVE,
).toString()
