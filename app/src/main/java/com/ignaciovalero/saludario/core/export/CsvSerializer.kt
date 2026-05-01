package com.ignaciovalero.saludario.core.export

import com.ignaciovalero.saludario.data.local.entity.HealthRecord
import com.ignaciovalero.saludario.data.local.entity.MedicationEntity
import com.ignaciovalero.saludario.data.local.entity.MedicationLogEntity
import java.time.format.DateTimeFormatter

/**
 * Genera el contenido CSV (incluida la cabecera) para cada tabla exportable.
 * Funciones puras y deterministas: no acceden a contexto, recursos ni reloj
 * del sistema, lo que las hace fáciles de testear.
 *
 * Formato: comas como separador, fechas en ISO-8601, listas (`times`,
 * `specificDays`) serializadas como cadenas separadas por `;` (no `,` para
 * que no rompan el CSV ni necesiten quoting innecesario).
 */
internal object CsvSerializer {

    private const val LIST_SEPARATOR = ";"
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_TIME
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun medicationsCsv(medications: List<MedicationEntity>): String = buildString {
        append(
            CsvBuilder.row(
                "id",
                "name",
                "dosage",
                "unit",
                "schedule_type",
                "times",
                "start_date",
                "end_date",
                "specific_days",
                "interval_hours",
                "stock_total",
                "stock_remaining",
                "low_stock_threshold"
            )
        )
        medications.forEach { med ->
            append(
                CsvBuilder.row(
                    med.id,
                    med.name,
                    med.dosage,
                    med.unit,
                    med.scheduleType.name,
                    med.times.joinToString(LIST_SEPARATOR) { it.format(timeFormatter) },
                    med.startDate.format(dateFormatter),
                    med.endDate?.format(dateFormatter).orEmpty(),
                    med.specificDays?.joinToString(LIST_SEPARATOR) { it.name }.orEmpty(),
                    med.intervalHours?.toString().orEmpty(),
                    med.stockTotal,
                    med.stockRemaining,
                    med.lowStockThreshold
                )
            )
        }
    }

    fun medicationLogsCsv(logs: List<MedicationLogEntity>): String = buildString {
        append(
            CsvBuilder.row(
                "id",
                "medication_id",
                "scheduled_time",
                "taken_time",
                "status",
                "postponed_until"
            )
        )
        logs.forEach { log ->
            append(
                CsvBuilder.row(
                    log.id,
                    log.medicationId,
                    log.scheduledTime.format(dateTimeFormatter),
                    log.takenTime?.format(dateTimeFormatter).orEmpty(),
                    log.status.name,
                    log.postponedUntil?.format(dateTimeFormatter).orEmpty()
                )
            )
        }
    }

    fun healthRecordsCsv(records: List<HealthRecord>): String = buildString {
        append(
            CsvBuilder.row(
                "id",
                "type",
                "value",
                "secondary_value",
                "unit",
                "recorded_at",
                "notes"
            )
        )
        records.forEach { record ->
            append(
                CsvBuilder.row(
                    record.id,
                    record.type.name,
                    record.value,
                    record.secondaryValue?.toString().orEmpty(),
                    record.unit,
                    record.recordedAt.format(dateTimeFormatter),
                    record.notes.orEmpty()
                )
            )
        }
    }
}
