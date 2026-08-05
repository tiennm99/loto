# Google Play Auto-Publish Setup

How to configure GitHub secrets so pushing a `v*.*.*` tag builds a signed
AAB/APK, attaches both to a GitHub Release, and uploads the AAB to the Play
Console **Internal track** automatically. Driven by
[`.github/workflows/android-release.yml`](../.github/workflows/android-release.yml).

Verified working: tag `v0.0.2` (2026-08-05) built, released, and uploaded to
the internal track end-to-end.

## Prerequisites (one-time, manual)

These cannot be automated:

1. Google Play Console account ($25 one-time) at [play.google.com/console](https://play.google.com/console/signup)
2. App entry created with package name `com.miti99.loto`
3. **First AAB uploaded manually** to the Internal Testing track via the Play
   Console UI — Google requires the first upload to be manual
4. Store listing completed (icon, screenshots, descriptions, content rating,
   privacy policy URL, data safety form)

## Signing secrets (required for any release build)

| Secret | Description |
|--------|-------------|
| `KEYSTORE_BASE64` | Base64 of the PKCS12 keystore file |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias (list aliases: `keytool -list -keystore your.p12`) |
| `KEY_PASSWORD` | Key password |

Encode the keystore:

```bash
# bash / Git Bash
base64 -w0 miti99-apps.p12 | gh secret set KEYSTORE_BASE64 -R tiennm99/loto
```

```powershell
# PowerShell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\path\to\miti99-apps.p12")) |
  gh secret set KEYSTORE_BASE64 -R tiennm99/loto
```

Set the remaining three with `gh secret set <NAME> -R tiennm99/loto` (prompts
for the value interactively, keeping it out of shell history).

## Play Store auto-publish: the 5 steps

Do this **after** the first manual AAB upload has been accepted.

How the pieces fit: the service account lives in a Google Cloud project, but
its **authority to publish comes from Play Console, not from Cloud IAM**. The
GCP side only creates an identity and a key; the Play Console side grants the
actual publishing rights. That is why the service account needs **zero IAM
roles** in the Cloud project.

### Step 1 — Create a GCP project and enable the API

1. Open [console.cloud.google.com](https://console.cloud.google.com) and sign
   in with any Google account (it does **not** have to be the Play Console
   owner account).
2. Project picker (top bar) → **New Project** → name it e.g.
   `loto-play-publishing` → **Create**. Reusing an existing project is fine.
3. **APIs & Services → Library** → search **Google Play Android Developer
   API** → **Enable**.

Or with the `gcloud` CLI:

```bash
gcloud projects create loto-play-publishing
gcloud config set project loto-play-publishing
gcloud services enable androidpublisher.googleapis.com
```

### Step 2 — Create the service account (no IAM roles)

1. **IAM & Admin → Service Accounts → Create service account**.
2. Name: e.g. `play-publisher` (email becomes
   `play-publisher@loto-play-publishing.iam.gserviceaccount.com`).
3. **"Grant this service account access to project" — skip it.** Leave the
   role empty; publishing rights come from Play Console in Step 4. Granting
   Editor/Owner here is a common mistake that only widens the blast radius if
   the key leaks.
4. **"Grant users access to this service account" — skip it** too. **Done**.

```bash
gcloud iam service-accounts create play-publisher \
  --display-name "Play Store publisher (CI)"
```

### Step 3 — Download a JSON key

1. On the service account row → **⋮ → Manage keys** (or the **Keys** tab) →
   **Add Key → Create new key → JSON → Create**. The file downloads once and
   cannot be re-downloaded — treat it like a password.
2. Keep it **outside the repo** (e.g. `~/secrets/`). It goes into a GitHub
   secret in Step 5 and can be deleted locally afterwards.

```bash
gcloud iam service-accounts keys create service-account.json \
  --iam-account play-publisher@loto-play-publishing.iam.gserviceaccount.com
```

### Step 4 — Add the service account to Play Console

Done in Play Console by the **account owner** (or an admin who can manage
users):

1. Copy the service-account **email** from Step 2.
2. [play.google.com/console](https://play.google.com/console) → **Users and
   permissions** → **Invite new users**.
3. Paste the service-account email. No invitation email is sent for service
   accounts — access activates as soon as you save.
4. Under **App permissions** tab → **Add app** → select **Lo To
   (`com.miti99.loto`)** — scope access to this one app instead of
   account-wide permissions.
5. Tick exactly these two permissions:
   - **View app information and download bulk reports (read-only)** — the
     API needs it to read the app's edit state
   - **Release apps to testing tracks** — enough for the workflow's
     `tracks: internal` upload
   Leave everything else (production releases, store presence, financial
   data, user management) unchecked. If you later automate production rollout
   (`tracks: production`), come back and add **Release to production, exclude
   devices, and use Play App Signing**.
6. **Invite user → Send invite**.

Propagation is usually instant, but the very first API call can take up to
~24 h after the account's first-ever manual upload; if the workflow fails
with a 401/403 right after setup, wait and re-run before changing anything.

### Step 5 — Store the JSON as a GitHub secret

Name it `PLAY_SERVICE_ACCOUNT_JSON`:

```bash
# bash / Git Bash
gh secret set PLAY_SERVICE_ACCOUNT_JSON -R tiennm99/loto < path/to/service-account.json
```

```powershell
# PowerShell — '<' redirection is NOT supported, pipe instead:
Get-Content -Raw C:\path\to\service-account.json |
  gh secret set PLAY_SERVICE_ACCOUNT_JSON -R tiennm99/loto
```

The workflow is gated on this secret: if it is missing, the Play upload step
skips silently and the run still produces a GitHub Release. This means tags
work before Play setup is finished.

## Cutting a release

1. Bump **both** values in `android/android/app/build.gradle` — Play rejects
   duplicate `versionCode`s:

   ```groovy
   versionCode 3          // must increase every release
   versionName "0.0.3"    // should match the tag
   ```

2. Commit, push, tag:

   ```bash
   git add android/android/app/build.gradle
   git commit -m "chore(android): bump version to 0.0.3 (versionCode 3)"
   git push origin main
   git tag v0.0.3
   git push origin v0.0.3
   ```

3. Watch and verify:

   ```bash
   gh run list -R tiennm99/loto --workflow android-release.yml --limit 1
   gh run watch <run-id> -R tiennm99/loto --exit-status
   gh release view v0.0.3 -R tiennm99/loto
   ```

Promotion beyond the internal track (closed → open → production) stays manual
in the Play Console UI, or change `tracks: internal` in
`android-release.yml` to automate further.

## Troubleshooting

- **Play upload fails with duplicate versionCode** — `versionCode` in
  `android/android/app/build.gradle` was not bumped before tagging.
- **`The '<' operator is reserved for future use`** — PowerShell does not
  support `<` input redirection; use the `Get-Content -Raw ... |` form above.
- **Play upload step skipped** — `PLAY_SERVICE_ACCOUNT_JSON` secret is not
  set (check `gh secret list -R tiennm99/loto`), or was set after the run
  started.
- **Play upload fails with 401/403** — either the service account was not
  invited in Play Console (Step 4), the app was not added under its App
  permissions, or the Play Developer API access has not finished propagating
  (can take up to ~24 h after the first-ever manual upload). Verify the
  invite, wait, then re-run the job.
- **First upload rejected** — Google requires the very first AAB to be
  uploaded manually through the Play Console UI; the API can only publish
  after that.

**Never commit** `*.jks`, `*.keystore`, `*.p12`, service-account JSON, or `.env`.
