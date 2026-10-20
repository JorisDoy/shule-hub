"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useState, type FormEvent } from "react";
import { ArrowLeft, GraduationCap, UserPlus } from "lucide-react";
import type { ApplicationDetail, ApplicationStatus } from "@/lib/api/types";
import { useAction, useQuery } from "@/lib/hooks";
import { APPLICATION_STATUS, GENDER, RELATIONSHIP } from "@/lib/labels";
import { gradeLabel } from "@/lib/grades";
import { addDaysIso, formatDate, formatDateTime, todayIso } from "@/lib/format";
import { formatKenyanPhone } from "@/lib/phone";
import { fieldError } from "@/lib/api/problem";
import { useSession } from "@/features/session/SessionProvider";
import { Alert, Badge, Button, ButtonLink, Card, CardBody, CardHeader, DetailList, Dialog, ErrorAlert, Input, Loading, PageHeader, Select, Textarea, useToast } from "@/components/ui";
import { cx } from "@/lib/cx";

const NEGATIVE: ApplicationStatus[] = ["REJECTED", "WITHDRAWN"];

export default function ApplicationPage() {
  const { id } = useParams<{ id: string }>();
  const session = useSession();
  const { api } = session;
  const app = useQuery(`app:${id}`, (signal) => api.admissions.get(id, { signal }));
  const [transitionTo, setTransitionTo] = useState<ApplicationStatus | null>(null);
  const [admitting, setAdmitting] = useState(false);
  const canManage = session.can("ADMISSIONS_MANAGE");

  const back = (
    <Link href="/app/admissions" className="inline-flex items-center gap-1 font-semibold text-ink-2 hover:text-ink">
      <ArrowLeft className="size-4" aria-hidden /> Admissions
    </Link>
  );

  if (app.error && !app.data) {
    return (
      <div className="max-w-4xl">
        <PageHeader title="Application" back={back} />
        <ErrorAlert error={app.error} onRetry={app.refetch} />
      </div>
    );
  }
  if (!app.data) return <Loading label="Loading application" />;

  const a = app.data;
  const status = APPLICATION_STATUS[a.status];
  const transitions = a.allowedTransitions.filter((t) => t !== "ADMITTED");

  return (
    <div className="max-w-5xl">
      <PageHeader
        back={back}
        title={a.learnerName}
        description={
          <span className="flex flex-wrap items-center gap-x-2 gap-y-1">
            <Badge tone={status.tone}>{status.label}</Badge>
            <span>
              {gradeLabel(a.gradeLevel)}, reference {a.reference}
            </span>
          </span>
        }
      />

      {a.status === "ADMITTED" && a.learnerId ? (
        <Alert tone="success" title={`Admitted as ${a.admissionNumber ?? "a learner"}`} className="mb-5" action={<ButtonLink href={`/app/learners/${a.learnerId}`} size="sm" variant="secondary">Open learner profile</ButtonLink>} />
      ) : null}

      {canManage && (transitions.length > 0 || a.status === "ACCEPTED") ? (
        <Card className="mb-5">
          <CardBody className="flex flex-col gap-3">
            <p className="font-semibold">Next step</p>
            <div className="flex flex-wrap gap-2">
              {a.status === "ACCEPTED" ? (
                <Button icon={<UserPlus className="size-4" aria-hidden />} onClick={() => setAdmitting(true)}>
                  Admit
                </Button>
              ) : null}
              {transitions.map((t) => (
                <Button key={t} variant={NEGATIVE.includes(t) ? "danger" : a.status === "ACCEPTED" ? "secondary" : t === transitions[0] ? "primary" : "secondary"} onClick={() => setTransitionTo(t)}>
                  {APPLICATION_STATUS[t].action}
                </Button>
              ))}
            </div>
          </CardBody>
        </Card>
      ) : null}

      <div className="grid gap-5 lg:grid-cols-[1fr_22rem]">
        <div className="flex flex-col gap-5">
          <Card>
            <CardHeader title="Child" />
            <CardBody>
              <DetailList
                items={[
                  { label: "Name", value: `${a.learner.givenName} ${a.learner.familyName}` },
                  { label: "Date of birth", value: formatDate(a.learner.dateOfBirth) },
                  { label: "Gender", value: GENDER[a.learner.gender] ?? a.learner.gender },
                  { label: "Joining", value: gradeLabel(a.gradeLevel) },
                  { label: "Boarding", value: a.boardingRequested ? "Requested" : "Day scholar" },
                  { label: "Academic year", value: session.years.find((y) => y.id === a.academicYearId)?.year ?? "Not set" },
                ]}
              />
            </CardBody>
          </Card>
          <Card>
            <CardHeader title="Parent or guardian" />
            <CardBody>
              <DetailList
                items={[
                  { label: "Name", value: `${a.guardian.givenName} ${a.guardian.familyName}` },
                  { label: "Relationship", value: RELATIONSHIP[a.guardian.relationship] ?? a.guardian.relationship },
                  {
                    label: "Phone",
                    value: (
                      <a href={`tel:+${a.guardian.phone}`} className="text-uniform-dark underline underline-offset-2">
                        {formatKenyanPhone(a.guardian.phone)}
                      </a>
                    ),
                  },
                  { label: "Email", value: a.guardian.email || "—" },
                ]}
              />
            </CardBody>
          </Card>
          <Card>
            <CardHeader title="Details" />
            <CardBody>
              <DetailList
                items={[
                  { label: "Source", value: a.source || "—" },
                  { label: "Received", value: formatDateTime(a.createdAt) },
                  { label: "Assessment date", value: formatDate(a.assessmentDate) },
                  { label: "Offer expires", value: formatDate(a.offerExpiresOn) },
                ]}
              />
              {a.notes ? (
                <div className="mt-4">
                  <p className="text-[0.8rem] text-ink-3">Notes</p>
                  <p className="whitespace-pre-line">{a.notes}</p>
                </div>
              ) : null}
            </CardBody>
          </Card>
        </div>

        <Card className="self-start">
          <CardHeader title="History" />
          <CardBody>
            {a.history.length === 0 ? (
              <p className="text-ink-2">No changes yet.</p>
            ) : (
              <ol className="relative flex flex-col gap-5 border-l-2 border-rule pl-5">
                {[...a.history].reverse().map((h, i) => (
                  <li key={`${h.occurredAt}-${i}`} className="relative">
                    <span aria-hidden className={cx("absolute top-1.5 -left-[1.6rem] size-3 rounded-full border-2 border-paper", i === 0 ? "bg-uniform" : "bg-line-strong")} />
                    <p className="font-semibold">
                      {h.fromStatus ? `${APPLICATION_STATUS[h.fromStatus]?.label ?? h.fromStatus} to ` : ""}
                      {APPLICATION_STATUS[h.toStatus]?.label ?? h.toStatus}
                    </p>
                    <p className="text-sm text-ink-2">
                      {formatDateTime(h.occurredAt)}
                      {h.actorName ? `, ${h.actorName}` : ""}
                    </p>
                    {h.note ? <p className="mt-1 text-[0.95rem] whitespace-pre-line">{h.note}</p> : null}
                  </li>
                ))}
              </ol>
            )}
          </CardBody>
        </Card>
      </div>

      {transitionTo ? (
        <TransitionDialog
          application={a}
          to={transitionTo}
          onClose={() => setTransitionTo(null)}
          onDone={(updated) => {
            app.setData(updated);
            setTransitionTo(null);
          }}
        />
      ) : null}
      {admitting ? <AdmitDialog application={a} onClose={() => setAdmitting(false)} onAdmitted={() => app.refetch()} /> : null}
    </div>
  );
}

