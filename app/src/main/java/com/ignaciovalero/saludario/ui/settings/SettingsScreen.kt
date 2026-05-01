package com.ignaciovalero.saludario.ui.settings

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ignaciovalero.saludario.R
import com.ignaciovalero.saludario.data.ads.AdConsentStatus
import com.ignaciovalero.saludario.data.notification.MedicationNotificationSound
import com.ignaciovalero.saludario.ui.notification.rememberNotificationSoundPreviewPlayer
import com.ignaciovalero.saludario.ui.theme.AppSpacing
import com.ignaciovalero.saludario.ui.widget.WidgetPinning
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenAdPrivacyOptions: () -> Unit,
    onOpenReliability: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    var languageExpanded by remember { mutableStateOf(false) }
    val isDarkModeEnabled = uiState.darkModeEnabled ?: isSystemInDarkTheme()
    val hiddenInsightsSummary = if (uiState.dismissedInsightsCount > 0) {
        pluralStringResource(
            R.plurals.settings_insights_hidden_count,
            uiState.dismissedInsightsCount,
            uiState.dismissedInsightsCount
        )
    } else {
        stringResource(R.string.settings_insights_hidden_none)
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { messageRes ->
            snackbarHostState.showSnackbar(message = context.getString(messageRes))
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.exportEvents.collectLatest { event ->
            when (event) {
                is SettingsExportEvent.Ready -> {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/zip"
                        putExtra(Intent.EXTRA_STREAM, Uri.parse(event.shareUriString))
                        putExtra(
                            Intent.EXTRA_SUBJECT,
                            context.getString(R.string.settings_export_share_subject)
                        )
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val chooser = Intent.createChooser(
                        shareIntent,
                        context.getString(R.string.settings_export_share_title)
                    ).apply { addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                    context.startActivity(chooser)
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = { Text(text = stringResource(R.string.settings_title)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back_button_cd)
                    )
                }
            }
        )

        // 1. Recordatorios: lo más crítico arriba.
        ReliabilityEntryCard(onClick = onOpenReliability)

        // 2. Sonido de los recordatorios: pertenece al mismo bloque mental que Fiabilidad.
        NotificationSoundCard(
            selected = uiState.notificationSound,
            onSelected = viewModel::setMedicationNotificationSound
        )

        // 3. Idioma y apariencia: personalización general.
        LanguageAndAppearanceCard(
            languageCode = uiState.languageCode,
            languageExpanded = languageExpanded,
            onLanguageExpandedChange = { languageExpanded = it },
            onSelectLanguage = { code ->
                viewModel.selectLanguage(code)
                languageExpanded = false
            },
            isDarkModeEnabled = isDarkModeEnabled,
            onDarkModeChange = viewModel::setDarkModeEnabled
        )

        // 4. Widgets en pantalla de inicio: acceso rápido.
        WidgetsCard(
            onAddNextDose = {
                handleWidgetPin(
                    context = context,
                    snackbarHostState = snackbarHostState,
                    coroutineScope = coroutineScope,
                    widget = WidgetPinning.Widget.NEXT_DOSE
                )
            },
            onAddToday = {
                handleWidgetPin(
                    context = context,
                    snackbarHostState = snackbarHostState,
                    coroutineScope = coroutineScope,
                    widget = WidgetPinning.Widget.TODAY_SUMMARY
                )
            }
        )

        // 5. Ayuda y tutoriales: fusión de "Mini tutoriales" y "Volver a ver el tutorial inicial".
        HelpAndTutorialsCard(
            onResetTutorials = viewModel::resetTutorials,
            onReplayOnboarding = viewModel::replayOnboarding
        )

        // 6. Análisis y avisos: gestión secundaria.
        AnalysisCard(
            hiddenInsightsSummary = hiddenInsightsSummary,
            restoreEnabled = uiState.dismissedInsightsCount > 0,
            onRestore = viewModel::restoreDismissedInsights
        )

        // 7. Exportación de datos: portabilidad para médico/cuidador.
        DataExportCard(
            isExporting = isExporting,
            onExport = viewModel::exportData
        )

        // 8. Política de privacidad: información legal sobre datos.
        PrivacyPolicyCard(onOpenPrivacyPolicy = onOpenPrivacyPolicy)

        // 9. Anuncios y privacidad: consentimiento publicitario, separado del documento legal.
        AdsPrivacyCard(
            adConsentStatus = uiState.adConsentStatus,
            adPrivacyOptionsRequired = uiState.adPrivacyOptionsRequired,
            onOpenAdPrivacyOptions = onOpenAdPrivacyOptions
        )
    }
}

@Composable
private fun LanguageAndAppearanceCard(
    languageCode: String,
    languageExpanded: Boolean,
    onLanguageExpandedChange: (Boolean) -> Unit,
    onSelectLanguage: (String) -> Unit,
    isDarkModeEnabled: Boolean,
    onDarkModeChange: (Boolean) -> Unit
) {
    val languageLabel = when (languageCode.lowercase()) {
        "en" -> stringResource(R.string.language_option_english)
        else -> stringResource(R.string.language_option_spanish)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.lg),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            Text(
                text = stringResource(R.string.settings_language_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.settings_language_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Box {
                OutlinedButton(onClick = { onLanguageExpandedChange(!languageExpanded) }) {
                    val flagRes = when (languageCode.lowercase()) {
                        "en" -> R.drawable.flag_gb
                        else -> R.drawable.flag_es
                    }
                    val flagCd = when (languageCode.lowercase()) {
                        "en" -> R.string.language_flag_english_cd
                        else -> R.string.language_flag_spanish_cd
                    }
                    Icon(
                        painter = painterResource(flagRes),
                        contentDescription = stringResource(flagCd),
                        modifier = Modifier.size(20.dp),
                        tint = androidx.compose.ui.graphics.Color.Unspecified
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = languageLabel)
                }

                DropdownMenu(
                    expanded = languageExpanded,
                    onDismissRequest = { onLanguageExpandedChange(false) }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.language_option_spanish)) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.flag_es),
                                contentDescription = stringResource(R.string.language_flag_spanish_cd),
                                modifier = Modifier.size(24.dp),
                                tint = androidx.compose.ui.graphics.Color.Unspecified
                            )
                        },
                        onClick = { onSelectLanguage("es") }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.language_option_english)) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.flag_gb),
                                contentDescription = stringResource(R.string.language_flag_english_cd),
                                modifier = Modifier.size(24.dp),
                                tint = androidx.compose.ui.graphics.Color.Unspecified
                            )
                        },
                        onClick = { onSelectLanguage("en") }
                    )
                }
            }

            Text(
                text = stringResource(R.string.settings_appearance_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.settings_appearance_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = isDarkModeEnabled,
                        role = Role.Switch,
                        onValueChange = onDarkModeChange
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                ) {
                    Text(
                        text = stringResource(R.string.settings_dark_mode_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(
                            if (isDarkModeEnabled) {
                                R.string.settings_dark_mode_enabled
                            } else {
                                R.string.settings_dark_mode_disabled
                            }
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isDarkModeEnabled,
                    onCheckedChange = null
                )
            }
        }
    }
}

