CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE "User" (
    "id" UUID NOT NULL DEFAULT gen_random_uuid(),
    "firebaseUid" TEXT NOT NULL,
    "email" TEXT,
    "isAnonymous" BOOLEAN NOT NULL DEFAULT false,
    "username" TEXT,
    "usernameNormalized" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,
    "lastSeenAt" TIMESTAMP(3),
    "mergedIntoUserId" UUID,
    CONSTRAINT "User_pkey" PRIMARY KEY ("id")
);

CREATE TABLE "PlayerProfile" (
    "id" UUID NOT NULL DEFAULT gen_random_uuid(),
    "userId" UUID NOT NULL,
    "bestScore" INTEGER NOT NULL DEFAULT 0,
    "bestLevel" INTEGER NOT NULL DEFAULT 1,
    "gamesPlayed" INTEGER NOT NULL DEFAULT 0,
    "totalLinesCleared" INTEGER NOT NULL DEFAULT 0,
    "totalContractsCompleted" INTEGER NOT NULL DEFAULT 0,
    "totalBombsUsed" INTEGER NOT NULL DEFAULT 0,
    "totalMegaBombsUsed" INTEGER NOT NULL DEFAULT 0,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,
    CONSTRAINT "PlayerProfile_pkey" PRIMARY KEY ("id")
);

CREATE TABLE "Run" (
    "id" UUID NOT NULL DEFAULT gen_random_uuid(),
    "userId" UUID NOT NULL,
    "score" INTEGER NOT NULL,
    "level" INTEGER NOT NULL,
    "linesCleared" INTEGER NOT NULL DEFAULT 0,
    "contractsCompleted" INTEGER NOT NULL DEFAULT 0,
    "bombsUsed" INTEGER NOT NULL DEFAULT 0,
    "megaBombsUsed" INTEGER NOT NULL DEFAULT 0,
    "durationSeconds" INTEGER,
    "appVersion" TEXT,
    "validationStatus" TEXT NOT NULL DEFAULT 'accepted',
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "Run_pkey" PRIMARY KEY ("id")
);

CREATE TABLE "GuestMergeToken" (
    "id" UUID NOT NULL DEFAULT gen_random_uuid(),
    "tokenHash" TEXT NOT NULL,
    "guestUserId" UUID NOT NULL,
    "expiresAt" TIMESTAMP(3) NOT NULL,
    "usedAt" TIMESTAMP(3),
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "GuestMergeToken_pkey" PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX "User_firebaseUid_key" ON "User"("firebaseUid");
CREATE UNIQUE INDEX "User_usernameNormalized_key" ON "User"("usernameNormalized");
CREATE UNIQUE INDEX "PlayerProfile_userId_key" ON "PlayerProfile"("userId");
CREATE INDEX "Run_userId_idx" ON "Run"("userId");
CREATE INDEX "Run_score_idx" ON "Run"("score");
CREATE UNIQUE INDEX "GuestMergeToken_tokenHash_key" ON "GuestMergeToken"("tokenHash");
CREATE INDEX "GuestMergeToken_guestUserId_idx" ON "GuestMergeToken"("guestUserId");
CREATE INDEX "GuestMergeToken_expiresAt_idx" ON "GuestMergeToken"("expiresAt");

ALTER TABLE "User" ADD CONSTRAINT "User_mergedIntoUserId_fkey" FOREIGN KEY ("mergedIntoUserId") REFERENCES "User"("id") ON DELETE SET NULL ON UPDATE CASCADE;
ALTER TABLE "PlayerProfile" ADD CONSTRAINT "PlayerProfile_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE "Run" ADD CONSTRAINT "Run_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE "GuestMergeToken" ADD CONSTRAINT "GuestMergeToken_guestUserId_fkey" FOREIGN KEY ("guestUserId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