function TransitionDialog({ application, to, onClose, onDone }: { application: ApplicationDetail; to: ApplicationStatus; onClose: () => void; onDone: (a: ApplicationDetail) => void }) {
  const { api } = useSession();
  const toast = useToast();
  const [note, setNote] = useState("");
  const [assessmentDate, setAssessmentDate] = useState(() => addDaysIso(todayIso(), 7));
  const [offerExpiresOn, setOfferExpiresOn] = useState(() => addDaysIso(todayIso(), 14));
  const [localError, setLocalError] = useState<string | null>(null);
  const action = useAction((body: Parameters<typeof api.admissions.transition>[1]) => api.admissions.transition(application.id, body));
  const label = APPLICATION_STATUS[to];
  const needsAssessmentDate = to === "ASSESSMENT_SCHEDULED";
  const needsOfferDate = to === "OFFERED";

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (needsAssessmentDate && !assessmentDate) return setLocalError("Choose the assessment date");
    if (needsOfferDate && !offerExpiresOn) return setLocalError("Choose when the offer expires");
    setLocalError(null);
    const updated = await action.run({
      to,
      note: note.trim() || undefined,
      assessmentDate: needsAssessmentDate ? assessmentDate : undefined,
      offerExpiresOn: needsOfferDate ? offerExpiresOn : undefined,
    });
    if (updated) {
      toast.show(`${application.learnerName}: ${label.label.toLowerCase()}`);
      onDone(updated);
    }
  }

  return (
    <Dialog
      open
      onClose={onClose}
      title={label.action}
      description={`${application.learnerName}, ${gradeLabel(application.gradeLevel)}`}
      size="sm"
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" form="transition-form" loading={action.pending} variant={NEGATIVE.includes(to) ? "danger" : "primary"}>
            {label.action}
          </Button>
        </>
      }
    >
      <form id="transition-form" onSubmit={submit} noValidate className="flex flex-col gap-4">
        {needsAssessmentDate ? (
          <Input label="Assessment date" type="date" value={assessmentDate} onChange={(e) => setAssessmentDate(e.target.value)} error={fieldError(action.error, "assessmentDate") ?? (localError && !assessmentDate ? localError : undefined)} required />
        ) : null}
        {needsOfferDate ? (
          <Input label="Offer expires on" type="date" min={todayIso()} value={offerExpiresOn} onChange={(e) => setOfferExpiresOn(e.target.value)} error={fieldError(action.error, "offerExpiresOn") ?? (localError && !offerExpiresOn ? localError : undefined)} hint="The parent must accept by this date" required />
        ) : null}
        <Textarea label="Note" hint="Visible in the application history" value={note} onChange={(e) => setNote(e.target.value)} error={fieldError(action.error, "note")} />
        <ErrorAlert error={action.error && !fieldError(action.error, "assessmentDate", "offerExpiresOn", "note") ? action.error : null} />
      </form>
    </Dialog>
  );
}

