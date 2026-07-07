import { describe, expect, it } from "vitest";
import { buildServer } from "../server";

describe("GET /health", () => {
  it("returns service health", async () => {
    const app = buildServer();
    const response = await app.inject({
      method: "GET",
      url: "/health"
    });

    expect(response.statusCode).toBe(200);
    expect(response.json()).toEqual({
      ok: true,
      service: "gridfall-api"
    });

    await app.close();
  });
});

