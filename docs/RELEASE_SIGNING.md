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

## Upgrade from earlier private Debug builds

Those APKs use the Android Debug certificate. Android cannot install the new
Release over them as an ordinary in-place update. Do not uninstall a user's old
copy or clear its data to bypass this: saved profiles can be lost. No automatic
data migration or signature rotation has been implemented in this release.
From version 1.0.1 onward, reuse the permanent Release key for compatible updates.

## Distribution boundary

- Source and experimental Macro archive remain in private DOF-Calculator.
- Public DoF-Master contains only app documentation/images and release assets.
- Publish the signed APK and its SHA-256 checksum, never an unsigned APK, keystore,
  password, local configuration, source history or Macro archive.
- Default public README and release notes are English; provide a Russian README.
