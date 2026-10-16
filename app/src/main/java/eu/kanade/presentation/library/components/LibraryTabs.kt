package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.category.visualName
import tachiyomi.domain.category.model.Category
import tachiyomi.presentation.core.components.InkGlassChip
import tachiyomi.presentation.core.components.InkGlassTokens

@Composable
internal fun LibraryTabs(
    categories: List<Category>,
    pagerState: PagerState,
    getItemCountForCategory: (Category) -> Int?,
    onTabItemClick: (Int) -> Unit,
) {
    val currentPageIndex = pagerState.currentPage.coerceAtMost(categories.lastIndex)
    LazyRow(
        modifier = Modifier,
        contentPadding = PaddingValues(
            start = InkGlassTokens.ScreenHorizontal,
            end = InkGlassTokens.ScreenHorizontal,
            top = InkGlassTokens.Space4,
            bottom = InkGlassTokens.Space12,
        ),
        horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
    ) {
        itemsIndexed(categories, key = { _, category -> category.id }) { index, category ->
            val count = getItemCountForCategory(category)
            InkGlassChip(
                label = if (count != null) "${category.visualName} · $count" else category.visualName,
                selected = currentPageIndex == index,
                onClick = { onTabItemClick(index) },
            )
        }
    }
}
