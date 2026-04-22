package com.ignaciovalero.saludario.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ignaciovalero.saludario.R
import com.ignaciovalero.saludario.domain.insights.InsightDetails
import com.ignaciovalero.saludario.domain.insights.InsightSeverity
import com.ignaciovalero.saludario.domain.insights.InsightType
import com.ignaciovalero.saludario.domain.insights.MedicationInsight
import com.ignaciovalero.saludario.ui.theme.AppSpacing
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import com.ignaciovalero.saludario.ui.theme.OnWarningContainerDark
import com.ignaciovalero.saludario.ui.theme.OnWarningContainerLight
import com.ignaciovalero.saludario.ui.theme.OnWarningDark
import com.ignaciovalero.saludario.ui.theme.OnWarningLight
import com.ignaciovalero.saludario.ui.theme.WarningContainerDark
import com.ignaciovalero.saludario.ui.theme.WarningContainerLight
import com.ignaciovalero.saludario.ui.theme.WarningDark
import com.ignaciovalero.saludario.ui.theme.WarningLight

@Composable
fun InsightsScreen(
    uiState: InsightsUiState,
    contentPadding: PaddingValues,
    snackbarHostState: SnackbarHostState? = null,
    onOpenMedication: (Long) -> Unit = {},
    onAddStock: (Long, Double) -> Unit = { _, _ -> },
    onDismissInsight: (MedicationInsight) -> Unit = {},
    onRetry: () -> Unit = {},
    onMessageShown: () -> Unit = {}
) {
    val context = LocalContext.current
    val loadingContentDescription = stringResource(R.string.insights_loading_cd)
    var selectedFilter by remember { mutableStateOf(InsightsFilter.ALL) }
    var restockTarget by remember { mutableStateOf<MedicationInsight?>(null) }
    var restockAmount by remember { mutableStateOf("") }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { messageRes ->
            snackbarHostState?.showSnackbar(context.getString(messageRes))
            onMessageShown()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.semantics {
                            contentDescription = loadingContentDescription
                        }
                    )
                }
            }

            uiState.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                    contentAlignment = Alignment.Center
                ) {
                    InsightsErrorState(
                        message = stringResource(uiState.errorMessage),
                        onRetry = onRetry
                    )
                }
            }

            uiState.insights.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.insights_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                val orderedInsights = uiState.insights.sortedWith(
                    compareByDescending<MedicationInsight> { it.severity.priority() }
                        .thenBy { it.medicationName.lowercase() }
                )
                val filteredInsights = orderedInsights.filter { selectedFilter.matches(it) }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = AppSpacing.lg,
                        end = AppSpacing.lg,
                        top = contentPadding.calculateTopPadding() + AppSpacing.lg,
                        bottom = contentPadding.calculateBottomPadding() + AppSpacing.lg
                    ),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
                ) {
                    item {
                        InsightsHeader(
                            selectedFilter = selectedFilter,
                            onFilterSelected = { selectedFilter = it },
                            onClearFilters = { selectedFilter = InsightsFilter.ALL }
                        )
                    }
                    if (filteredInsights.isEmpty()) {
                        item {
                            FilteredInsightsEmptyState()
                        }
                    } else {
                        items(filteredInsights, key = { "${it.medicationId}_${it.type}" }) { insight ->
                            InsightCard(
                                insight = insight,
                                onPrimaryAction = {
                                    if (insight.type == InsightType.LOW_STOCK) {
                                        restockTarget = insight
                                        restockAmount = ""
                                    } else {
                                        onOpenMedication(insight.medicationId)
                                    }
                                },
                                onDismiss = { onDismissInsight(insight) }
                            )
                        }
                    }
                }
            }
        }
    }

    val amountValue = restockAmount.replace(',', '.').toDoubleOrNull()
    val canConfirmRestock = amountValue != null && amountValue > 0.0

    restockTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { restockTarget = null },
            title = { Text(stringResource(R.string.medications_restock_title, target.medicationName)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                    Text(
                        text = stringResource(R.string.medications_restock_message),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = restockAmount,
                        onValueChange = { restockAmount = it },
                        label = { Text(stringResource(R.string.medications_restock_amount_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddStock(target.medicationId, amountValue!!)
                        restockTarget = null
                    },
                    enabled = canConfirmRestock
                ) {
                    Text(stringResource(R.string.medications_restock_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { restockTarget = null }) {
                    Text(stringResource(R.string.time_picker_cancel))
                }
            }
        )
    }
}

@Composable
private fun InsightCard(
    insight: MedicationInsight,
    onPrimaryAction: () -> Unit,
    onDismiss: () -> Unit
) {
    val visual = insight.visual()
    val actionLabelRes = insight.primaryActionLabelRes()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = visual.containerColor,
            contentColor = visual.contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = AppSpacing.xs)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = MaterialTheme.shapes.medium,
                color = visual.iconContainerColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = visual.icon,
                        contentDescription = null,
                        tint = visual.iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = insight.medicationName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    InsightTypeBadge(
                        label = stringResource(visual.labelRes),
                        color = visual.badgeColor,
                        onColor = visual.badgeOnColor
                    )
                }

                Text(
                    text = insight.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = visual.contentColor
                )

                val extraDetail = insight.extraDetailText()
                if (extraDetail != null) {
                    Text(
                        text = extraDetail,
                        style = MaterialTheme.typography.bodySmall,
                        color = visual.contentColor.copy(alpha = 0.9f)
                    )
                }

                if (insight.suggestion != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = AppSpacing.xs)
                            .background(
                                color = visual.suggestionBg,
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = visual.iconColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = insight.suggestion,
                            style = MaterialTheme.typography.bodySmall,
                            color = visual.contentColor
                        )
                    }
                }

                if (actionLabelRes != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = AppSpacing.sm),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(text = stringResource(R.string.insight_action_seen))
                        }
                        Button(onClick = onPrimaryAction) {
                            Text(text = stringResource(actionLabelRes))
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = AppSpacing.sm),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(text = stringResource(R.string.insight_action_seen))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MedicationInsight.extraDetailText(): String? {
    val context = LocalContext.current

    return when (val info = details) {
        is InsightDetails.LowStockInfo -> {
            when {
                info.estimatedDaysRemaining != null -> pluralStringResource(
                    R.plurals.insight_low_stock_days_remaining,
                    info.estimatedDaysRemaining,
                    info.estimatedDaysRemaining
                )

                info.remainingPercentage != null -> stringResource(
                    R.string.insight_low_stock_percentage_remaining,
                    info.remainingPercentage
                )

                else -> stringResource(R.string.insight_low_stock_days_unknown)
            }
        }

        is InsightDetails.AdherenceInfo -> {
            stringResource(R.string.insight_adherence_detail, info.takenCount, info.totalCount)
        }

        else -> null
    }
}

