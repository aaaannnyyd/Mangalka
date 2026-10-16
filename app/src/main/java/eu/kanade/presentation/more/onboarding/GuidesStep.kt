package eu.kanade.presentation.more.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tachiyomi.presentation.core.components.InkGlassButton
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkTypography

internal class GuidesStep(
    private val onRestoreBackup: () -> Unit,
) : OnboardingStep {

    override val isComplete: Boolean = true

    @Composable
    override fun Content() {
        Column(verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space16)) {
            Text(
                "Всё готово",
                style = InkTypography.Title,
                color = InkGlassTokens.TextPrimary,
            )
            Text(
                "Можно начать с каталога или восстановить прежнюю библиотеку из резервной копии.",
                style = InkTypography.Body,
                color = InkGlassTokens.TextSecondary,
            )
            InkGlassSurface(
                modifier = Modifier.fillMaxWidth(),
                level = InkGlassLevel.Soft,
                contentPadding = PaddingValues(InkGlassTokens.Space16),
                shadowElevation = 0.dp,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8)) {
                    Text(
                        "1 · Откройте «Каталог» и выберите источник",
                        style = InkTypography.Body,
                        color = InkGlassTokens.TextPrimary,
                    )
                    Text(
                        "2 · Добавьте мангу в библиотеку",
                        style = InkTypography.Body,
                        color = InkGlassTokens.TextPrimary,
                    )
                    Text(
                        "3 · Читайте главы; комментарии можно отключить в ридере",
                        style = InkTypography.Body,
                        color = InkGlassTokens.TextPrimary,
                    )
                }
            }
            InkGlassButton(
                label = "Восстановить резервную копию",
                modifier = Modifier.fillMaxWidth(),
                onClick = onRestoreBackup,
            )
            Text(
                "Эти действия доступны и позже в настройках.",
                style = InkTypography.BodySmall,
                color = InkGlassTokens.TextTertiary,
            )
        }
    }
}
