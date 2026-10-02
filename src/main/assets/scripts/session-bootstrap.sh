#!/data/data/com.glads1/files/usr/bin/bash
set -e

PREFIX="${PREFIX:-/data/data/com.glads1/files/usr}"
CONTAINER_NAME="${CONTAINER_NAME:-ubuntu}"
X11_DISPLAY="${X11_DISPLAY:-:0}"
PROGRESS="$PREFIX/var/log/progress.txt"
ROOTFS="$PREFIX/var/lib/proot-distro/containers/$CONTAINER_NAME/rootfs"

progress() { mkdir -p "$(dirname "$PROGRESS")"; printf 'PROGRESS|%s|%s\n' "$1" "$2" >> "$PROGRESS"; }
error_exit() { printf 'ERROR|%s\n' "$1" >> "$PROGRESS"; exit 1; }

: > "$PROGRESS"
trap 'error_exit "fallo en linea $LINENO"' ERR

export TERMUX_APP__PACKAGE_NAME="com.glads1"
export TERMUX__HOME="$PREFIX/home"
export TERMUX__PREFIX="$PREFIX"
export TERMUX_VERSION="gladiator"
export PATH="$PREFIX/bin:$PREFIX/bin/applets:/usr/bin:/bin"
export CONTAINER_NAME

progress 5 "Verificando container $CONTAINER_NAME…"
"$PREFIX/bin/extract-ubuntu.sh" || error_exit "no se pudo preparar $CONTAINER_NAME"

progress 35 "Extrayendo libs Mali…"
"$PREFIX/bin/extract-mali.sh" || true

progress 45 "Copiando libs Mali al container…"
mkdir -p "$ROOTFS/opt/mali"
cp -a "$PREFIX/lib/mali/." "$ROOTFS/opt/mali/"

progress 50 "Verificando servidor X…"
if ! pgrep -f 'termux-x11' >/dev/null 2>&1; then
    termux-x11 "$X11_DISPLAY" >/dev/null 2>&1 &
    sleep 2
fi

progress 60 "Preparando entorno grafico…"
mkdir -p "$ROOTFS/root" "$ROOTFS/tmp"
cp "$PREFIX/share/sesar/sesar-shell" "$ROOTFS/tmp/sesar-shell"
cp "$PREFIX/bin/ubuntu-init.sh" "$ROOTFS/tmp/ubuntu-init.sh"
chmod 755 "$ROOTFS/tmp/sesar-shell"
chmod +x "$ROOTFS/tmp/ubuntu-init.sh"

mkdir -p "$ROOTFS/etc/vulkan/icd.d"
cat > "$ROOTFS/etc/vulkan/icd.d/mali.json" << 'ICDEOF'
{"file_format_version":"1.0.0","ICD":{"library_path":"/opt/mali/vulkan.mali.so","api_version":"1.3.0"}}
ICDEOF

progress 70 "Instalando escritorio…"
"$PREFIX/bin/proot-distro" login "$CONTAINER_NAME" \
    -- /bin/bash -c '
        export DISPLAY='"$X11_DISPLAY"'
        export LD_LIBRARY_PATH=/opt/mali:$LD_LIBRARY_PATH
        export VK_ICD_FILENAMES=/etc/vulkan/icd.d/mali.json
        export PD_PROGRESS_FILE='"$PROGRESS"'
        /tmp/ubuntu-init.sh || { printf "ERROR|init ubuntu fallo\n" >> '"$PROGRESS"'; exit 1; }
    '

progress 96 "Lanzando escritorio…"
printf 'DONE\n' >> "$PROGRESS"

exec "$PREFIX/bin/proot-distro" login "$CONTAINER_NAME" \
    -- /bin/bash -c '
        export DISPLAY='"$X11_DISPLAY"'
        export LD_LIBRARY_PATH=/opt/mali:$LD_LIBRARY_PATH
        export VK_ICD_FILENAMES=/etc/vulkan/icd.d/mali.json
        cd /root
        ./sesar-shell setup
        exec ./sesar-shell session
    '
