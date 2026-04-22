package com.ignaciovalero.saludario.domain.scheduling

import com.ignaciovalero.saludario.data.local.entity.MedicationEntity
import com.ignaciovalero.saludario.data.local.entity.MedicationScheduleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class NextDoseCalculatorTest {

    // ═══════════════════════════════════════════
    //  DAILY
    // ═══════════════════════════════════════════

    @Test
    fun `daily - next time today if not yet passed`() {
        val med = makeMed(MedicationScheduleType.DAILY, times = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)))
        // At 07:00 → next is 08:00 today
        val after = LocalDateTime.of(2026, 4, 20, 7, 0)
        val next = NextDoseCalculator.nextDose(med, after)
        assertEquals(LocalDateTime.of(2026, 4, 20, 8, 0), next)
    }

    @Test
    fun `daily - skips past times and picks later today`() {
        val med = makeMed(MedicationScheduleType.DAILY, times = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)))
        // At 10:00 → 08:00 passed, next is 20:00 today
        val after = LocalDateTime.of(2026, 4, 20, 10, 0)
        val next = NextDoseCalculator.nextDose(med, after)
        assertEquals(LocalDateTime.of(2026, 4, 20, 20, 0), next)
    }

    @Test
    fun `daily - wraps to next day when all times passed`() {
        val med = makeMed(MedicationScheduleType.DAILY, times = listOf(LocalTime.of(8, 0), LocalTime.of(14, 0)))
        // At 22:00 → both passed, next is 08:00 tomorrow
        val after = LocalDateTime.of(2026, 4, 20, 22, 0)
        val next = NextDoseCalculator.nextDose(med, after)
        assertEquals(LocalDateTime.of(2026, 4, 21, 8, 0), next)
    }

    @Test
    fun `daily - single time wraps to next day`() {
        val med = makeMed(MedicationScheduleType.DAILY, times = listOf(LocalTime.of(9, 0)))
        val after = LocalDateTime.of(2026, 4, 20, 9, 0) // Exactly at time (not after)
        val next = NextDoseCalculator.nextDose(med, after)
        assertEquals(LocalDateTime.of(2026, 4, 21, 9, 0), next)
    }

    @Test
    fun `daily - null if past end date`() {
        val med = makeMed(
            MedicationScheduleType.DAILY,
            times = listOf(LocalTime.of(8, 0)),
            endDate = LocalDate.of(2026, 4, 20)
        )
        val after = LocalDateTime.of(2026, 4, 20, 22, 0)
        // Next would be Apr 21 but endDate is Apr 20
        assertNull(NextDoseCalculator.nextDose(med, after))
    }

    @Test
    fun `daily - valid if within end date`() {
        val med = makeMed(
            MedicationScheduleType.DAILY,
            times = listOf(LocalTime.of(8, 0)),
            endDate = LocalDate.of(2026, 4, 21)
        )
        val after = LocalDateTime.of(2026, 4, 20, 22, 0)
        assertEquals(LocalDateTime.of(2026, 4, 21, 8, 0), NextDoseCalculator.nextDose(med, after))
    }

    @Test
    fun `daily - empty times returns null`() {
        val med = makeMed(MedicationScheduleType.DAILY, times = emptyList())
        assertNull(NextDoseCalculator.nextDose(med, LocalDateTime.of(2026, 4, 20, 10, 0)))
    }

    // ═══════════════════════════════════════════
    //  SPECIFIC DAYS
    // ═══════════════════════════════════════════

    @Test
    fun `specific days - today is valid day with future time`() {
        // April 20, 2026 = Monday
        val med = makeMed(
            MedicationScheduleType.SPECIFIC_DAYS,
            times = listOf(LocalTime.of(14, 0)),
            specificDays = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)
        )
        val after = LocalDateTime.of(2026, 4, 20, 10, 0) // Monday 10:00
        val next = NextDoseCalculator.nextDose(med, after)
        assertEquals(LocalDateTime.of(2026, 4, 20, 14, 0), next) // Monday 14:00
    }

    @Test
    fun `specific days - today is valid but time passed, skips to next valid day`() {
        // April 20, 2026 = Monday
        val med = makeMed(
            MedicationScheduleType.SPECIFIC_DAYS,
            times = listOf(LocalTime.of(8, 0)),
            specificDays = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)
        )
        val after = LocalDateTime.of(2026, 4, 20, 10, 0) // Monday 10:00, 08:00 passed
        val next = NextDoseCalculator.nextDose(med, after)
        assertEquals(LocalDateTime.of(2026, 4, 22, 8, 0), next) // Wednesday
    }

    @Test
    fun `specific days - today is not a valid day, finds next`() {
        // April 21, 2026 = Tuesday (not in list)
        val med = makeMed(
            MedicationScheduleType.SPECIFIC_DAYS,
            times = listOf(LocalTime.of(8, 0)),
            specificDays = listOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)
        )
        val after = LocalDateTime.of(2026, 4, 21, 10, 0) // Tuesday
        val next = NextDoseCalculator.nextDose(med, after)
        assertEquals(LocalDateTime.of(2026, 4, 24, 8, 0), next) // Friday
    }

    @Test
    fun `specific days - wraps around week`() {
        // April 24, 2026 = Friday
        val med = makeMed(
            MedicationScheduleType.SPECIFIC_DAYS,
            times = listOf(LocalTime.of(8, 0)),
            specificDays = listOf(DayOfWeek.MONDAY)
        )
        val after = LocalDateTime.of(2026, 4, 24, 10, 0) // Friday
        val next = NextDoseCalculator.nextDose(med, after)
        assertEquals(LocalDateTime.of(2026, 4, 27, 8, 0), next) // Next Monday
    }

    @Test
    fun `specific days - single day Saturday from Monday`() {
        // April 20, 2026 = Monday
        val med = makeMed(
            MedicationScheduleType.SPECIFIC_DAYS,
            times = listOf(LocalTime.of(9, 0)),
            specificDays = listOf(DayOfWeek.SATURDAY)
        )
        val after = LocalDateTime.of(2026, 4, 20, 10, 0)
        val next = NextDoseCalculator.nextDose(med, after)
        assertEquals(LocalDateTime.of(2026, 4, 25, 9, 0), next) // Saturday
    }

    @Test
    fun `specific days - null specificDays returns null`() {
        val med = makeMed(MedicationScheduleType.SPECIFIC_DAYS, specificDays = null)
        assertNull(NextDoseCalculator.nextDose(med, LocalDateTime.of(2026, 4, 20, 10, 0)))
    }

    @Test
    fun `specific days - empty days list returns null`() {
        val med = makeMed(MedicationScheduleType.SPECIFIC_DAYS, specificDays = emptyList())
        assertNull(NextDoseCalculator.nextDose(med, LocalDateTime.of(2026, 4, 20, 10, 0)))
    }

    @Test
    fun `specific days - respects end date`() {
        val med = makeMed(
            MedicationScheduleType.SPECIFIC_DAYS,
            times = listOf(LocalTime.of(8, 0)),
            specificDays = listOf(DayOfWeek.FRIDAY),
            endDate = LocalDate.of(2026, 4, 23) // Wed
        )
        val after = LocalDateTime.of(2026, 4, 20, 10, 0)
        // Next Friday is Apr 24, but endDate is Apr 23
        assertNull(NextDoseCalculator.nextDose(med, after))
    }

    @Test
    fun `specific days - picks earliest time on valid day`() {
        val med = makeMed(
            MedicationScheduleType.SPECIFIC_DAYS,
            times = listOf(LocalTime.of(20, 0), LocalTime.of(8, 0)),
            specificDays = listOf(DayOfWeek.WEDNESDAY)
        )
        // April 20 = Monday
        val after = LocalDateTime.of(2026, 4, 20, 22, 0)
        val next = NextDoseCalculator.nextDose(med, after)
        assertEquals(LocalDateTime.of(2026, 4, 22, 8, 0), next) // Wed 08:00 (earliest)
    }

    // ═══════════════════════════════════════════
    //  INTERVAL
    // ═══════════════════════════════════════════

    @Test
    fun `interval - next time today`() {
        val med = makeMed(
            MedicationScheduleType.INTERVAL,
            times = listOf(LocalTime.of(6, 0), LocalTime.of(14, 0), LocalTime.of(22, 0)),
            intervalHours = 8
        )
        val after = LocalDateTime.of(2026, 4, 20, 10, 0)
        val next = NextDoseCalculator.nextDose(med, after)
        assertEquals(LocalDateTime.of(2026, 4, 20, 14, 0), next)
    }

    @Test
    fun `interval - wraps to next day first time`() {
        val med = makeMed(
            MedicationScheduleType.INTERVAL,
            times = listOf(LocalTime.of(6, 0), LocalTime.of(14, 0), LocalTime.of(22, 0)),
            intervalHours = 8
        )
        val after = LocalDateTime.of(2026, 4, 20, 23, 0) // All passed
        val next = NextDoseCalculator.nextDose(med, after)
        assertEquals(LocalDateTime.of(2026, 4, 21, 6, 0), next) // Tomorrow first slot
    }

    @Test
    fun `interval - picks correct slot when between slots`() {
        val med = makeMed(
            MedicationScheduleType.INTERVAL,
            times = listOf(LocalTime.of(8, 0), LocalTime.of(16, 0), LocalTime.MIDNIGHT),
            intervalHours = 8
        )
        val after = LocalDateTime.of(2026, 4, 20, 16, 0) // Exactly at 16:00
        val next = NextDoseCalculator.nextDose(med, after)
        assertEquals(LocalDateTime.of(2026, 4, 21, 0, 0), next)
    }

    @Test
    fun `interval - anchored to startDate and first time`() {
        val med = makeMed(
            MedicationScheduleType.INTERVAL,
            times = listOf(LocalTime.of(20, 0), LocalTime.of(2, 0), LocalTime.of(8, 0), LocalTime.of(14, 0)),
            intervalHours = 6,
            startDate = LocalDate.of(2026, 4, 20)
        )

        val afterBeforeStart = LocalDateTime.of(2026, 4, 20, 19, 0)
        assertEquals(
            LocalDateTime.of(2026, 4, 20, 20, 0),
            NextDoseCalculator.nextDose(med, afterBeforeStart)
        )

        val afterStart = LocalDateTime.of(2026, 4, 20, 20, 0)
        assertEquals(
            LocalDateTime.of(2026, 4, 21, 2, 0),
            NextDoseCalculator.nextDose(med, afterStart)
        )
    }

    @Test
    fun `interval - null intervalHours returns null`() {
        val med = makeMed(MedicationScheduleType.INTERVAL, intervalHours = null)
        assertNull(NextDoseCalculator.nextDose(med, LocalDateTime.of(2026, 4, 20, 10, 0)))
    }

    @Test
    fun `interval - respects end date`() {
        val med = makeMed(
            MedicationScheduleType.INTERVAL,
            times = listOf(LocalTime.of(6, 0), LocalTime.of(18, 0)),
            intervalHours = 12,
            endDate = LocalDate.of(2026, 4, 20)
        )
        val after = LocalDateTime.of(2026, 4, 20, 20, 0)
        assertNull(NextDoseCalculator.nextDose(med, after))
    }

    @Test
    fun `interval every 24h - single slot wraps to next day`() {
        val med = makeMed(
            MedicationScheduleType.INTERVAL,
            times = listOf(LocalTime.of(9, 0)),
            intervalHours = 24
        )
        val after = LocalDateTime.of(2026, 4, 20, 9, 30)
        val next = NextDoseCalculator.nextDose(med, after)
        assertEquals(LocalDateTime.of(2026, 4, 21, 9, 0), next)
    }

    // ═══════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════

    private fun makeMed(
        scheduleType: MedicationScheduleType,
        times: List<LocalTime> = listOf(LocalTime.of(8, 0)),
        specificDays: List<DayOfWeek>? = null,
        intervalHours: Int? = null,
        startDate: LocalDate = LocalDate.of(2026, 1, 1),
        endDate: LocalDate? = null
    ) = MedicationEntity(
        id = 1L,
        name = "TestMed",
        dosage = 1.0,
        unit = "tableta",
        scheduleType = scheduleType,
        times = times,
        startDate = startDate,
        endDate = endDate,
        specificDays = specificDays,
        intervalHours = intervalHours
    )
}
