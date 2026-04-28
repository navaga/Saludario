package com.ignaciovalero.saludario.ui.today

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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
import com.ignaciovalero.saludario.ui.theme.MedicationMissedAccentDark
import com.ignaciovalero.saludario.ui.theme.MedicationMissedAccentLight
import com.ignaciovalero.saludario.ui.theme.MedicationTakenAccentDark
import com.ignaciovalero.saludario.ui.theme.MedicationTakenAccentLight
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Selector de fecha integrado en Saludario, muestra cada día del mes con un
 * indicador visual del estado de medicación (completado, pendiente, olvidado,
 * mixto o sin tomas) y un resumen del día seleccionado.
 *
 * Mantiene la convención de "preselección + Aceptar" del DatePicker estándar
 * para no sorprender al usuario.
 */
@Composable
fun MedicationCalendarPickerDialog(
    selectedDate: LocalDate,
    visibleMonth: YearMonth,
    statuses: Map<LocalDate, CalendarDayMedicationStatus>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSetVisibleMonth: (YearMonth) -> Unit,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    today: LocalDate = LocalDate.now()
) {
    var pendingDate by remember(selectedDate) { mutableStateOf(selectedDate) }
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
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.time_picker_cancel))
            }
        },
        title = null,
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                MonthHeader(
                    title = visibleMonth
                        .format(monthTitleFormatter)
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() },
                    onPrevious = onPreviousMonth,
                    onNext = onNextMonth
                )

                Spacer(modifier = Modifier.height(AppSpacing.sm))

                QuickJumpRow(
                    today = today,
                    onJump = { jumped ->
                        pendingDate = jumped
                        // Asegúrate de que el mes mostrado contenga el día saltado.
                        val targetMonth = YearMonth.from(jumped)
                        if (targetMonth != visibleMonth) onSetVisibleMonth(targetMonth)
                    }
                )

                Spacer(modifier = Modifier.height(AppSpacing.sm))

                WeekdayHeader(locale)

                MonthGrid(
                    visibleMonth = visibleMonth,
                    pendingDate = pendingDate,
                    today = today,
                    statuses = statuses,
                    locale = locale,
                    onDayClick = { pendingDate = it }
                )

                Spacer(modifier = Modifier.height(AppSpacing.sm))

                DaySummary(status = statuses[pendingDate], date = pendingDate, locale = locale)

                Spacer(modifier = Modifier.height(AppSpacing.sm))

                CalendarLegend()
            }
        }
    )
}

