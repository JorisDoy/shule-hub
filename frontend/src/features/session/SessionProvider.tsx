"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import { usePathname, useRouter } from "next/navigation";
import { useAuth } from "react-oidc-context";
import { Building2, LogOut, RefreshCw } from "lucide-react";
import { createHttpClient } from "@/lib/api/client";
import { createApi, type Api } from "@/lib/api/endpoints";
import { ApiError } from "@/lib/api/problem";
import type { AcademicYear, Branch, CurrentTerm, Membership, Permission } from "@/lib/api/types";
import { useQuery } from "@/lib/hooks";
import { homeFor, hasStaffAccess, isPathAllowed } from "@/lib/permissions";
import { readStorage, removeStorage, STORAGE_KEYS, writeStorage } from "@/lib/storage";
import { ROLE_LABELS } from "@/lib/labels";
import { Button } from "@/components/ui/Button";
import { ErrorAlert } from "@/components/ui/Feedback";
import { FullScreen } from "@/features/session/FullScreen";
import { rememberReturnTo, takeReturnTo } from "@/features/auth/AuthRoot";

export interface Session {
  api: Api;
  userName: string;
  userEmail: string | null;
  memberships: Membership[];
  membership: Membership;
  permissions: Permission[];
  isStaff: boolean;
  isParent: boolean;
  can: (p: Permission) => boolean;
  canAny: (...p: Permission[]) => boolean;
  switchTenant: () => void;
  signOut: () => void;
  branches: Branch[];
  branchesLoading: boolean;
  branch: Branch | null;
  branchId: string | undefined;
  setBranchId: (id: string) => void;
  /** Academic years, newest first (staff only). */
  years: AcademicYear[];
  yearsLoading: boolean;
  yearsError: unknown;
  currentYear: AcademicYear | null;
  /** null when the school has no terms at all (404). */
  currentTerm: CurrentTerm | null;
  refreshAcademics: () => void;
}

const SessionContext = createContext<Session | null>(null);

/** Set once a 401 has started a fresh sign-in, so parallel failing calls don't each redirect. */
let reauthenticating = false;

export function useSession(): Session {
  const ctx = useContext(SessionContext);
  if (!ctx) throw new Error("useSession must be used inside <SessionProvider>");
  return ctx;
}

