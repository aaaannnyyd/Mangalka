package eu.kanade.presentation.reader.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkAccentButton
import tachiyomi.presentation.core.components.InkGlassButton
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ModeSelectionDialog(
    onApply: () -> Unit,
    onUseDefault: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    InkGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = InkGlassTokens.Space12, vertical = InkGlassTokens.Space12),
        level = InkGlassLevel.Strong,
        contentPadding = PaddingValues(vertical = InkGlassTokens.Space16),
        shadowElevation = InkGlassTokens.Space8,
        prominent = true,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space16)) {
            content()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = InkGlassTokens.Space16),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
            ) {
                onUseDefault?.let {
                    InkGlassButton(
                        label = stringResource(MR.strings.action_revert_to_default),
                        onClick = it,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                InkAccentButton(
                    label = stringResource(MR.strings.action_apply),
                    onClick = onApply,
                    icon = Icons.Outlined.Check,
                )
            }
        }
    }
}
