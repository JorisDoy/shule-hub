"use client";

import { useCallback, useEffect, useRef, useState, useSyncExternalStore } from "react";

export interface QueryResult<T> {
  data: T | undefined;
  error: unknown;
  /** True while the current key has not produced a result yet (first load or refetch). */
  loading: boolean;
  refetch: () => void;
  /** Replace the cached data, e.g. with the response of a mutation. */
  setData: (updater: T | ((prev: T | undefined) => T)) => void;
}

interface QueryState<T> {
  key: string | null;
  version: number;
  data?: T;
  error?: unknown;
}

/**
 * Minimal data fetching: runs `fetcher` whenever `key` changes (pass null to skip), aborts stale
 * requests, keeps the previous data for the same key while refetching. Put every input the fetcher
 * uses into `key`.
 */
export function useQuery<T>(key: string | null, fetcher: (signal: AbortSignal) => Promise<T>): QueryResult<T> {
  const [version, setVersion] = useState(0);
  const [state, setState] = useState<QueryState<T>>({ key: null, version: -1 });
  const fetcherRef = useRef(fetcher);

  useEffect(() => {
    fetcherRef.current = fetcher;
  });

  useEffect(() => {
    if (key === null) return;
    const controller = new AbortController();
    fetcherRef.current(controller.signal).then(
      (data) => {
        if (!controller.signal.aborted) setState({ key, version, data });
      },
      (error: unknown) => {
        if (controller.signal.aborted) return;
        setState((prev) => ({ key, version, error, data: prev.key === key ? prev.data : undefined }));
      },
    );
    return () => controller.abort();
  }, [key, version]);

  const sameKey = state.key === key;
  const settled = sameKey && state.version === version;

  const refetch = useCallback(() => setVersion((v) => v + 1), []);
  const setData = useCallback(
    (updater: T | ((prev: T | undefined) => T)) =>
      setState((prev) => ({
        key: prev.key,
        version: prev.version,
        data: typeof updater === "function" ? (updater as (p: T | undefined) => T)(prev.data) : updater,
      })),
    [],
  );

  return {
    data: sameKey ? state.data : undefined,
    error: settled ? state.error : undefined,
    loading: key !== null && !settled,
    refetch,
    setData,
  };
}

export interface ActionState<A extends unknown[], R> {
  run: (...args: A) => Promise<R | undefined>;
  pending: boolean;
  error: unknown;
  reset: () => void;
}

/** Wraps a mutation: tracks pending/error and never throws (returns undefined on failure). */
export function useAction<A extends unknown[], R>(fn: (...args: A) => Promise<R>): ActionState<A, R> {
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const fnRef = useRef(fn);
  useEffect(() => {
    fnRef.current = fn;
  });

  const run = useCallback(async (...args: A) => {
    setPending(true);
    setError(null);
    try {
      return await fnRef.current(...args);
    } catch (err) {
      setError(err);
      return undefined;
    } finally {
      setPending(false);
    }
  }, []);

  const reset = useCallback(() => setError(null), []);
  return { run, pending, error, reset };
}

/** Value that only changes after `delay` ms without updates. */
export function useDebounced<T>(value: T, delay = 350): T {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(t);
  }, [value, delay]);
  return debounced;
}

const noopSubscribe = () => () => {};

/** False during server render and hydration, true once running in the browser. */
export function useIsClient(): boolean {
  return useSyncExternalStore(
    noopSubscribe,
    () => true,
    () => false,
  );
}
