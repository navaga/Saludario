package com.ignaciovalero.saludario.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ignaciovalero.saludario.SaludarioApplication
import com.ignaciovalero.saludario.data.local.entity.MedicationLogEntity
import com.ignaciovalero.saludario.data.local.entity.MedicationStatus
import com.ignaciovalero.saludario.domain.repository.MedicationLogRepository
import com.ignaciovalero.saludario.domain.repository.MedicationRepository
import com.ignaciovalero.saludario.domain.scheduling.ScheduledDoseGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

class TodayViewModel(
    private val medicationRepository: MedicationRepository,
    private val medicationLogRepository: MedicationLogRepository,
    private val clock: Clock
) : ViewModel() {

    private val _currentDate = MutableStateFlow(nowDate())
    val currentDate: StateFlow<LocalDate> = _currentDate.asStateFlow()

    val uiState: StateFlow<TodayUiState> =
        combine(
            _currentDate,
            medicationRepository.observeAll(),
            medicationLogRepository.observeAll()
        ) { selectedDate, medications, logs ->
            val today = nowDate()
            val now = nowDateTime()
            val items = ScheduledDoseGenerator(
                medications = medications,
                logs = logs,
                now = now
            ).generateDosesForDate(selectedDate)

            TodayUiState(
                selectedDate = selectedDate,
                scheduledItems = items,
                canModifyIntake = selectedDate == today
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    fun previousDay() {
        _currentDate.value = _currentDate.value.minusDays(1)
    }

    fun nextDay() {
        _currentDate.value = _currentDate.value.plusDays(1)
    }

    fun setDate(date: LocalDate) {
        _currentDate.value = date
    }

    fun goToToday() {
        _currentDate.value = nowDate()
    }

    fun toggleTaken(medicationId: Long, time: String) {
        viewModelScope.launch {
            val today = nowDate()
            val selectedDate = _currentDate.value
            if (selectedDate != today) return@launch

            val scheduledDateTime = LocalDateTime.of(selectedDate, java.time.LocalTime.parse(time))

            val currentItems = uiState.value.scheduledItems
            val item = currentItems.find { it.medicationId == medicationId && it.time == time } ?: return@launch

            if (item.logId != null) {
                val log = medicationLogRepository.getById(item.logId) ?: return@launch
                val newStatus = if (log.status == MedicationStatus.TAKEN) MedicationStatus.PENDING else MedicationStatus.TAKEN
                val newTakenTime = if (newStatus == MedicationStatus.TAKEN) nowDateTime() else null
                medicationLogRepository.update(log.copy(status = newStatus, takenTime = newTakenTime))
                if (newStatus == MedicationStatus.TAKEN && log.status != MedicationStatus.TAKEN) {
                    medicationRepository.decreaseStockForTakenDose(medicationId)
                }
            } else {
                medicationLogRepository.insert(
                    MedicationLogEntity(
                        medicationId = medicationId,
                        scheduledTime = scheduledDateTime,
                        takenTime = nowDateTime(),
                        status = MedicationStatus.TAKEN
                    )
                )
                medicationRepository.decreaseStockForTakenDose(medicationId)
            }
        }
    }

    private fun nowDate(): LocalDate = LocalDate.now(clock)

    private fun nowDateTime(): LocalDateTime = LocalDateTime.now(clock)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as SaludarioApplication
                TodayViewModel(
                    app.container.medicationRepository,
                    app.container.medicationLogRepository,
                    Clock.systemDefaultZone()
                )
            }
        }
    }
}
