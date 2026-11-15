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

## Android code sharing (phase 3)

Settings → Saved Maps → Share Map publishes a snapshot with an optional description
and explicit public/unlisted choice (default unlisted). A named signed-in profile
is required; guests can use Enter Map Code without authentication. The preview
shows the actual grid, author, dimensions, pool and code. Copy Code uses Android's
clipboard. Save Map Offline and Import & Play refetch availability before saving
to the existing local store. Repeat imports use the same local code-based ID.
Imported maps run through the custom/unranked engine and need no network to play
after import. Existing local save encoding and recovery remain unchanged.

Phase 3 verification: Android debug APK builds; 276 unit tests pass, including
code normalization/path rejection, validated publication parsing, the shared
format round trip, and existing gameplay regressions. Device journey verification
is reserved for integration testing once the marketplace UI is complete.

## Marketplace API (phase 4)

- `GET /community/maps?q=&sort=rated|downloads|newest|popular&limit=10&cursor=`:
  guest discovery; public, published, unhidden maps only. Search matches name and
  creator word prefixes through PostgreSQL's `simple` full-text GIN index.
- `GET /community/maps/mine`: same pagination, authenticated owner's maps,
  including unlisted, unpublished and moderated status. No other owner's data.
- `POST /community/maps/:code/import` with `{requestId: UUID}`: intentional
  download only; returns the validated document after checking availability.
- `PUT /community/maps/:code/rating` with `{stars: 1..5}`: named non-guest users;
  one editable vote per user; self-ratings rejected.
- `POST /community/maps/:code/report` with `{reason: "inappropriate"|"spam"|"other",
  notes?: string}`: named non-guest users; one report per user/map.
- `GET /community/moderation/reports?page=0` and
  `PUT /community/moderation/:code` with `{action: "hide"|"remove", reason}`:
  require a **verified Firebase custom claim** `admin: true`. Public APIs never
  grant this role. Removal retains records for review but disables publication.

New tables: `CommunityMapRating`, `CommunityMapDownload`, `CommunityMapReport`.
All aggregate updates are serialized with a map row lock. Database constraints
enforce unique ratings/import actors/request IDs and valid star ranges. Reports
are rate limited (5/hour/IP), imports/ratings 30/hour/IP. Discovery has the global
300/minute/IP limit. Reverse proxies must preserve trustworthy client addresses;
do not enable blanket trust of forwarded headers from untrusted clients.

Configure a stable, random **32+ character** `COMMUNITY_DOWNLOAD_SECRET` before
enabling imports. Missing configuration fails closed with 503. Downloads retain
an HMAC of the authenticated identity or guest IP, never raw IPs/UIDs. They count
once per map/actor, with request UUID deduplication across retries. Guest devices
behind one NAT may conservatively share one count; signing in produces a separate
identity. This is abuse resistance, not a unique-human guarantee. Rotating the
secret resets actor deduplication and should be avoided routinely.

Rating ranking uses `(35 + sum(stars)) / (10 + voteCount)` (ten prior votes at
3.5). The UI still shows the actual average/count. Popularity multiplies this
rank by `ln(2 + downloads)`. Sort ties use creation time and UUID. Cursors bind
the search/sort/owner and a creation snapshot, preventing new publications from
displacing later pages. Existing maps can move after votes/imports; refresh to
restart ordering. Page size is capped at ten so full validated map previews stay
bounded. No private ownership identifiers or email addresses are returned.

Phase 4 verification: TypeScript build and all 35 backend tests pass, including
16 disposable-PostgreSQL publication/marketplace integration tests. The two
additive migrations were applied only to the isolated local test database.

## Android marketplace (phase 5)

Settings → Community Maps opens the themed, independently scrolling marketplace.
Search by map/creator, choose Top Rated / Popular / Most Downloaded / Newest,
and load subsequent pages. Cards display the actual blocked grid, author,
dimensions, vote average/count and intentional imports. Details support code
copying, offline save, import/play, editable 1–5-star voting and reporting.
My Published Maps includes unlisted/unpublished/hidden states and confirmed
unpublishing. Existing local copies remain accessible from Saved Maps.

Controls wrap and detail dialogs scroll at large fonts; gameplay layout and
gestures are untouched. Guest actions never request authentication tokens;
server-side eligibility and ownership remain authoritative. Loading, empty,
offline/retry and API failure states are shown. API requests distinguish read-only
previews from explicit import POSTs and reuse an import UUID for retries.

Verification: debug APK and test APK build, 277 Android unit tests pass, and four
new emulator tests pass (API 30): guest browsing/sorting/details at 150% font,
intentional import into the existing local store/custom engine, offline retry,
and empty search/guest publishing restrictions. HTTP-client tests also verify
that previews are GET-only and import retries preserve their request ID.
