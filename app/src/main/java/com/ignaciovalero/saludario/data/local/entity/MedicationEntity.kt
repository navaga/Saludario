package com.ignaciovalero.saludario.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "dosage")
    val dosage: Double,
    @ColumnInfo(name = "unit")
    val unit: String,
    @ColumnInfo(name = "schedule_type")
    val scheduleType: MedicationScheduleType,
    @ColumnInfo(name = "times")
    val times: List<LocalTime>,
    @ColumnInfo(name = "start_date")
    val startDate: LocalDate,
    @ColumnInfo(name = "end_date")
    val endDate: LocalDate? = null,
    @ColumnInfo(name = "specific_days")
    val specificDays: List<DayOfWeek>? = null,
    @ColumnInfo(name = "interval_hours")
    val intervalHours: Int? = null,
    @ColumnInfo(name = "stock_total")
    val stockTotal: Double = 0.0,
    @ColumnInfo(name = "stock_remaining")
    val stockRemaining: Double = 0.0,
    @ColumnInfo(name = "low_stock_threshold")
    val lowStockThreshold: Double = 0.0
)

enum class MedicationScheduleType {
    DAILY,
    SPECIFIC_DAYS,
    INTERVAL
}