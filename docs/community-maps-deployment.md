# Community Maps deployment and testing

This branch is **not deployed**. Issues #38 and #43 stay open until integration,
deployment and real-account verification. No production access is needed for the
local checks below. Never point test commands at the NAS database.

## Production rollout (requires explicit approval)

1. Review the feature branch and all four additive migrations:
   `20261115000000_community_publications`,
   `20261115010000_community_marketplace`,
   `20261115020000_community_popularity_index`, and
   `20261116000000_moderation_history`. Existing users, profiles, runs and
   leaderboards are retained. The second migration adds tables, aggregate
   columns, a generated full-text search column and indexes; it needs no extra
   PostgreSQL extensions. The third adds a partial expression index for popularity
   discovery (maintained in SQL because Prisma cannot represent expression indexes).
   PostgreSQL 16 migration/upgrade rehearsals and PostgreSQL 17 integration tests are supported.
   The fourth migration adds immutable moderation history; its UPDATE/DELETE trigger
   intentionally prevents routine history edits, including ORM deletion.
2. Rehearse on a **restored copy** of production data with isolated credentials,
   Firebase project and hostname. Confirm migration timing/locks and old Android
   clients' login, run submission and leaderboards. Building the generated search
   column/index locks the new community table; schedule a maintenance window if
   it already contains significant publication data.
3. Take a PostgreSQL custom-format backup with `pg_dump -Fc`, plus the deployment
   configuration and current API image digest. Keep backups encrypted/off-host.
   Check `pg_restore --list` and actually restore into an isolated database.
   Do not treat a backup as verified until that restore succeeds.
4. Build a versioned API image from `api/`. Provision a stable cryptographically
   random `COMMUNITY_DOWNLOAD_SECRET` (32+ characters) through the NAS environment
   or secret store. Existing Firebase service-account JSON remains a read-only
   secret mount, never in the repository/image. `.env.example` has no real secret.
5. Pause API writes and run **one explicit migration job** against the approved
   database using the new image, e.g. `docker compose run --rm api npx prisma
   migrate deploy`. Review `prisma migrate status` and logs before continuing.
   The Docker image now starts only `node dist/server.js`; startup and restart
   no longer run migrations. Do not use `prisma db push`, `migrate reset`, or
   `migrate dev` on production. Do not delete the PostgreSQL volume.
6. Start the new API. Verify HTTPS, Firebase issuer/project, authenticated named
   profile publishing, guest fetching/importing, privacy, public/unlisted
   separation, ratings, rate limits and moderation. Import requests fail closed
   with 503 if the deduplication secret is missing. Verify proxy addressing:
   Fastify deliberately does not trust arbitrary forwarded headers. The current
   deployment may see a reverse proxy's IP, conservatively grouping guest counts
   and limits; configure and audit a trusted proxy boundary before changing this.
7. Appoint moderators by securely assigning the Firebase custom claim
   `admin: true` with an existing administrative process. Ordinary clients cannot
   grant it. Reports are reviewed through the documented API; there is no web
   moderation console. Only hide/remove are supported for MVP.
8. Run the real-account checklist below. Only then approve integration into
   master and an Android version/tag/release. No release tag was created here.
   Deploy the API before distributing the app; older APIs yield a clear
   unavailable/not-found error, while imported/local maps still play offline.

### Rollback and roll-forward

Prefer an API image rollback while **retaining** the additive schema and all new
community data. The old API ignores new tables/columns; existing Android game
features remain compatible. Community actions become unavailable until repaired;
offline imports survive. Keep the original import secret to preserve deduplication.
Do not drop tables or undo migrations to roll back an image.

For a failed migration, keep writes paused, inspect the error/transaction state,
and repair through a reviewed forward migration. Use `prisma migrate resolve`
only after confirming the actual schema state. Restore from the verified backup
only as a last resort with explicit approval and an agreed data-loss window;
restoring discards writes after that backup. Rehearse this process first.

## Local backend verification

Use Node 22+ and an isolated PostgreSQL container bound to loopback. For example:

