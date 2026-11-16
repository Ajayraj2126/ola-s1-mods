# S1 Control by AJ — 0.2.1

Customized Android companion app maintained by **AJ (@Ajayraj2126)**.

- [Repository](https://github.com/Ajayraj2126/ola-s1-mods)
- [Latest APK and Android source ZIP](https://github.com/Ajayraj2126/ola-s1-mods/releases/latest)
- [Release history](https://github.com/Ajayraj2126/ola-s1-mods/releases)

AJ version: **0.2.1-aj**, version code **8**.
Android application ID: `com.aj.s1control`.
App-owned Kotlin namespace: `com.ajayraj2126.s1control`.

## This edition

The midnight-blue/teal interface includes a Home dashboard that reads live app
state. Setup, Tools, Apps and Shell provide network discovery, ADB authorization,
diagnostics, bundled app management, online/custom APK installation, screenshots
and settings shortcuts.

Connection success requires a diagnostic handshake. Failed connections do not
publish a connected state, closed clients are cleared before reconnecting, and
disconnect requires confirmation. Screen insets, scrolling and flexible dashboard
cards support smaller screens and larger text. AJ update notices use releases
from this repository and require an AJ-named APK asset.

Version 0.2.1 replaces active owner links and app-owned source identifiers with
Ajayraj2126 branding. Original authorship and license information are retained in
[NOTICE.md](NOTICE.md) and [LICENSE](LICENSE). Existing bundled-app package IDs
remain the identities declared by those APKs so installation and launch commands
continue to address the correct apps.

## Public-source compatibility

The published source tagged v1.4 points to
`e6c82b2c640381f183a2011bd9e2f0e383f83ef5`, but its Android source still declares
version 1.2 and omits several features in the released binary. The official v1.4
APK was downloaded and its GitHub SHA-256 verified during the source audit; its
Smart Ball and CAN assets are absent from this source.

Dynamic Island, My S1 CAN/SOH/cycles, vehicle lock/boot controls, full Smart Ball,
complete factory restoration and resume reconnect are not implemented here.
See [the source audit](AJ-SOURCE-AUDIT.md) for the verified comparison.

## Build

Use JDK 21, Android SDK Platform 36 and Build Tools 36.0.0. The project uses
Gradle 9.4.1, Android Gradle Plugin 9.0.1 and Kotlin/Compose compiler 2.2.10.
Set `ANDROID_HOME` to the SDK or let Android Studio create
`android/local.properties`.

```sh
cd android
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

Windows PowerShell:

```powershell
cd android
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

APK: `android/app/build/outputs/apk/debug/app-debug.apk`.
The APK is a debug-signed development build. Android 8.0/API 26 is the minimum.
For future AJ releases, use an APK name beginning `S1-Control-AJ-Edition` and a
numeric tag such as `v0.3.0-aj` so the update checker can identify it.

## Install note

Downloaded development APKs may use a different debug signing certificate from
version 0.2.0. If Android reports a signing conflict, uninstall the older AJ app
before installing this APK. Uninstalling removes local AJ app data, including
settings. The application ID remains `com.aj.s1control`; a matching ID alone does
not guarantee that differently signed builds can update one another.

## Use and validation

Install the APK on your phone, connect the scooter to the phone's hotspot and
keep its screen awake. Use discovery or a manual address; the dashboard opens
only after a successful diagnostic connection. Offline browsing is also available.

The source targets the MoveOS 4.0.5+ diagnostic interface. Phone and scooter
operation still require device testing. Use only on authorized hardware while
parked. Mod installation can uninstall an existing copy and remove its app data;
installed apps and settings may persist after restarting the scooter.

Build results and artifact digests are recorded in [BUILD-VALIDATION.md](BUILD-VALIDATION.md).
