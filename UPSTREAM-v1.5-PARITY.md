# S1 Control by AJ — upstream S1Mods v1.5 parity

Last upstream check: 2026-11-22.

## Upstream release and actual source

- Official release: https://github.com/Asjohn720/ola-s1-mods/releases/tag/v1.5
- Official release published: 2026-11-22 (UTC).
- Tag `v1.5` and upstream `main` both point to
  `3f30d706527dcbd37114b5c7346588c32c233742`.
- Compared to the public `v1.4` tag, only `README.md` changed (69 lines).
- The `android/app/build.gradle.kts` file in upstream `v1.5` is still
  `versionName = "1.2"` and `versionCode = 5`.
- The upstream release advertises a substantially newer companion APK and mod APKs.
  Those working implementations are **not contained in its tagged Kotlin source**.
- The official release APK and mod binaries have **not** been imported, modified,
  repackaged or represented as AJ-produced binaries.

## Current AJ 0.3 base (main)

**Already operational in source:** AJ Home, discovery and verified connection,
hotspot-aware reconnect, selected mod setup, installed-app handling, original
diagnostic shell, system properties, screenshot, head-unit RAM, and conservative
AJ change restoration.

## Changes staged in branch feature/aj-v1.5-parity

- **Read-only head-unit diagnostics**: Android model/build, uptime, system load
  and RAM usage, clearly separated from vehicle telemetry.
- **Launcher shortcuts** for Setup, Tools and Apps.
- **Official v1.5 mod importer** in AJ App Manager: on-demand download from
  the upstream release, exact SHA-256 verification, bounded ZIP extraction,
  APK package-identity checks, and per-app install confirmation.
  No mod APKs are checked into this fork. New SmartBall has a different package
  name from legacy Balls, so the older app is never silently removed.
- Unit tests for diagnostic output parsing.
- Preserves AJ identity `com.aj.s1control`, existing signing configuration,
  reconnect state handling, mod installers and current source attributions.
- No changes to motor, BMS, safety control, flash or CAN transmit.
- No automatic lock/unlock/boot actions.

## v1.5 parity checklist

| Official v1.5 feature | AJ implementation status | Missing dependency / verification |
| --- | --- | --- |
| Home screen widget with Lock / Unlock / Boot | Not implemented | Owner-authorized/paired command workflow and safe authentication |
| Quick launch shortcuts | Implemented on this development branch | Build and Android launcher test |
| Donate card, UPI and official Telegram contact | Not implemented | Upstream official contact details and opt-in UX |
| ADB stability | Existing AJ 0.3 reconnect implementation | Real device regression testing |
| Read-only head-unit diagnostics | Implemented on this development branch | Emulator/build/device validation |
| Read-only CAN battery SoC/SoH/cell values | Not implemented | Verified CAN signal profile for each generation/MoveOS |
| BMS/MCU/VCU/charger/sensor faults | Not implemented | Verified CAN frame decoding and read-only diagnostic access |
| Range, rails and charging watts | Not implemented | Confirmed signal source/scaling and test vectors |
| SmartBall v1.5 / Dynamic Island | Official packaged SmartBall import available; AJ recreation not implemented | Check overlay permissions on device, clean-room source still unavailable |
| SmartBall RAM, brightness, audio, True Range | Not implemented | Compatible standalone mod and generation-specific validation |
| Android Auto 60 FPS, exit fix | Official v1.5 Android Auto APK import available | Head-unit hardware tests and source reimplementation still required |
| Music Mod dual-mode video/music | Official v1.5 Music Mod APK import available | Head-unit functionality tests and independent source still required |
| OpenPlay CarPlay Gen 3 fixes | Official v1.5 OpenPlay APK import available | Hardware/phone testing and independent source still required |

**Do not call this fork "v1.5 feature complete" until real app and scooter
testing confirms those behaviors.** No claim that Gen 1 CAN decoder works on
Gen 2/Gen 3 is justified; do not silently use unverified signal scaling.

## Useful independent reference projects

- Gen 1 read-only CAN investigation: https://github.com/abhijithvijayan/olamate
  (MIT; explicitly Gen 1, not validated on Gen 2/Gen 3).
- CAN message catalog: https://github.com/abhi245y/ola-can-reference
  (MIT; Gen 1 HVDP 5.0.0 profile only).
- Upstream v1.5 release: https://github.com/Asjohn720/ola-s1-mods/releases/tag/v1.5

## Build and validation

GitHub Action `Validate AJ 0.3` runs unit tests, lint and unsigned optimized
release assembly on pull requests touching `android/`. This branch is staged
separately; do not modify the existing signed release or main branch without
a successful CI and integration review.

## Verified official v1.5 nested APK identity (GitHub Actions)

Public release `S1Mods-v1.5.apk`, SHA-256:
`3f859bd2cf3b6d0a0e94303ab19e39ea8cae42bbb0988a9fb13a4593364deb9f`.

- SmartBall v1.5: `com.asjohn720.smartball` (new package vs legacy Balls).
- Android Auto bundled APK: `com.asjohn720.androidauto`.
- Music Mod v1.5: `com.asjohn720.musicmod`.
- OpenPlay bundled APK: `com.asjohn720.openplay`.

These package identifiers were read with Android SDK `aapt` from the downloaded
official public v1.5 APK in an Actions metadata-only audit.
The installer downloads to the user's phone and never copies mod APKs into
the AJ Git repository. Installing official packaged mods is **not** the same
as reproducing their source. Some mod permissions and platform compatibility
need real S1 Pro Gen 2 testing.
