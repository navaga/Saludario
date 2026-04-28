package com.ignaciovalero.saludario.ui.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import com.ignaciovalero.saludario.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * Widget pequeño "Siguiente toma".
 *
 * Muestra una sola dosis destacada (olvidada urgente, próxima del día o
 * próxima futura). Es de solo lectura: el tap abre la app navegando a Today
 * y, cuando hay dosis concreta, resalta esa toma.
 */
class NextDoseWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        // Scope acotado a esta actualización: se cancela en `finally` para no
        // dejar trabajo huérfano si el provider se recrea.
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val now = LocalDateTime.now()
                val data = loadMedicationWidgetData(context, now)
                val state = data.nextDoseState()
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
        state: NextDoseWidgetState,
        now: LocalDateTime
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_next_dose)

        when (state) {
            is NextDoseWidgetState.NoMedications -> {
                renderEmpty(
                    views,
                    title = context.getString(R.string.widget_next_dose_empty_no_meds_title),
                    body = context.getString(R.string.widget_next_dose_empty_no_meds_body)
                )
                views.setOnClickPendingIntent(
                    R.id.widget_next_dose_root,
                    WidgetIntentFactory.openAppPendingIntent(context, appWidgetId)
                )
            }
            is NextDoseWidgetState.AllDone -> {
                renderEmpty(
                    views,
                    title = context.getString(R.string.widget_next_dose_all_done_title),
                    body = context.getString(R.string.widget_next_dose_all_done_body)
                )
                views.setOnClickPendingIntent(
                    R.id.widget_next_dose_root,
                    WidgetIntentFactory.openAppPendingIntent(context, appWidgetId)
                )
            }
            is NextDoseWidgetState.Dose -> {
                renderDose(context, views, state.item, now)
                views.setOnClickPendingIntent(
                    R.id.widget_next_dose_root,
                    WidgetIntentFactory.openDosePendingIntent(
                        context = context,
                        appWidgetId = appWidgetId,
                        medicationId = state.item.medicationId,
                        scheduledDateTime = state.item.scheduledDateTime
                    )
                )
            }
        }

        return views
    }

    private fun renderEmpty(views: RemoteViews, title: String, body: String) {
        views.setViewVisibility(R.id.widget_next_dose_content, View.GONE)
        views.setViewVisibility(R.id.widget_next_dose_empty_container, View.VISIBLE)
        views.setTextViewText(R.id.widget_next_dose_empty_title, title)
        views.setTextViewText(R.id.widget_next_dose_empty_body, body)
    }

    private fun renderDose(
        context: Context,
        views: RemoteViews,
        item: NextDoseWidgetItem,
        now: LocalDateTime
    ) {
        views.setViewVisibility(R.id.widget_next_dose_empty_container, View.GONE)
        views.setViewVisibility(R.id.widget_next_dose_content, View.VISIBLE)

        views.setTextViewText(R.id.widget_next_dose_header, WidgetPresentation.headerLabel(context, item))
        views.setTextColor(R.id.widget_next_dose_header, WidgetPresentation.headerColor(context, item))
        views.setTextViewText(R.id.widget_next_dose_name, item.medicationName)
        views.setTextViewText(R.id.widget_next_dose_dosage, item.dosageText)
        views.setTextViewText(R.id.widget_next_dose_time, item.timeText)

        val whenHint = WidgetPresentation.dateHintOrNull(context, item.scheduledDateTime, now)
        if (whenHint != null) {
            views.setViewVisibility(R.id.widget_next_dose_when, View.VISIBLE)
            views.setTextViewText(R.id.widget_next_dose_when, whenHint)
        } else {
            views.setViewVisibility(R.id.widget_next_dose_when, View.GONE)
        }
    }

    private fun buildErrorRemoteViews(context: Context, appWidgetId: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_next_dose)
        renderEmpty(
            views,
            title = context.getString(R.string.widget_error_title),
            body = context.getString(R.string.widget_error_body)
        )
        views.setOnClickPendingIntent(
            R.id.widget_next_dose_root,
            WidgetIntentFactory.openAppPendingIntent(context, appWidgetId)
        )
        return views
    }
}
