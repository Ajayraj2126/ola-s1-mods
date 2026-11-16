# S1 Control by AJ — source compatibility review

Reviewed 16 November 2026. This document distinguishes the public source from the official binary; it does not certify scooter operation.

## Verified provenance

- Upstream `main` and the official `v1.4` tag both point to `e6c82b2c640381f183a2011bd9e2f0e383f83ef5` at review time.
- The existing AJ branch started at `53de59a33b466ed04f1bee01e110e91e9118895a`: [AJ Edition branch](https://github.com/Ajayraj2126/ola-s1-mods/tree/aj-edition-ui).
- The upstream tagged Android source declares version `1.2`, code `5`. The starting AJ branch declared `0.1.0-aj`, code `6`, with application ID `com.aj.s1control`.
- The official [v1.4 release](https://github.com/Asjohn720/ola-s1-mods/releases/tag/v1.4) was published on 15 November 2026 at 06:49:24 UTC. Its downloadable APK is a different artifact from a build of the published Android source.
- The official APK was downloaded directly without a user upload. SHA-256 `33afdb54ee6043516eed815fc21bb75466464af82bf11d465476ea9ccda38334` matches the GitHub release asset digest. Its Android manifest identifies `com.asjohn720.s1mods`, version `1.4` / code `8` (minimum API 26, target 35), verified with SDK `aapt2 dump badging`. Its ZIP contains `assets/can_database.json` and `assets/smartball-latest.apk`; neither is in the public Android source assets. This archive inspection did not execute or repackage the official APK.

## Functional scope

The existing eight-file AJ customization changes the dashboard, theme, branding, application ID and explanatory copy. Its ADB, gRPC, discovery, model and bundled APK assets are byte-identical to the upstream source snapshot before the connection fixes described below.

The source implements network discovery and manual addresses; gRPC diagnostic sessions; ADB authorization; bundled navigation, Android Auto and music installation; online/custom APK installation and package management; display screenshots; model/software/RAM reads; settings shortcuts; and a gated diagnostic shell. These paths remain the functional foundation of AJ Edition.

The retained Kotlin namespace `com.asjohn720.s1mods` is compatible with the separate application ID `com.aj.s1control`. It preserves generated resource/BuildConfig references and activity class names while allowing side-by-side installation with upstream S1Mods.

| Official v1.4 announcement | Published source at `e6c82b2` |
| --- | --- |
| Dynamic Island and My Ola vehicle dashboard, introduced in [1.3](https://github.com/Asjohn720/ola-s1-mods/releases/tag/1.3) | No floating speedometer, vehicle-control dashboard or dedicated live ride telemetry source |
| Smart Ball, boot unlock and fast scooter lock | Bundles legacy `balls-1.1.0-lite-release.apk`; no companion Smart Ball settings implementation |
| My S1: CAN connection, SOH, cycle count, fault clearing | No CAN database or dedicated telemetry implementation |
| One-tap factory restoration | No complete restoration action |
| Reconnect when app resumes | No `onResume` reconnect implementation |
| Gen 3 and repeat-install improvements | Public source does not establish parity with the released APK; hardware validation required |
| Manual RAM refresh | Present: `/proc/meminfo` read and refresh controls |

AJ Edition must therefore be described as a customization of the public source, not a feature-complete v1.4 rebuild. Replacing its legacy mod asset with Smart Ball alone would not reproduce the missing companion logic.

## Concrete correctness findings

Connection lifecycle fixes were made in `ScooterManager.kt`: discard the closed client reference on disconnect; clear prior connection state before a new handshake; close a failed candidate client; and keep a TCP reachability test from publishing a connected diagnostic session. The corresponding UI must report success only after `manager.connect` verifies gRPC; the original connection card caught a connection exception and set the state to connected anyway.

The new AJ build is `0.2.0-aj`, code `7`. Update checks now use the AJ fork and require an AJ-named APK asset; the dialog opens its release page. This fixes the earlier comparison of AJ version 0.1.0 with official S1Mods v1.4.

The inherited setup and individual bundled-app installation paths uninstall existing copies before installing replacements. Existing app data may consequently be removed. The ADB bind mount being temporary does not undo installed packages, user data changes, or all settings on restart. The original copyright/license remains applicable and preserved.

## Verification boundaries

Performed: fresh GitHub release/tag/main verification; SHA-pinned source comparison; official APK digest and asset inspection; call-path review of connection, installation, app management, diagnostics and shell; focused connection lifecycle edits and code review. No scooter commands were sent. No physical phone or scooter behavior was tested by this review. The owner of the build records the final compiler/APK validation separately.
