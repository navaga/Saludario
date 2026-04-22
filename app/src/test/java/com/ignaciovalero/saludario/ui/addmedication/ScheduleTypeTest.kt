package com.ignaciovalero.saludario.ui.addmedication

import com.ignaciovalero.saludario.R
import com.ignaciovalero.saludario.data.local.entity.MedicationEntity
import com.ignaciovalero.saludario.data.local.entity.MedicationScheduleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class ScheduleTypeTest {

    // ═══════════════════════════════════════════
    //  VALIDACIÓN DEL FORMULARIO
    // ═══════════════════════════════════════════

    // ── Diario ──

    @Test
    fun `daily - valid when time is selected`() {
        assertNull(validateTime(LocalTime.of(8, 0)))
        assertNull(validateDays(MedicationScheduleType.DAILY, emptySet()))
        assertNull(validateInterval(MedicationScheduleType.DAILY, ""))
    }

    @Test
    fun `daily - no error for days even if empty`() {
        assertNull(validateDays(MedicationScheduleType.DAILY, emptySet()))
    }

    @Test
    fun `daily - no error for interval even if empty`() {
        assertNull(validateInterval(MedicationScheduleType.DAILY, ""))
    }

    // ── Días específicos ──

    @Test
    fun `specific days - error when no days selected`() {
        assertEquals(R.string.error_days_empty, validateDays(MedicationScheduleType.SPECIFIC_DAYS, emptySet()))
    }

    @Test
    fun `specific days - valid with one day`() {
        assertNull(validateDays(MedicationScheduleType.SPECIFIC_DAYS, setOf(DayOfWeek.MONDAY)))
    }

    @Test
    fun `specific days - valid with multiple days`() {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        assertNull(validateDays(MedicationScheduleType.SPECIFIC_DAYS, days))
    }

    @Test
    fun `specific days - valid with all days`() {
        assertNull(validateDays(MedicationScheduleType.SPECIFIC_DAYS, DayOfWeek.entries.toSet()))
    }

    @Test
    fun `specific days - no interval error`() {
        assertNull(validateInterval(MedicationScheduleType.SPECIFIC_DAYS, ""))
    }

    // ── Intervalo ──

    @Test
    fun `interval - error when hours empty`() {
        assertEquals(R.string.error_interval_empty, validateInterval(MedicationScheduleType.INTERVAL, ""))
    }

    @Test
    fun `interval - error when hours not a number`() {
        assertEquals(R.string.error_interval_invalid, validateInterval(MedicationScheduleType.INTERVAL, "abc"))
    }

    @Test
    fun `interval - error when hours is 0`() {
        assertEquals(R.string.error_interval_range, validateInterval(MedicationScheduleType.INTERVAL, "0"))
    }

    @Test
    fun `interval - error when hours exceeds 24`() {
        assertEquals(R.string.error_interval_range, validateInterval(MedicationScheduleType.INTERVAL, "25"))
    }

    @Test
    fun `interval - valid at 1 hour`() {
        assertNull(validateInterval(MedicationScheduleType.INTERVAL, "1"))
    }

    @Test
    fun `interval - valid at 24 hours`() {
        assertNull(validateInterval(MedicationScheduleType.INTERVAL, "24"))
    }

    @Test
    fun `interval - valid at 8 hours`() {
        assertNull(validateInterval(MedicationScheduleType.INTERVAL, "8"))
    }

    @Test
    fun `interval - no days error`() {
        assertNull(validateDays(MedicationScheduleType.INTERVAL, emptySet()))
    }

    // ── Hora común ──

    @Test
    fun `time - error when null`() {
        assertEquals(R.string.error_time_empty, validateTime(null))
    }

    @Test
    fun `time - valid when provided`() {
        assertNull(validateTime(LocalTime.of(14, 30)))
    }

    // ═══════════════════════════════════════════
    //  CÁLCULO DE TIEMPOS DE INTERVALO
    // ═══════════════════════════════════════════

    @Test
    fun `interval every 8h from 06_00 gives 3 times`() {
        val times = computeIntervalTimes(LocalTime.of(6, 0), 8)
        assertEquals(
            listOf(LocalTime.of(6, 0), LocalTime.of(14, 0), LocalTime.of(22, 0)),
            times
        )
    }

    @Test
    fun `interval every 12h from 08_00 gives 2 times`() {
        val times = computeIntervalTimes(LocalTime.of(8, 0), 12)
        assertEquals(
            listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)),
            times
        )
    }

    @Test
    fun `interval every 6h from 00_00 gives 4 times`() {
        val times = computeIntervalTimes(LocalTime.of(0, 0), 6)
        assertEquals(
            listOf(
                LocalTime.of(0, 0), LocalTime.of(6, 0),
                LocalTime.of(12, 0), LocalTime.of(18, 0)
            ),
            times
        )
    }

    @Test
    fun `interval every 24h gives single time`() {
        val times = computeIntervalTimes(LocalTime.of(9, 0), 24)
        assertEquals(listOf(LocalTime.of(9, 0)), times)
    }

    @Test
    fun `interval every 1h gives 24 times`() {
        val times = computeIntervalTimes(LocalTime.of(0, 0), 1)
        assertEquals(24, times.size)
        assertEquals(LocalTime.of(0, 0), times.first())
        assertEquals(LocalTime.of(23, 0), times.last())
    }

    @Test
    fun `interval preserves minutes from start time`() {
        val times = computeIntervalTimes(LocalTime.of(7, 30), 8)
        assertEquals(
            listOf(LocalTime.of(7, 30), LocalTime.of(15, 30), LocalTime.of(23, 30)),
            times
        )
    }

    @Test
    fun `interval 5h from 04_00 stops before wrapping`() {
        // 04:00 +5=09:00, +10=14:00, +15=19:00, +20=00:00 (20<24 so included)
        val times = computeIntervalTimes(LocalTime.of(4, 0), 5)
        assertEquals(
            listOf(
                LocalTime.of(4, 0), LocalTime.of(9, 0),
                LocalTime.of(14, 0), LocalTime.of(19, 0),
                LocalTime.MIDNIGHT
            ),
            times
        )
    }

    // ═══════════════════════════════════════════
    //  FILTRADO DE GENERACIÓN DE DOSIS POR TIPO
    // ═══════════════════════════════════════════

    @Test
    fun `daily medication generates doses every day`() {
        val med = makeMed(MedicationScheduleType.DAILY)
        // Mon through Sun — all should generate
        DayOfWeek.entries.forEach { day ->
            val date = dateForDayOfWeek(day)
            assertTrue("Daily should generate on $day", shouldGenerateDoses(med, date))
        }
    }

    @Test
    fun `specific days medication generates only on selected days`() {
        val med = makeMed(
            MedicationScheduleType.SPECIFIC_DAYS,
            specificDays = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        )
        val monday = dateForDayOfWeek(DayOfWeek.MONDAY)
        val tuesday = dateForDayOfWeek(DayOfWeek.TUESDAY)
        val wednesday = dateForDayOfWeek(DayOfWeek.WEDNESDAY)
        val thursday = dateForDayOfWeek(DayOfWeek.THURSDAY)
        val friday = dateForDayOfWeek(DayOfWeek.FRIDAY)
        val saturday = dateForDayOfWeek(DayOfWeek.SATURDAY)
        val sunday = dateForDayOfWeek(DayOfWeek.SUNDAY)

        assertTrue(shouldGenerateDoses(med, monday))
        assertTrue(!shouldGenerateDoses(med, tuesday))
        assertTrue(shouldGenerateDoses(med, wednesday))
        assertTrue(!shouldGenerateDoses(med, thursday))
        assertTrue(shouldGenerateDoses(med, friday))
        assertTrue(!shouldGenerateDoses(med, saturday))
        assertTrue(!shouldGenerateDoses(med, sunday))
    }

    @Test
    fun `specific days with null specificDays skips`() {
        val med = makeMed(MedicationScheduleType.SPECIFIC_DAYS, specificDays = null)
        val monday = dateForDayOfWeek(DayOfWeek.MONDAY)
        assertTrue(!shouldGenerateDoses(med, monday))
    }

    @Test
    fun `specific days all days selected generates every day`() {
        val med = makeMed(
            MedicationScheduleType.SPECIFIC_DAYS,
            specificDays = DayOfWeek.entries
        )
        DayOfWeek.entries.forEach { day ->
            val date = dateForDayOfWeek(day)
            assertTrue("All days should generate on $day", shouldGenerateDoses(med, date))
        }
    }

    @Test
    fun `specific days single day only generates on that day`() {
        val med = makeMed(
            MedicationScheduleType.SPECIFIC_DAYS,
            specificDays = listOf(DayOfWeek.SATURDAY)
        )
        DayOfWeek.entries.forEach { day ->
            val date = dateForDayOfWeek(day)
            if (day == DayOfWeek.SATURDAY) {
                assertTrue(shouldGenerateDoses(med, date))
            } else {
                assertTrue(!shouldGenerateDoses(med, date))
            }
        }
    }

    @Test
    fun `interval medication generates every day`() {
        val med = makeMed(
            MedicationScheduleType.INTERVAL,
            intervalHours = 8,
            times = listOf(LocalTime.of(6, 0), LocalTime.of(14, 0), LocalTime.of(22, 0))
        )
        DayOfWeek.entries.forEach { day ->
            val date = dateForDayOfWeek(day)
            assertTrue("Interval should generate on $day", shouldGenerateDoses(med, date))
        }
    }

    @Test
    fun `interval medication has multiple times`() {
        val med = makeMed(
            MedicationScheduleType.INTERVAL,
            intervalHours = 8,
            times = listOf(LocalTime.of(6, 0), LocalTime.of(14, 0), LocalTime.of(22, 0))
        )
        assertEquals(3, med.times.size)
    }

    // ═══════════════════════════════════════════
    //  INTEGRACIÓN: entidad se construye correctamente
    // ═══════════════════════════════════════════

    @Test
    fun `daily entity has no specificDays and no intervalHours`() {
        val med = makeMed(MedicationScheduleType.DAILY)
        assertNull(med.specificDays)
        assertNull(med.intervalHours)
        assertEquals(1, med.times.size)
    }

    @Test
    fun `specific days entity has specificDays and no intervalHours`() {
        val days = listOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY)
        val med = makeMed(MedicationScheduleType.SPECIFIC_DAYS, specificDays = days)
        assertNotNull(med.specificDays)
        assertEquals(days, med.specificDays)
        assertNull(med.intervalHours)
    }

    @Test
    fun `interval entity has intervalHours and multiple times`() {
        val times = computeIntervalTimes(LocalTime.of(8, 0), 6)
        val med = makeMed(MedicationScheduleType.INTERVAL, intervalHours = 6, times = times)
        assertEquals(6, med.intervalHours)
        assertEquals(4, med.times.size) // 08, 14, 20, 02 → stops at 20 (3*6=18<24, 4*6=24 not <24)
        assertNull(med.specificDays)
    }

    // ═══════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════

    /**
     * Replicates the interval times computation from AddMedicationViewModel.
     */
    private fun computeIntervalTimes(start: LocalTime, intervalH: Int): List<LocalTime> {
        val result = mutableListOf(start)
        var totalHours = intervalH
        while (totalHours < 24) {
            result.add(start.plusHours(totalHours.toLong()))
            totalHours += intervalH
        }
        return result
    }

    /**
     * Replicates the dose generation filtering from DailyDoseGenerationWorker.
     */
    private fun shouldGenerateDoses(medication: MedicationEntity, date: LocalDate): Boolean {
        if (medication.scheduleType == MedicationScheduleType.SPECIFIC_DAYS) {
            val selectedDays = medication.specificDays ?: return false
            if (date.dayOfWeek !in selectedDays) return false
        }
        return true
    }

    private fun makeMed(
        scheduleType: MedicationScheduleType,
        specificDays: List<DayOfWeek>? = null,
        intervalHours: Int? = null,
        times: List<LocalTime> = listOf(LocalTime.of(8, 0))
    ) = MedicationEntity(
        id = 1L,
        name = "TestMed",
        dosage = 1.0,
        unit = "tableta",
        scheduleType = scheduleType,
        times = times,
        startDate = LocalDate.of(2026, 1, 1),
        specificDays = specificDays,
        intervalHours = intervalHours
    )

    /**
     * Returns a date in April 2026 that falls on the given day of week.
     * April 2026: Mon=6, Tue=7, Wed=1, Thu=2, Fri=3, Sat=4, Sun=5
     */
    private fun dateForDayOfWeek(day: DayOfWeek): LocalDate {
        val april1 = LocalDate.of(2026, 4, 1) // Wednesday
        val diff = (day.value - april1.dayOfWeek.value + 7) % 7
        return april1.plusDays(diff.toLong())
    }
}
