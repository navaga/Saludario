package com.ignaciovalero.saludario.domain.repository

import com.ignaciovalero.saludario.data.local.entity.MedicationLogEntity
import kotlinx.coroutines.flow.Flow

interface MedicationLogRepository {
    fun observeAll(): Flow<List<MedicationLogEntity>>
    fun observeByMedicationId(medicationId: Long): Flow<List<MedicationLogEntity>>
    suspend fun getById(id: Long): MedicationLogEntity?
    suspend fun getByMedicationAndScheduledTime(medicationId: Long, scheduledTime: String): MedicationLogEntity?
    suspend fun getPendingBefore(beforeTime: String): List<MedicationLogEntity>
    suspend fun insert(log: MedicationLogEntity): Long
    suspend fun update(log: MedicationLogEntity)
    suspend fun delete(log: MedicationLogEntity)
}
