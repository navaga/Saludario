package com.ignaciovalero.saludario.ui.today

import androidx.compose.runtime.Immutable
import com.ignaciovalero.saludario.domain.scheduling.ScheduledDose
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

/**
 * Identifica una dosis concreta sobre la que la UI debe hacer scroll y
 * destacar visualmente, normalmente porque el usuario abrió la app desde una
 * notificación. La hora se compara contra `ScheduledDose.scheduledAt` (no
 * contra `effectiveTime`) para que también funcione con dosis pospuestas,
 * cuyo `time` mostrado puede ser distinto al original.
 */
data class HighlightedDose(
    val medicationId: Long,
    val originalScheduledTime: LocalTime
)

@Immutable
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
    val hasAnyMedication: Boolean = false,
    /**
     * Dosis a la que la UI debe hacer scroll y resaltar tras una apertura
     * desde notificación. Se limpia automáticamente cuando la pantalla la
     * consume.
     */
    val highlightedDose: HighlightedDose? = null,
    /**
     * Mes que el calendario está mostrando. Independiente de [selectedDate]:
     * cambiar de mes en el selector no debe alterar el día activo hasta que
     * el usuario lo confirme.
     */
    val visibleCalendarMonth: YearMonth = YearMonth.from(LocalDate.now()),
    /**
     * Estado de medicación agregado por día para el mes visible (incluye
     * días de relleno de meses adyacentes que el calendario muestre). Vacío
     * mientras se calcula la primera vez.
     */
    val calendarDayStatuses: Map<LocalDate, CalendarDayMedicationStatus> = emptyMap()
)
