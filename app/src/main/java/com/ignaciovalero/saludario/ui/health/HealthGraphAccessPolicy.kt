package com.ignaciovalero.saludario.ui.health

import com.ignaciovalero.saludario.data.local.entity.HealthRecordType
import com.ignaciovalero.saludario.data.preferences.UserPreferencesDataSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.Clock
import java.util.concurrent.TimeUnit

/**
 * Controla si una gráfica puede disparar un anuncio de entrada.
 */
class HealthGraphAccessPolicy(
    private val userPreferencesDataSource: UserPreferencesDataSource,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    fun canAccessGraph(type: HealthRecordType): Flow<Boolean> = flowOf(true)

    suspend fun shouldShowGraphEntryAd(type: HealthRecordType): Boolean {
        if (userPreferencesDataSource.isPremiumNoAds.first()) return false

        val lastShownAt = userPreferencesDataSource.getGraphAdLastShownAtMillis() ?: return true
        val cooldownMinutes = userPreferencesDataSource.getGraphAdCooldownMinutes()
        val elapsedMillis = clock.millis() - lastShownAt

        return elapsedMillis >= TimeUnit.MINUTES.toMillis(cooldownMinutes.toLong())
    }
}
