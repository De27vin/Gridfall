import type { FastifyPluginAsync, FastifyRequest } from "fastify";
import { z } from "zod";
import type { AuthenticatedUser } from "../firebase";
import { requireFirebaseAuth } from "../middleware/requireFirebaseAuth";
import {
  createGuestMergeToken,
  MergeTokenError,
  mergeGuestIntoCurrentUser
} from "../services/mergeService";

const mergeGuestBodySchema = z.object({
  mergeToken: z.string().min(1)
});

export const mergeRoutes: FastifyPluginAsync = async (app) => {
  app.post("/auth/guest-merge-token", { preHandler: requireFirebaseAuth }, async (request, reply) => {
    try {
      return await createGuestMergeToken(authUserFromRequest(request));
    } catch (error) {
      if (error instanceof MergeTokenError) {
        return reply.code(400).send({ error: error.message });
      }
      throw error;
    }
  });

  app.post("/auth/merge-guest", { preHandler: requireFirebaseAuth }, async (request, reply) => {
    const body = mergeGuestBodySchema.safeParse(request.body);
    if (!body.success) {
      return reply.code(400).send({ error: "mergeToken is required" });
    }

    try {
      const profile = await mergeGuestIntoCurrentUser(
        authUserFromRequest(request),
        body.data.mergeToken
      );
      return { profile };
    } catch (error) {
      if (error instanceof MergeTokenError) {
        return reply.code(400).send({ error: error.message });
      }
      throw error;
    }
  });
};

function authUserFromRequest(request: FastifyRequest): AuthenticatedUser {
  if (!request.authUser) {
    throw new Error("Authenticated user missing from request");
  }
  return request.authUser;
}

