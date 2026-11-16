# S1 Control by AJ

Customized Android companion app by **AJ (@Ajayraj2126)**, based on
[Anand's S1Mods](https://github.com/Asjohn720/ola-s1-mods). Original MIT
copyright and attribution are retained. Application ID: `com.aj.s1control`.
Version: **0.2.0-aj** (code 7), based on the public source tagged v1.4.

## This edition

- Midnight-blue/teal Compose interface and a Home dashboard using live app state.
- Original Setup, Tools, Apps and Shell screens, discovery, ADB, diagnostics,
  bundled apps, custom APK installation and package management.
- Connection failures no longer show a false connected state; closed clients
  are discarded before reconnecting. Disconnect requires confirmation.
- Screen insets, scrolling and flexible dashboard cards support smaller screens
  and larger text.
- AJ update notices come from this fork and require an AJ-named APK asset.
  Official S1Mods APKs cannot update this app's separate application ID.
- Build uses supported Android SDK 36 and aligns the Compose compiler with
  AGP 9.0.1's built-in Kotlin 2.2.10. Android 8.0/API 26 is the minimum.

## Source and v1.4 compatibility

The upstream `v1.4` tag points to
`e6c82b2c640381f183a2011bd9e2f0e383f83ef5`, but the tagged Android source
still declares version 1.2 and omits several released binary features.
The official v1.4 APK was downloaded directly and its GitHub SHA-256 verified;
it includes Smart Ball and CAN assets absent from this source.

This is **not a feature-complete rebuild of the official v1.4 APK**. Dynamic
Island, My S1 CAN/SOH/cycles, vehicle lock/boot controls, full Smart Ball,
complete factory restoration and resume reconnect are not implemented here.
See [the source audit](AJ-SOURCE-AUDIT.md) for the verified comparison.

## Build

Install JDK 17 or 21 and Android SDK Platform 36 / Build Tools 36.0.0. Set
`ANDROID_HOME` to the SDK or let Android Studio create `android/local.properties`.
Open `android/` in Android Studio, or use the included Gradle wrapper:

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
This is a development build signed with a debug key, not a Play Store release.
For future AJ releases, name the APK `S1-Control-AJ-Edition-<version>.apk` and
use a numeric tag such as `v0.3.0-aj` so the in-app checker can identify it.

## Use

Install the APK on your Android phone. Connect your scooter to the phone's
hotspot, keep its screen awake, then use automatic discovery or a manual
address. You may also browse the app without connecting. A live diagnostic
handshake is required before the app reports a connected scooter.

Use only with a scooter you own or are authorized to administer. The inherited
mod-installation flow can uninstall an existing mod first and remove its app
data. Restarting does not undo installed packages and all settings. Use the
app while parked. Physical phone/scooter operation still requires device testing.

## Build validation

Final results and artifact digest are recorded in `BUILD-VALIDATION.md`.
