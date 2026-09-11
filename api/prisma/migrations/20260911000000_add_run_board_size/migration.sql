ALTER TABLE "Run" ADD COLUMN "boardSize" INTEGER NOT NULL DEFAULT 8;
ALTER TABLE "Run" ADD CONSTRAINT "Run_boardSize_check" CHECK ("boardSize" IN (7, 8, 10));

CREATE INDEX "Run_boardSize_score_idx" ON "Run"("boardSize", "score");
