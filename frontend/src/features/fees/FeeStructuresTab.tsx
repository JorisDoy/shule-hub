"use client";

import { useState, type FormEvent } from "react";
import { FileStack, Plus, Trash2 } from "lucide-react";
import type { FeeAppliesTo, FeeStructure, GradeLevel } from "@/lib/api/types";
import { useAction, useQuery } from "@/lib/hooks";
import { fieldError } from "@/lib/api/problem";
import { formatKes, parseMoneyInput } from "@/lib/format";
import { GRADE_LEVELS } from "@/lib/api/types";
import { GRADE_OPTIONS, gradeLabel, isGradeLevel } from "@/lib/grades";
import { APPLIES_TO, FEE_STRUCTURE_STATUS } from "@/lib/labels";
import { useSession } from "@/features/session/SessionProvider";
import type { TermOption } from "@/features/academics/terms";
import { Alert, Badge, Button, Dialog, EmptyState, ErrorAlert, Input, Select, SkeletonRows, Table, TBody, Td, Th, THead, useToast } from "@/components/ui";

export function FeeStructuresTab({ termId, terms }: { termId: string; terms: TermOption[] }) {
  const session = useSession();
  const { api, branchId } = session;
  const canManage = session.can("FEES_MANAGE");
  const structures = useQuery(branchId && termId ? `structures:${branchId}:${termId}` : null, (signal) => api.fees.structures({ branchId, termId }, { signal }));
  const [editing, setEditing] = useState<FeeStructure | "new" | null>(null);
  const [publishing, setPublishing] = useState<FeeStructure | null>(null);
  const toast = useToast();
  const publish = useAction(api.fees.publishStructure);
  const term = terms.find((t) => t.id === termId);

  const list = [...(structures.data ?? [])].sort((a, b) => GRADE_LEVELS.indexOf(a.gradeLevel) - GRADE_LEVELS.indexOf(b.gradeLevel));

  async function confirmPublish() {
    if (!publishing) return;
    const updated = await publish.run(publishing.id);
    if (updated) {
      structures.setData((prev) => (prev ?? []).map((s) => (s.id === updated.id ? updated : s)));
      toast.show(`${updated.name} published`);
      setPublishing(null);
    }
  }

  return (
    <div>
      <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
        <p className="text-ink-2">One fee structure per grade for {term?.label ?? "the term"}. Publish a structure before generating invoices.</p>
        {canManage ? (
          <Button icon={<Plus className="size-4" aria-hidden />} onClick={() => setEditing("new")} disabled={!termId}>
            New fee structure
          </Button>
        ) : null}
      </div>
      {structures.error ? (
        <ErrorAlert error={structures.error} onRetry={structures.refetch} />
      ) : structures.loading && !structures.data ? (
        <SkeletonRows />
      ) : list.length === 0 ? (
        <EmptyState icon={<FileStack className="size-8" aria-hidden />} title="No fee structures for this term">
          {canManage ? "Create one per grade: tuition, lunch, transport and the other vote heads parents pay." : "The bursar hasn't set up fees for this term yet."}
        </EmptyState>
      ) : (
        <Table caption="Fee structures" className="-mx-4 sm:-mx-5">
          <THead>
            <tr>
              <Th>Grade</Th>
              <Th>Name</Th>
              <Th numeric>Day scholar</Th>
              <Th numeric>Boarder</Th>
              <Th>Status</Th>
              {canManage ? <Th className="text-right">Actions</Th> : null}
            </tr>
          </THead>
          <TBody>
            {list.map((s) => (
              <tr key={s.id}>
                <Td className="font-semibold whitespace-nowrap">{gradeLabel(s.gradeLevel)}</Td>
                <Td>
                  {s.name}
                  <span className="block text-sm text-ink-2">{s.items.length} items</span>
                </Td>
                <Td numeric>{formatKes(s.dayTotal)}</Td>
                <Td numeric>{formatKes(s.boardingTotal)}</Td>
                <Td>
                  <Badge tone={FEE_STRUCTURE_STATUS[s.status].tone}>{FEE_STRUCTURE_STATUS[s.status].label}</Badge>
                </Td>
                {canManage ? (
                  <Td className="text-right whitespace-nowrap">
                    <div className="flex justify-end gap-1.5">
                      <Button size="sm" variant="secondary" onClick={() => setEditing(s)}>
                        {s.status === "DRAFT" ? "Edit" : "View"}
                      </Button>
                      {s.status === "DRAFT" ? (
                        <Button size="sm" onClick={() => setPublishing(s)}>
                          Publish
                        </Button>
                      ) : null}
                    </div>
                  </Td>
                ) : null}
              </tr>
            ))}
          </TBody>
        </Table>
      )}

      {editing ? (
        <StructureDialog
          structure={editing === "new" ? null : editing}
          termId={termId}
          termLabel={term?.label}
          takenGrades={list.map((s) => s.gradeLevel)}
          onClose={() => setEditing(null)}
          onSaved={(saved) => {
            structures.setData((prev) => {
              const all = prev ?? [];
              return all.some((s) => s.id === saved.id) ? all.map((s) => (s.id === saved.id ? saved : s)) : [...all, saved];
            });
            setEditing(null);
          }}
        />
      ) : null}

      <Dialog
        open={!!publishing}
        onClose={() => setPublishing(null)}
        title="Publish fee structure?"
        size="sm"
        footer={
          <>
            <Button variant="ghost" onClick={() => setPublishing(null)}>
              Cancel
            </Button>
            <Button onClick={confirmPublish} loading={publish.pending}>
              Publish
            </Button>
          </>
        }
      >
        {publishing ? (
          <div className="flex flex-col gap-3">
            <p>
              <strong>{publishing.name}</strong> for {gradeLabel(publishing.gradeLevel)}: {formatKes(publishing.dayTotal)} day, {formatKes(publishing.boardingTotal)} boarding.
            </p>
            <p className="text-ink-2">Once published it can no longer be edited, and invoices can be generated from it.</p>
            <ErrorAlert error={publish.error} />
          </div>
        ) : null}
      </Dialog>
    </div>
  );
}

