package com.ignaciovalero.saludario.ui.notification

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.ignaciovalero.saludario.data.notification.MedicationNotificationSound

/**
 * Preescucha de sonidos de notificación reutilizable. Mantiene un único
 * [MediaPlayer] vivo y lo libera al salir de la composición o al solicitar
 * un nuevo sonido.
 */
class NotificationSoundPreviewPlayer(private val context: Context) {

    private var player: MediaPlayer? = null

    fun play(sound: MedicationNotificationSound) {
        stop()
        val uri = sound.soundUri(context)
        runCatching {
            val newPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context, uri)
                setOnCompletionListener {
                    runCatching { it.release() }
                    if (player === it) player = null
                }
                setOnErrorListener { mp, _, _ ->
                    runCatching { mp.release() }
                    if (player === mp) player = null
                    true
                }
                prepare()
                start()
            }
            player = newPlayer
        }.onFailure { error ->
            Log.w("SoundPreview", "No se pudo reproducir ${sound.storageKey}: ${error.message}")
            // Fallback: intenta sonar la notificación por defecto.
            if (sound != MedicationNotificationSound.SYSTEM) {
                runCatching {
                    val fallback = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        setDataSource(
                            context,
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        )
                        prepare()
                        start()
                    }
                    player = fallback
                }
            }
        }
    }

    fun stop() {
        player?.let { current ->
            runCatching {
                if (current.isPlaying) current.stop()
            }
            runCatching { current.release() }
        }
        player = null
    }
}

@Composable
fun rememberNotificationSoundPreviewPlayer(): NotificationSoundPreviewPlayer {
    val context = LocalContext.current
    val player = remember(context) { NotificationSoundPreviewPlayer(context.applicationContext) }
    DisposableEffect(player) {
        onDispose { player.stop() }
    }
    return player
}
