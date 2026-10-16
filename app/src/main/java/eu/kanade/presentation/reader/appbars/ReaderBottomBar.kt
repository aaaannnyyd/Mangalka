package eu.kanade.presentation.reader.appbars

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkGlassIconButton
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ReaderBottomBar(
    readingMode: ReadingMode,
    onClickReadingMode: () -> Unit,
    orientation: ReaderOrientation,
    onClickOrientation: () -> Unit,
    cropEnabled: Boolean,
    onClickCropBorder: () -> Unit,
    onClickSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InkGlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {},
        level = InkGlassLevel.Strong,
        shape = RoundedCornerShape(InkGlassTokens.RadiusLargeCard),
        contentPadding = PaddingValues(horizontal = InkGlassTokens.Space12, vertical = InkGlassTokens.Space8),
        shadowElevation = InkGlassTokens.Space8,
        prominent = true,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReaderPainterButton(
                painter = painterResource(readingMode.iconRes),
                contentDescription = stringResource(MR.strings.viewer),
                onClick = onClickReadingMode,
            )

            InkGlassIconButton(
                imageVector = orientation.icon,
                contentDescription = stringResource(MR.strings.rotation_type),
                onClick = onClickOrientation,
                modifier = Modifier.size(46.dp),
            )

            ReaderPainterButton(
                painter = painterResource(if (cropEnabled) R.drawable.ic_crop_24dp else R.drawable.ic_crop_off_24dp),
                contentDescription = stringResource(MR.strings.pref_crop_borders),
                selected = cropEnabled,
                onClick = onClickCropBorder,
            )

            InkGlassIconButton(
                imageVector = Icons.Outlined.Settings,
                contentDescription = stringResource(MR.strings.action_settings),
                onClick = onClickSettings,
                modifier = Modifier.size(46.dp),
            )
        }
    }
}

@Composable
private fun ReaderPainterButton(
    painter: Painter,
    contentDescription: String?,
    onClick: () -> Unit,
    selected: Boolean = false,
) {
    InkGlassSurface(
        modifier = Modifier.size(46.dp),
        level = if (selected) InkGlassLevel.Accent else InkGlassLevel.Strong,
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
        shadowElevation = 0.dp,
        onClick = onClick,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                painter = painter,
                contentDescription = contentDescription,
                modifier = Modifier.size(21.dp),
                tint = if (selected) InkGlassTokens.Accent else InkGlassTokens.TextPrimary,
            )
        }
    }
}
