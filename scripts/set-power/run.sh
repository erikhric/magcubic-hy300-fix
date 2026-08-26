#!/bin/sh
# Read/set Allwinner power-on mode. Usage: ./run.sh PROJECTOR_IP [0|1|2]
# 0=STANDBY  1=DIRECT (boot on plug)  2=MEMORY
set -eu
ip=${1:?usage: $0 PROJECTOR_IP [mode]}
mode=${2-}
cd "$(dirname "$0")"
if [ ! -f setpower.dex ]; then
  javac --release 8 -d stubs_out stubs/android/os/*.java
  jar cf stubs.jar -C stubs_out .
  mkdir -p out
  javac --release 8 -cp stubs.jar -d out SetPower.java
  d8=$(ls "$ANDROID_HOME"/build-tools/*/d8 2>/dev/null | tail -1)
  [ -n "$d8" ] || d8=$(command -v d8)
  [ -n "$d8" ] || { echo "need Android SDK d8 (ANDROID_HOME)"; exit 1; }
  "$d8" --output . out/SetPower.class
  mv classes.dex setpower.dex
fi
adb connect "$ip:5555"
adb -s "$ip:5555" push setpower.dex /data/local/tmp/setpower.dex >/dev/null
if [ -n "$mode" ]; then
  adb -s "$ip:5555" shell CLASSPATH=/data/local/tmp/setpower.dex app_process /data/local/tmp SetPower "$mode"
else
  adb -s "$ip:5555" shell CLASSPATH=/data/local/tmp/setpower.dex app_process /data/local/tmp SetPower
fi
