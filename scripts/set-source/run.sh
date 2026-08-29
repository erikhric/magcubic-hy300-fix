#!/bin/sh
# HIDL helper for vendor.aw.homlet.tvsystem.tvserver@1.0::ITvServer/default.
# Getters are safe. Setters hang tvserver if AV-MIPS is stalled (sys.svp_status=0).
# apply.sh / boot helpers never call set / svpstop / svpstart.
#
#   run.sh IP dump                 # GetSource + HDMI port + HAL dump (safe)
#   run.sh IP get                  # GetSource only (safe)
#   run.sh IP image | local        # Unblack + Uncover only — does NOT switch source
#   run.sh IP set 0                # Dummy=LOCAL — HANGS if svp_status=0
#   run.sh IP set 1 | 3 | 4 | 5    # VideoDec / HDMI_1 / HDMI_2 / HDMI_3
#   run.sh IP svpstop | svpstart   # HANGS if MIPS is stalled — do not use
#
# dump on a GOOD boot still shows current=1 (VideoDec). That is normal.
# These scripts do not disable the HDMI overlay. Black lamp → unplug 10s, not adb reboot.
#
# Needs: ANDROID_HOME (or ANDROID_SDK_ROOT) with d8 the first time.
set -eu
ip=${1:?usage: $0 PROJECTOR_IP_OR_SERIAL [dump|get|image|set N|unblack]}
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

# Refuse setters when the mixer is stalled. Getters stay fast until a setter wedges tvserver.
cmd=${1-}
case "$cmd" in
  set|svpstop|svpstart|vpdeinit|vpinit)
    svp=$(adb -s "$ADB_SERIAL" shell getprop sys.svp_status | tr -d '\r')
    if [ "$svp" = 0 ]; then
      echo "refuse: sys.svp_status=0 (AV-MIPS stalled). Unplug ~10s; do not $cmd." >&2
      echo "getters are safe: $0 $ip dump" >&2
      echo "HDMI overlay is not something these scripts turn off." >&2
      exit 1
    fi
    ;;
esac

adb -s "$ADB_SERIAL" push setsource.dex /data/local/tmp/setsource.dex >/dev/null
if [ $# -gt 0 ]; then
  adb -s "$ADB_SERIAL" shell CLASSPATH=/data/local/tmp/setsource.dex app_process /data/local/tmp SetSource "$@"
else
  adb -s "$ADB_SERIAL" shell CLASSPATH=/data/local/tmp/setsource.dex app_process /data/local/tmp SetSource dump
fi
