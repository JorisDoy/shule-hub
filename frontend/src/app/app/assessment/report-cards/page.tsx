"use client";

import Link from "next/link";
import { Suspense, useState } from "react";
import { ArrowLeft, CheckCheck, FileBadge2, Printer, Save, Send } from "lucide-react";
import type { ReportCard, ReportCardListItem } from "@/lib/api/types";
import { useAction, useQuery } from "@/lib/hooks";
import { REPORT_CARD_STATUS } from "@/lib/labels";
import { useSession } from "@/features/session/SessionProvider";
import { useStreamTerm } from "@/features/assessment/useStreamTerm";
import { StreamTermPicker } from "@/features/assessment/StreamTermPicker";
import { ReportCardDocument } from "@/features/report-cards/ReportCardDocument";
import { Badge, Button, Card, Dialog, EmptyState, ErrorAlert, Loading, PageHeader, SkeletonRows, useToast } from "@/components/ui";
import { cx } from "@/lib/cx";

export default function ReportCardsWrapper() {
  return (
    <Suspense fallback={<Loading />}>
      <ReportCardsPage />
    </Suspense>
  );
}

function ReportCardsPage() {
  const session = useSession();
  const { api } = session;
  const toast = useToast();
  const st = useStreamTerm();
  const learnerId = st.params.get("learnerId") ?? "";
  const cards = useQuery(st.streamId && st.termId ? `cards:${st.streamId}:${st.termId}` : null, (signal) => api.assessment.reportCards({ streamId: st.streamId, termId: st.termId }, { signal }));
  const [confirmRelease, setConfirmRelease] = useState(false);
  const release = useAction(() => api.assessment.releaseReportCards({ streamId: st.streamId, termId: st.termId }));
  const list = cards.data ?? [];
  const approvedCount = list.filter((c) => c.status === "APPROVED").length;
  const canRelease = session.can("REPORTS_RELEASE");

  async function doRelease() {
    const r = await release.run();
    if (r) {
      setConfirmRelease(false);
      toast.show(`${r.released} report card${r.released === 1 ? "" : "s"} released to parents`);
      cards.refetch();
    }
  }

  const back = (
    <Link href={st.streamId ? `/app/assessment?streamId=${st.streamId}&termId=${st.termId}` : "/app/assessment"} className="inline-flex items-center gap-1 font-semibold text-ink-2 hover:text-ink">
      <ArrowLeft className="size-4" aria-hidden /> Assessments
    </Link>
  );

  return (
    <div className="max-w-7xl">
      <PageHeader
        back={back}
        title="Report cards"
        description="Review each learner's card, add comments, approve, then release approved cards to parents."
        actions={
          canRelease && st.streamId ? (
            <Button variant="stamp" icon={<Send className="size-4" aria-hidden />} onClick={() => setConfirmRelease(true)} disabled={approvedCount === 0}>
              Release approved cards{approvedCount > 0 ? ` (${approvedCount})` : ""}
            </Button>
          ) : null
        }
      />
      <div className="no-print mb-4">
        <StreamTermPicker
          streams={st.streamList}
          terms={st.terms}
          streamId={st.streamId}
          termId={st.termId}
          year={st.year?.year}
          loading={st.streams.loading}
          onChange={(p) => st.setParams({ ...p, learnerId: null })}
        />
      </div>

      {!st.streamId ? (
        <Card>
          <EmptyState icon={<FileBadge2 className="size-8" aria-hidden />} title="Choose a class">
            Pick a class and term to see its report cards.
          </EmptyState>
        </Card>
      ) : (
        <div className="grid grid-cols-1 gap-5 lg:grid-cols-[18rem_minmax(0,1fr)]">
          <Card className={cx("no-print self-start", learnerId && "hidden lg:block")}>
            {cards.error ? (
              <div className="p-4">
                <ErrorAlert error={cards.error} onRetry={cards.refetch} />
              </div>
            ) : cards.loading && !cards.data ? (
              <SkeletonRows />
            ) : list.length === 0 ? (
              <EmptyState title="No report cards yet">Cards appear once assessments for this class are released.</EmptyState>
            ) : (
              <ul className="max-h-[70dvh] divide-y divide-line overflow-y-auto">
                {list.map((c) => (
                  <CardListItem key={c.learnerId} item={c} active={c.learnerId === learnerId} onSelect={() => st.setParams({ learnerId: c.learnerId })} />
                ))}
              </ul>
            )}
          </Card>

          <div className={cx("min-w-0", !learnerId && "hidden lg:block")}>
            {learnerId ? (
              <>
                <button type="button" onClick={() => st.setParams({ learnerId: null })} className="no-print mb-3 inline-flex items-center gap-1 font-semibold text-ink-2 hover:text-ink lg:hidden">
                  <ArrowLeft className="size-4" aria-hidden /> All learners
                </button>
                <ReportCardEditor
                  key={`${learnerId}:${st.termId}`}
                  learnerId={learnerId}
                  termId={st.termId}
                  onChanged={(card) => cards.setData((prev) => (prev ?? []).map((c) => (c.learnerId === card.learner.id ? { ...c, status: card.status } : c)))}
                />
              </>
            ) : (
              <Card>
                <EmptyState title="Choose a learner">Select a learner on the left to open their report card.</EmptyState>
              </Card>
            )}
          </div>
        </div>
      )}

      <Dialog
        open={confirmRelease}
        onClose={() => setConfirmRelease(false)}
        title="Release approved report cards?"
        size="sm"
        footer={
          <>
            <Button variant="ghost" onClick={() => setConfirmRelease(false)}>
              Cancel
            </Button>
            <Button variant="stamp" onClick={doRelease} loading={release.pending}>
              Release {approvedCount} card{approvedCount === 1 ? "" : "s"}
            </Button>
          </>
        }
      >
        <div className="flex flex-col gap-3">
          <p>
            {approvedCount} approved card{approvedCount === 1 ? "" : "s"} in {st.stream?.name ?? "this class"} will be visible to parents in the parent portal.
          </p>
          {list.length - approvedCount - list.filter((c) => c.status === "RELEASED").length > 0 ? (
            <p className="text-ink-2">Draft cards stay hidden until they are approved and released.</p>
          ) : null}
          <p className="text-ink-2">Released cards can&apos;t be edited.</p>
          <ErrorAlert error={release.error} />
        </div>
      </Dialog>
    </div>
  );
}

