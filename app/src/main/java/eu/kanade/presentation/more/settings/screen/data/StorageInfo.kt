package eu.kanade.presentation.more.settings.screen.data

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.util.storage.DiskUtil
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassProgressBar
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.i18n.stringResource
import java.io.File

@Composable
fun StorageInfo(
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val storages = remember { DiskUtil.getExternalStorages(context) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
    ) {
        storages.forEach { StorageInfo(it) }
    }
}

@Composable
private fun StorageInfo(file: File) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val available = remember(file) { DiskUtil.getAvailableStorageSpace(file) }
    val availableText = remember(available) { Formatter.formatFileSize(context, available) }
    val total = remember(file) { DiskUtil.getTotalStorageSpace(file) }
    val totalText = remember(total) { Formatter.formatFileSize(context, total) }
    val usedFraction = remember(available, total) {
        if (total <= 0L) 0f else (1f - available.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }

    InkGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        level = InkGlassLevel.Soft,
        contentPadding = PaddingValues(InkGlassTokens.Space16),
        shadowElevation = androidx.compose.ui.unit.Dp.Hairline,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8)) {
            androidx.compose.material3.Text(
                text = file.absolutePath,
                style = InkTypography.TitleSmall,
                color = InkGlassTokens.TextPrimary,
            )
            InkGlassProgressBar(
                progress = usedFraction,
                modifier = Modifier.fillMaxWidth(),
            )
            androidx.compose.material3.Text(
                text = stringResource(MR.strings.available_disk_space_info, availableText, totalText),
                style = InkTypography.BodySmall,
                color = InkGlassTokens.TextSecondary,
            )
        }
    }
}
