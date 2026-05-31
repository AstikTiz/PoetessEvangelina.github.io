#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_DIR="$ROOT_DIR/app"
BUILD_DIR="$ROOT_DIR/build/manual-sdk"
COMPILED_RES_DIR="$BUILD_DIR/compiled-res"
GENERATED_DIR="$BUILD_DIR/generated"
CLASSES_DIR="$BUILD_DIR/classes"
DEX_DIR="$BUILD_DIR/dex"
UNALIGNED_APK="$BUILD_DIR/waifu-wallpaper-unsigned.apk"
ALIGNED_APK="$BUILD_DIR/waifu-wallpaper-aligned.apk"
SIGNED_APK="$ROOT_DIR/app/build/outputs/apk/manual/waifu-wallpaper-manual-debug.apk"
DEBUG_KEYSTORE="$BUILD_DIR/debug.keystore"

SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ -z "$SDK_ROOT" || ! -d "$SDK_ROOT" ]]; then
  echo "ANDROID_SDK_ROOT or ANDROID_HOME must point to an installed Android SDK." >&2
  exit 1
fi

find_latest_dir() {
  local parent="$1"
  if [[ ! -d "$parent" ]]; then
    return 1
  fi
  find "$parent" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -n 1
}

PLATFORM_DIR="$(find_latest_dir "$SDK_ROOT/platforms")"
BUILD_TOOLS_DIR="$(find_latest_dir "$SDK_ROOT/build-tools")"
ANDROID_JAR="$PLATFORM_DIR/android.jar"
AAPT2="$BUILD_TOOLS_DIR/aapt2"
D8="$BUILD_TOOLS_DIR/d8"
ZIPALIGN="$BUILD_TOOLS_DIR/zipalign"
APKSIGNER="$BUILD_TOOLS_DIR/apksigner"

for tool in javac keytool zip "$AAPT2" "$D8" "$ZIPALIGN" "$APKSIGNER" "$ANDROID_JAR"; do
  if ! command -v "$tool" >/dev/null 2>&1 && [[ ! -e "$tool" ]]; then
    echo "Required build tool not found: $tool" >&2
    exit 1
  fi
done

rm -rf "$BUILD_DIR"
mkdir -p "$COMPILED_RES_DIR" "$GENERATED_DIR" "$CLASSES_DIR" "$DEX_DIR" "$(dirname "$SIGNED_APK")"

"$AAPT2" compile --dir "$APP_DIR/src/main/res" -o "$COMPILED_RES_DIR"
mapfile -t FLAT_RESOURCES < <(find "$COMPILED_RES_DIR" -name '*.flat' | sort)

"$AAPT2" link \
  -I "$ANDROID_JAR" \
  --manifest "$APP_DIR/src/main/AndroidManifest.xml" \
  --java "$GENERATED_DIR" \
  --auto-add-overlay \
  -o "$UNALIGNED_APK" \
  "${FLAT_RESOURCES[@]}"

mapfile -t JAVA_SOURCES < <(find "$APP_DIR/src/main/java" "$GENERATED_DIR" -name '*.java' | sort)
javac \
  -source 8 \
  -target 8 \
  -bootclasspath "$ANDROID_JAR" \
  -classpath "$ANDROID_JAR" \
  -d "$CLASSES_DIR" \
  "${JAVA_SOURCES[@]}"

"$D8" --min-api 26 --output "$DEX_DIR" "$CLASSES_DIR"
(cd "$DEX_DIR" && zip -q -r "$UNALIGNED_APK" classes.dex)

"$ZIPALIGN" -f 4 "$UNALIGNED_APK" "$ALIGNED_APK"

if [[ ! -f "$DEBUG_KEYSTORE" ]]; then
  keytool -genkeypair \
    -keystore "$DEBUG_KEYSTORE" \
    -storepass android \
    -keypass android \
    -alias androiddebugkey \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -dname "CN=Android Debug,O=Android,C=US" >/dev/null
fi

"$APKSIGNER" sign \
  --ks "$DEBUG_KEYSTORE" \
  --ks-pass pass:android \
  --key-pass pass:android \
  --out "$SIGNED_APK" \
  "$ALIGNED_APK"

"$APKSIGNER" verify "$SIGNED_APK"
echo "Manual SDK APK created: $SIGNED_APK"
