package com.ignaciovalero.saludario.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ignaciovalero.saludario.data.local.dao.HealthRecordDao
import com.ignaciovalero.saludario.data.local.dao.MedicationDao
import com.ignaciovalero.saludario.data.local.dao.MedicationLogDao
import com.ignaciovalero.saludario.data.local.entity.HealthRecord
import com.ignaciovalero.saludario.data.local.entity.MedicationEntity
import com.ignaciovalero.saludario.data.local.entity.MedicationLogEntity

@Database(
	entities = [
		MedicationEntity::class,
		MedicationLogEntity::class,
		HealthRecord::class
	],
	version = 5,
	exportSchema = true
)
@TypeConverters(RoomTypeConverters::class)
abstract class SaludarioDatabase : RoomDatabase() {
	abstract fun healthRecordDao(): HealthRecordDao
	abstract fun medicationDao(): MedicationDao
	abstract fun medicationLogDao(): MedicationLogDao
}