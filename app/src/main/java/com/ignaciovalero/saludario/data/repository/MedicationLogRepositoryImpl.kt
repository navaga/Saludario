package com.ignaciovalero.saludario.data.repository

import android.content.Context
import com.ignaciovalero.saludario.data.local.dao.MedicationLogDao
import com.ignaciovalero.saludario.data.local.entity.MedicationLogEntity
import com.ignaciovalero.saludario.domain.repository.MedicationLogRepository
import com.ignaciovalero.saludario.ui.widget.MedicationWidgetUpdater
import kotlinx.coroutines.flow.Flow

class MedicationLogRepositoryImpl(
    private val medicationLogDao: MedicationLogDao,
    private val appContext: Context? = null
) : MedicationLogRepository {

    override fun observeAll(): Flow<List<MedicationLogEntity>> =
        medicationLogDao.observeAll()

    override fun observeByMedicationId(medicationId: Long): Flow<List<MedicationLogEntity>> =
        medicationLogDao.observeByMedicationId(medicationId)

    override suspend fun getById(id: Long): MedicationLogEntity? =
        medicationLogDao.getById(id)

    override suspend fun getByMedicationAndScheduledTime(
        medicationId: Long,
        scheduledTime: String
    ): MedicationLogEntity? =
        medicationLogDao.getByMedicationAndScheduledTime(medicationId, scheduledTime)

    override suspend fun getPendingBefore(beforeTime: String): List<MedicationLogEntity> =
        medicationLogDao.getPendingBefore(beforeTime)

    override suspend fun insert(log: MedicationLogEntity): Long {
        val id = medicationLogDao.insert(log)
        notifyWidgets()
        return id
    }

    override suspend fun update(log: MedicationLogEntity) {
        medicationLogDao.update(log)
        notifyWidgets()
    }

    override suspend fun delete(log: MedicationLogEntity) {
        medicationLogDao.delete(log)
        notifyWidgets()
    }

    private fun notifyWidgets() {
        appContext?.let { MedicationWidgetUpdater.refreshAll(it) }
    }
}
