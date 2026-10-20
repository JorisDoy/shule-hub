"use client";

import Link from "next/link";
import { Suspense, useState, type FormEvent } from "react";
import { BookOpenCheck, FileBadge2, Plus } from "lucide-react";
import { ASSESSMENT_TYPES, type Assessment, type AssessmentType, type Stream } from "@/lib/api/types";
import { useAction, useQuery } from "@/lib/hooks";
import { fieldError } from "@/lib/api/problem";
import { formatDate, todayIso } from "@/lib/format";
import { gradeLabel, levelBandFor, LEVEL_BAND_LABELS } from "@/lib/grades";
import { ASSESSMENT_STATUS, ASSESSMENT_TYPE } from "@/lib/labels";
import { useSession } from "@/features/session/SessionProvider";
import { useStreamTerm } from "@/features/assessment/useStreamTerm";
import { StreamTermPicker } from "@/features/assessment/StreamTermPicker";
import { Alert, Badge, Button, ButtonLink, Card, Dialog, EmptyState, ErrorAlert, Input, Loading, PageHeader, Select, SkeletonRows, useToast } from "@/components/ui";

export default function AssessmentPageWrapper() {
  return (
    <Suspense fallback={<Loading />}>
      <AssessmentPage />
    </Suspense>
  );
}

function AssessmentPage() {
  const session = useSession();
  const { api } = session;
  const st = useStreamTerm();
  const [creating, setCreating] = useState(false);
  const assessments = useQuery(st.streamId && st.termId ? `assessments:${st.streamId}:${st.termId}` : null, (signal) =>
    api.assessment.list({ streamId: st.streamId, termId: st.termId }, { signal }),
  );
  const list = [...(assessments.data ?? [])].sort((a, b) => b.assessedOn.localeCompare(a.assessedOn) || a.learningAreaName.localeCompare(b.learningAreaName));

  return (
    <div className="max-w-6xl">
      <PageHeader
        title="Assessment"
        description="Record CATs, end-of-term exams, observations and projects. Report cards are built from released assessments."
        actions={
          st.streamId && st.termId ? (
            <ButtonLink href={`/app/assessment/report-cards?streamId=${st.streamId}&termId=${st.termId}`} variant="secondary" icon={<FileBadge2 className="size-4" aria-hidden />}>
              Report cards
            </ButtonLink>
          ) : null
        }
      />

      {!st.year && !session.yearsLoading ? (
        <Alert tone="warning" title="No academic year set up">
          Add the academic year, its terms and its classes in Settings first.
        </Alert>
      ) : (
        <>
          <div className="mb-4 flex flex-wrap items-end justify-between gap-3">
            <StreamTermPicker
              streams={st.streamList}
              terms={st.terms}
              streamId={st.streamId}
              termId={st.termId}
              year={st.year?.year}
              loading={st.streams.loading}
              onChange={(p) => st.setParams(p)}
            />
            {st.stream && st.termId ? (
              <Button icon={<Plus className="size-4" aria-hidden />} onClick={() => setCreating(true)}>
                New assessment
              </Button>
            ) : null}
          </div>
          {st.streams.error ? <ErrorAlert error={st.streams.error} onRetry={st.streams.refetch} className="mb-4" /> : null}

          <Card>
            {!st.streamId ? (
              <EmptyState icon={<BookOpenCheck className="size-8" aria-hidden />} title="Choose a class">
                Pick a class and term to see its assessments.
              </EmptyState>
            ) : assessments.error ? (
              <div className="p-4">
                <ErrorAlert error={assessments.error} onRetry={assessments.refetch} />
              </div>
            ) : assessments.loading && !assessments.data ? (
              <SkeletonRows />
            ) : list.length === 0 ? (
              <EmptyState
                icon={<BookOpenCheck className="size-8" aria-hidden />}
                title="No assessments this term"
                action={
                  <Button variant="secondary" onClick={() => setCreating(true)}>
                    Create the first assessment
                  </Button>
                }
              >
                Create one per learning area, then enter scores or rubric levels for the class.
              </EmptyState>
            ) : (
              <ul className="divide-y divide-line">
                {list.map((a) => (
                  <AssessmentItem key={a.id} a={a} />
                ))}
              </ul>
            )}
          </Card>
        </>
      )}

      {creating && st.stream && st.termId ? (
        <CreateAssessmentDialog
          stream={st.stream}
          termId={st.termId}
          onClose={() => setCreating(false)}
          onCreated={(a) => {
            assessments.setData((prev) => [a, ...(prev ?? [])]);
            setCreating(false);
          }}
        />
      ) : null}
    </div>
  );
}

