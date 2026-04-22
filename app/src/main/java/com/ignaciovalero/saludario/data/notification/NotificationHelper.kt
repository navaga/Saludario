package com.ignaciovalero.saludario.data.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.ignaciovalero.saludario.R
import com.ignaciovalero.saludario.core.localization.localizedMedicationDosage
import com.ignaciovalero.saludario.core.localization.localizedScheduledTime

object NotificationHelper {

    const val CHANNEL_ID = "medication_reminders"

    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun buildMedicationNotification(
        context: Context,
        medicationId: Long,
        medicationName: String,
        dosage: Double,
        unit: String,
        scheduledTime: String
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
            title = context.getString(R.string.notification_private_preview_title)
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
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
}
