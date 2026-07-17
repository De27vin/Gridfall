import crypto from "node:crypto";
import type { PlayerProfile } from "@prisma/client";
import { prisma } from "../db";
import type { AuthenticatedUser } from "../firebase";
import { getOrCreateUserWithProfile } from "./usersService";

export class MergeTokenError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "MergeTokenError";
  }
}

export interface ProfileStats {
  bestScore: number;
  bestLevel: number;
  gamesPlayed: number;
  totalPoints: number;
  totalLinesCleared: number;
  totalContractsCompleted: number;
  totalBombsUsed: number;
  totalMegaBombsUsed: number;
}

const mergeTokenTtlSeconds = 900;

export async function createGuestMergeToken(authUser: AuthenticatedUser) {
  if (!authUser.isAnonymous) {
    throw new MergeTokenError("Guest merge token requires anonymous Firebase auth");
  }

  const { user } = await getOrCreateUserWithProfile(authUser);
  const mergeToken = crypto.randomBytes(32).toString("base64url");
  const tokenHash = hashMergeToken(mergeToken);
  const expiresAt = new Date(Date.now() + mergeTokenTtlSeconds * 1000);

  await prisma.guestMergeToken.create({
    data: {
      tokenHash,
      guestUserId: user.id,
      expiresAt
    }
  });

  return {
    mergeToken,
    expiresInSeconds: mergeTokenTtlSeconds
  };
}

export async function mergeGuestIntoCurrentUser(authUser: AuthenticatedUser, mergeToken: string): Promise<PlayerProfile> {
  if (!mergeToken.trim()) {
    throw new MergeTokenError("Merge token is required");
  }

  const current = await getOrCreateUserWithProfile(authUser);
  const tokenHash = hashMergeToken(mergeToken);

  return prisma.$transaction(async (tx) => {
    const storedToken = await tx.guestMergeToken.findUnique({
      where: { tokenHash },
      include: {
        guestUser: {
          include: {
            profile: true
          }
        }
      }
    });

    if (!storedToken || storedToken.usedAt || storedToken.expiresAt <= new Date()) {
      throw new MergeTokenError("Merge token is invalid or expired");
    }

    if (storedToken.guestUserId === current.user.id) {
      await tx.guestMergeToken.update({
        where: { id: storedToken.id },
        data: { usedAt: new Date() }
      });
      return current.profile;
    }

    if (authUser.isAnonymous) {
      throw new MergeTokenError("Registered Firebase auth is required to merge guest progress");
    }

    const guestProfile = storedToken.guestUser.profile;
    const currentProfile = await tx.playerProfile.upsert({
      where: { userId: current.user.id },
      update: {},
      create: { userId: current.user.id }
    });

    await tx.run.updateMany({
      where: { userId: storedToken.guestUserId },
      data: { userId: current.user.id }
    });

    const mergedStats = mergeProfileStats(currentProfile, guestProfile);
    const updatedProfile = await tx.playerProfile.update({
      where: { userId: current.user.id },
      data: mergedStats
    });

    await tx.guestMergeToken.update({
      where: { id: storedToken.id },
      data: { usedAt: new Date() }
    });

    await tx.user.update({
      where: { id: storedToken.guestUserId },
      data: { mergedIntoUserId: current.user.id }
    });

    return updatedProfile;
  });
}

export function mergeProfileStats(
  registered: ProfileStats,
  guest: ProfileStats | null
): ProfileStats {
  if (!guest) return registered;

  return {
    bestScore: Math.max(registered.bestScore, guest.bestScore),
    bestLevel: Math.max(registered.bestLevel, guest.bestLevel),
    gamesPlayed: registered.gamesPlayed + guest.gamesPlayed,
    totalPoints: registered.totalPoints + guest.totalPoints,
    totalLinesCleared: registered.totalLinesCleared + guest.totalLinesCleared,
    totalContractsCompleted: registered.totalContractsCompleted + guest.totalContractsCompleted,
    totalBombsUsed: registered.totalBombsUsed + guest.totalBombsUsed,
    totalMegaBombsUsed: registered.totalMegaBombsUsed + guest.totalMegaBombsUsed
  };
}

export function hashMergeToken(mergeToken: string): string {
  return crypto.createHash("sha256").update(mergeToken).digest("hex");
}

