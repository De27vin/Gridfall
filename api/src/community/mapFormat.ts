import { z } from "zod";

export const MAX_MAP_BYTES = 65_536;
const text = (max: number, min = 0) => z.string().min(min).max(max).refine(s => s === s.trim() &&
  !/[\u0000-\u001f\u007f]/.test(s) && [...s].every(c => c.length === 2 ||
    c.charCodeAt(0) < 0xd800 || c.charCodeAt(0) > 0xdfff), "Invalid text");
const cell = z.object({ row: z.number().int().min(0).max(9), col: z.number().int().min(0).max(9) }).strict();
const cells = (max: number, min = 0) => z.array(cell).min(min).max(max).refine(a =>
  new Set(a.map(c => `${c.row},${c.col}`)).size === a.length, "Duplicate cells");
const block = z.object({
  id: z.string().regex(/^[A-Za-z0-9_-]{1,64}$/), name: text(64, 1),
  cells: cells(16, 1), colorVariant: z.number().int().min(1).max(4),
  spawnChanceTenthsPercent: z.number().int().min(0).max(1000)
}).strict();
export const communityMapSchema = z.object({
  formatVersion: z.literal(1), name: text(24, 1), description: text(500),
  rows: z.number().int().min(3).max(10), columns: z.number().int().min(3).max(10),
  blockedCells: cells(100), blockPool: z.array(block).min(1).max(64)
}).strict().superRefine((map, ctx) => {
  const fail = (message: string) => ctx.addIssue({ code: "custom", message });
  if (Buffer.byteLength(JSON.stringify(map), "utf8") > MAX_MAP_BYTES) fail("Map too large");
  if (map.blockedCells.some(c => c.row >= map.rows || c.col >= map.columns)) fail("Cell outside map");
  if (map.blockedCells.length >= map.rows * map.columns) fail("No playable cells");
  if (new Set(map.blockPool.map(b => b.id)).size !== map.blockPool.length) fail("Duplicate block IDs");
  if (map.blockPool.reduce((sum, b) => sum + b.spawnChanceTenthsPercent, 0) !== 1000) fail("Probabilities must total 1000");
  const blocked = new Set(map.blockedCells.map(c => `${c.row},${c.col}`));
  for (const b of map.blockPool) {
    if (b.cells.some(c => c.row > 3 || c.col > 3) || Math.min(...b.cells.map(c => c.row)) !== 0 ||
        Math.min(...b.cells.map(c => c.col)) !== 0) fail("Shapes must be normalized in a 4x4 field");
    const height = Math.max(...b.cells.map(c => c.row)) + 1;
    const width = Math.max(...b.cells.map(c => c.col)) + 1;
    if (height > map.rows || width > map.columns) fail("Block exceeds map dimensions");
    if (b.spawnChanceTenthsPercent > 0) {
      let fits = false;
      for (let r = 0; r <= map.rows - height; r++) for (let c = 0; c <= map.columns - width; c++) {
        if (b.cells.every(p => !blocked.has(`${r + p.row},${c + p.col}`))) fits = true;
      }
      if (!fits) fail("A spawnable block cannot fit this layout");
    }
  }
});
export type CommunityMapDocument = z.infer<typeof communityMapSchema>;

export function parseMap(input: unknown): CommunityMapDocument {
  const map = communityMapSchema.parse(input);
  const ordered = (a: {row: number; col: number}[]) => [...a].sort((a, b) => a.row - b.row || a.col - b.col);
  return { ...map, blockedCells: ordered(map.blockedCells),
    blockPool: map.blockPool.map(b => ({ ...b, cells: ordered(b.cells) })) };
}
export function canonicalJson(value: unknown): string {
  if (Array.isArray(value)) return `[${value.map(canonicalJson).join(",")}]`;
  if (typeof value === "object" && value !== null) return `{${Object.keys(value).sort().map(k =>
    `${JSON.stringify(k)}:${canonicalJson((value as Record<string, unknown>)[k])}`).join(",")}}`;
  return JSON.stringify(value);
}
export function encodeMap(input: unknown): string { return canonicalJson(parseMap(input)); }
export function decodeMap(raw: string): CommunityMapDocument {
  if (Buffer.byteLength(raw, "utf8") > MAX_MAP_BYTES) throw new Error("Map too large");
  return parseMap(JSON.parse(raw));
}
