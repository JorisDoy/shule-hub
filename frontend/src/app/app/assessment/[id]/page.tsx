"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import { ArrowLeft, Lock, Save, Send } from "lucide-react";
import type { ScoreInput, ScoreSheet } from "@/lib/api/types";
import { useAction, useQuery } from "@/lib/hooks";
import { formatDate } from "@/lib/format";
import { gradeLabel } from "@/lib/grades";
import { ASSESSMENT_STATUS, ASSESSMENT_TYPE } from "@/lib/labels";
import { useSession } from "@/features/session/SessionProvider";
import { ScoreEntryRow, parseScore, scoreError, type GridColumn, type ScoreDraft } from "@/features/assessment/ScoreEntryRow";
import { Alert, Badge, Button, Card, Dialog, EmptyState, ErrorAlert, Loading, PageHeader, useToast } from "@/components/ui";

function draftsFrom(sheet: ScoreSheet): Record<string, ScoreDraft> {
  return Object.fromEntries(
    sheet.rows.map((r) => [r.learnerId, { raw: r.rawScore === null || r.rawScore === undefined ? "" : String(r.rawScore), rubric: r.rubricLevel ?? "", comment: r.comment ?? "" }]),
  );
}

function sameDraft(a: ScoreDraft | undefined, b: ScoreDraft | undefined) {
  return !!a && !!b && a.raw.trim() === b.raw.trim() && a.rubric === b.rubric && a.comment.trim() === b.comment.trim();
}

