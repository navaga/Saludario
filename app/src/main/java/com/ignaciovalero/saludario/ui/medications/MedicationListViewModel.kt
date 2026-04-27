package com.ignaciovalero.saludario.ui.medications

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ignaciovalero.saludario.R
import com.ignaciovalero.saludario.SaludarioApplication
import com.ignaciovalero.saludario.core.localization.localizedLocalTime
import com.ignaciovalero.saludario.core.localization.localizedMedicationDosage
import com.ignaciovalero.saludario.data.local.entity.MedicationScheduleType
import com.ignaciovalero.saludario.data.work.AppWorkScheduler
import com.ignaciovalero.saludario.domain.repository.MedicationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalTime

class MedicationListViewModel(
    private val medicationRepository: MedicationRepository,
    private val workScheduler: AppWorkScheduler,
    private val context: Context
) : ViewModel() {

    private val _userMessage = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<MedicationListUiState> =
        combine(
            medicationRepository.observeAll(),
            _userMessage
        ) { entities, message ->
            MedicationListUiState(
                medications = entities.map { entity ->
                    val hasQuantityTracking = entity.stockTotal > 0.0 ||
                        entity.stockRemaining > 0.0 ||
                        entity.lowStockThreshold > 0.0
                    val isLowStock = hasQuantityTracking && (
                        entity.stockRemaining <= entity.lowStockThreshold ||
                            (entity.stockTotal > 0.0 && (entity.stockRemaining / entity.stockTotal) <= 0.15)
                        )
                    MedicationItem(
                        id = entity.id,
                        name = entity.name,
                        dosage = context.localizedMedicationDosage(entity.dosage, entity.unit),
                        schedule = formatSchedule(entity.scheduleType, entity.times),
                        hasQuantityTracking = hasQuantityTracking,
                        stockLabel = context.getString(
                            R.string.medications_stock_remaining,
                            formatQuantity(entity.stockRemaining),
                            formatQuantity(entity.stockTotal)
                        ),
                        stockProgress = if (hasQuantityTracking && entity.stockTotal > 0.0) {
                            (entity.stockRemaining / entity.stockTotal).toFloat().coerceIn(0f, 1f)
                        } else {
                            0f
                        },
                        isLowStock = isLowStock
                    )
                },
                userMessage = message
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MedicationListUiState())

    fun deleteMedication(id: Long) {
        viewModelScope.launch {
            try {
                medicationRepository.getById(id)?.let { medication ->
                    medicationRepository.delete(medication)
                    workScheduler.cancelMedicationReminders(id)
                    _userMessage.value = R.string.msg_medication_deleted
                }
            } catch (e: Exception) {
                _userMessage.value = R.string.msg_error_delete
            }
        }
    }

    fun addStock(id: Long, amount: Double) {
        viewModelScope.launch {
            try {
                medicationRepository.addStock(id, amount)
                _userMessage.value = R.string.msg_stock_reloaded
            } catch (e: Exception) {
                _userMessage.value = R.string.msg_error_stock_reload
            }
        }
    }

    fun onMessageShown() {
        _userMessage.value = null
    }

    private fun formatSchedule(type: MedicationScheduleType, times: List<LocalTime>): String {
        val label = when (type) {
            MedicationScheduleType.DAILY -> context.getString(R.string.schedule_daily)
            MedicationScheduleType.SPECIFIC_DAYS -> context.getString(R.string.schedule_specific_days)
            MedicationScheduleType.INTERVAL -> context.getString(R.string.schedule_interval)
        }
        return context.getString(
            R.string.schedule_format,
            label,
            times.joinToString(", ") { context.localizedLocalTime(it) }
        )
    }

    private fun formatQuantity(value: Double): String {
        // Usa el locale activo para que el separador decimal coincida con el
        // que el usuario usa en el resto de la app (coma en es-ES, punto en en-US).
        val locale = context.resources.configuration.locales[0]
        val formatter = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = MAX_QUANTITY_DECIMALS
            isGroupingUsed = false
        }
        return formatter.format(value)
    }

    companion object {
        private const val MAX_QUANTITY_DECIMALS = 2

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as SaludarioApplication
                MedicationListViewModel(
                    app.container.medicationRepository,
                    app.container.workScheduler,
                    app.applicationContext
                )
            }
        }
    }
}
