"use client";

import { usePathname } from "next/navigation";
import type { ReactNode } from "react";
import { AuthRoot } from "@/features/auth/AuthRoot";
import { SessionProvider } from "@/features/session/SessionProvider";
import { ToastProvider } from "@/components/ui/Feedback";
import { StaffShell } from "@/features/shell/StaffShell";
import { ParentShell } from "@/features/shell/ParentShell";

export function AppFrame({ children }: { children: ReactNode }) {
  const pathname = usePathname() ?? "/app";
  const parentView = pathname === "/app/parent" || pathname.startsWith("/app/parent/");
  return (
    <AuthRoot>
      <SessionProvider>
        <ToastProvider>{parentView ? <ParentShell>{children}</ParentShell> : <StaffShell>{children}</StaffShell>}</ToastProvider>
      </SessionProvider>
    </AuthRoot>
  );
}
