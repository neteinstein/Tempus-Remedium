#!/usr/bin/env bash
# Installs a minimal Android SDK (cmdline-tools, platform, build-tools) for
# building this project in a fresh container. Safe to re-run.
#
#   scripts/setup-android-sdk.sh                 # installs into $ANDROID_HOME or ~/android-sdk
#   scripts/setup-android-sdk.sh --maven-mirror  # also installs the sandbox Maven Central mirror
#
# Afterwards it writes local.properties so Gradle can find the SDK.
# --maven-mirror is meant for cloud/sandboxed agent containers, where Maven
# Central intermittently returns 404/429 (see AGENTS.md).
set -euo pipefail

CMDLINE_TOOLS_BUILD="${CMDLINE_TOOLS_BUILD:-16111833}"
PLATFORM="${ANDROID_PLATFORM:-platforms;android-37.0}"
BUILD_TOOLS="${ANDROID_BUILD_TOOLS:-build-tools;37.0.0}"

SDK_DIR="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/android-sdk}}"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SDKMANAGER="$SDK_DIR/cmdline-tools/latest/bin/sdkmanager"

if [[ "${1:-}" == "--maven-mirror" ]]; then
  mkdir -p "$HOME/.gradle/init.d"
  cp "$REPO_ROOT/scripts/sandbox/maven-mirror.init.gradle.kts" "$HOME/.gradle/init.d/"
  echo "Installed Maven Central mirror init script into ~/.gradle/init.d"
fi

if [[ ! -x "$SDKMANAGER" ]]; then
  echo "Installing Android cmdline-tools ($CMDLINE_TOOLS_BUILD) into $SDK_DIR"
  tmp="$(mktemp -d)"
  trap 'rm -rf "$tmp"' EXIT
  curl -fsSL -o "$tmp/cmdline-tools.zip" \
    "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_BUILD}_latest.zip"
  mkdir -p "$SDK_DIR/cmdline-tools"
  unzip -q "$tmp/cmdline-tools.zip" -d "$tmp"
  rm -rf "$SDK_DIR/cmdline-tools/latest"
  mv "$tmp/cmdline-tools" "$SDK_DIR/cmdline-tools/latest"
fi

yes | "$SDKMANAGER" --sdk_root="$SDK_DIR" --licenses >/dev/null || true
"$SDKMANAGER" --sdk_root="$SDK_DIR" --install "platform-tools" "$PLATFORM" "$BUILD_TOOLS" >/dev/null

echo "sdk.dir=$SDK_DIR" > "$REPO_ROOT/local.properties"
echo "Android SDK ready at $SDK_DIR (local.properties written)"
