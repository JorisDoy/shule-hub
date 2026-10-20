import { useState } from "react";
import { describe, expect, it, vi } from "vitest";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { GradingBand, ScoreRow } from "@/lib/api/types";
import { KNEC_8_LEVEL } from "@/lib/bands";
import { ScoreEntryRow, type GridColumn, type ScoreDraft } from "@/features/assessment/ScoreEntryRow";

const rows: ScoreRow[] = [
  { learnerId: "a", admissionNumber: "DA/2027/0001", learnerName: "Amani Otieno", rawScore: null, rubricLevel: null, percent: null, bandCode: null, comment: null },
  { learnerId: "b", admissionNumber: "DA/2027/0002", learnerName: "Baraka Mwangi", rawScore: null, rubricLevel: null, percent: null, bandCode: null, comment: null },
];

const CBE4: GradingBand[] = [
  { code: "EE", label: "Exceeding Expectations", minPercent: 75, maxPercent: 100, points: 4, descriptor: null },
  { code: "ME", label: "Meeting Expectations", minPercent: 50, maxPercent: 74, points: 3, descriptor: null },
  { code: "AE", label: "Approaching Expectations", minPercent: 25, maxPercent: 49, points: 2, descriptor: null },
  { code: "BE", label: "Below Expectations", minPercent: 0, maxPercent: 24, points: 1, descriptor: null },
];

/** A two-row grid wired like the real page, so keyboard navigation can be exercised. */
function Grid({ maxScore, bands, onChange }: { maxScore: number | null; bands: GradingBand[]; onChange?: (id: string, patch: Partial<ScoreDraft>) => void }) {
  const [drafts, setDrafts] = useState<Record<string, ScoreDraft>>({ a: { raw: "", rubric: "", comment: "" }, b: { raw: "", rubric: "", comment: "" } });
  const [fields] = useState(() => new Map<string, HTMLElement>());
  return (
    <ol>
      {rows.map((r, i) => (
        <ScoreEntryRow
          key={r.learnerId}
          row={r}
          index={i}
          draft={drafts[r.learnerId]}
          maxScore={maxScore}
          bands={bands}
          onChange={(id, patch) => {
            onChange?.(id, patch);
            setDrafts((d) => ({ ...d, [id]: { ...d[id], ...patch } }));
          }}
          registerField={(c: GridColumn, idx, el) => {
            if (el) fields.set(`${c}:${idx}`, el);
          }}
          onNavigate={(c, idx, delta) => fields.get(`${c}:${idx + delta}`)?.focus()}
        />
      ))}
    </ol>
  );
}

describe("ScoreEntryRow", () => {
  it("shows the live percent and KNEC band: 149/200 → 75% → EE2", async () => {
    const user = userEvent.setup();
    render(<Grid maxScore={200} bands={KNEC_8_LEVEL} />);
    const input = screen.getByLabelText("Score for Amani Otieno out of 200");
    await user.type(input, "149");
    const preview = screen.getByTestId("preview-a");
    expect(within(preview).getByText("75%")).toBeInTheDocument();
    expect(within(preview).getByText("EE2")).toBeInTheDocument();
  });

  it("updates the band as the score changes across a boundary", async () => {
    const user = userEvent.setup();
    render(<Grid maxScore={50} bands={KNEC_8_LEVEL} />);
    const input = screen.getByLabelText("Score for Amani Otieno out of 50");
    await user.type(input, "29"); // 58% → ME1
    expect(within(screen.getByTestId("preview-a")).getByText("ME1")).toBeInTheDocument();
    await user.clear(input);
    await user.type(input, "28.5"); // 57% → ME2
    expect(within(screen.getByTestId("preview-a")).getByText("57%")).toBeInTheDocument();
    expect(within(screen.getByTestId("preview-a")).getByText("ME2")).toBeInTheDocument();
  });

  it("flags scores above the maximum and shows no band", async () => {
    const user = userEvent.setup();
    render(<Grid maxScore={40} bands={KNEC_8_LEVEL} />);
    const input = screen.getByLabelText("Score for Amani Otieno out of 40");
    await user.type(input, "41");
    expect(screen.getByText("Max 40")).toBeInTheDocument();
    expect(input).toHaveAttribute("aria-invalid", "true");
    expect(within(screen.getByTestId("preview-a")).queryByText(/%/)).not.toBeInTheDocument();
  });

  it("moves down with Enter and ArrowDown, up with ArrowUp", async () => {
    const user = userEvent.setup();
    render(<Grid maxScore={100} bands={KNEC_8_LEVEL} />);
    const first = screen.getByLabelText("Score for Amani Otieno out of 100");
    const second = screen.getByLabelText("Score for Baraka Mwangi out of 100");
    await user.click(first);
    await user.keyboard("80{Enter}");
    expect(second).toHaveFocus();
    await user.keyboard("{ArrowUp}");
    expect(first).toHaveFocus();
    await user.keyboard("{ArrowDown}");
    expect(second).toHaveFocus();
  });

  it("uses a band select when the assessment is rubric-only", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<Grid maxScore={null} bands={CBE4} onChange={onChange} />);
    const select = screen.getByLabelText("Level for Amani Otieno");
    expect(select.tagName).toBe("SELECT");
    await user.selectOptions(select, "ME");
    expect(onChange).toHaveBeenCalledWith("a", { rubric: "ME" });
    expect(within(screen.getByTestId("preview-a")).getByText("ME")).toBeInTheDocument();
    expect(screen.queryByLabelText(/Score for/)).not.toBeInTheDocument();
  });
});
