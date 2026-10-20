import type { Membership, Permission } from "@/lib/api/types";

export type NavIcon = "dashboard" | "admissions" | "learners" | "fees" | "assessment" | "settings";

export interface NavItem {
  key: NavIcon;
  label: string;
  href: string;
  description: string;
  /** Shown when the user holds any one of these. Empty = everyone with staff access. */
  anyOf: Permission[];
}

export const NAV_ITEMS: NavItem[] = [
  { key: "dashboard", label: "Dashboard", href: "/app", description: "Current term and shortcuts", anyOf: [] },
  {
    key: "admissions",
    label: "Admissions",
    href: "/app/admissions",
    description: "Enquiries, offers and admission numbers",
    anyOf: ["ADMISSIONS_VIEW"],
  },
  { key: "learners", label: "Learners", href: "/app/learners", description: "Profiles, guardians and statements", anyOf: ["LEARNERS_VIEW"] },
  { key: "fees", label: "Fees", href: "/app/fees", description: "Fee structures, invoices, payments and arrears", anyOf: ["FEES_VIEW"] },
  {
    key: "assessment",
    label: "Assessment",
    href: "/app/assessment",
    description: "Scores, rubric levels and report cards",
    anyOf: ["ASSESSMENT_ENTER", "ASSESSMENT_MANAGE"],
  },
  {
    key: "settings",
    label: "Settings",
    href: "/app/settings",
    description: "Terms, streams and M-Pesa channels",
    anyOf: ["ACADEMICS_MANAGE", "PAYMENTS_CONFIGURE"],
  },
];

export function hasAny(permissions: readonly Permission[] | null | undefined, wanted: readonly Permission[]): boolean {
  if (wanted.length === 0) return true;
  if (!permissions) return false;
  return wanted.some((p) => permissions.includes(p));
}

export function can(permissions: readonly Permission[] | null | undefined, permission: Permission): boolean {
  return !!permissions && permissions.includes(permission);
}

/** Nav items the user may see. Dashboard is shown to anyone with at least one staff permission. */
export function navItemsFor(permissions: readonly Permission[] | null | undefined): NavItem[] {
  if (!hasStaffAccess(permissions)) return [];
  return NAV_ITEMS.filter((item) => hasAny(permissions, item.anyOf));
}

export function hasStaffAccess(permissions: readonly Permission[] | null | undefined): boolean {
  return !!permissions && permissions.length > 0;
}

/** Where a membership should land after sign-in. */
export function homeFor(membership: Pick<Membership, "permissions" | "parent">): "/app" | "/app/parent" | null {
  if (hasStaffAccess(membership.permissions)) return "/app";
  if (membership.parent) return "/app/parent";
  return null;
}

/** Is this pathname allowed for the membership? Used to bounce people off pages they cannot use. */
export function isPathAllowed(pathname: string, membership: Pick<Membership, "permissions" | "parent">): boolean {
  if (pathname === "/app/parent" || pathname.startsWith("/app/parent/")) return membership.parent;
  if (!hasStaffAccess(membership.permissions)) return false;
  const item = [...NAV_ITEMS]
    .filter((n) => n.href !== "/app")
    .find((n) => pathname === n.href || pathname.startsWith(`${n.href}/`));
  return item ? hasAny(membership.permissions, item.anyOf) : true;
}
