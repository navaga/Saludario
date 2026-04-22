package com.ignaciovalero.saludario.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ignaciovalero.saludario.data.local.entity.MedicationLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationLogDao {

    @Query("SELECT * FROM medication_logs ORDER BY scheduled_time DESC")
    fun observeAll(): Flow<List<MedicationLogEntity>>

    @Query("SELECT * FROM medication_logs WHERE id = :id")
    suspend fun getById(id: Long): MedicationLogEntity?

    @Query("SELECT * FROM medication_logs WHERE medication_id = :medicationId ORDER BY scheduled_time DESC")
    fun observeByMedicationId(medicationId: Long): Flow<List<MedicationLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: MedicationLogEntity): Long

    @Update
    suspend fun update(log: MedicationLogEntity)

    @Delete
    suspend fun delete(log: MedicationLogEntity)

    @Query("SELECT * FROM medication_logs WHERE medication_id = :medicationId AND scheduled_time = :scheduledTime LIMIT 1")
    suspend fun getByMedicationAndScheduledTime(medicationId: Long, scheduledTime: String): MedicationLogEntity?

    @Query("SELECT * FROM medication_logs WHERE status = 'PENDING' AND scheduled_time < :beforeTime")
    suspend fun getPendingBefore(beforeTime: String): List<MedicationLogEntity>

    @Query("DELETE FROM medication_logs")
    suspend fun deleteAll()
}