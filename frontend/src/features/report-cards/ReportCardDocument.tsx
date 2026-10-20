import type { ReactNode } from "react";
import type { ReportCard } from "@/lib/api/types";
import { gradeLabel } from "@/lib/grades";
import { formatDate } from "@/lib/format";
import { BandChip } from "@/components/ui/Badge";
import { Mark } from "@/components/Brand";
import { cx } from "@/lib/cx";

export interface ReportCardEditing {
  areaComments: Record<string, string>;
  classTeacherComment: string;
  headComment: string;
  onAreaComment: (learningAreaId: string, value: string) => void;
  onClassTeacherComment: (value: string) => void;
  onHeadComment: (value: string) => void;
}

function CommentSlot({ label, value, editing, onChange }: { label: string; value: string | null; editing?: boolean; onChange?: (v: string) => void }) {
  return (
    <div className="break-inside-avoid">
      <p className="text-sm font-semibold text-ink-2">{label}</p>
      {editing && onChange ? (
        <>
          <label className="sr-only" htmlFor={`slot-${label}`}>
            {label}
          </label>
          <textarea
            id={`slot-${label}`}
            rows={3}
            value={value ?? ""}
            onChange={(e) => onChange(e.target.value)}
            className="no-print mt-1 block w-full rounded-md border border-line-strong bg-paper px-3 py-2 leading-snug"
          />
          <p className="hidden min-h-[3.5rem] leading-7 print:block">{value}</p>
        </>
      ) : (
        <p className="mt-1 min-h-[3.5rem] leading-7 whitespace-pre-line">{value || <span className="text-ink-3">No comment yet.</span>}</p>
      )}
    </div>
  );
}

/**
 * The learner's CBE progress report, drawn as a printed school document. Prints on A4
 * (the page's print CSS shows only `.print-area`). Pass `editing` to turn the comment slots into inputs.
 */