```sh
docker run --rm -d --name gridfall-community-test --cpus=2 --memory=512m \
  -p 127.0.0.1:55439:5432 --tmpfs /var/lib/postgresql/data \
  -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=community_test_only \
  -e POSTGRES_DB=gridfall_community_test postgres:17-alpine
cd api
npm ci
export DATABASE_URL=postgresql://postgres:community_test_only@127.0.0.1:55439/gridfall_community_test
export COMMUNITY_TEST_DATABASE_URL="$DATABASE_URL"
npx prisma generate
npx prisma migrate deploy
npm run build
npm test -- --maxWorkers=2 --minWorkers=1
```

The password above is disposable **test-only** data. Test suites reject other
database hosts/ports/names; without the explicit opt-in, database integration
tests are skipped. Unit-only success is not evidence of PostgreSQL verification.
The tmpfs database disappears when its container stops. Never reuse NAS mounts.

## Android verification

From the repository root, with an isolated emulator connected:

```sh
bash ./gradlew :app:testDebugUnitTest :app:assembleDebug \
  :app:connectedDebugAndroidTest --max-workers=2 --console=plain
```

On Windows use `gradlew.bat` with the same tasks. The debug APK is
`app/build/outputs/apk/debug/app-debug.apk`. The default debug API address is the
existing LAN NAS URL; do not publish test maps there. UI tests inject a fake
gateway. The cross-stack journey below uses a strictly isolated loopback API.

### Opt-in Android → real API → PostgreSQL journey

After the local backend build/migrations above, in `api/` run:

```sh
NODE_ENV=test COMMUNITY_DEVICE_TEST=1 node test-support/community-device-server.cjs
```

Keep the two disposable database variables exported. The harness refuses other
databases/environments and binds only `127.0.0.1:55440`. It replaces Firebase
verification with two **synthetic test identities**, while using real routes,
validation, transactions and PostgreSQL. It is outside `src/`, excluded from the
production Docker image, and never part of API startup.

In another terminal:

```sh
adb reverse tcp:55440 tcp:55440
bash ./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.example.gridfall.ui.CommunityMapsJourneyDeviceTest \
  -Pandroid.testInstrumentationRunnerArguments.communityTestServer=true \
  --max-workers=2 --console=plain
```

The test saves a custom map, publishes through the UI, copies its real code,
fetches/imports as the second identity, checks shape/color/probability persistence
and engine placement/unranked eligibility, rates, discovers the same publication,
then unpublishes and verifies new lookup/import fails while the local copy stays.
This verifies the integrated protocol; it does **not** verify live Firebase sign-in.

Repeat marketplace device tests on a small phone and tablet with larger fonts.
Use a disposable emulator only, e.g. `adb shell wm size 640x1136` and
`adb shell wm density 320` for a 320dp-wide small display; `1600x2560` at density
240 for a tablet. Reset with `adb shell wm size reset` / `wm density reset`.
The suite also covers prior gameplay viewport, contract-center, contract offer
interaction guards, contract/grid spacing, touch gestures and inventory tests.
Stop the test API and database and remove `adb reverse` after verification.

## Remaining real-world/manual checklist

- Real Firebase account A with completed profile: create/save/publish both
  public and unlisted maps, copy codes; verify signed-out/unnamed publishing fails.
- Account B and guest on another device: preview, import, play; go offline and
  restart the app to play again. Existing local maps must still load/edit/play.
- B rates then updates; A cannot self-rate. Public search finds A by map/creator;
  unlisted maps never appear. Check all sorts and multi-page results.
- A unpublishes; new fetch/import fails, existing local copies remain. Moderator
  hides/removes a reported map; normal users cannot access moderation routes.
- Verify actual Samsung Fold open/closed, a small phone and tablet, keyboard,
  increased font sizes, themes, back navigation, screen insets, board drag/drop,
  contracts and joker inventory. Physical-device/Firebase verification is pending.
- Confirm custom/community scores never change official 7×7/8×8/10×10 standings.
  Android excludes custom runs at both policy and submission boundaries; the
  server rejects explicit custom/community markers. This does not establish
  anti-cheat for a malicious client falsely declaring an official run.
- Load-test the single API instance before wider release. Rate limits are
  process-local; multi-instance deployment needs a shared limiter. Guest counts
  are deliberately conservative by IP; no unique-human claim is made. Search
  uses word prefixes, not arbitrary substring/fuzzy matching. Mutable rankings
  can move between pages after new ratings/imports; refreshing restarts ordering.

Do not close either GitHub issue until the approved deployment and this checklist
have been completed and recorded.
