package com.ignaciovalero.saludario.ui.settings.reliability

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ignaciovalero.saludario.R
import com.ignaciovalero.saludario.core.permissions.ReminderReliability
import com.ignaciovalero.saludario.core.permissions.ReminderReliabilityStatus
import com.ignaciovalero.saludario.ui.theme.AppSpacing

/**
 * Pantalla "Asegura tus recordatorios". Se refresca cada vez que el usuario
 * vuelve desde Ajustes del sistema (gracias al [LifecycleEventObserver]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderReliabilityScreen(
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var status by remember { mutableStateOf(ReminderReliability.snapshot(context)) }

    // Refresca el estado al volver a primer plano (el usuario puede haber
    // tocado un permiso en Ajustes del sistema).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                status = ReminderReliability.snapshot(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        status = ReminderReliability.snapshot(context)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.reliability_title)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back_button_cd)
                    )
                }
            }
        )

        Text(
            text = stringResource(R.string.reliability_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                horizontal = AppSpacing.lg,
                vertical = AppSpacing.sm
            )
        )

        if (status.allOk) {
            ReliabilityAllOkCard()
        }

        ReliabilityCard(
            icon = Icons.Filled.Notifications,
            title = stringResource(R.string.reliability_card_notifications_title),
            ok = status.notificationsAllowed,
            bodyOk = stringResource(R.string.reliability_card_notifications_body_ok),
            bodyKo = stringResource(R.string.reliability_card_notifications_body_ko),
            actionLabel = stringResource(R.string.reliability_action_open_settings),
            onAction = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !ReminderReliability.areNotificationsAllowed(context)
                ) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    openAppNotificationSettings(context)
                }
            }
        )

        ReliabilityCard(
            icon = Icons.Filled.BatteryAlert,
            title = stringResource(R.string.reliability_card_battery_title),
            ok = status.batteryUnrestricted,
            bodyOk = stringResource(R.string.reliability_card_battery_body_ok),
            bodyKo = stringResource(R.string.reliability_card_battery_body_ko),
            actionLabel = stringResource(R.string.reliability_action_open_settings),
            onAction = { requestIgnoreBatteryOptimizations(context) }
        )

        // En Android < 12 las alarmas exactas no requieren permiso, no mostramos
        // tarjeta para evitar ruido.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ReliabilityCard(
                icon = Icons.Filled.Alarm,
                title = stringResource(R.string.reliability_card_exact_title),
                ok = status.exactAlarmsAllowed,
                bodyOk = stringResource(R.string.reliability_card_exact_body_ok),
                bodyKo = stringResource(R.string.reliability_card_exact_body_ko),
                actionLabel = stringResource(R.string.reliability_action_open_settings),
                onAction = { openExactAlarmSettings(context) }
            )
        }

        Spacer(modifier = Modifier.size(AppSpacing.lg))
    }
}

@Composable
private fun ReliabilityAllOkCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    text = stringResource(R.string.reliability_all_ok_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = stringResource(R.string.reliability_all_ok_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun ReliabilityCard(
    icon: ImageVector,
    title: String,
    ok: Boolean,
    bodyOk: String,
    bodyKo: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    val statusCd = stringResource(
        if (ok) R.string.reliability_status_ok_cd else R.string.reliability_status_ko_cd
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                StatusDot(ok = ok, contentDescription = statusCd)
            }
            Text(
                text = if (ok) bodyOk else bodyKo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!ok) {
                Button(
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun StatusDot(ok: Boolean, contentDescription: String) {
    val color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val onColor = if (ok) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onError
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(color, CircleShape)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (ok) Icons.Filled.CheckCircle else Icons.Filled.Warning,
            contentDescription = null,
            tint = onColor,
            modifier = Modifier.size(18.dp)
        )
    }
}

private fun openAppNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
        .onFailure { openAppDetailsSettings(context) }
}

@Suppress("BatteryLife")
private fun requestIgnoreBatteryOptimizations(context: Context) {
    // Intent oficial. La Play Store solo permite usarlo a apps cuya
    // funcionalidad principal exige funcionar sin restricciones — los
    // recordatorios médicos lo justifican (categoría MEDICAL).
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:${context.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
        .onFailure {
            // Fallback: abrir lista general de optimización de batería.
            val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(fallback) }
                .onFailure { openAppDetailsSettings(context) }
        }
}

private fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
        data = Uri.parse("package:${context.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
        .onFailure { openAppDetailsSettings(context) }
}

private fun openAppDetailsSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}

/** Marcador para que [ReminderReliabilityStatus] siga vivo aunque no se use aún. */
@Suppress("unused")
private val keepAlive: Class<ReminderReliabilityStatus> = ReminderReliabilityStatus::class.java
