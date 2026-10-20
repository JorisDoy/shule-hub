"use client";

import { useState, type FormEvent } from "react";
import { CalendarPlus, Check, Copy, Plus, Radio, Smartphone } from "lucide-react";
import type { AcademicYear, CreateAcademicYearRequest, MpesaChannel, MpesaChannelType, MpesaEnvironment, Stream } from "@/lib/api/types";
import { GRADE_LEVELS } from "@/lib/api/types";
import { useAction, useQuery } from "@/lib/hooks";
import { fieldError } from "@/lib/api/problem";
import { GRADE_OPTIONS, gradeLabel, isGradeLevel } from "@/lib/grades";
import { useSession } from "@/features/session/SessionProvider";
import { termRange, yearOptions } from "@/features/academics/terms";
import { Alert, Badge, Button, Card, CardBody, CardHeader, Dialog, EmptyState, ErrorAlert, Input, PageHeader, Select, SkeletonRows, Table, TBody, Td, Th, THead, Tabs, useToast } from "@/components/ui";

type Tab = "years" | "streams" | "mpesa";

export default function SettingsPage() {
  const session = useSession();
  const tabs = [
    ...(session.can("ACADEMICS_MANAGE")
      ? [
          { value: "years", label: "Academic years" },
          { value: "streams", label: "Classes and streams" },
        ]
      : []),
    ...(session.can("PAYMENTS_CONFIGURE") ? [{ value: "mpesa", label: "M-Pesa" }] : []),
  ];
  const [picked, setPicked] = useState<Tab | null>(null);
  const tab = (picked && tabs.some((t) => t.value === picked) ? picked : tabs[0]?.value) as Tab | undefined;

  return (
    <div className="max-w-6xl">
      <PageHeader title="Settings" description="The school calendar, classes, and how parents pay with M-Pesa." />
      {tabs.length > 1 ? <Tabs idBase="settings" label="Settings sections" items={tabs} value={tab ?? ""} onChange={(v) => setPicked(v as Tab)} className="mb-4" /> : null}
      <div role={tabs.length > 1 ? "tabpanel" : undefined} id="settings-panel" aria-labelledby={tabs.length > 1 ? `settings-tab-${tab}` : undefined}>
        {tab === "years" ? <YearsSection /> : tab === "streams" ? <StreamsSection /> : tab === "mpesa" ? <MpesaSection /> : null}
      </div>
    </div>
  );
}

// ---------------- Academic years ----------------

function YearsSection() {
  const { years, yearsLoading, yearsError, refreshAcademics } = useSession();
  const [creating, setCreating] = useState(false);
  return (
    <Card>
      <CardHeader
        title="Academic years and terms"
        description="Kenyan schools run three terms a year. Fees, invoices and report cards are all tied to a term."
        actions={
          <Button icon={<CalendarPlus className="size-4" aria-hidden />} onClick={() => setCreating(true)}>
            Add academic year
          </Button>
        }
      />
      {yearsError ? (
        <CardBody>
          <ErrorAlert error={yearsError} onRetry={refreshAcademics} />
        </CardBody>
      ) : yearsLoading && years.length === 0 ? (
        <SkeletonRows />
      ) : years.length === 0 ? (
        <EmptyState title="No academic years yet" action={<Button variant="secondary" onClick={() => setCreating(true)}>Add this year</Button>}>
          Start with the current year and its three terms.
        </EmptyState>
      ) : (
        <ul className="divide-y divide-line">
          {years.map((y) => (
            <li key={y.id} className="px-4 py-4 sm:px-5">
              <div className="flex flex-wrap items-center gap-2">
                <p className="font-display text-lg font-semibold">{y.year}</p>
                {y.current ? <Badge tone="green">Current</Badge> : null}
                <p className="text-sm text-ink-2">{termRange(y)}</p>
              </div>
              <ol className="mt-2 grid gap-2 sm:grid-cols-3">
                {[...y.terms]
                  .sort((a, b) => a.number - b.number)
                  .map((t) => (
                    <li key={t.id} className="rounded-md border border-line px-3 py-2">
                      <p className="font-semibold">Term {t.number}</p>
                      <p className="text-sm text-ink-2">{termRange(t)}</p>
                    </li>
                  ))}
              </ol>
            </li>
          ))}
        </ul>
      )}
      {creating ? (
        <CreateYearDialog
          existing={years}
          onClose={() => setCreating(false)}
          onCreated={() => {
            setCreating(false);
            refreshAcademics();
          }}
        />
      ) : null}
    </Card>
  );
}

