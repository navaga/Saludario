package com.ignaciovalero.saludario.ui.health

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ignaciovalero.saludario.R
import com.ignaciovalero.saludario.SaludarioApplication
import com.ignaciovalero.saludario.core.formatting.formatHealthValuePlain
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
        formState.update { it.copy(primaryValue = value, primaryError = null, primaryErrorArgs = null) }
    }

    fun onSecondaryValueChange(value: String) {
        formState.update { it.copy(secondaryValue = value, secondaryError = null, secondaryErrorArgs = null) }
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
        val unitTrimmed = current.unit.trim()

        var primaryError: Int? = if (primary == null || primary <= 0) R.string.health_error_invalid_value else null
        var secondaryError: Int? = if (type == HealthRecordType.BLOOD_PRESSURE && (secondary == null || secondary <= 0)) {
            R.string.health_error_invalid_value
        } else {
            null
        }
        val unitError = if (unitTrimmed.isBlank()) R.string.health_error_unit_required else null

        var primaryErrorArgs: List<String>? = null
        var secondaryErrorArgs: List<String>? = null

        if (primaryError == null && primary != null) {
            val range = plausibleRange(type, unitTrimmed, primary = true)
            if (range != null && (primary < range.first || primary > range.second)) {
                primaryError = R.string.health_error_value_out_of_range
                primaryErrorArgs = listOf(range.first.formatHealthValuePlain(), range.second.formatHealthValuePlain())
            }
        }
        if (type == HealthRecordType.BLOOD_PRESSURE && secondaryError == null && secondary != null) {
            val range = plausibleRange(type, unitTrimmed, primary = false)
            if (range != null && (secondary < range.first || secondary > range.second)) {
                secondaryError = R.string.health_error_value_out_of_range
                secondaryErrorArgs = listOf(range.first.formatHealthValuePlain(), range.second.formatHealthValuePlain())
            }
        }
        if (type == HealthRecordType.OXYGEN_SATURATION && primaryError == null && primary != null && primary > 100) {
            primaryError = R.string.health_error_oxygen_max_100
            primaryErrorArgs = null
        }
        if (type == HealthRecordType.BLOOD_PRESSURE &&
            primaryError == null && secondaryError == null &&
            primary != null && secondary != null && primary <= secondary
        ) {
            primaryError = R.string.health_error_systolic_must_exceed_diastolic
            primaryErrorArgs = null
        }

        formState.update {
            it.copy(
                primaryError = primaryError,
                primaryErrorArgs = primaryErrorArgs,
                secondaryError = secondaryError,
                secondaryErrorArgs = secondaryErrorArgs,
                unitError = unitError
            )
        }

        if (primaryError != null || secondaryError != null || unitError != null) return

        viewModelScope.launch {
            try {
                repository.insert(
                    HealthRecord(
                        type = type,
                        value = requireNotNull(primary),
                        secondaryValue = if (type == HealthRecordType.BLOOD_PRESSURE) secondary else null,
                        unit = unitTrimmed,
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
                        primaryErrorArgs = null,
                        secondaryError = null,
                        secondaryErrorArgs = null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error guardando registro de salud", e)
            }
        }
    }

    fun deleteRecord(record: HealthRecord) {
        viewModelScope.launch {
            try {
                repository.delete(record)
            } catch (e: Exception) {
                Log.e(TAG, "Error eliminando registro de salud", e)
            }
        }
    }

    companion object {
        private const val TAG = "HealthDetailVM"

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

private fun plausibleRange(
    type: HealthRecordType,
    unit: String,
    primary: Boolean
): Pair<Double, Double>? {
    val u = unit.trim().lowercase()
    return when (type) {
        HealthRecordType.BLOOD_PRESSURE -> if (primary) 60.0 to 260.0 else 30.0 to 160.0
        HealthRecordType.HEART_RATE -> 30.0 to 250.0
        HealthRecordType.OXYGEN_SATURATION -> 50.0 to 100.0
        HealthRecordType.TEMPERATURE -> when {
            u.contains("f") -> 86.0 to 113.0
            else -> 30.0 to 45.0
        }
        HealthRecordType.GLUCOSE -> when {
            u.contains("mmol") -> 2.0 to 35.0
            else -> 30.0 to 700.0
        }
        HealthRecordType.WEIGHT -> when {
            u.contains("lb") -> 4.0 to 1100.0
            else -> 2.0 to 500.0
        }
        HealthRecordType.CUSTOM -> null
    }
}
