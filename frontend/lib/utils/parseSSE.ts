export type SSEEvent = {
  type: string;
  data: string;
};

/**
 * Parses a chunk of SSE text into events.
 * SSE format: "event: <type>\ndata: <data>\n\n"
 */
export function parseSSEChunk(chunk: string): SSEEvent[] {
  const events: SSEEvent[] = [];
  const blocks = chunk.split("\n\n");

  for (const block of blocks) {
    if (!block.trim()) continue;

    let type = "message";
    let data = "";

    const lines = block.split("\n");
    for (const line of lines) {
      if (line.startsWith("event:")) {
        type = line.slice(6).trim();
      } else if (line.startsWith("data:")) {
        data = line.slice(5).trim();
      }
    }

    if (data) {
      events.push({ type, data });
    }
  }

  return events;
}