/** A typical Kenyan private-school calendar to start from; every date is editable. */
function suggestedCalendar(year: number) {
  return {
    startDate: `${year}-01-05`,
    endDate: `${year}-11-27`,
    terms: [
      { number: 1, startDate: `${year}-01-05`, endDate: `${year}-04-02` },
      { number: 2, startDate: `${year}-04-27`, endDate: `${year}-07-31` },
      { number: 3, startDate: `${year}-08-24`, endDate: `${year}-11-27` },
    ],
  };
}

function CreateYearDialog({ existing, onClose, onCreated }: { existing: AcademicYear[]; onClose: () => void; onCreated: (y: AcademicYear) => void }) {
  const { api } = useSession();
  const toast = useToast();
  const nextYear = existing.length > 0 ? Math.max(...existing.map((y) => y.year)) + 1 : new Date().getFullYear();
  const [year, setYear] = useState(String(nextYear));
  const [cal, setCal] = useState(() => suggestedCalendar(nextYear));
  const [errors, setErrors] = useState<Record<string, string>>({});
  const create = useAction((body: CreateAcademicYearRequest) => api.academics.createYear(body));

  function changeYear(v: string) {
    setYear(v);
    if (/^\d{4}$/.test(v)) setCal(suggestedCalendar(Number(v)));
  }

  function setTerm(i: number, key: "startDate" | "endDate", value: string) {
    setCal((c) => ({ ...c, terms: c.terms.map((t, idx) => (idx === i ? { ...t, [key]: value } : t)) }));
  }

  async function submit(e: FormEvent) {
    e.preventDefault();
    const next: Record<string, string> = {};
    if (!/^\d{4}$/.test(year)) next.year = "Enter a four-digit year";
    if (existing.some((y) => String(y.year) === year)) next.year = `${year} already exists`;
    if (!cal.startDate || !cal.endDate || cal.startDate > cal.endDate) next.range = "The year must start before it ends";
    cal.terms.forEach((t, i) => {
      if (!t.startDate || !t.endDate || t.startDate > t.endDate) next[`t${i}`] = "Opening must be before closing";
      else if (t.startDate < cal.startDate || t.endDate > cal.endDate) next[`t${i}`] = "Must fall inside the academic year";
      else if (i > 0 && t.startDate <= cal.terms[i - 1].endDate) next[`t${i}`] = `Must open after Term ${i} closes`;
    });
    setErrors(next);
    if (Object.keys(next).length > 0) return;
    const created = await create.run({ year: Number(year), startDate: cal.startDate, endDate: cal.endDate, terms: cal.terms });
    if (created) {
      toast.show(`${created.year} academic year added`);
      onCreated(created);
    }
  }

  return (
    <Dialog
      open
      onClose={onClose}
      title="Add academic year"
      size="lg"
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" form="create-year" loading={create.pending}>
            Add academic year
          </Button>
        </>
      }
    >
      <form id="create-year" noValidate onSubmit={submit} className="flex flex-col gap-5">
        <div className="grid gap-4 sm:grid-cols-3">
          <Input label="Year" inputMode="numeric" value={year} onChange={(e) => changeYear(e.target.value)} error={errors.year ?? fieldError(create.error, "year")} required />
          <Input label="Opens" type="date" value={cal.startDate} onChange={(e) => setCal((c) => ({ ...c, startDate: e.target.value }))} error={errors.range ?? fieldError(create.error, "startDate")} required />
          <Input label="Closes" type="date" value={cal.endDate} onChange={(e) => setCal((c) => ({ ...c, endDate: e.target.value }))} error={fieldError(create.error, "endDate")} required />
        </div>
        <fieldset>
          <legend className="mb-1 font-display text-base font-semibold">Terms</legend>
          <p className="mb-3 text-sm text-ink-2">Prefilled with a typical calendar. Adjust to match the Ministry of Education circular for {year}.</p>
          <ol className="flex flex-col gap-3">
            {cal.terms.map((t, i) => (
              <li key={t.number} className="grid gap-3 rounded-md border border-line p-3 sm:grid-cols-[5rem_1fr_1fr] sm:items-start">
                <p className="font-semibold sm:pt-8">Term {t.number}</p>
                <Input label="Opens" type="date" value={t.startDate} onChange={(e) => setTerm(i, "startDate", e.target.value)} error={errors[`t${i}`] ?? fieldError(create.error, `terms[${i}].startDate`)} required />
                <Input label="Closes" type="date" value={t.endDate} onChange={(e) => setTerm(i, "endDate", e.target.value)} error={fieldError(create.error, `terms[${i}].endDate`)} required />
              </li>
            ))}
          </ol>
        </fieldset>
        <ErrorAlert error={create.error && Object.keys((create.error as { fieldErrors?: object }).fieldErrors ?? {}).length === 0 ? create.error : null} />
      </form>
    </Dialog>
  );
}

