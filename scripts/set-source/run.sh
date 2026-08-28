#!/bin/sh
# Read/set Allwinner panel source. Usage: ./run.sh PROJECTOR_IP [image|set N|unblack]
# 2=Image (LOCAL/Android)  1=VideoDec (HDMI decoder, black if no cable)
set -eu
ip=${1:?usage: $0 PROJECTOR_IP_OR_SERIAL [image|set N|unblack]}
shift
# shellcheck source=../need-adb.sh
. "$(dirname "$0")/../need-adb.sh"
need_adb "$ip"
cd "$(dirname "$0")"
stubs=../set-power/stubs
if [ ! -f setsource.dex ]; then
  javac --release 8 -d stubs_out "$stubs"/android/os/*.java
  jar cf stubs.jar -C stubs_out .
  mkdir -p out
  javac --release 8 -cp stubs.jar -d out SetSource.java
  d8=$(ls "$ANDROID_HOME"/build-tools/*/d8 2>/dev/null | tail -1)
  [ -n "$d8" ] || d8=$(command -v d8)
  [ -n "$d8" ] || { echo "need Android SDK d8 (ANDROID_HOME)"; exit 1; }
  "$d8" --output . out/SetSource.class
  mv classes.dex setsource.dex
fi
adb -s "$ADB_SERIAL" push setsource.dex /data/local/tmp/setsource.dex >/dev/null
if [ $# -gt 0 ]; then
  adb -s "$ADB_SERIAL" shell CLASSPATH=/data/local/tmp/setsource.dex app_process /data/local/tmp SetSource "$@"
else
  adb -s "$ADB_SERIAL" shell CLASSPATH=/data/local/tmp/setsource.dex app_process /data/local/tmp SetSource
fi
