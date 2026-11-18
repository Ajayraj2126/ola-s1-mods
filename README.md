# S1 Control by AJ · owner-approved connection monitor

This Android 8+ phone application checks the status of an Android infotainment
unit on a trusted, private hotspot. It requires a unit that already exposes
standard ADB over TCP with Android's RSA device-approval prompt. Confirm support
with the manufacturer for the specific scooter and firmware. Android's separate
wireless debugging pairing-code protocol uses a different transport.

1. Park the scooter and connect both devices to the same private network.
2. Enter the scooter's private IPv4 address on the phone and tap **Connect**.
3. Approve the connection on the scooter's Android screen. A successful check
   displays the model from the Android device banner and the last verification time.
4. Keep the phone app visible for periodic status checks. It retries the saved
   address with delays up to one minute when the device goes out of range. If the
   hotspot assigns a new address, enter it manually. **Disconnect and forget
   address** also disables retries across app restarts.

The phone creates its own per-installation credential in Android Keystore. It uses
the Android authorization handshake and rejects endpoints that skip the challenge.
Automatic checks use an already approved key. Only an explicit connection can
request new approval. Classic ADB authenticates the phone to the device and
does not encrypt traffic or authenticate the server to the phone. Use your own
private hotspot.

The app reads the device banner and closes the socket after each check. The build
excludes command execution, vehicle controls, app installation, rollback, and
legacy diagnostic transports. Existing installations and scooter settings are
untouched. The earlier implementation is retained in repository history and the
legacy source directory.

## Build and verify

From `android/`, use JDK 21 and SDK 36. Run
`./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease`.
With an Android emulator running, use `./gradlew :app:connectedDebugAndroidTest`
to exercise the launcher and non-exportable phone credential. Results from
simulated devices cover phone behavior. MoveOS and range-loss checks require
an owner-approved, parked scooter.

The release build uses the existing AJ signing configuration. A publishable APK
must verify against the recorded AJ certificate SHA-256
`77ed171c551be82ca8b74e0ad664a60caa081e943fbd8588c2b9201217f2f4ee`.
The private APK signing key belongs in the owner's secure signing environment.
