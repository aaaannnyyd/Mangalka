package mihon.feature.migration.list.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.DialogProperties
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkAlertDialog
import tachiyomi.presentation.core.components.InkGlassProgressBar
import tachiyomi.presentation.core.components.material.TextButton
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun MigrationProgressDialog(
    progress: Float,
    exitMigration: () -> Unit,
) {
    InkAlertDialog(
        onDismissRequest = {},
        confirmButton = {
            TextButton(onClick = exitMigration) {
                Text(text = stringResource(MR.strings.migrationListScreen_progressDialog_cancelLabel))
            }
        },
        text = {
            if (!progress.isNaN()) {
                val progressAnimated by animateFloatAsState(
                    targetValue = progress,
                    label = "migration_progress",
                )
                InkGlassProgressBar(progress = progressAnimated)
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    )
}
