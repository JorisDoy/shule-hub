"use client";

import { useRef, type KeyboardEvent } from "react";
import { cx } from "@/lib/cx";

export interface TabItem {
  value: string;
  label: string;
  count?: number;
}

/**
 * ARIA tabs (roving tabindex, arrow keys). The panel is rendered by the caller with
 * id `${idBase}-panel` and aria-labelledby `${idBase}-tab-${value}`.
 */
export function Tabs({
  items,
  value,
  onChange,
  label,
  idBase,
  className,
}: {
  items: TabItem[];
  value: string;
  onChange: (value: string) => void;
  label: string;
  idBase: string;
  className?: string;
}) {
  const refs = useRef<(HTMLButtonElement | null)[]>([]);

  function onKeyDown(e: KeyboardEvent<HTMLButtonElement>, index: number) {
    let next = -1;
    if (e.key === "ArrowRight") next = (index + 1) % items.length;
    else if (e.key === "ArrowLeft") next = (index - 1 + items.length) % items.length;
    else if (e.key === "Home") next = 0;
    else if (e.key === "End") next = items.length - 1;
    if (next >= 0) {
      e.preventDefault();
      refs.current[next]?.focus();
      onChange(items[next].value);
    }
  }

  return (
    <div role="tablist" aria-label={label} className={cx("-mx-4 flex gap-1 overflow-x-auto border-b border-line px-4 sm:mx-0 sm:px-0", className)}>
      {items.map((item, i) => {
        const selected = item.value === value;
        return (
          <button
            key={item.value}
            ref={(el) => {
              refs.current[i] = el;
            }}
            type="button"
            role="tab"
            id={`${idBase}-tab-${item.value}`}
            aria-selected={selected}
            aria-controls={`${idBase}-panel`}
            tabIndex={selected ? 0 : -1}
            onClick={() => onChange(item.value)}
            onKeyDown={(e) => onKeyDown(e, i)}
            className={cx(
              "-mb-px flex shrink-0 items-center gap-1.5 border-b-[3px] px-3 py-2.5 text-[0.95rem] font-semibold whitespace-nowrap",
              selected ? "border-uniform text-ink" : "border-transparent text-ink-2 hover:text-ink",
            )}
          >
            {item.label}
            {item.count !== undefined ? <span className="rounded-sm bg-ink/[0.06] px-1.5 text-[0.75rem] tabular">{item.count}</span> : null}
          </button>
        );
      })}
    </div>
  );
}
