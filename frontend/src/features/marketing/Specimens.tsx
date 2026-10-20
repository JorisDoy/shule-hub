import { BandChip } from "@/components/ui/Badge";
import { Mark } from "@/components/Brand";
import { bandForPercent, CBE_4_LEVEL, KNEC_8_LEVEL } from "@/lib/bands";
import { formatKes } from "@/lib/format";
import { cx } from "@/lib/cx";

const HERO_AREAS: [string, number][] = [
  ["English", 64],
  ["Kiswahili", 71],
  ["Mathematics", 78],
  ["Integrated Science", 82],
  ["Pre-Technical Studies", 91],
  ["Social Studies", 57],
];

/** The hero specimen: a Junior School report card on exercise-book paper, with the head teacher's stamp. */
export function HeroReportCard() {
  const rows = HERO_AREAS.map(([name, percent]) => ({ name, percent, band: bandForPercent(percent, KNEC_8_LEVEL)! }));
  const total = rows.reduce((s, r) => s + r.band.points, 0);
  return (
    <figure className="relative sm:pb-28">
      <div className="paper-ruled relative rounded-md border border-line pt-5 pr-4 pb-16 pl-12 shadow-[0_1px_0_rgba(23,35,59,0.05),0_18px_40px_-18px_rgba(23,35,59,0.35)] sm:pr-6 sm:pl-14">
        <div className="flex items-start gap-2.5">
          <Mark className="mt-0.5 size-8" />
          <div>
            <p className="font-display text-lg leading-tight font-bold">Demo Academy</p>
            <p className="text-sm text-ink-2">Learner progress report, Term 1 2027</p>
          </div>
        </div>
        <span className="stamp stamp-land absolute right-5 bottom-4 text-[0.8rem]">
          <span>Released</span>
          <span className="text-[0.65rem] font-semibold">3 Apr 2027</span>
        </span>
        <p className="mt-3 text-[0.95rem]">
          <strong className="font-display">Amani Otieno</strong>
          <span className="text-ink-2">, DA/2027/0001, Grade 7 Blue</span>
        </p>
        <table className="mt-2 w-full border-collapse text-left text-[0.9rem] leading-7">
          <caption className="sr-only">Sample Junior School report card on the KNEC 8-level scale</caption>
          <thead className="text-[0.78rem] text-ink-2">
            <tr>
              <th scope="col" className="font-semibold">Learning area</th>
              <th scope="col" className="text-right font-semibold">%</th>
              <th scope="col" className="pl-3 font-semibold">Level</th>
              <th scope="col" className="text-right font-semibold">Pts</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.name}>
                <th scope="row" className="font-normal">{r.name}</th>
                <td className="text-right tabular">{r.percent}</td>
                <td className="pl-3">
                  <BandChip code={r.band.code} className="leading-5" />
                </td>
                <td className="text-right tabular">{r.band.points}</td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr>
              <th scope="row" colSpan={3} className="text-right font-semibold">
                Total points
              </th>
              <td className="text-right font-bold tabular">{total}</td>
            </tr>
          </tfoot>
        </table>
        <p className="mt-2 font-display text-[0.95rem] text-ink-2 italic">&ldquo;Amani reasons carefully in Mathematics. Keep reading every evening.&rdquo;</p>
      </div>
      <PhonePrompt className="absolute bottom-0 -left-3 hidden w-60 sm:block lg:-left-14" />
    </figure>
  );
}

/** A plain-styled phone notification showing what the payer sees after an STK push. */
export function PhonePrompt({ className, amount = 15000 }: { className?: string; amount?: number }) {
  return (
    <div className={cx("rounded-xl border border-ink/10 bg-ink p-1.5 shadow-[0_16px_36px_-12px_rgba(23,35,59,0.55)]", className)} aria-label="Example M-Pesa payment prompt on a parent's phone" role="img">
      <div className="rounded-lg bg-paper px-3.5 py-3">
        <p className="text-[0.7rem] font-semibold text-ink-3">M-Pesa</p>
        <p className="mt-1 text-[0.85rem] leading-snug">
          Pay <strong>{formatKes(amount, { whole: true })}</strong> to Demo Academy, account DA/2027/0001?
        </p>
        <div className="mt-2 rounded-sm border border-line-strong px-2 py-1.5 text-[0.8rem] text-ink-3">Enter M-Pesa PIN</div>
        <div className="mt-2 flex justify-end gap-4 text-[0.8rem] font-semibold">
          <span className="text-ink-3">Cancel</span>
          <span className="text-uniform">Send</span>
        </div>
      </div>
    </div>
  );
}

