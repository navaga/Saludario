package com.ignaciovalero.saludario.core.localization

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object AppLanguageManager {
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
     * Idioma cacheado de forma síncrona. Devuelve [DEFAULT_LANGUAGE_CODE] si
     * el usuario aún no ha elegido uno o si la caché no se ha inicializado.
     */
    fun cachedLanguageCode(): String =
        normalizeLanguageCode(cache?.getString(KEY_LANGUAGE, null) ?: DEFAULT_LANGUAGE_CODE)

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