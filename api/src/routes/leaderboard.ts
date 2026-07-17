import type { FastifyPluginAsync, FastifyRequest } from "fastify";
import type { PlayerProfile, Prisma, User } from "@prisma/client";
import { z } from "zod";
import { verifyFirebaseToken } from "../firebase";
import "../middleware/requireFirebaseAuth";
import { prisma } from "../db";
import { getOrCreateUserWithProfile } from "../services/usersService";

const leaderboardQuerySchema = z.object({
  limit: z.coerce.number().int().min(1).max(100).default(10)
});

const publicProfileWhere: Prisma.PlayerProfileWhereInput = {
  user: {
    username: { not: null },
    mergedIntoUserId: null
  }
};

type LeaderboardType = "bestScore" | "totalPoints" | "linesCleared" | "contractsCompleted";

interface LeaderboardEntry {
  rank: number;
  username: string;
  bestScore: number;
  bestLevel: number;
  totalPoints: number;
  totalLinesCleared: number;
  totalContractsCompleted: number;
  isInTop?: boolean;
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
    const limit = query.data.limit;

    const sections = await Promise.all([
      buildSection("bestScore", limit, current?.profile ?? null, current?.user ?? null),
      buildSection("totalPoints", limit, current?.profile ?? null, current?.user ?? null),
      buildSection("linesCleared", limit, current?.profile ?? null, current?.user ?? null),
      buildSection("contractsCompleted", limit, current?.profile ?? null, current?.user ?? null)
    ]);

    return {
      me: current ? toStats(current.profile, current.user) : null,
      leaderboards: {
        bestScore: sections[0],
        totalPoints: sections[1],
        linesCleared: sections[2],
        contractsCompleted: sections[3]
      }
    };
  });
};

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

async function buildSection(
  type: LeaderboardType,
  limit: number,
  currentProfile: PlayerProfile | null,
  currentUser: User | null
) {
  const profiles = await prisma.playerProfile.findMany({
    where: publicProfileWhere,
    include: { user: true },
    orderBy: leaderboardOrderBy(type),
    take: limit
  });
  const entries = profiles.map((profile, index) => toEntry(profile, profile.user, index + 1));

  if (!currentProfile || !currentUser?.username || currentUser.mergedIntoUserId) {
    return { entries, me: null };
  }

  const higherCount = await prisma.playerProfile.count({
    where: {
      AND: [publicProfileWhere, higherRankWhere(type, currentProfile)]
    }
  });
  const isInTop = entries.some((entry) => entry.username === currentUser.username);

  return {
    entries,
    me: {
      ...toEntry(currentProfile, currentUser, higherCount + 1),
      isInTop
    }
  };
}

function leaderboardOrderBy(type: LeaderboardType): Prisma.PlayerProfileOrderByWithRelationInput[] {
  switch (type) {
    case "bestScore":
      return [{ bestScore: "desc" }, { bestLevel: "desc" }, { updatedAt: "asc" }, { id: "asc" }];
    case "totalPoints":
      return [{ totalPoints: "desc" }, { bestScore: "desc" }, { updatedAt: "asc" }, { id: "asc" }];
    case "linesCleared":
      return [{ totalLinesCleared: "desc" }, { bestScore: "desc" }, { updatedAt: "asc" }, { id: "asc" }];
    case "contractsCompleted":
      return [{ totalContractsCompleted: "desc" }, { bestScore: "desc" }, { updatedAt: "asc" }, { id: "asc" }];
  }
}

function higherRankWhere(type: LeaderboardType, profile: PlayerProfile): Prisma.PlayerProfileWhereInput {
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
  }
}

function toEntry(profile: PlayerProfile, user: User, rank: number): LeaderboardEntry {
  return {
    rank,
    username: user.username ?? "You",
    bestScore: profile.bestScore,
    bestLevel: profile.bestLevel,
    totalPoints: profile.totalPoints,
    totalLinesCleared: profile.totalLinesCleared,
    totalContractsCompleted: profile.totalContractsCompleted
  };
}

function toStats(profile: PlayerProfile, user: User) {
  return {
    username: user.username,
    bestScore: profile.bestScore,
    bestLevel: profile.bestLevel,
    totalPoints: profile.totalPoints,
    gamesPlayed: profile.gamesPlayed,
    totalLinesCleared: profile.totalLinesCleared,
    totalContractsCompleted: profile.totalContractsCompleted
  };
}