import type { FastifyPluginAsync, FastifyRequest } from "fastify";
import type { PlayerProfile, Prisma, User } from "@prisma/client";
import { z } from "zod";
import { verifyFirebaseToken } from "../firebase";
import "../middleware/requireFirebaseAuth";
import { prisma } from "../db";
import { getOrCreateUserWithProfile } from "../services/usersService";

const leaderboardQuerySchema = z.object({
  limit: z.coerce.number().int().min(1).max(100).default(10),
  boardSize: z.coerce.number().int().refine((size) => [7, 8, 10].includes(size)).default(8)
});

export const publicProfileWhere: Prisma.PlayerProfileWhereInput = {
  user: {
    username: { not: null },
    mergedIntoUserId: null
  }
};

type LeaderboardType = "bestScore" | "totalPoints" | "linesCleared" | "contractsCompleted" | "riskSpinsUsed";

interface LeaderboardEntry {
  rank: number;
  username: string;
  bestScore: number;
  bestLevel: number;
  totalPoints: number;
  totalLinesCleared: number;
  totalContractsCompleted: number;
  totalRiskSpinsUsed: number;
  isInTop?: boolean;
}

interface GridRunRecord {
  userId: string;
  username: string | null;
  score: number;
  level: number;
  linesCleared: number;
  contractsCompleted: number;
  riskSpinsUsed: number;
}

export interface GridLeaderboardStats {
  userId: string;
  username: string;
  bestScore: number;
  bestLevel: number;
  gamesPlayed: number;
  totalPoints: number;
  totalLinesCleared: number;
  totalContractsCompleted: number;
  totalRiskSpinsUsed: number;
}

export const leaderboardRoutes: FastifyPluginAsync = async (app) => {
  // Kept for clients on the original single-score leaderboard contract.
  app.get("/leaderboard", async (request, reply) => {
    const query = leaderboardQuerySchema.safeParse(request.query);
    if (!query.success) {
      return reply.code(400).send({ error: "Invalid leaderboard query" });
    }

    const profiles = await prisma.playerProfile.findMany({
      where: publicProfileWhere,
      include: { user: true },
      orderBy: leaderboardOrderBy("bestScore"),
      take: query.data.limit
    });

    return {
      entries: profiles.map((profile, index) => toEntry(profile, profile.user, index + 1))
    };
  });

  app.get("/leaderboards", { preHandler: optionalFirebaseAuth }, async (request, reply) => {
    const query = leaderboardQuerySchema.safeParse(request.query);
    if (!query.success) {
      return reply.code(400).send({ error: "Invalid leaderboard query" });
    }

    const current = request.authUser
      ? await getOrCreateUserWithProfile(request.authUser)
      : null;
    const { limit, boardSize } = query.data;
    const runSelect = {
      userId: true,
      score: true,
      level: true,
      linesCleared: true,
      contractsCompleted: true,
      riskSpinsUsed: true,
      user: { select: { username: true } }
    } satisfies Prisma.RunSelect;
    const [publicRuns, currentRuns] = await Promise.all([
      prisma.run.findMany({
        where: {
          boardSize,
          validationStatus: "accepted",
          user: { username: { not: null }, mergedIntoUserId: null }
        },
        select: runSelect
      }),
      current
        ? prisma.run.findMany({
          where: { boardSize, validationStatus: "accepted", userId: current.user.id },
          select: runSelect
        })
        : Promise.resolve([])
    ]);
    const publicStats = aggregateGridRuns(publicRuns.map(toGridRunRecord));
    const currentStats = aggregateGridRuns(currentRuns.map(toGridRunRecord))[0] ?? null;
    const sections = ([
      "bestScore",
      "totalPoints",
      "linesCleared",
      "contractsCompleted",
      "riskSpinsUsed"
    ] as LeaderboardType[]).map((type) => buildGridSection(type, publicStats, currentStats, limit));

    return {
      boardSize,
      me: current ? toGridStats(currentStats, current.user) : null,
      leaderboards: {
        bestScore: sections[0],
        totalPoints: sections[1],
        linesCleared: sections[2],
        contractsCompleted: sections[3],
        riskSpinsUsed: sections[4]
      }
    };
  });
};

