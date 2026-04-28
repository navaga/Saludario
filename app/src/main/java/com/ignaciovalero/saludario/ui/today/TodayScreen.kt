package com.ignaciovalero.saludario.ui.today

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ignaciovalero.saludario.R
import com.ignaciovalero.saludario.core.localization.localizedDurationMinutes
import com.ignaciovalero.saludario.core.localization.localizedLocalTime
import com.ignaciovalero.saludario.core.localization.localizedMedicationDosage
import com.ignaciovalero.saludario.domain.scheduling.ScheduledDose
import com.ignaciovalero.saludario.domain.scheduling.ScheduledDoseStatus
import com.ignaciovalero.saludario.ui.theme.AppSpacing
import com.ignaciovalero.saludario.ui.theme.MedicationMissedAccentDark
import com.ignaciovalero.saludario.ui.theme.MedicationMissedAccentLight
import com.ignaciovalero.saludario.ui.theme.MedicationMissedContainerDark
import com.ignaciovalero.saludario.ui.theme.MedicationMissedContainerLight
import com.ignaciovalero.saludario.ui.theme.MedicationPendingContainerDark
import com.ignaciovalero.saludario.ui.theme.MedicationPendingContainerLight
import com.ignaciovalero.saludario.ui.theme.MedicationTakenAccentDark
import com.ignaciovalero.saludario.ui.theme.MedicationTakenAccentLight
import com.ignaciovalero.saludario.ui.theme.MedicationTakenContainerDark
import com.ignaciovalero.saludario.ui.theme.MedicationTakenContainerLight
import com.ignaciovalero.saludario.ui.theme.SaludarioTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayScreen(
    selectedDate: LocalDate,
    uiState: TodayUiState,
    onToggleTaken: (medicationId: Long, time: String) -> Unit,
    onPostpone: (medicationId: Long, time: String, minutes: Long) -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousCalendarMonth: () -> Unit,
    onNextCalendarMonth: () -> Unit,
    onSetVisibleCalendarMonth: (java.time.YearMonth) -> Unit,
    onEnterSimpleMode: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenReliability: () -> Unit,
    onAddFirstMedication: () -> Unit,
    showSimpleModeHint: Boolean,
    onDismissSimpleModeHint: () -> Unit,
    contentPadding: PaddingValues,
    onHighlightConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) {
        DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)
    }
    val formattedDate = remember(selectedDate, locale) {
        selectedDate
            .format(dateFormatter)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }
    val selectedDateDescription = stringResource(R.string.day_selected_date_cd, formattedDate)
    val totalDoses = uiState.scheduledItems.size
    val takenDoses = uiState.scheduledItems.count { it.isTaken }
    val missedDoses = uiState.scheduledItems.count { it.isMissed }
    val postponedDoses = uiState.scheduledItems.count { it.isPostponed }
    val pendingDoses = uiState.scheduledItems.count { it.isPending }
    val progress = if (totalDoses > 0) takenDoses.toFloat() / totalDoses.toFloat() else 0f
    val adherencePercent = (progress * 100f).roundToInt()
    val isToday = selectedDate == LocalDate.now()
    val nowLocalDateTime = remember(uiState.scheduledItems) { java.time.LocalDateTime.now() }
    // Una dosis "requiere atención" si está olvidada, pospuesta o si está pendiente
    // y su hora efectiva ya ha pasado.
    val attentionDoses = if (isToday) {
        uiState.scheduledItems.count {
            it.isMissed || it.isPostponed || (it.isPending && it.effectiveTime <= nowLocalDateTime)
        }
    } else 0
    val dayStatus = remember(adherencePercent, totalDoses) {
        when {
            totalDoses == 0 -> DayAdherenceStatus.REGULAR
            adherencePercent >= 80 -> DayAdherenceStatus.GOOD
            adherencePercent >= 50 -> DayAdherenceStatus.REGULAR
            else -> DayAdherenceStatus.BAD
        }
    }

    val dayStatusLabel = when (dayStatus) {
        DayAdherenceStatus.GOOD -> stringResource(R.string.day_status_good)
        DayAdherenceStatus.REGULAR -> stringResource(R.string.day_status_regular)
        DayAdherenceStatus.BAD -> stringResource(R.string.day_status_bad)
    }

    val statusContainerColor = when (dayStatus) {
        DayAdherenceStatus.GOOD -> MaterialTheme.colorScheme.primary
        DayAdherenceStatus.REGULAR -> MaterialTheme.colorScheme.tertiary
        DayAdherenceStatus.BAD -> MaterialTheme.colorScheme.error
    }

    val statusContentColor = when (dayStatus) {
        DayAdherenceStatus.GOOD -> MaterialTheme.colorScheme.onPrimary
        DayAdherenceStatus.REGULAR -> MaterialTheme.colorScheme.onTertiary
        DayAdherenceStatus.BAD -> MaterialTheme.colorScheme.onError
    }

    val smartSummaryMessage = when {
        totalDoses == 0 -> stringResource(R.string.day_message_no_doses)
        // Cuando estamos viendo el día actual, prioriza acción sobre porcentaje.
        isToday && attentionDoses > 0 -> stringResource(R.string.day_message_urgent, attentionDoses)
        isToday && pendingDoses > 0 -> stringResource(R.string.day_message_pending, pendingDoses)
        isToday && takenDoses == totalDoses -> stringResource(R.string.day_message_all_done)
        // Para fechas pasadas/futuras, mantenemos el mensaje basado en adherencia.
        dayStatus == DayAdherenceStatus.GOOD -> stringResource(R.string.day_message_good, adherencePercent)
        dayStatus == DayAdherenceStatus.REGULAR -> stringResource(R.string.day_message_regular, adherencePercent)
        else -> stringResource(R.string.day_message_bad, adherencePercent)
    }
    val smartSummaryColor = when {
        totalDoses == 0 -> MaterialTheme.colorScheme.onPrimaryContainer
        isToday && attentionDoses > 0 -> MaterialTheme.colorScheme.error
        isToday && (pendingDoses > 0 || takenDoses < totalDoses) -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    val showStatusBadge = totalDoses > 0 && !isToday

    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.today_title)) },
            actions = {
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onOpenSettings()
                }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.today_settings_cd)
                    )
                }
                Box {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDismissSimpleModeHint()
                        onEnterSimpleMode()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Accessibility,
                            contentDescription = stringResource(R.string.today_simple_mode_cd)
                        )
                    }

                    DropdownMenu(
                        expanded = showSimpleModeHint,
                        onDismissRequest = onDismissSimpleModeHint
                    ) {
                        DropdownMenuItem(
                            text = { Text(text = stringResource(R.string.today_simple_mode_tooltip)) },
                            onClick = onDismissSimpleModeHint
                        )
                    }
                }
            }
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.xs, vertical = AppSpacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPreviousDay()
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = stringResource(R.string.day_prev_cd)
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            contentDescription = selectedDateDescription
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showDatePicker = true
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = stringResource(R.string.day_pick_date_cd)
                    )
                }
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNextDay()
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = stringResource(R.string.day_next_cd)
                    )
                }
            }
        }

        com.ignaciovalero.saludario.ui.today.reliability.ReminderReliabilityBanner(
            onReview = onOpenReliability
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.lg),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.day_summary_adherence_label),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(
                                R.string.day_summary_dose_count,
                                takenDoses,
                                totalDoses
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (showStatusBadge) {
                            Spacer(modifier = Modifier.width(AppSpacing.sm))
                            Box(
                                modifier = Modifier
                                    .background(statusContainerColor, RoundedCornerShape(8.dp))
                                    .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs)
                            ) {
                                Text(
                                    text = dayStatusLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusContentColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(AppSpacing.xs))
                SegmentedAdherenceBar(
                    taken = takenDoses,
                    postponed = postponedDoses,
                    missed = missedDoses,
                    pending = pendingDoses,
                    progress = progress,
                    contentDescription = context.getString(
                        R.string.day_summary_progress_cd,
                        adherencePercent
                    )
                )
                Spacer(modifier = Modifier.height(AppSpacing.xs))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = smartSummaryMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = smartSummaryColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (uiState.streakDays >= 2) {
                        Spacer(modifier = Modifier.width(AppSpacing.sm))
                        StreakChip(days = uiState.streakDays)
                    }
                }
            }
        }

        if (!uiState.canModifyIntake) {
            Text(
                text = stringResource(R.string.day_read_only_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = AppSpacing.lg, vertical = AppSpacing.xs)
            )
        }

        if (uiState.scheduledItems.isEmpty()) {
            if (!uiState.hasAnyMedication && isToday) {
                FirstMedicationCallout(
                    onAddFirstMedication = onAddFirstMedication,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.lg)
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.day_empty),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            // Listas precomputadas: las usamos tanto para construir el
            // LazyColumn como para localizar el índice de la dosis a destacar
            // si el usuario abrió la app desde una notificación.
            val attentionItems = remember(uiState.scheduledItems, isToday, nowLocalDateTime) {
                if (isToday) uiState.scheduledItems.filter {
                    it.isMissed || it.isPostponed ||
                        (it.isPending && it.effectiveTime <= nowLocalDateTime)
                }.sortedBy { it.effectiveTime } else emptyList()
            }
            val upcomingItems = remember(uiState.scheduledItems, isToday, nowLocalDateTime) {
                if (isToday) uiState.scheduledItems.filter {
                    it.isPending && it.effectiveTime > nowLocalDateTime
                }.sortedBy { it.effectiveTime } else emptyList()
            }
            val takenItems = remember(uiState.scheduledItems, isToday) {
                if (isToday) uiState.scheduledItems.filter { it.isTaken }
                    .sortedBy { it.effectiveTime } else emptyList()
            }
            val groupedByHour = remember(uiState.scheduledItems, isToday) {
                if (!isToday) uiState.scheduledItems.groupBy { it.time.substringBefore(":") }
                else emptyMap()
            }

            // Determina el índice del item destacado dentro del LazyColumn,
            // recorriendo la misma estructura que se construye más abajo.
            val highlightedIndex = remember(
                uiState.highlightedDose,
                attentionItems,
                upcomingItems,
                takenItems,
                groupedByHour
            ) {
                val target = uiState.highlightedDose ?: return@remember -1
                fun matches(item: com.ignaciovalero.saludario.domain.scheduling.ScheduledDose): Boolean {
                    if (item.medicationId != target.medicationId) return false
                    return item.scheduledAt.toLocalTime() == target.originalScheduledTime ||
                        item.effectiveTime.toLocalTime() == target.originalScheduledTime
                }
                var idx = 0
                if (isToday) {
                    if (attentionItems.isNotEmpty()) {
                        idx++ // section header
                        for (item in attentionItems) {
                            if (matches(item)) return@remember idx
                            idx++
                        }
                    }
                    if (upcomingItems.isNotEmpty()) {
                        idx++ // section header
                        for (item in upcomingItems) {
                            if (matches(item)) return@remember idx
                            idx++
                        }
                    }
                    if (takenItems.isNotEmpty()) {
                        idx++ // section header
                        for (item in takenItems) {
                            if (matches(item)) return@remember idx
                            idx++
                        }
                    }
                } else {
                    for ((_, items) in groupedByHour) {
                        idx++ // hour header
                        for (item in items) {
                            if (matches(item)) return@remember idx
                            idx++
                        }
                        idx++ // spacer
                    }
                }
                -1
            }

            val highlightedKey = remember(uiState.highlightedDose, attentionItems, upcomingItems, takenItems, groupedByHour) {
                val target = uiState.highlightedDose ?: return@remember null
                val all = attentionItems + upcomingItems + takenItems +
                    groupedByHour.values.flatten()
                all.firstOrNull {
                    it.medicationId == target.medicationId && (
                        it.scheduledAt.toLocalTime() == target.originalScheduledTime ||
                            it.effectiveTime.toLocalTime() == target.originalScheduledTime
                        )
                }?.let { "${it.medicationId}_${it.time}" }
            }

            val listState = rememberLazyListState()
            LaunchedEffect(highlightedIndex) {
                if (highlightedIndex >= 0) {
                    // Pequeño offset negativo para que la tarjeta no quede
                    // pegada al borde superior cuando es viable.
                    listState.animateScrollToItem(highlightedIndex)
                    onHighlightConsumed()
                } else if (uiState.highlightedDose != null) {
                    // El target llegó pero no hay coincidencia (medicamento
                    // borrado, otro día, etc.). Lo descartamos para no dejar
                    // el estado colgado.
                    onHighlightConsumed()
                }
            }

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                if (isToday) {
                    if (attentionItems.isNotEmpty()) {
                        item(key = "section_attention") {
                            SectionHeader(
                                text = stringResource(R.string.today_section_needs_attention),
                                accentColor = MaterialTheme.colorScheme.error
                            )
                        }
                        items(attentionItems, key = { "a_${it.medicationId}_${it.time}" }) { item ->
                            ScheduledMedicationCard(
                                item = item,
                                canModifyIntake = uiState.canModifyIntake,
                                highlighted = highlightedKey == "${item.medicationId}_${item.time}",
                                onToggle = { onToggleTaken(item.medicationId, item.time) },
                                onPostpone = { minutes -> onPostpone(item.medicationId, item.time, minutes) }
                            )
                        }
                    }

                    if (upcomingItems.isNotEmpty()) {
                        item(key = "section_upcoming") {
                            SectionHeader(
                                text = stringResource(R.string.today_section_upcoming),
                                accentColor = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(upcomingItems, key = { "u_${it.medicationId}_${it.time}" }) { item ->
                            ScheduledMedicationCard(
                                item = item,
                                canModifyIntake = uiState.canModifyIntake,
                                highlighted = highlightedKey == "${item.medicationId}_${item.time}",
                                onToggle = { onToggleTaken(item.medicationId, item.time) },
                                onPostpone = { minutes -> onPostpone(item.medicationId, item.time, minutes) }
                            )
                        }
                    }

                    if (takenItems.isNotEmpty()) {
                        item(key = "section_taken") {
                            SectionHeader(
                                text = stringResource(R.string.today_section_taken),
                                accentColor = MaterialTheme.colorScheme.outline
                            )
                        }
                        items(takenItems, key = { "t_${it.medicationId}_${it.time}" }) { item ->
                            ScheduledMedicationCard(
                                item = item,
                                canModifyIntake = uiState.canModifyIntake,
                                highlighted = highlightedKey == "${item.medicationId}_${item.time}",
                                onToggle = { onToggleTaken(item.medicationId, item.time) },
                                onPostpone = { minutes -> onPostpone(item.medicationId, item.time, minutes) }
                            )
                        }
                    }
                } else {
                    groupedByHour.forEach { (hour, items) ->
                        item(key = "header_$hour") {
                            TimeGroupHeader(hour = hour)
                        }
                        items(items, key = { "${it.medicationId}_${it.time}" }) { item ->
                            ScheduledMedicationCard(
                                item = item,
                                canModifyIntake = uiState.canModifyIntake,
                                highlighted = highlightedKey == "${item.medicationId}_${item.time}",
                                onToggle = { onToggleTaken(item.medicationId, item.time) },
                                onPostpone = { minutes -> onPostpone(item.medicationId, item.time, minutes) }
                            )
                        }
                        item(key = "spacer_$hour") {
                            Spacer(modifier = Modifier.height(AppSpacing.sm))
                        }
                    }
                }
            }
        }

        if (showDatePicker) {
            MedicationCalendarPickerDialog(
                selectedDate = selectedDate,
                visibleMonth = uiState.visibleCalendarMonth,
                statuses = uiState.calendarDayStatuses,
                onPreviousMonth = onPreviousCalendarMonth,
                onNextMonth = onNextCalendarMonth,
                onSetVisibleMonth = onSetVisibleCalendarMonth,
                onConfirm = { picked ->
                    onDateSelected(picked)
                    showDatePicker = false
                },
                onDismiss = { showDatePicker = false }
            )
        }
    }
}

