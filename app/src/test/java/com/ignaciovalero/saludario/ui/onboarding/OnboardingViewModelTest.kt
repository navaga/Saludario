package com.ignaciovalero.saludario.ui.onboarding

import com.ignaciovalero.saludario.data.preferences.UserPreferencesDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var dataSource: UserPreferencesDataSource

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        dataSource = mockk(relaxed = true)
        every { dataSource.onboardingCompleted } returns flowOf(false)
        every { dataSource.notificationOnboardingPromptHandled } returns flowOf(false)
        every { dataSource.preferredLanguageCode } returns MutableStateFlow("es")
        coEvery { dataSource.setOnboardingCompleted(any()) } returns Unit
        coEvery { dataSource.setNotificationOnboardingPromptHandled(any()) } returns Unit
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * `uiState` usa `SharingStarted.WhileSubscribed`. Sin un colector activo
     * el flujo upstream no se ejecuta y `value` queda en el inicial.
     */
    private fun TestScope.createVmWithSubscription(): OnboardingViewModel {
        val vm = OnboardingViewModel(dataSource)
        backgroundScope.launch { vm.uiState.collect {} }
        dispatcher.scheduler.advanceUntilIdle()
        return vm
    }

    @Test
    fun `nextPage stops at the last onboarding page`() = runTest(dispatcher) {
        val vm = createVmWithSubscription()

        repeat(OnboardingViewModel.ONBOARDING_PAGE_COUNT + 5) { vm.nextPage() }
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(OnboardingViewModel.ONBOARDING_PAGE_COUNT - 1, vm.uiState.value.page)
    }

    @Test
    fun `previousPage cannot go below zero`() = runTest(dispatcher) {
        val vm = createVmWithSubscription()

        repeat(3) { vm.previousPage() }
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, vm.uiState.value.page)
    }

    @Test
    fun `setPage clamps to valid range`() = runTest(dispatcher) {
        val vm = createVmWithSubscription()

        vm.setPage(99)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(OnboardingViewModel.ONBOARDING_PAGE_COUNT - 1, vm.uiState.value.page)

        vm.setPage(-5)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, vm.uiState.value.page)
    }

    @Test
    fun `setNotificationDecision updates ui state`() = runTest(dispatcher) {
        val vm = createVmWithSubscription()

        vm.setNotificationDecision(NotificationDecision.SKIPPED)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(NotificationDecision.SKIPPED, vm.uiState.value.notificationDecision)
    }

    @Test
    fun `completeOnboarding does nothing when disclaimer not accepted`() = runTest(dispatcher) {
        val vm = createVmWithSubscription()

        vm.completeOnboarding()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { dataSource.setOnboardingCompleted(any()) }
        coVerify(exactly = 0) { dataSource.setNotificationOnboardingPromptHandled(any()) }
    }

    @Test
    fun `completeOnboarding persists completion and notification handled when accepted`() =
        runTest(dispatcher) {
            val vm = createVmWithSubscription()

            vm.setAcceptedDisclaimer(true)
            vm.completeOnboarding()
            dispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 1) { dataSource.setNotificationOnboardingPromptHandled(true) }
            coVerify(exactly = 1) { dataSource.setOnboardingCompleted(true) }
        }

    @Test
    fun `setAcceptedDisclaimer toggles flag`() = runTest(dispatcher) {
        val vm = createVmWithSubscription()

        vm.setAcceptedDisclaimer(true)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(true, vm.uiState.value.acceptedDisclaimer)

        vm.setAcceptedDisclaimer(false)
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.uiState.value.acceptedDisclaimer)
    }

    @Test
    fun `onboarding has four pages`() {
        assertEquals(4, OnboardingViewModel.ONBOARDING_PAGE_COUNT)
    }
}
