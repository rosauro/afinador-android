# Afinador

Afinador cromático de escritorio para **trompeta en Si♭** y **violín**, en Rust.
Ventana oscura y compacta, nota grande legible a distancia y medidor de ±50 cents.

- GUI: `eframe`/`egui`. Captura: `cpal` (ALSA, que en Tumbleweed pasa por PipeWire).
- Detección de tono: algoritmo **YIN implementado a mano** (sin crates de pitch).
- Sin Python en ningún punto del proyecto.

## Funcionamiento

- YIN sobre ventanas de 4096 muestras (salto de 1024), rango 150–2800 Hz,
  interpolación parabólica, umbral de confianza y umbral de silencio (RMS).
  La frecuencia se suaviza con la mediana de las últimas 5 lecturas.
- Notación en solfeo (Do Re Mi Fa Sol La Si, con ♯) y octava. Diapasón La4
  ajustable entre 415 y 466 Hz (440 por defecto).
- Instrumentos:
  - **Trompeta en Si♭**: muestra la nota *escrita* (frecuencia real + 2 semitonos;
    si suena Si♭3, muestra Do4).
  - **Violín**: sin transposición; resalta la cuerda al aire más cercana
    (Sol3, Re4, La4, Mi5).
  - **Cromático / concierto**: sin transposición.
- Color de la aguja: verde ≤ 5 cents, amarillo ≤ 15, rojo en el resto.
- Selector de dispositivo de entrada y barra de nivel (confirma que llega señal).

## Instalación en openSUSE Tumbleweed

Toolchain y paquetes de desarrollo (Wayland y X11):

```bash
sudo zypper install rust cargo alsa-devel libxkbcommon-devel libxkbcommon-x11-devel wayland-devel libX11-devel libxcb-devel libXcursor-devel libXrandr-devel libXi-devel Mesa-libGL-devel Mesa-libEGL-devel
```

Para que el micrófono sea accesible vía ALSA a través de PipeWire hace falta
`pipewire-alsa` (suele venir instalado). Opcional: `dejavu-fonts`; el afinador
lo usa para dibujar ♯ y ♭ y, si no está, muestra `#` y `b`.

## Compilar y ejecutar

```bash
cargo run --release      # compila y abre la ventana
cargo test               # tests unitarios
cargo clippy --all-targets
```

## Instalar en el menú de aplicaciones

```bash
cargo build --release
install -Dm755 target/release/afinador ~/.local/bin/afinador
install -Dm644 afinador.desktop ~/.local/share/applications/afinador.desktop
install -Dm644 assets/afinador.svg ~/.local/share/icons/hicolor/scalable/apps/afinador.svg
```

(Asegúrate de que `~/.local/bin` está en el `PATH`.)

## Estructura

| Módulo | Contenido |
|---|---|
| `src/pitch.rs` | YIN, RMS y suavizado por mediana |
| `src/notes.rs` | frecuencia → nota/cents, transposición Si♭, cuerdas del violín |
| `src/audio.rs` | captura con cpal, hilo de análisis, lista de dispositivos |
| `src/ui.rs` | interfaz egui: nota, medidor, selectores |

Hilos: el *stream* de cpal envía muestras mono por un canal a un hilo de
análisis, que publica el resultado en un `Arc<Mutex<_>>` que lee la GUI. Ni el
callback de audio ni la interfaz hacen el cálculo de YIN.

## Solución de problemas

- **Nivel a cero / sin nota**: elige otra entrada en el selector o revisa con
  `pavucontrol` que el micrófono no esté silenciado.
- **Un violín puede dar lecturas inestables en los armónicos agudos**; toca la
  cuerda con arco continuo y espera ~0,3 s a que la mediana se estabilice.
