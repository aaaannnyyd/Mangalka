package tachiyomi.presentation.core.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import tachiyomi.presentation.core.components.InkAccentButton
import tachiyomi.presentation.core.components.InkGlassButton
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkMangaBackdrop
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.components.material.Scaffold

@Composable
fun InfoScreen(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    headingText: String,
    subtitleText: String,
    acceptText: String,
    onAcceptClick: () -> Unit,
    canAccept: Boolean = true,
    rejectText: String? = null,
    onRejectClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(InkGlassTokens.BackgroundPrimary),
    ) {
        InkMangaBackdrop(intensity = 0.45f)
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            bottomBar = {
                InkGlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars),
                    level = InkGlassLevel.Strong,
                    shape = RoundedCornerShape(
                        topStart = InkGlassTokens.RadiusLargeCard,
                        topEnd = InkGlassTokens.RadiusLargeCard,
                    ),
                    contentPadding = PaddingValues(
                        horizontal = InkGlassTokens.ScreenHorizontal,
                        vertical = InkGlassTokens.Space12,
                    ),
                    shadowElevation = InkGlassTokens.Space12,
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
                    ) {
                        InkAccentButton(
                            label = acceptText,
                            onClick = onAcceptClick,
                            enabled = canAccept,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (rejectText != null && onRejectClick != null) {
                            InkGlassButton(
                                label = rejectText,
                                onClick = onRejectClick,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            },
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues)
                    .padding(
                        start = InkGlassTokens.ScreenHorizontal,
                        end = InkGlassTokens.ScreenHorizontal,
                        top = InkGlassTokens.Space48,
                        bottom = InkGlassTokens.Space24,
                    ),
                verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12),
            ) {
                InkGlassSurface(
                    level = InkGlassLevel.Accent,
                    shape = RoundedCornerShape(InkGlassTokens.RadiusMedium),
                    contentPadding = PaddingValues(InkGlassTokens.Space12),
                    shadowElevation = 0.dp,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = InkGlassTokens.Accent,
                    )
                }
                Text(
                    text = headingText,
                    style = InkTypography.Display,
                    color = InkGlassTokens.TextPrimary,
                )
                Text(
                    text = subtitleText,
                    style = InkTypography.Body,
                    color = InkGlassTokens.TextSecondary,
                )

                content()
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun InfoScaffoldPreview() {
    InfoScreen(
        icon = Icons.Outlined.Newspaper,
        headingText = "Heading",
        subtitleText = "Subtitle",
        acceptText = "Accept",
        onAcceptClick = {},
        rejectText = "Reject",
        onRejectClick = {},
    ) {
        Text("Hello world", color = InkGlassTokens.TextPrimary)
    }
}
