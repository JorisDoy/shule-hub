"use client";

import { useState, type FormEvent } from "react";
import type { ApplicationDetail, Gender, GradeLevel, Relationship } from "@/lib/api/types";
import { Button, Checkbox, Dialog, ErrorAlert, Input, Select, Textarea } from "@/components/ui";
import { useSession } from "@/features/session/SessionProvider";
import { useAction } from "@/lib/hooks";
import { fieldError } from "@/lib/api/problem";
import { GRADE_OPTIONS, isGradeLevel } from "@/lib/grades";
import { normalizeKenyanPhone, PHONE_ERROR, PHONE_HINT } from "@/lib/phone";
import { yearOptions } from "@/features/academics/terms";
import { todayIso } from "@/lib/format";

interface FormState {
  academicYearId: string;
  gradeLevel: string;
  boardingRequested: boolean;
  learnerGiven: string;
  learnerFamily: string;
  dateOfBirth: string;
  gender: string;
  guardianGiven: string;
  guardianFamily: string;
  phone: string;
  email: string;
  relationship: string;
  notes: string;
}

const EMPTY: FormState = {
  academicYearId: "",
  gradeLevel: "",
  boardingRequested: false,
  learnerGiven: "",
  learnerFamily: "",
  dateOfBirth: "",
  gender: "",
  guardianGiven: "",
  guardianFamily: "",
  phone: "",
  email: "",
  relationship: "MOTHER",
  notes: "",
};

type Errors = Partial<Record<keyof FormState, string>>;

