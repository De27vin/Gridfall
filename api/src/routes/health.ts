import type { FastifyPluginAsync } from "fastify";
import { prisma } from "../db";

export async function databaseReady(): Promise<boolean> {
  const rows = await prisma.$queryRaw<Record<string, string | null>[]>`
    SELECT to_regclass('"User"')::text AS users,
      to_regclass('"PlayerProfile"')::text AS profiles,
      to_regclass('"Run"')::text AS runs,
      to_regclass('"CommunityMap"')::text AS maps,
      to_regclass('"CommunityMapRating"')::text AS ratings,
      to_regclass('"CommunityMapDownload"')::text AS downloads,
      to_regclass('"CommunityMapReport"')::text AS reports,
      to_regclass('"CommunityModerationEvent"')::text AS audits,
      to_regclass('"CommunityMapContentRestriction"')::text AS restrictions,
      to_regprocedure('community_map_content_fingerprint(jsonb)')::text AS content_policy`;
  return rows.length === 1 && Object.values(rows[0]).every(Boolean);
}

export const healthRoutes: FastifyPluginAsync = async (app) => {
  app.get("/health", async () => ({ ok: true, service: "gridfall-api" }));

  // Coalesce concurrent probes; a slow database must not consume an unbounded
  // number of pool slots. The HTTP deadline does not claim to cancel driver I/O.
  let pending: Promise<boolean> | undefined;
  app.get("/ready", async (_request, reply) => {
    reply.header("Cache-Control", "no-store");
    if (!pending) pending = databaseReady().finally(() => { pending = undefined; });
    let timer: ReturnType<typeof setTimeout> | undefined;
    let ready = false;
    try {
      ready = await Promise.race([pending, new Promise<boolean>((_resolve, reject) => {
        timer = setTimeout(() => reject(new Error("Readiness deadline")), 2_000);
      })]);
    } catch { /* Dependency details and connection strings are not public. */ }
    finally { if (timer) clearTimeout(timer); }
    return reply.code(ready ? 200 : 503).send({ ok: ready, service: "gridfall-api" });
  });
};
