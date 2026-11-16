import { readFileSync } from "node:fs";
import { randomUUID } from "node:crypto";
import { afterAll, beforeAll, describe, expect, it, vi } from "vitest";
import { buildServer } from "../server";
import { prisma } from "../db";
import { generateMapCode, publishMap } from "./publications";
import { getOrCreateUserWithProfile } from "../services/usersService";

vi.mock("../firebase", () => ({ verifyFirebaseToken: vi.fn(async (token: string) => {
  if (!["community-owner", "community-other", "community-unnamed", "community-guest"].includes(token)) throw Error("Invalid");
  return { firebaseUid: token, isAnonymous: token === "community-guest", email: "private@example.invalid" };
}) }));
const testUrl = process.env.COMMUNITY_TEST_DATABASE_URL;
if (testUrl && (!/^postgresql:\/\/[^/]+@127\.0\.0\.1:55439\/gridfall_community_test$/.test(testUrl) ||
  process.env.DATABASE_URL !== testUrl)) throw Error("Refusing non-disposable test database");
describe.skipIf(!testUrl)("community publishing on disposable PostgreSQL", () => {
  const app = buildServer();
  const document = JSON.parse(readFileSync("../shared/community-map-v1.json", "utf8"));
  let code: string;
  const headers = (who: string) => ({ authorization: `Bearer ${who}` });
  const publish = (visibility = "public", token = "community-owner", requestId = randomUUID()) => app.inject({
    method: "POST", url: "/community/maps", headers: headers(token), payload: {document, visibility, requestId}
  });
  beforeAll(async () => {
    for (const name of ["community-owner", "community-other"]) {
      const {user} = await getOrCreateUserWithProfile({firebaseUid: name, isAnonymous: false});
      await prisma.user.update({where: {id: user.id}, data: {username: name, usernameNormalized: name}});
    }
  });
  afterAll(async () => { await app.close(); });
  it("requires authentication and eligible profiles", async () => {
    const unauth = await app.inject({method: "POST", url: "/community/maps", payload: {}});
    expect(unauth.statusCode).toBe(401);
    expect((await publish("public", "community-guest")).statusCode).toBe(403);
    expect((await publish("public", "community-unnamed")).statusCode).toBe(403);
  });
  it("publishes immutable codes with private identity excluded and idempotent retries", async () => {
    const requestId = randomUUID();
    const first = await publish("public", "community-owner", requestId);
    expect(first.statusCode).toBe(201);
    code = first.json().code;
    expect(code).toMatch(/^GF-[0-9A-HJKMNP-TV-Z]{10}$/);
    expect(first.body).not.toContain("firebaseUid");
    expect(first.body).not.toContain("private@example.invalid");
    expect((await publish("public", "community-owner", requestId)).json().code).toBe(code);
    expect((await publish("unlisted", "community-owner", requestId)).statusCode).toBe(409);
    expect((await publish()).json().code).not.toBe(code);
  });
  it("retrieves codes case-insensitively including unlisted maps", async () => {
    const hiddenCode = (await publish("unlisted")).json().code;
    const response = await app.inject({url: `/community/maps/${hiddenCode.toLowerCase()}`});
    expect(response.statusCode).toBe(200);
    expect(response.json().document).toEqual(document);
    expect((await app.inject({url: "/community/maps/not-a-code"})).statusCode).toBe(400);
  });
  it("enforces owner isolation and makes unpublishing effective immediately", async () => {
    expect((await app.inject({method: "DELETE", url: `/community/maps/${code}`, headers: headers("community-other")})).statusCode).toBe(404);
    expect((await app.inject({method: "DELETE", url: `/community/maps/${code}`, headers: headers("community-owner")})).statusCode).toBe(200);
    expect((await app.inject({url: `/community/maps/${code}`})).statusCode).toBe(404);
    expect((await app.inject({method: "PATCH", url: `/community/maps/${code}`, headers: headers("community-owner"), payload: {document}})).statusCode).toBe(404);
  });
  it("retries genuine sharing-code collisions", async () => {
    const auth = {firebaseUid: "community-owner", isAnonymous: false};
    const existing = await publishMap(auth, document, "public", randomUUID());
    let attempt = 0;
    const next = await publishMap(auth, document, "public", randomUUID(), () => attempt++ === 0 ? existing.code : generateMapCode());
    expect(next.code).not.toBe(existing.code);
    expect(attempt).toBe(2);
  });
  it("rejects malformed/oversized maps and limits code enumeration", async () => {
    expect((await app.inject({method: "POST", url: "/community/maps", headers: headers("community-owner"), payload: { document: {}, visibility: "public", requestId: randomUUID() }})).statusCode).toBe(400);
    expect((await app.inject({method: "POST", url: "/community/maps", headers: headers("community-owner"), payload: {padding: "x".repeat(71_000)}})).statusCode).toBe(413);
    for (let i = 0; i < 31; i++) await app.inject({url: "/community/maps/GF-0000000000", remoteAddress: "127.0.0.99"});
    expect((await app.inject({url: "/community/maps/GF-0000000000", remoteAddress: "127.0.0.99"})).statusCode).toBe(429);
  });
  it("preserves publication identity through concurrent retries and API recreation", async () => {
    const auth = {firebaseUid: "community-owner", isAnonymous: false};
    const requestId = randomUUID();
    const replies = await Promise.all(Array.from({length: 6}, () => publishMap(auth, document, "unlisted", requestId)));
    expect(new Set(replies.map(r => r.code)).size).toBe(1);
    await prisma.$disconnect();
    const restarted = buildServer();
    try {
      const response = await restarted.inject({method: "POST", url: "/community/maps", headers: headers("community-owner"),
        payload: {document, visibility: "unlisted", requestId}});
      expect(response.statusCode).toBe(201);
      expect(response.json().code).toBe(replies[0].code);
    } finally { await restarted.close(); }
  });

});
