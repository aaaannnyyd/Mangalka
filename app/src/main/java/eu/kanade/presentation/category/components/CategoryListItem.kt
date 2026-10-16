package eu.kanade.presentation.category.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sh.calvin.reorderable.ReorderableCollectionItemScope
import tachiyomi.domain.category.model.Category
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkGlassIconButton
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ReorderableCollectionItemScope.CategoryListItem(
    category: Category,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InkGlassSurface(
        modifier = modifier.fillMaxWidth(),
        level = InkGlassLevel.Soft,
        contentPadding = PaddingValues(
            horizontal = InkGlassTokens.Space12,
            vertical = InkGlassTokens.Space8,
        ),
        shadowElevation = 0.dp,
        onClick = onRename,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .draggableHandle(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.DragHandle,
                    contentDescription = null,
                    tint = InkGlassTokens.TextTertiary,
                )
            }
            Text(
                text = category.name,
                modifier = Modifier.weight(1f),
                style = InkTypography.TitleSmall,
                color = InkGlassTokens.TextPrimary,
            )
            InkGlassIconButton(
                imageVector = Icons.Outlined.Edit,
                contentDescription = stringResource(MR.strings.action_rename_category),
                onClick = onRename,
                modifier = Modifier.size(40.dp),
            )
            InkGlassIconButton(
                imageVector = Icons.Outlined.Delete,
                contentDescription = stringResource(MR.strings.action_delete),
                onClick = onDelete,
                modifier = Modifier.size(40.dp),
            )
        }
    }
}
