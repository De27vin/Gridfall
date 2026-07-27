import { describe, expect, it } from "vitest";
import {
  higherRankWhere,
  isCurrentUserInTop,
  leaderboardOrderBy,
  publicProfileWhere
} from "./leaderboard";

describe("risk spin leaderboard configuration", () => {
  it("sorts risk spins descending, then best score descending", () => {
    expect(leaderboardOrderBy("riskSpinsUsed")).toEqual([
      { totalRiskSpinsUsed: "desc" },
      { bestScore: "desc" },
      { updatedAt: "asc" },
      { id: "asc" }
    ]);
  });

  it("keeps unnamed and merged users out of public leaderboard queries", () => {
    expect(publicProfileWhere).toEqual({
      user: {
        username: { not: null },
        mergedIntoUserId: null
      }
    });
  });

  it("marks the current user outside the top entries as not in top", () => {
    expect(isCurrentUserInTop([
      { rank: 1, username: "player-one", bestScore: 100, bestLevel: 1, totalPoints: 100, totalLinesCleared: 1, totalContractsCompleted: 0, totalRiskSpinsUsed: 5 }
    ], "current-player")).toBe(false);
  });

  it("counts higher risk spin totals and best-score ties for the current user rank", () => {
    const profile = {
      id: "profile-id",
      totalRiskSpinsUsed: 8,
      bestScore: 500,
      updatedAt: new Date("2026-07-18T00:00:00.000Z")
    } as never;

    expect(higherRankWhere("riskSpinsUsed", profile)).toMatchObject({
      OR: expect.arrayContaining([
        { totalRiskSpinsUsed: { gt: 8 } },
        { totalRiskSpinsUsed: 8, bestScore: { gt: 500 } }
      ])
    });
  });
});
