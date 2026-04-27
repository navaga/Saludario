package com.ignaciovalero.saludario.domain.scheduling

import com.ignaciovalero.saludario.core.DoseConstants.GRACE_PERIOD_MINUTES
import com.ignaciovalero.saludario.data.local.entity.MedicationEntity
import com.ignaciovalero.saludario.data.local.entity.MedicationLogEntity
import com.ignaciovalero.saludario.data.local.entity.MedicationScheduleType
import com.ignaciovalero.saludario.data.local.entity.MedicationStatus
import java.time.LocalDate
import java.time.LocalDateTime

class ScheduledDoseGenerator(
    private val medications: List<MedicationEntity>,
    private val logs: List<MedicationLogEntity>,
    private val now: LocalDateTime
) {

    fun generateDosesForDate(date: LocalDate): List<ScheduledDose> {
        val targetLogs = logs.filter { it.scheduledTime.toLocalDate() == date }

        val dayMedications = medications.filter { med ->
            if (date.isBefore(med.startDate)) return@filter false
            if (med.endDate != null && date.isAfter(med.endDate)) return@filter false

            when (med.scheduleType) {
                MedicationScheduleType.SPECIFIC_DAYS -> {
                    val selectedDays = med.specificDays ?: return@filter false
                    date.dayOfWeek in selectedDays
                }

                else -> true
            }
        }

        return dayMedications
            .flatMap { med ->
                val scheduledDateTimes = when (med.scheduleType) {
                    MedicationScheduleType.INTERVAL -> IntervalDoseSchedule.dosesForDate(med, date)
                    else -> med.times.map { time -> LocalDateTime.of(date, time) }
                }

                scheduledDateTimes.mapNotNull { scheduledDateTime ->
                    val log = targetLogs.find {
                        it.medicationId == med.id && it.scheduledTime.toLocalTime() == scheduledDateTime.toLocalTime()
                    }

                    // Si la medicación empieza hoy y la dosis ya pasó el periodo de gracia
                    // sin ningún log previo, se omite para no mostrarla como olvidada
                    if (log == null &&
                        med.startDate == date &&
                        scheduledDateTime.plusMinutes(GRACE_PERIOD_MINUTES).isBefore(now)
                    ) {
                        return@mapNotNull null
                    }

                    val status = when {
                        log?.status == MedicationStatus.TAKEN -> ScheduledDoseStatus.TAKEN
                        log?.status == MedicationStatus.MISSED -> ScheduledDoseStatus.MISSED
                        log?.status == MedicationStatus.POSTPONED -> {
                            val until = log.postponedUntil
                            if (until != null && until.plusMinutes(GRACE_PERIOD_MINUTES).isBefore(now)) {
                                ScheduledDoseStatus.MISSED
                            } else {
                                ScheduledDoseStatus.POSTPONED
                            }
                        }
                        scheduledDateTime.isBefore(now) && log?.status != MedicationStatus.TAKEN -> ScheduledDoseStatus.MISSED
                        else -> ScheduledDoseStatus.PENDING
                    }

                    ScheduledDose(
                        medication = ScheduledDoseMedication(
                            id = med.id,
                            name = med.name,
                            dosage = med.dosage,
                            unit = med.unit
                        ),
                        scheduledAt = scheduledDateTime,
                        status = status,
                        logId = log?.id,
                        takenAt = log?.takenTime,
                        postponedUntil = log?.postponedUntil
                    )
                }
            }
            .sortedBy { it.effectiveTime }
    }
}
