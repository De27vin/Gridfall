import type { AuthenticatedUser } from "../firebase";
import { prisma } from "../db";
import { CommunityError, normalizeMapCode } from "./publications";
import { getOrCreateUserWithProfile } from "../services/usersService";

export async function moderateMap(auth: AuthenticatedUser, raw: string, action: "hide" | "remove", reason: string) {
  if (!auth.isAdmin || auth.isAnonymous) throw new CommunityError(403, "Administrator required");
  const {user} = await getOrCreateUserWithProfile(auth);
  const code = normalizeMapCode(raw);
  return prisma.$transaction(async tx => {
    const locked = await tx.$queryRaw<{id: string}[]>`SELECT "id" FROM "CommunityMap" WHERE "code" = ${code} FOR UPDATE`;
    if (!locked[0]) throw new CommunityError(404, "Map is unavailable");
    const previous = await tx.communityMap.findUniqueOrThrow({where: {id: locked[0].id}});
    const next = await tx.communityMap.update({where: {id: previous.id}, data: {hidden: true, moderationReason: reason,
      ...(action === "remove" ? {published: false, unpublishedAt: new Date()} : {})}});
    const state = (m: typeof previous) => ({hidden: m.hidden, published: m.published,
      reason: m.moderationReason, unpublishedAt: m.unpublishedAt?.toISOString() ?? null});
    await tx.communityModerationEvent.create({data: {mapId: previous.id, moderatorId: user.id, action, reason,
      previousState: state(previous), resultingState: state(next)}});
    return {code, hidden: true};
  });
}
