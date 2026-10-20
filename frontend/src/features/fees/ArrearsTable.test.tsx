import { describe, expect, it, vi } from "vitest";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { ArrearsLearner, ArrearsReport } from "@/lib/api/types";
import { ArrearsTable } from "@/features/fees/ArrearsTable";
import { sumAging } from "@/features/fees/arrears";

const norm = (s: string | null) => (s ?? "").replace(/[  ]/g, " ");

function learner(id: string, name: string, buckets: Partial<ArrearsLearner>): ArrearsLearner {
  const base = { notDue: 0, days1To30: 0, days31To60: 0, days61To90: 0, over90: 0, ...buckets };
  return {
    learnerId: id,
    learnerName: name,
    admissionNumber: `DA/2027/000${id}`,
    gradeLevel: "GRADE_4",
    primaryContactPhone: "254712345678",
    oldestDueOn: "2027-01-31",
    ...base,
    total: base.notDue + base.days1To30 + base.days31To60 + base.days61To90 + base.over90,
  };
}

const learners = [
  learner("1", "Amani Otieno", { days1To30: 15000 }),
  learner("2", "Wanjiru, Neema", { days31To60: 4000, over90: 30500.5 }),
  learner("3", "Kiprono Chebet", { notDue: 2500 }),
];

const report: ArrearsReport = { asOf: "2027-02-15", totals: sumAging(learners), learners };

describe("ArrearsTable", () => {
  it("shows the totals strip from the report", () => {
    render(<ArrearsTable report={report} />);
    expect(norm(screen.getByTestId("arrears-total").textContent)).toBe("Ksh 52,000.50");
    const strip = screen.getByLabelText("Arrears totals");
    expect(norm(within(strip).getByText("Over 90 days").nextSibling!.textContent)).toBe("Ksh 30,500.50");
    expect(norm(within(strip).getByText("1–30 days").nextSibling!.textContent)).toBe("Ksh 15,000.00");
  });

  it("sorts by total, largest first, and toggles", async () => {
    const user = userEvent.setup();
    render(<ArrearsTable report={report} />);
    const names = () => within(screen.getByRole("table")).getAllByRole("link").map((a) => a.textContent);
    expect(names()).toEqual(["Wanjiru, Neema", "Amani Otieno", "Kiprono Chebet"]);
    await user.click(screen.getByRole("button", { name: /Total/ }));
    expect(names()).toEqual(["Kiprono Chebet", "Amani Otieno", "Wanjiru, Neema"]);
    expect(screen.getByRole("columnheader", { name: /Total/ })).toHaveAttribute("aria-sort", "ascending");
  });

  it("exports the sorted rows as CSV with quoted names and raw amounts", async () => {
    const user = userEvent.setup();
    const onExport = vi.fn();
    render(<ArrearsTable report={report} onExport={onExport} />);
    await user.click(screen.getByRole("button", { name: "Download CSV" }));
    expect(onExport).toHaveBeenCalledOnce();
    const [filename, csv] = onExport.mock.calls[0] as [string, string];
    expect(filename).toBe("arrears-2027-02-15.csv");
    const lines = csv.trim().split("\r\n");
    expect(lines[0]).toBe("Admission number,Learner,Grade,Phone,Not due (KES),1–30 days (KES),31–60 days (KES),61–90 days (KES),Over 90 days (KES),Total (KES),Oldest due");
    expect(lines).toHaveLength(4);
    expect(lines[1]).toBe('DA/2027/0002,"Wanjiru, Neema",Grade 4,254712345678,0,0,4000,0,30500.5,34500.5,2027-01-31');
    const totalOfColumn = lines.slice(1).reduce((s, l) => s + Number(l.split(",").at(-2)), 0);
    expect(totalOfColumn).toBeCloseTo(report.totals.total, 2);
  });

  it("shows an empty state when nobody owes", () => {
    render(<ArrearsTable report={{ asOf: "2027-02-15", totals: sumAging([]), learners: [] }} />);
    expect(screen.getByText("Nobody owes fees")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Download CSV" })).toBeDisabled();
  });
});

describe("sumAging", () => {
  it("adds buckets without floating-point drift", () => {
    const t = sumAging([learner("a", "A", { days1To30: 0.1 }), learner("b", "B", { days1To30: 0.2 })]);
    expect(t.days1To30).toBe(0.3);
    expect(t.total).toBe(0.3);
  });
});
