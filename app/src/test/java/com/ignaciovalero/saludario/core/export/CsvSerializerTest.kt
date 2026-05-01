package com.ignaciovalero.saludario.core.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests "humo" del [CsvSerializer]. Validan que los CSVs generados con listas
 * vacías contienen exactamente la cabecera esperada y que el separador de
 * filas es CRLF. La serialización detallada de entidades se cubre de forma
 * implícita por los tests de [CsvBuilder].
 */
class CsvSerializerTest {

    @Test
    fun `medications csv with empty list contains only header`() {
        val csv = CsvSerializer.medicationsCsv(emptyList())
        assertEquals(
            "id,name,dosage,unit,schedule_type,times,start_date,end_date,specific_days," +
                "interval_hours,stock_total,stock_remaining,low_stock_threshold\r\n",
            csv
        )
    }

    @Test
    fun `medication logs csv with empty list contains only header`() {
        val csv = CsvSerializer.medicationLogsCsv(emptyList())
        assertEquals(
            "id,medication_id,scheduled_time,taken_time,status,postponed_until\r\n",
            csv
        )
    }

    @Test
    fun `health records csv with empty list contains only header`() {
        val csv = CsvSerializer.healthRecordsCsv(emptyList())
        assertEquals(
            "id,type,value,secondary_value,unit,recorded_at,notes\r\n",
            csv
        )
    }

    @Test
    fun `headers end with CRLF`() {
        val outputs = listOf(
            CsvSerializer.medicationsCsv(emptyList()),
            CsvSerializer.medicationLogsCsv(emptyList()),
            CsvSerializer.healthRecordsCsv(emptyList())
        )
        outputs.forEach { assertTrue("Falta CRLF en: $it", it.endsWith("\r\n")) }
    }
}
