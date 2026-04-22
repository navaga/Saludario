package com.ignaciovalero.saludario.ui.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ignaciovalero.saludario.R
import com.ignaciovalero.saludario.SaludarioApplication
import com.ignaciovalero.saludario.data.local.entity.HealthRecord
import com.ignaciovalero.saludario.data.local.entity.HealthRecordType
import com.ignaciovalero.saludario.domain.repository.HealthRecordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class HealthDetailViewModel(
    private val type: HealthRecordType,
    private val repository: HealthRecordRepository
) : ViewModel() {

    private val formState = MutableStateFlow(
        HealthDetailUiState(unit = defaultUnit(type))
    )

    val uiState: StateFlow<HealthDetailUiState> =
        combine(formState, repository.observeByType(type)) { form, records ->
            form.copy(records = records)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HealthDetailUiState(unit = defaultUnit(type)))

    fun onPrimaryValueChange(value: String) {
        formState.update { it.copy(primaryValue = value, primaryError = null) }
    }

    fun onSecondaryValueChange(value: String) {
        formState.update { it.copy(secondaryValue = value, secondaryError = null) }
    }

    fun onUnitChange(value: String) {
        formState.update { it.copy(unit = value, unitError = null) }
    }

    fun onNotesChange(value: String) {
        formState.update { it.copy(notes = value) }
    }

    fun save() {
        val current = formState.value

        val primary = current.primaryValue.toDoubleOrNull()
        val secondary = current.secondaryValue.toDoubleOrNull()
        val primaryError = if (primary == null || primary <= 0) R.string.health_error_invalid_value else null
        val secondaryError = if (type == HealthRecordType.BLOOD_PRESSURE && (secondary == null || secondary <= 0)) {
            R.string.health_error_invalid_value
        } else {
            null
        }
        val unitError = if (current.unit.isBlank()) R.string.health_error_unit_required else null

        formState.update {
            it.copy(
                primaryError = primaryError,
                secondaryError = secondaryError,
                unitError = unitError
            )
        }

        if (primaryError != null || secondaryError != null || unitError != null) return

        viewModelScope.launch {
            repository.insert(
                HealthRecord(
                    type = type,
                    value = primary!!,
                    secondaryValue = if (type == HealthRecordType.BLOOD_PRESSURE) secondary else null,
                    unit = current.unit.trim(),
                    recordedAt = LocalDateTime.now(),
                    notes = current.notes.trim().ifBlank { null }
                )
            )
            formState.update {
                it.copy(
                    primaryValue = "",
                    secondaryValue = "",
                    notes = "",
                    primaryError = null,
                    secondaryError = null
                )
            }
        }
    }

    fun deleteRecord(record: HealthRecord) {
        viewModelScope.launch {
            repository.delete(record)
        }
    }

    companion object {
        fun factory(type: HealthRecordType): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as SaludarioApplication
                HealthDetailViewModel(
                    type = type,
                    repository = app.container.healthRecordRepository
                )
            }
        }
    }
}

private fun defaultUnit(type: HealthRecordType): String = when (type) {
    HealthRecordType.BLOOD_PRESSURE -> "mmHg"
    HealthRecordType.GLUCOSE -> "mg/dL"
    HealthRecordType.WEIGHT -> "kg"
    HealthRecordType.HEART_RATE -> "bpm"
    HealthRecordType.TEMPERATURE -> "°C"
    HealthRecordType.OXYGEN_SATURATION -> "%"
    HealthRecordType.CUSTOM -> ""
}
