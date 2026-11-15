import { createHash, randomBytes } from "node:crypto";
import type { CommunityMap, Prisma } from "@prisma/client";
import type { AuthenticatedUser } from "../firebase";
import { prisma } from "../db";
import { getOrCreateUserWithProfile } from "../services/usersService";
import { encodeMap, parseMap } from "./mapFormat";

export class CommunityError extends Error {
  constructor(public statusCode: number, message: string) { super(message); }
}
const alphabet = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
export function generateMapCode(): string {
  return "GF-" + [...randomBytes(10)].map(b => alphabet[b & 31]).join("");
}
export function normalizeMapCode(raw: string): string {
  const code = raw.trim().toUpperCase();
  if (!/^GF-[0-9A-HJKMNP-TV-Z]{10}$/.test(code)) throw new CommunityError(400, "Invalid map code (expected GF-XXXXXXXXXX)");
  return code;
}
export async function eligibleUser(auth: AuthenticatedUser) {
  if (auth.isAnonymous) throw new CommunityError(403, "Sign in and complete your profile first");
  const { user } = await getOrCreateUserWithProfile(auth);
  if (!user.username || user.mergedIntoUserId || user.isAnonymous) throw new CommunityError(403, "Complete your profile first");
  return user;
}
export function publicationDto(map: CommunityMap) {
  return { code: map.code, creator: map.creatorName, createdAt: map.createdAt.toISOString(),
    visibility: map.visibility, published: map.published, hidden: map.hidden,
    downloads: map.downloads, ratingAverage: map.ratingCount ? map.ratingSum / map.ratingCount : 0,
    ratingCount: map.ratingCount, document: map.document };
}
export async function publishMap(auth: AuthenticatedUser, document: unknown, visibility: string,
  requestId: string, makeCode = generateMapCode) {
  const user = await eligibleUser(auth);
  const canonical = encodeMap(document);
  const map = parseMap(document);
  const payloadHash = createHash("sha256").update(visibility + ":" + canonical).digest("hex");
  for (let attempt = 0; attempt < 6; attempt++) {
    const existing = await prisma.communityMap.findUnique({ where: { ownerId_requestId: { ownerId: user.id, requestId } } });
    if (existing) {
      if (existing.payloadHash !== payloadHash) throw new CommunityError(409, "Publish request was already used for different content");
      return publicationDto(existing);
    }
    try {
      return publicationDto(await prisma.communityMap.create({ data: {
        code: makeCode(), ownerId: user.id, requestId, payloadHash, name: map.name,
        description: map.description, creatorName: user.username!, visibility,
        document: JSON.parse(canonical) as Prisma.InputJsonValue
      } }));
    } catch (error) {
      if ((error as {code?: string}).code !== "P2002") throw error;
    }
  }
  throw new CommunityError(503, "Could not allocate a map code; please retry");
}
export async function getPublication(raw: string) {
  const code = normalizeMapCode(raw);
  const map = await prisma.communityMap.findFirst({ where: { code, published: true, hidden: false } });
  if (!map) throw new CommunityError(404, "Map is unavailable");
  return publicationDto(map);
}
export async function unpublishMap(auth: AuthenticatedUser, raw: string) {
  const owner = await eligibleUser(auth);
  const code = normalizeMapCode(raw);
  const map = await prisma.communityMap.findFirst({ where: { code, ownerId: owner.id } });
  if (!map) throw new CommunityError(404, "Map is unavailable");
  await prisma.communityMap.updateMany({ where: { id: map.id, published: true },
    data: { published: false, unpublishedAt: new Date() } });
  return { code, published: false };
}
