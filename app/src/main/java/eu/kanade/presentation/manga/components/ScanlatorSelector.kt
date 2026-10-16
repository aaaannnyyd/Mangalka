package eu.kanade.presentation.manga.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkGlassDropdownMenu
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ScanlatorSelector(
    availableScanlators: Set<String>,
    excludedScanlators: Set<String>,
    onChangeExcluded: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (availableScanlators.size <= 1) return

    val sorted = remember(availableScanlators) { availableScanlators.sortedWith(String.CASE_INSENSITIVE_ORDER) }
    val enabled = remember(availableScanlators, excludedScanlators) { availableScanlators - excludedScanlators }
    val allLabel = stringResource(MR.strings.all)
    val selection = when {
        excludedScanlators.isEmpty() || enabled.size == availableScanlators.size -> allLabel
        enabled.size == 1 -> enabled.first()
        enabled.isEmpty() -> allLabel
        else -> stringResource(MR.strings.ml_g1_scanlator_multiple)
    }

    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        InkGlassSurface(
            modifier = Modifier.fillMaxWidth(),
            level = InkGlassLevel.Soft,
            contentPadding = PaddingValues(
                horizontal = InkGlassTokens.Space16,
                vertical = InkGlassTokens.Space12,
            ),
            shadowElevation = 0.dp,
            onClick = { expanded = true },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(MR.strings.ml_g1_scanlator_eyebrow),
                        style = InkTypography.Eyebrow,
                        color = InkGlassTokens.Accent,
                    )
                    Text(
                        text = selection,
                        style = InkTypography.TitleSmall,
                        color = InkGlassTokens.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = InkGlassTokens.TextSecondary,
                )
            }
        }

        InkGlassDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(MR.strings.ml_g1_scanlator_all),
                        color = if (excludedScanlators.isEmpty()) InkGlassTokens.Accent else InkGlassTokens.TextPrimary,
                    )
                },
                onClick = {
                    onChangeExcluded(emptySet())
                    expanded = false
                },
            )
            sorted.forEach { scanlator ->
                val selected = enabled.size == 1 && scanlator in enabled
                DropdownMenuItem(
                    text = {
                        Text(
                            text = scanlator,
                            color = if (selected) InkGlassTokens.Accent else InkGlassTokens.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    onClick = {
                        onChangeExcluded(availableScanlators - scanlator)
                        expanded = false
                    },
                )
            }
        }
    }
}
