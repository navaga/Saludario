package com.ignaciovalero.saludario

import android.app.Application
import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import com.ignaciovalero.saludario.core.localization.AppLanguageManager
import com.ignaciovalero.saludario.data.di.AppContainer
import com.ignaciovalero.saludario.data.di.DefaultAppContainer
import com.ignaciovalero.saludario.data.notification.NotificationHelper
import com.ignaciovalero.saludario.ui.widget.MedicationWidgetUpdater
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

class SaludarioApplication : Application() {
    lateinit var container: AppContainer
        private set

    private var isCrashlyticsEnabled: Boolean = false

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(applicationContext)
        // Aplica el idioma desde una caché síncrona (SharedPreferences) para
        // evitar bloquear el hilo principal leyendo DataStore en arranque.
        AppLanguageManager.init(this)
        AppLanguageManager.applyLanguage(AppLanguageManager.cachedLanguageCode())
        initializeFirebase()
        installCrashLogging()
        NotificationHelper.createNotificationChannels(this)
        container.workScheduler.scheduleDailyDoseGeneration()
        container.workScheduler.scheduleMissedDoseCheck()
        container.workScheduler.runImmediateDoseGeneration()
        // Refresca los widgets de inicio en cuanto el proceso arranca, para
        // que reflejen el estado actual aunque el sistema no haya disparado
        // todavía el `updatePeriodMillis` programado.
        MedicationWidgetUpdater.refreshAll(this)
    }

    private fun installCrashLogging() {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (isCrashlyticsEnabled) {
                runCatching { FirebaseCrashlytics.getInstance().recordException(throwable) }
            }
            Log.e("SaludarioCrash", "Uncaught exception in thread ${thread.name}", throwable)
            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun initializeFirebase() {
        val firebaseApp = runCatching { FirebaseApp.initializeApp(this) }
            .onFailure { Log.w("SaludarioFirebase", "Firebase no inicializado: ${it.message}") }
            .getOrNull()

        isCrashlyticsEnabled = firebaseApp != null
        if (isCrashlyticsEnabled) {
            runCatching {
                val packageInfo = packageManager.getPackageInfo(packageName, 0)
                val isDebugBuild = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
                FirebaseCrashlytics.getInstance().apply {
                    setCrashlyticsCollectionEnabled(!isDebugBuild)
                    setCustomKey("app_version_name", packageInfo.versionName.orEmpty())
                    setCustomKey("app_version_code", PackageInfoCompat.getLongVersionCode(packageInfo))
                    setCustomKey("is_debug_build", isDebugBuild)
                }
                FirebaseAnalytics.getInstance(this).logEvent("app_start", null)
            }.onFailure {
                Log.w("SaludarioFirebase", "Firebase inicializado con advertencias: ${it.message}")
            }
        }
    }
}