@Composable
private fun HelpAndTutorialsCard(
    onResetTutorials: () -> Unit,
    onReplayOnboarding: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.lg),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            Text(
                text = stringResource(R.string.settings_help_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.settings_help_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.settings_tutorials_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.settings_tutorials_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onResetTutorials,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.settings_reset_tutorials))
            }
            Text(
                text = stringResource(R.string.settings_replay_onboarding_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.settings_replay_onboarding_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = onReplayOnboarding,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.settings_replay_onboarding_action))
            }
        }
    }
}

@Composable
private fun AnalysisCard(
    hiddenInsightsSummary: String,
    restoreEnabled: Boolean,
    onRestore: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.lg),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            Text(
                text = stringResource(R.string.settings_analysis_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.settings_analysis_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = hiddenInsightsSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onRestore,
                enabled = restoreEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.settings_restore_hidden_insights))
            }
        }
    }
}

@Composable
private fun DataExportCard(
    isExporting: Boolean,
    onExport: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.lg),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            Text(
                text = stringResource(R.string.settings_export_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.settings_export_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onExport,
                enabled = !isExporting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isExporting) {
                        stringResource(R.string.settings_export_in_progress)
                    } else {
                        stringResource(R.string.settings_export_action)
                    }
                )
            }
        }
    }
}

