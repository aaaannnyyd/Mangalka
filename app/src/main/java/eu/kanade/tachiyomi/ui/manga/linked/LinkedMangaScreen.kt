package eu.kanade.tachiyomi.ui.manga.linked

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
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
import eu.kanade.presentation.more.settings.InkSettingsTopBar
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.model.LinkedMangaReference
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.linkedVersions
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkAccentButton
import tachiyomi.presentation.core.components.InkAlertDialog
import tachiyomi.presentation.core.components.InkEmptyState
import tachiyomi.presentation.core.components.InkErrorState
import tachiyomi.presentation.core.components.InkGlassButton
import tachiyomi.presentation.core.components.InkGlassIconButton
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkLoadingState
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

class LinkedMangaScreen(private val mangaId: Long) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val model = assistedMetroViewModel<Model, Model.Factory> { create(mangaId) }
        val state by model.state.collectAsStateWithLifecycle()
        BackHandler(enabled = state.busy) {}

        LaunchedEffect(model) {
            model.events.collectLatest { result ->
                context.toast(
                    if (result.read + result.bookmarks > 0) {
                        context.stringResource(
                            MR.strings.ml_g2_source_switched_progress,
                            result.read,
                            result.bookmarks,
                        )
                    } else {
                        context.stringResource(MR.strings.ml_g2_source_switched)
                    },
                )
                navigator.pop()
                navigator.replace(MangaScreen(result.mangaId))
            }
        }

        LinkedMangaContent(
            state = state,
            navigateUp = { if (!state.busy) navigator.pop() },
            onSearch = { navigator.push(LinkEditionSearchScreen(mangaId, state.title)) },
            onRefresh = model::refresh,
            onSwitch = { model.confirm(it, unlink = false) },
            onUnlink = { model.confirm(it, unlink = true) },
            onRepairSource = {
                navigator.popUntil { it is HomeScreen }
                model.openExtensions()
            },
        )

        state.confirmation?.let { confirmation ->
            InkAlertDialog(
                onDismissRequest = model::dismissConfirmation,
                title = {
                    Text(
                        when {
                            confirmation.unlink -> stringResource(MR.strings.ml_g2_unlink_title)
                            confirmation.version.current -> stringResource(MR.strings.ml_g2_make_primary_title)
                            else -> stringResource(MR.strings.ml_g2_switch_version_title)
                        },
                    )
                },
                text = {
                    Text(
                        when {
                            confirmation.unlink ->
                                stringResource(MR.strings.ml_g2_unlink_message, confirmation.version.sourceName)
                            confirmation.version.current ->
                                stringResource(MR.strings.ml_g2_make_primary_message, confirmation.version.sourceName)
                            else ->
                                stringResource(MR.strings.ml_g2_switch_version_message, confirmation.version.sourceName)
                        },
                    )
                },
                confirmButton = {
                    InkAccentButton(
                        label = when {
                            confirmation.unlink -> stringResource(MR.strings.ml_g2_unlink)
                            confirmation.version.current -> stringResource(MR.strings.ml_g2_make_primary)
                            else -> stringResource(MR.strings.ml_g2_switch)
                        },
                        onClick = model::confirmOperation,
                    )
                },
                dismissButton = {
                    InkGlassButton(stringResource(MR.strings.action_cancel), model::dismissConfirmation)
                },
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
            field = MutableStateFlow<State>(State())
        private val eventChannel = Channel<LinkedMangaService.SwitchResult>(Channel.BUFFERED)
        val events = eventChannel.receiveAsFlow()
        private var refreshJob: Job? = null

        init {
            viewModelScope.launch {
                combine(getManga.subscribe(mangaId), sourceManager.sources) { manga, _ -> manga }
                    .distinctUntilChangedBy { it.title to it.linkedVersions }
                    .catch { state.update { it.copy(loading = false, error = LOAD_ERROR) } }
                    .collectLatest { load(it) }
            }
        }

        @AssistedFactory
        @ManualViewModelAssistedFactoryKey
        @ContributesIntoMap(AppScope::class)
        interface Factory : ManualViewModelAssistedFactory {
            fun create(mangaId: Long): Model
        }

        fun refresh() {
            if (state.value.busy) return
            refreshJob?.cancel()
            refreshJob = viewModelScope.launch {
                val manga = getManga.await(mangaId)
                if (manga != null) {
                    load(manga)
                } else {
                    state.update {
                        it.copy(
                            loading = false,
                            error = LOAD_ERROR,
                        )
                    }
                }
            }
        }

        private suspend fun load(manga: Manga) {
            try {
                val versions = withIOContext {
                    linkedMangaService.versions(mangaId).map { version ->
                        val source = sourceManager.get(version.reference.source)
                        Version(
                            reference = version.reference,
                            sourceName = source?.name ?: sourceManager.getOrStub(version.reference.source).name,
                            chapters = version.chapters,
                            read = version.read,
                            current = version.mangaId == mangaId,
                            preferred = version.preferred,
                            available = source != null,
                        )
                    }
                }
                state.update { it.copy(title = manga.title, versions = versions, loading = false, error = null) }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                state.update { it.copy(loading = false, error = LOAD_ERROR) }
            }
        }

        fun confirm(version: Version, unlink: Boolean) {
            if (state.value.busy || (unlink && version.current)) return
            if (!unlink && ((version.current && version.preferred) || (!version.current && !version.available))) return
            state.update { it.copy(confirmation = Confirmation(version, unlink)) }
        }

        fun dismissConfirmation() = state.update { it.copy(confirmation = null) }

        fun confirmOperation() {
            val confirmation = state.value.confirmation ?: return
            if (state.value.busy) return
            state.update { it.copy(confirmation = null, busy = true, error = null) }
            viewModelScope.launch {
                try {
                    withIOContext {
                        if (confirmation.unlink) {
                            linkedMangaService.unlink(mangaId, confirmation.version.reference)
                        } else {
                            eventChannel.send(linkedMangaService.switch(mangaId, confirmation.version.reference))
                        }
                    }
                    if (confirmation.unlink) getManga.await(mangaId)?.let { load(it) }
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    state.update { it.copy(error = MR.strings.ml_g2_source_unavailable) }
                } finally {
                    state.update { it.copy(busy = false) }
                }
            }
        }

        fun openExtensions() {
            viewModelScope.launchNonCancellable { HomeScreen.openTab(HomeScreen.Tab.Browse(toExtensions = true)) }
        }
    }

    data class State(
        val title: String = "",
        val versions: List<Version> = emptyList(),
        val loading: Boolean = true,
        val busy: Boolean = false,
        val error: StringResource? = null,
        val confirmation: Confirmation? = null,
    )

    data class Version(
        val reference: LinkedMangaReference,
        val sourceName: String,
        val chapters: Int,
        val read: Int,
        val current: Boolean,
        val preferred: Boolean,
        val available: Boolean,
    )

    data class Confirmation(val version: Version, val unlink: Boolean)

    private companion object {
        val LOAD_ERROR = MR.strings.ml_g2_versions_load_error
    }
}

