import { describe, expect, it } from "vitest";
import { addDaysIso, balanceTone, describeBalance, formatDate, formatDateTime, formatKes, parseMoneyInput, todayIso } from "@/lib/format";

// Intl output can use a narrow no-break space between "Ksh" and the digits depending on ICU data.
const norm = (s: string) => s.replace(/[  ]/g, " ");

describe("formatKes", () => {
  it("formats Kenya shillings with Intl en-KE / KES", () => {
    const expected = new Intl.NumberFormat("en-KE", { style: "currency", currency: "KES" }).format(12500);
    expect(formatKes(12500)).toBe(expected);
    expect(norm(formatKes(12500))).toBe("Ksh 12,500.00");
  });

  it("keeps cents and groups thousands", () => {
    expect(norm(formatKes(1234567.5))).toBe("Ksh 1,234,567.50");
    expect(norm(formatKes(0))).toBe("Ksh 0.00");
  });

  it("formats negatives (credits)", () => {
    expect(norm(formatKes(-1500))).toMatch(/^-\s?Ksh 1,500\.00$/);
  });

  it("drops cents with whole", () => {
    expect(norm(formatKes(24000, { whole: true }))).toBe("Ksh 24,000");
  });

  it("shows a dash for missing values", () => {
    expect(formatKes(null)).toBe("—");
    expect(formatKes(undefined)).toBe("—");
    expect(formatKes(Number.NaN)).toBe("—");
  });
});

describe("formatDate", () => {
  it('formats ISO dates like "5 Jan 2027"', () => {
    expect(formatDate("2027-01-05")).toBe("5 Jan 2027");
    expect(formatDate("2027-09-05")).toBe("5 Sep 2027");
    expect(formatDate("2026-12-31")).toBe("31 Dec 2026");
  });

  it("uses the Nairobi calendar day for instants", () => {
    expect(formatDate("2027-01-05T07:30:00Z")).toBe("5 Jan 2027");
    // 22:30 UTC on 4 Jan is 01:30 on 5 Jan in Nairobi (UTC+3)
    expect(formatDate("2027-01-04T22:30:00Z")).toBe("5 Jan 2027");
  });

  it("formats instants with time in EAT", () => {
    expect(formatDateTime("2027-01-05T07:30:00Z")).toBe("5 Jan 2027, 10:30");
  });

  it("handles empty and unparseable input", () => {
    expect(formatDate(null)).toBe("—");
    expect(formatDate("")).toBe("—");
    expect(formatDate("soon")).toBe("soon");
  });
});

describe("date helpers", () => {
  it("todayIso uses Nairobi time", () => {
    expect(todayIso(new Date("2027-01-04T21:30:00Z"))).toBe("2027-01-05");
    expect(todayIso(new Date("2027-01-04T20:59:00Z"))).toBe("2027-01-04");
  });

  it("addDaysIso crosses month and year boundaries", () => {
    expect(addDaysIso("2026-12-25", 14)).toBe("2027-01-08");
    expect(addDaysIso("2028-02-28", 1)).toBe("2028-02-29");
  });
});

describe("balances", () => {
  it("positive owes, negative is credit", () => {
    expect(balanceTone(15000)).toBe("owes");
    expect(balanceTone(-200)).toBe("credit");
    expect(balanceTone(0)).toBe("settled");
    expect(norm(describeBalance(15000))).toBe("Owes Ksh 15,000.00");
    expect(norm(describeBalance(-200))).toBe("Ksh 200.00 credit");
    expect(describeBalance(0)).toBe("Fully paid");
  });

  it("parses typed money", () => {
    expect(parseMoneyInput("12,500")).toBe(12500);
    expect(parseMoneyInput(" 300.50 ")).toBe(300.5);
    expect(parseMoneyInput("KES 1000")).toBe(1000);
    expect(parseMoneyInput("12.345")).toBeNull();
    expect(parseMoneyInput("-5")).toBeNull();
    expect(parseMoneyInput("abc")).toBeNull();
  });
});
