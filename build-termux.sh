#!/data/data/com.termux/files/usr/bin/bash
# Build the APK inside Termux (Android, arm64). Best effort - the VPS script is the safer way.
# Needs ~3 GB free storage and 3 GB RAM.   Run:  bash build-termux.sh
set -e
cd "$(dirname "$0")"
. ./common.sh
SDK="$HOME/android-sdk"

pkg update -y
pkg install -y openjdk-17 gradle wget unzip zip aapt aapt2 zipalign apksigner 2>/dev/null || pkg install -y openjdk-17 gradle wget unzip zip aapt aapt2
export JAVA_HOME="$PREFIX/lib/jvm/java-17-openjdk"

# Android SDK: platform (pure Java) + build-tools, then swap the x86 binaries for Termux's arm64 ones
mkdir -p "$SDK/cmdline-tools"
if [ ! -d "$SDK/cmdline-tools/latest" ]; then
  wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O "$TMPDIR/cmdtools.zip"
  unzip -q "$TMPDIR/cmdtools.zip" -d "$SDK/cmdline-tools" && mv "$SDK/cmdline-tools/cmdline-tools" "$SDK/cmdline-tools/latest"
fi
yes | "$SDK/cmdline-tools/latest/bin/sdkmanager" --licenses >/dev/null 2>&1 || true
"$SDK/cmdline-tools/latest/bin/sdkmanager" "platforms;android-34" "build-tools;34.0.0"
BT="$SDK/build-tools/34.0.0"
for b in aapt aapt2 zipalign; do [ -x "$PREFIX/bin/$b" ] && ln -sf "$PREFIX/bin/$b" "$BT/$b"; done
echo "sdk.dir=$SDK" > local.properties

# tell Gradle to use Termux's own aapt2 (Google's is x86 only)
mkdir -p "$HOME/.gradle"
grep -q aapt2FromMavenOverride "$HOME/.gradle/gradle.properties" 2>/dev/null || echo "android.aapt2FromMavenOverride=$PREFIX/bin/aapt2" >> "$HOME/.gradle/gradle.properties"
grep -q 'org.gradle.jvmargs' "$HOME/.gradle/gradle.properties" || echo "org.gradle.jvmargs=-Xmx1536m" >> "$HOME/.gradle/gradle.properties"

make_keystore
gradle --no-daemon assembleRelease
collect_apk
