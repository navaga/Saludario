package com.ignaciovalero.saludario.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ignaciovalero.saludario.ui.addmedication.AddMedicationScreen
import com.ignaciovalero.saludario.ui.addmedication.AddMedicationViewModel
import com.ignaciovalero.saludario.data.local.entity.HealthRecordType
import com.ignaciovalero.saludario.ui.health.HealthDetailScreen
import com.ignaciovalero.saludario.ui.health.HealthDetailViewModel
import com.ignaciovalero.saludario.ui.health.HealthGraphScreen
import com.ignaciovalero.saludario.ui.health.HealthGraphViewModel
import com.ignaciovalero.saludario.ui.health.HealthScreen
import com.ignaciovalero.saludario.ui.insights.InsightsScreen
import com.ignaciovalero.saludario.ui.insights.InsightsViewModel
import com.ignaciovalero.saludario.ui.medications.MedicationListScreen
import com.ignaciovalero.saludario.ui.medications.MedicationListViewModel
import com.ignaciovalero.saludario.ui.navigation.Screen
import com.ignaciovalero.saludario.ui.navigation.bottomBarScreens
import com.ignaciovalero.saludario.ui.onboarding.OnboardingScreen
import com.ignaciovalero.saludario.ui.onboarding.OnboardingViewModel
import com.ignaciovalero.saludario.ui.permissions.NotificationPermissionEffect
import com.ignaciovalero.saludario.ui.settings.PrivacyPolicyScreen
import com.ignaciovalero.saludario.ui.settings.SettingsScreen
import com.ignaciovalero.saludario.ui.settings.SettingsViewModel
import com.ignaciovalero.saludario.ui.simplemode.SimpleModeScreen
import com.ignaciovalero.saludario.ui.simplemode.SimpleModeViewModel
import com.ignaciovalero.saludario.ui.today.DayScreen
import com.ignaciovalero.saludario.ui.today.TodayViewModel
import com.ignaciovalero.saludario.ui.theme.AppSpacing
import com.ignaciovalero.saludario.ui.tutorial.TutorialOverlayHost
import com.ignaciovalero.saludario.ui.tutorial.TutorialScreen
import com.ignaciovalero.saludario.ui.tutorial.TutorialViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument

@Composable
fun SaludarioApp() {
    val onboardingViewModel: OnboardingViewModel = viewModel(factory = OnboardingViewModel.Factory)
    val onboardingCompleted by onboardingViewModel.onboardingCompleted.collectAsState()
    val onboardingState by onboardingViewModel.uiState.collectAsState()

    // null = DataStore aún cargando → no mostrar nada para evitar flash
    if (onboardingCompleted == null) return

    if (onboardingCompleted == false) {
        OnboardingScreen(
            uiState = onboardingState,
            onSelectLanguage = onboardingViewModel::selectLanguage,
            onPageSelected = onboardingViewModel::setPage,
            onNext = onboardingViewModel::nextPage,
            onBack = onboardingViewModel::previousPage,
            onAcceptedDisclaimerChange = onboardingViewModel::setAcceptedDisclaimer,
            onComplete = onboardingViewModel::completeOnboarding
        )
        return
    }

    NotificationPermissionEffect()

    val simpleModeViewModel: SimpleModeViewModel = viewModel(factory = SimpleModeViewModel.Factory)
    val isSimpleMode by simpleModeViewModel.isSimpleMode.collectAsState()

    if (isSimpleMode) {
        SimpleModeContent(
            onExitSimpleMode = { simpleModeViewModel.setSimpleMode(false) }
        )
    } else {
        NormalModeContent(
            onEnterSimpleMode = { simpleModeViewModel.setSimpleMode(true) }
        )
    }
}

@Composable
private fun SimpleModeContent(onExitSimpleMode: () -> Unit) {
    val todayViewModel: TodayViewModel = viewModel(factory = TodayViewModel.Factory)
    val todayState by todayViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        todayViewModel.goToToday()
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        SimpleModeScreen(
            uiState = todayState,
            onConfirmTaken = todayViewModel::toggleTaken,
            onExitSimpleMode = onExitSimpleMode,
            contentPadding = innerPadding
        )
    }
}

