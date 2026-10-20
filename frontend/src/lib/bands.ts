import type { GradingBand } from "@/lib/api/types";

// Band preview. Must match the backend exactly:
//   percent = rawScore / maxScore × 100, rounded half-up to a whole number
//   band    = the band with the highest minPercent that is ≤ percent

function decimalPlaces(n: number): number {
  if (Number.isInteger(n)) return 0;
  const s = n.toString();
  if (s.includes("e-")) return Number(s.split("e-")[1]);
  return s.split(".")[1]?.length ?? 0;
}

/** Rounds half-up (towards +∞ on .5), like BigDecimal HALF_UP for non-negative values. */
export function roundHalfUp(value: number): number {
  if (!Number.isFinite(value)) return NaN;
  const sign = value < 0 ? -1 : 1;
  const abs = Math.abs(value);
  // Nudge by a tiny epsilon so binary representation error (74.49999999999999) still rounds as 74.5 would.
  return sign * Math.floor(abs + 0.5 + 1e-9);
}

/**
 * Whole-number percent for a raw score, computed with integer arithmetic so 149/200 → 74.5 → 75 exactly.
 * Returns null when inputs are missing or invalid (negative, NaN, maxScore ≤ 0).
 */
export function percentFor(rawScore: number | null | undefined, maxScore: number | null | undefined): number | null {
  if (rawScore === null || rawScore === undefined || maxScore === null || maxScore === undefined) return null;
  if (!Number.isFinite(rawScore) || !Number.isFinite(maxScore) || maxScore <= 0 || rawScore < 0) return null;
  const places = Math.min(Math.max(decimalPlaces(rawScore), decimalPlaces(maxScore)), 6);
  const scale = 10 ** places;
  const r = Math.round(rawScore * scale);
  const m = Math.round(maxScore * scale);
  // round half-up of (r * 100 / m) == floor((2 * r * 100 + m) / (2 * m))
  return Math.floor((2 * r * 100 + m) / (2 * m));
}

/** The band with the highest minPercent ≤ percent. Null if no band qualifies or percent is null. */
export function bandForPercent(percent: number | null | undefined, bands: readonly GradingBand[] | null | undefined): GradingBand | null {
  if (percent === null || percent === undefined || !bands || bands.length === 0) return null;
  let best: GradingBand | null = null;
  for (const band of bands) {
    if (band.minPercent <= percent && (best === null || band.minPercent > best.minPercent)) best = band;
  }
  return best;
}

export function previewBand(
  rawScore: number | null | undefined,
  maxScore: number | null | undefined,
  bands: readonly GradingBand[] | null | undefined,
): { percent: number | null; band: GradingBand | null } {
  const percent = percentFor(rawScore, maxScore);
  return { percent, band: bandForPercent(percent, bands) };
}

/** KNEC 8-level scale for Junior School (Grades 7–9). Used for marketing specimens and tests. */
export const KNEC_8_LEVEL: GradingBand[] = [
  { code: "EE1", label: "Exceeding Expectations 1", minPercent: 90, maxPercent: 100, points: 8, descriptor: null },
  { code: "EE2", label: "Exceeding Expectations 2", minPercent: 75, maxPercent: 89, points: 7, descriptor: null },
  { code: "ME1", label: "Meeting Expectations 1", minPercent: 58, maxPercent: 74, points: 6, descriptor: null },
  { code: "ME2", label: "Meeting Expectations 2", minPercent: 41, maxPercent: 57, points: 5, descriptor: null },
  { code: "AE1", label: "Approaching Expectations 1", minPercent: 31, maxPercent: 40, points: 4, descriptor: null },
  { code: "AE2", label: "Approaching Expectations 2", minPercent: 21, maxPercent: 30, points: 3, descriptor: null },
  { code: "BE1", label: "Below Expectations 1", minPercent: 11, maxPercent: 20, points: 2, descriptor: null },
  { code: "BE2", label: "Below Expectations 2", minPercent: 0, maxPercent: 10, points: 1, descriptor: null },
];

/** The 4-level CBE rubric used from PP1 to Grade 6. */
export const CBE_4_LEVEL: { code: string; label: string; points: number }[] = [
  { code: "EE", label: "Exceeding Expectations", points: 4 },
  { code: "ME", label: "Meeting Expectations", points: 3 },
  { code: "AE", label: "Approaching Expectations", points: 2 },
  { code: "BE", label: "Below Expectations", points: 1 },
];

/** Colour family for a band code, by its first letters: EE / ME / AE / BE. */
export function bandTone(code: string | null | undefined): "ee" | "me" | "ae" | "be" | "none" {
  if (!code) return "none";
  const c = code.toUpperCase();
  if (c.startsWith("EE")) return "ee";
  if (c.startsWith("ME")) return "me";
  if (c.startsWith("AE")) return "ae";
  if (c.startsWith("BE")) return "be";
  return "none";
}
