import type { Statement } from "@/lib/api/types";
import { balanceTone, describeBalance, formatDate, formatKes } from "@/lib/format";
import { cx } from "@/lib/cx";
import { EmptyState } from "@/components/ui/Feedback";

export function BalanceFigure({ balance, className }: { balance: number; className?: string }) {
  const tone = balanceTone(balance);
  return (
    <span className={cx("font-bold tabular", tone === "owes" ? "text-owe" : tone === "credit" ? "text-uniform" : "text-ink", className)}>
      {describeBalance(balance)}
    </span>
  );
}

/** Fee statement: totals then a running-balance ledger. Prints on A4 when wrapped in .print-area. */
export function StatementView({ statement, schoolName }: { statement: Statement; schoolName?: string }) {
  return (
    <div>
      {schoolName ? (
        <div className="mb-3 hidden print:block">
          <p className="font-display text-xl font-bold">{schoolName}</p>
          <p>
            Fee statement for {statement.learnerName}, {statement.admissionNumber}
          </p>
        </div>
      ) : null}
      <dl className="grid grid-cols-1 gap-px overflow-hidden rounded-md border border-line bg-line sm:grid-cols-3">
        <div className="bg-paper px-4 py-3">
          <dt className="text-sm text-ink-2">Invoiced</dt>
          <dd className="text-lg font-bold tabular">{formatKes(statement.totalInvoiced)}</dd>
        </div>
        <div className="bg-paper px-4 py-3">
          <dt className="text-sm text-ink-2">Paid</dt>
          <dd className="text-lg font-bold tabular">{formatKes(statement.totalPaid)}</dd>
        </div>
        <div className="bg-paper px-4 py-3">
          <dt className="text-sm text-ink-2">Balance</dt>
          <dd className="text-lg">
            <BalanceFigure balance={statement.balance} />
          </dd>
        </div>
      </dl>

      {statement.entries.length === 0 ? (
        <EmptyState title="No invoices or payments yet">Entries appear here once the term&apos;s invoices are generated.</EmptyState>
      ) : (
        <>
          {/* Phone: one block per entry */}
          <ol className="mt-4 divide-y divide-line rounded-md border border-line sm:hidden print:hidden">
            {statement.entries.map((e, i) => (
              <li key={`${e.reference}-${i}`} className="flex items-start justify-between gap-3 px-3.5 py-3">
                <div className="min-w-0">
                  <p className="font-semibold">{e.description}</p>
                  <p className="text-sm text-ink-2">
                    {formatDate(e.date)}, {e.reference}
                  </p>
                </div>
                <div className="text-right tabular">
                  <p className={cx("font-bold", e.credit > 0 ? "text-uniform" : "text-ink")}>
                    {e.credit > 0 ? `−${formatKes(e.credit)}` : formatKes(e.debit)}
                  </p>
                  <p className="text-sm text-ink-3">Bal. {formatKes(e.runningBalance)}</p>
                </div>
              </li>
            ))}
          </ol>
          {/* Wider screens and print: ledger table */}
          <div className="mt-4 hidden overflow-x-auto sm:block print:block">
            <table className="w-full border-collapse text-left text-[0.92rem]">
              <caption className="sr-only">Statement entries</caption>
              <thead className="border-b border-line-strong text-[0.8rem] text-ink-2">
                <tr>
                  <th scope="col" className="py-2 pr-3 font-semibold">Date</th>
                  <th scope="col" className="px-3 py-2 font-semibold">Reference</th>
                  <th scope="col" className="px-3 py-2 font-semibold">Description</th>
                  <th scope="col" className="px-3 py-2 text-right font-semibold">Charged</th>
                  <th scope="col" className="px-3 py-2 text-right font-semibold">Paid</th>
                  <th scope="col" className="py-2 pl-3 text-right font-semibold">Balance</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-line">
                {statement.entries.map((e, i) => (
                  <tr key={`${e.reference}-${i}`}>
                    <td className="py-2.5 pr-3 whitespace-nowrap">{formatDate(e.date)}</td>
                    <td className="px-3 py-2.5 whitespace-nowrap">{e.reference}</td>
                    <td className="px-3 py-2.5">{e.description}</td>
                    <td className="px-3 py-2.5 text-right whitespace-nowrap tabular">{e.debit ? formatKes(e.debit) : ""}</td>
                    <td className="px-3 py-2.5 text-right whitespace-nowrap text-uniform tabular">{e.credit ? formatKes(e.credit) : ""}</td>
                    <td className="py-2.5 pl-3 text-right font-semibold whitespace-nowrap tabular">{formatKes(e.runningBalance)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  );
}
