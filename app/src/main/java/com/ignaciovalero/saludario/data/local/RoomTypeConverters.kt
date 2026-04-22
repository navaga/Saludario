package com.ignaciovalero.saludario.data.local

import androidx.room.TypeConverter
import com.ignaciovalero.saludario.data.local.entity.HealthRecordType
import com.ignaciovalero.saludario.data.local.entity.MedicationScheduleType
import com.ignaciovalero.saludario.data.local.entity.MedicationStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class RoomTypeConverters {

    @TypeConverter
    fun fromHealthRecordType(value: HealthRecordType): String = value.name

    @TypeConverter
    fun toHealthRecordType(value: String): HealthRecordType = HealthRecordType.valueOf(value)

    @TypeConverter
    fun fromMedicationScheduleType(value: MedicationScheduleType): String = value.name

    @TypeConverter
    fun toMedicationScheduleType(value: String): MedicationScheduleType =
        MedicationScheduleType.valueOf(value)

    @TypeConverter
    fun fromMedicationStatus(value: MedicationStatus): String = value.name

    @TypeConverter
    fun toMedicationStatus(value: String): MedicationStatus =
        MedicationStatus.valueOf(value)

    @TypeConverter
    fun fromLocalDate(value: LocalDate): String = value.toString()

    @TypeConverter
    fun toLocalDate(value: String): LocalDate = LocalDate.parse(value)

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime): String = value.toString()

    @TypeConverter
    fun toLocalDateTime(value: String): LocalDateTime = LocalDateTime.parse(value)

    @TypeConverter
    fun fromNullableLocalDateTime(value: LocalDateTime?): String? = value?.toString()

    @TypeConverter
    fun toNullableLocalDateTime(value: String?): LocalDateTime? = value?.let(LocalDateTime::parse)

    @TypeConverter
    fun fromNullableLocalDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toNullableLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun fromLocalTimeList(values: List<LocalTime>): String =
        values.joinToString(separator = ",") { it.toString() }

    @TypeConverter
    fun toLocalTimeList(value: String): List<LocalTime> =
        if (value.isBlank()) {
            emptyList()
        } else {
            value.split(",").map(LocalTime::parse)
        }

    @TypeConverter
    fun fromDayOfWeekList(days: List<DayOfWeek>?): String? =
        days?.joinToString(",") { it.name }

    @TypeConverter
    fun toDayOfWeekList(value: String?): List<DayOfWeek>? =
        value?.takeIf { it.isNotBlank() }?.split(",")?.map { DayOfWeek.valueOf(it) }
}