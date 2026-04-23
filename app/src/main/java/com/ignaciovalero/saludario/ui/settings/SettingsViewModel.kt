package com.ignaciovalero.saludario.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ignaciovalero.saludario.SaludarioApplication
import com.ignaciovalero.saludario.core.localization.AppLanguageManager
import com.ignaciovalero.saludario.data.ads.AdConsentStatus
import com.ignaciovalero.saludario.data.preferences.UserPreferencesDataSource
import com.ignaciovalero.saludario.R
import com.ignaciovalero.saludario.ui.tutorial.TutorialManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val languageCode: String = AppLanguageManager.DEFAULT_LANGUAGE_CODE,
    val dismissedInsightsCount: Int = 0,
    val darkModeEnabled: Boolean? = null,
    val adConsentStatus: AdConsentStatus = AdConsentStatus.UNKNOWN,
    val adPrivacyOptionsRequired: Boolean = false
)

class SettingsViewModel(
    private val tutorialManager: TutorialManager,
    private val userPreferencesDataSource: UserPreferencesDataSource
) : ViewModel() {

    private val _events = MutableSharedFlow<Int>()
    val events: SharedFlow<Int> = _events.asSharedFlow()
    val uiState: StateFlow<SettingsUiState> = combine(
        userPreferencesDataSource.preferredLanguageCode,
        userPreferencesDataSource.dismissedInsightKeys(),
        userPreferencesDataSource.darkModeEnabled,
        userPreferencesDataSource.adConsentStatus,
        userPreferencesDataSource.adPrivacyOptionsRequired
    ) { languageCode, dismissedInsights, darkModeEnabled, adConsentStatus, adPrivacyOptionsRequired ->
        SettingsUiState(
            languageCode = AppLanguageManager.normalizeLanguageCode(languageCode),
            dismissedInsightsCount = dismissedInsights.size,
            darkModeEnabled = darkModeEnabled,
            adConsentStatus = adConsentStatus,
            adPrivacyOptionsRequired = adPrivacyOptionsRequired
        )
    }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            SettingsUiState()
        )

    fun selectLanguage(languageCode: String) {
        val normalizedCode = AppLanguageManager.normalizeLanguageCode(languageCode)
        viewModelScope.launch {
            userPreferencesDataSource.setPreferredLanguageCode(normalizedCode)
            AppLanguageManager.applyLanguage(normalizedCode)
        }
    }

    fun setDarkModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesDataSource.setDarkModeEnabled(enabled)
        }
    }

    fun resetTutorials() {
        viewModelScope.launch {
            tutorialManager.resetAllTutorials()
            _events.emit(R.string.settings_reset_done)
        }
    }

    fun restoreDismissedInsights() {
        viewModelScope.launch {
            userPreferencesDataSource.resetDismissedInsights()
            _events.emit(R.string.settings_insights_restore_done)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as SaludarioApplication
                SettingsViewModel(
                    tutorialManager = TutorialManager(app.container.userPreferencesDataSource),
                    userPreferencesDataSource = app.container.userPreferencesDataSource
                )
            }
        }
    }
}
