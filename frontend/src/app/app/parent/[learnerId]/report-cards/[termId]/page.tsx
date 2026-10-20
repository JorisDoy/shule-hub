"use client";

import { useParams } from "next/navigation";
import { Printer } from "lucide-react";
import { useQuery } from "@/lib/hooks";
import { useSession } from "@/features/session/SessionProvider";
import { ChildHeader } from "@/features/parent/ChildHeader";
import { ReportCardDocument } from "@/features/report-cards/ReportCardDocument";
import { Button, ErrorAlert, Loading } from "@/components/ui";

export default function ParentReportCardPage() {
  const { learnerId, termId } = useParams<{ learnerId: string; termId: string }>();
  const { api, membership } = useSession();
  const card = useQuery(`parent-card:${learnerId}:${termId}`, (signal) => api.parent.reportCard(learnerId, termId, { signal }));
  const title = card.data ? `Term ${card.data.term.number}, ${card.data.term.year}` : "Report card";

  return (
    <div className="max-w-3xl">
      <ChildHeader
        learnerId={learnerId}
        title={title}
        backHref={`/app/parent/${learnerId}/report-cards`}
        backLabel="All report cards"
        actions={
          <Button variant="secondary" size="sm" icon={<Printer className="size-4" aria-hidden />} onClick={() => window.print()} disabled={!card.data}>
            Print
          </Button>
        }
      />
      {card.error ? <ErrorAlert error={card.error} onRetry={card.refetch} /> : !card.data ? <Loading label="Loading report card" /> : <ReportCardDocument card={card.data} schoolName={membership.tenantName} />}
    </div>
  );
}
