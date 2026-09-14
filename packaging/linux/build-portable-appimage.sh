#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
BUILD="$ROOT/build/portable-appimage"
STAGE="$BUILD/stage/app"
DIST="$ROOT/dist"
VERSION="${DUBL_VERSION:-0.2.0}"
ARTIFACT="$DIST/DUBL-Character-${VERSION}-linux-x86_64.AppImage"
KOTLINC="${KOTLINC:-$(command -v kotlinc || true)}"

[[ -n "$KOTLINC" ]] || { echo "kotlinc is required to build the portable fallback" >&2; exit 1; }
command -v jlink >/dev/null || { echo "jlink is required" >&2; exit 1; }
command -v gcc >/dev/null || { echo "gcc is required" >&2; exit 1; }
command -v ld >/dev/null || { echo "ld is required" >&2; exit 1; }
command -v tar >/dev/null || { echo "tar is required" >&2; exit 1; }
command -v jar >/dev/null || { echo "jar is required" >&2; exit 1; }

rm -rf "$BUILD"
mkdir -p "$STAGE/bin" "$STAGE/lib" "$DIST" "$BUILD/obj"

mapfile -t MODEL_SOURCES < <(find "$ROOT/shared/src/commonMain/kotlin/com/dubl/character/android/model" -maxdepth 1 -name '*.kt' | sort)
mapfile -t DATA_SOURCES < <(find "$ROOT/shared/src/commonMain/kotlin/com/dubl/character/android/data" -maxdepth 1 -name '*.kt' | sort)
mapfile -t STATE_SOURCES < <(find "$ROOT/shared/src/commonMain/kotlin/com/dubl/character/android/state" -maxdepth 1 -name '*.kt' | sort)
mapfile -t DESKTOP_SOURCES < <(find "$ROOT/shared/src/desktopMain/kotlin/com/dubl/character" -name '*.kt' | sort)
mapfile -t PORTABLE_SOURCES < <(find "$ROOT/packaging/linux/portable-src" -name '*.kt' | sort)

"$KOTLINC" \
  "${MODEL_SOURCES[@]}" \
  "${DATA_SOURCES[@]}" \
  "${STATE_SOURCES[@]}" \
  "${DESKTOP_SOURCES[@]}" \
  "${PORTABLE_SOURCES[@]}" \
  -jvm-target 17 \
  -include-runtime \
  -d "$STAGE/lib/dubl.jar"

# Canonical Android catalogs are shared resources and are embedded in the desktop JAR.
jar uf "$STAGE/lib/dubl.jar" -C "$ROOT/shared/src/commonMain/resources" .

jlink \
  --add-modules java.base,java.desktop \
  --strip-debug \
  --no-header-files \
  --no-man-pages \
  --compress=2 \
  --output "$STAGE/runtime"

cat > "$STAGE/bin/dubl" <<'LAUNCH'
#!/usr/bin/env sh
set -eu
HERE="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
exec "$HERE/runtime/bin/java" -Dfile.encoding=UTF-8 -jar "$HERE/lib/dubl.jar" "$@"
LAUNCH
chmod +x "$STAGE/bin/dubl"

# Verify the staged application using the bundled runtime before wrapping it.
FRESH_STAGE_HOME="$(mktemp -d)"
trap 'rm -rf "$FRESH_STAGE_HOME"' EXIT
HOME="$FRESH_STAGE_HOME" JAVA_HOME=/definitely/missing PATH=/usr/bin:/bin "$STAGE/bin/dubl" --smoke-test | grep -q '^DUBL_SMOKE_OK '

(
  cd "$BUILD/stage"
  tar --format=ustar -cf "$BUILD/payload.tar" app
)

(
  cd "$BUILD"
  ld -r -b binary -o obj/payload.o payload.tar
)

gcc -O2 -static -s \
  -DDUBL_PORTABLE_VERSION=\"${VERSION}\" \
  "$ROOT/packaging/linux/launcher/appimage_launcher.c" \
  "$BUILD/obj/payload.o" \
  -o "$ARTIFACT"
chmod +x "$ARTIFACT"

"$ROOT/packaging/linux/tests/appimage-smoke.sh" "$ARTIFACT" "$VERSION"
sha256sum "$ARTIFACT" > "$ARTIFACT.sha256"

echo "Built: $ARTIFACT"
