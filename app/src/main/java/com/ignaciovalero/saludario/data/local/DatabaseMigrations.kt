package com.ignaciovalero.saludario.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migraciones del esquema de la base de datos `saludario_database`.
 *
 * Centralizadas aquí para mantener `AppContainer` enfocado en la composición
 * de dependencias. Cada migración debe ser referencialmente transparente:
 * no debe usar estado externo y debe ser idempotente cuando sea posible.
 */
object DatabaseMigrations {

    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS health_records")
        }
    }

    val MIGRATION_2_3: Migration = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE medications ADD COLUMN specific_days TEXT")
            db.execSQL("ALTER TABLE medications ADD COLUMN interval_hours INTEGER")
        }
    }

    val MIGRATION_3_4: Migration = object : Migration(3, 4) {
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
    }

    val MIGRATION_4_5: Migration = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE medications ADD COLUMN stock_total REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE medications ADD COLUMN stock_remaining REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE medications ADD COLUMN low_stock_threshold REAL NOT NULL DEFAULT 0.0")
        }
    }

    val MIGRATION_5_6: Migration = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // A\u00f1ade soporte para posponer dosis: se guarda hasta cu\u00e1ndo se ha
            // pospuesto y un nuevo valor en el enum `MedicationStatus.POSTPONED`.
            db.execSQL("ALTER TABLE medication_logs ADD COLUMN postponed_until TEXT")
        }
    }

    /** Lista ordenada de todas las migraciones disponibles. */
    val ALL: Array<Migration> = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6
    )

    private fun tableExists(db: SupportSQLiteDatabase, tableName: String): Boolean {
        db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
            arrayOf(tableName)
        ).use { cursor ->
            return cursor.moveToFirst()
        }
    }

    private fun tableHasColumn(
        db: SupportSQLiteDatabase,
        tableName: String,
        columnName: String
    ): Boolean {
        db.query("PRAGMA table_info($tableName)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                if (nameIndex >= 0 && cursor.getString(nameIndex) == columnName) return true
            }
        }
        return false
    }
}