@Composable
private fun InsightsHeader(
    selectedFilter: InsightsFilter,
    onFilterSelected: (InsightsFilter) -> Unit,
    onClearFilters: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            Text(
                text = stringResource(R.string.insights_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.insights_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.insights_filter_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                if (selectedFilter != InsightsFilter.ALL) {
                    TextButton(onClick = onClearFilters) {
                        Text(text = stringResource(R.string.insights_clear_filters))
                    }
                }
            }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                items(InsightsFilter.entries, key = { it.name }) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { onFilterSelected(filter) },
                        label = { Text(text = stringResource(filter.labelRes)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun FilteredInsightsEmptyState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = stringResource(R.string.insights_empty_filtered),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(AppSpacing.lg)
        )
    }
}

@Composable
private fun InsightsErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.lg),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            Text(
                text = stringResource(R.string.insights_error_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = onRetry) {
                    Text(text = stringResource(R.string.insights_retry))
                }
            }
        }
    }
}

@Composable
private fun InsightTypeBadge(
    label: String,
    color: Color,
    onColor: Color
) {
    Surface(
        color = color,
        contentColor = onColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs)
        )
    }
}

@Composable
private fun MedicationInsight.visual(): InsightVisual {
    val severityStyle = severity.style()

    return when (type) {
        InsightType.FREQUENTLY_MISSED -> InsightVisual(
            icon = Icons.Default.WarningAmber,
            labelRes = severityStyle.labelRes,
            containerColor = severityStyle.containerColor,
            contentColor = severityStyle.contentColor,
            badgeColor = severityStyle.badgeColor,
            badgeOnColor = severityStyle.badgeOnColor,
            iconContainerColor = severityStyle.iconContainerColor,
            iconColor = severityStyle.iconColor,
            suggestionBg = MaterialTheme.colorScheme.surface,
            suggestionBorder = severityStyle.suggestionBorder
        )

        InsightType.FREQUENT_DELAYS -> InsightVisual(
            icon = Icons.Default.Info,
            labelRes = severityStyle.labelRes,
            containerColor = severityStyle.containerColor,
            contentColor = severityStyle.contentColor,
            badgeColor = severityStyle.badgeColor,
            badgeOnColor = severityStyle.badgeOnColor,
            iconContainerColor = severityStyle.iconContainerColor,
            iconColor = severityStyle.iconColor,
            suggestionBg = MaterialTheme.colorScheme.surface,
            suggestionBorder = severityStyle.suggestionBorder
        )

        InsightType.GOOD_ADHERENCE -> InsightVisual(
            icon = Icons.Default.CheckCircle,
            labelRes = severityStyle.labelRes,
            containerColor = severityStyle.containerColor,
            contentColor = severityStyle.contentColor,
            badgeColor = severityStyle.badgeColor,
            badgeOnColor = severityStyle.badgeOnColor,
            iconContainerColor = severityStyle.iconContainerColor,
            iconColor = severityStyle.iconColor,
            suggestionBg = MaterialTheme.colorScheme.surface,
            suggestionBorder = severityStyle.suggestionBorder
        )

        InsightType.LOW_STOCK -> InsightVisual(
            icon = if (severity == InsightSeverity.CRITICAL) Icons.Default.Error else Icons.Default.WarningAmber,
            labelRes = severityStyle.labelRes,
            containerColor = severityStyle.containerColor,
            contentColor = severityStyle.contentColor,
            badgeColor = severityStyle.badgeColor,
            badgeOnColor = severityStyle.badgeOnColor,
            iconContainerColor = severityStyle.iconContainerColor,
            iconColor = severityStyle.iconColor,
            suggestionBg = MaterialTheme.colorScheme.surface,
            suggestionBorder = severityStyle.suggestionBorder
        )
    }
}