function AdmitDialog({ application, onClose, onAdmitted }: { application: ApplicationDetail; onClose: () => void; onAdmitted: () => void }) {
  const { api, years, currentYear } = useSession();
  const yearId = application.academicYearId ?? currentYear?.id;
  const streams = useQuery(yearId ? `streams:${application.branchId}:${yearId}` : null, (signal) => api.academics.streams({ branchId: application.branchId, academicYearId: yearId }, { signal }));
  const matching = (streams.data ?? []).filter((s) => s.gradeLevel === application.gradeLevel);
  const [streamId, setStreamId] = useState("");
  const admit = useAction((sid: string) => api.admissions.admit(application.id, sid));
  const [result, setResult] = useState<{ learnerId: string; admissionNumber: string } | null>(null);
  const yearLabel = years.find((y) => y.id === yearId)?.year;
  const chosen = streamId || (matching.length === 1 ? matching[0].id : "");

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (!chosen) return;
    const r = await admit.run(chosen);
    if (r) {
      setResult(r);
      onAdmitted();
    }
  }

  return (
    <Dialog
      open
      onClose={onClose}
      title={result ? "Admitted" : "Admit learner"}
      description={`${application.learnerName}, ${gradeLabel(application.gradeLevel)}`}
      size="sm"
      footer={
        result ? (
          <>
            <Button variant="ghost" onClick={onClose}>
              Close
            </Button>
            <ButtonLink href={`/app/learners/${result.learnerId}`} icon={<GraduationCap className="size-4" aria-hidden />}>
              Open learner profile
            </ButtonLink>
          </>
        ) : (
          <>
            <Button variant="ghost" onClick={onClose}>
              Cancel
            </Button>
            <Button type="submit" form="admit-form" loading={admit.pending} disabled={!chosen}>
              Admit to stream
            </Button>
          </>
        )
      }
    >
      {result ? (
        <div className="paper-margin rounded-md border border-line py-5 pr-4 pl-14" aria-live="polite">
          <p className="text-sm text-ink-2">Admission number</p>
          <p className="font-display text-3xl font-bold tracking-wide">{result.admissionNumber}</p>
          <p className="mt-2 text-ink-2">Write this on the learner&apos;s file. Invoices and report cards will use it.</p>
        </div>
      ) : (
        <form id="admit-form" onSubmit={submit} className="flex flex-col gap-4">
          {!yearId ? (
            <Alert tone="warning">Set up an academic year in Settings before admitting learners.</Alert>
          ) : streams.loading ? (
            <Loading label="Loading streams" className="px-0 py-2" />
          ) : streams.error ? (
            <ErrorAlert error={streams.error} onRetry={streams.refetch} />
          ) : matching.length === 0 ? (
            <Alert tone="warning" title={`No ${gradeLabel(application.gradeLevel)} stream for ${yearLabel ?? "this year"}`}>
              Create a stream for this grade in Settings, then come back to admit.
            </Alert>
          ) : (
            <Select
              label="Stream"
              placeholder={matching.length > 1 ? "Choose stream" : undefined}
              options={matching.map((s) => ({ value: s.id, label: `${s.name} (capacity ${s.capacity})` }))}
              value={chosen}
              onChange={(e) => setStreamId(e.target.value)}
              error={fieldError(admit.error, "streamId")}
              required
            />
          )}
          <ErrorAlert error={admit.error && !fieldError(admit.error, "streamId") ? admit.error : null} />
        </form>
      )}
    </Dialog>
  );
}
