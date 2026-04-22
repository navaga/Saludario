package com.ignaciovalero.saludario.core.localization

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object AppLanguageManager {
    const val DEFAULT_LANGUAGE_CODE = "es"
    private val supportedLanguageCodes = setOf("es", "en")

    fun applyLanguage(languageCode: String) {
        val normalizedCode = normalizeLanguageCode(languageCode)
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