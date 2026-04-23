package com.ignaciovalero.saludario.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.WorkManager
import com.ignaciovalero.saludario.data.ads.MonetizationManager
import com.ignaciovalero.saludario.data.local.SaludarioDatabase
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
}

class DefaultAppContainer(
    private val context: Context
) : AppContainer {

    private val migration1to2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS health_records")
        }
    }

    private val migration2to3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE medications ADD COLUMN specific_days TEXT")
            db.execSQL("ALTER TABLE medications ADD COLUMN interval_hours INTEGER")
        }
    }

    private val migration3to4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val hasHealthRecords = tableExists(db, "health_records")

            if (!hasHealthRecords) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS health_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        type TEXT NOT NULL,
                        value REAL NOT NULL,
                        secondary_value REAL,
                        unit TEXT NOT NULL,
                        recorded_at TEXT NOT NULL,
                        notes TEXT
                    )
                    """.trimIndent()
                )
                return
            }

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS health_records_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    type TEXT NOT NULL,
                    value REAL NOT NULL,
                    secondary_value REAL,
                    unit TEXT NOT NULL,
                    recorded_at TEXT NOT NULL,
                    notes TEXT
                )
                """.trimIndent()
            )

            val hasLegacyRecordDate = tableHasColumn(db, "health_records", "record_date")
            val hasRecordedAt = tableHasColumn(db, "health_records", "recorded_at")
            val hasSecondary = tableHasColumn(db, "health_records", "secondary_value")
            val hasUnit = tableHasColumn(db, "health_records", "unit")
            val hasNotes = tableHasColumn(db, "health_records", "notes")

            val secondaryExpr = if (hasSecondary) "secondary_value" else "NULL"
            val unitExpr = if (hasUnit) "unit" else "''"
            val notesExpr = if (hasNotes) "notes" else "NULL"
            val dateExpr = when {
                hasRecordedAt -> "recorded_at"
                hasLegacyRecordDate -> "record_date"
                else -> "CURRENT_TIMESTAMP"
            }

            db.execSQL(
                """
                INSERT INTO health_records_new (id, type, value, secondary_value, unit, recorded_at, notes)
                SELECT id, type, value, $secondaryExpr, $unitExpr, $dateExpr, $notesExpr
                FROM health_records
                """.trimIndent()
            )

            db.execSQL("DROP TABLE health_records")
            db.execSQL("ALTER TABLE health_records_new RENAME TO health_records")
        }

        private fun tableExists(db: SupportSQLiteDatabase, tableName: String): Boolean {
            db.query("SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(tableName)).use { cursor ->
                return cursor.moveToFirst()
            }
        }

        private fun tableHasColumn(db: SupportSQLiteDatabase, tableName: String, columnName: String): Boolean {
            db.query("PRAGMA table_info($tableName)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (nameIndex >= 0 && cursor.getString(nameIndex) == columnName) return true
                }
            }
            return false
        }
    }

    private val migration4to5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE medications ADD COLUMN stock_total REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE medications ADD COLUMN stock_remaining REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE medications ADD COLUMN low_stock_threshold REAL NOT NULL DEFAULT 0.0")
        }
    }

    private val database: SaludarioDatabase by lazy {
        Room.databaseBuilder(
            context,
            SaludarioDatabase::class.java,
            "saludario_database"
        )
            .addMigrations(migration1to2, migration2to3, migration3to4, migration4to5)
            .fallbackToDestructiveMigration(false)
            .build()
    }

    private val dataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create(
            produceFile = { File(context.filesDir, "datastore/app_preferences.preferences_pb") }
        )
    }

    override val medicationRepository: MedicationRepository by lazy {
        MedicationRepositoryImpl(
            medicationDao = database.medicationDao(),
            appContext = context.applicationContext,
            userPreferencesDataSource = userPreferencesDataSource
        )
    }

    override val healthRecordRepository: HealthRecordRepository by lazy {
        HealthRecordRepositoryImpl(database.healthRecordDao())
    }

    override val medicationLogRepository: MedicationLogRepository by lazy {
        MedicationLogRepositoryImpl(database.medicationLogDao())
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
}