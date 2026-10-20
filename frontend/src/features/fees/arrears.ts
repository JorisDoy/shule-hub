import type { AgingBuckets, ArrearsLearner } from "@/lib/api/types";
import { toCsv } from "@/lib/csv";
import { gradeLabel } from "@/lib/grades";

export const AGING_COLUMNS: { key: keyof Omit<AgingBuckets, "total">; label: string }[] = [
  { key: "notDue", label: "Not due" },
  { key: "days1To30", label: "1–30 days" },
  { key: "days31To60", label: "31–60 days" },
  { key: "days61To90", label: "61–90 days" },
  { key: "over90", label: "Over 90 days" },
];

/** Totals computed from the rows (used to cross-check / when the server totals are absent). */
export function sumAging(rows: readonly AgingBuckets[]): AgingBuckets {
  const round = (n: number) => Math.round(n * 100) / 100;
  const t = { notDue: 0, days1To30: 0, days31To60: 0, days61To90: 0, over90: 0, total: 0 };
  for (const r of rows) {
    t.notDue += r.notDue;
    t.days1To30 += r.days1To30;
    t.days31To60 += r.days31To60;
    t.days61To90 += r.days61To90;
    t.over90 += r.over90;
    t.total += r.total;
  }
  return {
    notDue: round(t.notDue),
    days1To30: round(t.days1To30),
    days31To60: round(t.days31To60),
    days61To90: round(t.days61To90),
    over90: round(t.over90),
    total: round(t.total),
  };
}

export function sortArrears(rows: readonly ArrearsLearner[], dir: "asc" | "desc"): ArrearsLearner[] {
  return [...rows].sort((a, b) => (dir === "asc" ? a.total - b.total : b.total - a.total) || a.learnerName.localeCompare(b.learnerName));
}

export function arrearsCsv(rows: readonly ArrearsLearner[]): string {
  return toCsv(
    ["Admission number", "Learner", "Grade", "Phone", ...AGING_COLUMNS.map((c) => `${c.label} (KES)`), "Total (KES)", "Oldest due"],
    rows.map((r) => [
      r.admissionNumber,
      r.learnerName,
      gradeLabel(r.gradeLevel),
      r.primaryContactPhone ?? "",
      ...AGING_COLUMNS.map((c) => r[c.key]),
      r.total,
      r.oldestDueOn ?? "",
    ]),
  );
}
