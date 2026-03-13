import { describe, it, expect, vi, afterEach } from "vitest";
import { groupByDate } from "@/lib/utils/dateGrouping";

type Item = { id: string; updatedAt: string };

describe("groupByDate", () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it("should group items into 'Hoje' for today's date", () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date("2026-03-13T14:00:00Z"));

    const items: Item[] = [
      { id: "1", updatedAt: "2026-03-13T10:00:00Z" }
    ];
    const groups = groupByDate(items, (item) => item.updatedAt);
    expect(groups).toHaveLength(1);
    expect(groups[0].label).toBe("Hoje");
    expect(groups[0].items).toHaveLength(1);
  });

  it("should group items into 'Ontem' for yesterday", () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date("2026-03-13T14:00:00Z"));

    const items: Item[] = [
      { id: "1", updatedAt: "2026-03-12T10:00:00Z" }
    ];
    const groups = groupByDate(items, (item) => item.updatedAt);
    expect(groups).toHaveLength(1);
    expect(groups[0].label).toBe("Ontem");
  });

  it("should group items into multiple buckets", () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date("2026-03-13T14:00:00Z"));

    const items: Item[] = [
      { id: "1", updatedAt: "2026-03-13T08:00:00Z" },
      { id: "2", updatedAt: "2026-03-12T08:00:00Z" },
      { id: "3", updatedAt: "2026-03-10T08:00:00Z" },
      { id: "4", updatedAt: "2026-02-01T08:00:00Z" }
    ];
    const groups = groupByDate(items, (item) => item.updatedAt);
    const labels = groups.map((g) => g.label);
    expect(labels).toContain("Hoje");
    expect(labels).toContain("Ontem");
    expect(labels).toContain("Ultimos 7 dias");
    expect(labels).toContain("Anteriores");
  });

  it("should return empty array for no items", () => {
    const groups = groupByDate([], (item: Item) => item.updatedAt);
    expect(groups).toHaveLength(0);
  });
});
