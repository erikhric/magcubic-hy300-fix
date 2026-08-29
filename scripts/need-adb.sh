#!/bin/sh
# Fail unless adb exists and the projector is in state "device".
# Usage: need_adb 192.168.68.113
#    or: need_adb 127.0.0.1:15555   (TCP relay serial)
# Sets ADB_SERIAL and ADB_SH (adb -s $ADB_SERIAL shell).
need_adb() {
  spec=$1
  if ! command -v adb >/dev/null 2>&1; then
    echo "adb not found. Install Android platform-tools, then retry." >&2
    echo "  macOS:   brew install android-platform-tools" >&2
    echo "  Debian:  sudo apt install adb" >&2
    echo "  Windows: https://developer.android.com/tools/releases/platform-tools" >&2
    echo "On the projector enable USB debugging + network/wireless ADB (port 5555) and allow this computer." >&2
    exit 1
  fi
  case "$spec" in
    *:*) ADB_SERIAL=$spec ;;
    *) ADB_SERIAL=$spec:5555 ;;
  esac
  adb connect "$ADB_SERIAL" >/dev/null
  state=$(adb -s "$ADB_SERIAL" get-state 2>/dev/null || true)
    if [ "$state" != device ]; then
    echo "adb cannot talk to $ADB_SERIAL (state=${state:-unreachable})." >&2
    echo "Same Wi-Fi? Remote debugging on? RSA prompt allowed?  Try: adb connect $ADB_SERIAL && adb devices -l" >&2
    if [ "$state" = offline ]; then
      echo "offline after a HIDL setter hang: adb kill-server, reconnect. Do not setprop sys.powerctl shutdown." >&2
    fi
    exit 1
  fi
  ADB_SH="adb -s $ADB_SERIAL shell"
}
