package eu.kanade.presentation.more.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkGlassButton
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.i18n.stringResource

internal class GuidesStep(
    private val onRestoreBackup: () -> Unit,
) : OnboardingStep {

    override val isComplete: Boolean = true

    @Composable
    override fun Content() {
        Column(verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space16)) {
            Text(
                stringResource(MR.strings.ml_g1_guides_ready),
                style = InkTypography.Title,
                color = InkGlassTokens.TextPrimary,
            )
            Text(
                stringResource(MR.strings.ml_g1_guides_intro),
                style = InkTypography.Body,
                color = InkGlassTokens.TextSecondary,
            )
            InkGlassSurface(
                modifier = Modifier.fillMaxWidth(),
                level = InkGlassLevel.Soft,
                contentPadding = PaddingValues(InkGlassTokens.Space16),
                shadowElevation = 0.dp,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8)) {
                    Text(
                        stringResource(MR.strings.ml_g1_guides_step_browse),
                        style = InkTypography.Body,
                        color = InkGlassTokens.TextPrimary,
                    )
                    Text(
                        stringResource(MR.strings.ml_g1_guides_step_library),
                        style = InkTypography.Body,
                        color = InkGlassTokens.TextPrimary,
                    )
                    Text(
                        stringResource(MR.strings.ml_g1_guides_step_read),
                        style = InkTypography.Body,
                        color = InkGlassTokens.TextPrimary,
                    )
                }
            }
            InkGlassButton(
                label = stringResource(MR.strings.ml_g1_guides_restore_backup),
                modifier = Modifier.fillMaxWidth(),
                onClick = onRestoreBackup,
            )
            Text(
                stringResource(MR.strings.ml_g1_guides_later_hint),
                style = InkTypography.BodySmall,
                color = InkGlassTokens.TextTertiary,
            )
        }
    }
}
