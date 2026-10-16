package eu.kanade.presentation.reader

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkTypography

@Composable
fun ReaderPageIndicator(
    currentPage: Int,
    totalPages: Int,
    modifier: Modifier = Modifier,
) {
    if (currentPage <= 0 || totalPages <= 0) return

    InkGlassSurface(
        modifier = modifier,
        level = InkGlassLevel.Soft,
        contentPadding = PaddingValues(
            horizontal = InkGlassTokens.Space12,
            vertical = InkGlassTokens.Space4,
        ),
        shadowElevation = 0.dp,
    ) {
        Text(
            text = "$currentPage / $totalPages",
            style = InkTypography.Eyebrow,
            color = InkGlassTokens.TextPrimary,
        )
    }
}

@PreviewLightDark
@Composable
private fun ReaderPageIndicatorPreview() {
    TachiyomiPreviewTheme {
        ReaderPageIndicator(currentPage = 10, totalPages = 69)
    }
}
