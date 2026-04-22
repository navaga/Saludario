package com.ignaciovalero.saludario.domain.scheduling

import java.time.LocalDateTime

enum class ScheduledDoseStatus {
    PENDING,
    TAKEN,
    MISSED
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
    val logId: Long? = null
) {
    val medicationId: Long get() = medication.id
    val medicationName: String get() = medication.name
    val dosage: String get() = "${medication.dosage} ${medication.unit}"
    val time: String get() = scheduledAt.toLocalTime().toString()

    val isTaken: Boolean get() = status == ScheduledDoseStatus.TAKEN
    val isMissed: Boolean get() = status == ScheduledDoseStatus.MISSED
    val isPending: Boolean get() = status == ScheduledDoseStatus.PENDING
}
