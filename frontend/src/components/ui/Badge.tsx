import type { ReactNode } from "react";
import { cx } from "@/lib/cx";
import type { Tone } from "@/lib/labels";
import { bandTone } from "@/lib/bands";

const tones: Record<Tone, string> = {
  neutral: "bg-ink/[0.06] text-ink-2",
  green: "bg-uniform-tint text-uniform-dark",
  stamp: "bg-stamp-tint text-stamp",
  owe: "bg-owe-tint text-owe",
  danger: "bg-danger-tint text-danger",
  info: "bg-info-tint text-info",
};

export function Badge({ tone = "neutral", children, className }: { tone?: Tone; children: ReactNode; className?: string }) {
  return (
    <span className={cx("inline-flex items-center rounded-sm px-2 py-0.5 text-[0.8rem] leading-5 font-semibold whitespace-nowrap", tones[tone], className)}>
      {children}
    </span>
  );
}

const bandTones = {
  ee: "bg-uniform text-white",
  me: "bg-uniform-tint text-uniform-dark",
  ae: "bg-owe-tint text-owe",
  be: "bg-danger-tint text-danger",
  none: "bg-ink/[0.06] text-ink-2",
};

/** A CBE band chip: EE greens, ME pale green, AE amber, BE red. Text is always the code itself. */
export function BandChip({ code, className, title }: { code: string | null | undefined; className?: string; title?: string }) {
  if (!code) return <span className="text-ink-3">—</span>;
  return (
    <span
      title={title}
      className={cx("inline-flex min-w-10 items-center justify-center rounded-sm px-1.5 text-[0.8rem] leading-6 font-bold tabular", bandTones[bandTone(code)], className)}
    >
      {code}
    </span>
  );
}
