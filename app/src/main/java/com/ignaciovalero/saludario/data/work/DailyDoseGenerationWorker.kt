package com.ignaciovalero.saludario.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ignaciovalero.saludario.SaludarioApplication
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

        val today = LocalDate.now()
        val activeMedications = medicationRepo.getActiveForDate(today.toString())

        for (medication in activeMedications) {
            // Skip SPECIFIC_DAYS medications that don't match today's day of week
            if (medication.scheduleType == MedicationScheduleType.SPECIFIC_DAYS) {
                val selectedDays = medication.specificDays ?: continue
                if (today.dayOfWeek !in selectedDays) continue
            }

            for (time in medication.times) {
                val scheduledDateTime = LocalDateTime.of(today, time)
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

        return Result.success()
    }
}
