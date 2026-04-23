package com.ignaciovalero.saludario.ui.addmedication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ignaciovalero.saludario.SaludarioApplication
import com.ignaciovalero.saludario.data.local.entity.MedicationEntity
import com.ignaciovalero.saludario.data.local.entity.MedicationScheduleType
import com.ignaciovalero.saludario.data.work.AppWorkScheduler
import com.ignaciovalero.saludario.domain.repository.MedicationRepository
import com.ignaciovalero.saludario.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class AddMedicationViewModel(
    private val medicationRepository: MedicationRepository,
    private val workScheduler: AppWorkScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddMedicationUiState())
    val uiState: StateFlow<AddMedicationUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, nameError = null) }
    }

    fun onDosageChange(value: String) {
        _uiState.update { it.copy(dosage = value, dosageError = null) }
    }

    fun onUnitChange(value: String) {
        _uiState.update { it.copy(unit = value) }
    }

    fun onStockTotalChange(value: String) {
        _uiState.update {
            it.copy(
                stockTotal = value,
                stockTotalError = null,
                stockRemainingError = null
            )
        }
    }

    fun onStockRemainingChange(value: String) {
        _uiState.update {
            it.copy(
                stockRemaining = value,
                stockRemainingError = null,
                lowStockThresholdError = null
            )
        }
    }

    fun onLowStockThresholdChange(value: String) {
        _uiState.update { it.copy(lowStockThreshold = value, lowStockThresholdError = null) }
    }

    fun clearStockFields() {
        _uiState.update {
            it.copy(
                stockTotal = "",
                stockRemaining = "",
                lowStockThreshold = "",
                stockTotalError = null,
                stockRemainingError = null,
                lowStockThresholdError = null
            )
        }
    }

    fun onScheduleTypeChange(value: MedicationScheduleType) {
        _uiState.update { it.copy(
            scheduleType = value,
            daysError = null,
            intervalError = null
        ) }
    }

    fun onTimeSelected(time: LocalTime) {
        _uiState.update { it.copy(
            selectedTime = time,
            time = "%02d:%02d".format(time.hour, time.minute),
            timeError = null
        ) }
    }

    fun onDayToggle(day: DayOfWeek) {
        _uiState.update {
            val current = it.selectedDays.toMutableSet()
            if (current.contains(day)) current.remove(day) else current.add(day)
            it.copy(selectedDays = current, daysError = null)
        }
    }

    fun onIntervalHoursChange(value: String) {
        _uiState.update { it.copy(intervalHours = value, intervalError = null) }
    }

    fun loadMedication(id: Long) {
        // Only load once — avoid re-loading on recomposition
        if (_uiState.value.editingId == id) return
        viewModelScope.launch {
            try {
                val medication = medicationRepository.getById(id) ?: return@launch
                val hasQuantityTracking = medication.stockTotal > 0.0 ||
                    medication.stockRemaining > 0.0 ||
                    medication.lowStockThreshold > 0.0
                _uiState.value = AddMedicationUiState(
                    editingId = medication.id,
                    name = medication.name,
                    dosage = medication.dosage.toString(),
                    unit = medication.unit,
                    stockTotal = if (hasQuantityTracking) medication.stockTotal.toEditableNumber() else "",
                    stockRemaining = if (hasQuantityTracking) medication.stockRemaining.toEditableNumber() else "",
                    lowStockThreshold = if (hasQuantityTracking) medication.lowStockThreshold.toEditableNumber() else "",
                    scheduleType = medication.scheduleType,
                    selectedTime = medication.times.firstOrNull(),
                    time = medication.times.firstOrNull()
                        ?.let { "%02d:%02d".format(it.hour, it.minute) } ?: "",
                    startDate = medication.startDate,
                    endDate = medication.endDate,
                    selectedDays = medication.specificDays?.toSet() ?: emptySet(),
                    intervalHours = medication.intervalHours?.toString() ?: ""
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = R.string.msg_error_load) }
            }
        }
    }

    fun save() {
        val current = _uiState.value
        val nameError = validateName(current.name)
        val dosageError = validateDosage(current.dosage)
        val stockTotalError = validateOptionalNonNegativeDecimal(current.stockTotal)
        val stockRemainingError = validateStockRemaining(current.stockTotal, current.stockRemaining)
        val lowStockThresholdError = validateLowStockThreshold(
            if (current.stockRemaining.isBlank()) current.stockTotal else current.stockRemaining,
            current.lowStockThreshold
        )
        val timeError = validateTime(current.selectedTime)
        val daysError = validateDays(current.scheduleType, current.selectedDays)
        val intervalError = validateInterval(current.scheduleType, current.intervalHours)

        _uiState.update { it.copy(
            nameError = nameError,
            dosageError = dosageError,
            stockTotalError = stockTotalError,
            stockRemainingError = stockRemainingError,
            lowStockThresholdError = lowStockThresholdError,
            timeError = timeError,
            daysError = daysError,
            intervalError = intervalError
        ) }
        if (_uiState.value.hasErrors) return

        val dosageValue = current.dosage.toDouble()
        val stockTotalValue = current.stockTotal.parseDecimalOrZero()
        val stockRemainingValue = if (current.stockRemaining.isBlank()) {
            stockTotalValue
        } else {
            current.stockRemaining.parseDecimalOrZero()
        }
        val lowStockThresholdValue = current.lowStockThreshold.parseDecimalOrZero()
        val baseTime = current.selectedTime!!
        val scheduleType = current.scheduleType

        val times = if (scheduleType == MedicationScheduleType.INTERVAL) {
            computeIntervalTimes(baseTime, current.intervalHours.toInt())
        } else {
            listOf(baseTime)
        }

        val specificDays = if (scheduleType == MedicationScheduleType.SPECIFIC_DAYS) {
            current.selectedDays.toList()
        } else {
            null
        }

        val intervalHours = if (scheduleType == MedicationScheduleType.INTERVAL) {
            current.intervalHours.toInt()
        } else {
            null
        }

        viewModelScope.launch {
            try {
                val editingId = current.editingId
                if (editingId != null) {
                    val entity = MedicationEntity(
                        id = editingId,
                        name = current.name.trim(),
                        dosage = dosageValue,
                        unit = current.unit,
                        scheduleType = scheduleType,
                        times = times,
                        startDate = current.startDate,
                        endDate = current.endDate,
                        specificDays = specificDays,
                        intervalHours = intervalHours,
                        stockTotal = stockTotalValue,
                        stockRemaining = stockRemainingValue,
                        lowStockThreshold = lowStockThresholdValue
                    )
                    medicationRepository.update(entity)
                    workScheduler.cancelMedicationReminders(editingId)
                    workScheduler.scheduleMedicationReminders(
                        medicationId = editingId,
                        times = times
                    )
                } else {
                    val id = medicationRepository.insert(
                        MedicationEntity(
                            name = current.name.trim(),
                            dosage = dosageValue,
                            unit = current.unit,
                            scheduleType = scheduleType,
                            times = times,
                            startDate = current.startDate,
                            endDate = current.endDate,
                            specificDays = specificDays,
                            intervalHours = intervalHours,
                            stockTotal = stockTotalValue,
                            stockRemaining = stockRemainingValue,
                            lowStockThreshold = lowStockThresholdValue
                        )
                    )
                    workScheduler.scheduleMedicationReminders(
                        medicationId = id,
                        times = times
                    )
                }
                _uiState.update { it.copy(isSaved = true) }
            } catch (e: Exception) {
                val msgRes = if (current.editingId != null) R.string.msg_error_update
                             else R.string.msg_error_save
                _uiState.update { it.copy(userMessage = msgRes) }
            }
        }
    }

    private fun computeIntervalTimes(start: LocalTime, intervalH: Int): List<LocalTime> {
        val result = mutableListOf(start)
        var totalHours = intervalH
        while (totalHours < 24) {
            result.add(start.plusHours(totalHours.toLong()))
            totalHours += intervalH
        }
        return result
    }

    private fun String.parseDecimalOrZero(): Double = replace(',', '.').toDoubleOrNull() ?: 0.0

    private fun Double.toEditableNumber(): String {
        return if (this % 1.0 == 0.0) toInt().toString() else toString().trimEnd('0').trimEnd('.')
    }

    fun onMessageShown() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun resetForm() {
        _uiState.value = AddMedicationUiState()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as SaludarioApplication
                AddMedicationViewModel(
                    app.container.medicationRepository,
                    app.container.workScheduler
                )
            }
        }
    }
}
