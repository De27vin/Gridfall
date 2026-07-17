import { describe, expect, it } from "vitest";
import { hashMergeToken, mergeProfileStats } from "./mergeService";

describe("mergeProfileStats", () => {
  it("uses max best values and adds totals", () => {
    expect(
      mergeProfileStats(
        {
          bestScore: 500,
          bestLevel: 5,
          gamesPlayed: 10,
          totalPoints: 5000,
          totalLinesCleared: 100,
          totalContractsCompleted: 4,
          totalBombsUsed: 3,
          totalMegaBombsUsed: 1
        },
        {
          bestScore: 900,
          bestLevel: 4,
          gamesPlayed: 3,
          totalPoints: 2000,
          totalLinesCleared: 20,
          totalContractsCompleted: 2,
          totalBombsUsed: 5,
          totalMegaBombsUsed: 2
        }
      )
    ).toEqual({
      bestScore: 900,
      bestLevel: 5,
      gamesPlayed: 13,
      totalPoints: 7000,
      totalLinesCleared: 120,
      totalContractsCompleted: 6,
      totalBombsUsed: 8,
      totalMegaBombsUsed: 3
    });
  });
});

describe("hashMergeToken", () => {
  it("hashes tokens deterministically without returning the raw token", () => {
    const first = hashMergeToken("secret-token");
    const second = hashMergeToken("secret-token");

    expect(first).toBe(second);
    expect(first).not.toBe("secret-token");
    expect(first).toHaveLength(64);
  });
});

