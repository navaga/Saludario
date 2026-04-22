package com.ignaciovalero.saludario.ui.simplemode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ignaciovalero.saludario.SaludarioApplication
import com.ignaciovalero.saludario.data.preferences.UserPreferencesDataSource
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SimpleModeViewModel(
    private val userPreferences: UserPreferencesDataSource
) : ViewModel() {

    val isSimpleMode: StateFlow<Boolean> = userPreferences.isSimpleMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setSimpleMode(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setSimpleMode(enabled)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as SaludarioApplication
                SimpleModeViewModel(app.container.userPreferencesDataSource)
            }
        }
    }
}
