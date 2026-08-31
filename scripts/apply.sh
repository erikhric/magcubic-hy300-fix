#!/bin/sh
# Persist Android-side lock: Magcubic splash, no boot video, LOCAL settings, Live TV off, HOME.
# Does not disable the HDMI overlay at the MIPS mixer. Black lamp + Android up → unplug, not adb reboot.
# Usage: ./apply.sh PROJECTOR_IP_OR_SERIAL
set -eu
ip=${1:?usage: $0 PROJECTOR_IP_OR_SERIAL}
root=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
# shellcheck source=need-adb.sh
. "$(dirname "$0")/need-adb.sh"
need_adb "$ip"
s="adb -s $ADB_SERIAL"

$s shell setprop persist.sys.bootanim.video_enable 0
$s shell setprop persist.sys.default_source LOCAL
$s shell setprop persist.sys.homePackage com.spocky.projengmenu
$s shell setprop persist.sys.AliveAppName com.spocky.projengmenu
$s shell settings put global input_recovery_record LOCAL
$s shell settings put global input_source_recent_record LOCAL

if ! $s shell grep -q persist.sys.bootanim.video_enable /oem/customer.prop; then
  $s shell 'printf "\n# static logo only; disable boot video\npersist.sys.bootanim.video_enable=0\npersist.sys.default_source=LOCAL\n" >> /oem/customer.prop'
fi

# Magcubic kernel splash (zeros BMP = black from t=0)
"$root/scripts/restore-bootlogo.sh" "$ip"

# HIDL dump helper for status.sh. Boot script no longer calls SetSource (setters hang MIPS).
if [ ! -f "$root/scripts/set-source/setsource.dex" ]; then
  "$root/scripts/set-source/run.sh" "$ip" dump >/dev/null
fi
$s push "$root/scripts/set-source/setsource.dex" /data/local/tmp/setsource.dex >/dev/null
$s push "$root/scripts/set-source/setsource.dex" /sdcard/setsource.dex >/dev/null
$s shell 'cp /sdcard/setsource.dex /oem/setsource.dex; rm /sdcard/setsource.dex'
$s push "$root/scripts/oem/hy300-local.sh" /sdcard/hy300-local.sh >/dev/null
$s shell 'cp /sdcard/hy300-local.sh /oem/hy300-local.sh; chmod 755 /oem/hy300-local.sh; rm /sdcard/hy300-local.sh'
# This unit's vendor init does not import /oem; disable-user + Projectivy HOME is the persist path.
$s push "$root/scripts/oem/init.hy300.rc" /sdcard/init.hy300.rc >/dev/null
$s shell 'cp /sdcard/init.hy300.rc /oem/init.hy300.rc; rm /sdcard/init.hy300.rc'
# If vendor already imports an oem rc, append our trigger once.
if $s shell grep -q 'import /oem/' /vendor/etc/init/*.rc /system/etc/init/*.rc 2>/dev/null; then
  if ! $s shell grep -q hy300-local /oem/init.oem.rc 2>/dev/null; then
    $s shell 'printf "\nimport /oem/init.hy300.rc\n" >> /oem/init.oem.rc' 2>/dev/null || true
  fi
fi

# Disable Live TV / factory test so they are less likely to request HDMI at boot.
# HDMI later: pm enable com.softwinner.awlivetv
$s shell pm disable-user --user 0 com.softwinner.awlivetv >/dev/null || true
$s shell pm disable-user --user 0 com.htc.hyk_test >/dev/null || true

$s shell 'cmd package set-home-activity com.spocky.projengmenu/.ui.home.MainActivity' >/dev/null || true

# v1 localhome had a launcher icon and poked tvserver HIDL — opening it froze the box.
$s shell am force-stop com.hy300.localhome >/dev/null 2>&1 || true
$s uninstall com.hy300.localhome >/dev/null 2>&1 || true

# HOME only. Do not SetSource Dummy/Image or DeviceSvpStop — those RPC-hang AV-MIPS when stalled.
$s shell 'am start -a android.intent.action.MAIN -c android.intent.category.HOME' >/dev/null || true

echo "video_enable=$($s shell getprop persist.sys.bootanim.video_enable)"
echo "default_source=$($s shell getprop persist.sys.default_source)"
echo "svp_status=$($s shell getprop sys.svp_status)"
echo "home=$($s shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME 2>/dev/null | tail -1)"
echo "awlivetv=$($s shell dumpsys package com.softwinner.awlivetv 2>/dev/null | grep enabled= | head -1)"
echo "done. HDMI overlay is not off (GetSource still VideoDec on a good boot)."
echo "if the wall is black: unplug 10s, plug in. do not adb reboot."
