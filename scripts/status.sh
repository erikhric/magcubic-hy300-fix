#!/bin/sh
# Diagnose lamp-black vs Android-up. Getters only — does not switch HDMI.
# Usage: ./status.sh PROJECTOR_IP_OR_SERIAL
set -eu
ip=${1:?usage: $0 PROJECTOR_IP_OR_SERIAL}
# shellcheck source=need-adb.sh
. "$(dirname "$0")/need-adb.sh"
need_adb "$ip"
s="adb -s $ADB_SERIAL"

echo "wakefulness=$($s shell dumpsys power 2>/dev/null | grep mWakefulness= | head -1)"
echo "svp_status=$($s shell getprop sys.svp_status)"
echo "svp_thal=$($s shell getprop sys.svp_thal_status)"
echo "msp_thal=$($s shell getprop sys.msp_thal_status)"
echo "svp_drv=$($s shell getprop sys.svp_drvload_done)"
echo "video_enable=$($s shell getprop persist.sys.bootanim.video_enable)"
echo "default_source=$($s shell getprop persist.sys.default_source)"
echo "recovery=$($s shell settings get global input_recovery_record)"
echo "home=$($s shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME 2>/dev/null | tail -1)"
echo "awlivetv=$($s shell dumpsys package com.softwinner.awlivetv 2>/dev/null | grep enabled= | head -1)"
if [ -f "$(dirname "$0")/set-source/setsource.dex" ]; then
  $s push "$(dirname "$0")/set-source/setsource.dex" /data/local/tmp/setsource.dex >/dev/null
  $s shell CLASSPATH=/data/local/tmp/setsource.dex app_process /data/local/tmp SetSource dump || true
fi
echo "note: svp_status=0 → unplug (do not adb reboot). VideoDec on a good boot is normal; HDMI overlay is not disabled."
