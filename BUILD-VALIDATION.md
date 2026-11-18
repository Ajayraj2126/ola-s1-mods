# AJ Edition 0.3.0 release validation

Validated locally on 18 November 2026 from the `aj-0.3-foundation` worktree.

- Application ID `com.aj.s1control`, version `0.3.0-aj`, code 9.
- Launcher namespace `com.ajayraj2126.s1control`.
- 17 unit tests passed with zero failures or errors.
- Debug APK compilation, packaging and signature validation passed.
- Optimized release compilation and R8/resource shrinking passed.
- Release signing validation and APK Signature Scheme v2 verification passed.
- Release APK identity was verified with Android Build Tools 36.0.0.
- Source diff whitespace validation passed.
- No phone/emulator or physical scooter runtime test was available.

Local Android lint could not download its JVM-stub-only dependencies from Google
Maven inside the execution sandbox. The clean GitHub runner subsequently passed
unit tests, Android lint and the optimized release build in
[validation run 44253069586](https://github.com/Ajayraj2126/ola-s1-mods/actions/runs/44253069586).

## Local validation artifacts

```text
Debug APK
path: android/app/build/outputs/apk/debug/app-debug.apk
size: approximately 46 MiB
sha256: 0a48f5dacb58ebfab787e8e2727493e4505dabc231e7f4af161936eda2ecf39a

Signed optimized APK
path: android/app/build/outputs/apk/release/app-release.apk
size: approximately 29 MiB
```

The release uses the protected, reusable AJ release key supplied through
`AJ_RELEASE_STORE_FILE`, `AJ_RELEASE_STORE_PASSWORD`, `AJ_RELEASE_KEY_ALIAS`, and
`AJ_RELEASE_KEY_PASSWORD`. The two passwords can instead be read from protected
files using `AJ_RELEASE_STORE_PASSWORD_FILE` and `AJ_RELEASE_KEY_PASSWORD_FILE` so
they never appear in shell history. The final signed APK certificate and checksum
must be recorded in the release checksum manifest after signing.

```text
Certificate owner: CN=Ajay Raj, OU=AJ S1 Control, O=AJ (Ajayraj2126), C=IN
Certificate validity: 18 November 2026 to 5 April 2054
Certificate SHA-256: 77ed171c551be82ca8b74e0ad664a60caa081e943fbd8588c2b9201217f2f4ee
Public-key SHA-256: a1202382d379391dff50174d41acf2dbea85cc150b7d905089b69fdf3c75d063
Key algorithm: RSA 4096
APK signature: v2
```

Older 0.2.x downloads used disposable debug certificates. The first properly
release-signed 0.3 installation can therefore require a one-time uninstall; every
subsequent AJ release must reuse the same release key.
