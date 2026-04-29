package com.ignaciovalero.saludario.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ignaciovalero.saludario.data.ads.AdConsentStatus
import com.ignaciovalero.saludario.data.ads.MonetizationConfig
import com.ignaciovalero.saludario.data.notification.MedicationNotificationSound
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class UserPreferencesDataSource(
    private val dataStore: DataStore<Preferences>
) {
    val isSimpleMode: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[SIMPLE_MODE_KEY] ?: false
    }

    val onboardingCompleted: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[ONBOARDING_COMPLETED_KEY] ?: false
    }

    /**
     * Verdadero cuando el usuario ya ha visto el paso de notificaciones del
     * onboarding (haya aceptado o no). Sirve para no volver a mostrar el
     * diálogo global [NotificationPermissionEffect] como modal sorpresa.
     */
    val notificationOnboardingPromptHandled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[NOTIFICATION_ONBOARDING_PROMPT_HANDLED_KEY] ?: false
    }

    val preferredLanguageCode: Flow<String> = dataStore.data.map { prefs ->
        prefs[PREFERRED_LANGUAGE_KEY] ?: DEFAULT_LANGUAGE
    }

    val darkModeEnabled: Flow<Boolean?> = dataStore.data.map { prefs ->
        prefs[DARK_MODE_ENABLED_KEY]
    }

    val adConsentStatus: Flow<AdConsentStatus> = dataStore.data.map { prefs ->
        AdConsentStatus.fromStorage(prefs[AD_CONSENT_STATUS_KEY])
    }

    val adPrivacyOptionsRequired: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[AD_PRIVACY_OPTIONS_REQUIRED_KEY] ?: false
    }

    val graphAdLastShownAtMillis: Flow<Long?> = dataStore.data.map { prefs ->
        prefs[GRAPH_AD_LAST_SHOWN_AT_MILLIS_KEY]
    }

    val graphAdCooldownMinutes: Flow<Int> = dataStore.data.map { prefs ->
        prefs[GRAPH_AD_COOLDOWN_MINUTES_KEY] ?: DEFAULT_GRAPH_AD_COOLDOWN_MINUTES
    }

    val isPremiumNoAds: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PREMIUM_NO_ADS_KEY] ?: false
    }

    /**
     * Sonido seleccionado para los recordatorios de medicación. Si no se ha
     * elegido ninguno, devuelve [MedicationNotificationSound.DEFAULT] (sistema).
     */
    val medicationNotificationSound: Flow<MedicationNotificationSound> = dataStore.data.map { prefs ->
        MedicationNotificationSound.fromStorage(prefs[MEDICATION_NOTIFICATION_SOUND_KEY])
    }

    suspend fun setMedicationNotificationSound(sound: MedicationNotificationSound) {
        dataStore.edit { prefs ->
            prefs[MEDICATION_NOTIFICATION_SOUND_KEY] = sound.storageKey
        }
    }

    suspend fun setSimpleMode(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[SIMPLE_MODE_KEY] = enabled
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { prefs ->
            prefs[ONBOARDING_COMPLETED_KEY] = completed
        }
    }

    suspend fun setNotificationOnboardingPromptHandled(handled: Boolean) {
        dataStore.edit { prefs ->
            prefs[NOTIFICATION_ONBOARDING_PROMPT_HANDLED_KEY] = handled
        }
    }

    /**
     * Marca el onboarding como pendiente para que vuelva a mostrarse al volver
     * a abrir la app. Conserva el idioma preferido para no forzar al usuario a
     * volver a elegirlo.
     */
    suspend fun resetOnboarding() {
        dataStore.edit { prefs ->
            prefs.remove(ONBOARDING_COMPLETED_KEY)
            prefs.remove(NOTIFICATION_ONBOARDING_PROMPT_HANDLED_KEY)
        }
    }

    suspend fun setPreferredLanguageCode(languageCode: String) {
        dataStore.edit { prefs ->
            prefs[PREFERRED_LANGUAGE_KEY] = languageCode
        }
    }

    suspend fun setDarkModeEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[DARK_MODE_ENABLED_KEY] = enabled
        }
    }

    suspend fun setAdConsentStatus(status: AdConsentStatus) {
        dataStore.edit { prefs ->
            prefs[AD_CONSENT_STATUS_KEY] = status.name
        }
    }

    suspend fun setAdPrivacyOptionsRequired(required: Boolean) {
        dataStore.edit { prefs ->
            prefs[AD_PRIVACY_OPTIONS_REQUIRED_KEY] = required
        }
    }

    suspend fun getGraphAdLastShownAtMillis(): Long? {
        val prefs = dataStore.data.first()
        return prefs[GRAPH_AD_LAST_SHOWN_AT_MILLIS_KEY]
    }

    suspend fun setGraphAdLastShownAtMillis(timestampMillis: Long) {
        dataStore.edit { prefs ->
            prefs[GRAPH_AD_LAST_SHOWN_AT_MILLIS_KEY] = timestampMillis
        }
    }

    suspend fun getGraphAdCooldownMinutes(): Int {
        val prefs = dataStore.data.first()
        return prefs[GRAPH_AD_COOLDOWN_MINUTES_KEY] ?: DEFAULT_GRAPH_AD_COOLDOWN_MINUTES
    }

    suspend fun setGraphAdCooldownMinutes(minutes: Int) {
        dataStore.edit { prefs ->
            prefs[GRAPH_AD_COOLDOWN_MINUTES_KEY] = minutes.coerceAtLeast(1)
        }
    }

    suspend fun setPremiumNoAds(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[PREMIUM_NO_ADS_KEY] = enabled
        }
    }

    fun isTutorialSeen(screenKey: String): Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[tutorialKey(screenKey)] ?: false
    }

    suspend fun setTutorialSeen(screenKey: String, seen: Boolean = true) {
        dataStore.edit { prefs ->
            prefs[tutorialKey(screenKey)] = seen
        }
    }

    fun dismissedInsightKeys(): Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs.asMap().keys
            .mapNotNull { key ->
                key.name.takeIf { it.startsWith(DISMISSED_INSIGHT_KEY_PREFIX) }
                    ?.removePrefix(DISMISSED_INSIGHT_KEY_PREFIX)
            }
            .toSet()
    }

    suspend fun setInsightDismissed(insightKey: String, dismissed: Boolean = true) {
        dataStore.edit { prefs ->
            prefs[dismissedInsightKey(insightKey)] = dismissed
        }
    }

    suspend fun clearDismissedInsight(insightKey: String) {
        dataStore.edit { prefs ->
            prefs.remove(dismissedInsightKey(insightKey))
        }
    }

    suspend fun resetDismissedInsights() {
        dataStore.edit { prefs ->
            val dismissedKeys = prefs.asMap().keys
                .filter { it.name.startsWith(DISMISSED_INSIGHT_KEY_PREFIX) }
            dismissedKeys.forEach { key -> prefs.remove(key) }
        }
    }

    suspend fun resetTutorials() {
        dataStore.edit { prefs ->
            val tutorialKeys = prefs.asMap().keys
                .filter { it.name.startsWith(TUTORIAL_KEY_PREFIX) }
            tutorialKeys.forEach { key -> prefs.remove(key) }
        }
    }
    suspend fun getLastLowStockNotifiedValue(medicationId: Long): Double? {
        val prefs = dataStore.data.first()
        return prefs[lowStockNotifiedKey(medicationId)]
    }

    suspend fun setLastLowStockNotifiedValue(medicationId: Long, stockRemaining: Double) {
        dataStore.edit { prefs ->
            prefs[lowStockNotifiedKey(medicationId)] = stockRemaining
        }
    }

    suspend fun clearLastLowStockNotifiedValue(medicationId: Long) {
        dataStore.edit { prefs ->
            prefs.remove(lowStockNotifiedKey(medicationId))
        }
    }

    suspend fun getLastLowStockNotifiedState(medicationId: Long): String? {
        val prefs = dataStore.data.first()
        return prefs[lowStockNotifiedStateKey(medicationId)]
    }

    suspend fun setLastLowStockNotifiedState(medicationId: Long, state: String) {
        dataStore.edit { prefs ->
            prefs[lowStockNotifiedStateKey(medicationId)] = state
        }
    }

    suspend fun clearLastLowStockNotifiedState(medicationId: Long) {
        dataStore.edit { prefs ->
            prefs.remove(lowStockNotifiedStateKey(medicationId))
        }
    }

    /** Timestamp (epoch millis) en el que el usuario descartó el banner de
     *  fiabilidad de recordatorios. `null` si no lo ha descartado nunca. */
    val reminderReliabilityBannerDismissedAtMillis: Flow<Long?> = dataStore.data.map { prefs ->
        prefs[REMINDER_RELIABILITY_BANNER_DISMISSED_AT_KEY]
    }

    suspend fun setReminderReliabilityBannerDismissedAtMillis(timestampMillis: Long) {
        dataStore.edit { prefs ->
            prefs[REMINDER_RELIABILITY_BANNER_DISMISSED_AT_KEY] = timestampMillis
        }
    }

    private companion object {
        val SIMPLE_MODE_KEY = booleanPreferencesKey("simple_mode")
        val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
        val NOTIFICATION_ONBOARDING_PROMPT_HANDLED_KEY =
            booleanPreferencesKey("notification_onboarding_prompt_handled")
        val PREFERRED_LANGUAGE_KEY = stringPreferencesKey("preferred_language")
        val DARK_MODE_ENABLED_KEY = booleanPreferencesKey("dark_mode_enabled")
        val AD_CONSENT_STATUS_KEY = stringPreferencesKey("ad_consent_status")
        val AD_PRIVACY_OPTIONS_REQUIRED_KEY = booleanPreferencesKey("ad_privacy_options_required")
        val GRAPH_AD_LAST_SHOWN_AT_MILLIS_KEY = longPreferencesKey("graph_ad_last_shown_at_millis")
        val GRAPH_AD_COOLDOWN_MINUTES_KEY = intPreferencesKey("graph_ad_cooldown_minutes")
        val PREMIUM_NO_ADS_KEY = booleanPreferencesKey("premium_no_ads")
        val REMINDER_RELIABILITY_BANNER_DISMISSED_AT_KEY =
            longPreferencesKey("reminder_reliability_banner_dismissed_at")
        val MEDICATION_NOTIFICATION_SOUND_KEY =
            stringPreferencesKey("medication_notification_sound")
        const val DEFAULT_LANGUAGE = "es"
        val DEFAULT_GRAPH_AD_COOLDOWN_MINUTES = MonetizationConfig.defaultGraphAdCooldownMinutes
        const val TUTORIAL_KEY_PREFIX = "tutorial_seen_"
        const val DISMISSED_INSIGHT_KEY_PREFIX = "dismissed_insight_"
        const val LOW_STOCK_NOTIFIED_PREFIX = "low_stock_notified_"
        const val LOW_STOCK_NOTIFIED_STATE_PREFIX = "low_stock_notified_state_"

        fun tutorialKey(screenKey: String) = booleanPreferencesKey("$TUTORIAL_KEY_PREFIX$screenKey")
        fun dismissedInsightKey(insightKey: String) =
            booleanPreferencesKey("$DISMISSED_INSIGHT_KEY_PREFIX$insightKey")
        fun lowStockNotifiedKey(medicationId: Long) =
            doublePreferencesKey("$LOW_STOCK_NOTIFIED_PREFIX$medicationId")
        fun lowStockNotifiedStateKey(medicationId: Long) =
            stringPreferencesKey("$LOW_STOCK_NOTIFIED_STATE_PREFIX$medicationId")
    }
}