@Composable
fun LinkedMangaEntry(versionCount: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(InkGlassTokens.RadiusChip))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = InkGlassTokens.Space8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12),
    ) {
        Icon(Icons.Outlined.Link, null, tint = InkGlassTokens.Accent, modifier = Modifier.size(InkGlassTokens.Space20))
        Text(
            stringResource(MR.strings.ml_g2_other_versions_count, versionCount - 1),
            style = InkTypography.BodySmall,
            color = InkGlassTokens.TextSecondary,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.AutoMirrored.Outlined.ArrowForward,
            null,
            tint = InkGlassTokens.TextTertiary,
            modifier = Modifier.size(InkGlassTokens.Space16),
        )
    }
}

@Composable
private fun LinkedMangaContent(
    state: LinkedMangaScreen.State,
    navigateUp: () -> Unit,
    onSearch: () -> Unit,
    onRefresh: () -> Unit,
    onSwitch: (LinkedMangaScreen.Version) -> Unit,
    onUnlink: (LinkedMangaScreen.Version) -> Unit,
    onRepairSource: () -> Unit,
) {
    Scaffold(
        containerColor = InkGlassTokens.BackgroundPrimary,
        topBar = {
            InkSettingsTopBar(stringResource(MR.strings.ml_g2_versions_and_sources), navigateUp) {
                InkGlassIconButton(
                    Icons.Outlined.Search,
                    stringResource(MR.strings.ml_g2_find_other_version),
                    onSearch,
                    enabled = !state.busy,
                )
            }
        },
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                InkLoadingState(label = stringResource(MR.strings.ml_g2_loading_versions))
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
            contentPadding = PaddingValues(InkGlassTokens.ScreenHorizontal),
            verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space4)) {
                    Text(state.title, style = InkTypography.Headline, color = InkGlassTokens.TextPrimary)
                    Text(
                        if (state.busy) {
                            stringResource(MR.strings.ml_g2_transferring_progress)
                        } else {
                            stringResource(MR.strings.ml_g2_only_primary_in_library)
                        },
                        style = InkTypography.BodySmall,
                        color = InkGlassTokens.TextSecondary,
                    )
                }
            }
            state.error?.let { error ->
                item {
                    InkErrorState(
                        stringResource(MR.strings.ml_g2_something_went_wrong),
                        stringResource(error),
                        onRefresh,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            if (state.versions.size <= 1 && state.error == null) {
                item {
                    InkEmptyState(
                        stringResource(MR.strings.ml_g2_no_other_versions),
                        stringResource(MR.strings.ml_g2_no_other_versions_hint),
                        modifier = Modifier.fillMaxWidth(),
                        actionLabel = stringResource(MR.strings.ml_g2_find_other_version),
                        onAction = onSearch,
                    )
                }
            } else {
                item {
                    InkGlassSurface(modifier = Modifier.fillMaxWidth(), level = InkGlassLevel.Soft) {
                        Column(verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space16)) {
                            state.versions.forEach { version ->
                                VersionRow(
                                    version = version,
                                    enabled = !state.busy,
                                    onSwitch = { onSwitch(version) },
                                    onUnlink = { onUnlink(version) },
                                    onRepairSource = onRepairSource,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VersionRow(
    version: LinkedMangaScreen.Version,
    enabled: Boolean,
    onSwitch: () -> Unit,
    onUnlink: () -> Unit,
    onRepairSource: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space4)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
            ) {
                Text(
                    version.sourceName,
                    style = InkTypography.TitleSmall,
                    color = InkGlassTokens.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                when {
                    version.preferred -> Text(
                        stringResource(MR.strings.ml_g2_badge_primary),
                        style = InkTypography.Label,
                        color = InkGlassTokens.Accent,
                    )
                    version.current -> Text(
                        stringResource(MR.strings.ml_g2_badge_opened),
                        style = InkTypography.Label,
                        color = InkGlassTokens.TextTertiary,
                    )
                }
            }
            Text(
                version.reference.title,
                style = InkTypography.BodySmall,
                color = InkGlassTokens.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (version.available) {
                    stringResource(MR.strings.ml_g2_version_chapters_read, version.chapters, version.read)
                } else {
                    stringResource(MR.strings.ml_g2_extension_missing)
                },
                style = InkTypography.BodySmall,
                color = if (version.available) InkGlassTokens.TextSecondary else InkGlassTokens.Warning,
            )
        }
        when {
            !version.available -> InkGlassButton(stringResource(MR.strings.label_extensions), onRepairSource)
            !version.preferred -> InkGlassButton(
                if (version.current) {
                    stringResource(MR.strings.ml_g2_primary)
                } else {
                    stringResource(MR.strings.ml_g2_switch)
                },
                onSwitch,
                enabled = enabled,
            )
        }
        if (!version.current) {
            InkGlassIconButton(
                Icons.Outlined.LinkOff,
                stringResource(MR.strings.ml_g2_unlink_from, version.sourceName),
                onUnlink,
                enabled = enabled,
            )
        }
    }
}

@Preview(name = "Linked sources · phone", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun LinkedMangaPreview() {
    TachiyomiPreviewTheme {
        LinkedMangaContent(
            state = LinkedMangaScreen.State(
                title = "Бродяга",
                loading = false,
                versions = listOf(
                    LinkedMangaScreen.Version(
                        LinkedMangaReference(1, "/vagabond", "Бродяга"),
                        "MangaLib",
                        327,
                        84,
                        current = true,
                        preferred = true,
                        available = true,
                    ),
                    LinkedMangaScreen.Version(
                        LinkedMangaReference(2, "/vagabond", "Бродяга"),
                        "ReManga",
                        327,
                        0,
                        current = false,
                        preferred = false,
                        available = true,
                    ),
                    LinkedMangaScreen.Version(
                        LinkedMangaReference(3, "/vagabond", "Бродяга"),
                        "MangaShi",
                        300,
                        52,
                        current = false,
                        preferred = false,
                        available = false,
                    ),
                ),
            ),
            navigateUp = {},
            onSearch = {},
            onRefresh = {},
            onSwitch = {},
            onUnlink = {},
            onRepairSource = {},
        )
    }
}
