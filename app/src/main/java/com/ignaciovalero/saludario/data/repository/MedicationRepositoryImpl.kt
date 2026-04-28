package com.ignaciovalero.saludario.data.repository

import com.ignaciovalero.saludario.data.local.dao.MedicationDao
import com.ignaciovalero.saludario.data.local.entity.MedicationEntity
import com.ignaciovalero.saludario.data.notification.LowStockNotifier
import com.ignaciovalero.saludario.domain.insights.InsightSeverity
import com.ignaciovalero.saludario.domain.repository.MedicationRepository
import kotlinx.coroutines.flow.Flow

class MedicationRepositoryImpl(
    private val medicationDao: MedicationDao,
    private val lowStockNotifier: LowStockNotifier
) : MedicationRepository {

    override fun observeAll(): Flow<List<MedicationEntity>> =
        medicationDao.observeAll()

    override suspend fun getById(id: Long): MedicationEntity? =
        medicationDao.getById(id)

    override suspend fun getActiveForDate(date: String): List<MedicationEntity> =
        medicationDao.getActiveForDate(date)

    override suspend fun insert(medication: MedicationEntity): Long =
        medicationDao.insert(medication)

    override suspend fun update(medication: MedicationEntity) =
        medicationDao.update(medication)

    override suspend fun delete(medication: MedicationEntity) =
        medicationDao.delete(medication)

    override suspend fun decreaseStockForTakenDose(medicationId: Long) {
        val medication = medicationDao.getById(medicationId) ?: return
        val updatedRemaining = (medication.stockRemaining - medication.dosage).coerceAtLeast(0.0)
        medicationDao.updateStockRemaining(medicationId, updatedRemaining)

        val severity = lowStockSeverityFor(
            stockRemaining = updatedRemaining,
            stockTotal = medication.stockTotal,
            lowStockThreshold = medication.lowStockThreshold
        )
        lowStockNotifier.notifyIfSeverityChanged(
            medicationId = medicationId,
            medicationName = medication.name,
            severity = severity
        )
    }

    override suspend fun addStock(medicationId: Long, amount: Double) {
        if (amount <= 0.0) return

        val medication = medicationDao.getById(medicationId) ?: return
        val updatedRemaining = medication.stockRemaining + amount
        // El total inicial se mantiene como referencia. Solo se recalibra si el
        // restante supera el total previo (te has pasado del tamaño que tenías
        // configurado, así que ampliamos el máximo para que la barra de
        // progreso y los avisos de stock bajo sigan teniendo sentido).
        val updatedTotal = maxOf(medication.stockTotal, updatedRemaining)

        medicationDao.updateStockFields(
            medicationId = medicationId,
            stockTotal = updatedTotal,
            stockRemaining = updatedRemaining,
            lowStockThreshold = medication.lowStockThreshold
        )

        lowStockNotifier.clear(medicationId)
    }

    private fun lowStockSeverityFor(
        stockRemaining: Double,
        stockTotal: Double,
        lowStockThreshold: Double
    ): InsightSeverity? {
        val hasQuantityTracking = stockTotal > 0.0 || stockRemaining > 0.0 || lowStockThreshold > 0.0
        if (!hasQuantityTracking) return null

        val isCritical = stockRemaining <= CRITICAL_LOW_STOCK_UNITS
        if (isCritical) return InsightSeverity.CRITICAL

        val isWarningByThreshold = stockRemaining <= lowStockThreshold
        val isWarningByPercent = stockTotal > 0.0 && (stockRemaining / stockTotal) <= LOW_STOCK_THRESHOLD_PERCENT
        return if (isWarningByThreshold || isWarningByPercent) InsightSeverity.WARNING else null
    }

    private companion object {
        const val CRITICAL_LOW_STOCK_UNITS = 2.0
        const val LOW_STOCK_THRESHOLD_PERCENT = 0.15
    }
}
