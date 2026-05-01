package com.ignaciovalero.saludario.data.notification

import android.content.ContentResolver
import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.net.toUri
import com.ignaciovalero.saludario.R

/**
 * Opciones de sonido configurables para los recordatorios de medicación.
 *
 * En Android 8+ el sonido efectivo lo controla el [android.app.NotificationChannel],
 * por eso cada opción tiene su propio `channelId`. La opción [SYSTEM] usa el
 * sonido por defecto del dispositivo y reutiliza el canal heredado.
 *
 * Los recursos `R.raw.notification_*` se resuelven por nombre para evitar romper
 * el build mientras los MP3 aún no están en `res/raw/`. Si el archivo no existe,
 * el canal se crea sin sonido personalizado y se cae al sonido del sistema.
 */
enum class MedicationNotificationSound(
    /** Clave estable persistida en DataStore. No traducir ni cambiar. */
    val storageKey: String,
    /** Identificador estable del [android.app.NotificationChannel]. */
    val channelId: String,
    /** Nombre visible en UI (onboarding y ajustes). */
    val displayNameRes: Int,
    /** Nombre del archivo en `res/raw/` sin extensión. `null` para SYSTEM. */
    val rawResourceName: String?
) {
    SYSTEM(
        storageKey = "system",
        channelId = "medication_reminders",
        displayNameRes = R.string.medication_sound_system,
        rawResourceName = null
    ),
    SILOFONO(
        storageKey = "silofono",
        channelId = "medication_reminders_silofono_v2",
        displayNameRes = R.string.medication_sound_silofono,
        rawResourceName = "notification_silofono"
    ),
    TIMBRE(
        storageKey = "timbre",
        channelId = "medication_reminders_timbre_v2",
        displayNameRes = R.string.medication_sound_timbre,
        rawResourceName = "notification_timbre"
    ),
    ESTRELLAS(
        storageKey = "estrellas",
        channelId = "medication_reminders_estrellas_v2",
        displayNameRes = R.string.medication_sound_estrellas,
        rawResourceName = "notification_estrellas"
    ),
    CAMPANAS(
        storageKey = "campanas",
        channelId = "medication_reminders_campanas_v2",
        displayNameRes = R.string.medication_sound_campanas,
        rawResourceName = "notification_campanas"
    ),
    UNIVERSO(
        storageKey = "universo",
        channelId = "medication_reminders_universo_v2",
        displayNameRes = R.string.medication_sound_universo,
        rawResourceName = "notification_universo"
    );

    /** Devuelve el id del recurso raw o `0` si no existe. */
    fun rawResId(context: Context): Int {
        val name = rawResourceName ?: return 0
        return context.resources.getIdentifier(name, "raw", context.packageName)
    }

    /**
     * URI del sonido a usar para preescucha o para el canal.
     * Para [SYSTEM] devuelve la notificación por defecto del sistema.
     * Para opciones propias devuelve la URI `android.resource://...` del raw,
     * o la del sistema si el archivo aún no está disponible.
     */
    fun soundUri(context: Context): Uri {
        if (this == SYSTEM) {
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
        val resId = rawResId(context)
        return if (resId == 0) {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        } else {
            "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/$resId".toUri()
        }
    }

    companion object {
        val DEFAULT: MedicationNotificationSound = SYSTEM

        fun fromStorage(value: String?): MedicationNotificationSound {
            if (value.isNullOrBlank()) return DEFAULT
            return entries.firstOrNull { it.storageKey == value } ?: DEFAULT
        }
    }
}
