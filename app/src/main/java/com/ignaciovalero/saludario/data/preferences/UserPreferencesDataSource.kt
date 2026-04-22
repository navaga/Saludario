package com.ignaciovalero.saludario.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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

    val preferredLanguageCode: Flow<String> = dataStore.data.map { prefs ->
        prefs[PREFERRED_LANGUAGE_KEY] ?: DEFAULT_LANGUAGE
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

    suspend fun setPreferredLanguageCode(languageCode: String) {
        dataStore.edit { prefs ->
            prefs[PREFERRED_LANGUAGE_KEY] = languageCode
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

    private companion object {
        val SIMPLE_MODE_KEY = booleanPreferencesKey("simple_mode")
        val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
        val PREFERRED_LANGUAGE_KEY = stringPreferencesKey("preferred_language")
        const val DEFAULT_LANGUAGE = "es"
        const val TUTORIAL_KEY_PREFIX = "tutorial_seen_"
        const val LOW_STOCK_NOTIFIED_PREFIX = "low_stock_notified_"
        const val LOW_STOCK_NOTIFIED_STATE_PREFIX = "low_stock_notified_state_"

        fun tutorialKey(screenKey: String) = booleanPreferencesKey("$TUTORIAL_KEY_PREFIX$screenKey")
        fun lowStockNotifiedKey(medicationId: Long) =
            doublePreferencesKey("$LOW_STOCK_NOTIFIED_PREFIX$medicationId")
        fun lowStockNotifiedStateKey(medicationId: Long) =
            stringPreferencesKey("$LOW_STOCK_NOTIFIED_STATE_PREFIX$medicationId")
    }
}