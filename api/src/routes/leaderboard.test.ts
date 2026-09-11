import { describe, expect, it } from "vitest";
import {
  aggregateGridRuns,
  higherRankWhere,
  isCurrentUserInTop,
  leaderboardOrderBy,
  publicProfileWhere,
  rankGridStats
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

describe("grid-specific leaderboard ranking", () => {
  const runs = [
    { userId: "rush", username: "RushPlayer", score: 700, level: 5, linesCleared: 8, contractsCompleted: 1, riskSpinsUsed: 2 },
    { userId: "rush", username: "RushPlayer", score: 500, level: 8, linesCleared: 4, contractsCompleted: 2, riskSpinsUsed: 1 },
    { userId: "classic", username: "ClassicPlayer", score: 900, level: 6, linesCleared: 3, contractsCompleted: 0, riskSpinsUsed: 5 }
  ];

  it("aggregates a user's runs without mixing best score and best level", () => {
    const rush = aggregateGridRuns(runs).find((entry) => entry.userId === "rush");

    expect(rush).toMatchObject({
      bestScore: 700,
      bestLevel: 5,
      gamesPlayed: 2,
      totalPoints: 1200,
      totalLinesCleared: 12,
      totalContractsCompleted: 3,
      totalRiskSpinsUsed: 3
    });
  });

  it("ranks independently by the selected statistic", () => {
    const stats = aggregateGridRuns(runs);

    expect(rankGridStats("bestScore", stats)[0].username).toBe("ClassicPlayer");
    expect(rankGridStats("totalPoints", stats)[0].username).toBe("RushPlayer");
  });
});
