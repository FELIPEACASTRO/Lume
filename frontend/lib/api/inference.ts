import { apiFetch, getApiBaseUrl } from "@/lib/api/http";
import { MultiInferenceRequest, MultiInferenceResult } from "@/lib/api/types";
import { parseSSEChunk, SSEEvent } from "@/lib/utils/parseSSE";

export type StreamInferenceRequest = {
  providerCode: string;
  modelCode: string;
  messages?: Array<{ role: string; content: string }>;
  prompt?: string;
  systemPrompt?: string;
  maxTokens?: number;
  temperature?: number;
};

export type StreamCallbacks = {
  onEvent: (event: SSEEvent) => void;
  onDone: () => void;
  onError: (error: Error) => void;
};

export function executeMultiInference(request: MultiInferenceRequest): Promise<MultiInferenceResult> {
  return apiFetch<MultiInferenceResult>("/v1/inference/multi-execute", {
    method: "POST",
    body: JSON.stringify(request)
  });
}

export function streamInference(
  request: StreamInferenceRequest,
  callbacks: StreamCallbacks,
  signal?: AbortSignal
): void {
  streamFetch(`${getApiBaseUrl()}/v1/inference/stream`, request, callbacks, signal);
}

export function streamMultiInference(
  request: MultiInferenceRequest,
  callbacks: StreamCallbacks,
  signal?: AbortSignal
): void {
  streamFetch(`${getApiBaseUrl()}/v1/inference/multi-stream`, request, callbacks, signal);
}

function streamFetch(
  url: string,
  body: unknown,
  callbacks: StreamCallbacks,
  signal?: AbortSignal
): void {
  fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify(body),
    signal
  })
    .then(async (response) => {
      if (!response.ok) {
        throw new Error(`Stream failed: ${response.status}`);
      }

      const reader = response.body?.getReader();
      if (!reader) {
        throw new Error("No readable stream");
      }

      const decoder = new TextDecoder();
      let buffer = "";

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });

        const lastDoubleNewline = buffer.lastIndexOf("\n\n");
        if (lastDoubleNewline !== -1) {
          const complete = buffer.slice(0, lastDoubleNewline + 2);
          buffer = buffer.slice(lastDoubleNewline + 2);

          const events = parseSSEChunk(complete);
          for (const event of events) {
            callbacks.onEvent(event);
          }
        }
      }

      if (buffer.trim()) {
        const events = parseSSEChunk(buffer);
        for (const event of events) {
          callbacks.onEvent(event);
        }
      }

      callbacks.onDone();
    })
    .catch((error) => {
      if (signal?.aborted) return;
      callbacks.onError(error instanceof Error ? error : new Error(String(error)));
    });
}
