# Authenticated Community Maps E2E — 2026-11-18

## Current status: blocked on owner login

Branch: `fix/community-maps-android-e2e`, starting HEAD `cb71cf3`.
This is an interim verification record, not a completed authenticated QA approval.
No production write or test publication has been made in this execution.

The visible `Gridfall_UI_Pixel_7` emulator (Android 11, API 30, emulator-5554)
has the actual production-backed Gridfall debug application open on Login.
The owner was asked to sign in directly using existing named Account A and reply
“Account A ready”. No credentials, account emails, ID tokens or passwords were
requested through chat, inspected from device storage, or included in this report.
The emulator has no configured autofill service. No physical device is connected.

## Changes prepared locally

- MainActivity: debug-only login entry via existing `communityReadOnlyQa` plus
  `communityLoginQa`; explicit existing-session reconnect for QA restarts.
- GameScreen: QA sessions skip guest progress merging, pending-score retries and
  official-run submission; QA logout signs out without creating a Firebase guest.
  Normal application defaults remain unchanged. Login can open directly without
  navigating through the game or creating an account.
- GridfallAuthManager: explicit sign-out without guest creation.
- AuthDialog: password keyboard type prevents normal text keyboard treatment.
- CommunityAuthenticatedProductionDeviceTest: opt-in phased production harness,
  real Firebase SDK session, no synthetic tokens, no authentication bypass.
  Publishes a clearly labelled temporary public map, persists request and identity
  fingerprints in app-private QA metadata, verifies retries and ownership,
  imports/rates/reports with a distinct Account B, exercises real GameScreen Import
  & Play, then unpublishes the exact recorded map as Account A. It refuses changed
  A/B identities or cleanup of a document different from this run's document.
- CommunityLoginSafetyDeviceTest: no-login UI regression asserting opening and
  dismissing the login form leaves Firebase identity unchanged.

The last two device tests are compiled, not yet executed. No claim of successful
production authentication follows from their presence or from previous mock tests.
The installed app was built during this run; a later APK with existing-session
reconnect refinements is available locally and must be installed after the owner
finishes credential entry. Do not reinstall/restart while credentials are being typed.

## Verification matrix — this execution only

| Check | Environment/evidence | Result |
|---|---|---|
| Fresh Android unit/local HTTP tests | `--rerun-tasks`, XML totals | 286 passed; 0 failures/errors/skips |
| Debug APK and instrumentation APK | Fresh Gradle build | PASS |
| Private signed release APK | Existing local signing prerequisites; assembleRelease; apksigner verify | PASS build/signature only |
| `/health`, `/ready` | Real production GET | 200 / 200 |
| Public newest browse, limit 10 | Real production GET | 200 |
| Owned maps without token | Real production GET | 401 |
| Moderation reports without token | Real production GET | 401 |
| Owned maps with malformed bearer | Real production GET | 401 |
| Actual signed expired/wrong-project tokens | No real test token available | NOT TESTED |
| Account A login, profile, publish, retry, mine, self-rating denial | Waiting for interactive login | BLOCKED |
| Account B browse/import/counter/rating/report/ownership | Requires distinct interactive login | BLOCKED |
| Production Import & Play navigation | Requires this run's publication and Account B | BLOCKED |
| Cleanup/unpublication and offline retained import | No test resources exist yet | NOT APPLICABLE yet |
| Fresh emulator regression suite | Deferred to avoid disrupting credential entry | NOT RUN this execution |
| Physical Pixel 9 Pro / GrapheneOS | Not connected | UNAVAILABLE |
| Release APK install and authenticated runtime | Only debug app currently installed | NOT TESTED |

Six real production HTTP checks passed. These were low-volume GETs using the actual
application user agent `okhttp/4.12.0`; none changed production state. Missing and
malformed token rejection is not signed-token expiry/project/admin verification.
Firebase Android configuration selects project `gridfall-auth` and package
`com.example.gridfall`; this metadata check is not a live login result.

Previous execution's 62 device tests and 18 HTTP checks are historical evidence,
not freshly executed here. Previous synthetic/local cross-stack journeys do not
establish authenticated production behavior. Import & Play's existing fix remains
in source; its production-backed path has not yet been exercised with a real map.

## Resume safely after owner confirmation

Install the latest debug APK with `adb install -r` (preserves app/session data),
then run ONLY the opted-in phase below. Never run it without owner-confirmed login.
No token is passed in command arguments:

```sh
adb -s emulator-5554 shell am instrument -w \
  -e class com.example.gridfall.ui.CommunityAuthenticatedProductionDeviceTest \
  -e productionAuthenticated accountA \
  com.example.gridfall.test/androidx.test.runner.AndroidJUnitRunner
```

After A succeeds, use QA sign-out (no anonymous account creation), reopen Login,
and request manual existing **distinct Account B** login. Repeat the command with
`accountB`. After B succeeds, request original Account A login and run `cleanup`.
Inspect results before moving phases. On failures, retain run metadata and retry
the same phase; never create replacement request IDs to bypass an uncertain result.
Backend restart is not authorized by this task; verify client/process recreation
only, and do not claim actual production backend restart recovery.

The live harness intentionally makes a report on its own temporary map. Supported
unpublication removes public availability but does not hard-delete publication,
rating, download or report audit rows. Record the map code and retained QA report
in the completed report. No SQL deletion or unrelated record modification is allowed.

Once credential entry is complete, run the normal isolated emulator regressions
and the no-login safety test. Do not run local synthetic-token test harnesses against
production. A physically connected device and release runtime still need verification.

## Release decision

**NO-GO for final authenticated release approval at this checkpoint.** Required
positive two-account evidence does not yet exist. No production infrastructure,
backend, migration, release, GitHub issue or master branch was modified. No APK
was published. Existing client-submitted score-forgery limitations are unchanged;
QA safeguards and custom-run exclusion are not an anti-cheat redesign.

## Beta authorization checkpoint — 2026-11-19

The owner subsequently authorized a public GitHub beta without waiting for the
blocked authenticated two-account journey. Fresh beta verification passed286 unit
tests and61 emulator tests, with3 explicitly skipped opt-in tests (the authenticated
production phase and two isolated API/PG cross-stack journeys). The new no-login
safety test passed. This supersedes the “not yet executed” status for that safety
test only; it does not establish positive production Firebase authentication.
See community-maps-beta-verification.md for beta scope, signature/update evidence
and the corrected populated-market read-only test. No production QA writes were made.
