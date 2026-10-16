package eu.kanade.presentation.more.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkGlassIconButton
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun InkSettingsTopBar(
    title: String,
    navigateUp: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(
                start = InkGlassTokens.ScreenHorizontal,
                end = InkGlassTokens.ScreenHorizontal,
                top = InkGlassTokens.Space12,
                bottom = InkGlassTokens.Space12,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12),
    ) {
        if (navigateUp != null) {
            InkGlassIconButton(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(MR.strings.ml_g2_back),
                onClick = navigateUp,
            )
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = InkTypography.Headline,
            color = InkGlassTokens.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        actions()
    }
}
