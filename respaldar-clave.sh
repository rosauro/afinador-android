#!/usr/bin/env bash
#
# respaldar-clave.sh
# -------------------
# Copia la clave de firma de la app (el .jks y keystore.properties) a una carpeta
# fuera de este equipo (USB, disco externo, carpeta sincronizada...), comprueba que
# la copia es idéntica y que la clave se abre con su contraseña y, si todo va bien,
# marca la clave como respaldada (.clave-respaldada) para que publicar.sh deje publicar.
#
# Uso:
#   ./respaldar-clave.sh DESTINO [--marcar] [--aunque-sea-este-disco]
#     DESTINO                  carpeta de destino, p. ej. /run/media/$USER/USB
#     --marcar                 marca .clave-respaldada sin preguntar (útil sin terminal)
#     --aunque-sea-este-disco  permite un destino en el mismo disco que la clave
#                              (no es una copia de seguridad real; solo para pruebas)
#
# Notas: no necesita sudo ni internet. Nunca imprime contraseñas ni las pasa por
# la línea de comandos. La clave NO debe subirse a git (keystore.properties y *.jks
# están en .gitignore). Si no hay keytool, usa el JDK de /datos/android-sdk/env.sh.

set -euo pipefail

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

info()  { echo -e "${GREEN}[INFO]${NC} $*"; }
skip()  { echo -e "${YELLOW}[YA HECHO]${NC} $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
fail()  { echo -e "${RED}[ERROR]${NC} $*" >&2; exit 1; }
step()  { echo -e "\n${YELLOW}==>${NC} $*"; }

need_cmd() { command -v "$1" >/dev/null 2>&1; }

cd "$(dirname "$0")"
PROPS=keystore.properties

DEST=""
MARCAR=0
MISMO_DISCO=0
for a in "$@"; do
    case "$a" in
        --marcar) MARCAR=1 ;;
        --aunque-sea-este-disco) MISMO_DISCO=1 ;;
        -h|--help) sed -n '2,19p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
        -*) fail "Opción desconocida: $a" ;;
        *) [ -z "$DEST" ] || fail "Solo se admite una carpeta de destino"; DEST="$a" ;;
    esac
done
[ -n "$DEST" ] || fail "Falta la carpeta de destino. Uso: $0 DESTINO [--marcar]"

prop() { grep -E "^$1=" "$PROPS" | head -n1 | cut -d= -f2-; }

step "Comprobaciones"
[ -f "$PROPS" ] || fail "Falta $PROPS (no hay clave de firma en este proyecto)"
JKS=$(prop storeFile)
ALIAS=$(prop keyAlias)
[ -n "$JKS" ] && [ -n "$ALIAS" ] || fail "$PROPS no tiene storeFile o keyAlias"
[ -f "$JKS" ] || fail "No existe la clave $JKS"
[ -d "$DEST" ] || fail "La carpeta de destino no existe (¿está montado el USB?): $DEST"
[ -w "$DEST" ] || fail "No puedo escribir en $DEST"

DEST=$(cd "$DEST" && pwd -P)
case "$DEST/" in
    "$(pwd -P)"/*) fail "El destino está dentro del repositorio: la clave acabaría en git" ;;
esac
if [ "$(stat -c %d "$DEST")" = "$(stat -c %d "$JKS")" ]; then
    if [ "$MISMO_DISCO" -eq 1 ]; then
        warn "El destino está en el mismo disco que la clave: esto NO es una copia de seguridad real."
    else
        fail "El destino está en el mismo disco que la clave. Usa un USB u otro equipo (o --aunque-sea-este-disco para probar)."
    fi
fi
if ! need_cmd keytool && [ -f /datos/android-sdk/env.sh ]; then
    # shellcheck source=/dev/null
    source /datos/android-sdk/env.sh
fi
need_cmd keytool || fail "No encuentro keytool (instala el JDK o revisa /datos/android-sdk/env.sh)"
need_cmd sha256sum || fail "Falta sha256sum"
info "Clave: $JKS (alias $ALIAS)"
info "Destino: $DEST"

copia() {   # copia($1 origen) → $DEST, solo si cambia
    local src="$1" dst
    dst="$DEST/$(basename "$1")"
    if [ -f "$dst" ] && [ "$(sha256sum <"$src")" = "$(sha256sum <"$dst")" ]; then
        skip "$(basename "$dst") ya está en el destino y es idéntico."
    else
        install -m 600 "$src" "$dst" || fail "No se pudo copiar $src"
        info "Copiado $(basename "$dst")."
    fi
    [ "$(sha256sum <"$src")" = "$(sha256sum <"$dst")" ] || fail "La copia de $(basename "$dst") no coincide con el original"
}

step "Copia de la clave y sus contraseñas"
copia "$JKS"
copia "$PROPS"

step "Verificación de la copia"
# La contraseña viaja por variable de entorno, no por la línea de comandos (se vería en ps).
KS_PASS=$(prop storePassword)
export KS_PASS
if keytool -list -keystore "$DEST/$(basename "$JKS")" -alias "$ALIAS" -storepass:env KS_PASS >/dev/null 2>&1; then
    info "La copia se abre con su contraseña y contiene el alias «$ALIAS»."
else
    fail "La copia no se abre con la contraseña de $PROPS: no sirve como respaldo."
fi
unset KS_PASS
info "Huella SHA-256 de la clave (anótala si quieres): $(sha256sum <"$JKS" | cut -d' ' -f1)"

step "Marcar la clave como respaldada"
if [ -f .clave-respaldada ]; then
    skip "Ya estaba marcada (.clave-respaldada)."
else
    ok=0
    if [ "$MARCAR" -eq 1 ]; then ok=1
    elif [ -t 0 ]; then
        read -r -p "¿Confirmas que $DEST es un sitio fuera de este equipo y lo guardas bien? [s/N] " r || r=""
        [[ "$r" =~ ^[sSyY]$ ]] && ok=1
    fi
    if [ "$ok" -eq 1 ]; then
        ./publicar.sh --clave-respaldada
    else
        warn "No se ha marcado. Cuando quieras: ./respaldar-clave.sh '$DEST' --marcar"
    fi
fi

info "Listo. Guarda $DEST en un lugar seguro: sin la clave nadie podrá actualizar la app."
