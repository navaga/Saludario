package com.ignaciovalero.saludario.ui.today

import com.ignaciovalero.saludario.core.DoseConstants
import com.ignaciovalero.saludario.data.local.entity.MedicationLogEntity

/**
 * Eventos one-shot que la pantalla "Hoy" usa para mostrar Snackbars con
 * acción de "Deshacer" tras una interacción.
 */
enum class TodaySnackbarEvent {
    MARKED_TAKEN,
    UNMARKED_TAKEN,
    POSTPONED_30,
    POSTPONED_60,
    POSTPONED_120,
    POSTPONED_OTHER;

    companion object {
        fun postponed(minutes: Long): TodaySnackbarEvent = when (minutes) {
            DoseConstants.POSTPONE_MINUTES, 30L -> POSTPONED_30
            60L -> POSTPONED_60
            120L -> POSTPONED_120
            else -> POSTPONED_OTHER
        }
    }
}

/**
 * Representa la última acción reversible. Permite un único nivel de "deshacer".
 */
internal sealed interface UndoableAction {
    data class UpdateLog(val original: MedicationLogEntity) : UndoableAction
    data class InsertLog(val logId: Long, val medicationId: Long) : UndoableAction
}