export function RubricSpecimens() {
  return (
    <div className="grid gap-5 xl:grid-cols-[1fr_1.4fr]">
      <div className="rounded-md border border-line bg-paper p-4 sm:p-5">
        <p className="font-display text-lg font-semibold">PP1 to Grade 6</p>
        <p className="text-sm text-ink-2">The 4-level CBE rubric</p>
        <table className="mt-3 w-full text-left text-[0.92rem]">
          <caption className="sr-only">4-level CBE rubric</caption>
          <thead className="border-b border-line-strong text-[0.78rem] text-ink-2">
            <tr>
              <th scope="col" className="py-1.5 font-semibold">Level</th>
              <th scope="col" className="py-1.5 font-semibold">Meaning</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-line">
            {CBE_4_LEVEL.map((l) => (
              <tr key={l.code}>
                <td className="py-2 pr-3">
                  <BandChip code={l.code} />
                </td>
                <td className="py-2">{l.label}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="rounded-md border border-line bg-paper p-4 sm:p-5">
        <p className="font-display text-lg font-semibold">Grades 7 to 9</p>
        <p className="text-sm text-ink-2">The KNEC 8-level scale for Junior School</p>
        <table className="mt-3 w-full text-left text-[0.92rem]">
          <caption className="sr-only">KNEC 8-level scale</caption>
          <thead className="border-b border-line-strong text-[0.78rem] text-ink-2">
            <tr>
              <th scope="col" className="py-1.5 font-semibold">Level</th>
              <th scope="col" className="py-1.5 pl-3 text-right font-semibold">Marks</th>
              <th scope="col" className="py-1.5 pl-3 text-right font-semibold">Points</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-line">
            {KNEC_8_LEVEL.map((b) => (
              <tr key={b.code}>
                <td className="py-1.5 pr-3">
                  <span className="flex items-center gap-2">
                    <BandChip code={b.code} />
                    <span className="hidden text-sm leading-tight text-ink-2 sm:inline">{b.label}</span>
                  </span>
                </td>
                <td className="py-1.5 pl-3 text-right whitespace-nowrap tabular">
                  {b.minPercent}–{b.maxPercent}%
                </td>
                <td className="py-1.5 pl-3 text-right tabular">{b.points}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

const AGING = [
  { label: "Not due", amount: 182000 },
  { label: "1–30 days", amount: 246500 },
  { label: "31–60 days", amount: 98000 },
  { label: "61–90 days", amount: 41500 },
  { label: "Over 90 days", amount: 27000 },
];

export function ArrearsSpecimen() {
  const total = AGING.reduce((s, a) => s + a.amount, 0);
  const max = Math.max(...AGING.map((a) => a.amount));
  return (
    <figure className="rounded-md border border-line bg-paper p-4 sm:p-5">
      <figcaption className="flex flex-wrap items-baseline justify-between gap-2">
        <span className="font-display text-lg font-semibold">Arrears as of 15 Feb 2027</span>
        <span className="text-ink-2">
          Total <strong className="text-ink tabular">{formatKes(total, { whole: true })}</strong>
        </span>
      </figcaption>
      <ul className="mt-4 flex flex-col gap-2.5">
        {AGING.map((a, i) => (
          <li key={a.label} className="grid grid-cols-[6.5rem_1fr_6.5rem] items-center gap-3 text-[0.92rem]">
            <span className="text-ink-2">{a.label}</span>
            <span className="h-3 rounded-sm bg-canvas">
              <span className={cx("block h-full rounded-sm", i === 0 ? "bg-line-strong" : i < 3 ? "bg-owe/70" : "bg-danger/80")} style={{ width: `${(a.amount / max) * 100}%` }} />
            </span>
            <span className="text-right font-semibold tabular">{formatKes(a.amount, { whole: true })}</span>
          </li>
        ))}
      </ul>
    </figure>
  );
}

export function ParentPhone() {
  return (
    <div className="mx-auto w-full max-w-[19rem] rounded-[1.6rem] border border-ink/15 bg-ink p-2.5 shadow-[0_24px_50px_-24px_rgba(23,35,59,0.6)]" role="img" aria-label="The parent app on a phone, showing a child's fee balance and a Pay with M-Pesa button">
      <div className="overflow-hidden rounded-[1.15rem] bg-canvas">
        <div className="flex items-center gap-2 border-b border-line bg-paper px-3 py-2.5">
          <Mark className="size-5" />
          <span className="font-display text-sm font-bold">Demo Academy</span>
        </div>
        <div className="p-3">
          <p className="font-display text-base font-semibold">Hello, Grace</p>
          <div className="mt-2 rounded-md border border-line bg-paper">
            <div className="px-3 pt-3">
              <p className="font-display font-semibold">Neema Wanjiru</p>
              <p className="text-[0.8rem] text-ink-2">PP1 Sunflower, adm. DA/2027/0042</p>
            </div>
            <div className="mx-3 mt-2 flex items-baseline justify-between rounded-sm bg-owe-tint px-2.5 py-2">
              <span className="text-[0.75rem] font-semibold text-owe">Balance due</span>
              <span className="font-bold text-owe tabular">{formatKes(12500)}</span>
            </div>
            <div className="grid grid-cols-2 gap-1.5 p-3">
              <span className="col-span-2 rounded-sm bg-uniform py-2 text-center text-[0.8rem] font-semibold text-white">Pay with M-Pesa</span>
              <span className="rounded-sm border border-line-strong py-1.5 text-center text-[0.75rem] font-semibold">Statement</span>
              <span className="rounded-sm border border-line-strong py-1.5 text-center text-[0.75rem] font-semibold">Report cards</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