/** Loads memberships, settles on a tenant, then renders the app for it. */
export function SessionProvider({ children }: { children: ReactNode }) {
  const auth = useAuth();
  const token = auth.user?.access_token ?? null;
  const router = useRouter();

  const onUnauthorized = useCallback(() => {
    if (reauthenticating) return;
    reauthenticating = true;
    rememberReturnTo();
    void auth.removeUser().finally(() => auth.signinRedirect());
  }, [auth]);

  // Identity calls (/api/me/**) carry no tenant header; the tenant-scoped client is built in TenantSession.
  const api = useMemo(
    () => createApi(createHttpClient({ getAccessToken: () => token, getTenantId: () => null, onUnauthorized })),
    [token, onUnauthorized],
  );

  const membershipsQuery = useQuery("memberships", (signal) => api.me.memberships({ signal }));
  const [chosenTenant, setChosenTenant] = useState<string | null>(() => readStorage(STORAGE_KEYS.tenant));

  const memberships = membershipsQuery.data;
  const membership = useMemo(() => {
    if (!memberships || memberships.length === 0) return null;
    if (memberships.length === 1) return memberships[0];
    return memberships.find((m) => m.tenantId === chosenTenant) ?? null;
  }, [memberships, chosenTenant]);

  const chooseTenant = useCallback((tenantId: string) => {
    writeStorage(STORAGE_KEYS.tenant, tenantId);
    setChosenTenant(tenantId);
  }, []);

  const switchTenant = useCallback(() => {
    removeStorage(STORAGE_KEYS.tenant);
    setChosenTenant(null);
  }, []);

  const signOut = useCallback(() => {
    removeStorage(STORAGE_KEYS.tenant);
    void auth.signoutRedirect().catch(() => auth.removeUser().then(() => router.replace("/")));
  }, [auth, router]);

  const profile = auth.user?.profile;
  const userName =
    (profile?.name as string | undefined) ||
    [profile?.given_name, profile?.family_name].filter(Boolean).join(" ") ||
    (profile?.preferred_username as string | undefined) ||
    "Signed in";
  const userEmail = (profile?.email as string | undefined) ?? null;

  if (membershipsQuery.loading && !memberships) {
    return (
      <FullScreen title="Opening your school" busy>
        <p>Loading your schools…</p>
      </FullScreen>
    );
  }

  if (membershipsQuery.error && !memberships) {
    return (
      <FullScreen
        title="We couldn't load your schools"
        action={
          <>
            <Button icon={<RefreshCw className="size-4" aria-hidden />} onClick={membershipsQuery.refetch}>
              Try again
            </Button>
            <Button variant="ghost" onClick={signOut}>
              Sign out
            </Button>
          </>
        }
      >
        <ErrorAlert error={membershipsQuery.error} />
      </FullScreen>
    );
  }

  if (!memberships || memberships.length === 0) {
    return (
      <FullScreen
        title="Your login isn't linked to a school yet"
        action={
          <Button variant="secondary" icon={<LogOut className="size-4" aria-hidden />} onClick={signOut}>
            Sign out
          </Button>
        }
      >
        <p>
          You&apos;re signed in as <strong className="text-ink">{userEmail ?? userName}</strong>, but no school has added this account yet.
        </p>
        <p className="mt-3">
          Ask your school office to add you as staff or as a parent using this email address, then sign in again. If you used a different
          email with the school, sign out and use that one.
        </p>
      </FullScreen>
    );
  }

  if (!membership) {
    return <TenantPicker memberships={memberships} onChoose={chooseTenant} onSignOut={signOut} userName={userName} />;
  }

  return (
    <TenantSession
      key={membership.tenantId}
      token={token}
      onUnauthorized={onUnauthorized}
      membership={membership}
      memberships={memberships}
      userName={userName}
      userEmail={userEmail}
      switchTenant={switchTenant}
      signOut={signOut}
    >
      {children}
    </TenantSession>
  );
}

function TenantPicker({
  memberships,
  onChoose,
  onSignOut,
  userName,
}: {
  memberships: Membership[];
  onChoose: (id: string) => void;
  onSignOut: () => void;
  userName: string;
}) {
  return (
    <FullScreen
      title="Choose a school"
      action={
        <Button variant="ghost" onClick={onSignOut}>
          Sign out
        </Button>
      }
    >
      <p>
        {userName}, your account is linked to {memberships.length} schools. Pick the one you want to work in. You can switch later from the
        menu.
      </p>
      <ul className="mt-5 flex flex-col gap-2">
        {memberships.map((m) => (
          <li key={m.tenantId}>
            <button
              type="button"
              onClick={() => onChoose(m.tenantId)}
              className="flex w-full items-start gap-3 rounded-md border border-line-strong bg-paper px-3.5 py-3 text-left hover:border-uniform hover:bg-uniform-tint/50"
            >
              <Building2 className="mt-0.5 size-5 shrink-0 text-uniform" aria-hidden />
              <span className="min-w-0">
                <span className="block font-semibold text-ink">{m.tenantName}</span>
                <span className="block text-sm text-ink-2">{describeMembership(m)}</span>
              </span>
            </button>
          </li>
        ))}
      </ul>
    </FullScreen>
  );
}

export function describeMembership(m: Membership): string {
  const roles = [...new Set(m.grants.map((g) => (g.branchName ? `${ROLE_LABELS[g.role] ?? g.role}, ${g.branchName}` : ROLE_LABELS[g.role] ?? g.role)))];
  if (m.parent && !m.grants.some((g) => g.role === "PARENT")) roles.push("Parent");
  return roles.length > 0 ? roles.join(" · ") : "No role assigned yet";
}

