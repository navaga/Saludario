package com.ignaciovalero.saludario.ui.today

import com.ignaciovalero.saludario.domain.scheduling.ScheduledDose
import com.ignaciovalero.saludario.domain.scheduling.ScheduledDoseStatus
import java.time.LocalDate

/**
 * Resumen agregado del estado de medicación de un día concreto. Se usa para
 * pintar indicadores en el calendario y un texto resumen al seleccionar el
 * día. Los conteos son independientes (una misma dosis solo cuenta en una
 * categoría).
 */
data class CalendarDayMedicationStatus(
    val date: LocalDate,
    val totalDoses: Int,
    val takenDoses: Int,
    val pendingDoses: Int,
    val missedDoses: Int,
    val postponedDoses: Int
) {
    val visualStatus: CalendarDayVisualStatus
        get() = when {
            totalDoses == 0 -> CalendarDayVisualStatus.EMPTY
            missedDoses > 0 -> CalendarDayVisualStatus.MISSED
            takenDoses == totalDoses -> CalendarDayVisualStatus.COMPLETED
            takenDoses > 0 -> CalendarDayVisualStatus.MIXED
            else -> CalendarDayVisualStatus.PENDING
        }

    companion object {
        fun fromDoses(date: LocalDate, doses: List<ScheduledDose>): CalendarDayMedicationStatus {
            var taken = 0
            var pending = 0
            var missed = 0
            var postponed = 0
            for (dose in doses) {
                when (dose.status) {
                    ScheduledDoseStatus.TAKEN -> taken++
                    ScheduledDoseStatus.PENDING -> pending++
                    ScheduledDoseStatus.MISSED -> missed++
                    ScheduledDoseStatus.POSTPONED -> postponed++
                }
            }
            return CalendarDayMedicationStatus(
                date = date,
                totalDoses = doses.size,
                takenDoses = taken,
                pendingDoses = pending,
                missedDoses = missed,
                postponedDoses = postponed
            )
        }
    }
}

enum class CalendarDayVisualStatus {
    /** Día sin dosis programadas. */
    EMPTY,

    /** Todas las dosis fueron tomadas. */
    COMPLETED,

    /** Sólo hay dosis pendientes (sin olvidos ni tomadas). */
    PENDING,

    /** Hay al menos una dosis olvidada. Tiene prioridad visual. */
    MISSED,

    /** Combinación de tomadas y pendientes/pospuestas, sin olvidos. */
    MIXED
}
