# Checklist de Publicacion en Google Play

Ultima revision tecnica: 22 de abril de 2026

## 1. Estado tecnico del proyecto

Comprobaciones realizadas en local:

- OK: :app:compileDebugKotlin
- OK: :app:testDebugUnitTest
- OK: :app:lintRelease
- OK: :app:assembleRelease
- OK: :app:bundleRelease

Cambios tecnicos ya aplicados:

- Corregido acceso a longVersionCode para minSdk 26.
- Corregido acceso a locale en Compose para evitar errores de lint y recomposicion obsoleta.
- Eliminados del manifiesto release los permisos AD_ID y AdServices.
- La release sigue generando APK y AAB correctamente.

## 2. Requisitos pendientes fuera del codigo

Antes de subir a Google Play debes confirmar estos puntos:

- Tener upload key / release keystore real. El proyecto puede caer a firma debug si no se configuran credenciales de release.
- Tener una URL publica HTTPS para la politica de privacidad.
- Tener email de soporte visible en la ficha.
- Tener nombre legal o titular de la app para la politica de privacidad y la ficha.

## 3. Variables de firma release

Opciones soportadas por el proyecto:

- keystore.properties en la raiz con storeFile, storePassword, keyAlias y keyPassword.
- O variables/propiedades RELEASE_STORE_FILE, RELEASE_STORE_PASSWORD, RELEASE_KEY_ALIAS y RELEASE_KEY_PASSWORD.

No subas a Play un artefacto firmado con debug.

## 4. Google Play Console: App content

Revisar y completar:

- Privacy policy: usar la URL publica del documento final.
- App access: marcar que no requiere cuenta, si ese sigue siendo el comportamiento final.
- Ads: marcar que la app no muestra anuncios, si ese sigue siendo el comportamiento final.
- Data safety: revisar Firebase Analytics y Crashlytics antes de responder. No responder como si no hubiera recopilacion tecnica.
- Content rating: completar cuestionario real segun la funcionalidad.
- Target audience: no marcar ninos si la app no esta dirigida especificamente a ellos.
- Health disclaimer: mantener claro que la app es una herramienta de apoyo y no un servicio medico.

## 5. Data safety: puntos a revisar con cuidado

El codigo actual sugiere esta situacion funcional:

- Los datos de medicacion y salud se guardan localmente en el dispositivo.
- La app usa Firebase Analytics y Firebase Crashlytics.
- La app envia al menos un evento tecnico de arranque de aplicacion.
- La app no solicita Advertising ID en la release final.

Antes de enviar el formulario, revisa la documentacion oficial de Firebase y responde de forma coherente con tu configuracion real en produccion.

## 6. QA manual antes de enviar

Probar en al menos un dispositivo real o emulador representativo:

- Primer arranque y onboarding.
- Cambio de idioma ES/EN.
- Permiso de notificaciones en Android 13+.
- Recordatorios, posponer y marcar toma desde notificacion.
- Stock bajo y aviso correspondiente.
- Flujos de Medicamentos, Hoy, Insights y Salud.
- Comportamiento en segundo plano tras reinicio del dispositivo.
- Verificar en un dispositivo real que la pantalla bloqueada muestra solo la vista privada y generica de las notificaciones.

## 7. Store listing

Preparar antes de enviar:

- Titulo y descripcion corta.
- Descripcion completa sin claims medicos impropios.
- Capturas actualizadas en ES y, si vais a listar en ingles, tambien en EN.
- Icono final.
- Categoria adecuada.
- Datos de contacto y URL de politica de privacidad.

## 8. Artefacto recomendado para subir

Subir el AAB de release generado por Gradle, no el APK.

## 9. Criterio de salida

La app puede considerarse lista para envio cuando se cumpla todo esto:

- lintRelease en verde.
- bundleRelease en verde.
- Firma release real configurada.
- Politica de privacidad publicada en URL publica.
- Data safety revisada y coherente con Firebase.
- QA manual final completado.