@Composable
private fun MonthHeader(
    title: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
private fun QuickJumpRow(
    today: LocalDate,
    onJump: (LocalDate) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        AssistChip(
            onClick = { onJump(today.minusDays(1)) },
            label = { Text(text = stringResource(R.string.calendar_yesterday)) },
            colors = AssistChipDefaults.assistChipColors()
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
    // Empieza siempre en lunes para coincidir con el grid.
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
    statuses: Map<LocalDate, CalendarDayMedicationStatus>,
    locale: Locale,
    onDayClick: (LocalDate) -> Unit
) {
    val firstOfMonth = visibleMonth.atDay(1)
    val daysFromMonday = (firstOfMonth.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val gridStart = firstOfMonth.minusDays(daysFromMonday.toLong())

    // El formatter es el único recurso "caro" por celda; lo creamos una
    // sola vez y lo compartimos entre las 42 celdas del grid.
    val descFormatter = remember(locale) { localizedShortDayMonthFormatter(locale) }

    Column(modifier = Modifier.fillMaxWidth()) {
        for (week in 0 until 6) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (dow in 0 until 7) {
                    val day = gridStart.plusDays((week * 7 + dow).toLong())
                    val status = statuses[day]
                    DayCell(
                        date = day,
                        inCurrentMonth = day.month == visibleMonth.month,
                        isToday = day == today,
                        isSelected = day == pendingDate,
                        status = status,
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
    status: CalendarDayMedicationStatus?,
    descriptionFormatter: DateTimeFormatter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val visual = status?.visualStatus ?: CalendarDayVisualStatus.EMPTY
    val indicatorColor = visual.indicatorColor(isDark)
    // El día "hoy" se distingue con un contorno (border), no con relleno;
    // por eso sólo el día seleccionado pinta fondo.
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val contentColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        !inCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.onSurface
    }

    val cellDescription = buildDayDescription(date, status, isSelected, isToday, descriptionFormatter)

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(containerColor)
            .then(
                if (isToday && !isSelected) {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                } else Modifier
            )
            .clickable(onClick = onClick)
            .semantics { contentDescription = cellDescription },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                fontWeight = if (isToday || isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
            // Indicador de estado: punto bajo el número.
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(
                        if (visual == CalendarDayVisualStatus.EMPTY || !inCurrentMonth) Color.Transparent
                        else if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else indicatorColor
                    )
            )
        }
    }
}

@Composable
private fun DaySummary(
    status: CalendarDayMedicationStatus?,
    date: LocalDate,
    locale: java.util.Locale
) {
    val formatter = remember(locale) { localizedFullDateFormatter(locale) }
    val dateText = remember(date, locale) {
        date.format(formatter)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }
    val total = status?.totalDoses ?: 0
    val summary = when {
        total == 0 -> stringResource(R.string.calendar_summary_no_doses)
        status!!.takenDoses == total -> stringResource(
            R.string.calendar_summary_all_taken,
            total
        )
        status.missedDoses > 0 -> stringResource(
            R.string.calendar_summary_with_missed,
            total,
            status.takenDoses,
            status.missedDoses
        )
        else -> stringResource(
            R.string.calendar_summary_basic,
            total,
            status.takenDoses,
            status.pendingDoses + status.postponedDoses
        )
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
            color = if (status != null && status.missedDoses > 0)
                MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CalendarLegend() {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendDot(
            color = CalendarDayVisualStatus.COMPLETED.indicatorColor(isDark),
            label = stringResource(R.string.calendar_legend_completed)
        )
        LegendDot(
            color = CalendarDayVisualStatus.PENDING.indicatorColor(isDark),
            label = stringResource(R.string.calendar_legend_pending)
        )
        LegendDot(
            color = CalendarDayVisualStatus.MISSED.indicatorColor(isDark),
            label = stringResource(R.string.calendar_legend_missed)
        )
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun CalendarDayVisualStatus.indicatorColor(isDark: Boolean): Color = when (this) {
    CalendarDayVisualStatus.COMPLETED ->
        if (isDark) MedicationTakenAccentDark else MedicationTakenAccentLight
    CalendarDayVisualStatus.MISSED ->
        if (isDark) MedicationMissedAccentDark else MedicationMissedAccentLight
    CalendarDayVisualStatus.PENDING -> MaterialTheme.colorScheme.primary
    CalendarDayVisualStatus.MIXED -> MaterialTheme.colorScheme.tertiary
    CalendarDayVisualStatus.EMPTY -> Color.Transparent
}

/**
 * Formato fecha completo ("jueves 25 de abril" / "Thursday, April 25")
 * usando el patrón preferido del locale en lugar de strings hardcodeadas.
 */
internal fun localizedFullDateFormatter(locale: Locale): DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)

/** Formato corto día + mes ("25 de abril" / "April 25"). */
internal fun localizedShortDayMonthFormatter(locale: Locale): DateTimeFormatter {
    val pattern = try {
        android.text.format.DateFormat.getBestDateTimePattern(locale, "dMMMM")
    } catch (_: Throwable) {
        "d MMMM"
    }
    return DateTimeFormatter.ofPattern(pattern, locale)
}

@Composable
private fun buildDayDescription(
    date: LocalDate,
    status: CalendarDayMedicationStatus?,
    isSelected: Boolean,
    isToday: Boolean,
    formatter: DateTimeFormatter
): String {
    val datePart = date.format(formatter)
    val statePart = when {
        status == null || status.totalDoses == 0 -> stringResource(R.string.calendar_summary_no_doses)
        status.missedDoses > 0 -> stringResource(
            R.string.calendar_summary_with_missed,
            status.totalDoses, status.takenDoses, status.missedDoses
        )
        status.takenDoses == status.totalDoses -> stringResource(
            R.string.calendar_summary_all_taken, status.totalDoses
        )
        else -> stringResource(
            R.string.calendar_summary_basic,
            status.totalDoses, status.takenDoses,
            status.pendingDoses + status.postponedDoses
        )
    }
    val markers = buildList {
        if (isToday) add(stringResource(R.string.calendar_today))
        if (isSelected) add(stringResource(R.string.calendar_selected_cd))
    }.joinToString(", ")
    return if (markers.isEmpty()) "$datePart. $statePart" else "$datePart. $statePart. $markers"
}
