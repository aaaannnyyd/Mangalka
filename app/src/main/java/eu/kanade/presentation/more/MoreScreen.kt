package eu.kanade.presentation.more

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
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.GetApp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.more.DownloadQueueState
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkGlassIconButton
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkMangaBackdrop
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun MoreScreen(
    downloadQueueStateProvider: () -> DownloadQueueState,
    downloadedOnly: Boolean,
    onDownloadedOnlyChange: (Boolean) -> Unit,
    incognitoMode: Boolean,
    onIncognitoModeChange: (Boolean) -> Unit,
    onClickDownloadQueue: () -> Unit,
    onClickCategories: () -> Unit,
    onClickStats: () -> Unit,
    onClickDataAndStorage: () -> Unit,
    onClickSettings: () -> Unit,
    onClickSupport: () -> Unit,
    onClickAbout: () -> Unit,
) {
    val downloadQueueState = downloadQueueStateProvider()
    val downloadSummary = when (downloadQueueState) {
        DownloadQueueState.Stopped -> "Очередь пуста"
        is DownloadQueueState.Paused -> {
            val pending = downloadQueueState.pending
            if (pending == 0) {
                stringResource(MR.strings.paused)
            } else {
                "${stringResource(
                    MR.strings.paused,
                )} · ${pluralStringResource(MR.plurals.download_queue_summary, count = pending, pending)}"
            }
        }
        is DownloadQueueState.Downloading -> {
            val pending = downloadQueueState.pending
            pluralStringResource(MR.plurals.download_queue_summary, count = pending, pending)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(InkGlassTokens.BackgroundPrimary)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
    ) {
        InkMangaBackdrop(intensity = 0.9f)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = InkGlassTokens.ScreenHorizontal,
                end = InkGlassTokens.ScreenHorizontal,
                top = InkGlassTokens.Space24,
                bottom = InkGlassTokens.Space32,
            ),
            verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12),
        ) {
            item(key = "more_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = InkGlassTokens.Space16),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("МАНГАЛКА", style = InkTypography.Eyebrow, color = InkGlassTokens.Accent)
                        Text("Ещё", style = InkTypography.Display, color = InkGlassTokens.TextPrimary)
                    }
                    InkGlassIconButton(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(MR.strings.label_settings),
                        onClick = onClickSettings,
                    )
                }
            }

            item(key = "more_modes_title") { MoreSectionLabel("Режимы") }
            item(key = "downloaded_only") {
                MoreToggleRow(
                    title = "Только скачанное",
                    subtitle = "Показывать только загруженные тайтлы",
                    icon = Icons.Outlined.CloudOff,
                    checked = downloadedOnly,
                    onClick = { onDownloadedOnlyChange(!downloadedOnly) },
                )
            }
            item(key = "incognito") {
                MoreToggleRow(
                    title = "Инкогнито",
                    subtitle = "Не сохранять историю чтения",
                    icon = ImageVector.vectorResource(R.drawable.ic_glasses_24dp),
                    checked = incognitoMode,
                    onClick = { onIncognitoModeChange(!incognitoMode) },
                )
            }

            item(key = "more_library_title") {
                MoreSectionLabel("Библиотека", Modifier.padding(top = InkGlassTokens.Space12))
            }
            item(key = "downloads") {
                MoreActionRow(
                    title = "Очередь загрузок",
                    subtitle = downloadSummary,
                    icon = Icons.Outlined.GetApp,
                    onClick = onClickDownloadQueue,
                )
            }
            item(key = "categories") {
                MoreActionRow(
                    title = "Категории",
                    subtitle = "Организация вашей коллекции",
                    icon = Icons.AutoMirrored.Outlined.Label,
                    onClick = onClickCategories,
                )
            }
            item(key = "stats") {
                MoreActionRow(
                    title = "Статистика",
                    subtitle = "Чтение, главы и прогресс",
                    icon = Icons.Outlined.QueryStats,
                    onClick = onClickStats,
                )
            }
            item(key = "storage") {
                MoreActionRow(
                    title = "Данные и хранилище",
                    subtitle = "Загрузки, резервные копии и кэш",
                    icon = Icons.Outlined.Storage,
                    onClick = onClickDataAndStorage,
                )
            }

            item(key = "more_app_title") {
                MoreSectionLabel("Приложение", Modifier.padding(top = InkGlassTokens.Space12))
            }
            item(key = "settings") {
                MoreActionRow(
                    title = "Настройки",
                    subtitle = "Читалка, внешний вид и источники",
                    icon = Icons.Outlined.Settings,
                    onClick = onClickSettings,
                )
            }
            item(key = "about") {
                MoreActionRow(
                    title = "О приложении",
                    subtitle = "Версия и лицензии",
                    icon = Icons.Outlined.Info,
                    onClick = onClickAbout,
                )
            }
            item(key = "support") {
                MoreActionRow(
                    title = "Поддержать проект",
                    subtitle = "Поддержать исходный проект",
                    icon = Icons.Default.VolunteerActivism,
                    onClick = onClickSupport,
                )
            }
        }
    }
}

@Composable
private fun MoreSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier.padding(start = InkGlassTokens.Space4, bottom = InkGlassTokens.Space4),
        style = InkTypography.Eyebrow,
        color = InkGlassTokens.TextTertiary,
    )
}

@Composable
private fun MoreToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onClick: () -> Unit,
) {
    MoreActionRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        status = if (checked) "Включено" else "Выключено",
        statusAccent = checked,
        onClick = onClick,
    )
}

@Composable
private fun MoreActionRow(
    title: String,
    subtitle: String?,
    icon: ImageVector,
    onClick: () -> Unit,
    status: String? = null,
    statusAccent: Boolean = false,
) {
    InkGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        level = InkGlassLevel.Soft,
        shape = RoundedCornerShape(InkGlassTokens.RadiusMedium),
        contentPadding = PaddingValues(horizontal = InkGlassTokens.Space16, vertical = InkGlassTokens.Space16),
        shadowElevation = 0.dp,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space16),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(InkGlassTokens.AccentSoft, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(21.dp), tint = InkGlassTokens.Accent)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space4)) {
                Text(
                    title,
                    style = InkTypography.TitleSmall,
                    color = InkGlassTokens.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                subtitle?.let {
                    Text(
                        it,
                        style = InkTypography.BodySmall,
                        color = InkGlassTokens.TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            status?.let {
                Text(
                    it,
                    style = InkTypography.Eyebrow,
                    color = if (statusAccent) InkGlassTokens.Accent else InkGlassTokens.TextTertiary,
                )
            }
        }
    }
}
