import { describe, expect, it } from "vitest";
import { ApiError, errorMessage, fieldError, parseProblem } from "@/lib/api/problem";

describe("parseProblem", () => {
  it("uses detail as the message and maps field errors", () => {
    const err = parseProblem(400, {
      type: "about:blank",
      title: "Bad Request",
      status: 400,
      detail: "Amount must be a whole number of shillings",
      instance: "/api/fees/stk",
      errors: [{ field: "amount", message: "must be greater than 0" }],
    });
    expect(err).toBeInstanceOf(ApiError);
    expect(err.status).toBe(400);
    expect(err.message).toBe("Amount must be a whole number of shillings");
    expect(err.fieldErrors).toEqual({ amount: "must be greater than 0" });
  });

  it("joins several messages for the same field", () => {
    const err = parseProblem(400, {
      status: 400,
      errors: [
        { field: "guardian.phone", message: "must not be blank" },
        { field: "guardian.phone", message: "must be a Kenyan mobile number" },
      ],
    });
    expect(err.fieldErrors["guardian.phone"]).toBe("must not be blank; must be a Kenyan mobile number");
    expect(err.message).toMatch(/highlighted fields/);
  });

  it("falls back to a readable message when there's no detail", () => {
    expect(parseProblem(409, { title: "Conflict", status: 409 }).message).toMatch(/conflicts/);
    expect(parseProblem(403, null).message).toMatch(/permission/);
    expect(parseProblem(502, "").message).toMatch(/M-Pesa/);
    expect(parseProblem(500, "<html>oops</html>").message).toMatch(/went wrong/);
  });

  it("keeps a meaningful title when detail is missing", () => {
    expect(parseProblem(409, { title: "Application is not ACCEPTED", status: 409 }).message).toBe("Application is not ACCEPTED");
  });

  it("parses problem JSON delivered as a string", () => {
    const err = parseProblem(404, '{"status":404,"detail":"Learner not found"}');
    expect(err.message).toBe("Learner not found");
    expect(err.isNotFound).toBe(true);
  });

  it("prefers the status inside the problem body", () => {
    expect(parseProblem(400, { status: 409, detail: "x" }).status).toBe(409);
  });

  it("ignores malformed errors entries", () => {
    const err = parseProblem(400, { detail: "Invalid", errors: [null, { field: "name" }, { message: "orphan" }, "x"] });
    expect(err.fieldErrors).toEqual({ _: "orphan" });
  });
});

describe("helpers", () => {
  it("errorMessage works on anything thrown", () => {
    expect(errorMessage(parseProblem(400, { detail: "Bad phone" }))).toBe("Bad phone");
    expect(errorMessage(new Error("boom"))).toBe("boom");
    expect(errorMessage("weird")).toMatch(/went wrong/);
  });

  it("fieldError looks up the first matching path", () => {
    const err = parseProblem(400, { errors: [{ field: "guardian.phone", message: "invalid" }] });
    expect(fieldError(err, "phone", "guardian.phone")).toBe("invalid");
    expect(fieldError(err, "email")).toBeUndefined();
    expect(fieldError(new Error("x"), "phone")).toBeUndefined();
  });
});
