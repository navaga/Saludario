package com.ignaciovalero.saludario.ui.widget

import com.ignaciovalero.saludario.data.local.entity.MedicationEntity
import com.ignaciovalero.saludario.data.local.entity.MedicationLogEntity
import com.ignaciovalero.saludario.data.local.entity.MedicationScheduleType
import com.ignaciovalero.saludario.data.local.entity.MedicationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class MedicationWidgetDataManagerTest {

    private val today: LocalDate = LocalDate.of(2026, 4, 20) // Lunes
    private val now: LocalDateTime = LocalDateTime.of(today, LocalTime.of(10, 0))

    @Test
    fun `nextDoseState - sin medicamentos devuelve NoMedications`() {
        val state = manager(medications = emptyList(), logs = emptyList()).nextDoseState()
        assertEquals(NextDoseWidgetState.NoMedications, state)
    }

    @Test
    fun `nextDoseState - una toma pendiente posterior se elige como destacada`() {
        val med = dailyMed(id = 1, times = listOf(LocalTime.of(8, 0), LocalTime.of(14, 0)))
        val state = manager(medications = listOf(med), logs = emptyList()).nextDoseState()

        // 08:00 ya pasó (sin log) → es MISSED y debería tener prioridad sobre 14:00.
        val item = (state as NextDoseWidgetState.Dose).item
        assertEquals(NextDoseWidgetItemStatus.MISSED, item.status)
        assertEquals(LocalTime.of(8, 0), item.scheduledDateTime.toLocalTime())
    }

    @Test
    fun `nextDoseState - olvidada gana a pendiente futura`() {
        val med = dailyMed(id = 1, times = listOf(LocalTime.of(8, 0), LocalTime.of(14, 0)))
        // Dejamos 08:00 sin log (será MISSED) y nada para 14:00 → 08:00 prioritaria
        val state = manager(medications = listOf(med), logs = emptyList()).nextDoseState()
        val item = (state as NextDoseWidgetState.Dose).item
        assertEquals(NextDoseWidgetItemStatus.MISSED, item.status)
    }

    @Test
    fun `nextDoseState - dosis tomada no aparece, se muestra la siguiente pendiente`() {
        val med = dailyMed(id = 1, times = listOf(LocalTime.of(8, 0), LocalTime.of(14, 0)))
        val takenLog = MedicationLogEntity(
            id = 10,
            medicationId = 1,
            scheduledTime = LocalDateTime.of(today, LocalTime.of(8, 0)),
            takenTime = LocalDateTime.of(today, LocalTime.of(8, 5)),
            status = MedicationStatus.TAKEN
        )
        val state = manager(medications = listOf(med), logs = listOf(takenLog)).nextDoseState()
        val item = (state as NextDoseWidgetState.Dose).item
        assertEquals(NextDoseWidgetItemStatus.PENDING, item.status)
        assertEquals(LocalTime.of(14, 0), item.scheduledDateTime.toLocalTime())
    }

    @Test
    fun `nextDoseState - todas tomadas hoy devuelve siguiente futura`() {
        val med = dailyMed(id = 1, times = listOf(LocalTime.of(8, 0)))
        val takenLog = MedicationLogEntity(
            id = 10,
            medicationId = 1,
            scheduledTime = LocalDateTime.of(today, LocalTime.of(8, 0)),
            takenTime = LocalDateTime.of(today, LocalTime.of(8, 5)),
            status = MedicationStatus.TAKEN
        )
        val state = manager(medications = listOf(med), logs = listOf(takenLog)).nextDoseState()
        val item = (state as NextDoseWidgetState.Dose).item
        assertEquals(NextDoseWidgetItemStatus.UPCOMING_FUTURE, item.status)
        assertEquals(today.plusDays(1), item.scheduledDateTime.toLocalDate())
    }

    @Test
    fun `nextDoseState - postponedUntil futuro se prioriza por effectiveTime`() {
        val med = dailyMed(id = 1, times = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)))
        val postponedLog = MedicationLogEntity(
            id = 10,
            medicationId = 1,
            scheduledTime = LocalDateTime.of(today, LocalTime.of(8, 0)),
            status = MedicationStatus.POSTPONED,
            postponedUntil = LocalDateTime.of(today, LocalTime.of(11, 0)) // futuro
        )
        val state = manager(medications = listOf(med), logs = listOf(postponedLog)).nextDoseState()
        val item = (state as NextDoseWidgetState.Dose).item
        // La pospuesta a 11:00 es más próxima que la pendiente futura de 20:00
        assertEquals(NextDoseWidgetItemStatus.POSTPONED, item.status)
    }

    @Test
    fun `nextDoseState - sin tomas hoy y sin futuras devuelve AllDone`() {
        // Medicamento que termina ayer → NextDoseCalculator devolverá null.
        val med = dailyMed(
            id = 1,
            times = listOf(LocalTime.of(8, 0)),
            endDate = today.minusDays(1)
        )
        val state = manager(medications = listOf(med), logs = emptyList()).nextDoseState()
        assertEquals(NextDoseWidgetState.AllDone, state)
    }

    @Test
    fun `todaySummaryState - cuenta tomadas pendientes y olvidadas`() {
        val med = dailyMed(id = 1, times = listOf(LocalTime.of(8, 0), LocalTime.of(14, 0), LocalTime.of(20, 0)))
        val takenLog = MedicationLogEntity(
            id = 10,
            medicationId = 1,
            scheduledTime = LocalDateTime.of(today, LocalTime.of(8, 0)),
            takenTime = LocalDateTime.of(today, LocalTime.of(8, 5)),
            status = MedicationStatus.TAKEN
        )
        val state = manager(medications = listOf(med), logs = listOf(takenLog))
            .todaySummaryState() as TodaySummaryWidgetState.Summary

        assertEquals(3, state.summary.total)
        assertEquals(1, state.summary.taken)
        // 14:00 sigue siendo pending (no ha pasado), 20:00 también.
        assertEquals(2, state.summary.pending)
        assertEquals(0, state.summary.missed)
    }

    @Test
    fun `todaySummaryState - lista de upcoming excluye la destacada`() {
        val med = dailyMed(id = 1, times = listOf(LocalTime.of(11, 0), LocalTime.of(14, 0), LocalTime.of(20, 0)))
        val state = manager(medications = listOf(med), logs = emptyList())
            .todaySummaryState() as TodaySummaryWidgetState.Summary

        // 11:00 es la más cercana en el futuro → highlight
        val highlight = state.highlight
        assertNotNull(highlight)
        assertEquals(LocalTime.of(11, 0), highlight!!.scheduledDateTime.toLocalTime())
        // upcoming debe contener 14:00 y 20:00, no 11:00
        val upcomingTimes = state.upcoming.map { it.scheduledDateTime.toLocalTime() }
        assertTrue(upcomingTimes.contains(LocalTime.of(14, 0)))
        assertTrue(upcomingTimes.contains(LocalTime.of(20, 0)))
        assertTrue(!upcomingTimes.contains(LocalTime.of(11, 0)))
    }

    @Test
    fun `todaySummaryState - sin medicamentos devuelve NoMedications`() {
        val state = manager(medications = emptyList(), logs = emptyList()).todaySummaryState()
        assertEquals(TodaySummaryWidgetState.NoMedications, state)
    }

    @Test
    fun `todaySummaryState - dia sin doses pero medicamento activo manana usa fallback futuro`() {
        // SPECIFIC_DAYS solo martes (today=lunes) → no hay doses hoy, hay futura mañana
        val med = MedicationEntity(
            id = 1L,
            name = "TestMed",
            dosage = 1.0,
            unit = "tableta",
            scheduleType = MedicationScheduleType.SPECIFIC_DAYS,
            times = listOf(LocalTime.of(9, 0)),
            startDate = LocalDate.of(2026, 1, 1),
            specificDays = listOf(DayOfWeek.TUESDAY)
        )
        val state = manager(medications = listOf(med), logs = emptyList())
            .todaySummaryState() as TodaySummaryWidgetState.Summary

        assertEquals(0, state.summary.total)
        assertNotNull(state.highlight)
        assertEquals(NextDoseWidgetItemStatus.UPCOMING_FUTURE, state.highlight!!.status)
        assertEquals(today.plusDays(1), state.highlight!!.scheduledDateTime.toLocalDate())
    }

    @Test
    fun `nextDoseState - intervalo no tiene tomas hoy si startDate es hoy y primera ya paso grace`() {
        // Intervalo cada 4h, startDate hoy 08:00 → 08:00 omitida por grace, próxima 12:00
        val med = MedicationEntity(
            id = 1L,
            name = "Iv",
            dosage = 1.0,
            unit = "tableta",
            scheduleType = MedicationScheduleType.INTERVAL,
            times = listOf(LocalTime.of(8, 0)),
            intervalHours = 4,
            startDate = today
        )
        val state = manager(medications = listOf(med), logs = emptyList()).nextDoseState()
        assertTrue(state is NextDoseWidgetState.Dose)
        val item = (state as NextDoseWidgetState.Dose).item
        // 12:00 debería ser PENDING (es futura)
        assertEquals(NextDoseWidgetItemStatus.PENDING, item.status)
        assertEquals(LocalTime.of(12, 0), item.scheduledDateTime.toLocalTime())
    }

    private fun manager(
        medications: List<MedicationEntity>,
        logs: List<MedicationLogEntity>
    ): MedicationWidgetDataManager = MedicationWidgetDataManager(
        medications = medications,
        logs = logs,
        now = now,
        formatTime = { it.toLocalTime().toString() },
        formatDosage = { dosage, unit -> "$dosage $unit" }
    )

    private fun dailyMed(
        id: Long,
        times: List<LocalTime>,
        startDate: LocalDate = LocalDate.of(2026, 1, 1),
        endDate: LocalDate? = null
    ) = MedicationEntity(
        id = id,
        name = "Med$id",
        dosage = 1.0,
        unit = "tableta",
        scheduleType = MedicationScheduleType.DAILY,
        times = times,
        startDate = startDate,
        endDate = endDate
    )
}
