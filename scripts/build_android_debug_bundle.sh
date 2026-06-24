#!/usr/bin/env bash
set -Eeuo pipefail

# SWRLZ-CORE Android debug APK builder for GitHub Codespaces / Linux builders.
# Goal: let a phone-only workflow upload the full source ZIP, extract it, run one script,
# and download a ZIP containing the debug APK + SHA-256 checksum.

show_help() {
  cat <<'EOF'
SWRLZ-CORE Android Debug Bundle Builder

Usage:
  bash scripts/build_android_debug_bundle.sh

Optional environment variables:
  SWRLZ_APK_OUT_DIR   Output directory for APK bundle. Default: <project-root>/APK_DOWNLOAD
  ANDROID_HOME        Android SDK path. Usually already set in GitHub Codespaces.
  ANDROID_SDK_ROOT    Android SDK path fallback if ANDROID_HOME is not set.

What it does:
  1. Locates the project root and android/gradlew.
  2. Writes android/local.properties using ANDROID_HOME or ANDROID_SDK_ROOT.
  3. Runs ./gradlew --no-daemon --stacktrace clean assembleDebug.
  4. Copies the debug APK into APK_DOWNLOAD/ with a project/version-safe name.
  5. Creates SHA-256 checksum files.
  6. Creates a downloadable ZIP containing APK + checksums + build log.

If the build fails, the script does not edit source files. It leaves a build log in APK_DOWNLOAD/.
EOF
}

if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
  show_help
  exit 0
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
PROJECT_NAME="$(basename "$PROJECT_ROOT")"
ANDROID_DIR="$PROJECT_ROOT/android"
GRADLEW="$ANDROID_DIR/gradlew"
OUT_DIR="${SWRLZ_APK_OUT_DIR:-$PROJECT_ROOT/APK_DOWNLOAD}"
LOG_FILE="$OUT_DIR/build_android_debug.log"

mkdir -p "$OUT_DIR"

fail() {
  echo "ERROR: $*" >&2
  echo "Build helper stopped. Check: $LOG_FILE" >&2
  exit 1
}

sanitize_name() {
  # Keep names filesystem/download friendly.
  printf '%s' "$1" | tr -c 'A-Za-z0-9._-' '_'
}

SDK_DIR="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
[[ -n "$SDK_DIR" ]] || fail "ANDROID_HOME or ANDROID_SDK_ROOT is not set. In Codespaces, install/enable Android SDK first."
[[ -d "$SDK_DIR" ]] || fail "Android SDK path does not exist: $SDK_DIR"
[[ -d "$ANDROID_DIR" ]] || fail "Could not find android/ directory at: $ANDROID_DIR"
[[ -f "$GRADLEW" ]] || fail "Could not find Gradle wrapper at: $GRADLEW"

chmod +x "$GRADLEW"
printf 'sdk.dir=%s\n' "$SDK_DIR" > "$ANDROID_DIR/local.properties"

SAFE_PROJECT_NAME="$(sanitize_name "$PROJECT_NAME")"
STARTED_AT="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

{
  echo "SWRLZ-CORE Android Debug Bundle Build"
  echo "Started: $STARTED_AT"
  echo "Project root: $PROJECT_ROOT"
  echo "Android dir: $ANDROID_DIR"
  echo "SDK dir: $SDK_DIR"
  echo "Output dir: $OUT_DIR"
  echo "Gradle command: ./gradlew --no-daemon --stacktrace clean assembleDebug"
  echo
} | tee "$LOG_FILE"

pushd "$ANDROID_DIR" >/dev/null
set +e
./gradlew --no-daemon --stacktrace clean assembleDebug 2>&1 | tee -a "$LOG_FILE"
GRADLE_STATUS=${PIPESTATUS[0]}
set -e
popd >/dev/null

if [[ "$GRADLE_STATUS" -ne 0 ]]; then
  echo "Build failed with Gradle exit code: $GRADLE_STATUS" | tee -a "$LOG_FILE"
  echo "No source files were modified by this script except android/local.properties." | tee -a "$LOG_FILE"
  exit "$GRADLE_STATUS"
fi

mapfile -t APK_CANDIDATES < <(find "$ANDROID_DIR/app/build/outputs/apk/debug" -type f -name '*.apk' 2>/dev/null | sort)
[[ "${#APK_CANDIDATES[@]}" -gt 0 ]] || fail "Gradle succeeded but no debug APK was found under android/app/build/outputs/apk/debug."

APK_SRC="${APK_CANDIDATES[-1]}"
APK_NAME="${SAFE_PROJECT_NAME}_debug.apk"
APK_DST="$OUT_DIR/$APK_NAME"
cp "$APK_SRC" "$APK_DST"

pushd "$OUT_DIR" >/dev/null
sha256sum "$APK_NAME" > "${APK_NAME}.sha256"
sha256sum "$LOG_FILE" > "build_android_debug.log.sha256"

BUNDLE_NAME="${SAFE_PROJECT_NAME}_APK_DOWNLOAD.zip"
rm -f "$BUNDLE_NAME"
if command -v zip >/dev/null 2>&1; then
  zip -9 "$BUNDLE_NAME" "$APK_NAME" "${APK_NAME}.sha256" "build_android_debug.log" "build_android_debug.log.sha256" >/dev/null
else
  python3 - <<PY
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
out = Path('$BUNDLE_NAME')
files = [Path('$APK_NAME'), Path('${APK_NAME}.sha256'), Path('build_android_debug.log'), Path('build_android_debug.log.sha256')]
with ZipFile(out, 'w', ZIP_DEFLATED) as z:
    for p in files:
        z.write(p, p.name)
PY
fi
sha256sum "$BUNDLE_NAME" > "${BUNDLE_NAME}.sha256"
popd >/dev/null

FINISHED_AT="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
{
  echo
  echo "Build succeeded."
  echo "Finished: $FINISHED_AT"
  echo "APK: $APK_DST"
  echo "Bundle: $OUT_DIR/$BUNDLE_NAME"
  echo "Bundle checksum: $OUT_DIR/${BUNDLE_NAME}.sha256"
} | tee -a "$LOG_FILE"
