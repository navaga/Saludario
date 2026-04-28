package com.ignaciovalero.saludario.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build

/**
 * Helper para solicitar al launcher fijar uno de los widgets de la app en la
 * pantalla de inicio. Encapsula `AppWidgetManager.requestPinAppWidget`
 * comprobando soporte por API y por launcher para devolver un resultado
 * sencillo a la UI.
 */
object WidgetPinning {

    /** Tipo de widget que se quiere fijar. */
    enum class Widget {
        NEXT_DOSE,
        TODAY_SUMMARY
    }

    /** Resultado del intento de fijado. */
    enum class Result {
        /** Solicitud enviada al launcher; el usuario aún debe confirmar. */
        REQUESTED,

        /** El launcher actual no soporta fijar widgets de forma programática. */
        NOT_SUPPORTED,

        /** Error inesperado al lanzar la solicitud. */
        ERROR
    }

    /**
     * Solicita fijar el widget indicado. Debe llamarse desde un contexto con
     * Activity activa para que el launcher pueda mostrar su diálogo encima.
     */
    fun requestPin(context: Context, widget: Widget): Result {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return Result.NOT_SUPPORTED
        }
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext) ?: return Result.ERROR
        if (!manager.isRequestPinAppWidgetSupported) {
            return Result.NOT_SUPPORTED
        }

        val component = when (widget) {
            Widget.NEXT_DOSE -> ComponentName(appContext, NextDoseWidgetProvider::class.java)
            Widget.TODAY_SUMMARY -> ComponentName(appContext, TodaySummaryWidgetProvider::class.java)
        }

        // Callback opcional. Lo dejamos en null porque el feedback de éxito real
        // depende del launcher y puede no dispararse de forma fiable; el usuario
        // verá su decisión en la pantalla de inicio.
        val successCallback: PendingIntent? = null

        return runCatching {
            val ok = manager.requestPinAppWidget(component, null, successCallback)
            if (ok) Result.REQUESTED else Result.NOT_SUPPORTED
        }.getOrElse { Result.ERROR }
    }

    /**
     * Indica si el dispositivo y launcher actuales soportan fijar widgets.
     * Útil para ocultar/deshabilitar la UI cuando no tiene sentido mostrarla.
     */
    fun isSupported(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val manager = AppWidgetManager.getInstance(context.applicationContext) ?: return false
        return manager.isRequestPinAppWidgetSupported
    }
}
