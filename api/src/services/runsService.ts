import { z } from "zod";
import { prisma } from "../db";

export const submitRunSchema = z.object({
  score: z.number().int().min(0).max(10_000_000),
  level: z.number().int().min(1).max(10_000),
  linesCleared: z.number().int().min(0).max(100_000).default(0),
  contractsCompleted: z.number().int().min(0).max(10_000).default(0),
  bombsUsed: z.number().int().min(0).max(10_000).default(0),
  megaBombsUsed: z.number().int().min(0).max(10_000).default(0),
  durationSeconds: z.number().int().min(0).max(604_800).nullable().optional(),
  appVersion: z.string().trim().max(64).nullable().optional()
});

export type SubmitRunInput = z.infer<typeof submitRunSchema>;

export async function saveRunAndUpdateProfile(userId: string, input: SubmitRunInput) {
  return prisma.$transaction(async (tx) => {
    const currentProfile = await tx.playerProfile.upsert({
      where: { userId },
      update: {},
      create: { userId }
    });

    const run = await tx.run.create({
      data: {
        userId,
        score: input.score,
        level: input.level,
        linesCleared: input.linesCleared,
        contractsCompleted: input.contractsCompleted,
        bombsUsed: input.bombsUsed,
        megaBombsUsed: input.megaBombsUsed,
        durationSeconds: input.durationSeconds,
        appVersion: input.appVersion
      }
    });

    const profile = await tx.playerProfile.update({
      where: { userId },
      data: {
        bestScore: Math.max(currentProfile.bestScore, input.score),
        bestLevel: Math.max(currentProfile.bestLevel, input.level),
        gamesPlayed: { increment: 1 },
        totalLinesCleared: { increment: input.linesCleared },
        totalContractsCompleted: { increment: input.contractsCompleted },
        totalBombsUsed: { increment: input.bombsUsed },
        totalMegaBombsUsed: { increment: input.megaBombsUsed }
      }
    });

    return { run, profile };
  });
}

export function summarizeRunStats(input: SubmitRunInput) {
  return {
    bestScore: input.score,
    bestLevel: input.level,
    gamesPlayed: 1,
    totalLinesCleared: input.linesCleared,
    totalContractsCompleted: input.contractsCompleted,
    totalBombsUsed: input.bombsUsed,
    totalMegaBombsUsed: input.megaBombsUsed
  };
}

