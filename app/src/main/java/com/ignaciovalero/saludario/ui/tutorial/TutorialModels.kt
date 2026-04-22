package com.ignaciovalero.saludario.ui.tutorial

import androidx.annotation.StringRes
import com.ignaciovalero.saludario.R

enum class TutorialScreen(
    val key: String,
    @param:StringRes val titleRes: Int,
    @param:StringRes val messageRes: Int
) {
    TODAY(
        key = "today",
        titleRes = R.string.tutorial_today_title,
        messageRes = R.string.tutorial_today_message
    ),
    MEDICATIONS(
        key = "medications",
        titleRes = R.string.tutorial_medications_title,
        messageRes = R.string.tutorial_medications_message
    ),
    ADD(
        key = "add",
        titleRes = R.string.tutorial_add_title,
        messageRes = R.string.tutorial_add_message
    ),
    HEALTH(
        key = "health",
        titleRes = R.string.tutorial_health_title,
        messageRes = R.string.tutorial_health_message
    ),
    INSIGHTS(
        key = "insights",
        titleRes = R.string.tutorial_insights_title,
        messageRes = R.string.tutorial_insights_message
    ),
    SIMPLE_MODE_HINT(
        key = "simple_mode_hint",
        titleRes = R.string.tutorial_today_title,
        messageRes = R.string.today_simple_mode_tooltip
    ),
    HEALTH_DETAIL(
        key = "health_detail",
        titleRes = R.string.tutorial_health_detail_title,
        messageRes = R.string.tutorial_health_detail_message
    )
}
