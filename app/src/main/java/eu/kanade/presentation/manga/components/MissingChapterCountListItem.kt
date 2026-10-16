package eu.kanade.presentation.manga.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassScaffold
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.i18n.pluralStringResource

@Composable
fun MissingChapterCountListItem(
    count: Int,
    modifier: Modifier = Modifier,
) {
    InkGlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = InkGlassTokens.ScreenHorizontal,
                vertical = InkGlassTokens.Space4,
            ),
        level = InkGlassLevel.Soft,
        contentPadding = PaddingValues(
            horizontal = InkGlassTokens.Space16,
            vertical = InkGlassTokens.Space12,
        ),
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = pluralStringResource(MR.plurals.missing_chapters, count = count, count),
                modifier = Modifier.weight(1f),
                style = InkTypography.BodySmall,
                color = InkGlassTokens.Warning,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun Preview() {
    TachiyomiPreviewTheme {
        InkGlassScaffold {
            MissingChapterCountListItem(count = 42)
        }
    }
}
