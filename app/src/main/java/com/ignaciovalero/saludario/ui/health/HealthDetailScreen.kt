package com.ignaciovalero.saludario.ui.health

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ignaciovalero.saludario.R
import com.ignaciovalero.saludario.data.local.entity.HealthRecord
import com.ignaciovalero.saludario.data.local.entity.HealthRecordType
import com.ignaciovalero.saludario.ui.theme.AppSpacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
fun HealthDetailScreen(
    uiState: HealthDetailUiState,
    type: HealthRecordType,
    onBack: () -> Unit,
    onOpenGraph: () -> Unit,
    onDeleteRecord: (HealthRecord) -> Unit,
    onPrimaryValueChange: (String) -> Unit,
    onSecondaryValueChange: (String) -> Unit,
    onUnitChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val unitConfig = type.unitConfig()
    var unitMenuExpanded by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<HealthRecord?>(null) }
    var selectedHistoryDate by remember { mutableStateOf<LocalDate?>(null) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val locale = LocalConfiguration.current.locales[0]
    val filteredRecords = remember(uiState.records, selectedHistoryDate) {
        uiState.records.filter { record ->
            selectedHistoryDate == null || record.recordedAt.toLocalDate() == selectedHistoryDate
        }
    }
    val groupedRecords = filteredRecords.groupBy { it.recordedAt.toLocalDate() }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(text = stringResource(R.string.health_detail_title, type.displayName())) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back_button_cd)
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentPadding = PaddingValues(AppSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            item(key = "health-detail-disclaimer") {
                HealthDisclaimerBanner()
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppSpacing.lg),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                    ) {
                        Text(
                            text = stringResource(R.string.health_add_record_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        OutlinedTextField(
                            value = uiState.primaryValue,
                            onValueChange = onPrimaryValueChange,
                            label = {
                                Text(
                                    if (type == HealthRecordType.BLOOD_PRESSURE) {
                                        stringResource(R.string.health_label_systolic)
                                    } else {
                                        stringResource(R.string.health_label_value)
                                    }
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = uiState.primaryError != null,
                            supportingText = uiState.primaryError?.let { errorRes ->
                                {
                                    val args = uiState.primaryErrorArgs
                                    val text = if (args != null) {
                                        stringResource(errorRes, *args.toTypedArray())
                                    } else {
                                        stringResource(errorRes)
                                    }
                                    Text(text, color = MaterialTheme.colorScheme.error)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (type == HealthRecordType.BLOOD_PRESSURE) {
                            OutlinedTextField(
                                value = uiState.secondaryValue,
                                onValueChange = onSecondaryValueChange,
                                label = { Text(stringResource(R.string.health_label_diastolic)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                isError = uiState.secondaryError != null,
                                supportingText = uiState.secondaryError?.let { errorRes ->
                                    {
                                        val args = uiState.secondaryErrorArgs
                                        val text = if (args != null) {
                                            stringResource(errorRes, *args.toTypedArray())
                                        } else {
                                            stringResource(errorRes)
                                        }
                                        Text(text, color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        when (unitConfig.mode) {
                            HealthUnitMode.FIXED -> {
                                OutlinedTextField(
                                    value = uiState.unit,
                                    onValueChange = {},
                                    label = { Text(stringResource(R.string.health_label_unit)) },
                                    singleLine = true,
                                    readOnly = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            HealthUnitMode.OPTIONS -> {
                                ExposedDropdownMenuBox(
                                    expanded = unitMenuExpanded,
                                    onExpandedChange = { unitMenuExpanded = !unitMenuExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = uiState.unit,
                                        onValueChange = {},
                                        readOnly = true,
                                        singleLine = true,
                                        label = { Text(stringResource(R.string.health_label_unit)) },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitMenuExpanded)
                                        },
                                        modifier = Modifier
                                            .menuAnchor(
                                                type = MenuAnchorType.PrimaryNotEditable,
                                                enabled = true
                                            )
                                            .fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = unitMenuExpanded,
                                        onDismissRequest = { unitMenuExpanded = false }
                                    ) {
                                        unitConfig.options.forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text(option) },
                                                onClick = {
                                                    onUnitChange(option)
                                                    unitMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            HealthUnitMode.FREE -> {
                                OutlinedTextField(
                                    value = uiState.unit,
                                    onValueChange = onUnitChange,
                                    label = { Text(stringResource(R.string.health_label_unit)) },
                                    singleLine = true,
                                    isError = uiState.unitError != null,
                                    supportingText = uiState.unitError?.let { errorRes ->
                                        { Text(stringResource(errorRes), color = MaterialTheme.colorScheme.error) }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        OutlinedTextField(
                            value = uiState.notes,
                            onValueChange = onNotesChange,
                            label = { Text(stringResource(R.string.health_label_notes_optional)) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 3
                        )

                        Button(
                            onClick = {
                                focusManager.clearFocus(force = true)
                                keyboardController?.hide()
                                onSave()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = stringResource(R.string.health_save_button))
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.health_previous_records_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // TODO [PREMIUM] Vista rápida últimos 10 días — desactivada hasta versión premium
            // item {
            //     QuickLastTenDaysSection(
            //         type = type,
            //         records = uiState.records
            //     )
            // }

            item {
                HealthDateFilterBar(
                    selectedDate = selectedHistoryDate,
                    onSelectDate = { selectedHistoryDate = it },
                    onClearDate = { selectedHistoryDate = null }
                )
            }

            item {
                Button(
                    onClick = onOpenGraph,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.records.size >= 2
                ) {
                    Text(text = stringResource(R.string.health_view_chart_button))
                }
            }

            if (filteredRecords.isEmpty()) {
                item {
                    Text(
                        text = if (selectedHistoryDate == null) {
                            stringResource(R.string.health_previous_records_empty)
                        } else {
                            stringResource(R.string.health_previous_records_empty_filtered)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                groupedRecords.forEach { (date, dayRecords) ->
                    stickyHeader(key = "day_header_$date") {
                        DayHeader(label = dayLabel(date, locale))
                    }
                    items(dayRecords, key = { it.id }) { record ->
                        HealthRecordItem(
                            record = record,
                            onDeleteClick = { pendingDelete = record }
                        )
                    }
                }
            }
        }

        if (pendingDelete != null) {
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = { Text(text = stringResource(R.string.health_delete_confirm_title)) },
                text = { Text(text = stringResource(R.string.health_delete_confirm_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            pendingDelete?.let(onDeleteRecord)
                            pendingDelete = null
                        }
                    ) {
                        Text(text = stringResource(R.string.health_delete_confirm_action))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDelete = null }) {
                        Text(text = stringResource(R.string.time_picker_cancel))
                    }
                }
            )
        }
    }
}

private data class DailyHealthSummary(
    val date: LocalDate,
    val primaryAvg: Double,
    val secondaryAvg: Double?
)

private enum class TrendDirection { UP, DOWN, STABLE }

@Composable
private fun QuickLastTenDaysSection(
    type: HealthRecordType,
    records: List<HealthRecord>
) {
    val locale = LocalConfiguration.current.locales[0]
    val byDate = records.groupBy { it.recordedAt.toLocalDate() }
    val lastTenDates = byDate.keys
        .sortedDescending()
        .take(10)
        .sorted()

    if (lastTenDates.isEmpty()) return

    val summaries = lastTenDates
        .sortedDescending()
        .mapNotNull { date ->
            val dayRecords = byDate[date].orEmpty()
            if (dayRecords.isEmpty()) return@mapNotNull null
            val primaryAvg = dayRecords.map { it.value }.average()
            val secondaryAvg = dayRecords.mapNotNull { it.secondaryValue }.takeIf { it.isNotEmpty() }?.average()
            DailyHealthSummary(
                date = date,
                primaryAvg = primaryAvg,
                secondaryAvg = secondaryAvg
            )
        }

    val first = summaries.lastOrNull()?.primaryAvg
    val last = summaries.firstOrNull()?.primaryAvg
    val trendDirection = when {
        first == null || last == null -> TrendDirection.STABLE
        abs(last - first) < 0.5 -> TrendDirection.STABLE
        last > first -> TrendDirection.UP
        else -> TrendDirection.DOWN
    }
    val trendText = when (trendDirection) {
        TrendDirection.UP -> stringResource(R.string.health_quick_trend_up)
        TrendDirection.DOWN -> stringResource(R.string.health_quick_trend_down)
        TrendDirection.STABLE -> stringResource(R.string.health_quick_trend_stable)
    }

    val lastTenDateSet = lastTenDates.toSet()
    val recordsForMiniChart = records.filter { it.recordedAt.toLocalDate() in lastTenDateSet }
    val unit = records.firstOrNull()?.unit.orEmpty()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            Text(
                text = stringResource(R.string.health_quick_last_10_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            if (recordsForMiniChart.size >= 2) {
                HealthEvolutionChart(
                    type = type,
                    records = recordsForMiniChart,
                    title = stringResource(R.string.health_quick_chart_title),
                    chartHeight = 140
                )
            } else {
                Text(
                    text = stringResource(R.string.health_quick_not_enough_data),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = stringResource(R.string.health_quick_trend_label, trendText),
                style = MaterialTheme.typography.bodyMedium,
                color = trendColor(type, trendDirection)
            )

            summaries.forEach { summary ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = dayLabel(summary.date, locale),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatSummaryValue(type, summary.primaryAvg, summary.secondaryAvg, unit),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun trendColor(type: HealthRecordType, direction: TrendDirection) = when (direction) {
    TrendDirection.STABLE -> MaterialTheme.colorScheme.onSurfaceVariant
    TrendDirection.UP -> when (type) {
        HealthRecordType.GLUCOSE,
        HealthRecordType.BLOOD_PRESSURE,
        HealthRecordType.HEART_RATE,
        HealthRecordType.TEMPERATURE -> MaterialTheme.colorScheme.error

        HealthRecordType.OXYGEN_SATURATION -> MaterialTheme.colorScheme.primary
        HealthRecordType.WEIGHT,
        HealthRecordType.CUSTOM -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    TrendDirection.DOWN -> when (type) {
        HealthRecordType.OXYGEN_SATURATION -> MaterialTheme.colorScheme.error
        HealthRecordType.GLUCOSE,
        HealthRecordType.BLOOD_PRESSURE,
        HealthRecordType.HEART_RATE,
        HealthRecordType.TEMPERATURE -> MaterialTheme.colorScheme.primary

        HealthRecordType.WEIGHT,
        HealthRecordType.CUSTOM -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

private fun formatSummaryValue(
    type: HealthRecordType,
    primary: Double,
    secondary: Double?,
    unit: String
): String {
    return if (type == HealthRecordType.BLOOD_PRESSURE && secondary != null) {
        "${formatNumericValue(primary)}/${formatNumericValue(secondary)} $unit"
    } else {
        "${formatNumericValue(primary)} $unit"
    }
}

@Composable
private fun DayHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .fillMaxWidth()
            .padding(vertical = AppSpacing.xs)
    )
}

@Composable
private fun HealthRecordItem(
    record: HealthRecord,
    onDeleteClick: () -> Unit
) {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", locale)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.recordedAt.format(formatter),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(AppSpacing.xs))
                Text(
                    text = record.displayValue(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!record.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(AppSpacing.xs))
                    Text(
                        text = record.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.health_delete_record_cd),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun HealthRecordType.displayName(): String {
    return when (this) {
        HealthRecordType.BLOOD_PRESSURE -> stringResource(R.string.health_type_blood_pressure)
        HealthRecordType.GLUCOSE -> stringResource(R.string.health_type_glucose)
        HealthRecordType.WEIGHT -> stringResource(R.string.health_type_weight)
        HealthRecordType.HEART_RATE -> stringResource(R.string.health_type_heart_rate)
        HealthRecordType.TEMPERATURE -> stringResource(R.string.health_type_temperature)
        HealthRecordType.OXYGEN_SATURATION -> stringResource(R.string.health_type_oxygen_saturation)
        HealthRecordType.CUSTOM -> stringResource(R.string.health_type_custom)
    }
}

private fun HealthRecord.displayValue(): String {
    val primaryText = formatNumericValue(value)
    return if (type == HealthRecordType.BLOOD_PRESSURE && secondaryValue != null) {
        "${primaryText}/${formatNumericValue(secondaryValue)} $unit"
    } else {
        "$primaryText $unit"
    }
}

private fun formatNumericValue(value: Double): String {
    return if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
}

@Composable
private fun dayLabel(date: LocalDate, locale: Locale): String {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    return when (date) {
        today -> stringResource(R.string.health_day_today)
        yesterday -> stringResource(R.string.health_day_yesterday)
        else -> date
            .format(DateTimeFormatter.ofPattern("EEEE d MMMM", locale))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }
}

private enum class HealthUnitMode { FIXED, OPTIONS, FREE }

private data class HealthUnitConfig(
    val mode: HealthUnitMode,
    val options: List<String> = emptyList()
)

private fun HealthRecordType.unitConfig(): HealthUnitConfig = when (this) {
    HealthRecordType.BLOOD_PRESSURE -> HealthUnitConfig(HealthUnitMode.FIXED)
    HealthRecordType.HEART_RATE -> HealthUnitConfig(HealthUnitMode.FIXED)
    HealthRecordType.OXYGEN_SATURATION -> HealthUnitConfig(HealthUnitMode.FIXED)
    HealthRecordType.GLUCOSE -> HealthUnitConfig(HealthUnitMode.OPTIONS, options = listOf("mg/dL", "mmol/L"))
    HealthRecordType.WEIGHT -> HealthUnitConfig(HealthUnitMode.OPTIONS, options = listOf("kg", "lb"))
    HealthRecordType.TEMPERATURE -> HealthUnitConfig(HealthUnitMode.OPTIONS, options = listOf("°C", "°F"))
    HealthRecordType.CUSTOM -> HealthUnitConfig(HealthUnitMode.FREE)
}
