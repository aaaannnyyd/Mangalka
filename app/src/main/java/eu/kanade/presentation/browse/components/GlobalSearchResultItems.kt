package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkCircularProgress
import tachiyomi.presentation.core.components.InkGlassIconButton
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun GlobalSearchResultItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.padding(vertical = InkGlassTokens.Space8),
        verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
    ) {
        InkGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = InkGlassTokens.ScreenHorizontal),
            level = InkGlassLevel.Soft,
            contentPadding = PaddingValues(
                horizontal = InkGlassTokens.Space16,
                vertical = InkGlassTokens.Space12,
            ),
            shadowElevation = 0.dp,
            onClick = onClick,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = InkTypography.TitleSmall,
                        color = InkGlassTokens.TextPrimary,
                    )
                    Text(
                        text = subtitle,
                        style = InkTypography.BodySmall,
                        color = InkGlassTokens.TextSecondary,
                    )
                }
                InkGlassIconButton(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    onClick = onClick,
                    modifier = Modifier.size(40.dp),
                )
            }
        }
        content()
    }
}

@Composable
fun GlobalSearchLoadingResultItem() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = InkGlassTokens.Space16),
    ) {
        InkCircularProgress(
            modifier = Modifier
                .size(18.dp)
                .align(Alignment.Center),
        )
    }
}

@Composable
fun GlobalSearchErrorResultItem(message: String?) {
    Column(
        modifier = Modifier
            .padding(
                horizontal = InkGlassTokens.ScreenHorizontal,
                vertical = InkGlassTokens.Space12,
            )
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Error,
            contentDescription = null,
            tint = InkGlassTokens.Accent,
        )
        Spacer(Modifier.height(InkGlassTokens.Space4))
        Text(
            text = message ?: stringResource(MR.strings.unknown_error),
            textAlign = TextAlign.Center,
            style = InkTypography.BodySmall,
            color = InkGlassTokens.TextSecondary,
        )
    }
}
