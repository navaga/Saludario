package com.ignaciovalero.saludario.data.ads

enum class AdConsentStatus {
    UNKNOWN,
    REQUIRED,
    NOT_REQUIRED,
    OBTAINED;

    companion object {
        fun fromStorage(value: String?): AdConsentStatus {
            return entries.firstOrNull { it.name == value } ?: UNKNOWN
        }
    }
}