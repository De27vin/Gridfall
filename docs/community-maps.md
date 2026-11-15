# Community maps

## Format v1

`shared/community-map-v1.json` is the Kotlin/TypeScript golden fixture. Documents
contain exactly `formatVersion`, `name`, `description` (empty when absent), `rows`,
`columns`, `blockedCells`, and `blockPool`. Dimensions are 3–10 per axis. Blocks
retain ordered IDs, names, colors 1–4, disconnected shapes within a normalized
4×4 field, and integer spawn probabilities in tenths of a percent totaling 1000.

Canonical JSON uses lexically sorted object keys, row/column-sorted cell arrays,
unchanged block-pool order, UTF-8, and no whitespace. Titles are at most 24 UTF-16
units; descriptions 500; block names/IDs 64. Up to 64 blocks and 64 KiB per map.
No coercion, duplicate cells/IDs, unknown fields/versions, invalid Unicode,
unplayable boards, or spawnable shapes with no valid anchor are accepted.

This format is separate from local save recovery. Existing local maps are not
rewritten or rejected by a new loader. Export normalizes shape origins exactly as
the existing engine does; pool ordering, probabilities and colors are preserved.
Maps that local editing permits but cannot be played may remain saved locally;
they cannot be published through this stricter interchange boundary.

## Publications (phase 2)

- `POST /community/maps`: verified Firebase bearer token, non-guest named profile;
  `{document, visibility: "public"|"unlisted", requestId: UUID}`. Retrying the same
  owner/request ID returns the same immutable publication; changed content is 409.
- `GET /community/maps/:code`: public, case-insensitive lookup; unavailable codes
  return 404 and responses are not cached. Codes have ten random Crockford symbols
  after `GF-` (50 random bits); uniqueness conflicts are retried safely.
- `GET /community/maps/mine`: authenticated owner's publications only.
- `DELETE /community/maps/:code`: owner-only, idempotent soft unpublish. Content
  is never edited in place; republishing uses a new request ID and code.

`CommunityMap` is additive and references the existing user ID internally. Public
responses expose only code, author display name, timestamps, visibility, status,
and validated map content; no Firebase UID or private email is serialized.
Publication is limited to ten requests/hour/IP; lookups to thirty/minute/IP.
The migration file is supplied for review; application startup does not migrate.

Integration tests require `COMMUNITY_TEST_DATABASE_URL` and `DATABASE_URL` set to
the same disposable database at `127.0.0.1:55439/gridfall_community_test`.
They refuse any other database, and skip when the explicit test opt-in is absent.
