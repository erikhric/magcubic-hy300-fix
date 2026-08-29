#!/system/bin/sh
# Send HOME after boot if vendor ever imports /oem (this unit does not).
# Do not call SetSource Dummy/svpstop — HIDL setters hang when AV-MIPS is stalled.
i=0
while [ "$i" -lt 3 ]; do
  am start -a android.intent.action.MAIN -c android.intent.category.HOME >/dev/null 2>&1
  i=$((i + 1))
  sleep 5
done
