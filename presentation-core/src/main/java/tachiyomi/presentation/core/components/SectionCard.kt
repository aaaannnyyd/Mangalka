package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.icerock.moko.resources.StringResource
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun LazyItemScope.SectionCard(
    titleRes: StringResource? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    SectionCard(title = titleRes?.let { stringResource(it) }, content = content)
}

@Composable
fun LazyItemScope.SectionCard(
    title: String?,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (title != null) {
        Text(
            modifier = Modifier.padding(
                start = InkGlassTokens.ScreenHorizontal,
                top = InkGlassTokens.Space8,
                end = InkGlassTokens.ScreenHorizontal,
                bottom = InkGlassTokens.Space4,
            ),
            text = title.uppercase(),
            style = InkTypography.Eyebrow,
            color = InkGlassTokens.TextTertiary,
        )
    }

    InkGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = InkGlassTokens.ScreenHorizontal,
                vertical = InkGlassTokens.Space4,
            ),
        level = InkGlassLevel.Soft,
        contentPadding = PaddingValues(InkGlassTokens.Space16),
        shadowElevation = InkGlassTokens.Space4,
    ) {
        Column {
            content()
        }
    }
}
