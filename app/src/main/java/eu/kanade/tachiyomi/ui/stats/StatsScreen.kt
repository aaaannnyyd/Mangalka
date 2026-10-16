package eu.kanade.tachiyomi.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.more.settings.InkSettingsTopBar
import eu.kanade.presentation.more.stats.StatsScreenContent
import eu.kanade.presentation.more.stats.StatsScreenState
import eu.kanade.presentation.util.Screen
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkLoadingState
import tachiyomi.presentation.core.components.InkMangaBackdrop
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

class StatsScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = metroViewModel<StatsViewModel>()
        val state by viewModel.state.collectAsState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(InkGlassTokens.BackgroundPrimary),
        ) {
            InkMangaBackdrop(intensity = 0.45f)
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    InkSettingsTopBar(
                        title = stringResource(MR.strings.label_stats),
                        navigateUp = navigator::pop,
                    )
                },
            ) { paddingValues ->
                if (state is StatsScreenState.Loading) {
                    InkLoadingState(
                        modifier = Modifier
                            .padding(paddingValues)
                            .fillMaxWidth()
                            .padding(top = InkGlassTokens.Space48),
                        label = stringResource(MR.strings.ml_g3_stats_loading),
                    )
                    return@Scaffold
                }

                StatsScreenContent(
                    state = state as StatsScreenState.Success,
                    paddingValues = paddingValues,
                )
            }
        }
    }
}
