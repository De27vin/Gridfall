ALTER TABLE "PlayerProfile" ADD COLUMN "totalPoints" INTEGER NOT NULL DEFAULT 0;

UPDATE "PlayerProfile" AS profile
SET "totalPoints" = COALESCE((
  SELECT SUM(run."score")
  FROM "Run" AS run
  WHERE run."userId" = profile."userId"
), 0);