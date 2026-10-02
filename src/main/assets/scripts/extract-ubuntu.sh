#!/data/data/com.glads1/files/usr/bin/bash
set -e

PREFIX="${PREFIX:-/data/data/com.glads1/files/usr}"
CONTAINER_NAME="${CONTAINER_NAME:-ubuntu}"
PROGRESS="$PREFIX/var/log/progress.txt"

export TERMUX_APP__PACKAGE_NAME="com.glads1"
export TERMUX__HOME="$PREFIX/home"
export TERMUX__PREFIX="$PREFIX"
export TERMUX_VERSION="gladiator"
export PATH="$PREFIX/bin:$PREFIX/bin/applets:/usr/bin:/bin"

CONTAINER="$PREFIX/var/lib/proot-distro/containers/$CONTAINER_NAME"
BUNDLE="$PREFIX/share/ubuntu-rootfs.tar.gz"

progress() { printf 'PROGRESS|%s|%s\n' "$1" "$2" >> "$PROGRESS"; }

if [ -f "$CONTAINER/manifest.json" ] && [ -x "$CONTAINER/rootfs/bin/bash" ]; then
    echo "container ya instalado"
    exit 0
fi

if [ ! -f "$BUNDLE" ]; then
    echo "ERROR: falta $BUNDLE"
    exit 1
fi

if [ -e "$CONTAINER" ]; then
    echo "container incompleto, limpiando..."
    rm -rf "$CONTAINER"
fi

progress 8 "Instalando container $CONTAINER_NAME…"
proot-distro install -n "$CONTAINER_NAME" "$BUNDLE" 2>&1 | while IFS= read -r line; do
    echo "$line"
    case "$line" in
        *"Applying layer 1"*)      progress 12 "Aplicando rootfs (1/2)…" ;;
        *"Applying layer 2"*)      progress 18 "Aplicando capa final (2/2)…" ;;
        *"resolv.conf"*)           progress 22 "Configurando red…" ;;
        *"Registering"*)           progress 26 "Registrando UIDs…" ;;
        *"Finished installation"*) progress 28 "Container listo" ;;
    esac
done

if [ ! -x "$CONTAINER/rootfs/bin/bash" ]; then
    echo "ERROR: instalacion incompleta"
    exit 1
fi

echo "ok: $CONTAINER"
