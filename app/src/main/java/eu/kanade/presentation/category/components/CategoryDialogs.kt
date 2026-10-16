package eu.kanade.presentation.category.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.category.visualName
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.domain.category.model.Category
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
fun CategoryCreateDialog(
    onDismissRequest: () -> Unit,
    onCreate: (String) -> Unit,
    categories: List<String>,
) {
    var name by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val nameAlreadyExists = remember(name, categories) { categories.contains(name) }

    CategoryNameDialog(
        title = stringResource(MR.strings.action_add_category),
        value = name,
        onValueChange = { name = it },
        isError = name.isNotEmpty() && nameAlreadyExists,
        supportingText = stringResource(
            if (name.isNotEmpty() &&
                nameAlreadyExists
            ) {
                MR.strings.error_category_exists
            } else {
                MR.strings.information_required_plain
            },
        ),
        confirmLabel = stringResource(MR.strings.action_add),
        confirmEnabled = name.isNotEmpty() && !nameAlreadyExists,
        focusRequester = focusRequester,
        onConfirm = {
            onCreate(name)
            onDismissRequest()
        },
        onDismissRequest = onDismissRequest,
    )

    LaunchedEffect(focusRequester) { focusRequester.requestFocus() }
}

@Composable
fun CategoryRenameDialog(
    onDismissRequest: () -> Unit,
    onRename: (String) -> Unit,
    categories: List<String>,
    category: String,
) {
    var name by remember { mutableStateOf(category) }
    val focusRequester = remember { FocusRequester() }
    val valueHasChanged = name != category
    val nameAlreadyExists = remember(name, categories) { categories.contains(name) }

    CategoryNameDialog(
        title = stringResource(MR.strings.action_rename_category),
        value = name,
        onValueChange = { name = it },
        isError = valueHasChanged && nameAlreadyExists,
        supportingText = stringResource(
            if (valueHasChanged &&
                nameAlreadyExists
            ) {
                MR.strings.error_category_exists
            } else {
                MR.strings.information_required_plain
            },
        ),
        confirmLabel = stringResource(MR.strings.action_ok),
        confirmEnabled = valueHasChanged && name.isNotBlank() && !nameAlreadyExists,
        focusRequester = focusRequester,
        onConfirm = {
            onRename(name)
            onDismissRequest()
        },
        onDismissRequest = onDismissRequest,
    )

    LaunchedEffect(focusRequester) { focusRequester.requestFocus() }
}

@Composable
private fun CategoryNameDialog(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    supportingText: String,
    confirmLabel: String,
    confirmEnabled: Boolean,
    focusRequester: FocusRequester,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    InkAlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title, style = InkTypography.Title, color = InkGlassTokens.TextPrimary) },
        text = {
            InkGlassSurface(
                modifier = Modifier.fillMaxWidth(),
                level = if (isError) InkGlassLevel.Accent else InkGlassLevel.Soft,
                shape = RoundedCornerShape(InkGlassTokens.RadiusSmall),
                contentPadding = PaddingValues(InkGlassTokens.Space16),
                shadowElevation = InkGlassTokens.Space4,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8)) {
                    Text(
                        stringResource(MR.strings.name).uppercase(),
                        style = InkTypography.Eyebrow,
                        color = if (isError) InkGlassTokens.Accent else InkGlassTokens.TextTertiary,
                    )
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        textStyle = InkTypography.Body.copy(color = InkGlassTokens.TextPrimary),
                        cursorBrush = SolidColor(InkGlassTokens.Accent),
                    )
                    Text(
                        supportingText,
                        style = InkTypography.BodySmall,
                        color = if (isError) InkGlassTokens.Accent else InkGlassTokens.TextTertiary,
                    )
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
                label = confirmLabel,
                enabled = confirmEnabled,
                onClick = onConfirm,
            )
        },
    )
}

