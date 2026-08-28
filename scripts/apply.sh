#!/bin/sh
# Persist HY300 Pro+ black-boot workarounds. Usage: ./apply.sh 192.168.x.x
set -eu
ip=${1:?usage: $0 PROJECTOR_IP_OR_SERIAL}
# shellcheck source=need-adb.sh
. "$(dirname "$0")/need-adb.sh"
need_adb "$ip"
s="adb -s $ADB_SERIAL"

$s shell setprop persist.sys.bootanim.video_enable 0
$s shell setprop persist.sys.default_source LOCAL
$s shell settings put global input_recovery_record LOCAL
$s shell settings put global input_source_recent_record LOCAL

# OEM overlay re-applies on boot; append if missing. Leave enable.bootup=0.
if ! $s shell grep -q persist.sys.bootanim.video_enable /oem/customer.prop; then
  $s shell 'printf "\n# static logo only; disable boot video\npersist.sys.bootanim.video_enable=0\npersist.sys.default_source=LOCAL\n" >> /oem/customer.prop'
fi

echo "video_enable=$($s shell getprop persist.sys.bootanim.video_enable)"
echo "default_source=$($s shell getprop persist.sys.default_source)"
echo "enable.bootup=$($s shell getprop persist.sys.enable.bootup)  (must stay 0)"
echo "Now set power mode DIRECT:  scripts/set-power/run.sh $ip 1"
echo "If boot is black from t=0 (no Magcubic logo):  scripts/restore-bootlogo.sh $ip"
echo "If lamp is on but picture is HDMI-black after Android is up:  scripts/set-source/run.sh $ip image"
