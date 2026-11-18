# Android Community Maps integration / QA — 2026-11-18

## Scope and inspected state

Original checkout: feat/community-maps @9583ed2. Backend packaging/security corrections
and unrelated IDE files were uncommitted there; preserved untouched. Android work
uses separate worktree Gridfall_android_e2e, branch fix/community-maps-android-e2e,
based on9583ed2. No merge, push, tag, APK upload, backend deployment, NAS/Compose/
credential change, production migration or database reset was performed.

Normal source ALREADY used CommunityMapsClient (OkHttp4.12), not Retrofit or a fake
repository. Fake gateways are test-only; previous offline preview was a separate
/tmp build/package. Release API URL already https://api.gridfall.site; default debug
used NAS LAN http://192.168.222.172:8080. Actual test APK now explicitly selects the
production URL through a validated debug Gradle property; LAN default and strict
staging isolation remain unchanged. Production Firebase Android public configuration
project metadata is gridfall-auth. No service-account or ID token was committed.

## Implementation

- Import & Play previously called requestCustomMapStart, which forced Settings open
  both on immediate restart and after progress-reset confirmation. New immutable
  CustomMapStartRequest retains the requested menu destination through confirmation.
  Community imports pass keepSettingsOpen=false, close community/leaderboard overlays
  and enter actual gameplay. Existing saved-map/settings selection behavior is kept.
  Failed import stays in the dialog; cancel preserves prior game; retry keeps intent
  ID; repeated taps do not issue duplicate import calls. Downloaded maps remain local.
- Browser guards concurrent page loads and ignores stale responses/finalizers;
  preserves search/sort/detail state through saveable-state restoration. Results
  remain deduplicated by map code. UI is keyed by Firebase account identity so owned
  lists/dialog state cannot carry into another account's presentation.
- Client explicitly requests10 maps/page, bounding legitimate map documents within
  its existing1MiB response limit; validates page/cursor/publication metadata and
  detects repeated cursors. Invalid JSON/data is a protocol error, never empty results.
- API403 JSON permission messages (including self-rating denial) remain meaningful.
  Non-JSON403 edge blocks no longer misleadingly tell the user to complete a profile.
  Offline, protocol, permission, unavailable-map and rate-limit errors are separate.
- Import/publish UUIDs survive dialog saved-state recreation. Existing persistent,
  account-scoped publication journal and server idempotency protocol are reused.
- Auth actions already force Firebase ID-token refresh; now reject identity changes
  during asynchronous refresh/session retrieval. No fabricated production identity.
- Explicit DEBUG-only communityReadOnlyQa launch disables automatic guest creation
  and background queued-run retries, and opens the real browser. Manual login remains
  available for authorized testers. Release and normal app behavior are unchanged.
  Optional gateway injection is a UI test seam; no mock implementation ships in main.

## Verification — distinct evidence categories

| Category | Actual result |
|---|---|
| Fresh debug unit/local HTTP tests |286 passed,0 failed/skipped; includes7 new client contract tests |
| Fresh debug app + instrumentation APK build |PASS, --rerun-tasks,76 tasks executed |
| Release Kotlin compilation |PASS; no signed/public release APK built |
| Release unit variant |Not available: project has no testReleaseUnitTest task; no invented result |
| Full Pixel7 Android11/API30 suite |62 passed,0 failed/skipped |
| Actual production GET instrumentation |2 of those62; real OkHttp/public UI, no token/mutation |
| Isolated API/PostgreSQL instrumentation |2 of62; synthetic identity verifier, real loopback HTTP/transactions |
| Other device regressions |58 of62 isolated UI/storage/gameplay tests; NOT authenticated production evidence |
| Small-phone viewport720x1280/density320 |23 selected tests passed,0 failed/skipped |
| Tablet viewport1600x2560/density240 |23 selected tests passed,0 failed/skipped |
| Host production GET matrix with actual app UA |18 assertions passed; no valid credentials or writes |
| Real emulator offline/recovery |Disable WiFi+data -> no active network -> explicit network error/Retry (not empty); restore radios -> Retry -> real empty response |
| Production authenticated writes/admin/account switching |BLOCKED: authorized test-account access not supplied |
| Physical device/foldable/latest Android |Not available; emulator hardware profile is Pixel7 but system is Android11 |

