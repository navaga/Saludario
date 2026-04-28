package com.ignaciovalero.saludario.ui.notification

import android.content.Intent
import com.ignaciovalero.saludario.data.notification.NotificationHelper
import java.time.LocalDateTime

/**
 * Dosis concreta que la app debe destacar tras tocar el cuerpo de una
 * notificación. La notificación entrega `scheduledTime` como ISO
 * `LocalDateTime` (igual que `MedicationReminderWorker.KEY_SCHEDULED_TIME`),
 * por lo que aquí lo conservamos ya parseado para que cualquier consumidor
 * pueda derivar fecha, hora o ambas sin reparsear.
 */
data class MedicationNotificationTarget(
    val medicationId: Long,
    val scheduledDateTime: LocalDateTime
) {
    companion object {
        /**
         * Extrae un objetivo válido del Intent recibido por `MainActivity`.
         * Devuelve `null` si el intent no procede de la notificación, le
         * faltan extras o el `scheduledTime` no es parseable.
         */
        fun fromIntent(intent: Intent?): MedicationNotificationTarget? {
            if (intent == null) return null
            if (intent.action != NotificationHelper.ACTION_OPEN_DOSE) return null
            val medicationId = intent.getLongExtra(NotificationHelper.EXTRA_OPEN_MEDICATION_ID, -1L)
            val scheduledTimeStr = intent.getStringExtra(NotificationHelper.EXTRA_OPEN_SCHEDULED_TIME)
            if (medicationId == -1L || scheduledTimeStr.isNullOrEmpty()) return null
            val parsed = runCatching { LocalDateTime.parse(scheduledTimeStr) }.getOrNull()
                ?: return null
            return MedicationNotificationTarget(medicationId, parsed)
        }
    }
}
