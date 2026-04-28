package com.ignaciovalero.saludario

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import com.ignaciovalero.saludario.ui.SaludarioApp
import com.ignaciovalero.saludario.ui.notification.MedicationNotificationTarget
import com.ignaciovalero.saludario.ui.theme.SaludarioTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    // Buffer 1 para no perder el evento si la activity aún no tiene suscriptores
    // (caso "app cerrada" → onCreate emite antes de que SaludarioApp colecte).
    private val _notificationTargets = MutableSharedFlow<MedicationNotificationTarget>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val notificationTargets = _notificationTargets.asSharedFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as SaludarioApplication
        lifecycleScope.launch {
            app.container.monetizationManager.refreshConsent(this@MainActivity)
        }
        setContent {
            val app = LocalContext.current.applicationContext as SaludarioApplication
            val darkModeEnabled by app.container.userPreferencesDataSource.darkModeEnabled
                .collectAsState(initial = null)
            val useDarkTheme = darkModeEnabled ?: isSystemInDarkTheme()

            SaludarioTheme(darkTheme = useDarkTheme) {
                SaludarioApp(notificationTargets = notificationTargets)
            }
        }
        // Procesa el intent que abrió la activity (caso app cerrada).
        consumeNotificationIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Mantén el último intent entregado a `getIntent()` para coherencia
        // con cualquier consumidor que lo lea más tarde.
        setIntent(intent)
        consumeNotificationIntent(intent)
    }

    private fun consumeNotificationIntent(intent: Intent?) {
        val target = MedicationNotificationTarget.fromIntent(intent) ?: return
        // tryEmit por simplicidad: hay buffer 1, así que el evento sobrevive
        // hasta que el primer suscriptor lo recoja.
        _notificationTargets.tryEmit(target)
    }
}