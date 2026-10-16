package mihon.feature.support

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import tachiyomi.core.common.Constants
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkMangaBackdrop
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.icons.CustomIcons
import tachiyomi.presentation.core.icons.Discord
import tachiyomi.presentation.core.icons.OpenCollective
import tachiyomi.presentation.core.icons.Patreon

class SupportUsScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val uriHandler = LocalUriHandler.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(InkGlassTokens.BackgroundPrimary),
        ) {
            InkMangaBackdrop(intensity = 0.45f)
            Scaffold(
                containerColor = Color.Transparent,
                topBar = { scrollBehavior ->
                    AppBar(
                        title = stringResource(MR.strings.ml_g3_support_title),
                        subtitle = stringResource(MR.strings.ml_g3_support_subtitle),
                        navigateUp = navigator::pop,
                        scrollBehavior = scrollBehavior,
                    )
                },
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start = InkGlassTokens.ScreenHorizontal,
                            end = InkGlassTokens.ScreenHorizontal,
                            top = paddingValues.calculateTopPadding() + InkGlassTokens.Space16,
                            bottom = paddingValues.calculateBottomPadding() + InkGlassTokens.Space32,
                        ),
                    verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12),
                ) {
                    Text(
                        text = "OPEN SOURCE",
                        style = InkTypography.Eyebrow,
                        color = InkGlassTokens.Accent,
                    )
                    Text(
                        text = stringResource(MR.strings.ml_g3_support_headline),
                        style = InkTypography.Headline,
                        color = InkGlassTokens.TextPrimary,
                    )
                    Text(
                        text = stringResource(MR.strings.ml_g3_support_body),
                        style = InkTypography.Body,
                        color = InkGlassTokens.TextSecondary,
                    )

                    Spacer(Modifier.height(InkGlassTokens.Space8))
                    SupportItem(
                        icon = CustomIcons.Patreon,
                        title = "Patreon",
                        subtitle = stringResource(MR.strings.ml_g3_support_patreon),
                        onClick = { uriHandler.openUri(Constants.URL_DONATE_PATREON) },
                    )
                    SupportItem(
                        icon = CustomIcons.OpenCollective,
                        title = "Open Collective",
                        subtitle = stringResource(MR.strings.ml_g3_support_opencollective),
                        onClick = { uriHandler.openUri(Constants.URL_DONATE_OPENCOLLECTIVE) },
                    )
                    SupportItem(
                        icon = CustomIcons.Discord,
                        title = "Discord Mihon",
                        subtitle = stringResource(MR.strings.ml_g3_support_discord),
                        onClick = { uriHandler.openUri(Constants.URL_DISCORD) },
                    )

                    InkGlassSurface(
                        modifier = Modifier.fillMaxWidth(),
                        level = InkGlassLevel.Soft,
                        contentPadding = PaddingValues(InkGlassTokens.Space16),
                        shadowElevation = 0.dp,
                    ) {
                        Text(
                            text = stringResource(MR.strings.ml_g3_support_footer),
                            style = InkTypography.BodySmall,
                            color = InkGlassTokens.TextTertiary,
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun SupportItem(
        icon: ImageVector,
        title: String,
        subtitle: String,
        onClick: () -> Unit,
    ) {
        InkGlassSurface(
            modifier = Modifier.fillMaxWidth(),
            level = InkGlassLevel.Soft,
            contentPadding = PaddingValues(InkGlassTokens.Space16),
            shadowElevation = 0.dp,
            onClick = onClick,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space16),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = InkGlassTokens.Accent,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space4),
                ) {
                    Text(title, style = InkTypography.TitleSmall, color = InkGlassTokens.TextPrimary)
                    Text(subtitle, style = InkTypography.BodySmall, color = InkGlassTokens.TextSecondary)
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = InkGlassTokens.TextTertiary,
                )
            }
        }
    }
}
