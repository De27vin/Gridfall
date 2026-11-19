CREATE TABLE "CommunityMap" (
  "id" UUID NOT NULL PRIMARY KEY,
  "code" VARCHAR(13) NOT NULL UNIQUE,
  "ownerId" UUID NOT NULL REFERENCES "User"("id") ON DELETE RESTRICT,
  "requestId" UUID NOT NULL,
  "payloadHash" VARCHAR(64) NOT NULL,
  "name" VARCHAR(24) NOT NULL,
  "description" VARCHAR(500) NOT NULL,
  "creatorName" VARCHAR(64) NOT NULL,
  "visibility" VARCHAR(8) NOT NULL CHECK ("visibility" IN ('public','unlisted')),
  "document" JSONB NOT NULL,
  "published" BOOLEAN NOT NULL DEFAULT TRUE,
  "createdAt" TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "unpublishedAt" TIMESTAMPTZ(3),
  CONSTRAINT "CommunityMap_ownerId_requestId_key" UNIQUE ("ownerId", "requestId")
);
CREATE INDEX "CommunityMap_ownerId_createdAt_id_idx" ON "CommunityMap"("ownerId", "createdAt", "id");
CREATE INDEX "CommunityMap_visibility_published_createdAt_id_idx" ON "CommunityMap"("visibility", "published", "createdAt", "id");
