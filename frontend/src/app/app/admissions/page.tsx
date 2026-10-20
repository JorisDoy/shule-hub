"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { ClipboardList, Plus } from "lucide-react";
import { APPLICATION_STATUSES, type ApplicationStatus } from "@/lib/api/types";
import { useQuery } from "@/lib/hooks";
import { APPLICATION_STATUS } from "@/lib/labels";
import { gradeLabel } from "@/lib/grades";
import { formatDate } from "@/lib/format";
import { formatKenyanPhone } from "@/lib/phone";
import { useSession } from "@/features/session/SessionProvider";
import { NewApplicationDialog } from "@/features/admissions/NewApplicationDialog";
import { Badge, Button, Card, EmptyState, ErrorAlert, PageHeader, SkeletonRows, Table, TBody, Td, Th, THead } from "@/components/ui";
import { cx } from "@/lib/cx";

const FILTERS: { value: "" | ApplicationStatus; label: string }[] = [
  { value: "", label: "All" },
  ...APPLICATION_STATUSES.map((s) => ({ value: s, label: APPLICATION_STATUS[s].label })),
];

export default function AdmissionsPage() {
  const session = useSession();
  const router = useRouter();
  const { api, branchId } = session;
  const [status, setStatus] = useState<"" | ApplicationStatus>("");
  const [creating, setCreating] = useState(false);
  const canManage = session.can("ADMISSIONS_MANAGE");

  const apps = useQuery(branchId ? `apps:${branchId}:${status}` : null, (signal) => api.admissions.list({ branchId, status: status || undefined }, { signal }));
  const list = apps.data ?? [];

  return (
    <div className="max-w-6xl">
      <PageHeader
        title="Admissions"
        description="Enquiries from the website and walk-ins, through assessment and offer to admission."
        actions={
          canManage ? (
            <Button icon={<Plus className="size-4" aria-hidden />} onClick={() => setCreating(true)} disabled={!branchId}>
              New application
            </Button>
          ) : null
        }
      />

      <div className="mb-4 flex flex-col gap-2 sm:flex-row sm:items-center">
        <label htmlFor="status-filter" className="text-sm font-semibold sm:sr-only">
          Status
        </label>
        <select
          id="status-filter"
          value={status}
          onChange={(e) => setStatus(e.target.value as "" | ApplicationStatus)}
          className="h-10 rounded-md border border-line-strong bg-paper px-3 sm:hidden"
        >
          {FILTERS.map((f) => (
            <option key={f.value} value={f.value}>
              {f.label}
            </option>
          ))}
        </select>
        <div className="hidden flex-wrap gap-1.5 sm:flex" role="group" aria-label="Filter by status">
          {FILTERS.map((f) => (
            <button
              key={f.value}
              type="button"
              aria-pressed={status === f.value}
              onClick={() => setStatus(f.value)}
              className={cx(
                "h-8 rounded-full border px-3 text-sm font-semibold",
                status === f.value ? "border-ink bg-ink text-white" : "border-line-strong bg-paper text-ink-2 hover:text-ink",
              )}
            >
              {f.label}
            </button>
          ))}
        </div>
      </div>

      <Card>
        {apps.error ? (
          <div className="p-4">
            <ErrorAlert error={apps.error} onRetry={apps.refetch} />
          </div>
        ) : apps.loading && !apps.data ? (
          <SkeletonRows />
        ) : list.length === 0 ? (
          <EmptyState
            icon={<ClipboardList className="size-8" aria-hidden />}
            title={status ? `No ${APPLICATION_STATUS[status].label.toLowerCase()} applications` : "No applications yet"}
            action={
              canManage && !status ? (
                <Button variant="secondary" onClick={() => setCreating(true)}>
                  Add the first application
                </Button>
              ) : undefined
            }
          >
            {status ? "Try another status, or choose All." : "Website enquiries and applications you add will be listed here."}
          </EmptyState>
        ) : (
          <>
            <ul className="divide-y divide-line md:hidden">
              {list.map((a) => (
                <li key={a.id}>
                  <Link href={`/app/admissions/${a.id}`} className="block px-4 py-3 hover:bg-canvas">
                    <div className="flex items-start justify-between gap-2">
                      <p className="font-semibold">{a.learnerName}</p>
                      <Badge tone={APPLICATION_STATUS[a.status].tone}>{APPLICATION_STATUS[a.status].label}</Badge>
                    </div>
                    <p className="text-sm text-ink-2">
                      {gradeLabel(a.gradeLevel)}, {a.reference}
                    </p>
                    <p className="text-sm text-ink-2">
                      {a.guardianName}, {formatKenyanPhone(a.guardianPhone)}
                    </p>
                  </Link>
                </li>
              ))}
            </ul>
            <Table caption="Applications" className="hidden md:block">
              <THead>
                <tr>
                  <Th>Reference</Th>
                  <Th>Child</Th>
                  <Th>Grade</Th>
                  <Th>Parent</Th>
                  <Th>Status</Th>
                  <Th>Received</Th>
                </tr>
              </THead>
              <TBody>
                {list.map((a) => (
                  <tr key={a.id} className="cursor-pointer hover:bg-canvas" onClick={() => router.push(`/app/admissions/${a.id}`)}>
                    <Td className="whitespace-nowrap">
                      <Link href={`/app/admissions/${a.id}`} className="font-semibold text-uniform-dark underline-offset-2 hover:underline">
                        {a.reference}
                      </Link>
                    </Td>
                    <Td className="font-semibold">{a.learnerName}</Td>
                    <Td className="whitespace-nowrap">{gradeLabel(a.gradeLevel)}</Td>
                    <Td>
                      <span className="block">{a.guardianName}</span>
                      <span className="block text-sm whitespace-nowrap text-ink-2">{formatKenyanPhone(a.guardianPhone)}</span>
                    </Td>
                    <Td>
                      <Badge tone={APPLICATION_STATUS[a.status].tone}>{APPLICATION_STATUS[a.status].label}</Badge>
                    </Td>
                    <Td className="whitespace-nowrap text-ink-2">{formatDate(a.createdAt)}</Td>
                  </tr>
                ))}
              </TBody>
            </Table>
          </>
        )}
      </Card>

      {canManage ? (
        <NewApplicationDialog
          open={creating}
          onClose={() => setCreating(false)}
          onCreated={(app) => {
            setCreating(false);
            router.push(`/app/admissions/${app.id}`);
          }}
        />
      ) : null}
    </div>
  );
}
