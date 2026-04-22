package com.ignaciovalero.saludario.ui.today

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.ignaciovalero.saludario.core.localization.localizedLocalTime
import com.ignaciovalero.saludario.core.localization.localizedMedicationDosage
import com.ignaciovalero.saludario.domain.scheduling.ScheduledDose
import com.ignaciovalero.saludario.domain.scheduling.ScheduledDoseStatus
import com.ignaciovalero.saludario.ui.theme.AppSpacing
import com.ignaciovalero.saludario.ui.theme.MedicationMissedContainer
import com.ignaciovalero.saludario.ui.theme.MedicationPendingContainer
import com.ignaciovalero.saludario.ui.theme.MedicationTakenContainer
import com.ignaciovalero.saludario.ui.theme.SaludarioTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import java.util.Locale

private const val MILLIS_IN_DAY = 24L * 60L * 60L * 1000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayScreen(
    selectedDate: LocalDate,
    uiState: TodayUiState,
    onToggleTaken: (medicationId: Long, time: String) -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onEnterSimpleMode: () -> Unit,
    onOpenSettings: () -> Unit,
    showSimpleModeHint: Boolean,
    onDismissSimpleModeHint: () -> Unit,
    contentPadding: PaddingValues,
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
    val progress = if (totalDoses > 0) takenDoses.toFloat() / totalDoses.toFloat() else 0f
    val adherencePercent = (progress * 100f).roundToInt()
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
        dayStatus == DayAdherenceStatus.GOOD -> stringResource(R.string.day_message_good, adherencePercent)
        dayStatus == DayAdherenceStatus.REGULAR -> stringResource(R.string.day_message_regular, adherencePercent)
        else -> stringResource(R.string.day_message_bad, adherencePercent)
    }

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

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.lg),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = AppSpacing.lg, vertical = AppSpacing.lg)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.day_summary_taken_total, takenDoses, totalDoses),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = stringResource(R.string.day_summary_percent, adherencePercent),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .semantics {
                            contentDescription = context.getString(
                                R.string.day_summary_progress_cd,
                                adherencePercent
                            )
                            progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                        },
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = smartSummaryMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
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

        if (!uiState.canModifyIntake) {
            Text(
                text = stringResource(R.string.day_read_only_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = AppSpacing.lg, vertical = AppSpacing.xs)
            )
        }

        if (uiState.scheduledItems.isEmpty()) {
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
        } else {
            val groupedByHour = uiState.scheduledItems.groupBy { it.time.substringBefore(":") }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                groupedByHour.forEach { (hour, items) ->
                    item(key = "header_$hour") {
                        TimeGroupHeader(hour = hour)
                    }
                    items(items, key = { "${it.medicationId}_${it.time}" }) { item ->
                        ScheduledMedicationCard(
                            item = item,
                            canModifyIntake = uiState.canModifyIntake,
                            onToggle = { onToggleTaken(item.medicationId, item.time) }
                        )
                    }
                    item(key = "spacer_$hour") {
                        Spacer(modifier = Modifier.height(AppSpacing.sm))
                    }
                }
            }
        }

        if (showDatePicker) {
            val selectedMillis = selectedDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = selectedMillis
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val pickedMillis = datePickerState.selectedDateMillis
                        if (pickedMillis != null) {
                            val normalizedMillis = pickedMillis + MILLIS_IN_DAY / 2
                            val pickedDate = Instant.ofEpochMilli(normalizedMillis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            onDateSelected(pickedDate)
                        }
                        showDatePicker = false
                    }) {
                        Text(text = stringResource(R.string.time_picker_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text(text = stringResource(R.string.time_picker_cancel))
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

private enum class DayAdherenceStatus {
    GOOD,
    REGULAR,
    BAD
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val takenSoftColor = MedicationTakenContainer
    val takenAccentColor = Color(0xFF2E7D32)
    val missedSoftColor = MedicationMissedContainer
    val missedAccentColor = Color(0xFFC62828)
    val localizedTime = context.localizedLocalTime(item.scheduledAt.toLocalTime())
    val localizedDosage = context.localizedMedicationDosage(item.medication.dosage, item.medication.unit)

    val accentColor = when (item.status) {
        ScheduledDoseStatus.TAKEN -> takenAccentColor
        ScheduledDoseStatus.MISSED -> missedAccentColor
        ScheduledDoseStatus.PENDING -> MaterialTheme.colorScheme.primary
    }

    val containerColor by animateColorAsState(
        targetValue = when (item.status) {
            ScheduledDoseStatus.TAKEN -> takenSoftColor
            ScheduledDoseStatus.MISSED -> missedSoftColor
            ScheduledDoseStatus.PENDING -> MedicationPendingContainer
        },
        animationSpec = tween(durationMillis = 300),
        label = "cardColor"
    )

    val canTakeAction = canModifyIntake && item.status != ScheduledDoseStatus.TAKEN
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

    val actionContainerColor = when (item.status) {
        ScheduledDoseStatus.PENDING -> MaterialTheme.colorScheme.primary
        ScheduledDoseStatus.MISSED -> MaterialTheme.colorScheme.error
        ScheduledDoseStatus.TAKEN -> MedicationTakenContainer
    }

    val actionContentColor = when (item.status) {
        ScheduledDoseStatus.PENDING -> MaterialTheme.colorScheme.onPrimary
        ScheduledDoseStatus.MISSED -> MaterialTheme.colorScheme.onError
        ScheduledDoseStatus.TAKEN -> takenAccentColor
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (item.status != ScheduledDoseStatus.PENDING) {
            BorderStroke(1.dp, accentColor.copy(alpha = 0.45f))
        } else {
            null
        }
    ) {
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
                }

                Box(
                    modifier = Modifier
                        .background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
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

                    ScheduledDoseStatus.MISSED -> Triple(
                        stringResource(R.string.today_status_missed),
                        MaterialTheme.colorScheme.errorContainer,
                        MaterialTheme.colorScheme.onErrorContainer
                    )

                    ScheduledDoseStatus.TAKEN -> Triple("", Color.Transparent, Color.Transparent)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
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

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggle()
                        },
                        modifier = Modifier.semantics {
                            contentDescription = markTakenDescription
                        },
                        enabled = canTakeAction,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = actionContainerColor,
                            contentColor = actionContentColor,
                            disabledContainerColor = actionContainerColor,
                            disabledContentColor = actionContentColor
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = stringResource(R.string.today_mark_missed_taken),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = stringResource(R.string.simple_mode_taken_button))
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
            onPreviousDay = {},
            onNextDay = {},
            onDateSelected = {},
            onEnterSimpleMode = {},
            onOpenSettings = {},
            showSimpleModeHint = true,
            onDismissSimpleModeHint = {},
            contentPadding = PaddingValues(0.dp)
        )
    }
}
