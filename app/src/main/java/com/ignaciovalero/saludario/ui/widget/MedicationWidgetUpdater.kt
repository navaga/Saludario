package com.ignaciovalero.saludario.ui.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * Punto único de refresco de los widgets de medicación.
 *
 * Envía un broadcast `ACTION_APPWIDGET_UPDATE` a cada provider con la lista
 * de IDs instalados, lo que dispara [android.appwidget.AppWidgetProvider.onUpdate]
 * en cada uno. Los providers se encargan de cargar los datos en segundo plano.
 *
 * Es seguro llamarlo desde cualquier hilo: solo envía intents.
 */
object MedicationWidgetUpdater {

    /**
     * Refresca los dos providers (pequeño y mediano). Si el usuario no ha
     * añadido ningún widget, los broadcasts no provocarán ninguna acción.
     */
    fun refreshAll(context: Context) {
        refreshProvider(context, NextDoseWidgetProvider::class.java)
        refreshProvider(context, TodaySummaryWidgetProvider::class.java)
    }

    private fun refreshProvider(context: Context, provider: Class<*>) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext) ?: return
        val component = ComponentName(appContext, provider)
        val ids = manager.getAppWidgetIds(component)
        if (ids.isEmpty()) return

        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
            this.component = component
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        appContext.sendBroadcast(intent)
    }
}
