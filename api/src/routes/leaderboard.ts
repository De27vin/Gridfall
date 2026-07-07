import type { FastifyPluginAsync } from "fastify";
import { z } from "zod";
import { prisma } from "../db";

const leaderboardQuerySchema = z.object({
  limit: z.coerce.number().int().min(1).max(100).default(50)
});

export const leaderboardRoutes: FastifyPluginAsync = async (app) => {
  app.get("/leaderboard", async (request, reply) => {
    const query = leaderboardQuerySchema.safeParse(request.query);
    if (!query.success) {
      return reply.code(400).send({ error: "Invalid leaderboard query" });
    }

    const profiles = await prisma.playerProfile.findMany({
      where: {
        user: {
          username: {
            not: null
          }
        }
      },
      include: {
        user: true
      },
      orderBy: [
        { bestScore: "desc" },
        { bestLevel: "desc" },
        { updatedAt: "asc" }
      ],
      take: query.data.limit
    });

    return {
      entries: profiles.map((profile, index) => ({
        rank: index + 1,
        username: profile.user.username,
        bestScore: profile.bestScore,
        bestLevel: profile.bestLevel
      }))
    };
  });
};

