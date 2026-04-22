package com.ignaciovalero.saludario.domain.scheduling

import com.ignaciovalero.saludario.data.local.entity.MedicationEntity
import com.ignaciovalero.saludario.data.local.entity.MedicationScheduleType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class IntervalDoseScheduleTest {

    @Test
    fun `dosesForDate - start today 20 every 6h`() {
        val med = makeIntervalMed(
            startDate = LocalDate.of(2026, 4, 20),
            startTime = LocalTime.of(20, 0),
            intervalHours = 6
        )

        val todayDoses = IntervalDoseSchedule.dosesForDate(med, LocalDate.of(2026, 4, 20))
        assertEquals(
            listOf(LocalDateTime.of(2026, 4, 20, 20, 0)),
            todayDoses
        )

        val tomorrowDoses = IntervalDoseSchedule.dosesForDate(med, LocalDate.of(2026, 4, 21))
        assertEquals(
            listOf(
                LocalDateTime.of(2026, 4, 21, 2, 0),
                LocalDateTime.of(2026, 4, 21, 8, 0),
                LocalDateTime.of(2026, 4, 21, 14, 0),
                LocalDateTime.of(2026, 4, 21, 20, 0)
            ),
            tomorrowDoses
        )
    }

    @Test
    fun `dosesForDate - no doses before start date`() {
        val med = makeIntervalMed(
            startDate = LocalDate.of(2026, 4, 20),
            startTime = LocalTime.of(20, 0),
            intervalHours = 6
        )

        val previousDayDoses = IntervalDoseSchedule.dosesForDate(med, LocalDate.of(2026, 4, 19))
        assertEquals(emptyList<LocalDateTime>(), previousDayDoses)
    }

    @Test
    fun `nextDose - returns start when after is before start`() {
        val med = makeIntervalMed(
            startDate = LocalDate.of(2026, 4, 20),
            startTime = LocalTime.of(20, 0),
            intervalHours = 6
        )

        val next = IntervalDoseSchedule.nextDose(med, LocalDateTime.of(2026, 4, 20, 19, 0))
        assertEquals(LocalDateTime.of(2026, 4, 20, 20, 0), next)
    }

    @Test
    fun `nextDose - exact schedule jumps to next interval`() {
        val med = makeIntervalMed(
            startDate = LocalDate.of(2026, 4, 20),
            startTime = LocalTime.of(20, 0),
            intervalHours = 6
        )

        val next = IntervalDoseSchedule.nextDose(med, LocalDateTime.of(2026, 4, 20, 20, 0))
        assertEquals(LocalDateTime.of(2026, 4, 21, 2, 0), next)
    }

    private fun makeIntervalMed(
        startDate: LocalDate,
        startTime: LocalTime,
        intervalHours: Int
    ): MedicationEntity {
        return MedicationEntity(
            id = 1,
            name = "Med",
            dosage = 1.0,
            unit = "tableta",
            scheduleType = MedicationScheduleType.INTERVAL,
            // first element is the anchor (selected start time)
            times = listOf(startTime),
            startDate = startDate,
            endDate = null,
            specificDays = null,
            intervalHours = intervalHours
        )
    }
}
