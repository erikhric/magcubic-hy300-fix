#!/bin/sh
# Restore Magcubic kernel splash. /oem/bootlogo.bmp was a valid 1280x720 BMP of zeros —
# lamp on, no logo from t=0. Do not adb-push onto /oem (fchown deletes the file).
# Splash only: does not reset a stalled AV-MIPS mixer (sys.svp_status=0 → unplug 10s).
set -eu
ip=${1:?usage: $0 PROJECTOR_IP_OR_SERIAL}
# shellcheck source=need-adb.sh
. "$(dirname "$0")/need-adb.sh"
need_adb "$ip"
root=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
png=$root/media/bootlogo.png
bmp=$(mktemp /tmp/hy300-bootlogo.XXXXXX.bmp)
python3 "$root/scripts/png2bmp.py" "$png" "$bmp"
adb -s "$ADB_SERIAL" push "$bmp" /sdcard/bootlogo.bmp >/dev/null
adb -s "$ADB_SERIAL" shell 'cp /sdcard/bootlogo.bmp /oem/bootlogo.bmp && rm /sdcard/bootlogo.bmp'
rm -f "$bmp"
echo "bootlogo=$($ADB_SH ls -l /oem/bootlogo.bmp)"
