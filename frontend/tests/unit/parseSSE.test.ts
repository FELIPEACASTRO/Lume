import { describe, it, expect } from "vitest";
import { parseSSEChunk } from "@/lib/utils/parseSSE";

describe("parseSSEChunk", () => {
  it("should parse a single SSE event", () => {
    const chunk = "event: delta\ndata: hello world\n\n";
    const events = parseSSEChunk(chunk);
    expect(events).toEqual([{ type: "delta", data: "hello world" }]);
  });

  it("should parse multiple SSE events", () => {
    const chunk = "event: delta\ndata: first\n\nevent: delta\ndata: second\n\nevent: done\ndata: end\n\n";
    const events = parseSSEChunk(chunk);
    expect(events).toHaveLength(3);
    expect(events[0]).toEqual({ type: "delta", data: "first" });
    expect(events[1]).toEqual({ type: "delta", data: "second" });
    expect(events[2]).toEqual({ type: "done", data: "end" });
  });

  it("should default to 'message' type when event line is missing", () => {
    const chunk = "data: no event type\n\n";
    const events = parseSSEChunk(chunk);
    expect(events).toEqual([{ type: "message", data: "no event type" }]);
  });

  it("should skip empty blocks", () => {
    const chunk = "\n\n\n\nevent: delta\ndata: content\n\n\n\n";
    const events = parseSSEChunk(chunk);
    expect(events).toEqual([{ type: "delta", data: "content" }]);
  });

  it("should skip blocks with no data", () => {
    const chunk = "event: heartbeat\n\n";
    const events = parseSSEChunk(chunk);
    expect(events).toHaveLength(0);
  });
});
