package com.ignaciovalero.saludario.ui.health

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ignaciovalero.saludario.R
import com.ignaciovalero.saludario.data.local.entity.HealthRecord
import com.ignaciovalero.saludario.data.local.entity.HealthRecordType
import com.ignaciovalero.saludario.ui.theme.AppSpacing
import com.patrykandpatrick.vico.core.axis.AxisItemPlacer
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.chart.decoration.Decoration
import com.patrykandpatrick.vico.core.chart.decoration.ThresholdLine
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.core.component.shape.ShapeComponent
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf
import java.time.format.DateTimeFormatter

@Composable
fun HealthEvolutionChart(
    type: HealthRecordType,
    records: List<HealthRecord>,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.health_evolution_title),
    chartHeight: Int = 280
) {
    val ordered = records.sortedBy { it.recordedAt }
    val primarySeries = ordered.map { it.value.toFloat() }
    val secondarySeries = ordered.mapNotNull { it.secondaryValue?.toFloat() }
    val primaryEntries = primarySeries.mapIndexed { index, value -> FloatEntry(index.toFloat(), value) }
    val secondaryEntries = secondarySeries.mapIndexed { index, value -> FloatEntry(index.toFloat(), value) }
    val locale = LocalConfiguration.current.locales[0]
    val hasSingleDay = remember(ordered) {
        ordered.map { it.recordedAt.toLocalDate() }.distinct().size <= 1
    }
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.tertiary
    val latestRecord = if (type == HealthRecordType.BLOOD_PRESSURE) {
        ordered.lastOrNull()?.takeIf { it.secondaryValue != null }
    } else {
        null
    }

    val primaryColorArgb = primaryColor.toArgb()
    val secondaryColorArgb = secondaryColor.toArgb()

    val zones = type.healthZones()
    val decorations: List<Decoration> = zones.map { zone ->
        ThresholdLine(
            thresholdRange = zone.min..zone.max,
            lineComponent = ShapeComponent(color = zone.fillColor),
            thresholdLabel = ""
        )
    }

    val chart = lineChart(
        lines = if (type == HealthRecordType.BLOOD_PRESSURE) {
            listOf(
                LineChart.LineSpec(lineColor = primaryColorArgb, lineThicknessDp = 2.5f),
                LineChart.LineSpec(lineColor = secondaryColorArgb, lineThicknessDp = 2.5f)
            )
        } else {
            listOf(LineChart.LineSpec(lineColor = primaryColorArgb, lineThicknessDp = 2.5f))
        },
        decorations = decorations.ifEmpty { null }
    )

    val model = if (type == HealthRecordType.BLOOD_PRESSURE && secondaryEntries.size == primaryEntries.size) {
        entryModelOf(primaryEntries, secondaryEntries)
    } else {
        entryModelOf(primaryEntries)
    }
    val bottomAxisValueFormatter = remember(ordered, locale, hasSingleDay) {
        val formatter = DateTimeFormatter.ofPattern(
            if (hasSingleDay) "HH:mm" else if (ordered.size <= 6) "d MMM" else "d/M",
            locale
        )
        AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
            val index = value.toInt()
            if (index !in ordered.indices || value != index.toFloat()) {
                ""
            } else {
                val record = ordered[index]
                if (hasSingleDay) {
                    record.recordedAt.toLocalTime().format(formatter)
                } else {
                    record.recordedAt.toLocalDate().format(formatter)
                }
            }
        }
    }
    val bottomAxisItemPlacer = remember(ordered.size) {
        AxisItemPlacer.Horizontal.default(
            spacing = when {
                ordered.size <= 4 -> 1
                ordered.size <= 8 -> 2
                else -> 3
            }
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
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
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (latestRecord != null) {
                BloodPressureSummary(
                    record = latestRecord,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor
                )
            }
            if (zones.isNotEmpty()) {
                ZoneLegend(zones = zones)
            }
            Chart(
                chart = chart,
                model = model,
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis(
                    valueFormatter = bottomAxisValueFormatter,
                    itemPlacer = bottomAxisItemPlacer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight.dp)
            )
        }
    }
}

