package tachiyomi.presentation.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Stable visual QA target for the shared InkFlow component family. */
@Preview(name = "InkFlow component gallery", widthDp = 412, heightDp = 892, showBackground = true)
@Composable
private fun InkGlassGalleryPreview() {
    InkGlassScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(InkGlassTokens.Space20),
            verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space24),
        ) {
            InkGlassTopBar(
                title = "InkFlow",
                eyebrow = "Component gallery",
                action = {
                    InkGlassIconButton(Icons.Outlined.Search, "Поиск", {})
                },
            )
            InkGlassSurface(level = InkGlassLevel.Strong) {
                Column(verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space16)) {
                    InkSectionHeader("Optical glass")
                    InkGlassProgressBar(0.68f)
                    Row(horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8)) {
                        InkGlassButton("Открыть", {})
                        InkAccentButton("Читать", {}, icon = Icons.Outlined.Bookmark)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8)) {
                InkGlassChip("Все", true, {})
                InkGlassChip("Новинки", false, {})
                InkGlassIconButton(Icons.Outlined.MoreHoriz, "Ещё", {})
            }
            InkMangaCard(
                title = "Провожающая в последний путь Фрирен",
                subtitle = "Глава 142",
                meta = "Сегодня",
                onClick = {},
                modifier = Modifier.width(174.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(248.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(InkGlassTokens.BackgroundRaised, InkGlassTokens.GlassBase),
                            ),
                        ),
                )
            }
            InkEmptyState(
                title = "Здесь пока тихо",
                message = "Добавьте мангу в библиотеку — InkFlow соберёт продолжение чтения здесь.",
                actionLabel = "Открыть каталог",
                onAction = {},
            )
        }
    }
}
