package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkAccentButton
import tachiyomi.presentation.core.components.InkAlertDialog
import tachiyomi.presentation.core.components.InkGlassButton
import tachiyomi.presentation.core.components.InkGlassIconButton
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun EditTextPreferenceWidget(
    title: String,
    subtitle: String?,
    icon: ImageVector?,
    value: String,
    onConfirm: suspend (String) -> Boolean,
) {
    var isDialogShown by remember { mutableStateOf(false) }

    TextPreferenceWidget(
        title = title,
        subtitle = subtitle?.format(value),
        icon = icon,
        onPreferenceClick = { isDialogShown = true },
    )

    if (isDialogShown) {
        val scope = rememberCoroutineScope()
        val onDismissRequest = { isDialogShown = false }
        var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
            mutableStateOf(TextFieldValue(value))
        }
        val isValid = textFieldValue.text.isNotBlank()

        InkAlertDialog(
            onDismissRequest = onDismissRequest,
            title = {
                Text(
                    text = title,
                    style = InkTypography.Title,
                    color = InkGlassTokens.TextPrimary,
                )
            },
            text = {
                InkGlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    level = if (isValid) InkGlassLevel.Soft else InkGlassLevel.Accent,
                    contentPadding = PaddingValues(
                        start = InkGlassTokens.Space16,
                        end = InkGlassTokens.Space8,
                        top = InkGlassTokens.Space12,
                        bottom = InkGlassTokens.Space12,
                    ),
                    shadowElevation = 0.dp,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
                    ) {
                        BasicTextField(
                            value = textFieldValue,
                            onValueChange = { textFieldValue = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = InkTypography.Body.copy(color = InkGlassTokens.TextPrimary),
                            cursorBrush = SolidColor(InkGlassTokens.Accent),
                            decorationBox = { inner ->
                                if (textFieldValue.text.isEmpty()) {
                                    Text(
                                        text = stringResource(MR.strings.ml_g2_enter_value),
                                        style = InkTypography.Body,
                                        color = InkGlassTokens.TextTertiary,
                                    )
                                }
                                inner()
                            },
                        )
                        if (textFieldValue.text.isNotEmpty()) {
                            InkGlassIconButton(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = stringResource(MR.strings.ml_g2_clear),
                                onClick = { textFieldValue = TextFieldValue("") },
                            )
                        }
                    }
                }
            },
            properties = DialogProperties(usePlatformDefaultWidth = true),
            confirmButton = {
                InkAccentButton(
                    label = stringResource(MR.strings.action_ok),
                    enabled = textFieldValue.text != value && isValid,
                    onClick = {
                        scope.launch {
                            if (onConfirm(textFieldValue.text)) onDismissRequest()
                        }
                    },
                )
            },
            dismissButton = {
                InkGlassButton(
                    label = stringResource(MR.strings.action_cancel),
                    onClick = onDismissRequest,
                )
            },
        )
    }
}
