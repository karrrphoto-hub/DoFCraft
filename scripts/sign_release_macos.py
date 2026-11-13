#!/usr/bin/env python3
"""Sign an unsigned release APK using this app's local key and macOS Keychain."""
import argparse
import ctypes
import fcntl
import hashlib
import json
import os
from pathlib import Path
import secrets
import ssl
import subprocess
import sys
import tempfile

SERVICE = b"com.example.dofcalculator.release-signing"
ACCOUNT = b"dof-master-release"
ALIAS = "dof-master-release"
PASSWORD_ENV = "DOF_MASTER_SIGNING_PASSWORD"
KEY_DIRECTORY = Path.home() / "Library/Application Support/DoF Master/signing"
KEYSTORE = KEY_DIRECTORY / "dof-master-release.p12"


class Keychain:
    """Access only the single generic-password item owned by this signing workflow."""
    def __init__(self):
        self.api = ctypes.CDLL("/System/Library/Frameworks/Security.framework/Security")
        self.api.SecKeychainFindGenericPassword.argtypes = [
            ctypes.c_void_p, ctypes.c_uint32, ctypes.c_char_p,
            ctypes.c_uint32, ctypes.c_char_p,
            ctypes.POINTER(ctypes.c_uint32), ctypes.POINTER(ctypes.c_void_p),
            ctypes.c_void_p,
        ]
        self.api.SecKeychainFindGenericPassword.restype = ctypes.c_int32
        self.api.SecKeychainAddGenericPassword.argtypes = [
            ctypes.c_void_p, ctypes.c_uint32, ctypes.c_char_p,
            ctypes.c_uint32, ctypes.c_char_p,
            ctypes.c_uint32, ctypes.c_char_p, ctypes.c_void_p,
        ]
        self.api.SecKeychainAddGenericPassword.restype = ctypes.c_int32
        self.api.SecKeychainItemFreeContent.argtypes = [ctypes.c_void_p, ctypes.c_void_p]
        self.api.SecKeychainItemFreeContent.restype = ctypes.c_int32

    def read(self):
        length, data = ctypes.c_uint32(), ctypes.c_void_p()
        status = self.api.SecKeychainFindGenericPassword(
            None, len(SERVICE), SERVICE, len(ACCOUNT), ACCOUNT,
            ctypes.byref(length), ctypes.byref(data), None,
        )
        if status == -25300:
            return None
        if status != 0:
            raise RuntimeError(f"Cannot read signing Keychain item (OSStatus {status}).")
        try:
            return ctypes.string_at(data, length.value).decode("ascii")
        finally:
            self.api.SecKeychainItemFreeContent(None, data)

    def add(self, password):
        encoded = password.encode("ascii")
        status = self.api.SecKeychainAddGenericPassword(
            None, len(SERVICE), SERVICE, len(ACCOUNT), ACCOUNT,
            len(encoded), encoded, None,
        )
        if status != 0:
            raise RuntimeError(f"Cannot save signing Keychain item (OSStatus {status}).")


def run(command, env=None):
    result = subprocess.run(command, env=env, text=True, capture_output=True, check=False)
    if result.returncode:
        # Child commands receive only an environment-variable name, never a literal password.
        raise RuntimeError(result.stderr.strip() or result.stdout.strip() or "Signing tool failed.")
    return result.stdout


