package com.ignaciovalero.saludario.domain.insights

import android.content.Context
import com.ignaciovalero.saludario.R
import com.ignaciovalero.saludario.data.local.entity.MedicationEntity
import com.ignaciovalero.saludario.data.local.entity.MedicationLogEntity
import com.ignaciovalero.saludario.data.local.entity.MedicationStatus
import java.time.Duration
import java.time.LocalTime
import kotlin.math.roundToInt

class MedicationInsightsAnalyzer(private val context: Context) {

    fun analyze(
        medications: List<MedicationEntity>,
        logs: List<MedicationLogEntity>
    ): List<MedicationInsight> {
        val logsByMedication = logs
            .filter { it.status != MedicationStatus.PENDING }
            .groupBy { it.medicationId }

        val rawInsights = medications.flatMap { medication ->
            val medLogs = logsByMedication[medication.id].orEmpty()

            buildList {
                analyzeLowStock(medication, medLogs)?.let(::add)

                if (medLogs.size >= MIN_LOGS_FOR_ANALYSIS) {
                    analyzeMissedDoses(medication, medLogs)?.let(::add)
                    analyzeDelays(medication, medLogs)?.let(::add)
                    analyzeGoodAdherence(medication, medLogs)?.let(::add)
                }
            }
        }

        return deduplicateInsights(rawInsights)
    }

    private fun deduplicateInsights(insights: List<MedicationInsight>): List<MedicationInsight> {
        return insights
            .groupBy { insight ->
                if (insight.type == InsightType.LOW_STOCK) {
                    "stock_${insight.medicationId}"
                } else {
                    "${insight.medicationId}_${insight.type}_${insight.message}"
                }
            }
            .map { (_, groupedInsights) ->
                groupedInsights.maxWithOrNull(
                    compareBy<MedicationInsight> { severityRank(it.severity) }
                        .thenBy { it.message.length }
                )!!
            }
            .distinctBy { "${it.medicationId}_${it.type}_${it.message}" }
            .sortedWith(
                compareByDescending<MedicationInsight> { severityRank(it.severity) }
                    .thenBy { it.medicationName.lowercase() }
            )
    }

    private fun severityRank(severity: InsightSeverity): Int = when (severity) {
        InsightSeverity.CRITICAL -> 4
        InsightSeverity.WARNING -> 3
        InsightSeverity.INFO -> 2
        InsightSeverity.SUCCESS -> 1
    }

    private fun analyzeLowStock(
        medication: MedicationEntity,
        logs: List<MedicationLogEntity>
    ): MedicationInsight? {
        val hasQuantityTracking = medication.stockTotal > 0.0 ||
            medication.stockRemaining > 0.0 ||
            medication.lowStockThreshold > 0.0
        if (!hasQuantityTracking) return null

        val remainingPercentage = if (medication.stockTotal > 0.0) {
            ((medication.stockRemaining / medication.stockTotal) * 100).roundToInt().coerceAtLeast(0)
        } else {
            null
        }

        val isCriticalStock = medication.stockRemaining <= CRITICAL_LOW_STOCK_UNITS
        val isLowStock = medication.stockRemaining <= medication.lowStockThreshold ||
            (remainingPercentage != null && remainingPercentage <= LOW_STOCK_THRESHOLD_PERCENT)

        val estimatedDaysRemaining = estimateDaysRemaining(medication, logs)

        val severity = when {
            isCriticalStock -> InsightSeverity.CRITICAL
            isLowStock -> InsightSeverity.WARNING
            else -> null
        } ?: return null

        val messageRes = when (severity) {
            InsightSeverity.CRITICAL -> R.string.insight_low_stock_critical_message
            InsightSeverity.WARNING -> R.string.insight_low_stock_warning_message
            else -> return null
        }

        return MedicationInsight(
            medicationId = medication.id,
            medicationName = medication.name,
            type = InsightType.LOW_STOCK,
            severity = severity,
            message = context.getString(messageRes, medication.name),
            suggestion = context.getString(R.string.insight_low_stock_suggestion, medication.name),
            details = InsightDetails.LowStockInfo(
                remainingUnits = medication.stockRemaining,
                totalUnits = medication.stockTotal,
                thresholdUnits = medication.lowStockThreshold,
                remainingPercentage = remainingPercentage,
                estimatedDaysRemaining = estimatedDaysRemaining
            )
        )
    }

    private fun analyzeMissedDoses(
        medication: MedicationEntity,
        logs: List<MedicationLogEntity>
    ): MedicationInsight? {
        val total = logs.size
        val missed = logs.count { it.status == MedicationStatus.MISSED }
        val skipped = logs.count { it.status == MedicationStatus.SKIPPED }
        val notTaken = missed + skipped
        val percentage = (notTaken * 100) / total

        if (percentage < MISSED_THRESHOLD_PERCENT) return null

        val missedByHour = logs
            .filter { it.status == MedicationStatus.MISSED || it.status == MedicationStatus.SKIPPED }
            .groupBy { it.scheduledTime.toLocalTime().hour }

        val worstHour = missedByHour.maxByOrNull { it.value.size }?.key

        val suggestion = if (worstHour != null) {
            context.getString(R.string.insight_missed_with_hour, formatHour(worstHour))
        } else {
            context.getString(R.string.insight_missed_generic)
        }

        return MedicationInsight(
            medicationId = medication.id,
            medicationName = medication.name,
            type = InsightType.FREQUENTLY_MISSED,
            severity = InsightSeverity.WARNING,
            message = context.getString(R.string.insight_missed_message, medication.name, percentage, notTaken, total),
            suggestion = suggestion,
            details = InsightDetails.MissedInfo(
                missedCount = notTaken,
                totalCount = total,
                missedPercentage = percentage
            )
        )
    }

