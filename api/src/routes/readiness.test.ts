import { afterEach, describe, expect, it, vi } from "vitest";
import { prisma } from "../db";
import { buildServer } from "../server";

afterEach(() => vi.restoreAllMocks());
const schema = [{users: "User", profiles: "PlayerProfile", runs: "Run", maps: "CommunityMap", ratings: "CommunityMapRating", downloads: "CommunityMapDownload", reports: "CommunityMapReport", audits: "CommunityModerationEvent"}];

describe("database readiness separate from liveness", () => {
  it("returns 503 on outage, keeps liveness healthy and recovers without restart", async () => {
    const query = vi.spyOn(prisma, "$queryRaw").mockRejectedValueOnce(new Error("private connection failure"))
      .mockResolvedValueOnce(schema);
    const app = buildServer();
    try {
      const failed = await app.inject({url: "/ready"});
      expect(failed.statusCode).toBe(503);
      expect(failed.body).not.toContain("private");
      expect(failed.headers["cache-control"]).toBe("no-store");
      expect((await app.inject({url: "/health"})).statusCode).toBe(200);
      expect((await app.inject({url: "/ready"})).statusCode).toBe(200);
      expect(query).toHaveBeenCalledTimes(2);
    } finally { await app.close(); }
  });
  it("does not report ready when the community migration is absent", async () => {
    vi.spyOn(prisma, "$queryRaw").mockResolvedValueOnce([{...schema[0], audits: null}]);
    const app = buildServer();
    try { expect((await app.inject({url: "/ready"})).statusCode).toBe(503); }
    finally { await app.close(); }
  });
  it("bounds slow probes and coalesces concurrent callers", async () => {
    let complete!: (value: typeof schema) => void;
    const query = vi.spyOn(prisma, "$queryRaw").mockImplementation(() => new Promise(resolve => {complete = resolve;}) as never);
    const app = buildServer();
    try {
      const first = app.inject({url: "/ready"});
      const second = app.inject({url: "/ready"});
      await vi.waitFor(() => expect(query).toHaveBeenCalledTimes(1));
      expect((await first).statusCode).toBe(503);
      expect((await second).statusCode).toBe(503);
      complete(schema);
    } finally { await app.close(); }
  });
});
