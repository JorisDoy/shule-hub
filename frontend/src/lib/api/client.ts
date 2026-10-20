import { ApiError, parseProblem } from "@/lib/api/problem";

export type QueryValue = string | number | boolean | null | undefined;

export interface RequestOptions {
  query?: Record<string, QueryValue>;
  body?: unknown;
  signal?: AbortSignal;
}

export interface HttpClient {
  get<T>(path: string, opts?: RequestOptions): Promise<T>;
  post<T>(path: string, body?: unknown, opts?: RequestOptions): Promise<T>;
  put<T>(path: string, body?: unknown, opts?: RequestOptions): Promise<T>;
  patch<T>(path: string, body?: unknown, opts?: RequestOptions): Promise<T>;
}

export interface HttpClientConfig {
  /** Returns the current access token, or null when signed out. */
  getAccessToken: () => string | null | undefined;
  /** Returns the selected tenant (membership) id, or null before one is chosen. */
  getTenantId: () => string | null | undefined;
  /** Called once per 401 so the app can send the person back to sign-in. */
  onUnauthorized?: () => void;
  fetchImpl?: typeof fetch;
  baseUrl?: string;
}

/**
 * Calls that are NOT tenant-scoped (docs/api.md, Conventions): /api/me, /api/me/memberships,
 * /api/platform/**, /api/public/**, /api/callbacks/**. Everything else gets X-Tenant-Id —
 * including /api/me/children/**.
 */
export function needsTenantHeader(path: string): boolean {
  const p = path.split("?")[0].replace(/\/+$/, "");
  if (p === "/api/me" || p === "/api/me/memberships") return false;
  return !["/api/platform/", "/api/public/", "/api/callbacks/"].some((prefix) => p.startsWith(prefix));
}

export function needsAuth(path: string): boolean {
  return !path.startsWith("/api/public/") && !path.startsWith("/api/callbacks/");
}

export function buildUrl(path: string, query?: Record<string, QueryValue>): string {
  if (!query) return path;
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(query)) {
    if (value === undefined || value === null || value === "") continue;
    params.set(key, String(value));
  }
  const qs = params.toString();
  return qs ? `${path}?${qs}` : path;
}

export function createHttpClient(config: HttpClientConfig): HttpClient {
  const doFetch = config.fetchImpl ?? ((...args: Parameters<typeof fetch>) => fetch(...args));

  async function request<T>(method: string, path: string, opts: RequestOptions = {}): Promise<T> {
    const headers: Record<string, string> = { Accept: "application/json, application/problem+json" };
    if (needsAuth(path)) {
      const token = config.getAccessToken();
      if (token) headers.Authorization = `Bearer ${token}`;
    }
    if (needsTenantHeader(path)) {
      const tenantId = config.getTenantId();
      if (tenantId) headers["X-Tenant-Id"] = tenantId;
    }
    let body: string | undefined;
    if (opts.body !== undefined) {
      headers["Content-Type"] = "application/json";
      body = JSON.stringify(opts.body);
    }

    let response: Response;
    try {
      response = await doFetch(`${config.baseUrl ?? ""}${buildUrl(path, opts.query)}`, {
        method,
        headers,
        body,
        signal: opts.signal,
        cache: "no-store",
      });
    } catch (err) {
      if (err instanceof DOMException && err.name === "AbortError") throw err;
      throw new ApiError({ status: 0, message: parseProblem(0, null).message });
    }

    const text = await response.text().catch(() => "");
    let parsed: unknown = undefined;
    if (text) {
      try {
        parsed = JSON.parse(text);
      } catch {
        parsed = text;
      }
    }

    if (!response.ok) {
      if (response.status === 401) config.onUnauthorized?.();
      throw parseProblem(response.status, parsed);
    }
    return parsed as T;
  }

  return {
    get: (path, opts) => request("GET", path, opts),
    post: (path, body, opts) => request("POST", path, { ...opts, body }),
    put: (path, body, opts) => request("PUT", path, { ...opts, body }),
    patch: (path, body, opts) => request("PATCH", path, { ...opts, body }),
  };
}
