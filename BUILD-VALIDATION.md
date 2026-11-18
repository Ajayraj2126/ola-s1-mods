# AJ Edition 0.3.0 release-candidate validation

Validated locally on 18 November 2026 from the `aj-0.3-foundation` worktree.

- Application ID `com.aj.s1control`, version `0.3.0-aj`, code 9.
- Launcher namespace `com.ajayraj2126.s1control`.
- 17 unit tests passed with zero failures or errors.
- Debug APK compilation, packaging and signature validation passed.
- Optimized release compilation and R8/resource shrinking passed.
- Release APK identity was verified with Android Build Tools 36.0.0.
- Source diff whitespace validation passed.
- No phone/emulator or physical scooter runtime test was available.

Local Android lint could not download its JVM-stub-only dependencies from Google
Maven inside the execution sandbox. The clean GitHub runner subsequently passed
unit tests, Android lint and the optimized release build in
[validation run 44253069586](https://github.com/Ajayraj2126/ola-s1-mods/actions/runs/44253069586).

## Candidate artifacts

```text
Debug APK
path: android/app/build/outputs/apk/debug/app-debug.apk
size: approximately 46 MiB
sha256: 0a48f5dacb58ebfab787e8e2727493e4505dabc231e7f4af161936eda2ecf39a

Optimized unsigned APK
path: android/app/build/outputs/apk/release/app-release-unsigned.apk
size: approximately 29 MiB
sha256: 5a2fb8a5de6522bb680bb98f8f571bdb30ecaec8f3eb2f32933371d8a2e358c4
```

The unsigned APK is an R8 validation artifact only and must not be published.
Publication requires a protected, reusable AJ release key supplied through
`AJ_RELEASE_STORE_FILE`, `AJ_RELEASE_STORE_PASSWORD`, `AJ_RELEASE_KEY_ALIAS`, and
`AJ_RELEASE_KEY_PASSWORD`. The final signed APK certificate and checksum must be
recorded here after signing.

Older 0.2.x downloads used disposable debug certificates. The first properly
release-signed 0.3 installation can therefore require a one-time uninstall; every
subsequent AJ release must reuse the same release key.
