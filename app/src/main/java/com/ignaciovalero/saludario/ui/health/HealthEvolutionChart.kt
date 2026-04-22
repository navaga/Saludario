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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ignaciovalero.saludario.R
import com.ignaciovalero.saludario.data.local.entity.HealthRecord
import com.ignaciovalero.saludario.data.local.entity.HealthRecordType
import com.ignaciovalero.saludario.ui.theme.AppSpacing
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

    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val secondaryColor = MaterialTheme.colorScheme.tertiary.toArgb()

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
                LineChart.LineSpec(lineColor = primaryColor, lineThicknessDp = 2.5f),
                LineChart.LineSpec(lineColor = secondaryColor, lineThicknessDp = 2.5f)
            )
        } else {
            listOf(LineChart.LineSpec(lineColor = primaryColor, lineThicknessDp = 2.5f))
        },
        decorations = decorations.ifEmpty { null }
    )

    val model = if (type == HealthRecordType.BLOOD_PRESSURE && secondaryEntries.size == primaryEntries.size) {
        entryModelOf(primaryEntries, secondaryEntries)
    } else {
        entryModelOf(primaryEntries)
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
            if (zones.isNotEmpty()) {
                ZoneLegend(zones = zones)
            }
            Chart(
                chart = chart,
                model = model,
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight.dp)
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
    HealthRecordType.BLOOD_PRESSURE -> listOf(
        HealthZone(
            "< 90 mmHg (tensión baja)",
            0f, 90f,
            android.graphics.Color.argb(55, 33, 150, 243),
            android.graphics.Color.argb(255, 21, 101, 192)
        ),
        HealthZone(
            "90-120 mmHg (normal)",
            90f, 120f,
            android.graphics.Color.argb(55, 76, 175, 80),
            android.graphics.Color.argb(255, 46, 125, 50)
        ),
        HealthZone(
            "120-140 mmHg (elevado)",
            120f, 140f,
            android.graphics.Color.argb(55, 255, 152, 0),
            android.graphics.Color.argb(255, 230, 81, 0)
        ),
        HealthZone(
            "> 140 mmHg (hipertensión)",
            140f, 300f,
            android.graphics.Color.argb(55, 244, 67, 54),
            android.graphics.Color.argb(255, 183, 28, 28)
        )
    )
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
