package com.ignaciovalero.saludario.core.localization

import android.content.Context
import com.ignaciovalero.saludario.R
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

fun Context.localizedMedicationUnit(unitKey: String): String {
    val resId = when (unitKey.trim().lowercase(Locale.ROOT)) {
        "tableta" -> R.string.unit_tablet
        "cápsula" -> R.string.unit_capsule
        "ml" -> R.string.unit_ml
        "gotas" -> R.string.unit_drops
        "mg" -> R.string.unit_mg
        else -> null
    }

    return resId?.let(::getString) ?: unitKey
}

/**
 * Devuelve la unidad de medicación localizada y pluralizada según [quantity]
 * (p. ej. "1 tableta" vs "2 tabletas"). Si la clave de unidad no es una de
 * las canónicas reconocidas, devuelve `null` para que el llamante decida si
 * cae a la versión singular o muestra el `unitKey` crudo.
 */
fun Context.localizedMedicationUnitPluralized(unitKey: String, quantity: Double): String? {
    val pluralRes = when (unitKey.trim().lowercase(Locale.ROOT)) {
        "tableta" -> R.plurals.unit_tablet_plural
        "cápsula" -> R.plurals.unit_capsule_plural
        "ml" -> R.plurals.unit_ml_plural
        "gotas" -> R.plurals.unit_drops_plural
        "mg" -> R.plurals.unit_mg_plural
        else -> return null
    }

    val pluralQuantity = if (quantity == 1.0) 1 else 2
    return runCatching { resources.getQuantityString(pluralRes, pluralQuantity) }.getOrNull()
}

fun Context.localizedMedicationDosage(dosage: Double, unitKey: String): String {
    val unit = localizedMedicationUnitPluralized(unitKey, dosage)
        ?: localizedMedicationUnit(unitKey)
    return getString(
        R.string.medication_dosage_format,
        formatMedicationAmount(dosage),
        unit
    )
}

fun Context.localizedLocalTime(time: LocalTime): String {
    return runCatching {
        time.format(
            DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                .withLocale(currentLocale())
        )
    }.getOrElse {
        time.toString()
    }
}

fun Context.localizedDurationMinutes(totalMinutes: Long): String {
    val safeMinutes = totalMinutes.coerceAtLeast(0L)
    val hours = (safeMinutes / 60L).toInt()
    val minutes = (safeMinutes % 60L).toInt()

    return runCatching {
        when {
            hours == 0 -> resources.getQuantityString(R.plurals.duration_minutes, minutes, minutes)
            minutes == 0 -> resources.getQuantityString(R.plurals.duration_hours, hours, hours)
            else -> {
                val hoursText = resources.getQuantityString(R.plurals.duration_hours, hours, hours)
                val minutesText = resources.getQuantityString(R.plurals.duration_minutes, minutes, minutes)
                getString(R.string.duration_hours_minutes, hoursText, minutesText)
            }
        }
    }.getOrElse {
        when {
            hours == 0 -> "$minutes min"
            minutes == 0 -> "$hours h"
            else -> "$hours h $minutes min"
        }
    }
}

fun Context.localizedScheduledTime(rawDateTime: String): String {
    return runCatching {
        localizedLocalTime(LocalDateTime.parse(rawDateTime).toLocalTime())
    }.getOrElse {
        rawDateTime
    }
}

private fun Context.formatMedicationAmount(value: Double): String {
    return NumberFormat.getNumberInstance(currentLocale()).run {
        maximumFractionDigits = 2
        minimumFractionDigits = if (value % 1.0 == 0.0) 1 else 0
        isGroupingUsed = false
        format(value)
    }
}

private fun Context.currentLocale(): Locale {
    return runCatching {
        val locales = resources.configuration.locales
        if (locales.isEmpty) Locale.getDefault() else locales[0]
    }.getOrDefault(Locale.getDefault())
}