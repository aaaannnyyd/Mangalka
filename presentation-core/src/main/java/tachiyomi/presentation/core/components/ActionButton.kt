package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ActionButton(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InkGlassSurface(
        modifier = modifier,
        level = InkGlassLevel.Soft,
        contentPadding = PaddingValues(
            horizontal = InkGlassTokens.Space12,
            vertical = InkGlassTokens.Space12,
        ),
        shadowElevation = 0.dp,
        onClick = onClick,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = InkGlassTokens.Accent,
            )
            Text(
                text = title,
                textAlign = TextAlign.Center,
                style = InkTypography.Label,
                color = InkGlassTokens.TextPrimary,
            )
        }
    }
}
