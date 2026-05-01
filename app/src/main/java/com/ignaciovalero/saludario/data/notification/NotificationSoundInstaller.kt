package com.ignaciovalero.saludario.data.notification

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log

/**
 * Copia los archivos `res/raw/notification_*.mp3` al almacenamiento del
 * dispositivo a través de [MediaStore.Audio.Media] para que estén disponibles
 * como sonidos de notificación reales del sistema.
 *
 * Motivo: las capas OEM como MIUI/HyperOS de Xiaomi ignoran el sonido cuando
 * el [android.app.NotificationChannel] se crea con un URI `android.resource://`
 * apuntando a un raw del propio APK, y reproducen siempre el sonido por
 * defecto del sistema. Al insertar el archivo en `MediaStore.Audio` queda
 * registrado como sonido de notificación real (`IS_NOTIFICATION = 1`) y el
 * URI `content://media/...` resultante sí es respetado por los canales.
 *
 * Solo intenta la instalación en API 29+ porque a partir de Android Q ya no
 * se necesita `WRITE_EXTERNAL_STORAGE` para escribir en `MediaStore.Audio`
 * gracias a scoped storage. En API < 29 se devuelve `null` y los canales
 * caen al URI `android.resource://` (mismo comportamiento que antes).
 */
object NotificationSoundInstaller {

    private const val TAG = "SoundInstaller"

    /**
     * Asegura que todos los sonidos personalizados estén instalados en
     * [MediaStore.Audio.Media]. Es idempotente: si ya existen no hace nada.
     * Pensado para llamarse al arrancar la app, antes de crear los canales.
     */
    fun installAll(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        MedicationNotificationSound.entries.forEach { sound ->
            if (sound == MedicationNotificationSound.SYSTEM) return@forEach
            runCatching { ensureInstalled(context, sound) }
                .onFailure { Log.w(TAG, "No se pudo instalar ${sound.storageKey}: ${it.message}") }
        }
    }

    /**
     * Devuelve el URI `content://media/...` del sonido si está instalado o
     * `null` si no se pudo localizar/instalar. Intenta instalarlo si todavía
     * no existe (solo en API 29+).
     */
    fun installedUri(context: Context, sound: MedicationNotificationSound): Uri? {
        if (sound == MedicationNotificationSound.SYSTEM) return null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return runCatching { ensureInstalled(context, sound) }
            .onFailure { Log.w(TAG, "installedUri ${sound.storageKey}: ${it.message}") }
            .getOrNull()
    }

    private fun ensureInstalled(context: Context, sound: MedicationNotificationSound): Uri? {
        val rawId = sound.rawResId(context)
        if (rawId == 0) return null

        val displayName = mediaFileName(sound)
        val existing = findExisting(context, displayName)
        if (existing != null) return existing

        val resolver = context.contentResolver
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
            put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_NOTIFICATIONS}/Saludario")
            put(MediaStore.Audio.Media.IS_NOTIFICATION, 1)
            put(MediaStore.Audio.Media.IS_RINGTONE, 0)
            put(MediaStore.Audio.Media.IS_ALARM, 0)
            put(MediaStore.Audio.Media.IS_MUSIC, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
        }

        val uri = resolver.insert(collection, values) ?: return null
        try {
            resolver.openOutputStream(uri).use { out ->
                if (out == null) {
                    resolver.delete(uri, null, null)
                    return null
                }
                context.resources.openRawResource(rawId).use { input ->
                    input.copyTo(out)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val finalize = ContentValues().apply {
                    put(MediaStore.Audio.Media.IS_PENDING, 0)
                }
                resolver.update(uri, finalize, null, null)
            }
        } catch (t: Throwable) {
            runCatching { resolver.delete(uri, null, null) }
            throw t
        }
        return uri
    }

    private fun findExisting(context: Context, displayName: String): Uri? {
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val projection = arrayOf(MediaStore.Audio.Media._ID)
        val selection = "${MediaStore.Audio.Media.DISPLAY_NAME} = ?"
        val args = arrayOf(displayName)
        context.contentResolver.query(collection, projection, selection, args, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(0)
                return Uri.withAppendedPath(collection, id.toString())
            }
        }
        return null
    }

    private fun mediaFileName(sound: MedicationNotificationSound): String {
        val raw = sound.rawResourceName ?: return "saludario_${sound.storageKey}.mp3"
        return "saludario_$raw.mp3"
    }
}
