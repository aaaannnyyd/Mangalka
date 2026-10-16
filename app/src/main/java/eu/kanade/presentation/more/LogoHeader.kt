package eu.kanade.presentation.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.R
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkTypography

@Composable
fun LogoHeader(
    iconPadding: PaddingValues = PaddingValues(),
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(iconPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12),
    ) {
        InkGlassSurface(
            modifier = Modifier.size(112.dp),
            level = InkGlassLevel.Strong,
            shape = RoundedCornerShape(InkGlassTokens.RadiusLargeCard),
            contentPadding = PaddingValues(0.dp),
            shadowElevation = InkGlassTokens.Space12,
            prominent = true,
        ) {
            Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                // ic_mangalka_splash is a layer-list, which painterResource cannot load (it crashed
                // the About screen). Use the vector foreground directly; its artwork sits in the
                // central 2/3 of the adaptive-icon canvas, so it fills the whole tile.
                Icon(
                    painter = painterResource(R.drawable.ic_mangalka_launcher_foreground),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.matchParentSize(),
                )
            }
        }
        Text(
            text = "Мангалка",
            style = InkTypography.Headline,
            color = InkGlassTokens.TextPrimary,
        )
        Text(
            text = "Манга без визуального шума",
            style = InkTypography.BodySmall,
            color = InkGlassTokens.TextSecondary,
        )
    }
}