interface Row {
  key: number;
  voteHeadId: string;
  amount: string;
  appliesTo: FeeAppliesTo;
}

function StructureDialog({
  structure,
  termId,
  termLabel,
  takenGrades,
  onClose,
  onSaved,
}: {
  structure: FeeStructure | null;
  termId: string;
  termLabel?: string;
  takenGrades: GradeLevel[];
  onClose: () => void;
  onSaved: (s: FeeStructure) => void;
}) {
  const { api, branchId, branch } = useSession();
  const toast = useToast();
  const voteHeads = useQuery("vote-heads", (signal) => api.fees.voteHeads({ signal }));
  const readOnly = structure?.status === "PUBLISHED";
  const [grade, setGrade] = useState<string>(structure?.gradeLevel ?? "");
  const [name, setName] = useState(structure?.name ?? "");
  const [nextKey, setNextKey] = useState(100);
  const [rows, setRows] = useState<Row[]>(() =>
    structure?.items.length
      ? structure.items.map((i, k) => ({ key: k, voteHeadId: i.voteHeadId, amount: String(i.amount), appliesTo: i.appliesTo }))
      : [{ key: 0, voteHeadId: "", amount: "", appliesTo: "ALL" }],
  );
  const [errors, setErrors] = useState<Record<string, string>>({});
  const save = useAction((body: Parameters<typeof api.fees.createStructure>[0]) => (structure ? api.fees.updateStructure(structure.id, body) : api.fees.createStructure(body)));

  const heads = [...(voteHeads.data ?? [])].sort((a, b) => a.priority - b.priority);
  const amounts = rows.map((r) => parseMoneyInput(r.amount) ?? 0);
  const dayTotal = rows.reduce((s, r, i) => s + (r.appliesTo !== "BOARDING" ? amounts[i] : 0), 0);
  const boardingTotal = rows.reduce((s, r, i) => s + (r.appliesTo !== "DAY" ? amounts[i] : 0), 0);

  const updateRow = (key: number, patch: Partial<Row>) => setRows((rs) => rs.map((r) => (r.key === key ? { ...r, ...patch } : r)));

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (!branchId) return;
    const next: Record<string, string> = {};
    if (!isGradeLevel(grade)) next.grade = "Choose a grade";
    if (!name.trim()) next.name = "Give the structure a name, e.g. Grade 4 Term 1 2027";
    if (rows.length === 0) next.rows = "Add at least one fee item";
    rows.forEach((r) => {
      if (!r.voteHeadId) next[`vh-${r.key}`] = "Choose a vote head";
      const amt = parseMoneyInput(r.amount);
      if (amt === null || amt <= 0) next[`amt-${r.key}`] = "Enter an amount above 0";
    });
    const ids = rows.map((r) => `${r.voteHeadId}:${r.appliesTo}`);
    if (new Set(ids).size !== ids.length) next.rows = "Each vote head can appear once per day/boarding option";
    setErrors(next);
    if (Object.keys(next).length > 0) return;
    const saved = await save.run({
      branchId,
      termId,
      gradeLevel: grade as GradeLevel,
      name: name.trim(),
      items: rows.map((r) => ({ voteHeadId: r.voteHeadId, amount: parseMoneyInput(r.amount) ?? 0, appliesTo: r.appliesTo })),
    });
    if (saved) {
      toast.show(structure ? "Fee structure saved" : "Fee structure created as a draft");
      onSaved(saved);
    }
  }

  const gradeOptions = GRADE_OPTIONS.map((o) => ({ ...o, disabled: o.value !== structure?.gradeLevel && takenGrades.includes(o.value) }));

  return (
    <Dialog
      open
      onClose={onClose}
      size="lg"
      title={readOnly ? structure?.name ?? "Fee structure" : structure ? "Edit fee structure" : "New fee structure"}
      description={[termLabel, branch?.name].filter(Boolean).join(", ")}
      footer={
        readOnly ? (
          <Button variant="secondary" onClick={onClose}>
            Close
          </Button>
        ) : (
          <>
            <Button variant="ghost" onClick={onClose}>
              Cancel
            </Button>
            <Button type="submit" form="structure-form" loading={save.pending}>
              {structure ? "Save changes" : "Save draft"}
            </Button>
          </>
        )
      }
    >
      <form id="structure-form" noValidate onSubmit={submit} className="flex flex-col gap-5">
        {readOnly ? <Alert tone="info">This structure is published, so it can&apos;t be changed.</Alert> : null}
        <div className="grid gap-4 sm:grid-cols-2">
          <Select label="Grade" placeholder="Choose grade" options={gradeOptions} value={grade} onChange={(e) => setGrade(e.target.value)} error={errors.grade ?? fieldError(save.error, "gradeLevel")} disabled={readOnly || !!structure} required />
          <Input label="Name" value={name} onChange={(e) => setName(e.target.value)} error={errors.name ?? fieldError(save.error, "name")} disabled={readOnly} placeholder="Grade 4 fees, Term 1" required />
        </div>

        <fieldset>
          <legend className="mb-2 font-display text-base font-semibold">Fee items</legend>
          {voteHeads.error ? <ErrorAlert error={voteHeads.error} onRetry={voteHeads.refetch} /> : null}
          {voteHeads.data && heads.length === 0 ? <Alert tone="warning">No vote heads exist yet. Ask the system administrator to add tuition, lunch, transport and the other vote heads first.</Alert> : null}
          <ol className="flex flex-col gap-3">
            {rows.map((r, idx) => (
              <li key={r.key} className="grid gap-2 rounded-md border border-line p-3 sm:grid-cols-[1fr_9rem_11rem_auto] sm:items-start sm:border-0 sm:p-0">
                <Select
                  label="Vote head"
                  hideLabel={idx > 0}
                  placeholder="Choose"
                  options={heads.map((h) => ({ value: h.id, label: h.name }))}
                  value={r.voteHeadId}
                  onChange={(e) => updateRow(r.key, { voteHeadId: e.target.value })}
                  error={errors[`vh-${r.key}`] ?? fieldError(save.error, `items[${idx}].voteHeadId`)}
                  disabled={readOnly}
                />
                <Input
                  label="Amount"
                  hideLabel={idx > 0}
                  prefix="KES"
                  inputMode="decimal"
                  value={r.amount}
                  onChange={(e) => updateRow(r.key, { amount: e.target.value })}
                  error={errors[`amt-${r.key}`] ?? fieldError(save.error, `items[${idx}].amount`)}
                  disabled={readOnly}
                />
                <Select
                  label="Applies to"
                  hideLabel={idx > 0}
                  options={(Object.keys(APPLIES_TO) as FeeAppliesTo[]).map((k) => ({ value: k, label: APPLIES_TO[k] }))}
                  value={r.appliesTo}
                  onChange={(e) => updateRow(r.key, { appliesTo: e.target.value as FeeAppliesTo })}
                  disabled={readOnly}
                />
                {!readOnly ? (
                  <Button
                    variant="ghost"
                    size="md"
                    className={idx === 0 ? "sm:mt-[1.85rem]" : ""}
                    aria-label={`Remove item ${idx + 1}`}
                    onClick={() => setRows((rs) => rs.filter((x) => x.key !== r.key))}
                    disabled={rows.length === 1}
                    icon={<Trash2 className="size-4" aria-hidden />}
                  >
                    <span className="sm:sr-only">Remove</span>
                  </Button>
                ) : null}
              </li>
            ))}
          </ol>
          {errors.rows ? <p className="mt-2 text-[0.8rem] font-semibold text-danger">{errors.rows}</p> : null}
          {!readOnly ? (
            <Button
              variant="secondary"
              size="sm"
              className="mt-3"
              icon={<Plus className="size-4" aria-hidden />}
              onClick={() => {
                setRows((rs) => [...rs, { key: nextKey, voteHeadId: "", amount: "", appliesTo: "ALL" }]);
                setNextKey((k) => k + 1);
              }}
            >
              Add item
            </Button>
          ) : null}
        </fieldset>

        <dl className="grid grid-cols-2 gap-px overflow-hidden rounded-md border border-line bg-line">
          <div className="bg-canvas px-3.5 py-2.5">
            <dt className="text-sm text-ink-2">Day scholar total</dt>
            <dd className="font-bold tabular">{formatKes(dayTotal)}</dd>
          </div>
          <div className="bg-canvas px-3.5 py-2.5">
            <dt className="text-sm text-ink-2">Boarder total</dt>
            <dd className="font-bold tabular">{formatKes(boardingTotal)}</dd>
          </div>
        </dl>
        <ErrorAlert error={save.error && Object.keys((save.error as { fieldErrors?: object }).fieldErrors ?? {}).length === 0 ? save.error : null} />
      </form>
    </Dialog>
  );
}
