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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ignaciovalero.saludario.R
import com.ignaciovalero.saludario.domain.insights.InsightDetails
import com.ignaciovalero.saludario.domain.insights.InsightSeverity
import com.ignaciovalero.saludario.domain.insights.InsightType
import com.ignaciovalero.saludario.domain.insights.MedicationInsight
import com.ignaciovalero.saludario.ui.theme.AppSpacing

@Composable
fun InsightsScreen(
    uiState: InsightsUiState,
    contentPadding: PaddingValues
) {
    val loadingContentDescription = stringResource(R.string.insights_loading_cd)

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
                        InsightsHeader()
                    }
                    items(orderedInsights, key = { "${it.medicationId}_${it.type}" }) { insight ->
                        InsightCard(insight)
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightCard(insight: MedicationInsight) {
    val visual = insight.visual()

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
            }
        }
    }
}

@Composable
private fun MedicationInsight.extraDetailText(): String? {
    return when (val info = details) {
        is InsightDetails.LowStockInfo -> {
            info.estimatedDaysRemaining?.let { days ->
                stringResource(R.string.insight_low_stock_days_remaining, days)
            }
        }

        else -> null
    }
}

@Composable
private fun InsightsHeader() {
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
    return when (this) {
        InsightSeverity.CRITICAL -> InsightVisualStyle(
            labelRes = R.string.insight_type_critical,
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
            badgeColor = MaterialTheme.colorScheme.onError,
            badgeOnColor = MaterialTheme.colorScheme.error,
            iconContainerColor = MaterialTheme.colorScheme.onError.copy(alpha = 0.16f),
            iconColor = MaterialTheme.colorScheme.onError,
            suggestionBorder = MaterialTheme.colorScheme.onError.copy(alpha = 0.45f)
        )

        InsightSeverity.WARNING -> InsightVisualStyle(
            labelRes = R.string.insight_type_warning,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            badgeColor = MaterialTheme.colorScheme.error,
            badgeOnColor = MaterialTheme.colorScheme.onError,
            iconContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
            iconColor = MaterialTheme.colorScheme.error,
            suggestionBorder = MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
        )

        InsightSeverity.INFO -> InsightVisualStyle(
            labelRes = R.string.insight_type_info,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            badgeColor = MaterialTheme.colorScheme.tertiary,
            badgeOnColor = MaterialTheme.colorScheme.onTertiary,
            iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
            iconColor = MaterialTheme.colorScheme.tertiary,
            suggestionBorder = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)
        )

        InsightSeverity.SUCCESS -> InsightVisualStyle(
            labelRes = R.string.insight_type_success,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            badgeColor = MaterialTheme.colorScheme.primary,
            badgeOnColor = MaterialTheme.colorScheme.onPrimary,
            iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            iconColor = MaterialTheme.colorScheme.primary,
            suggestionBorder = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
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
