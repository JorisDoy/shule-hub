"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { GraduationCap, Search } from "lucide-react";
import { useDebounced, useQuery } from "@/lib/hooks";
import { GRADE_OPTIONS, gradeLabel } from "@/lib/grades";
import { BOARDING, LEARNER_STATUS } from "@/lib/labels";
import { formatKenyanPhone } from "@/lib/phone";
import { useSession } from "@/features/session/SessionProvider";
import { Badge, Card, EmptyState, ErrorAlert, PageHeader, Pager, Select, SkeletonRows, Table, TBody, Td, Th, THead } from "@/components/ui";

const PAGE_SIZE = 25;

export default function LearnersPage() {
  const { api, branchId } = useSession();
  const router = useRouter();
  const [q, setQ] = useState("");
  const [grade, setGrade] = useState("");
  const [status, setStatus] = useState("ACTIVE");
  const [page, setPage] = useState(0);
  const query = useDebounced(q.trim(), 350);

  const key = branchId ? `learners:${branchId}:${grade}:${status}:${query}:${page}` : null;
  const learners = useQuery(key, (signal) =>
    api.learners.search({ branchId, gradeLevel: grade || undefined, status: status || undefined, q: query || undefined, page, size: PAGE_SIZE }, { signal }),
  );
  const data = learners.data;
  const list = data?.content ?? [];

  return (
    <div className="max-w-6xl">
      <PageHeader title="Learners" description="Search by name or admission number." />

      <div className="mb-4 grid gap-3 sm:grid-cols-[1fr_12rem_10rem]">
        <div className="flex flex-col gap-1.5">
          <label htmlFor="learner-q" className="text-sm font-semibold">
            Search
          </label>
          <div className="relative">
            <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-ink-3" aria-hidden />
            <input
              id="learner-q"
              type="search"
              value={q}
              onChange={(e) => {
                setQ(e.target.value);
                setPage(0);
              }}
              placeholder="e.g. Amani or DA/2027/0001"
              className="block h-10 w-full rounded-md border border-line-strong bg-paper pr-3 pl-9 placeholder:text-ink-3/80"
            />
          </div>
        </div>
        <Select
          label="Grade"
          options={[{ value: "", label: "All grades" }, ...GRADE_OPTIONS]}
          value={grade}
          onChange={(e) => {
            setGrade(e.target.value);
            setPage(0);
          }}
        />
        <Select
          label="Status"
          options={[
            { value: "ACTIVE", label: "Active" },
            { value: "WITHDRAWN", label: "Withdrawn" },
            { value: "ALUMNI", label: "Alumni" },
            { value: "", label: "Any status" },
          ]}
          value={status}
          onChange={(e) => {
            setStatus(e.target.value);
            setPage(0);
          }}
        />
      </div>

      <Card>
        {learners.error ? (
          <div className="p-4">
            <ErrorAlert error={learners.error} onRetry={learners.refetch} />
          </div>
        ) : learners.loading && !data ? (
          <SkeletonRows rows={6} />
        ) : list.length === 0 ? (
          <EmptyState icon={<GraduationCap className="size-8" aria-hidden />} title={query || grade ? "No learners match" : "No learners yet"}>
            {query || grade ? "Check the spelling, or clear the grade filter." : "Learners appear here once they are admitted from Admissions."}
          </EmptyState>
        ) : (
          <>
            <ul className="divide-y divide-line md:hidden">
              {list.map((l) => (
                <li key={l.id}>
                  <Link href={`/app/learners/${l.id}`} className="block px-4 py-3 hover:bg-canvas">
                    <div className="flex items-start justify-between gap-2">
                      <p className="font-semibold">{l.fullName}</p>
                      <span className="text-sm whitespace-nowrap text-ink-2 tabular">{l.admissionNumber}</span>
                    </div>
                    <p className="text-sm text-ink-2">
                      {l.streamName ?? gradeLabel(l.gradeLevel)}, {BOARDING[l.boardingStatus].toLowerCase()}
                    </p>
                  </Link>
                </li>
              ))}
            </ul>
            <Table caption="Learners" className="hidden md:block">
              <THead>
                <tr>
                  <Th>Adm. no.</Th>
                  <Th>Name</Th>
                  <Th>Class</Th>
                  <Th>Day / boarding</Th>
                  <Th>Contact</Th>
                  <Th>Status</Th>
                </tr>
              </THead>
              <TBody>
                {list.map((l) => (
                  <tr key={l.id} className="cursor-pointer hover:bg-canvas" onClick={() => router.push(`/app/learners/${l.id}`)}>
                    <Td className="whitespace-nowrap tabular">{l.admissionNumber}</Td>
                    <Td>
                      <Link href={`/app/learners/${l.id}`} className="font-semibold text-uniform-dark underline-offset-2 hover:underline">
                        {l.fullName}
                      </Link>
                    </Td>
                    <Td className="whitespace-nowrap">{l.streamName ?? gradeLabel(l.gradeLevel)}</Td>
                    <Td>{BOARDING[l.boardingStatus]}</Td>
                    <Td className="whitespace-nowrap">{formatKenyanPhone(l.primaryContactPhone)}</Td>
                    <Td>
                      <Badge tone={LEARNER_STATUS[l.status].tone}>{LEARNER_STATUS[l.status].label}</Badge>
                    </Td>
                  </tr>
                ))}
              </TBody>
            </Table>
            {data ? <Pager page={data.page} totalPages={data.totalPages} totalElements={data.totalElements} size={data.size} onPage={setPage} /> : null}
          </>
        )}
      </Card>
    </div>
  );
}
