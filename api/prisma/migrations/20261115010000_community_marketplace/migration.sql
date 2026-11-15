ALTER TABLE "CommunityMap"
 ADD COLUMN "hidden" BOOLEAN NOT NULL DEFAULT false,
 ADD COLUMN "downloads" INTEGER NOT NULL DEFAULT 0 CHECK ("downloads" >= 0),
 ADD COLUMN "ratingSum" INTEGER NOT NULL DEFAULT 0,
 ADD COLUMN "ratingCount" INTEGER NOT NULL DEFAULT 0 CHECK ("ratingCount" >= 0),
 ADD COLUMN "ratingRank" DOUBLE PRECISION NOT NULL DEFAULT 3.5,
 ADD COLUMN "moderationReason" VARCHAR(500),
 ADD COLUMN "searchVector" tsvector GENERATED ALWAYS AS (to_tsvector('simple'::regconfig, "name" || ' ' || "creatorName")) STORED;
CREATE INDEX "CommunityMap_search_idx" ON "CommunityMap" USING GIN ("searchVector");
CREATE INDEX "CommunityMap_visibility_published_hidden_ratingRank_createdAt_idx" ON "CommunityMap" ("visibility", "published", "hidden", "ratingRank", "createdAt", "id");
CREATE INDEX "CommunityMap_visibility_published_hidden_downloads_createdAt_idx" ON "CommunityMap" ("visibility", "published", "hidden", "downloads", "createdAt", "id");
CREATE TABLE "CommunityMapRating" (
 "mapId" UUID NOT NULL REFERENCES "CommunityMap"("id") ON DELETE CASCADE,
 "userId" UUID NOT NULL REFERENCES "User"("id") ON DELETE RESTRICT,
 "stars" INTEGER NOT NULL CHECK ("stars" BETWEEN 1 AND 5),
 "updatedAt" TIMESTAMPTZ(3) NOT NULL,
 PRIMARY KEY ("mapId", "userId")
);
CREATE TABLE "CommunityMapDownload" (
 "mapId" UUID NOT NULL REFERENCES "CommunityMap"("id") ON DELETE CASCADE,
 "actorHash" VARCHAR(64) NOT NULL,
 "requestId" UUID NOT NULL,
 "createdAt" TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY ("mapId", "actorHash"), UNIQUE ("mapId", "requestId")
);
CREATE TABLE "CommunityMapReport" (
 "mapId" UUID NOT NULL REFERENCES "CommunityMap"("id") ON DELETE CASCADE,
 "userId" UUID NOT NULL REFERENCES "User"("id") ON DELETE RESTRICT,
 "reason" VARCHAR(32) NOT NULL,
 "notes" VARCHAR(500) NOT NULL,
 "createdAt" TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY ("mapId", "userId")
);
CREATE INDEX "CommunityMapReport_createdAt_mapId_userId_idx" ON "CommunityMapReport" ("createdAt", "mapId", "userId");
