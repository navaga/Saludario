package com.ignaciovalero.saludario.ui.health

import com.ignaciovalero.saludario.data.local.entity.HealthRecordType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Punto de extensión para premium: hoy devuelve acceso libre a todas las gráficas.
 */
class HealthGraphAccessPolicy {
    fun canAccessGraph(type: HealthRecordType): Flow<Boolean> = flowOf(true)
}
