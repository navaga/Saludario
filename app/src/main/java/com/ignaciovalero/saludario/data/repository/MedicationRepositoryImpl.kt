package com.ignaciovalero.saludario.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.ignaciovalero.saludario.data.local.dao.MedicationDao
import com.ignaciovalero.saludario.data.local.entity.MedicationEntity
import com.ignaciovalero.saludario.data.notification.NotificationHelper
import com.ignaciovalero.saludario.data.preferences.UserPreferencesDataSource
import com.ignaciovalero.saludario.domain.insights.InsightSeverity
import com.ignaciovalero.saludario.domain.repository.MedicationRepository
import kotlinx.coroutines.flow.Flow

class MedicationRepositoryImpl(
    private val medicationDao: MedicationDao,
    private val appContext: Context,
    private val userPreferencesDataSource: UserPreferencesDataSource
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

        val currentSeverity = lowStockSeverityFor(
            stockRemaining = updatedRemaining,
            stockTotal = medication.stockTotal,
            lowStockThreshold = medication.lowStockThreshold
        )
        val lastState = userPreferencesDataSource.getLastLowStockNotifiedState(medicationId)
        val currentState = currentSeverity?.name

        if (currentState == null) {
            userPreferencesDataSource.clearLastLowStockNotifiedState(medicationId)
            return
        }

        if (lastState == currentState) {
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            appContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) {
            val notification = NotificationHelper
                .buildLowStockNotification(appContext, medication.name)
                .build()
            NotificationManagerCompat.from(appContext)
                .notify(NotificationHelper.lowStockNotificationId(medicationId), notification)
        }

        userPreferencesDataSource.setLastLowStockNotifiedState(medicationId, currentState)
    }

    override suspend fun addStock(medicationId: Long, amount: Double) {
        if (amount <= 0.0) return

        val medication = medicationDao.getById(medicationId) ?: return
        val updatedTotal = medication.stockTotal + amount
        val updatedRemaining = medication.stockRemaining + amount

        medicationDao.updateStockFields(
            medicationId = medicationId,
            stockTotal = updatedTotal,
            stockRemaining = updatedRemaining,
            lowStockThreshold = medication.lowStockThreshold
        )

        NotificationManagerCompat.from(appContext)
            .cancel(NotificationHelper.lowStockNotificationId(medicationId))
        userPreferencesDataSource.clearLastLowStockNotifiedState(medicationId)
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
