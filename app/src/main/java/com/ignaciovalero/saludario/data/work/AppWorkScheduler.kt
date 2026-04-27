package com.ignaciovalero.saludario.data.work

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ignaciovalero.saludario.data.local.entity.MedicationEntity
import com.ignaciovalero.saludario.domain.scheduling.NextDoseCalculator
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class AppWorkScheduler(
    private val workManager: WorkManager
) {

    fun scheduleMedicationReminders(
        medicationId: Long,
        times: List<LocalTime>
    ) {
        times.forEach { time ->
            scheduleForTime(medicationId, time)
        }
    }

    fun cancelMedicationReminders(medicationId: Long) {
        workManager.cancelAllWorkByTag(tagForMedication(medicationId))
    }

    fun schedulePostponed(
        medicationId: Long,
        postponeMinutes: Long,
        scheduledTimeOriginal: LocalDateTime? = null
    ) {
        val scheduledTime = scheduledTimeOriginal ?: LocalDateTime.now().plusMinutes(postponeMinutes)
        val data = workDataOf(
            MedicationReminderWorker.KEY_MEDICATION_ID to medicationId,
            MedicationReminderWorker.KEY_SCHEDULED_TIME to scheduledTime.toString()
        )

        val request = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
            .setInitialDelay(postponeMinutes, TimeUnit.MINUTES)
            .setInputData(data)
            .addTag(TAG_MEDICATION_REMINDER)
            .addTag(tagForMedication(medicationId))
            .build()

        workManager.enqueue(request)
    }

    /**
     * Cancela cualquier worker de recordatorio pendiente para una dosis
     * concreta (usado al posponer desde la UI antes de re-programar).
     */
    fun cancelReminderForScheduledTime(medicationId: Long, scheduledTime: LocalDateTime) {
        workManager.cancelUniqueWork(
            uniqueWorkName(medicationId, scheduledTime.toLocalTime().toString())
        )
    }

    fun scheduleNextDose(medication: MedicationEntity) {
        val now = LocalDateTime.now()
        val nextRun = NextDoseCalculator.nextDose(medication, now) ?: return
        val delay = Duration.between(now, nextRun)
        if (delay.isNegative) return

        val data = workDataOf(
            MedicationReminderWorker.KEY_MEDICATION_ID to medication.id,
            MedicationReminderWorker.KEY_SCHEDULED_TIME to nextRun.toString()
        )

        val request = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(TAG_MEDICATION_REMINDER)
            .addTag(tagForMedication(medication.id))
            .build()

        workManager.enqueueUniqueWork(
            uniqueWorkName(medication.id, nextRun.toLocalTime().toString()),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun scheduleForTime(
        medicationId: Long,
        time: LocalTime
    ) {
        val now = LocalDateTime.now()
        var targetDateTime = LocalDateTime.of(LocalDate.now(), time)
        if (targetDateTime.isBefore(now)) {
            targetDateTime = targetDateTime.plusDays(1)
        }
        val delay = Duration.between(now, targetDateTime)

        val data = workDataOf(
            MedicationReminderWorker.KEY_MEDICATION_ID to medicationId,
            MedicationReminderWorker.KEY_SCHEDULED_TIME to targetDateTime.toString()
        )

        val request = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(TAG_MEDICATION_REMINDER)
            .addTag(tagForMedication(medicationId))
            .build()

        workManager.enqueueUniqueWork(
            uniqueWorkName(medicationId, time.toString()),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    companion object {
        private const val TAG_MEDICATION_REMINDER = "medication_reminder"
        private const val WORK_DAILY_DOSE_GENERATION = "daily_dose_generation"
        private const val WORK_MISSED_DOSE_CHECK = "missed_dose_check"

        private fun tagForMedication(medicationId: Long) =
            "medication_$medicationId"

        private fun uniqueWorkName(medicationId: Long, time: String = "") =
            "reminder_${medicationId}_$time"
    }

    fun scheduleDailyDoseGeneration() {
        val request = PeriodicWorkRequestBuilder<DailyDoseGenerationWorker>(
            24, TimeUnit.HOURS
        ).build()

        workManager.enqueueUniquePeriodicWork(
            WORK_DAILY_DOSE_GENERATION,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun scheduleMissedDoseCheck() {
        val request = PeriodicWorkRequestBuilder<MissedDoseWorker>(
            15, TimeUnit.MINUTES
        ).build()

        workManager.enqueueUniquePeriodicWork(
            WORK_MISSED_DOSE_CHECK,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun runImmediateDoseGeneration() {
        val request = OneTimeWorkRequestBuilder<DailyDoseGenerationWorker>().build()
        workManager.enqueue(request)
    }
}