@Composable
private fun NormalModeContent(onEnterSimpleMode: () -> Unit) {
    val navController = rememberNavController()
    val tutorialViewModel: TutorialViewModel = viewModel(factory = TutorialViewModel.Factory)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val snackbarHostState = remember { SnackbarHostState() }
    val showMainNavigation = bottomBarScreens.any { screen ->
        currentDestination?.hierarchy?.any { it.route == screen.route } == true
    }
    val showAddMedicationFab = currentDestination?.hierarchy?.any {
        it.route == Screen.MedicationList.route
    } == true

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(snackbarData = data)
            }
        },
        floatingActionButton = {
            if (showAddMedicationFab) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.AddMedication.route) }
                ) {
                    Icon(
                        imageVector = Screen.AddMedication.icon,
                        contentDescription = stringResource(Screen.AddMedication.labelRes)
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        bottomBar = {
            if (showMainNavigation) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 4.dp
                ) {
                    bottomBarScreens.forEach { screen ->
                        val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = stringResource(screen.labelRes)
                                )
                            },
                            label = { Text(stringResource(screen.labelRes)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            selected = isSelected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Today.route
        ) {
            composable(Screen.Today.route) {
                val viewModel: TodayViewModel = viewModel(factory = TodayViewModel.Factory)
                val todayState by viewModel.uiState.collectAsState()
                val showSimpleModeHint by tutorialViewModel
                    .shouldShow(TutorialScreen.SIMPLE_MODE_HINT)
                    .collectAsState(initial = false)
                Box(modifier = Modifier.fillMaxSize()) {
                    DayScreen(
                        selectedDate = todayState.selectedDate,
                        uiState = todayState,
                        onToggleTaken = viewModel::toggleTaken,
                        onPreviousDay = viewModel::previousDay,
                        onNextDay = viewModel::nextDay,
                        onDateSelected = viewModel::setDate,
                        onEnterSimpleMode = onEnterSimpleMode,
                        onOpenSettings = { navController.navigate(Screen.Settings.route) },
                        showSimpleModeHint = showSimpleModeHint,
                        onDismissSimpleModeHint = {
                            tutorialViewModel.onUnderstood(TutorialScreen.SIMPLE_MODE_HINT)
                        },
                        contentPadding = innerPadding
                    )
                    TutorialOverlayHost(
                        screen = TutorialScreen.TODAY,
                        tutorialViewModel = tutorialViewModel
                    )
                }
            }
            composable(Screen.MedicationList.route) {
                val viewModel: MedicationListViewModel = viewModel(factory = MedicationListViewModel.Factory)
                val listState by viewModel.uiState.collectAsState()
                Box(modifier = Modifier.fillMaxSize()) {
                    MedicationListScreen(
                        uiState = listState,
                        onEdit = { medicationId ->
                            navController.navigate(Screen.EditMedication.createRoute(medicationId))
                        },
                        onAddStock = viewModel::addStock,
                        onDelete = viewModel::deleteMedication,
                        onMessageShown = viewModel::onMessageShown,
                        snackbarHostState = snackbarHostState,
                        contentPadding = innerPadding
                    )
                    TutorialOverlayHost(
                        screen = TutorialScreen.MEDICATIONS,
                        tutorialViewModel = tutorialViewModel
                    )
                }
            }
            composable(Screen.AddMedication.route) {
                val viewModel: AddMedicationViewModel = viewModel(factory = AddMedicationViewModel.Factory)
                val addState by viewModel.uiState.collectAsState()
                Box(modifier = Modifier.fillMaxSize()) {
                    AddMedicationScreen(
                        uiState = addState,
                        onNameChange = viewModel::onNameChange,
                        onDosageChange = viewModel::onDosageChange,
                        onUnitChange = viewModel::onUnitChange,
                        onStockTotalChange = viewModel::onStockTotalChange,
                        onStockRemainingChange = viewModel::onStockRemainingChange,
                        onLowStockThresholdChange = viewModel::onLowStockThresholdChange,
                        onClearStockFields = viewModel::clearStockFields,
                        onScheduleTypeChange = viewModel::onScheduleTypeChange,
                        onTimeSelected = viewModel::onTimeSelected,
                        onDayToggle = viewModel::onDayToggle,
                        onIntervalHoursChange = viewModel::onIntervalHoursChange,
                        onSave = viewModel::save,
                        onSaved = {
                            viewModel.resetForm()
                            navController.navigate(Screen.MedicationList.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                            }
                        },
                        onMessageShown = viewModel::onMessageShown,
                        snackbarHostState = snackbarHostState,
                        contentPadding = innerPadding
                    )
                    TutorialOverlayHost(
                        screen = TutorialScreen.ADD,
                        tutorialViewModel = tutorialViewModel
                    )
                }
            }
            composable(Screen.Insights.route) {
                val viewModel: InsightsViewModel = viewModel(factory = InsightsViewModel.Factory)
                val insightsState by viewModel.uiState.collectAsState()
                Box(modifier = Modifier.fillMaxSize()) {
                    InsightsScreen(
                        uiState = insightsState,
                        contentPadding = innerPadding,
                        snackbarHostState = snackbarHostState,
                        onOpenMedication = { medicationId ->
                            navController.navigate(Screen.EditMedication.createRoute(medicationId))
                        },
                        onAddStock = viewModel::addStock,
                        onDismissInsight = viewModel::dismissInsight,
                        onRetry = viewModel::retry,
                        onMessageShown = viewModel::onMessageShown
                    )
                    TutorialOverlayHost(
                        screen = TutorialScreen.INSIGHTS,
                        tutorialViewModel = tutorialViewModel
                    )
                }
            }
            composable(Screen.Health.route) {
                Box(modifier = Modifier.fillMaxSize()) {
                    HealthScreen(
                        onTypeSelected = { type ->
                            navController.navigate(Screen.HealthDetail.createRoute(type.name))
                        },
                        contentPadding = innerPadding
                    )
                    TutorialOverlayHost(
                        screen = TutorialScreen.HEALTH,
                        tutorialViewModel = tutorialViewModel
                    )
                }
            }
            composable(
                route = Screen.HealthDetail.route,
                arguments = listOf(navArgument("type") { type = NavType.StringType })
            ) { backStackEntry ->
                val typeRaw = backStackEntry.arguments?.getString("type")
                val type = runCatching { HealthRecordType.valueOf(typeRaw.orEmpty()) }
                    .getOrDefault(HealthRecordType.CUSTOM)
                val viewModel: HealthDetailViewModel = viewModel(factory = HealthDetailViewModel.factory(type))
                val healthState by viewModel.uiState.collectAsState()
                Box(modifier = Modifier.fillMaxSize()) {
                    HealthDetailScreen(
                        uiState = healthState,
                        type = type,
                        onBack = { navController.popBackStack() },
                        onOpenGraph = { navController.navigate(Screen.HealthGraph.createRoute(type.name)) },
                        onDeleteRecord = viewModel::deleteRecord,
                        onPrimaryValueChange = viewModel::onPrimaryValueChange,
                        onSecondaryValueChange = viewModel::onSecondaryValueChange,
                        onUnitChange = viewModel::onUnitChange,
                        onNotesChange = viewModel::onNotesChange,
                        onSave = viewModel::save,
                        contentPadding = innerPadding
                    )
                    TutorialOverlayHost(
                        screen = TutorialScreen.HEALTH_DETAIL,
                        tutorialViewModel = tutorialViewModel
                    )
                }
            }
            composable(
                route = Screen.HealthGraph.route,
                arguments = listOf(navArgument("type") { type = NavType.StringType })
            ) { backStackEntry ->
                val typeRaw = backStackEntry.arguments?.getString("type")
                val type = runCatching { HealthRecordType.valueOf(typeRaw.orEmpty()) }
                    .getOrDefault(HealthRecordType.CUSTOM)
                val viewModel: HealthGraphViewModel = viewModel(factory = HealthGraphViewModel.factory(type))
                val graphState by viewModel.uiState.collectAsState()
                HealthGraphScreen(
                    uiState = graphState,
                    type = type,
                    onBack = { navController.popBackStack() },
                    contentPadding = innerPadding
                )
            }
            composable(
                route = Screen.EditMedication.route,
                arguments = listOf(navArgument("medicationId") { type = NavType.LongType })
            ) { backStackEntry ->
                val medicationId = backStackEntry.arguments!!.getLong("medicationId")
                val viewModel: AddMedicationViewModel = viewModel(factory = AddMedicationViewModel.Factory)
                val editState by viewModel.uiState.collectAsState()
                androidx.compose.runtime.LaunchedEffect(medicationId) {
                    viewModel.loadMedication(medicationId)
                }
                AddMedicationScreen(
                    uiState = editState,
                    onNameChange = viewModel::onNameChange,
                    onDosageChange = viewModel::onDosageChange,
                    onUnitChange = viewModel::onUnitChange,
                    onStockTotalChange = viewModel::onStockTotalChange,
                    onStockRemainingChange = viewModel::onStockRemainingChange,
                    onLowStockThresholdChange = viewModel::onLowStockThresholdChange,
                    onClearStockFields = viewModel::clearStockFields,
                    onScheduleTypeChange = viewModel::onScheduleTypeChange,
                    onTimeSelected = viewModel::onTimeSelected,
                    onDayToggle = viewModel::onDayToggle,
                    onIntervalHoursChange = viewModel::onIntervalHoursChange,
                    onSave = viewModel::save,
                    onSaved = {
                        viewModel.resetForm()
                        navController.popBackStack()
                    },
                    onMessageShown = viewModel::onMessageShown,
                    snackbarHostState = snackbarHostState,
                    contentPadding = innerPadding
                )
            }
            composable(Screen.Settings.route) {
                val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
                SettingsScreen(
                    viewModel = viewModel,
                    snackbarHostState = snackbarHostState,
                    onBack = { navController.popBackStack() },
                    onOpenPrivacyPolicy = { navController.navigate(Screen.PrivacyPolicy.route) },
                    contentPadding = innerPadding
                )
            }
            composable(Screen.PrivacyPolicy.route) {
                PrivacyPolicyScreen(
                    onBack = { navController.popBackStack() },
                    contentPadding = innerPadding
                )
            }
        }
    }
}