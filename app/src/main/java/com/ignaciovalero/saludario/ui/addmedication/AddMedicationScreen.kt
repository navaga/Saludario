package com.ignaciovalero.saludario.ui.addmedication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ignaciovalero.saludario.ui.theme.AppSpacing
import com.ignaciovalero.saludario.R
import com.ignaciovalero.saludario.data.local.entity.MedicationScheduleType
import com.ignaciovalero.saludario.ui.theme.SaludarioTheme
import java.time.DayOfWeek
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicationScreen(
    uiState: AddMedicationUiState,
    onNameChange: (String) -> Unit,
    onDosageChange: (String) -> Unit,
    onUnitChange: (String) -> Unit,
    onStockTotalChange: (String) -> Unit,
    onStockRemainingChange: (String) -> Unit,
    onLowStockThresholdChange: (String) -> Unit,
    onClearStockFields: () -> Unit,
    onScheduleTypeChange: (MedicationScheduleType) -> Unit,
    onTimeSelected: (LocalTime) -> Unit,
    onDayToggle: (DayOfWeek) -> Unit,
    onIntervalHoursChange: (String) -> Unit,
    onSave: () -> Unit,
    onSaved: () -> Unit,
    onMessageShown: () -> Unit,
    snackbarHostState: SnackbarHostState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let {
            snackbarHostState.showSnackbar(context.getString(it))
            onMessageShown()
        }
    }
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSaved()
    }

    var showTimePicker by remember { mutableStateOf(false) }
    var stockVisibilityInitialized by rememberSaveable(uiState.editingId) { mutableStateOf(false) }
    var showInventoryFields by rememberSaveable(uiState.editingId) { mutableStateOf(false) }

    LaunchedEffect(uiState.editingId, uiState.stockTotal, uiState.stockRemaining, uiState.lowStockThreshold) {
        if (!stockVisibilityInitialized) {
            showInventoryFields = uiState.stockTotal.isNotBlank() ||
                uiState.stockRemaining.isNotBlank() ||
                uiState.lowStockThreshold.isNotBlank()
            stockVisibilityInitialized = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        TopAppBar(title = {
            val titleRes = if (uiState.editingId != null) R.string.edit_medication_title
            else R.string.add_medication_title
            Text(stringResource(titleRes))
        })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = onNameChange,
                        label = { Text(stringResource(R.string.add_medication_name_label)) },
                        singleLine = true,
                        isError = uiState.nameError != null,
                        supportingText = uiState.nameError?.let { errorRes -> { Text(stringResource(errorRes), color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = uiState.dosage,
                        onValueChange = onDosageChange,
                        label = { Text(stringResource(R.string.add_medication_dosage_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = uiState.dosageError != null,
                        supportingText = uiState.dosageError?.let { errorRes -> { Text(stringResource(errorRes), color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier.fillMaxWidth()
                    )

                    DropdownField(
                        label = stringResource(R.string.add_medication_unit_label),
                        selected = uiState.unit,
                        options = unitOptions,
                        onSelect = onUnitChange,
                        displayMapper = { key ->
                            when (key) {
                                "tableta" -> stringResource(R.string.unit_tablet)
                                "cápsula" -> stringResource(R.string.unit_capsule)
                                "ml" -> stringResource(R.string.unit_ml)
                                "gotas" -> stringResource(R.string.unit_drops)
                                "mg" -> stringResource(R.string.unit_mg)
                                else -> key
                            }
                        }
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = stringResource(R.string.add_medication_quantity_section_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (showInventoryFields) {
                        OutlinedButton(
                            onClick = {
                                showInventoryFields = false
                                onClearStockFields()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.add_medication_hide_quantity_button))
                        }

                        OutlinedTextField(
                            value = uiState.stockTotal,
                            onValueChange = onStockTotalChange,
                            label = { Text(stringResource(R.string.add_medication_stock_total_label)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = uiState.stockTotalError != null,
                            supportingText = uiState.stockTotalError?.let { errorRes ->
                                { Text(stringResource(errorRes), color = MaterialTheme.colorScheme.error) }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = uiState.stockRemaining,
                            onValueChange = onStockRemainingChange,
                            label = { Text(stringResource(R.string.add_medication_stock_remaining_label)) },
                            placeholder = { Text(stringResource(R.string.add_medication_stock_remaining_placeholder)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = uiState.stockRemainingError != null,
                            supportingText = uiState.stockRemainingError?.let { errorRes ->
                                { Text(stringResource(errorRes), color = MaterialTheme.colorScheme.error) }
                            } ?: {
                                Text(stringResource(R.string.add_medication_stock_remaining_hint))
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = uiState.lowStockThreshold,
                            onValueChange = onLowStockThresholdChange,
                            label = { Text(stringResource(R.string.add_medication_low_stock_threshold_label)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = uiState.lowStockThresholdError != null,
                            supportingText = uiState.lowStockThresholdError?.let { errorRes ->
                                { Text(stringResource(errorRes), color = MaterialTheme.colorScheme.error) }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.add_medication_quantity_optional_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = { showInventoryFields = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.add_medication_show_quantity_button))
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.add_medication_schedule_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FrequencyChips(
                        selected = uiState.scheduleType,
                        onSelect = onScheduleTypeChange
                    )
                }
            }

            // Day selector for SPECIFIC_DAYS
            if (uiState.scheduleType == MedicationScheduleType.SPECIFIC_DAYS) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                    Text(
                        text = stringResource(R.string.add_medication_days_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (uiState.daysError != null) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        DayOfWeek.entries.forEach { day ->
                            FilterChip(
                                selected = day in uiState.selectedDays,
                                onClick = { onDayToggle(day) },
                                label = { Text(dayAbbreviation(day)) },
                                modifier = Modifier.widthIn(min = 56.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                    if (uiState.daysError != null) {
                        Text(
                            text = stringResource(uiState.daysError),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    }
                }
            }

            // Interval hours input for INTERVAL
            if (uiState.scheduleType == MedicationScheduleType.INTERVAL) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.intervalHours,
                            onValueChange = onIntervalHoursChange,
                            label = { Text(stringResource(R.string.add_medication_interval_label)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = uiState.intervalError != null,
                            supportingText = uiState.intervalError?.let { errorRes ->
                                { Text(stringResource(errorRes), color = MaterialTheme.colorScheme.error) }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Time picker field
            val timeLabel = if (uiState.scheduleType == MedicationScheduleType.INTERVAL) {
                stringResource(R.string.add_medication_start_time_label)
            } else {
                stringResource(R.string.add_medication_time_label)
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.time,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(timeLabel) },
                        isError = uiState.timeError != null,
                        supportingText = uiState.timeError?.let { errorRes ->
                            { Text(stringResource(errorRes), color = MaterialTheme.colorScheme.error) }
                        },
                        modifier = Modifier
                            .fillMaxWidth(),
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            .also { interactionSource ->
                                LaunchedEffect(interactionSource) {
                                    interactionSource.interactions.collect { interaction ->
                                        if (interaction is androidx.compose.foundation.interaction.PressInteraction.Release) {
                                            showTimePicker = true
                                        }
                                    }
                                }
                            }
                    )
                }
            }

            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(stringResource(R.string.add_medication_save))
            }

            Spacer(modifier = Modifier.height(AppSpacing.lg))
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = uiState.selectedTime?.hour ?: 8,
            initialMinute = uiState.selectedTime?.minute ?: 0,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onTimeSelected(LocalTime.of(timePickerState.hour, timePickerState.minute))
                    showTimePicker = false
                }) {
                    Text(stringResource(R.string.time_picker_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.time_picker_cancel))
                }
            },
            title = { Text(stringResource(R.string.time_picker_title)) },
            text = { TimePicker(state = timePickerState) }
        )
    }
}

@Composable
private fun dayAbbreviation(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> stringResource(R.string.day_mon)
    DayOfWeek.TUESDAY -> stringResource(R.string.day_tue)
    DayOfWeek.WEDNESDAY -> stringResource(R.string.day_wed)
    DayOfWeek.THURSDAY -> stringResource(R.string.day_thu)
    DayOfWeek.FRIDAY -> stringResource(R.string.day_fri)
    DayOfWeek.SATURDAY -> stringResource(R.string.day_sat)
    DayOfWeek.SUNDAY -> stringResource(R.string.day_sun)
}

@Composable
private fun scheduleTypeLabel(type: MedicationScheduleType): String = when (type) {
    MedicationScheduleType.DAILY -> stringResource(R.string.schedule_daily)
    MedicationScheduleType.SPECIFIC_DAYS -> stringResource(R.string.schedule_specific_days)
    MedicationScheduleType.INTERVAL -> stringResource(R.string.schedule_interval)
}

@Composable
private fun FrequencyChips(
    selected: MedicationScheduleType,
    onSelect: (MedicationScheduleType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MedicationScheduleType.entries.forEach { type ->
            FilterChip(
                selected = type == selected,
                onClick = { onSelect(type) },
                label = { Text(scheduleTypeLabel(type)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    displayMapper: @Composable (String) -> String = { it }
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = displayMapper(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(displayMapper(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AddMedicationScreenPreview() {
    SaludarioTheme {
        AddMedicationScreen(
            uiState = AddMedicationUiState(name = "Ibuprofeno", dosage = "1"),
            onNameChange = {},
            onDosageChange = {},
            onUnitChange = {},
            onStockTotalChange = {},
            onStockRemainingChange = {},
            onLowStockThresholdChange = {},
            onClearStockFields = {},
            onScheduleTypeChange = {},
            onTimeSelected = {},
            onDayToggle = {},
            onIntervalHoursChange = {},
            onSave = {},
            onSaved = {},
            onMessageShown = {},
            snackbarHostState = androidx.compose.material3.SnackbarHostState(),
            contentPadding = PaddingValues(0.dp)
        )
    }
}
