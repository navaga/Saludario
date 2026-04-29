package com.ignaciovalero.saludario.ui.insights

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ignaciovalero.saludario.R
import com.ignaciovalero.saludario.SaludarioApplication
import com.ignaciovalero.saludario.core.logging.ErrorReporter
import com.ignaciovalero.saludario.data.preferences.UserPreferencesDataSource
import com.ignaciovalero.saludario.domain.insights.MedicationInsightsAnalyzer
import com.ignaciovalero.saludario.domain.insights.MedicationInsight
import com.ignaciovalero.saludario.domain.insights.dismissalKey
import com.ignaciovalero.saludario.domain.repository.MedicationLogRepository
import com.ignaciovalero.saludario.domain.repository.MedicationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InsightsViewModel(
    private val medicationRepository: MedicationRepository,
    medicationLogRepository: MedicationLogRepository,
    private val userPreferencesDataSource: UserPreferencesDataSource,
    private val analyzer: MedicationInsightsAnalyzer
) : ViewModel() {

    private val _userMessage = MutableStateFlow<Int?>(null)
    private val _refreshTrigger = MutableStateFlow(0)

    val uiState: StateFlow<InsightsUiState> =
        combine(
            medicationRepository.observeAll(),
            medicationLogRepository.observeAll(),
            userPreferencesDataSource.dismissedInsightKeys(),
            _userMessage,
            _refreshTrigger
        ) { medications, logs, dismissedKeys, message, _ ->
            runCatching {
                analyzer.analyze(medications, logs)
                    .filterNot { dismissedKeys.contains(it.dismissalKey()) }
            }.fold(
                onSuccess = { insights ->
                    InsightsUiState(
                        insights = insights,
                        isLoading = false,
                        userMessage = message,
                        errorMessage = null
                    )
                },
                onFailure = {
                    InsightsUiState(
                        insights = emptyList(),
                        isLoading = false,
                        userMessage = message,
                        errorMessage = R.string.insights_error_message
                    )
                }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsUiState())

    fun addStock(id: Long, amount: Double) {
        viewModelScope.launch {
            try {
                medicationRepository.addStock(id, amount)
                _userMessage.value = R.string.msg_stock_reloaded
            } catch (e: Exception) {
                ErrorReporter.report(TAG, "Error recargando stock id=$id amount=$amount", e)
                _userMessage.value = R.string.msg_error_stock_reload
            }
        }
    }

    fun onMessageShown() {
        _userMessage.value = null
    }

    fun dismissInsight(insight: MedicationInsight) {
        viewModelScope.launch {
            userPreferencesDataSource.setInsightDismissed(insight.dismissalKey())
        }
    }

    fun retry() {
        _refreshTrigger.value += 1
    }

    companion object {
        private const val TAG = "InsightsVM"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as SaludarioApplication
                InsightsViewModel(
                    medicationRepository = app.container.medicationRepository,
                    medicationLogRepository = app.container.medicationLogRepository,
                    userPreferencesDataSource = app.container.userPreferencesDataSource,
                    analyzer = MedicationInsightsAnalyzer(app.applicationContext)
                )
            }
        }
    }
}
