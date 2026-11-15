import { readFileSync } from "node:fs";
import { describe, expect, it } from "vitest";
import { decodeMap, encodeMap } from "./mapFormat";
const fixture = readFileSync("../shared/community-map-v1.json", "utf8").trim();
describe("community map v1", () => {
  it("round trips the cross-language canonical fixture", () => expect(encodeMap(decodeMap(fixture))).toBe(fixture));
  it("rejects malformed, unknown-version, oversized and coerced data", () => {
    for (const raw of ["{", " ".repeat(65537), fixture.replace('"formatVersion":1', '"formatVersion":2'),
      fixture.replace('"rows":4', '"rows":"4"'), fixture.replace('"colorVariant":2', '"colorVariant":9')]) {
      expect(() => decodeMap(raw)).toThrow();
    }
  });
  it("rejects impossible maps and invalid probabilities", () => {
    const d = decodeMap(fixture);
    expect(() => encodeMap({ ...d, blockPool: [{ ...d.blockPool[0], spawnChanceTenthsPercent: 999 }] })).toThrow();
    expect(() => encodeMap({ ...d, blockedCells: Array.from({length: 16}, (_, i) => ({row: i >> 2, col: i % 4})) })).toThrow();
    expect(() => encodeMap({ ...d, blockPool: [d.blockPool[0], d.blockPool[0]] })).toThrow();
  });
});
