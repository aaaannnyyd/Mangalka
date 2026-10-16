package eu.kanade.presentation.manga.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.kanade.presentation.util.SourceProblem
import eu.kanade.tachiyomi.ui.manga.ExtensionFix
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkAccentButton
import tachiyomi.presentation.core.components.InkCircularProgress
import tachiyomi.presentation.core.components.InkGlassButton
import tachiyomi.presentation.core.components.InkGlassIconButton
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.i18n.stringResource

/** Explains a broken source on manga details and offers the two fixes that usually work. */
@Composable
fun SourceProblemBanner(
    problem: SourceProblem,
    extensionFix: ExtensionFix,
    onUpdateExtension: () -> Unit,
    onOtherSource: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InkGlassSurface(modifier = modifier.fillMaxWidth(), level = InkGlassLevel.Soft) {
        Column(verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12),
            ) {
                Icon(
                    Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = InkGlassTokens.Warning,
                    modifier = Modifier.size(InkGlassTokens.Space24),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space4)) {
                    Text(problem.title, style = InkTypography.TitleSmall, color = InkGlassTokens.TextPrimary)
                    Text(
                        text = when (extensionFix) {
                            ExtensionFix.NoUpdate ->
                                stringResource(MR.strings.ml_g2_extension_no_update)
                            ExtensionFix.Failed ->
                                stringResource(MR.strings.ml_g2_extension_update_failed)
                            else -> problem.hint
                        },
                        style = InkTypography.BodySmall,
                        color = InkGlassTokens.TextSecondary,
                    )
                }
                InkGlassIconButton(Icons.Outlined.Close, stringResource(MR.strings.ml_g2_hide), onDismiss)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8)) {
                when {
                    extensionFix == ExtensionFix.Working -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
                    ) {
                        InkCircularProgress(Modifier.size(InkGlassTokens.Space20))
                        Text(
                            stringResource(MR.strings.ml_g2_extension_updating),
                            style = InkTypography.BodySmall,
                            color = InkGlassTokens.TextSecondary,
                        )
                    }
                    problem.kind.extensionUpdateMayHelp && extensionFix == ExtensionFix.Idle -> InkAccentButton(
                        stringResource(MR.strings.ml_g2_update_extension),
                        onUpdateExtension,
                        icon = Icons.Outlined.SystemUpdateAlt,
                    )
                }
                if (extensionFix != ExtensionFix.Working) {
                    InkGlassButton(
                        stringResource(MR.strings.ml_g2_other_source),
                        onOtherSource,
                        icon = Icons.Outlined.SwapHoriz,
                    )
                }
            }
        }
    }
}
