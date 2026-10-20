"use client";

import Link from "next/link";
import { useState, type FormEvent } from "react";
import { Banknote, Plus, UserCheck } from "lucide-react";
import { PAYMENT_STATUSES, type PaymentStatus, type PaymentView } from "@/lib/api/types";
import { useAction, useQuery } from "@/lib/hooks";
import { fieldError } from "@/lib/api/problem";
import { formatDateTime, formatKes, parseMoneyInput, todayIso } from "@/lib/format";
import { formatKenyanPhone } from "@/lib/phone";
import { PAYMENT_CHANNEL, PAYMENT_STATUS } from "@/lib/labels";
import { useSession } from "@/features/session/SessionProvider";
import { LearnerPicker, type PickedLearner } from "@/features/learners/LearnerPicker";
import { Badge, Button, Dialog, EmptyState, ErrorAlert, Input, Select, SkeletonRows, Table, TBody, Td, Th, THead, useToast } from "@/components/ui";

export function PaymentsTab() {
  const session = useSession();
  const { api, branchId } = session;
  const canManage = session.can("FEES_MANAGE");
  const [status, setStatus] = useState<"" | PaymentStatus>("");
  const [recording, setRecording] = useState(false);
  const [assigning, setAssigning] = useState<PaymentView | null>(null);
  const payments = useQuery(branchId ? `payments:${branchId}:${status}` : null, (signal) => api.fees.payments({ branchId, status: status || undefined }, { signal }));
  const list = payments.data ?? [];
  const suspenseCount = status === "" ? list.filter((p) => p.status === "SUSPENSE").length : 0;

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <Select
          label="Status"
          containerClassName="w-48"
          options={[{ value: "", label: "All payments" }, ...PAYMENT_STATUSES.map((s) => ({ value: s, label: PAYMENT_STATUS[s].label }))]}
          value={status}
          onChange={(e) => setStatus(e.target.value as "" | PaymentStatus)}
        />
        {canManage ? (
          <Button icon={<Plus className="size-4" aria-hidden />} onClick={() => setRecording(true)}>
            Record cash or bank payment
          </Button>
        ) : null}
      </div>

      {suspenseCount > 0 ? (
        <button type="button" onClick={() => setStatus("SUSPENSE")} className="self-start rounded-md bg-owe-tint px-3 py-2 text-left text-[0.92rem] text-owe">
          <strong>{suspenseCount} payment{suspenseCount === 1 ? "" : "s"} in suspense.</strong> The account number didn&apos;t match a learner. Show them to assign.
        </button>
      ) : null}

      {payments.error ? (
        <ErrorAlert error={payments.error} onRetry={payments.refetch} />
      ) : payments.loading && !payments.data ? (
        <SkeletonRows />
      ) : list.length === 0 ? (
        <EmptyState icon={<Banknote className="size-8" aria-hidden />} title={status === "SUSPENSE" ? "Nothing in suspense" : "No payments yet"}>
          {status === "SUSPENSE" ? "Every M-Pesa payment matched a learner." : "M-Pesa payments arrive here automatically. Cash and bank payments are recorded by the bursar."}
        </EmptyState>
      ) : (
        <Table caption="Payments" className="-mx-4 sm:-mx-5">
          <THead>
            <tr>
              <Th>Received</Th>
              <Th>Reference</Th>
              <Th>From</Th>
              <Th>Learner</Th>
              <Th numeric>Amount</Th>
              <Th>Status</Th>
              {canManage ? <Th className="text-right">Action</Th> : null}
            </tr>
          </THead>
          <TBody>
            {list.map((p) => (
              <tr key={p.id}>
                <Td className="whitespace-nowrap">{formatDateTime(p.receivedAt)}</Td>
                <Td>
                  <span className="block font-semibold tracking-wide whitespace-nowrap">{p.reference}</span>
                  <span className="block text-sm text-ink-2">{PAYMENT_CHANNEL[p.channel]}</span>
                </Td>
                <Td className="min-w-[9rem]">
                  {p.payerName ?? "—"}
                  {p.payerPhone ? <span className="block text-sm whitespace-nowrap text-ink-2">{formatKenyanPhone(p.payerPhone)}</span> : null}
                  {p.accountReference ? <span className="block text-sm text-ink-2">Acc. {p.accountReference}</span> : null}
                </Td>
                <Td className="min-w-[9rem]">
                  {p.learnerId ? (
                    <Link href={`/app/learners/${p.learnerId}`} className="font-semibold text-uniform-dark underline-offset-2 hover:underline">
                      {p.learnerName}
                    </Link>
                  ) : (
                    <span className="text-ink-3">Unmatched</span>
                  )}
                </Td>
                <Td numeric>
                  <span className="font-semibold">{formatKes(p.amount)}</span>
                  {p.unallocated > 0 && p.status === "ALLOCATED" ? <span className="block text-sm text-uniform">{formatKes(p.unallocated)} credit</span> : null}
                </Td>
                <Td>
                  <Badge tone={PAYMENT_STATUS[p.status].tone}>{PAYMENT_STATUS[p.status].label}</Badge>
                </Td>
                {canManage ? (
                  <Td className="text-right">
                    {p.status === "SUSPENSE" ? (
                      <Button size="sm" variant="secondary" icon={<UserCheck className="size-4" aria-hidden />} onClick={() => setAssigning(p)}>
                        Assign
                      </Button>
                    ) : null}
                  </Td>
                ) : null}
              </tr>
            ))}
          </TBody>
        </Table>
      )}

      {recording ? (
        <RecordPaymentDialog
          onClose={() => setRecording(false)}
          onSaved={(p) => {
            payments.setData((prev) => [p, ...(prev ?? [])]);
            setRecording(false);
          }}
        />
      ) : null}
      {assigning ? (
        <AssignDialog
          payment={assigning}
          onClose={() => setAssigning(null)}
          onSaved={(p) => {
            payments.setData((prev) => (prev ?? []).map((x) => (x.id === p.id ? p : x)).filter((x) => !status || x.status === status));
            setAssigning(null);
          }}
        />
      ) : null}
    </div>
  );
}

