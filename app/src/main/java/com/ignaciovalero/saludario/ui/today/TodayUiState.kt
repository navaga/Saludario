package com.ignaciovalero.saludario.ui.today

import com.ignaciovalero.saludario.domain.scheduling.ScheduledDose
import java.time.LocalDate

data class TodayUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val scheduledItems: List<ScheduledDose> = emptyList(),
    val canModifyIntake: Boolean = true
)
