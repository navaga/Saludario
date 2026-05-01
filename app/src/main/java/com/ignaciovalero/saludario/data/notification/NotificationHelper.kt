package com.ignaciovalero.saludario.data.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import androidx.core.app.NotificationCompat
import com.ignaciovalero.saludario.R
import com.ignaciovalero.saludario.core.localization.localizedMedicationDosage
import com.ignaciovalero.saludario.core.localization.localizedScheduledTime

object NotificationHelper {

    const val CHANNEL_ID = "medication_reminders"

    /** Acción usada en el Intent que abre la app al tocar el cuerpo de la notificación. */
    const val ACTION_OPEN_DOSE = "com.ignaciovalero.saludario.ACTION_OPEN_DOSE"

    /** Identificador del medicamento de la dosis que originó la notificación. */
    const val EXTRA_OPEN_MEDICATION_ID = "extra_open_medication_id"

    /**
     * Hora programada (ISO `LocalDateTime`, igual al valor que usa
     * `MedicationReminderWorker.KEY_SCHEDULED_TIME`) de la dosis a abrir.
     */
    const val EXTRA_OPEN_SCHEDULED_TIME = "extra_open_scheduled_time"

    fun createNotificationChannel(context: Context) {
        createNotificationChannels(context)
    }

    /**
     * Registra un canal por cada [MedicationNotificationSound]. Cada canal lleva
     * su propio sonido, lo que sortea la limitación de Android 8+ por la que
     * el sonido de un canal es inmutable tras crearlo: el usuario cambia de
     * sonido eligiendo otro canal, no editando uno existente.
     */
    /**
     * IDs de canales antiguos (versión 1.x) que apuntaban a sonidos como
     * `android.resource://...`. En MIUI/HyperOS estos canales reproducen
     * siempre el sonido por defecto del sistema, por lo que se sustituyen
     * por canales `_v2` que apuntan a un URI de [MediaStore.Audio.Media].
     * Mantenemos esta lista para borrarlos en updates.
     */
    private val LEGACY_CHANNEL_IDS = listOf(
        "medication_reminders_silofono",
        "medication_reminders_timbre",
        "medication_reminders_estrellas",
        "medication_reminders_campanas",
        "medication_reminders_universo"
    )

    fun createNotificationChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        // Asegura que los MP3 estén instalados como sonidos reales del
        // sistema antes de registrar los canales (necesario para Xiaomi).
        NotificationSoundInstaller.installAll(context)

        // Limpia los canales antiguos para que no aparezcan duplicados en
        // ajustes y para que el usuario no siga usando los rotos.
        LEGACY_CHANNEL_IDS.forEach { legacyId ->
            runCatching { manager.deleteNotificationChannel(legacyId) }
        }

