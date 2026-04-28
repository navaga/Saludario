package com.ignaciovalero.saludario.ui.widget

import android.content.Context
import androidx.core.content.ContextCompat
import com.ignaciovalero.saludario.R
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Helpers de presentación compartidos por los `AppWidgetProvider`.
 *
 * Centralizar aquí evita duplicar la lógica de etiquetas y colores entre el
 * widget pequeño y el mediano cuando aparecen estados equivalentes.
 */
internal object WidgetPresentation {

    /** Etiqueta corta del estado (cabecera del bloque destacado). */
    fun headerLabel(context: Context, item: NextDoseWidgetItem): String {
        return when (item.status) {
            NextDoseWidgetItemStatus.MISSED ->
                context.getString(R.string.widget_status_label_missed)
            NextDoseWidgetItemStatus.POSTPONED ->
                context.getString(R.string.widget_status_label_postponed)
            NextDoseWidgetItemStatus.UPCOMING_FUTURE ->
                if (item.isToday) context.getString(R.string.widget_status_label_next)
                else context.getString(R.string.widget_status_label_upcoming)
            NextDoseWidgetItemStatus.PENDING,
            NextDoseWidgetItemStatus.TAKEN ->
                context.getString(R.string.widget_status_label_next)
        }
    }

    /** Color resaltado para la etiqueta del estado. */
    fun headerColor(context: Context, item: NextDoseWidgetItem): Int {
        val resId = when (item.status) {
            NextDoseWidgetItemStatus.MISSED -> R.color.widget_status_missed
            NextDoseWidgetItemStatus.POSTPONED -> R.color.widget_status_postponed
            else -> R.color.widget_accent
        }
        return ContextCompat.getColor(context, resId)
    }

    /**
     * Drawable de fondo para la tarjeta destacada. Devuelve una variante con
     * franja lateral coloreada cuando la dosis está olvidada o pospuesta para
     * reforzar visualmente el estado sin recurrir a más texto.
     */
    fun highlightCardBackground(item: NextDoseWidgetItem): Int {
        return when (item.status) {
            NextDoseWidgetItemStatus.MISSED -> R.drawable.widget_card_background_missed
            NextDoseWidgetItemStatus.POSTPONED -> R.drawable.widget_card_background_postponed
            else -> R.drawable.widget_card_background
        }
    }

    /**
     * Línea secundaria con la fecha cuando la toma no es hoy. Devuelve `null`
     * si la dosis es del día actual (no hace falta repetirlo).
     */
    fun dateHintOrNull(context: Context, dateTime: LocalDateTime, today: LocalDateTime): String? {
        val targetDate = dateTime.toLocalDate()
        val todayDate = today.toLocalDate()
        if (targetDate == todayDate) return null
        if (targetDate == todayDate.plusDays(1)) {
            return context.getString(R.string.widget_when_tomorrow)
        }
        val locale = currentLocale(context)
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
        return targetDate.format(formatter)
    }

    private fun currentLocale(context: Context): Locale {
        val locales = context.resources.configuration.locales
        return if (locales.isEmpty) Locale.getDefault() else locales[0]
    }
}
