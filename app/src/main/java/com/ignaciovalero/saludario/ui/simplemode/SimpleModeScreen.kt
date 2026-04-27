package com.ignaciovalero.saludario.ui.simplemode

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ignaciovalero.saludario.R
import com.ignaciovalero.saludario.core.localization.localizedLocalTime
import com.ignaciovalero.saludario.core.localization.localizedMedicationDosage
import com.ignaciovalero.saludario.domain.scheduling.ScheduledDose
import com.ignaciovalero.saludario.domain.scheduling.ScheduledDoseMedication
import com.ignaciovalero.saludario.domain.scheduling.ScheduledDoseStatus
import com.ignaciovalero.saludario.ui.theme.AppSpacing
import com.ignaciovalero.saludario.ui.theme.MedicationMissedContainerDark
import com.ignaciovalero.saludario.ui.theme.MedicationMissedContainerLight
import com.ignaciovalero.saludario.ui.theme.MedicationPendingContainerDark
import com.ignaciovalero.saludario.ui.theme.MedicationPendingContainerLight
import com.ignaciovalero.saludario.ui.theme.MedicationTakenContainerDark
import com.ignaciovalero.saludario.ui.theme.MedicationTakenContainerLight
import com.ignaciovalero.saludario.ui.theme.SaludarioTheme
import com.ignaciovalero.saludario.ui.today.TodayUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleModeScreen(
    uiState: TodayUiState,
    onConfirmTaken: (medicationId: Long, time: String) -> Unit,
    onPostpone: (medicationId: Long, time: String) -> Unit,
    onExitSimpleMode: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val pending = uiState.scheduledItems.filter { it.isPending || it.isPostponed }
    val missed = uiState.scheduledItems.filter { it.isMissed }
    val taken = uiState.scheduledItems.filter { it.isTaken }
    val takenCount = taken.size

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.simple_mode_title),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onExitSimpleMode()
                    },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.simple_mode_exit_cd),
                        modifier = Modifier.size(32.dp)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors()
        )

        if (uiState.scheduledItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.simple_mode_empty),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    lineHeight = 36.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = AppSpacing.lg, vertical = AppSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
                modifier = Modifier.fillMaxSize()
            ) {
                if (pending.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.simple_mode_pending_count, pending.size),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    items(pending, key = { "p_${it.medicationId}_${it.time}" }) { item ->
                        SimpleMedicationCard(
                            item = item,
                            onConfirm = { onConfirmTaken(item.medicationId, item.time) },
                            onPostpone = { onPostpone(item.medicationId, item.time) }
                        )
                    }
                }

                if (missed.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.simple_mode_missed_count, missed.size),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    items(missed, key = { "m_${it.medicationId}_${it.time}" }) { item ->
                        SimpleMedicationCard(
                            item = item,
                            onConfirm = { onConfirmTaken(item.medicationId, item.time) },
                            onPostpone = { onPostpone(item.medicationId, item.time) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(AppSpacing.xs + 2.dp))
                    Text(
                        text = stringResource(R.string.simple_mode_taken_summary, takenCount),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (taken.isNotEmpty()) {
                    items(taken, key = { "t_${it.medicationId}_${it.time}" }) { item ->
                        CompactTakenItem(item = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun SimpleMedicationCard(
    item: ScheduledDose,
    onConfirm: () -> Unit,
    onPostpone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val localizedTime = context.localizedLocalTime(item.effectiveTime.toLocalTime())
    val localizedDosage = context.localizedMedicationDosage(item.medication.dosage, item.medication.unit)
    val originalScheduledLocalized = context.localizedLocalTime(item.scheduledAt.toLocalTime())
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val pendingContainer = if (isDarkTheme) MedicationPendingContainerDark else MedicationPendingContainerLight
    val takenContainer = if (isDarkTheme) MedicationTakenContainerDark else MedicationTakenContainerLight
    val missedContainer = if (isDarkTheme) MedicationMissedContainerDark else MedicationMissedContainerLight
    // El botón de confirmación siempre es primario (acción positiva). El estado
    // "Olvidada" se comunica con el badge y el contenedor rojizo de la tarjeta.
    val confirmContainerColor = MaterialTheme.colorScheme.primary
    val confirmContentColor = MaterialTheme.colorScheme.onPrimary
    val statusLabel = when (item.status) {
        ScheduledDoseStatus.TAKEN -> stringResource(R.string.today_status_taken)
        ScheduledDoseStatus.MISSED -> stringResource(R.string.today_status_missed)
        ScheduledDoseStatus.PENDING -> stringResource(R.string.today_status_pending)
        ScheduledDoseStatus.POSTPONED -> stringResource(R.string.today_status_postponed)
    }
    val statusContainerColor = when (item.status) {
        ScheduledDoseStatus.TAKEN -> MaterialTheme.colorScheme.primaryContainer
        ScheduledDoseStatus.MISSED -> MaterialTheme.colorScheme.errorContainer
        ScheduledDoseStatus.PENDING -> MaterialTheme.colorScheme.secondaryContainer
        ScheduledDoseStatus.POSTPONED -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val statusContentColor = when (item.status) {
        ScheduledDoseStatus.TAKEN -> MaterialTheme.colorScheme.onPrimaryContainer
        ScheduledDoseStatus.MISSED -> MaterialTheme.colorScheme.onErrorContainer
        ScheduledDoseStatus.PENDING -> MaterialTheme.colorScheme.onSecondaryContainer
        ScheduledDoseStatus.POSTPONED -> MaterialTheme.colorScheme.onTertiaryContainer
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (item.status) {
                ScheduledDoseStatus.TAKEN -> takenContainer
                ScheduledDoseStatus.MISSED -> missedContainer
                ScheduledDoseStatus.PENDING,
                ScheduledDoseStatus.POSTPONED -> pendingContainer
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.5.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = if (isDarkTheme) 0.75f else 0.45f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.xxl)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = localizedTime,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                SimpleStatusBadge(
                    label = statusLabel,
                    containerColor = statusContainerColor,
                    contentColor = statusContentColor
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.sm + 2.dp))

            Text(
                text = item.medicationName,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 32.sp
            )

            Text(
                text = localizedDosage,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (item.isPostponed) {
                Spacer(modifier = Modifier.height(AppSpacing.xs))
                Text(
                    text = stringResource(R.string.today_originally_scheduled, originalScheduledLocalized),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.lg + 2.dp))

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onConfirm()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(78.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = confirmContainerColor,
                    contentColor = confirmContentColor
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp)
                )
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = stringResource(R.string.simple_mode_taken_button),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            val postponeCd = stringResource(R.string.simple_mode_postpone_cd, item.medicationName)
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onPostpone()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .semantics { contentDescription = postponeCd },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = stringResource(R.string.simple_mode_postpone_button),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SimpleStatusBadge(
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = containerColor
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs)
        )
    }
}

@Composable
private fun CompactTakenItem(
    item: ScheduledDose,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val localizedTime = context.localizedLocalTime(item.scheduledAt.toLocalTime())
    val localizedDosage = context.localizedMedicationDosage(item.medication.dosage, item.medication.unit)
    val takenAtLocalized = item.takenAt?.let { context.localizedLocalTime(it.toLocalTime()) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (
                MaterialTheme.colorScheme.surface.luminance() < 0.5f
            ) {
                MedicationTakenContainerDark
            } else {
                MedicationTakenContainerLight
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(R.string.simple_mode_taken_cd),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.medicationName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$localizedDosage · $localizedTime",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (takenAtLocalized != null) {
                    Text(
                        text = stringResource(R.string.simple_mode_taken_at, takenAtLocalized),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SimpleModeScreenPreview() {
    SaludarioTheme {
        SimpleModeScreen(
            uiState = TodayUiState(
                scheduledItems = listOf(
                    ScheduledDose(
                        medication = ScheduledDoseMedication(1L, "Paracetamol", 1.0, "tableta"),
                        scheduledAt = java.time.LocalDateTime.of(2026, 4, 20, 8, 0),
                        status = ScheduledDoseStatus.PENDING
                    ),
                    ScheduledDose(
                        medication = ScheduledDoseMedication(2L, "Ibuprofeno", 1.0, "tableta"),
                        scheduledAt = java.time.LocalDateTime.of(2026, 4, 20, 14, 0),
                        status = ScheduledDoseStatus.TAKEN,
                        logId = 2L
                    ),
                    ScheduledDose(
                        medication = ScheduledDoseMedication(3L, "Omeprazol", 1.0, "cápsula"),
                        scheduledAt = java.time.LocalDateTime.of(2026, 4, 20, 21, 0),
                        status = ScheduledDoseStatus.PENDING
                    )
                )
            ),
            onConfirmTaken = { _, _ -> },
            onPostpone = { _, _ -> },
            onExitSimpleMode = {},
            contentPadding = PaddingValues(0.dp)
        )
    }
}
