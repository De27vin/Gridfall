import { describe, expect, it } from "vitest";
import { serializeUserWithProfile } from "./usersService";

describe("serializeUserWithProfile", () => {
  it("includes total risk spins in the profile response", () => {
    const result = serializeUserWithProfile({
      user: {
        id: "user-id",
        firebaseUid: "firebase-id",
        isAnonymous: false,
        username: "player"
      } as never,
      profile: {
        bestScore: 500,
        bestLevel: 5,
        gamesPlayed: 3,
        totalPoints: 1_000,
        totalLinesCleared: 20,
        totalContractsCompleted: 2,
        totalBombsUsed: 1,
        totalMegaBombsUsed: 1,
        totalRiskSpinsUsed: 7
      } as never
    });

    expect(result.profile.totalRiskSpinsUsed).toBe(7);
  });
});
