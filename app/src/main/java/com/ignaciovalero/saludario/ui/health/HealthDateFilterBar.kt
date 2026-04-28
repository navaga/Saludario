package com.ignaciovalero.saludario.ui.health

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.ignaciovalero.saludario.R
import com.ignaciovalero.saludario.data.local.entity.HealthRecord
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HealthDateFilterBar(
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit,
    onClearDate: () -> Unit,
    availableRecords: List<HealthRecord>,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]

    // Conteo de mediciones por fecha para pintar indicadores en el calendario.
    // Recalcula sólo cuando cambia el listado, así no penalizamos en cada
    // recomposición del filtro.
    val countsByDate = remember(availableRecords) {
        availableRecords
            .groupingBy { it.recordedAt.toLocalDate() }
            .eachCount()
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = if (selectedDate == null) {
                stringResource(R.string.health_filter_all_dates)
            } else {
                stringResource(
                    R.string.health_filter_selected_date,
                    selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", locale))
                )
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selectedDate != null) {
                IconButton(onClick = onClearDate) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.health_clear_date_filter_cd)
                    )
                }
            }
            IconButton(onClick = { showDatePicker = true }) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = stringResource(R.string.health_pick_date_filter_cd)
                )
            }
        }
    }

    if (showDatePicker) {
        HealthDateFilterCalendarDialog(
            selectedDate = selectedDate,
            countsByDate = countsByDate,
            onConfirm = {
                onSelectDate(it)
                showDatePicker = false
            },
            onClearFilter = {
                onClearDate()
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}
