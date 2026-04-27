package com.ignaciovalero.saludario.ui.today.reliability

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ignaciovalero.saludario.R
import com.ignaciovalero.saludario.SaludarioApplication
import com.ignaciovalero.saludario.core.permissions.ReminderReliability
import com.ignaciovalero.saludario.ui.theme.AppSpacing
import kotlinx.coroutines.launch

private const val DISMISS_GRACE_DAYS = 7L
private const val DISMISS_GRACE_MILLIS = DISMISS_GRACE_DAYS * 24L * 60L * 60L * 1000L

/**
 * Banner compacto sobre la pantalla "Hoy" que avisa cuando hay permisos
 * o ajustes que pueden hacer que los recordatorios no suenen a tiempo.
 *
 * Reglas:
 * - Solo se muestra si el snapshot del sistema detecta algún problema.
 * - El usuario puede descartarlo. Vuelve a aparecer pasados [DISMISS_GRACE_DAYS] días.
 */
@Composable
fun ReminderReliabilityBanner(
    onReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as SaludarioApplication }
    val prefs = remember(app) { app.container.userPreferencesDataSource }
    val coroutineScope = rememberCoroutineScope()

    var status by remember { mutableStateOf(ReminderReliability.snapshot(context)) }

    // Refresca al volver a primer plano (el usuario pudo cambiar permisos).
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

    val dismissedAtMillis by prefs.reminderReliabilityBannerDismissedAtMillis
        .collectAsState(initial = null)

    val now = System.currentTimeMillis()
    val recentlyDismissed = dismissedAtMillis?.let { now - it < DISMISS_GRACE_MILLIS } == true

    if (status.allOk || recentlyDismissed) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = AppSpacing.md,
                    top = AppSpacing.sm,
                    bottom = AppSpacing.sm,
                    end = AppSpacing.xs
                ),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Filled.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier
                    .size(22.dp)
                    .padding(top = 4.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                Text(
                    text = stringResource(R.string.reliability_banner_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = stringResource(
                        R.string.reliability_banner_body,
                        status.issueCount
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                TextButton(onClick = onReview) {
                    Text(stringResource(R.string.reliability_action_review))
                }
            }
            val dismissCd = stringResource(R.string.reliability_banner_dismiss_cd)
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        prefs.setReminderReliabilityBannerDismissedAtMillis(
                            System.currentTimeMillis()
                        )
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = dismissCd,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}
