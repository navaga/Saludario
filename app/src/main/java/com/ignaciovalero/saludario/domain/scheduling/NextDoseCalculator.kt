package com.ignaciovalero.saludario.domain.scheduling

import com.ignaciovalero.saludario.data.local.entity.MedicationEntity
import com.ignaciovalero.saludario.data.local.entity.MedicationScheduleType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

object NextDoseCalculator {

    /**
     * Calculates the next scheduled [LocalDateTime] for a medication after [after].
     * Returns null if the medication has ended or no valid next time exists.
     */
    fun nextDose(medication: MedicationEntity, after: LocalDateTime): LocalDateTime? {
        return when (medication.scheduleType) {
            MedicationScheduleType.DAILY -> nextDaily(medication.times, after, medication.endDate)
            MedicationScheduleType.SPECIFIC_DAYS -> nextSpecificDays(
                medication.times, medication.specificDays ?: return null, after, medication.endDate
            )
            MedicationScheduleType.INTERVAL -> nextInterval(
                medication, after
            )
        }
    }

    private fun nextDaily(
        times: List<LocalTime>,
        after: LocalDateTime,
        endDate: LocalDate?
    ): LocalDateTime? {
        // Try remaining times today, then first time tomorrow
        val today = after.toLocalDate()
        val candidate = times.sorted()
            .firstOrNull { LocalDateTime.of(today, it).isAfter(after) }
            ?.let { LocalDateTime.of(today, it) }
            ?: times.minOrNull()?.let { LocalDateTime.of(today.plusDays(1), it) }
            ?: return null

        return candidate.takeUnless { endDate != null && it.toLocalDate().isAfter(endDate) }
    }

    private fun nextSpecificDays(
        times: List<LocalTime>,
        days: List<DayOfWeek>,
        after: LocalDateTime,
        endDate: LocalDate?
    ): LocalDateTime? {
        if (days.isEmpty()) return null
        val sortedTimes = times.sorted()
        val today = after.toLocalDate()

        // If today is a valid day, check remaining times
        if (today.dayOfWeek in days) {
            val todayCandidate = sortedTimes
                .firstOrNull { LocalDateTime.of(today, it).isAfter(after) }
            if (todayCandidate != null) {
                val dt = LocalDateTime.of(today, todayCandidate)
                return dt.takeUnless { endDate != null && it.toLocalDate().isAfter(endDate) }
            }
        }

        // Search next valid day (up to 7 days ahead)
        for (offset in 1L..7L) {
            val date = today.plusDays(offset)
            if (date.dayOfWeek in days) {
                val firstTime = sortedTimes.firstOrNull() ?: return null
                val dt = LocalDateTime.of(date, firstTime)
                return dt.takeUnless { endDate != null && it.toLocalDate().isAfter(endDate) }
            }
        }
        return null
    }

    private fun nextInterval(
        medication: MedicationEntity,
        after: LocalDateTime
    ): LocalDateTime? {
        val candidate = IntervalDoseSchedule.nextDose(medication, after) ?: return null
        return candidate.takeUnless { medication.endDate != null && it.toLocalDate().isAfter(medication.endDate) }
    }
}
