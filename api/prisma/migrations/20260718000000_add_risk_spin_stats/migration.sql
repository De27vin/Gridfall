ALTER TABLE "Run" ADD COLUMN "riskSpinsUsed" INTEGER NOT NULL DEFAULT 0;

ALTER TABLE "PlayerProfile" ADD COLUMN "totalRiskSpinsUsed" INTEGER NOT NULL DEFAULT 0;

UPDATE "PlayerProfile" AS profile
SET "totalRiskSpinsUsed" = COALESCE((
  SELECT SUM(run."riskSpinsUsed")
  FROM "Run" AS run
  WHERE run."userId" = profile."userId"
), 0);
