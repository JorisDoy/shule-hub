import { describe, expect, it } from "vitest";
import type { Permission } from "@/lib/api/types";
import { homeFor, isPathAllowed, navItemsFor } from "@/lib/permissions";

const labels = (perms: Permission[]) => navItemsFor(perms).map((n) => n.label);

describe("navItemsFor", () => {
  it("shows nothing without staff permissions", () => {
    expect(labels([])).toEqual([]);
    expect(navItemsFor(null)).toEqual([]);
  });

  it("gives a bursar dashboard, learners, fees and settings (via PAYMENTS_CONFIGURE)", () => {
    expect(labels(["FEES_VIEW", "FEES_MANAGE", "LEARNERS_VIEW", "PAYMENTS_CONFIGURE"])).toEqual(["Dashboard", "Learners", "Fees", "Settings"]);
  });

  it("gives an admissions officer dashboard and admissions", () => {
    expect(labels(["ADMISSIONS_VIEW", "ADMISSIONS_MANAGE"])).toEqual(["Dashboard", "Admissions"]);
  });

  it("shows assessment for ASSESSMENT_ENTER or ASSESSMENT_MANAGE", () => {
    expect(labels(["ASSESSMENT_ENTER"])).toEqual(["Dashboard", "Assessment"]);
    expect(labels(["ASSESSMENT_MANAGE"])).toEqual(["Dashboard", "Assessment"]);
  });

  it("shows settings for ACADEMICS_MANAGE or PAYMENTS_CONFIGURE", () => {
    expect(labels(["ACADEMICS_MANAGE"])).toEqual(["Dashboard", "Settings"]);
    expect(labels(["PAYMENTS_CONFIGURE"])).toEqual(["Dashboard", "Settings"]);
  });

  it("does not grant modules from manage-only permissions without the view permission", () => {
    expect(labels(["FEES_MANAGE"])).toEqual(["Dashboard"]);
    expect(labels(["LEARNERS_EDIT"])).toEqual(["Dashboard"]);
  });

  it("gives a director everything, in nav order", () => {
    const all: Permission[] = [
      "TENANT_ADMIN",
      "BRANCH_MANAGE",
      "ACADEMICS_MANAGE",
      "ADMISSIONS_VIEW",
      "ADMISSIONS_MANAGE",
      "LEARNERS_VIEW",
      "LEARNERS_EDIT",
      "FEES_VIEW",
      "FEES_MANAGE",
      "PAYMENTS_CONFIGURE",
      "ASSESSMENT_ENTER",
      "ASSESSMENT_MANAGE",
      "REPORTS_RELEASE",
    ];
    expect(labels(all)).toEqual(["Dashboard", "Admissions", "Learners", "Fees", "Assessment", "Settings"]);
  });
});

describe("homeFor", () => {
  it("sends staff to /app and parent-only users to /app/parent", () => {
    expect(homeFor({ permissions: ["FEES_VIEW"], parent: false })).toBe("/app");
    expect(homeFor({ permissions: ["FEES_VIEW"], parent: true })).toBe("/app");
    expect(homeFor({ permissions: [], parent: true })).toBe("/app/parent");
    expect(homeFor({ permissions: [], parent: false })).toBeNull();
  });
});

describe("isPathAllowed", () => {
  const bursar = { permissions: ["FEES_VIEW", "LEARNERS_VIEW"] as Permission[], parent: false };
  const parent = { permissions: [] as Permission[], parent: true };
  const staffParent = { permissions: ["ASSESSMENT_ENTER"] as Permission[], parent: true };

  it("checks module permissions for staff pages", () => {
    expect(isPathAllowed("/app", bursar)).toBe(true);
    expect(isPathAllowed("/app/fees", bursar)).toBe(true);
    expect(isPathAllowed("/app/learners/abc", bursar)).toBe(true);
    expect(isPathAllowed("/app/admissions", bursar)).toBe(false);
    expect(isPathAllowed("/app/assessment/report-cards", bursar)).toBe(false);
  });

  it("keeps parent pages for parents", () => {
    expect(isPathAllowed("/app/parent", parent)).toBe(true);
    expect(isPathAllowed("/app/parent/x/statement", parent)).toBe(true);
    expect(isPathAllowed("/app", parent)).toBe(false);
    expect(isPathAllowed("/app/parent", bursar)).toBe(false);
  });

  it("lets staff who are also parents use both", () => {
    expect(isPathAllowed("/app/assessment", staffParent)).toBe(true);
    expect(isPathAllowed("/app/parent", staffParent)).toBe(true);
  });
});
