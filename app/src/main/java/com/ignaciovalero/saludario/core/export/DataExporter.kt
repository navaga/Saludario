package com.ignaciovalero.saludario.core.export

import android.content.Context
import androidx.core.content.FileProvider
import com.ignaciovalero.saludario.data.local.entity.HealthRecord
import com.ignaciovalero.saludario.data.local.entity.MedicationEntity
import com.ignaciovalero.saludario.data.local.entity.MedicationLogEntity
import com.ignaciovalero.saludario.domain.repository.HealthRecordRepository
import com.ignaciovalero.saludario.domain.repository.MedicationLogRepository
import com.ignaciovalero.saludario.domain.repository.MedicationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Resultado de una exportación lista para compartir vía
 * `Intent.ACTION_SEND`: incluye el `Uri` autorizado por
 * [FileProvider] y un resumen de cuántos registros se incluyeron.
 */
data class DataExportResult(
    val shareUri: android.net.Uri,
    val medicationCount: Int,
    val logCount: Int,
    val healthRecordCount: Int
)

/**
 * Genera un fichero ZIP con tres CSV (medicamentos, tomas, salud) en la
 * carpeta `cache/exports/` y devuelve un `Uri` listo para compartir mediante
 * el `FileProvider` declarado en el manifiesto.
 *
 * Trabaja en `Dispatchers.IO` y consume las fuentes leyendo el primer
 * snapshot de cada `Flow` con `first()`, evitando suscripciones largas o
 * recolectar de forma indefinida.
 */
class DataExporter(
    private val context: Context,
    private val medicationRepository: MedicationRepository,
    private val medicationLogRepository: MedicationLogRepository,
    private val healthRecordRepository: HealthRecordRepository
) {

    suspend fun exportToZip(): DataExportResult = withContext(Dispatchers.IO) {
        val medications = medicationRepository.observeAll().first()
        val logs = medicationLogRepository.observeAll().first()
        val healthRecords = healthRecordRepository.observeAll().first()

        val exportDir = File(context.cacheDir, EXPORT_DIR).apply {
            if (!exists()) mkdirs()
        }
        // Limpia ficheros antiguos para que el cache no crezca indefinido.
        exportDir.listFiles()?.forEach { it.delete() }

        val timestamp = LocalDateTime.now().format(FILENAME_FORMATTER)
        val zipFile = File(exportDir, "saludario-export-$timestamp.zip")

        ZipOutputStream(zipFile.outputStream()).use { zip ->
            zip.writeEntry("medications.csv", CsvSerializer.medicationsCsv(medications))
            zip.writeEntry("medication_logs.csv", CsvSerializer.medicationLogsCsv(logs))
            zip.writeEntry("health_records.csv", CsvSerializer.healthRecordsCsv(healthRecords))
            zip.writeEntry(
                "summary.txt",
                buildSummary(medications, logs, healthRecords, timestamp)
            )
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            zipFile
        )

        DataExportResult(
            shareUri = uri,
            medicationCount = medications.size,
            logCount = logs.size,
            healthRecordCount = healthRecords.size
        )
    }

    private fun ZipOutputStream.writeEntry(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun buildSummary(
        medications: List<MedicationEntity>,
        logs: List<MedicationLogEntity>,
        healthRecords: List<HealthRecord>,
        timestamp: String
    ): String = buildString {
        appendLine("Saludario data export")
        appendLine("Generated at: $timestamp")
        appendLine()
        appendLine("medications.csv: ${medications.size} rows")
        appendLine("medication_logs.csv: ${logs.size} rows")
        appendLine("health_records.csv: ${healthRecords.size} rows")
        appendLine()
        appendLine("All datetimes are in ISO-8601 local time.")
        appendLine("List fields (times, specific_days) are joined with ';'.")
    }

    companion object {
        private const val EXPORT_DIR = "exports"
        private val FILENAME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
    }
}
