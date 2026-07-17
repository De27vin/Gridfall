import type { PlayerProfile, User } from "@prisma/client";
import { prisma } from "../db";
import type { AuthenticatedUser } from "../firebase";
import { normalizeUsername } from "../utils/username";

export class UsernameConflictError extends Error {
  constructor() {
    super("Username is already taken");
    this.name = "UsernameConflictError";
  }
}

export interface UserWithProfile {
  user: User;
  profile: PlayerProfile;
}

export async function getOrCreateUserWithProfile(authUser: AuthenticatedUser): Promise<UserWithProfile> {
  return prisma.$transaction(async (tx) => {
    const user = await tx.user.upsert({
      where: { firebaseUid: authUser.firebaseUid },
      update: {
        email: authUser.email,
        isAnonymous: authUser.isAnonymous,
        lastSeenAt: new Date()
      },
      create: {
        firebaseUid: authUser.firebaseUid,
        email: authUser.email,
        isAnonymous: authUser.isAnonymous,
        lastSeenAt: new Date()
      }
    });

    const profile = await tx.playerProfile.upsert({
      where: { userId: user.id },
      update: {},
      create: { userId: user.id }
    });

    return { user, profile };
  });
}

export async function setUsernameForAuthUser(authUser: AuthenticatedUser, rawUsername: string): Promise<UserWithProfile> {
  const normalized = normalizeUsername(rawUsername);
  const current = await getOrCreateUserWithProfile(authUser);
  const existing = await prisma.user.findUnique({
    where: { usernameNormalized: normalized.usernameNormalized }
  });

  if (existing && existing.id !== current.user.id) {
    throw new UsernameConflictError();
  }

  try {
    const user = await prisma.user.update({
      where: { id: current.user.id },
      data: {
        username: normalized.username,
        usernameNormalized: normalized.usernameNormalized
      }
    });

    return { user, profile: current.profile };
  } catch (error) {
    if (isUniqueConstraintError(error)) {
      throw new UsernameConflictError();
    }
    throw error;
  }
}

export function serializeUserWithProfile({ user, profile }: UserWithProfile) {
  return {
    id: user.id,
    firebaseUid: user.firebaseUid,
    email: user.email,
    isAnonymous: user.isAnonymous,
    username: user.username,
    profile: {
      bestScore: profile.bestScore,
      bestLevel: profile.bestLevel,
      gamesPlayed: profile.gamesPlayed,
      totalPoints: profile.totalPoints,
      totalLinesCleared: profile.totalLinesCleared,
      totalContractsCompleted: profile.totalContractsCompleted,
      totalBombsUsed: profile.totalBombsUsed,
      totalMegaBombsUsed: profile.totalMegaBombsUsed
    }
  };
}

function isUniqueConstraintError(error: unknown): boolean {
  return typeof error === "object" &&
    error !== null &&
    "code" in error &&
    (error as { code?: string }).code === "P2002";
}

