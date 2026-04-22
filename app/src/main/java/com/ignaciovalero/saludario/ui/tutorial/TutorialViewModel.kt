package com.ignaciovalero.saludario.ui.tutorial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ignaciovalero.saludario.SaludarioApplication
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class TutorialViewModel(
    private val tutorialManager: TutorialManager
) : ViewModel() {

    fun shouldShow(screen: TutorialScreen): Flow<Boolean> = tutorialManager.shouldShow(screen)

    fun onUnderstood(screen: TutorialScreen) {
        viewModelScope.launch {
            tutorialManager.acknowledge(screen)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as SaludarioApplication
                TutorialViewModel(
                    TutorialManager(app.container.userPreferencesDataSource)
                )
            }
        }
    }
}
