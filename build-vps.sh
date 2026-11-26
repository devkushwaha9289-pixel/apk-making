#!/usr/bin/env bash
# Build the APK on a Linux VPS (Ubuntu/Debian, x86_64). Run:  bash build-vps.sh
set -e
cd "$(dirname "$0")"
. ./common.sh
SDK="${ANDROID_HOME:-$HOME/android-sdk}"

# 1) Java 17 + tools
if ! java -version 2>&1 | grep -Eq '"(17|21)\.'; then
  sudo apt-get update && sudo apt-get install -y openjdk-17-jdk-headless
fi
sudo apt-get install -y unzip wget zip >/dev/null 2>&1 || true
export JAVA_HOME="${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v javac || command -v java)")")")}"

# 2) Android SDK (command line tools, platform 34, build-tools 34)
mkdir -p "$SDK/cmdline-tools"
if [ ! -d "$SDK/cmdline-tools/latest" ]; then
  wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O /tmp/cmdtools.zip
  unzip -q /tmp/cmdtools.zip -d "$SDK/cmdline-tools" && mv "$SDK/cmdline-tools/cmdline-tools" "$SDK/cmdline-tools/latest"
fi
yes | "$SDK/cmdline-tools/latest/bin/sdkmanager" --licenses >/dev/null 2>&1 || true
"$SDK/cmdline-tools/latest/bin/sdkmanager" "platform-tools" "platforms;android-34" "build-tools;34.0.0"
echo "sdk.dir=$SDK" > local.properties

# 3) Gradle 8.x
GRADLE=gradle
if ! command -v gradle >/dev/null 2>&1 || ! gradle -v 2>/dev/null | grep -Eq 'Gradle (8\.([2-9]|1[0-9])|9)'; then
  [ -d "$HOME/gradle-8.5" ] || { wget -q https://services.gradle.org/distributions/gradle-8.5-bin.zip -O /tmp/gradle.zip && unzip -q /tmp/gradle.zip -d "$HOME"; }
  GRADLE="$HOME/gradle-8.5/bin/gradle"
fi

# 4) sign + build
make_keystore
"$GRADLE" --no-daemon assembleRelease
collect_apk
