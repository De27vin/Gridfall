-- Database-owned normalization is the single source of truth for existing and new maps.
-- Ignore editor IDs and pool/cell ordering, retain authored text/geometry/weights/colors.
CREATE FUNCTION community_map_content_fingerprint(doc JSONB) RETURNS VARCHAR(64)
LANGUAGE SQL IMMUTABLE STRICT AS $$
  SELECT encode(digest('community-content-v1:' || jsonb_build_object(
    'formatVersion', doc->'formatVersion', 'name', doc->'name',
    'description', doc->'description', 'rows', doc->'rows', 'columns', doc->'columns',
    'blockedCells', (SELECT COALESCE(jsonb_agg(cell ORDER BY (cell->>'row')::int, (cell->>'col')::int), '[]'::jsonb)
      FROM jsonb_array_elements(doc->'blockedCells') AS cell),
    'blockPool', (SELECT COALESCE(jsonb_agg(entry ORDER BY entry::text COLLATE "C"), '[]'::jsonb)
      FROM (SELECT (block - 'id') || jsonb_build_object('cells',
        (SELECT jsonb_agg(cell ORDER BY (cell->>'row')::int, (cell->>'col')::int)
          FROM jsonb_array_elements(block->'cells') AS cell)) AS entry
        FROM jsonb_array_elements(doc->'blockPool') AS block) AS normalized)
  )::text, 'sha256'), 'hex')
$$;

ALTER TABLE "CommunityMap" ADD COLUMN "contentFingerprint" VARCHAR(64);
UPDATE "CommunityMap" SET "contentFingerprint" = community_map_content_fingerprint("document");
ALTER TABLE "CommunityMap" ALTER COLUMN "contentFingerprint" SET NOT NULL;
CREATE INDEX "CommunityMap_ownerId_contentFingerprint_idx" ON "CommunityMap" ("ownerId", "contentFingerprint");
CREATE TABLE "CommunityMapContentRestriction" (
  "ownerId" UUID NOT NULL REFERENCES "User"("id") ON DELETE RESTRICT,
  "fingerprint" VARCHAR(64) NOT NULL,
  "sourceMapId" UUID NOT NULL REFERENCES "CommunityMap"("id") ON DELETE RESTRICT,
  "createdAt" TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY ("ownerId", "fingerprint")
);
-- Respect historical moderation, without inventing a moderator or rewriting audit events.
-- Owner-only unpublishing (hidden=false) does not create a restriction.
INSERT INTO "CommunityMapContentRestriction" ("ownerId", "fingerprint", "sourceMapId")
SELECT DISTINCT ON ("ownerId", "contentFingerprint") "ownerId", "contentFingerprint", "id"
FROM "CommunityMap" WHERE "hidden"
ORDER BY "ownerId", "contentFingerprint", "createdAt", "id";
