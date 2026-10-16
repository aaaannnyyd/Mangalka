package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkTypography

@Composable
fun PreferenceGroupHeader(title: String) {
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = InkGlassTokens.ScreenHorizontal,
                end = InkGlassTokens.ScreenHorizontal,
                bottom = InkGlassTokens.Space8,
                top = InkGlassTokens.Space20,
            ),
    ) {
        Text(
            text = title.uppercase(),
            color = InkGlassTokens.Accent,
            style = InkTypography.Eyebrow,
        )
    }
}
