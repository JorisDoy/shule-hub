import { describe, expect, it } from "vitest";
import { formatKenyanPhone, normalizeKenyanPhone } from "@/lib/phone";

describe("normalizeKenyanPhone", () => {
  it.each([
    ["0712345678", "254712345678"],
    ["0110123456", "254110123456"],
    ["+254712345678", "254712345678"],
    ["+254110123456", "254110123456"],
    ["254712345678", "254712345678"],
    ["254110123456", "254110123456"],
    ["712345678", "254712345678"],
    ["0712 345 678", "254712345678"],
    ["+254 712-345-678", "254712345678"],
    ["(0712) 345.678", "254712345678"],
    ["  0722000111  ", "254722000111"],
  ])("normalises %s to %s", (input, expected) => {
    expect(normalizeKenyanPhone(input)).toBe(expected);
  });

  it("always returns 12 digits starting 2547 or 2541", () => {
    for (const input of ["0712345678", "0110123456", "+254799999999", "254100000000"]) {
      const out = normalizeKenyanPhone(input)!;
      expect(out).toMatch(/^254[17]\d{8}$/);
      expect(out).toHaveLength(12);
    }
  });

  it.each([
    ["", "empty"],
    ["0812345678", "08 is not a mobile prefix"],
    ["0212345678", "02 is a landline"],
    ["071234567", "too short"],
    ["07123456789", "too long"],
    ["+255712345678", "Tanzanian country code"],
    ["+0712345678", "plus without country code"],
    ["2540712345678", "254 with a leading 0 kept"],
    ["07123abc78", "letters"],
    ["0712345678x", "trailing letter"],
  ])("rejects %s (%s)", (input) => {
    expect(normalizeKenyanPhone(input)).toBeNull();
  });

  it("handles null and undefined", () => {
    expect(normalizeKenyanPhone(null)).toBeNull();
    expect(normalizeKenyanPhone(undefined)).toBeNull();
  });
});

describe("formatKenyanPhone", () => {
  it("formats the API's 12-digit form for display", () => {
    expect(formatKenyanPhone("254712345678")).toBe("0712 345 678");
    expect(formatKenyanPhone("254110123456")).toBe("0110 123 456");
  });

  it("falls back to the raw value or a dash", () => {
    expect(formatKenyanPhone("not a phone")).toBe("not a phone");
    expect(formatKenyanPhone(null)).toBe("—");
  });
});
