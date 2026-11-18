# 🛵 S1 Control by AJ — Android Application

[![AJ Release](https://img.shields.io/badge/AJ%20Release-0.3.0--aj-14b8a6.svg)](https://github.com/Ajayraj2126/ola-s1-mods/releases/latest)
[![Android](https://img.shields.io/badge/Android-8.0%2B-blue.svg)](app/build.gradle.kts)
[![Stack](https://img.shields.io/badge/Stack-Kotlin%20Compose%20%2B%20ADB-brightgreen.svg)](app/)
[![Build](https://img.shields.io/badge/Build-Release%20Candidate-orange.svg)](../BUILD-VALIDATION.md)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](../LICENSE)

**S1 Control by AJ** is an Android companion app maintained by **AJ (@Ajayraj2126)**
for compatible Ola S1 infotainment units. Connect over your phone's hotspot to
manage mod setup, installed apps, screenshots, settings and diagnostics from one
Compose interface.

[📥 **Download the latest APK and source**](https://github.com/Ajayraj2126/ola-s1-mods/releases/latest)
· [💻 **Repository**](https://github.com/Ajayraj2126/ola-s1-mods)
· [📖 **Main guide**](../README.md)

---

## ⚡ Quick Start — Phone Hotspot

1. **Download the APK** from the [latest AJ release](https://github.com/Ajayraj2126/ola-s1-mods/releases/latest).
   Your phone must run **Android 8.0/API 26 or newer**.
2. **Install and open S1 Control by AJ.** Allow the requested discovery permissions.
3. **Turn on your phone's hotspot** and connect the scooter through its Wi-Fi settings.
4. **Keep the scooter parked and its display awake.**
5. Tap **Auto Scan**, then **Connect**. If discovery is unavailable, use **Manual**
   to enter the scooter's IP or MAC address. A diagnostic handshake must succeed
   before the app reports a connected scooter.
6. **Choose the screen you need.** For mod setup, review the selected apps and
   setup prompt before starting. You can also browse the app offline.

> [!NOTE]
> Version 0.3 uses the AJ release-signing configuration. Older 0.2.x debug builds
> can require a one-time uninstall before 0.3 is installed.

> [!IMPORTANT]
> Mod updates preserve existing app data. AJ restoration removes only changes it
> recorded and can still verify. Use the app
> only with a scooter you own or are authorized to administer.

---

## ✨ Key Features

- **AJ Home dashboard:** Actual connection, model, MoveOS, setup and recent-activity state.
- **Hotspot discovery:** Automatic local discovery or manual MAC/IP entry, followed by a diagnostic handshake.
- **Automatic reconnect:** Saved endpoint retries with capped backoff after hotspot Wi-Fi returns.
- **Mod setup and app management:** Bundled apps, online/custom APK installation and package removal.
- **AJ restoration:** Conservative preview and rollback of AJ-owned packages and settings.
- **Diagnostics and tools:** Display screenshots, model/software/RAM information and Android settings shortcuts.
- **AJ ownership and updates:** Dark teal interface, AJ repository/release links and AJ-only update checks.

### 📱 App Screens

| Screen | What you can do |
| :--- | :--- |
| **Home** | Review connection details, setup progress, recent activity and quick access controls. |
| **Setup** | Discover/connect to the scooter, choose bundled apps and run the setup flow. |
| **Tools** | Capture screenshots, inspect system information and open settings panels. |
| **Apps** | Manage installed packages and install bundled, online or custom APKs. |
| **Shell** | Run advanced diagnostic commands and copy their output. |

---

## 📋 Toolchain & Application Identity

| Component | Current value |
| :--- | :--- |
| **AJ version / code** | `0.3.0-aj` / `9` |
| **Phone minimum** | Android 8.0 / API 26 |
| **Compile / target SDK** | API 36 / API 35 |
| **JDK** | 21 |
| **Gradle / Android Gradle Plugin** | 9.4.1 / 9.0.1 |
| **Kotlin / Compose compiler** | 2.2.10 |
| **Android Build Tools** | 36.0.0 |
| **App-owned namespace** | `com.ajayraj2126.s1control` |
| **Installation application ID** | `com.aj.s1control` |

---

## 🏗️ Build & Test

Open this `android/` directory in **Android Studio**, or set `ANDROID_HOME` to
your Android SDK and use the included Gradle wrapper. Install **SDK Platform 36**
and **Build Tools 36.0.0**.

Run these commands **from the `android/` directory**.

**Linux / macOS:**

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

**Windows PowerShell:**

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

**APK output:**

```text
app/build/outputs/apk/debug/app-debug.apk
```

The existing unit tests cover AJ release version comparisons and APK filename
filtering. Build and artifact checks are recorded in
[BUILD-VALIDATION.md](../BUILD-VALIDATION.md); release attachments include the
CI rebuild's validation and checksums.

---

## 📁 Android Project Structure

```text
android/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── assets/                  # Bundled apps and original license
│       │   └── java/com/ajayraj2126/s1control/
│       │       ├── MainActivity.kt
│       │       ├── adb/                 # ADB client integration
│       │       ├── discovery/           # Local network discovery
│       │       ├── grpc/                # Diagnostic client
│       │       ├── model/               # Connection and app-management state
│       │       ├── ui/                  # Compose screens and theme
│       │       └── util/                # Downloads and AJ update checks
│       └── test/                        # Unit tests
├── gradle/                             # Version catalog and wrapper
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
└── gradlew.bat
```

---

## 🔎 Compatibility & Release Scope

The source targets the **MoveOS 4.0.5+ diagnostic interface**. Phone/emulator and
physical scooter operation have not been validated for this AJ build; model and
firmware compatibility still require checking on the actual device.

The published source omits some official v1.4 binary features, including Dynamic
Island, CAN battery-health screens and the full Smart Ball implementation.
Read the [source compatibility audit](../AJ-SOURCE-AUDIT.md) and
[AJ Edition guide](../AJ-EDITION.md) for the current scope.

---

## 👨‍💻 Maintainer, Credits & License

Maintained by **AJ (@Ajayraj2126)**. Original authorship and bundled-app credits
are retained in [NOTICE.md](../NOTICE.md) and the [MIT license](../LICENSE).
The app's **Open-source licenses** dialog displays the original full license.
Product names describe compatible hardware or functionality; this edition is
independent of Ola Electric.
