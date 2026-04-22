package com.ignaciovalero.saludario.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ignaciovalero.saludario.SaludarioApplication
import com.ignaciovalero.saludario.data.local.entity.MedicationStatus
import java.time.LocalDateTime

class MissedDoseWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? SaludarioApplication ?: return Result.failure()
        val logRepo = app.container.medicationLogRepository

        val cutoff = LocalDateTime.now().minusMinutes(GRACE_PERIOD_MINUTES)
        val pendingLogs = logRepo.getPendingBefore(cutoff.toString())

        for (log in pendingLogs) {
            logRepo.update(log.copy(status = MedicationStatus.MISSED))
        }

        return Result.success()
    }

    companion object {
        const val GRACE_PERIOD_MINUTES = 60L
    }
}
