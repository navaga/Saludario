package com.ignaciovalero.saludario

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
import com.ignaciovalero.saludario.ui.theme.SaludarioTheme
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
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
                SaludarioApp()
            }
        }
    }
}