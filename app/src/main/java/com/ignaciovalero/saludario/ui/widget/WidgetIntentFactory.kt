package com.ignaciovalero.saludario.ui.widget

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.ignaciovalero.saludario.data.notification.NotificationHelper
import java.time.LocalDateTime

/**
 * Genera los `PendingIntent` que disparan los widgets al recibir un tap.
 *
 * Reutiliza el mismo contrato que las notificaciones (`ACTION_OPEN_DOSE`,
 * `EXTRA_OPEN_MEDICATION_ID`, `EXTRA_OPEN_SCHEDULED_TIME`) para que
 * `MainActivity` los procese a través de [com.ignaciovalero.saludario.ui.notification.MedicationNotificationTarget].
 */
internal object WidgetIntentFactory {

    /** Abre la app sin destacar ninguna toma concreta. */
    fun openAppPendingIntent(context: Context, appWidgetId: Int): PendingIntent {
        val intent = mainActivityIntent(context).apply {
            // requestCode/data únicos para que coexistan varios widgets sin
            // sobrescribirse PendingIntents.
            data = android.net.Uri.parse("saludario-widget://open/$appWidgetId")
        }
        return PendingIntent.getActivity(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Abre la app destacando la dosis indicada (mismo flujo que la
     * notificación). Si más de un PendingIntent comparte requestCode, los
     * extras se diferencian por `data` (`Uri`) para que el sistema considere
     * los intents distintos.
     */
    fun openDosePendingIntent(
        context: Context,
        appWidgetId: Int,
        medicationId: Long,
        scheduledDateTime: LocalDateTime
    ): PendingIntent {
        val scheduledTimeString = scheduledDateTime.toString()
        val intent = mainActivityIntent(context).apply {
            action = NotificationHelper.ACTION_OPEN_DOSE
            putExtra(NotificationHelper.EXTRA_OPEN_MEDICATION_ID, medicationId)
            putExtra(NotificationHelper.EXTRA_OPEN_SCHEDULED_TIME, scheduledTimeString)
            data = android.net.Uri.parse(
                "saludario-widget://dose/$appWidgetId/$medicationId/$scheduledTimeString"
            )
        }
        // Combinamos appWidgetId + medicationId para no colisionar entre filas
        // distintas dentro del mismo widget mediano.
        val requestCode = (appWidgetId.toLong() * 31L + medicationId).toInt()
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun mainActivityIntent(context: Context): Intent {
        return Intent().apply {
            component = ComponentName(
                context.packageName,
                "com.ignaciovalero.saludario.MainActivity"
            )
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }
}
