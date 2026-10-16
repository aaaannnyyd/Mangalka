package eu.kanade.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxBy
import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkCircularProgress
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun WarningBanner(
    textRes: StringResource,
    modifier: Modifier = Modifier,
) {
    InkGlassSurface(
        modifier = modifier.fillMaxWidth(),
        level = InkGlassLevel.Accent,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(InkGlassTokens.Space12),
        shadowElevation = 0.dp,
    ) {
        Text(
            text = stringResource(textRes),
            modifier = Modifier.fillMaxWidth(),
            color = InkGlassTokens.TextPrimary,
            style = InkTypography.BodySmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun AppStateBanners(
    downloadedOnlyMode: Boolean,
    incognitoMode: Boolean,
    indexing: Boolean,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val mainInsets = WindowInsets.statusBars
    val mainInsetsTop = mainInsets.getTop(density)
    SubcomposeLayout(modifier = modifier) { constraints ->
        val indexingPlaceable = subcompose(0) {
            AnimatedVisibility(
                visible = indexing,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                IndexingDownloadBanner(
                    modifier = Modifier.windowInsetsPadding(mainInsets),
                )
            }
        }.fastMap { it.measure(constraints) }
        val indexingHeight = indexingPlaceable.fastMaxBy { it.height }?.height ?: 0

        val downloadedOnlyPlaceable = subcompose(1) {
            AnimatedVisibility(
                visible = downloadedOnlyMode,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                val top = (mainInsetsTop - indexingHeight).coerceAtLeast(0)
                DownloadedOnlyModeBanner(
                    modifier = Modifier.windowInsetsPadding(WindowInsets(top = top)),
                )
            }
        }.fastMap { it.measure(constraints) }
        val downloadedOnlyHeight = downloadedOnlyPlaceable.fastMaxBy { it.height }?.height ?: 0

        val incognitoPlaceable = subcompose(2) {
            AnimatedVisibility(
                visible = incognitoMode,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                val top = (mainInsetsTop - indexingHeight - downloadedOnlyHeight).coerceAtLeast(0)
                IncognitoModeBanner(
                    modifier = Modifier.windowInsetsPadding(WindowInsets(top = top)),
                )
            }
        }.fastMap { it.measure(constraints) }
        val incognitoHeight = incognitoPlaceable.fastMaxBy { it.height }?.height ?: 0

        layout(constraints.maxWidth, indexingHeight + downloadedOnlyHeight + incognitoHeight) {
            indexingPlaceable.fastForEach {
                it.place(0, 0)
            }
            downloadedOnlyPlaceable.fastForEach {
                it.place(0, indexingHeight)
            }
            incognitoPlaceable.fastForEach {
                it.place(0, indexingHeight + downloadedOnlyHeight)
            }
        }
    }
}

@Composable
private fun DownloadedOnlyModeBanner(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(MR.strings.label_downloaded_only),
        modifier = Modifier
            .background(InkGlassTokens.GlassStrong)
            .fillMaxWidth()
            .padding(vertical = InkGlassTokens.Space8, horizontal = InkGlassTokens.Space12)
            .then(modifier),
        color = InkGlassTokens.TextSecondary,
        textAlign = TextAlign.Center,
        style = InkTypography.Eyebrow,
    )
}

@Composable
private fun IncognitoModeBanner(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(MR.strings.pref_incognito_mode),
        modifier = Modifier
            .background(InkGlassTokens.AccentSoft)
            .fillMaxWidth()
            .padding(vertical = InkGlassTokens.Space8, horizontal = InkGlassTokens.Space12)
            .then(modifier),
        color = InkGlassTokens.Accent,
        textAlign = TextAlign.Center,
        style = InkTypography.Eyebrow,
    )
}

@Composable
private fun IndexingDownloadBanner(modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    Row(
        modifier = Modifier
            .background(InkGlassTokens.GlassStrong)
            .fillMaxWidth()
            .padding(InkGlassTokens.Space8)
            .then(modifier),
        horizontalArrangement = Arrangement.Center,
    ) {
        var textHeight by remember { mutableStateOf(0.dp) }
        InkCircularProgress(
            modifier = Modifier.requiredSize(textHeight),
            strokeWidth = textHeight / 8,
            color = InkGlassTokens.Accent,
            trackColor = androidx.compose.ui.graphics.Color.Transparent,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(MR.strings.download_notifier_cache_renewal),
            color = InkGlassTokens.TextSecondary,
            textAlign = TextAlign.Center,
            style = InkTypography.Label,
            onTextLayout = {
                with(density) {
                    textHeight = it.size.height.toDp()
                }
            },
        )
    }
}
