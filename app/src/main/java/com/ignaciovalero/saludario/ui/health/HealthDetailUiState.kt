package com.ignaciovalero.saludario.ui.health

import androidx.annotation.StringRes
import com.ignaciovalero.saludario.data.local.entity.HealthRecord

data class HealthDetailUiState(
    val primaryValue: String = "",
    val secondaryValue: String = "",
    val unit: String = "",
    val notes: String = "",
    val records: List<HealthRecord> = emptyList(),
    @param:StringRes val primaryError: Int? = null,
    val primaryErrorArgs: List<String>? = null,
    @param:StringRes val secondaryError: Int? = null,
    val secondaryErrorArgs: List<String>? = null,
    @param:StringRes val unitError: Int? = null
)
