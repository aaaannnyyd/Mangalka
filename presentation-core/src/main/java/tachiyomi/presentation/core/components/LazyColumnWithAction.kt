package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun LazyColumnWithAction(
    contentPadding: PaddingValues,
    actionLabel: String,
    onClickAction: () -> Unit,
    modifier: Modifier = Modifier,
    actionEnabled: Boolean = true,
    content: LazyListScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .padding(contentPadding)
            .fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            content = content,
        )

        InkAccentButton(
            label = actionLabel,
            onClick = onClickAction,
            enabled = actionEnabled,
            modifier = Modifier
                .padding(
                    horizontal = InkGlassTokens.ScreenHorizontal,
                    vertical = InkGlassTokens.Space12,
                )
                .fillMaxWidth(),
        )
    }
}
