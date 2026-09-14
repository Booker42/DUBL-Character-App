#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
VERSION="${DUBL_VERSION:-0.2.0}"
ARCH="x86_64"
DIST="$ROOT/dist"
APPDIR="$ROOT/build/appimage/DUBL.AppDir"
APPIMAGE="$DIST/DUBL-Character-${VERSION}-linux-${ARCH}.AppImage"
APPIMAGETOOL_VERSION="1.9.1"
APPIMAGETOOL_SHA256="ed4ce84f0d9caff66f50bcca6ff6f35aae54ce8135408b3fa33abfc3cb384eb0"
APPIMAGETOOL_URL="https://github.com/AppImage/appimagetool/releases/download/${APPIMAGETOOL_VERSION}/appimagetool-x86_64.AppImage"
APPIMAGETOOL="${APPIMAGETOOL:-$ROOT/build/appimage/appimagetool-x86_64.AppImage}"

mkdir -p "$DIST" "$(dirname "$APPIMAGETOOL")"

# Canonical product build: Compose Desktop + bundled JVM.
"$ROOT/gradlew" :desktopApp:createDistributable

SOURCE="$ROOT/desktopApp/build/compose/binaries/main/app/DUBL"
[[ -d "$SOURCE" ]] || { echo "Compose distributable not found: $SOURCE" >&2; exit 1; }
rm -rf "$APPDIR"
mkdir -p "$APPDIR/usr/lib/dubl" "$APPDIR/usr/bin" "$APPDIR/usr/share/applications"
cp -a "$SOURCE/." "$APPDIR/usr/lib/dubl/"
ln -s ../lib/dubl/bin/DUBL "$APPDIR/usr/bin/dubl"
cat > "$APPDIR/AppRun" <<'APPRUN'
#!/usr/bin/env sh
HERE="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
exec "$HERE/usr/bin/dubl" "$@"
APPRUN
chmod +x "$APPDIR/AppRun"
cat > "$APPDIR/dubl.desktop" <<'DESKTOP'
[Desktop Entry]
Type=Application
Name=DUBL Character
Comment=DUBL character sheet
Exec=dubl
Icon=dubl
Terminal=false
Categories=Game;Utility;
DESKTOP
cp "$APPDIR/dubl.desktop" "$APPDIR/usr/share/applications/dubl.desktop"
cat > "$APPDIR/dubl.svg" <<'SVG'
<svg xmlns="http://www.w3.org/2000/svg" width="256" height="256" viewBox="0 0 256 256">
  <rect width="256" height="256" rx="48" fill="#0E1014"/>
  <rect x="18" y="18" width="220" height="220" rx="36" fill="none" stroke="#A83948" stroke-width="12"/>
  <path d="M70 62h48c44 0 72 25 72 66s-28 66-72 66H70V62zm44 102c24 0 38-12 38-36s-14-36-38-36H106v72h8z" fill="#EEE9E1"/>
</svg>
SVG
ln -s dubl.svg "$APPDIR/.DirIcon"

if [[ ! -f "$APPIMAGETOOL" ]]; then
  curl -fsSL "$APPIMAGETOOL_URL" -o "$APPIMAGETOOL"
fi
printf '%s  %s\n' "$APPIMAGETOOL_SHA256" "$APPIMAGETOOL" | sha256sum -c -
chmod +x "$APPIMAGETOOL"

ARCH="$ARCH" APPIMAGE_EXTRACT_AND_RUN=1 "$APPIMAGETOOL" "$APPDIR" "$APPIMAGE"
chmod +x "$APPIMAGE"
sha256sum "$APPIMAGE" > "$APPIMAGE.sha256"
echo "Built: $APPIMAGE"
