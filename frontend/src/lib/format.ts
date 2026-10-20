// Kenya formatting helpers. Deterministic output (no locale month names) so server and
// client render identically and we always get "5 Sep 2027", never "5 Sept 2027".

const KES = new Intl.NumberFormat("en-KE", { style: "currency", currency: "KES" });
const KES_WHOLE = new Intl.NumberFormat("en-KE", {
  style: "currency",
  currency: "KES",
  minimumFractionDigits: 0,
  maximumFractionDigits: 0,
});
const PLAIN = new Intl.NumberFormat("en-KE", { maximumFractionDigits: 2 });

const MONTHS = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"] as const;

/** Kenya shillings, e.g. "Ksh 12,500.00". `whole` drops the cents for prices and headline figures. */
export function formatKes(amount: number | null | undefined, opts: { whole?: boolean } = {}): string {
  if (amount === null || amount === undefined || Number.isNaN(amount)) return "—";
  return (opts.whole ? KES_WHOLE : KES).format(amount);
}

export function formatNumber(value: number | null | undefined): string {
  if (value === null || value === undefined || Number.isNaN(value)) return "—";
  return PLAIN.format(value);
}

export type BalanceTone = "owes" | "credit" | "settled";

/** Statement balance: > 0 the family owes, < 0 is a credit. */
export function balanceTone(balance: number): BalanceTone {
  if (balance > 0.004) return "owes";
  if (balance < -0.004) return "credit";
  return "settled";
}

export function describeBalance(balance: number): string {
  const tone = balanceTone(balance);
  if (tone === "owes") return `Owes ${formatKes(balance)}`;
  if (tone === "credit") return `${formatKes(Math.abs(balance))} credit`;
  return "Fully paid";
}

/** Parses money typed by a person: "12,500", "12500.50", " 300 ". Returns null when not a valid non-negative amount. */
export function parseMoneyInput(input: string): number | null {
  const cleaned = input.replace(/[,\s]/g, "").replace(/^(ksh|kes)\.?/i, "");
  if (!/^\d+(\.\d{1,2})?$/.test(cleaned)) return null;
  return Number(cleaned);
}

const ISO_DATE = /^(\d{4})-(\d{2})-(\d{2})/;

/** "2027-01-05" → "5 Jan 2027". Accepts full instants too (uses the Nairobi calendar day). */
export function formatDate(value: string | null | undefined): string {
  if (!value) return "—";
  if (value.length > 10 && value.includes("T")) {
    const d = toNairobi(value);
    if (!d) return value;
    return `${d.day} ${MONTHS[d.month - 1]} ${d.year}`;
  }
  const m = ISO_DATE.exec(value);
  if (!m) return value;
  const month = Number(m[2]);
  if (month < 1 || month > 12) return value;
  return `${Number(m[3])} ${MONTHS[month - 1]} ${m[1]}`;
}

/** Instant → "5 Jan 2027, 10:30" in East Africa Time (UTC+3, no daylight saving). */
export function formatDateTime(value: string | null | undefined): string {
  if (!value) return "—";
  const d = toNairobi(value);
  if (!d) return value;
  return `${d.day} ${MONTHS[d.month - 1]} ${d.year}, ${String(d.hour).padStart(2, "0")}:${String(d.minute).padStart(2, "0")}`;
}

function toNairobi(instant: string) {
  const ms = Date.parse(instant);
  if (Number.isNaN(ms)) return null;
  const d = new Date(ms + 3 * 60 * 60 * 1000);
  return {
    year: d.getUTCFullYear(),
    month: d.getUTCMonth() + 1,
    day: d.getUTCDate(),
    hour: d.getUTCHours(),
    minute: d.getUTCMinutes(),
  };
}

/** Today's date in Nairobi as "YYYY-MM-DD". */
export function todayIso(now: Date = new Date()): string {
  const d = new Date(now.getTime() + 3 * 60 * 60 * 1000);
  return d.toISOString().slice(0, 10);
}

export function addDaysIso(iso: string, days: number): string {
  const [y, m, d] = iso.split("-").map(Number);
  const date = new Date(Date.UTC(y, m - 1, d + days));
  return date.toISOString().slice(0, 10);
}

export function termLabel(term: { number: number; year: number }): string {
  return `Term ${term.number} ${term.year}`;
}
