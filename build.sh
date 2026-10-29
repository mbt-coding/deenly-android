#!/usr/bin/env bash
# Local build of a debug APK and a release AAB.
# Needs: Node 18+, JDK 17, the Android SDK (ANDROID_HOME set), and internet for npm and Gradle.
# Optional signing for the AAB: export KEYSTORE=/path/deenly.jks KEYSTORE_PASSWORD=... KEY_ALIAS=... KEY_PASSWORD=...
set -e
cd "$(dirname "$0")"
[ -f ../index.html ] && cp -f ../index.html www/index.html && { [ -d ../images ] && cp -rf ../images www/ || true; }
npm install
[ -d android ] || npx cap add android
python3 patch_android.py
npx cap sync android
cd android
./gradlew assembleDebug
if [ -n "$KEYSTORE" ]; then
  ./gradlew bundleRelease -Pandroid.injected.signing.store.file="$KEYSTORE" -Pandroid.injected.signing.store.password="$KEYSTORE_PASSWORD" -Pandroid.injected.signing.key.alias="$KEY_ALIAS" -Pandroid.injected.signing.key.password="$KEY_PASSWORD"
else
  ./gradlew bundleRelease
  echo "NOTE: the AAB is unsigned. Set KEYSTORE variables (see README) to sign it for Google Play."
fi
echo "APK: android/app/build/outputs/apk/debug/app-debug.apk"
echo "AAB: android/app/build/outputs/bundle/release/app-release.aab"
