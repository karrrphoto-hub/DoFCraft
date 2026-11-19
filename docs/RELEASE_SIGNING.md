# Release signing on macOS

Version 1.0.1 (versionCode 2) is the first public production-signed APK.
The application ID, minimum SDK and user-data schema are unchanged.

## Permanent signing identity

- Keystore: `~/Library/Application Support/DoF Master/signing/dof-master-release.p12`
- Alias: `dof-master-release`
- Password: a randomly generated secret in the login macOS Keychain.
  Generic-password service: `com.example.dofcalculator.release-signing`;
  account: `dof-master-release`.
- Certificate SHA-256:
  `cbfdebe0362a27c425046bd6eda81cd1520d7e63c20b5d4c714cbff9b8c5fa97`.
- RSA 4096, SHA256withRSA, certificate validity 10000 days.
- Signing directory permissions 0700; keystore permissions 0600.

Keep the keystore and its Keychain credential for every future update. Back up the
keystore to encrypted storage and the password using the owner's secure password
manager / Keychain workflow. Neither is uploaded to GitHub, including the private
repository. The signing certificate and its fingerprint are public, not secrets.
Losing either the key or its password prevents ordinary signed updates.

## Repeatable build

Use Android Studio Gradle tasks:

```text
:app:testDebugUnitTest :app:lintRelease :app:assembleRelease :app:assembleDebug
```

This project's optimized Release variant does not expose a
`testReleaseUnitTest` task. Run the complete JVM suite on Debug, then smoke-test
the actual signed Release APK separately on an emulator.

The Release variant explicitly disables Java and native debugging. R8 optimizes the
whole app; the experimental packageScope setting was removed after an actual
Release launch exposed an IllegalAccessError across Kotlin sequence classes.
The full-app configuration passed the emulator smoke test. Gradle produces
an unsigned APK; signing is a separate local step so Gradle files and logs do not
contain signing passwords:

```bash
/usr/bin/python3 scripts/sign_release_macos.py \
  app/build/outputs/apk/release/app-release-unsigned.apk \
  app/build/outputs/apk/release/DoF-Master-1.0.1.apk
```

Choose a new output filename for a new version; the helper refuses to overwrite
an existing APK. Increment versionCode and versionName before building an update.
It reuses the existing key and refuses mismatched/missing key-password pairs.

Only on the first setup, add `--initialize`. It creates a key only when both
the keystore and its Keychain item are absent; it never replaces an existing key.
It uses the Security framework for this specific Keychain item. Passwords are
passed to keytool/apksigner in a child-process environment, never literal command
arguments or plaintext files. A denied/locked Keychain is an error, not a reason
to fall back to plaintext storage.

Default tools: Android Studio bundled JBR and SDK build-tools 36.0.0.
`--java-home` and `--build-tools` override these paths. No new app dependency
or Android permission is required.

The final 1.0.1 APK is 1,886,160 bytes, SHA-256
`866ee34841a15839f6566df3e3af46220ab0b545c342a476a46f631054203b74`.

The helper checks the manifest for debuggable/test-only flags, 16 KB ZIP alignment,
APK v2/v3 signatures and the actual signing certificate fingerprint. Independently
audit the finished APK's manifest and contents before publishing.

## Release 1.0.2

VersionCode 3; signed with the same permanent production certificate as 1.0.1.
APK: `DoF-Master-1.0.2.apk` (1,894,352 bytes).
SHA-256: `1659576af86eaa836d78aab17a7766dd34f8dcf7a50924ddda15f76cbba75b81`.

Adds per-camera lens selection, shared profile bypass, lens distance-scale units,
dual hyperfocal readings and localized explanations. Preserves existing kit records.
Build validation: JVM tests, Release lint, optimized Release build and signed APK
launch on the emulator. Public distribution remains documentation plus APK/checksum.

## Release 1.0.3

DoFCraft branding; versionCode 4. Same application ID and permanent signing
certificate as previous production releases; no data migration or reset.
APK: `DoFCraft-1.0.3.apk` (1,894,352 bytes).
SHA-256: `9f92845d90c0a8e68226ba633f261aaa3b76f6ba030f527e677850d5e496fa42`.
Public distribution stays in the existing DoF-Master repository, with updated
DoFCraft presentation. Source and the Macro archive remain private.

## Release 1.0.4

VersionCode 5; same application ID and permanent production certificate.
APK: `DoFCraft-1.0.4.apk` (1,898,448 bytes).
SHA-256: `bae028ffd93097329da3999e965fbdb0edd318c0ef75cfa4bbf033390ca53dd1`.

Separates user and default camera profiles with user profiles first; sorts lens
lists by focal length; makes camera-picker lens descriptions compact with bullets,
three visible lenses and a remaining count. Distance-scale units appear there only
when they differ from the app's unit family, without current focus-distance rows.

Build validation: full JVM suite, Release lint, optimized Release and Debug builds,
and installation/launch of the actual signed Release APK on an isolated read-only emulator.
Public distribution contains documentation plus the signed APK and checksum.

## Upgrade from earlier private Debug builds

Those APKs use the Android Debug certificate. Android cannot install the new
Release over them as an ordinary in-place update. Do not uninstall a user's old
copy or clear its data to bypass this: saved profiles can be lost. No automatic
data migration or signature rotation has been implemented in this release.
From version 1.0.1 onward, reuse the permanent Release key for compatible updates.

## Repository and distribution

- [DoFCraft](https://github.com/karrrphoto-hub/DoFCraft) is the sole, private repository for source, app documentation and signed APK/checksum releases. Keep its visibility private unless the user explicitly authorizes a change.
- The former public DoF-Master distribution repository was consolidated on 2026-11-19. Its Git history is retained in `archive/dof-master-distribution`, original tags under `distribution-archive/`, and original release metadata in [distribution-archive/releases.json](distribution-archive/releases.json). Earlier references to public distribution above describe the original publication.
- Releases 1.0.1–1.0.4 are transferred without rebuilding or re-signing their APKs. Keep the historical asset filenames and SHA-256 checksums.
- The app page and screenshots are in [app/README.md](app/README.md), with a [Russian version](app/README.ru.md). Download links point to this repository and require repository access.
- Preserve the private `archive/macro-prototype` branch. Signing paths, alias, Keychain identifiers and permanent certificate are unchanged.
- Upload only signed production APKs and checksums as new release assets. Never upload an unsigned APK, keystore, password, credentials or local configuration.
- Default release notes are English; retain Russian app documentation.
