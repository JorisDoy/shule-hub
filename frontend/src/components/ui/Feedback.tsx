"use client";

import { createContext, useCallback, useContext, useMemo, useRef, useState, type ReactNode } from "react";
import { AlertTriangle, CheckCircle2, Info, X, XCircle } from "lucide-react";
import { cx } from "@/lib/cx";
import { errorMessage } from "@/lib/api/problem";
import { Button, Spinner } from "@/components/ui/Button";

type AlertTone = "info" | "success" | "warning" | "error";

const alertTone: Record<AlertTone, { box: string; Icon: typeof Info }> = {
  info: { box: "border-info/25 bg-info-tint text-info", Icon: Info },
  success: { box: "border-uniform/25 bg-uniform-tint text-uniform-dark", Icon: CheckCircle2 },
  warning: { box: "border-owe/25 bg-owe-tint text-owe", Icon: AlertTriangle },
  error: { box: "border-danger/25 bg-danger-tint text-danger", Icon: XCircle },
};

export function Alert({ tone = "info", title, children, className, action }: { tone?: AlertTone; title?: ReactNode; children?: ReactNode; className?: string; action?: ReactNode }) {
  const { box, Icon } = alertTone[tone];
  return (
    <div role={tone === "error" ? "alert" : "status"} className={cx("flex gap-3 rounded-md border px-3.5 py-3 text-[0.92rem]", box, className)}>
      <Icon aria-hidden className="mt-0.5 size-5 shrink-0" />
      <div className="min-w-0 flex-1">
        {title ? <p className="font-semibold">{title}</p> : null}
        {children ? <div className={cx(title ? "mt-0.5" : null, "text-ink")}>{children}</div> : null}
        {action ? <div className="mt-2">{action}</div> : null}
      </div>
    </div>
  );
}

/** Shows an API/JS error as a readable alert, with an optional retry. */
export function ErrorAlert({ error, onRetry, className, title }: { error: unknown; onRetry?: () => void; className?: string; title?: string }) {
  if (!error) return null;
  return (
    <Alert
      tone="error"
      title={title}
      className={className}
      action={
        onRetry ? (
          <Button size="sm" variant="secondary" onClick={onRetry}>
            Try again
          </Button>
        ) : undefined
      }
    >
      {errorMessage(error)}
    </Alert>
  );
}

export function EmptyState({ icon, title, children, action, className }: { icon?: ReactNode; title: ReactNode; children?: ReactNode; action?: ReactNode; className?: string }) {
  return (
    <div className={cx("flex flex-col items-start gap-2 px-4 py-10 sm:items-center sm:px-6 sm:text-center", className)}>
      {icon ? <div className="text-ink-3">{icon}</div> : null}
      <p className="font-display text-lg">{title}</p>
      {children ? <div className="max-w-[48ch] text-ink-2">{children}</div> : null}
      {action ? <div className="mt-2">{action}</div> : null}
    </div>
  );
}

export function Loading({ label = "Loading", className }: { label?: string; className?: string }) {
  return (
    <div className={cx("flex items-center gap-2.5 px-4 py-8 text-ink-2 sm:px-5", className)} role="status">
      <Spinner />
      <span>{label}…</span>
    </div>
  );
}

/** Grey bars while a list loads; cheaper than a spinner for perceived speed. */
export function SkeletonRows({ rows = 4 }: { rows?: number }) {
  return (
    <div className="divide-y divide-line" aria-hidden>
      {Array.from({ length: rows }, (_, i) => (
        <div key={i} className="flex items-center gap-4 px-4 py-3.5 sm:px-5">
          <div className="h-3.5 w-2/5 animate-pulse rounded-sm bg-ink/[0.07]" />
          <div className="h-3.5 w-1/5 animate-pulse rounded-sm bg-ink/[0.05]" />
        </div>
      ))}
      <span className="sr-only" role="status">
        Loading…
      </span>
    </div>
  );
}

// ---- Toasts ----

interface ToastItem {
  id: number;
  tone: AlertTone;
  message: string;
}

const ToastContext = createContext<{ show: (message: string, tone?: AlertTone) => void } | null>(null);

export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<ToastItem[]>([]);
  const nextId = useRef(1);

  const dismiss = useCallback((id: number) => setItems((all) => all.filter((t) => t.id !== id)), []);
  const show = useCallback(
    (message: string, tone: AlertTone = "success") => {
      const id = nextId.current++;
      setItems((all) => [...all.slice(-2), { id, tone, message }]);
      setTimeout(() => dismiss(id), tone === "error" ? 8000 : 4500);
    },
    [dismiss],
  );
  const value = useMemo(() => ({ show }), [show]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div aria-live="polite" className="no-print pointer-events-none fixed inset-x-0 bottom-0 z-50 flex flex-col items-center gap-2 p-3 sm:items-end sm:p-5">
        {items.map((t) => {
          const { Icon, box } = alertTone[t.tone];
          return (
            <div key={t.id} role="status" className={cx("pointer-events-auto flex w-full max-w-sm items-start gap-2.5 rounded-md border bg-paper px-3.5 py-3 shadow-[0_6px_24px_rgba(23,35,59,0.18)]", box)}>
              <Icon aria-hidden className="mt-0.5 size-5 shrink-0" />
              <p className="flex-1 text-[0.92rem] text-ink">{t.message}</p>
              <button type="button" onClick={() => dismiss(t.id)} aria-label="Dismiss" className="rounded-sm p-0.5 text-ink-2 hover:text-ink">
                <X className="size-4" aria-hidden />
              </button>
            </div>
          );
        })}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  return ctx ?? { show: () => {} };
}
