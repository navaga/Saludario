# Guia Practica de Data Safety para Google Play

Ultima revision: 23 de abril de 2026

## 1. Alcance de esta guia

Esta guia esta basada en el estado actual del codigo de Saludario.

Situacion observada en el repo:

- La app usa Firebase Crashlytics.
- La app usa Firebase Analytics.
- La app registra al menos un evento tecnico de arranque de aplicacion.
- La app integra Google Mobile Ads SDK y User Messaging Platform (UMP) con IDs de prueba para desarrollo del flujo de anuncios y consentimiento.
- La app no usa autenticacion, backend propio, API REST propia ni sincronizacion funcional de medicacion o registros de salud.
- Los datos funcionales principales de medicacion y salud se guardan localmente en el dispositivo.
- La release final ya no incluye permisos AD_ID ni AdServices en el manifiesto fusionado.

## 2. Lo que NO deberias declarar como datos recopilados remotamente por la app

Con el codigo actual, estos datos funcionales no se observan enviados a un backend propio desde la app:

- Nombre del medicamento.
- Dosis y unidad.
- Horarios de toma.
- Stock disponible.
- Registros de salud.
- Preferencias locales de tutoriales, idioma y onboarding.

Eso no elimina la necesidad de revisar Firebase, pero evita sobredescribir la app como si subiera historiales clinicos a un servidor propio.

## 3. SDK relevantes para la declaracion

SDK directos observados en el proyecto:

- Firebase Crashlytics.
- Firebase Analytics.
- Google Mobile Ads SDK.
- Google User Messaging Platform (UMP).

SDK transitivos o relacionados que Firebase pide tener en cuenta en su documentacion:

- Firebase Installations.
- Firebase Sessions.

Referencia oficial:

- https://firebase.google.com/docs/android/play-data-disclosure
- https://firebase.google.com/support/privacy

## 4. Respuesta base recomendada para orientar el formulario

### 4.1 Datos recopilados

Con la configuracion actual, lo prudente es revisar al menos estas categorias en Google Play Console:

- App info and performance:
  - Crash logs.
  - Diagnostics.

- App activity:
  - Actividad de la app o interacciones basicas, debido a Firebase Analytics y sus eventos automaticos / tecnicos.

- Device or other IDs:
  - Identificadores por instalacion o equivalentes usados por Firebase Installations / Crashlytics / Analytics.

- Si la version publicada activa anuncios reales:
  - Revisar la divulgacion especifica de Google Mobile Ads SDK segun el modo de servicio real del anuncio y el consentimiento obtenido con UMP.

## 4.2 Datos compartidos

Base sugerida para revisar con criterio legal:

- En general, con el uso actual del proyecto, la respuesta suele orientarse a “collected” pero no “shared” como venta o cesion a terceros para fines propios.
- Aun asi, la respuesta final debe revisarse segun la definicion exacta de Google Play y vuestro uso real de servicios vinculados en Firebase/Google.

## 4.3 Fines del tratamiento

Los fines mas coherentes con el estado actual del repo son:

- App functionality: para recordatorios, estabilidad y operativa tecnica de la app.
- Analytics: por el uso de Firebase Analytics.
- Advertising: solo si la version publicada sustituye los IDs de prueba y habilita anuncios reales con Google Mobile Ads.

No marques publicidad personalizada por defecto si no puedes demostrar que el modo real de servicio y el consentimiento implementado se corresponden exactamente con esa declaracion.

## 4.4 Encriptacion en transito

Firebase indica que cifra los datos en transito con HTTPS. Esto apoya responder que los datos recopilados por estos SDK se transmiten cifrados.

## 4.5 Eliminacion de datos

Aqui hay que ser estricto:

- No marques alegremente que la persona usuaria puede solicitar borrado si no vais a ofrecer un proceso real de soporte o gestion de eliminacion.
- Borrar datos locales del dispositivo no equivale automaticamente a borrar todos los datos tecnicos ya enviados a Firebase.
- Si vais a ofrecer borrado bajo solicitud, documentad ese proceso en la politica de privacidad y en soporte.

## 5. Lo que puedes contestar con bastante confianza hoy

- La app no requiere cuenta.
- La implementacion actual usa IDs de prueba para anuncios; antes de publicar con monetizacion real deben sustituirse por IDs reales y revisarse de nuevo las declaraciones.
- La release final sigue removiendo Advertising ID del manifiesto de la app.
- La app usa Firebase Crashlytics, Firebase Analytics, Google Mobile Ads SDK y UMP.
- Los datos de medicacion y salud del uso principal permanecen en almacenamiento local del dispositivo.

## 6. Lo que debes revisar manualmente antes de enviar el formulario

- Si Google Analytics esta enlazado con productos o configuraciones de publicidad fuera del repo.
- Si en Firebase Console habeis activado funciones adicionales no visibles en este codigo.
- Si vais a ofrecer un canal real para ejercer peticiones de privacidad o borrado.
- Si la politica de privacidad publicada coincide exactamente con el comportamiento final de la app publicada.

## 7. Resumen operativo

Si tuviera que resumir el punto de partida actual para Play Console, seria este:

- Marca que hay recopilacion tecnica vinculada a Firebase.
- No describas la app como si subiera historiales de salud a un backend propio, porque el repo no muestra eso.
- Si publicas una version con anuncios reales, marca que la app contiene anuncios y revisa la divulgacion oficial de Google Mobile Ads SDK antes de enviar el formulario.
- Mantén una revision separada de Advertising ID: el manifiesto actual lo elimina, pero debes comprobar el artefacto final publicado.
- Revisa con cuidado las categorias Crash logs, Diagnostics, App activity y Device or other IDs.

## 8. Importante

Esta guia reduce mucho el riesgo de contestar mal el formulario, pero no sustituye la decision final del titular de la app. Google Play exige que la declaracion refleje la configuracion real publicada, incluyendo servicios activados desde consola.
