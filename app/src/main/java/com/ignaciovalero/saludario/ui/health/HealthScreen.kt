package com.ignaciovalero.saludario.ui.health

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ignaciovalero.saludario.R
import com.ignaciovalero.saludario.data.local.entity.HealthRecordType
import com.ignaciovalero.saludario.ui.theme.AppSpacing
import com.ignaciovalero.saludario.ui.theme.SaludarioTheme

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HealthScreen(
    onTypeSelected: (HealthRecordType) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val types = HealthRecordType.entries

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.health_title)) }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            items(types, key = { it.name }) { type ->
                HealthTypeCard(
                    type = type,
                    onClick = { onTypeSelected(type) }
                )
            }


        }
    }
}

@Composable
private fun HealthTypeCard(
    type: HealthRecordType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, label) = type.toUi()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = AppSpacing.xs)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(
                    modifier = Modifier.padding(AppSpacing.sm),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.size(AppSpacing.md))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun HealthRecordType.toUi(): Pair<ImageVector, String> {
    return when (this) {
        HealthRecordType.BLOOD_PRESSURE -> Icons.Default.MonitorHeart to stringResource(R.string.health_type_blood_pressure)
        HealthRecordType.GLUCOSE -> Icons.Default.Science to stringResource(R.string.health_type_glucose)
        HealthRecordType.WEIGHT -> Icons.Default.Scale to stringResource(R.string.health_type_weight)
        HealthRecordType.HEART_RATE -> Icons.Default.Favorite to stringResource(R.string.health_type_heart_rate)
        HealthRecordType.TEMPERATURE -> Icons.Default.Thermostat to stringResource(R.string.health_type_temperature)
        HealthRecordType.OXYGEN_SATURATION -> Icons.Default.WaterDrop to stringResource(R.string.health_type_oxygen_saturation)
        HealthRecordType.CUSTOM -> Icons.Default.Speed to stringResource(R.string.health_type_custom)
    }
}

@Preview(showBackground = true)
@Composable
private fun HealthScreenPreview() {
    SaludarioTheme {
        HealthScreen(
            onTypeSelected = {},
            contentPadding = PaddingValues(0.dp)
        )
    }
}
