"use client";

import { useCallback } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useQuery } from "@/lib/hooks";
import { useSession } from "@/features/session/SessionProvider";

/**
 * Stream + term chosen on the assessment pages, kept in the URL (?streamId=&termId=) so links,
 * the back button and a page refresh all keep the teacher's place.
 */
export function useStreamTerm() {
  const { api, branchId, currentYear, currentTerm } = useSession();
  const params = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();

  const streams = useQuery(branchId && currentYear ? `streams:${branchId}:${currentYear.id}` : null, (signal) =>
    api.academics.streams({ branchId, academicYearId: currentYear?.id }, { signal }),
  );
  const terms = [...(currentYear?.terms ?? [])].sort((a, b) => a.number - b.number);

  const urlStream = params.get("streamId") ?? "";
  const urlTerm = params.get("termId") ?? "";
  const streamList = streams.data ?? [];
  const streamId = streamList.some((s) => s.id === urlStream) ? urlStream : streamList.length === 1 ? streamList[0].id : urlStream && !streams.data ? urlStream : "";
  const termId = terms.some((t) => t.id === urlTerm)
    ? urlTerm
    : terms.find((t) => t.id === currentTerm?.id)?.id ?? terms[0]?.id ?? urlTerm;
  const stream = streamList.find((s) => s.id === streamId) ?? null;
  const term = terms.find((t) => t.id === termId) ?? null;

  const setParams = useCallback(
    (patch: Record<string, string | null>) => {
      const next = new URLSearchParams(params.toString());
      for (const [k, v] of Object.entries(patch)) {
        if (v) next.set(k, v);
        else next.delete(k);
      }
      const qs = next.toString();
      router.replace(qs ? `${pathname}?${qs}` : pathname, { scroll: false });
    },
    [params, pathname, router],
  );

  return { streams, streamList, terms, streamId, termId, stream, term, year: currentYear, setParams, params };
}
