import type { FastifyReply, FastifyRequest } from "fastify";
import type { AuthenticatedUser } from "../firebase";
import { verifyFirebaseToken } from "../firebase";

declare module "fastify" {
  interface FastifyRequest {
    authUser?: AuthenticatedUser;
  }
}

export async function requireFirebaseAuth(
  request: FastifyRequest,
  reply: FastifyReply
): Promise<void> {
  const authorization = request.headers.authorization;
  if (!authorization?.startsWith("Bearer ")) {
    await reply.code(401).send({ error: "Missing bearer token" });
    return;
  }

  const token = authorization.slice("Bearer ".length).trim();
  if (!token) {
    await reply.code(401).send({ error: "Missing bearer token" });
    return;
  }

  try {
    request.authUser = await verifyFirebaseToken(token);
  } catch {
    await reply.code(401).send({ error: "Invalid Firebase token" });
  }
}