@Composable
private fun BloodPressureSummary(
    record: HealthRecord,
    primaryColor: Color,
    secondaryColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        PressureSummaryCard(
            label = stringResource(R.string.health_label_systolic),
            value = formatHealthValue(record.value),
            unit = record.unit,
            accentColor = primaryColor,
            modifier = Modifier.weight(1f)
        )
        PressureSummaryCard(
            label = stringResource(R.string.health_label_diastolic),
            value = formatHealthValue(record.secondaryValue ?: 0.0),
            unit = record.unit,
            accentColor = secondaryColor,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PressureSummaryCard(
    label: String,
    value: String,
    unit: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$value $unit",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = accentColor
            )
        }
    }
}

@Composable
private fun ZoneLegend(zones: List<HealthZone>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        zones.forEach { zone ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(color = Color(zone.legendColor), shape = CircleShape)
                )
                Text(
                    text = zone.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(zone.legendColor)
                )
            }
        }
    }
}

private data class HealthZone(
    val label: String,
    val min: Float,
    val max: Float,
    val fillColor: Int,
    val legendColor: Int
)

private fun formatHealthValue(value: Double): String {
    return if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
}

private fun HealthRecordType.healthZones(): List<HealthZone> = when (this) {
    HealthRecordType.GLUCOSE -> listOf(
        HealthZone(
            "< 70 mg/dL (hipoglucemia)",
            0f, 70f,
            android.graphics.Color.argb(55, 33, 150, 243),
            android.graphics.Color.argb(255, 21, 101, 192)
        ),
        HealthZone(
            "70-130 mg/dL (normal)",
            70f, 130f,
            android.graphics.Color.argb(55, 76, 175, 80),
            android.graphics.Color.argb(255, 46, 125, 50)
        ),
        HealthZone(
            "130-180 mg/dL (elevado)",
            130f, 180f,
            android.graphics.Color.argb(55, 255, 152, 0),
            android.graphics.Color.argb(255, 230, 81, 0)
        ),
        HealthZone(
            "> 180 mg/dL (peligroso)",
            180f, 700f,
            android.graphics.Color.argb(55, 244, 67, 54),
            android.graphics.Color.argb(255, 183, 28, 28)
        )
    )
    HealthRecordType.BLOOD_PRESSURE -> emptyList()
    HealthRecordType.HEART_RATE -> listOf(
        HealthZone(
            "< 60 lpm (bradicardia)",
            0f, 60f,
            android.graphics.Color.argb(55, 33, 150, 243),
            android.graphics.Color.argb(255, 21, 101, 192)
        ),
        HealthZone(
            "60-100 lpm (normal)",
            60f, 100f,
            android.graphics.Color.argb(55, 76, 175, 80),
            android.graphics.Color.argb(255, 46, 125, 50)
        ),
        HealthZone(
            "> 100 lpm (taquicardia)",
            100f, 300f,
            android.graphics.Color.argb(55, 244, 67, 54),
            android.graphics.Color.argb(255, 183, 28, 28)
        )
    )
    HealthRecordType.OXYGEN_SATURATION -> listOf(
        HealthZone(
            "< 90% (peligro crítico)",
            0f, 90f,
            android.graphics.Color.argb(55, 244, 67, 54),
            android.graphics.Color.argb(255, 183, 28, 28)
        ),
        HealthZone(
            "90-95% (nivel bajo)",
            90f, 95f,
            android.graphics.Color.argb(55, 255, 152, 0),
            android.graphics.Color.argb(255, 230, 81, 0)
        ),
        HealthZone(
            "95-100% (normal)",
            95f, 100f,
            android.graphics.Color.argb(55, 76, 175, 80),
            android.graphics.Color.argb(255, 46, 125, 50)
        )
    )
    HealthRecordType.TEMPERATURE -> listOf(
        HealthZone(
            "< 36°C (hipotermia)",
            30f, 36f,
            android.graphics.Color.argb(55, 33, 150, 243),
            android.graphics.Color.argb(255, 21, 101, 192)
        ),
        HealthZone(
            "36-37.5°C (normal)",
            36f, 37.5f,
            android.graphics.Color.argb(55, 76, 175, 80),
            android.graphics.Color.argb(255, 46, 125, 50)
        ),
        HealthZone(
            "37.5-38°C (febrícula)",
            37.5f, 38f,
            android.graphics.Color.argb(55, 255, 152, 0),
            android.graphics.Color.argb(255, 230, 81, 0)
        ),
        HealthZone(
            "> 38°C (fiebre)",
            38f, 45f,
            android.graphics.Color.argb(55, 244, 67, 54),
            android.graphics.Color.argb(255, 183, 28, 28)
        )
    )
    HealthRecordType.WEIGHT, HealthRecordType.CUSTOM -> emptyList()
}
