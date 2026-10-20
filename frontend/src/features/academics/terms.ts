import type { AcademicYear } from "@/lib/api/types";
import { formatDate } from "@/lib/format";

export interface TermOption {
  id: string;
  number: number;
  year: number;
  academicYearId: string;
  startDate: string;
  endDate: string;
  label: string;
}

/** Every term across academic years, newest first, labelled "Term 1 2027". */
export function termOptions(years: readonly AcademicYear[]): TermOption[] {
  return years.flatMap((y) =>
    [...y.terms]
      .sort((a, b) => b.number - a.number)
      .map((t) => ({
        id: t.id,
        number: t.number,
        year: y.year,
        academicYearId: y.id,
        startDate: t.startDate,
        endDate: t.endDate,
        label: `Term ${t.number} ${y.year}`,
      })),
  );
}

export function termRange(t: { startDate: string; endDate: string }): string {
  return `${formatDate(t.startDate)} – ${formatDate(t.endDate)}`;
}

export function yearOptions(years: readonly AcademicYear[]) {
  return years.map((y) => ({ value: y.id, label: y.current ? `${y.year} (current)` : String(y.year) }));
}
