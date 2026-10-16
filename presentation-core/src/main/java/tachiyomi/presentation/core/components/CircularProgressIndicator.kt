package tachiyomi.presentation.core.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

/**
 * Mangalka progress indicator that supports both indeterminate and determinate states
 * without falling back to Material's default spinner.
 */
@Composable
fun CombinedCircularProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
) {
    val current = progress().coerceIn(0f, 1f)
    AnimatedContent(
        targetState = current == 0f,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "progressState",
        modifier = modifier,
    ) { indeterminate ->
        InkCircularProgress(
            progress = if (indeterminate) null else current,
        )
    }
}

@Preview
@Composable
private fun CombinedCircularProgressIndicatorPreview() {
    var progress by remember { mutableFloatStateOf(0.45f) }
    CombinedCircularProgressIndicator(progress = { progress })
}
