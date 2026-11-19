# Community Maps Android beta verification — 2026-11-19

## Release scope

Public GitHub prerelease only, tag `beta-community-maps-20261119-r3`, from
`fix/community-maps-android-e2e`. No merge into master, backend deployment,
production migration, Play Store publication or website/R2 replacement is included.
The owner explicitly authorized this beta before authenticated two-account E2E.
This is not approval of those outstanding tests or a stable public release.

Package `com.example.gridfall`, versionName `1.8.0-beta.1`, versionCode **22**.
The downloaded website APK was `1.7.1` / **21**. Both use certificate SHA-256:

`cb43563295f8134b162e71dfe43db34f325d4f22336129aa5dc825c6ad221d24`

The production key was reused, not regenerated. Release BuildConfig is non-debug,
contains `https://api.gridfall.site`, and includes the actual CommunityMapsClient
and navigation code; fake gateways and authenticated QA harnesses are test sources.
The ignored Firebase Android client configuration selects project `gridfall-auth`
and application package `com.example.gridfall`. This is configuration verification,
not positive Firebase login or proof of authenticated API authorization.

## Verification performed

- Fresh `:app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
  :app:assembleRelease --rerun-tasks --max-workers=2 --no-daemon`: successful.
  Debug override explicitly selected the production HTTPS API for read-only tests.
- Unit/local HTTP tests: **286 passed**, zero failures/errors/skips.
- Local signed release APK: apksigner verification passed; package/version,
  production URL and signing certificate checked against the downloaded public APK.
- Actual Android package manager update test on a fresh, disposable Android 11
  emulator: install public 1.7.1, create synthetic local marker, install beta with
  `adb install -r`, verify versionCode22 and marker preservation. Passed without
  uninstalling, launching the app, creating accounts or making production writes.
- Beta workflow YAML, tag restriction and all inline shell syntax checked locally.
  CI also verifies the expected signing certificate, package/version and Firebase
  client project/package before creating the prerelease.

### Emulator regressions

Pixel 7 profile, Android11/API30, normal1080x2400/density420. Disk space prevented
restarting the original AVD, so a temporary RAM-backed copy was used; no original
emulator/project data was removed. Three pre-existing gameplay UI fixtures were
made explicitly non-authenticating to avoid automatic production guest creation.

The first full run reported60 passes,3 opt-in skips,1 failure. The failing public
browser test assumed production had no maps. A read-only GET confirmed1 existing
public map. The test was corrected to allow populated browsing and use a unique
search term for empty-state verification. No other user's map was changed.

Final rerun result is recorded below after completion. Coverage includes actual
GameScreen Import & Play navigation with synthetic gateways, duplicate taps,
failed import/retry, code-entry, reset confirmation/cancellation, saved maps,
publication journals, account-free login dismissal, responsive gameplay, contract
spacing/interaction guards, center detection, drag/inventory behavior and themes.
Only the opt-in read-only class contacts production: browsing/search/sorting/error
contracts. Mock/local UI assertions are not authenticated production evidence.

## Publication controls

`publish-android-beta.yml` runs only on `beta-community-maps-*` tags and requests
job-scoped GitHub release permission. It reuses existing Android signing/Firebase
secrets and never references Cloudflare secrets or an R2 upload command. The
original `publish-android-apk.yml` is unchanged and remains restricted to `v*`
tags or explicit manual dispatch. Beta pushes must disable automatic follow-tags.

The workflow uploads `Gridfall-Community-Maps-Beta.apk` and its `.sha256` file as
actual prerelease assets. Hosted bytes must be downloaded and checked after CI;
local and hosted APK hashes may differ due to independent build metadata. Final
publication evidence and the hosted checksum belong in the delivery report.

## Remaining checks

- Two real, distinct Firebase accounts: publish, mine, ownership/self-rating,
  import-count idempotency, ratings/reporting, retries and supported cleanup.
- Signed expired/wrong-project/non-admin token cases with authorized test identities.
- Production-backed Import & Play with those controlled test publications.
- Physical Pixel9Pro/GrapheneOS install/update, auth, offline recovery, navigation
  and layout behavior, including the actual GitHub-hosted release build.
- No actual production backend restart was performed or authorized.
- Existing client-submitted score-forgery risks remain unchanged.

No authenticated production QA maps/reports/ratings were created in this beta
preparation. Existing production content is not test data to clean up.

### Final emulator rerun

**61 passed,0 failures/errors,3 skipped** out of64 discovered tests. The runner's
summary says “OK (64 tests)”; individual instrumentation statuses distinguish the
61 passes from3 assumption skips. Skipped: authenticated production phase and the
two opt-in isolated local API/PostgreSQL journeys. Those are not counted as passes.
Both real production read-only tests passed, including populated browse and empty
search. The prior failing assumption is fixed and no failure remains in the rerun.

### First CI attempt

Run44499736432 successfully built and tested the signed APK, but exited during
pre-publication verification. No release asset was published. The verification
now prints only the public certificate fingerprint and gives an explicit mismatch
error, retaining the strict production-certificate requirement. A fresh `-r2`
beta tag is used rather than moving or force-pushing the original tag.

The diagnostic retry also built successfully but could not parse its signer text
report; it did not establish a certificate mismatch. Verification now pins stable
Android build-tools36.0.0 and hashes the DER certificate bytes extracted from the
verified PEM output, removing preview-tool/text-label dependence. The complete
verification shell block is executed locally before the new `-r3` tag.
