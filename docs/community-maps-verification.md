# Community Maps implementation record

Branch: `feat/community-maps`. Neither issue is closed; no merge, Android release
tag, production migration or production deployment is part of this work.

## Phase commits and scope

| Phase | Commit | Result |
| --- | --- | --- |
| 1 — shared format | `86a96ba` | Canonical v1 Kotlin/TypeScript format, bounded validation, compatibility fixture; 274 Android / 19 API tests and builds passed. |
| 2 — publications | `9eca70b` | Persistent immutable maps, random collision-safe codes, auth/profile eligibility, visibility and owner-only unpublish; 25 API tests/build passed. |
| 3 — Android codes | `e163b39` | Share/publish/copy, guest preview/import/play, local storage; 276 Android tests/debug APK passed. |
| 4 — marketplace API | `00848de` | Indexed discovery, keyset pagination, deduplicated imports, weighted ratings, reports/moderation; 35 API tests/build passed. |
| 5 — marketplace UI | `e48cbc7` | Search/sorts/previews/details/voting/reporting, owner management and offline states; 277 Android tests/debug APK + 4 device tests passed. |
| 6 — integration/release preparation | See phase-6 commit in branch history | Cross-stack journey, legacy-save checks, explicit leaderboard separation, keyboard/inset fixes, popularity index, deployment safety and documentation. |

## Verification evidence

- Final Android unit suite: **278 passed**, zero failures/errors/skips; debug APK
  and instrumentation APK build successfully. The shared fixture also plays a
  real engine placement and retains custom/unranked status.
- Full API-30 normal-phone device suite: **49 passed**, zero failures/skips,
  including the opt-in cross-stack journey and prior viewport, center-contract,
  contract-offer interaction, inventory/drag and contract/grid-spacing regressions.
- Small phone: **6 community device tests passed**, 640×1136 pixels at density
  320 (320dp width), system font 130%; one test explicitly uses 150% font.
- Tablet: **6 community device tests passed**, 1600×2560 at density 240, system
  font 150%. The marketplace limits content width and keeps lists/dialogs scrollable.
- Backend: TypeScript build and **37 tests passed**, including **16 disposable
  PostgreSQL integration tests**. Authentication, ownership, case-insensitive
  lookup, code collisions, unlisted discovery exclusion, concurrent rating/import
  retries, ranking, moderation, limits and leaderboard exclusion are covered.
- All seven migrations applied successfully to an additional empty local database
  as a fresh-install rehearsal. Three new migrations are additive. Prisma schema
  validation passed; read-only schema comparison found no community-schema drift.
  Four pre-existing UUID-default differences in unrelated base models were left
  unchanged; no generated diff was applied.
- Docker Compose configuration validates using dummy values without starting any
  NAS services. Docker startup no longer applies migrations automatically.
- `git diff --check` passed. Production credentials, signing files and service
  accounts were not added. No production infrastructure was accessed for deployment.

The cross-stack test publishes via Android UI, copies the generated code, fetches
and imports under a second synthetic identity, saves offline with exact shapes,
colors and probabilities, checks custom-engine eligibility/placement, rates and
discovers the same public publication, then unpublishes and verifies new imports
fail while the stored map remains playable. Firebase verification alone is mocked
in an opt-in, loopback-only harness; API routes and PostgreSQL are real.

Early verification caught and corrected a non-void JUnit test signature, lazy-list
test scrolling assumptions, and the marketplace dialog's keyboard/system-inset
handling on a small display. The final runs above passed after those corrections.

## Changed-file inventory

Paths below are relative to the repository root; repeated files show the phase in
which they changed. The phase-6 SHA is reported separately to avoid self-referential
commit metadata in this document.

### Phase 1

- `api/src/community/mapFormat.ts`
- `api/src/community/mapFormat.test.ts`
- `app/src/main/java/com/example/gridfall/game/CommunityMapDocument.kt`
- `app/src/test/java/com/example/gridfall/game/CommunityMapFormatTest.kt`
- `shared/community-map-v1.json`
- `docs/community-maps.md`

### Phase 2

- `api/prisma/schema.prisma`
- `api/prisma/migrations/20261115000000_community_publications/migration.sql`
- `api/src/community/publications.ts`
- `api/src/community/publications.integration.test.ts`
- `api/src/routes/community.ts`
- `api/src/server.ts`
- `docs/community-maps.md`

