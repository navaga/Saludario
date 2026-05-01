package com.ignaciovalero.saludario.core.localization

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object AppLanguageManager {
    /**
     * Idioma utilizado como último recurso cuando ni el usuario ni el sistema
     * proporcionan uno soportado. No se considera el idioma «por defecto»
     * para el usuario: en una instalación nueva se prefiere siempre el del
     * dispositivo (ver [systemLanguageCode]).
     */
    const val DEFAULT_LANGUAGE_CODE = "es"
    private val supportedLanguageCodes = setOf("es", "en")

    private const val PREFS_NAME = "language_cache"
    private const val KEY_LANGUAGE = "preferred_language_code"

    @Volatile
    private var cache: SharedPreferences? = null

    /**
     * Inicializa la caché síncrona de idioma. Debe llamarse muy pronto en
     * [android.app.Application.onCreate] para poder leer el idioma sin
     * bloquear el hilo principal con DataStore.
     */
    fun init(context: Context) {
        cache = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Idioma cacheado de forma síncrona. Si el usuario aún no ha elegido uno,
     * usa el idioma actual del sistema cuando esté soportado, y como último
     * recurso [DEFAULT_LANGUAGE_CODE].
     */
    fun cachedLanguageCode(): String {
        val stored = cache?.getString(KEY_LANGUAGE, null)
        if (!stored.isNullOrBlank()) return normalizeLanguageCode(stored)
        return systemLanguageCode()
    }

    /**
     * Devuelve el idioma del dispositivo si está soportado por la app,
     * o [DEFAULT_LANGUAGE_CODE] si no lo está.
     */
    fun systemLanguageCode(): String {
        val systemTag = Locale.getDefault().language.lowercase()
        return if (systemTag in supportedLanguageCodes) systemTag else DEFAULT_LANGUAGE_CODE
    }

    fun applyLanguage(languageCode: String) {
        val normalizedCode = normalizeLanguageCode(languageCode)
        cache?.edit()?.putString(KEY_LANGUAGE, normalizedCode)?.apply()
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(normalizedCode))
    }

    fun normalizeLanguageCode(languageCode: String): String {
        return languageCode
            .trim()
            .lowercase()
            .takeIf { it in supportedLanguageCodes }
            ?: DEFAULT_LANGUAGE_CODE
    }
}