private fun InsightSeverity.priority(): Int = when (this) {
    InsightSeverity.CRITICAL -> 4
    InsightSeverity.WARNING -> 3
    InsightSeverity.INFO -> 2
    InsightSeverity.SUCCESS -> 1
}

@Composable
private fun InsightSeverity.style(): InsightVisualStyle {
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    return when (this) {
        InsightSeverity.CRITICAL -> InsightVisualStyle(
            labelRes = R.string.insight_type_critical,
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
            badgeColor = MaterialTheme.colorScheme.onError,
            badgeOnColor = MaterialTheme.colorScheme.error,
            iconContainerColor = MaterialTheme.colorScheme.onError.copy(alpha = 0.25f),
            iconColor = MaterialTheme.colorScheme.onError,
            suggestionBorder = MaterialTheme.colorScheme.onError.copy(alpha = 0.6f)
        )

        InsightSeverity.WARNING -> InsightVisualStyle(
            labelRes = R.string.insight_type_warning,
            containerColor = if (isDarkTheme) WarningContainerDark else WarningContainerLight,
            contentColor = if (isDarkTheme) OnWarningContainerDark else OnWarningContainerLight,
            badgeColor = if (isDarkTheme) WarningDark else WarningLight,
            badgeOnColor = if (isDarkTheme) OnWarningDark else OnWarningLight,
            iconContainerColor = (if (isDarkTheme) WarningDark else WarningLight).copy(alpha = 0.25f),
            iconColor = if (isDarkTheme) WarningDark else WarningLight,
            suggestionBorder = (if (isDarkTheme) WarningDark else WarningLight).copy(alpha = 0.6f)
        )

        InsightSeverity.INFO -> InsightVisualStyle(
            labelRes = R.string.insight_type_info,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            badgeColor = MaterialTheme.colorScheme.secondary,
            badgeOnColor = MaterialTheme.colorScheme.onSecondary,
            iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f),
            iconColor = MaterialTheme.colorScheme.secondary,
            suggestionBorder = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
        )

        InsightSeverity.SUCCESS -> InsightVisualStyle(
            labelRes = R.string.insight_type_success,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            badgeColor = MaterialTheme.colorScheme.primary,
            badgeOnColor = MaterialTheme.colorScheme.onPrimary,
            iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
            iconColor = MaterialTheme.colorScheme.primary,
            suggestionBorder = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
    }
}

private data class InsightVisual(
    val icon: ImageVector,
    val labelRes: Int,
    val containerColor: Color,
    val contentColor: Color,
    val badgeColor: Color,
    val badgeOnColor: Color,
    val iconContainerColor: Color,
    val iconColor: Color,
    val suggestionBg: Color,
    val suggestionBorder: Color
)

private data class InsightVisualStyle(
    val labelRes: Int,
    val containerColor: Color,
    val contentColor: Color,
    val badgeColor: Color,
    val badgeOnColor: Color,
    val iconContainerColor: Color,
    val iconColor: Color,
    val suggestionBorder: Color
)

private enum class InsightsFilter(val labelRes: Int) {
    ALL(R.string.insights_filter_all),
    MISSED(R.string.insights_filter_missed),
    DELAYS(R.string.insights_filter_delays),
    STOCK(R.string.insights_filter_stock),
    POSITIVE(R.string.insights_filter_positive);

    fun matches(insight: MedicationInsight): Boolean = when (this) {
        ALL -> true
        MISSED -> insight.type == InsightType.FREQUENTLY_MISSED
        DELAYS -> insight.type == InsightType.FREQUENT_DELAYS
        STOCK -> insight.type == InsightType.LOW_STOCK
        POSITIVE -> insight.type == InsightType.GOOD_ADHERENCE
    }
}

private fun MedicationInsight.primaryActionLabelRes(): Int? = when (type) {
    InsightType.FREQUENTLY_MISSED,
    InsightType.FREQUENT_DELAYS -> R.string.insight_action_review_schedule

    InsightType.LOW_STOCK -> R.string.medications_restock_confirm
    InsightType.GOOD_ADHERENCE -> null
}
