import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { apiFetch, ApiError } from "@/lib/api/http";

describe("apiFetch", () => {
  const originalFetch = globalThis.fetch;

  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    globalThis.fetch = originalFetch;
    vi.useRealTimers();
  });

  it("should return parsed JSON on success", async () => {
    globalThis.fetch = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ id: 1, name: "test" }), {
        status: 200,
        headers: { "Content-Type": "application/json" }
      })
    );

    const result = await apiFetch<{ id: number; name: string }>("/test");
    expect(result).toEqual({ id: 1, name: "test" });
  });

  it("should throw ApiError on non-ok response", async () => {
    globalThis.fetch = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ message: "Not found" }), { status: 404 })
    );

    try {
      await apiFetch("/missing");
      expect.unreachable("should have thrown");
    } catch (error) {
      expect(error).toBeInstanceOf(ApiError);
      expect((error as ApiError).status).toBe(404);
    }
  });

  it("should return undefined for 204 No Content", async () => {
    globalThis.fetch = vi.fn().mockResolvedValue(
      new Response(null, { status: 204 })
    );

    const result = await apiFetch("/delete");
    expect(result).toBeUndefined();
  });

  it("should handle non-JSON response body", async () => {
    globalThis.fetch = vi.fn().mockResolvedValue(
      new Response("plain text response", { status: 200 })
    );

    const result = await apiFetch<string>("/text");
    expect(result).toBe("plain text response");
  });

  it("should set Content-Type header when body is provided", async () => {
    globalThis.fetch = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ ok: true }), { status: 200 })
    );

    await apiFetch("/create", {
      method: "POST",
      body: JSON.stringify({ name: "test" })
    });

    const fetchCall = vi.mocked(globalThis.fetch).mock.calls[0];
    const headers = fetchCall[1]?.headers as Headers;
    expect(headers.get("Content-Type")).toBe("application/json");
  });
});
