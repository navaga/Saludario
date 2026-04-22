package com.ignaciovalero.saludario

import android.app.Application
import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import com.ignaciovalero.saludario.core.localization.AppLanguageManager
import com.ignaciovalero.saludario.data.di.AppContainer
import com.ignaciovalero.saludario.data.di.DefaultAppContainer
import com.ignaciovalero.saludario.data.notification.NotificationHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class SaludarioApplication : Application() {
    lateinit var container: AppContainer
        private set

    private var isCrashlyticsEnabled: Boolean = false

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(applicationContext)
        runBlocking {
            AppLanguageManager.applyLanguage(
                container.userPreferencesDataSource.preferredLanguageCode.first()
            )
        }
        initializeFirebase()
        installCrashLogging()
        NotificationHelper.createNotificationChannel(this)
        container.workScheduler.scheduleDailyDoseGeneration()
        container.workScheduler.scheduleMissedDoseCheck()
        container.workScheduler.runImmediateDoseGeneration()
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