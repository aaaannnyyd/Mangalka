package eu.kanade.presentation.more.settings.widget

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Preview
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import tachiyomi.presentation.core.components.InkGlassTokens

@Composable
fun SwitchPreferenceWidget(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    checked: Boolean = false,
    onCheckedChanged: (Boolean) -> Unit,
) {
    TextPreferenceWidget(
        modifier = modifier,
        title = title,
        subtitle = subtitle,
        icon = icon,
        widget = {
            InkPreferenceSwitch(
                checked = checked,
                modifier = Modifier.padding(start = TrailingWidgetBuffer),
            )
        },
        onPreferenceClick = { onCheckedChanged(!checked) },
    )
}

@Composable
fun InkPreferenceSwitch(
    checked: Boolean,
    modifier: Modifier = Modifier,
) {
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 2.dp,
        label = "inkPreferenceSwitchThumb",
    )
    val trackShape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .size(width = 44.dp, height = 26.dp)
            .background(
                if (checked) InkGlassTokens.AccentSoft else InkGlassTokens.GlassSoft,
                trackShape,
            )
            .border(
                InkGlassTokens.BorderWidth,
                if (checked) InkGlassTokens.AccentBorder else InkGlassTokens.GlassBorder,
                trackShape,
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(22.dp)
                .background(
                    if (checked) InkGlassTokens.Accent else InkGlassTokens.TextSecondary,
                    CircleShape,
                ),
        )
    }
}

@PreviewLightDark
@Composable
private fun SwitchPreferenceWidgetPreview() {
    TachiyomiPreviewTheme {
        Column {
            SwitchPreferenceWidget(
                title = "Text preference with icon",
                subtitle = "Text preference summary",
                icon = Icons.Filled.Preview,
                checked = true,
                onCheckedChanged = {},
            )
            SwitchPreferenceWidget(
                title = "Text preference",
                subtitle = "Text preference summary",
                checked = false,
                onCheckedChanged = {},
            )
        }
    }
}
