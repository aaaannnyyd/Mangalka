package tachiyomi.presentation.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = InkGlassTokens.GlassSoft,
    contentColor: Color = InkGlassTokens.TextPrimary,
    style: TextStyle = InkTypography.BodySmall,
) {
    val shape = RoundedCornerShape(InkGlassTokens.RadiusChip)
    Box(
        modifier = modifier
            .padding(start = InkGlassTokens.Space4)
            .clip(shape)
            .background(color)
            .border(InkGlassTokens.BorderWidth, InkGlassTokens.GlassBorderSoft, shape)
            .padding(horizontal = 7.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            maxLines = 1,
            style = style,
            color = contentColor,
        )
    }
}

@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = InkGlassTokens.GlassSoft,
    contentColor: Color = InkGlassTokens.TextPrimary,
    fontSize: TextUnit = LocalTextStyle.current.fontSize,
) {
    Pill(
        text = text,
        modifier = modifier,
        color = color,
        contentColor = contentColor,
        style = InkTypography.BodySmall.merge(fontSize = fontSize),
    )
}
