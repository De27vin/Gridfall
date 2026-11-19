import type { AuthenticatedUser } from "../firebase";
import { prisma } from "../db";
import { CommunityError, normalizeMapCode } from "./publications";
import { getOrCreateUserWithProfile } from "../services/usersService";

export async function moderateMap(auth: AuthenticatedUser, raw: string, action: "hide" | "remove", reason: string) {
  if (!auth.isAdmin || auth.isAnonymous) throw new CommunityError(403, "Administrator required");
  const {user} = await getOrCreateUserWithProfile(auth);
  const code = normalizeMapCode(raw);
  return prisma.$transaction(async tx => {
    const target = await tx.communityMap.findUnique({where: {code}});
    if (!target) throw new CommunityError(404, "Map is unavailable");
    // Same owner-first lock order as publishing. A restriction also excludes
    // existing identical codes from discovery/import without an unbounded rewrite.
    await tx.$queryRaw`SELECT "id" FROM "User" WHERE "id" = ${target.ownerId}::uuid FOR UPDATE`;
    await tx.$queryRaw`SELECT "id" FROM "CommunityMap" WHERE "id" = ${target.id}::uuid FOR UPDATE`;
    const previous = await tx.communityMap.findUniqueOrThrow({where: {id: target.id}});
    await tx.communityMapContentRestriction.upsert({where: {ownerId_fingerprint: {ownerId: target.ownerId, fingerprint: target.contentFingerprint}},
      create: {ownerId: target.ownerId, fingerprint: target.contentFingerprint, sourceMapId: target.id}, update: {}});
    const next = await tx.communityMap.update({where: {id: target.id}, data: {hidden: true, moderationReason: reason,
      ...(action === "remove" ? {published: false, unpublishedAt: new Date()} : {})}});
    const state = (m: typeof previous) => ({hidden: m.hidden, published: m.published,
      reason: m.moderationReason, unpublishedAt: m.unpublishedAt?.toISOString() ?? null});
    await tx.communityModerationEvent.create({data: {mapId: previous.id, moderatorId: user.id, action, reason,
      previousState: state(previous), resultingState: state(next)}});
    return {code, hidden: true};
  });
}
