# Staging isolation and rollout contracts

Android staging requires `-PgridfallStagingApiBaseUrl=https://YOUR-STAGING-HOST`
or `GRIDFALL_STAGING_API_BASE_URL`. Missing/production URLs fail validation;
there is no production fallback. Staging has application ID
`com.example.gridfall.staging` and version suffix `-staging`. Supply an authorized,
separate Firebase Android configuration at ignored
`app/src/staging/google-services.json`; its project must differ from the default
client. Google Services also validates the suffixed package. Never commit this
configuration or service-account material. Debug/release endpoint behavior remains
unchanged. Default staging API clients refuse redirects so a redirect cannot silently
select another environment. DNS, Firebase project ownership and the actual staging host
must still be verified against the authorized staging infrastructure.

Run `bash scripts/check-staging-isolation.sh` for missing/unsafe endpoint checks.
The validation task has no outputs, so it always checks the environment-specific
file before staging build tasks execute, including when configuration cache is reused.

Portable Compose now requires `GRIDFALL_DATABASE_STORAGE` and
`GRIDFALL_FIREBASE_CREDENTIALS_FILE`; no NAS path is embedded. Select explicitly
isolated paths and a separate Compose project/network/port plan for staging.
The bind sources must already exist; Compose must not silently create a directory
for a missing credential file. Existing production operators may supply their existing mount paths through their
uncommitted environment; changing this template does not move or reset data.
Validate interpolation with `docker compose config --quiet` before any approved
future rollout. No deployment is performed by these checks. Production and staging
must have separate credentials, databases and secrets.

## Readiness

`GET /health` is liveness only and does not query dependencies. `GET /ready` checks
connectivity and essential user/run/community tables, returns 200 only when the
probe succeeds, and otherwise returns 503 without exposing connection details.
Responses are not cacheable. Probes share one in-flight database operation and
have a two-second HTTP deadline; this deadline does not claim to cancel underlying
driver I/O. Configure bounded driver connection/pool timeouts as well. Use readiness,
explicit migration status and authenticated smoke tests before routing traffic;
readiness does not certify Firebase credentials, proxy topology or data correctness.

After building/migrating an explicitly disposable database, run
`NODE_ENV=test node test-support/readiness-outage.cjs` in `api/` with DATABASE_URL
and COMMUNITY_TEST_DATABASE_URL both set to the documented loopback test URL.
The script removes/restores a local TCP proxy to prove real dependency failure and
recovery without restarting the API; it never stops a production database.

## Owner-scoped moderation content policy

Migration `20261116020000_owner_content_restrictions` adds a SHA-256 fingerprint
computed by one immutable PostgreSQL normalization function, plus owner/fingerprint
restrictions. Editor-generated block IDs, pool/cell ordering and publication visibility
do not change the fingerprint. Authored text, dimensions, geometry, weights and colors
do. Hide/remove atomically creates the restriction alongside the existing immutable,
attributable moderation event. Publishing takes the same owner lock; a new request
for restricted content returns 422. Existing idempotency keys still return their
original publication, marked unavailable, rather than creating a replacement.
Existing identical codes are excluded from discovery, lookup, rating and imports.
Other owners and genuinely edited documents are not blocked. Android clears a
pending request on definitive 422 rejection; uncertain failures retain it for retry.

The migration backfills historical hidden content without rewriting old audit events.
Owner-only unpublishing creates no restriction and cannot remove one. This is exact
content moderation, not fuzzy similarity detection or a user ban: deliberate authored
changes or another account still require normal reporting/moderation. There is no
unaudited automatic restriction-lifting endpoint.

The migration updates existing map rows and builds an index: rehearse duration and
locks on a representative database copy before an approved rollout. All nine migrations
passed a fresh disposable PostgreSQL 16 installation. An eight-to-nine migration
rehearsal preserved seeded users, maps and audit events byte-for-byte (excluding the
new fingerprint field), and excluded an old same-owner clone while retaining another
owner's identical map. No production database was accessed. The updated API requires
this migration; an older community API must not be exposed as a rollback because it
does not enforce the restriction or populate the new required column. Prefer a forward
repair, or keep community routes unavailable until a compatible image is restored.

## Staging verification gates (not a deployment authorization)

