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
import com.ignaciovalero.saludario.core.formatting.formatHealthValueCompact
import com.ignaciovalero.saludario.data.local.entity.HealthRecord
import com.ignaciovalero.saludario.data.local.entity.HealthRecordType
import com.ignaciovalero.saludario.ui.theme.AppSpacing
import com.patrykandpatrick.vico.compose.axis.axisGuidelineComponent
import com.patrykandpatrick.vico.compose.axis.axisLabelComponent
import com.patrykandpatrick.vico.compose.axis.axisLineComponent
import com.patrykandpatrick.vico.compose.axis.axisTickComponent
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
    val axisLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val axisLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
    val axisGuidelineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val latestRecord = if (type == HealthRecordType.BLOOD_PRESSURE) {
        ordered.lastOrNull()?.takeIf { it.secondaryValue != null }
    } else {
        null
    }

    val primaryColorArgb = primaryColor.toArgb()
    val secondaryColorArgb = secondaryColor.toArgb()

    val zones = type.healthZones(unit = ordered.lastOrNull()?.unit ?: defaultUnitFor(type))
    val orientativeNote = if (zones.isNotEmpty()) stringResource(R.string.health_zone_orientative_note) else null
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
        val firstIndexByDay = if (!hasSingleDay) {
            buildMap<java.time.LocalDate, Int> {
                ordered.forEachIndexed { idx, r ->
                    val d = r.recordedAt.toLocalDate()
                    if (!containsKey(d)) put(d, idx)
                }
            }
        } else emptyMap()
        AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
            val index = value.toInt()
            if (index !in ordered.indices || value != index.toFloat()) {
                ""
            } else {
                val record = ordered[index]
                if (hasSingleDay) {
                    record.recordedAt.toLocalTime().format(formatter)
                } else {
                    val day = record.recordedAt.toLocalDate()
                    if (firstIndexByDay[day] == index) day.format(formatter) else ""
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
    val axisLabel = axisLabelComponent(color = axisLabelColor)
    val axisLine = axisLineComponent(color = axisLineColor, dynamicShader = null)
    val axisTick = axisTickComponent(color = axisLineColor, dynamicShader = null)
    val axisGuideline = axisGuidelineComponent(color = axisGuidelineColor, dynamicShader = null)

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
            if (type == HealthRecordType.BLOOD_PRESSURE) {
                BloodPressureLegend(
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor
                )
            }
            if (orientativeNote != null) {
                Text(
                    text = orientativeNote,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Chart(
                chart = chart,
                model = model,
                startAxis = rememberStartAxis(
                    label = axisLabel,
                    axis = axisLine,
                    tick = axisTick,
                    guideline = axisGuideline
                ),
                bottomAxis = rememberBottomAxis(
                    label = axisLabel,
                    axis = axisLine,
                    tick = axisTick,
                    guideline = axisGuideline,
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
            value = record.value.formatHealthValueCompact(),
            unit = record.unit,
            accentColor = primaryColor,
            modifier = Modifier.weight(1f)
        )
        PressureSummaryCard(
            label = stringResource(R.string.health_label_diastolic),
            value = (record.secondaryValue ?: 0.0).formatHealthValueCompact(),
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

private fun defaultUnitFor(type: HealthRecordType): String = when (type) {
    HealthRecordType.BLOOD_PRESSURE -> "mmHg"
    HealthRecordType.GLUCOSE -> "mg/dL"
    HealthRecordType.WEIGHT -> "kg"
    HealthRecordType.HEART_RATE -> "bpm"
    HealthRecordType.TEMPERATURE -> "°C"
    HealthRecordType.OXYGEN_SATURATION -> "%"
    HealthRecordType.CUSTOM -> ""
}

private val ColorBlueFill = android.graphics.Color.argb(55, 33, 150, 243)
private val ColorBlueLegend = android.graphics.Color.argb(255, 21, 101, 192)
private val ColorGreenFill = android.graphics.Color.argb(55, 76, 175, 80)
private val ColorGreenLegend = android.graphics.Color.argb(255, 46, 125, 50)
private val ColorAmberFill = android.graphics.Color.argb(55, 255, 152, 0)
private val ColorAmberLegend = android.graphics.Color.argb(255, 230, 81, 0)
private val ColorRedFill = android.graphics.Color.argb(55, 244, 67, 54)
private val ColorRedLegend = android.graphics.Color.argb(255, 183, 28, 28)

private fun zoneLabel(name: String, range: String): String = "$name · $range"

@Composable
private fun HealthRecordType.healthZones(unit: String): List<HealthZone> {
    val low = stringResource(R.string.health_zone_low)
    val normal = stringResource(R.string.health_zone_normal)
    val elevated = stringResource(R.string.health_zone_elevated)
    val high = stringResource(R.string.health_zone_high)
    val criticalLow = stringResource(R.string.health_zone_critical_low)
    val u = unit.trim().lowercase()

    return when (this) {
        HealthRecordType.GLUCOSE -> {
            val mmol = u.contains("mmol")
            if (mmol) listOf(
                HealthZone(zoneLabel(low, "< 3.9 mmol/L"), 0f, 3.9f, ColorBlueFill, ColorBlueLegend),
                HealthZone(zoneLabel(normal, "3.9–7.2 mmol/L"), 3.9f, 7.2f, ColorGreenFill, ColorGreenLegend),
                HealthZone(zoneLabel(elevated, "7.2–10.0 mmol/L"), 7.2f, 10f, ColorAmberFill, ColorAmberLegend),
                HealthZone(zoneLabel(high, "> 10.0 mmol/L"), 10f, 40f, ColorRedFill, ColorRedLegend)
            ) else listOf(
                HealthZone(zoneLabel(low, "< 70 mg/dL"), 0f, 70f, ColorBlueFill, ColorBlueLegend),
                HealthZone(zoneLabel(normal, "70–130 mg/dL"), 70f, 130f, ColorGreenFill, ColorGreenLegend),
                HealthZone(zoneLabel(elevated, "130–180 mg/dL"), 130f, 180f, ColorAmberFill, ColorAmberLegend),
                HealthZone(zoneLabel(high, "> 180 mg/dL"), 180f, 700f, ColorRedFill, ColorRedLegend)
            )
        }
        HealthRecordType.BLOOD_PRESSURE -> emptyList()
        HealthRecordType.HEART_RATE -> listOf(
            HealthZone(zoneLabel(low, "< 60 bpm"), 0f, 60f, ColorBlueFill, ColorBlueLegend),
            HealthZone(zoneLabel(normal, "60–100 bpm"), 60f, 100f, ColorGreenFill, ColorGreenLegend),
            HealthZone(zoneLabel(high, "> 100 bpm"), 100f, 300f, ColorRedFill, ColorRedLegend)
        )
        HealthRecordType.OXYGEN_SATURATION -> listOf(
            HealthZone(zoneLabel(criticalLow, "< 90%"), 0f, 90f, ColorRedFill, ColorRedLegend),
            HealthZone(zoneLabel(low, "90–95%"), 90f, 95f, ColorAmberFill, ColorAmberLegend),
            HealthZone(zoneLabel(normal, "95–100%"), 95f, 100f, ColorGreenFill, ColorGreenLegend)
        )
        HealthRecordType.TEMPERATURE -> {
            val fahrenheit = u.contains("f")
            if (fahrenheit) listOf(
                HealthZone(zoneLabel(low, "< 96.8 °F"), 86f, 96.8f, ColorBlueFill, ColorBlueLegend),
                HealthZone(zoneLabel(normal, "96.8–99.5 °F"), 96.8f, 99.5f, ColorGreenFill, ColorGreenLegend),
                HealthZone(zoneLabel(elevated, "99.5–100.4 °F"), 99.5f, 100.4f, ColorAmberFill, ColorAmberLegend),
                HealthZone(zoneLabel(high, "> 100.4 °F"), 100.4f, 113f, ColorRedFill, ColorRedLegend)
            ) else listOf(
                HealthZone(zoneLabel(low, "< 36 °C"), 30f, 36f, ColorBlueFill, ColorBlueLegend),
                HealthZone(zoneLabel(normal, "36–37.5 °C"), 36f, 37.5f, ColorGreenFill, ColorGreenLegend),
                HealthZone(zoneLabel(elevated, "37.5–38 °C"), 37.5f, 38f, ColorAmberFill, ColorAmberLegend),
                HealthZone(zoneLabel(high, "> 38 °C"), 38f, 45f, ColorRedFill, ColorRedLegend)
            )
        }
        HealthRecordType.WEIGHT, HealthRecordType.CUSTOM -> emptyList()
    }
}

@Composable
private fun BloodPressureLegend(primaryColor: Color, secondaryColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendDot(stringResource(R.string.health_label_systolic), primaryColor)
        LegendDot(stringResource(R.string.health_label_diastolic), secondaryColor)
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(10.dp).background(color = color, shape = CircleShape))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
