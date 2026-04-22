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

fun Context.localizedMedicationDosage(dosage: Double, unitKey: String): String {
    return getString(
        R.string.medication_dosage_format,
        formatMedicationAmount(dosage),
        localizedMedicationUnit(unitKey)
    )
}

fun Context.localizedLocalTime(time: LocalTime): String {
    return time.format(
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
            .withLocale(currentLocale())
    )
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
    val locales = resources.configuration.locales
    return if (locales.isEmpty) Locale.getDefault() else locales[0]
}