Known navigation bug independently reproduced: temporarily reintroduced old
keep-Settings-open behavior on the isolated worktree, then ran the new real
GameScreen regression; it FAILED with the expected missing-gameplay timeout.
An EXIT trap restored fixed source. Final tests pass immediate + confirmed starts,
cancel preservation, failed import/retry, code-entry path and duplicate-tap cases.
An initial new HTTP unit assertion failed before explicit limit10 was added; fixed
and freshly rerun all286. No known failing assertions remain in final test runs.

Existing #54 non-scrollable viewport, #58 center targeting, #59 contract/grid spacing,
#60 offer interaction guards, drag/tray/inventory and old map compatibility tests
were included. No mechanics, scoring, official leaderboard submission eligibility,
contracts, joker inventory, theme design or backend code were changed.

### Real production read-only checks

https://api.gridfall.site health/ready200; browse with omitted/empty query, Unicode
search, limit1, all4 sorts200; invalid sort/limit/cursor400; invalid map code400;
nonexistent valid code404; owned maps/admin reports without token401; malformed
bearer owned-map request401. Marketplace was empty with nextCursor:null. Actual
multi-page/live-rating data cannot be verified against an empty marketplace without
authorized test publications; pagination is covered by local HTTP/UI cases.

Default Python urllib UA received Cloudflare1010/HTTP403. Requests identifying as
actual application okhttp/4.12.0 and real Android OkHttp calls succeeded. No browser
impersonation, Cloudflare policy change or authentication bypass was used. The app
now reports a non-JSON edge denial as access failure rather than login failure.
No credentials or live private response bodies are included in this report.

Authenticated production test count:0. Missing/malformed-token rejection does NOT
establish signed expired/wrong-project/admin-token handling. Positive login, publish,
owned maps, real imports/counter idempotency, different-user ratings/self-rating,
reporting/unpublishing and live account isolation remain unverified. Fake tokens in
localhost tests are NOT live Firebase verification. Real production test records
created:0; no production cleanup was needed or attempted. Local fixture maps were
unpublished/deleted from test storage where applicable; disposable PG is removed
only after local verification. No real user content was touched.

## Reproduce safely

From this Android worktree (ignored existing production Android client config required):

```sh
bash ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest \
  -PgridfallDebugApiBaseUrl=https://api.gridfall.site --rerun-tasks \
  --max-workers=2 --no-daemon --console=plain
```

Local cross-stack tests require the existing strictly guarded device harness on
127.0.0.1:55440 and disposable PostgreSQL127.0.0.1:55439/gridfall_community_test.
Do not point that harness at production. adb reverse tcp:55440 tcp:55440; then:

```sh
bash ./gradlew :app:connectedDebugAndroidTest \
  -PgridfallDebugApiBaseUrl=https://api.gridfall.site \
  -Pandroid.testInstrumentationRunnerArguments.communityTestServer=true \
  -Pandroid.testInstrumentationRunnerArguments.productionReadOnly=true \
  --max-workers=2 --no-daemon --console=plain
```

productionReadOnly opts into GET-only production tests. communityTestServer opts
into separate synthetic/local write journeys. Without them, their assumptions skip;
do not describe skipped cases as passed. Do not create production fake accounts.

Installed APK: app/build/outputs/apk/debug/app-debug.apk, com.example.gridfall.
It is the real source/client, not com.example.gridfall.preview. Safe public QA launch:

```sh
adb -s emulator-5554 shell am start -n com.example.gridfall/com.example.gridfall.MainActivity \
  --ez communityReadOnlyQa true
```

The flag is honored only by debug builds. A normal launcher start preserves existing
normal anonymous-auth behavior; use this command for this public-only QA session.
Production publishing must wait for authorized real test accounts, entered locally
without sharing passwords/ID tokens. No test sign-in or account creation automated.

## Remaining release gates / verdict

**CONDITIONAL GO** for controlled Android release preparation, not public publication.
Required: authorized test accounts A/B (completed profiles) and a pre-authorized
moderator where moderation verification is needed; publish clearly named unlisted
test maps, verify fetch/mine/import idempotency/rating/owner denials/report/unpublish,
refresh/expiry and account switching, clean up only those maps through supported
APIs, then verify physical Android/foldable behavior and an actual signed build.
No production writes until those accounts are explicitly available. No public APK
or release/tag/newsletter was produced. No production backend restart/migration needed
for these Android fixes. Existing malicious-client score forgery remains separate;
honest custom-run exclusion is not server-authoritative anti-cheat.

Raw local evidence (not committed): /tmp/gridfall-android-e2e-20261118/, including
Gradle XML/logs,18 GET statuses, baseline reproduction, offline/recovered UI dumps.
