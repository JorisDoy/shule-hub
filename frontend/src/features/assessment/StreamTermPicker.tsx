"use client";

import { Select } from "@/components/ui";
import { gradeLabel } from "@/lib/grades";
import type { Stream, Term } from "@/lib/api/types";

export function StreamTermPicker({
  streams,
  terms,
  streamId,
  termId,
  loading,
  onChange,
  year,
}: {
  streams: Stream[];
  terms: Term[];
  streamId: string;
  termId: string;
  loading?: boolean;
  onChange: (patch: { streamId?: string; termId?: string }) => void;
  year?: number;
}) {
  return (
    <div className="grid gap-3 sm:grid-cols-[minmax(0,16rem)_minmax(0,12rem)]">
      <Select
        label="Class"
        placeholder={loading ? "Loading classes…" : streams.length === 0 ? "No classes this year" : "Choose a class"}
        options={streams.map((s) => ({ value: s.id, label: `${s.name} (${gradeLabel(s.gradeLevel)})` }))}
        value={streamId}
        onChange={(e) => onChange({ streamId: e.target.value })}
        disabled={loading || streams.length === 0}
      />
      <Select
        label="Term"
        options={terms.map((t) => ({ value: t.id, label: `Term ${t.number}${year ? ` ${year}` : ""}` }))}
        value={termId}
        onChange={(e) => onChange({ termId: e.target.value })}
        disabled={terms.length === 0}
        placeholder={terms.length === 0 ? "No terms" : undefined}
      />
    </div>
  );
}