// ---------------- Streams ----------------

function StreamsSection() {
  const { api, branchId, branch, years, currentYear } = useSession();
  const [yearId, setYearId] = useState("");
  const effectiveYear = yearId || currentYear?.id || "";
  const streams = useQuery(branchId && effectiveYear ? `streams:${branchId}:${effectiveYear}` : null, (signal) => api.academics.streams({ branchId, academicYearId: effectiveYear }, { signal }));
  const [creating, setCreating] = useState(false);
  const list = [...(streams.data ?? [])].sort((a, b) => GRADE_LEVELS.indexOf(a.gradeLevel) - GRADE_LEVELS.indexOf(b.gradeLevel) || a.name.localeCompare(b.name));

  return (
    <Card>
      <CardHeader
        title="Classes and streams"
        description={`Each grade can have one or more streams (for example Grade 4 East and Grade 4 West)${branch ? ` at ${branch.name}` : ""}.`}
        actions={
          <>
            {years.length > 1 ? <Select label="Academic year" hideLabel options={yearOptions(years)} value={effectiveYear} onChange={(e) => setYearId(e.target.value)} containerClassName="w-40" /> : null}
            <Button icon={<Plus className="size-4" aria-hidden />} onClick={() => setCreating(true)} disabled={!effectiveYear || !branchId}>
              Add stream
            </Button>
          </>
        }
      />
      {!effectiveYear ? (
        <CardBody>
          <Alert tone="warning">Add an academic year first. Streams belong to a year.</Alert>
        </CardBody>
      ) : streams.error ? (
        <CardBody>
          <ErrorAlert error={streams.error} onRetry={streams.refetch} />
        </CardBody>
      ) : streams.loading && !streams.data ? (
        <SkeletonRows />
      ) : list.length === 0 ? (
        <EmptyState title="No streams for this year">Add a stream for every grade you teach, from Daycare to Grade 9.</EmptyState>
      ) : (
        <Table caption="Streams">
          <THead>
            <tr>
              <Th>Grade</Th>
              <Th>Stream</Th>
              <Th numeric>Capacity</Th>
            </tr>
          </THead>
          <TBody>
            {list.map((s) => (
              <tr key={s.id}>
                <Td className="whitespace-nowrap">{gradeLabel(s.gradeLevel)}</Td>
                <Td className="font-semibold">{s.name}</Td>
                <Td numeric>{s.capacity}</Td>
              </tr>
            ))}
          </TBody>
        </Table>
      )}
      {creating && branchId && effectiveYear ? (
        <CreateStreamDialog
          branchId={branchId}
          academicYearId={effectiveYear}
          onClose={() => setCreating(false)}
          onCreated={(s) => {
            streams.setData((prev) => [...(prev ?? []), s]);
            setCreating(false);
          }}
        />
      ) : null}
    </Card>
  );
}

