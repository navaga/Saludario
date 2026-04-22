package com.ignaciovalero.saludario.ui.insights

import com.ignaciovalero.saludario.domain.insights.MedicationInsight

data class InsightsUiState(
    val insights: List<MedicationInsight> = emptyList(),
    val isLoading: Boolean = true
)
