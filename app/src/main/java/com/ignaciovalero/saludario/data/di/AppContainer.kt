package com.ignaciovalero.saludario.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.work.WorkManager
import com.ignaciovalero.saludario.core.export.DataExporter
import com.ignaciovalero.saludario.data.ads.MonetizationManager
import com.ignaciovalero.saludario.data.local.DatabaseMigrations
import com.ignaciovalero.saludario.data.local.SaludarioDatabase
import com.ignaciovalero.saludario.data.notification.LowStockNotifier
import com.ignaciovalero.saludario.data.preferences.UserPreferencesDataSource
import com.ignaciovalero.saludario.data.repository.HealthRecordRepositoryImpl
import com.ignaciovalero.saludario.data.repository.MedicationLogRepositoryImpl
import com.ignaciovalero.saludario.data.repository.MedicationRepositoryImpl
import com.ignaciovalero.saludario.data.work.AppWorkScheduler
import com.ignaciovalero.saludario.domain.repository.HealthRecordRepository
import com.ignaciovalero.saludario.domain.repository.MedicationLogRepository
import com.ignaciovalero.saludario.domain.repository.MedicationRepository
import com.ignaciovalero.saludario.ui.health.HealthGraphAccessPolicy
import java.io.File

interface AppContainer {
    val healthRecordRepository: HealthRecordRepository
    val medicationRepository: MedicationRepository
    val medicationLogRepository: MedicationLogRepository
    val userPreferencesDataSource: UserPreferencesDataSource
    val monetizationManager: MonetizationManager
    val healthGraphAccessPolicy: HealthGraphAccessPolicy
    val workScheduler: AppWorkScheduler
    val dataExporter: DataExporter
}

class DefaultAppContainer(
    private val context: Context
) : AppContainer {

    private val database: SaludarioDatabase by lazy {
        Room.databaseBuilder(
            context,
            SaludarioDatabase::class.java,
            "saludario_database"
        )
            .addMigrations(*DatabaseMigrations.ALL)
            .fallbackToDestructiveMigration(false)
            .build()
    }

    private val dataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create(
            produceFile = { File(context.filesDir, "datastore/app_preferences.preferences_pb") }
        )
    }

    private val lowStockNotifier: LowStockNotifier by lazy {
        LowStockNotifier(
            appContext = context.applicationContext,
            userPreferencesDataSource = userPreferencesDataSource
        )
    }

    override val medicationRepository: MedicationRepository by lazy {
        MedicationRepositoryImpl(
            medicationDao = database.medicationDao(),
            lowStockNotifier = lowStockNotifier,
            appContext = context.applicationContext
        )
    }

    override val healthRecordRepository: HealthRecordRepository by lazy {
        HealthRecordRepositoryImpl(database.healthRecordDao())
    }

    override val medicationLogRepository: MedicationLogRepository by lazy {
        MedicationLogRepositoryImpl(
            medicationLogDao = database.medicationLogDao(),
            appContext = context.applicationContext
        )
    }

    override val userPreferencesDataSource: UserPreferencesDataSource by lazy {
        UserPreferencesDataSource(dataStore)
    }

    override val monetizationManager: MonetizationManager by lazy {
        MonetizationManager(userPreferencesDataSource)
    }

    override val healthGraphAccessPolicy: HealthGraphAccessPolicy by lazy {
        HealthGraphAccessPolicy(userPreferencesDataSource)
    }

    override val workScheduler: AppWorkScheduler by lazy {
        AppWorkScheduler(WorkManager.getInstance(context))
    }

    override val dataExporter: DataExporter by lazy {
        DataExporter(
            context = context.applicationContext,
            medicationRepository = medicationRepository,
            medicationLogRepository = medicationLogRepository,
            healthRecordRepository = healthRecordRepository
        )
    }
}