    private fun analyzeDelays(
        medication: MedicationEntity,
        logs: List<MedicationLogEntity>
    ): MedicationInsight? {
        val takenLogs = logs.filter {
            it.status == MedicationStatus.TAKEN && it.takenTime != null
        }
        if (takenLogs.size < MIN_LOGS_FOR_ANALYSIS) return null

        val delays = takenLogs.map { log ->
            Duration.between(log.scheduledTime, log.takenTime).toMinutes()
        }.filter { it > 0 }

        if (delays.isEmpty()) return null

        val avgDelay = delays.sum() / delays.size
        if (avgDelay < DELAY_THRESHOLD_MINUTES) return null

        val delayByScheduledHour = takenLogs
            .filter { Duration.between(it.scheduledTime, it.takenTime).toMinutes() > DELAY_THRESHOLD_MINUTES }
            .groupBy { it.scheduledTime.toLocalTime().hour }

        val worstHour = delayByScheduledHour.maxByOrNull { it.value.size }?.key
            ?: return null

        val actualTimes = takenLogs
            .filter { it.scheduledTime.toLocalTime().hour == worstHour }
            .mapNotNull { it.takenTime?.toLocalTime() }

        val suggestedTime = if (actualTimes.isNotEmpty()) {
            averageTime(actualTimes)
        } else null

        val suggestion = if (suggestedTime != null) {
            context.getString(R.string.insight_delay_with_suggestion, medication.name, suggestedTime.toString(), formatHour(worstHour))
        } else {
            context.getString(R.string.insight_delay_generic, medication.name)
        }

        return MedicationInsight(
            medicationId = medication.id,
            medicationName = medication.name,
            type = InsightType.FREQUENT_DELAYS,
            severity = InsightSeverity.INFO,
            message = context.getString(R.string.insight_delay_message, medication.name, avgDelay),
            suggestion = suggestion,
            details = InsightDetails.DelayInfo(
                averageDelayMinutes = avgDelay,
                worstTime = LocalTime.of(worstHour, 0),
                suggestedTime = suggestedTime
            )
        )
    }

    private fun analyzeGoodAdherence(
        medication: MedicationEntity,
        logs: List<MedicationLogEntity>
    ): MedicationInsight? {
        val total = logs.size
        val taken = logs.count { it.status == MedicationStatus.TAKEN }
        val percentage = (taken * 100) / total

        if (percentage < GOOD_ADHERENCE_PERCENT) return null

        return MedicationInsight(
            medicationId = medication.id,
            medicationName = medication.name,
            type = InsightType.GOOD_ADHERENCE,
            severity = InsightSeverity.SUCCESS,
            message = context.getString(R.string.insight_adherence_message, medication.name, percentage),
            suggestion = null,
            details = InsightDetails.AdherenceInfo(
                takenCount = taken,
                totalCount = total,
                adherencePercentage = percentage
            )
        )
    }

    private fun averageTime(times: List<LocalTime>): LocalTime {
        val totalMinutes = times.sumOf { it.hour * 60L + it.minute }
        val avgMinutes = totalMinutes / times.size
        return LocalTime.of(
            (avgMinutes / 60).toInt().coerceIn(0, 23),
            (avgMinutes % 60).toInt()
        )
    }

    private fun formatHour(hour: Int): String =
        LocalTime.of(hour, 0).toString()

    private fun estimateDaysRemaining(
        medication: MedicationEntity,
        logs: List<MedicationLogEntity>
    ): Int? {
        val takenLogs = logs.filter { it.status == MedicationStatus.TAKEN }
        if (takenLogs.isEmpty() || medication.dosage <= 0.0 || medication.stockRemaining <= 0.0) return null

        val firstDate = takenLogs.minOfOrNull { it.scheduledTime.toLocalDate() } ?: return null
        val lastDate = takenLogs.maxOfOrNull { it.scheduledTime.toLocalDate() } ?: return null
        val observedDays = java.time.temporal.ChronoUnit.DAYS.between(firstDate, lastDate).toInt() + 1
        if (observedDays <= 0) return null

        val totalConsumedUnits = takenLogs.size * medication.dosage
        val averageDailyConsumption = totalConsumedUnits / observedDays
        if (averageDailyConsumption <= 0.0) return null

        return kotlin.math.ceil(medication.stockRemaining / averageDailyConsumption).toInt()
    }

    private fun formatQuantity(value: Double): String {
        return if (value % 1.0 == 0.0) value.toInt().toString() else value.toString().trimEnd('0').trimEnd('.')
    }

    companion object {
        const val MIN_LOGS_FOR_ANALYSIS = 3
        const val MISSED_THRESHOLD_PERCENT = 25
        const val DELAY_THRESHOLD_MINUTES = 15L
        const val GOOD_ADHERENCE_PERCENT = 90
        const val CRITICAL_LOW_STOCK_UNITS = 2.0
        const val LOW_STOCK_THRESHOLD_PERCENT = 15
    }
}
