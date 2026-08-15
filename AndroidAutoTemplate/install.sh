#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ADB="$PROJECT_DIR/../.android-sdk/platform-tools/adb"
APK="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"

if [[ ! -f "$APK" ]]; then
    "$PROJECT_DIR/build.sh"
fi

exec "$ADB" install -r "$APK"
