// Browser storage can throw (private mode, blocked site data, quota) or be missing (server render).
// Every access goes through these helpers so a failure only loses the remembered preference.

type Area = "local" | "session";

function area(kind: Area): Storage | null {
  try {
    if (typeof window === "undefined") return null;
    return kind === "local" ? window.localStorage : window.sessionStorage;
  } catch {
    return null;
  }
}

export function readStorage(key: string, kind: Area = "local"): string | null {
  try {
    return area(kind)?.getItem(key) ?? null;
  } catch {
    return null;
  }
}

export function writeStorage(key: string, value: string, kind: Area = "local"): void {
  try {
    area(kind)?.setItem(key, value);
  } catch {
    // ignore: preference simply isn't remembered
  }
}

export function removeStorage(key: string, kind: Area = "local"): void {
  try {
    area(kind)?.removeItem(key);
  } catch {
    // ignore
  }
}

export const STORAGE_KEYS = {
  tenant: "shulehub.tenantId",
  branch: (tenantId: string) => `shulehub.branchId.${tenantId}`,
  returnTo: "shulehub.returnTo",
} as const;
