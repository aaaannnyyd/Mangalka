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
import dev.icerock.moko.resources.StringResource
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
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkAccentButton
import tachiyomi.presentation.core.components.InkAlertDialog
import tachiyomi.presentation.core.components.InkEmptyState
import tachiyomi.presentation.core.components.InkGlassButton
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkLoadingState
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

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
                        stringResource(MR.strings.ml_g2_link_search_hint),
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
                            state.searchQuery.isNullOrBlank() -> stringResource(MR.strings.ml_g2_enter_manga_title)
                            state.total == 0 -> stringResource(MR.strings.ml_g2_no_sources_to_search)
                            else -> stringResource(MR.strings.ml_g2_nothing_found)
                        },
                        message = when {
                            state.searchQuery.isNullOrBlank() -> stringResource(MR.strings.ml_g2_find_on_other_site)
                            state.total == 0 -> stringResource(MR.strings.ml_g2_install_extension_hint)
                            else -> stringResource(MR.strings.ml_g2_try_other_title)
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
                title = { Text(stringResource(MR.strings.ml_g2_same_manga_title)) },
                text = {
                    Column {
                        Text(
                            stringResource(
                                MR.strings.ml_g2_same_manga_message,
                                candidate.title,
                                linkState.sourceName,
                                linkState.title,
                            ),
                        )
                        if (linkState.linking) {
                            InkLoadingState(
                                label = stringResource(MR.strings.ml_g2_linking),
                                modifier = Modifier.padding(top = InkGlassTokens.Space16),
                            )
                        }
                        linkState.error?.let {
                            Text(
                                stringResource(it),
                                color = InkGlassTokens.Error,
                                modifier = Modifier.padding(top = InkGlassTokens.Space12),
                            )
                        }
                    }
                },
                confirmButton = {
                    InkAccentButton(stringResource(MR.strings.ml_g2_link), model::link, enabled = !linkState.linking)
                },
                dismissButton = {
                    InkGlassButton(
                        stringResource(MR.strings.action_cancel),
                        model::dismiss,
                        enabled = !linkState.linking,
                    )
                },
            )
        }
        if (linkState.candidate == null && linkState.error != null) {
            InkAlertDialog(
                onDismissRequest = model::dismiss,
                title = { Text(stringResource(MR.strings.ml_g2_failed_title)) },
                text = { Text(stringResource(linkState.error!!)) },
                confirmButton = { InkGlassButton(stringResource(MR.strings.ml_g2_got_it), model::dismiss) },
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
                state.update { it.copy(error = MR.strings.ml_g2_already_open_version) }
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
                    state.update { it.copy(error = MR.strings.ml_g2_link_save_error) }
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
        val error: StringResource? = null,
    )
}
