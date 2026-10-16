package mihon.feature.migration.config

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import eu.kanade.core.util.fastFilterNot
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.components.AdaptiveSheet
import mihon.domain.migration.models.MigrationFlag
import mihon.feature.common.utils.getLabel
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.core.common.preference.toggle
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkAccentButton
import tachiyomi.presentation.core.components.InkGlassChip
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.theme.active
import tachiyomi.presentation.core.theme.header
import tachiyomi.presentation.core.util.collectAsState

@Composable
fun MigrationConfigScreenSheet(
    preferences: SourcePreferences,
    onDismissRequest: () -> Unit,
    onStartMigration: (extraSearchQuery: String?) -> Unit,
) {
    var extraSearchQuery by rememberSaveable { mutableStateOf("") }
    val migrationFlags by preferences.migrationFlags.collectAsState()
    AdaptiveSheet(onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = MaterialTheme.padding.medium),
            ) {
                Text(
                    text = stringResource(MR.strings.migrationConfigScreen_dataToMigrateHeader),
                    style = MaterialTheme.typography.header,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MaterialTheme.padding.extraSmall)
                        .padding(horizontal = MaterialTheme.padding.medium),
                )
                Spacer(modifier = Modifier.height(MaterialTheme.padding.extraSmall))
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.padding.medium)
                        .padding(bottom = MaterialTheme.padding.extraSmall),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                ) {
                    MigrationFlag.entries
                        .fastFilterNot { it == MigrationFlag.REMOVE_DOWNLOAD }
                        .fastForEach { flag ->
                            val selected = flag in migrationFlags
                            InkGlassChip(
                                label = stringResource(flag.getLabel()),
                                selected = selected,
                                onClick = {
                                    preferences.migrationFlags.getAndSet { currentFlags ->
                                        if (flag in currentFlags) {
                                            currentFlags - flag
                                        } else {
                                            currentFlags + flag
                                        }
                                    }
                                },
                                leadingIcon = Icons.Outlined.Check,
                            )
                        }
                }
                val removeDownloads = MigrationFlag.REMOVE_DOWNLOAD in migrationFlags
                MigrationSheetSwitchItem(
                    title = stringResource(MR.strings.migrationConfigScreen_removeDownloadsTitle),
                    subtitle = null,
                    checked = removeDownloads,
                    onClick = {
                        preferences.migrationFlags.getAndSet {
                            if (removeDownloads) {
                                it - MigrationFlag.REMOVE_DOWNLOAD
                            } else {
                                it + MigrationFlag.REMOVE_DOWNLOAD
                            }
                        }
                    },
                )
                MigrationSheetDividerItem()
                InkGlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = InkGlassTokens.ScreenHorizontal,
                            vertical = InkGlassTokens.Space4,
                        ),
                    level = InkGlassLevel.Soft,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = InkGlassTokens.Space16,
                        vertical = InkGlassTokens.Space12,
                    ),
                    shadowElevation = 0.dp,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space4)) {
                        Text(
                            text = stringResource(
                                MR.strings.migrationConfigScreen_additionalSearchQueryLabel,
                            ).uppercase(),
                            style = InkTypography.Eyebrow,
                            color = InkGlassTokens.TextTertiary,
                        )
                        BasicTextField(
                            value = extraSearchQuery,
                            onValueChange = { extraSearchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = InkTypography.Body.copy(color = InkGlassTokens.TextPrimary),
                            cursorBrush = SolidColor(InkGlassTokens.Accent),
                        )
                        Text(
                            text = stringResource(MR.strings.migrationConfigScreen_additionalSearchQuerySupportingText),
                            style = InkTypography.BodySmall,
                            color = InkGlassTokens.TextTertiary,
                        )
                    }
                }
                MigrationSheetSwitchItem(
                    title = stringResource(MR.strings.migrationConfigScreen_hideUnmatchedTitle),
                    subtitle = null,
                    preference = preferences.migrationHideUnmatched,
                )
                MigrationSheetSwitchItem(
                    title = stringResource(MR.strings.migrationConfigScreen_hideWithoutUpdatesTitle),
                    subtitle = stringResource(MR.strings.migrationConfigScreen_hideWithoutUpdatesSubtitle),
                    preference = preferences.migrationHideWithoutUpdates,
                )
                MigrationSheetDividerItem()
                MigrationSheetWarningItem(stringResource(MR.strings.migrationConfigScreen_enhancedOptionsWarning))
                MigrationSheetSwitchItem(
                    title = stringResource(MR.strings.migrationConfigScreen_deepSearchModeTitle),
                    subtitle = stringResource(MR.strings.migrationConfigScreen_deepSearchModeSubtitle),
                    preference = preferences.migrationDeepSearchMode,
                )
                MigrationSheetSwitchItem(
                    title = stringResource(MR.strings.migrationConfigScreen_prioritizeByChaptersTitle),
                    subtitle = stringResource(MR.strings.migrationConfigScreen_prioritizeByChaptersSubtitle),
                    preference = preferences.migrationPrioritizeByChapters,
                )
            }
            InkAccentButton(
                label = stringResource(MR.strings.migrationConfigScreen_continueButtonText),
                onClick = {
                    val cleanedExtraSearchQuery = extraSearchQuery.trim().ifBlank { null }
                    onStartMigration(cleanedExtraSearchQuery)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = InkGlassTokens.ScreenHorizontal,
                        vertical = InkGlassTokens.Space12,
                    ),
            )
        }
    }
}

@Composable
private fun MigrationSheetSwitchItem(
    title: String,
    subtitle: String?,
    preference: Preference<Boolean>,
) {
    MigrationSheetSwitchItem(
        title = title,
        subtitle = subtitle,
        checked = preference.collectAsState().value,
        onClick = { preference.toggle() },
    )
}

@Composable
private fun MigrationSheetSwitchItem(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onClick: () -> Unit,
) {
    InkGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = InkGlassTokens.ScreenHorizontal, vertical = InkGlassTokens.Space4),
        level = if (checked) InkGlassLevel.Accent else InkGlassLevel.Soft,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = InkGlassTokens.Space16,
            vertical = InkGlassTokens.Space12,
        ),
        shadowElevation = 0.dp,
        onClick = onClick,
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12),
        ) {
            Column(Modifier.weight(1f)) {
                Text(text = title, style = InkTypography.TitleSmall, color = InkGlassTokens.TextPrimary)
                subtitle?.let {
                    Text(text = it, style = InkTypography.BodySmall, color = InkGlassTokens.TextSecondary)
                }
            }
            InkGlassChip(
                label = if (checked) {
                    stringResource(
                        MR.strings.ml_g3_chip_on,
                    )
                } else {
                    stringResource(MR.strings.ml_g3_chip_off)
                },
                selected = checked,
                onClick = onClick,
            )
        }
    }
}

@Composable
private fun MigrationSheetDividerItem() {
    Spacer(modifier = Modifier.height(InkGlassTokens.Space8))
}

@Composable
private fun MigrationSheetWarningItem(
    text: String,
) {
    InkGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = InkGlassTokens.ScreenHorizontal, vertical = InkGlassTokens.Space4),
        level = InkGlassLevel.Accent,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(InkGlassTokens.Space16),
        shadowElevation = 0.dp,
    ) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12),
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = InkGlassTokens.Accent,
            )
            Text(
                text = text,
                style = InkTypography.BodySmall,
                color = InkGlassTokens.TextSecondary,
            )
        }
    }
}
