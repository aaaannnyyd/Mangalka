package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.stats.data.StatsData
import tachiyomi.domain.readingactivity.model.ReadingActivityDay
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.components.SectionCard
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Mon–Sun reading activity: two totals and one compact column chart; tapping a column shows that day. */
@Composable
fun LazyItemScope.WeeklyActivityCard(data: StatsData.Weekly) {
    var selected by rememberSaveable(data.today) { mutableStateOf(data.today.toEpochDay()) }
    SectionCard(title = stringResource(MR.strings.ml_g4_stats_this_week)) {
        if (data.unavailable) {
            Text(
                stringResource(MR.strings.ml_g4_stats_log_unavailable),
                style = InkTypography.Body,
                color = InkGlassTokens.TextSecondary,
            )
            return@SectionCard
        }
        Row {
            StatsOverviewItem(
                title = data.completedChapters.toString(),
                subtitle = pluralStringResource(
                    MR.plurals.ml_g4_stats_chapters_word,
                    data.completedChapters.toInt(),
                ),
                icon = Icons.Outlined.LocalLibrary,
            )
            StatsOverviewItem(
                title = formatMeasured(data.durationMillis),
                subtitle = stringResource(MR.strings.ml_g4_stats_of_reading),
                icon = Icons.Outlined.Schedule,
            )
        }
        Spacer(Modifier.height(InkGlassTokens.Space16))
        WeekChart(data, selectedDay = selected, onSelect = { selected = it })
        Spacer(Modifier.height(InkGlassTokens.Space12))
        val day = data.days.firstOrNull { it.date.toEpochDay() == selected }
        if (day != null) {
            Text(
                text = dayDetails(day, data),
                style = InkTypography.BodySmall,
                color = InkGlassTokens.TextSecondary,
            )
        }
    }
}

@Composable
private fun WeekChart(data: StatsData.Weekly, selectedDay: Long, onSelect: (Long) -> Unit) {
    val maximum = data.days.maxOf { it.durationMillis }.coerceAtLeast(1)
    val locale = Locale.getDefault()
    Row(
        modifier = Modifier.fillMaxWidth().height(ChartHeight),
        horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
        verticalAlignment = Alignment.Bottom,
    ) {
        data.days.forEach { day ->
            val epochDay = day.date.toEpochDay()
            val isToday = day.date == data.today
            val isFuture = day.date.isAfter(data.today)
            val fraction = day.durationMillis.toFloat() / maximum
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(InkGlassTokens.RadiusChip))
                    .clickable(enabled = !isFuture, role = Role.Button) { onSelect(epochDay) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = InkGlassTokens.Space4),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(if (day.durationMillis > 0) fraction.coerceAtLeast(MIN_BAR_FRACTION) else 0f)
                            .clip(RoundedCornerShape(InkGlassTokens.RadiusChip / 2))
                            .background(
                                when {
                                    isToday -> InkGlassTokens.Accent
                                    epochDay == selectedDay -> InkGlassTokens.TextSecondary
                                    else -> InkGlassTokens.GlassBorder
                                },
                            ),
                    )
                }
                Spacer(Modifier.height(InkGlassTokens.Space4))
                Text(
                    text = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                    style = InkTypography.Label,
                    color = when {
                        epochDay == selectedDay -> InkGlassTokens.TextPrimary
                        isFuture -> InkGlassTokens.TextTertiary.copy(alpha = 0.5f)
                        else -> InkGlassTokens.TextTertiary
                    },
                )
            }
        }
    }
}

@Composable
private fun dayDetails(day: ReadingActivityDay, data: StatsData.Weekly): String {
    val date = day.date.format(DayFormatter).replaceFirstChar { it.titlecase() }
    val since = data.measuredSince
    return when {
        since == null || day.date.isBefore(since) ->
            stringResource(MR.strings.ml_g4_stats_day_log_not_started, date)
        day.durationMillis == 0L && day.completedChapters == 0L ->
            stringResource(MR.strings.ml_g4_stats_day_no_reading, date)
        else -> stringResource(
            MR.strings.ml_g4_stats_day_details,
            date,
            pluralStringResource(
                MR.plurals.ml_g4_stats_chapters_count,
                day.completedChapters.toInt(),
                day.completedChapters,
            ),
            formatMeasured(day.durationMillis),
        )
    }
}

@Composable
private fun formatMeasured(millis: Long): String {
    if (millis in 1L..59_999L) return stringResource(MR.strings.ml_g4_duration_under_minute)
    val minutes = millis / 60_000L
    return if (minutes >= 60) {
        stringResource(MR.strings.ml_g4_duration_hours_minutes, minutes / 60, minutes % 60)
    } else {
        stringResource(MR.strings.ml_g4_duration_minutes, minutes)
    }
}

private val ChartHeight = 120.dp
private val DayFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM")
private const val MIN_BAR_FRACTION = 0.04f
