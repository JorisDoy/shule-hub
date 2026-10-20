"use client";

import Link from "next/link";
import { useEffect, useId, useRef, useState } from "react";
import { ArrowLeftRight, ChevronDown, LogOut, School, Users } from "lucide-react";
import { useSession } from "@/features/session/SessionProvider";
import { cx } from "@/lib/cx";

function initials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  return ((parts[0]?.[0] ?? "") + (parts.length > 1 ? parts[parts.length - 1][0] : "")).toUpperCase() || "?";
}

export function UserMenu({ view }: { view: "staff" | "parent" }) {
  const session = useSession();
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);
  const menuId = useId();

  useEffect(() => {
    if (!open) return;
    const onDoc = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") setOpen(false);
    };
    document.addEventListener("mousedown", onDoc);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onDoc);
      document.removeEventListener("keydown", onKey);
    };
  }, [open]);

  const itemClass = "flex w-full items-center gap-2.5 px-3.5 py-2.5 text-left text-[0.95rem] hover:bg-canvas";

  return (
    <div ref={ref} className="relative">
      <button
        type="button"
        aria-expanded={open}
        aria-controls={menuId}
        onClick={() => setOpen((o) => !o)}
        className="flex items-center gap-1.5 rounded-md py-1 pr-1.5 pl-1 hover:bg-ink/5"
      >
        <span className="flex size-8 items-center justify-center rounded-full bg-ink text-[0.8rem] font-bold text-white" aria-hidden>
          {initials(session.userName)}
        </span>
        <span className="sr-only">Account menu for {session.userName}</span>
        <ChevronDown className="size-4 text-ink-2" aria-hidden />
      </button>
      {open ? (
        <div id={menuId} className="absolute right-0 z-40 mt-1.5 w-72 overflow-hidden rounded-md border border-line bg-paper shadow-[0_8px_28px_rgba(23,35,59,0.16)]">
          <div className="border-b border-line px-3.5 py-3">
            <p className="font-semibold">{session.userName}</p>
            {session.userEmail ? <p className="truncate text-sm text-ink-2">{session.userEmail}</p> : null}
            <p className="mt-1 text-sm text-ink-2">{session.membership.tenantName}</p>
          </div>
          <div className="py-1">
            {view === "staff" && session.isParent ? (
              <Link href="/app/parent" className={itemClass} onClick={() => setOpen(false)}>
                <Users className="size-4 text-ink-2" aria-hidden /> Parent view
              </Link>
            ) : null}
            {view === "parent" && session.isStaff ? (
              <Link href="/app" className={itemClass} onClick={() => setOpen(false)}>
                <School className="size-4 text-ink-2" aria-hidden /> Staff view
              </Link>
            ) : null}
            {session.memberships.length > 1 ? (
              <button type="button" className={itemClass} onClick={session.switchTenant}>
                <ArrowLeftRight className="size-4 text-ink-2" aria-hidden /> Switch school
              </button>
            ) : null}
            <button type="button" className={cx(itemClass, "text-danger")} onClick={session.signOut}>
              <LogOut className="size-4" aria-hidden /> Sign out
            </button>
          </div>
        </div>
      ) : null}
    </div>
  );
}
