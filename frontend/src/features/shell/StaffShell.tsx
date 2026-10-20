"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState, type ReactNode } from "react";
import { BookOpenCheck, ClipboardList, GraduationCap, LayoutDashboard, Menu, Settings, Users, Wallet, X } from "lucide-react";
import { Mark } from "@/components/Brand";
import { useSession } from "@/features/session/SessionProvider";
import { UserMenu } from "@/features/shell/UserMenu";
import { navItemsFor, type NavIcon } from "@/lib/permissions";
import { cx } from "@/lib/cx";

const ICONS: Record<NavIcon, typeof LayoutDashboard> = {
  dashboard: LayoutDashboard,
  admissions: ClipboardList,
  learners: GraduationCap,
  fees: Wallet,
  assessment: BookOpenCheck,
  settings: Settings,
};

function isActive(pathname: string, href: string) {
  if (href === "/app") return pathname === "/app";
  return pathname === href || pathname.startsWith(`${href}/`);
}

function BranchSelect({ id, className }: { id: string; className?: string }) {
  const { branches, branch, setBranchId, branchesLoading } = useSession();
  if (branchesLoading && branches.length === 0) return <span className={cx("text-sm text-ink-3", className)}>Loading branches…</span>;
  if (branches.length === 0) return <span className={cx("text-sm text-ink-3", className)}>No branches yet</span>;
  if (branches.length === 1) {
    return (
      <span className={cx("text-sm text-ink-2", className)}>
        <span className="sr-only">Branch: </span>
        {branches[0].name} branch
      </span>
    );
  }
  return (
    <div className={cx("flex items-center gap-2", className)}>
      <label htmlFor={id} className="text-sm text-ink-2">
        Branch
      </label>
      <select
        id={id}
        value={branch?.id ?? ""}
        onChange={(e) => setBranchId(e.target.value)}
        className="h-9 min-w-0 rounded-md border border-line-strong bg-paper pr-2 pl-2.5 text-[0.95rem] font-semibold"
      >
        {branches.map((b) => (
          <option key={b.id} value={b.id}>
            {b.name}
          </option>
        ))}
      </select>
    </div>
  );
}

export function StaffShell({ children }: { children: ReactNode }) {
  const session = useSession();
  const pathname = usePathname() ?? "/app";
  const [navOpen, setNavOpen] = useState(false);
  const items = navItemsFor(session.permissions);

  const nav = (
    <ul className="flex flex-col gap-0.5">
      {items.map((item) => {
        const Icon = ICONS[item.key];
        const active = isActive(pathname, item.href);
        return (
          <li key={item.href}>
            <Link
              href={item.href}
              aria-current={active ? "page" : undefined}
              onClick={() => setNavOpen(false)}
              className={cx(
                "relative flex items-center gap-3 rounded-md px-3 py-2.5 text-[0.97rem] font-semibold",
                active ? "bg-paper text-ink shadow-[0_1px_0_rgba(23,35,59,0.06)]" : "text-ink-2 hover:bg-paper/70 hover:text-ink",
              )}
            >
              {active ? <span aria-hidden className="absolute top-2 bottom-2 left-0 w-[3px] rounded-r-sm bg-margin" /> : null}
              <Icon className={cx("size-[1.15rem]", active ? "text-uniform" : "text-ink-3")} aria-hidden />
              {item.label}
            </Link>
          </li>
        );
      })}
    </ul>
  );

  return (
    <div className="min-h-dvh">
      <a href="#main" className="sr-only z-50 rounded-md bg-paper px-3 py-2 focus:not-sr-only focus:absolute focus:top-2 focus:left-2">
        Skip to content
      </a>
      <header className="no-print sticky top-0 z-30 border-b border-line bg-paper">
        <div className="flex h-14 items-center gap-2 px-3 sm:px-5">
          <button
            type="button"
            className="-ml-1 rounded-md p-2 text-ink-2 hover:bg-ink/5 lg:hidden"
            aria-expanded={navOpen}
            aria-controls="staff-nav"
            onClick={() => setNavOpen((o) => !o)}
          >
            {navOpen ? <X className="size-5" aria-hidden /> : <Menu className="size-5" aria-hidden />}
            <span className="sr-only">{navOpen ? "Close menu" : "Open menu"}</span>
          </button>
          <Link href="/app" className="flex min-w-0 items-center gap-2.5">
            <Mark className="size-7" />
            <span className="min-w-0">
              <span className="block truncate font-display text-[1.05rem] leading-tight font-bold">{session.membership.tenantName}</span>
              <span className="hidden text-[0.75rem] leading-tight text-ink-3 sm:block">Shule Hub</span>
            </span>
          </Link>
          <div className="ml-auto flex items-center gap-3">
            <BranchSelect id="branch-top" className="hidden md:flex" />
            {session.isParent ? (
              <Link href="/app/parent" className="hidden rounded-md px-2.5 py-1.5 text-sm font-semibold text-ink-2 hover:bg-ink/5 hover:text-ink md:block">
                Parent view
              </Link>
            ) : null}
            <UserMenu view="staff" />
          </div>
        </div>
      </header>

      <div className="mx-auto flex max-w-[1400px]">
        <nav
          id="staff-nav"
          aria-label="Main"
          className={cx(
            "no-print fixed inset-x-0 top-14 z-20 border-b border-line bg-canvas px-3 pt-3 pb-4 shadow-[0_10px_24px_rgba(23,35,59,0.12)]",
            "lg:sticky lg:top-14 lg:block lg:h-[calc(100dvh-3.5rem)] lg:w-60 lg:shrink-0 lg:border-r lg:border-b-0 lg:px-3 lg:pt-5 lg:shadow-none",
            navOpen ? "block" : "hidden",
          )}
        >
          <BranchSelect id="branch-nav" className="mb-3 px-1 md:hidden" />
          {nav}
          {session.isParent ? (
            <Link href="/app/parent" onClick={() => setNavOpen(false)} className="mt-3 flex items-center gap-3 rounded-md px-3 py-2.5 font-semibold text-ink-2 md:hidden">
              <Users className="size-[1.15rem] text-ink-3" aria-hidden /> Parent view
            </Link>
          ) : null}
        </nav>
        {navOpen ? <button type="button" aria-hidden tabIndex={-1} className="fixed inset-0 top-14 z-10 bg-ink/30 lg:hidden" onClick={() => setNavOpen(false)} /> : null}
        <main id="main" className="min-w-0 flex-1 px-4 pt-5 pb-16 sm:px-6 lg:px-8 lg:pt-7">
          {children}
        </main>
      </div>
    </div>
  );
}
