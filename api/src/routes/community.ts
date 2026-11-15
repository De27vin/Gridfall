import type { FastifyPluginAsync } from "fastify";
import { z } from "zod";
import { prisma } from "../db";
import { requireFirebaseAuth } from "../middleware/requireFirebaseAuth";
import { communityMapSchema } from "../community/mapFormat";
import { CommunityError, eligibleUser, getPublication, publicationDto, publishMap, unpublishMap } from "../community/publications";

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
  app.get("/community/maps/mine", { preHandler: requireFirebaseAuth }, async request => {
    const owner = await eligibleUser(request.authUser!);
    const items = await prisma.communityMap.findMany({ where: { ownerId: owner.id }, orderBy: [{createdAt: "desc"}, {id: "desc"}], take: 100 });
    return { items: items.map(publicationDto) };
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
