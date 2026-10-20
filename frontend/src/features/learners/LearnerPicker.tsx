"use client";

import { useId, useState } from "react";
import { Search, X } from "lucide-react";
import type { LearnerSummary } from "@/lib/api/types";
import { useDebounced, useQuery } from "@/lib/hooks";
import { gradeLabel } from "@/lib/grades";
import { useSession } from "@/features/session/SessionProvider";
import { Spinner } from "@/components/ui/Button";
import { cx } from "@/lib/cx";

export interface PickedLearner {
  id: string;
  fullName: string;
  admissionNumber: string;
  gradeLevel?: LearnerSummary["gradeLevel"];
  streamName?: string | null;
}

/** Search-as-you-type learner picker (GET /api/learners?q=) scoped to the selected branch. */
export function LearnerPicker({
  value,
  onChange,
  label = "Learner",
  error,
  initialQuery = "",
}: {
  value: PickedLearner | null;
  onChange: (learner: PickedLearner | null) => void;
  label?: string;
  error?: string;
  initialQuery?: string;
}) {
  const { api, branchId } = useSession();
  const id = useId();
  const [q, setQ] = useState(initialQuery);
  const debounced = useDebounced(q.trim(), 300);
  const enabled = !value && debounced.length >= 2;
  const results = useQuery(enabled ? `pick:${branchId}:${debounced}` : null, (signal) =>
    api.learners.search({ branchId, q: debounced, status: "ACTIVE", size: 8 }, { signal }),
  );

  if (value) {
    return (
      <div className="flex flex-col gap-1.5">
        <span className="text-sm font-semibold">{label}</span>
        <div className="flex items-center justify-between gap-3 rounded-md border border-uniform/40 bg-uniform-tint/50 px-3 py-2">
          <div className="min-w-0">
            <p className="truncate font-semibold">{value.fullName}</p>
            <p className="text-sm text-ink-2">
              {value.admissionNumber}
              {value.gradeLevel ? `, ${gradeLabel(value.gradeLevel)}` : ""}
              {value.streamName ? ` (${value.streamName})` : ""}
            </p>
          </div>
          <button type="button" onClick={() => onChange(null)} className="flex items-center gap-1 rounded-sm text-sm font-semibold text-ink-2 hover:text-ink">
            <X className="size-4" aria-hidden /> Change
          </button>
        </div>
        {error ? <p className="text-[0.8rem] font-semibold text-danger">{error}</p> : null}
      </div>
    );
  }

  const list = results.data?.content ?? [];
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={id} className="text-sm font-semibold">
        {label}
      </label>
      <div className="relative">
        <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-ink-3" aria-hidden />
        <input
          id={id}
          type="search"
          autoComplete="off"
          placeholder="Name or admission number"
          value={q}
          onChange={(e) => setQ(e.target.value)}
          aria-invalid={error ? true : undefined}
          aria-describedby={`${id}-status`}
          className={cx(
            "block h-10 w-full rounded-md border border-line-strong bg-paper pr-9 pl-9 placeholder:text-ink-3/80",
            error && "border-danger",
          )}
        />
        {results.loading ? <Spinner className="absolute top-1/2 right-3 -translate-y-1/2 text-ink-3" /> : null}
      </div>
      <p id={`${id}-status`} className="text-[0.8rem] text-ink-3" aria-live="polite">
        {debounced.length < 2
          ? "Type at least two letters"
          : results.loading
            ? "Searching…"
            : results.error
              ? "Search failed. Check your connection."
              : list.length === 0
                ? "No active learners match"
                : `${list.length} match${list.length === 1 ? "" : "es"}`}
      </p>
      {list.length > 0 ? (
        <ul className="max-h-60 divide-y divide-line overflow-y-auto rounded-md border border-line">
          {list.map((l) => (
            <li key={l.id}>
              <button
                type="button"
                onClick={() => onChange({ id: l.id, fullName: l.fullName, admissionNumber: l.admissionNumber, gradeLevel: l.gradeLevel, streamName: l.streamName })}
                className="w-full px-3 py-2 text-left hover:bg-uniform-tint/50"
              >
                <span className="block font-semibold">{l.fullName}</span>
                <span className="block text-sm text-ink-2">
                  {l.admissionNumber}, {gradeLabel(l.gradeLevel)}
                  {l.streamName ? ` (${l.streamName})` : ""}
                </span>
              </button>
            </li>
          ))}
        </ul>
      ) : null}
      {error ? <p className="text-[0.8rem] font-semibold text-danger">{error}</p> : null}
    </div>
  );
}
