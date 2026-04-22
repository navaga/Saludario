package com.ignaciovalero.saludario

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.ignaciovalero.saludario.ui.SaludarioApp
import com.ignaciovalero.saludario.ui.theme.SaludarioTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SaludarioTheme {
                SaludarioApp()
            }
        }
    }
}