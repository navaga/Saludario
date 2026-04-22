package com.ignaciovalero.saludario.ui.medications

import androidx.annotation.StringRes

data class MedicationListUiState(
    val medications: List<MedicationItem> = emptyList(),
    @StringRes val userMessage: Int? = null
)

data class MedicationItem(
    val id: Long,
    val name: String,
    val dosage: String,
    val schedule: String,
    val hasQuantityTracking: Boolean,
    val stockLabel: String,
    val stockProgress: Float,
    val isLowStock: Boolean
)
