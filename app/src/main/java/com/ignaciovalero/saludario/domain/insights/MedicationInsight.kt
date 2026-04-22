package com.ignaciovalero.saludario.domain.insights

import java.time.LocalTime

data class MedicationInsight(
    val medicationId: Long,
    val medicationName: String,
    val type: InsightType,
    val severity: InsightSeverity,
    val message: String,
    val suggestion: String?,
    val details: InsightDetails? = null
)

fun MedicationInsight.dismissalKey(): String =
    "${medicationId}_${type.name}_${message.hashCode()}"

enum class InsightType {
    FREQUENTLY_MISSED,
    FREQUENT_DELAYS,
    GOOD_ADHERENCE,
    LOW_STOCK
}

enum class InsightSeverity {
    CRITICAL,
    WARNING,
    INFO,
    SUCCESS
}

sealed class InsightDetails {
    data class MissedInfo(
        val missedCount: Int,
        val totalCount: Int,
        val missedPercentage: Int
    ) : InsightDetails()

    data class DelayInfo(
        val averageDelayMinutes: Long,
        val worstTime: LocalTime,
        val suggestedTime: LocalTime?
    ) : InsightDetails()

    data class AdherenceInfo(
        val takenCount: Int,
        val totalCount: Int,
        val adherencePercentage: Int
    ) : InsightDetails()

    data class LowStockInfo(
        val remainingUnits: Double,
        val totalUnits: Double,
        val thresholdUnits: Double,
        val remainingPercentage: Int?,
        val estimatedDaysRemaining: Int?
    ) : InsightDetails()
}
