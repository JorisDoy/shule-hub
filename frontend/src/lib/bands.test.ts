import { describe, expect, it } from "vitest";
import type { GradingBand } from "@/lib/api/types";
import { bandForPercent, bandTone, KNEC_8_LEVEL, percentFor, previewBand, roundHalfUp } from "@/lib/bands";

describe("percentFor (rawScore / maxScore × 100, rounded half-up)", () => {
  it("rounds 74.5 up to 75", () => {
    expect(percentFor(149, 200)).toBe(75);
    expect(percentFor(74.5, 100)).toBe(75);
  });

  it("rounds below .5 down", () => {
    expect(percentFor(74.4, 100)).toBe(74);
    expect(percentFor(1, 3)).toBe(33); // 33.33…
    expect(percentFor(2, 3)).toBe(67); // 66.66…
  });

  it("handles binary-awkward fractions exactly", () => {
    // 0.145 * 100 = 14.499999999999998 in floating point; must still round as 14.5 → 15
    expect(percentFor(0.145, 1)).toBe(15);
    expect(percentFor(29, 40)).toBe(73); // 72.5 → 73
    expect(percentFor(17, 40)).toBe(43); // 42.5 → 43
  });

  it("returns whole numbers at the extremes", () => {
    expect(percentFor(0, 50)).toBe(0);
    expect(percentFor(50, 50)).toBe(100);
  });

  it("returns null for missing or invalid input", () => {
    expect(percentFor(null, 50)).toBeNull();
    expect(percentFor(10, null)).toBeNull();
    expect(percentFor(10, 0)).toBeNull();
    expect(percentFor(-1, 50)).toBeNull();
    expect(percentFor(Number.NaN, 50)).toBeNull();
  });
});

describe("roundHalfUp", () => {
  it("rounds .5 away from zero for positives", () => {
    expect(roundHalfUp(74.5)).toBe(75);
    expect(roundHalfUp(74.49)).toBe(74);
    expect(roundHalfUp(0.5)).toBe(1);
  });
});

describe("bandForPercent on the KNEC 8-level scale", () => {
  it("74.5 → 75 → EE2 (7 points)", () => {
    const { percent, band } = previewBand(74.5, 100, KNEC_8_LEVEL);
    expect(percent).toBe(75);
    expect(band?.code).toBe("EE2");
    expect(band?.points).toBe(7);
  });

  it.each([
    [100, "EE1", 8],
    [90, "EE1", 8],
    [89, "EE2", 7],
    [75, "EE2", 7],
    [74, "ME1", 6],
    [58, "ME1", 6],
    [57, "ME2", 5],
    [41, "ME2", 5],
    [40, "AE1", 4],
    [31, "AE1", 4],
    [30, "AE2", 3],
    [21, "AE2", 3],
    [20, "BE1", 2],
    [11, "BE1", 2],
    [10, "BE2", 1],
    [0, "BE2", 1],
  ])("%i%% → %s (%i points)", (percent, code, points) => {
    const band = bandForPercent(percent, KNEC_8_LEVEL);
    expect(band?.code).toBe(code);
    expect(band?.points).toBe(points);
  });

  it("does not depend on the order bands are listed in", () => {
    const shuffled = [...KNEC_8_LEVEL].reverse();
    expect(bandForPercent(75, shuffled)?.code).toBe("EE2");
    expect(bandForPercent(57, shuffled)?.code).toBe("ME2");
  });

  it("picks the highest minPercent ≤ percent even with gaps in the scale", () => {
    const gappy: GradingBand[] = [
      { code: "A", label: "A", minPercent: 80, maxPercent: 100, points: 3, descriptor: null },
      { code: "B", label: "B", minPercent: 50, maxPercent: 70, points: 2, descriptor: null },
    ];
    expect(bandForPercent(75, gappy)?.code).toBe("B");
    expect(bandForPercent(40, gappy)).toBeNull();
  });

  it("returns null without a percent or a scale", () => {
    expect(bandForPercent(null, KNEC_8_LEVEL)).toBeNull();
    expect(bandForPercent(50, null)).toBeNull();
    expect(bandForPercent(50, [])).toBeNull();
  });
});

describe("bandTone", () => {
  it("groups codes by their family", () => {
    expect(bandTone("EE1")).toBe("ee");
    expect(bandTone("ME")).toBe("me");
    expect(bandTone("AE2")).toBe("ae");
    expect(bandTone("BE1")).toBe("be");
    expect(bandTone(null)).toBe("none");
  });
});