function CreateStreamDialog({ branchId, academicYearId, onClose, onCreated }: { branchId: string; academicYearId: string; onClose: () => void; onCreated: (s: Stream) => void }) {
  const { api, years } = useSession();
  const toast = useToast();
  const [grade, setGrade] = useState("");
  const [name, setName] = useState("");
  const [capacity, setCapacity] = useState("30");
  const [errors, setErrors] = useState<Record<string, string>>({});
  const create = useAction(api.academics.createStream);

  async function submit(e: FormEvent) {
    e.preventDefault();
    const cap = Number(capacity);
    const next: Record<string, string> = {};
    if (!isGradeLevel(grade)) next.grade = "Choose a grade";
    if (!name.trim()) next.name = "Enter a stream name";
    if (!Number.isInteger(cap) || cap < 1) next.capacity = "Enter a whole number of learners";
    setErrors(next);
    if (Object.keys(next).length > 0 || !isGradeLevel(grade)) return;
    const s = await create.run({ branchId, academicYearId, gradeLevel: grade, name: name.trim(), capacity: cap });
    if (s) {
      toast.show(`${s.name} added`);
      onCreated(s);
    }
  }

  return (
    <Dialog
      open
      onClose={onClose}
      title="Add stream"
      description={`Academic year ${years.find((y) => y.id === academicYearId)?.year ?? ""}`}
      size="sm"
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" form="create-stream" loading={create.pending}>
            Add stream
          </Button>
        </>
      }
    >
      <form id="create-stream" noValidate onSubmit={submit} className="flex flex-col gap-4">
        <Select
          label="Grade"
          placeholder="Choose grade"
          options={GRADE_OPTIONS}
          value={grade}
          onChange={(e) => {
            setGrade(e.target.value);
            if (!name && isGradeLevel(e.target.value)) setName(`${gradeLabel(e.target.value)} `);
          }}
          error={errors.grade ?? fieldError(create.error, "gradeLevel")}
          required
        />
        <Input label="Stream name" value={name} onChange={(e) => setName(e.target.value)} placeholder="Grade 4 East" error={errors.name ?? fieldError(create.error, "name")} required />
        <Input label="Capacity" inputMode="numeric" value={capacity} onChange={(e) => setCapacity(e.target.value)} error={errors.capacity ?? fieldError(create.error, "capacity")} hint="Admissions stop when the stream is full" required />
        <ErrorAlert error={create.error && !fieldError(create.error, "gradeLevel", "name", "capacity") ? create.error : null} />
      </form>
    </Dialog>
  );
}

// ---------------- M-Pesa ----------------