def certificate_fingerprint(keytool, key, env):
    pem = run([
        str(keytool), "-exportcert", "-rfc", "-keystore", str(key),
        "-alias", ALIAS, "-storepass:env", PASSWORD_ENV,
    ], env)
    return hashlib.sha256(ssl.PEM_cert_to_DER_cert(pem)).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("unsigned_apk", type=Path)
    parser.add_argument("signed_apk", type=Path)
    parser.add_argument("--initialize", action="store_true",
                        help="Create a permanent signing key only if both key and credential are absent.")
    parser.add_argument("--java-home", type=Path,
                        default=Path("/Applications/Android Studio.app/Contents/jbr/Contents/Home"))
    parser.add_argument("--build-tools", type=Path,
                        default=Path.home() / "Library/Android/sdk/build-tools/36.0.0")
    args = parser.parse_args()
    if sys.platform != "darwin":
        raise RuntimeError("This signing helper requires macOS Keychain.")
    if not args.unsigned_apk.is_file():
        raise RuntimeError("Unsigned APK does not exist.")
    if args.signed_apk.exists():
        raise RuntimeError("Output already exists; use a new output filename.")
    java = args.java_home / "bin/java"
    keytool = args.java_home / "bin/keytool"
    signer = args.build_tools / "lib/apksigner.jar"
    align = args.build_tools / "zipalign"
    for tool in (java, keytool, signer, align):
        if not tool.is_file():
            raise RuntimeError(f"Required tool not found: {tool}")
    manifest = run([str(args.build_tools / "aapt"), "dump", "xmltree",
                    str(args.unsigned_apk), "AndroidManifest.xml"])
    for line in manifest.splitlines():
        if ("android:debuggable" in line or "android:testOnly" in line) and "0xffffffff" in line:
            raise RuntimeError("Refusing to sign a debuggable or test-only APK.")
    run([str(align), "-c", "-P", "16", "4", str(args.unsigned_apk)])

    os.umask(0o077)
    if KEY_DIRECTORY.is_symlink():
        raise RuntimeError("Signing directory must not be a symlink.")
    KEY_DIRECTORY.mkdir(parents=True, exist_ok=True, mode=0o700)
    KEY_DIRECTORY.chmod(0o700)
    with (KEY_DIRECTORY / ".signing.lock").open("a") as lock:
        fcntl.flock(lock, fcntl.LOCK_EX)
        if KEYSTORE.is_symlink():
            raise RuntimeError("Signing keystore must not be a symlink.")
        keychain = Keychain()
        password = keychain.read()
        if KEYSTORE.exists() != (password is not None):
            raise RuntimeError("Signing key/Keychain mismatch. Restore the original pair; do not regenerate.")
        initialized = False
        if password is None:
            if not args.initialize:
                raise RuntimeError("No signing key. First use requires explicit --initialize.")
            password = secrets.token_urlsafe(48)
            env = dict(os.environ, **{PASSWORD_ENV: password})
            with tempfile.TemporaryDirectory(prefix=".new-key-", dir=KEY_DIRECTORY) as staging:
                temporary_key = Path(staging) / "release.p12"
                run([
                    str(keytool), "-genkeypair", "-noprompt", "-storetype", "PKCS12",
                    "-keystore", str(temporary_key), "-alias", ALIAS,
                    "-keyalg", "RSA", "-keysize", "4096", "-sigalg", "SHA256withRSA",
                    "-validity", "10000", "-dname", "CN=DoF Master,O=DoF Master",
                    "-storepass:env", PASSWORD_ENV, "-keypass:env", PASSWORD_ENV,
                ], env)
                certificate_fingerprint(keytool, temporary_key, env)
                # Keep a recoverable key before adding its password. If Keychain denies access,
                # remove only this unpublished new key; existing keys are never replaced.
                os.link(temporary_key, KEYSTORE)
                try:
                    keychain.add(password)
                except Exception:
                    KEYSTORE.unlink()
                    raise
            initialized = True
        KEYSTORE.chmod(0o600)
        env = dict(os.environ, **{PASSWORD_ENV: password})
        fingerprint = certificate_fingerprint(keytool, KEYSTORE, env)
        args.signed_apk.parent.mkdir(parents=True, exist_ok=True)
        run([
            str(java), "--enable-native-access=ALL-UNNAMED", "-jar", str(signer), "sign",
            "--ks", str(KEYSTORE), "--ks-key-alias", ALIAS,
            "--ks-pass", "env:" + PASSWORD_ENV, "--key-pass", "env:" + PASSWORD_ENV,
            "--v1-signing-enabled", "false", "--v2-signing-enabled", "true",
            "--v3-signing-enabled", "true", "--v4-signing-enabled", "false",
            "--out", str(args.signed_apk), str(args.unsigned_apk),
        ], env)
        verification = run([
            str(java), "--enable-native-access=ALL-UNNAMED", "-jar", str(signer),
            "verify", "--verbose", "--print-certs", str(args.signed_apk),
        ])
        if f"Signer #1 certificate SHA-256 digest: {fingerprint}" not in verification:
            raise RuntimeError("Signed APK certificate does not match the permanent release key.")
        run([str(align), "-c", "-P", "16", "4", str(args.signed_apk)])
        args.signed_apk.chmod(0o644)
        print(json.dumps({
            "apk": str(args.signed_apk),
            "size": args.signed_apk.stat().st_size,
            "sha256": hashlib.sha256(args.signed_apk.read_bytes()).hexdigest(),
            "certificate_sha256": fingerprint,
            "initialized_new_key": initialized,
            "keystore": str(KEYSTORE),
            "password_storage": "macOS Keychain (never printed or written to a plaintext file)",
        }, indent=2))


if __name__ == "__main__":
    try:
        main()
    except Exception as error:
        print(f"Release signing failed: {error}", file=sys.stderr)
        sys.exit(1)
