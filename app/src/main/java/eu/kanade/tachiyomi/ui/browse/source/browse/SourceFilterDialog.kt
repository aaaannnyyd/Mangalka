package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.RemoveCircleOutline
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import tachiyomi.core.common.preference.TriState
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkAccentButton
import tachiyomi.presentation.core.components.InkGlassButton
import tachiyomi.presentation.core.components.InkGlassDropdownMenu
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun SourceFilterDialog(
    onDismissRequest: () -> Unit,
    filters: FilterList,
    onReset: () -> Unit,
    onFilter: () -> Unit,
    onUpdate: (FilterList) -> Unit,
) {
    val updateFilters = { onUpdate(filters) }

    AdaptiveSheet(onDismissRequest = onDismissRequest) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = InkGlassTokens.Space24),
            verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
        ) {
            stickyHeader {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(InkGlassTokens.BackgroundPrimary.copy(alpha = 0.97f))
                        .padding(
                            horizontal = InkGlassTokens.ScreenHorizontal,
                            vertical = InkGlassTokens.Space12,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12),
                ) {
                    InkGlassButton(
                        label = stringResource(MR.strings.action_reset),
                        onClick = onReset,
                        modifier = Modifier.weight(1f),
                    )
                    InkAccentButton(
                        label = stringResource(MR.strings.action_filter),
                        onClick = {
                            onFilter()
                            onDismissRequest()
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            items(filters) { filter ->
                Box(
                    modifier = Modifier.padding(horizontal = InkGlassTokens.ScreenHorizontal),
                ) {
                    FilterItem(filter, updateFilters)
                }
            }
        }
    }
}

@Composable
private fun FilterItem(filter: Filter<*>, onUpdate: () -> Unit) {
    when (filter) {
        is Filter.Header -> FilterHeading(filter.name)
        is Filter.Separator -> FilterSeparator()
        is Filter.CheckBox -> FilterToggle(
            label = filter.name,
            state = if (filter.state) TriState.ENABLED_IS else TriState.DISABLED,
            onClick = {
                filter.state = !filter.state
                onUpdate()
            },
        )
        is Filter.TriState -> FilterToggle(
            label = filter.name,
            state = filter.state.toTriStateFilter(),
            onClick = {
                filter.state = filter.state.toTriStateFilter().next().toTriStateInt()
                onUpdate()
            },
        )
        is Filter.Text -> FilterTextField(
            label = filter.name,
            value = filter.state,
            onValueChange = {
                filter.state = it
                onUpdate()
            },
        )
        is Filter.Select<*> -> FilterSelect(
            label = filter.name,
            options = filter.values,
            selectedIndex = filter.state,
            onSelect = {
                filter.state = it
                onUpdate()
            },
        )
        is Filter.Sort -> FilterSort(filter, onUpdate)
        is Filter.Group<*> -> FilterGroup(filter, onUpdate)
    }
}

@Composable
private fun FilterHeading(text: String) {
    Text(
        text = text.uppercase(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = InkGlassTokens.Space12, bottom = InkGlassTokens.Space4),
        style = InkTypography.Eyebrow,
        color = InkGlassTokens.Accent,
    )
}

@Composable
private fun FilterSeparator() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = InkGlassTokens.Space8)
            .height(1.dp)
            .background(InkGlassTokens.GlassBorderSoft),
    )
}

@Composable
private fun FilterTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    InkGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        level = InkGlassLevel.Soft,
        shape = RoundedCornerShape(InkGlassTokens.RadiusSmall),
        contentPadding = PaddingValues(
            horizontal = InkGlassTokens.Space16,
            vertical = InkGlassTokens.Space12,
        ),
        shadowElevation = 0.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space4)) {
            Text(label.uppercase(), style = InkTypography.Eyebrow, color = InkGlassTokens.TextTertiary)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = InkTypography.Body.copy(color = InkGlassTokens.TextPrimary),
                cursorBrush = SolidColor(InkGlassTokens.Accent),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(label, style = InkTypography.Body, color = InkGlassTokens.TextTertiary)
                    }
                    inner()
                },
            )
        }
    }
}

