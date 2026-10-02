#!/data/data/com.glads1/files/usr/bin/bash
# gladiator: extrae libs Mali de /vendor a $PREFIX/lib/mali/
# idempotente. pensado para correr desde Ajustes > Lib Fix, o al instalar bootstrap.
set -e

PREFIX="${PREFIX:-/data/data/com.glads1/files/usr}"
MANIFEST="$PREFIX/etc/mali-manifest.txt"
DST="$PREFIX/lib/mali"
ICD_DIR="$PREFIX/share/vulkan/icd.d"
ICD="$ICD_DIR/mali.json"

if [ ! -r "$MANIFEST" ]; then
    echo "ERROR: no encuentro $MANIFEST"
    exit 1
fi

mkdir -p "$DST" "$ICD_DIR"

n_ok=0
n_skip=0
n_fail=0

while IFS='|' read -r src dst; do
    src=$(echo "$src" | sed 's/^[ \t]*//;s/[ \t]*$//')
    dst=$(echo "$dst" | sed 's/^[ \t]*//;s/[ \t]*$//')
    [ -z "$src" ] && continue
    case "$src" in '#'*) continue ;; esac

    if [ ! -f "$src" ]; then
        echo "  SKIP $src (no existe en este dispositivo)"
        n_skip=$((n_skip+1))
        continue
    fi

    dst_path="$DST/$dst"
    if [ -f "$dst_path" ]; then
        s_src=$(stat -c '%s' "$src" 2>/dev/null || echo 0)
        s_dst=$(stat -c '%s' "$dst_path" 2>/dev/null || echo 0)
        m_src=$(stat -c '%Y' "$src" 2>/dev/null || echo 0)
        m_dst=$(stat -c '%Y' "$dst_path" 2>/dev/null || echo 0)
        if [ "$s_src" = "$s_dst" ] && [ "$m_dst" -ge "$m_src" ]; then
            n_ok=$((n_ok+1))
            continue
        fi
    fi

    if cp "$src" "$dst_path" 2>/dev/null; then
        chmod 644 "$dst_path"
        strip --strip-unneeded "$dst_path" 2>/dev/null || true
        echo "  OK   $dst"
        n_ok=$((n_ok+1))
    else
        echo "  FAIL $src"
        n_fail=$((n_fail+1))
    fi
done < "$MANIFEST"

cat > "$ICD" << ICDEOF
{
    "file_format_version": "1.0.0",
    "ICD": {
        "library_path": "$DST/vulkan.mali.so",
        "api_version": "1.3.0"
    }
}
ICDEOF

echo ""
echo "resumen: $n_ok ok, $n_skip skip, $n_fail fail"
echo "icd: $ICD"
echo "libs: $DST"

exit $n_fail