function toGridRunRecord(run: {
  userId: string;
  user: { username: string | null };
  score: number;
  level: number;
  linesCleared: number;
  contractsCompleted: number;
  riskSpinsUsed: number;
}): GridRunRecord {
  return { ...run, username: run.user.username };
}

export function aggregateGridRuns(runs: GridRunRecord[]): GridLeaderboardStats[] {
  const byUser = new Map<string, GridLeaderboardStats>();
  for (const run of runs) {
    const stats = byUser.get(run.userId) ?? {
      userId: run.userId,
      username: run.username ?? "You",
      bestScore: 0,
      bestLevel: 1,
      gamesPlayed: 0,
      totalPoints: 0,
      totalLinesCleared: 0,
      totalContractsCompleted: 0,
      totalRiskSpinsUsed: 0
    };
    if (run.score > stats.bestScore || (run.score === stats.bestScore && run.level > stats.bestLevel)) {
      stats.bestScore = run.score;
      stats.bestLevel = run.level;
    }
    stats.gamesPlayed += 1;
    stats.totalPoints += run.score;
    stats.totalLinesCleared += run.linesCleared;
    stats.totalContractsCompleted += run.contractsCompleted;
    stats.totalRiskSpinsUsed += run.riskSpinsUsed;
    byUser.set(run.userId, stats);
  }
  return [...byUser.values()];
}

export function rankGridStats(type: LeaderboardType, stats: GridLeaderboardStats[]): GridLeaderboardStats[] {
  const value = (entry: GridLeaderboardStats) => {
    switch (type) {
      case "bestScore": return entry.bestScore;
      case "totalPoints": return entry.totalPoints;
      case "linesCleared": return entry.totalLinesCleared;
      case "contractsCompleted": return entry.totalContractsCompleted;
      case "riskSpinsUsed": return entry.totalRiskSpinsUsed;
    }
  };
  return [...stats].sort((left, right) =>
    value(right) - value(left) ||
    right.bestScore - left.bestScore ||
    right.bestLevel - left.bestLevel ||
    left.username.localeCompare(right.username) ||
    left.userId.localeCompare(right.userId)
  );
}

function buildGridSection(
  type: LeaderboardType,
  stats: GridLeaderboardStats[],
  currentStats: GridLeaderboardStats | null,
  limit: number
) {
  const ranked = rankGridStats(type, stats);
  const entries = ranked.slice(0, limit).map((entry, index) => gridStatsToEntry(entry, index + 1));
  const currentRank = currentStats
    ? ranked.findIndex((entry) => entry.userId === currentStats.userId) + 1
    : 0;
  return {
    entries,
    me: currentStats && currentRank > 0
      ? { ...gridStatsToEntry(currentStats, currentRank), isInTop: currentRank <= limit }
      : null
  };
}

function gridStatsToEntry(stats: GridLeaderboardStats, rank: number): LeaderboardEntry {
  return {
    rank,
    username: stats.username,
    bestScore: stats.bestScore,
    bestLevel: stats.bestLevel,
    totalPoints: stats.totalPoints,
    totalLinesCleared: stats.totalLinesCleared,
    totalContractsCompleted: stats.totalContractsCompleted,
    totalRiskSpinsUsed: stats.totalRiskSpinsUsed
  };
}

function toGridStats(stats: GridLeaderboardStats | null, user: User) {
  return {
    username: user.username,
    bestScore: stats?.bestScore ?? 0,
    bestLevel: stats?.bestLevel ?? 1,
    totalPoints: stats?.totalPoints ?? 0,
    gamesPlayed: stats?.gamesPlayed ?? 0,
    totalLinesCleared: stats?.totalLinesCleared ?? 0,
    totalContractsCompleted: stats?.totalContractsCompleted ?? 0,
    totalRiskSpinsUsed: stats?.totalRiskSpinsUsed ?? 0
  };
}

async function optionalFirebaseAuth(request: FastifyRequest, reply: { code: (statusCode: number) => { send: (payload: unknown) => unknown } }) {
  const authorization = request.headers.authorization;
  if (!authorization) return;
  if (!authorization.startsWith("Bearer ")) {
    return reply.code(401).send({ error: "Invalid bearer token" });
  }

  try {
    request.authUser = await verifyFirebaseToken(authorization.slice("Bearer ".length).trim());
  } catch {
    return reply.code(401).send({ error: "Invalid Firebase token" });
  }
}

