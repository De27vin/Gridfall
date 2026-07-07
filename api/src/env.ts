import dotenv from "dotenv";

dotenv.config();

export interface Env {
  nodeEnv: string;
  port: number;
  databaseUrl?: string;
  firebaseProjectId?: string;
  googleApplicationCredentials?: string;
  corsOrigins: string[];
}

function requireProduction(name: string): string | undefined {
  const value = process.env[name];
  if (process.env.NODE_ENV === "production" && !value) {
    throw new Error(`Missing required production environment variable: ${name}`);
  }
  return value;
}

function parsePort(value: string | undefined): number {
  const parsed = Number(value ?? "8080");
  if (!Number.isInteger(parsed) || parsed <= 0 || parsed > 65535) {
    throw new Error("PORT must be a valid TCP port");
  }
  return parsed;
}

function parseCorsOrigins(value: string | undefined): string[] {
  return (value ?? "")
    .split(",")
    .map((origin) => origin.trim())
    .filter(Boolean);
}

export const env: Env = {
  nodeEnv: process.env.NODE_ENV ?? "development",
  port: parsePort(process.env.PORT),
  databaseUrl: requireProduction("DATABASE_URL"),
  firebaseProjectId: requireProduction("FIREBASE_PROJECT_ID"),
  googleApplicationCredentials: requireProduction("GOOGLE_APPLICATION_CREDENTIALS"),
  corsOrigins: parseCorsOrigins(process.env.CORS_ORIGINS)
};

