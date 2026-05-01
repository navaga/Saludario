package com.ignaciovalero.saludario.core.export

/**
 * Construye filas CSV escapando correctamente comas, comillas y saltos de
 * línea (RFC 4180). Mantiene un flujo `String` para permitir streaming a un
 * `OutputStream` o a un test sin tocar disco.
 *
 * No es una librería completa: cubre los casos que necesita la exportación
 * de Saludario (datos planos sin tipos binarios) y nada más.
 */
internal object CsvBuilder {

    private const val SEPARATOR = ","
    private const val LINE_END = "\r\n"

    /**
     * Devuelve una línea CSV terminada con CRLF. Cualquier valor `null` se
     * serializa como cadena vacía. Los valores que contengan `,`, `"`, `\r`
     * o `\n` se escapan entre comillas duplicando las comillas internas.
     */
    fun row(vararg values: Any?): String =
        values.joinToString(separator = SEPARATOR, postfix = LINE_END) { escape(it) }

    fun row(values: List<Any?>): String =
        values.joinToString(separator = SEPARATOR, postfix = LINE_END) { escape(it) }

    private fun escape(raw: Any?): String {
        val value = raw?.toString().orEmpty()
        val needsQuoting = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        return if (needsQuoting) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
