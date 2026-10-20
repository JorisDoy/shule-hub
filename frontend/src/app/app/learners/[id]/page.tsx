"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useState, type FormEvent } from "react";
import { ArrowLeft, BookOpenCheck, Pencil, Printer, Smartphone } from "lucide-react";
import type { BoardingStatus, LearnerDetail } from "@/lib/api/types";
import { useAction, useQuery } from "@/lib/hooks";
import { fieldError } from "@/lib/api/problem";
import { gradeLabel } from "@/lib/grades";
import { BOARDING, GENDER, LEARNER_STATUS, RELATIONSHIP } from "@/lib/labels";
import { formatDate } from "@/lib/format";
import { formatKenyanPhone } from "@/lib/phone";
import { useSession } from "@/features/session/SessionProvider";
import { MpesaPayDialog } from "@/features/payments/MpesaPayDialog";
import { BalanceFigure, StatementView } from "@/features/fees/StatementView";
import { Badge, Button, ButtonLink, Card, CardBody, CardHeader, DetailList, Dialog, ErrorAlert, Input, Loading, PageHeader, Select, useToast } from "@/components/ui";

export default function LearnerPage() {
  const { id } = useParams<{ id: string }>();
  const session = useSession();
  const { api, currentTerm } = session;
  const learner = useQuery(`learner:${id}`, (signal) => api.learners.get(id, { signal }));
  const canFees = session.can("FEES_VIEW");
  const statement = useQuery(canFees ? `statement:${id}` : null, (signal) => api.fees.statement(id, { signal }));
  const [editing, setEditing] = useState(false);
  const [paying, setPaying] = useState(false);

  const back = (
    <Link href="/app/learners" className="inline-flex items-center gap-1 font-semibold text-ink-2 hover:text-ink">
      <ArrowLeft className="size-4" aria-hidden /> Learners
    </Link>
  );

  if (learner.error && !learner.data) {
    return (
      <div className="max-w-4xl">
        <PageHeader title="Learner" back={back} />
        <ErrorAlert error={learner.error} onRetry={learner.refetch} />
      </div>
    );
  }
  if (!learner.data) return <Loading label="Loading learner" />;
  const l = learner.data;
  const canReport = session.canAny("ASSESSMENT_ENTER", "ASSESSMENT_MANAGE") && l.streamId && currentTerm;

  return (
    <div className="max-w-5xl">
      <PageHeader
        back={back}
        title={l.fullName}
        description={
          <span className="flex flex-wrap items-center gap-x-2 gap-y-1">
            <span className="font-semibold text-ink tabular">{l.admissionNumber}</span>
            <span>{l.streamName ?? gradeLabel(l.gradeLevel)}</span>
            <Badge tone={LEARNER_STATUS[l.status].tone}>{LEARNER_STATUS[l.status].label}</Badge>
          </span>
        }
        actions={
          canReport ? (
            <ButtonLink href={`/app/assessment/report-cards?streamId=${l.streamId}&termId=${currentTerm.id}&learnerId=${l.id}`} variant="secondary" icon={<BookOpenCheck className="size-4" aria-hidden />}>
              Report cards
            </ButtonLink>
          ) : null
        }
      />

      <div className="grid gap-5 lg:grid-cols-[1fr_1fr]">
        <Card>
          <CardHeader
            title="Profile"
            actions={
              session.can("LEARNERS_EDIT") ? (
                <Button size="sm" variant="secondary" icon={<Pencil className="size-3.5" aria-hidden />} onClick={() => setEditing(true)}>
                  Edit
                </Button>
              ) : null
            }
          />
          <CardBody>
            <DetailList
              items={[
                { label: "Full name", value: [l.givenName, l.otherNames, l.familyName].filter(Boolean).join(" ") },
                { label: "Date of birth", value: formatDate(l.dateOfBirth) },
                { label: "Gender", value: l.gender ? GENDER[l.gender] : "—" },
                { label: "Grade", value: gradeLabel(l.gradeLevel) },
                { label: "Stream", value: l.streamName ?? "Not placed" },
                { label: "Day or boarding", value: BOARDING[l.boardingStatus] },
                { label: "UPI", value: l.upi || <span className="font-normal text-ink-3">Not recorded</span> },
                { label: "KNEC assessment no.", value: l.assessmentNumber || <span className="font-normal text-ink-3">Not recorded</span> },
                { label: "Admitted", value: formatDate(l.admittedOn) },
              ]}
            />
          </CardBody>
        </Card>

        <Card>
          <CardHeader title="Guardians" />
          {l.guardians.length === 0 ? (
            <CardBody>
              <p className="text-ink-2">No guardians recorded.</p>
            </CardBody>
          ) : (
            <ul className="divide-y divide-line">
              {l.guardians.map((g) => (
                <li key={g.personId} className="px-4 py-3 sm:px-5">
                  <p className="font-semibold">
                    {g.fullName} <span className="font-normal text-ink-2">({RELATIONSHIP[g.relationship]})</span>
                  </p>
                  <p className="text-[0.95rem]">
                    {g.phone ? (
                      <a href={`tel:+${g.phone}`} className="text-uniform-dark underline underline-offset-2">
                        {formatKenyanPhone(g.phone)}
                      </a>
                    ) : (
                      "No phone"
                    )}
                    {g.email ? <span className="text-ink-2">, {g.email}</span> : null}
                  </p>
                  <p className="mt-1 flex flex-wrap gap-1.5">
                    {g.primaryContact ? <Badge tone="info">Primary contact</Badge> : null}
                    {g.feePayer ? <Badge tone="green">Pays fees</Badge> : null}
                    {g.canPickUp ? <Badge>Can pick up</Badge> : null}
                  </p>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>

      {canFees ? (
        <Card className="mt-5">
          <CardHeader
            title="Fee statement"
            description={statement.data ? <BalanceFigure balance={statement.data.balance} /> : undefined}
            actions={
              <>
                <Button size="sm" variant="ghost" icon={<Printer className="size-4" aria-hidden />} onClick={() => window.print()} disabled={!statement.data}>
                  Print
                </Button>
                {session.can("FEES_MANAGE") ? (
                  <Button size="sm" icon={<Smartphone className="size-4" aria-hidden />} onClick={() => setPaying(true)}>
                    Request M-Pesa payment
                  </Button>
                ) : null}
              </>
            }
          />
          <CardBody>
            {statement.error ? (
              <ErrorAlert error={statement.error} onRetry={statement.refetch} />
            ) : !statement.data ? (
              <Loading label="Loading statement" className="px-0" />
            ) : (
              <div className="print-area">
                <StatementView statement={statement.data} schoolName={session.membership.tenantName} />
              </div>
            )}
          </CardBody>
        </Card>
      ) : null}

      {editing ? <EditLearnerDialog learner={l} onClose={() => setEditing(false)} onSaved={(updated) => { learner.setData(updated); setEditing(false); }} /> : null}
      {session.can("FEES_MANAGE") ? (
        <MpesaPayDialog
          open={paying}
          onClose={() => setPaying(false)}
          learnerName={l.fullName}
          balance={statement.data?.balance}
          initiate={(amount, phone) => api.fees.stk({ learnerId: l.id, amount, phone })}
          poll={(intentId, signal) => api.fees.stkStatus(intentId, { signal })}
          onPaid={statement.refetch}
        />
      ) : null}
    </div>
  );
}

function EditLearnerDialog({ learner, onClose, onSaved }: { learner: LearnerDetail; onClose: () => void; onSaved: (l: LearnerDetail) => void }) {
  const { api, branches } = useSession();
  const toast = useToast();
  const [upi, setUpi] = useState(learner.upi ?? "");
  const [assessmentNumber, setAssessmentNumber] = useState(learner.assessmentNumber ?? "");
  const [boarding, setBoarding] = useState<BoardingStatus>(learner.boardingStatus);
  const save = useAction((body: Parameters<typeof api.learners.update>[1]) => api.learners.update(learner.id, body));
  const boardingEnabled = branches.find((b) => b.id === learner.branchId)?.boardingEnabled ?? true;

  async function submit(e: FormEvent) {
    e.preventDefault();
    const body: Parameters<typeof api.learners.update>[1] = {};
    if (upi.trim() !== (learner.upi ?? "")) body.upi = upi.trim();
    if (assessmentNumber.trim() !== (learner.assessmentNumber ?? "")) body.assessmentNumber = assessmentNumber.trim();
    if (boarding !== learner.boardingStatus) body.boardingStatus = boarding;
    if (Object.keys(body).length === 0) return onClose();
    const updated = await save.run(body);
    if (updated) {
      toast.show("Learner details saved");
      onSaved(updated);
    }
  }

  return (
    <Dialog
      open
      onClose={onClose}
      title="Edit learner details"
      description={learner.fullName}
      size="sm"
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" form="edit-learner" loading={save.pending}>
            Save details
          </Button>
        </>
      }
    >
      <form id="edit-learner" onSubmit={submit} className="flex flex-col gap-4">
        <Input label="UPI" hint="Unique Personal Identifier from NEMIS" value={upi} onChange={(e) => setUpi(e.target.value.toUpperCase())} error={fieldError(save.error, "upi")} autoComplete="off" />
        <Input label="KNEC assessment number" value={assessmentNumber} onChange={(e) => setAssessmentNumber(e.target.value.toUpperCase())} error={fieldError(save.error, "assessmentNumber")} autoComplete="off" />
        <Select
          label="Day or boarding"
          options={[
            { value: "DAY", label: "Day scholar" },
            { value: "BOARDING", label: "Boarder", disabled: !boardingEnabled && learner.boardingStatus !== "BOARDING" },
          ]}
          value={boarding}
          onChange={(e) => setBoarding(e.target.value as BoardingStatus)}
          hint={!boardingEnabled ? "This branch doesn't offer boarding" : "Changes which fee items apply from the next invoice"}
          error={fieldError(save.error, "boardingStatus")}
        />
        <ErrorAlert error={save.error && !fieldError(save.error, "upi", "assessmentNumber", "boardingStatus") ? save.error : null} />
      </form>
    </Dialog>
  );
}
