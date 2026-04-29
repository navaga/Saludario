package com.ignaciovalero.saludario.core.formatting

import java.util.Locale

/**
 * Formatea un valor numérico de salud preservando su forma "natural":
 *
 * - Si el valor es un entero exacto (p. ej. `120.0`) se muestra sin decimales (`"120"`).
 * - En caso contrario se devuelve la representación completa de [Double.toString]
 *   (`"12.5"`, `"36.789"`).
 *
 * Útil para mensajes textuales (validaciones de rango, summary numérico,
 * ítems de listado) donde queremos mantener intactos los decimales que el
 * usuario o el sistema introdujo.
 */
fun Double.formatHealthValuePlain(): String =
    if (this % 1.0 == 0.0) toInt().toString() else toString()

/**
 * Variante "compacta" pensada para gráficas y etiquetas con espacio
 * limitado:
 *
 * - Enteros se muestran sin decimales.
 * - Valores con parte decimal se redondean a 1 decimal en el [Locale]
 *   indicado (por defecto el del sistema).
 *
 * Mantiene el comportamiento original de la pantalla de evolución.
 */
fun Double.formatHealthValueCompact(locale: Locale = Locale.getDefault()): String = when {
    this % 1.0 == 0.0 -> toInt().toString()
    else -> String.format(locale, "%.1f", this)
}