Firebase authentication still uses Admin SDK `verifyIdToken`, the configured project,
and `decoded.admin === true`; JSON, query parameters and forwarded headers do not
supply administrator authority. An anonymous identity is ineligible for publishing,
rating and moderation mutations. Token revocation is not checked on every request:
a removed claim/revoked session can remain effective until the ID token expires.
Use short-lived, controlled staging administrator access; urgent revocation requires
a server-side revocation/deny policy before relying on immediate revocation.

On 2026-11-16, six production-mode SDK negative assertions rejected malformed,
wrong-audience, wrong-issuer, empty-subject and unsigned-admin tokens (including
HTTP 401 on moderation). Seven additional SDK emulator-mode assertions included
expiry rejection and fail-closed behavior when the local Auth emulator user service
was absent. These are NOT successful signed-token, live Firebase, real-admin or
account-isolation verification. Database integration tests substitute identities to
exercise ownership and authorization branches; emulator UI tests use a synthetic
loopback API. No production credentials or Firebase users were used.

Before staging acceptance, provision an explicitly authorized separate Firebase
project and verify real registered, anonymous, incomplete-profile, non-owner and
administrator sessions against staging: valid tokens succeed only where eligible;
wrong-project, expired, malformed and non-admin tokens fail; logout/account switching
cannot reuse another account's pending publication. Confirm Firebase Auth emulator
variables are absent from the staging deployment. Never copy production credentials
as a substitute for these checks.

### Proxy and rate limits

The repository Compose file describes one API service with a fixed published port;
it does not establish the live Cloudflare connector topology. Fastify leaves
`trustProxy` disabled. Tests confirmed changing `X-Forwarded-For` and
`CF-Connecting-IP` cannot rotate the limiter identity. This prevents header spoofing,
but a tunnel/reverse proxy can collapse every visitor onto its socket address,
sharing quotas and guest download identity. It is safe against spoofing, not proof
of correct per-client accounting. IPv4 and IPv6 direct socket addresses work as
identities, but IPv6 address rotation and multi-account abuse remain possible;
this is not unique-human counting.

Rate limits are process-local (global 300/minute, plus sensitive route limits).
Restarts reset them; multiple replicas multiply effective limits. Only the database
publication quota (100 per owner in rolling 24 hours) survives replicas/restarts.
Import deduplication and rating uniqueness also persist, but are integrity rules,
not a distributed request limiter. Do not enable multiple replicas behind these
limits as if they were shared protection. A shared limiter/edge policy is a
multi-replica deployment gate and has not been silently added as a dependency.

Before exposing staging: map connector/hop addresses; restrict direct API access;
choose exact trusted proxy CIDRs/hops only after verifying the proxy overwrites
untrusted identity headers. Test forged forwarding headers, IPv4/IPv6 clients,
shared NATs, restarts, and guest/registered quotas through the actual tunnel.
Do not enable unrestricted `trustProxy: true` or trust a caller-supplied Cloudflare
header. If retaining proxy socket identity, explicitly accept shared quotas and
conservative guest counts. The API's public browse/preview/import routes are
intentional; providing an invalid Authorization header to import fails authentication.
Publishing, ratings, reports and moderation still require verified authentication.

## Existing official-score trust limitation (separate security work)

`POST /runs` validates numeric ranges but accepts client-supplied score/level without
replaying gameplay. This predates Community Maps: the feature adds rejection of
explicit custom/community markers, not server-authoritative scoring. A modified
client can omit those markers and claim an official board size. Authentication
identifies a caller; it does not prove their score.

A safe reproduction on the disposable loopback harness accepted
`{"score":1234567,"level":100,"boardSize":8}` as `validationStatus: accepted`
and updated the synthetic player's best score. No real user or production endpoint
was involved. The same payload class was accepted by the master baseline. Impact:
authenticated malicious clients can forge official rankings and aggregate stats;
Community Maps' honest-client exclusion must not be described as anti-cheat.

A separate remediation should introduce account-bound, server-issued run IDs and
versioned official rules/presets; authoritative or replay-verifiable piece generation,
move/contract/joker validation and score reconstruction; idempotent finalization and
replay protection; and an explicit noncompetitive scope for custom/community runs.
Do not trust a client flag, embedded signing key or app attestation alone. Plan
legacy/offline compatibility via clearly separated verified/unverified rankings,
plus resource/abuse limits and an account/stat migration policy. Stage that change
with replay/forgery tests before changing ranking guarantees. No scoring architecture
was changed in this remediation.
