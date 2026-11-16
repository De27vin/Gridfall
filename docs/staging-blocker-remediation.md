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
