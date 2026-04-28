package com.ignaciovalero.saludario.ui.health

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ignaciovalero.saludario.R
import com.ignaciovalero.saludario.ui.theme.AppSpacing
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

/**
 * Calendario para filtrar el histórico de salud por día. Muestra un punto en
 * cada día que contiene mediciones del tipo actual, un resumen del día
 * preseleccionado y un botón "Quitar filtro" para limpiar la selección.
 *
 * Mantiene la convención preselección + Aceptar de los pickers nativos para
 * no sorprender al usuario.
 */
@Composable
fun HealthDateFilterCalendarDialog(
    selectedDate: LocalDate?,
    countsByDate: Map<LocalDate, Int>,
    onConfirm: (LocalDate) -> Unit,
    onClearFilter: () -> Unit,
    onDismiss: () -> Unit,
    today: LocalDate = LocalDate.now()
) {
    val initial = selectedDate ?: today
    var pendingDate by rememberSaveable(selectedDate, stateSaver = LocalDateSaver) {
        mutableStateOf(initial)
    }
    var visibleMonth by rememberSaveable(selectedDate, stateSaver = YearMonthSaver) {
        mutableStateOf(YearMonth.from(initial))
    }
    val locale = LocalConfiguration.current.locales[0]
    val monthTitleFormatter = remember(locale) { DateTimeFormatter.ofPattern("LLLL yyyy", locale) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(pendingDate) }) {
                Text(text = stringResource(R.string.time_picker_confirm))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                if (selectedDate != null) {
                    TextButton(onClick = onClearFilter) {
                        Text(text = stringResource(R.string.health_calendar_clear_filter))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.time_picker_cancel))
                }
            }
        },
        title = null,
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                MonthHeader(
                    title = visibleMonth
                        .format(monthTitleFormatter)
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() },
                    onPrevious = { visibleMonth = visibleMonth.minusMonths(1) },
                    onNext = { visibleMonth = visibleMonth.plusMonths(1) }
                )

                Spacer(modifier = Modifier.height(AppSpacing.sm))

                QuickJumpRow(
                    today = today,
                    onJump = {
                        pendingDate = it
                        visibleMonth = YearMonth.from(it)
                    }
                )

                Spacer(modifier = Modifier.height(AppSpacing.sm))

                WeekdayHeader(locale)

                MonthGrid(
                    visibleMonth = visibleMonth,
                    pendingDate = pendingDate,
                    today = today,
                    countsByDate = countsByDate,
                    locale = locale,
                    onDayClick = { pendingDate = it }
                )

                Spacer(modifier = Modifier.height(AppSpacing.sm))

                DaySummary(
                    date = pendingDate,
                    count = countsByDate[pendingDate] ?: 0,
                    locale = locale
                )

                Spacer(modifier = Modifier.height(AppSpacing.sm))

                Legend()
            }
        }
    )
}

@Composable
private fun MonthHeader(title: String, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = stringResource(R.string.calendar_previous_month_cd)
            )
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = stringResource(R.string.calendar_next_month_cd)
            )
        }
    }
}

@Composable
private fun QuickJumpRow(today: LocalDate, onJump: (LocalDate) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        AssistChip(
            onClick = { onJump(today.minusDays(1)) },
            label = { Text(text = stringResource(R.string.calendar_yesterday)) }
        )
        AssistChip(
            onClick = { onJump(today) },
            label = { Text(text = stringResource(R.string.calendar_today)) }
        )
        AssistChip(
            onClick = { onJump(today.plusDays(1)) },
            label = { Text(text = stringResource(R.string.calendar_tomorrow)) }
        )
    }
}

@Composable
private fun WeekdayHeader(locale: java.util.Locale) {
    val days = remember(locale) {
        listOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
        ).map { it.getDisplayName(TextStyle.NARROW, locale).uppercase(locale) }
    }
    Row(modifier = Modifier.fillMaxWidth()) {
        days.forEach { label ->
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun MonthGrid(
    visibleMonth: YearMonth,
    pendingDate: LocalDate,
    today: LocalDate,
    countsByDate: Map<LocalDate, Int>,
    locale: java.util.Locale,
    onDayClick: (LocalDate) -> Unit
) {
    val firstOfMonth = visibleMonth.atDay(1)
    val daysFromMonday = (firstOfMonth.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val gridStart = firstOfMonth.minusDays(daysFromMonday.toLong())

    // Hoist del formatter para no recrearlo por celda.
    val descFormatter = remember(locale) {
        com.ignaciovalero.saludario.ui.today.localizedShortDayMonthFormatter(locale)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        for (week in 0 until 6) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (dow in 0 until 7) {
                    val day = gridStart.plusDays((week * 7 + dow).toLong())
                    DayCell(
                        date = day,
                        inCurrentMonth = day.month == visibleMonth.month,
                        isToday = day == today,
                        isSelected = day == pendingDate,
                        count = countsByDate[day] ?: 0,
                        descriptionFormatter = descFormatter,
                        onClick = { onDayClick(day) },
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    inCurrentMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    count: Int,
    descriptionFormatter: DateTimeFormatter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val contentColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        !inCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.onSurface
    }
    val description = buildDescription(date, count, isSelected, isToday, descriptionFormatter)
    val showIndicator = inCurrentMonth && count > 0
    val indicatorColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(containerColor)
            .then(
                if (isToday && !isSelected)
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                else Modifier
            )
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                fontWeight = if (isToday || isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (showIndicator) indicatorColor else Color.Transparent)
            )
        }
    }
}

@Composable
private fun DaySummary(date: LocalDate, count: Int, locale: java.util.Locale) {
    val formatter = remember(locale) {
        com.ignaciovalero.saludario.ui.today.localizedFullDateFormatter(locale)
    }
    val dateText = remember(date, locale) {
        date.format(formatter)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }
    val summary = if (count == 0) {
        stringResource(R.string.health_calendar_summary_empty)
    } else {
        stringResource(R.string.health_calendar_summary_count, count)
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = dateText,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun Legend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.health_calendar_legend_has_records),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun buildDescription(
    date: LocalDate,
    count: Int,
    isSelected: Boolean,
    isToday: Boolean,
    formatter: DateTimeFormatter
): String {
    val datePart = date.format(formatter)
    val countPart = if (count == 0)
        stringResource(R.string.health_calendar_summary_empty)
    else stringResource(R.string.health_calendar_summary_count, count)
    val markers = buildList {
        if (isToday) add(stringResource(R.string.calendar_today))
        if (isSelected) add(stringResource(R.string.calendar_selected_cd))
    }.joinToString(", ")
    return if (markers.isEmpty()) "$datePart. $countPart" else "$datePart. $countPart. $markers"
}

/**
 * Saver para LocalDate basado en epoch days. Permite que el d\u00eda
 * preseleccionado sobreviva a rotaciones y muerte de proceso.
 */
private val LocalDateSaver: Saver<LocalDate, Long> =
    Saver(save = { it.toEpochDay() }, restore = { LocalDate.ofEpochDay(it) })

/**
 * Saver para YearMonth como entero "YYYYMM". Asume a\u00f1os positivos
 * (suficiente para una app de salud).
 */
private val YearMonthSaver: Saver<YearMonth, Int> =
    Saver(
        save = { it.year * 100 + it.monthValue },
        restore = { YearMonth.of(it / 100, it % 100) }
    )
