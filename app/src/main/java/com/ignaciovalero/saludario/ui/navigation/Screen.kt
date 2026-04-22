package com.ignaciovalero.saludario.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.ui.graphics.vector.ImageVector
import com.ignaciovalero.saludario.R

sealed class Screen(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector
) {
    data object Today : Screen("today", R.string.nav_today, Icons.Default.Today)
    data object MedicationList : Screen("medications", R.string.nav_medications, Icons.AutoMirrored.Filled.List)
    data object AddMedication : Screen("add_medication", R.string.nav_add, Icons.Default.Add)
    data object Insights : Screen("insights", R.string.nav_insights, Icons.Default.Insights)
    data object Health : Screen("health", R.string.nav_health, Icons.Default.MonitorHeart)
    data object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)
    data object PrivacyPolicy : Screen("privacy_policy", R.string.privacy_policy_title, Icons.Default.Settings)
    data object HealthDetail : Screen("health_detail/{type}", R.string.nav_health, Icons.Default.MonitorHeart) {
        fun createRoute(type: String) = "health_detail/$type"
    }
    data object HealthGraph : Screen("health_graph/{type}", R.string.nav_health, Icons.Default.MonitorHeart) {
        fun createRoute(type: String) = "health_graph/$type"
    }
    data object EditMedication : Screen("edit_medication/{medicationId}", R.string.edit_medication_title, Icons.Default.Edit) {
        fun createRoute(medicationId: Long) = "edit_medication/$medicationId"
    }
}

val bottomBarScreens = listOf(Screen.Today, Screen.MedicationList, Screen.Insights, Screen.Health)
