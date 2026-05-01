package com.ignaciovalero.saludario.ui.settings

/**
 * Eventos emitidos desde [SettingsViewModel] al completar una exportación
 * de datos. La UI los consume para lanzar el Intent de compartir.
 */
sealed class SettingsExportEvent {
    data class Ready(val shareUriString: String) : SettingsExportEvent()
}