### Phase 3

- `app/src/main/java/com/example/gridfall/network/CommunityMapsClient.kt`
- `app/src/main/java/com/example/gridfall/ui/CommunityMapDialogs.kt`
- `app/src/main/java/com/example/gridfall/ui/GameScreen.kt`
- `app/src/main/java/com/example/gridfall/ui/SettingsScreen.kt`
- `app/src/test/java/com/example/gridfall/network/CommunityMapsClientTest.kt`
- `docs/community-maps.md`

### Phase 4

- `api/prisma/schema.prisma`
- `api/prisma/migrations/20261115010000_community_marketplace/migration.sql`
- `api/src/community/marketplace.ts`
- `api/src/community/marketplace.integration.test.ts`
- `api/src/community/publications.ts`
- `api/src/routes/community.ts`
- `api/src/firebase.ts`
- `docs/community-maps.md`

### Phase 5

- `app/src/main/java/com/example/gridfall/network/CommunityMapsClient.kt`
- `app/src/main/java/com/example/gridfall/ui/CommunityMapDialogs.kt`
- `app/src/main/java/com/example/gridfall/ui/CommunityMapsScreen.kt`
- `app/src/main/java/com/example/gridfall/ui/GameScreen.kt`
- `app/src/main/java/com/example/gridfall/ui/SettingsScreen.kt`
- `app/src/test/java/com/example/gridfall/network/CommunityMapsClientTest.kt`
- `app/src/androidTest/java/com/example/gridfall/ui/CommunityMapsDeviceTest.kt`
- `docs/community-maps.md`

### Phase 6

- `README.md`
- `api/.env.example`
- `api/Dockerfile`
- `api/docker-compose.yml`
- `api/prisma/schema.prisma`
- `api/prisma/migrations/20261115020000_community_popularity_index/migration.sql`
- `api/src/community/marketplace.integration.test.ts`
- `api/src/services/runsService.ts`
- `api/src/services/runsService.test.ts`
- `api/test-support/community-device-server.cjs`
- `app/src/debug/res/xml/network_security_config.xml`
- `app/src/main/java/com/example/gridfall/game/SavedCustomMap.kt`
- `app/src/main/java/com/example/gridfall/network/CommunityMapsClient.kt`
- `app/src/main/java/com/example/gridfall/sync/RunSubmissionPolicy.kt`
- `app/src/main/java/com/example/gridfall/ui/CommunityMapDialogs.kt`
- `app/src/main/java/com/example/gridfall/ui/CommunityMapsScreen.kt`
- `app/src/main/java/com/example/gridfall/ui/GameScreen.kt`
- `app/src/main/java/com/example/gridfall/ui/MapBlockListScreen.kt`
- `app/src/main/java/com/example/gridfall/ui/SavedCustomMapStore.kt`
- `app/src/test/java/com/example/gridfall/game/CommunityMapFormatTest.kt`
- `app/src/test/java/com/example/gridfall/sync/RunSubmissionPolicyTest.kt`
- `app/src/androidTest/java/com/example/gridfall/ui/CommunityMapsDeviceTest.kt`
- `app/src/androidTest/java/com/example/gridfall/ui/CommunityMapsJourneyDeviceTest.kt`
- `app/src/androidTest/java/com/example/gridfall/ui/CommunityMapLocalCompatibilityDeviceTest.kt`
- `docs/community-maps.md`
- `docs/community-maps-deployment.md`
- `docs/community-maps-verification.md`

## Boundaries and remaining verification

See [format, models and endpoints](community-maps.md) and the
[deployment/testing runbook](community-maps-deployment.md) for exact instructions.
Remaining work is approved deployment/integration and the manual checklist: real
Firebase accounts, production-copy backup/restore rehearsal, live reverse-proxy
and rate-limit configuration, moderation role setup, physical Fold/phone/tablet
testing and release approval. Synthetic identities do not substitute for live
Firebase testing. Guest counts are conservative by IP; multi-instance rate limits
need shared storage; search is word-prefix based; changing rankings can move maps
between pages. No fuzzy search, moderation web console or anti-cheat attestation
is claimed. Community/custom runs remain excluded from official leaderboards.
