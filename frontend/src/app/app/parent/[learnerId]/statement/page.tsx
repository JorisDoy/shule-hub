"use client";

import { useParams } from "next/navigation";
import { Printer } from "lucide-react";
import { useQuery } from "@/lib/hooks";
import { useSession } from "@/features/session/SessionProvider";
import { ChildHeader } from "@/features/parent/ChildHeader";
import { StatementView } from "@/features/fees/StatementView";
import { Button, ErrorAlert, Loading } from "@/components/ui";

export default function ParentStatementPage() {
  const { learnerId } = useParams<{ learnerId: string }>();
  const { api, membership } = useSession();
  const statement = useQuery(`parent-statement:${learnerId}`, (signal) => api.parent.statement(learnerId, { signal }));

  return (
    <div>
      <ChildHeader
        learnerId={learnerId}
        title="Fee statement"
        actions={
          <Button variant="secondary" size="sm" icon={<Printer className="size-4" aria-hidden />} onClick={() => window.print()} disabled={!statement.data}>
            Print
          </Button>
        }
      />
      {statement.error ? (
        <ErrorAlert error={statement.error} onRetry={statement.refetch} />
      ) : !statement.data ? (
        <Loading label="Loading statement" />
      ) : (
        <div className="print-area rounded-lg border border-line bg-paper p-4">
          <StatementView statement={statement.data} schoolName={membership.tenantName} />
        </div>
      )}
    </div>
  );
}
