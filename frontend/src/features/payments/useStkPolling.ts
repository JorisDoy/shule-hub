"use client";

import { useEffect, useRef, useState } from "react";
import type { PaymentIntentStatus, StkStatus } from "@/lib/api/types";

export const STK_POLL_INTERVAL_MS = 3000;
export const STK_POLL_TIMEOUT_MS = 2 * 60 * 1000;

export function isFinalIntent(status: PaymentIntentStatus | undefined): boolean {
  return status === "SUCCEEDED" || status === "FAILED";
}

export interface StkPollState {
  status: StkStatus | null;
  /** Two minutes passed without SUCCEEDED or FAILED. */
  timedOut: boolean;
  /** The last poll failed (network blip); polling continues. */
  lastError: unknown;
}

/**
 * Polls an STK push intent every 3 s until it succeeds, fails or two minutes pass.
 * Pass intentId = null to stay idle.
 */
export function useStkPolling(
  intentId: string | null,
  poll: (intentId: string, signal: AbortSignal) => Promise<StkStatus>,
  opts: { intervalMs?: number; timeoutMs?: number } = {},
): StkPollState {
  const interval = opts.intervalMs ?? STK_POLL_INTERVAL_MS;
  const timeout = opts.timeoutMs ?? STK_POLL_TIMEOUT_MS;
  const [state, setState] = useState<StkPollState & { intentId: string | null }>({ intentId: null, status: null, timedOut: false, lastError: null });
  const pollRef = useRef(poll);
  useEffect(() => {
    pollRef.current = poll;
  });

  useEffect(() => {
    if (!intentId) return;
    const controller = new AbortController();
    const deadline = Date.now() + timeout;
    let timer: ReturnType<typeof setTimeout> | undefined;

    const tick = async () => {
      try {
        const status = await pollRef.current(intentId, controller.signal);
        if (controller.signal.aborted) return;
        setState({ intentId, status, timedOut: false, lastError: null });
        if (isFinalIntent(status.status)) return;
      } catch (err) {
        if (controller.signal.aborted) return;
        setState((prev) => ({ ...prev, intentId, lastError: err }));
      }
      if (Date.now() + interval > deadline) {
        setState((prev) => ({ ...prev, intentId, timedOut: true }));
        return;
      }
      timer = setTimeout(tick, interval);
    };

    timer = setTimeout(tick, interval);
    return () => {
      controller.abort();
      if (timer) clearTimeout(timer);
    };
  }, [intentId, interval, timeout]);

  if (state.intentId !== intentId) return { status: null, timedOut: false, lastError: null };
  return { status: state.status, timedOut: state.timedOut, lastError: state.lastError };
}
