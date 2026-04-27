package com.ignaciovalero.saludario.core.permissions

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat

/**
 * Snapshot del estado de los requisitos para que las notificaciones de
 * recordatorio se entreguen con fiabilidad.
 *
 * - [notificationsAllowed]: el usuario concedió `POST_NOTIFICATIONS`
 *   (Android 13+). En versiones anteriores siempre `true`.
 * - [batteryUnrestricted]: la app está fuera de la optimización de batería
 *   (Doze no la afecta).
 * - [exactAlarmsAllowed]: en Android 12+ el sistema permite `setExact*`.
 *   En versiones anteriores siempre `true`. Hoy no se usan, pero queda
 *   listo por si migramos a `AlarmManager.setAlarmClock`.
 */
data class ReminderReliabilityStatus(
    val notificationsAllowed: Boolean,
    val batteryUnrestricted: Boolean,
    val exactAlarmsAllowed: Boolean
) {
    val allOk: Boolean get() = notificationsAllowed && batteryUnrestricted && exactAlarmsAllowed
    val issueCount: Int =
        (if (!notificationsAllowed) 1 else 0) +
        (if (!batteryUnrestricted) 1 else 0) +
        (if (!exactAlarmsAllowed) 1 else 0)
}

object ReminderReliability {

    fun snapshot(context: Context): ReminderReliabilityStatus = ReminderReliabilityStatus(
        notificationsAllowed = areNotificationsAllowed(context),
        batteryUnrestricted = isBatteryUnrestricted(context),
        exactAlarmsAllowed = areExactAlarmsAllowed(context)
    )

    fun areNotificationsAllowed(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isBatteryUnrestricted(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            ?: return true
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun areExactAlarmsAllowed(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return true
        return am.canScheduleExactAlarms()
    }
}
