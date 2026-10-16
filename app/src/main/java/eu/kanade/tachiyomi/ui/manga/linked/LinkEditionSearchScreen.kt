package eu.kanade.tachiyomi.ui.manga.linked

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import eu.kanade.domain.manga.interactor.LinkedMangaService
import eu.kanade.presentation.browse.GlobalSearchContent
import eu.kanade.presentation.browse.components.GlobalSearchToolbar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreen
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchViewModel
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SourceFilter
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.presentation.core.components.InkAccentButton
import tachiyomi.presentation.core.components.InkAlertDialog
import tachiyomi.presentation.core.components.InkEmptyState
import tachiyomi.presentation.core.components.InkGlassButton
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkLoadingState
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.components.material.Scaffold

/** Global search across all sources where tapping a result links it as another edition of [mangaId]. */
class LinkEditionSearchScreen(
    private val mangaId: Long,
    private val query: String,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val search = assistedMetroViewModel<GlobalSearchViewModel, GlobalSearchViewModel.Factory> {
            create(initialQuery = query, initialExtensionFilter = null)
        }
        val state by search.state.collectAsStateWithLifecycle()
        val model = assistedMetroViewModel<Model, Model.Factory> { create(mangaId) }
        val linkState by model.state.collectAsStateWithLifecycle()

        LaunchedEffect(search) { search.setSourceFilter(SourceFilter.All) }
        LaunchedEffect(model) { model.events.collectLatest { navigator.pop() } }
        BackHandler(enabled = linkState.linking) {}

        Scaffold(
            containerColor = InkGlassTokens.BackgroundPrimary,
            topBar = { scrollBehavior ->
                Column {
                    GlobalSearchToolbar(
                        searchQuery = state.searchQuery,
                        progress = state.progress,
                        total = state.total,
                        navigateUp = { if (!linkState.linking) navigator.pop() },
                        onChangeSearchQuery = search::updateSearchQuery,
                        onSearch = { search.search() },
                        hideSourceFilter = false,
                        sourceFilter = state.sourceFilter,
                        onChangeSearchFilter = search::setSourceFilter,
                        onlyShowHasResults = state.onlyShowHasResults,
                        onToggleResults = search::toggleFilterResults,
                        scrollBehavior = scrollBehavior,
                    )
                    Text(
                        "Выберите ту же мангу на другом сайте. Удержите обложку, чтобы сначала её посмотреть.",
                        style = InkTypography.BodySmall,
                        color = InkGlassTokens.TextSecondary,
                        modifier = Modifier.fillMaxWidth().padding(
                            horizontal = InkGlassTokens.ScreenHorizontal,
                            vertical = InkGlassTokens.Space12,
                        ),
                    )
                }
            },
        ) { padding ->
            if (state.filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding)
                        .padding(horizontal = InkGlassTokens.ScreenHorizontal),
                    contentAlignment = Alignment.Center,
                ) {
                    InkEmptyState(
                        title = when {
                            state.searchQuery.isNullOrBlank() -> "Введите название манги"
                            state.total == 0 -> "Нет источников для поиска"
                            else -> "Ничего не нашлось"
                        },
                        message = when {
                            state.searchQuery.isNullOrBlank() -> "Найдите эту мангу на другом сайте."
                            state.total == 0 -> "Установите расширение и включите его язык в каталоге."
                            else -> "Попробуйте другое название, например оригинальное или английское."
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                GlobalSearchContent(
                    items = state.filteredItems,
                    contentPadding = padding,
                    getManga = { search.getManga(it) },
                    onClickSource = { navigator.push(BrowseSourceScreen(it.id, state.searchQuery)) },
                    onClickItem = model::select,
                    onLongClickItem = { if (!linkState.linking) navigator.push(MangaScreen(it.id, true)) },
                )
            }
        }

        linkState.candidate?.let { candidate ->
            InkAlertDialog(
                onDismissRequest = model::dismiss,
                title = { Text("Это та же манга?") },
                text = {
                    Column {
                        Text(
                            "«${candidate.title}» · ${linkState.sourceName}\n\n" +
                                "Станет версией «${linkState.title}». В библиотеке останется одна карточка, " +
                                "переключаться можно в «Версиях и источниках».",
                        )
                        if (linkState.linking) {
                            InkLoadingState(
                                label = "Связываем",
                                modifier = Modifier.padding(top = InkGlassTokens.Space16),
                            )
                        }
                        linkState.error?.let {
                            Text(
                                it,
                                color = InkGlassTokens.Error,
                                modifier = Modifier.padding(top = InkGlassTokens.Space12),
                            )
                        }
                    }
                },
                confirmButton = { InkAccentButton("Связать", model::link, enabled = !linkState.linking) },
                dismissButton = { InkGlassButton("Отмена", model::dismiss, enabled = !linkState.linking) },
            )
        }
        if (linkState.candidate == null && linkState.error != null) {
            InkAlertDialog(
                onDismissRequest = model::dismiss,
                title = { Text("Не получилось") },
                text = { Text(linkState.error!!) },
                confirmButton = { InkGlassButton("Понятно", model::dismiss) },
            )
        }
    }

    @AssistedInject
    class Model(
        @Assisted private val mangaId: Long,
        private val linkedMangaService: LinkedMangaService,
        private val getManga: GetManga,
        private val sourceManager: SourceManager,
    ) : ViewModel() {
        val state: StateFlow<State>
            field = MutableStateFlow(State())
        private val eventChannel = Channel<Unit>(Channel.BUFFERED)
        val events = eventChannel.receiveAsFlow()

        init {
            viewModelScope.launch {
                getManga.await(mangaId)?.let { manga -> state.update { it.copy(title = manga.title) } }
            }
        }

        @AssistedFactory
        @ManualViewModelAssistedFactoryKey
        @ContributesIntoMap(AppScope::class)
        interface Factory : ManualViewModelAssistedFactory {
            fun create(mangaId: Long): Model
        }

        fun select(manga: Manga) {
            if (state.value.linking) return
            if (manga.id == mangaId) {
                state.update { it.copy(error = "Это и есть открытая версия. Выберите другой сайт.") }
                return
            }
            state.update {
                it.copy(candidate = manga, sourceName = sourceManager.getOrStub(manga.source).name, error = null)
            }
        }

        fun dismiss() {
            if (!state.value.linking) state.update { it.copy(candidate = null, error = null) }
        }

        fun link() {
            val candidate = state.value.candidate ?: return
            if (state.value.linking) return
            state.update { it.copy(linking = true, error = null) }
            viewModelScope.launch {
                try {
                    withIOContext { linkedMangaService.link(mangaId, candidate.id) }
                    state.update { it.copy(candidate = null) }
                    eventChannel.send(Unit)
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    state.update { it.copy(error = "Не удалось сохранить связь. Попробуйте ещё раз.") }
                } finally {
                    state.update { it.copy(linking = false) }
                }
            }
        }
    }

    data class State(
        val title: String = "",
        val candidate: Manga? = null,
        val sourceName: String = "",
        val linking: Boolean = false,
        val error: String? = null,
    )
}