private enum class DayAdherenceStatus {
    GOOD,
    REGULAR,
    BAD
}

@Composable
private fun SegmentedAdherenceBar(
    taken: Int,
    postponed: Int,
    missed: Int,
    pending: Int,
    progress: Float,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val total = taken + postponed + missed + pending
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val takenColor = if (isDark) MedicationTakenAccentDark else MedicationTakenAccentLight
    val missedColor = if (isDark) MedicationMissedAccentDark else MedicationMissedAccentLight
    val postponedColor = MaterialTheme.colorScheme.tertiary
    val pendingColor = MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .semantics {
                this.contentDescription = contentDescription
                progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
            }
            .background(pendingColor.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
    ) {
        if (total == 0) return@Row
        val segments = listOf(
            taken to takenColor,
            postponed to postponedColor,
            missed to missedColor,
            pending to pendingColor
        )
        segments.forEach { (count, color) ->
            if (count > 0) {
                Box(
                    modifier = Modifier
                        .weight(count.toFloat())
                        .fillMaxHeight()
                        .background(color)
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    text: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = accentColor,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = AppSpacing.sm, bottom = AppSpacing.xs)
    )
}

@Composable
private fun TimeGroupHeader(hour: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = AppSpacing.sm, bottom = AppSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.today_hour_header, hour),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
    }
}

