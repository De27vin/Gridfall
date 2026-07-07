import { describe, expect, it } from "vitest";
import { normalizeUsername, UsernameValidationError } from "./username";

describe("normalizeUsername", () => {
  it("trims and collapses spaces", () => {
    expect(normalizeUsername("  Player   One  ")).toEqual({
      username: "Player One",
      usernameNormalized: "player one"
    });
  });

  it("keeps allowed symbols", () => {
    expect(normalizeUsername("Player_123-4")).toEqual({
      username: "Player_123-4",
      usernameNormalized: "player_123-4"
    });
  });

  it("rejects short, long, and unsupported usernames", () => {
    expect(() => normalizeUsername("ab")).toThrow(UsernameValidationError);
    expect(() => normalizeUsername("a".repeat(17))).toThrow(UsernameValidationError);
    expect(() => normalizeUsername("bad!name")).toThrow(UsernameValidationError);
  });
});

