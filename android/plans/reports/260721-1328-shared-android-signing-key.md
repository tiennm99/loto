# Shared Android Signing Key Research

---
date: 2026-07-21
status: completed
scope: Android signing and GitHub Actions secret storage
---

## Summary

Using one `miti99-apps.p12` PKCS12 keystore and one `games` alias across multiple games is technically valid. The same certificate creates a shared security identity across those apps, so compromise affects every game using it.

The keystore must not be committed. A small PKCS12 file can be Base64-encoded into the encrypted GitHub Actions secret `KEYSTORE_BASE64`; passwords and alias belong in separate encrypted secrets. The release workflow decodes the PKCS12 file only on the runner and signs both APK and AAB outputs.

## Findings

- Android requires release APKs and upload AABs to be signed.
- Android supports multiple apps signed by the same certificate, including signature-level permissions between them.
- Play App Signing separates the locally held upload key from the app-signing key Google uses for distribution.
- GitHub supports Base64-encoded small binary blobs in Actions secrets.
- GitHub Actions secrets have a 48 KB limit; Base64 is encoding, not encryption.
- The keystore and passwords require an independent, durable backup. Losing or compromising a self-managed signing key can prevent safe future updates.

## Recommendation

1. Generate `C:\Users\miti99\.android\miti99-apps.p12` as a PKCS12 keystore.
2. Create alias `games` with RSA 4096 and long certificate validity.
3. Use strong generated store and key passwords.
4. Save the PKCS12 file and credentials in an encrypted password manager or offline encrypted backup.
5. Configure `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD` as GitHub Actions secrets.
6. Never commit the PKCS12 file, Base64 material, or passwords.

## References

- [Android: Sign your app](https://developer.android.com/studio/publish/app-signing)
- [GitHub: Using secrets in GitHub Actions](https://docs.github.com/en/actions/security-for-github-actions/security-guides/using-secrets-in-github-actions)

## Unresolved Questions

- Where the recoverable offline copy of the keystore and credentials will be stored.
- Whether this shared key will remain only an upload key under Play App Signing or also be supplied as the shared app-signing key.
