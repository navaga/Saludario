package com.ignaciovalero.saludario.core

/**
 * Constantes globales relacionadas con la planificación y evaluación de dosis.
 *
 * Centralizadas aquí para evitar duplicación entre `ScheduledDoseGenerator`,
 * `MissedDoseWorker` y `DailyDoseGenerationWorker`.
 */
object DoseConstants {
    /**
     * Minutos de gracia tras la hora programada antes de considerar una dosis
     * como olvidada (MISSED). También se usa para no crear logs retroactivos
     * al añadir una medicación cuya hora de hoy ya ha pasado este umbral.
     */
    const val GRACE_PERIOD_MINUTES: Long = 60L

    /**
     * Minutos por defecto al posponer una dosis (acción "Posponer 30 min" o
     * acción de la notificación). Nunca acumula: cada posponer reinicia desde
     * `now`.
     */
    const val POSTPONE_MINUTES: Long = 30L
}
