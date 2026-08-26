# Magcubic HY300 Pro+ — black boot + boot-on-plug

Allwinner H713 projector (SpectraOS / Hotack OEM, `persist.sys.deviceName=HY300 Pro+`).
No root required.

## Prerequisites

### 1. `adb` on the computer

The scripts call `adb`. Install platform-tools if `adb version` fails:

- macOS: `brew install android-platform-tools`
- Debian/Ubuntu: `sudo apt install adb`
- Windows: [platform-tools zip](https://developer.android.com/tools/releases/platform-tools) and put it on `PATH`

### 2. Remote debugging on the projector

The computer and projector must be on the **same LAN**. Then enable **network / wireless ADB** (remote debugging), not only a one-shot USB cable:

1. On the projector: **Settings → Other settings → Developer** (or Android **Settings → Device Preferences → About** and click Build until developer options unlock, then **Developer options**).
2. Turn on **USB debugging** and **Network debugging** / **Wireless debugging** / **ADB over network**. Allow this computer if a prompt appears.
3. Note the projector IP (Wi‑Fi status). Port is **`5555`** on these Hotack builds.

Check:

```bash
adb connect PROJECTOR_IP:5555
adb devices -l
# expect:  PROJECTOR_IP:5555    device
```

`unauthorized` = accept the RSA prompt on the projector. `offline` / cannot connect = debugging still off, wrong IP, or not on the same network.

`scripts/apply.sh` and `scripts/set-power/run.sh` refuse to run if `adb` is missing or the device does not come up as `device`.

## Symptoms

- Lamp comes on, **no image** (black). Stays that way forever.
- A working boot shows a **static Magcubic logo** (not a video), then Android.
- Holding the power button until the logo appears is a hard reboot of a system that often **already reached userspace**.
- Boot history looks like clean `shutdown`, not panic. Android was up; the panel stayed black.

Two independent software causes match that:

1. **Classic Allwinner boot-video hang.** `persist.sys.bootanim.video_enable=1` makes `bootanimation` drop the kernel logo, open `MediaPlayer`, and look for `/oem/media/bootvideo.mp4` (and similar paths). This firmware has **no mp4**. You get a black video layer, lamp already on, system_server still running. Sometimes it falls through to the PNG zip (static logo). Sometimes it never gives the layer back.
2. **Last HDMI input restored** (`input_recovery_record=HDMI1`) with nothing plugged in → black picture, Android still running.

OEM **Power mode** default is **standby**, so plugging the adapter does nothing until you press power. That is a separate setting.

## Fix with the remote (no ADB)

1. **Settings → Other settings → Power mode** → **Equipment is powered on and in startup** (not “in standby”).
2. Same screen: **External input during startup** / boot source → **LOCAL** (not HDMI).
3. There is no UI toggle for boot video; use ADB for that.

## Fix over ADB

```bash
adb connect PROJECTOR_IP:5555

# 1) static logo, not missing boot video
adb shell setprop persist.sys.bootanim.video_enable 0

# 2) do not boot into HDMI with no source
adb shell setprop persist.sys.default_source LOCAL
adb shell settings put global input_recovery_record LOCAL
adb shell settings put global input_source_recent_record LOCAL
```

Make the video/source lines survive OEM overlay (this partition is world-writable on these units):

```bash
adb shell grep -q bootanim.video_enable /oem/customer.prop \
  || adb shell 'printf "\n# static logo only; disable boot video\npersist.sys.bootanim.video_enable=0\npersist.sys.default_source=LOCAL\n" >> /oem/customer.prop'
```

Equivalent diff against stock `/oem/customer.prop` (do **not** copy a full `customer.prop` around — it contains channel keys):

```diff
 persist.sys.enable.bootup=0
+# static logo only; disable boot video
+persist.sys.bootanim.video_enable=0
+persist.sys.default_source=LOCAL
```

`persist.sys.enable.bootup` is a **factory-test / setup-wizard flag**, not “boot when power is applied”. Leave it `0`.

### Boot when power is applied

That is Allwinner TV **power-on mode**, stored in U-Boot env via `tvserver`:

| value | name    | meaning                                      |
|------:|---------|----------------------------------------------|
|     0 | STANDBY | plug in → stay off until power button        |
|     1 | DIRECT  | plug in → boot                               |
|     2 | MEMORY  | last state                                   |

Settings UI writes `factorySetPowerMode` (HIDL `vendor.aw.homlet.tvsystem.tvserver@1.0::ITvServer`, transactions 152 set / 153 get). The activity is not exported, so `am start` from the shell cannot open it.

```bash
./scripts/apply.sh PROJECTOR_IP
./scripts/set-power/run.sh PROJECTOR_IP 1
```

`set-power/run.sh` needs `javac` plus Android SDK `d8` (`ANDROID_HOME`) the first time, to build `setpower.dex`. After that it only needs `adb`.

Then `adb reboot` (or unplug — with DIRECT, the next plug should boot).

## What this does *not* fix

- A noisy / weak PSU. Still replace that if it screams.
- Confirmed on a black boot: these units often never leave a `last_kmsg`. Turn on `persist.debug.logpersistd true` if you need logs next time.
- The factory APK `com.htc.hyk_test` runs at `BOOT_COMPLETED` (too late to explain a missing logo from t=0). Do not confuse it with `enable.bootup`.

## Identify the box

```
adb shell getprop persist.sys.modelName          # HY300 Pro+
adb shell getprop ro.hardware                    # sun50iw12p1
adb shell getprop persist.sys.bootanim.video_enable
adb shell CLASSPATH=/data/local/tmp/setpower.dex app_process /data/local/tmp SetPower
```

Kernel logo lives at `/oem/bootlogo.bmp`. OEM bootanimation is a single PNG zip: `/oem/media/bootanimation.zip`. There is no `bootvideo.mp4`.