@Composable
private fun ScheduledMedicationCard(
    item: ScheduledDose,
    canModifyIntake: Boolean,
    onToggle: () -> Unit,
    onPostpone: (minutes: Long) -> Unit,
    highlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val pendingSoftColor = if (isDarkTheme) MedicationPendingContainerDark else MedicationPendingContainerLight
    val takenSoftColor = if (isDarkTheme) MedicationTakenContainerDark else MedicationTakenContainerLight
    val takenAccentColor = if (isDarkTheme) MedicationTakenAccentDark else MedicationTakenAccentLight
    val missedSoftColor = if (isDarkTheme) MedicationMissedContainerDark else MedicationMissedContainerLight
    val missedAccentColor = if (isDarkTheme) MedicationMissedAccentDark else MedicationMissedAccentLight
    val localizedTime = context.localizedLocalTime(item.effectiveTime.toLocalTime())
    val localizedDosage = context.localizedMedicationDosage(item.medication.dosage, item.medication.unit)
    val takenAtLocalized = item.takenAt?.let { context.localizedLocalTime(it.toLocalTime()) }
    val originalScheduledLocalized = context.localizedLocalTime(item.scheduledAt.toLocalTime())

    val accentColor = when (item.status) {
        ScheduledDoseStatus.TAKEN -> takenAccentColor
        ScheduledDoseStatus.MISSED -> missedAccentColor
        ScheduledDoseStatus.PENDING,
        ScheduledDoseStatus.POSTPONED -> MaterialTheme.colorScheme.primary
    }

    val containerColor by animateColorAsState(
        targetValue = when (item.status) {
            ScheduledDoseStatus.TAKEN -> takenSoftColor
            ScheduledDoseStatus.MISSED -> missedSoftColor
            ScheduledDoseStatus.PENDING,
            ScheduledDoseStatus.POSTPONED -> pendingSoftColor
        },
        animationSpec = tween(durationMillis = 300),
        label = "cardColor"
    )

    val canTakeAction = canModifyIntake && item.status != ScheduledDoseStatus.TAKEN
    val canPostpone = canModifyIntake && (item.isPending || item.isPostponed || item.isMissed)
    val takenStatusDescription = stringResource(
        R.string.today_taken_status_cd,
        item.medicationName,
        localizedTime
    )
    val markTakenDescription = stringResource(
        R.string.today_mark_taken_for_cd,
        item.medicationName,
        localizedTime
    )
    val postponeDescription = stringResource(
        R.string.today_postpone_cd,
        item.medicationName
    )

    // El botón de acción principal siempre usa color primario (acción positiva).
    // El rojo se reserva para indicar el estado "Olvidada" en la etiqueta y borde,
    // no en el botón que el usuario debe pulsar para corregir la situación.
    val actionContainerColor = when (item.status) {
        ScheduledDoseStatus.PENDING,
        ScheduledDoseStatus.POSTPONED,
        ScheduledDoseStatus.MISSED -> MaterialTheme.colorScheme.primary
        ScheduledDoseStatus.TAKEN -> takenSoftColor
    }

    val actionContentColor = when (item.status) {
        ScheduledDoseStatus.PENDING,
        ScheduledDoseStatus.POSTPONED,
        ScheduledDoseStatus.MISSED -> MaterialTheme.colorScheme.onPrimary
        ScheduledDoseStatus.TAKEN -> takenAccentColor
    }
    val actionLabel = when (item.status) {
        ScheduledDoseStatus.TAKEN -> ""
        ScheduledDoseStatus.PENDING,
        ScheduledDoseStatus.POSTPONED,
        ScheduledDoseStatus.MISSED -> stringResource(R.string.today_mark_taken_button)
    }

    // Borde lateral por estado: codifica de un vistazo la urgencia de la dosis.
    val sideAccentColor = when (item.status) {
        ScheduledDoseStatus.MISSED -> missedAccentColor
        ScheduledDoseStatus.POSTPONED -> MaterialTheme.colorScheme.tertiary
        ScheduledDoseStatus.TAKEN -> takenAccentColor
        ScheduledDoseStatus.PENDING -> MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = if (highlighted) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null,
        elevation = if (highlighted) {
            CardDefaults.cardElevation(defaultElevation = 6.dp)
        } else CardDefaults.cardElevation(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(sideAccentColor)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md)
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.medicationName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.xs))
                    Text(
                        text = localizedDosage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.isTaken && takenAtLocalized != null) {
                        Spacer(modifier = Modifier.height(AppSpacing.xs))
                        Text(
                            text = stringResource(R.string.today_taken_at, takenAtLocalized),
                            style = MaterialTheme.typography.bodySmall,
                            color = takenAccentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (item.isPostponed) {
                        Spacer(modifier = Modifier.height(AppSpacing.xs))
                        Text(
                            text = stringResource(R.string.today_originally_scheduled, originalScheduledLocalized),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (item.isMissed) {
                        // Tiempo relativo legible: "hace 3 h", "hace 25 min".
                        val minutesAgo = java.time.Duration
                            .between(item.scheduledAt, java.time.LocalDateTime.now())
                            .toMinutes()
                            .coerceAtLeast(0L)
                        val durationText = context.localizedDurationMinutes(minutesAgo)
                        Spacer(modifier = Modifier.height(AppSpacing.xs))
                        Text(
                            text = stringResource(R.string.today_relative_ago, durationText),
                            style = MaterialTheme.typography.bodySmall,
                            color = missedAccentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .padding(horizontal = AppSpacing.sm + 2.dp, vertical = AppSpacing.xs + 1.dp)
                ) {
                    Text(
                        text = localizedTime,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor.copy(alpha = 0.9f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            if (item.status == ScheduledDoseStatus.TAKEN) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .background(takenAccentColor.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                            .semantics {
                                contentDescription = takenStatusDescription
                            }
                            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = takenAccentColor
                        )
                        Text(
                            text = stringResource(R.string.today_status_taken),
                            style = MaterialTheme.typography.labelLarge,
                            color = takenAccentColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
                val (label, bgColor, textColor) = when (item.status) {
                    ScheduledDoseStatus.PENDING -> Triple(
                        stringResource(R.string.today_status_pending),
                        MaterialTheme.colorScheme.secondaryContainer,
                        MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    ScheduledDoseStatus.POSTPONED -> Triple(
                        stringResource(R.string.today_status_postponed),
                        MaterialTheme.colorScheme.tertiaryContainer,
                        MaterialTheme.colorScheme.onTertiaryContainer
                    )

                    ScheduledDoseStatus.MISSED -> Triple(
                        stringResource(R.string.today_status_missed),
                        MaterialTheme.colorScheme.errorContainer,
                        MaterialTheme.colorScheme.onErrorContainer
                    )

                    ScheduledDoseStatus.TAKEN -> Triple("", Color.Transparent, Color.Transparent)
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    Box(
                        modifier = Modifier
                            .background(bgColor, RoundedCornerShape(8.dp))
                            .semantics {
                                contentDescription = label
                            }
                            .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onToggle()
                            },
                            modifier = Modifier
                                .weight(1.75f)
                                .semantics {
                                    contentDescription = markTakenDescription
                                },
                            enabled = canTakeAction,
                            contentPadding = PaddingValues(horizontal = AppSpacing.xs, vertical = AppSpacing.xs + 2.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = actionContainerColor,
                                contentColor = actionContentColor,
                                disabledContainerColor = actionContainerColor,
                                disabledContentColor = actionContentColor
                            )
                        ) {
                            Text(
                                text = actionLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }

                        if (canPostpone) {
                            var postponeMenuExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        postponeMenuExpanded = true
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .semantics {
                                            contentDescription = postponeDescription
                                        },
                                    contentPadding = PaddingValues(horizontal = AppSpacing.xs, vertical = AppSpacing.xs),
                                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = stringResource(R.string.today_postpone_short_label),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = stringResource(R.string.today_postpone_short_minutes),
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1
                                        )
                                    }
                                }
                                PostponeOptionsMenu(
                                    expanded = postponeMenuExpanded,
                                    onDismiss = { postponeMenuExpanded = false },
                                    onSelect = { minutes ->
                                        postponeMenuExpanded = false
                                        onPostpone(minutes)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TodayScreenPreview() {
    SaludarioTheme {
        DayScreen(
            selectedDate = LocalDate.of(2026, 4, 20),
            uiState = TodayUiState(
                selectedDate = LocalDate.of(2026, 4, 20),
                scheduledItems = listOf(
                    ScheduledDose(
                        medication = com.ignaciovalero.saludario.domain.scheduling.ScheduledDoseMedication(
                            id = 1L,
                            name = "Paracetamol",
                            dosage = 1.0,
                            unit = "tableta"
                        ),
                        scheduledAt = java.time.LocalDateTime.of(2026, 4, 20, 8, 0),
                        status = ScheduledDoseStatus.TAKEN,
                        logId = 1L
                    ),
                    ScheduledDose(
                        medication = com.ignaciovalero.saludario.domain.scheduling.ScheduledDoseMedication(
                            id = 2L,
                            name = "Omeprazol",
                            dosage = 1.0,
                            unit = "cápsula"
                        ),
                        scheduledAt = java.time.LocalDateTime.of(2026, 4, 20, 8, 0),
                        status = ScheduledDoseStatus.MISSED
                    ),
                    ScheduledDose(
                        medication = com.ignaciovalero.saludario.domain.scheduling.ScheduledDoseMedication(
                            id = 3L,
                            name = "Ibuprofeno",
                            dosage = 1.0,
                            unit = "tableta"
                        ),
                        scheduledAt = java.time.LocalDateTime.of(2026, 4, 20, 14, 0),
                        status = ScheduledDoseStatus.PENDING
                    )
                )
            ),
            onToggleTaken = { _, _ -> },
            onPostpone = { _, _, _ -> },
            onPreviousDay = {},
            onNextDay = {},
            onDateSelected = {},
            onPreviousCalendarMonth = {},
            onNextCalendarMonth = {},
            onSetVisibleCalendarMonth = {},
            onEnterSimpleMode = {},
            onOpenSettings = {},
            onOpenReliability = {},
            onAddFirstMedication = {},
            showSimpleModeHint = true,
            onDismissSimpleModeHint = {},
            contentPadding = PaddingValues(0.dp)
        )
    }
}

@Composable
private fun StreakChip(days: Int) {
    val cd = stringResource(R.string.today_streak_chip_cd, days)
    Row(
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.tertiaryContainer,
                RoundedCornerShape(999.dp)
            )
            .padding(horizontal = AppSpacing.sm + 2.dp, vertical = AppSpacing.xs)
            .semantics { contentDescription = cd },
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.LocalFireDepartment,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onTertiaryContainer
        )
        Text(
            text = stringResource(R.string.today_streak_chip, days),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

@Composable
private fun FirstMedicationCallout(
    onAddFirstMedication: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = stringResource(R.string.today_no_medications_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = stringResource(R.string.today_no_medications_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Button(
                    onClick = onAddFirstMedication,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.xs))
                    Text(text = stringResource(R.string.today_no_medications_cta))
                }
            }
        }
    }
}

@Composable
private fun PostponeOptionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onSelect: (minutes: Long) -> Unit
) {
    val tonightMinutes: Long? = remember(expanded) {
        if (!expanded) {
            null
        } else {
            val now = java.time.LocalDateTime.now()
            val tonightAt22 = now.toLocalDate().atTime(22, 0)
            val diff = java.time.Duration.between(now, tonightAt22).toMinutes()
            // Solo se ofrece si quedan al menos 15 min hasta las 22:00.
            if (diff >= 15L) diff else null
        }
    }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
    ) {
        Text(
            text = stringResource(R.string.today_postpone_options_title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs)
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.today_postpone_option_30min)) },
            onClick = { onSelect(30L) }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.today_postpone_option_1h)) },
            onClick = { onSelect(60L) }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.today_postpone_option_2h)) },
            onClick = { onSelect(120L) }
        )
        if (tonightMinutes != null) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.today_postpone_option_tonight)) },
                onClick = { onSelect(tonightMinutes) }
            )
        }
    }
}
