"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { ChevronRight, FileBadge2 } from "lucide-react";
import { useQuery } from "@/lib/hooks";
import { formatDate } from "@/lib/format";
import { useSession } from "@/features/session/SessionProvider";
import { ChildHeader } from "@/features/parent/ChildHeader";
import { EmptyState, ErrorAlert, SkeletonRows } from "@/components/ui";

export default function ParentReportCardsPage() {
  const { learnerId } = useParams<{ learnerId: string }>();
  const { api } = useSession();
  const cards = useQuery(`parent-cards:${learnerId}`, (signal) => api.parent.reportCards(learnerId, { signal }));
  const list = [...(cards.data ?? [])].sort((a, b) => b.year - a.year || b.number - a.number);

  return (
    <div>
      <ChildHeader learnerId={learnerId} title="Report cards" />
      <div className="rounded-lg border border-line bg-paper">
        {cards.error ? (
          <div className="p-4">
            <ErrorAlert error={cards.error} onRetry={cards.refetch} />
          </div>
        ) : cards.loading && !cards.data ? (
          <SkeletonRows rows={3} />
        ) : list.length === 0 ? (
          <EmptyState icon={<FileBadge2 className="size-8" aria-hidden />} title="No report cards released yet">
            The school releases report cards at the end of each term. They&apos;ll appear here.
          </EmptyState>
        ) : (
          <ul className="divide-y divide-line">
            {list.map((c) => (
              <li key={c.termId}>
                <Link href={`/app/parent/${learnerId}/report-cards/${c.termId}`} className="flex items-center justify-between gap-3 px-4 py-3.5 hover:bg-canvas">
                  <span>
                    <span className="block font-semibold">
                      Term {c.number}, {c.year}
                    </span>
                    {c.releasedAt ? <span className="block text-sm text-ink-2">Released {formatDate(c.releasedAt)}</span> : null}
                  </span>
                  <ChevronRight className="size-5 text-ink-3" aria-hidden />
                </Link>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
