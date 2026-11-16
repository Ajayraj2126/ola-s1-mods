# AJ Edition 0.2.1 branding validation

Local working tree based on `fd498005a6c02c4ee8f9fb48a31a31c72bdfa991`.

- APK build and both unit tests passed (0 failures/errors).
- Package `com.aj.s1control`, version `0.2.1-aj`, code 8.
- Launcher `com.ajayraj2126.s1control.MainActivity` verified.
- App-owned old namespace absent from top-level DEX.
- Primary UI sources contain no original-owner handle; repository/release links use Ajayraj2126.
- Original full license is bundled unchanged and available from the legal dialog.
- Renamed Android Auto asset and all bundled package identities verified.
- APK v2 signature verified.

APK SHA-256: `240343213732fa8d5f04662aad7d5469a3fb232fc0cc87ce4c0738672df5bd88`

The local and GitHub builds use different debug certificates. The CI validation file describes the downloadable APK. An older debug installation may need uninstalling before installing a build signed with a different key; this removes local AJ app settings. No phone/emulator or physical scooter runtime test was performed. Public-source v1.4 feature gaps remain documented in AJ-SOURCE-AUDIT.md.

```text
Verifies
Verified using v1 scheme (JAR signing): false
Verified using v2 scheme (APK Signature Scheme v2): true
Verified using v3 scheme (APK Signature Scheme v3): false
Verified using v3.1 scheme (APK Signature Scheme v3.1): false
Verified using v4 scheme (APK Signature Scheme v4): false
Verified for SourceStamp: false
Number of signers: 1
Signer #1 certificate DN: C=US, O=Android, CN=Android Debug
Signer #1 certificate SHA-256 digest: 3f62f5a10fc152be0ba45a685afb7d2f100e5de2fa05c5afa24de6519e0cde82
Signer #1 certificate SHA-1 digest: a5076e2eee9c49f9721179d77b02fd6414081a81
Signer #1 certificate MD5 digest: 050bf425ac6e19f2767c4e53261a9397
Signer #1 key algorithm: RSA
Signer #1 key size (bits): 2048
Signer #1 public key SHA-256 digest: 79e9a2aaa29688c290d2dbff79c5a4f2bbe399087d3095f6199969539a118216
Signer #1 public key SHA-1 digest: d0f4400157840a14e32cbfdef6b43c73c1eddc22
Signer #1 public key MD5 digest: 946b8a587dc0d9c2dff0b51fc8a68c63
```