export function ReportCardDocument({ card, schoolName, editing, footer }: { card: ReportCard; schoolName: string; editing?: ReportCardEditing; footer?: ReactNode }) {
  const isEditing = !!editing;
  const totalPoints = card.areas.reduce((sum, a) => sum + (a.points ?? 0), 0);
  const hasPoints = card.areas.some((a) => a.points !== null && a.points !== undefined);

  return (
    <article className="print-area paper-margin relative min-w-0 rounded-lg border border-line py-6 pr-4 pl-12 shadow-[0_1px_2px_rgba(23,35,59,0.06)] sm:py-8 sm:pr-8 sm:pl-16">
      <header className="flex flex-wrap items-start justify-between gap-x-4 gap-y-3 border-b-2 border-ink pb-4">
        <div className="flex min-w-0 flex-1 basis-60 items-start gap-3">
          <Mark className="mt-0.5 size-10" />
          <div>
            <p className="font-display text-xl leading-tight font-bold sm:text-2xl">{schoolName}</p>
            <p className="text-ink-2">Learner progress report, Term {card.term.number} {card.term.year}</p>
          </div>
        </div>
        {card.status === "RELEASED" ? (
          <span className="stamp shrink-0 text-sm" aria-label={`Released ${card.releasedAt ? formatDate(card.releasedAt) : ""}`}>
            <span>Released</span>
            {card.releasedAt ? <span className="text-[0.7rem] font-semibold">{formatDate(card.releasedAt)}</span> : null}
          </span>
        ) : card.status === "APPROVED" ? (
          <span className="stamp shrink-0 text-sm opacity-80">Approved</span>
        ) : (
          <span className="no-print shrink-0 rounded-sm border border-dashed border-line-strong px-2 py-1 text-sm text-ink-3">Draft</span>
        )}
      </header>

      <dl className="mt-4 grid grid-cols-2 gap-x-6 gap-y-2 sm:grid-cols-4">
        <div className="col-span-2">
          <dt className="text-sm text-ink-3">Learner</dt>
          <dd className="font-display text-lg font-semibold">{card.learner.fullName}</dd>
        </div>
        <div>
          <dt className="text-sm text-ink-3">Admission no.</dt>
          <dd className="font-semibold tabular">{card.learner.admissionNumber}</dd>
        </div>
        <div>
          <dt className="text-sm text-ink-3">Class</dt>
          <dd className="font-semibold">{card.learner.streamName ?? gradeLabel(card.learner.gradeLevel)}</dd>
        </div>
      </dl>

      <div className="mt-5">
        <table className="block w-full border-collapse text-left text-[0.92rem] sm:table print:table">
          <caption className="block pb-2 text-left text-sm text-ink-2 sm:table-caption print:table-caption">Graded on the {card.scale?.name ?? "school rubric"}.</caption>
          <thead className="hidden border-y border-ink/70 text-[0.8rem] sm:table-header-group print:table-header-group">
            <tr>
              <th scope="col" className="py-2 pr-3 font-semibold">Learning area</th>
              <th scope="col" className="px-2 py-2 text-right font-semibold">%</th>
              <th scope="col" className="px-2 py-2 font-semibold">Level</th>
              {hasPoints ? <th scope="col" className="px-2 py-2 text-right font-semibold">Points</th> : null}
              <th scope="col" className="hidden py-2 pl-3 font-semibold sm:table-cell print:table-cell">Comment</th>
            </tr>
          </thead>
          <tbody className="block divide-y divide-rule border-t border-ink/70 sm:table-row-group sm:border-t-0 print:table-row-group print:border-t-0">
            {card.areas.map((a) => (
              <tr key={a.learningAreaId} className="flex flex-wrap items-start align-top sm:table-row print:table-row">
                <th scope="row" className="w-full pt-2.5 pr-3 font-semibold sm:w-auto sm:py-2.5 print:w-auto print:py-2.5">
                  {a.name}
                </th>
                <td className="py-1.5 pr-3 tabular sm:px-2 sm:py-2.5 sm:text-right print:px-2 print:py-2.5 print:text-right">
                  {a.percent ?? "—"}
                  <span className="sm:hidden print:hidden">%</span>
                </td>
                <td className="py-1 sm:px-2 sm:py-2.5 print:px-2 print:py-2.5">
                  <span className="flex flex-wrap items-center gap-x-2 gap-y-1">
                    <BandChip code={a.bandCode} />
                    {a.bandLabel ? <span className="text-sm text-ink-2">{a.bandLabel}</span> : null}
                  </span>
                </td>
                {hasPoints ? (
                  <td className="ml-auto py-1.5 tabular sm:ml-0 sm:px-2 sm:py-2.5 sm:text-right print:ml-0 print:px-2 print:py-2.5 print:text-right">
                    {a.points ?? "—"}
                    <span className="text-sm text-ink-3 sm:hidden print:hidden"> pts</span>
                  </td>
                ) : null}
                <td className={cx("w-full pb-2.5 sm:w-auto sm:py-2 sm:pl-3 print:w-auto print:py-2 print:pl-3", isEditing ? "sm:min-w-[14rem]" : "")}>
                  {editing ? (
                    <>
                      <label className="sr-only" htmlFor={`area-${a.learningAreaId}`}>
                        Comment for {a.name}
                      </label>
                      <textarea
                        id={`area-${a.learningAreaId}`}
                        rows={2}
                        value={editing.areaComments[a.learningAreaId] ?? ""}
                        onChange={(e) => editing.onAreaComment(a.learningAreaId, e.target.value)}
                        className="no-print block w-full rounded-md border border-line-strong bg-paper px-2.5 py-1.5 text-[0.92rem] leading-snug"
                      />
                      <span className="hidden print:inline">{editing.areaComments[a.learningAreaId]}</span>
                    </>
                  ) : (
                    a.comment || <span className="hidden text-ink-3 sm:inline print:inline">—</span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
          {hasPoints ? (
            <tfoot className="block border-t border-ink/70 sm:table-footer-group print:table-footer-group">
              <tr className="flex justify-between sm:table-row print:table-row">
                <th scope="row" className="py-2 pr-3 font-semibold sm:text-left" colSpan={3}>
                  Total points
                </th>
                <td className="px-2 py-2 text-right font-bold tabular">{totalPoints}</td>
                <td className="hidden sm:table-cell print:table-cell" />
              </tr>
            </tfoot>
          ) : null}
        </table>
        {card.areas.length === 0 ? <p className="py-6 text-ink-2">No learning areas have been assessed this term.</p> : null}
      </div>

      <div className="mt-6 grid gap-5 sm:grid-cols-2">
        <CommentSlot label="Class teacher" value={editing ? editing.classTeacherComment : card.classTeacherComment} editing={isEditing} onChange={editing?.onClassTeacherComment} />
        <CommentSlot label="Head teacher" value={editing ? editing.headComment : card.headComment} editing={isEditing} onChange={editing?.onHeadComment} />
      </div>
      {footer ? <div className="no-print mt-6">{footer}</div> : null}
    </article>
  );
}
