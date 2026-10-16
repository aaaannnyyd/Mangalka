package eu.kanade.presentation.reader.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsViewModel
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkGlassChip
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ReaderSettingsDialog(
    onDismissRequest: () -> Unit,
    onShowMenus: () -> Unit,
    onHideMenus: () -> Unit,
    viewModel: ReaderSettingsViewModel,
) {
    val tabTitles = listOf(
        stringResource(MR.strings.pref_category_reading_mode),
        stringResource(MR.strings.pref_category_general),
        stringResource(MR.strings.custom_filter),
    )
    val pagerState = rememberPagerState { tabTitles.size }
    val scope = rememberCoroutineScope()

    AdaptiveSheet(
        onDismissRequest = {
            onDismissRequest()
            onShowMenus()
        },
        modifier = Modifier.fillMaxHeight(0.90f),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window

        LaunchedEffect(pagerState.currentPage) {
            if (pagerState.currentPage == 2) {
                window?.setDimAmount(0.12f)
                onHideMenus()
            } else {
                window?.setDimAmount(0.58f)
                onShowMenus()
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = InkGlassTokens.Space8, bottom = InkGlassTokens.Space12),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(InkGlassTokens.GlassBorder),
                )
            }

            Column(
                modifier = Modifier.padding(horizontal = InkGlassTokens.Space20),
                verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12),
            ) {
                Text(
                    stringResource(MR.strings.ml_g4_reader_eyebrow),
                    style = InkTypography.Eyebrow,
                    color = InkGlassTokens.Accent,
                )
                Text(
                    stringResource(MR.strings.ml_g4_reader_settings_title),
                    style = InkTypography.Headline,
                    color = InkGlassTokens.TextPrimary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        InkGlassChip(
                            label = title,
                            selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start = InkGlassTokens.Space4,
                            top = InkGlassTokens.Space12,
                            end = InkGlassTokens.Space4,
                            bottom = InkGlassTokens.Space32,
                        ),
                ) {
                    when (page) {
                        0 -> ReadingModePage(viewModel)
                        1 -> GeneralPage(viewModel)
                        2 -> ColorFilterPage(viewModel)
                    }
                }
            }
        }
    }
}
