"use client";

import { useEffect, useId, useRef, type ReactNode } from "react";
import { X } from "lucide-react";
import { cx } from "@/lib/cx";

/**
 * Accessible modal built on the native <dialog> element (focus trap, Esc, top layer for free).
 * On phones it docks to the bottom of the screen like a sheet.
 */
export function Dialog({
  open,
  onClose,
  title,
  description,
  children,
  footer,
  size = "md",
}: {
  open: boolean;
  onClose: () => void;
  title: ReactNode;
  description?: ReactNode;
  children?: ReactNode;
  footer?: ReactNode;
  size?: "sm" | "md" | "lg";
}) {
  const ref = useRef<HTMLDialogElement>(null);
  const titleId = useId();
  const descId = useId();

  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    if (open && !el.open) {
      if (typeof el.showModal === "function") el.showModal();
      else el.setAttribute("open", "");
    } else if (!open && el.open) {
      if (typeof el.close === "function") el.close();
      else el.removeAttribute("open");
    }
  }, [open]);

  return (
    <dialog
      ref={ref}
      aria-labelledby={titleId}
      aria-describedby={description ? descId : undefined}
      onCancel={(e) => {
        e.preventDefault();
        onClose();
      }}
      onClick={(e) => {
        if (e.target === ref.current) onClose();
      }}
      className={cx(
        "m-0 mt-auto max-h-[92dvh] w-full max-w-none rounded-t-xl border border-line bg-paper p-0 text-ink shadow-[0_-8px_30px_rgba(23,35,59,0.18)] backdrop:bg-ink/45",
        "sm:m-auto sm:rounded-lg sm:shadow-[0_12px_40px_rgba(23,35,59,0.22)]",
        size === "sm" && "sm:max-w-md",
        size === "md" && "sm:max-w-lg",
        size === "lg" && "sm:max-w-2xl",
      )}
    >
      {open ? (
        <div className="flex max-h-[92dvh] flex-col">
          <div className="flex items-start justify-between gap-3 border-b border-line px-4 py-3 sm:px-5">
            <div>
              <h2 id={titleId} className="font-display text-lg">
                {title}
              </h2>
              {description ? (
                <p id={descId} className="mt-0.5 text-sm text-ink-2">
                  {description}
                </p>
              ) : null}
            </div>
            <button type="button" onClick={onClose} className="-mr-1 rounded-md p-1.5 text-ink-2 hover:bg-ink/5" aria-label="Close">
              <X className="size-5" aria-hidden />
            </button>
          </div>
          <div className="overflow-y-auto px-4 py-4 sm:px-5">{children}</div>
          {footer ? <div className="flex flex-wrap justify-end gap-2 border-t border-line px-4 py-3 sm:px-5">{footer}</div> : null}
        </div>
      ) : null}
    </dialog>
  );
}
