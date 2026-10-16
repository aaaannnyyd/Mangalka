package eu.kanade.tachiyomi.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.TabNavigator
import eu.kanade.presentation.util.Screen
import eu.kanade.presentation.util.isTabletUi
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.download.DownloadQueueScreen
import eu.kanade.tachiyomi.ui.history.HistoryTab
import eu.kanade.tachiyomi.ui.library.LibraryTab
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.more.MoreTab
import eu.kanade.tachiyomi.ui.today.TodayTab
import eu.kanade.tachiyomi.ui.updates.UpdatesTab
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import soup.compose.material.motion.animation.materialFadeThroughIn
import soup.compose.material.motion.animation.materialFadeThroughOut
import tachiyomi.presentation.core.components.InkFloatingBottomNavigation
import tachiyomi.presentation.core.components.InkFloatingNavigationRail
import tachiyomi.presentation.core.components.InkGlassScaffold
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkNavigationItem

object HomeScreen : Screen() {

    private val LIBRARY_SEARCH_EVENT = Channel<String>()
    private val OPEN_TAB_EVENT = Channel<Tab>()
    private val SHOW_BOTTOM_NAV_EVENT = Channel<Boolean>()

    private const val TAB_FADE_DURATION = 220
    private const val TAB_NAVIGATOR_KEY = "HomeTabs"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        TabNavigator(tab = TodayTab, key = TAB_NAVIGATOR_KEY) { tabNavigator ->
            CompositionLocalProvider(LocalNavigator provides navigator) {
                var showNavigation by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    SHOW_BOTTOM_NAV_EVENT.receiveAsFlow().collectLatest { showNavigation = it }
                }
                val scope = rememberCoroutineScope()
                val tabletUi = isTabletUi()
                val navigationBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

                InkGlassScaffold {
                    val current = tabNavigator.current
                    AnimatedContent(
                        targetState = current,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = if (showNavigation &&
                                    tabletUi
                                ) {
                                    InkGlassTokens.NavigationReservedSpace
                                } else {
                                    0.dp
                                },
                                bottom = if (showNavigation && !tabletUi && current != TodayTab) {
                                    InkGlassTokens.NavigationReservedSpace + navigationBarInset
                                } else {
                                    0.dp
                                },
                            ),
                        transitionSpec = {
                            materialFadeThroughIn(initialScale = 1f, durationMillis = TAB_FADE_DURATION) togetherWith
                                materialFadeThroughOut(durationMillis = TAB_FADE_DURATION)
                        },
                        label = "tabContent",
                    ) { tab ->
                        tabNavigator.saveableState(key = "currentTab", tab) { tab.Content() }
                    }

                    if (showNavigation) {
                        val todayOptions = TodayTab.options
                        val libraryOptions = LibraryTab.options
                        val browseOptions = BrowseTab.options
                        val moreOptions = MoreTab.options
                        val navigationItems = listOf(
                            TodayTab to (todayOptions to 0),
                            LibraryTab to (libraryOptions to 0),
                            BrowseTab to (browseOptions to 0),
                            MoreTab to (moreOptions to 0),
                        )
                        val items = navigationItems.map { (tab, configuration) ->
                            val (options, badgeCount) = configuration
                            val selected = when (tab) {
                                TodayTab -> current == TodayTab || current == UpdatesTab || current == HistoryTab
                                else -> current::class == tab::class
                            }
                            InkNavigationItem(
                                key = tab.key,
                                label = options.title,
                                icon = options.icon!!,
                                selected = selected,
                                badgeCount = badgeCount,
                                onClick = {
                                    if (!selected || current != tab) {
                                        tabNavigator.current = tab
                                    } else {
                                        scope.launch { tab.onReselect(navigator) }
                                    }
                                },
                            )
                        }
                        if (tabletUi) {
                            InkFloatingNavigationRail(
                                items = items,
                                modifier = Modifier.align(Alignment.CenterStart),
                            )
                        } else {
                            InkFloatingBottomNavigation(
                                items = items,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .widthIn(max = 520.dp),
                            )
                        }
                    }
                }
            }

            val goToToday = { tabNavigator.current = TodayTab }
            BackHandler(enabled = tabNavigator.current != TodayTab, onBack = goToToday)

            LaunchedEffect(Unit) {
                launch {
                    LIBRARY_SEARCH_EVENT.receiveAsFlow().collectLatest {
                        tabNavigator.current = LibraryTab
                        LibraryTab.search(it)
                    }
                }
                launch {
                    OPEN_TAB_EVENT.receiveAsFlow().collectLatest { target ->
                        tabNavigator.current = when (target) {
                            is Tab.Library -> LibraryTab
                            Tab.Updates -> UpdatesTab
                            Tab.History -> HistoryTab
                            is Tab.Browse -> {
                                if (target.toExtensions) BrowseTab.showExtension()
                                BrowseTab
                            }
                            is Tab.More -> MoreTab
                        }
                        if (target is Tab.Library && target.mangaIdToOpen != null) {
                            navigator.push(MangaScreen(target.mangaIdToOpen))
                        }
                        if (target is Tab.More && target.toDownloads) {
                            navigator.push(DownloadQueueScreen)
                        }
                    }
                }
            }
        }
    }

    suspend fun search(query: String) = LIBRARY_SEARCH_EVENT.send(query)

    suspend fun openTab(tab: Tab) = OPEN_TAB_EVENT.send(tab)

    suspend fun showBottomNav(show: Boolean) = SHOW_BOTTOM_NAV_EVENT.send(show)

    sealed interface Tab {
        data class Library(val mangaIdToOpen: Long? = null) : Tab
        data object Updates : Tab
        data object History : Tab
        data class Browse(val toExtensions: Boolean = false) : Tab
        data class More(val toDownloads: Boolean = false) : Tab
    }
}
