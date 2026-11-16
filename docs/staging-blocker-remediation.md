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
