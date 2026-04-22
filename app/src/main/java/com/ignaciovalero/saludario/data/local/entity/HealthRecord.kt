package com.ignaciovalero.saludario.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "health_records")
data class HealthRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "type")
    val type: HealthRecordType,
    @ColumnInfo(name = "value")
    val value: Double,
    @ColumnInfo(name = "secondary_value")
    val secondaryValue: Double? = null,
    @ColumnInfo(name = "unit")
    val unit: String,
    @ColumnInfo(name = "recorded_at")
    val recordedAt: LocalDateTime,
    @ColumnInfo(name = "notes")
    val notes: String? = null
)

enum class HealthRecordType {
    BLOOD_PRESSURE,
    GLUCOSE,
    WEIGHT,
    HEART_RATE,
    TEMPERATURE,
    OXYGEN_SATURATION,
    CUSTOM
}
