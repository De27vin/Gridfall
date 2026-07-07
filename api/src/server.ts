import cors from "@fastify/cors";
import helmet from "@fastify/helmet";
import rateLimit from "@fastify/rate-limit";
import Fastify from "fastify";
import { prisma } from "./db";
import { env } from "./env";
import { healthRoutes } from "./routes/health";
import { leaderboardRoutes } from "./routes/leaderboard";
import { meRoutes } from "./routes/me";
import { mergeRoutes } from "./routes/merge";
import { runsRoutes } from "./routes/runs";

export function buildServer() {
  const app = Fastify({
    logger: env.nodeEnv !== "test"
  });

  app.register(helmet);
  app.register(cors, {
    origin: env.corsOrigins.length > 0 ? env.corsOrigins : false
  });
  app.register(rateLimit, {
    max: 300,
    timeWindow: "1 minute"
  });

  app.register(healthRoutes);
  app.register(meRoutes);
  app.register(runsRoutes);
  app.register(leaderboardRoutes);
  app.register(mergeRoutes);

  app.setErrorHandler((error, _request, reply) => {
    app.log.error(error);
    reply.code(500).send({ error: "Internal server error" });
  });

  app.addHook("onClose", async () => {
    await prisma.$disconnect();
  });

  return app;
}

async function main() {
  const app = buildServer();
  await app.listen({
    port: env.port,
    host: "0.0.0.0"
  });
}

if (require.main === module) {
  main().catch((error) => {
    console.error(error);
    process.exit(1);
  });
}

