"use client";

import type { KeyboardEvent } from "react";
import type { GradingBand, ScoreRow } from "@/lib/api/types";
import { previewBand } from "@/lib/bands";
import { BandChip } from "@/components/ui/Badge";
import { cx } from "@/lib/cx";

export interface ScoreDraft {
  raw: string;
  rubric: string;
  comment: string;
}

export type GridColumn = "score" | "comment";

/** Parses a typed score. Empty → null; invalid → NaN. Accepts "34", "34.5", "34,5". */
export function parseScore(input: string): number | null {
  const t = input.trim().replace(",", ".");
  if (t === "") return null;
  if (!/^\d+(\.\d{1,2})?$/.test(t)) return NaN;
  return Number(t);
}

export function scoreError(input: string, maxScore: number | null): string | null {
  if (maxScore === null) return null;
  const n = parseScore(input);
  if (n === null) return null;
  if (Number.isNaN(n)) return "Numbers only";
  if (n > maxScore) return `Max ${maxScore}`;
  return null;
}

/**
 * One learner in the score entry grid. Numeric mode shows a live percent and band preview using
 * the same rounding as the backend; rubric mode shows a band select.
 * Enter / ArrowDown move to the same field in the next row, ArrowUp to the previous row
 * (on the band select, arrows keep their normal job of changing the level; Enter still moves down).
 */
export function ScoreEntryRow({
  row,
  index,
  draft,
  maxScore,
  bands,
  readOnly,
  dirty,
  onChange,
  onNavigate,
  registerField,
}: {
  row: ScoreRow;
  index: number;
  draft: ScoreDraft;
  maxScore: number | null;
  bands: readonly GradingBand[] | null;
  readOnly?: boolean;
  dirty?: boolean;
  onChange: (learnerId: string, patch: Partial<ScoreDraft>) => void;
  onNavigate?: (column: GridColumn, index: number, delta: 1 | -1) => void;
  registerField?: (column: GridColumn, index: number, el: HTMLElement | null) => void;
}) {
  const rubricOnly = maxScore === null;
  const parsed = parseScore(draft.raw);
  const error = scoreError(draft.raw, maxScore);
  const preview = !rubricOnly && parsed !== null && !Number.isNaN(parsed) && !error ? previewBand(parsed, maxScore, bands) : { percent: null, band: null };
  const rubricBand = rubricOnly ? bands?.find((b) => b.code === draft.rubric) ?? null : null;

  function keyNav(column: GridColumn, allowArrows: boolean) {
    return (e: KeyboardEvent<HTMLElement>) => {
      if (!onNavigate) return;
      if (e.key === "Enter" && !e.shiftKey) {
        e.preventDefault();
        onNavigate(column, index, 1);
      } else if (allowArrows && e.key === "ArrowDown") {
        e.preventDefault();
        onNavigate(column, index, 1);
      } else if (allowArrows && e.key === "ArrowUp") {
        e.preventDefault();
        onNavigate(column, index, -1);
      }
    };
  }

  const scoreId = `score-${row.learnerId}`;
  const commentId = `comment-${row.learnerId}`;

  return (
    <li className={cx("grid grid-cols-[1fr_auto] gap-x-3 gap-y-2 px-4 py-3 sm:px-5 md:grid-cols-[minmax(12rem,1.2fr)_8.5rem_8rem_minmax(12rem,1.5fr)] md:items-center", dirty && "bg-owe-tint/35")}>
      <div className="col-span-2 min-w-0 md:col-span-1">
        <p className="truncate font-semibold">
          <span className="mr-2 inline-block w-6 text-right text-sm font-normal text-ink-3 tabular">{index + 1}</span>
          {row.learnerName}
        </p>
        <p className="pl-8 text-sm text-ink-2 tabular">{row.admissionNumber}</p>
      </div>

      <div>
        <label htmlFor={scoreId} className="sr-only">
          {rubricOnly ? `Level for ${row.learnerName}` : `Score for ${row.learnerName} out of ${maxScore}`}
        </label>
        {rubricOnly ? (
          bands && bands.length > 0 ? (
            <select
              id={scoreId}
              ref={(el) => registerField?.("score", index, el)}
              value={draft.rubric}
              disabled={readOnly}
              onChange={(e) => onChange(row.learnerId, { rubric: e.target.value })}
              onKeyDown={keyNav("score", false)}
              className="h-10 w-full rounded-md border border-line-strong bg-paper px-2 font-semibold disabled:bg-canvas"
            >
              <option value="">Level…</option>
              {bands.map((b) => (
                <option key={b.code} value={b.code}>
                  {b.code}, {b.label}
                </option>
              ))}
            </select>
          ) : (
            <span className="text-sm text-ink-3">Comment only</span>
          )
        ) : (
          <div className="flex items-center gap-1.5">
            <input
              id={scoreId}
              ref={(el) => registerField?.("score", index, el)}
              type="text"
              inputMode="decimal"
              autoComplete="off"
              value={draft.raw}
              disabled={readOnly}
              aria-invalid={error ? true : undefined}
              aria-describedby={error ? `${scoreId}-err` : undefined}
              onChange={(e) => onChange(row.learnerId, { raw: e.target.value })}
              onKeyDown={keyNav("score", true)}
              onFocus={(e) => e.currentTarget.select()}
              className="h-10 w-20 rounded-md border border-line-strong bg-paper px-2 text-right text-[1.05rem] font-semibold tabular disabled:bg-canvas aria-[invalid=true]:border-danger"
            />
            <span className="text-sm text-ink-3">/ {maxScore}</span>
          </div>
        )}
        {error ? (
          <p id={`${scoreId}-err`} className="mt-1 text-[0.8rem] font-semibold text-danger">
            {error}
          </p>
        ) : null}
      </div>

      <div className="flex items-center gap-2 md:justify-start" aria-live="polite" data-testid={`preview-${row.learnerId}`}>
        {rubricOnly ? (
          rubricBand ? (
            <>
              <BandChip code={rubricBand.code} title={rubricBand.label} />
              <span className="text-sm text-ink-2 md:sr-only">{rubricBand.label}</span>
            </>
          ) : null
        ) : preview.percent !== null ? (
          <>
            <span className="w-11 text-right font-semibold tabular">{preview.percent}%</span>
            {preview.band ? <BandChip code={preview.band.code} title={preview.band.label} /> : null}
          </>
        ) : (
          <span className="text-sm text-ink-3">Not scored</span>
        )}
      </div>

      <div className="col-span-2 md:col-span-1">
        <label htmlFor={commentId} className="sr-only">
          Comment for {row.learnerName}
        </label>
        <input
          id={commentId}
          ref={(el) => registerField?.("comment", index, el)}
          type="text"
          value={draft.comment}
          disabled={readOnly}
          placeholder="Comment (optional)"
          onChange={(e) => onChange(row.learnerId, { comment: e.target.value })}
          onKeyDown={keyNav("comment", true)}
          className="h-10 w-full rounded-md border border-line bg-paper px-2.5 text-[0.95rem] placeholder:text-ink-3/70 disabled:bg-canvas"
        />
      </div>
    </li>
  );
}
