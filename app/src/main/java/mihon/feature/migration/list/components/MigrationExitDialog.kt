package mihon.feature.migration.list.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkAlertDialog
import tachiyomi.presentation.core.components.material.TextButton
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun MigrationExitDialog(
    onDismissRequest: () -> Unit,
    exitMigration: () -> Unit,
) {
    InkAlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(MR.strings.migrationListScreen_exitDialogTitle))
        },
        confirmButton = {
            TextButton(onClick = exitMigration) {
                Text(text = stringResource(MR.strings.migrationListScreen_exitDialog_stopLabel))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.migrationListScreen_exitDialog_cancelLabel))
            }
        },
    )
}
