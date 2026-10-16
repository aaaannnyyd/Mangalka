package eu.kanade.presentation.library

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.icerock.moko.resources.StringResource
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkAccentButton
import tachiyomi.presentation.core.components.InkAlertDialog
import tachiyomi.presentation.core.components.InkGlassButton
import tachiyomi.presentation.core.components.LabeledCheckbox
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun DeleteLibraryMangaDialog(
    containsLocalManga: Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: (Boolean, Boolean) -> Unit,
) {
    var list by remember {
        mutableStateOf(
            buildList<CheckboxState.State<StringResource>> {
                add(CheckboxState.State.None(MR.strings.manga_from_library))
                if (!containsLocalManga) {
                    add(CheckboxState.State.None(MR.strings.downloaded_chapters))
                }
            },
        )
    }
    InkAlertDialog(
        onDismissRequest = onDismissRequest,
        dismissButton = {
            InkGlassButton(stringResource(MR.strings.action_cancel), onDismissRequest)
        },
        confirmButton = {
            InkAccentButton(
                label = stringResource(MR.strings.action_ok),
                enabled = list.any { it.isChecked },
                onClick = {
                    onDismissRequest()
                    onConfirm(
                        list[0].isChecked,
                        list.getOrElse(1) { CheckboxState.State.None(0) }.isChecked,
                    )
                },
            )
        },
        title = {
            Text(text = stringResource(MR.strings.action_remove))
        },
        text = {
            Column {
                list.forEach { state ->
                    LabeledCheckbox(
                        label = stringResource(state.value),
                        checked = state.isChecked,
                        onCheckedChange = {
                            val index = list.indexOf(state)
                            if (index != -1) {
                                val mutableList = list.toMutableList()
                                mutableList[index] = state.next() as CheckboxState.State<StringResource>
                                list = mutableList.toList()
                            }
                        },
                    )
                }
            }
        },
    )
}
