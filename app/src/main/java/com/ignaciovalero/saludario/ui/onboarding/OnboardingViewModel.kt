package com.ignaciovalero.saludario.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ignaciovalero.saludario.SaludarioApplication
import com.ignaciovalero.saludario.core.localization.AppLanguageManager
import com.ignaciovalero.saludario.data.preferences.UserPreferencesDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val page: Int = 0,
    val languageCode: String = "",
    val acceptedDisclaimer: Boolean = false
)

class OnboardingViewModel(
    private val userPreferencesDataSource: UserPreferencesDataSource
) : ViewModel() {

    /** null = cargando, false = no completado, true = completado */
    val onboardingCompleted: StateFlow<Boolean?> = userPreferencesDataSource.onboardingCompleted
        .map<Boolean, Boolean?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val localState = MutableStateFlow(OnboardingUiState())

    val uiState: StateFlow<OnboardingUiState> = combine(
        localState,
        userPreferencesDataSource.preferredLanguageCode
    ) { local, savedLanguage ->
        local.copy(languageCode = local.languageCode.ifBlank { savedLanguage })
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        OnboardingUiState(languageCode = AppLanguageManager.DEFAULT_LANGUAGE_CODE)
    )

    fun selectLanguage(languageCode: String) {
        val normalizedCode = AppLanguageManager.normalizeLanguageCode(languageCode)
        localState.update { it.copy(languageCode = normalizedCode) }
        AppLanguageManager.applyLanguage(normalizedCode)
        viewModelScope.launch {
            userPreferencesDataSource.setPreferredLanguageCode(normalizedCode)
        }
    }

    fun nextPage() {
        localState.update { it.copy(page = (it.page + 1).coerceAtMost(1)) }
    }

    fun previousPage() {
        localState.update { it.copy(page = (it.page - 1).coerceAtLeast(0)) }
    }

    fun setPage(page: Int) {
        localState.update { it.copy(page = page.coerceIn(0, 1)) }
    }

    fun setAcceptedDisclaimer(accepted: Boolean) {
        localState.update { it.copy(acceptedDisclaimer = accepted) }
    }

    fun completeOnboarding() {
        val current = localState.value
        if (!current.acceptedDisclaimer) return

        viewModelScope.launch {
            userPreferencesDataSource.setOnboardingCompleted(true)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as SaludarioApplication
                OnboardingViewModel(app.container.userPreferencesDataSource)
            }
        }
    }
}
