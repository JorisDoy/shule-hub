import type { ReactNode } from "react";
import { cx } from "@/lib/cx";

export function Card({ children, className, as: Tag = "section" }: { children: ReactNode; className?: string; as?: "section" | "div" | "article" }) {
  return <Tag className={cx("rounded-lg border border-line bg-paper", className)}>{children}</Tag>;
}

export function CardHeader({ title, description, actions, className }: { title: ReactNode; description?: ReactNode; actions?: ReactNode; className?: string }) {
  return (
    <div className={cx("flex flex-wrap items-start justify-between gap-3 border-b border-line px-4 py-3 sm:px-5", className)}>
      <div className="min-w-0">
        <h2 className="font-display text-lg">{title}</h2>
        {description ? <p className="mt-0.5 text-sm text-ink-2">{description}</p> : null}
      </div>
      {actions ? <div className="flex flex-wrap items-center gap-2">{actions}</div> : null}
    </div>
  );
}

export function CardBody({ children, className }: { children: ReactNode; className?: string }) {
  return <div className={cx("px-4 py-4 sm:px-5", className)}>{children}</div>;
}

/** Page title row used at the top of every app screen. */
export function PageHeader({ title, description, actions, back }: { title: ReactNode; description?: ReactNode; actions?: ReactNode; back?: ReactNode }) {
  return (
    <div className="mb-5 flex flex-wrap items-end justify-between gap-3">
      <div className="min-w-0">
        {back ? <div className="mb-1.5 text-sm">{back}</div> : null}
        <h1 className="font-display text-[1.6rem] sm:text-[1.85rem]">{title}</h1>
        {description ? <p className="mt-1 max-w-[65ch] text-ink-2">{description}</p> : null}
      </div>
      {actions ? <div className="flex flex-wrap items-center gap-2">{actions}</div> : null}
    </div>
  );
}

/** A label/value pair list, two columns on wider screens. */
export function DetailList({ items, className }: { items: { label: string; value: ReactNode }[]; className?: string }) {
  return (
    <dl className={cx("grid grid-cols-1 gap-x-6 gap-y-3 sm:grid-cols-2", className)}>
      {items.map((item) => (
        <div key={item.label} className="min-w-0">
          <dt className="text-[0.8rem] text-ink-3">{item.label}</dt>
          <dd className="font-semibold break-words">{item.value}</dd>
        </div>
      ))}
    </dl>
  );
}
