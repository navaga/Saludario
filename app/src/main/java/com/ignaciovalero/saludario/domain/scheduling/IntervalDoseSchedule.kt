package com.ignaciovalero.saludario.domain.scheduling

import com.ignaciovalero.saludario.data.local.entity.MedicationEntity
import com.ignaciovalero.saludario.data.local.entity.MedicationScheduleType
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

object IntervalDoseSchedule {

    fun nextDose(medication: MedicationEntity, after: LocalDateTime): LocalDateTime? {
        if (medication.scheduleType != MedicationScheduleType.INTERVAL) return null
        val intervalHours = medication.intervalHours ?: return null
        val start = startDateTime(medication) ?: return null
        if (intervalHours <= 0) return null

        if (after.isBefore(start)) return start

        val intervalMinutes = intervalHours * 60L
        val elapsedMinutes = Duration.between(start, after).toMinutes()
        val steps = (elapsedMinutes / intervalMinutes) + 1
        return start.plusMinutes(steps * intervalMinutes)
    }

    fun dosesForDate(medication: MedicationEntity, date: LocalDate): List<LocalDateTime> {
        if (medication.scheduleType != MedicationScheduleType.INTERVAL) return emptyList()
        val intervalHours = medication.intervalHours ?: return emptyList()
        val start = startDateTime(medication) ?: return emptyList()
        if (intervalHours <= 0) return emptyList()
        if (date.isBefore(medication.startDate)) return emptyList()
        if (medication.endDate != null && date.isAfter(medication.endDate)) return emptyList()

        val dayStart = LocalDateTime.of(date, LocalTime.MIN)
        val dayEnd = LocalDateTime.of(date, LocalTime.MAX)
        val from = if (dayStart.isAfter(start)) dayStart else start
        val first = firstAtOrAfter(start, from, intervalHours)

        val doses = mutableListOf<LocalDateTime>()
        var current = first
        while (!current.isAfter(dayEnd)) {
            if (medication.endDate == null || !current.toLocalDate().isAfter(medication.endDate)) {
                doses.add(current)
            }
            current = current.plusHours(intervalHours.toLong())
        }

        return doses
    }

    private fun startDateTime(medication: MedicationEntity): LocalDateTime? {
        val startTime = medication.times.firstOrNull() ?: return null
        return LocalDateTime.of(medication.startDate, startTime)
    }

    private fun firstAtOrAfter(
        anchor: LocalDateTime,
        boundary: LocalDateTime,
        intervalHours: Int
    ): LocalDateTime {
        if (!boundary.isAfter(anchor)) return anchor

        val intervalMinutes = intervalHours * 60L
        val elapsedMinutes = Duration.between(anchor, boundary).toMinutes()
        val steps = (elapsedMinutes + intervalMinutes - 1) / intervalMinutes
        return anchor.plusMinutes(steps * intervalMinutes)
    }
}