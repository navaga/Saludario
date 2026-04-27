package com.ignaciovalero.saludario.data.notification

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.ignaciovalero.saludario.data.preferences.UserPreferencesDataSource
import com.ignaciovalero.saludario.domain.insights.InsightSeverity

/**
 * Encapsula la emisión y cancelación de notificaciones de stock bajo,
 * incluyendo la coordinación con el estado persistido en `UserPreferences`
 * para evitar notificaciones duplicadas y la verificación de permisos.
 *
 * Antes esta lógica vivía en `MedicationRepositoryImpl`, mezclando
 * responsabilidades de persistencia con UI/sistema.
 */
class LowStockNotifier(
    private val appContext: Context,
    private val userPreferencesDataSource: UserPreferencesDataSource
) {

    /**
     * Notifica al usuario si la severidad de stock bajo cambió desde la última
     * notificación. Si la severidad es `null`, limpia el estado para permitir
     * futuras notificaciones cuando vuelva a entrar en estado bajo.
     */
    suspend fun notifyIfSeverityChanged(
        medicationId: Long,
        medicationName: String,
        severity: InsightSeverity?
    ) {
        val lastState = userPreferencesDataSource.getLastLowStockNotifiedState(medicationId)
        val currentState = severity?.name

        if (currentState == null) {
            userPreferencesDataSource.clearLastLowStockNotifiedState(medicationId)
            return
        }

        if (lastState == currentState) return

        if (canPostNotifications()) {
            val notification = NotificationHelper
                .buildLowStockNotification(appContext, medicationName)
                .build()
            NotificationManagerCompat.from(appContext)
                .notify(NotificationHelper.lowStockNotificationId(medicationId), notification)
        }

        userPreferencesDataSource.setLastLowStockNotifiedState(medicationId, currentState)
    }

    /**
     * Cancela cualquier notificación activa de stock bajo para esta medicación
     * y limpia el estado persistido.
     */
    suspend fun clear(medicationId: Long) {
        NotificationManagerCompat.from(appContext)
            .cancel(NotificationHelper.lowStockNotificationId(medicationId))
        userPreferencesDataSource.clearLastLowStockNotifiedState(medicationId)
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            appContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }
}
