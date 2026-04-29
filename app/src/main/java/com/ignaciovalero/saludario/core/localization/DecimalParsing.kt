package com.ignaciovalero.saludario.core.localization

/**
 * Convierte una cadena introducida por el usuario en un [Double], aceptando
 * tanto coma como punto como separador decimal. Devuelve `null` si la entrada
 * está vacía o no es numérica.
 */
fun String.parseDecimalOrNull(): Double? = trim()
    .takeIf { it.isNotEmpty() }
    ?.replace(',', '.')
    ?.toDoubleOrNull()

/**
 * Variante de [parseDecimalOrNull] que devuelve `0.0` para entradas vacías o
 * no numéricas. Útil cuando el campo asociado se considera opcional.
 */
fun String.parseDecimalOrZero(): Double = parseDecimalOrNull() ?: 0.0
