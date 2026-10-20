"use client";

import Link from "next/link";
import type { ReactNode } from "react";
import { Mark } from "@/components/Brand";
import { useSession } from "@/features/session/SessionProvider";
import { UserMenu } from "@/features/shell/UserMenu";

export function ParentShell({ children }: { children: ReactNode }) {
  const session = useSession();
  return (
    <div className="min-h-dvh">
      <a href="#main" className="sr-only z-50 rounded-md bg-paper px-3 py-2 focus:not-sr-only focus:absolute focus:top-2 focus:left-2">
        Skip to content
      </a>
      <header className="no-print sticky top-0 z-30 border-b border-line bg-paper">
        <div className="mx-auto flex h-14 max-w-3xl items-center gap-2 px-4">
          <Link href="/app/parent" className="flex min-w-0 items-center gap-2.5">
            <Mark className="size-7" />
            <span className="min-w-0">
              <span className="block truncate font-display text-[1.05rem] leading-tight font-bold">{session.membership.tenantName}</span>
              <span className="block text-[0.75rem] leading-tight text-ink-3">Parent portal</span>
            </span>
          </Link>
          <div className="ml-auto flex items-center gap-2">
            {session.isStaff ? (
              <Link href="/app" className="rounded-md px-2.5 py-1.5 text-sm font-semibold text-ink-2 hover:bg-ink/5 hover:text-ink">
                Staff view
              </Link>
            ) : null}
            <UserMenu view="parent" />
          </div>
        </div>
      </header>
      <main id="main" className="mx-auto max-w-3xl px-4 pt-5 pb-16">
        {children}
      </main>
    </div>
  );
}
