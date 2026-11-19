-- Prisma cannot represent expression indexes. Keep this index in reviewed SQL
-- migrations so popularity discovery does not sort the entire public catalog.
CREATE INDEX "CommunityMap_popularity_idx" ON "CommunityMap"
 (("ratingRank" * ln(2.0 + "downloads")) DESC, "createdAt" DESC, "id" DESC)
 WHERE "visibility" = 'public' AND "published" AND NOT "hidden";
