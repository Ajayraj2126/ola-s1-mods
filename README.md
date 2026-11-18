# 🛵 S1 Control by AJ — Android Companion for MoveOS

[![AJ Release](https://img.shields.io/badge/AJ%20Release-0.3.0--aj-14b8a6.svg)](https://github.com/Ajayraj2126/ola-s1-mods/releases/latest)
[![Android](https://img.shields.io/badge/Android-8.0%2B-blue.svg)](android/README.md)
[![Stack](https://img.shields.io/badge/Stack-Kotlin%20Compose%20%2B%20ADB-brightgreen.svg)](android/)
[![Build](https://img.shields.io/badge/Build-Release%20Validated-blue.svg)](BUILD-VALIDATION.md)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**S1 Control by AJ** is an Android companion app maintained by **AJ (@Ajayraj2126)**
for compatible Ola S1 infotainment units. Connect over your phone's hotspot to
manage mod setup, installed apps, screenshots, settings and diagnostics from one
Compose interface.

[📥 **Download the latest APK and source**](https://github.com/Ajayraj2126/ola-s1-mods/releases/latest)
· [📦 **Release history**](https://github.com/Ajayraj2126/ola-s1-mods/releases)
· [📖 **Edition guide**](AJ-EDITION.md)

---

## ⚡ Quick Start — Android App

1. **Download the APK** from the [latest AJ release](https://github.com/Ajayraj2126/ola-s1-mods/releases/latest).
   Your phone must run **Android 8.0/API 26 or newer**.
2. **Install and open S1 Control by AJ.** Allow the requested discovery permissions.
3. **Turn on your phone's hotspot** and connect the scooter through its Wi-Fi settings.
4. **Keep the scooter parked and its display awake.**
5. Tap **Auto Scan**, then **Connect**. If discovery is unavailable, use **Manual**
   to enter the scooter's IP or MAC address.
6. After the diagnostic handshake succeeds, open **Setup**, **Tools**, **Apps** or
   **Shell** for the action you need. Offline browsing is also available.

> [!NOTE]
> Version 0.3 introduces a stable AJ release-signing configuration. Because older
> 0.2.x downloads used disposable debug certificates, installing 0.3 may require a
> one-time uninstall. Keep the 0.3 signing key for every later update.

> [!IMPORTANT]
> Mod updates are installed in place so AJ does not intentionally erase an existing
> mod's data. Installed applications and settings may persist after a scooter restart.

---

## ✨ Key Features

- 🏠 **AJ Home Dashboard:** Actual connection status, model, MoveOS information,
  setup progress and recent session activity.
- 🔍 **Discovery & Manual Connection:** Local-network discovery, address entry
  and a verified diagnostic handshake before showing a connected scooter.
- 🔄 **Automatic Reconnect:** Waits for hotspot Wi-Fi, retries the last verified
  endpoint with capped delays and never replays a scooter-changing action.
- ⚡ **Mod Setup:** Select bundled apps, authorize ADB and apply the supported
  installation and permission steps from the public source.
- ♻️ **AJ Restoration:** Previews and reverses only verified AJ-owned packages and
  settings while preserving pre-existing or subsequently changed state.
- 📦 **App Management:** Manage bundled apps, download supported online apps,
  install a local APK and remove installed packages.
- 🧰 **Scooter Toolbox:** Capture the display, inspect model/software/RAM
  information and open Android settings panels.
- 💻 **Diagnostic Shell:** An advanced terminal with command presets and
  session output.
- 🎨 **AJ Interface & Updates:** Dark teal styling, flexible dashboard cards,
  owner repository links and update checks from AJ releases.

---

## 📱 Compatibility & Release Scope

| Requirement | Current AJ Edition |
| :--- | :--- |
| **Phone OS** | Android 8.0/API 26 or newer |
| **Target interface** | Published MoveOS 4.0.5+ diagnostics for Android-based infotainment with local Wi-Fi connectivity |
| **Vehicle and firmware** | Confirm compatibility on the exact scooter and firmware |
| **Device validation** | Phone/emulator and physical scooter operation have not been tested for this build |
| **Official v1.4 feature coverage** | Some features are absent from the published source used by this edition |

> [!IMPORTANT]
> This edition customizes the **published source**. Dynamic Island, My S1
> CAN/SOH/cycles, vehicle lock/boot controls, full Smart Ball and an OEM factory
> reset are not implemented here. AJ restoration reverses only changes recorded by
> version 0.3 onward. AJ Edition adds verified Wi-Fi session
> reconnection without replaying scooter changes. See the
> [source compatibility audit](AJ-SOURCE-AUDIT.md) for the verified comparison.

Use the app while parked and only with hardware you own or are authorized to
administer. This edition is independent of Ola Electric.

---

## 📋 Technical Specifications

| Component | Current configuration |
| :--- | :--- |
| **AJ version** | `0.3.0-aj` · version code `9` |
| **Android application ID** | `com.aj.s1control` |
| **App-owned namespace** | `com.ajayraj2126.s1control` |
| **Android SDK** | Minimum `26` · target `35` · compile `36` |
| **Interface** | Kotlin and Jetpack Compose |
| **Diagnostics / app transport** | gRPC on TCP `50005` · ADB on TCP `5555` |
| **Distribution** | Release APK and Android source ZIP on GitHub |
| **Recorded validation** | Unit tests, lint, optimized build, APK identity/signature and artifact checksums |

The [validation report](BUILD-VALIDATION.md) records the local build. The release's
CI validation file records the downloadable APK and its signing certificate.

---

## 🏗️ Build & Test

Install **JDK 21**, **Android SDK Platform 36** and **Build Tools 36.0.0**.
Set `ANDROID_HOME` to your SDK, or open `android/` in Android Studio and let it
create `local.properties`.

From the repository root on Linux or macOS:

```sh
cd android
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

Windows PowerShell:

```powershell
cd android
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

**APK output:** `android/app/build/outputs/apk/debug/app-debug.apk`.

For the module layout and complete toolchain, see the
[Android project guide](android/README.md).

---

## 📁 Repository Structure

```text
ola-s1-mods/
├── android/               # Compose app, bundled assets and unit tests
│   └── README.md          # Android build and usage guide
├── AJ-EDITION.md           # Edition details and installation notes
├── AJ-SOURCE-AUDIT.md      # Public-source and official v1.4 comparison
├── BUILD-VALIDATION.md     # Build checks and artifact details
├── CHANGELOG.md            # AJ changes and historical release notes
├── NOTICE.md               # Original authorship and bundled-app credits
├── LICENSE                 # Preserved MIT license and original notices
├── ola.py                  # Included Python diagnostic controller
├── setup.py                # Included Python setup workflow
└── setup.bat               # Windows setup launcher
```

---

## 👨‍💻 Maintainer, Credits & License

**AJ ([@Ajayraj2126](https://github.com/Ajayraj2126))** maintains this customized
edition. Follow the [repository](https://github.com/Ajayraj2126/ola-s1-mods)
and [releases](https://github.com/Ajayraj2126/ola-s1-mods/releases) for updates.

Original authorship, research acknowledgments and bundled-app credits are
preserved in [NOTICE.md](NOTICE.md) and the [MIT license](LICENSE). The complete
original license is also available through **Open-source licenses** in the app.
Bundled apps retain their own package identities and applicable licenses.

Ola, MoveOS and other product names identify compatible hardware or functionality;
their respective owners retain their trademarks.
