#!/usr/bin/env bash
set -euo pipefail

GRADLE_VERSION="9.6.0"
CACHE_ROOT="${HOME}/.gradle/dubl-bootstrap"
DIST_DIR="${CACHE_ROOT}/gradle-${GRADLE_VERSION}"
ZIP_PATH="${CACHE_ROOT}/gradle-${GRADLE_VERSION}-bin.zip"
URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"

mkdir -p "${CACHE_ROOT}"

if [[ ! -x "${DIST_DIR}/bin/gradle" ]]; then
  echo "Downloading Gradle ${GRADLE_VERSION}..."
  if command -v curl >/dev/null 2>&1; then
    curl -fL --retry 3 -o "${ZIP_PATH}" "${URL}"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "${ZIP_PATH}" "${URL}"
  else
    echo "Need curl or wget to bootstrap Gradle." >&2
    exit 1
  fi

  command -v unzip >/dev/null 2>&1 || {
    echo "Need unzip. On CachyOS/Arch: sudo pacman -S unzip" >&2
    exit 1
  }

  rm -rf "${DIST_DIR}"
  unzip -q "${ZIP_PATH}" -d "${CACHE_ROOT}"
fi

"${DIST_DIR}/bin/gradle" wrapper --gradle-version "${GRADLE_VERSION}" --distribution-type bin

echo
echo "Gradle wrapper ready. Next run:"
echo "  ./gradlew assembleDebug"
