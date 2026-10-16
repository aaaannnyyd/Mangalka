package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkAlertDialog
import tachiyomi.presentation.core.components.InkGlassButton
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun <T> ListPreferenceWidget(
    value: T,
    title: String,
    subtitle: String?,
    icon: ImageVector?,
    entries: Map<out T, String>,
    onValueChange: (T) -> Unit,
) {
    var isDialogShown by remember { mutableStateOf(false) }

    TextPreferenceWidget(
        title = title,
        subtitle = subtitle,
        icon = icon,
        onPreferenceClick = { isDialogShown = true },
    )

    if (isDialogShown) {
        InkAlertDialog(
            onDismissRequest = { isDialogShown = false },
            title = {
                Text(
                    text = title,
                    style = InkTypography.Title,
                    color = InkGlassTokens.TextPrimary,
                )
            },
            text = {
                Box {
                    val state = rememberLazyListState()
                    ScrollbarLazyColumn(
                        state = state,
                        verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
                    ) {
                        entries.forEach { current ->
                            val isSelected = value == current.key
                            item {
                                DialogRow(
                                    label = current.value,
                                    isSelected = isSelected,
                                    onSelected = {
                                        onValueChange(current.key!!)
                                        isDialogShown = false
                                    },
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                InkGlassButton(
                    label = stringResource(MR.strings.action_cancel),
                    onClick = { isDialogShown = false },
                )
            },
        )
    }
}

@Composable
private fun DialogRow(
    label: String,
    isSelected: Boolean,
    onSelected: () -> Unit,
) {
    InkGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = { if (!isSelected) onSelected() },
            ),
        level = if (isSelected) InkGlassLevel.Accent else InkGlassLevel.Soft,
        shape = RoundedCornerShape(InkGlassTokens.RadiusSmall),
        contentPadding = PaddingValues(
            horizontal = InkGlassTokens.Space16,
            vertical = InkGlassTokens.Space12,
        ),
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12),
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = InkTypography.Body,
                color = if (isSelected) InkGlassTokens.Accent else InkGlassTokens.TextPrimary,
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = InkGlassTokens.Accent,
                )
            }
        }
    }
}
