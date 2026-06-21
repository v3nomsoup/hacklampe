#!/usr/bin/env bash
# Baut signierte Release-Artefakte und benennt sie mit Version + versionCode:
#   HackLampe-<versionName>-<versionCode>.apk            (Sideload/Teilen)
#   HackLampe-<versionName>-<versionCode>-playstore.aab  (Play-Upload)
#
# Nutzung:  scripts/build-release.sh [zielordner]
# Standard-Zielordner: ~/Downloads
set -euo pipefail

OUT_DIR="${1:-$HOME/Downloads}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

export JAVA_HOME="${JAVA_HOME:-/usr/local/opt/openjdk@17}"
export ANDROID_HOME="${ANDROID_HOME:-/usr/local/share/android-commandlinetools}"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"

./gradlew bundleRelease assembleRelease

APK="app/build/outputs/apk/release/app-release.apk"
AAB="app/build/outputs/bundle/release/app-release.aab"

# versionName/versionCode aus dem gebauten APK lesen
AAPT="$(ls "$ANDROID_HOME"/build-tools/*/aapt | sort -V | tail -1)"
INFO="$("$AAPT" dump badging "$APK")"
VN="$(echo "$INFO" | sed -nE "s/.*versionName='([^']+)'.*/\1/p")"
VC="$(echo "$INFO" | sed -nE "s/.*versionCode='([0-9]+)'.*/\1/p")"

mkdir -p "$OUT_DIR"
cp "$APK" "$OUT_DIR/HackLampe-${VN}-${VC}.apk"
cp "$AAB" "$OUT_DIR/HackLampe-${VN}-${VC}-playstore.aab"

echo "Fertig:"
echo "  $OUT_DIR/HackLampe-${VN}-${VC}.apk"
echo "  $OUT_DIR/HackLampe-${VN}-${VC}-playstore.aab"
