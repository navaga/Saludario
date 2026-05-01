# Changelog

Todas las versiones publicadas de Saludario en Google Play.
Formato basado en [Keep a Changelog](https://keepachangelog.com/) y versionado semántico.

## [1.2] - 2026-04-29 (versionCode 3)

### Added — Nuevas funcionalidades para el usuario
- **Exportación de datos (CSV + ZIP)**: nueva tarjeta en Ajustes → "Exportar mis datos".
  Genera un ZIP con `medications.csv`, `medication_logs.csv`, `health_records.csv` y
  `summary.txt`, y lo abre con el selector de compartir de Android (correo, mensajería,
  Drive, etc.) sin exponer ficheros internos. Pensado para enviar al médico o cuidador.
- **Calendario mensual mejorado**: vista de mes con marcadores por día (tomas tomadas,
  perdidas, pospuestas) y navegación rápida entre meses; mejor distinción del día actual
  y de los seleccionados.
- **Widgets más útiles**:
  - "Siguiente toma" con datos al día y mejor legibilidad.
  - "Hoy" con resumen visual de adherencia y contador `tomadas/total`.
  - Pinning de widgets desde Ajustes y aviso si el launcher no lo soporta.

### Improved — Mejoras de UX e interfaz
- Pantallas de **Hoy**, **Insights** y **Salud** revisadas: jerarquía tipográfica,
  contraste y espaciado más coherentes con Material 3.
- **Insights** descartables ahora se pueden restaurar desde Ajustes.
- **Modo oscuro** con interruptor explícito en Ajustes (toggleable accesible).
- **Sonido de notificación** seleccionable y previsualizable desde Ajustes y onboarding.
- **Onboarding** revisado: páginas con jerarquía clara, ilustraciones con icono héroe,
  página de presentación de widgets y toggle de aviso médico/legal.
- **Stock**: diálogo de reposición unificado y reutilizable; chip de estado más claro.
- **Pluralización real** (Android `<plurals>`) en unidades (tableta/tabletas, ml, mg…)
  para español e inglés, en lugar de concatenar "2 tableta".
- **Formato numérico** consistente por idioma en valores de salud (peso, glucosa, etc.).
- Mensajes y *snackbars* de error/éxito reescritos para ser más cortos y útiles.

### Accessibility — Accesibilidad
- Zonas táctiles del onboarding ampliadas a 48 dp (cumple WCAG 2.5.5) sin cambiar
  el aspecto visual de los indicadores.
- Checkbox del aviso legal del onboarding ahora es *toggleable* a nivel de fila:
  TalkBack lee el texto como etiqueta del checkbox y se puede activar pulsando
  cualquier parte de la fila.
- Banner de aviso médico (`HealthDisclaimerBanner`) con `contentDescription` real
  para el icono informativo.
- Switch de modo oscuro con rol semántico correcto (`Role.Switch`) en el contenedor.

### Stability — Estabilidad y robustez
- Eliminado `runBlocking` en `Application.onCreate` que podía provocar ANR al iniciar
  con el `DataStore` lento; sustituido por una caché síncrona en `SharedPreferences`
  para el idioma activo.
- Reportes de errores centralizados en `ErrorReporter` (Logcat + Crashlytics) para
  receivers, workers y ViewModels que antes silenciaban excepciones.
- Casts en `Worker`, `BroadcastReceiver` y *AppWidget* migrados a `as?` con manejo
  defensivo, evitando `ClassCastException` ante estados inesperados del sistema.
- `NotificationActionReceiver`, `MedicationReminderWorker` y `WidgetDataLoader`
  reforzados frente a procesos reciclados por Android.
- Migraciones Room con esquemas exportados en `app/schemas/` para depuración futura.

### Internal — Cambios técnicos
- Utilidades comunes extraídas:
  - `core/export/CsvBuilder` y `CsvSerializer` (RFC 4180).
  - `core/export/DataExporter` con `FileProvider` y URI seguro.
  - `core/formatting/HealthValueFormatter` (formatos plain/compact por locale).
  - `core/localization/DecimalParsing` y `MedicationLocalization`.
  - `core/logging/ErrorReporter`.
  - `ui/common/RestockDialog` reutilizable.
- Nueva entrada `<provider FileProvider>` en `AndroidManifest.xml` con paths
  `@xml/file_provider_paths`.
- Tests unitarios añadidos: `CsvBuilderTest`, `CsvSerializerTest` y casos para
  formatters y parsers.
- KSP habilitado con `android.disallowKotlinSourceSets=false`.

### Notes — Notas
- La app sigue siendo **local-first**: ningún dato se envía a servidores propios.
  La exportación se queda en tu dispositivo hasta que tú decidas compartirla.
- El consentimiento publicitario (UMP) y la política de privacidad permanecen
  accesibles desde Ajustes.

## [1.1] - 2026-04-XX (versionCode 2)
- Versión base previa a esta tanda de mejoras (publicidad en gráficas, mejoras de
  estabilidad y de iconografía, ajustes legales).

## [1.0] - 2025
- Lanzamiento inicial: medicación, recordatorios, stock, registros de salud, widgets,
  onboarding, modo simple, multilenguaje (es/en) y monetización opt-in con UMP.
