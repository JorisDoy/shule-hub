"use client";

import Link from "next/link";
import { BookOpenCheck, CalendarDays, ClipboardList, GraduationCap, Settings, Wallet } from "lucide-react";
import { useSession } from "@/features/session/SessionProvider";
import { navItemsFor, type NavIcon } from "@/lib/permissions";
import { formatDate } from "@/lib/format";
import { Alert, ButtonLink, Card, Loading } from "@/components/ui";
import { termRange } from "@/features/academics/terms";

const ICONS: Partial<Record<NavIcon, typeof Wallet>> = {
  admissions: ClipboardList,
  learners: GraduationCap,
  fees: Wallet,
  assessment: BookOpenCheck,
  settings: Settings,
};

function greeting(): string {
  const hour = (new Date().getUTCHours() + 3) % 24;
  if (hour < 12) return "Good morning";
  if (hour < 17) return "Good afternoon";
  return "Good evening";
}

export default function DashboardPage() {
  const session = useSession();
  const { currentTerm, currentYear, yearsLoading, branch } = session;
  const shortcuts = navItemsFor(session.permissions).filter((n) => n.key !== "dashboard");
  const firstName = session.userName.split(" ")[0];

  const today = new Date().toISOString().slice(0, 10);
  const inTerm = currentTerm ? currentTerm.startDate <= today && today <= currentTerm.endDate : false;
  const daysLeft = currentTerm && inTerm ? Math.max(0, Math.round((Date.parse(currentTerm.endDate) - Date.parse(today)) / 86_400_000)) : null;

  return (
    <div className="max-w-5xl">
      <div className="mb-6">
        <h1 className="font-display text-[1.75rem] sm:text-[2rem]">
          {greeting()}, {firstName}
        </h1>
        <p className="mt-1 text-ink-2">
          {session.membership.tenantName}
          {branch ? `, ${branch.name} branch` : ""}
        </p>
      </div>

      <Card className="paper-margin mb-6 py-5 pr-5 pl-14">
        {yearsLoading && !currentTerm ? (
          <Loading label="Loading the school calendar" className="px-0 py-0" />
        ) : currentTerm ? (
          <div className="flex flex-wrap items-end justify-between gap-4">
            <div>
              <p className="flex items-center gap-2 text-sm text-ink-2">
                <CalendarDays className="size-4" aria-hidden />
                {inTerm ? "Current term" : "Most recent term"}
              </p>
              <p className="mt-1 font-display text-2xl font-semibold sm:text-[1.75rem]">
                Term {currentTerm.number}, {currentTerm.year}
              </p>
              <p className="text-ink-2">{termRange(currentTerm)}</p>
            </div>
            {daysLeft !== null ? (
              <p className="text-right">
                <span className="block font-display text-3xl font-semibold tabular">{daysLeft}</span>
                <span className="text-sm text-ink-2">days to closing on {formatDate(currentTerm.endDate)}</span>
              </p>
            ) : (
              <p className="text-sm text-ink-2">The school is between terms.</p>
            )}
          </div>
        ) : (
          <div>
            <p className="font-display text-xl font-semibold">No term set up yet</p>
            <p className="mt-1 text-ink-2">
              {session.can("ACADEMICS_MANAGE")
                ? "Add this year's three terms so fees, invoices and report cards have dates to work with."
                : "Ask the head teacher or director to add this year's terms in Settings."}
            </p>
            {session.can("ACADEMICS_MANAGE") ? (
              <ButtonLink href="/app/settings" className="mt-3">
                Set up the academic year
              </ButtonLink>
            ) : null}
          </div>
        )}
      </Card>

      {!currentYear && !yearsLoading && session.yearsError ? <Alert tone="warning">The school calendar couldn&apos;t be loaded. Some pages may be empty.</Alert> : null}

      <h2 className="mb-3 font-display text-xl">Your work</h2>
      {shortcuts.length === 0 ? (
        <p className="text-ink-2">Your role doesn&apos;t include any modules yet. Ask your school administrator to update your access.</p>
      ) : (
        <ul className="grid gap-3 sm:grid-cols-2">
          {shortcuts.map((item) => {
            const Icon = ICONS[item.key] ?? Wallet;
            return (
              <li key={item.href}>
                <Link href={item.href} className="group flex h-full items-start gap-3.5 rounded-lg border border-line bg-paper px-4 py-4 hover:border-uniform/50">
                  <span className="flex size-10 shrink-0 items-center justify-center rounded-md bg-uniform-tint text-uniform">
                    <Icon className="size-5" aria-hidden />
                  </span>
                  <span>
                    <span className="block font-semibold text-ink group-hover:text-uniform-dark">{item.label}</span>
                    <span className="block text-sm text-ink-2">{item.description}</span>
                  </span>
                </Link>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
