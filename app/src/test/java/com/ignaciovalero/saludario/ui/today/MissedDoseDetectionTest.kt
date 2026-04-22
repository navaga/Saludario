package com.ignaciovalero.saludario.ui.today

import com.ignaciovalero.saludario.data.local.entity.MedicationEntity
import com.ignaciovalero.saludario.data.local.entity.MedicationLogEntity
import com.ignaciovalero.saludario.data.local.entity.MedicationScheduleType
import com.ignaciovalero.saludario.data.local.entity.MedicationStatus
import com.ignaciovalero.saludario.domain.scheduling.ScheduledDose
import com.ignaciovalero.saludario.domain.scheduling.ScheduledDoseGenerator
import com.ignaciovalero.saludario.domain.scheduling.ScheduledDoseMedication
import com.ignaciovalero.saludario.domain.scheduling.ScheduledDoseStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class MissedDoseDetectionTest {

    private val today = LocalDate.of(2026, 4, 20)

    private val baseMedication = MedicationEntity(
        id = 1L,
        name = "Paracetamol",
        dosage = 1.0,
        unit = "tableta",
        scheduleType = MedicationScheduleType.DAILY,
        times = listOf(LocalTime.of(8, 0), LocalTime.of(14, 0), LocalTime.of(20, 0)),
        startDate = LocalDate.of(2026, 1, 1)
    )

    @Test
    fun `pending status when scheduled time is in the future and no log`() {
        val now = LocalDateTime.of(today, LocalTime.of(7, 0))
        val items = computeItems(listOf(baseMedication), emptyList(), now)

        assertEquals(ScheduledDoseStatus.PENDING, items.find { it.time == "08:00" }!!.status)
    }

    @Test
    fun `missed status when scheduled time has passed and no log`() {
        val now = LocalDateTime.of(today, LocalTime.of(9, 0))
        val items = computeItems(listOf(baseMedication), emptyList(), now)

        assertEquals(ScheduledDoseStatus.MISSED, items.find { it.time == "08:00" }!!.status)
    }

    @Test
    fun `taken status when log exists with TAKEN status`() {
        val log = MedicationLogEntity(
            id = 1L,
            medicationId = 1L,
            scheduledTime = LocalDateTime.of(today, LocalTime.of(8, 0)),
            takenTime = LocalDateTime.of(today, LocalTime.of(8, 5)),
            status = MedicationStatus.TAKEN
        )
        val now = LocalDateTime.of(today, LocalTime.of(9, 0))
        val items = computeItems(listOf(baseMedication), listOf(log), now)

        assertEquals(ScheduledDoseStatus.TAKEN, items.find { it.time == "08:00" }!!.status)
    }

    @Test
    fun `logs are matched to correct medication`() {
        val med2 = baseMedication.copy(id = 2L, name = "Ibuprofeno", times = listOf(LocalTime.of(8, 0)))
        val log = MedicationLogEntity(
            id = 1L,
            medicationId = 2L,
            scheduledTime = LocalDateTime.of(today, LocalTime.of(8, 0)),
            takenTime = LocalDateTime.of(today, LocalTime.of(8, 5)),
            status = MedicationStatus.TAKEN
        )
        val now = LocalDateTime.of(today, LocalTime.of(9, 0))

        val items = computeItems(listOf(baseMedication, med2), listOf(log), now)
        val med1Item = items.find { it.medicationId == 1L && it.time == "08:00" }!!
        val med2Item = items.find { it.medicationId == 2L && it.time == "08:00" }!!
        assertEquals(ScheduledDoseStatus.MISSED, med1Item.status)
        assertEquals(ScheduledDoseStatus.TAKEN, med2Item.status)
    }

    @Test
    fun `items are sorted by time`() {
        val now = LocalDateTime.of(today, LocalTime.of(7, 0))
        val items = computeItems(listOf(baseMedication), emptyList(), now)

        assertEquals("08:00", items[0].time)
        assertEquals("14:00", items[1].time)
        assertEquals("20:00", items[2].time)
    }

    @Test
    fun `logs from yesterday are ignored`() {
        val yesterday = today.minusDays(1)
        val log = MedicationLogEntity(
            id = 1L,
            medicationId = 1L,
            scheduledTime = LocalDateTime.of(yesterday, LocalTime.of(8, 0)),
            takenTime = LocalDateTime.of(yesterday, LocalTime.of(8, 5)),
            status = MedicationStatus.TAKEN
        )
        val now = LocalDateTime.of(today, LocalTime.of(9, 0))
        val items = computeItems(listOf(baseMedication), listOf(log), now)

        assertEquals(ScheduledDoseStatus.MISSED, items.find { it.time == "08:00" }!!.status)
    }

    @Test
    fun `medication with startDate after selected day is not shown`() {
        val med = baseMedication.copy(startDate = today.plusDays(1))
        val now = LocalDateTime.of(today, LocalTime.of(9, 0))

        val items = computeItems(listOf(med), emptyList(), now)
        assertTrue(items.isEmpty())
    }

    @Test
    fun `specific days only generates doses on selected weekday`() {
        val med = baseMedication.copy(
            scheduleType = MedicationScheduleType.SPECIFIC_DAYS,
            specificDays = listOf(DayOfWeek.MONDAY),
            times = listOf(LocalTime.of(10, 0))
        )
        val now = LocalDateTime.of(today, LocalTime.of(9, 0)) // 2026-04-20 is Monday

        val items = computeItems(listOf(med), emptyList(), now)
        assertEquals(1, items.size)
        assertEquals("10:00", items.first().time)
        assertEquals(ScheduledDoseStatus.PENDING, items.first().status)
    }

    @Test
    fun `interval schedule generates expected aligned doses for the day`() {
        val med = baseMedication.copy(
            scheduleType = MedicationScheduleType.INTERVAL,
            times = listOf(LocalTime.of(8, 0)),
            intervalHours = 8
        )
        val now = LocalDateTime.of(today, LocalTime.of(23, 0))

        val items = computeItems(listOf(med), emptyList(), now)
        assertEquals(listOf("00:00", "08:00", "16:00"), items.map { it.time })
        assertEquals(
            listOf(ScheduledDoseStatus.MISSED, ScheduledDoseStatus.MISSED, ScheduledDoseStatus.MISSED),
            items.map { it.status }
        )
    }

    @Test
    fun `scheduled dose computed properties reflect status`() {
        val pending = ScheduledDose(
            medication = ScheduledDoseMedication(1L, "Test", 1.0, "tab"),
            scheduledAt = LocalDateTime.of(today, LocalTime.of(8, 0)),
            status = ScheduledDoseStatus.PENDING
        )

        assertEquals(1L, pending.medicationId)
        assertEquals("Test", pending.medicationName)
        assertEquals("1.0 tab", pending.dosage)
        assertEquals("08:00", pending.time)
        assertTrue(pending.isPending)
        assertTrue(!pending.isTaken)
        assertTrue(!pending.isMissed)
    }

    private fun computeItems(
        medications: List<MedicationEntity>,
        logs: List<MedicationLogEntity>,
        now: LocalDateTime
    ): List<ScheduledDose> {
        return ScheduledDoseGenerator(
            medications = medications,
            logs = logs,
            now = now
        ).generateDosesForDate(today)
    }
}
