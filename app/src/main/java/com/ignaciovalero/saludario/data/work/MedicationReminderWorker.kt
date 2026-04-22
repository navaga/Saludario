package com.ignaciovalero.saludario.data.work

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.app.NotificationManager
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ignaciovalero.saludario.SaludarioApplication
import com.ignaciovalero.saludario.data.notification.NotificationHelper

class MedicationReminderWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_MEDICATION_ID = "medication_id"
        const val KEY_MEDICATION_NAME = "medication_name"
        const val KEY_DOSAGE = "dosage"
        const val KEY_SCHEDULED_TIME = "scheduled_time"
    }

    override suspend fun doWork(): Result {
        val medicationId = inputData.getLong(KEY_MEDICATION_ID, -1L)
        val scheduledTime = inputData.getString(KEY_SCHEDULED_TIME) ?: return Result.failure()

        if (medicationId == -1L) return Result.failure()

        // Verify the medication still exists
        val app = appContext.applicationContext as SaludarioApplication
        val medication = app.container.medicationRepository.getById(medicationId)
            ?: return Result.success() // Medication was deleted, skip silently

        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.failure()
        }

        val notification = NotificationHelper.buildMedicationNotification(
            context = appContext,
            medicationId = medicationId,
            medicationName = medication.name,
            dosage = medication.dosage,
            unit = medication.unit,
            scheduledTime = scheduledTime
        ).build()

        val notifId = NotificationHelper.notificationId(medicationId, scheduledTime)
        val manager = appContext.getSystemService(NotificationManager::class.java)
        manager.notify(notifId, notification)

        // Reschedule for the next dose based on schedule type
        app.container.workScheduler.scheduleNextDose(
            medication = medication
        )

        return Result.success()
    }
}
