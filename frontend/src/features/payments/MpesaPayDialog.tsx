"use client";

import { useState, type FormEvent } from "react";
import { CheckCircle2, Smartphone, XCircle } from "lucide-react";
import { Button, Dialog, ErrorAlert, Input, Spinner } from "@/components/ui";
import type { StkInitiation, StkStatus } from "@/lib/api/types";
import { fieldError } from "@/lib/api/problem";
import { formatDateTime, formatKes } from "@/lib/format";
import { formatKenyanPhone, normalizeKenyanPhone, PHONE_ERROR, PHONE_HINT } from "@/lib/phone";
import { useAction } from "@/lib/hooks";
import { isFinalIntent, useStkPolling } from "@/features/payments/useStkPolling";

export function parseWholeShillings(input: string): number | null {
  const cleaned = input.replace(/[,\s]/g, "");
  if (!/^\d+$/.test(cleaned)) return null;
  const n = Number(cleaned);
  return n >= 1 ? n : null;
}

/**
 * Sends an M-Pesa STK push and follows it until the payer enters their PIN (or doesn't).
 * Used by staff on a learner's page and by parents on their children's cards.
 */
export function MpesaPayDialog({
  open,
  onClose,
  learnerName,
  balance,
  initiate,
  poll,
  onPaid,
}: {
  open: boolean;
  onClose: () => void;
  learnerName: string;
  balance?: number;
  initiate: (amount: number, phone: string) => Promise<StkInitiation>;
  poll: (intentId: string, signal: AbortSignal) => Promise<StkStatus>;
  onPaid?: () => void;
}) {
  const [amount, setAmount] = useState("");
  const [phone, setPhone] = useState("");
  const [errors, setErrors] = useState<{ amount?: string; phone?: string }>({});
  const [initiation, setInitiation] = useState<(StkInitiation & { phone: string; amount: number }) | null>(null);
  const send = useAction(initiate);
  const polling = useStkPolling(initiation && !isFinalIntent(initiation.status) ? initiation.intentId : null, poll);
  const [notified, setNotified] = useState<string | null>(null);

  const finalStatus = polling.status ?? (initiation && isFinalIntent(initiation.status) ? { status: initiation.status, mpesaReceipt: null, resultDesc: initiation.customerMessage } : null);
  const succeeded = finalStatus?.status === "SUCCEEDED";
  const failed = finalStatus?.status === "FAILED";

  if (succeeded && initiation && notified !== initiation.intentId) {
    setNotified(initiation.intentId);
    onPaid?.();
  }

  function reset() {
    setInitiation(null);
    send.reset();
  }

  function close() {
    reset();
    setAmount("");
    setPhone("");
    setErrors({});
    onClose();
  }

  async function submit(e: FormEvent) {
    e.preventDefault();
    const amt = parseWholeShillings(amount);
    const msisdn = normalizeKenyanPhone(phone);
    const next = {
      amount: amt === null ? "Enter a whole number of shillings, at least 1" : undefined,
      phone: msisdn === null ? PHONE_ERROR : undefined,
    };
    setErrors(next);
    if (amt === null || msisdn === null) return;
    const result = await send.run(amt, msisdn);
    if (result) setInitiation({ ...result, phone: msisdn, amount: amt });
  }

  const waiting = initiation && !succeeded && !failed && !polling.timedOut;

  return (
    <Dialog
      open={open}
      onClose={close}
      title="Request M-Pesa payment"
      description={`For ${learnerName}`}
      size="sm"
      footer={
        initiation ? (
          <>
            {failed || polling.timedOut ? (
              <Button variant="secondary" onClick={reset}>
                Try again
              </Button>
            ) : null}
            <Button variant={succeeded ? "primary" : "ghost"} onClick={close}>
              {waiting ? "Close and check later" : "Done"}
            </Button>
          </>
        ) : (
          <>
            <Button variant="ghost" onClick={close}>
              Cancel
            </Button>
            <Button type="submit" form="stk-form" loading={send.pending} icon={<Smartphone className="size-4" aria-hidden />}>
              Send payment request
            </Button>
          </>
        )
      }
    >
      {!initiation ? (
        <form id="stk-form" onSubmit={submit} noValidate className="flex flex-col gap-4">
          <Input
            label="Amount"
            prefix="KES"
            inputMode="numeric"
            autoComplete="off"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            error={errors.amount ?? fieldError(send.error, "amount")}
            hint={
              balance !== undefined && balance > 0 ? (
                <button type="button" className="font-semibold text-uniform underline underline-offset-2" onClick={() => setAmount(String(Math.ceil(balance)))}>
                  Use the full balance of {formatKes(Math.ceil(balance), { whole: true })}
                </button>
              ) : (
                "Whole shillings only"
              )
            }
            required
          />
          <Input
            label="M-Pesa phone number"
            type="tel"
            inputMode="tel"
            autoComplete="tel"
            placeholder="0712 345 678"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            error={errors.phone ?? fieldError(send.error, "phone")}
            hint={PHONE_HINT}
            required
          />
          <p className="text-sm text-ink-2">The phone will show an M-Pesa prompt. The payer enters their PIN to confirm.</p>
          <ErrorAlert error={send.error && !fieldError(send.error, "amount", "phone") ? send.error : null} />
        </form>
      ) : (
        <div aria-live="polite" className="flex flex-col gap-3">
          {succeeded ? (
            <div className="flex items-start gap-3">
              <CheckCircle2 className="mt-0.5 size-7 shrink-0 text-uniform" aria-hidden />
              <div>
                <p className="font-display text-xl">Payment received</p>
                <p className="mt-1 text-ink-2">
                  {formatKes(initiation.amount)} from {formatKenyanPhone(initiation.phone)}.
                </p>
                {polling.status?.mpesaReceipt ? (
                  <p className="mt-3">
                    M-Pesa receipt <strong className="text-lg tracking-wide">{polling.status.mpesaReceipt}</strong>
                  </p>
                ) : null}
                {polling.status?.updatedAt ? <p className="text-sm text-ink-3">{formatDateTime(polling.status.updatedAt)}</p> : null}
              </div>
            </div>
          ) : failed ? (
            <div className="flex items-start gap-3">
              <XCircle className="mt-0.5 size-7 shrink-0 text-danger" aria-hidden />
              <div>
                <p className="font-display text-xl">Payment not completed</p>
                <p className="mt-1 text-ink-2">{finalStatus?.resultDesc || "M-Pesa didn't confirm the payment."}</p>
                <p className="mt-2 text-sm text-ink-2">No money was taken. You can send the request again.</p>
              </div>
            </div>
          ) : polling.timedOut ? (
            <div>
              <p className="font-display text-xl">Still waiting for M-Pesa</p>
              <p className="mt-1 text-ink-2">
                We haven&apos;t had a confirmation after two minutes. If the payer completed it, the payment will appear on the statement once M-Pesa
                confirms. Check the phone&apos;s M-Pesa messages before sending another request.
              </p>
            </div>
          ) : (
            <div className="flex items-start gap-3">
              <Spinner className="mt-1.5 size-6 text-uniform" />
              <div>
                <p className="font-display text-xl">Check the phone</p>
                <p className="mt-1 text-ink-2">
                  An M-Pesa prompt for {formatKes(initiation.amount, { whole: true })} was sent to {formatKenyanPhone(initiation.phone)}. Enter the
                  M-Pesa PIN to pay.
                </p>
                {initiation.customerMessage ? <p className="mt-2 text-sm text-ink-3">{initiation.customerMessage}</p> : null}
                {polling.status?.status === "SUBMITTED" ? <p className="mt-2 text-sm font-semibold text-info">Prompt delivered. Waiting for the PIN…</p> : null}
                {polling.lastError ? <p className="mt-2 text-sm text-owe">Connection is unsteady. Still checking…</p> : null}
              </div>
            </div>
          )}
        </div>
      )}
    </Dialog>
  );
}