function MpesaSection() {
  const { api, branches } = useSession();
  const toast = useToast();
  const channels = useQuery("mpesa-channels", (signal) => api.mpesa.channels({ signal }));
  const [creating, setCreating] = useState(false);
  const [registering, setRegistering] = useState<string | null>(null);
  const register = useAction(api.mpesa.registerC2b);

  async function doRegister(c: MpesaChannel) {
    setRegistering(c.id);
    const r = await register.run(c.id);
    setRegistering(null);
    if (r) toast.show(`Safaricom: ${r.responseDescription}`);
  }

  return (
    <Card>
      <CardHeader
        title="M-Pesa channels"
        description="The Paybill or Till numbers parents pay into. Daraja credentials are encrypted and never shown again."
        actions={
          <Button icon={<Plus className="size-4" aria-hidden />} onClick={() => setCreating(true)}>
            Add channel
          </Button>
        }
      />
      {register.error ? (
        <CardBody>
          <ErrorAlert error={register.error} title="Registering C2B URLs failed" />
        </CardBody>
      ) : null}
      {channels.error ? (
        <CardBody>
          <ErrorAlert error={channels.error} onRetry={channels.refetch} />
        </CardBody>
      ) : channels.loading && !channels.data ? (
        <SkeletonRows />
      ) : (channels.data ?? []).length === 0 ? (
        <EmptyState icon={<Smartphone className="size-8" aria-hidden />} title="No M-Pesa channel yet" action={<Button variant="secondary" onClick={() => setCreating(true)}>Add your Paybill</Button>}>
          Add the school&apos;s Paybill or Till with its Daraja keys to send payment prompts and receive Paybill payments automatically.
        </EmptyState>
      ) : (
        <ul className="divide-y divide-line">
          {(channels.data ?? []).map((c) => (
            <li key={c.id} className="px-4 py-4 sm:px-5">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <p className="flex flex-wrap items-center gap-2">
                    <span className="font-display text-lg font-semibold tabular">
                      {c.channelType === "PAYBILL" ? "Paybill" : "Till"} {c.shortcode}
                    </span>
                    <Badge tone={c.environment === "PRODUCTION" ? "green" : "owe"}>{c.environment === "PRODUCTION" ? "Live" : "Sandbox"}</Badge>
                    {!c.active ? <Badge>Inactive</Badge> : null}
                  </p>
                  <p className="text-sm text-ink-2">
                    {branches.find((b) => b.id === c.branchId)?.name ?? "Branch"} branch
                    {c.partyB ? `, pays into ${c.partyB}` : ""}
                  </p>
                </div>
                <Button size="sm" variant="secondary" icon={<Radio className="size-4" aria-hidden />} onClick={() => doRegister(c)} loading={registering === c.id}>
                  Register C2B URLs
                </Button>
              </div>
              <dl className="mt-3 flex flex-col gap-2">
                <CallbackUrl label="STK callback" url={c.callbackUrls.stk} />
                <CallbackUrl label="C2B validation" url={c.callbackUrls.validation} />
                <CallbackUrl label="C2B confirmation" url={c.callbackUrls.confirmation} />
              </dl>
            </li>
          ))}
        </ul>
      )}
      {creating ? (
        <CreateChannelDialog
          onClose={() => setCreating(false)}
          onCreated={(c) => {
            channels.setData((prev) => [...(prev ?? []), c]);
            setCreating(false);
          }}
        />
      ) : null}
    </Card>
  );
}

function CallbackUrl({ label, url }: { label: string; url: string }) {
  const [copied, setCopied] = useState(false);
  async function copy() {
    try {
      await navigator.clipboard.writeText(url);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      setCopied(false);
    }
  }
  return (
    <div className="grid gap-1 sm:grid-cols-[9rem_1fr_auto] sm:items-center sm:gap-3">
      <dt className="text-sm text-ink-2">{label}</dt>
      <dd className="min-w-0 rounded-sm bg-canvas px-2 py-1 text-[0.85rem] break-all">{url}</dd>
      <dd>
        <Button size="sm" variant="ghost" onClick={copy} icon={copied ? <Check className="size-4 text-uniform" aria-hidden /> : <Copy className="size-4" aria-hidden />} aria-label={`Copy ${label} URL`}>
          {copied ? "Copied" : "Copy"}
        </Button>
      </dd>
    </div>
  );
}

