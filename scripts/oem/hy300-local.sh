#!/system/bin/sh
# Force Android framebuffer + HOME after boot. Runs as shell from oem init when imported.
# ponytail: tvserver can re-assert last HDMI; three shots with a short gap.
dex=/oem/setsource.dex
[ -f "$dex" ] || dex=/data/local/tmp/setsource.dex
i=0
while [ "$i" -lt 3 ]; do
  if [ -f "$dex" ]; then
    CLASSPATH="$dex" app_process /data/local/tmp SetSource image >/dev/null 2>&1
  fi
  am start -a android.intent.action.MAIN -c android.intent.category.HOME >/dev/null 2>&1
  i=$((i + 1))
  sleep 5
done
