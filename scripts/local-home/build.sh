#!/bin/sh
# Build localhome.apk (BOOT_COMPLETED → HOME only; no tvserver HIDL).
set -eu
cd "$(dirname "$0")"
BT=$(ls "$ANDROID_HOME"/build-tools/*/aapt2 2>/dev/null | tail -1)
BT=$(dirname "$BT")
AJ=$(ls "$ANDROID_HOME"/platforms/android-*/android.jar | tail -1)
[ -n "$BT" ] && [ -n "$AJ" ] || { echo "need ANDROID_HOME build-tools + platforms"; exit 1; }
rm -rf out gen classes.dex
mkdir -p out gen
"$BT/aapt2" link -o out/unsigned.apk --manifest AndroidManifest.xml -I "$AJ" \
  --min-sdk-version 26 --target-sdk-version 30 --version-code 2 --version-name 2
mkdir -p stubs_out
javac --release 8 -cp "$AJ" -d out BootReceiver.java GoActivity.java
"$BT/d8" --min-api 26 --lib "$AJ" --output . out/com/hy300/localhome/*.class
# aapt2 unsigned apk is a zip; inject dex
zip -j out/unsigned.apk classes.dex >/dev/null
"$BT/zipalign" -f 4 out/unsigned.apk out/aligned.apk
"$BT/apksigner" sign --ks "$HOME/.android/debug.keystore" --ks-pass pass:android \
  --key-pass pass:android --out localhome.apk out/aligned.apk
rm -f classes.dex
echo "built $PWD/localhome.apk"
