package eu.kanade.presentation.more.settings.screen.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
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

@Composable
fun ExtensionStoreCreateDialog(
    onDismissRequest: () -> Unit,
    onCreate: (String) -> Unit,
    storeIndexUrls: Set<String>,
    processing: Boolean,
    errorMessage: String?,
) {
    var value by remember { mutableStateOf("") }
    val storeAlreadyExists = value in storeIndexUrls
    val focusRequester = remember { FocusRequester() }

    InkAlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = stringResource(MR.strings.extensionStoresScreen_addStore_title),
                style = InkTypography.Title,
                color = InkGlassTokens.TextPrimary,
            )
        },
        text = {
            StoreUrlField(
                value = value,
                onValueChange = { value = it },
                label = stringResource(MR.strings.extensionStoresScreen_addStoreInput_inputLabel),
                supportingText = errorMessage ?: stringResource(
                    if (storeAlreadyExists) {
                        MR.strings.extensionStoresScreen_addStore_alreadyExists
                    } else {
                        MR.strings.information_required_plain
                    },
                ),
                isError = errorMessage != null || storeAlreadyExists,
                modifier = Modifier.focusRequester(focusRequester),
            )
        },
        confirmButton = {
            InkAccentButton(
                label = stringResource(
                    if (processing) MR.strings.extensionStoresScreen_addStore_processing else MR.strings.action_add,
                ),
                onClick = { onCreate(value) },
                enabled = !processing && value.isNotEmpty() && !storeAlreadyExists,
            )
        },
        dismissButton = {
            InkGlassButton(stringResource(MR.strings.action_cancel), onDismissRequest)
        },
    )

    LaunchedEffect(focusRequester) { focusRequester.requestFocus() }
}

@Composable
fun ExtensionStoreDeleteDialog(
    onDismissRequest: () -> Unit,
    onDelete: () -> Unit,
    storeName: String,
    storeIndexUrl: String,
) {
    InkAlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = stringResource(MR.strings.extensionStoresScreen_deleteStore_title),
                style = InkTypography.Title,
            )
        },
        text = {
            Text(
                text = stringResource(MR.strings.extensionStoresScreen_deleteStore_body, storeName, storeIndexUrl),
                style = InkTypography.Body,
                color = InkGlassTokens.TextSecondary,
            )
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
        dismissButton = {
            InkGlassButton(stringResource(MR.strings.action_cancel), onDismissRequest)
        },
    )
}

@Composable
fun ExtensionStoreConfirmDialog(
    onDismissRequest: () -> Unit,
    onCreate: () -> Unit,
    storeIndexUrl: String,
    storeAlreadyExists: Boolean,
    processing: Boolean,
    errorMessage: String?,
) {
    InkAlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = stringResource(MR.strings.extensionStoresScreen_addStore_title),
                style = InkTypography.Title,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12)) {
                Text(
                    text = stringResource(MR.strings.extensionStoresScreen_addStoreDeeplink_bodyText),
                    style = InkTypography.Body,
                    color = InkGlassTokens.TextSecondary,
                )
                StoreUrlField(
                    value = storeIndexUrl,
                    onValueChange = {},
                    label = stringResource(MR.strings.extensionStoresScreen_addStoreInput_inputLabel),
                    supportingText = when {
                        storeAlreadyExists -> stringResource(MR.strings.extensionStoresScreen_addStore_alreadyExists)
                        errorMessage != null -> errorMessage
                        else -> null
                    },
                    isError = errorMessage != null || storeAlreadyExists,
                    readOnly = true,
                )
            }
        },
        confirmButton = {
            InkAccentButton(
                label = stringResource(
                    if (processing) MR.strings.extensionStoresScreen_addStore_processing else MR.strings.action_add,
                ),
                onClick = onCreate,
                enabled = !storeAlreadyExists && !processing,
            )
        },
        dismissButton = {
            InkGlassButton(stringResource(MR.strings.action_cancel), onDismissRequest)
        },
    )
}

@Composable
private fun StoreUrlField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    supportingText: String?,
    isError: Boolean,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
) {
    InkGlassSurface(
        modifier = modifier.fillMaxWidth(),
        level = if (isError) InkGlassLevel.Accent else InkGlassLevel.Soft,
        shape = RoundedCornerShape(InkGlassTokens.RadiusSmall),
        contentPadding = PaddingValues(InkGlassTokens.Space16),
        shadowElevation = 0.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8)) {
            Text(
                label.uppercase(),
                style = InkTypography.Eyebrow,
                color = if (isError) InkGlassTokens.Accent else InkGlassTokens.TextTertiary,
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                readOnly = readOnly,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = InkTypography.Body.copy(color = InkGlassTokens.TextPrimary),
                cursorBrush = SolidColor(InkGlassTokens.Accent),
            )
            supportingText?.let {
                Text(
                    it,
                    style = InkTypography.BodySmall,
                    color = if (isError) InkGlassTokens.Accent else InkGlassTokens.TextTertiary,
                )
            }
        }
    }
}
