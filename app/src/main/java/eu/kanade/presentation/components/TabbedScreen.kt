package eu.kanade.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.icerock.moko.resources.StringResource
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkGlassChip
import tachiyomi.presentation.core.components.InkGlassIconButton
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkMangaBackdrop
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.TabText
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun TabbedScreen(
    titleRes: StringResource,
    tabs: List<TabContent>,
    state: PagerState = rememberPagerState { tabs.size },
    searchQuery: String? = null,
    onChangeSearchQuery: (String?) -> Unit = {},
    inkStyle: Boolean = false,
    inkTitleOverride: String? = null,
    inkTabTitleOverrides: List<String>? = null,
) {
    if (inkStyle) {
        InkTabbedScreen(
            title = inkTitleOverride ?: stringResource(titleRes),
            tabs = tabs,
            state = state,
            searchQuery = searchQuery,
            onChangeSearchQuery = onChangeSearchQuery,
            tabTitleOverrides = inkTabTitleOverrides,
        )
        return
    }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            val tab = tabs[state.currentPage]
            val searchEnabled = tab.searchEnabled

            SearchToolbar(
                titleContent = { AppBarTitle(stringResource(titleRes)) },
                searchEnabled = searchEnabled,
                searchQuery = if (searchEnabled) searchQuery else null,
                onChangeSearchQuery = onChangeSearchQuery,
                actions = { AppBarActions(tab.actions) },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { contentPadding ->
        Column(
            modifier = Modifier.padding(
                top = contentPadding.calculateTopPadding(),
                start = contentPadding.calculateStartPadding(LocalLayoutDirection.current),
                end = contentPadding.calculateEndPadding(LocalLayoutDirection.current),
            ),
        ) {
            PrimaryTabRow(
                selectedTabIndex = state.currentPage,
                modifier = Modifier.zIndex(1f),
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = state.currentPage == index,
                        onClick = { scope.launch { state.animateScrollToPage(index) } },
                        text = { TabText(text = stringResource(tab.titleRes), badgeCount = tab.badgeNumber) },
                        unselectedContentColor = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            HorizontalPager(
                modifier = Modifier.fillMaxSize(),
                state = state,
                verticalAlignment = Alignment.Top,
            ) { page ->
                tabs[page].content(
                    PaddingValues(bottom = contentPadding.calculateBottomPadding()),
                    snackbarHostState,
                )
            }
        }
    }
}

@Composable
private fun InkTabbedScreen(
    title: String,
    tabs: List<TabContent>,
    state: PagerState,
    searchQuery: String?,
    onChangeSearchQuery: (String?) -> Unit,
    tabTitleOverrides: List<String>?,
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentTab = tabs[state.currentPage]
    val searching = currentTab.searchEnabled && searchQuery != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(InkGlassTokens.BackgroundPrimary)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
    ) {
        InkMangaBackdrop(intensity = 0.8f)
        Column(modifier = Modifier.fillMaxSize()) {
            if (searching) {
                InkGlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = InkGlassTokens.ScreenHorizontal,
                            end = InkGlassTokens.ScreenHorizontal,
                            top = InkGlassTokens.Space20,
                        ),
                    level = InkGlassLevel.Strong,
                    contentPadding = PaddingValues(
                        horizontal = InkGlassTokens.Space16,
                        vertical = InkGlassTokens.Space12,
                    ),
                    shadowElevation = 0.dp,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BasicTextField(
                            value = searchQuery.orEmpty(),
                            onValueChange = { onChangeSearchQuery(it) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = InkTypography.Body.copy(color = InkGlassTokens.TextPrimary),
                            cursorBrush = SolidColor(InkGlassTokens.Accent),
                            decorationBox = { inner ->
                                if (searchQuery.isNullOrEmpty()) {
                                    Text(
                                        stringResource(MR.strings.ml_g3_search_extensions_hint),
                                        style = InkTypography.Body,
                                        color = InkGlassTokens.TextTertiary,
                                    )
                                }
                                inner()
                            },
                        )
                        InkGlassIconButton(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(MR.strings.ml_g3_close_search),
                            onClick = { onChangeSearchQuery(null) },
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = InkGlassTokens.ScreenHorizontal,
                            end = InkGlassTokens.ScreenHorizontal,
                            top = InkGlassTokens.Space20,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(MR.strings.ml_g3_brand_eyebrow),
                            style = InkTypography.Eyebrow,
                            color = InkGlassTokens.Accent,
                        )
                        Text(
                            title,
                            style = InkTypography.Display,
                            color = InkGlassTokens.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (currentTab.searchEnabled) {
                            InkGlassIconButton(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = stringResource(MR.strings.ml_g3_search),
                                onClick = { onChangeSearchQuery("") },
                            )
                        }
                        AppBarActions(currentTab.actions)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = InkGlassTokens.ScreenHorizontal,
                        end = InkGlassTokens.ScreenHorizontal,
                        top = InkGlassTokens.Space20,
                        bottom = InkGlassTokens.Space16,
                    ),
                horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
            ) {
                tabs.forEachIndexed { index, tab ->
                    val badge = tab.badgeNumber?.takeIf { it > 0 }
                    val baseLabel = tabTitleOverrides?.getOrNull(index) ?: stringResource(tab.titleRes)
                    val label = if (badge != null) "$baseLabel · $badge" else baseLabel
                    InkGlassChip(
                        label = label,
                        selected = state.currentPage == index,
                        onClick = { scope.launch { state.animateScrollToPage(index) } },
                    )
                }
            }

            HorizontalPager(
                modifier = Modifier.fillMaxSize(),
                state = state,
                verticalAlignment = Alignment.Top,
            ) { page ->
                tabs[page].content(PaddingValues(bottom = InkGlassTokens.Space24), snackbarHostState)
            }
        }
    }
}

data class TabContent(
    val titleRes: StringResource,
    val badgeNumber: Int? = null,
    val searchEnabled: Boolean = false,
    val actions: List<AppBar.AppBarAction> = listOf(),
    val content: @Composable (contentPadding: PaddingValues, snackbarHostState: SnackbarHostState) -> Unit,
)