@Composable
private fun PrivacyPolicyCard(onOpenPrivacyPolicy: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.lg),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            Text(
                text = stringResource(R.string.settings_privacy_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.settings_privacy_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onOpenPrivacyPolicy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.settings_open_privacy_policy))
            }
        }
    }
}

@Composable
private fun AdsPrivacyCard(
    adConsentStatus: AdConsentStatus,
    adPrivacyOptionsRequired: Boolean,
    onOpenAdPrivacyOptions: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.lg),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            Text(
                text = stringResource(R.string.settings_ads_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.settings_ads_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = when (adConsentStatus) {
                    AdConsentStatus.REQUIRED -> stringResource(R.string.settings_ads_consent_required)
                    AdConsentStatus.NOT_REQUIRED -> stringResource(R.string.settings_ads_consent_not_required)
                    AdConsentStatus.OBTAINED -> stringResource(R.string.settings_ads_consent_obtained)
                    AdConsentStatus.UNKNOWN -> stringResource(R.string.settings_ads_consent_unknown)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (adPrivacyOptionsRequired) {
                OutlinedButton(
                    onClick = onOpenAdPrivacyOptions,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.settings_ads_privacy_options))
                }
            } else {
                Text(
                    text = stringResource(R.string.settings_ads_privacy_options_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ReliabilityEntryCard(onClick: () -> Unit) {
    val context = LocalContext.current
    // Snapshot recalculado en cada recomposición. Suficiente para mostrar
    // el estado al entrar; si el usuario cambia un permiso desde el sistema
    // y vuelve, la pantalla se recompone (collectAsState provoca refresh).
    val status = remember(context) {
        com.ignaciovalero.saludario.core.permissions.ReminderReliability.snapshot(context)
    }
    val statusText = if (status.allOk) {
        stringResource(R.string.settings_reliability_entry_status_ok)
    } else {
        stringResource(R.string.settings_reliability_entry_status_ko, status.issueCount)
    }
    val statusColor = if (status.allOk) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm)
            .toggleable(
                value = false,
                role = Role.Button,
                onValueChange = { onClick() }
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.settings_reliability_entry_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor
                )
            }
            Text(
                text = stringResource(R.string.settings_reliability_entry_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WidgetsCard(
    onAddNextDose: () -> Unit,
    onAddToday: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.lg),
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
            Text(
                text = stringResource(R.string.settings_widgets_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.settings_widgets_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onAddNextDose,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.settings_widgets_add_next_dose))
            }
            OutlinedButton(
                onClick = onAddToday,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.settings_widgets_add_today))
            }
        }
    }
}

@Composable
private fun NotificationSoundCard(
    selected: MedicationNotificationSound,
    onSelected: (MedicationNotificationSound) -> Unit
) {
    val previewPlayer = rememberNotificationSoundPreviewPlayer()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.lg),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            Text(
                text = stringResource(R.string.settings_sound_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.settings_sound_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            MedicationNotificationSound.entries.forEach { sound ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelected(sound)
                            previewPlayer.play(sound)
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    RadioButton(
                        selected = sound == selected,
                        onClick = {
                            onSelected(sound)
                            previewPlayer.play(sound)
                        }
                    )
                    Text(
                        text = stringResource(sound.displayNameRes),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { previewPlayer.play(sound) }) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(text = stringResource(R.string.medication_sound_preview))
                    }
                }
            }
        }
    }
}

private fun handleWidgetPin(
    context: android.content.Context,
    snackbarHostState: SnackbarHostState,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    widget: WidgetPinning.Widget
) {
    val result = WidgetPinning.requestPin(context, widget)
    val messageRes = when (result) {
        WidgetPinning.Result.REQUESTED -> R.string.settings_widgets_pin_requested
        WidgetPinning.Result.NOT_SUPPORTED -> R.string.settings_widgets_pin_not_supported
        WidgetPinning.Result.ERROR -> R.string.settings_widgets_pin_error
    }
    coroutineScope.launch {
        snackbarHostState.showSnackbar(message = context.getString(messageRes))
    }
}
