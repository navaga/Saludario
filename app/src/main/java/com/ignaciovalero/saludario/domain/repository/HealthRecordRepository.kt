package com.ignaciovalero.saludario.domain.repository

import com.ignaciovalero.saludario.data.local.entity.HealthRecord
import com.ignaciovalero.saludario.data.local.entity.HealthRecordType
import kotlinx.coroutines.flow.Flow

interface HealthRecordRepository {
    fun observeAll(): Flow<List<HealthRecord>>
    fun observeByType(type: HealthRecordType): Flow<List<HealthRecord>>
    suspend fun insert(record: HealthRecord): Long
    suspend fun delete(record: HealthRecord)
}
