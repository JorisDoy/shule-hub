import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { act, renderHook } from "@testing-library/react";
import type { StkStatus } from "@/lib/api/types";
import { useStkPolling } from "@/features/payments/useStkPolling";
import { parseWholeShillings } from "@/features/payments/MpesaPayDialog";

const status = (s: StkStatus["status"], extra: Partial<StkStatus> = {}): StkStatus => ({
  intentId: "i1",
  status: s,
  amount: 1500,
  phone: "254712345678",
  resultDesc: null,
  mpesaReceipt: null,
  updatedAt: "2027-01-05T07:30:00Z",
  ...extra,
});

describe("useStkPolling", () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it("polls every 3 s and stops on success", async () => {
    const poll = vi
      .fn()
      .mockResolvedValueOnce(status("PENDING"))
      .mockResolvedValueOnce(status("SUBMITTED"))
      .mockResolvedValueOnce(status("SUCCEEDED", { mpesaReceipt: "SAB1C2D3E4" }));
    const { result } = renderHook(() => useStkPolling("i1", poll));

    expect(poll).not.toHaveBeenCalled();
    await act(() => vi.advanceTimersByTimeAsync(3000));
    expect(poll).toHaveBeenCalledTimes(1);
    expect(result.current.status?.status).toBe("PENDING");
    await act(() => vi.advanceTimersByTimeAsync(3000));
    expect(result.current.status?.status).toBe("SUBMITTED");
    await act(() => vi.advanceTimersByTimeAsync(3000));
    expect(result.current.status?.status).toBe("SUCCEEDED");
    expect(result.current.status?.mpesaReceipt).toBe("SAB1C2D3E4");
    await act(() => vi.advanceTimersByTimeAsync(30_000));
    expect(poll).toHaveBeenCalledTimes(3);
  });

  it("stops after two minutes without a final status", async () => {
    const poll = vi.fn().mockResolvedValue(status("PENDING"));
    const { result } = renderHook(() => useStkPolling("i1", poll));
    await act(() => vi.advanceTimersByTimeAsync(2 * 60 * 1000 + 10_000));
    expect(result.current.timedOut).toBe(true);
    expect(poll.mock.calls.length).toBeLessThanOrEqual(40);
    const calls = poll.mock.calls.length;
    await act(() => vi.advanceTimersByTimeAsync(60_000));
    expect(poll.mock.calls.length).toBe(calls);
  });

  it("keeps polling through a network error", async () => {
    const poll = vi.fn().mockRejectedValueOnce(new Error("offline")).mockResolvedValueOnce(status("FAILED", { resultDesc: "Request cancelled by user" }));
    const { result } = renderHook(() => useStkPolling("i1", poll));
    await act(() => vi.advanceTimersByTimeAsync(3000));
    expect(result.current.lastError).toBeTruthy();
    await act(() => vi.advanceTimersByTimeAsync(3000));
    expect(result.current.status?.status).toBe("FAILED");
    expect(result.current.status?.resultDesc).toBe("Request cancelled by user");
  });

  it("does nothing without an intent", async () => {
    const poll = vi.fn();
    renderHook(() => useStkPolling(null, poll));
    await act(() => vi.advanceTimersByTimeAsync(10_000));
    expect(poll).not.toHaveBeenCalled();
  });
});

describe("parseWholeShillings", () => {
  it("accepts whole shillings ≥ 1 only", () => {
    expect(parseWholeShillings("1,500")).toBe(1500);
    expect(parseWholeShillings("1")).toBe(1);
    expect(parseWholeShillings("0")).toBeNull();
    expect(parseWholeShillings("150.50")).toBeNull();
    expect(parseWholeShillings("")).toBeNull();
  });
});