function AssessmentItem({ a }: { a: Assessment }) {
  const progress = a.learnerCount > 0 ? Math.round((a.scoredCount / a.learnerCount) * 100) : 0;
  return (
    <li>
      <Link href={`/app/assessment/${a.id}`} className="flex flex-wrap items-center gap-x-4 gap-y-2 px-4 py-3.5 hover:bg-canvas sm:px-5">
        <div className="min-w-0 flex-1 basis-60">
          <p className="font-semibold text-ink">
            {a.learningAreaName}: {a.title}
          </p>
          <p className="text-sm text-ink-2">
            {ASSESSMENT_TYPE[a.type]}, {formatDate(a.assessedOn)}, {a.maxScore !== null ? `out of ${a.maxScore}` : "rubric levels"}
          </p>
        </div>
        <div className="flex items-center gap-3">
          <div className="w-28">
            <p className="text-right text-sm text-ink-2 tabular">
              {a.scoredCount}/{a.learnerCount} scored
            </p>
            <div className="mt-1 h-1.5 overflow-hidden rounded-full bg-ink/[0.08]" aria-hidden>
              <div className="h-full rounded-full bg-uniform" style={{ width: `${progress}%` }} />
            </div>
          </div>
          <Badge tone={ASSESSMENT_STATUS[a.status].tone}>{ASSESSMENT_STATUS[a.status].label}</Badge>
        </div>
      </Link>
    </li>
  );
}

function CreateAssessmentDialog({ stream, termId, onClose, onCreated }: { stream: Stream; termId: string; onClose: () => void; onCreated: (a: Assessment) => void }) {
  const { api } = useSession();
  const toast = useToast();
  const levelBand = levelBandFor(stream.gradeLevel);
  const areas = useQuery(`areas:${levelBand}`, (signal) => api.academics.learningAreas(levelBand, { signal }));
  const [title, setTitle] = useState("");
  const [type, setType] = useState<AssessmentType>("CAT");
  const [learningAreaId, setLearningAreaId] = useState("");
  const [maxScore, setMaxScore] = useState("");
  const [weight, setWeight] = useState("1");
  const [assessedOn, setAssessedOn] = useState(() => todayIso());
  const [errors, setErrors] = useState<Record<string, string>>({});
  const create = useAction(api.assessment.create);
  const sortedAreas = [...(areas.data ?? [])].sort((a, b) => a.sortOrder - b.sortOrder);

  async function submit(e: FormEvent) {
    e.preventDefault();
    const next: Record<string, string> = {};
    if (!title.trim()) next.title = "Give it a title, e.g. Week 4 CAT";
    if (!learningAreaId) next.learningAreaId = "Choose a learning area";
    const max = maxScore.trim() === "" ? undefined : Number(maxScore);
    if (max !== undefined && (!Number.isFinite(max) || max <= 0)) next.maxScore = "Enter a number above 0, or leave blank for rubric levels";
    const w = Number(weight);
    if (!Number.isFinite(w) || w <= 0) next.weight = "Enter a weight above 0";
    if (!assessedOn) next.assessedOn = "Choose the date";
    setErrors(next);
    if (Object.keys(next).length > 0) return;
    const created = await create.run({ title: title.trim(), type, termId, streamId: stream.id, learningAreaId, maxScore: max, weight: w, assessedOn });
    if (created) {
      toast.show("Assessment created");
      onCreated(created);
    }
  }

  return (
    <Dialog
      open
      onClose={onClose}
      title="New assessment"
      description={`${stream.name}, ${gradeLabel(stream.gradeLevel)} (${LEVEL_BAND_LABELS[levelBand]})`}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" form="new-assessment" loading={create.pending}>
            Create assessment
          </Button>
        </>
      }
    >
      <form id="new-assessment" noValidate onSubmit={submit} className="grid gap-4 sm:grid-cols-2">
        <Select
          label="Learning area"
          containerClassName="sm:col-span-2"
          placeholder={areas.loading ? "Loading…" : "Choose learning area"}
          options={sortedAreas.map((a) => ({ value: a.id, label: a.name }))}
          value={learningAreaId}
          onChange={(e) => setLearningAreaId(e.target.value)}
          error={errors.learningAreaId ?? fieldError(create.error, "learningAreaId")}
          required
        />
        {areas.error ? <ErrorAlert error={areas.error} onRetry={areas.refetch} className="sm:col-span-2" /> : null}
        <Input label="Title" containerClassName="sm:col-span-2" value={title} onChange={(e) => setTitle(e.target.value)} error={errors.title ?? fieldError(create.error, "title")} placeholder="Week 4 CAT" required />
        <Select label="Type" options={ASSESSMENT_TYPES.map((t) => ({ value: t, label: ASSESSMENT_TYPE[t] }))} value={type} onChange={(e) => setType(e.target.value as AssessmentType)} error={fieldError(create.error, "type")} />
        <Input label="Date" type="date" value={assessedOn} onChange={(e) => setAssessedOn(e.target.value)} error={errors.assessedOn ?? fieldError(create.error, "assessedOn")} required />
        <Input
          label="Marked out of"
          inputMode="decimal"
          value={maxScore}
          onChange={(e) => setMaxScore(e.target.value)}
          error={errors.maxScore ?? fieldError(create.error, "maxScore")}
          hint="Leave blank to record a rubric level (EE, ME, AE, BE) instead of marks"
        />
        <Input label="Weight" inputMode="decimal" value={weight} onChange={(e) => setWeight(e.target.value)} error={errors.weight ?? fieldError(create.error, "weight")} hint="How much it counts towards the term result" required />
        <ErrorAlert className="sm:col-span-2" error={create.error && Object.keys((create.error as { fieldErrors?: object }).fieldErrors ?? {}).length === 0 ? create.error : null} />
      </form>
    </Dialog>
  );
}