function CardListItem({ item, active, onSelect }: { item: ReportCardListItem; active: boolean; onSelect: () => void }) {
  const s = REPORT_CARD_STATUS[item.status];
  return (
    <li>
      <button type="button" onClick={onSelect} aria-current={active ? "true" : undefined} className={cx("flex w-full items-center justify-between gap-2 px-4 py-2.5 text-left hover:bg-canvas", active && "bg-uniform-tint/60")}>
        <span className="min-w-0">
          <span className="block truncate font-semibold">{item.learnerName}</span>
          <span className="block text-sm text-ink-2 tabular">{item.admissionNumber}</span>
        </span>
        <Badge tone={s.tone}>{s.label}</Badge>
      </button>
    </li>
  );
}

function ReportCardEditor({ learnerId, termId, onChanged }: { learnerId: string; termId: string; onChanged: (c: ReportCard) => void }) {
  const session = useSession();
  const { api } = session;
  const toast = useToast();
  const card = useQuery(`card:${learnerId}:${termId}`, (signal) => api.assessment.reportCard(learnerId, termId, { signal }));
  const [loadedFor, setLoadedFor] = useState<ReportCard | null>(null);
  const [areaComments, setAreaComments] = useState<Record<string, string>>({});
  const [classTeacherComment, setClassTeacher] = useState("");
  const [headComment, setHead] = useState("");
  const save = useAction((body: Parameters<typeof api.assessment.updateReportCard>[2]) => api.assessment.updateReportCard(learnerId, termId, body));
  const approve = useAction(() => api.assessment.approveReportCard(learnerId, termId));

  if (card.data && card.data !== loadedFor) {
    setLoadedFor(card.data);
    setAreaComments(Object.fromEntries(card.data.areas.map((a) => [a.learningAreaId, a.comment ?? ""])));
    setClassTeacher(card.data.classTeacherComment ?? "");
    setHead(card.data.headComment ?? "");
  }

  if (card.error && !card.data) return <ErrorAlert error={card.error} onRetry={card.refetch} />;
  if (!card.data) return <Loading label="Loading report card" />;
  const c = card.data;
  const editable = c.status !== "RELEASED" && session.canAny("ASSESSMENT_ENTER", "ASSESSMENT_MANAGE");
  const dirty =
    classTeacherComment !== (c.classTeacherComment ?? "") || headComment !== (c.headComment ?? "") || c.areas.some((a) => (areaComments[a.learningAreaId] ?? "") !== (a.comment ?? ""));

  async function doSave() {
    const updated = await save.run({
      classTeacherComment: classTeacherComment.trim(),
      headComment: headComment.trim(),
      areaComments: c.areas.map((a) => ({ learningAreaId: a.learningAreaId, comment: (areaComments[a.learningAreaId] ?? "").trim() })),
    });
    if (updated) {
      card.setData(updated);
      onChanged(updated);
      toast.show("Comments saved");
    }
  }

  async function doApprove() {
    const updated = await approve.run();
    if (updated) {
      card.setData(updated);
      onChanged(updated);
      toast.show(`${updated.learner.fullName}'s report card approved`);
    }
  }

  return (
    <div>
      <div className="no-print mb-3 flex flex-wrap items-center gap-2 sm:justify-end">
        <Button variant="ghost" icon={<Printer className="size-4" aria-hidden />} onClick={() => window.print()}>
          Print
        </Button>
        {editable ? (
          <Button variant="secondary" icon={<Save className="size-4" aria-hidden />} onClick={doSave} loading={save.pending} disabled={!dirty}>
            Save comments
          </Button>
        ) : null}
        {editable && c.status === "DRAFT" && session.can("ASSESSMENT_MANAGE") ? (
          <Button icon={<CheckCheck className="size-4" aria-hidden />} onClick={doApprove} loading={approve.pending} disabled={dirty}>
            Approve
          </Button>
        ) : null}
      </div>
      {save.error || approve.error ? <ErrorAlert error={save.error || approve.error} className="no-print mb-3" /> : null}
      {dirty ? <p className="no-print mb-3 text-sm text-owe">Unsaved comments. Save before approving.</p> : null}
      <ReportCardDocument
        card={c}
        schoolName={session.membership.tenantName}
        editing={
          editable
            ? {
                areaComments,
                classTeacherComment,
                headComment,
                onAreaComment: (id, v) => setAreaComments((m) => ({ ...m, [id]: v })),
                onClassTeacherComment: setClassTeacher,
                onHeadComment: setHead,
              }
            : undefined
        }
      />
    </div>
  );
}
