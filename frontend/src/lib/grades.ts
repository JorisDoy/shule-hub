import { GRADE_LEVELS, type GradeLevel, type LevelBand } from "@/lib/api/types";

export const GRADE_LABELS: Record<GradeLevel, string> = {
  DAYCARE: "Daycare",
  PP1: "PP1",
  PP2: "PP2",
  GRADE_1: "Grade 1",
  GRADE_2: "Grade 2",
  GRADE_3: "Grade 3",
  GRADE_4: "Grade 4",
  GRADE_5: "Grade 5",
  GRADE_6: "Grade 6",
  GRADE_7: "Grade 7",
  GRADE_8: "Grade 8",
  GRADE_9: "Grade 9",
};

export const LEVEL_BAND_LABELS: Record<LevelBand, string> = {
  DAYCARE: "Daycare",
  PRE_PRIMARY: "Pre-primary",
  LOWER_PRIMARY: "Lower primary",
  UPPER_PRIMARY: "Upper primary",
  JUNIOR_SCHOOL: "Junior school",
};

const BAND_BY_GRADE: Record<GradeLevel, LevelBand> = {
  DAYCARE: "DAYCARE",
  PP1: "PRE_PRIMARY",
  PP2: "PRE_PRIMARY",
  GRADE_1: "LOWER_PRIMARY",
  GRADE_2: "LOWER_PRIMARY",
  GRADE_3: "LOWER_PRIMARY",
  GRADE_4: "UPPER_PRIMARY",
  GRADE_5: "UPPER_PRIMARY",
  GRADE_6: "UPPER_PRIMARY",
  GRADE_7: "JUNIOR_SCHOOL",
  GRADE_8: "JUNIOR_SCHOOL",
  GRADE_9: "JUNIOR_SCHOOL",
};

/** CBE level band used to pick learning areas: DAYCARE, PP1–2, Grades 1–3, 4–6, 7–9. */
export function levelBandFor(grade: GradeLevel): LevelBand {
  return BAND_BY_GRADE[grade];
}

export function gradeLabel(grade: GradeLevel | null | undefined): string {
  return grade ? GRADE_LABELS[grade] ?? grade : "—";
}

export const GRADE_OPTIONS = GRADE_LEVELS.map((g) => ({ value: g, label: GRADE_LABELS[g] }));

export function isGradeLevel(value: string | null | undefined): value is GradeLevel {
  return !!value && (GRADE_LEVELS as readonly string[]).includes(value);
}