export function leaderboardOrderBy(type: LeaderboardType): Prisma.PlayerProfileOrderByWithRelationInput[] {
  switch (type) {
    case "bestScore":
      return [{ bestScore: "desc" }, { bestLevel: "desc" }, { updatedAt: "asc" }, { id: "asc" }];
    case "totalPoints":
      return [{ totalPoints: "desc" }, { bestScore: "desc" }, { updatedAt: "asc" }, { id: "asc" }];
    case "linesCleared":
      return [{ totalLinesCleared: "desc" }, { bestScore: "desc" }, { updatedAt: "asc" }, { id: "asc" }];
    case "contractsCompleted":
      return [{ totalContractsCompleted: "desc" }, { bestScore: "desc" }, { updatedAt: "asc" }, { id: "asc" }];
    case "riskSpinsUsed":
      return [{ totalRiskSpinsUsed: "desc" }, { bestScore: "desc" }, { updatedAt: "asc" }, { id: "asc" }];
  }
}

export function higherRankWhere(type: LeaderboardType, profile: PlayerProfile): Prisma.PlayerProfileWhereInput {
  const finalTie = { updatedAt: profile.updatedAt, id: { lt: profile.id } };
  switch (type) {
    case "bestScore":
      return { OR: [
        { bestScore: { gt: profile.bestScore } },
        { bestScore: profile.bestScore, bestLevel: { gt: profile.bestLevel } },
        { bestScore: profile.bestScore, bestLevel: profile.bestLevel, updatedAt: { lt: profile.updatedAt } },
        { bestScore: profile.bestScore, bestLevel: profile.bestLevel, ...finalTie }
      ] };
    case "totalPoints":
      return { OR: [
        { totalPoints: { gt: profile.totalPoints } },
        { totalPoints: profile.totalPoints, bestScore: { gt: profile.bestScore } },
        { totalPoints: profile.totalPoints, bestScore: profile.bestScore, updatedAt: { lt: profile.updatedAt } },
        { totalPoints: profile.totalPoints, bestScore: profile.bestScore, ...finalTie }
      ] };
    case "linesCleared":
      return { OR: [
        { totalLinesCleared: { gt: profile.totalLinesCleared } },
        { totalLinesCleared: profile.totalLinesCleared, bestScore: { gt: profile.bestScore } },
        { totalLinesCleared: profile.totalLinesCleared, bestScore: profile.bestScore, updatedAt: { lt: profile.updatedAt } },
        { totalLinesCleared: profile.totalLinesCleared, bestScore: profile.bestScore, ...finalTie }
      ] };
    case "contractsCompleted":
      return { OR: [
        { totalContractsCompleted: { gt: profile.totalContractsCompleted } },
        { totalContractsCompleted: profile.totalContractsCompleted, bestScore: { gt: profile.bestScore } },
        { totalContractsCompleted: profile.totalContractsCompleted, bestScore: profile.bestScore, updatedAt: { lt: profile.updatedAt } },
        { totalContractsCompleted: profile.totalContractsCompleted, bestScore: profile.bestScore, ...finalTie }
      ] };
    case "riskSpinsUsed":
      return { OR: [
        { totalRiskSpinsUsed: { gt: profile.totalRiskSpinsUsed } },
        { totalRiskSpinsUsed: profile.totalRiskSpinsUsed, bestScore: { gt: profile.bestScore } },
        { totalRiskSpinsUsed: profile.totalRiskSpinsUsed, bestScore: profile.bestScore, updatedAt: { lt: profile.updatedAt } },
        { totalRiskSpinsUsed: profile.totalRiskSpinsUsed, bestScore: profile.bestScore, ...finalTie }
      ] };
  }
}

export function isCurrentUserInTop(entries: LeaderboardEntry[], username: string): boolean {
  return entries.some((entry) => entry.username === username);
}

function toEntry(profile: PlayerProfile, user: User, rank: number): LeaderboardEntry {
  return {
    rank,
    username: user.username ?? "You",
    bestScore: profile.bestScore,
    bestLevel: profile.bestLevel,
    totalPoints: profile.totalPoints,
    totalLinesCleared: profile.totalLinesCleared,
    totalContractsCompleted: profile.totalContractsCompleted,
    totalRiskSpinsUsed: profile.totalRiskSpinsUsed
  };
}
