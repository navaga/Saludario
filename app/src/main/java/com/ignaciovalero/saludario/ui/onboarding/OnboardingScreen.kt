package com.ignaciovalero.saludario.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ignaciovalero.saludario.R
import com.ignaciovalero.saludario.ui.settings.PrivacyPolicyScreen
import com.ignaciovalero.saludario.ui.theme.AppSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    onSelectLanguage: (String) -> Unit,
    onPageSelected: (Int) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onAcceptedDisclaimerChange: (Boolean) -> Unit,
    onNotificationDecisionChange: (NotificationDecision) -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var languageExpanded by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    val pageCount = OnboardingViewModel.ONBOARDING_PAGE_COUNT
    val pagerState = rememberPagerState(initialPage = uiState.page, pageCount = { pageCount })
    val currentPage by rememberUpdatedState(uiState.page)
    val currentOnPageSelected by rememberUpdatedState(onPageSelected)

    LaunchedEffect(uiState.page) {
        if (pagerState.currentPage != uiState.page) {
            pagerState.animateScrollToPage(uiState.page)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .collect { settled ->
                if (settled != currentPage) {
                    currentOnPageSelected(settled)
                }
            }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                    )
                )
            )
            .safeDrawingPadding()
            .padding(AppSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)
    ) {
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

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            val pageModifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
            when (page) {
                0 -> WelcomePage(modifier = pageModifier)
                1 -> LegalPage(
                    accepted = uiState.acceptedDisclaimer,
                    onAcceptedChange = onAcceptedDisclaimerChange,
                    onOpenPrivacy = { showPrivacy = true },
                    modifier = pageModifier
                )
                2 -> NotificationsPage(
                    decision = uiState.notificationDecision,
                    onDecision = onNotificationDecisionChange,
                    modifier = pageModifier
                )
                else -> ReliabilityPage(modifier = pageModifier)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (index in 0 until pageCount) {
                    if (index > 0) Spacer(modifier = Modifier.size(10.dp))
                    IndicatorDot(
                        active = uiState.page == index,
                        contentDescription = stringResource(
                            R.string.onboarding_page_indicator_cd,
                            index + 1,
                            pageCount
                        ),
                        onClick = { onPageSelected(index) }
                    )
                }
            }

            when (uiState.page) {
                0 -> {
                    Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                        Text(text = stringResource(R.string.onboarding_start_now))
                    }
                }
                pageCount - 1 -> {
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
                            Text(text = stringResource(R.string.onboarding_finish))
                        }
                    }
                }
                else -> {
                    val canAdvance = when (uiState.page) {
                        1 -> uiState.acceptedDisclaimer
                        else -> true
                    }
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
                            onClick = onNext,
                            enabled = canAdvance,
                            modifier = Modifier.weight(2f)
                        ) {
                            Text(text = stringResource(R.string.onboarding_start_now))
                        }
                    }
                }
            }
        }
    }

    if (showPrivacy) {
        Dialog(
            onDismissRequest = { showPrivacy = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                PrivacyPolicyScreen(
                    onBack = { showPrivacy = false },
                    contentPadding = PaddingValues(0.dp)
                )
            }
        }
    }
}

@Composable
private fun WelcomePage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        OnboardingHeroIcon(icon = Icons.Default.Favorite)
        Text(
            text = stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
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
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Text(
                text = stringResource(R.string.onboarding_language_future_hint),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.md)
            )
        }
    }
}

@Composable
private fun LegalPage(
    accepted: Boolean,
    onAcceptedChange: (Boolean) -> Unit,
    onOpenPrivacy: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        OnboardingHeroIcon(icon = Icons.Default.Security)
        Text(
            text = stringResource(R.string.onboarding_legal_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
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

        TextButton(
            onClick = onOpenPrivacy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.onboarding_legal_privacy_link))
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.md),
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
}

@Composable
private fun NotificationsPage(
    decision: NotificationDecision,
    onDecision: (NotificationDecision) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val needsRuntimePermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val initiallyGranted = remember {
        if (needsRuntimePermission) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    LaunchedEffect(initiallyGranted) {
        if (initiallyGranted && decision == NotificationDecision.PENDING) {
            onDecision(NotificationDecision.GRANTED)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onDecision(if (granted) NotificationDecision.GRANTED else NotificationDecision.SKIPPED)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        OnboardingHeroIcon(icon = Icons.Default.Notifications)
        Text(
            text = stringResource(R.string.onboarding_notifications_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Text(
                text = stringResource(R.string.onboarding_notifications_body),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(AppSpacing.lg)
            )
        }

        when (decision) {
            NotificationDecision.PENDING -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    OutlinedButton(
                        onClick = { onDecision(NotificationDecision.SKIPPED) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = stringResource(R.string.onboarding_notifications_skip))
                    }
                    Button(
                        onClick = {
                            if (needsRuntimePermission) {
                                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onDecision(NotificationDecision.GRANTED)
                            }
                        },
                        modifier = Modifier.weight(2f)
                    ) {
                        Text(text = stringResource(R.string.onboarding_notifications_allow))
                    }
                }
            }

            NotificationDecision.GRANTED -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.onboarding_notifications_granted),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            NotificationDecision.SKIPPED -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_notifications_denied),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppSpacing.md)
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.onboarding_notifications_hint),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ReliabilityPage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        OnboardingHeroIcon(icon = Icons.Default.BatteryAlert)
        Text(
            text = stringResource(R.string.onboarding_reliability_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Text(
                text = stringResource(R.string.onboarding_reliability_body),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(AppSpacing.lg)
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Text(
                text = stringResource(R.string.onboarding_reliability_hint),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.md)
            )
        }
    }
}

@Composable
private fun OnboardingHeroIcon(icon: ImageVector) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    ) {
        Box(
            modifier = Modifier.size(112.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
        }
    }
}

@Composable
private fun OnboardingInfoCard(
    icon: ImageVector,
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
private fun IndicatorDot(
    active: Boolean,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(if (active) 28.dp else 10.dp)
            .height(10.dp)
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
            }
            .clickable(onClick = onClick)
            .background(
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(999.dp)
            )
    )
}
