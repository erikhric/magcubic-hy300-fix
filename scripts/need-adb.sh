#!/bin/sh
# Fail unless adb exists and PROJECTOR_IP:5555 is in state "device".
need_adb() {
  ip=$1
  if ! command -v adb >/dev/null 2>&1; then
    echo "adb not found. Install Android platform-tools, then retry." >&2
    echo "  macOS:   brew install android-platform-tools" >&2
    echo "  Debian:  sudo apt install adb" >&2
    echo "  Windows: https://developer.android.com/tools/releases/platform-tools" >&2
    echo "On the projector enable USB debugging + network/wireless ADB (port 5555) and allow this computer." >&2
    exit 1
  fi
  adb connect "$ip:5555" >/dev/null
  state=$(adb -s "$ip:5555" get-state 2>/dev/null || true)
  if [ "$state" != device ]; then
    echo "adb cannot talk to $ip:5555 (state=${state:-unreachable})." >&2
    echo "Same Wi-Fi? Remote debugging on? RSA prompt allowed?  Try: adb connect $ip:5555 && adb devices -l" >&2
    exit 1
  fi
}
