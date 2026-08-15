#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WORKSPACE_DIR="$(dirname "$PROJECT_DIR")"
export JAVA_HOME="$WORKSPACE_DIR/.jdk"
export ANDROID_HOME="$WORKSPACE_DIR/.android-sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"

exec "$PROJECT_DIR/gradlew" assembleDebug "$@"
