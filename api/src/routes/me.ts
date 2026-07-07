import type { FastifyPluginAsync, FastifyRequest } from "fastify";
import { z } from "zod";
import type { AuthenticatedUser } from "../firebase";
import { requireFirebaseAuth } from "../middleware/requireFirebaseAuth";
import {
  getOrCreateUserWithProfile,
  serializeUserWithProfile,
  setUsernameForAuthUser,
  UsernameConflictError
} from "../services/usersService";
import { UsernameValidationError } from "../utils/username";

const setUsernameBodySchema = z.object({
  username: z.string()
});

export const meRoutes: FastifyPluginAsync = async (app) => {
  app.get("/me", { preHandler: requireFirebaseAuth }, async (request) => {
    const userWithProfile = await getOrCreateUserWithProfile(authUserFromRequest(request));
    return serializeUserWithProfile(userWithProfile);
  });

  app.post("/me/username", { preHandler: requireFirebaseAuth }, async (request, reply) => {
    const body = setUsernameBodySchema.safeParse(request.body);
    if (!body.success) {
      return reply.code(400).send({ error: "username is required" });
    }

    try {
      const userWithProfile = await setUsernameForAuthUser(
        authUserFromRequest(request),
        body.data.username
      );
      return serializeUserWithProfile(userWithProfile);
    } catch (error) {
      if (error instanceof UsernameValidationError) {
        return reply.code(400).send({ error: error.message });
      }
      if (error instanceof UsernameConflictError) {
        return reply.code(409).send({ error: error.message });
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

