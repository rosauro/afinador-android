# Especificación: Afinador (Android)

Versión móvil del afinador de escritorio (`../afinador`, Rust). Afinador cromático para **trompeta en Si♭**, **violín** y modo cromático.

## Alcance v1
- Detección de tono con **YIN propio** (`Yin.kt`, portado de `pitch.rs`): ventana 4096, salto 1024, rango 150–2800 Hz, interpolación parabólica, confianza ≥ 0,85, mediana de 5 lecturas y retención de 3 saltos.
- Nota en solfeo (es) o letras (en) con octava; desviación en cents [−50, 50]; aguja verde ≤5, ámbar ≤15, roja el resto.
- Trompeta en Si♭: muestra la nota escrita (+2 semitonos). Violín: resalta la cuerda al aire más cercana (Sol3, Re4, La4, Mi5).
- Diapasón La4 ajustable 415–466 Hz (440 por defecto), botones −/+ de 1 Hz.
- **Solo en primer plano**: escucha en `onResume`, para en `onPause`; pantalla encendida mientras está abierta. Sin servicio ni pantalla apagada.

## Cuestionario de fiabilidad (L-SPEC-1)
| Pregunta | Respuesta |
|---|---|
| Trabajo en segundo plano | NO (la tarjeta Fiabilidad de la plantilla solo afecta al actualizador) |
| Avisos a hora exacta | NO |
| Audio | Entrada: `AudioRecord` MIC, 44,1 kHz mono, hilo propio `URGENT_AUDIO` |
| Temporización crítica | NO (el análisis tolera retrasos de decenas de ms) |
| Datos persistentes | `SharedPreferences("afinador")`: `instrument`, `a4`, `micAsked`; `allowBackup=false` |
| Red | Solo el actualizador (GitHub) |
| Permisos | `RECORD_AUDIO` (se explica con diálogo antes de pedirlo; si se deniega, tarjeta con botón a Ajustes del sistema) |

## Lógica pura con tests
`Yin`, `MedianSmoother`, `Analyzer`, `Notes` (nota/cents/transposición/cuerdas/zonas), `StringsTest` (es/en).