        val channelName = context.getString(R.string.notification_channel_name)
        val channelDescription = context.getString(R.string.notification_channel_description)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        MedicationNotificationSound.entries.forEach { sound ->
            val displayName = when (sound) {
                MedicationNotificationSound.SYSTEM -> channelName
                else -> context.getString(
                    R.string.notification_channel_name_with_sound,
                    channelName,
                    context.getString(sound.displayNameRes)
                )
            }
            val channel = NotificationChannel(
                sound.channelId,
                displayName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = channelDescription
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                if (sound != MedicationNotificationSound.SYSTEM) {
                    val resId = sound.rawResId(context)
                    if (resId != 0) {
                        // Prioriza el URI del MediaStore (compatible con
                        // OEMs como MIUI). Si la instalación falló, cae al
                        // URI `android.resource://` del raw del APK.
                        val uri = NotificationSoundInstaller.installedUri(context, sound)
                            ?: sound.soundUri(context)
                        setSound(uri, audioAttributes)
                    }
                }
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun buildMedicationNotification(
        context: Context,
        medicationId: Long,
        medicationName: String,
        dosage: Double,
        unit: String,
        scheduledTime: String,
        sound: MedicationNotificationSound = MedicationNotificationSound.DEFAULT
    ): NotificationCompat.Builder {
        val localizedDosage = context.localizedMedicationDosage(dosage, unit)
        val localizedTime = context.localizedScheduledTime(scheduledTime)

        val takenIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_TAKEN
            putExtra(NotificationActionReceiver.EXTRA_MEDICATION_ID, medicationId)
            putExtra(NotificationActionReceiver.EXTRA_SCHEDULED_TIME, scheduledTime)
        }
        val takenPending = PendingIntent.getBroadcast(
            context,
            (medicationId * 1000 + 1).toInt(),
            takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val postponeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_POSTPONE
            putExtra(NotificationActionReceiver.EXTRA_MEDICATION_ID, medicationId)
            putExtra(NotificationActionReceiver.EXTRA_SCHEDULED_TIME, scheduledTime)
        }
        val postponePending = PendingIntent.getBroadcast(
            context,
            (medicationId * 1000 + 2).toInt(),
            postponeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val skipIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SKIP
            putExtra(NotificationActionReceiver.EXTRA_MEDICATION_ID, medicationId)
            putExtra(NotificationActionReceiver.EXTRA_SCHEDULED_TIME, scheduledTime)
        }
        val skipPending = PendingIntent.getBroadcast(
            context,
            (medicationId * 1000 + 3).toInt(),
            skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentIntent = buildOpenDosePendingIntent(
            context = context,
            medicationId = medicationId,
            scheduledTime = scheduledTime
        )
        val publicVersion = buildPrivatePreviewNotification(
            context = context,
            contentIntent = contentIntent,
            title = context.getString(R.string.notification_private_preview_title)
        )

        return NotificationCompat.Builder(context, sound.channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText("$medicationName — $localizedDosage")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.notification_body, medicationName, localizedDosage, localizedTime))
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setPublicVersion(publicVersion.build())
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .addAction(R.drawable.ic_launcher_foreground, context.getString(R.string.notification_action_taken), takenPending)
            .addAction(R.drawable.ic_launcher_foreground, context.getString(R.string.notification_action_postpone), postponePending)
            .addAction(R.drawable.ic_launcher_foreground, context.getString(R.string.notification_action_skip), skipPending)
    }

    fun notificationId(medicationId: Long, scheduledTime: String): Int {
        return (medicationId.toString() + scheduledTime.replace(":", "")).hashCode()
    }

    fun lowStockNotificationId(medicationId: Long): Int {
        return "low-stock-$medicationId".hashCode()
    }

    fun buildLowStockNotification(
        context: Context,
        medicationName: String
    ): NotificationCompat.Builder {
        val contentIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.let { intent ->
                PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
        val publicVersion = buildPrivatePreviewNotification(
            context = context,
            contentIntent = contentIntent,
            title = context.getString(R.string.low_stock_notification_private_preview_title)
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.low_stock_notification_title))
            .setContentText(context.getString(R.string.low_stock_notification_message, medicationName))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.low_stock_notification_message, medicationName))
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion.build())
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
    }

    private fun buildPrivatePreviewNotification(
        context: Context,
        contentIntent: PendingIntent?,
        title: String
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.notification_private_preview_body))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.notification_private_preview_body))
            )
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
    }

    /**
     * Construye el `PendingIntent` que abre `MainActivity` indicándole qué
     * dosis debe destacar al pulsar el cuerpo de la notificación. Reutiliza el
     * mismo `notificationId(...)` como `requestCode` para que extras nuevos
     * (`FLAG_UPDATE_CURRENT`) reemplacen a los antiguos sin colisionar entre
     * notificaciones distintas.
     */
    private fun buildOpenDosePendingIntent(
        context: Context,
        medicationId: Long,
        scheduledTime: String
    ): PendingIntent {
        val intent = Intent().apply {
            // Componente explícito por nombre para no depender de import directo
            // de MainActivity (evita ciclos entre módulos/paquetes).
            component = ComponentName(context.packageName, "com.ignaciovalero.saludario.MainActivity")
            action = ACTION_OPEN_DOSE
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_MEDICATION_ID, medicationId)
            putExtra(EXTRA_OPEN_SCHEDULED_TIME, scheduledTime)
        }
        val requestCode = notificationId(medicationId, scheduledTime)
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