export default function ScoreEntryPage() {
  const { id } = useParams<{ id: string }>();
  const session = useSession();
  const { api } = session;
  const toast = useToast();
  const sheet = useQuery(`scores:${id}`, (signal) => api.assessment.scores(id, { signal }));
  const [drafts, setDrafts] = useState<Record<string, ScoreDraft>>({});
  const [saved, setSaved] = useState<Record<string, ScoreDraft>>({});
  const [loadedFor, setLoadedFor] = useState<ScoreSheet | null>(null);
  const [confirmRelease, setConfirmRelease] = useState(false);
  const fields = useRef<Map<string, HTMLElement>>(new Map());

  // Reset the drafts whenever a new sheet arrives from the server (load, save, release).
  if (sheet.data && sheet.data !== loadedFor) {
    setLoadedFor(sheet.data);
    const d = draftsFrom(sheet.data);
    setDrafts(d);
    setSaved(d);
  }

  const save = useAction((scores: ScoreInput[]) => api.assessment.saveScores(id, scores));
  const release = useAction(() => api.assessment.release(id));

  const data = sheet.data;
  const assessment = data?.assessment;
  const maxScore = assessment?.maxScore ?? null;
  const readOnly = assessment?.status === "RELEASED" || !session.canAny("ASSESSMENT_ENTER", "ASSESSMENT_MANAGE");
  const rows = data?.rows ?? [];
  const dirtyIds = rows.filter((r) => !sameDraft(drafts[r.learnerId], saved[r.learnerId])).map((r) => r.learnerId);
  const invalid = rows.some((r) => scoreError(drafts[r.learnerId]?.raw ?? "", maxScore));
  const scored = rows.filter((r) => {
    const d = drafts[r.learnerId];
    return d && (maxScore === null ? d.rubric !== "" : parseScore(d.raw) !== null);
  }).length;

  useEffect(() => {
    if (dirtyIds.length === 0) return;
    const warn = (e: BeforeUnloadEvent) => {
      e.preventDefault();
    };
    window.addEventListener("beforeunload", warn);
    return () => window.removeEventListener("beforeunload", warn);
  }, [dirtyIds.length]);

  const onChange = useCallback((learnerId: string, patch: Partial<ScoreDraft>) => {
    setDrafts((d) => ({ ...d, [learnerId]: { ...d[learnerId], ...patch } }));
  }, []);

  const registerField = useCallback((column: GridColumn, index: number, el: HTMLElement | null) => {
    const key = `${column}:${index}`;
    if (el) fields.current.set(key, el);
    else fields.current.delete(key);
  }, []);

  const onNavigate = useCallback((column: GridColumn, index: number, delta: 1 | -1) => {
    const target = fields.current.get(`${column}:${index + delta}`);
    if (target) target.focus();
    else if (delta === 1 && column === "score") fields.current.get("comment:0")?.focus();
  }, []);

  async function doSave() {
    if (!data) return;
    const scores: ScoreInput[] = dirtyIds.map((learnerId) => {
      const d = drafts[learnerId];
      const raw = parseScore(d.raw);
      return {
        learnerId,
        rawScore: maxScore !== null && raw !== null && !Number.isNaN(raw) ? raw : undefined,
        rubricLevel: maxScore === null && d.rubric ? d.rubric : undefined,
        comment: d.comment.trim() || undefined,
      };
    });
    const result = await save.run(scores);
    if (result) {
      sheet.setData(result);
      toast.show(`Saved ${scores.length} learner${scores.length === 1 ? "" : "s"}`);
    }
  }

  async function doRelease() {
    const updated = await release.run();
    if (updated) {
      setConfirmRelease(false);
      sheet.setData((prev) => (prev ? { ...prev, assessment: updated } : prev!));
      toast.show("Scores released. They now count towards report cards.");
    }
  }

  const back = (
    <Link href={assessment ? `/app/assessment?streamId=${assessment.streamId}&termId=${assessment.termId}` : "/app/assessment"} className="inline-flex items-center gap-1 font-semibold text-ink-2 hover:text-ink">
      <ArrowLeft className="size-4" aria-hidden /> Assessments
    </Link>
  );

  if (sheet.error && !data) {
    return (
      <div className="max-w-4xl">
        <PageHeader title="Scores" back={back} />
        <ErrorAlert error={sheet.error} onRetry={sheet.refetch} />
      </div>
    );
  }
  if (!data || !assessment) return <Loading label="Loading score sheet" />;

  const bands = data.scale?.bands ?? null;

  return (
    <div className="max-w-6xl pb-20">
      <PageHeader
        back={back}
        title={`${assessment.learningAreaName}: ${assessment.title}`}
        description={
          <span className="flex flex-wrap items-center gap-x-2 gap-y-1">
            <Badge tone={ASSESSMENT_STATUS[assessment.status].tone}>{ASSESSMENT_STATUS[assessment.status].label}</Badge>
            <span>
              {assessment.streamName} ({gradeLabel(assessment.gradeLevel)}), {ASSESSMENT_TYPE[assessment.type]}, {formatDate(assessment.assessedOn)}
            </span>
          </span>
        }
      />

      {assessment.status === "RELEASED" ? (
        <Alert tone="info" title="Released" className="mb-4">
          These scores are final and feed into the term&apos;s report cards.
        </Alert>
      ) : (
        <p className="mb-4 max-w-[70ch] text-ink-2">
          {maxScore !== null
            ? `Type each mark out of ${maxScore}. The percent and level update as you type. Press Enter to move to the next learner.`
            : "Choose a level for each learner. Press Enter to move to the next learner."}
        </p>
      )}

      {bands && maxScore !== null ? (
        <details className="mb-4 rounded-md border border-line bg-paper px-4 py-2.5 text-sm">
          <summary className="cursor-pointer font-semibold">{data.scale?.name}</summary>
          <ul className="mt-2 grid grid-cols-2 gap-x-6 gap-y-1 sm:grid-cols-4">
            {bands.map((b) => (
              <li key={b.code} className="tabular">
                <strong>{b.code}</strong> {b.minPercent}–{b.maxPercent}%
              </li>
            ))}
          </ul>
        </details>
      ) : null}

      <Card>
        <div className="hidden border-b border-line-strong px-5 py-2 text-[0.8rem] font-semibold text-ink-2 md:grid md:grid-cols-[minmax(12rem,1.2fr)_8.5rem_8rem_minmax(12rem,1.5fr)] md:gap-x-3">
          <span className="pl-8">Learner</span>
          <span>{maxScore !== null ? `Score / ${maxScore}` : "Level"}</span>
          <span>{maxScore !== null ? "Percent, level" : ""}</span>
          <span>Comment</span>
        </div>
        {rows.length === 0 ? (
          <EmptyState title="No learners in this class">Admit learners into {assessment.streamName} before entering scores.</EmptyState>
        ) : (
          <ol className="divide-y divide-line">
            {rows.map((r, i) => (
              <ScoreEntryRow
                key={r.learnerId}
                row={r}
                index={i}
                draft={drafts[r.learnerId] ?? { raw: "", rubric: "", comment: "" }}
                maxScore={maxScore}
                bands={bands}
                readOnly={readOnly}
                dirty={dirtyIds.includes(r.learnerId)}
                onChange={onChange}
                onNavigate={onNavigate}
                registerField={registerField}
              />
            ))}
          </ol>
        )}
      </Card>

      {!readOnly ? (
        <div className="no-print sticky bottom-0 z-10 -mx-4 mt-4 border-t border-line bg-paper/95 px-4 py-3 backdrop-blur sm:mx-0 sm:rounded-lg sm:border">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <p className="text-sm text-ink-2" aria-live="polite">
              <span className="font-semibold text-ink tabular">
                {scored}/{rows.length}
              </span>{" "}
              scored
              {dirtyIds.length > 0 ? <span className="text-owe">, {dirtyIds.length} unsaved</span> : null}
            </p>
            <div className="flex gap-2">
              {/* Releasing locks scores after moderation, so the API only allows it with ASSESSMENT_MANAGE. */}
              {session.can("ASSESSMENT_MANAGE") ? (
                <Button variant="secondary" icon={<Send className="size-4" aria-hidden />} onClick={() => setConfirmRelease(true)} disabled={dirtyIds.length > 0 || scored === 0}>
                  Release
                </Button>
              ) : null}
              <Button icon={<Save className="size-4" aria-hidden />} onClick={doSave} loading={save.pending} disabled={dirtyIds.length === 0 || invalid}>
                Save
              </Button>
            </div>
          </div>
          {save.error ? <ErrorAlert error={save.error} className="mt-3" /> : null}
        </div>
      ) : null}

      <Dialog
        open={confirmRelease}
        onClose={() => setConfirmRelease(false)}
        title="Release these scores?"
        size="sm"
        footer={
          <>
            <Button variant="ghost" onClick={() => setConfirmRelease(false)}>
              Cancel
            </Button>
            <Button variant="stamp" icon={<Lock className="size-4" aria-hidden />} onClick={doRelease} loading={release.pending}>
              Release scores
            </Button>
          </>
        }
      >
        <div className="flex flex-col gap-3">
          <p>
            {scored} of {rows.length} learners have a {maxScore !== null ? "score" : "level"}.
            {scored < rows.length ? " Learners without one won't have this assessment counted." : ""}
          </p>
          <p className="text-ink-2">Released scores are locked and count towards report cards. They can&apos;t be edited afterwards.</p>
          <ErrorAlert error={release.error} />
        </div>
      </Dialog>
    </div>
  );
}
