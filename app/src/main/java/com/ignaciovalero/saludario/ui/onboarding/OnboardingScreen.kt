package com.ignaciovalero.saludario.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ignaciovalero.saludario.R
import com.ignaciovalero.saludario.ui.theme.AppSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    onSelectLanguage: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onAcceptedDisclaimerChange: (Boolean) -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var languageExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f)
                    )
                )
            )
            .safeDrawingPadding()
            .padding(AppSpacing.lg),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    OutlinedButton(onClick = { languageExpanded = !languageExpanded }) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(text = uiState.languageCode.uppercase())
                    }

                    androidx.compose.material3.DropdownMenu(
                        expanded = languageExpanded,
                        onDismissRequest = { languageExpanded = false }
                    ) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(stringResource(R.string.language_option_spanish)) },
                            onClick = {
                                onSelectLanguage("es")
                                languageExpanded = false
                            }
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(stringResource(R.string.language_option_english)) },
                            onClick = {
                                onSelectLanguage("en")
                                languageExpanded = false
                            }
                        )
                    }
                }
            }

            if (uiState.page == 0) {
                FirstPage()
            } else {
                SecondPage(
                    accepted = uiState.acceptedDisclaimer,
                    onAcceptedChange = onAcceptedDisclaimerChange
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                IndicatorDot(active = uiState.page == 0)
                Spacer(modifier = Modifier.size(10.dp))
                IndicatorDot(active = uiState.page == 1)
            }

            if (uiState.page == 0) {
                Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.onboarding_start_now))
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = stringResource(R.string.onboarding_back))
                    }
                    Button(
                        onClick = onComplete,
                        enabled = uiState.acceptedDisclaimer,
                        modifier = Modifier.weight(2f)
                    ) {
                        Text(text = stringResource(R.string.onboarding_accept_continue))
                    }
                }
            }
        }
    }
}

@Composable
private fun FirstPage() {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(84.dp)
            )
        }
        Text(
            text = stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        OnboardingInfoCard(
            icon = Icons.Default.CheckCircle,
            title = stringResource(R.string.onboarding_feature_1_title),
            body = stringResource(R.string.onboarding_feature_1_body)
        )
        OnboardingInfoCard(
            icon = Icons.Default.MonitorHeart,
            title = stringResource(R.string.onboarding_feature_2_title),
            body = stringResource(R.string.onboarding_feature_2_body)
        )
        OnboardingInfoCard(
            icon = Icons.Default.Security,
            title = stringResource(R.string.onboarding_feature_3_title),
            body = stringResource(R.string.onboarding_feature_3_body)
        )
        Text(
            text = stringResource(R.string.onboarding_language_future_hint),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SecondPage(
    accepted: Boolean,
    onAcceptedChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_legal_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Text(
                text = stringResource(R.string.onboarding_legal_body),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(AppSpacing.lg)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = accepted,
                onCheckedChange = onAcceptedChange
            )
            Text(
                text = stringResource(R.string.onboarding_legal_checkbox),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun OnboardingInfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = title, fontWeight = FontWeight.SemiBold)
                Text(text = body, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun IndicatorDot(active: Boolean) {
    Box(
        modifier = Modifier
            .size(if (active) 22.dp else 10.dp)
            .background(
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(999.dp)
            )
    )
}
