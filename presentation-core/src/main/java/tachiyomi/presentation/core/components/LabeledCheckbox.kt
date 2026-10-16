package tachiyomi.presentation.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun LabeledCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(InkGlassTokens.RadiusSmall)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(if (checked) InkGlassTokens.AccentSoft else InkGlassTokens.GlassSoft.copy(alpha = 0.45f))
            .border(
                InkGlassTokens.BorderWidth,
                if (checked) InkGlassTokens.AccentBorder else InkGlassTokens.GlassBorderSoft,
                shape,
            )
            .clickable(
                enabled = enabled,
                role = Role.Checkbox,
                onClick = { onCheckedChange(!checked) },
            )
            .padding(horizontal = InkGlassTokens.Space12, vertical = InkGlassTokens.Space8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12),
    ) {
        Icon(
            imageVector = if (checked) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
            contentDescription = null,
            tint = when {
                !enabled -> InkGlassTokens.TextTertiary
                checked -> InkGlassTokens.Accent
                else -> InkGlassTokens.TextSecondary
            },
        )
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = InkTypography.Body,
            color = if (enabled) InkGlassTokens.TextPrimary else InkGlassTokens.TextTertiary,
        )
    }
}
