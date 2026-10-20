"use client";

import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import type { ReactNode } from "react";
import { useQuery } from "@/lib/hooks";
import { gradeLabel } from "@/lib/grades";
import { useSession } from "@/features/session/SessionProvider";

/** Back link + child's name for the parent sub-pages. Children are fetched once and cached per key. */
export function ChildHeader({ learnerId, title, backHref = "/app/parent", backLabel = "My children", actions }: { learnerId: string; title: string; backHref?: string; backLabel?: string; actions?: ReactNode }) {
  const { api, membership } = useSession();
  const children = useQuery(`children:${membership.tenantId}`, (signal) => api.parent.children({ signal }));
  const child = children.data?.find((c) => c.learnerId === learnerId);
  return (
    <div className="no-print mb-5">
      <Link href={backHref} className="inline-flex items-center gap-1 text-sm font-semibold text-ink-2 hover:text-ink">
        <ArrowLeft className="size-4" aria-hidden /> {backLabel}
      </Link>
      <div className="mt-1.5 flex flex-wrap items-end justify-between gap-2">
        <div>
          <h1 className="font-display text-[1.5rem]">{title}</h1>
          {child ? (
            <p className="text-ink-2">
              {child.fullName}, {child.streamName ?? gradeLabel(child.gradeLevel)}
            </p>
          ) : null}
        </div>
        {actions}
      </div>
    </div>
  );
}
