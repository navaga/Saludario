package com.ignaciovalero.saludario.core.logging

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Punto único para reportar errores que se capturan en `try/catch` y no
 * llegan al `UncaughtExceptionHandler` de [com.ignaciovalero.saludario.SaludarioApplication].
 *
 * Siempre loguea con [Log.e] para que sea visible en Logcat durante desarrollo
 * y, si Firebase Crashlytics está disponible y habilitado, reporta la
 * excepción para análisis remoto. Cualquier fallo del propio Crashlytics se
 * traga deliberadamente para no provocar bucles ni ocultar el error original.
 */
object ErrorReporter {

    fun report(tag: String, message: String, throwable: Throwable) {
        Log.e(tag, message, throwable)
        runCatching { FirebaseCrashlytics.getInstance().recordException(throwable) }
    }
}
