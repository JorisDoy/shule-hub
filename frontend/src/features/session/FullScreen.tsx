import type { ReactNode } from "react";
import { Spinner } from "@/components/ui/Button";
import { Wordmark } from "@/components/Brand";

/** Centered message screen used before the app shell exists (sign-in, tenant picking, no access). */
export function FullScreen({ title, children, action, busy }: { title: ReactNode; children?: ReactNode; action?: ReactNode; busy?: boolean }) {
  return (
    <main className="flex min-h-dvh flex-col items-center justify-center px-4 py-10">
      <div className="w-full max-w-md">
        <Wordmark className="mb-8" />
        <div className="paper-margin rounded-lg border border-line py-7 pr-6 pl-14 shadow-[0_1px_0_rgba(23,35,59,0.04)]">
          <h1 className="flex items-center gap-2.5 font-display text-2xl">
            {busy ? <Spinner className="size-5 text-uniform" /> : null}
            {title}
          </h1>
          {children ? <div className="mt-2 text-ink-2">{children}</div> : null}
          {action ? <div className="mt-5 flex flex-wrap gap-2">{action}</div> : null}
        </div>
      </div>
    </main>
  );
}