export function NewApplicationDialog({ open, onClose, onCreated }: { open: boolean; onClose: () => void; onCreated: (app: ApplicationDetail) => void }) {
  const { api, branch, years, currentYear } = useSession();
  const [form, setForm] = useState<FormState>(() => ({ ...EMPTY, academicYearId: currentYear?.id ?? "" }));
  const [errors, setErrors] = useState<Errors>({});
  const create = useAction(api.admissions.create);

  const set = <K extends keyof FormState>(key: K, value: FormState[K]) => setForm((f) => ({ ...f, [key]: value }));
  const err = (key: keyof FormState, ...apiPaths: string[]) => errors[key] ?? fieldError(create.error, ...apiPaths);

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (!branch) return;
    const phone = normalizeKenyanPhone(form.phone);
    const next: Errors = {};
    if (!isGradeLevel(form.gradeLevel)) next.gradeLevel = "Choose the grade the child is joining";
    if (!form.learnerGiven.trim()) next.learnerGiven = "Enter the child's first name";
    if (!form.learnerFamily.trim()) next.learnerFamily = "Enter the child's surname";
    if (!form.dateOfBirth) next.dateOfBirth = "Enter the date of birth";
    else if (form.dateOfBirth > todayIso()) next.dateOfBirth = "Date of birth can't be in the future";
    if (!form.gender) next.gender = "Choose one";
    if (!form.guardianGiven.trim()) next.guardianGiven = "Enter the parent's first name";
    if (!form.guardianFamily.trim()) next.guardianFamily = "Enter the parent's surname";
    if (!phone) next.phone = PHONE_ERROR;
    if (form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) next.email = "Enter a valid email or leave it blank";
    setErrors(next);
    if (Object.keys(next).length > 0 || !phone) return;

    const created = await create.run({
      branchId: branch.id,
      academicYearId: form.academicYearId || undefined,
      gradeLevel: form.gradeLevel as GradeLevel,
      boardingRequested: form.boardingRequested,
      learner: {
        givenName: form.learnerGiven.trim(),
        familyName: form.learnerFamily.trim(),
        dateOfBirth: form.dateOfBirth,
        gender: form.gender as Gender,
      },
      guardian: {
        givenName: form.guardianGiven.trim(),
        familyName: form.guardianFamily.trim(),
        phone,
        email: form.email.trim() || undefined,
        relationship: form.relationship as Relationship,
      },
      notes: form.notes.trim() || undefined,
    });
    if (created) {
      setForm({ ...EMPTY, academicYearId: currentYear?.id ?? "" });
      onCreated(created);
    }
  }

  const generalError = create.error && Object.keys((create.error as { fieldErrors?: object }).fieldErrors ?? {}).length === 0 ? create.error : null;

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title="New application"
      description={branch ? `${branch.name} branch` : undefined}
      size="lg"
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" form="new-application" loading={create.pending}>
            Save application
          </Button>
        </>
      }
    >
      <form id="new-application" noValidate onSubmit={submit} className="flex flex-col gap-6">
        <fieldset className="grid gap-4 sm:grid-cols-2">
          <legend className="mb-3 font-display text-base font-semibold">Joining</legend>
          <Select
            label="Grade"
            placeholder="Choose grade"
            options={GRADE_OPTIONS}
            value={form.gradeLevel}
            onChange={(e) => set("gradeLevel", e.target.value)}
            error={err("gradeLevel", "gradeLevel")}
            required
          />
          <Select
            label="Academic year"
            placeholder="Not decided yet"
            options={yearOptions(years)}
            value={form.academicYearId}
            onChange={(e) => set("academicYearId", e.target.value)}
            error={err("academicYearId", "academicYearId")}
          />
          {branch?.boardingEnabled ? (
            <div className="sm:col-span-2">
              <Checkbox label="Boarding requested" checked={form.boardingRequested} onChange={(v) => set("boardingRequested", v)} />
            </div>
          ) : null}
        </fieldset>

        <fieldset className="grid gap-4 sm:grid-cols-2">
          <legend className="mb-3 font-display text-base font-semibold">Child</legend>
          <Input label="First name" value={form.learnerGiven} onChange={(e) => set("learnerGiven", e.target.value)} error={err("learnerGiven", "learner.givenName")} autoComplete="off" required />
          <Input label="Surname" value={form.learnerFamily} onChange={(e) => set("learnerFamily", e.target.value)} error={err("learnerFamily", "learner.familyName")} autoComplete="off" required />
          <Input label="Date of birth" type="date" max={todayIso()} value={form.dateOfBirth} onChange={(e) => set("dateOfBirth", e.target.value)} error={err("dateOfBirth", "learner.dateOfBirth")} required />
          <Select
            label="Gender"
            placeholder="Choose"
            options={[
              { value: "FEMALE", label: "Female" },
              { value: "MALE", label: "Male" },
            ]}
            value={form.gender}
            onChange={(e) => set("gender", e.target.value)}
            error={err("gender", "learner.gender")}
            required
          />
        </fieldset>

        <fieldset className="grid gap-4 sm:grid-cols-2">
          <legend className="mb-3 font-display text-base font-semibold">Parent or guardian</legend>
          <Input label="First name" value={form.guardianGiven} onChange={(e) => set("guardianGiven", e.target.value)} error={err("guardianGiven", "guardian.givenName")} required />
          <Input label="Surname" value={form.guardianFamily} onChange={(e) => set("guardianFamily", e.target.value)} error={err("guardianFamily", "guardian.familyName")} required />
          <Input label="Phone" type="tel" inputMode="tel" placeholder="0712 345 678" hint={PHONE_HINT} value={form.phone} onChange={(e) => set("phone", e.target.value)} error={err("phone", "guardian.phone")} required />
          <Input label="Email" type="email" inputMode="email" value={form.email} onChange={(e) => set("email", e.target.value)} error={err("email", "guardian.email")} />
          <Select
            label="Relationship"
            options={[
              { value: "MOTHER", label: "Mother" },
              { value: "FATHER", label: "Father" },
              { value: "GUARDIAN", label: "Guardian" },
              { value: "OTHER", label: "Other" },
            ]}
            value={form.relationship}
            onChange={(e) => set("relationship", e.target.value)}
            error={err("relationship", "guardian.relationship")}
          />
        </fieldset>

        <Textarea label="Notes" hint="Previous school, siblings already enrolled, special needs" value={form.notes} onChange={(e) => set("notes", e.target.value)} error={err("notes", "notes")} />
        <ErrorAlert error={generalError} />
      </form>
    </Dialog>
  );
}