function TenantSession({
  token,
  onUnauthorized,
  membership,
  memberships,
  userName,
  userEmail,
  switchTenant,
  signOut,
  children,
}: {
  token: string | null;
  onUnauthorized: () => void;
  membership: Membership;
  memberships: Membership[];
  userName: string;
  userEmail: string | null;
  switchTenant: () => void;
  signOut: () => void;
  children: ReactNode;
}) {
  const router = useRouter();
  const pathname = usePathname() ?? "/app";
  const isStaff = hasStaffAccess(membership.permissions);
  const tenantId = membership.tenantId;
  const api = useMemo(
    () => createApi(createHttpClient({ getAccessToken: () => token, getTenantId: () => tenantId, onUnauthorized })),
    [token, tenantId, onUnauthorized],
  );

  const branchesQuery = useQuery(isStaff ? `branches:${tenantId}` : null, (signal) => api.branches.list({ signal }));
  const yearsQuery = useQuery(isStaff ? `years:${tenantId}` : null, (signal) => api.academics.years({ signal }));
  const termQuery = useQuery(isStaff ? `term:${tenantId}` : null, async (signal) => {
    try {
      return await api.academics.currentTerm({ signal });
    } catch (err) {
      if (err instanceof ApiError && err.isNotFound) return null;
      throw err;
    }
  });

  const [storedBranch, setStoredBranch] = useState<string | null>(() => readStorage(STORAGE_KEYS.branch(tenantId)));
  const branches = useMemo(() => branchesQuery.data ?? [], [branchesQuery.data]);
  const branch = branches.find((b) => b.id === storedBranch) ?? branches[0] ?? null;

  const setBranchId = useCallback(
    (id: string) => {
      writeStorage(STORAGE_KEYS.branch(tenantId), id);
      setStoredBranch(id);
    },
    [tenantId],
  );

  const years = useMemo(() => yearsQuery.data ?? [], [yearsQuery.data]);
  const currentTerm = termQuery.data ?? null;
  const currentYear = years.find((y) => y.current) ?? years.find((y) => y.id === currentTerm?.academicYearId) ?? years[0] ?? null;
  const refetchYears = yearsQuery.refetch;
  const refetchTerm = termQuery.refetch;
  const refreshAcademics = useCallback(() => {
    refetchYears();
    refetchTerm();
  }, [refetchYears, refetchTerm]);

  // Send people to a page they can use; after sign-in, return to the deep link they opened.
  const allowed = isPathAllowed(pathname, membership);
  const home = homeFor(membership);
  const returned = useRef(false);
  useEffect(() => {
    if (!returned.current) {
      returned.current = true;
      const target = takeReturnTo();
      if (target && target !== pathname && isPathAllowed(target.split("?")[0], membership)) {
        router.replace(target);
        return;
      }
    }
    if (!allowed && home && pathname !== home) router.replace(home);
  }, [allowed, home, pathname, router, membership]);

  const value = useMemo<Session>(
    () => ({
      api,
      userName,
      userEmail,
      memberships,
      membership,
      permissions: membership.permissions,
      isStaff,
      isParent: membership.parent,
      can: (p) => membership.permissions.includes(p),
      canAny: (...ps) => ps.some((p) => membership.permissions.includes(p)),
      switchTenant,
      signOut,
      branches,
      branchesLoading: branchesQuery.loading,
      branch,
      branchId: branch?.id,
      setBranchId,
      years,
      yearsLoading: yearsQuery.loading,
      yearsError: yearsQuery.error,
      currentYear,
      currentTerm,
      refreshAcademics,
    }),
    [
      api,
      userName,
      userEmail,
      memberships,
      membership,
      isStaff,
      switchTenant,
      signOut,
      branches,
      branchesQuery.loading,
      branch,
      setBranchId,
      years,
      yearsQuery.loading,
      yearsQuery.error,
      currentYear,
      currentTerm,
      refreshAcademics,
    ],
  );

  if (!home) {
    return (
      <FullScreen
        title="No access in this school yet"
        action={
          <>
            {memberships.length > 1 ? <Button onClick={switchTenant}>Choose another school</Button> : null}
            <Button variant="ghost" onClick={signOut}>
              Sign out
            </Button>
          </>
        }
      >
        <p>
          Your account is linked to <strong className="text-ink">{membership.tenantName}</strong> but has no staff role and no children on
          record. Ask the school office to finish setting up your access.
        </p>
      </FullScreen>
    );
  }

  if (!allowed) {
    return (
      <FullScreen title="Opening your school" busy>
        <p>Taking you to your home page…</p>
      </FullScreen>
    );
  }

  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>;
}
