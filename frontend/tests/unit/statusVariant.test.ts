import { describe, it, expect } from "vitest";
import { toStatusVariant } from "@/lib/utils/statusVariant";

describe("toStatusVariant", () => {
  it("should return 'active' for running/ready/completed/live states", () => {
    expect(toStatusVariant("running")).toBe("active");
    expect(toStatusVariant("ready")).toBe("active");
    expect(toStatusVariant("completed")).toBe("active");
    expect(toStatusVariant("core_live")).toBe("active");
    expect(toStatusVariant("active")).toBe("active");
  });

  it("should return 'unavailable' for error/failed states", () => {
    expect(toStatusVariant("error")).toBe("unavailable");
    expect(toStatusVariant("failed")).toBe("unavailable");
    expect(toStatusVariant("unavailable")).toBe("unavailable");
  });

  it("should return 'restricted' for blocked/restricted states", () => {
    expect(toStatusVariant("blocked")).toBe("restricted");
    expect(toStatusVariant("restricted")).toBe("restricted");
  });

  it("should return 'attention' as default for unknown states", () => {
    expect(toStatusVariant("pending")).toBe("attention");
    expect(toStatusVariant("queued")).toBe("attention");
    expect(toStatusVariant("")).toBe("attention");
  });

  it("should be case-insensitive", () => {
    expect(toStatusVariant("RUNNING")).toBe("active");
    expect(toStatusVariant("Blocked")).toBe("restricted");
    expect(toStatusVariant("FAILED")).toBe("unavailable");
  });
});
