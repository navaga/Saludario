package com.ignaciovalero.saludario.data.repository

import com.ignaciovalero.saludario.data.local.dao.HealthRecordDao
import com.ignaciovalero.saludario.data.local.entity.HealthRecord
import com.ignaciovalero.saludario.data.local.entity.HealthRecordType
import com.ignaciovalero.saludario.domain.repository.HealthRecordRepository
import kotlinx.coroutines.flow.Flow

class HealthRecordRepositoryImpl(
    private val healthRecordDao: HealthRecordDao
) : HealthRecordRepository {

    override fun observeByType(type: HealthRecordType): Flow<List<HealthRecord>> =
        healthRecordDao.observeByType(type)

    override suspend fun insert(record: HealthRecord): Long =
        healthRecordDao.insert(record)

    override suspend fun delete(record: HealthRecord) {
        healthRecordDao.delete(record)
    }
}
