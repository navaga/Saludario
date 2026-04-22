package com.ignaciovalero.saludario.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ignaciovalero.saludario.SaludarioApplication
import com.ignaciovalero.saludario.domain.insights.MedicationInsightsAnalyzer
import com.ignaciovalero.saludario.domain.repository.MedicationLogRepository
import com.ignaciovalero.saludario.domain.repository.MedicationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class InsightsViewModel(
    medicationRepository: MedicationRepository,
    medicationLogRepository: MedicationLogRepository,
    private val analyzer: MedicationInsightsAnalyzer
) : ViewModel() {

    val uiState: StateFlow<InsightsUiState> =
        combine(
            medicationRepository.observeAll(),
            medicationLogRepository.observeAll()
        ) { medications, logs ->
            InsightsUiState(
                insights = analyzer.analyze(medications, logs),
                isLoading = false
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsUiState())

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as SaludarioApplication
                InsightsViewModel(
                    medicationRepository = app.container.medicationRepository,
                    medicationLogRepository = app.container.medicationLogRepository,
                    analyzer = MedicationInsightsAnalyzer(app.applicationContext)
                )
            }
        }
    }
}
