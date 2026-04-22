package com.ignaciovalero.saludario.ui.tutorial

import com.ignaciovalero.saludario.data.preferences.UserPreferencesDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TutorialManager(
    private val userPreferencesDataSource: UserPreferencesDataSource
) {
    fun shouldShow(screen: TutorialScreen): Flow<Boolean> {
        return userPreferencesDataSource.isTutorialSeen(screen.key)
            .map { seen -> !seen }
    }

    suspend fun acknowledge(screen: TutorialScreen) {
        userPreferencesDataSource.setTutorialSeen(screen.key, seen = true)
    }

    suspend fun resetAllTutorials() {
        userPreferencesDataSource.resetTutorials()
    }
}
