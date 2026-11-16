# S1 Control by AJ

Android companion app maintained by **AJ (@Ajayraj2126)** for compatible Ola
S1 infotainment units. Current AJ version: **0.2.1-aj**.

[Repository](https://github.com/Ajayraj2126/ola-s1-mods) ·
[Download the latest APK and source](https://github.com/Ajayraj2126/ola-s1-mods/releases/latest) ·
[Release history](https://github.com/Ajayraj2126/ola-s1-mods/releases)

## Features

- Home dashboard showing actual connection, model, MoveOS, setup and activity state.
- Local network discovery and manual connection with a verified diagnostic handshake.
- Mod setup, bundled app management, online/custom APK installation and package removal.
- Display screenshots, model/software/RAM information, settings shortcuts and diagnostic shell.
- Dark teal interface, AJ branding and update checks from this repository.

Version 0.2.1 moves the app-owned Kotlin namespace to
`com.ajayraj2126.s1control`. The Android application ID remains
`com.aj.s1control`.

## Install and connect

1. Open the [latest release](https://github.com/Ajayraj2126/ola-s1-mods/releases/latest)
   and download its Android APK. The phone must run Android 8.0/API 26 or newer.
2. Install and open **S1 Control by AJ**. The distributed APK is a debug-signed
   development build.
3. Connect the scooter to your phone's hotspot and keep its display awake.
4. Grant the requested local network/location permissions, then use automatic
   discovery or enter the scooter address manually. You can also browse offline.

Phone and physical scooter operation still require device validation. The public
source targets the MoveOS 4.0.5+ diagnostic interface; model and firmware
compatibility must be checked on the actual scooter. Use the app while parked
and only on a scooter you own or are authorized to administer.

## Source compatibility

This edition is based on published source that omits several features of the
official v1.4 binary. Dynamic Island, My S1 CAN/SOH/cycles, vehicle lock/boot
controls, full Smart Ball, complete factory restoration and resume reconnect are
not implemented here. See [the source audit](AJ-SOURCE-AUDIT.md).

The mod installation flow can uninstall an existing app and remove its data.
Installed apps and settings may persist after a scooter restart.

## Build

Install JDK 21 and Android SDK Platform 36 / Build Tools 36.0.0. Set
`ANDROID_HOME` to the SDK, or open `android/` in Android Studio and let it create
`local.properties`.

```sh
cd android
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

Windows PowerShell:

```powershell
cd android
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

APK output: `android/app/build/outputs/apk/debug/app-debug.apk`.
See [edition details](AJ-EDITION.md) and [build validation](BUILD-VALIDATION.md).

## Credits and license

AJ maintains this customized edition. Original authorship, research credits and
bundled-app notices are recorded in [NOTICE.md](NOTICE.md). The original
[MIT license](LICENSE) is retained. This project is independent of Ola Electric;
product names identify compatible hardware.
