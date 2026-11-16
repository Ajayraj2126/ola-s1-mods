# S1 Control by AJ — UI customization

This is the **AJ Edition** of the upstream S1Mods Android application.

- Customized interface by **AJ (@Ajayraj2126)**.
- Original project by **Anand (@Asjohn720)**: https://github.com/Asjohn720/ola-s1-mods
- Original MIT LICENSE and upstream authorship are preserved.
- The app launcher displays **S1 Control by AJ**.
- Application ID: `com.aj.s1control`, allowing installation alongside original S1Mods.
- AJ Edition version: `0.1.0-aj`; not the official v1.4 release APK.

## What's changed

New read-only Home dashboard, deep-blue/teal theme, and AJ branding. Existing
Setup, Tools, Apps, Shell, discovery, ScooterManager, ADB, gRPC and installation
implementation remain from the public source.

## Important v1.4 source discrepancy

The **official S1Mods v1.4 APK** released on 2026-11-15 describes Smart Ball,
factory reset, Gen 3 refinements, CAN battery health, and other enhancements.
However, the **public v1.4 Git tag**, commit
`e6c82b2c640381f183a2011bd9e2f0e383f83ef5`, still contains
`versionName = "1.2"` and is missing those advertised features.
This AJ Edition branch customizes that publicly available source;
it **does not reproduce the full feature set of the v1.4 APK**.

## Build locally on Windows

Install JDK 17, Android Studio and the Android SDK. Open `android/` in Android
Studio and build the debug variant, or run:

```powershell
cd android
.\gradlew.bat :app:assembleDebug
```

The resulting debug APK normally appears under
`android/app/build/outputs/apk/debug/app-debug.apk`.

**Not compiled or scooter-tested.** Validate the app on an Android phone first,
and do not operate mod controls while riding. Keep copyright and bundled third
party app license notices when redistributing.
