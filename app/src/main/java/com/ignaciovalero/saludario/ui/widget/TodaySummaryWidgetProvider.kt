package com.ignaciovalero.saludario.ui.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.ignaciovalero.saludario.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * Widget mediano "Hoy".
 *
 * Muestra el progreso del día (X/Y), una toma destacada y hasta 3 próximas
 * tomas. Solo lectura: cualquier tap abre la app, ya sea destacando una
 * dosis concreta o navegando a Today.
 */
class TodaySummaryWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val now = LocalDateTime.now()
                val data = loadMedicationWidgetData(context, now)
                val state = data.todaySummaryState(maxUpcoming = MAX_UPCOMING_ROWS)
                appWidgetIds.forEach { id ->
                    val views = buildRemoteViews(context, id, state, now)
                    appWidgetManager.updateAppWidget(id, views)
                }
            } catch (_: Throwable) {
                appWidgetIds.forEach { id ->
                    val views = buildErrorRemoteViews(context, id)
                    appWidgetManager.updateAppWidget(id, views)
                }
            } finally {
                scope.cancel()
                pendingResult.finish()
            }
        }
    }

    private fun buildRemoteViews(
        context: Context,
        appWidgetId: Int,
        state: TodaySummaryWidgetState,
        now: LocalDateTime
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_today_summary)
        views.setTextViewText(
            R.id.widget_today_title,
            context.getString(R.string.widget_today_title)
        )

        when (state) {
            is TodaySummaryWidgetState.NoMedications -> renderEmpty(context, views)
            is TodaySummaryWidgetState.Summary -> renderSummary(context, views, appWidgetId, state, now)
        }

        views.setOnClickPendingIntent(
            R.id.widget_today_root,
            WidgetIntentFactory.openAppPendingIntent(context, appWidgetId)
        )
        return views
    }

    private fun renderEmpty(context: Context, views: RemoteViews) {
        views.setViewVisibility(R.id.widget_today_content, View.GONE)
        views.setViewVisibility(R.id.widget_today_empty_container, View.VISIBLE)
        views.setTextViewText(
            R.id.widget_today_empty_title,
            context.getString(R.string.widget_today_empty_no_meds_title)
        )
        views.setTextViewText(
            R.id.widget_today_empty_body,
            context.getString(R.string.widget_today_empty_no_meds_body)
        )
    }

    private fun renderSummary(
        context: Context,
        views: RemoteViews,
        appWidgetId: Int,
        state: TodaySummaryWidgetState.Summary,
        now: LocalDateTime
    ) {
        views.setViewVisibility(R.id.widget_today_empty_container, View.GONE)
        views.setViewVisibility(R.id.widget_today_content, View.VISIBLE)

        renderProgress(context, views, state.summary)
        renderHighlight(context, views, appWidgetId, state.highlight, now)
        renderUpcoming(context, views, appWidgetId, state.upcoming)
    }

    private fun renderProgress(
        context: Context,
        views: RemoteViews,
        summary: TodayWidgetSummary
    ) {
        val counter = context.getString(
            R.string.widget_today_counter_format,
            summary.taken,
            summary.total
        )
        views.setTextViewText(R.id.widget_today_counter, counter)
        // El progreso se mide en escala absoluta para que el color del thumb
        // refleje la proporción real (0% si total=0).
        views.setProgressBar(
            R.id.widget_today_progress,
            summary.total.coerceAtLeast(1),
            summary.taken,
            false
        )

        val sub = buildSubline(context, summary)
        if (sub.isNotEmpty()) {
            views.setViewVisibility(R.id.widget_today_subline, View.VISIBLE)
            views.setTextViewText(R.id.widget_today_subline, sub)
        } else if (summary.total == 0) {
            views.setViewVisibility(R.id.widget_today_subline, View.VISIBLE)
            views.setTextViewText(
                R.id.widget_today_subline,
                context.getString(R.string.widget_today_empty_no_pending_today)
            )
        } else {
            views.setViewVisibility(R.id.widget_today_subline, View.GONE)
        }
    }

    /**
     * Construye una línea con varios contadores separados por "·" coloreando
     * cada segmento según la severidad (rojo olvidadas, naranja pospuestas).
     */
    private fun buildSubline(context: Context, summary: TodayWidgetSummary): CharSequence {
        val pieces = mutableListOf<Pair<String, Int>>()
        if (summary.pending > 0) {
            pieces += context.getString(R.string.widget_today_sub_pending, summary.pending) to
                ContextCompat.getColor(context, R.color.widget_text_secondary)
        }
        if (summary.missed > 0) {
            pieces += context.getString(R.string.widget_today_sub_missed, summary.missed) to
                ContextCompat.getColor(context, R.color.widget_status_missed)
        }
        if (summary.postponed > 0) {
            pieces += context.getString(R.string.widget_today_sub_postponed, summary.postponed) to
                ContextCompat.getColor(context, R.color.widget_status_postponed)
        }
        if (pieces.isEmpty()) return ""

        val builder = SpannableStringBuilder()
        pieces.forEachIndexed { index, (text, color) ->
            if (index > 0) builder.append(" · ")
            val start = builder.length
            builder.append(text)
            builder.setSpan(
                ForegroundColorSpan(color),
                start,
                builder.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return builder
    }

    private fun renderHighlight(
        context: Context,
        views: RemoteViews,
        appWidgetId: Int,
        highlight: NextDoseWidgetItem?,
        now: LocalDateTime
    ) {
        if (highlight == null) {
            views.setViewVisibility(R.id.widget_today_highlight_container, View.GONE)
            return
        }

        views.setViewVisibility(R.id.widget_today_highlight_container, View.VISIBLE)
        views.setInt(
            R.id.widget_today_highlight_container,
            "setBackgroundResource",
            WidgetPresentation.highlightCardBackground(highlight)
        )
        views.setTextViewText(
            R.id.widget_today_highlight_label,
            WidgetPresentation.headerLabel(context, highlight)
        )
        views.setTextColor(
            R.id.widget_today_highlight_label,
            WidgetPresentation.headerColor(context, highlight)
        )
        views.setTextViewText(R.id.widget_today_highlight_name, highlight.medicationName)

        // Cuando no es hoy añadimos el día (ej. "Mañana") junto a la dosis para
        // que el usuario no confunda la hora con una toma del día actual.
        val whenHint = WidgetPresentation.dateHintOrNull(context, highlight.scheduledDateTime, now)
        val dosageLine = if (whenHint != null) {
            "${highlight.dosageText} · $whenHint"
        } else {
            highlight.dosageText
        }
        views.setTextViewText(R.id.widget_today_highlight_dosage, dosageLine)
        views.setTextViewText(R.id.widget_today_highlight_time, highlight.timeText)

        views.setOnClickPendingIntent(
            R.id.widget_today_highlight_container,
            WidgetIntentFactory.openDosePendingIntent(
                context = context,
                appWidgetId = appWidgetId,
                medicationId = highlight.medicationId,
                scheduledDateTime = highlight.scheduledDateTime
            )
        )
    }

    private fun renderUpcoming(
        context: Context,
        views: RemoteViews,
        appWidgetId: Int,
        upcoming: List<NextDoseWidgetItem>
    ) {
        UPCOMING_ROW_IDS.forEachIndexed { index, ids ->
            val item = upcoming.getOrNull(index)
            if (item == null) {
                views.setViewVisibility(ids.row, View.GONE)
            } else {
                views.setViewVisibility(ids.row, View.VISIBLE)
                views.setTextViewText(ids.name, "${item.medicationName} · ${item.dosageText}")
                views.setTextViewText(ids.time, item.timeText)
                views.setOnClickPendingIntent(
                    ids.row,
                    WidgetIntentFactory.openDosePendingIntent(
                        context = context,
                        appWidgetId = appWidgetId,
                        medicationId = item.medicationId,
                        scheduledDateTime = item.scheduledDateTime
                    )
                )
            }
        }
    }

    private fun buildErrorRemoteViews(context: Context, appWidgetId: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_today_summary)
        views.setTextViewText(
            R.id.widget_today_title,
            context.getString(R.string.widget_today_title)
        )
        views.setViewVisibility(R.id.widget_today_content, View.GONE)
        views.setViewVisibility(R.id.widget_today_empty_container, View.VISIBLE)
        views.setTextViewText(
            R.id.widget_today_empty_title,
            context.getString(R.string.widget_error_title)
        )
        views.setTextViewText(
            R.id.widget_today_empty_body,
            context.getString(R.string.widget_error_body)
        )
        views.setOnClickPendingIntent(
            R.id.widget_today_root,
            WidgetIntentFactory.openAppPendingIntent(context, appWidgetId)
        )
        return views
    }

    private data class UpcomingRowIds(val row: Int, val name: Int, val time: Int)

    companion object {
        private const val MAX_UPCOMING_ROWS = 3

        private val UPCOMING_ROW_IDS = listOf(
            UpcomingRowIds(
                R.id.widget_today_upcoming_row_1,
                R.id.widget_today_upcoming_name_1,
                R.id.widget_today_upcoming_time_1
            ),
            UpcomingRowIds(
                R.id.widget_today_upcoming_row_2,
                R.id.widget_today_upcoming_name_2,
                R.id.widget_today_upcoming_time_2
            ),
            UpcomingRowIds(
                R.id.widget_today_upcoming_row_3,
                R.id.widget_today_upcoming_name_3,
                R.id.widget_today_upcoming_time_3
            )
        )
    }
}
