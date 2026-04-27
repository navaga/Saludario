package com.ignaciovalero.saludario.data.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ignaciovalero.saludario.SaludarioApplication
import com.ignaciovalero.saludario.core.DoseConstants.GRACE_PERIOD_MINUTES
import com.ignaciovalero.saludario.data.local.entity.MedicationLogEntity
import com.ignaciovalero.saludario.data.local.entity.MedicationScheduleType
import com.ignaciovalero.saludario.data.local.entity.MedicationStatus
import java.time.LocalDate
import java.time.LocalDateTime

class DailyDoseGenerationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? SaludarioApplication ?: return Result.failure()
        val medicationRepo = app.container.medicationRepository
        val logRepo = app.container.medicationLogRepository

        return try {
            val today = LocalDate.now()
            val now = LocalDateTime.now()
            val activeMedications = medicationRepo.getActiveForDate(today.toString())

            for (medication in activeMedications) {
                // Skip SPECIFIC_DAYS medications that don't match today's day of week
                if (medication.scheduleType == MedicationScheduleType.SPECIFIC_DAYS) {
                    val selectedDays = medication.specificDays ?: continue
                    if (today.dayOfWeek !in selectedDays) continue
                }

                for (time in medication.times) {
                    val scheduledDateTime = LocalDateTime.of(today, time)

                    // Si la medicación empieza hoy y la hora ya pasó el periodo de gracia,
                    // no crear log para evitar que aparezca como olvidada al añadirla tarde
                    if (medication.startDate == today &&
                        scheduledDateTime.plusMinutes(GRACE_PERIOD_MINUTES).isBefore(now)
                    ) {
                        continue
                    }

                    val scheduledTimeStr = scheduledDateTime.toString()

                    val existing = logRepo.getByMedicationAndScheduledTime(
                        medication.id,
                        scheduledTimeStr
                    )
                    if (existing == null) {
                        logRepo.insert(
                            MedicationLogEntity(
                                medicationId = medication.id,
                                scheduledTime = scheduledDateTime,
                                status = MedicationStatus.PENDING
                            )
                        )
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error generando dosis diarias", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "DailyDoseGenWorker"
    }
}
