import Fastify from "fastify";
import cors from "@fastify/cors";
import helmet from "@fastify/helmet";
import rateLimit from "@fastify/rate-limit";
import contentRoutes from "./routes/content.js";
import adminRoutes from "./routes/admin.js";
import billingRoutes from "./routes/billing.js";
import analyticsRoutes from "./routes/analytics.js";
import { pool } from "./db/pool.js";

export function buildServer(options = {}) {
  const fastify = Fastify({
    logger: process.env.NODE_ENV === "production"
      ? { level: "info", redact: ["req.headers.authorization", "req.headers['x-admin-key']"] }
      : { level: "debug", transport: undefined },
    bodyLimit: 8 * 1024 * 1024,
    ...options,
  });

  fastify.register(helmet, { contentSecurityPolicy: false });
  fastify.register(cors, {
    origin: (process.env.CORS_ORIGINS ?? "").split(",").filter(Boolean),
    methods: ["GET", "POST"],
  });
  fastify.register(rateLimit, { max: 120, timeWindow: "1 minute" });

  fastify.get("/health", async () => ({ ok: true, at: new Date().toISOString() }));
  fastify.get("/ready", async (_request, reply) => {
    try {
      await pool.query("SELECT 1");
      return { ready: true };
    } catch {
      return reply.code(503).send({ ready: false });
    }
  });

  fastify.register(contentRoutes);
  fastify.register(billingRoutes);
  fastify.register(analyticsRoutes);
  fastify.register(adminRoutes);

  return fastify;
}

const isMain = process.argv[1]?.endsWith("index.js");
if (isMain) {
  const server = buildServer();
  const port = Number(process.env.PORT || 8080);
  server.listen({ port, host: "0.0.0.0" }).catch((error) => {
    server.log.error(error);
    process.exit(1);
  });

  for (const signal of ["SIGINT", "SIGTERM"]) {
    process.on(signal, async () => {
      await server.close();
      await pool.end();
      process.exit(0);
    });
  }
}
