import { afterEach, describe, expect, it } from "vitest";
import { downloadActor } from "./marketplace";

describe("download actor classification", () => {
  const previous = process.env.COMMUNITY_DOWNLOAD_SECRET;
  afterEach(() => {
    if (previous === undefined) delete process.env.COMMUNITY_DOWNLOAD_SECRET;
    else process.env.COMMUNITY_DOWNLOAD_SECRET = previous;
  });
  it("deduplicates rotating anonymous identities with unauthenticated guests", () => {
    process.env.COMMUNITY_DOWNLOAD_SECRET = "test-only-stable-secret-at-least-32-characters";
    const guest = downloadActor("127.0.0.9");
    for (let i = 0; i < 100; i++) {
      expect(downloadActor("127.0.0.9", {firebaseUid: `anonymous-${i}`, isAnonymous: true})).toBe(guest);
    }
    expect(downloadActor("127.0.0.10")).not.toBe(guest);
    expect(downloadActor("127.0.0.9", {firebaseUid: "registered", isAnonymous: false})).not.toBe(guest);
  });
  it("fails closed without a sufficiently long secret", () => {
    delete process.env.COMMUNITY_DOWNLOAD_SECRET;
    expect(() => downloadActor("127.0.0.9")).toThrow();
    process.env.COMMUNITY_DOWNLOAD_SECRET = "short";
    expect(() => downloadActor("127.0.0.9")).toThrow();
  });
});
