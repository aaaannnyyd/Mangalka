package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkAccentButton
import tachiyomi.presentation.core.components.InkAlertDialog
import tachiyomi.presentation.core.components.InkGlassButton
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.i18n.stringResource

private enum class State {
    CHECKED,
    INVERSED,
    UNCHECKED,
}

@Composable
fun <T> TriStateListDialog(
    title: String,
    message: String? = null,
    items: List<T>,
    initialChecked: List<T>,
    initialInversed: List<T>,
    itemLabel: @Composable (T) -> String,
    onDismissRequest: () -> Unit,
    onValueChanged: (newIncluded: List<T>, newExcluded: List<T>) -> Unit,
) {
    val selected = remember {
        items.map {
            when (it) {
                in initialChecked -> State.CHECKED
                in initialInversed -> State.INVERSED
                else -> State.UNCHECKED
            }
        }.toMutableStateList()
    }

    InkAlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(title, style = InkTypography.Title, color = InkGlassTokens.TextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12)) {
                message?.let {
                    Text(it, style = InkTypography.BodySmall, color = InkGlassTokens.TextSecondary)
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8)) {
                    itemsIndexed(items = items) { index, item ->
                        val state = selected[index]
                        val active = state != State.UNCHECKED
                        InkGlassSurface(
                            modifier = Modifier.fillMaxWidth(),
                            level = if (active) InkGlassLevel.Accent else InkGlassLevel.Soft,
                            shape = RoundedCornerShape(InkGlassTokens.RadiusSmall),
                            contentPadding = PaddingValues(
                                horizontal = InkGlassTokens.Space16,
                                vertical = InkGlassTokens.Space12,
                            ),
                            shadowElevation = 0.dp,
                            onClick = {
                                selected[index] = when (state) {
                                    State.UNCHECKED -> State.CHECKED
                                    State.CHECKED -> State.INVERSED
                                    State.INVERSED -> State.UNCHECKED
                                }
                            },
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12),
                            ) {
                                Icon(
                                    imageVector = when (state) {
                                        State.UNCHECKED -> Icons.Outlined.CheckBoxOutlineBlank
                                        State.CHECKED -> Icons.Outlined.CheckBox
                                        State.INVERSED -> Icons.Outlined.RemoveCircleOutline
                                    },
                                    contentDescription = null,
                                    tint = if (active) InkGlassTokens.Accent else InkGlassTokens.TextTertiary,
                                )
                                Text(
                                    text = itemLabel(item),
                                    modifier = Modifier.weight(1f),
                                    style = InkTypography.Body,
                                    color = InkGlassTokens.TextPrimary,
                                )
                                if (state == State.INVERSED) {
                                    Text(
                                        stringResource(MR.strings.ml_g1_exclude_badge),
                                        style = InkTypography.Eyebrow,
                                        color = InkGlassTokens.Accent,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        dismissButton = {
            InkGlassButton(
                label = stringResource(MR.strings.action_cancel),
                onClick = onDismissRequest,
            )
        },
        confirmButton = {
            InkAccentButton(
                label = stringResource(MR.strings.action_ok),
                onClick = {
                    val included = items.mapIndexedNotNull { index, category ->
                        if (selected[index] == State.CHECKED) category else null
                    }
                    val excluded = items.mapIndexedNotNull { index, category ->
                        if (selected[index] == State.INVERSED) category else null
                    }
                    onValueChanged(included, excluded)
                },
            )
        },
    )
}
