package com.ignaciovalero.saludario.domain.repository

import com.ignaciovalero.saludario.data.local.entity.MedicationEntity
import kotlinx.coroutines.flow.Flow

interface MedicationRepository {
    fun observeAll(): Flow<List<MedicationEntity>>
    suspend fun getById(id: Long): MedicationEntity?
    suspend fun getActiveForDate(date: String): List<MedicationEntity>
    suspend fun insert(medication: MedicationEntity): Long
    suspend fun update(medication: MedicationEntity)
    suspend fun delete(medication: MedicationEntity)
    suspend fun decreaseStockForTakenDose(medicationId: Long)
    suspend fun addStock(medicationId: Long, amount: Double)
}
