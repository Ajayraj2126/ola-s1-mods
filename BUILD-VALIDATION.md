# AJ Edition 0.2.0 — build validation

Validated 16 November 2026 from the AJ branch starting at
`53de59a33b466ed04f1bee01e110e91e9118895a`, with the changes in this submission.
Upstream source: `e6c82b2c640381f183a2011bd9e2f0e383f83ef5`.

## Results

- `:app:assembleDebug`: passed (fresh build completed in 6m 38s).
- `:app:testDebugUnitTest`: passed; 2 tests, 0 failures, 0 errors.
  Tests cover AJ release version comparisons and rejecting original/unrelated
  APK filenames as AJ updates. They do not perform network requests.
- Final incremental build/test check: passed after all source edits.
- `git diff --check`: passed.
- `aapt2 dump badging`: package `com.aj.s1control`, label `S1 Control by AJ`,
  version `0.2.0-aj` / code 7, minimum Android API 26, target API 35,
  compile API 36.
- `apksigner verify --verbose --print-certs`: passed; APK Signature Scheme v2,
  one Android Debug signer (RSA 2048).

## Toolchain

Eclipse Temurin JDK 21.0.13, Gradle 9.4.1, Android Gradle Plugin 9.0.1,
built-in Kotlin / Compose compiler 2.2.10, SDK Platform 36,
Android Build Tools 36.0.0. Dependencies and bundled mod APKs come from the
original public project. No official v1.4 binary code was repackaged.

## Artifact

File: `S1-Control-AJ-Edition-0.2.0-debug.apk`

Size: 47,782,385 bytes.

SHA-256:

```text
a2676ea48e81c23424966c5819867f96d914b7c5819a19b3a6f3a059a03aa636
```

Debug signing certificate SHA-256:

```text
3f62f5a10fc152be0ba45a685afb7d2f100e5de2fa05c5afa24de6519e0cde82
```

## Limits

This is a development/debug build. No Android phone/emulator UI session or
physical scooter was available for runtime testing. Successful compilation
and signature checks do not establish connectivity or firmware compatibility.
Connection changes were independently reviewed. Existing install and diagnostic
paths were preserved at source level; all physical device behavior still needs
verification. See `AJ-SOURCE-AUDIT.md` for official v1.4 features absent from the
published source.
