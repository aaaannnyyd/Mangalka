package eu.kanade.presentation.more.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.domain.ui.model.ThemeMode
import eu.kanade.domain.ui.model.setAppCompatDelegateThemeMode
import mihon.app.di.appGraph
import soup.compose.material.motion.animation.materialSharedAxisX
import soup.compose.material.motion.animation.rememberSlideDistance
import tachiyomi.presentation.core.components.InkAccentButton
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassScaffold
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkTypography

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onRestoreBackup: () -> Unit,
) {
    val context = LocalContext.current
    val uiPreferences = remember { context.appGraph.uiPreferences }
    LaunchedEffect(Unit) {
        uiPreferences.themeMode.set(ThemeMode.DARK)
        uiPreferences.appTheme.set(AppTheme.DEFAULT)
        uiPreferences.themeDarkAmoled.set(false)
        setAppCompatDelegateThemeMode(ThemeMode.DARK)
    }

    val slideDistance = rememberSlideDistance()
    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    val steps = remember(onRestoreBackup) {
        listOf(
            StorageStep(),
            PermissionStep(),
            GuidesStep(onRestoreBackup = onRestoreBackup),
        )
    }
    val titles = listOf("Хранилище", "Разрешения", "Готово")
    val isLastStep = currentStep == steps.lastIndex

    BackHandler(enabled = currentStep != 0) { currentStep-- }

    InkGlassScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .padding(horizontal = InkGlassTokens.ScreenHorizontal),
        ) {
            Spacer(Modifier.height(InkGlassTokens.Space40))
            Text("МАНГАЛКА", style = InkTypography.Eyebrow, color = InkGlassTokens.Accent)
            Spacer(Modifier.height(InkGlassTokens.Space8))
            Text("Добро пожаловать", style = InkTypography.Display, color = InkGlassTokens.TextPrimary)
            Spacer(Modifier.height(InkGlassTokens.Space8))
            Text(
                "Три коротких шага — и можно читать.",
                style = InkTypography.Body,
                color = InkGlassTokens.TextSecondary,
            )
            Spacer(Modifier.height(InkGlassTokens.Space24))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                steps.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .weight(1f)
                            .clip(CircleShape)
                            .background(
                                if (index <= currentStep) {
                                    InkGlassTokens.Accent
                                } else {
                                    InkGlassTokens.GlassBorder
                                },
                            ),
                    )
                }
            }
            Spacer(Modifier.height(InkGlassTokens.Space12))
            Text(
                titles[currentStep].uppercase(),
                style = InkTypography.Eyebrow,
                color = InkGlassTokens.TextTertiary,
            )
            Spacer(Modifier.height(InkGlassTokens.Space16))

            InkGlassSurface(
                modifier = Modifier.fillMaxWidth(),
                level = InkGlassLevel.Standard,
                shape = RoundedCornerShape(InkGlassTokens.RadiusLargeCard),
                contentPadding = PaddingValues(InkGlassTokens.Space20),
                shadowElevation = 0.dp,
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        materialSharedAxisX(
                            forward = targetState > initialState,
                            slideDistance = slideDistance,
                        )
                    },
                    label = "onboardingStep",
                ) { index ->
                    steps[index].Content()
                }
            }

            Spacer(Modifier.weight(1f))
            InkAccentButton(
                label = if (isLastStep) "Начать" else "Далее",
                onClick = {
                    if (isLastStep) onComplete() else currentStep++
                },
                enabled = steps[currentStep].isComplete,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = InkGlassTokens.Space16),
            )
        }
    }
}
