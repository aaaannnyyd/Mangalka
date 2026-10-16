package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkAccentButton
import tachiyomi.presentation.core.components.InkAlertDialog
import tachiyomi.presentation.core.components.InkGlassButton
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun <T> MultiSelectListPreferenceWidget(
    values: Set<T>,
    title: String,
    subtitle: String?,
    icon: ImageVector?,
    entries: Map<out T, String>,
    onValuesChange: (Set<T>) -> Unit,
) {
    var isDialogShown by remember { mutableStateOf(false) }

    TextPreferenceWidget(
        title = title,
        subtitle = subtitle,
        icon = icon,
        onPreferenceClick = { isDialogShown = true },
    )

    if (isDialogShown) {
        val selected = remember {
            entries.keys.filter { values.contains(it) }.toMutableStateList()
        }
        InkAlertDialog(
            onDismissRequest = { isDialogShown = false },
            title = {
                Text(title, style = InkTypography.Title, color = InkGlassTokens.TextPrimary)
            },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8)) {
                    items(entries.entries.toList()) { current ->
                        val isSelected = selected.contains(current.key)
                        InkGlassSurface(
                            modifier = Modifier.fillMaxWidth(),
                            level = if (isSelected) InkGlassLevel.Accent else InkGlassLevel.Soft,
                            shape = RoundedCornerShape(InkGlassTokens.RadiusSmall),
                            contentPadding = PaddingValues(
                                horizontal = InkGlassTokens.Space16,
                                vertical = InkGlassTokens.Space12,
                            ),
                            shadowElevation = 0.dp,
                            onClick = {
                                if (isSelected) selected.remove(current.key) else selected.add(current.key)
                            },
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12),
                            ) {
                                Icon(
                                    imageVector = if (isSelected) {
                                        Icons.Outlined.CheckBox
                                    } else {
                                        Icons.Outlined.CheckBoxOutlineBlank
                                    },
                                    contentDescription = null,
                                    tint = if (isSelected) InkGlassTokens.Accent else InkGlassTokens.TextTertiary,
                                )
                                Text(
                                    current.value,
                                    modifier = Modifier.weight(1f),
                                    style = InkTypography.Body,
                                    color = InkGlassTokens.TextPrimary,
                                )
                            }
                        }
                    }
                }
            },
            properties = DialogProperties(usePlatformDefaultWidth = true),
            confirmButton = {
                InkAccentButton(
                    label = stringResource(MR.strings.action_ok),
                    onClick = {
                        onValuesChange(selected.toSet())
                        isDialogShown = false
                    },
                )
            },
            dismissButton = {
                InkGlassButton(
                    label = stringResource(MR.strings.action_cancel),
                    onClick = { isDialogShown = false },
                )
            },
        )
    }
}
