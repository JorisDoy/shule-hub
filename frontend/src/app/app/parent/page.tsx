"use client";

import Link from "next/link";
import { useState } from "react";
import { BookOpenCheck, ReceiptText, Smartphone, Users } from "lucide-react";
import type { Child } from "@/lib/api/types";
import { useQuery } from "@/lib/hooks";
import { balanceTone, formatKes } from "@/lib/format";
import { gradeLabel } from "@/lib/grades";
import { useSession } from "@/features/session/SessionProvider";
import { MpesaPayDialog } from "@/features/payments/MpesaPayDialog";
import { Button, buttonClass, EmptyState, ErrorAlert, SkeletonRows } from "@/components/ui";
import { cx } from "@/lib/cx";

export default function ParentHome() {
  const session = useSession();
  const { api } = session;
  const children = useQuery(`children:${session.membership.tenantId}`, (signal) => api.parent.children({ signal }));
  const [paying, setPaying] = useState<Child | null>(null);
  const list = children.data ?? [];
  const firstName = session.userName.split(" ")[0];

  return (
    <div>
      <h1 className="font-display text-[1.6rem]">Hello, {firstName}</h1>
      <p className="mt-1 mb-5 text-ink-2">Fees, payments and report cards for your children at {session.membership.tenantName}.</p>

      {children.error ? (
        <ErrorAlert error={children.error} onRetry={children.refetch} />
      ) : children.loading && !children.data ? (
        <div className="rounded-lg border border-line bg-paper">
          <SkeletonRows rows={2} />
        </div>
      ) : list.length === 0 ? (
        <div className="rounded-lg border border-line bg-paper">
          <EmptyState icon={<Users className="size-8" aria-hidden />} title="No children linked yet">
            Ask the school office to link your children to this account using your phone number or email.
          </EmptyState>
        </div>
      ) : (
        <ul className="flex flex-col gap-4">
          {list.map((c) => (
            <ChildCard key={c.learnerId} child={c} onPay={() => setPaying(c)} />
          ))}
        </ul>
      )}

      {paying ? (
        <MpesaPayDialog
          open
          onClose={() => setPaying(null)}
          learnerName={paying.fullName}
          balance={paying.balance}
          initiate={(amount, phone) => api.parent.pay(paying.learnerId, { amount, phone })}
          poll={(intentId, signal) => api.parent.paymentStatus(paying.learnerId, intentId, { signal })}
          onPaid={children.refetch}
        />
      ) : null}
    </div>
  );
}

function ChildCard({ child, onPay }: { child: Child; onPay: () => void }) {
  const tone = balanceTone(child.balance);
  return (
    <li className="overflow-hidden rounded-lg border border-line bg-paper">
      <div className="flex flex-wrap items-start justify-between gap-3 px-4 pt-4 pb-3">
        <div className="min-w-0">
          <p className="font-display text-xl font-semibold">{child.fullName}</p>
          <p className="text-ink-2">
            {child.streamName ?? gradeLabel(child.gradeLevel)}
            <span className="text-ink-3">, adm. {child.admissionNumber}</span>
          </p>
        </div>
      </div>
      <div
        className={cx(
          "mx-4 flex items-baseline justify-between gap-3 rounded-md px-3.5 py-3",
          tone === "owes" ? "bg-owe-tint" : tone === "credit" ? "bg-uniform-tint" : "bg-canvas",
        )}
      >
        <span className={cx("text-sm font-semibold", tone === "owes" ? "text-owe" : tone === "credit" ? "text-uniform-dark" : "text-ink-2")}>
          {tone === "owes" ? "Balance due" : tone === "credit" ? "Credit on account" : "Fees"}
        </span>
        <span className={cx("text-xl font-bold tabular", tone === "owes" ? "text-owe" : tone === "credit" ? "text-uniform-dark" : "text-ink")}>
          {tone === "settled" ? "Fully paid" : formatKes(Math.abs(child.balance))}
        </span>
      </div>
      <div className="grid grid-cols-1 gap-2 px-4 pt-3 pb-4 min-[400px]:grid-cols-3">
        <Button onClick={onPay} icon={<Smartphone className="size-4" aria-hidden />} className="min-[400px]:col-span-3 sm:col-span-1">
          Pay with M-Pesa
        </Button>
        <Link href={`/app/parent/${child.learnerId}/statement`} className={buttonClass("secondary", "md", "min-[400px]:col-span-1")}>
          <ReceiptText className="size-4" aria-hidden />
          Statement
        </Link>
        <Link href={`/app/parent/${child.learnerId}/report-cards`} className={buttonClass("secondary", "md", "min-[400px]:col-span-2 sm:col-span-1")}>
          <BookOpenCheck className="size-4" aria-hidden />
          Report cards
        </Link>
      </div>
    </li>
  );
}
