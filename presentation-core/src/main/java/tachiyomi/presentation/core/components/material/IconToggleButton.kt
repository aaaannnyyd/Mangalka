package tachiyomi.presentation.core.components.material

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkTypography

@Composable
fun IconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    imageVector: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
) {
    InkGlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        level = if (checked) InkGlassLevel.Accent else InkGlassLevel.Soft,
        contentPadding = PaddingValues(
            horizontal = InkGlassTokens.Space12,
            vertical = InkGlassTokens.Space12,
        ),
        shadowElevation = 0.dp,
        onClick = { onCheckedChange(!checked) },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (checked) InkGlassTokens.Accent else InkGlassTokens.TextSecondary,
            )
            Text(
                text = title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = InkTypography.Label,
                color = if (checked) InkGlassTokens.Accent else InkGlassTokens.TextPrimary,
            )
        }
    }
}
