package com.ignaciovalero.saludario.ui.widget

import com.ignaciovalero.saludario.data.local.entity.MedicationEntity
import com.ignaciovalero.saludario.data.local.entity.MedicationLogEntity
import com.ignaciovalero.saludario.domain.scheduling.NextDoseCalculator
import com.ignaciovalero.saludario.domain.scheduling.ScheduledDose
import com.ignaciovalero.saludario.domain.scheduling.ScheduledDoseGenerator
import com.ignaciovalero.saludario.domain.scheduling.ScheduledDoseStatus
import java.time.LocalDateTime

/**
 * Calcula los estados de los widgets de medicación a partir de los datos
 * crudos de medicamentos y logs.
 *
 * Esta clase se mantiene como pura lógica de dominio (sin Context, ni
 * Compose, ni acceso a recursos) para poder testearla. La traducción a
 * cadenas localizadas (hora, dosis) se inyecta por parámetro.
 *
 * @param medications todos los medicamentos del usuario.
 * @param logs todos los logs del usuario (al menos los que cubran hoy y los
 *  últimos días). El generador filtrará por fecha internamente.
 * @param now instante de cálculo. Inyectable para tests.
 * @param formatTime función para formatear `LocalDateTime` a la hora
 *  presentada al usuario (debe respetar el locale activo).
 * @param formatDosage función para formatear `(dosage, unit)` a la cadena
 *  presentada al usuario.
 */
class MedicationWidgetDataManager(
    private val medications: List<MedicationEntity>,
    private val logs: List<MedicationLogEntity>,
    private val now: LocalDateTime,
    private val formatTime: (LocalDateTime) -> String,
    private val formatDosage: (Double, String) -> String
) {

    /**
     * Devuelve el estado del widget pequeño "Siguiente toma".
     *
     * Prioridad:
     *  1. Si no hay medicamentos -> [NextDoseWidgetState.NoMedications].
     *  2. Si hoy hay alguna toma olvidada -> la más antigua (oldest first).
     *  3. Si hoy hay alguna toma pendiente o pospuesta -> la primera por
     *     `effectiveTime`.
     *  4. Si no, próxima toma futura calculada con [NextDoseCalculator].
     *  5. Si tampoco hay próxima -> [NextDoseWidgetState.AllDone].
     */
    fun nextDoseState(): NextDoseWidgetState {
        if (medications.isEmpty()) return NextDoseWidgetState.NoMedications

        val todayDoses = generateTodayDoses()
        val highlight = pickHighlight(todayDoses)
        if (highlight != null) {
            return NextDoseWidgetState.Dose(toItem(highlight, isToday = true))
        }

        val futureItem = nextFutureDoseItem()
        return if (futureItem != null) NextDoseWidgetState.Dose(futureItem) else NextDoseWidgetState.AllDone
    }

    /**
     * Devuelve el estado del widget mediano "Hoy".
     */
    fun todaySummaryState(maxUpcoming: Int = 3): TodaySummaryWidgetState {
        if (medications.isEmpty()) return TodaySummaryWidgetState.NoMedications

        val todayDoses = generateTodayDoses()

        val total = todayDoses.size
        val taken = todayDoses.count { it.status == ScheduledDoseStatus.TAKEN }
        val missed = todayDoses.count { it.status == ScheduledDoseStatus.MISSED }
        val postponed = todayDoses.count { it.status == ScheduledDoseStatus.POSTPONED }
        val pending = todayDoses.count { it.status == ScheduledDoseStatus.PENDING }

        val summary = TodayWidgetSummary(
            total = total,
            taken = taken,
            pending = pending,
            missed = missed,
            postponed = postponed
        )

        val highlightDose = pickHighlight(todayDoses)
        val highlightItem = highlightDose?.let { toItem(it, isToday = true) }
            ?: nextFutureDoseItem()

        // El resto de las próximas: pendientes/pospuestas hoy ordenadas, sin
        // incluir la dosis ya destacada.
        val upcoming = todayDoses
            .asSequence()
            .filter { it.status == ScheduledDoseStatus.PENDING || it.status == ScheduledDoseStatus.POSTPONED }
            .sortedBy { it.effectiveTime }
            .filter { dose ->
                highlightDose == null ||
                    dose.medicationId != highlightDose.medicationId ||
                    dose.scheduledAt != highlightDose.scheduledAt
            }
            .take(maxUpcoming)
            .map { toItem(it, isToday = true) }
            .toList()

        return TodaySummaryWidgetState.Summary(
            summary = summary,
            highlight = highlightItem,
            upcoming = upcoming
        )
    }

    private fun generateTodayDoses(): List<ScheduledDose> {
        return ScheduledDoseGenerator(medications, logs, now)
            .generateDosesForDate(now.toLocalDate())
    }

    /**
     * Selecciona la dosis a destacar:
     *  - Olvidada más antigua si existe.
     *  - En su defecto, la pendiente/pospuesta cuya `effectiveTime` esté más
     *    próxima en el futuro (o la más reciente si todas son pasadas).
     */
    private fun pickHighlight(todayDoses: List<ScheduledDose>): ScheduledDose? {
        val missed = todayDoses
            .filter { it.status == ScheduledDoseStatus.MISSED }
            .minByOrNull { it.scheduledAt }
        if (missed != null) return missed

        val actionable = todayDoses.filter {
            it.status == ScheduledDoseStatus.PENDING || it.status == ScheduledDoseStatus.POSTPONED
        }
        if (actionable.isEmpty()) return null

        return actionable
            .filter { !it.effectiveTime.isBefore(now) }
            .minByOrNull { it.effectiveTime }
            ?: actionable.minByOrNull { it.effectiveTime }
    }

    /**
     * Próxima toma futura calculada para todos los medicamentos. Sirve como
     * fallback cuando ya no quedan dosis accionables hoy.
     */
    private fun nextFutureDoseItem(): NextDoseWidgetItem? {
        val candidates = medications.mapNotNull { med ->
            NextDoseCalculator.nextDose(med, now)?.let { dt -> med to dt }
        }
        val (med, dt) = candidates.minByOrNull { it.second } ?: return null

        return NextDoseWidgetItem(
            medicationId = med.id,
            medicationName = med.name,
            dosageText = formatDosage(med.dosage, med.unit),
            timeText = formatTime(dt),
            scheduledDateTime = dt,
            status = NextDoseWidgetItemStatus.UPCOMING_FUTURE,
            isToday = dt.toLocalDate() == now.toLocalDate()
        )
    }

    private fun toItem(dose: ScheduledDose, isToday: Boolean): NextDoseWidgetItem {
        val displayTime = dose.effectiveTime
        return NextDoseWidgetItem(
            medicationId = dose.medicationId,
            medicationName = dose.medicationName,
            dosageText = formatDosage(dose.medication.dosage, dose.medication.unit),
            timeText = formatTime(displayTime),
            scheduledDateTime = dose.scheduledAt,
            status = when (dose.status) {
                ScheduledDoseStatus.PENDING -> NextDoseWidgetItemStatus.PENDING
                ScheduledDoseStatus.TAKEN -> NextDoseWidgetItemStatus.TAKEN
                ScheduledDoseStatus.MISSED -> NextDoseWidgetItemStatus.MISSED
                ScheduledDoseStatus.POSTPONED -> NextDoseWidgetItemStatus.POSTPONED
            },
            isToday = isToday
        )
    }
}
