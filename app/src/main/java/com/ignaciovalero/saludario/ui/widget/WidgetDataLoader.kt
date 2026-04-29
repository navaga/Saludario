package com.ignaciovalero.saludario.ui.widget

import android.content.Context
import com.ignaciovalero.saludario.SaludarioApplication
import com.ignaciovalero.saludario.core.localization.localizedLocalTime
import com.ignaciovalero.saludario.core.localization.localizedMedicationDosage
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime

/**
 * Carga los datos de los widgets desde el [com.ignaciovalero.saludario.data.di.AppContainer]
 * y construye el [MedicationWidgetDataManager].
 *
 * Las dos lecturas de Room se hacen en paralelo porque son independientes;
 * así reducimos el tiempo total de cada actualización del widget cuando hay
 * histórico de logs grande.
 */
suspend fun loadMedicationWidgetData(
    context: Context,
    now: LocalDateTime = LocalDateTime.now()
): MedicationWidgetDataManager = coroutineScope {
    val appContext = context.applicationContext
    val app = appContext as? SaludarioApplication
        ?: error("WidgetDataLoader requiere SaludarioApplication como Context.applicationContext")
    val container = app.container

    val medicationsDeferred = async { container.medicationRepository.observeAll().first() }
    val logsDeferred = async { container.medicationLogRepository.observeAll().first() }

    MedicationWidgetDataManager(
        medications = medicationsDeferred.await(),
        logs = logsDeferred.await(),
        now = now,
        formatTime = { dateTime -> appContext.localizedLocalTime(dateTime.toLocalTime()) },
        formatDosage = { dosage, unit -> appContext.localizedMedicationDosage(dosage, unit) }
    )
}
