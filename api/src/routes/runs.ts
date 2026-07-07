import type { FastifyPluginAsync, FastifyRequest } from "fastify";
import type { AuthenticatedUser } from "../firebase";
import { requireFirebaseAuth } from "../middleware/requireFirebaseAuth";
import { saveRunAndUpdateProfile, submitRunSchema } from "../services/runsService";
import { getOrCreateUserWithProfile } from "../services/usersService";

export const runsRoutes: FastifyPluginAsync = async (app) => {
  app.post("/runs", { preHandler: requireFirebaseAuth }, async (request, reply) => {
    const body = submitRunSchema.safeParse(request.body);
    if (!body.success) {
      return reply.code(400).send({
        error: "Invalid run payload",
        details: body.error.flatten()
      });
    }

    const { user } = await getOrCreateUserWithProfile(authUserFromRequest(request));
    return saveRunAndUpdateProfile(user.id, body.data);
  });
};

function authUserFromRequest(request: FastifyRequest): AuthenticatedUser {
  if (!request.authUser) {
    throw new Error("Authenticated user missing from request");
  }
  return request.authUser;
}

