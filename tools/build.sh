#!/usr/bin/env bash
# Builds the debug APK and installs it on a connected device (if any).
# Usage: tools/build.sh [gradle tasks...]   (default: assembleDebug)
# Overridable env vars: ANDROID_HOME, JAVA_HOME, ANDROID_SERIAL
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROJECT="$ROOT/Android Studio"

export ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/terra/space/Android/Sdk}}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export JAVA_HOME="${JAVA_HOME:-$HOME/.jdks/jbr-17.0.14}"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"

# A stale GRADLE_USER_HOME (e.g. inherited from an IDE) that cannot be created breaks the wrapper:
# fall back to the default ~/.gradle.
if [ -n "${GRADLE_USER_HOME:-}" ] && ! mkdir -p "$GRADLE_USER_HOME" 2>/dev/null; then
	echo "Warning: cannot use GRADLE_USER_HOME=$GRADLE_USER_HOME, using $HOME/.gradle"
	export GRADLE_USER_HOME="$HOME/.gradle"
fi

# Gradle needs to know where the SDK is.
if [ ! -f "$PROJECT/local.properties" ]; then
	echo "sdk.dir=$ANDROID_HOME" > "$PROJECT/local.properties"
fi

cd "$PROJECT"
if [ $# -gt 0 ]; then
	./gradlew "$@"
	exit 0
fi
./gradlew assembleDebug

APK="$(ls -t app/build/outputs/apk/debug/*.apk | head -n 1)"
echo "Built: $PROJECT/$APK"

DEVICES="$(adb devices | awk 'NR>1 && $2=="device" {print $1}')"
if [ -z "$DEVICES" ]; then
	echo "No device connected: skipping install."
	exit 0
fi
if [ -z "${ANDROID_SERIAL:-}" ] && [ "$(echo "$DEVICES" | wc -l)" -gt 1 ]; then
	echo "Several devices connected, set ANDROID_SERIAL to pick one:"; echo "$DEVICES"
	exit 1
fi
adb install -r "$APK"
echo "Installed. Enable it once with:"
echo "  adb shell ime enable com.foschia.tondokeyboard/.KeyboardActionListener"
