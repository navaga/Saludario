# Firebase Setup Paso a Paso (Saludario)

Esta guia deja Firebase operativo para analitica y crash reporting en Android.

## 1) Crear proyecto en Firebase

1. Ve a Firebase Console y crea un proyecto nuevo.
2. Usa un nombre claro, por ejemplo: Saludario-Prod.
3. Activa Google Analytics en el asistente inicial.

## 2) Registrar app Android

1. Dentro del proyecto, anade una app Android.
2. Package name exacto: com.ignaciovalero.saludario.
3. App nickname opcional: Saludario Android.
4. Descarga el archivo google-services.json.
5. Copia el archivo en app/google-services.json.

Nota: El proyecto ya esta preparado para activar automaticamente Google Services y Crashlytics cuando existe ese archivo.

## 3) Verificar integracion local

1. En la raiz del proyecto ejecuta:
   - .\\gradlew.bat :app:assembleDebug
2. Debe compilar sin errores.
3. Al arrancar la app, Firebase se inicializa automaticamente.

## 4) Activar Crashlytics correctamente

1. En Firebase Console abre Crashlytics.
2. Sigue el asistente de activacion para Android.
3. Ejecuta una build release local para subir mapeos y validar plugin:
   - .\\gradlew.bat :app:assembleRelease

## 5) Generar un crash de prueba

1. Instala la app en un dispositivo real o emulador con Google Play Services.
2. Abre la pestaña Salud.
3. En debug veras la tarjeta "Diagnóstico Firebase (debug)".
4. Pulsa "Enviar no fatal" para validar un evento no fatal en Crashlytics.
5. Pulsa "Forzar crash" para disparar un crash controlado.
6. Reabre la app.
7. Espera unos minutos y revisa Crashlytics en consola.

## 6) Firmado release para entorno real

Opciones soportadas por el proyecto:

- Archivo keystore.properties en la raiz con:
  - storeFile=...
  - storePassword=...
  - keyAlias=...
  - keyPassword=...

- O variables/propiedades:
  - RELEASE_STORE_FILE
  - RELEASE_STORE_PASSWORD
  - RELEASE_KEY_ALIAS
  - RELEASE_KEY_PASSWORD

Sin estas credenciales, release usa firma debug para no bloquear desarrollo.

## 7) Configuracion recomendada en Firebase

1. Analytics:
   - Crear eventos sugeridos: medication_taken, medication_missed, health_record_saved.
2. Crashlytics:
   - Configurar alertas por email para nuevos crashes y regresiones.
3. Proyecto:
   - Roles minimos para equipo (Viewer/Editor segun necesidad).

## 8) Pre-check antes de Google Play

1. Build y tests en verde:
   - .\\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest :app:assembleRelease :app:bundleRelease
2. Crashlytics recibiendo eventos en release.
3. Politica de privacidad y Data Safety alineadas con uso de Firebase Analytics/Crashlytics.

## Estado de codigo ya aplicado

- Dependencias y plugins de Firebase agregados.
- Inicializacion de Firebase en Application.
- Crashlytics con:
  - Collection habilitada solo fuera de debug.
  - Claves de contexto: version, build type.
- Evento app_start enviado a Analytics al iniciar.
