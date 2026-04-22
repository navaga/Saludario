package com.ignaciovalero.saludario.ui.addmedication

import androidx.annotation.StringRes
import com.ignaciovalero.saludario.R
import com.ignaciovalero.saludario.data.local.entity.MedicationScheduleType
import java.time.DayOfWeek
import java.time.LocalTime

data class AddMedicationUiState(
    val editingId: Long? = null,
    val name: String = "",
    val dosage: String = "",
    val unit: String = "tableta",
    val stockTotal: String = "",
    val stockRemaining: String = "",
    val lowStockThreshold: String = "",
    val scheduleType: MedicationScheduleType = MedicationScheduleType.DAILY,
    val time: String = "",
    val selectedTime: LocalTime? = null,
    val selectedDays: Set<DayOfWeek> = emptySet(),
    val intervalHours: String = "",
    val isSaved: Boolean = false,
    @StringRes val userMessage: Int? = null,
    @StringRes val nameError: Int? = null,
    @StringRes val dosageError: Int? = null,
    @StringRes val stockTotalError: Int? = null,
    @StringRes val stockRemainingError: Int? = null,
    @StringRes val lowStockThresholdError: Int? = null,
    @StringRes val timeError: Int? = null,
    @StringRes val daysError: Int? = null,
    @StringRes val intervalError: Int? = null
) {
    val hasErrors: Boolean
        get() = nameError != null || dosageError != null || stockTotalError != null ||
                stockRemainingError != null || lowStockThresholdError != null ||
                timeError != null || daysError != null || intervalError != null
}

val unitOptions = listOf("tableta", "cápsula", "ml", "gotas", "mg")

@StringRes
fun validateName(name: String): Int? = when {
    name.isBlank() -> R.string.error_name_empty
    else -> null
}

@StringRes
fun validateDosage(dosage: String): Int? = when {
    dosage.isBlank() -> R.string.error_dosage_empty
    dosage.toDoubleOrNull() == null -> R.string.error_dosage_invalid
    dosage.toDouble() <= 0 -> R.string.error_dosage_zero
    else -> null
}

@StringRes
fun validateOptionalNonNegativeDecimal(value: String): Int? = when {
    value.isBlank() -> null
    value.replace(',', '.').toDoubleOrNull() == null -> R.string.error_stock_invalid
    value.replace(',', '.').toDouble() < 0 -> R.string.error_stock_negative
    else -> null
}

@StringRes
fun validateStockRemaining(stockTotal: String, stockRemaining: String): Int? {
    val total = stockTotal.replace(',', '.').toDoubleOrNull() ?: 0.0
    if (stockRemaining.isBlank()) return null
    val remaining = stockRemaining.replace(',', '.').toDoubleOrNull() ?: return R.string.error_stock_invalid
    if (remaining < 0) return R.string.error_stock_negative
    if (remaining > total && total > 0.0) return R.string.error_stock_remaining_gt_total
    return null
}

@StringRes
fun validateLowStockThreshold(stockRemaining: String, lowStockThreshold: String): Int? {
    val remaining = stockRemaining.replace(',', '.').toDoubleOrNull() ?: return null
    if (lowStockThreshold.isBlank()) return null
    val threshold = lowStockThreshold.replace(',', '.').toDoubleOrNull() ?: return R.string.error_stock_invalid
    if (threshold < 0) return R.string.error_stock_negative
    if (threshold > remaining && remaining > 0.0) return R.string.error_low_stock_threshold_gt_remaining
    return null
}

@StringRes
fun validateTime(selectedTime: LocalTime?): Int? = when {
    selectedTime == null -> R.string.error_time_empty
    else -> null
}

@StringRes
fun validateDays(scheduleType: MedicationScheduleType, selectedDays: Set<DayOfWeek>): Int? = when {
    scheduleType == MedicationScheduleType.SPECIFIC_DAYS && selectedDays.isEmpty() -> R.string.error_days_empty
    else -> null
}

@StringRes
fun validateInterval(scheduleType: MedicationScheduleType, intervalHours: String): Int? = when {
    scheduleType != MedicationScheduleType.INTERVAL -> null
    intervalHours.isBlank() -> R.string.error_interval_empty
    intervalHours.toIntOrNull() == null -> R.string.error_interval_invalid
    intervalHours.toInt() !in 1..24 -> R.string.error_interval_range
    else -> null
}
