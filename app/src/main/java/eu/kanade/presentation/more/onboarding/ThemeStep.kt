package eu.kanade.presentation.more.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.domain.ui.model.ThemeMode
import eu.kanade.domain.ui.model.setAppCompatDelegateThemeMode
import mihon.app.di.appGraph
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkTypography

internal class ThemeStep : OnboardingStep {

    override val isComplete: Boolean = true

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val uiPreferences = remember { context.appGraph.uiPreferences }

        LaunchedEffect(Unit) {
            uiPreferences.themeMode.set(ThemeMode.DARK)
            uiPreferences.appTheme.set(AppTheme.DEFAULT)
            uiPreferences.themeDarkAmoled.set(false)
            setAppCompatDelegateThemeMode(ThemeMode.DARK)
        }

        InkGlassSurface(
            modifier = Modifier.fillMaxWidth(),
            level = InkGlassLevel.Soft,
            contentPadding = PaddingValues(InkGlassTokens.Space20),
            shadowElevation = 0.dp,
        ) {
            Column {
                Text(
                    text = "ТЁМНОЕ ОФОРМЛЕНИЕ",
                    style = InkTypography.Eyebrow,
                    color = InkGlassTokens.Accent,
                )
                Text(
                    text = "Мангалка уже настроен",
                    style = InkTypography.Title,
                    color = InkGlassTokens.TextPrimary,
                )
                Text(
                    text = "Графитовый интерфейс и коралловый акцент применены ко всему приложению. " +
                        "Параметры экрана можно изменить позже в настройках.",
                    style = InkTypography.Body,
                    color = InkGlassTokens.TextSecondary,
                )
            }
        }
    }
}
