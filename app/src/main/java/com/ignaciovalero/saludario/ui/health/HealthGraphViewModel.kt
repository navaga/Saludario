package com.ignaciovalero.saludario.ui.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ignaciovalero.saludario.SaludarioApplication
import com.ignaciovalero.saludario.data.local.entity.HealthRecordType
import com.ignaciovalero.saludario.domain.repository.HealthRecordRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class HealthGraphViewModel(
    type: HealthRecordType,
    repository: HealthRecordRepository,
    accessPolicy: HealthGraphAccessPolicy
) : ViewModel() {

    val uiState: StateFlow<HealthGraphUiState> = combine(
        repository.observeByType(type),
        accessPolicy.canAccessGraph(type)
    ) { records, canAccess ->
        HealthGraphUiState(
            records = records,
            isPremiumLocked = !canAccess
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HealthGraphUiState()
    )

    companion object {
        fun factory(type: HealthRecordType): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as SaludarioApplication
                HealthGraphViewModel(
                    type = type,
                    repository = app.container.healthRecordRepository,
                    accessPolicy = HealthGraphAccessPolicy()
                )
            }
        }
    }
}
