package eu.kanade.presentation.reader.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens

@Composable
internal fun ReaderFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    InkGlassSurface(
        modifier = modifier,
        level = if (selected) InkGlassLevel.Accent else InkGlassLevel.Soft,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(InkGlassTokens.RadiusChip),
        contentPadding = PaddingValues(horizontal = InkGlassTokens.Space12, vertical = InkGlassTokens.Space8),
        shadowElevation = 0.dp,
        onClick = onClick,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides if (selected) {
                InkGlassTokens.Accent
            } else {
                InkGlassTokens.TextSecondary
            },
            content = label,
        )
    }
}
