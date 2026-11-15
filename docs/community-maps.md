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
