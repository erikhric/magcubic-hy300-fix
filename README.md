# Magcubic HY300 Pro+ — black boot + boot-on-plug

Allwinner H713 projector (SpectraOS / Hotack OEM, `persist.sys.deviceName=HY300 Pro+`).
No root required.

## If the wall is black and the lamp is on

**Unplug the power cord, wait ~10 seconds, plug it back in.** Do not `adb reboot` and do not hold the power button expecting that to reset the picture.

Android can already be up (ADB, screenshots show Projectivy) while the lamp stays black. The picture is mixed on a second CPU (**AV-MIPS**). A software reboot restarts Android only; that MIPS block stays wedged. A cord pull power-cycles it.

Check (ADB, getters only — do not run `set` / `svpstop` / `svpstart` on a black box):

```bash
./scripts/status.sh PROJECTOR_IP    # or 127.0.0.1:15555 through the TCP relay
```

| `sys.svp_status` | Meaning |
|------------------|---------|
| `1` | MIPS video path came up. A picture is possible. `GetSource` still often says `VideoDec` even with Android on the wall — that is **not** “HDMI overlay forced off”. |
| `0` | MIPS stalled. Lamp on, wall black, `screencap` still shows the launcher. Unplug. |

These scripts **do not disable the HDMI overlay**. On a working boot, `tvserver` still dumps `SourceType: kHalSourceID_VideoDec` and `hdmiPort=-1`. A picture after unplugging is the MIPS reset, not this repo turning HDMI off. HDMI still works later: `adb shell pm enable com.softwinner.awlivetv`.

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

`scripts/apply.sh`, `scripts/status.sh`, `scripts/set-power/run.sh`, `scripts/set-source/run.sh`, and `scripts/restore-bootlogo.sh` refuse to run if `adb` is missing or the device does not come up as `device`.

macOS “Local Network” permission can block `adb` from reaching `PROJECTOR_IP:5555`. A TCP relay on loopback works: `python3 /tmp/adb-relay.py 15555 PROJECTOR_IP 5555`, then pass `127.0.0.1:15555` as the serial to the scripts.

## Symptoms

- Lamp comes on, **no image** (black). Stays that way forever.
- A working boot shows a **static Magcubic logo** (not a video), then Android.
- Holding the power button until the logo appears is a hard reboot of **Android**. The AV-MIPS mixer often **does not** reset; ADB comes back, wall stays black.
- Boot history looks like clean `shutdown`, not panic. Android was up; the panel stayed black.
- **No Magcubic logo from the first frame** (lamp already on, wall black) can be a zeros `/oem/bootlogo.bmp` **or** MIPS never compositing (logo file is fine, lamp still black). ADB screenshots can still show the launcher.

Stacked causes:

1. **Black kernel splash.** `/oem/bootlogo.bmp` was a valid 1280×720 24-bit BMP of zeros. U-Boot/kernel can put that on the panel from t=0. The Magcubic mark also lives in `/oem/media/bootanimation.zip` (`part0/01.png`) and only appears if bootanimation composites. `adb push` **directly onto** `/oem/bootlogo.bmp` fails `fchown` and can **delete** the file; push to `/sdcard` then `cp`.
2. **Classic Allwinner boot-video hang.** `persist.sys.bootanim.video_enable=1` makes `bootanimation` drop the kernel logo, open `MediaPlayer`, and look for `/oem/media/bootvideo.mp4`. This firmware has **no mp4**. Do not set `video_enable=1`.
3. **Android asked for HDMI.** SpectraOS `startSource("HDMI1")` launches `com.softwinner.awlivetv` with extra `input_source`. `input_recovery_record=HDMI1` is the settings-level form. `apply.sh` disables Live TV and writes `LOCAL` so Android is less likely to **request** HDMI at boot. That is not a hardware mux off.
4. **AV-MIPS stalled** (`sys.svp_status=0`). HIDL `SubDeviceSetSource` to Dummy (`kHalSourceID_Dummy=0`, the LOCAL plane) reaches `THal_Vp_SetSource` then **hangs** on RPC (`Slave is not ready` / reset). `DeviceSvpStop` / `DeviceSvpStart` hang the same way. Getters (`dump`, `GetSource`) stay fast until a setter wedges `tvserver`. **Unplug.**

OEM **Power mode** default is **standby**, so plugging the adapter does nothing until you press power. That is a separate setting (`scripts/set-power/run.sh`).

## Fix with the remote (no ADB)

1. If the wall is black: **unplug**, wait, plug in (not a long power-button hold).
2. **Settings → Other settings → Power mode** → **Equipment is powered on and in startup** (not “in standby”).
3. Same screen: **External input during startup** / boot source → **LOCAL** (not HDMI).
4. There is no UI toggle for boot video; use ADB for that.
5. Do not press OK on Projectivy’s **HDMI 1** tile if you want Android on the lamp.

## Fix over ADB

```bash
adb connect PROJECTOR_IP:5555
./scripts/apply.sh PROJECTOR_IP
./scripts/status.sh PROJECTOR_IP
```

