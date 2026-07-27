import { describe, expect, it } from "vitest";
import { submitRunSchema, summarizeRunStats } from "./runsService";

describe("submitRunSchema", () => {
  it("accepts a reasonable run payload", () => {
    const parsed = submitRunSchema.parse({
      score: 1234,
      level: 7,
      linesCleared: 50,
      contractsCompleted: 4,
      bombsUsed: 3,
      megaBombsUsed: 1,
      riskSpinsUsed: 6,
      durationSeconds: 480,
      appVersion: "1.0.0"
    });

    expect(summarizeRunStats(parsed)).toEqual({
      bestScore: 1234,
      bestLevel: 7,
      gamesPlayed: 1,
      totalPoints: 1234,
      totalLinesCleared: 50,
      totalContractsCompleted: 4,
      totalBombsUsed: 3,
      totalMegaBombsUsed: 1,
      totalRiskSpinsUsed: 6
    });
  });

  it("defaults missing risk spins to zero for older clients", () => {
    const parsed = submitRunSchema.parse({ score: 100, level: 2 });

    expect(parsed.riskSpinsUsed).toBe(0);
    expect(summarizeRunStats(parsed).totalRiskSpinsUsed).toBe(0);
  });

  it("rejects negative and absurdly large run values", () => {
    expect(() => submitRunSchema.parse({ score: -1, level: 1 })).toThrow();
    expect(() => submitRunSchema.parse({ score: 10_000_001, level: 1 })).toThrow();
    expect(() => submitRunSchema.parse({ score: 10, level: 0 })).toThrow();
    expect(() => submitRunSchema.parse({ score: 10, level: 1, riskSpinsUsed: -1 })).toThrow();
  });
});
