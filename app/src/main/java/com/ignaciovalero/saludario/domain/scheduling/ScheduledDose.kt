package com.ignaciovalero.saludario.domain.scheduling

import java.time.LocalDateTime

enum class ScheduledDoseStatus {
    PENDING,
    TAKEN,
    MISSED,
    POSTPONED
}

data class ScheduledDoseMedication(
    val id: Long,
    val name: String,
    val dosage: Double,
    val unit: String
)

data class ScheduledDose(
    val medication: ScheduledDoseMedication,
    val scheduledAt: LocalDateTime,
    val status: ScheduledDoseStatus,
    val logId: Long? = null,
    val takenAt: LocalDateTime? = null,
    val postponedUntil: LocalDateTime? = null
) {
    val medicationId: Long get() = medication.id
    val medicationName: String get() = medication.name
    val dosage: String get() = "${medication.dosage} ${medication.unit}"

    /**
     * Hora efectiva mostrada en la UI: si la dosis ha sido pospuesta usa
     * `postponedUntil`, en caso contrario la hora original programada.
     */
    val effectiveTime: LocalDateTime get() = postponedUntil ?: scheduledAt
    val time: String get() = effectiveTime.toLocalTime().toString()

    val isTaken: Boolean get() = status == ScheduledDoseStatus.TAKEN
    val isMissed: Boolean get() = status == ScheduledDoseStatus.MISSED
    val isPending: Boolean get() = status == ScheduledDoseStatus.PENDING
    val isPostponed: Boolean get() = status == ScheduledDoseStatus.POSTPONED
}
