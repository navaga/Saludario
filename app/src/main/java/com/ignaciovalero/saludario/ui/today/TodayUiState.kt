package com.ignaciovalero.saludario.ui.today

import com.ignaciovalero.saludario.domain.scheduling.ScheduledDose
import java.time.LocalDate

data class TodayUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val scheduledItems: List<ScheduledDose> = emptyList(),
    val canModifyIntake: Boolean = true,
    val streakDays: Int = 0,
    /**
     * `true` cuando el usuario tiene al menos un medicamento creado.
     * Permite distinguir el estado vacío "sin medicamentos aún" del
     * estado "sin tomas para este día".
     */
    val hasAnyMedication: Boolean = false
)
