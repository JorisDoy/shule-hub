"use client";

import { useState } from "react";
import Link from "next/link";
import { Download } from "lucide-react";
import type { ArrearsReport } from "@/lib/api/types";
import { formatDate, formatKes } from "@/lib/format";
import { gradeLabel } from "@/lib/grades";
import { formatKenyanPhone } from "@/lib/phone";
import { downloadCsv } from "@/lib/csv";
import { cx } from "@/lib/cx";
import { Button, EmptyState, SortableTh, Table, TBody, Td, Th, THead, type SortDir } from "@/components/ui";
import { AGING_COLUMNS, arrearsCsv, sortArrears } from "@/features/fees/arrears";

/** Totals strip + aging table, sortable by total, with client-side CSV export. */
export function ArrearsTable({ report, onExport }: { report: ArrearsReport; onExport?: (filename: string, csv: string) => void }) {
  const [dir, setDir] = useState<SortDir>("desc");
  const rows = sortArrears(report.learners, dir);
  const t = report.totals;

  function exportCsv() {
    const filename = `arrears-${report.asOf}.csv`;
    const csv = arrearsCsv(rows);
    (onExport ?? downloadCsv)(filename, csv);
  }

  return (
    <div>
      <dl aria-label="Arrears totals" className="grid grid-cols-2 gap-px overflow-hidden rounded-md border border-line bg-line sm:grid-cols-3 lg:grid-cols-6">
        {AGING_COLUMNS.map((c) => (
          <div key={c.key} className={cx("bg-paper px-3.5 py-3", c.key === "notDue" && "col-span-2 sm:col-span-1")}>
            <dt className="text-sm text-ink-2">{c.label}</dt>
            <dd className={cx("font-bold tabular", c.key === "over90" && t.over90 > 0 && "text-danger")}>{formatKes(t[c.key])}</dd>
          </div>
        ))}
        <div className="col-span-2 bg-ink px-3.5 py-3 text-white sm:col-span-1">
          <dt className="text-sm text-white/75">Total owed</dt>
          <dd className="text-lg font-bold tabular" data-testid="arrears-total">
            {formatKes(t.total)}
          </dd>
        </div>
      </dl>

      <div className="mt-4 flex flex-wrap items-center justify-between gap-2">
        <p className="text-sm text-ink-2">
          {report.learners.length} learner{report.learners.length === 1 ? "" : "s"} owing as of {formatDate(report.asOf)}
        </p>
        <Button size="sm" variant="secondary" icon={<Download className="size-4" aria-hidden />} onClick={exportCsv} disabled={rows.length === 0}>
          Download CSV
        </Button>
      </div>

      {rows.length === 0 ? (
        <EmptyState title="Nobody owes fees">Every invoice due by {formatDate(report.asOf)} has been paid.</EmptyState>
      ) : (
        <>
        <ul className="mt-3 -mx-4 divide-y divide-line border-t border-line md:hidden" aria-label="Learners owing">
          {rows.map((r) => (
            <li key={r.learnerId} className="px-4 py-3">
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <Link href={`/app/learners/${r.learnerId}`} className="font-semibold text-uniform-dark underline-offset-2 hover:underline">
                    {r.learnerName}
                  </Link>
                  <p className="text-sm text-ink-2">
                    {r.admissionNumber}, {gradeLabel(r.gradeLevel)}
                  </p>
                  {r.primaryContactPhone ? (
                    <a href={`tel:+${r.primaryContactPhone}`} className="text-sm text-ink-2 underline underline-offset-2">
                      {formatKenyanPhone(r.primaryContactPhone)}
                    </a>
                  ) : null}
                </div>
                <p className="text-right font-bold whitespace-nowrap tabular">{formatKes(r.total)}</p>
              </div>
              <ul className="mt-1.5 flex flex-wrap gap-x-4 gap-y-0.5 text-sm">
                {AGING_COLUMNS.filter((c) => r[c.key] !== 0).map((c) => (
                  <li key={c.key} className={cx("tabular", c.key === "over90" ? "font-semibold text-danger" : "text-ink-2")}>
                    {c.label}: {formatKes(r[c.key])}
                  </li>
                ))}
              </ul>
            </li>
          ))}
        </ul>
        <Table caption={`Arrears as of ${formatDate(report.asOf)}`} className="mt-3 -mx-4 hidden sm:-mx-5 md:block">
          <THead>
            <tr>
              <Th>Learner</Th>
              {AGING_COLUMNS.map((c) => (
                <Th key={c.key} numeric>
                  {c.label}
                </Th>
              ))}
              <SortableTh numeric active dir={dir} onSort={() => setDir((d) => (d === "desc" ? "asc" : "desc"))}>
                Total
              </SortableTh>
            </tr>
          </THead>
          <TBody>
            {rows.map((r) => (
              <tr key={r.learnerId}>
                <Td className="min-w-[12rem]">
                  <Link href={`/app/learners/${r.learnerId}`} className="font-semibold text-uniform-dark underline-offset-2 hover:underline">
                    {r.learnerName}
                  </Link>
                  <span className="block text-sm text-ink-2">
                    {r.admissionNumber}, {gradeLabel(r.gradeLevel)}
                    {r.primaryContactPhone ? `, ${formatKenyanPhone(r.primaryContactPhone)}` : ""}
                  </span>
                </Td>
                {AGING_COLUMNS.map((c) => (
                  <Td key={c.key} numeric className={cx(r[c.key] === 0 && "text-ink-3", c.key === "over90" && r.over90 > 0 && "font-semibold text-danger")}>
                    {r[c.key] === 0 ? "–" : formatKes(r[c.key])}
                  </Td>
                ))}
                <Td numeric className="font-bold">
                  {formatKes(r.total)}
                </Td>
              </tr>
            ))}
          </TBody>
        </Table>
        </>
      )}
    </div>
  );
}
