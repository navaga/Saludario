package com.ignaciovalero.saludario.domain.insights

import android.content.Context
import com.ignaciovalero.saludario.R
import com.ignaciovalero.saludario.data.local.entity.MedicationEntity
import com.ignaciovalero.saludario.data.local.entity.MedicationLogEntity
import com.ignaciovalero.saludario.data.local.entity.MedicationScheduleType
import com.ignaciovalero.saludario.data.local.entity.MedicationStatus
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class MedicationInsightsAnalyzerTest {

    private lateinit var context: Context
    private lateinit var analyzer: MedicationInsightsAnalyzer

    private val baseMedication = MedicationEntity(
        id = 1L,
        name = "Paracetamol",
        dosage = 1.0,
        unit = "tableta",
        scheduleType = MedicationScheduleType.DAILY,
        times = listOf(LocalTime.of(8, 0)),
        startDate = LocalDate.of(2026, 1, 1)
    )

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        every { context.getString(any()) } returns "stub"
        every { context.getString(any(), *anyVararg()) } returns "stub_formatted"
        analyzer = MedicationInsightsAnalyzer(context)
    }

    // ────────── analyze() general ──────────

    @Test
    fun `analyze returns empty when no medications`() {
        val result = analyzer.analyze(emptyList(), emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `analyze returns empty when no logs`() {
        val result = analyzer.analyze(listOf(baseMedication), emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `analyze skips PENDING logs`() {
        val logs = (1..5).map { i ->
            MedicationLogEntity(
                id = i.toLong(),
                medicationId = 1L,
                scheduledTime = LocalDateTime.of(2026, 4, i, 8, 0),
                status = MedicationStatus.PENDING
            )
        }
        val result = analyzer.analyze(listOf(baseMedication), logs)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `analyze skips medication with fewer than MIN_LOGS_FOR_ANALYSIS logs`() {
        val logs = listOf(
            makeLog(1L, 1, MedicationStatus.TAKEN),
            makeLog(1L, 2, MedicationStatus.MISSED)
        )
        val result = analyzer.analyze(listOf(baseMedication), logs)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `analyze ignores logs for unknown medication ids`() {
        val logs = (1..5).map { i ->
            makeLog(medicationId = 999L, day = i, status = MedicationStatus.MISSED)
        }
        val result = analyzer.analyze(listOf(baseMedication), logs)
        assertTrue(result.isEmpty())
    }

    // ────────── analyzeMissedDoses ──────────

    @Test
    fun `missed doses detected when percentage at threshold`() {
        // 1 missed / 4 total = 25% exactly at threshold
        val logs = listOf(
            makeLog(1L, 1, MedicationStatus.MISSED),
            makeLog(1L, 2, MedicationStatus.TAKEN),
            makeLog(1L, 3, MedicationStatus.TAKEN),
            makeLog(1L, 4, MedicationStatus.TAKEN)
        )
        val result = analyzer.analyze(listOf(baseMedication), logs)
        val missed = result.find { it.type == InsightType.FREQUENTLY_MISSED }
        assertNotNull(missed)
        val details = missed!!.details as InsightDetails.MissedInfo
        assertEquals(1, details.missedCount)
        assertEquals(4, details.totalCount)
        assertEquals(25, details.missedPercentage)
    }

    @Test
    fun `missed doses not detected below threshold`() {
        // 1 missed / 5 total = 20% below 25%
        val logs = listOf(
            makeLog(1L, 1, MedicationStatus.MISSED),
            makeLog(1L, 2, MedicationStatus.TAKEN),
            makeLog(1L, 3, MedicationStatus.TAKEN),
            makeLog(1L, 4, MedicationStatus.TAKEN),
            makeLog(1L, 5, MedicationStatus.TAKEN)
        )
        val result = analyzer.analyze(listOf(baseMedication), logs)
        val missed = result.find { it.type == InsightType.FREQUENTLY_MISSED }
        assertNull(missed)
    }

    @Test
    fun `skipped doses count as missed`() {
        // 2 skipped / 4 total = 50%
        val logs = listOf(
            makeLog(1L, 1, MedicationStatus.SKIPPED),
            makeLog(1L, 2, MedicationStatus.SKIPPED),
            makeLog(1L, 3, MedicationStatus.TAKEN),
            makeLog(1L, 4, MedicationStatus.TAKEN)
        )
        val result = analyzer.analyze(listOf(baseMedication), logs)
        val missed = result.find { it.type == InsightType.FREQUENTLY_MISSED }
        assertNotNull(missed)
        val details = missed!!.details as InsightDetails.MissedInfo
        assertEquals(2, details.missedCount)
        assertEquals(50, details.missedPercentage)
    }

    @Test
    fun `all doses missed gives 100 percent`() {
        val logs = (1..4).map { makeLog(1L, it, MedicationStatus.MISSED) }
        val result = analyzer.analyze(listOf(baseMedication), logs)
        val details = result.first { it.type == InsightType.FREQUENTLY_MISSED }
            .details as InsightDetails.MissedInfo
        assertEquals(100, details.missedPercentage)
        assertEquals(4, details.missedCount)
    }

    @Test
    fun `missed doses suggestion includes worst hour`() {
        // All missed at 8:00 — triggers hour-specific suggestion
        val logs = (1..4).map { makeLog(1L, it, MedicationStatus.MISSED, hour = 8) }
        every { context.getString(eq(R.string.insight_missed_with_hour), any()) } returns "hour_suggestion"

        val result = analyzer.analyze(listOf(baseMedication), logs)
        val missed = result.find { it.type == InsightType.FREQUENTLY_MISSED }
        assertNotNull(missed)
    }

    // ────────── analyzeDelays ──────────

    @Test
    fun `delays detected when average above threshold`() {
        // 3 taken logs, each 30 min late
        val logs = (1..3).map { day ->
            MedicationLogEntity(
                id = day.toLong(),
                medicationId = 1L,
                scheduledTime = LocalDateTime.of(2026, 4, day, 8, 0),
                takenTime = LocalDateTime.of(2026, 4, day, 8, 30),
                status = MedicationStatus.TAKEN
            )
        }
        val result = analyzer.analyze(listOf(baseMedication), logs)
        val delay = result.find { it.type == InsightType.FREQUENT_DELAYS }
        assertNotNull(delay)
        val details = delay!!.details as InsightDetails.DelayInfo
        assertEquals(30L, details.averageDelayMinutes)
        assertEquals(LocalTime.of(8, 0), details.worstTime)
    }

    @Test
    fun `delays not detected when average below threshold`() {
        // 3 taken logs, each 10 min late (below 15 min threshold)
        val logs = (1..3).map { day ->
            MedicationLogEntity(
                id = day.toLong(),
                medicationId = 1L,
                scheduledTime = LocalDateTime.of(2026, 4, day, 8, 0),
                takenTime = LocalDateTime.of(2026, 4, day, 8, 10),
                status = MedicationStatus.TAKEN
            )
        }
        val result = analyzer.analyze(listOf(baseMedication), logs)
        val delay = result.find { it.type == InsightType.FREQUENT_DELAYS }
        assertNull(delay)
    }

    @Test
    fun `delays ignore on-time doses for average`() {
        val logs = listOf(
            // On-time (0 delay — filtered out since delay <= 0)
            MedicationLogEntity(1L, 1L, LocalDateTime.of(2026, 4, 1, 8, 0), LocalDateTime.of(2026, 4, 1, 8, 0), MedicationStatus.TAKEN),
            // 20 min late
            MedicationLogEntity(2L, 1L, LocalDateTime.of(2026, 4, 2, 8, 0), LocalDateTime.of(2026, 4, 2, 8, 20), MedicationStatus.TAKEN),
            // 30 min late
            MedicationLogEntity(3L, 1L, LocalDateTime.of(2026, 4, 3, 8, 0), LocalDateTime.of(2026, 4, 3, 8, 30), MedicationStatus.TAKEN)
        )
        val result = analyzer.analyze(listOf(baseMedication), logs)
        val delay = result.find { it.type == InsightType.FREQUENT_DELAYS }
        assertNotNull(delay)
        // avg of 20 and 30 = 25 (only positive delays)
        assertEquals(25L, (delay!!.details as InsightDetails.DelayInfo).averageDelayMinutes)
    }

    @Test
    fun `delays not reported when all taken early or on time`() {
        val logs = (1..3).map { day ->
            MedicationLogEntity(
                id = day.toLong(),
                medicationId = 1L,
                scheduledTime = LocalDateTime.of(2026, 4, day, 8, 0),
                takenTime = LocalDateTime.of(2026, 4, day, 7, 55),
                status = MedicationStatus.TAKEN
            )
        }
        val result = analyzer.analyze(listOf(baseMedication), logs)
        val delay = result.find { it.type == InsightType.FREQUENT_DELAYS }
        assertNull(delay)
    }

    @Test
    fun `delays need minimum logs for analysis`() {
        // Only 2 TAKEN logs (below MIN=3), even with big delays
        val logs = listOf(
            MedicationLogEntity(1L, 1L, LocalDateTime.of(2026, 4, 1, 8, 0), LocalDateTime.of(2026, 4, 1, 9, 0), MedicationStatus.TAKEN),
            MedicationLogEntity(2L, 1L, LocalDateTime.of(2026, 4, 2, 8, 0), LocalDateTime.of(2026, 4, 2, 9, 0), MedicationStatus.TAKEN),
            makeLog(1L, 3, MedicationStatus.MISSED)
        )
        val result = analyzer.analyze(listOf(baseMedication), logs)
        val delay = result.find { it.type == InsightType.FREQUENT_DELAYS }
        assertNull(delay)
    }

    @Test
    fun `delay suggests actual average time`() {
        val logs = listOf(
            MedicationLogEntity(1L, 1L, LocalDateTime.of(2026, 4, 1, 8, 0), LocalDateTime.of(2026, 4, 1, 8, 30), MedicationStatus.TAKEN),
            MedicationLogEntity(2L, 1L, LocalDateTime.of(2026, 4, 2, 8, 0), LocalDateTime.of(2026, 4, 2, 8, 40), MedicationStatus.TAKEN),
            MedicationLogEntity(3L, 1L, LocalDateTime.of(2026, 4, 3, 8, 0), LocalDateTime.of(2026, 4, 3, 8, 50), MedicationStatus.TAKEN)
        )
        val result = analyzer.analyze(listOf(baseMedication), logs)
        val delay = result.find { it.type == InsightType.FREQUENT_DELAYS }
        assertNotNull(delay)
        val details = delay!!.details as InsightDetails.DelayInfo
        // Average of 08:30, 08:40, 08:50 = 08:40
        assertEquals(LocalTime.of(8, 40), details.suggestedTime)
    }

    // ────────── analyzeGoodAdherence ──────────

    @Test
    fun `good adherence detected at threshold`() {
        // 9/10 = 90% at threshold
        val logs = (1..10).map { day ->
            val status = if (day <= 9) MedicationStatus.TAKEN else MedicationStatus.MISSED
            makeLog(1L, day, status)
        }
        val result = analyzer.analyze(listOf(baseMedication), logs)
        val adherence = result.find { it.type == InsightType.GOOD_ADHERENCE }
        assertNotNull(adherence)
        val details = adherence!!.details as InsightDetails.AdherenceInfo
        assertEquals(9, details.takenCount)
        assertEquals(10, details.totalCount)
        assertEquals(90, details.adherencePercentage)
    }

    @Test
    fun `good adherence not detected below threshold`() {
        // 8/10 = 80% below 90%
        val logs = (1..10).map { day ->
            val status = if (day <= 8) MedicationStatus.TAKEN else MedicationStatus.MISSED
            makeLog(1L, day, status)
        }
        val result = analyzer.analyze(listOf(baseMedication), logs)
        val adherence = result.find { it.type == InsightType.GOOD_ADHERENCE }
        assertNull(adherence)
    }

    @Test
    fun `perfect adherence reports 100 percent`() {
        val logs = (1..5).map { makeLog(1L, it, MedicationStatus.TAKEN) }
        val result = analyzer.analyze(listOf(baseMedication), logs)
        val adherence = result.find { it.type == InsightType.GOOD_ADHERENCE }
        assertNotNull(adherence)
        assertEquals(100, (adherence!!.details as InsightDetails.AdherenceInfo).adherencePercentage)
    }

    @Test
    fun `good adherence has null suggestion`() {
        val logs = (1..5).map { makeLog(1L, it, MedicationStatus.TAKEN) }
        val result = analyzer.analyze(listOf(baseMedication), logs)
        val adherence = result.find { it.type == InsightType.GOOD_ADHERENCE }
        assertNull(adherence!!.suggestion)
    }

    // ────────── multiple medications ──────────

    @Test
    fun `analyze returns insights for multiple medications independently`() {
        val med2 = baseMedication.copy(id = 2L, name = "Ibuprofeno")
        val logs = listOf(
            // Med1: all missed
            makeLog(1L, 1, MedicationStatus.MISSED),
            makeLog(1L, 2, MedicationStatus.MISSED),
            makeLog(1L, 3, MedicationStatus.MISSED),
            // Med2: all taken
            makeLog(2L, 1, MedicationStatus.TAKEN),
            makeLog(2L, 2, MedicationStatus.TAKEN),
            makeLog(2L, 3, MedicationStatus.TAKEN)
        )
        val result = analyzer.analyze(listOf(baseMedication, med2), logs)
        val med1Insights = result.filter { it.medicationId == 1L }
        val med2Insights = result.filter { it.medicationId == 2L }

        assertTrue(med1Insights.any { it.type == InsightType.FREQUENTLY_MISSED })
        assertTrue(med2Insights.any { it.type == InsightType.GOOD_ADHERENCE })
    }

    // ────────── edge cases ──────────

    @Test
    fun `mixed statuses in same medication produce multiple insights`() {
        // 6 taken with delay + 4 missed = frequently missed + delays (possibly + no adherence)
        val logs = (1..6).map { day ->
            MedicationLogEntity(
                id = day.toLong(), medicationId = 1L,
                scheduledTime = LocalDateTime.of(2026, 4, day, 8, 0),
                takenTime = LocalDateTime.of(2026, 4, day, 8, 30),
                status = MedicationStatus.TAKEN
            )
        } + (7..10).map { day ->
            makeLog(1L, day, MedicationStatus.MISSED)
        }
        val result = analyzer.analyze(listOf(baseMedication), logs)
        assertTrue(result.any { it.type == InsightType.FREQUENTLY_MISSED })
        assertTrue(result.any { it.type == InsightType.FREQUENT_DELAYS })
    }

    // ────────── helpers ──────────

    private fun makeLog(
        medicationId: Long,
        day: Int,
        status: MedicationStatus,
        hour: Int = 8
    ): MedicationLogEntity {
        val scheduled = LocalDateTime.of(2026, 4, day, hour, 0)
        val taken = if (status == MedicationStatus.TAKEN) scheduled else null
        return MedicationLogEntity(
            id = (medicationId * 100 + day),
            medicationId = medicationId,
            scheduledTime = scheduled,
            takenTime = taken,
            status = status
        )
    }
}