function CreateChannelDialog({ onClose, onCreated }: { onClose: () => void; onCreated: (c: MpesaChannel) => void }) {
  const { api, branches, branchId } = useSession();
  const toast = useToast();
  const [form, setForm] = useState({
    branchId: branchId ?? "",
    shortcode: "",
    partyB: "",
    channelType: "PAYBILL" as MpesaChannelType,
    environment: "SANDBOX" as MpesaEnvironment,
    consumerKey: "",
    consumerSecret: "",
    passkey: "",
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const create = useAction(api.mpesa.createChannel);
  const set = (k: keyof typeof form, v: string) => setForm((f) => ({ ...f, [k]: v }));
  const err = (k: string) => errors[k] ?? fieldError(create.error, k);

  async function submit(e: FormEvent) {
    e.preventDefault();
    const next: Record<string, string> = {};
    if (!form.branchId) next.branchId = "Choose a branch";
    if (!/^\d{5,7}$/.test(form.shortcode.trim())) next.shortcode = "Enter the 5–7 digit shortcode";
    if (form.channelType === "TILL" && form.partyB && !/^\d{5,7}$/.test(form.partyB.trim())) next.partyB = "Enter the store number, digits only";
    if (!form.consumerKey.trim()) next.consumerKey = "Paste the consumer key from the Daraja portal";
    if (!form.consumerSecret.trim()) next.consumerSecret = "Paste the consumer secret";
    if (!form.passkey.trim()) next.passkey = "Paste the Lipa na M-Pesa passkey";
    setErrors(next);
    if (Object.keys(next).length > 0) return;
    const c = await create.run({
      branchId: form.branchId,
      shortcode: form.shortcode.trim(),
      partyB: form.partyB.trim() || undefined,
      channelType: form.channelType,
      environment: form.environment,
      consumerKey: form.consumerKey.trim(),
      consumerSecret: form.consumerSecret.trim(),
      passkey: form.passkey.trim(),
    });
    if (c) {
      toast.show(`${c.channelType === "PAYBILL" ? "Paybill" : "Till"} ${c.shortcode} added`);
      onCreated(c);
    }
  }

  return (
    <Dialog
      open
      onClose={onClose}
      title="Add M-Pesa channel"
      size="lg"
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" form="create-channel" loading={create.pending}>
            Add channel
          </Button>
        </>
      }
    >
      <form id="create-channel" noValidate onSubmit={submit} autoComplete="off" className="grid gap-4 sm:grid-cols-2">
        <Select label="Branch" options={branches.map((b) => ({ value: b.id, label: b.name }))} value={form.branchId} onChange={(e) => set("branchId", e.target.value)} error={err("branchId")} required />
        <Select
          label="Environment"
          options={[
            { value: "SANDBOX", label: "Sandbox (testing)" },
            { value: "PRODUCTION", label: "Production (live money)" },
          ]}
          value={form.environment}
          onChange={(e) => set("environment", e.target.value)}
        />
        <Select
          label="Type"
          options={[
            { value: "PAYBILL", label: "Paybill" },
            { value: "TILL", label: "Buy Goods Till" },
          ]}
          value={form.channelType}
          onChange={(e) => set("channelType", e.target.value)}
        />
        <Input label="Shortcode" inputMode="numeric" value={form.shortcode} onChange={(e) => set("shortcode", e.target.value)} error={err("shortcode")} hint={form.channelType === "TILL" ? "The Head Office number used for STK" : "The Paybill number"} required />
        {form.channelType === "TILL" ? (
          <Input label="Till (store) number" inputMode="numeric" value={form.partyB} onChange={(e) => set("partyB", e.target.value)} error={err("partyB")} hint="Where the money lands, if different from the shortcode" containerClassName="sm:col-span-2" />
        ) : null}
        <Input label="Consumer key" value={form.consumerKey} onChange={(e) => set("consumerKey", e.target.value)} error={err("consumerKey")} spellCheck={false} required containerClassName="sm:col-span-2" />
        <Input label="Consumer secret" type="password" value={form.consumerSecret} onChange={(e) => set("consumerSecret", e.target.value)} error={err("consumerSecret")} autoComplete="new-password" required />
        <Input label="Passkey" type="password" value={form.passkey} onChange={(e) => set("passkey", e.target.value)} error={err("passkey")} autoComplete="new-password" required />
        <p className="text-sm text-ink-2 sm:col-span-2">Secrets are encrypted when saved and can&apos;t be viewed again. To change them, add the channel again.</p>
        <ErrorAlert className="sm:col-span-2" error={create.error && Object.keys((create.error as { fieldErrors?: object }).fieldErrors ?? {}).length === 0 ? create.error : null} />
      </form>
    </Dialog>
  );
}

