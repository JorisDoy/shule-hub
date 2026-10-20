import { describe, expect, it } from "vitest";
import { csvCell, toCsv } from "@/lib/csv";

describe("csv", () => {
  it("quotes commas, quotes and newlines", () => {
    expect(csvCell("Otieno, Amani")).toBe('"Otieno, Amani"');
    expect(csvCell('say "hi"')).toBe('"say ""hi"""');
    expect(csvCell("a\nb")).toBe('"a\nb"');
  });

  it("neutralises spreadsheet formulas", () => {
    expect(csvCell("=SUM(A1)")).toBe("'=SUM(A1)");
    expect(csvCell("+254712345678")).toBe("'+254712345678");
  });

  it("writes numbers raw and blanks for missing", () => {
    expect(csvCell(15000.5)).toBe("15000.5");
    expect(csvCell(null)).toBe("");
  });

  it("builds rows with CRLF", () => {
    expect(toCsv(["a", "b"], [[1, "x"]])).toBe("a,b\r\n1,x\r\n");
  });
});
