package eu.kanade.presentation.more.onboarding

import android.content.ActivityNotFoundException
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.more.settings.screen.SettingsDataScreen
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.collectLatest
import mihon.app.di.appGraph
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkAccentButton
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.i18n.stringResource

internal class StorageStep : OnboardingStep {

    private var _isComplete by mutableStateOf(false)

    override val isComplete: Boolean
        get() = _isComplete

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val storagePref = remember { context.appGraph.storagePreferences.baseStorageDirectory }
        val pickStorageLocation = SettingsDataScreen.storageLocationPicker(storagePref)
        val selected = SettingsDataScreen.storageLocationText(storagePref)

        Column(
            modifier = Modifier.padding(InkGlassTokens.Space4),
            verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space16),
        ) {
            Text(
                stringResource(MR.strings.ml_g4_onboarding_storage_title),
                style = InkTypography.Title,
                color = InkGlassTokens.TextPrimary,
            )
            Text(
                stringResource(MR.strings.ml_g4_onboarding_storage_summary),
                style = InkTypography.Body,
                color = InkGlassTokens.TextSecondary,
            )
            Text(
                stringResource(MR.strings.ml_g4_onboarding_storage_current, selected),
                style = InkTypography.BodySmall,
                color = if (_isComplete) InkGlassTokens.TextSecondary else InkGlassTokens.TextTertiary,
            )
            InkAccentButton(
                label = stringResource(MR.strings.ml_g4_onboarding_storage_pick),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    try {
                        pickStorageLocation.launch(null)
                    } catch (e: ActivityNotFoundException) {
                        context.toast(MR.strings.file_picker_error)
                    }
                },
            )
            Text(
                stringResource(MR.strings.ml_g4_onboarding_storage_later),
                style = InkTypography.BodySmall,
                color = InkGlassTokens.TextTertiary,
            )
        }

        LaunchedEffect(Unit) {
            storagePref.changes().collectLatest { _isComplete = storagePref.isSet() }
        }
    }
}
