package com.ignaciovalero.saludario.ui.health

import com.ignaciovalero.saludario.data.local.entity.HealthRecord

data class HealthGraphUiState(
    val records: List<HealthRecord> = emptyList(),
    val isPremiumLocked: Boolean = false
)
