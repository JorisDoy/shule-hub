import { describe, expect, it } from "vitest";
import { GRADE_LEVELS, type GradeLevel, type LevelBand } from "@/lib/api/types";
import { gradeLabel, isGradeLevel, levelBandFor } from "@/lib/grades";

describe("levelBandFor", () => {
  const cases: [GradeLevel, LevelBand][] = [
    ["DAYCARE", "DAYCARE"],
    ["PP1", "PRE_PRIMARY"],
    ["PP2", "PRE_PRIMARY"],
    ["GRADE_1", "LOWER_PRIMARY"],
    ["GRADE_2", "LOWER_PRIMARY"],
    ["GRADE_3", "LOWER_PRIMARY"],
    ["GRADE_4", "UPPER_PRIMARY"],
    ["GRADE_5", "UPPER_PRIMARY"],
    ["GRADE_6", "UPPER_PRIMARY"],
    ["GRADE_7", "JUNIOR_SCHOOL"],
    ["GRADE_8", "JUNIOR_SCHOOL"],
    ["GRADE_9", "JUNIOR_SCHOOL"],
  ];

  it.each(cases)("%s → %s", (grade, band) => {
    expect(levelBandFor(grade)).toBe(band);
  });

  it("covers every grade level in the contract", () => {
    expect(cases.map(([g]) => g)).toEqual([...GRADE_LEVELS]);
  });
});

describe("grade labels", () => {
  it("labels grades for people", () => {
    expect(gradeLabel("GRADE_7")).toBe("Grade 7");
    expect(gradeLabel("PP1")).toBe("PP1");
    expect(gradeLabel("DAYCARE")).toBe("Daycare");
    expect(gradeLabel(null)).toBe("—");
  });

  it("recognises valid grade values", () => {
    expect(isGradeLevel("GRADE_9")).toBe(true);
    expect(isGradeLevel("GRADE_10")).toBe(false);
    expect(isGradeLevel("")).toBe(false);
  });
});
