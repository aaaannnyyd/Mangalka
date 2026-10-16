package eu.kanade.presentation.more.onboarding

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import eu.kanade.presentation.util.rememberRequestPackageInstallsPermissionState
import eu.kanade.tachiyomi.util.system.launchRequestPackageInstallsPermission
import eu.kanade.tachiyomi.util.system.telemetryIncluded
import mihon.app.di.appGraph
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.util.collectAsState

internal class PermissionStep : OnboardingStep {

    private var notificationGranted by mutableStateOf(false)
    private var batteryGranted by mutableStateOf(false)

    override val isComplete: Boolean = true

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val privacyPreferences = remember { context.appGraph.privacyPreferences }
        val lifecycleOwner = LocalLifecycleOwner.current
        val installGranted = rememberRequestPackageInstallsPermissionState()

        DisposableEffect(lifecycleOwner.lifecycle) {
            val observer = object : DefaultLifecycleObserver {
                override fun onResume(owner: LifecycleOwner) {
                    notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                            PackageManager.PERMISSION_GRANTED
                    } else {
                        true
                    }
                    batteryGranted = context.getSystemService<PowerManager>()!!
                        .isIgnoringBatteryOptimizations(context.packageName)
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        Column(verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12)) {
            Text(
                "Нужные разрешения",
                style = InkTypography.Title,
                color = InkGlassTokens.TextPrimary,
            )
            Text(
                "Ничего лишнего: только установка расширений, уведомления и стабильные фоновые задачи.",
                style = InkTypography.Body,
                color = InkGlassTokens.TextSecondary,
            )

            PermissionRow(
                title = "Установка расширений",
                subtitle = "Нужно для источников манги.",
                granted = installGranted,
                onClick = { context.launchRequestPackageInstallsPermission() },
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permissionRequester = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = {},
                )
                PermissionRow(
                    title = "Уведомления",
                    subtitle = "Обновления библиотеки и загрузки.",
                    granted = notificationGranted,
                    onClick = { permissionRequester.launch(Manifest.permission.POST_NOTIFICATIONS) },
                )
            }

            PermissionRow(
                title = "Работа в фоне",
                subtitle = "Чтобы загрузки и резервные копии не обрывались.",
                granted = batteryGranted,
                onClick = {
                    @SuppressLint("BatteryLife")
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = "package:${context.packageName}".toUri()
                    }
                    context.startActivity(intent)
                },
            )

            if (telemetryIncluded) {
                val crashlyticsPref = privacyPreferences.crashlytics
                val crashlytics by crashlyticsPref.collectAsState()
                PermissionRow(
                    title = "Отчёты о сбоях",
                    subtitle = "Можно отключить в любой момент.",
                    granted = crashlytics,
                    onClick = { crashlyticsPref.set(!crashlytics) },
                    grantedLabel = "Включено",
                    actionLabel = "Выключено",
                )

                val analyticsPref = privacyPreferences.analytics
                val analytics by analyticsPref.collectAsState()
                PermissionRow(
                    title = "Анонимная аналитика",
                    subtitle = "Помогает находить проблемы интерфейса.",
                    granted = analytics,
                    onClick = { analyticsPref.set(!analytics) },
                    grantedLabel = "Включено",
                    actionLabel = "Выключено",
                )
            }
        }
    }

    @Composable
    private fun PermissionRow(
        title: String,
        subtitle: String,
        granted: Boolean,
        onClick: () -> Unit,
        grantedLabel: String = "Готово",
        actionLabel: String = "Разрешить",
    ) {
        InkGlassSurface(
            modifier = Modifier.fillMaxWidth(),
            level = InkGlassLevel.Soft,
            contentPadding = PaddingValues(InkGlassTokens.Space16),
            shadowElevation = 0.dp,
            onClick = onClick,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (granted) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (granted) InkGlassTokens.Accent else InkGlassTokens.TextTertiary,
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space4)) {
                    Text(title, style = InkTypography.TitleSmall, color = InkGlassTokens.TextPrimary)
                    Text(subtitle, style = InkTypography.BodySmall, color = InkGlassTokens.TextSecondary)
                }
                Text(
                    if (granted) grantedLabel else actionLabel,
                    style = InkTypography.Label,
                    color = if (granted) InkGlassTokens.Accent else InkGlassTokens.TextSecondary,
                )
            }
        }
    }
}
