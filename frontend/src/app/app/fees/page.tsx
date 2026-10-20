"use client";

import { useState } from "react";
import { useQuery } from "@/lib/hooks";
import { todayIso } from "@/lib/format";
import { useSession } from "@/features/session/SessionProvider";
import { termOptions } from "@/features/academics/terms";
import { FeeStructuresTab } from "@/features/fees/FeeStructuresTab";
import { InvoicesTab } from "@/features/fees/InvoicesTab";
import { PaymentsTab } from "@/features/fees/PaymentsTab";
import { ArrearsTable } from "@/features/fees/ArrearsTable";
import { Alert, Card, CardBody, ErrorAlert, Input, Loading, PageHeader, Select, Tabs } from "@/components/ui";

type Tab = "structures" | "invoices" | "payments" | "arrears";
const TABS: { value: Tab; label: string }[] = [
  { value: "structures", label: "Fee structures" },
  { value: "invoices", label: "Invoices" },
  { value: "payments", label: "Payments" },
  { value: "arrears", label: "Arrears" },
];

export default function FeesPage() {
  const session = useSession();
  const [tab, setTab] = useState<Tab>("structures");
  const terms = termOptions(session.years);
  const [pickedTerm, setPickedTerm] = useState("");
  const termId = pickedTerm || session.currentTerm?.id || terms[0]?.id || "";
  const needsTerm = tab === "structures" || tab === "invoices";

  return (
    <div className="max-w-6xl">
      <PageHeader
        title="Fees"
        description="Fee structures per grade, term invoices, M-Pesa and bank payments, and who owes what."
        actions={
          needsTerm && terms.length > 0 ? (
            <Select label="Term" hideLabel containerClassName="w-44" options={terms.map((t) => ({ value: t.id, label: t.label }))} value={termId} onChange={(e) => setPickedTerm(e.target.value)} />
          ) : null
        }
      />
      <Tabs idBase="fees" label="Fees sections" items={TABS} value={tab} onChange={(v) => setTab(v as Tab)} />
      <Card className="mt-4">
        <CardBody>
          <div role="tabpanel" id="fees-panel" aria-labelledby={`fees-tab-${tab}`}>
            {needsTerm && session.yearsLoading && terms.length === 0 ? (
              <Loading label="Loading terms" className="px-0" />
            ) : needsTerm && terms.length === 0 ? (
              <Alert tone="warning" title="No terms set up">
                Add the academic year and its terms in Settings before creating fee structures or invoices.
              </Alert>
            ) : tab === "structures" ? (
              <FeeStructuresTab termId={termId} terms={terms} />
            ) : tab === "invoices" ? (
              <InvoicesTab termId={termId} terms={terms} />
            ) : tab === "payments" ? (
              <PaymentsTab />
            ) : (
              <ArrearsPanel />
            )}
          </div>
        </CardBody>
      </Card>
    </div>
  );
}

function ArrearsPanel() {
  const { api, branchId } = useSession();
  const [asOf, setAsOf] = useState(() => todayIso());
  const report = useQuery(branchId && asOf ? `arrears:${branchId}:${asOf}` : null, (signal) => api.fees.arrears({ branchId, asOf }, { signal }));

  return (
    <div className="flex flex-col gap-4">
      <Input label="As of" type="date" value={asOf} onChange={(e) => setAsOf(e.target.value)} containerClassName="w-48" hint="Ages each unpaid invoice from its due date" />
      {report.error ? (
        <ErrorAlert error={report.error} onRetry={report.refetch} />
      ) : !report.data ? (
        <Loading label="Calculating arrears" className="px-0" />
      ) : (
        <ArrearsTable report={report.data} />
      )}
    </div>
  );
}
