# Afinador (app Android)
Afinador cromático sencillo: nota, desviación en cents, trompeta en Si♭ y violín. Español por defecto + inglés.
- Entorno: `source /datos/android-sdk/env.sh` (se instala con `node /datos/proyectos/n8n/apps-moviles/motor.mjs toolchain`, sin root).
- Compilar y probar: `./gradlew --no-daemon testDebugUnitTest assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`.
- Linter de lecciones (0 tokens): `node /datos/proyectos/n8n/apps-moviles/motor.mjs lint .` — son los errores ya vividos en otras apps; no los ignores.
- Probar en dispositivo y sacar capturas: `node /datos/proyectos/n8n/apps-moviles/motor.mjs smoke .` (adb).
- Publicar: `./publicar.sh X.Y` (necesita `keystore.properties`, `.clave-respaldada` y `gh`). La clave NO está en el repo: si se pierde, nadie puede actualizar.
- Un solo repo (remoto `origin`); no mantengas copias de publicación aparte.
- Textos: `res/values/strings_*.xml` (es) y `res/values-en/strings_*.xml` (en), mismas claves (`StringsTest`). Nada de literales en Kotlin.
- Kotlin sin dependencias externas; UI por código con `Ui.kt` y `Ui.dialog { }`; SQLite nativo. Toda Activity/Service: `attachBaseContext` con `Lang.wrap`.
- Trabajo en segundo plano: guarda el plan en disco y respáldalo con una alarma exacta (ver lección L-REL-1).
- Especificación y decisiones: `docs/ESPECIFICACION.md`.
