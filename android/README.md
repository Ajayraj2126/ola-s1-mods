# S1 Control by AJ — Android project

Android companion app maintained by **AJ (@Ajayraj2126)**.
Version **0.2.1-aj**, minimum Android 8.0/API 26.

[Repository](https://github.com/Ajayraj2126/ola-s1-mods) ·
[Latest APK and source](https://github.com/Ajayraj2126/ola-s1-mods/releases/latest)

The Compose app provides the AJ Home dashboard, network discovery, diagnostic
connection, mod setup, app management, screenshots, settings shortcuts and shell.
App-owned code uses namespace `com.ajayraj2126.s1control`; the installation
application ID remains `com.aj.s1control`.

## Build and test

Use JDK 21, SDK Platform 36 and Android Build Tools 36.0.0. Set `ANDROID_HOME`
to your Android SDK, or open this directory in Android Studio.

From this directory:

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

Windows PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.
The distributed build is debug-signed. Phone and physical scooter behavior
remain subject to device testing.

## Install and use

Download the APK from [AJ releases](https://github.com/Ajayraj2126/ola-s1-mods/releases/latest),
install it on your Android phone, and connect your scooter to the phone's hotspot.
Keep the scooter screen awake and allow the requested discovery permissions.
Use automatic discovery or enter its address manually. You can browse the app
offline before connecting.

The public source omits some official v1.4 binary features. See the
[source audit](../AJ-SOURCE-AUDIT.md), [edition guide](../AJ-EDITION.md),
[notices](../NOTICE.md) and [MIT license](../LICENSE).