@Composable
private fun FilterSelect(
    label: String,
    options: Array<out Any?>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val safeIndex = selectedIndex.coerceIn(0, (options.size - 1).coerceAtLeast(0))

    Box {
        InkGlassSurface(
            modifier = Modifier.fillMaxWidth(),
            level = InkGlassLevel.Soft,
            shape = RoundedCornerShape(InkGlassTokens.RadiusSmall),
            contentPadding = PaddingValues(
                horizontal = InkGlassTokens.Space16,
                vertical = InkGlassTokens.Space12,
            ),
            shadowElevation = 0.dp,
            onClick = { if (options.isNotEmpty()) expanded = true },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(label.uppercase(), style = InkTypography.Eyebrow, color = InkGlassTokens.TextTertiary)
                    Text(
                        text = options.getOrNull(safeIndex)?.toString().orEmpty(),
                        style = InkTypography.Body,
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
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            option.toString(),
                            color = if (index == selectedIndex) InkGlassTokens.Accent else InkGlassTokens.TextPrimary,
                        )
                    },
                    onClick = {
                        onSelect(index)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun FilterToggle(
    label: String,
    state: TriState,
    onClick: () -> Unit,
) {
    val active = state != TriState.DISABLED
    InkGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        level = if (active) InkGlassLevel.Accent else InkGlassLevel.Soft,
        shape = RoundedCornerShape(InkGlassTokens.RadiusSmall),
        contentPadding = PaddingValues(
            horizontal = InkGlassTokens.Space16,
            vertical = InkGlassTokens.Space12,
        ),
        shadowElevation = 0.dp,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12),
        ) {
            Icon(
                imageVector = when (state) {
                    TriState.DISABLED -> Icons.Outlined.CheckBoxOutlineBlank
                    TriState.ENABLED_IS -> Icons.Outlined.CheckBox
                    TriState.ENABLED_NOT -> Icons.Outlined.RemoveCircleOutline
                },
                contentDescription = null,
                tint = if (active) InkGlassTokens.Accent else InkGlassTokens.TextTertiary,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = InkTypography.Body,
                color = InkGlassTokens.TextPrimary,
            )
            if (state == TriState.ENABLED_NOT) {
                Text("ИСКЛЮЧИТЬ", style = InkTypography.Eyebrow, color = InkGlassTokens.Accent)
            }
        }
    }
}

@Composable
private fun FilterSort(filter: Filter.Sort, onUpdate: () -> Unit) {
    InkGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        level = InkGlassLevel.Soft,
        shape = RoundedCornerShape(InkGlassTokens.RadiusMedium),
        contentPadding = PaddingValues(InkGlassTokens.Space12),
        shadowElevation = 0.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space4)) {
            Text(
                filter.name.uppercase(),
                style = InkTypography.Eyebrow,
                color = InkGlassTokens.TextTertiary,
                modifier = Modifier.padding(horizontal = InkGlassTokens.Space4, vertical = InkGlassTokens.Space4),
            )
            filter.values.forEachIndexed { index, value ->
                val selected = filter.state?.index == index
                val ascending = filter.state?.ascending == true
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val nextAscending = if (selected) !ascending else true
                            filter.state = Filter.Sort.Selection(index, nextAscending)
                            onUpdate()
                        }
                        .padding(horizontal = InkGlassTokens.Space8, vertical = InkGlassTokens.Space12),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12),
                ) {
                    Text(
                        text = value,
                        modifier = Modifier.weight(1f),
                        style = InkTypography.Body,
                        color = if (selected) InkGlassTokens.Accent else InkGlassTokens.TextPrimary,
                    )
                    if (selected) {
                        Icon(
                            imageVector = if (ascending) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                            contentDescription = null,
                            tint = InkGlassTokens.Accent,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterGroup(filter: Filter.Group<*>, onUpdate: () -> Unit) {
    InkGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        level = InkGlassLevel.Soft,
        shape = RoundedCornerShape(InkGlassTokens.RadiusMedium),
        contentPadding = PaddingValues(InkGlassTokens.Space12),
        shadowElevation = 0.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8)) {
            Text(
                filter.name.uppercase(),
                style = InkTypography.Eyebrow,
                color = InkGlassTokens.Accent,
                modifier = Modifier.padding(horizontal = InkGlassTokens.Space4, vertical = InkGlassTokens.Space4),
            )
            filter.state
                .filterIsInstance<Filter<*>>()
                .forEach { child -> FilterItem(child, onUpdate) }
        }
    }
}

private fun Int.toTriStateFilter(): TriState {
    return when (this) {
        Filter.TriState.STATE_IGNORE -> TriState.DISABLED
        Filter.TriState.STATE_INCLUDE -> TriState.ENABLED_IS
        Filter.TriState.STATE_EXCLUDE -> TriState.ENABLED_NOT
        else -> throw IllegalStateException("Unknown TriState state: $this")
    }
}

private fun TriState.toTriStateInt(): Int {
    return when (this) {
        TriState.DISABLED -> Filter.TriState.STATE_IGNORE
        TriState.ENABLED_IS -> Filter.TriState.STATE_INCLUDE
        TriState.ENABLED_NOT -> Filter.TriState.STATE_EXCLUDE
    }
}
