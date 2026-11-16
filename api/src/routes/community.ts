import type { FastifyPluginAsync } from "fastify";
import { z } from "zod";
import { prisma } from "../db";
import { requireFirebaseAuth } from "../middleware/requireFirebaseAuth";
import { moderateMap } from "../community/moderation";
import { communityMapSchema } from "../community/mapFormat";
import { browseMaps, browseSchema, downloadActor, importMap, rateMap, reportMap } from "../community/marketplace";
import { CommunityError, eligibleUser, getPublication, normalizeMapCode, publishMap, unpublishMap } from "../community/publications";

const publishBody = z.object({ document: communityMapSchema, visibility: z.enum(["public", "unlisted"]),
  requestId: z.string().uuid() }).strict();
export const communityRoutes: FastifyPluginAsync = async app => {
  app.setErrorHandler((error, _request, reply) => {
    if (error instanceof CommunityError) return reply.code(error.statusCode).send({ error: error.message });
    const status = typeof error === "object" && error !== null && "statusCode" in error &&
      typeof error.statusCode === "number" ? error.statusCode : 500;
    if (status >= 400 && status < 500) {
      return reply.code(status).send({ error: "Request rejected" });
    }
    app.log.error(error);
    return reply.code(500).send({ error: "Community maps are temporarily unavailable" });
  });
  app.post("/community/maps", { bodyLimit: 70_000, preHandler: requireFirebaseAuth,
    config: { rateLimit: { max: 10, timeWindow: "1 hour" } } }, async (request, reply) => {
    const parsed = publishBody.safeParse(request.body);
    if (!parsed.success) return reply.code(400).send({ error: "Invalid map publication" });
    return reply.code(201).send(await publishMap(request.authUser!, parsed.data.document, parsed.data.visibility, parsed.data.requestId));
  });
  app.get("/community/maps", async (request, reply) => {
    const query = browseSchema.safeParse(request.query);
    if (!query.success) return reply.code(400).send({error: "Invalid search"});
    reply.header("Cache-Control", "no-store");
    return browseMaps(query.data);
  });
  app.get("/community/maps/mine", { preHandler: requireFirebaseAuth }, async (request, reply) => {
    const query = browseSchema.safeParse(request.query);
    if (!query.success) return reply.code(400).send({error: "Invalid search"});
    const owner = await eligibleUser(request.authUser!);
    reply.header("Cache-Control", "no-store");
    return browseMaps(query.data, owner.id);
  });
  app.post<{Params: {code: string}}>("/community/maps/:code/import", {
    bodyLimit: 1024, config: {rateLimit: {max: 30, timeWindow: "1 hour"}},
    preHandler: async (request, reply) => { if (request.headers.authorization) await requireFirebaseAuth(request, reply); }
  }, async (request, reply) => {
    const body = z.object({requestId: z.string().uuid()}).strict().safeParse(request.body);
    if (!body.success) return reply.code(400).send({error: "Invalid import request"});
    return importMap(request.params.code, downloadActor(request.ip, request.authUser), body.data.requestId);
  });
  app.put<{Params: {code: string}}>("/community/maps/:code/rating", {preHandler: requireFirebaseAuth,
    bodyLimit: 1024, config: {rateLimit: {max: 30, timeWindow: "1 hour"}}}, async (request, reply) => {
    const body = z.object({stars: z.number().int().min(1).max(5)}).strict().safeParse(request.body);
    if (!body.success) return reply.code(400).send({error: "Stars must be 1–5"});
    return rateMap(request.authUser!, request.params.code, body.data.stars);
  });
  app.post<{Params: {code: string}}>("/community/maps/:code/report", {preHandler: requireFirebaseAuth,
    bodyLimit: 2048, config: {rateLimit: {max: 5, timeWindow: "1 hour"}}}, async (request, reply) => {
    const body = z.object({reason: z.enum(["inappropriate", "spam", "other"]), notes: z.string().trim().max(500).default("")}).strict().safeParse(request.body);
    if (!body.success) return reply.code(400).send({error: "Invalid report"});
    return reportMap(request.authUser!, request.params.code, body.data.reason, body.data.notes);
  });
  app.get("/community/moderation/reports", {preHandler: requireFirebaseAuth}, async (request, reply) => {
    if (!request.authUser?.isAdmin) return reply.code(403).send({error: "Administrator required"});
    const query = z.object({page: z.coerce.number().int().min(0).max(10000).default(0)}).strict().safeParse(request.query);
    if (!query.success) return reply.code(400).send({error: "Invalid page"});
    return {items: await prisma.communityMapReport.findMany({take: 50, skip: query.data.page * 50,
      orderBy: [{createdAt: "desc"}, {mapId: "desc"}, {userId: "desc"}],
      select: {reason: true, notes: true, createdAt: true, map: {select: {code: true, name: true, hidden: true}}}})};
  });
  app.put<{Params: {code: string}}>("/community/moderation/:code", {preHandler: requireFirebaseAuth, bodyLimit: 2048}, async (request, reply) => {
    if (!request.authUser?.isAdmin) return reply.code(403).send({error: "Administrator required"});
    const body = z.object({action: z.enum(["hide", "remove"]), reason: z.string().trim().min(1).max(500)}).strict().safeParse(request.body);
    if (!body.success) return reply.code(400).send({error: "Invalid moderation action"});
    return moderateMap(request.authUser!, request.params.code, body.data.action, body.data.reason);
  });
  app.get<{Params: {code: string}}>("/community/moderation/:code/history", {preHandler: requireFirebaseAuth}, async (request, reply) => {
    if (!request.authUser?.isAdmin || request.authUser.isAnonymous) return reply.code(403).send({error: "Administrator required"});
    const code = normalizeMapCode(request.params.code);
    return {items: await prisma.communityModerationEvent.findMany({where: {map: {code}}, take: 100,
      orderBy: [{createdAt: "desc"}, {id: "desc"}]})};
  });
  app.get<{Params: {code: string}}>("/community/maps/:code", {
    config: { rateLimit: { max: 30, timeWindow: "1 minute" } }
  }, async (request, reply) => {
    reply.header("Cache-Control", "no-store");
    return getPublication(request.params.code);
  });
  app.delete<{Params: {code: string}}>("/community/maps/:code", { preHandler: requireFirebaseAuth },
    async request => unpublishMap(request.authUser!, request.params.code));
};
