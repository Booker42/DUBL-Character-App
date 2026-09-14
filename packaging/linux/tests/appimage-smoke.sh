#!/usr/bin/env bash
set -euo pipefail
artifact="${1:-dist/DUBL-Character-0.2.0-linux-x86_64.AppImage}"
expected_version="${2:-${DUBL_VERSION:-0.2.0}}"
[[ -x "$artifact" ]] || { echo "artifact is missing or not executable: $artifact" >&2; exit 1; }
fresh_home="$(mktemp -d)"
trap 'rm -rf "$fresh_home"' EXIT
output="$(HOME="$fresh_home" JAVA_HOME=/definitely/missing PATH=/usr/bin:/bin "$artifact" --smoke-test)"
grep -q '^DUBL_SMOKE_OK ' <<<"$output" || { echo "unexpected smoke output: $output" >&2; exit 1; }
version="$(HOME="$fresh_home" JAVA_HOME=/definitely/missing PATH=/usr/bin:/bin "$artifact" --version)"
[[ "$version" == "DUBL Character $expected_version" ]] || { echo "unexpected version: $version" >&2; exit 1; }
echo "APPIMAGE_SMOKE_OK $expected_version"
