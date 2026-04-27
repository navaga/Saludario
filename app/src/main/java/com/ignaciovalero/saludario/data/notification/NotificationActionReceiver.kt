package com.ignaciovalero.saludario.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationManager
import com.ignaciovalero.saludario.SaludarioApplication
import com.ignaciovalero.saludario.core.DoseConstants
import com.ignaciovalero.saludario.data.local.entity.MedicationLogEntity
import com.ignaciovalero.saludario.data.local.entity.MedicationStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TAKEN = "com.ignaciovalero.saludario.ACTION_TAKEN"
        const val ACTION_POSTPONE = "com.ignaciovalero.saludario.ACTION_POSTPONE"
        const val ACTION_SKIP = "com.ignaciovalero.saludario.ACTION_SKIP"
        const val EXTRA_MEDICATION_ID = "extra_medication_id"
        const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getLongExtra(EXTRA_MEDICATION_ID, -1L)
        val scheduledTimeStr = intent.getStringExtra(EXTRA_SCHEDULED_TIME) ?: return
        if (medicationId == -1L) return

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val notifId = NotificationHelper.notificationId(medicationId, scheduledTimeStr)

        val app = context.applicationContext as SaludarioApplication
        val container = app.container

        when (intent.action) {
            ACTION_TAKEN -> {
                val pendingResult = goAsync()
                scope.launch {
                    try {
                        val scheduledDateTime = LocalDateTime.parse(scheduledTimeStr)
                        val existingLog = container.medicationLogRepository
                            .getByMedicationAndScheduledTime(medicationId, scheduledTimeStr)

                        when {
                            existingLog == null -> {
                                container.medicationLogRepository.insert(
                                    MedicationLogEntity(
                                        medicationId = medicationId,
                                        scheduledTime = scheduledDateTime,
                                        takenTime = LocalDateTime.now(),
                                        status = MedicationStatus.TAKEN
                                    )
                                )
                                container.medicationRepository.decreaseStockForTakenDose(medicationId)
                            }

                            existingLog.status != MedicationStatus.TAKEN -> {
                                container.medicationLogRepository.update(
                                    existingLog.copy(
                                        status = MedicationStatus.TAKEN,
                                        takenTime = LocalDateTime.now()
                                    )
                                )
                                container.medicationRepository.decreaseStockForTakenDose(medicationId)
                            }
                        }

                        // Cancelar la notificación solo tras confirmar la escritura en BD
                        notificationManager.cancel(notifId)
                    } catch (e: Exception) {
                        // La escritura falló — mantener la notificación visible para que
                        // el usuario pueda volver a intentarlo
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            ACTION_POSTPONE -> {
                val pendingResult = goAsync()
                scope.launch {
                    try {
                        val scheduledDateTime = LocalDateTime.parse(scheduledTimeStr)
                        val postponeMinutes = DoseConstants.POSTPONE_MINUTES
                        val postponedUntil = LocalDateTime.now().plusMinutes(postponeMinutes)

                        val existingLog = container.medicationLogRepository
                            .getByMedicationAndScheduledTime(medicationId, scheduledTimeStr)

                        if (existingLog == null) {
                            container.medicationLogRepository.insert(
                                MedicationLogEntity(
                                    medicationId = medicationId,
                                    scheduledTime = scheduledDateTime,
                                    status = MedicationStatus.POSTPONED,
                                    postponedUntil = postponedUntil
                                )
                            )
                        } else if (existingLog.status != MedicationStatus.TAKEN) {
                            container.medicationLogRepository.update(
                                existingLog.copy(
                                    status = MedicationStatus.POSTPONED,
                                    postponedUntil = postponedUntil
                                )
                            )
                        }

                        // Programar primero; cancelar solo si tiene éxito
                        container.workScheduler.schedulePostponed(
                            medicationId = medicationId,
                            postponeMinutes = postponeMinutes,
                            scheduledTimeOriginal = scheduledDateTime
                        )
                        notificationManager.cancel(notifId)
                    } catch (e: Exception) {
                        // El reprogramado falló — mantener la notificación visible
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            ACTION_SKIP -> {
                val pendingResult = goAsync()
                scope.launch {
                    try {
                        val scheduledDateTime = LocalDateTime.parse(scheduledTimeStr)
                        val existingLog = container.medicationLogRepository
                            .getByMedicationAndScheduledTime(medicationId, scheduledTimeStr)

                        if (existingLog == null) {
                            container.medicationLogRepository.insert(
                                MedicationLogEntity(
                                    medicationId = medicationId,
                                    scheduledTime = scheduledDateTime,
                                    status = MedicationStatus.SKIPPED
                                )
                            )
                        } else if (existingLog.status != MedicationStatus.TAKEN) {
                            container.medicationLogRepository.update(
                                existingLog.copy(
                                    status = MedicationStatus.SKIPPED,
                                    postponedUntil = null,
                                    takenTime = null
                                )
                            )
                        }
                        notificationManager.cancel(notifId)
                    } catch (e: Exception) {
                        // La escritura falló — mantener la notificación visible
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
