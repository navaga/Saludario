package com.ignaciovalero.saludario.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "medication_logs",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medication_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["medication_id"])]
)
data class MedicationLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "medication_id")
    val medicationId: Long,
    @ColumnInfo(name = "scheduled_time")
    val scheduledTime: LocalDateTime,
    @ColumnInfo(name = "taken_time")
    val takenTime: LocalDateTime? = null,
    @ColumnInfo(name = "status")
    val status: MedicationStatus,
    @ColumnInfo(name = "postponed_until")
    val postponedUntil: LocalDateTime? = null
)

enum class MedicationStatus {
    PENDING,
    TAKEN,
    SKIPPED,
    MISSED,
    POSTPONED
}