"use client";

import Link from "next/link";
import { useState, type FormEvent } from "react";
import { FileText, Receipt } from "lucide-react";
import { INVOICE_STATUSES, type InvoiceStatus } from "@/lib/api/types";
import { useAction, useQuery } from "@/lib/hooks";
import { fieldError } from "@/lib/api/problem";
import { addDaysIso, formatDate, formatKes, todayIso } from "@/lib/format";
import { INVOICE_STATUS } from "@/lib/labels";
import { useSession } from "@/features/session/SessionProvider";
import type { TermOption } from "@/features/academics/terms";
import { Alert, Badge, Button, Card, CardBody, EmptyState, ErrorAlert, Input, Select, SkeletonRows, Table, TBody, Td, Th, THead } from "@/components/ui";

export function InvoicesTab({ termId, terms }: { termId: string; terms: TermOption[] }) {
  const session = useSession();
  const { api, branchId, branch } = session;
  const [status, setStatus] = useState<"" | InvoiceStatus>("");
  const invoices = useQuery(branchId && termId ? `invoices:${branchId}:${termId}:${status}` : null, (signal) =>
    api.fees.invoices({ branchId, termId, status: status || undefined }, { signal }),
  );
  const term = terms.find((t) => t.id === termId);
  const list = invoices.data ?? [];

  const totals = list.reduce((acc, i) => ({ total: acc.total + i.total, paid: acc.paid + i.paid, balance: acc.balance + i.balance }), { total: 0, paid: 0, balance: 0 });

  return (
    <div className="flex flex-col gap-5">
      {session.can("FEES_MANAGE") ? <GenerateInvoices termId={termId} termLabel={term?.label} defaultDue={term ? addDaysIso(term.startDate, 14) : addDaysIso(todayIso(), 14)} branchName={branch?.name} onGenerated={invoices.refetch} /> : null}

      <div className="flex flex-wrap items-end justify-between gap-3">
        <Select
          label="Status"
          containerClassName="w-48"
          options={[{ value: "", label: "All invoices" }, ...INVOICE_STATUSES.map((s) => ({ value: s, label: INVOICE_STATUS[s].label }))]}
          value={status}
          onChange={(e) => setStatus(e.target.value as "" | InvoiceStatus)}
        />
        {list.length > 0 ? (
          <p className="text-sm text-ink-2">
            {list.length} invoices, {formatKes(totals.total)} billed, <span className="font-semibold text-owe">{formatKes(totals.balance)} outstanding</span>
          </p>
        ) : null}
      </div>

      {invoices.error ? (
        <ErrorAlert error={invoices.error} onRetry={invoices.refetch} />
      ) : invoices.loading && !invoices.data ? (
        <SkeletonRows />
      ) : list.length === 0 ? (
        <EmptyState icon={<FileText className="size-8" aria-hidden />} title="No invoices">
          {status ? "No invoices have this status." : "Generate invoices for the term once its fee structures are published."}
        </EmptyState>
      ) : (
        <Table caption="Invoices" className="-mx-4 sm:-mx-5">
          <THead>
            <tr>
              <Th>Invoice</Th>
              <Th>Learner</Th>
              <Th>Due</Th>
              <Th numeric>Total</Th>
              <Th numeric>Paid</Th>
              <Th numeric>Balance</Th>
              <Th>Status</Th>
            </tr>
          </THead>
          <TBody>
            {list.map((i) => (
              <tr key={i.id}>
                <Td className="whitespace-nowrap tabular">{i.number}</Td>
                <Td className="min-w-[10rem]">
                  <Link href={`/app/learners/${i.learnerId}`} className="font-semibold text-uniform-dark underline-offset-2 hover:underline">
                    {i.learnerName}
                  </Link>
                  <span className="block text-sm text-ink-2 tabular">{i.admissionNumber}</span>
                </Td>
                <Td className="whitespace-nowrap">{formatDate(i.dueOn)}</Td>
                <Td numeric>{formatKes(i.total)}</Td>
                <Td numeric>{formatKes(i.paid)}</Td>
                <Td numeric className={i.balance > 0 ? "font-semibold text-owe" : ""}>
                  {formatKes(i.balance)}
                </Td>
                <Td>
                  <Badge tone={INVOICE_STATUS[i.status].tone}>{INVOICE_STATUS[i.status].label}</Badge>
                </Td>
              </tr>
            ))}
          </TBody>
        </Table>
      )}
    </div>
  );
}

function GenerateInvoices({ termId, termLabel, defaultDue, branchName, onGenerated }: { termId: string; termLabel?: string; defaultDue: string; branchName?: string; onGenerated: () => void }) {
  const { api, branchId } = useSession();
  const [dueOn, setDueOn] = useState(defaultDue);
  const generate = useAction(api.fees.generateInvoices);
  const [result, setResult] = useState<{ created: number; skipped: number } | null>(null);

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (!branchId || !termId || !dueOn) return;
    setResult(null);
    const r = await generate.run({ branchId, termId, dueOn });
    if (r) {
      setResult(r);
      onGenerated();
    }
  }

  return (
    <Card className="bg-canvas">
      <CardBody>
        <form onSubmit={submit} className="flex flex-col gap-3">
          <div>
            <p className="font-semibold">Generate invoices</p>
            <p className="text-sm text-ink-2">
              Creates one invoice per active learner in {branchName ?? "this branch"} for {termLabel ?? "the term"}, from the published fee structures. Learners already invoiced are skipped, so
              it&apos;s safe to run again after new admissions.
            </p>
          </div>
          <div className="flex flex-wrap items-end gap-3">
            <Input label="Due date" type="date" value={dueOn} onChange={(e) => setDueOn(e.target.value)} error={fieldError(generate.error, "dueOn")} containerClassName="w-48" required />
            <Button type="submit" loading={generate.pending} icon={<Receipt className="size-4" aria-hidden />} disabled={!termId || !dueOn}>
              Generate invoices
            </Button>
          </div>
          {result ? (
            <Alert tone="success" title={`${result.created} invoice${result.created === 1 ? "" : "s"} created`}>
              {result.skipped > 0 ? `${result.skipped} learner${result.skipped === 1 ? " was" : "s were"} skipped because they already have an invoice for this term or no published structure matches their grade.` : "No learners were skipped."}
            </Alert>
          ) : null}
          <ErrorAlert error={generate.error && !fieldError(generate.error, "dueOn") ? generate.error : null} />
        </form>
      </CardBody>
    </Card>
  );
}