`apply.sh` does **not** turn HDMI off at the HAL. It:

- restores the Magcubic kernel splash
- keeps boot video off and `persist.sys.default_source=LOCAL`
- `pm disable-user` on `com.softwinner.awlivetv` and factory `com.htc.hyk_test`
- sets Projectivy as HOME and installs `com.hy300.localhome` (BOOT_COMPLETED → HOME)
- copies `/oem/hy300-local.sh` in case some build imports `/oem` (this unit’s vendor init **does not**)

It does **not** call `SetSource` Dummy / Image-as-HDMI2 / `svpstop` (those hang MIPS). After apply, if the wall is still black, unplug — do not `adb reboot`.

HDMI later: `adb shell pm enable com.softwinner.awlivetv`.

## Scripts

| Script | Role |
|--------|------|
| `scripts/status.sh` | Diagnose. Getters only (`svp_status`, HOME, Live TV, HIDL dump). |
| `scripts/apply.sh` | Persist lock (splash, props, disable-user, HOME). **No live mux switch.** |
| `scripts/restore-bootlogo.sh` | Magcubic `/oem/bootlogo.bmp` via sdcard then `cp`. Splash only. |
| `scripts/set-power/run.sh` | `factorySetPowerMode` DIRECT=1 (boot on plug). Does not reset MIPS. |
| `scripts/set-source/run.sh` | HIDL helper. Refuses `set` / `svpstop` / `svpstart` when `svp_status=0`. |
| `scripts/oem/hy300-local.sh` | HOME loop if vendor ever imports `/oem` (this unit does not). No HIDL. |
| `scripts/local-home/` | `BOOT_COMPLETED` → HOME. No tvserver calls. |

Do not run `set-source` setters, `loadmips`, or `svp-suspend` on a black lamp. Those hang `tvserver` (`Slave is not ready`) and can take ADB `offline`. Recover with `adb kill-server` and reconnect; do not `setprop sys.powerctl shutdown`.

Manual pieces:

```bash
# 1) static logo, not missing boot video
adb shell setprop persist.sys.bootanim.video_enable 0

# 2) do not let Settings/Live TV prefer HDMI with no cable
adb shell setprop persist.sys.default_source LOCAL
adb shell settings put global input_recovery_record LOCAL
adb shell settings put global input_source_recent_record LOCAL
```

Make the video/source lines survive OEM overlay (`/oem` is world-writable on these units):

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

HIDL helper (getters are safe; avoid `set` / `svpstop` / `svpstart` when `svp_status=0`):

```bash
./scripts/set-source/run.sh PROJECTOR_IP dump
```

`portmap.cfg` maps Android TIF ports 1/2/3 → HDMI1/2/3. HAL `TvSourceID` Dummy=0, VideoDec=1, Image=2. `SetSource(2)` is **not** a reliable “LOCAL” switch on this firmware. Dummy=0 is LOCAL in HAL logs, but the RPC hangs if MIPS is down.

### Boot when power is applied

That is Allwinner TV **power-on mode**, stored in U-Boot env via `tvserver` (shell cannot read `/dev/block/by-name/env_*`):

| value | name    | meaning                                      |
|------:|---------|----------------------------------------------|
|     0 | STANDBY | plug in → stay off until power button        |
|     1 | DIRECT  | plug in → boot                               |
|     2 | MEMORY  | last state                                   |

Settings UI writes `factorySetPowerMode` (HIDL `vendor.aw.homlet.tvsystem.tvserver@1.0::ITvServer`, transactions 152 set / 153 get). The activity is not exported, so `am start` from the shell cannot open it.

```bash
./scripts/restore-bootlogo.sh PROJECTOR_IP
./scripts/set-power/run.sh PROJECTOR_IP 1
```

`set-power/run.sh` / `set-source/run.sh` need `javac` plus Android SDK `d8` (`ANDROID_HOME`) the first time, to build the dex. After that they only need `adb`. `restore-bootlogo.sh` needs `python3` (stdlib only).

With DIRECT, the **next plug after an unplug** should boot. `adb reboot` is the wrong tool for a black lamp.

## What this does *not* fix

- HDMI overlay at the MIPS mixer. `GetSource=VideoDec` on a good boot is normal.
- A noisy / weak PSU. Still replace that if it screams.
- Confirmed on a black boot: these units often never leave a `last_kmsg`. Turn on `persist.debug.logpersistd true` if you need logs next time.

## Identify the box

```
adb shell getprop persist.sys.modelName          # HY300 Pro+
adb shell getprop ro.hardware                    # sun50iw12p1
adb shell getprop persist.sys.bootanim.video_enable
adb shell getprop sys.svp_status                 # 1=MIPS up, 0=unplug
adb shell CLASSPATH=/data/local/tmp/setpower.dex app_process /data/local/tmp SetPower
```

Kernel logo lives at `/oem/bootlogo.bmp` (restore from `media/bootlogo.png` via `scripts/restore-bootlogo.sh`). OEM bootanimation is a single PNG zip: `/oem/media/bootanimation.zip`. There is no `bootvideo.mp4`.
