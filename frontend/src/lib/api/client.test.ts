import { describe, expect, it, vi } from "vitest";
import { buildUrl, createHttpClient, needsTenantHeader } from "@/lib/api/client";
import { ApiError } from "@/lib/api/problem";

function fakeFetch(status: number, body: unknown) {
  return vi.fn<typeof fetch>(async () => new Response(body === undefined ? "" : JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } }));
}

describe("needsTenantHeader", () => {
  it("skips identity, platform, public and callback calls", () => {
    expect(needsTenantHeader("/api/me")).toBe(false);
    expect(needsTenantHeader("/api/me/memberships")).toBe(false);
    expect(needsTenantHeader("/api/platform/tenants")).toBe(false);
    expect(needsTenantHeader("/api/public/demo/enquiries")).toBe(false);
    expect(needsTenantHeader("/api/callbacks/mpesa/abc/stk")).toBe(false);
  });

  it("sends it for tenant data, including the parent portal", () => {
    expect(needsTenantHeader("/api/me/children")).toBe(true);
    expect(needsTenantHeader("/api/learners?q=a")).toBe(true);
    expect(needsTenantHeader("/api/fees/arrears")).toBe(true);
  });
});

describe("buildUrl", () => {
  it("drops empty values", () => {
    expect(buildUrl("/api/learners", { q: "Amani", gradeLevel: "", page: 0, streamId: undefined })).toBe("/api/learners?q=Amani&page=0");
  });
});

describe("createHttpClient", () => {
  it("sends bearer token and tenant header", async () => {
    const f = fakeFetch(200, [{ id: "1" }]);
    const http = createHttpClient({ getAccessToken: () => "tok", getTenantId: () => "tenant-1", fetchImpl: f });
    await http.get("/api/branches");
    const [url, init] = f.mock.calls[0];
    const headers = init?.headers as Record<string, string>;
    expect(url).toBe("/api/branches");
    expect(headers.Authorization).toBe("Bearer tok");
    expect(headers["X-Tenant-Id"]).toBe("tenant-1");
  });

  it("does not send the tenant header to /api/me/memberships", async () => {
    const f = fakeFetch(200, []);
    const http = createHttpClient({ getAccessToken: () => "tok", getTenantId: () => "tenant-1", fetchImpl: f });
    await http.get("/api/me/memberships");
    const headers = f.mock.calls[0][1]?.headers as Record<string, string>;
    expect(headers["X-Tenant-Id"]).toBeUndefined();
    expect(headers.Authorization).toBe("Bearer tok");
  });

  it("throws a parsed ApiError and reports 401s", async () => {
    const onUnauthorized = vi.fn();
    const http = createHttpClient({ getAccessToken: () => "tok", getTenantId: () => "t", fetchImpl: fakeFetch(401, { status: 401, detail: "Token expired" }), onUnauthorized });
    await expect(http.get("/api/learners")).rejects.toMatchObject({ status: 401, message: "Token expired" });
    expect(onUnauthorized).toHaveBeenCalledOnce();
  });

  it("turns network failures into a status-0 ApiError", async () => {
    const http = createHttpClient({
      getAccessToken: () => "tok",
      getTenantId: () => "t",
      fetchImpl: vi.fn(async () => {
        throw new TypeError("Failed to fetch");
      }),
    });
    const err = await http.get("/api/learners").catch((e: unknown) => e);
    expect(err).toBeInstanceOf(ApiError);
    expect((err as ApiError).status).toBe(0);
  });

  it("serialises JSON bodies", async () => {
    const f = fakeFetch(201, { intentId: "i" });
    const http = createHttpClient({ getAccessToken: () => "tok", getTenantId: () => "t", fetchImpl: f });
    await http.post("/api/fees/stk", { learnerId: "l", amount: 100, phone: "254712345678" });
    const init = f.mock.calls[0][1]!;
    expect(init.method).toBe("POST");
    expect(JSON.parse(init.body as string)).toEqual({ learnerId: "l", amount: 100, phone: "254712345678" });
  });
});
