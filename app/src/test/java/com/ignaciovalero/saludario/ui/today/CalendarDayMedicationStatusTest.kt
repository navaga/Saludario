package com.ignaciovalero.saludario.ui.today

import com.ignaciovalero.saludario.domain.scheduling.ScheduledDose
import com.ignaciovalero.saludario.domain.scheduling.ScheduledDoseMedication
import com.ignaciovalero.saludario.domain.scheduling.ScheduledDoseStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class CalendarDayMedicationStatusTest {

    private val date = LocalDate.of(2026, 4, 20)

    private fun dose(status: ScheduledDoseStatus, hour: Int = 8, medId: Long = 1L) = ScheduledDose(
        medication = ScheduledDoseMedication(id = medId, name = "M", dosage = 1.0, unit = "u"),
        scheduledAt = LocalDateTime.of(date, java.time.LocalTime.of(hour, 0)),
        status = status
    )

    @Test
    fun `empty doses returns EMPTY status`() {
        val s = CalendarDayMedicationStatus.fromDoses(date, emptyList())
        assertEquals(0, s.totalDoses)
        assertEquals(CalendarDayVisualStatus.EMPTY, s.visualStatus)
    }

    @Test
    fun `all taken returns COMPLETED`() {
        val s = CalendarDayMedicationStatus.fromDoses(
            date,
            listOf(dose(ScheduledDoseStatus.TAKEN, 8), dose(ScheduledDoseStatus.TAKEN, 14))
        )
        assertEquals(2, s.totalDoses)
        assertEquals(2, s.takenDoses)
        assertEquals(CalendarDayVisualStatus.COMPLETED, s.visualStatus)
    }

    @Test
    fun `only pending returns PENDING`() {
        val s = CalendarDayMedicationStatus.fromDoses(
            date,
            listOf(dose(ScheduledDoseStatus.PENDING, 8), dose(ScheduledDoseStatus.PENDING, 20))
        )
        assertEquals(CalendarDayVisualStatus.PENDING, s.visualStatus)
        assertEquals(2, s.pendingDoses)
    }

    @Test
    fun `any missed returns MISSED even if some taken`() {
        val s = CalendarDayMedicationStatus.fromDoses(
            date,
            listOf(
                dose(ScheduledDoseStatus.TAKEN, 8),
                dose(ScheduledDoseStatus.MISSED, 14),
                dose(ScheduledDoseStatus.PENDING, 20)
            )
        )
        assertEquals(CalendarDayVisualStatus.MISSED, s.visualStatus)
        assertEquals(1, s.missedDoses)
        assertEquals(1, s.takenDoses)
        assertEquals(1, s.pendingDoses)
    }

    @Test
    fun `mix of taken and pending without missed returns MIXED`() {
        val s = CalendarDayMedicationStatus.fromDoses(
            date,
            listOf(
                dose(ScheduledDoseStatus.TAKEN, 8),
                dose(ScheduledDoseStatus.PENDING, 14)
            )
        )
        assertEquals(CalendarDayVisualStatus.MIXED, s.visualStatus)
    }

    @Test
    fun `postponed counts independently`() {
        val s = CalendarDayMedicationStatus.fromDoses(
            date,
            listOf(
                dose(ScheduledDoseStatus.POSTPONED, 8),
                dose(ScheduledDoseStatus.TAKEN, 14)
            )
        )
        assertEquals(1, s.postponedDoses)
        assertEquals(1, s.takenDoses)
        // tomadas != total y no hay olvidadas -> MIXED
        assertEquals(CalendarDayVisualStatus.MIXED, s.visualStatus)
    }
}
