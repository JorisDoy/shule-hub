import type { ReactNode, ThHTMLAttributes, TdHTMLAttributes } from "react";
import { ArrowDown, ArrowUp, ArrowUpDown } from "lucide-react";
import { cx } from "@/lib/cx";

/**
 * Tables scroll horizontally inside their own box on narrow screens, so the page itself never
 * scrolls sideways. Wrap rows that need stacking in <ResponsiveList> instead.
 */
export function Table({ children, className, caption }: { children: ReactNode; className?: string; caption?: string }) {
  return (
    <div className={cx("overflow-x-auto", className)}>
      <table className="w-full border-collapse text-left text-[0.92rem]">
        {caption ? <caption className="sr-only">{caption}</caption> : null}
        {children}
      </table>
    </div>
  );
}

export function THead({ children }: { children: ReactNode }) {
  return <thead className="border-b border-line-strong text-[0.8rem] text-ink-2">{children}</thead>;
}

export function TBody({ children }: { children: ReactNode }) {
  return <tbody className="divide-y divide-line">{children}</tbody>;
}

export function Th({ children, className, numeric, ...rest }: ThHTMLAttributes<HTMLTableCellElement> & { numeric?: boolean }) {
  return (
    <th scope="col" className={cx("px-3 py-2 font-semibold whitespace-nowrap first:pl-4 last:pr-4 sm:first:pl-5 sm:last:pr-5", numeric && "text-right", className)} {...rest}>
      {children}
    </th>
  );
}

export function Td({ children, className, numeric, ...rest }: TdHTMLAttributes<HTMLTableCellElement> & { numeric?: boolean }) {
  return (
    <td className={cx("px-3 py-2.5 align-middle first:pl-4 last:pr-4 sm:first:pl-5 sm:last:pr-5", numeric && "text-right tabular whitespace-nowrap", className)} {...rest}>
      {children}
    </td>
  );
}

export type SortDir = "asc" | "desc";

export function SortableTh({
  children,
  active,
  dir,
  onSort,
  numeric,
}: {
  children: ReactNode;
  active: boolean;
  dir: SortDir;
  onSort: () => void;
  numeric?: boolean;
}) {
  const Icon = !active ? ArrowUpDown : dir === "asc" ? ArrowUp : ArrowDown;
  return (
    <th
      scope="col"
      aria-sort={active ? (dir === "asc" ? "ascending" : "descending") : "none"}
      className={cx("px-3 py-2 font-semibold whitespace-nowrap first:pl-4 last:pr-4 sm:first:pl-5 sm:last:pr-5", numeric && "text-right")}
    >
      <button type="button" onClick={onSort} className={cx("inline-flex items-center gap-1 rounded-sm hover:text-ink", active && "text-ink")}>
        {children}
        <Icon aria-hidden className="size-3.5" />
      </button>
    </th>
  );
}

/** Small "Showing x of y" plus prev/next pager. */
export function Pager({ page, totalPages, totalElements, size, onPage }: { page: number; totalPages: number; totalElements: number; size: number; onPage: (p: number) => void }) {
  if (totalElements === 0) return null;
  const from = page * size + 1;
  const to = Math.min(totalElements, (page + 1) * size);
  return (
    <nav aria-label="Pagination" className="flex flex-wrap items-center justify-between gap-2 border-t border-line px-4 py-3 text-sm sm:px-5">
      <p className="text-ink-2">
        {from}–{to} of {totalElements}
      </p>
      <div className="flex gap-2">
        <button
          type="button"
          className="h-9 rounded-md border border-line-strong bg-paper px-3 font-semibold disabled:opacity-50"
          onClick={() => onPage(page - 1)}
          disabled={page <= 0}
        >
          Previous
        </button>
        <button
          type="button"
          className="h-9 rounded-md border border-line-strong bg-paper px-3 font-semibold disabled:opacity-50"
          onClick={() => onPage(page + 1)}
          disabled={page + 1 >= totalPages}
        >
          Next
        </button>
      </div>
    </nav>
  );
}
