import { createHash, createHmac } from "node:crypto";
import { Prisma, type CommunityMap } from "@prisma/client";
import { z } from "zod";
import { prisma } from "../db";
import type { AuthenticatedUser } from "../firebase";
import { CommunityError, eligibleUser, normalizeMapCode, publicationDto } from "./publications";

export const browseSchema = z.object({ q: z.string().trim().max(80).default(""),
  sort: z.enum(["rated", "downloads", "newest", "popular"]).default("rated"),
  limit: z.coerce.number().int().min(1).max(10).default(10), cursor: z.string().max(1500).optional() }).strict();
const cursorSchema = z.object({ id: z.string().uuid(), at: z.string().datetime(), value: z.number().finite(),
  query: z.string(), snapshot: z.string().datetime() }).strict();
export function ratingRank(sum: number, count: number) { return (35 + sum) / (10 + count); }

/** Keyset pagination with stable unique ties. New publications are excluded after page one.
 * Rating/download edits can move existing items between pages; refresh starts a fresh ordering. */
export async function browseMaps(query: z.infer<typeof browseSchema>, ownerId?: string) {
  const fingerprint = createHash("sha256").update(JSON.stringify([query.q, query.sort, ownerId ?? null])).digest("hex");
  let cursor: z.infer<typeof cursorSchema> | undefined;
  if (query.cursor) {
    try { cursor = cursorSchema.parse(JSON.parse(Buffer.from(query.cursor, "base64url").toString())); }
    catch { throw new CommunityError(400, "Invalid pagination cursor"); }
    if (cursor.query !== fingerprint) throw new CommunityError(400, "Cursor does not match search");
  }
  const snapshot = cursor?.snapshot ?? new Date().toISOString();
  const ranking = query.sort === "rated" ? Prisma.sql`"ratingRank"` :
    query.sort === "downloads" ? Prisma.sql`"downloads"::float8` :
    query.sort === "popular" ? Prisma.sql`("ratingRank" * ln(2.0 + "downloads"))::float8` : Prisma.sql`0::float8`;
  const search = query.q.match(/[\p{L}\p{N}]+/gu)?.slice(0, 12).map(t => `${t}:*`).join(" & ");
  const rows = await prisma.$queryRaw<(CommunityMap & {rankValue: number})[]>(Prisma.sql`
    SELECT "id", "code", "creatorName", "visibility", "published", "hidden", "downloads", "ratingSum", "ratingCount", "document", "createdAt",
    ${ranking} AS "rankValue" FROM "CommunityMap"
    WHERE ${ownerId ? Prisma.sql`"ownerId" = ${ownerId}::uuid` : Prisma.sql`"published" AND NOT "hidden" AND "visibility" = 'public'`}
    AND "createdAt" <= ${new Date(snapshot)}
    ${query.q ? search ? Prisma.sql`AND "searchVector" @@ to_tsquery('simple', ${search})` : Prisma.sql`AND false` : Prisma.empty}
    ${cursor ? Prisma.sql`AND (${ranking}, "createdAt", "id") < (${cursor.value}, ${new Date(cursor.at)}, ${cursor.id}::uuid)` : Prisma.empty}
    ORDER BY ${ranking} DESC, "createdAt" DESC, "id" DESC LIMIT ${query.limit + 1}`);
  const items = rows.slice(0, query.limit);
  const last = items[items.length - 1];
  return { items: items.map(publicationDto), nextCursor: rows.length > query.limit && last ?
    Buffer.from(JSON.stringify({ id: last.id, at: last.createdAt.toISOString(), value: last.rankValue,
      query: fingerprint, snapshot })).toString("base64url") : null };
}

async function lockAvailable(tx: Prisma.TransactionClient, raw: string) {
  const rows = await tx.$queryRaw<{id: string}[]>`SELECT "id" FROM "CommunityMap" WHERE "code" = ${normalizeMapCode(raw)} FOR UPDATE`;
  const map = rows[0] ? await tx.communityMap.findUnique({where: {id: rows[0].id}}) : null;
  if (!map || !map.published || map.hidden) throw new CommunityError(404, "Map is unavailable");
  return map;
}

export async function rateMap(auth: AuthenticatedUser, code: string, stars: number) {
  const user = await eligibleUser(auth);
  return prisma.$transaction(async tx => {
    const map = await lockAvailable(tx, code);
    if (map.ownerId === user.id) throw new CommunityError(403, "You cannot rate your own map");
    const previous = await tx.communityMapRating.findUnique({where: {mapId_userId: {mapId: map.id, userId: user.id}}});
    await tx.communityMapRating.upsert({where: {mapId_userId: {mapId: map.id, userId: user.id}},
      create: {mapId: map.id, userId: user.id, stars}, update: {stars}});
    const ratingSum = map.ratingSum - (previous?.stars ?? 0) + stars;
    const ratingCount = map.ratingCount + (previous ? 0 : 1);
    const updated = await tx.communityMap.update({where: {id: map.id}, data: {ratingSum, ratingCount, ratingRank: ratingRank(ratingSum, ratingCount)}});
    return { ...publicationDto(updated), yourRating: stars };
  });
}

export function downloadActor(ip: string, auth?: AuthenticatedUser) {
  const secret = process.env.COMMUNITY_DOWNLOAD_SECRET;
  if (!secret || secret.length < 32) throw new CommunityError(503, "Community imports are temporarily unavailable");
  // Guests are conservatively deduplicated by IP; no raw address or Firebase UID is stored.
  return createHmac("sha256", secret).update(auth ? `account:${auth.firebaseUid}` : `guest:${ip}`).digest("hex");
}
export async function importMap(code: string, actorHash: string, requestId: string) {
  return prisma.$transaction(async tx => {
    const map = await lockAvailable(tx, code);
    const inserted = await tx.communityMapDownload.createMany({data: [{mapId: map.id, actorHash, requestId}], skipDuplicates: true});
    const updated = inserted.count ? await tx.communityMap.update({where: {id: map.id}, data: {downloads: {increment: 1}}}) : map;
    return publicationDto(updated);
  });
}
export async function reportMap(auth: AuthenticatedUser, code: string, reason: string, notes: string) {
  const user = await eligibleUser(auth);
  return prisma.$transaction(async tx => {
    const map = await lockAvailable(tx, code);
    await tx.communityMapReport.upsert({where: {mapId_userId: {mapId: map.id, userId: user.id}},
      create: {mapId: map.id, userId: user.id, reason, notes}, update: {reason, notes}});
    return {reported: true};
  });
}
