import type { ProblemDetail } from "@/lib/api/types";

/** An error from the Shule Hub API, built from an RFC 9457 problem detail. */
export class ApiError extends Error {
  readonly status: number;
  readonly title: string | undefined;
  readonly detail: string | undefined;
  /** Field path (e.g. "amount", "guardian.phone") → message. */
  readonly fieldErrors: Record<string, string>;
  readonly problem: ProblemDetail | null;

  constructor(init: {
    status: number;
    message: string;
    title?: string;
    detail?: string;
    fieldErrors?: Record<string, string>;
    problem?: ProblemDetail | null;
  }) {
    super(init.message);
    this.name = "ApiError";
    this.status = init.status;
    this.title = init.title;
    this.detail = init.detail;
    this.fieldErrors = init.fieldErrors ?? {};
    this.problem = init.problem ?? null;
  }

  get isNotFound() {
    return this.status === 404;
  }
  get isConflict() {
    return this.status === 409;
  }
  get isNetwork() {
    return this.status === 0;
  }
}

const FALLBACK: Record<number, string> = {
  0: "Can't reach Shule Hub. Check your internet connection and try again.",
  400: "Some of the details aren't valid. Check the form and try again.",
  401: "Your session has ended. Sign in again to continue.",
  403: "Your account doesn't have permission to do that.",
  404: "We couldn't find that record. It may have been removed.",
  409: "That change conflicts with the record's current state. Refresh and try again.",
  502: "M-Pesa didn't respond in time. Wait a minute and try again.",
  503: "Shule Hub is temporarily unavailable. Try again shortly.",
  504: "Shule Hub took too long to respond. Try again shortly.",
};

export function fallbackMessage(status: number): string {
  if (FALLBACK[status]) return FALLBACK[status];
  if (status >= 500) return "Something went wrong on our side. Try again in a moment.";
  return "The request didn't go through. Try again.";
}

function asString(value: unknown): string | undefined {
  return typeof value === "string" && value.trim() !== "" ? value.trim() : undefined;
}

/**
 * Turns an HTTP status and response body (parsed JSON, raw text or nothing) into an ApiError.
 * Prefers `detail`, then a meaningful `title`, then a status-based message. Collects `errors[]`
 * into a field → message map; several messages for one field are joined.
 */
export function parseProblem(status: number, body: unknown): ApiError {
  let problem: ProblemDetail | null = null;
  if (body && typeof body === "object" && !Array.isArray(body)) {
    problem = body as ProblemDetail;
  } else if (typeof body === "string" && body.trim().startsWith("{")) {
    try {
      problem = JSON.parse(body) as ProblemDetail;
    } catch {
      problem = null;
    }
  }

  const fieldErrors: Record<string, string> = {};
  if (problem && Array.isArray(problem.errors)) {
    for (const err of problem.errors) {
      if (!err || typeof err !== "object") continue;
      const field = asString((err as { field?: unknown }).field) ?? "_";
      const message = asString((err as { message?: unknown }).message);
      if (!message) continue;
      fieldErrors[field] = fieldErrors[field] ? `${fieldErrors[field]}; ${message}` : message;
    }
  }

  const detail = asString(problem?.detail);
  const title = asString(problem?.title);
  const effectiveStatus = typeof problem?.status === "number" ? problem.status : status;
  // Generic HTTP reason phrases ("Bad Request") are less helpful than our own fallback copy.
  const genericTitles = new Set(["bad request", "unauthorized", "forbidden", "not found", "conflict", "internal server error", "bad gateway"]);
  const usefulTitle = title && !genericTitles.has(title.toLowerCase()) ? title : undefined;

  let message = detail ?? usefulTitle ?? fallbackMessage(effectiveStatus);
  if (!detail && Object.keys(fieldErrors).length > 0) {
    message = "Some of the details aren't valid. Check the highlighted fields.";
  }

  return new ApiError({ status: effectiveStatus, message, title, detail, fieldErrors, problem });
}

/** Readable message for anything thrown. */
export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) return error.message;
  if (error instanceof Error && error.message) return error.message;
  return "Something went wrong. Try again.";
}

/** Field error lookup that tolerates both "phone" and "guardian.phone" style paths. */
export function fieldError(error: unknown, ...paths: string[]): string | undefined {
  if (!(error instanceof ApiError)) return undefined;
  for (const p of paths) {
    if (error.fieldErrors[p]) return error.fieldErrors[p];
  }
  return undefined;
}
