package com.ignaciovalero.saludario.ui.widget

import java.time.LocalDateTime

/**
 * Estado de UI ya resuelto para el widget pequeño "Siguiente toma".
 *
 * Los providers de widget no pueden depender de Compose ni de la jerarquía de
 * Activities, así que aquí se modela el estado en variantes que el provider
 * puede traducir directamente a `RemoteViews`.
 */
sealed interface NextDoseWidgetState {
    /** No hay medicamentos configurados. CTA: añadir medicamento. */
    data object NoMedications : NextDoseWidgetState

    /** Todas las tomas de hoy ya están tomadas y no hay próximas calculables. */
    data object AllDone : NextDoseWidgetState

    /**
     * Hay una toma destacada (puede ser olvidada urgente, pendiente de hoy o
     * próxima futura). Es la única variante que renderiza tarjeta detallada.
     */
    data class Dose(val item: NextDoseWidgetItem) : NextDoseWidgetState
}

/**
 * Estado de UI ya resuelto para el widget mediano "Hoy".
 *
 * Mantiene el contador agregado y la lista corta de próximas tomas para no
 * recargar el widget cuando se redimensiona.
 */
sealed interface TodaySummaryWidgetState {
    data object NoMedications : TodaySummaryWidgetState

    data class Summary(
        val summary: TodayWidgetSummary,
        val highlight: NextDoseWidgetItem?,
        val upcoming: List<NextDoseWidgetItem>
    ) : TodaySummaryWidgetState
}

/**
 * Resumen agregado de las tomas del día usado por el widget mediano.
 */
data class TodayWidgetSummary(
    val total: Int,
    val taken: Int,
    val pending: Int,
    val missed: Int,
    val postponed: Int
) {
    val progress: Float
        get() = if (total <= 0) 0f else taken.toFloat() / total.toFloat()
}

/**
 * Una toma concreta lista para mostrarse en widget. La hora viene como cadena
 * ya formateada por el `MedicationWidgetDataManager` para evitar tener que
 * acceder a recursos desde dentro del provider.
 */
data class NextDoseWidgetItem(
    val medicationId: Long,
    val medicationName: String,
    val dosageText: String,
    val timeText: String,
    val scheduledDateTime: LocalDateTime,
    val status: NextDoseWidgetItemStatus,
    val isToday: Boolean
)

enum class NextDoseWidgetItemStatus {
    PENDING,
    TAKEN,
    MISSED,
    POSTPONED,
    UPCOMING_FUTURE
}
