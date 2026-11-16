import { readFileSync } from "node:fs";
import { randomUUID } from "node:crypto";
import { afterAll, beforeAll, describe, expect, it, vi } from "vitest";
import { buildServer } from "../server";
import { prisma } from "../db";
import { publishMap } from "./publications";
import { getOrCreateUserWithProfile } from "../services/usersService";
import { ratingRank } from "./marketplace";

vi.mock("../firebase", () => ({verifyFirebaseToken: vi.fn(async (token: string) => {
  if (!["market-owner", "market-rater", "market-admin", "market-guest", "market-guest2"].includes(token)) throw Error("Invalid");
  return {firebaseUid: token, isAnonymous: token.startsWith("market-guest"), isAdmin: token === "market-admin"};
})}));
const testUrl = process.env.COMMUNITY_TEST_DATABASE_URL;
if (testUrl && (!/^postgresql:\/\/[^/]+@127\.0\.0\.1:55439\/gridfall_community_test$/.test(testUrl) || process.env.DATABASE_URL !== testUrl))
  throw Error("Refusing non-disposable database");
describe("rating confidence", () => {
  it("does not promote a lone five-star above many good votes", () => expect(ratingRank(90, 20)).toBeGreaterThan(ratingRank(5, 1)));
});
describe.skipIf(!testUrl)("marketplace on disposable PostgreSQL", () => {
  const app = buildServer();
  const prefix = `Market${randomUUID().replaceAll("-", "").slice(0, 8)}`;
  const headers = (who = "market-rater") => ({authorization: `Bearer ${who}`});
  const document = JSON.parse(readFileSync("../shared/community-map-v1.json", "utf8"));
  let codes: string[] = []; let unlisted: string;
  const publish = async (name: string, visibility = "public") => (await publishMap({firebaseUid: "market-owner", isAnonymous: false},
    {...document, name}, visibility, randomUUID())).code;
  beforeAll(async () => {
    process.env.COMMUNITY_DOWNLOAD_SECRET = "disposable-test-only-secret-32-characters";
    for (const name of ["market-owner", "market-rater", "market-admin"]) {
      const {user} = await getOrCreateUserWithProfile({firebaseUid: name, isAnonymous: false});
      await prisma.user.update({where: {id: user.id}, data: {username: name, usernameNormalized: name}});
    }
    codes = [await publish(prefix + " A"), await publish(prefix + " B"), await publish(prefix + " C")];
    unlisted = await publish(prefix + " Secret", "unlisted");
  });
  afterAll(async () => { await app.close(); });
  it("allows guest discovery with indexed name/creator search and excludes unlisted", async () => {
    const response = await app.inject({url: `/community/maps?q=${prefix}&sort=newest`});
    expect(response.statusCode).toBe(200);
    expect(response.json().items.map((m: {code: string}) => m.code).sort()).toEqual([...codes].sort());
    expect(response.body).not.toMatch(/firebaseUid|ownerId|email|payloadHash|searchVector/);
    expect((await app.inject({url: "/community/maps?q=market-owner"})).json().items.length).toBeGreaterThan(0);
    expect((await app.inject({url: `/community/maps/${unlisted}`})).statusCode).toBe(200);
  });
  it("paginates deterministically without ties/duplicates and binds cursors to search", async () => {
    const found: string[] = []; let cursor: string | null = null;
    do {
      const response = await app.inject({url: `/community/maps?q=${prefix}&sort=newest&limit=1${cursor ? `&cursor=${cursor}` : ""}`});
      expect(response.statusCode).toBe(200);
      found.push(...response.json().items.map((m: {code: string}) => m.code)); cursor = response.json().nextCursor;
      if (cursor) expect((await app.inject({url: `/community/maps?sort=rated&cursor=${cursor}`})).statusCode).toBe(400);
    } while (cursor);
    expect(found).toEqual([...codes].reverse());
    expect((await app.inject({url: "/community/maps?cursor=bad"})).statusCode).toBe(400);
    expect((await app.inject({url: "/community/maps?limit=100000"})).statusCode).toBe(400);
  });
  it("isolates owner management and includes private/unpublished publications", async () => {
    const own = await app.inject({url: `/community/maps/mine?q=${prefix}`, headers: headers("market-owner")});
    expect(own.json().items).toHaveLength(4);
    expect((await app.inject({url: `/community/maps/mine?q=${prefix}`, headers: headers()})).json().items).toHaveLength(0);
    expect((await app.inject({url: "/community/maps/mine"})).statusCode).toBe(401);
  });
  it("requires eligible users, rejects self-rating and invalid stars", async () => {
    const url = `/community/maps/${codes[0]}/rating`;
    expect((await app.inject({method: "PUT", url, payload: {stars: 5}})).statusCode).toBe(401);
    for (const who of ["market-owner", "market-guest"]) expect((await app.inject({method: "PUT", url, headers: headers(who), payload: {stars: 5}})).statusCode).toBe(403);
    for (const stars of [0, 6, 1.5, "5"]) expect((await app.inject({method: "PUT", url, headers: headers(), payload: {stars}})).statusCode).toBe(400);
  });
  it("updates exactly one active rating under concurrent retries and sorts by confidence", async () => {
    const url = `/community/maps/${codes[0]}/rating`;
    await Promise.all(Array.from({length: 4}, () => app.inject({method: "PUT", url, headers: headers(), payload: {stars: 5}})));
    const update = await app.inject({method: "PUT", url, headers: headers(), payload: {stars: 3}});
    expect(update.json()).toMatchObject({ratingCount: 1, ratingAverage: 3, yourRating: 3});
    await app.inject({method: "PUT", url: `/community/maps/${codes[1]}/rating`, headers: headers(), payload: {stars: 5}});
    expect((await app.inject({url: `/community/maps?q=${prefix}&sort=rated`})).json().items[0].code).toBe(codes[1]);
  });
  it("counts intentional imports only and deduplicates nonce and actor retries concurrently", async () => {
    const code = codes[0];
    expect((await app.inject({url: `/community/maps/${code}`})).json().downloads).toBe(0);
    const url = `/community/maps/${code}/import`; const requestId = randomUUID();
    const results = await Promise.all(Array.from({length: 5}, () => app.inject({method: "POST", url, payload: {requestId}})));
    expect(results.every(r => r.statusCode === 200 && r.json().downloads === 1)).toBe(true);
    expect((await app.inject({method: "POST", url, payload: {requestId: randomUUID()}})).json().downloads).toBe(1);
    expect((await app.inject({method: "POST", url, remoteAddress: "127.0.0.5", payload: {requestId}})).json().downloads).toBe(1);
    for (const guest of ["market-guest", "market-guest2"]) {
      expect((await app.inject({method: "POST", url, headers: headers(guest), payload: {requestId: randomUUID()}})).json().downloads).toBe(1);
    }
    expect((await app.inject({method: "POST", url, headers: headers(), payload: {requestId: randomUUID()}})).json().downloads).toBe(2);
    expect((await app.inject({url: `/community/maps?q=${prefix}&sort=downloads`})).json().items[0].code).toBe(code);
    expect((await app.inject({url: `/community/maps?q=${prefix}&sort=popular`})).statusCode).toBe(200);
  });
  it("supports reports and admin-only moderation; hidden maps disappear from lookup/import/rating", async () => {
    const code = codes[2];
    const report = await app.inject({method: "POST", url: `/community/maps/${code}/report`, headers: headers(), payload: {reason: "inappropriate", notes: "Test report"}});
    expect(report.json().reported).toBe(true);
    expect((await app.inject({url: "/community/moderation/reports", headers: headers()})).statusCode).toBe(403);
    const reports = await app.inject({url: "/community/moderation/reports", headers: headers("market-admin")});
    expect(reports.json().items.some((r: {map: {code: string}}) => r.map.code === code)).toBe(true);
    const moderation = {method: "PUT" as const, url: `/community/moderation/${code}`, payload: {action: "hide", reason: "Test moderation"}};
    expect((await app.inject({...moderation, headers: headers()})).statusCode).toBe(403);
    expect((await app.inject({...moderation, headers: headers("market-admin")})).statusCode).toBe(200);
    expect((await app.inject({url: `/community/maps/${code}`})).statusCode).toBe(404);
    expect((await app.inject({method: "POST", url: `/community/maps/${code}/import`, payload: {requestId: randomUUID()}})).statusCode).toBe(404);
    expect((await app.inject({method: "PUT", url: `/community/maps/${code}/rating`, headers: headers(), payload: {stars: 5}})).statusCode).toBe(404);
    expect((await app.inject({url: `/community/maps?q=${prefix}`})).json().items).toHaveLength(2);
  });
  it("rejects new imports after unpublishing while retaining immutable documents", async () => {
    expect((await app.inject({method: "DELETE", url: `/community/maps/${unlisted}`, headers: headers("market-owner")})).statusCode).toBe(200);
    expect((await app.inject({method: "POST", url: `/community/maps/${unlisted}/import`, payload: {requestId: randomUUID()}})).statusCode).toBe(404);
    expect((await prisma.communityMap.findUnique({where: {code: unlisted}}))?.document).toBeTruthy();
  });
  it("rejects oversized and malformed imports and rate limits abuse", async () => {
    const url = `/community/maps/${codes[0]}/import`;
    expect((await app.inject({method: "POST", url, payload: {requestId: "bad"}})).statusCode).toBe(400);
    expect((await app.inject({method: "POST", url, payload: {padding: "x".repeat(2048)}})).statusCode).toBe(413);
    for (let i = 0; i < 31; i++) await app.inject({method: "POST", url, remoteAddress: "127.0.0.66", payload: {requestId: randomUUID()}});
    expect((await app.inject({method: "POST", url, remoteAddress: "127.0.0.66", payload: {requestId: randomUUID()}})).statusCode).toBe(429);
  });
  it("rejects custom/community leaderboard submissions before writing any run", async () => {
    const before = await prisma.run.count();
    for (const marker of [{runKind: "community"}, {isCustomMap: true}, {communityMapCode: codes[0]}]) {
      expect((await app.inject({method: "POST", url: "/runs", headers: headers(),
        payload: {score: 99000, level: 20, boardSize: 8, ...marker}})).statusCode).toBe(400);
    }
    expect(await prisma.run.count()).toBe(before);
  });
  it("records concurrent moderation atomically and prevents history mutation", async () => {
    const code = await publish(prefix + " Audit");
    const url = `/community/moderation/${code}`;
    const payload = {action: "hide", reason: "audit test"};
    expect((await app.inject({method: "PUT", url, headers: headers(), payload})).statusCode).toBe(403);
    const results = await Promise.all(["hide", "remove"].map(action => app.inject({method: "PUT", url,
      headers: headers("market-admin"), payload: {action, reason: action}})));
    expect(results.map(r => r.statusCode)).toEqual([200, 200]);
    const map = await prisma.communityMap.findUniqueOrThrow({where: {code}});
    const events = await prisma.communityModerationEvent.findMany({where: {mapId: map.id}});
    expect(events).toHaveLength(2);
    expect(events.every(e => e.moderatorId && e.mapId === map.id)).toBe(true);
    const transitions = events.map(e => e.previousState as {hidden: boolean});
    expect(transitions.filter(e => !e.hidden)).toHaveLength(1);
    expect(map).toMatchObject({hidden: true, published: false});
    expect((await app.inject({url: url + "/history", headers: headers()})).statusCode).toBe(403);
    expect((await app.inject({url: url + "/history", headers: headers("market-admin")})).json().items).toHaveLength(2);
    await expect(prisma.communityModerationEvent.update({where: {id: events[0].id}, data: {reason: "tamper"}})).rejects.toThrow();
    await expect(prisma.communityModerationEvent.delete({where: {id: events[0].id}})).rejects.toThrow();
  });

  it("does not accept arbitrary forwarded headers as rate-limit identities", async () => {
    for (let i = 0; i < 31; i++) await app.inject({url: "/community/maps/GF-0000000000", remoteAddress: "127.0.0.177",
      headers: {"x-forwarded-for": `192.0.2.${i}`, "cf-connecting-ip": `2001:db8::${i}`}});
    expect((await app.inject({url: "/community/maps/GF-0000000000", remoteAddress: "127.0.0.177",
      headers: {"x-forwarded-for": "198.51.100.9"}})).statusCode).toBe(429);
  });

  it("rolls back moderation when durable audit insertion fails", async () => {
    const code = await publish(prefix + " Rollback");
    await prisma.$executeRawUnsafe("CREATE FUNCTION fail_test_audit_insert() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'simulated audit storage failure'; END; $$");
    await prisma.$executeRawUnsafe('CREATE TRIGGER fail_test_audit BEFORE INSERT ON "CommunityModerationEvent" FOR EACH ROW EXECUTE FUNCTION fail_test_audit_insert()');
    try {
      const response = await app.inject({method: "PUT", url: `/community/moderation/${code}`, headers: headers("market-admin"),
        payload: {action: "remove", reason: "must roll back"}});
      expect(response.statusCode).toBe(500);
      const map = await prisma.communityMap.findUniqueOrThrow({where: {code}});
      expect(map).toMatchObject({hidden: false, published: true, moderationReason: null});
      expect(await prisma.communityModerationEvent.count({where: {mapId: map.id}})).toBe(0);
    } finally {
      await prisma.$executeRawUnsafe('DROP TRIGGER fail_test_audit ON "CommunityModerationEvent"');
      await prisma.$executeRawUnsafe('DROP FUNCTION fail_test_audit_insert()');
    }
  });

});