@Composable
fun CategoryDeleteDialog(
    onDismissRequest: () -> Unit,
    onDelete: () -> Unit,
    category: String,
) {
    InkAlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(MR.strings.delete_category), style = InkTypography.Title) },
        text = {
            Text(
                text = stringResource(MR.strings.delete_category_confirmation, category),
                style = InkTypography.Body,
                color = InkGlassTokens.TextSecondary,
            )
        },
        dismissButton = {
            InkGlassButton(stringResource(MR.strings.action_cancel), onDismissRequest)
        },
        confirmButton = {
            InkAccentButton(
                label = stringResource(MR.strings.action_ok),
                onClick = {
                    onDelete()
                    onDismissRequest()
                },
            )
        },
    )
}

@Composable
fun ChangeCategoryDialog(
    initialSelection: List<CheckboxState<Category>>,
    onDismissRequest: () -> Unit,
    onEditCategories: () -> Unit,
    onConfirm: (List<Long>, List<Long>) -> Unit,
) {
    if (initialSelection.isEmpty()) {
        InkAlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(stringResource(MR.strings.action_move_category), style = InkTypography.Title) },
            text = {
                Text(
                    stringResource(MR.strings.information_empty_category_dialog),
                    style = InkTypography.Body,
                    color = InkGlassTokens.TextSecondary,
                )
            },
            confirmButton = {
                InkAccentButton(
                    label = stringResource(MR.strings.action_edit_categories),
                    onClick = {
                        onDismissRequest()
                        onEditCategories()
                    },
                )
            },
        )
        return
    }

    var selection by remember { mutableStateOf(initialSelection) }
    InkAlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(MR.strings.action_move_category), style = InkTypography.Title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
            ) {
                selection.forEach { checkbox ->
                    val index = selection.indexOf(checkbox)
                    CategorySelectionRow(
                        checkbox = checkbox,
                        onClick = {
                            if (index >= 0) {
                                val next = selection.toMutableList()
                                next[index] = checkbox.next()
                                selection = next
                            }
                        },
                    )
                }
            }
        },
        dismissButton = {
            InkGlassButton(
                label = stringResource(MR.strings.action_edit),
                onClick = {
                    onDismissRequest()
                    onEditCategories()
                },
            )
        },
        confirmButton = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
            ) {
                InkGlassButton(stringResource(MR.strings.action_cancel), onDismissRequest)
                InkAccentButton(
                    label = stringResource(MR.strings.action_ok),
                    onClick = {
                        onDismissRequest()
                        onConfirm(
                            selection
                                .filter { it is CheckboxState.State.Checked || it is CheckboxState.TriState.Include }
                                .map { it.value.id },
                            selection
                                .filter { it is CheckboxState.State.None || it is CheckboxState.TriState.None }
                                .map { it.value.id },
                        )
                    },
                )
            }
        },
    )
}

@Composable
private fun CategorySelectionRow(
    checkbox: CheckboxState<Category>,
    onClick: () -> Unit,
) {
    val included = checkbox is CheckboxState.State.Checked || checkbox is CheckboxState.TriState.Include
    val excluded = checkbox is CheckboxState.TriState.Exclude
    InkGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        level = if (included || excluded) InkGlassLevel.Accent else InkGlassLevel.Soft,
        shape = RoundedCornerShape(InkGlassTokens.RadiusSmall),
        contentPadding = PaddingValues(horizontal = InkGlassTokens.Space16, vertical = InkGlassTokens.Space12),
        shadowElevation = 0.dp,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12),
        ) {
            Icon(
                imageVector = when {
                    excluded -> Icons.Outlined.RemoveCircleOutline
                    included -> Icons.Outlined.CheckBox
                    else -> Icons.Outlined.CheckBoxOutlineBlank
                },
                contentDescription = null,
                tint = if (included || excluded) InkGlassTokens.Accent else InkGlassTokens.TextTertiary,
            )
            Text(
                checkbox.value.visualName,
                modifier = Modifier.weight(1f),
                style = InkTypography.Body,
                color = InkGlassTokens.TextPrimary,
            )
            if (excluded) {
                Text("ИСКЛЮЧИТЬ", style = InkTypography.Eyebrow, color = InkGlassTokens.Accent)
            }
        }
    }
}