function RecordPaymentDialog({ onClose, onSaved }: { onClose: () => void; onSaved: (p: PaymentView) => void }) {
  const { api } = useSession();
  const toast = useToast();
  const [learner, setLearner] = useState<PickedLearner | null>(null);
  const [channel, setChannel] = useState<"CASH" | "BANK">("BANK");
  const [amount, setAmount] = useState("");
  const [reference, setReference] = useState("");
  const [receivedOn, setReceivedOn] = useState(() => todayIso());
  const [errors, setErrors] = useState<Record<string, string>>({});
  const save = useAction(api.fees.recordPayment);

  async function submit(e: FormEvent) {
    e.preventDefault();
    const amt = parseMoneyInput(amount);
    const next: Record<string, string> = {};
    if (!learner) next.learner = "Choose the learner this payment is for";
    if (amt === null || amt <= 0) next.amount = "Enter the amount received";
    if (!reference.trim()) next.reference = channel === "BANK" ? "Enter the bank slip or transaction reference" : "Enter the receipt book number";
    if (!receivedOn) next.receivedOn = "Enter the date received";
    setErrors(next);
    if (Object.keys(next).length > 0 || !learner || amt === null) return;
    const p = await save.run({ learnerId: learner.id, amount: amt, channel, reference: reference.trim(), receivedOn });
    if (p) {
      toast.show(`${formatKes(p.amount)} recorded for ${learner.fullName}`);
      onSaved(p);
    }
  }

  return (
    <Dialog
      open
      onClose={onClose}
      title="Record cash or bank payment"
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" form="record-payment" loading={save.pending}>
            Record payment
          </Button>
        </>
      }
    >
      <form id="record-payment" noValidate onSubmit={submit} className="flex flex-col gap-4">
        <LearnerPicker value={learner} onChange={setLearner} error={errors.learner ?? fieldError(save.error, "learnerId")} />
        <div className="grid gap-4 sm:grid-cols-2">
          <Select
            label="Paid by"
            options={[
              { value: "BANK", label: "Bank deposit" },
              { value: "CASH", label: "Cash at the office" },
            ]}
            value={channel}
            onChange={(e) => setChannel(e.target.value as "CASH" | "BANK")}
          />
          <Input label="Amount" prefix="KES" inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value)} error={errors.amount ?? fieldError(save.error, "amount")} required />
          <Input label={channel === "BANK" ? "Bank reference" : "Receipt number"} value={reference} onChange={(e) => setReference(e.target.value)} error={errors.reference ?? fieldError(save.error, "reference")} required />
          <Input label="Date received" type="date" max={todayIso()} value={receivedOn} onChange={(e) => setReceivedOn(e.target.value)} error={errors.receivedOn ?? fieldError(save.error, "receivedOn")} required />
        </div>
        <ErrorAlert error={save.error && !fieldError(save.error, "learnerId", "amount", "reference", "receivedOn") ? save.error : null} />
      </form>
    </Dialog>
  );
}

function AssignDialog({ payment, onClose, onSaved }: { payment: PaymentView; onClose: () => void; onSaved: (p: PaymentView) => void }) {
  const { api } = useSession();
  const toast = useToast();
  const [learner, setLearner] = useState<PickedLearner | null>(null);
  const assign = useAction((learnerId: string) => api.fees.assignPayment(payment.id, learnerId));

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (!learner) return;
    const p = await assign.run(learner.id);
    if (p) {
      toast.show(`${payment.reference} assigned to ${learner.fullName}`);
      onSaved(p);
    }
  }

  return (
    <Dialog
      open
      onClose={onClose}
      title="Assign payment to a learner"
      description={`${payment.reference}, ${formatKes(payment.amount)}${payment.payerName ? ` from ${payment.payerName}` : ""}`}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" form="assign-payment" loading={assign.pending} disabled={!learner}>
            Assign payment
          </Button>
        </>
      }
    >
      <form id="assign-payment" onSubmit={submit} className="flex flex-col gap-4">
        {payment.accountReference ? (
          <p className="text-[0.95rem] text-ink-2">
            The payer typed <strong className="text-ink">{payment.accountReference}</strong> as the account number.
          </p>
        ) : null}
        <LearnerPicker value={learner} onChange={setLearner} initialQuery={payment.accountReference ?? ""} error={fieldError(assign.error, "learnerId")} />
        <ErrorAlert error={assign.error && !fieldError(assign.error, "learnerId") ? assign.error : null} />
      </form>
    </Dialog>
  );
}
