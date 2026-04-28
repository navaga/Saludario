package com.ignaciovalero.saludario.ui.health

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.ignaciovalero.saludario.R
import com.ignaciovalero.saludario.data.local.entity.HealthRecordType
import com.ignaciovalero.saludario.ui.theme.AppSpacing
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthGraphScreen(
    uiState: HealthGraphUiState,
    type: HealthRecordType,
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    val filteredRecords = remember(uiState.records, selectedDate) {
        uiState.records.filter { record ->
            selectedDate == null || record.recordedAt.toLocalDate() == selectedDate
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        TopAppBar(
            title = { Text(text = stringResource(R.string.health_graph_title, type.displayName())) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back_button_cd)
                    )
                }
            }
        )

        HealthDateFilterBar(
            selectedDate = selectedDate,
            onSelectDate = { selectedDate = it },
            onClearDate = { selectedDate = null },
            modifier = Modifier.padding(horizontal = AppSpacing.lg),
            availableRecords = uiState.records
        )

        when {
            uiState.isPremiumLocked -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(AppSpacing.lg),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.health_graph_premium_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.health_graph_premium_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            filteredRecords.size < 2 -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(AppSpacing.lg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedDate == null) {
                            stringResource(R.string.health_graph_need_more_data)
                        } else {
                            stringResource(R.string.health_previous_records_empty_filtered)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                HealthEvolutionChart(
                    type = type,
                    records = filteredRecords,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppSpacing.lg),
                    chartHeight = 360
                )
            }
        }
    }
}
